import { screen, within, waitFor } from '@testing-library/react';
import { fakeBackend, reply } from '../../testing/fakeBackend';
import { renderAt } from '../../testing/renderAt';
import { tableRows } from '../../testing/screenReaders';

const CODEBASES = [
    { name: 'ldod', representations: [] },
    { name: 'quizzes', representations: [] },
];

function decomposition(name, expert, clusters, metrics) {
    return {
        name, codebaseName: 'ldod', strategyName: 'ldod_Accesses', similarityName: 'sim1',
        type: 'Partitions Decomposition', metrics, outdated: false, expert,
        clusters: Object.fromEntries(Object.entries(clusters).map(([clusterName, elements]) =>
            [clusterName, { name: clusterName, metrics: {}, couplingDependencies: {}, elements }])),
        representationInformations: [{ type: 'Accesses Based' }],
    };
}

const AUTHOR = { id: 1, name: 'Author' };
const BOOK = { id: 2, name: 'Book' };
const READER = { id: 3, name: 'Reader' };
const SHELF = { id: 4, name: 'Shelf' };

const EXPERT = decomposition('expert', true,
    { A: [AUTHOR, BOOK], B: [READER, SHELF] }, { Cohesion: 0.8, Coupling: 0.2 });
const DEC1 = decomposition('dec1', false,
    { A: [AUTHOR], C: [BOOK, READER, SHELF] }, { Cohesion: 0.5, Coupling: 0.3, 'Silhouette Score': 0.1 });
const DEC2 = decomposition('dec2', false, { A: [AUTHOR, BOOK, READER, SHELF] }, {});

// Shapes from comparisonTool/dto/DefaultComparisonToolResponse and domain/{MoJoFM,Purity}.java
const COMPARISON = {
    decomposition1: EXPERT,
    decomposition2: DEC1,
    analysisList: [
        {
            type: 'MOJO',
            truePositive: 1, trueNegative: 2, falsePositive: 2, falseNegative: 1,
            falsePairs: [['1', 'A', 'A', '2', 'A', 'C'], ['2', 'A', 'C', '3', 'B', 'C']],
            accuracy: 0.5, precision: 0.33, recall: 0.5, specificity: 0.5, fmeasure: 0.4,
            mojoCommon: 75, mojoBiggest: 50, mojoNew: 50, mojoSingletons: 25,
        },
        { type: 'PURITY', purity: 0.75, clusterPurityMap: { C: 0.6667, A: 1 }, clusterMapping: { C: 'B', A: 'A' } },
    ],
};

describe('the comparison tool screen', () => {
    let backend;
    beforeEach(() => {
        backend = fakeBackend({
            'GET /codebases': CODEBASES,
            'GET /codebase/ldod/getCodebaseDecompositions': [DEC1, EXPERT, DEC2],
            // the backend answers 200, and the screen accepts nothing else
            'POST /comparison/expert/dec1': reply(200, COMPARISON),
            'GET /representation/ldod/getIdToEntity': { 1: 'Author', 2: 'Book', 3: 'Reader', 4: 'Shelf' },
        });
    });

    // The form's rows: a label and a dropdown button.
    const field = label => within(screen.getByText(label).parentElement);

    async function choose(user, label, option) {
        await user.click(field(label).getAllByRole('button')[0]);
        await user.click(field(label).getByRole('button', { name: option }));
    }

    async function compare(user) {
        await choose(user, 'Codebase', 'ldod');
        await waitFor(() => expect(backend.sent()).toContain('GET /codebase/ldod/getCodebaseDecompositions'));
        await choose(user, 'Source of Truth', 'expert');
        await choose(user, 'Compare to Cut', 'dec1');
        await user.click(screen.getByRole('button', { name: 'Submit' }));
        await screen.findByText('Upload completed successfully.');
    }

    it('loads the codebases on opening, and has three choices and a disabled Submit', async () => {
        const { user } = renderAt('/comparisonTool');

        await waitFor(() => expect(backend.sent()).toEqual(['GET /codebases']));
        expect(screen.getByRole('heading', { name: 'Comparison Tool' })).toBeInTheDocument();
        expect(screen.getByRole('heading', { name: 'Metrics' })).toBeInTheDocument();
        expect(field('Codebase').getByRole('button')).toHaveTextContent('Select Codebase');
        expect(field('Source of Truth').getByRole('button')).toHaveTextContent('Select Cut');
        expect(field('Compare to Cut').getByRole('button')).toHaveTextContent('Select Cut');
        expect(screen.getByRole('button', { name: 'Submit' })).toBeDisabled();

        await user.click(field('Codebase').getByRole('button'));
        expect(field('Codebase').getAllByRole('button').slice(1).map(b => b.textContent)).toEqual(['ldod', 'quizzes']);
    });

    it('loads the decompositions of the chosen codebase, the expert ones first', async () => {
        const { user } = renderAt('/comparisonTool');

        await choose(user, 'Codebase', 'ldod');

        await waitFor(() => expect(backend.sent())
            .toEqual(['GET /codebases', 'GET /codebase/ldod/getCodebaseDecompositions']));
        expect(field('Codebase').getAllByRole('button')[0]).toHaveTextContent('ldod');
        for (const label of ['Source of Truth', 'Compare to Cut']) {
            await user.click(field(label).getByRole('button', { name: 'Select Cut' }));
            expect(field(label).getAllByRole(/button|separator/).slice(1).map(b => b.textContent || '---'))
                .toEqual(['expert', '---', 'dec1', 'dec2']);
            await user.click(field(label).getAllByRole('button')[0]);
        }
    });

    it('enables Submit once a codebase and both cuts are chosen', async () => {
        const { user } = renderAt('/comparisonTool');
        const submit = screen.getByRole('button', { name: 'Submit' });

        await choose(user, 'Codebase', 'ldod');
        await waitFor(() => expect(backend.sent()).toContain('GET /codebase/ldod/getCodebaseDecompositions'));
        await choose(user, 'Source of Truth', 'expert');
        expect(submit).toBeDisabled();
        await choose(user, 'Compare to Cut', 'dec1');

        expect(submit).toBeEnabled();
        expect(field('Source of Truth').getAllByRole('button')[0]).toHaveTextContent('expert');
        expect(field('Compare to Cut').getAllByRole('button')[0]).toHaveTextContent('dec1');
    });

    it('compares the two cuts by name, then loads the entity names', async () => {
        const { user } = renderAt('/comparisonTool');

        await compare(user);

        await waitFor(() => expect(backend.sent()).toEqual([
            'GET /codebases',
            'GET /codebase/ldod/getCodebaseDecompositions',
            'POST /comparison/expert/dec1',
            'GET /representation/ldod/getIdToEntity',
        ]));
        expect(backend.bodyOf('POST /comparison/expert/dec1')).toBeUndefined();
        // ODDITY: the comparison reports itself in upload wording
        expect(screen.getByText('Upload completed successfully.')).toBeInTheDocument();
        expect(sectionTitles()).toEqual(
            ['MojoFM', 'Entity Pairs', 'False Pairs', 'Purity Results', 'Statistics', 'Decomposition Differences']);
    });

    it('says when the comparison fails, and shows no results', async () => {
        backend.on('POST /comparison/expert/dec1', reply(400));
        const { user } = renderAt('/comparisonTool');

        await choose(user, 'Codebase', 'ldod');
        await waitFor(() => expect(backend.sent()).toContain('GET /codebase/ldod/getCodebaseDecompositions'));
        await choose(user, 'Source of Truth', 'expert');
        await choose(user, 'Compare to Cut', 'dec1');
        await user.click(screen.getByRole('button', { name: 'Submit' }));

        expect(await screen.findByText('Upload failed.')).toBeInTheDocument();
        expect(sectionTitles()).toEqual([]);
    });

    it('shows the MoJoFM scores and the entity pair counts', async () => {
        const { user } = renderAt('/comparisonTool');
        await compare(user);

        expect(cells(await openSection(user, 'MojoFM'))).toEqual([
            'Accuracy', 'Precision', 'Recall', 'Specificity', 'F-score',
            '0.5', '0.33', '0.5', '0.5', '0.4',
            'Mojo Common Entities', 'Mojo Biggest Cluster', 'Mojo New Cluster', 'Mojo Singletons',
            '75', '50', '50', '25',
        ]);
        expect(cells(await openSection(user, 'Entity Pairs'))).toEqual([
            'True Positives', 'True Negatives', 'False Positives', 'False Negatives', '1', '2', '2', '1',
        ]);
    });

    it('lists the false pairs with each entity\'s cluster in either cut, searchable', async () => {
        const { user } = renderAt('/comparisonTool');
        await compare(user);

        const region = await openSection(user, 'False Pairs');
        const table = within(region).getByRole('table');
        // renderAt translates an entity id as itself
        expect(tableRows(table)).toEqual([
            ['Entity 1', 'expert', 'dec1', '', 'Entity 2', 'expert', 'dec1'],
            ['1', 'A', 'A', '', '2', 'A', 'C'],
            ['2', 'A', 'C', '', '3', 'B', 'C'],
        ]);

        await user.type(within(region).getByRole('textbox'), 'B');
        await waitFor(() => expect(tableRows(table).slice(1)).toEqual([['2', 'A', 'C', '', '3', 'B', 'C']]));
    });

    it('shows the purity of each cluster of the compared cut against its expert cluster', async () => {
        const { user } = renderAt('/comparisonTool');
        await compare(user);

        const region = await openSection(user, 'Purity Results');
        expect(within(region).getByText('Overall Cluster Purity: 0.75')).toBeInTheDocument();
        expect(cells(region).slice(1)).toEqual([
            'Cluster', 'Purity', 'Mapped to Expert Cluster', 'Entities in Proposed Cluster', 'Entities in Expert Cluster', 'Entities in Common',
            'C', '0.67', 'B', 'Book, Reader, Shelf', 'Reader, Shelf', 'Reader, Shelf',
            'A', '1.00', 'A', 'Author', 'Author, Book', 'Author',
        ]);
    });

    it('compares the cuts\' cluster sizes and metrics side by side', async () => {
        const { user } = renderAt('/comparisonTool');
        await compare(user);

        expect(cells(await openSection(user, 'Statistics'))).toEqual([
            'Property', 'expert', 'dec1',
            'Largest Cluster Size:', '2', '3',
            // BUG: the first cut's smallest size is seeded with the second cut's first
            // cluster (DefaultComparisonTool.js:25), so expert shows 1 where its smallest is 2.
            'Smallest Cluster Size:', '1', '1',
            'Number of Singleton Clusters:', '0', '1',
            'Cohesion:', '0.8', '0.5',
            'Coupling:', '0.2', '0.3',
            'Silhouette Score:', 'Not Present', '0.1',
        ]);
    });

    it('lists, per cluster, the entities both cuts keep together and those that moved', async () => {
        const { user } = renderAt('/comparisonTool');
        await compare(user);

        const region = await openSection(user, 'Decomposition Differences');
        expect(within(region).getAllByRole('button').map(b => [b.textContent, b.getAttribute('aria-disabled') === 'true']))
            .toEqual([['A', false], ['B does not exist in dec1', true], ['C does not exist in expert', true]]);

        const cluster = await openSection(user, 'A', region);
        expect(within(cluster).getAllByRole('list').map(list => within(list).getAllByRole('listitem').map(i => i.textContent)))
            .toEqual([
                ['Entities Belonging to the Same Cluster in Both Decompositions', 'Author'],
                ["Entities Missing From the Second Decomposition's Clusters", 'Book (belong to C)'],
            ]);
    });
});

// The titles of the closed accordion sections. The codebase and cut pickers are
// closed dropdown buttons too, titled by their prompt or by the choice made, so
// those texts are left out.
function sectionTitles() {
    return screen.queryAllByRole('button', { expanded: false }).map(b => b.textContent)
        .filter(t => !['Select Codebase', 'Select Cut', 'ldod', 'expert', 'dec1'].includes(t));
}

// The texts of the innermost elements of a region: the cells of its grids.
function cells(region) {
    return [...region.querySelectorAll('*')]
        .filter(el => el.children.length === 0 && el.textContent.trim() !== '')
        .map(el => el.textContent.trim());
}

// Opens an accordion section by its title and returns its (unnamed) region.
async function openSection(user, title, container = screen) {
    const summary = (container === screen ? screen : within(container)).getByRole('button', { name: title });
    await user.click(summary);
    return within(summary.parentElement).getAllByRole('region')[0];
}
