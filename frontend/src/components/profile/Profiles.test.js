import { screen, within, waitFor } from '@testing-library/react';
import { fakeBackend, reply } from '../../testing/fakeBackend';
import { renderAt } from '../../testing/renderAt';

// AccessesRepresentationDto: name, type, codebaseName, and profiles (a map of
// profile name to the functionalities in it).
const REPRESENTATION = {
    name: 'ldod & Accesses',
    type: 'Accesses',
    codebaseName: 'ldod',
    profiles: {
        Generic: ['FenixController.login', 'FenixController.logout', 'ReaderController.read'],
        Admin: ['AdminController.removeUser'],
        Empty: [],
    },
};

const REP = '/representation/ldod%20&%20Accesses';
const LOAD = `GET ${REP}/getRepresentation`;

async function openPage() {
    const view = renderAt('/codebase/ldod/ldod & Accesses/profiles');
    await screen.findByRole('checkbox', { name: 'FenixController.login' });
    return view;
}

function profileNames() {
    return screen.getAllByText(/^(Generic|Admin|Empty)$/, { selector: 'div' }).map(d => d.firstChild.textContent);
}

describe('the functionality profiles screen', () => {
    let backend;
    beforeEach(() => {
        // Route keys are URL-encoded paths, as the fake reads them.
        backend = fakeBackend({
            [LOAD]: REPRESENTATION,
            [`POST ${REP}/addAccessesProfile`]: reply(200),
            [`POST ${REP}/moveAccessesFunctionalities`]: reply(200),
            [`DELETE ${REP}/deleteAccessesProfile`]: null,
        });
    });

    it('loads the representation by its name on opening', async () => {
        await openPage();

        expect(backend.sent()).toEqual([LOAD]);
        expect(backend.unhandled).toEqual([]);
    });

    it('shows breadcrumbs down to Profiles, with the representation crumb linking back to the codebase', async () => {
        await openPage();

        const crumbs = screen.getByRole('navigation', { name: 'breadcrumb' });
        expect(within(crumbs).getAllByRole('listitem').map(i => i.textContent))
            .toEqual(['Home', 'Codebases', 'ldod', 'ldod & Accesses', 'Profiles']);
        expect(within(crumbs).getByText('Home')).toHaveAttribute('href', '/');
        expect(within(crumbs).getByText('Codebases')).toHaveAttribute('href', '/codebases');
        expect(within(crumbs).getByText('ldod')).toHaveAttribute('href', '/codebases/ldod');
        // Oddity: the representation crumb points at the codebase page too.
        expect(within(crumbs).getByText('ldod & Accesses')).toHaveAttribute('href', '/codebases/ldod');
        expect(within(crumbs).getByText('Profiles')).not.toHaveAttribute('href');
    });

    // Oddity (Profiles.js `{Object.keys(representation).length && …}`): until the
    // representation arrives, the falsy 0 is rendered as a stray "0".
    it('shows a stray "0" under Functionality Profiles while the representation loads', async () => {
        backend.on(LOAD, () => new Promise(() => {}));
        renderAt('/codebase/ldod/ldod & Accesses/profiles');

        const heading = await screen.findByRole('heading', { name: 'Functionality Profiles' });
        expect(heading.parentElement.lastChild.textContent).toBe('0');
        expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
    });

    it('has one field, the profile name (at most 30 characters), and disables Create Profile until it is filled', async () => {
        const { user } = await openPage();

        expect(screen.getByRole('heading', { name: 'Create Functionality Profile' })).toBeInTheDocument();
        expect(screen.getAllByRole('textbox').map(f => f.placeholder)).toEqual(['Profile Name']);
        const field = screen.getByLabelText('Profile Name');
        expect(field).toHaveAttribute('maxLength', '30');
        const create = screen.getByRole('button', { name: 'Create Profile' });
        expect(create).toBeDisabled();

        await user.type(field, 'Reader');
        expect(create).toBeEnabled();
    });

    it('creates a profile by name as a query parameter, then reloads the representation', async () => {
        const { user } = await openPage();

        await user.type(screen.getByLabelText('Profile Name'), 'Reader Only');
        await user.click(screen.getByRole('button', { name: 'Create Profile' }));

        expect(await screen.findByText('Upload completed successfully.')).toBeInTheDocument();
        expect(backend.sent()).toEqual([LOAD, `POST ${REP}/addAccessesProfile?profile=Reader+Only`, LOAD]);
        expect(backend.bodyOf(`POST ${REP}/addAccessesProfile?profile=Reader+Only`)).toBeUndefined();
        // The name stays in the field after creating.
        expect(screen.getByLabelText('Profile Name')).toHaveValue('Reader Only');
    });

    // The backend answers 200; any other success status reads as a failure.
    it('says "Upload failed." on a 201 reply, without reloading', async () => {
        backend.on(`POST ${REP}/addAccessesProfile`, reply(201));
        const { user } = await openPage();

        await user.type(screen.getByLabelText('Profile Name'), 'Reader');
        await user.click(screen.getByRole('button', { name: 'Create Profile' }));

        expect(await screen.findByText('Upload failed.')).toBeInTheDocument();
        expect(backend.sent()).toEqual([LOAD, `POST ${REP}/addAccessesProfile?profile=Reader`]);
    });

    it('says when the profile name is taken', async () => {
        backend.on(`POST ${REP}/addAccessesProfile`, reply(401));
        const { user } = await openPage();

        await user.type(screen.getByLabelText('Profile Name'), 'Generic');
        await user.click(screen.getByRole('button', { name: 'Create Profile' }));

        expect(await screen.findByText('Upload failed. Profile name already exists.')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Create Profile' })).toBeEnabled();
    });

    it('lists each profile with a checkbox per functionality, and a delete button only on empty profiles', async () => {
        await openPage();

        expect(profileNames()).toEqual(['Generic', 'Admin', 'Empty']);
        expect(screen.getAllByRole('checkbox').map(c => c.labels[0].textContent)).toEqual([
            'FenixController.login', 'FenixController.logout', 'ReaderController.read', 'AdminController.removeUser',
        ]);
        screen.getAllByRole('checkbox').forEach(c => expect(c).not.toBeChecked());
        expect(screen.getAllByRole('button', { name: '-' })).toHaveLength(1);
        expect(screen.getByText('Empty', { selector: 'div' })).toContainElement(screen.getByRole('button', { name: '-' }));
    });

    it('shows a toolbar to move functionalities, with Submit disabled until functionalities and a target profile are chosen', async () => {
        const { user } = await openPage();

        // "Move selected functionalities to" is a plain button that does nothing.
        expect(screen.getAllByRole('button').map(b => b.textContent).slice(1, 4))
            .toEqual(['Move selected functionalities to', 'Functionality Profile', 'Submit']);
        const submit = screen.getByRole('button', { name: 'Submit' });
        expect(submit).toBeDisabled();

        await user.click(screen.getByRole('button', { name: 'Functionality Profile' }));
        const options = await screen.findAllByRole('button', { name: /^(Generic|Admin|Empty)$/ });
        expect(options.map(o => o.textContent)).toEqual(['Generic', 'Admin', 'Empty']);
        await user.click(screen.getByRole('button', { name: 'Admin' }));
        expect(screen.getByRole('button', { name: 'Admin', expanded: false })).toBeInTheDocument();
        expect(submit).toBeDisabled();

        await user.click(screen.getByRole('checkbox', { name: 'FenixController.login' }));
        expect(submit).toBeEnabled();

        await user.click(screen.getByRole('checkbox', { name: 'FenixController.login' }));
        expect(submit).toBeDisabled();

        await user.click(screen.getByRole('button', { name: 'Move selected functionalities to' }));
        expect(backend.sent()).toEqual([LOAD]);
    });

    it('moves the selected functionalities to the target profile, then clears the selection and reloads', async () => {
        const { user } = await openPage();

        await user.click(screen.getByRole('checkbox', { name: 'ReaderController.read' }));
        await user.click(screen.getByRole('checkbox', { name: 'FenixController.login' }));
        await user.click(screen.getByRole('button', { name: 'Functionality Profile' }));
        await user.click(await screen.findByRole('button', { name: 'Admin' }));
        await user.click(screen.getByRole('button', { name: 'Submit' }));

        await waitFor(() => expect(backend.sent()).toEqual(
            [LOAD, `POST ${REP}/moveAccessesFunctionalities?targetProfile=Admin`, LOAD]));
        expect(backend.bodyOf(`POST ${REP}/moveAccessesFunctionalities?targetProfile=Admin`))
            .toEqual(['ReaderController.read', 'FenixController.login']);
        await waitFor(() => screen.getAllByRole('checkbox').forEach(c => expect(c).not.toBeChecked()));
        // The target profile stays chosen.
        expect(screen.getByRole('button', { name: 'Admin', expanded: false })).toBeInTheDocument();
    });

    it('deletes an empty profile by name as a query parameter, then reloads', async () => {
        const { user } = await openPage();

        await user.click(screen.getByRole('button', { name: '-' }));

        await waitFor(() => expect(backend.sent()).toEqual(
            [LOAD, `DELETE ${REP}/deleteAccessesProfile?profile=Empty`, LOAD]));
    });
});
