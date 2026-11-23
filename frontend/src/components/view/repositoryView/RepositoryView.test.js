import { screen, within, waitFor } from '@testing-library/react';
import { fakeBackend, reply } from '../../../testing/fakeBackend';
import { renderAt } from '../../../testing/renderAt';
import { quietConsole } from '../../../testing/quietConsole';
import { clusterNetwork, resetNetworks, finishStabilizing, rightClick, doubleClickNode } from '../../../testing/fakeVisNetwork';
import { graphNodes, graphEdges, chooseFromSpeedDial, UNMOUNTED_TOAST_WARNING } from '../../../testing/viewTestUtils';
import { speedDialActions, menuOptions } from '../../../testing/screenReaders';

// vis-network needs a canvas; the stand-in keeps the graph's DataSets and shows
// their labels as text, see src/testing/fakeVisNetwork.js.
jest.mock('vis-network/standalone', () => {
    const actual = jest.requireActual('vis-network/standalone');
    return { ...actual, Network: require('../../../testing/fakeVisNetwork').FakeNetwork };
});

const URL = '/codebases/ldod/ldod_Repository/sim1/dec1/repositoryView';

const CLUSTERS = {
    Cluster0: {
        name: 'Cluster0', metrics: {}, couplingDependencies: {},
        elements: [{ id: 1, name: 'Author' }, { id: 2, name: 'Book' }],
    },
    Cluster1: { name: 'Cluster1', metrics: {}, couplingDependencies: {}, elements: [{ id: 3, name: 'Reader' }] },
};

const DECOMPOSITION = {
    name: 'dec1', codebaseName: 'ldod', strategyName: 'ldod_Repository', similarityName: 'sim1',
    type: 'Partitions Decomposition', metrics: { Cohesion: 0.5 }, outdated: false, expert: false,
    clusters: CLUSTERS,
    representationInformations: [{
        type: 'Repository Based',
        authors: { 1: ['ana', 'rito'], 2: ['ana'], 3: ['rito', 'joao'] },
        commitsInCommon: { 1: { 3: 2 }, 2: { 3: 1 } },
        totalCommits: { 1: 5, 2: 3, 3: 4 },
    }],
};

// The backend lists one number per commit in common; the frontend counts them.
const EDGE_WEIGHTS = [
    { e1ID: 1, e2ID: 3, dist: 0.4, commits: [0, 1] },
    { e1ID: 2, e2ID: 3, dist: 0.6, commits: [0] },
];

describe('the repository view screen', () => {
    let backend;
    quietConsole({ error: [UNMOUNTED_TOAST_WARNING] });
    beforeEach(() => {
        resetNetworks();
        backend = fakeBackend({
            'GET /decomposition/dec1': DECOMPOSITION,
            'GET /decomposition/dec1/Repository%20Based/getEdgeWeights': EDGE_WEIGHTS,
            'GET /history/dec1/canUndoRedo': { undo: false, redo: false },
            'GET /positionHistory/dec1/getGraphPositions': reply(404),
            'POST /positionHistory/dec1/saveGraphPositions': null,
            'GET /decomposition/dec1/Repository/getSearchItems': [
                { name: 'Cluster0', type: 'Cluster', id: 'Cluster0', entities: '2' },
                { name: 'Author', type: 'Entity', id: '1', cluster: 'Cluster0' },
            ],
        });
    });

    async function open() {
        const view = renderAt(URL);
        await waitFor(() => expect(clusterNetwork()).toBeDefined());
        const network = clusterNetwork();
        finishStabilizing(network);
        await waitFor(() => expect(backend.sent()).toContain('POST /positionHistory/dec1/saveGraphPositions'));
        return { ...view, network };
    }

    it('loads the decomposition and its repository edge weights on opening, and draws one node per cluster', async () => {
        renderAt(URL);

        await waitFor(() => expect(graphNodes()).toEqual(['Cluster0', 'Cluster1']));
        expect(backend.sent()).toEqual([
            'GET /decomposition/dec1',
            'GET /decomposition/dec1/Repository%20Based/getEdgeWeights',
            'GET /history/dec1/canUndoRedo',
            'GET /positionHistory/dec1/getGraphPositions',
            'GET /history/dec1/canUndoRedo',
        ]);
        expect(graphEdges()).toEqual(['Cluster0 -- Cluster1']);
        const nav = screen.getByRole('navigation', { name: 'breadcrumb' });
        expect(within(nav).getAllByRole('listitem').map(i => i.textContent))
            .toEqual(['Home', 'Codebases', 'ldod', 'ldod_Repository', 'sim1', 'dec1']);
    });

    it('weighs the edges by the commits their entities share, and saves positions under that property', async () => {
        const { network } = await open();

        // ODDITY: value is meant as the commits in common (2 + 1), but the backend sends each
        // pair's commits as indexes 0..n-1, which the union dedups to 2. length: mean distance x 400
        expect(network.edges()).toEqual([
            { id: 'Cluster0&Cluster1', from: 'Cluster0', to: 'Cluster1', type: 6, length: 200, value: 2 },
        ]);
        expect(backend.bodyOf('POST /positionHistory/dec1/saveGraphPositions').property).toBe('commits');
    });

    it('offers only search in the speed dial, plus undo/redo as the history allows', async () => {
        backend.on('GET /history/dec1/canUndoRedo', { undo: false, redo: true });
        await open();

        await waitFor(() => expect(speedDialActions()).toEqual(['Search Element', 'Redo']));
    });

    it('searches the Repository items, without a functionality type column', async () => {
        const { user } = await open();

        await chooseFromSpeedDial(user, 'Search Element');

        const dialog = await screen.findByRole('dialog');
        // BUG: as in the accesses view, the strategy type ("Repository") is sent where
        // the backend expects the representation type ("Repository Based").
        expect(backend.sent().slice(-1)).toEqual(['GET /decomposition/dec1/Repository/getSearchItems']);
        await within(dialog).findByText('Author');
        expect(within(dialog).getAllByRole('columnheader').map(th => th.firstChild.textContent))
            .toEqual(['Name', 'Type', 'Belongs to Cluster', 'Entities']);
    });

    it('offers the same cluster operations as the other views, and renames through the same request', async () => {
        backend.on('POST /decomposition/dec1/rename', null);
        backend.on('GET /decomposition/dec1/getClusters', CLUSTERS);
        const { user, network } = await open();

        rightClick(network, 'Cluster1');
        expect(menuOptions())
            .toEqual(['Expand Cluster', 'Rename Cluster', 'Split', 'Merge', 'Transfer Entities', 'Only Show Neighbours']);
        await user.click(screen.getByRole('button', { name: 'Rename Cluster' }));
        await user.type(screen.getByLabelText('New name'), 'Readers');
        await user.click(screen.getByRole('button', { name: 'Rename' }));

        expect(await screen.findByText('Successfully changed name!')).toBeInTheDocument();
        expect(backend.bodyOf('POST /decomposition/dec1/rename'))
            .toEqual({ type: 'RenameOperation', clusterName: 'Cluster1', newClusterName: 'Readers' });
    });

    it('lists a cluster\'s entities and authors on double-clicking it', async () => {
        const { user, network } = await open();

        doubleClickNode(network, 'Cluster0');

        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText('Cluster Cluster0 informations')).toBeInTheDocument();
        expect(within(dialog).getAllByRole('button').map(b => b.textContent))
            .toEqual(["Cluster's Entities (2 Entities)", 'Related Authors', 'Authors in Common', 'Close']);

        await user.click(within(dialog).getByRole('button', { name: 'Related Authors' }));
        expect(within(dialog).getByText('Authors that made modifications to Cluster0')).toBeInTheDocument();
        expect(within(dialog).getByText('2 Authors:')).toBeInTheDocument();
        expect(within(dialog).getAllByText(/^(ana|rito)$/).map(i => i.textContent)).toEqual(['ana', 'rito']);
    });

    it('shows an entity\'s commits, cluster and authors on double-clicking it', async () => {
        backend.on('GET /positionHistory/dec1/getGraphPositions', {
            property: 'commits',
            nodes: [
                { id: 'Cluster0', label: 'Cluster0', type: 1, group: 'Cluster0', elements: CLUSTERS.Cluster0.elements },
                { id: 3, label: 'Reader', type: 3, group: 'Cluster1' },
            ],
            edges: [],
        });
        const { user, network } = await open();

        doubleClickNode(network, 3);

        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText('Entity Reader informations')).toBeInTheDocument();
        // ODDITY: "Total commits" and "Number of modifications done" show the same number
        expect(within(dialog).getAllByRole('button').map(b => [b.textContent, b.disabled])).toEqual([
            ['Total commits: 4', true], ['Belongs to: Cluster1', true], ['Number of modifications done: 4', true],
            ['Related Authors', false], ['Close', false],
        ]);

        await user.click(within(dialog).getByRole('button', { name: 'Related Authors' }));
        expect(within(dialog).getByText('2 Authors:')).toBeInTheDocument();
        expect(within(dialog).getByText('joao')).toBeInTheDocument();
    });
});
