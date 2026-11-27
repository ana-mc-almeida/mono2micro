import { screen, within, waitFor } from '@testing-library/react';
import { fakeBackend, reply } from '../../../testing/fakeBackend';
import { renderAt } from '../../../testing/renderAt';
import { quietConsole } from '../../../testing/quietConsole';
import { clusterNetwork, resetNetworks, finishStabilizing, rightClick, doubleClickNode } from '../../../testing/fakeVisNetwork';
import { graphNodes, graphEdges, chooseFromSpeedDial, UNMOUNTED_TOAST_WARNING } from '../../../testing/viewTestUtils';
import { menuOptions } from '../../../testing/screenReaders';

// vis-network needs a canvas; the stand-in keeps the graph's DataSets and shows
// their labels as text, see src/testing/fakeVisNetwork.js.
jest.mock('vis-network/standalone', () => {
    const actual = jest.requireActual('vis-network/standalone');
    return { ...actual, Network: require('../../../testing/fakeVisNetwork').FakeNetwork };
});

const URL = '/softwareSystems/sys1/similarities/sim1/dec1/structureView';

const CLUSTERS = {
    Cluster0: {
        name: 'Cluster0', metrics: {}, couplingDependencies: {},
        elements: [{ id: 1, name: 'Author' }, { id: 2, name: 'Book' }],
    },
    Cluster1: { name: 'Cluster1', metrics: {}, couplingDependencies: {}, elements: [{ id: 3, name: 'Reader' }] },
    Cluster2: { name: 'Cluster2', metrics: {}, couplingDependencies: {}, elements: [{ id: 4, name: 'Shelf' }] },
};

const DECOMPOSITION = {
    id: 'dec1', name: 'dec1', softwareSystemId: 'sys1', softwareSystemName: 'ldod', similarityId: 'sim1', similarityName: 'sim1',
    type: 'Partitions Cluster Representation', metrics: {}, outdated: false, expert: false,
    clusters: CLUSTERS,
    representationInformations: [{
        type: 'Structure Based',
        entitiesContained: { Reader: ['Author', 'Book'], Author: [] },
        entitySuperClass: { Reader: 'Person' },
    }],
};

// "appearences" lists the entity contained in the other; "superclass" the one extended.
const EDGE_WEIGHTS = [
    { e1ID: 1, e2ID: 3, dist: 0.5, appearences: [1], superclass: [] },
    { e1ID: 2, e2ID: 3, dist: 0.2, appearences: [], superclass: [2] },
];

describe('the structure view screen', () => {
    let backend;
    quietConsole({ error: [UNMOUNTED_TOAST_WARNING] });
    beforeEach(() => {
        resetNetworks();
        backend = fakeBackend({
            'GET /decomposition/dec1': DECOMPOSITION,
            'GET /decomposition/dec1/Structure%20Based/getEdgeWeights': EDGE_WEIGHTS,
            'GET /history/dec1/canUndoRedo': { undo: false, redo: false },
            'GET /positionHistory/dec1/getGraphPositions': reply(404),
            'POST /positionHistory/dec1/saveGraphPositions': null,
            'GET /decomposition/dec1/Structure%20Based/getSearchItems': [
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

    it('loads the decomposition and its structure edge weights on opening, and draws one node per cluster', async () => {
        renderAt(URL);

        await waitFor(() => expect(graphNodes()).toEqual(['Cluster0', 'Cluster1', 'Cluster2']));
        expect(backend.sent()).toEqual([
            'GET /decomposition/dec1',
            'GET /decomposition/dec1/Structure%20Based/getEdgeWeights',
            'GET /history/dec1/canUndoRedo',
            'GET /positionHistory/dec1/getGraphPositions',
            'GET /history/dec1/canUndoRedo',
        ]);
        expect(graphEdges()).toEqual(['Cluster0 -- Cluster1']);
        const nav = screen.getByRole('navigation', { name: 'breadcrumb' });
        expect(within(nav).getAllByRole('listitem').map(i => i.textContent))
            .toEqual(['Home', 'Software Systems', 'ldod', 'sim1', 'dec1']);
    });

    it('weighs the edges by references and extensions, and saves positions under the appearences property', async () => {
        const { network } = await open();

        // a superclass link counts once more, at the same distance: (0.5 + 0.2 + 0.2) / 3 x 400
        expect(network.edges()).toEqual([
            { id: 'Cluster0&Cluster1', from: 'Cluster0', to: 'Cluster1', type: 6, length: 120, value: 2 },
        ]);
        expect(backend.bodyOf('POST /positionHistory/dec1/saveGraphPositions').property).toBe('appearences');
    });

    it('searches the Structure items', async () => {
        const { user } = await open();

        await chooseFromSpeedDial(user, 'Search Element');

        const dialog = await screen.findByRole('dialog');
        // U1 is resolved by ticket 09, as in the accesses view: the representation
        // type ("Structure Based") is sent.
        expect(backend.sent().slice(-1)).toEqual(['GET /decomposition/dec1/Structure%20Based/getSearchItems']);
        await within(dialog).findByText('Author');
        expect(within(dialog).getAllByRole('columnheader').map(th => th.firstChild.textContent))
            .toEqual(['Name', 'Type', 'Belongs to Cluster', 'Entities']);
    });

    it('offers the same graph operations on the background, and snapshots through the same request', async () => {
        backend.on('GET /decomposition/dec1/snapshotDecomposition', null);
        const { user, network } = await open();

        rightClick(network);
        expect(menuOptions())
            .toEqual(['Snapshot Decomposition', 'Save Graph Positions', 'Restore Graph Positions', 'Expand All', 'Toggle Physics']);
        await user.click(screen.getByRole('button', { name: 'Snapshot Decomposition' }));

        expect(await screen.findByText('Successfully snapshot decomposition!')).toBeInTheDocument();
        expect(backend.sent().slice(-1)).toEqual(['GET /decomposition/dec1/snapshotDecomposition']);
    });

    it('offers only the entity list on double-clicking a cluster', async () => {
        const { network } = await open();

        doubleClickNode(network, 'Cluster0');

        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText('Cluster Cluster0 informations')).toBeInTheDocument();
        expect(within(dialog).getAllByRole('button').map(b => b.textContent))
            .toEqual(["Cluster's Entities (2 Entities)", 'Close']);
    });

    it('lists the entities an entity references, and its superclass, on double-clicking it', async () => {
        backend.on('GET /positionHistory/dec1/getGraphPositions', {
            property: 'appearences',
            nodes: [
                { id: 'Cluster0', label: 'Cluster0', type: 1, group: 'Cluster0', elements: CLUSTERS.Cluster0.elements },
                { id: 3, label: 'Reader', type: 3, group: 'Cluster1' },
                { id: 'Cluster2', label: 'Cluster2', type: 1, group: 'Cluster2', elements: CLUSTERS.Cluster2.elements },
            ],
            edges: [],
        });
        const { user, network } = await open();

        doubleClickNode(network, 3);

        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText('Entity Reader informations')).toBeInTheDocument();
        expect(within(dialog).getAllByRole('button').map(b => [b.textContent, b.disabled]))
            .toEqual([['Belongs to: Cluster1', true], ['Entities Referenced By', false], ['Close', false]]);

        await user.click(within(dialog).getByRole('button', { name: 'Entities Referenced By' }));
        expect(within(dialog).getByText('Entities Referenced By Reader')).toBeInTheDocument();
        expect(within(dialog).getByText('2 Entities:')).toBeInTheDocument();
        expect(within(dialog).getByText('Book')).toBeInTheDocument();
        expect(within(dialog).getByText('Superclass:')).toBeInTheDocument();
        expect(within(dialog).getByText('Person')).toBeInTheDocument();
    });
});
