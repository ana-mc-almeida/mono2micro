import { screen, within, waitFor } from '@testing-library/react';
import { fakeBackend, held, reply } from '../../testing/fakeBackend';
import { renderAt } from '../../testing/renderAt';
import { cardOf } from '../../testing/screenReaders';

// The software system's page replaces upstream's strategy screen (Strategies.test.js
// at slice 1). Ticket 07 addresses it by id; ticket 09 removed the strategies, so the
// page now offers the Similarity kinds itself. The kind's form and the Similarity
// cards are pinned in similarity/forms/SimilarityForm.test.js.
//
// Response shapes follow the backend: SoftwareSystemDto, RepresentationDto,
// SoftwareSystemController's representation groups, the DerivationRecord of
// /similarities/available and ProfileController's profiled representation types.

const AT = '/softwareSystems/sys1';

function representation(type) {
    return { id: 'rep-' + type, name: 'ldod & ' + type, type, softwareSystemId: 'sys1' };
}

const REPRESENTATIONS = [representation('IDToEntity'), representation('Accesses')];

const ALL_REPRESENTATIONS = [
    ...REPRESENTATIONS,
    ...['Changes Authorship', 'File Changes', 'EntityToID', 'Code Embeddings', 'Structure'].map(representation),
];

const REPRESENTATION_GROUPS = {
    'Accesses Based': ['IDToEntity', 'Accesses'],
    'Repository Based': ['IDToEntity', 'Accesses', 'Changes Authorship', 'File Changes'],
    'Code Embeddings Based': ['IDToEntity', 'EntityToID', 'Accesses', 'Code Embeddings'],
    'Structure Based': ['EntityToID', 'IDToEntity', 'Accesses', 'Structure'],
};

// Ticket 09: the kinds the backend offers on the software system's representations.
const OFFERED_KINDS = {
    features: [
        { name: 'Accesses', available: true, failedRequirements: [] },
        { name: 'Repository', available: false, failedRequirements: ['Repository Based'] },
        { name: 'Class Vectorization', available: true, failedRequirements: [] },
    ],
};

const SOFTWARE_SYSTEM = 'GET /softwareSystems/sys1';
const LOAD_REPRESENTATIONS = [
    'GET /softwareSystems/sys1/getRepresentations',
    'GET /softwareSystems/sys1/profiles/representationTypes',
    'GET /softwareSystems/sys1/similarities',
    'GET /softwareSystems/sys1/similarities/available',
];
const LOAD_GROUPS = 'GET /softwareSystems/sys1/getSoftwareSystemRepresentationGroups';
const ON_OPENING = [SOFTWARE_SYSTEM, ...LOAD_REPRESENTATIONS, LOAD_GROUPS, 'GET /softwareSystems/getRepresentationGroups'];
const ADD_REPOSITORY = 'POST /softwareSystems/sys1/addRepresentations/Repository%20Based';

function section(heading) {
    return screen.getByRole('heading', { name: heading }).nextElementSibling;
}

function cardTitles(heading) {
    return Array.from(section(heading).querySelectorAll('.card-header')).map(h => h.textContent);
}

// The "+" card that opens the Add modal has no text or role; its only handle is the icon.
function addCard(heading) {
    return within(section(heading)).getByTestId('AddCircleOutlineIcon');
}

// Some card titles also show elsewhere on the screen; a card's header names it.
const HEADER = { selector: '.card-header' };

function fileField(dialog, label) {
    return within(dialog).getByText(label).parentElement.querySelector('input[type=file]');
}

async function openPage() {
    const view = renderAt(AT);
    await screen.findByText('Accesses', HEADER);
    await screen.findByText('ldod');
    await screen.findByRole('radio', { name: 'Accesses' });
    return view;
}

describe('the software system page', () => {
    let backend;
    beforeEach(() => {
        backend = fakeBackend({
            [SOFTWARE_SYSTEM]: { id: 'sys1', name: 'ldod', representations: [] },
            'GET /softwareSystems/sys1/getRepresentations': REPRESENTATIONS,
            'GET /softwareSystems/sys1/profiles/representationTypes': ['Accesses', 'Structure'],
            'GET /softwareSystems/sys1/similarities': [],
            'GET /softwareSystems/sys1/similarities/available': OFFERED_KINDS,
            [LOAD_GROUPS]: ['Accesses Based'],
            'GET /softwareSystems/getRepresentationGroups': REPRESENTATION_GROUPS,
            [ADD_REPOSITORY]: reply(201),
            'DELETE /representation/rep-Accesses/delete': null,
        });
    });

    it('loads the software system, its representations, Similarities, offered kinds and representation groups on opening', async () => {
        await openPage();

        expect(backend.sent()).toEqual(ON_OPENING);
        expect(backend.unhandled).toEqual([]);
    });

    it('shows breadcrumbs to Home and Software Systems, the software system name, and a Help popover', async () => {
        const { user } = await openPage();

        const crumbs = screen.getByRole('navigation', { name: 'breadcrumb' });
        expect(within(crumbs).getByText('Home')).toHaveAttribute('href', '/');
        expect(within(crumbs).getByText('Software Systems')).toHaveAttribute('href', '/softwareSystems');
        expect(within(crumbs).getByText('ldod')).not.toHaveAttribute('href');

        await user.click(screen.getByRole('button', { name: 'Help' }));
        expect(await screen.findByText(/Select the decomposition type and add the required files/)).toBeInTheDocument();
    });

    it('shows one card per representation, titled by its type, under Representation Files', async () => {
        backend.on('GET /softwareSystems/sys1/getRepresentations', ALL_REPRESENTATIONS);
        await openPage();

        expect(cardTitles('Representation Files')).toEqual(
            ['IDToEntity', 'Accesses', 'Changes Authorship', 'File Changes', 'EntityToID', 'Code Embeddings', 'Structure']);
    });

    // Ticket 08: the profiles are records listed on their own screen, so the link reads
    // "Profiles" (upstream: "Change Profile"), and goes there by representation id (07).
    // Ticket 09: only a representation the backend offers profiles on gets the link.
    it('gives the profiled representations\' cards a Profiles link to the profiles screen, the others only Delete', async () => {
        backend.on('GET /softwareSystems/sys1/getRepresentations', ALL_REPRESENTATIONS);
        backend.on('GET /softwareSystems/sys1/profiles/representationTypes', ['Accesses']);
        await openPage();

        const controls = type => within(cardOf(type, HEADER)).getAllByRole(/button|link/).map(b => b.textContent);
        expect(controls('Accesses')).toEqual(['Profiles', 'Delete']);
        ['IDToEntity', 'Changes Authorship', 'File Changes', 'EntityToID', 'Code Embeddings', 'Structure']
            .forEach(type => expect(controls(type)).toEqual(['Delete']));

        expect(within(cardOf('Accesses', HEADER)).getByText('Profiles'))
            .toHaveAttribute('href', '/softwareSystems/sys1/rep-Accesses/profiles');
    });

    it('gives the Structure card the Profiles link, and not the Accesses one, when the backend offers profiles only on Structure', async () => {
        backend.on('GET /softwareSystems/sys1/getRepresentations', ALL_REPRESENTATIONS);
        backend.on('GET /softwareSystems/sys1/profiles/representationTypes', ['Structure']);
        await openPage();

        const controls = type => within(cardOf(type, HEADER)).getAllByRole(/button|link/).map(b => b.textContent);
        expect(controls('Structure')).toEqual(['Profiles', 'Delete']);
        expect(controls('Accesses')).toEqual(['Delete']);
        expect(within(cardOf('Structure', HEADER)).getByText('Profiles'))
            .toHaveAttribute('href', '/softwareSystems/sys1/rep-Structure/profiles');
    });

    // Ticket 09: the Similarities section replaces the strategies; the kinds come from the
    // backend in one request, so upstream's stale Criteria list (U4) has no counterpart.
    it('offers one radio per kind the backend offers, none chosen, and links to the recommendation', async () => {
        await openPage();

        const heading = screen.getByRole('heading', { name: /^Similarities/ });
        expect(within(heading).getByText('Recommendation')).toHaveAttribute('href', '/softwareSystems/sys1/recommendation');
        expect(screen.getAllByRole('radio').map(r => [r.labels[0].textContent, r.checked]))
            .toEqual([['Accesses', false], ['Class Vectorization', false]]);
        expect(screen.queryByRole('button', { name: 'Generate Similarity Distances' })).not.toBeInTheDocument();
    });

    it('says to upload representations when the backend offers no kind', async () => {
        backend.on('GET /softwareSystems/sys1/similarities/available', { features: [] });
        renderAt(AT);

        expect(await screen.findByText('Upload representations to be offered a Similarity kind.')).toBeInTheDocument();
        expect(screen.queryAllByRole('radio')).toEqual([]);
    });

    it('asks to confirm a representation deletion, then deletes it by id and reloads everything that follows the representations', async () => {
        const { user } = await openPage();

        await user.click(within(cardOf('Accesses', HEADER)).getByRole('button', { name: 'Delete' }));
        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText('Representation Deletion')).toBeInTheDocument();
        // Ticket 09: the deletion takes the Similarities, not strategies, with it
        expect(dialog).toHaveTextContent('Similarities that read this representation will be deleted.');
        expect(dialog).toHaveTextContent('Are you sure you want to delete this Representation?');
        expect(within(dialog).getAllByRole('button').map(b => b.textContent)).toEqual(['Close', 'Delete']);
        expect(backend.sent()).toEqual(ON_OPENING);

        await user.click(within(dialog).getByRole('button', { name: 'Delete' }));

        await waitFor(() => expect(backend.sent()).toEqual([...ON_OPENING,
            'DELETE /representation/rep-Accesses/delete',
            LOAD_GROUPS,
            ...LOAD_REPRESENTATIONS,
        ]));
    });

    // U25 fixed with P3: the page renders a ToastContainer, so the toast shows.
    it('shows a toast while a representation deletion is pending', async () => {
        const deletion = held(null);
        backend.on('DELETE /representation/rep-Accesses/delete', deletion.route);
        const { user } = await openPage();

        await user.click(within(cardOf('Accesses', HEADER)).getByRole('button', { name: 'Delete' }));
        await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Delete' }));
        await waitFor(() => expect(backend.sent()).toContain('DELETE /representation/rep-Accesses/delete'));

        expect(await screen.findByText('Deleting representation...')).toBeInTheDocument();

        deletion.release();
        await waitFor(() => expect(backend.sent()).toEqual([...ON_OPENING,
            'DELETE /representation/rep-Accesses/delete', LOAD_GROUPS, ...LOAD_REPRESENTATIONS]));
        await screen.findByRole('radio', { name: 'Accesses' });
    });

    it('sends nothing when the representation deletion is closed', async () => {
        const { user } = await openPage();

        await user.click(within(cardOf('Accesses', HEADER)).getByRole('button', { name: 'Delete' }));
        await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Close' }));

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(backend.sent()).toEqual(ON_OPENING);
    });

    describe('Add Representation', () => {
        async function openAddRepresentation(user) {
            await user.click(addCard('Representation Files'));
            return screen.findByRole('dialog');
        }

        async function chooseRepository(user, dialog) {
            await user.click(within(dialog).getByRole('button', { name: 'Choose Representation Type' }));
            await user.click(await within(dialog).findByRole('button', { name: 'Repository Based' }));
        }

        async function uploadRepositoryFiles(user, dialog) {
            await user.upload(fileField(dialog, 'Changes Authorship File'), new File(['{}'], 'authors.json'));
            await user.upload(fileField(dialog, 'File Changes File'), new File(['{}'], 'commits.json'));
        }

        it('opens from the + card, offering only the representation groups the software system does not have yet', async () => {
            const { user } = await openPage();

            const dialog = await openAddRepresentation(user);
            expect(within(dialog).getByText('Add Representation')).toBeInTheDocument();
            expect(within(dialog).getAllByRole('button').map(b => b.textContent))
                .toEqual(['Choose Representation Type', 'Close']);

            await user.click(within(dialog).getByRole('button', { name: 'Choose Representation Type' }));
            const options = await within(dialog).findAllByRole('button', { name: /Based$/ });
            expect(options.map(o => o.textContent))
                .toEqual(['Repository Based', 'Code Embeddings Based', 'Structure Based']);
            expect(backend.sent()).toEqual(ON_OPENING);
        });

        it('asks for a file per missing representation of the chosen group, marking the ones present as Already Added', async () => {
            const { user } = await openPage();
            const dialog = await openAddRepresentation(user);

            await chooseRepository(user, dialog);

            expect(within(dialog).getByRole('button', { name: 'Repository Based', expanded: false })).toBeInTheDocument();
            expect(within(dialog).queryByRole('button', { name: 'Choose Representation Type' })).not.toBeInTheDocument();
            expect(Array.from(dialog.querySelectorAll('label')).map(l => l.textContent))
                .toEqual(['IDToEntity File', 'Accesses File', 'Changes Authorship File', 'File Changes File']);
            expect(within(dialog).getAllByRole('button', { name: 'Already Added' })).toHaveLength(2);
            expect(fileField(dialog, 'IDToEntity File')).toBeNull();
            expect(fileField(dialog, 'Accesses File')).toBeNull();
            expect(fileField(dialog, 'Changes Authorship File')).not.toBeNull();
            expect(fileField(dialog, 'File Changes File')).not.toBeNull();
        });

        it('enables Submit only once every missing file is chosen', async () => {
            const { user } = await openPage();
            const dialog = await openAddRepresentation(user);
            await chooseRepository(user, dialog);

            const submit = within(dialog).getByRole('button', { name: 'Submit' });
            expect(submit).toBeDisabled();

            await user.upload(fileField(dialog, 'Changes Authorship File'), new File(['{}'], 'authors.json'));
            expect(submit).toBeDisabled();

            await user.upload(fileField(dialog, 'File Changes File'), new File(['{}'], 'commits.json'));
            expect(submit).toBeEnabled();
        });

        it('uploads the chosen files under the group, then reloads the representations and closes', async () => {
            const { user } = await openPage();
            const dialog = await openAddRepresentation(user);
            await chooseRepository(user, dialog);
            await uploadRepositoryFiles(user, dialog);

            await user.click(within(dialog).getByRole('button', { name: 'Submit' }));

            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            await waitFor(() => expect(backend.sent()).toEqual([...ON_OPENING,
                ADD_REPOSITORY,
                LOAD_GROUPS,
                ...LOAD_REPRESENTATIONS,
            ]));
            expect(backend.bodyOf(ADD_REPOSITORY)).toEqual({
                representationTypes: ['Changes Authorship', 'File Changes'],
                representations: ['File(authors.json)', 'File(commits.json)'],
            });
        });

        // Ticket 09: a 401 no longer says "Strategy already submitted.", a message upstream
        // had copied from the strategy form.
        it.each([
            [400, 'Upload failed.'],
            [401, 'Upload failed. These representations were already submitted.'],
            [404, 'Upload failed. Invalid datafile path.'],
        ])('on a %i reply, stays open, says "%s" and disables Submit', async (status, message) => {
            backend.on(ADD_REPOSITORY, reply(status));
            const { user } = await openPage();
            const dialog = await openAddRepresentation(user);
            await chooseRepository(user, dialog);
            await uploadRepositoryFiles(user, dialog);

            await user.click(within(dialog).getByRole('button', { name: 'Submit' }));

            expect(await within(dialog).findByText(message)).toBeInTheDocument();
            expect(within(dialog).getByRole('button', { name: 'Submit' })).toBeDisabled();
            expect(backend.sent()).toEqual([...ON_OPENING, ADD_REPOSITORY]);
        });
    });
});
