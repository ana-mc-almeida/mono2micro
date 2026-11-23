import { screen, within, waitFor } from '@testing-library/react';
import { fakeBackend, held, reply } from '../../testing/fakeBackend';
import { renderAt } from '../../testing/renderAt';

// Ticket 07: the screen is addressed by the software system's and the
// representation's ids. Ticket 08: profiles are records of their own ({id,
// name, units}), loaded apart from the representation, with a computed
// Unassigned list; the screen says "units", not "functionalities".
const SOFTWARE_SYSTEM = { id: 'sys1', name: 'ldod', representations: [] };
const REPRESENTATION = { id: 'rep1', name: 'ldod & Accesses', type: 'Accesses', softwareSystemId: 'sys1' };
const PROFILES = {
    profiles: [
        { id: 'p1', name: 'Users', units: ['FenixController.login', 'FenixController.logout', 'ReaderController.read'] },
        { id: 'p2', name: 'Admin', units: ['AdminController.removeUser'] },
        { id: 'p3', name: 'Empty', units: [] },
    ],
    unassigned: ['ReaderController.search'],
};

const LOAD_SYSTEM = 'GET /softwareSystems/sys1';
const LOAD_REPRESENTATION = 'GET /representation/rep1/getRepresentation';
const LOAD_PROFILES = 'GET /representation/rep1/getProfiles';
const OPENING = [LOAD_SYSTEM, LOAD_REPRESENTATION, LOAD_PROFILES];

async function openPage() {
    const view = renderAt('/softwareSystems/sys1/rep1/profiles');
    await screen.findByRole('checkbox', { name: 'FenixController.login' });
    return view;
}

function profileNames() {
    return screen.getAllByText(/^(Users|Admin|Empty|Unassigned)$/, { selector: 'div' }).map(d => d.firstChild.textContent);
}

describe('the profiles screen', () => {
    let backend;
    beforeEach(() => {
        backend = fakeBackend({
            [LOAD_SYSTEM]: SOFTWARE_SYSTEM,
            [LOAD_REPRESENTATION]: REPRESENTATION,
            [LOAD_PROFILES]: PROFILES,
            'POST /representation/rep1/addProfile': reply(201),
            'POST /representation/rep1/moveUnits': reply(200),
            'DELETE /profile/p3/delete': null,
        });
    });

    it('loads the software system, the representation and its profiles by id on opening', async () => {
        await openPage();

        expect(backend.sent()).toEqual(OPENING);
        expect(backend.unhandled).toEqual([]);
    });

    it('shows breadcrumbs down to Profiles, with the representation crumb linking back to the software system', async () => {
        await openPage();

        const crumbs = screen.getByRole('navigation', { name: 'breadcrumb' });
        await waitFor(() => expect(within(crumbs).getAllByRole('listitem').map(i => i.textContent))
            .toEqual(['Home', 'Software Systems', 'ldod', 'ldod & Accesses', 'Profiles']));
        expect(within(crumbs).getByText('Home')).toHaveAttribute('href', '/');
        expect(within(crumbs).getByText('Software Systems')).toHaveAttribute('href', '/softwareSystems');
        expect(within(crumbs).getByText('ldod')).toHaveAttribute('href', '/softwareSystems/sys1');
        // U15: the representation crumb points at the software system page too.
        expect(within(crumbs).getByText('ldod & Accesses')).toHaveAttribute('href', '/softwareSystems/sys1');
        expect(within(crumbs).getByText('Profiles')).not.toHaveAttribute('href');
    });

    // U16 is resolved by ticket 08: the list waits for the profiles, not the
    // representation, so nothing (no stray "0") shows while they load.
    it('shows nothing under Profiles while the profiles load', async () => {
        const profiles = held(PROFILES);
        backend.on(LOAD_PROFILES, profiles.route);
        renderAt('/softwareSystems/sys1/rep1/profiles');

        const heading = await screen.findByRole('heading', { name: 'Profiles' });
        await waitFor(() => expect(backend.sent()).toEqual(OPENING));
        expect(heading.nextSibling).toBeNull();
        expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();

        profiles.release();
        expect(await screen.findByRole('checkbox', { name: 'FenixController.login' })).toBeInTheDocument();
    });

    it('has one field, the profile name (at most 30 characters), and disables Create Profile until it is filled', async () => {
        const { user } = await openPage();

        expect(screen.getByRole('heading', { name: 'Create Profile' })).toBeInTheDocument();
        expect(screen.getAllByRole('textbox').map(f => f.placeholder)).toEqual(['Profile Name']);
        const field = screen.getByLabelText('Profile Name');
        expect(field).toHaveAttribute('maxLength', '30');
        const create = screen.getByRole('button', { name: 'Create Profile' });
        expect(create).toBeDisabled();

        await user.type(field, 'Reader');
        expect(create).toBeEnabled();
    });

    it('creates a profile by name as a query parameter, then reloads the profiles', async () => {
        const { user } = await openPage();

        await user.type(screen.getByLabelText('Profile Name'), 'Reader Only');
        await user.click(screen.getByRole('button', { name: 'Create Profile' }));

        expect(await screen.findByText('Upload completed successfully.')).toBeInTheDocument();
        expect(backend.sent()).toEqual([...OPENING, 'POST /representation/rep1/addProfile?profile=Reader+Only', LOAD_PROFILES]);
        expect(backend.bodyOf('POST /representation/rep1/addProfile?profile=Reader+Only')).toBeUndefined();
        // The name stays in the field after creating.
        expect(screen.getByLabelText('Profile Name')).toHaveValue('Reader Only');
    });

    // Ticket 08's endpoint answers 201 Created; any other success status reads as a failure.
    it('says "Upload failed." on a 200 reply, without reloading', async () => {
        backend.on('POST /representation/rep1/addProfile', reply(200));
        const { user } = await openPage();

        await user.type(screen.getByLabelText('Profile Name'), 'Reader');
        await user.click(screen.getByRole('button', { name: 'Create Profile' }));

        expect(await screen.findByText('Upload failed.')).toBeInTheDocument();
        expect(backend.sent()).toEqual([...OPENING, 'POST /representation/rep1/addProfile?profile=Reader']);
    });

    it('says when the profile name is taken', async () => {
        backend.on('POST /representation/rep1/addProfile', reply(401));
        const { user } = await openPage();

        await user.type(screen.getByLabelText('Profile Name'), 'Users');
        await user.click(screen.getByRole('button', { name: 'Create Profile' }));

        expect(await screen.findByText('Upload failed. Profile name already exists.')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Create Profile' })).toBeEnabled();
    });

    // Ticket 08: every profile can be deleted (one in use is retired), and the
    // units no profile selects are listed, read-only, under Unassigned.
    it('lists each profile with a checkbox per unit and a delete button, then Unassigned with its units', async () => {
        await openPage();

        expect(profileNames()).toEqual(['Users', 'Admin', 'Empty', 'Unassigned']);
        expect(screen.getAllByRole('checkbox').map(c => c.labels[0].textContent)).toEqual([
            'FenixController.login', 'FenixController.logout', 'ReaderController.read', 'AdminController.removeUser',
            'ReaderController.search',
        ]);
        screen.getAllByRole('checkbox').forEach(c => expect(c).not.toBeChecked());
        expect(screen.getAllByRole('button', { name: '-' })).toHaveLength(3);
        ['Users', 'Admin', 'Empty'].forEach(name => expect(within(screen.getByText(name, { selector: 'div' }))
            .getByRole('button', { name: '-' })).toBeInTheDocument());
        expect(within(screen.getByText('Unassigned', { selector: 'div' })).queryByRole('button')).not.toBeInTheDocument();
        expect(screen.getByText('The units no profile selects. This list is computed and cannot be edited or chosen for a Similarity.'))
            .toBeInTheDocument();
    });

    it('shows a toolbar to move units, with Submit disabled until units and a target are chosen', async () => {
        const { user } = await openPage();

        // "Move selected units to" is a plain button that does nothing.
        expect(screen.getAllByRole('button').map(b => b.textContent).slice(1, 4))
            .toEqual(['Move selected units to', 'Profile', 'Submit']);
        const submit = screen.getByRole('button', { name: 'Submit' });
        expect(submit).toBeDisabled();

        await user.click(screen.getByRole('button', { name: 'Profile' }));
        const options = await screen.findAllByRole('button', { name: /^(Users|Admin|Empty|Unassigned)$/ });
        expect(options.map(o => o.textContent)).toEqual(['Users', 'Admin', 'Empty', 'Unassigned']);
        await user.click(screen.getByRole('button', { name: 'Admin' }));
        expect(screen.getByRole('button', { name: 'Admin', expanded: false })).toBeInTheDocument();
        expect(submit).toBeDisabled();

        await user.click(screen.getByRole('checkbox', { name: 'FenixController.login' }));
        expect(submit).toBeEnabled();

        await user.click(screen.getByRole('checkbox', { name: 'FenixController.login' }));
        expect(submit).toBeDisabled();

        await user.click(screen.getByRole('button', { name: 'Move selected units to' }));
        expect(backend.sent()).toEqual(OPENING);
    });

    // Ticket 08: a move saves new versions of the profiles it changes, so the
    // target is cleared too (its id is gone), where upstream kept it chosen.
    it('moves the selected units to the target profile by id, then clears the selection and the target and reloads', async () => {
        const { user } = await openPage();

        await user.click(screen.getByRole('checkbox', { name: 'ReaderController.read' }));
        await user.click(screen.getByRole('checkbox', { name: 'FenixController.login' }));
        await user.click(screen.getByRole('button', { name: 'Profile' }));
        await user.click(await screen.findByRole('button', { name: 'Admin' }));
        await user.click(screen.getByRole('button', { name: 'Submit' }));

        await waitFor(() => expect(backend.sent()).toEqual(
            [...OPENING, 'POST /representation/rep1/moveUnits?targetProfileId=p2', LOAD_PROFILES]));
        expect(backend.bodyOf('POST /representation/rep1/moveUnits?targetProfileId=p2'))
            .toEqual(['ReaderController.read', 'FenixController.login']);
        await waitFor(() => screen.getAllByRole('checkbox').forEach(c => expect(c).not.toBeChecked()));
        expect(screen.getByRole('button', { name: 'Profile', expanded: false })).toBeInTheDocument();
    });

    // Ticket 08: Unassigned is a move target; the request then names no target.
    it('moves the selected units to Unassigned without a target id', async () => {
        const { user } = await openPage();

        await user.click(screen.getByRole('checkbox', { name: 'AdminController.removeUser' }));
        await user.click(screen.getByRole('button', { name: 'Profile' }));
        await user.click(await screen.findByRole('button', { name: 'Unassigned' }));
        expect(screen.getByRole('button', { name: 'Unassigned', expanded: false })).toBeInTheDocument();
        await user.click(screen.getByRole('button', { name: 'Submit' }));

        await waitFor(() => expect(backend.sent()).toEqual(
            [...OPENING, 'POST /representation/rep1/moveUnits', LOAD_PROFILES]));
        expect(backend.bodyOf('POST /representation/rep1/moveUnits')).toEqual(['AdminController.removeUser']);
    });

    it('deletes a profile by id, then reloads the profiles', async () => {
        const { user } = await openPage();

        await user.click(within(screen.getByText('Empty', { selector: 'div' })).getByRole('button', { name: '-' }));

        await waitFor(() => expect(backend.sent()).toEqual(
            [...OPENING, 'DELETE /profile/p3/delete', LOAD_PROFILES]));
    });
});
