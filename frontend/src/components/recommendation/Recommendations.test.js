import { screen, within, waitFor } from '@testing-library/react';
import { fakeBackend, held, reply } from '../../testing/fakeBackend';
import { renderAt } from '../../testing/renderAt';
import { cardOf, tableRows } from '../../testing/screenReaders';

// Ticket 07: everything is addressed by id. Ticket 09: a recommendation is made
// on the software system for a Similarity kind it offers, with no strategy, and
// lists the software system's decompositions. Ticket 08: the profile is a record,
// chosen by id, with "No profile" as the default. Ticket 23: the kind's default
// weights come from its description (D-029), not from a per-type form.
const SOFTWARE_SYSTEM = { id: 'sys1', name: 'ldod', representations: [] };

const KIND = 'Accesses and Repository';
const OFFERED_KINDS = {
    features: [
        { name: 'Accesses', available: true, failedRequirements: [] },
        { name: KIND, available: true, failedRequirements: [] },
        { name: 'Structure', available: false, failedRequirements: ['some requirement'] },
        { name: 'Class Vectorization', available: true, failedRequirements: [] },
    ],
};

const ACCESSES_WEIGHTS = {
    type: 'ACCESSES_WEIGHTS',
    numberOfWeights: 4,
    weightsLabel: {
        accessMetricWeight: 'Access Weight', writeMetricWeight: 'Write Weight',
        readMetricWeight: 'Read Weight', sequenceMetricWeight: 'Sequence Weight',
    },
};

const REPOSITORY_WEIGHTS = {
    type: 'REPOSITORY_WEIGHTS',
    numberOfWeights: 2,
    weightsLabel: { authorMetricWeight: 'Author Weight', commitMetricWeight: 'Commit Weight' },
};

const KIND_DESCRIPTION = {
    name: KIND,
    similarityType: 'SIMILARITY_SCIPY_ACCESSES_REPOSITORY',
    settings: [],
    defaultWeights: [
        { type: 'ACCESSES_WEIGHTS', accessMetricWeight: 17, writeMetricWeight: 17, readMetricWeight: 17, sequenceMetricWeight: 17 },
        { type: 'REPOSITORY_WEIGHTS', authorMetricWeight: 16, commitMetricWeight: 16 },
    ],
};

const PROFILES = [
    { id: 'p1', name: 'Users', units: ['f1', 'f2'] },
    { id: 'p2', name: 'Admin', units: ['f3'] },
];

function decomposition(name, overrides = {}) {
    return {
        id: name,
        name,
        softwareSystemId: 'sys1',
        similarityId: 'sim1',
        type: 'Partitions Cluster Representation',
        metrics: { Complexity: 12.5, Cohesion: 0.75 },
        clusters: {
            C0: { name: 'C0', elements: [{ id: 1, name: 'A' }, { id: 2, name: 'B' }], metrics: {} },
            C1: { name: 'C1', elements: [{ id: 3, name: 'C' }], metrics: {} },
        },
        representationInformations: [{ type: 'Accesses Based' }, { type: 'Repository Based' }],
        outdated: false,
        expert: false,
        ...overrides,
    };
}

// RecommendationDto: `isCompleted` is serialised by Jackson as `completed`.
const RECOMMENDATION = {
    type: 'RECOMMEND_MATRIX_SCIPY',
    id: 'rec1', 
    name: 'sys1_recommendation', 
    completed: false 
};

// One row per candidate decomposition, as SciPyClustering writes them: the
// weights and the number of clusters as strings, the metrics as numbers.
function resultRow(name, numberOfClusters, complexity) {
    return {
        name,
        accessMetricWeight: '17', writeMetricWeight: '17', readMetricWeight: '17', sequenceMetricWeight: '17',
        authorMetricWeight: '16', commitMetricWeight: '16',
        numberOfClusters, maxClusterSize: 4,
        Complexity: complexity, Cohesion: 0.5, Coupling: 0.25, Performance: 10, TSR: 0.75,
    };
}

const RESULT = [
    resultRow('sim_a,3', '3', 11),
    resultRow('sim_a,4', '4', 22),
];

const LOAD_DECOMPOSITIONS = 'GET /softwareSystems/sys1/getSoftwareSystemDecompositions';
const OPENING = ['GET /softwareSystems/sys1', 'GET /softwareSystems/sys1/similarities/available', LOAD_DECOMPOSITIONS];

const KIND_KEY = 'GET /similarityKinds/Accesses%20and%20Repository';
const PROFILES_OFFERED = 'GET /softwareSystems/sys1/profiles/available';
const PROFILES_LIST = 'GET /softwareSystems/sys1/profiles';
const CHOOSING = [
    `${PROFILES_OFFERED}?kindName=Accesses+and+Repository`,
    KIND_KEY,
    `${PROFILES_LIST}?kindName=Accesses+and+Repository`,
];

const RECOMMENDING = ['PUT /recommendation/createRecommendation', 'GET /recommendation/rec1/getRecommendationResult'];
const CREATE = 'POST /recommendation/rec1/createDecompositions';

const ALL_COLUMNS = [
    'Access Weight', 'Write Weight', 'Read Weight', 'Sequence Weight', 'Author Weight', 'Commit Weight',
    'Controllers Weight', 'Services Weight', 'Intermediate Methods Weight', 'Entities Weight',
    'Number Of Clusters', 'Max Cluster Size', 'Complexity', 'Cohesion', 'Coupling', 'Performance',
    'Team Size Reduction Ratio',
];

const offered = available => ({ features: [{ name: 'Profile', available, failedRequirements: [] }] });

function recommendButton() {
    return screen.getByRole('button', { name: 'Recommend Decompositions' });
}

// The column titles, without the selection column and the filter controls.
function columnsOf(dialog) {
    return within(dialog).getAllByRole('columnheader').slice(1).map(h => h.firstChild.textContent);
}

// The cells of each body row, without the selection checkbox.
function rowsOf(dialog) {
    return tableRows(dialog).slice(1).map(row => row.slice(1));
}

describe('the recommendation screen', () => {
    let backend;
    beforeEach(() => {
        backend = fakeBackend({
            'GET /softwareSystems/sys1': SOFTWARE_SYSTEM,
            'GET /softwareSystems/sys1/similarities/available': OFFERED_KINDS,
            [LOAD_DECOMPOSITIONS]: [decomposition('d1')],
            [KIND_KEY]: KIND_DESCRIPTION,
            [PROFILES_OFFERED]: offered(true),
            [PROFILES_LIST]: PROFILES,
            'PUT /recommendation/createRecommendation': reply(201, RECOMMENDATION),
            'GET /recommendation/rec1/getRecommendationResult': RESULT,
            [CREATE]: reply(200),
            'DELETE /decomposition/d1/delete': reply(200),
        });
    });

    async function open() {
        const view = renderAt('/softwareSystems/sys1/recommendation');
        await screen.findByRole('radio', { name: KIND });
        await waitFor(() => expect(backend.sent()).toHaveLength(OPENING.length));
        return view;
    }

    // Opens the screen and chooses the kind, then waits for its form to load.
    async function openWithKind(kind = KIND) {
        const view = await open();
        await view.user.click(screen.getByRole('radio', { name: kind }));
        await waitFor(() => expect(recommendButton()).toBeEnabled());
        return view;
    }

    async function requestRecommendation(user) {
        await user.click(recommendButton());
        const dialog = await screen.findByRole('dialog');
        await waitFor(() => expect(within(dialog).getAllByRole('row').length).toBeGreaterThan(1));
        return dialog;
    }

    it('loads the software system, the kinds it offers and its decompositions on opening', async () => {
        await open();

        expect(backend.sent()).toEqual(OPENING);
        expect(backend.unhandled).toEqual([]);
        const crumbs = screen.getByRole('navigation', { name: 'breadcrumb' });
        expect(within(crumbs).getAllByRole('listitem').map(i => i.textContent))
            .toEqual(['Home', 'Software Systems', 'ldod', 'Recommendation']);
        expect(within(crumbs).getByText('Home').closest('a')).toHaveAttribute('href', '/');
        expect(within(crumbs).getByText('Software Systems').closest('a')).toHaveAttribute('href', '/softwareSystems');
        expect(within(crumbs).getByText('ldod').closest('a')).toHaveAttribute('href', '/softwareSystems/sys1');
        expect(within(crumbs).getByText('Recommendation').closest('a')).toBeNull();
    });

    // Ticket 09: the form waits for a kind. The decompositions show on opening, as
    // upstream's did once its strategy loaded (P2 fixed: they had waited for a kind too).
    it('offers the available kinds, none chosen, and shows the decompositions but not the form until one is', async () => {
        await open();

        expect(screen.getAllByRole('radio').map(r => [r.labels[0].textContent, r.checked])).toEqual([
            ['Accesses', false], [KIND, false], ['Class Vectorization', false],
        ]);
        expect(screen.queryByRole('button', { name: 'Recommend Decompositions' })).toBeNull();
        expect(await screen.findByRole('heading', { name: 'Decompositions' })).toBeInTheDocument();
        expect(screen.getByText('d1')).toBeInTheDocument();
    });

    it('loads the kind and whether it takes a profile once chosen', async () => {
        await openWithKind();

        expect(backend.sent()).toEqual([...OPENING, ...CHOOSING]);
        expect(backend.unhandled).toEqual([]);
    });

    // The note from ticket 23: a kind without weights must still enable the
    // button once its description loads, so "no weights" is not "not loaded".
    it('keeps Recommend Decompositions disabled until the kind loads, even for a kind without weights', async () => {
        const description = held({ name: 'Class Vectorization', similarityType: 'SIMILARITY_SCIPY_CLASS_VECTORIZATION', settings: [], defaultWeights: [] });
        backend.on('GET /similarityKinds/Class%20Vectorization', description.route);
        backend.on(PROFILES_OFFERED, offered(false));
        const { user } = await open();

        await user.click(screen.getByRole('radio', { name: 'Class Vectorization' }));
        expect(await screen.findByRole('button', { name: 'Recommend Decompositions' })).toBeDisabled();

        description.release();
        await waitFor(() => expect(recommendButton()).toBeEnabled());
    });

    // Ticket 23: what the form shows when the kind cannot be loaded.
    it('says when the kind could not be loaded, and keeps the button disabled', async () => {
        backend.on(KIND_KEY, reply(400));
        const { user } = await open();

        await user.click(screen.getByRole('radio', { name: KIND }));

        expect(await screen.findByText("Could not load this kind's settings.")).toBeInTheDocument();
        expect(recommendButton()).toBeDisabled();
    });

    it('shows the profile, traces, trace type and linkage fields, in that order, with their defaults', async () => {
        const { container } = await openWithKind();

        const form = recommendButton().closest('form');
        expect([...form.querySelectorAll('.col-form-label')].map(l => l.textContent.trim())).toEqual([
            'Profile', 'Amount of Traces per Functionality', 'Type of traces', 'Linkage Type',
        ]);
        expect(within(form).getByRole('button', { name: 'No profile (all units)' })).toBeInTheDocument();

        const traces = within(form).getByRole('spinbutton');
        expect(traces).toHaveAttribute('placeholder', '0 by default');
        expect(traces).toHaveValue(null);
        expect(within(form).getByText(
            'If no number is inserted, 0 is assumed to be the default value meaning the maximum number of traces'
        )).toBeInTheDocument();

        expect(within(form).getAllByRole('radio').map(r => [r.labels[0].textContent, r.checked])).toEqual([
            ['All', true], ['Longest', false], ['With more different accesses', false],
            ['Average', true], ['Single', false], ['Complete', false],
        ]);
        expect(container.querySelector('[role=dialog]')).toBeNull();
    });

    // Ticket 08: the Profile field shows only for a kind a profile can scope.
    it('shows no Profile field for a kind no profile scopes, and asks for no profiles', async () => {
        backend.on(PROFILES_OFFERED, offered(false));
        await openWithKind();

        const form = recommendButton().closest('form');
        expect([...form.querySelectorAll('.col-form-label')].map(l => l.textContent.trim()))
            .toEqual(['Amount of Traces per Functionality', 'Type of traces', 'Linkage Type']);
        expect(backend.sent()).toEqual([...OPENING, ...CHOOSING.slice(0, 2)]);
    });

    // U3 is resolved here by ticket 08: the shared ProfileSelect marks only
    // the chosen profile active.
    it('offers No profile and the offered profiles, with only the chosen one active', async () => {
        const { user } = await openWithKind();

        await user.click(screen.getByRole('button', { name: 'No profile (all units)' }));

        const items = await screen.findAllByRole('button', { name: /^(No profile \(all units\)|Users|Admin)$/ });
        const options = items.filter(i => i.classList.contains('dropdown-item'));
        expect(options.map(o => o.textContent)).toEqual(['No profile (all units)', 'Users', 'Admin']);
        expect(options.map(o => o.classList.contains('active'))).toEqual([true, false, false]);
    });

    it("requests a recommendation with the defaults and the kind's default weights, then loads its result", async () => {
        const { user } = await openWithKind();

        await requestRecommendation(user);

        expect(backend.sent()).toEqual([...OPENING, ...CHOOSING, ...RECOMMENDING]);
        expect(backend.bodyOf('PUT /recommendation/createRecommendation')).toEqual({
            type: 'RECOMMEND_MATRIX_SCIPY',
            softwareSystemId: 'sys1',
            kindName: KIND,
            weightsList: [
                { ...ACCESSES_WEIGHTS, accessMetricWeight: 17, writeMetricWeight: 17, readMetricWeight: 17, sequenceMetricWeight: 17 },
                { ...REPOSITORY_WEIGHTS, authorMetricWeight: 16, commitMetricWeight: 16 },
            ],
            profileId: null,
            linkageType: 'average',
            tracesMaxLimit: 0,
            traceType: 'ALL',
        });
    });

    it('sends the chosen profile by id, amount of traces, trace type and linkage', async () => {
        const { user } = await openWithKind();

        await user.click(screen.getByRole('button', { name: 'No profile (all units)' }));
        await user.click(await screen.findByRole('button', { name: 'Admin' }));
        await user.type(screen.getByRole('spinbutton'), '50');
        await user.click(screen.getByRole('radio', { name: 'With more different accesses' }));
        await user.click(screen.getByRole('radio', { name: 'Complete' }));
        expect(screen.getByRole('button', { name: 'Admin', expanded: false })).toBeInTheDocument();
        await requestRecommendation(user);

        expect(backend.bodyOf('PUT /recommendation/createRecommendation')).toEqual(expect.objectContaining({
            profileId: 'p2',
            linkageType: 'complete',
            tracesMaxLimit: 50,
            traceType: 'WITH_MORE_DIFFERENT_ACCESSES',
        }));
    });

    // Tickets 09 and 23: every kind is a RECOMMEND_MATRIX_SCIPY recommendation
    // named by its kind (upstream had one type per strategy type).
    it('requests a recommendation of a kind without weights by its name, with no weights', async () => {
        backend.on('GET /similarityKinds/Class%20Vectorization',
            { name: 'Class Vectorization', similarityType: 'SIMILARITY_SCIPY_CLASS_VECTORIZATION', settings: [], defaultWeights: [] });
        backend.on(PROFILES_OFFERED, offered(false));
        const { user } = await openWithKind('Class Vectorization');

        await requestRecommendation(user);

        expect(backend.bodyOf('PUT /recommendation/createRecommendation')).toEqual(expect.objectContaining({
            type: 'RECOMMEND_MATRIX_SCIPY', kindName: 'Class Vectorization', weightsList: [], profileId: null,
        }));
    });

    it('shows the result in a dialog, one row per candidate, with the columns the result has', async () => {
        const { user } = await openWithKind();

        const dialog = await requestRecommendation(user);

        expect(within(dialog).getByText('Recommendation List')).toBeInTheDocument();
        expect(dialog).toHaveTextContent(
            'This list can be refreshed to display more decompositions.' +
            'Click in the Refresh button to update the list.' +
            'Select the desired decompositions and click in the Create button to create de decompositions.');
        expect(columnsOf(dialog)).toEqual([
            'Access Weight', 'Write Weight', 'Read Weight', 'Sequence Weight', 'Author Weight', 'Commit Weight',
            'Number Of Clusters', 'Max Cluster Size', 'Complexity', 'Cohesion', 'Coupling', 'Performance',
            'Team Size Reduction Ratio',
        ]);
        // U17: the candidate's name is the table key but no column shows it.
        expect(rowsOf(dialog)).toEqual([
            ['17', '17', '17', '17', '16', '16', '3', '4', '11', '0.5', '0.25', '10', '0.75'],
            ['17', '17', '17', '17', '16', '16', '4', '4', '22', '0.5', '0.25', '10', '0.75'],
        ]);
        expect(within(dialog).getByText('Showing rows 1 to 2 of 2')).toBeInTheDocument();
        expect(within(dialog).getAllByRole('button').map(b => b.textContent.trim()))
            .toEqual(['5', 'Create', 'Refresh', 'Close']);
    });

    it('shows every column while the result is not written yet', async () => {
        // The backend answers 200 with no body until the first results are saved.
        backend.on('GET /recommendation/rec1/getRecommendationResult', '');
        const { user } = await openWithKind();

        await user.click(recommendButton());
        const dialog = await screen.findByRole('dialog');

        await waitFor(() => expect(columnsOf(dialog)).toEqual(ALL_COLUMNS));
        expect(within(dialog).getAllByRole('row')).toHaveLength(1); // only the header row
    });

    it('keeps Recommend Decompositions disabled while the dialog is open, and resets it on Close', async () => {
        const { user } = await openWithKind();

        const dialog = await requestRecommendation(user);
        expect(recommendButton()).toBeDisabled();
        expect(screen.getByText('Gathering recommendations...')).toBeInTheDocument();

        await user.click(within(dialog).getByRole('button', { name: 'Close' }));

        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
        expect(screen.queryByText('Gathering recommendations...')).toBeNull();
        expect(recommendButton()).toBeEnabled();
    });

    it('refreshes an unfinished recommendation by requesting it again', async () => {
        const { user } = await openWithKind();
        const dialog = await requestRecommendation(user);

        const refresh = within(dialog).getByRole('button', { name: 'Refresh' });
        expect(refresh).toBeEnabled();
        backend.on('GET /recommendation/rec1/getRecommendationResult', [...RESULT, resultRow('sim_a,5', '5', 33)]);
        await user.click(refresh);

        await waitFor(() => expect(rowsOf(dialog)).toHaveLength(3));
        // U18: Refresh re-sends createRecommendation (same body) instead of
        // only re-reading the result; the screen never polls on its own.
        expect(backend.sent()).toEqual([...OPENING, ...CHOOSING, ...RECOMMENDING, ...RECOMMENDING]);
    });

    it('disables Refresh once the recommendation is completed', async () => {
        backend.on('PUT /recommendation/createRecommendation', reply(201, { ...RECOMMENDATION, completed: true }));
        const { user } = await openWithKind();

        const dialog = await requestRecommendation(user);

        expect(within(dialog).getByRole('button', { name: 'Refresh' })).toBeDisabled();
    });

    it('says when Create is clicked with no row selected, and sends nothing', async () => {
        const { user } = await openWithKind();
        const dialog = await requestRecommendation(user);

        await user.click(within(dialog).getByRole('button', { name: 'Create' }));

        expect(await within(dialog).findByText('No decomposition was selected')).toBeInTheDocument();
        expect(backend.sent()).toEqual([...OPENING, ...CHOOSING, ...RECOMMENDING]);
    });

    it('creates the selected candidates by name, then reloads the decompositions', async () => {
        const { user } = await openWithKind();
        const dialog = await requestRecommendation(user);

        const rows = within(dialog).getAllByRole('row').slice(1);
        await user.click(within(rows[1]).getByRole('checkbox'));
        expect(within(rows[1]).getByRole('checkbox')).toBeChecked();
        expect(within(rows[0]).getByRole('checkbox')).not.toBeChecked();
        await user.click(within(dialog).getByRole('button', { name: 'Create' }));

        expect(await within(dialog).findByText('Decompositions created.')).toBeInTheDocument();
        await waitFor(() => expect(backend.sent()).toEqual([
            ...OPENING, ...CHOOSING, ...RECOMMENDING, CREATE, LOAD_DECOMPOSITIONS,
        ]));
        // The trailing empty name keeps Spring from splitting names on commas.
        expect(backend.bodyOf(CREATE)).toEqual({ decompositionNames: ['sim_a,4', ''] });
    });

    it('creates every candidate when all are selected from the header', async () => {
        const { user } = await openWithKind();
        const dialog = await requestRecommendation(user);

        await user.click(within(within(dialog).getAllByRole('row')[0]).getByRole('checkbox'));
        await user.click(within(dialog).getByRole('button', { name: 'Create' }));

        await within(dialog).findByText('Decompositions created.');
        expect(backend.bodyOf(CREATE)).toEqual({ decompositionNames: ['sim_a,3', 'sim_a,4', ''] });
    });

    it('says when the decompositions could not be created', async () => {
        backend.on(CREATE, reply(400));
        const { user } = await openWithKind();
        const dialog = await requestRecommendation(user);

        await user.click(within(within(dialog).getAllByRole('row')[1]).getByRole('checkbox'));
        await user.click(within(dialog).getByRole('button', { name: 'Create' }));

        expect(await within(dialog).findByText("Error during decomposition's creation.")).toBeInTheDocument();
        expect(backend.sent()).toEqual([...OPENING, ...CHOOSING, ...RECOMMENDING, CREATE]);
    });

    // Ticket 10 renamed the type to "Partitions Cluster Representation".
    it("shows the software system's decompositions as cards", async () => {
        await openWithKind();

        expect(screen.getByRole('heading', { name: 'Decompositions' })).toBeInTheDocument();
        const card = cardOf('d1');
        expect(card).toHaveTextContent(
            'Type: Partitions Cluster Representation Number of Clusters: 2 Number of Entities: 3 ' +
            'Singleton Clusters: 1 Maximum Cluster Size: 2 Complexity: 12.5 Cohesion: 0.75');
        expect(within(card).getAllByRole(/button|link/).map(b => b.textContent))
            .toEqual(['View Accesses', 'View Repository', 'Refactorization Tool', 'Delete']);
    });

    it('shows no Decompositions heading when the software system has none', async () => {
        backend.on(LOAD_DECOMPOSITIONS, []);
        await openWithKind();

        expect(screen.queryByRole('heading', { name: 'Decompositions' })).toBeNull();
    });

    it('deletes a decomposition by id, then reloads the decompositions', async () => {
        const { user } = await openWithKind();

        await user.click(within(cardOf('d1')).getByRole('button', { name: 'Delete' }));

        await waitFor(() => expect(backend.sent()).toEqual([
            ...OPENING, ...CHOOSING, 'DELETE /decomposition/d1/delete', LOAD_DECOMPOSITIONS,
        ]));
    });
});
