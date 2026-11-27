import { screen, within, waitFor } from '@testing-library/react';
import { fakeBackend, reply } from '../../../testing/fakeBackend';
import { renderAt } from '../../../testing/renderAt';
import { quietConsole } from '../../../testing/quietConsole';
import {
    clusterNetwork, resetNetworks, finishStabilizing, stabilized, rightClick, clickNode, doubleClickNode,
} from '../../../testing/fakeVisNetwork';
import { graphNodes, graphEdges, chooseFromSpeedDial, UNMOUNTED_TOAST_WARNING } from '../../../testing/viewTestUtils';
import { tableRows, speedDialActions, menuOptions } from '../../../testing/screenReaders';

// vis-network needs a canvas; the stand-in keeps the graph's DataSets and shows
// their labels as text, see src/testing/fakeVisNetwork.js.
jest.mock('vis-network/standalone', () => {
    const actual = jest.requireActual('vis-network/standalone');
    return { ...actual, Network: require('../../../testing/fakeVisNetwork').FakeNetwork };
});

const URL = '/softwareSystems/sys1/similarities/sim1/dec1/accessesViews';

// Ticket 07: the view loads its decomposition by id, for the breadcrumbs' names.
const DECOMPOSITION = {
    id: 'dec1', name: 'dec1', softwareSystemId: 'sys1', softwareSystemName: 'ldod', similarityId: 'sim1', similarityName: 'sim1',
    type: 'Partitions Cluster Representation', metrics: {}, outdated: false, expert: false, clusters: {},
    representationInformations: [{ type: 'Accesses Based' }],
};

const CLUSTERS = {
    Cluster0: {
        name: 'Cluster0', metrics: { Cohesion: 0.5, Coupling: 0.25, Complexity: 3 },
        elements: [{ id: 1, name: 'Author' }, { id: 2, name: 'Book' }], couplingDependencies: { Cluster1: [3] },
    },
    Cluster1: {
        name: 'Cluster1', metrics: { Cohesion: 1, Coupling: 0.5, Complexity: 2 },
        elements: [{ id: 3, name: 'Reader' }], couplingDependencies: { Cluster0: [1] },
    },
    Cluster2: {
        name: 'Cluster2', metrics: { Cohesion: 1, Coupling: 0, Complexity: 0 },
        elements: [{ id: 4, name: 'Shelf' }], couplingDependencies: {},
    },
};

function functionality(name, type, entities, entitiesPerCluster) {
    return {
        id: 'dec1' + name, name, type, metrics: { Complexity: 1 }, entities, entitiesPerCluster,
        functionalityRedesigns: { 'Monolith Trace': 'dec1' + name + 'Monolith Trace' },
        functionalityRedesignNameUsedForMetrics: 'Monolith Trace',
    };
}
// The functionality view gets FunctionalityDtos: the redesigns come as a list.
function functionalityDto({ id, functionalityRedesigns, ...rest }) {
    return {
        ...rest,
        functionalityRedesigns: Object.keys(functionalityRedesigns).map(name =>
            ({ name, redesign: [], pivotTransaction: -1, metrics: { 'Functionality Complexity': 1, 'System Complexity': 2 } })),
    };
}
const LEND = functionality('lend', 'SAGA', { 1: 2, 3: 1 }, { Cluster0: [1], Cluster1: [3] });
const GET_BOOK = functionality('getBook', 'QUERY', { 2: 1, 3: 1 }, { Cluster0: [2], Cluster1: [3] });

const CLUSTERS_FUNCTIONALITIES = { Cluster0: [LEND, GET_BOOK], Cluster1: [LEND, GET_BOOK] };

// Only entity pairs some functionality accesses together have an edge.
const EDGE_WEIGHTS = [
    { e1ID: 1, e2ID: 3, dist: 0.5, functionalities: ['lend'] },
    { e1ID: 2, e2ID: 3, dist: 0.3, functionalities: ['getBook'] },
];

const SEARCH_ITEMS = [
    { name: 'Cluster0', type: 'Cluster', id: 'Cluster0', entities: '2' },
    { name: 'Author', type: 'Entity', id: '1', cluster: 'Cluster0' },
    { name: 'Book', type: 'Entity', id: '2', cluster: 'Cluster0' },
    { name: 'Cluster1', type: 'Cluster', id: 'Cluster1', entities: '1' },
    { name: 'Reader', type: 'Entity', id: '3', cluster: 'Cluster1' },
    { name: 'lend', type: 'Functionality', id: 'lend', entities: '2', funcType: 'SAGA' },
];

describe('the accesses view screen', () => {
    let backend;
    quietConsole({ error: [UNMOUNTED_TOAST_WARNING] });
    beforeEach(() => {
        resetNetworks();
        window.scrollTo = jest.fn();
        backend = fakeBackend({
            'GET /decomposition/dec1': DECOMPOSITION,
            'GET /accesses/dec1/getClustersAndClustersFunctionalities':
                { clusters: CLUSTERS, clustersFunctionalities: CLUSTERS_FUNCTIONALITIES },
            'GET /representation/sys1/getIdToEntity': { 1: 'Author', 2: 'Book', 3: 'Reader', 4: 'Shelf' },
            'GET /decomposition/dec1/Accesses%20Based/getEdgeWeights': EDGE_WEIGHTS,
            'GET /history/dec1/canUndoRedo': { undo: false, redo: false },
            'GET /positionHistory/dec1/getGraphPositions': reply(404),
            'POST /positionHistory/dec1/saveGraphPositions': null,
            'GET /decomposition/dec1/Accesses%20Based/getSearchItems': SEARCH_ITEMS,
            'GET /decomposition/dec1/getClusters': CLUSTERS,
            'GET /accesses/dec1/getFunctionalitiesAndFunctionalitiesClusters': {
                functionalities: { lend: functionalityDto(LEND), getBook: functionalityDto(GET_BOOK) },
                functionalitiesClusters: { lend: [CLUSTERS.Cluster0, CLUSTERS.Cluster1], getBook: [CLUSTERS.Cluster0, CLUSTERS.Cluster1] },
            },
        });
    });

    // Opens the screen and lets the graph settle, as the browser would.
    async function open() {
        const view = renderAt(URL);
        await waitFor(() => expect(clusterNetwork()).toBeDefined());
        const network = clusterNetwork();
        finishStabilizing(network);
        await waitFor(() => expect(backend.sent()).toContain('POST /positionHistory/dec1/saveGraphPositions'));
        return { ...view, network };
    }

    it('loads the clusters, entity names and edge weights on opening, and draws one node per cluster', async () => {
        renderAt(URL);

        await waitFor(() => expect(graphNodes()).toEqual(['Cluster0', 'Cluster1', 'Cluster2']));
        expect(backend.sent()).toEqual([
            'GET /decomposition/dec1',
            'GET /accesses/dec1/getClustersAndClustersFunctionalities',
            'GET /representation/sys1/getIdToEntity',
            'GET /history/dec1/canUndoRedo',
            'GET /history/dec1/canUndoRedo',
            'GET /history/dec1/canUndoRedo',
            'GET /decomposition/dec1/Accesses%20Based/getEdgeWeights',
            'GET /positionHistory/dec1/getGraphPositions',
        ]);
        expect(graphEdges()).toEqual(['Cluster0 -- Cluster1']);
        expect(await screen.findByText('No previous positions saved.')).toBeInTheDocument();

        const nav = screen.getByRole('navigation', { name: 'breadcrumb' });
        expect(within(nav).getAllByRole('listitem').map(i => i.textContent))
            .toEqual(['Home', 'Software Systems', 'ldod', 'sim1', 'dec1']);
        expect(within(nav).getByText('sim1')).toHaveAttribute('href', '/softwareSystems/sys1/similarities/sim1/decomposition');
    });

    it('hands the graph cluster nodes, and edges weighted by the functionalities their entities share', async () => {
        renderAt(URL);
        await waitFor(() => expect(clusterNetwork()).toBeDefined());
        const network = clusterNetwork();

        expect(network.nodes().map(({ id, label, value, elements }) => ({ id, label, value, elements }))).toEqual([
            { id: 'Cluster0', label: 'Cluster0', value: 2, elements: CLUSTERS.Cluster0.elements },
            { id: 'Cluster1', label: 'Cluster1', value: 1, elements: CLUSTERS.Cluster1.elements },
            { id: 'Cluster2', label: 'Cluster2', value: 1, elements: CLUSTERS.Cluster2.elements },
        ]);
        // value: distinct functionalities in common; length: mean distance x 400
        expect(network.edges()).toEqual([
            { id: 'Cluster0&Cluster1', from: 'Cluster0', to: 'Cluster1', type: 6, length: 160, value: 2 },
        ]);
    });

    it('saves the graph positions, with the edge property, once the graph settles', async () => {
        await open();

        const saved = backend.bodyOf('POST /positionHistory/dec1/saveGraphPositions');
        expect(saved.property).toBe('functionalities');
        expect(saved.nodes.map(n => [n.id, n.x, n.y])).toEqual([['Cluster0', 0, 0], ['Cluster1', 0, 0], ['Cluster2', 0, 0]]);
        expect(saved.edges.map(e => e.id)).toEqual(['Cluster0&Cluster1']);
        expect(await screen.findByText('Successfully Saved Graph Positions!')).toBeInTheDocument();
    });

    it('draws the saved positions when they still match the clusters', async () => {
        backend.on('GET /positionHistory/dec1/getGraphPositions', {
            property: 'functionalities',
            nodes: [
                { id: 'Cluster0', label: 'Cluster0', type: 1, group: 'Cluster0', elements: CLUSTERS.Cluster0.elements, x: 5, y: 5 },
                { id: 'Cluster1', label: 'Cluster1', type: 1, group: 'Cluster1', elements: CLUSTERS.Cluster1.elements, x: 9, y: 9 },
                { id: 4, label: 'Shelf', type: 3, group: 'Cluster2', x: 1, y: 1 },
            ],
            edges: [],
        });
        renderAt(URL);

        await waitFor(() => expect(graphNodes()).toEqual(['Cluster0', 'Cluster1', 'Shelf']));
        expect(graphEdges()).toEqual([]);
    });

    it('redraws from the clusters when the saved positions no longer match them', async () => {
        backend.on('GET /positionHistory/dec1/getGraphPositions', {
            property: 'functionalities',
            nodes: [{ id: 'Old', label: 'Old', type: 1, group: 'Old', elements: [{ id: 1, name: 'Author' }] }],
            edges: [],
        });
        renderAt(URL);

        await waitFor(() => expect(graphNodes()).toEqual(['Cluster0', 'Cluster1', 'Cluster2']));
    });

    it('offers search, functionalities and metrics in the speed dial, and undo/redo only as the history allows', async () => {
        backend.on('GET /history/dec1/canUndoRedo', { undo: true, redo: false });
        await open();

        await waitFor(() => expect(speedDialActions())
            .toEqual(['Search Element', 'Go to Functionalities', 'Go to Metrics', 'Undo']));
    });

    it('shows each cluster\'s metrics, and the coupling between clusters, under Go to Metrics', async () => {
        const { user } = await open();
        const before = backend.sent().length;

        await chooseFromSpeedDial(user, 'Go to Metrics');

        const table = await screen.findByRole('table');
        expect(tableRows(table)).toEqual([
            ['Cluster', 'Entities', 'Functionalities', 'Cohesion', 'Coupling', 'Complexity'],
            ['Cluster0', '2', '2', '0.5', '0.25', '3'],
            ['Cluster1', '1', '2', '1', '0.5', '2'],
            ['Cluster2', '1', 'fetching...', '1', '0', '0'],
        ]);
        // the clusters are fetched again, so the table matches them
        expect(backend.sent().slice(before)).toContain('GET /accesses/dec1/getClustersAndClustersFunctionalities');
        expect(screen.getByRole('button', { name: 'Metrics' })).toBeDisabled();

        await user.click(screen.getByRole('button', { name: 'Inter Coupling Cluster' }));

        expect(tableRows(screen.getByRole('table'))).toEqual([
            ['', 'Cluster0', 'Cluster1', 'Cluster2'],
            ['Cluster0', '---', '1.00', '0'],
            ['Cluster1', '0.50', '---', '0'],
            ['Cluster2', '0', '0', '---'],
        ]);
        expect(speedDialActions()).toContain('Go to Top');
    });

    it('searches the Accesses items and, on picking a cluster, shows only it and its neighbours', async () => {
        const { user } = await open();

        await chooseFromSpeedDial(user, 'Search Element');

        const dialog = await screen.findByRole('dialog');
        // U1 is resolved by ticket 09: the search bar now asks by representation
        // type ("Accesses Based"), the type the backend looks the items up by.
        expect(backend.sent().slice(-1)).toEqual(['GET /decomposition/dec1/Accesses%20Based/getSearchItems']);
        await within(dialog).findByText('Author');
        // each header is its title followed by its filter
        expect(within(dialog).getAllByRole('columnheader').map(th => th.firstChild.textContent))
            .toEqual(['Name', 'Type', 'Functionality Type', 'Belongs to Cluster', 'Entities']);

        await user.type(within(dialog).getByLabelText('Search Element'), 'Reader');
        expect(tableRows(within(dialog).getByRole('table')).slice(1).map(r => r[0])).toEqual(['Reader']);

        await user.clear(within(dialog).getByLabelText('Search Element'));
        const clusterRow = within(dialog).getAllByRole('row').find(row => row.cells[1].textContent === 'Cluster' && row.cells[0].textContent === 'Cluster0');
        await user.click(clusterRow.cells[0]);

        await waitFor(() => expect(graphNodes()).toEqual(['Cluster0', 'Cluster1']));
        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('offers the cluster operations on right-clicking a cluster, and the graph operations on the background', async () => {
        const { network } = await open();

        rightClick(network, 'Cluster0');
        expect(menuOptions()).toEqual(
            ['Expand Cluster', 'Rename Cluster', 'Split', 'Merge', 'Transfer Entities', 'Only Show Neighbours']);

        rightClick(network);
        expect(menuOptions()).toEqual(
            ['Snapshot Decomposition', 'Save Graph Positions', 'Restore Graph Positions', 'Expand All', 'Toggle Physics']);
    });

    it('renames a cluster, then reloads the clusters and relabels the node', async () => {
        backend.on('POST /decomposition/dec1/operations', null);
        const { user, network } = await open();

        rightClick(network, 'Cluster0');
        await user.click(screen.getByRole('button', { name: 'Rename Cluster' }));
        const dialog = screen.getByRole('dialog');
        expect(within(dialog).getByText('Rename Cluster Cluster0')).toBeInTheDocument();
        await user.type(within(dialog).getByLabelText('New name'), 'Authors');
        await user.click(within(dialog).getByRole('button', { name: 'Rename' }));

        expect(await screen.findByText('Successfully changed name!')).toBeInTheDocument();
        expect(backend.bodyOf('POST /decomposition/dec1/operations'))
            .toEqual({ type: 'RenameOperation', clusterName: 'Cluster0', newClusterName: 'Authors' });
        await waitFor(() => expect(backend.sent()).toContain('GET /decomposition/dec1/getClusters'));
        await waitFor(() => expect(graphNodes()).toEqual(['Cluster1', 'Cluster2', 'Authors']));
        expect(graphEdges()).toEqual(['Authors -- Cluster1']);

        // once the graph settles again, the new positions are saved
        const saves = () => backend.sent().filter(r => r.startsWith('POST /positionHistory')).length;
        const savedBefore = saves();
        stabilized(network);
        await waitFor(() => expect(saves()).toBe(savedBefore + 1));
    });

    // U7: ClusterView's dialogResponse effect chains the clusters' reload on each
    // operation's request (rename, transfer, merge, split, form) without a catch,
    // so a refused operation (e.g. a rename to a taken name, 401) leaves an
    // unhandled promise rejection.
    // Jest fails any test that causes one, so the refusal toast ("This name is
    // already being used by a cluster.") cannot be pinned here.
    it.todo('says when the new cluster name is taken');

    it('merges two clusters, the second picked by clicking it, under a new name', async () => {
        backend.on('POST /decomposition/dec1/operations', null);
        const { user, network } = await open();

        rightClick(network, 'Cluster0');
        await user.click(screen.getByRole('button', { name: 'Merge' }));
        expect(await screen.findByText(/Click in the cluster to be merged to\./)).toBeInTheDocument();
        rightClick(network, 'Cluster2');
        expect(menuOptions()).toEqual(['Cancel Operation']);

        clickNode(network, 'Cluster1');
        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText('Merge Cluster Cluster0 + Cluster1')).toBeInTheDocument();
        await user.type(within(dialog).getByLabelText("Merged cluster's name"), 'Library');
        await user.click(within(dialog).getByRole('button', { name: 'Merge' }));

        expect(await screen.findByText('Successfully merged clusters!')).toBeInTheDocument();
        expect(backend.bodyOf('POST /decomposition/dec1/operations'))
            .toEqual({ type: 'MergeOperation', cluster1Name: 'Cluster0', cluster2Name: 'Cluster1', newName: 'Library' });
        await waitFor(() => expect(graphNodes()).toEqual(['Cluster2', 'Library']));
    });

    it('splits the chosen entities of a cluster into a new cluster', async () => {
        backend.on('POST /decomposition/dec1/operations', null);
        const { user, network } = await open();

        rightClick(network, 'Cluster0');
        await user.click(screen.getByRole('button', { name: 'Split' }));
        const dialog = screen.getByRole('dialog');
        expect(within(dialog).getByText('Split cluster Cluster0')).toBeInTheDocument();
        expect(tableRows(within(dialog).getByRole('table')).map(r => r[1])).toEqual(['Entity Name', 'Author', 'Book']);
        await user.type(within(dialog).getByLabelText('New cluster'), 'Writers');
        await user.click(within(dialog).getByText('Author'));
        await user.click(within(dialog).getByRole('button', { name: 'Split' }));

        expect(await screen.findByText('Successfully split cluster!')).toBeInTheDocument();
        expect(backend.bodyOf('POST /decomposition/dec1/operations'))
            .toEqual({ type: 'SplitOperation', originalCluster: 'Cluster0', newCluster: 'Writers', entities: '1' });
        await waitFor(() => expect(graphNodes()).toEqual(['Cluster0', 'Cluster1', 'Cluster2', 'Writers']));
    });

    it('turns a split of every entity into a rename', async () => {
        backend.on('POST /decomposition/dec1/operations', null);
        const { user, network } = await open();

        rightClick(network, 'Cluster0');
        await user.click(screen.getByRole('button', { name: 'Split' }));
        await user.type(screen.getByLabelText('New cluster'), 'Writers');
        await user.click(screen.getByText('Author'));
        await user.click(screen.getByText('Book'));
        await user.click(screen.getByRole('button', { name: 'Split' }));

        await waitFor(() => expect(backend.sent()).toContain('POST /decomposition/dec1/operations'));
        expect(backend.bodyOf('POST /decomposition/dec1/operations'))
            .toEqual({ type: 'RenameOperation', clusterName: 'Cluster0', newClusterName: 'Writers' });
    });

    it('transfers the chosen entities to the cluster clicked next', async () => {
        backend.on('POST /decomposition/dec1/operations', null);
        const { user, network } = await open();

        rightClick(network, 'Cluster0');
        await user.click(screen.getByRole('button', { name: 'Transfer Entities' }));
        expect(await screen.findByText(/Click in the cluster where the entities will be transferred to\./)).toBeInTheDocument();
        clickNode(network, 'Cluster2');
        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText(/Transfer Entities From Cluster Cluster0/)).toHaveTextContent('Cluster0 Cluster2');
        await user.click(within(dialog).getByText('Book'));
        await user.click(within(dialog).getByRole('button', { name: 'Transfer' }));

        expect(await screen.findByText('Successfully transferred entities!')).toBeInTheDocument();
        expect(backend.bodyOf('POST /decomposition/dec1/operations'))
            .toEqual({ type: 'TransferOperation', fromCluster: 'Cluster0', toCluster: 'Cluster2', entities: '2' });
    });

    it('turns a transfer of every entity into a merge named after the receiving cluster', async () => {
        backend.on('POST /decomposition/dec1/operations', null);
        const { user, network } = await open();

        rightClick(network, 'Cluster1');
        await user.click(screen.getByRole('button', { name: 'Transfer Entities' }));
        clickNode(network, 'Cluster2');
        await user.click(await within(await screen.findByRole('dialog')).findByText('Reader'));
        await user.click(screen.getByRole('button', { name: 'Transfer' }));

        await waitFor(() => expect(backend.sent()).toContain('POST /decomposition/dec1/operations'));
        expect(backend.bodyOf('POST /decomposition/dec1/operations'))
            .toEqual({ type: 'MergeOperation', cluster1Name: 'Cluster1', cluster2Name: 'Cluster2', newName: 'Cluster2' });
    });

    it('forms a new cluster from entities of several selected clusters', async () => {
        backend.on('POST /decomposition/dec1/operations', null);
        const { user, network } = await open();

        network.selected = ['Cluster0', 'Cluster1'];
        rightClick(network, 'Cluster0');
        expect(menuOptions()).toEqual(['Form Cluster']);
        await user.click(screen.getByRole('button', { name: 'Form Cluster' }));

        const dialog = screen.getByRole('dialog');
        expect(tableRows(within(dialog).getByRole('table')).map(r => r.slice(1))).toEqual([
            ['Entity Name', 'Cluster Name'], ['Author', 'Cluster0'], ['Book', 'Cluster0'], ['Reader', 'Cluster1'],
        ]);
        await user.type(within(dialog).getByLabelText('New cluster'), 'Readers');
        await user.click(within(dialog).getByText('Book'));
        await user.click(within(dialog).getByText('Reader'));
        await user.click(within(dialog).getByRole('button', { name: 'Create' }));

        expect(await screen.findByText('Successfully formed new cluster!')).toBeInTheDocument();
        expect(backend.bodyOf('POST /decomposition/dec1/operations'))
            .toEqual({ type: 'FormClusterOperation', newCluster: 'Readers', entities: { Cluster0: [2], Cluster1: [3] } });
        await waitFor(() => expect(graphNodes()).toEqual(['Cluster0', 'Cluster2', 'Readers']));
    });

    it('cancels a pending operation from the menu without sending anything', async () => {
        const { user, network } = await open();
        const before = backend.sent().length;

        rightClick(network, 'Cluster0');
        await user.click(screen.getByRole('button', { name: 'Merge' }));
        rightClick(network);
        await user.click(screen.getByRole('button', { name: 'Cancel Operation' }));

        rightClick(network, 'Cluster0');
        expect(menuOptions()).toContain('Merge');
        expect(backend.sent().length).toBe(before);
    });

    it('snapshots the decomposition from the background menu', async () => {
        backend.on('GET /decomposition/dec1/snapshotDecomposition', null);
        const { user, network } = await open();

        rightClick(network);
        await user.click(screen.getByRole('button', { name: 'Snapshot Decomposition' }));

        expect(await screen.findByText('Successfully snapshot decomposition!')).toBeInTheDocument();
        expect(backend.sent().slice(-1)).toEqual(['GET /decomposition/dec1/snapshotDecomposition']);
    });

    it('undoes and redoes through the history, reloading the clusters and their saved positions', async () => {
        backend.on('GET /history/dec1/canUndoRedo', { undo: true, redo: true });
        backend.on('GET /history/dec1/undoOperation', null);
        backend.on('GET /history/dec1/redoOperation', null);
        const { user } = await open();
        await waitFor(() => expect(speedDialActions()).toContain('Redo'));

        let before = backend.sent().length;
        await chooseFromSpeedDial(user, 'Undo');
        await waitFor(() => expect(backend.sent().slice(before)).toContain('GET /positionHistory/dec1/getGraphPositions'));
        expect(backend.sent().slice(before, before + 2))
            .toEqual(['GET /history/dec1/undoOperation', 'GET /decomposition/dec1/getClusters']);

        before = backend.sent().length;
        await chooseFromSpeedDial(user, 'Redo');
        await waitFor(() => expect(backend.sent().slice(before)).toContain('GET /positionHistory/dec1/getGraphPositions'));
        expect(backend.sent().slice(before, before + 2))
            .toEqual(['GET /history/dec1/redoOperation', 'GET /decomposition/dec1/getClusters']);
    });

    it('shows a cluster\'s entities and functionalities on double-clicking it', async () => {
        const { user, network } = await open();

        doubleClickNode(network, 'Cluster0');

        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText('Cluster Cluster0 informations')).toBeInTheDocument();
        const entities = await within(dialog).findByRole('button', { name: "Cluster's Entities (2 Entities)" });
        expect(within(dialog).getAllByRole('button').map(b => b.textContent))
            .toEqual(["Cluster's Entities (2 Entities)", 'Coupling Dependencies', 'Related Functionalities', 'Close']);

        await user.click(entities);
        expect(within(dialog).getByText('Entities that belong to cluster Cluster0')).toBeInTheDocument();
        expect(within(dialog).getByText('2 entities:')).toBeInTheDocument();
        expect(within(dialog).getByText('Author')).toBeInTheDocument();

        await user.click(within(dialog).getByRole('button', { name: 'Go Back' }));
        await user.click(within(dialog).getByRole('button', { name: 'Related Functionalities' }));
        expect(within(dialog).getByText('Functionalities that access cluster Cluster0')).toBeInTheDocument();
        expect(within(dialog).getByText('2 Functionalities:')).toBeInTheDocument();
        expect(within(dialog).getByText('lend')).toBeInTheDocument();
        expect(within(dialog).getByText('getBook')).toBeInTheDocument();
    });

    it('switches to the functionality view, loading the functionalities, from the speed dial', async () => {
        const { user } = await open();

        await chooseFromSpeedDial(user, 'Go to Functionalities');

        await waitFor(() => expect(backend.sent().slice(-1))
            .toEqual(['GET /accesses/dec1/getFunctionalitiesAndFunctionalitiesClusters']));
        expect(screen.getByRole('button', { name: 'Select Functionality' })).toBeVisible();
        await waitFor(() => expect(speedDialActions()).toEqual(['Search Element', 'Go to Clusters']));
    });
});
