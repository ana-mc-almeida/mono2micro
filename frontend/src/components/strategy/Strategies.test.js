import { screen, within, waitFor } from '@testing-library/react';
import { fakeBackend, held, reply } from '../../testing/fakeBackend';
import { renderAt } from '../../testing/renderAt';
import { cardOf } from '../../testing/screenReaders';

// Response shapes follow the backend: RepresentationDto (+ profiles for
// Accesses/Structure), StrategyDto, Representation.representationGroupToRepresentations,
// ClusteringFactory.algorithmTypes and SciPyClustering's supported strategy types.

const REPRESENTATIONS = [
    { name: 'ldod & IDToEntity', type: 'IDToEntity', codebaseName: 'ldod' },
    { name: 'ldod & Accesses', type: 'Accesses', codebaseName: 'ldod', profiles: { Generic: ['Controller.a', 'Controller.b'] } },
];

const ALL_REPRESENTATIONS = [
    ...REPRESENTATIONS,
    { name: 'ldod & Changes Authorship', type: 'Changes Authorship', codebaseName: 'ldod' },
    { name: 'ldod & File Changes', type: 'File Changes', codebaseName: 'ldod' },
    { name: 'ldod & EntityToID', type: 'EntityToID', codebaseName: 'ldod' },
    { name: 'ldod & Code Embeddings', type: 'Code Embeddings', codebaseName: 'ldod' },
    { name: 'ldod & Structure', type: 'Structure', codebaseName: 'ldod', profiles: { Generic: ['Controller.a'] } },
];

const REPRESENTATION_GROUPS = {
    'Accesses Based': ['IDToEntity', 'Accesses'],
    'Repository Based': ['IDToEntity', 'Accesses', 'Changes Authorship', 'File Changes'],
    'Code Embeddings Based': ['IDToEntity', 'EntityToID', 'Accesses', 'Code Embeddings'],
    'Structure Based': ['EntityToID', 'IDToEntity', 'Accesses', 'Structure'],
};

const STRATEGIES = [
    { codebaseName: 'ldod', name: 'ldod - A Strategy', algorithmType: 'SciPy Clustering', strategyTypes: ['Accesses'], parameterTypes: [] },
    { codebaseName: 'ldod', name: 'ldod - A+R Strategy', algorithmType: 'SciPy Clustering', strategyTypes: ['Accesses', 'Repository'], parameterTypes: [] },
];

const SUPPORTED_STRATEGY_TYPES = [
    'Accesses', 'Repository', 'Structure', 'Class Vectorization', 'Entity Vectorization',
    'Functionality Vectorization Call Graph', 'Functionality Vectorization Sequence Accesses',
];

const ON_OPENING = [
    'GET /codebase/ldod/getCodebaseStrategies',
    'GET /codebase/ldod/getRepresentations',
    'GET /codebase/ldod/getCodebaseRepresentationGroups',
    'GET /strategy/getAlgorithms',
    'GET /codebase/getRepresentationGroups',
];

const ALLOWABLE = 'GET /codebase/ldod/getAllowableCodebaseStrategyTypes';
const SUPPORTED = 'GET /clustering/SciPy%20Clustering/getAlgorithmSupportedStrategyTypes';

// The allowable types answer after the supported types: they are released when
// the supported types are asked, and both responses then take the same path
// through axios, so the supported types reach the screen first.
function answerAllowableLast(backend) {
    const allowable = held(SUPPORTED_STRATEGY_TYPES);
    backend.on(ALLOWABLE, allowable.route);
    backend.on(SUPPORTED, () => {
        allowable.release();
        return SUPPORTED_STRATEGY_TYPES;
    });
}

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
    const view = renderAt('/codebases/ldod');
    await screen.findByText('Accesses', { selector: '.card-header' });
    await screen.findByText('ldod - A Strategy');
    return view;
}

async function openAddStrategy(user) {
    await user.click(addCard('Strategies'));
    return screen.findByRole('dialog');
}

describe('the codebase screen', () => {
    let backend;
    beforeEach(() => {
        // Route keys are URL-encoded paths, as the fake reads them.
        backend = fakeBackend({
            'GET /codebase/ldod/getCodebaseStrategies': STRATEGIES,
            'GET /codebase/ldod/getRepresentations': REPRESENTATIONS,
            'GET /codebase/ldod/getCodebaseRepresentationGroups': ['Accesses Based'],
            'GET /strategy/getAlgorithms': ['SciPy Clustering'],
            'GET /codebase/getRepresentationGroups': REPRESENTATION_GROUPS,
            'GET /codebase/ldod/getAllowableCodebaseStrategyTypes': ['Accesses'],
            [SUPPORTED]: SUPPORTED_STRATEGY_TYPES,
            'POST /codebase/ldod/addRepresentations/Repository%20Based': reply(201),
            'POST /codebase/ldod/createStrategy': reply(201),
            'DELETE /representation/ldod%20&%20Accesses/delete': null,
            'DELETE /strategy/ldod%20-%20A%20Strategy/delete': null,
        });
    });

    it('loads strategies, representations and the add options on opening', async () => {
        await openPage();

        expect(backend.sent()).toEqual(ON_OPENING);
        expect(backend.unhandled).toEqual([]);
    });

    it('shows breadcrumbs to Home and Codebases, the codebase name, and a Help popover', async () => {
        const { user } = await openPage();

        const crumbs = screen.getByRole('navigation', { name: 'breadcrumb' });
        expect(within(crumbs).getByText('Home')).toHaveAttribute('href', '/');
        expect(within(crumbs).getByText('Codebases')).toHaveAttribute('href', '/codebases');
        expect(within(crumbs).getByText('ldod')).not.toHaveAttribute('href');

        await user.click(screen.getByRole('button', { name: 'Help' }));
        expect(await screen.findByText(/Select the decomposition type and add the required files/)).toBeInTheDocument();
    });

    it('shows one card per representation, titled by its type, under Representation Files', async () => {
        backend.on('GET /codebase/ldod/getRepresentations', ALL_REPRESENTATIONS);
        await openPage();

        expect(cardTitles('Representation Files')).toEqual(
            ['IDToEntity', 'Accesses', 'Changes Authorship', 'File Changes', 'EntityToID', 'Code Embeddings', 'Structure']);
    });

    it('gives Accesses and Structure cards a Change Profile link to the profiles screen, the others only Delete', async () => {
        backend.on('GET /codebase/ldod/getRepresentations', ALL_REPRESENTATIONS);
        await openPage();

        const controls = type => within(cardOf(type, HEADER)).getAllByRole(/button|link/).map(b => b.textContent);
        expect(controls('Accesses')).toEqual(['Change Profile', 'Delete']);
        expect(controls('Structure')).toEqual(['Change Profile', 'Delete']);
        ['IDToEntity', 'Changes Authorship', 'File Changes', 'EntityToID', 'Code Embeddings']
            .forEach(type => expect(controls(type)).toEqual(['Delete']));

        expect(within(cardOf('Accesses', HEADER)).getByText('Change Profile'))
            .toHaveAttribute('href', '/codebase/ldod/ldod & Accesses/profiles');
        expect(within(cardOf('Structure', HEADER)).getByText('Change Profile'))
            .toHaveAttribute('href', '/codebase/ldod/ldod & Structure/profiles');
    });

    it('shows one card per strategy, with its algorithm, criteria and links to its screens', async () => {
        await openPage();

        expect(cardTitles('Strategies')).toEqual(['ldod - A Strategy', 'ldod - A+R Strategy']);
        const card = cardOf('ldod - A+R Strategy', HEADER);
        expect(within(card).getByText('SciPy Clustering')).toBeInTheDocument();
        expect(within(card).getByText('Accesses')).toBeInTheDocument();
        expect(within(card).getByText('Repository')).toBeInTheDocument();
        expect(within(card).getAllByRole(/button|link/).map(b => b.textContent))
            .toEqual(['Manual Setup', 'Recommendation', 'Delete']);
        expect(within(card).getByText('Manual Setup'))
            .toHaveAttribute('href', '/codebases/ldod/ldod - A+R Strategy/similarity');
        expect(within(card).getByText('Recommendation'))
            .toHaveAttribute('href', '/codebases/ldod/ldod - A+R Strategy/recommendation');
    });

    it('asks to confirm a representation deletion, then deletes it by name and reloads everything', async () => {
        const { user } = await openPage();

        await user.click(within(cardOf('Accesses', HEADER)).getByRole('button', { name: 'Delete' }));
        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText('Representation Deletion')).toBeInTheDocument();
        expect(dialog).toHaveTextContent('Strategies created from this representation will be deleted.');
        expect(dialog).toHaveTextContent('Are you sure you want to delete this Representation?');
        expect(within(dialog).getAllByRole('button').map(b => b.textContent)).toEqual(['Close', 'Delete']);
        expect(backend.sent()).toEqual(ON_OPENING);

        await user.click(within(dialog).getByRole('button', { name: 'Delete' }));

        await waitFor(() => expect(backend.sent()).toEqual([...ON_OPENING,
            'DELETE /representation/ldod%20&%20Accesses/delete',
            'GET /codebase/ldod/getCodebaseRepresentationGroups',
            'GET /codebase/ldod/getRepresentations',
            'GET /codebase/ldod/getCodebaseStrategies',
        ]));
    });

    it('sends nothing when the representation deletion is closed', async () => {
        const { user } = await openPage();

        await user.click(within(cardOf('Accesses', HEADER)).getByRole('button', { name: 'Delete' }));
        await user.click(within(await screen.findByRole('dialog')).getByRole('button', { name: 'Close' }));

        await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
        expect(backend.sent()).toEqual(ON_OPENING);
    });

    it('deletes a strategy by name without confirmation, then reloads the strategies', async () => {
        const { user } = await openPage();

        await user.click(within(cardOf('ldod - A Strategy', HEADER)).getByRole('button', { name: 'Delete' }));

        // The screen raises "Deleting strategy..." / "Strategy deleted." toasts, but it
        // renders no ToastContainer, so nothing is shown.
        await waitFor(() => expect(backend.sent()).toEqual([...ON_OPENING,
            'DELETE /strategy/ldod%20-%20A%20Strategy/delete',
            'GET /codebase/ldod/getCodebaseStrategies',
        ]));
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    describe('Add Representation', () => {
        async function openAddRepresentation(user) {
            await user.click(addCard('Representation Files'));
            return screen.findByRole('dialog');
        }

        it('opens from the + card, offering only the representation groups the codebase does not have yet', async () => {
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

            await user.click(within(dialog).getByRole('button', { name: 'Choose Representation Type' }));
            await user.click(await within(dialog).findByRole('button', { name: 'Repository Based' }));

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
            await user.click(within(dialog).getByRole('button', { name: 'Choose Representation Type' }));
            await user.click(await within(dialog).findByRole('button', { name: 'Repository Based' }));

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
            await user.click(within(dialog).getByRole('button', { name: 'Choose Representation Type' }));
            await user.click(await within(dialog).findByRole('button', { name: 'Repository Based' }));
            await user.upload(fileField(dialog, 'Changes Authorship File'), new File(['{}'], 'authors.json'));
            await user.upload(fileField(dialog, 'File Changes File'), new File(['{}'], 'commits.json'));

            await user.click(within(dialog).getByRole('button', { name: 'Submit' }));

            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            expect(backend.sent()).toEqual([...ON_OPENING,
                'POST /codebase/ldod/addRepresentations/Repository%20Based',
                'GET /codebase/ldod/getCodebaseRepresentationGroups',
                'GET /codebase/ldod/getRepresentations',
            ]);
            expect(backend.bodyOf('POST /codebase/ldod/addRepresentations/Repository%20Based')).toEqual({
                representationTypes: ['Changes Authorship', 'File Changes'],
                representations: ['File(authors.json)', 'File(commits.json)'],
            });
        });

        // 401 says "Strategy already submitted." on the representation form too: the
        // messages are copied from the strategy form.
        it.each([
            [400, 'Upload failed.'],
            [401, 'Upload failed. Strategy already submitted.'],
            [404, 'Upload failed. Invalid datafile path.'],
        ])('on a %i reply, stays open, says "%s" and disables Submit', async (status, message) => {
            backend.on('POST /codebase/ldod/addRepresentations/Repository%20Based', reply(status));
            const { user } = await openPage();
            const dialog = await openAddRepresentation(user);
            await user.click(within(dialog).getByRole('button', { name: 'Choose Representation Type' }));
            await user.click(await within(dialog).findByRole('button', { name: 'Repository Based' }));
            await user.upload(fileField(dialog, 'Changes Authorship File'), new File(['{}'], 'authors.json'));
            await user.upload(fileField(dialog, 'File Changes File'), new File(['{}'], 'commits.json'));

            await user.click(within(dialog).getByRole('button', { name: 'Submit' }));

            expect(await within(dialog).findByText(message)).toBeInTheDocument();
            expect(within(dialog).getByRole('button', { name: 'Submit' })).toBeDisabled();
            expect(backend.sent()).toEqual([...ON_OPENING, 'POST /codebase/ldod/addRepresentations/Repository%20Based']);
        });
    });

    describe('Add Strategy', () => {
        it('opens from the + card with one Algorithm radio per algorithm, and Submit disabled', async () => {
            const { user } = await openPage();

            const dialog = await openAddStrategy(user);
            expect(within(dialog).getByText('Add Strategy')).toBeInTheDocument();
            expect(within(dialog).getByText('Algorithm')).toBeInTheDocument();
            expect(within(dialog).getAllByRole('radio').map(r => r.value)).toEqual(['SciPy Clustering']);
            expect(within(dialog).getByRole('radio', { name: 'SciPy Clustering' })).not.toBeChecked();
            expect(within(dialog).queryByText('Criteria')).not.toBeInTheDocument();
            expect(within(dialog).getAllByRole('button').map(b => b.textContent)).toEqual(['Submit', 'Close']);
            expect(within(dialog).getByRole('button', { name: 'Submit' })).toBeDisabled();
        });

        it('on choosing an algorithm, asks for the codebase\'s allowable and the algorithm\'s supported strategy types', async () => {
            const { user } = await openPage();
            const dialog = await openAddStrategy(user);

            await user.click(within(dialog).getByRole('radio', { name: 'SciPy Clustering' }));

            await waitFor(() => expect(backend.sent()).toEqual([...ON_OPENING, ALLOWABLE, SUPPORTED]));
        });

        // BUG (Strategies.js clickStrategyAlgorithm): the supported-types callback
        // intersects with `availableStrategyTypes` from a stale closure (still [] on the
        // first click), so when it answers last the Criteria list ends up empty.
        it("pins upstream's stale-closure bug: no Criteria when the supported types answer last", async () => {
            const supported = held(SUPPORTED_STRATEGY_TYPES);
            backend.on(SUPPORTED, supported.route);
            backend.on(ALLOWABLE, ['Accesses', 'Repository']);
            const { user } = await openPage();
            const dialog = await openAddStrategy(user);

            await user.click(within(dialog).getByRole('radio', { name: 'SciPy Clustering' }));
            await waitFor(() => expect(backend.sent()).toEqual([...ON_OPENING, ALLOWABLE, SUPPORTED]));
            // The allowable types are on screen; only then do the supported types answer.
            await within(dialog).findByText('Criteria');
            supported.release();

            await waitFor(() => expect(within(dialog).queryByText('Criteria')).not.toBeInTheDocument());
            expect(within(dialog).queryAllByRole('checkbox')).toEqual([]);
            expect(within(dialog).getByRole('button', { name: 'Submit' })).toBeDisabled();
        });

        it('shows the allowable types as Criteria checkboxes when they answer last, minus those already used up', async () => {
            answerAllowableLast(backend);
            backend.on('GET /codebase/ldod/getCodebaseStrategies', [...STRATEGIES,
                { codebaseName: 'ldod', name: 'ldod - CV Strategy', algorithmType: 'SciPy Clustering', strategyTypes: ['Class Vectorization'], parameterTypes: [] },
            ]);
            const { user } = await openPage();
            const dialog = await openAddStrategy(user);

            await user.click(within(dialog).getByRole('radio', { name: 'SciPy Clustering' }));

            expect(await within(dialog).findByText('Criteria')).toBeInTheDocument();
            // Accesses is in two strategies and Class Vectorization is a strategy alone: both left out.
            expect(within(dialog).getAllByRole('checkbox').map(c => c.value)).toEqual([
                'Repository', 'Structure', 'Entity Vectorization',
                'Functionality Vectorization Call Graph', 'Functionality Vectorization Sequence Accesses',
            ]);
            expect(within(dialog).getByRole('button', { name: 'Submit' })).toBeDisabled();
        });

        it('lets non-vectorization criteria combine, but a vectorization criterion only stand alone', async () => {
            answerAllowableLast(backend);
            const { user } = await openPage();
            const dialog = await openAddStrategy(user);
            await user.click(within(dialog).getByRole('radio', { name: 'SciPy Clustering' }));
            await within(dialog).findByText('Criteria');
            const box = name => within(dialog).getByRole('checkbox', { name });
            const checked = () => within(dialog).getAllByRole('checkbox').filter(c => c.checked).map(c => c.value);

            await user.click(box('Repository'));
            await user.click(box('Structure'));
            expect(checked()).toEqual(['Repository', 'Structure']);
            expect(within(dialog).getByRole('button', { name: 'Submit' })).toBeEnabled();

            await user.click(box('Class Vectorization'));
            expect(checked()).toEqual(['Class Vectorization']);

            await user.click(box('Repository'));
            expect(checked()).toEqual(['Repository']);

            await user.click(box('Repository'));
            expect(checked()).toEqual([]);
            expect(within(dialog).getByRole('button', { name: 'Submit' })).toBeDisabled();
        });

        it('creates the strategy from the algorithm and the checked criteria, then reloads the strategies and closes', async () => {
            answerAllowableLast(backend);
            const { user } = await openPage();
            const dialog = await openAddStrategy(user);
            await user.click(within(dialog).getByRole('radio', { name: 'SciPy Clustering' }));
            await within(dialog).findByText('Criteria');
            await user.click(within(dialog).getByRole('checkbox', { name: 'Repository' }));
            await user.click(within(dialog).getByRole('checkbox', { name: 'Structure' }));

            await user.click(within(dialog).getByRole('button', { name: 'Submit' }));

            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            expect(backend.sent()).toEqual([...ON_OPENING, ALLOWABLE, SUPPORTED,
                'POST /codebase/ldod/createStrategy',
                'GET /codebase/ldod/getCodebaseStrategies',
            ]);
            expect(backend.bodyOf('POST /codebase/ldod/createStrategy')).toEqual({
                strategyTypes: ['Repository', 'Structure'],
                algorithmType: 'SciPy Clustering',
            });
        });

        it('sends a single criterion as one strategyTypes field', async () => {
            answerAllowableLast(backend);
            const { user } = await openPage();
            const dialog = await openAddStrategy(user);
            await user.click(within(dialog).getByRole('radio', { name: 'SciPy Clustering' }));
            await within(dialog).findByText('Criteria');
            await user.click(within(dialog).getByRole('checkbox', { name: 'Entity Vectorization' }));

            await user.click(within(dialog).getByRole('button', { name: 'Submit' }));

            await waitFor(() => expect(backend.sent()).toContain('POST /codebase/ldod/createStrategy'));
            expect(backend.bodyOf('POST /codebase/ldod/createStrategy')).toEqual({
                strategyTypes: 'Entity Vectorization',
                algorithmType: 'SciPy Clustering',
            });
        });

        it('on a failed creation, stays open and says "Upload failed."', async () => {
            answerAllowableLast(backend);
            backend.on('POST /codebase/ldod/createStrategy', reply(400));
            const { user } = await openPage();
            const dialog = await openAddStrategy(user);
            await user.click(within(dialog).getByRole('radio', { name: 'SciPy Clustering' }));
            await within(dialog).findByText('Criteria');
            await user.click(within(dialog).getByRole('checkbox', { name: 'Repository' }));

            await user.click(within(dialog).getByRole('button', { name: 'Submit' }));

            expect(await within(dialog).findByText('Upload failed.')).toBeInTheDocument();
            expect(backend.sent()).toEqual([...ON_OPENING, ALLOWABLE, SUPPORTED, 'POST /codebase/ldod/createStrategy']);
        });

        it('forgets the chosen algorithm and criteria when closed and reopened', async () => {
            answerAllowableLast(backend);
            const { user } = await openPage();
            let dialog = await openAddStrategy(user);
            await user.click(within(dialog).getByRole('radio', { name: 'SciPy Clustering' }));
            await within(dialog).findByText('Criteria');
            await user.click(within(dialog).getByRole('checkbox', { name: 'Repository' }));

            await user.click(within(dialog).getByRole('button', { name: 'Close' }));
            await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
            dialog = await openAddStrategy(user);

            expect(within(dialog).getByRole('radio', { name: 'SciPy Clustering' })).not.toBeChecked();
            expect(within(dialog).queryByText('Criteria')).not.toBeInTheDocument();
            expect(within(dialog).getByRole('button', { name: 'Submit' })).toBeDisabled();
        });
    });
});
