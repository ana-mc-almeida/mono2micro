import { screen, within, waitFor } from '@testing-library/react';
import { fakeBackend, reply } from '../../testing/fakeBackend';
import { renderAt } from '../../testing/renderAt';
import { cardOf, tableRows } from '../../testing/screenReaders';

const STRATEGY = {
    codebaseName: 'ldod',
    name: 's1',
    algorithmType: 'SciPy Clustering',
    strategyTypes: ['Accesses', 'Repository'],
    parameterTypes: [],
};

const ACCESSES_REPRESENTATION = {
    type: 'Accesses',
    name: 'ldod_Accesses',
    codebaseName: 'ldod',
    inputFile: 'ldod.json',
    profiles: { Generic: ['f1', 'f2'], Admin: ['f3'] },
};

function decomposition(name, overrides = {}) {
    return {
        name,
        codebaseName: 'ldod',
        strategyName: 's1',
        similarityName: 'sim1',
        type: 'Partitions Decomposition',
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
    strategyName: 's1',
    name: 's1_recommendation',
    completed: false,
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

const OPENING = [
    'GET /strategy/s1/getStrategy',
    'GET /strategy/s1/getStrategyDecompositions',
    'GET /codebase/ldod/representation/Accesses/getCodebaseRepresentation',
];

const RECOMMENDING = [
    'PUT /recommendation/createRecommendation',
    'GET /recommendation/s1_recommendation/getRecommendationResult',
];

const RELOAD = [
    'GET /strategy/s1/getStrategy',
    'GET /strategy/s1/getStrategyDecompositions',
];

const ALL_COLUMNS = [
    'Access Weight', 'Write Weight', 'Read Weight', 'Sequence Weight', 'Author Weight', 'Commit Weight',
    'Controllers Weight', 'Services Weight', 'Intermediate Methods Weight', 'Entities Weight',
    'Number Of Clusters', 'Max Cluster Size', 'Complexity', 'Cohesion', 'Coupling', 'Performance',
    'Team Size Reduction Ratio',
];

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
            'GET /strategy/s1/getStrategy': STRATEGY,
            'GET /strategy/s1/getStrategyDecompositions': [decomposition('d1')],
            'GET /codebase/ldod/representation/Accesses/getCodebaseRepresentation': ACCESSES_REPRESENTATION,
            'PUT /recommendation/createRecommendation': reply(201, RECOMMENDATION),
            'GET /recommendation/s1_recommendation/getRecommendationResult': RESULT,
            'POST /recommendation/s1_recommendation/createDecompositions': reply(200),
            'DELETE /decomposition/d1/delete': reply(200),
        });
    });

    async function open() {
        const view = renderAt('/codebases/ldod/s1/recommendation');
        await screen.findByText('d1');
        await waitFor(() => expect(backend.sent()).toHaveLength(OPENING.length));
        return view;
    }

    async function requestRecommendation(user) {
        await user.click(recommendButton());
        const dialog = await screen.findByRole('dialog');
        await waitFor(() => expect(within(dialog).getAllByRole('row').length).toBeGreaterThan(1));
        return dialog;
    }

    it('loads the strategy, its decompositions and the codebase profiles on opening', async () => {
        await open();

        expect(backend.sent()).toEqual(OPENING);
        expect(backend.unhandled).toEqual([]);
        const crumbs = screen.getByRole('navigation', { name: 'breadcrumb' });
        expect(within(crumbs).getAllByRole('listitem').map(i => i.textContent)).toEqual(['Home', 'Codebases', 'ldod', 's1']);
        expect(within(crumbs).getByText('Home').closest('a')).toHaveAttribute('href', '/');
        expect(within(crumbs).getByText('Codebases').closest('a')).toHaveAttribute('href', '/codebases');
        expect(within(crumbs).getByText('ldod').closest('a')).toHaveAttribute('href', '/codebases/ldod');
        expect(within(crumbs).getByText('s1').closest('a')).toBeNull();
    });

    it('shows the profile, traces, trace type and linkage fields, in that order, with their defaults', async () => {
        const { container } = await open();

        const form = recommendButton().closest('form');
        expect([...form.querySelectorAll('.col-form-label')].map(l => l.textContent.trim())).toEqual([
            'Select Codebase Profile', 'Amount of Traces per Functionality', 'Type of traces', 'Linkage Type',
        ]);
        expect(within(form).getByRole('button', { name: 'Generic' })).toBeInTheDocument();

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
        expect(recommendButton()).toBeEnabled();
        expect(container.querySelector('[role=dialog]')).toBeNull();
    });

    it('offers the codebase profiles from the accesses representation, all marked active', async () => {
        const { user } = await open();

        await user.click(screen.getByRole('button', { name: 'Generic' }));

        const items = await screen.findAllByRole('button', { name: /^(Generic|Admin)$/ });
        const options = items.filter(i => i.classList.contains('dropdown-item'));
        expect(options.map(o => o.textContent)).toEqual(['Generic', 'Admin']);
        // Bug: RecommendMatrixSciPy.js:224 compares the map variable with itself
        // (`profile === profile`), so every profile shows as the active one.
        expect(options.map(o => o.classList.contains('active'))).toEqual([true, true]);
    });

    it('requests a recommendation with the defaults and the strategy weights, then loads its result', async () => {
        const { user } = await open();

        await requestRecommendation(user);

        expect(backend.sent()).toEqual([...OPENING, ...RECOMMENDING]);
        expect(backend.bodyOf('PUT /recommendation/createRecommendation')).toEqual({
            type: 'RECOMMEND_MATRIX_SCIPY',
            strategyName: 's1',
            weightsList: [
                { ...ACCESSES_WEIGHTS, accessMetricWeight: 17, writeMetricWeight: 17, readMetricWeight: 17, sequenceMetricWeight: 17 },
                { ...REPOSITORY_WEIGHTS, authorMetricWeight: 16, commitMetricWeight: 16 },
            ],
            profile: 'Generic',
            linkageType: 'average',
            tracesMaxLimit: 0,
            traceType: 'ALL',
        });
    });

    it('sends the chosen profile, amount of traces, trace type and linkage', async () => {
        const { user } = await open();

        await user.click(screen.getByRole('button', { name: 'Generic' }));
        await user.click(await screen.findByRole('button', { name: 'Admin' }));
        await user.type(screen.getByRole('spinbutton'), '50');
        await user.click(screen.getByRole('radio', { name: 'With more different accesses' }));
        await user.click(screen.getByRole('radio', { name: 'Complete' }));
        expect(screen.getByRole('button', { name: 'Admin', expanded: false })).toBeInTheDocument();
        await requestRecommendation(user);

        expect(backend.bodyOf('PUT /recommendation/createRecommendation')).toEqual(expect.objectContaining({
            profile: 'Admin',
            linkageType: 'complete',
            tracesMaxLimit: 50,
            traceType: 'WITH_MORE_DIFFERENT_ACCESSES',
        }));
    });

    it.each([
        [['Entity Vectorization'], 'RECOMMEND_MATRIX_ENTITY_VECTORIZATION', []],
        [['Class Vectorization'], 'RECOMMEND_MATRIX_CLASS_VECTORIZATION', []],
        [['Functionality Vectorization Call Graph'], 'RECOMMEND_MATRIX_FUNCTIONALITY_VECTORIZATION_CALLGRAPH', [{
            type: 'FUNCTIONALITY_VECTORIZATION_CALLGRAPH_WEIGHTS',
            numberOfWeights: 4,
            weightsLabel: {
                controllersWeight: 'Controllers Weight', servicesWeight: 'Services Weight',
                intermediateMethodsWeight: 'Intermediate Methods Weight', entitiesWeight: 'Entities Weight',
            },
            controllersWeight: 25, servicesWeight: 25, intermediateMethodsWeight: 25, entitiesWeight: 25,
        }]],
        [['Functionality Vectorization Sequence Accesses'], 'RECOMMEND_MATRIX_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES', [{
            type: 'FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS',
            numberOfWeights: 2,
            weightsLabel: { readMetricWeight: 'Read Weight', writeMetricWeight: 'Write Weight' },
            readMetricWeight: 50, writeMetricWeight: 50,
        }]],
        [['Structure'], 'RECOMMEND_MATRIX_SCIPY_STRUCTURE', [{
            type: 'STRUCTURE_WEIGHTS',
            numberOfWeights: 3,
            weightsLabel: { oneToOneWeight: 'One to One Weight', oneToManyWeight: 'One to Many Weight', heritageWeight: 'Inheritance Weight' },
            oneToOneWeight: 33, oneToManyWeight: 33, heritageWeight: 34,
        }]],
        [['Accesses'], 'RECOMMEND_MATRIX_SCIPY', [
            { ...ACCESSES_WEIGHTS, accessMetricWeight: 25, writeMetricWeight: 25, readMetricWeight: 25, sequenceMetricWeight: 25 },
        ]],
        [['Repository'], 'RECOMMEND_MATRIX_SCIPY', [
            { ...REPOSITORY_WEIGHTS, authorMetricWeight: 50, commitMetricWeight: 50 },
        ]],
    ])('for a %j strategy, requests a %s recommendation with its default weights', async (strategyTypes, type, weightsList) => {
        backend.on('GET /strategy/s1/getStrategy', { ...STRATEGY, strategyTypes });
        const { user } = await open();

        await requestRecommendation(user);

        expect(backend.bodyOf('PUT /recommendation/createRecommendation')).toEqual(
            expect.objectContaining({ type, strategyName: 's1', weightsList }));
    });

    it('shows no recommendation form for a strategy of another algorithm', async () => {
        backend.on('GET /strategy/s1/getStrategy', { ...STRATEGY, algorithmType: 'Other' });
        renderAt('/codebases/ldod/s1/recommendation');

        await screen.findByText('d1');
        expect(screen.queryByRole('button', { name: 'Recommend Decompositions' })).toBeNull();
        expect(backend.sent()).toEqual(OPENING.slice(0, 2));
    });

    it('shows the result in a dialog, one row per candidate, with the columns the result has', async () => {
        const { user } = await open();

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
        // Oddity: the candidate's name is the table key but no column shows it.
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
        backend.on('GET /recommendation/s1_recommendation/getRecommendationResult', '');
        const { user } = await open();

        await user.click(recommendButton());
        const dialog = await screen.findByRole('dialog');

        await waitFor(() => expect(columnsOf(dialog)).toEqual(ALL_COLUMNS));
        expect(within(dialog).getAllByRole('row')).toHaveLength(1); // only the header row
    });

    it('keeps Recommend Decompositions disabled while the dialog is open, and resets it on Close', async () => {
        const { user } = await open();

        const dialog = await requestRecommendation(user);
        expect(recommendButton()).toBeDisabled();
        expect(screen.getByText('Gathering recommendations...')).toBeInTheDocument();

        await user.click(within(dialog).getByRole('button', { name: 'Close' }));

        await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
        expect(screen.queryByText('Gathering recommendations...')).toBeNull();
        expect(recommendButton()).toBeEnabled();
    });

    it('refreshes an unfinished recommendation by requesting it again', async () => {
        const { user } = await open();
        const dialog = await requestRecommendation(user);

        const refresh = within(dialog).getByRole('button', { name: 'Refresh' });
        expect(refresh).toBeEnabled();
        backend.on('GET /recommendation/s1_recommendation/getRecommendationResult',
            [...RESULT, resultRow('sim_a,5', '5', 33)]);
        await user.click(refresh);

        await waitFor(() => expect(rowsOf(dialog)).toHaveLength(3));
        // Oddity: Refresh re-sends createRecommendation (same body) instead of
        // only re-reading the result; the screen never polls on its own.
        expect(backend.sent()).toEqual([...OPENING, ...RECOMMENDING, ...RECOMMENDING]);
    });

    it('disables Refresh once the recommendation is completed', async () => {
        backend.on('PUT /recommendation/createRecommendation', reply(201, { ...RECOMMENDATION, completed: true }));
        const { user } = await open();

        const dialog = await requestRecommendation(user);

        expect(within(dialog).getByRole('button', { name: 'Refresh' })).toBeDisabled();
    });

    it('says when Create is clicked with no row selected, and sends nothing', async () => {
        const { user } = await open();
        const dialog = await requestRecommendation(user);

        await user.click(within(dialog).getByRole('button', { name: 'Create' }));

        expect(await within(dialog).findByText('No decomposition was selected')).toBeInTheDocument();
        expect(backend.sent()).toEqual([...OPENING, ...RECOMMENDING]);
    });

    it('creates the selected candidates by name, then reloads the strategy and its decompositions', async () => {
        const { user } = await open();
        const dialog = await requestRecommendation(user);

        const rows = within(dialog).getAllByRole('row').slice(1);
        await user.click(within(rows[1]).getByRole('checkbox'));
        expect(within(rows[1]).getByRole('checkbox')).toBeChecked();
        expect(within(rows[0]).getByRole('checkbox')).not.toBeChecked();
        await user.click(within(dialog).getByRole('button', { name: 'Create' }));

        expect(await within(dialog).findByText('Decompositions created.')).toBeInTheDocument();
        await waitFor(() => expect(backend.sent()).toEqual([
            ...OPENING, ...RECOMMENDING,
            'POST /recommendation/s1_recommendation/createDecompositions',
            ...RELOAD,
        ]));
        // The trailing empty name keeps Spring from splitting names on commas.
        expect(backend.bodyOf('POST /recommendation/s1_recommendation/createDecompositions'))
            .toEqual({ decompositionNames: ['sim_a,4', ''] });
    });

    it('creates every candidate when all are selected from the header', async () => {
        const { user } = await open();
        const dialog = await requestRecommendation(user);

        await user.click(within(within(dialog).getAllByRole('row')[0]).getByRole('checkbox'));
        await user.click(within(dialog).getByRole('button', { name: 'Create' }));

        await within(dialog).findByText('Decompositions created.');
        expect(backend.bodyOf('POST /recommendation/s1_recommendation/createDecompositions'))
            .toEqual({ decompositionNames: ['sim_a,3', 'sim_a,4', ''] });
    });

    it('says when the decompositions could not be created', async () => {
        backend.on('POST /recommendation/s1_recommendation/createDecompositions', reply(400));
        const { user } = await open();
        const dialog = await requestRecommendation(user);

        await user.click(within(within(dialog).getAllByRole('row')[1]).getByRole('checkbox'));
        await user.click(within(dialog).getByRole('button', { name: 'Create' }));

        expect(await within(dialog).findByText("Error during decomposition's creation.")).toBeInTheDocument();
        expect(backend.sent()).toEqual([
            ...OPENING, ...RECOMMENDING, 'POST /recommendation/s1_recommendation/createDecompositions',
        ]);
    });

    it("shows the strategy's decompositions as cards", async () => {
        await open();

        expect(screen.getByRole('heading', { name: 'Decompositions' })).toBeInTheDocument();
        const card = cardOf('d1');
        expect(card).toHaveTextContent(
            'Type: Partitions Decomposition Number of Clusters: 2 Number of Entities: 3 ' +
            'Singleton Clusters: 1 Maximum Cluster Size: 2 Complexity: 12.5 Cohesion: 0.75');
        expect(within(card).getAllByRole(/button|link/).map(b => b.textContent))
            .toEqual(['View Accesses', 'View Repository', 'Refactorization Tool', 'Delete']);
    });

    it('shows no Decompositions heading when the strategy has none', async () => {
        backend.on('GET /strategy/s1/getStrategyDecompositions', []);
        renderAt('/codebases/ldod/s1/recommendation');

        await screen.findByRole('button', { name: 'Recommend Decompositions' });
        await waitFor(() => expect(backend.sent()).toHaveLength(OPENING.length));
        expect(screen.queryByRole('heading', { name: 'Decompositions' })).toBeNull();
    });

    it("deletes a decomposition by its name, then reloads the strategy's decompositions", async () => {
        const { user } = await open();

        const card = cardOf('d1');
        await user.click(within(card).getByRole('button', { name: 'Delete' }));

        await waitFor(() => expect(backend.sent()).toEqual([
            ...OPENING, 'DELETE /decomposition/d1/delete', 'GET /strategy/s1/getStrategyDecompositions',
        ]));
    });
});
