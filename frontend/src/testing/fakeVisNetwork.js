import { act } from '@testing-library/react';

// A stand-in for vis-network's `Network`, which needs a canvas jsdom lacks.
// The views draw their graphs with `new Network(container, {nodes, edges}, options)`
// over real `DataSet`s; this fake keeps those DataSets, writes the visible node
// labels and edges into the container as plain text (so a test can read what
// the graph shows), and lets a test fire the canvas events a user would cause
// (right click, click, double click, end of stabilization).
//
// Install in a test file with:
//   jest.mock('vis-network/standalone', () => {
//       const actual = jest.requireActual('vis-network/standalone');
//       return { ...actual, Network: require('../../../testing/fakeVisNetwork').FakeNetwork };
//   });

export const networks = [];

export class FakeNetwork {
    constructor(container, data, options) {
        this.container = container;
        this.data = data || {};
        this.options = options;
        this.handlers = {};
        this.selected = [];
        this.physics = { physicsEnabled: true };
        networks.push(this);
        ['nodes', 'edges'].forEach(set => {
            if (this.data[set] && this.data[set].on) this.data[set].on('*', () => this.draw());
        });
        this.draw();
    }

    on(event, handler) {
        (this.handlers[event] = this.handlers[event] || []).push(handler);
    }

    once(event, handler) {
        const wrapped = payload => {
            this.handlers[event] = this.handlers[event].filter(h => h !== wrapped);
            handler(payload);
        };
        this.on(event, wrapped);
    }

    emit(event, payload) {
        (this.handlers[event] || []).slice().forEach(handler => handler(payload));
    }

    setOptions(options) {
        if (options.physics !== undefined) {
            const enabled = typeof options.physics === 'object' ? options.physics.enabled : options.physics;
            if (enabled !== undefined) this.physics.physicsEnabled = enabled;
        }
    }

    storePositions() {}

    getSelectedNodes() { return [...this.selected]; }

    // The fake pointer position carries the node under it, see `rightClick`.
    getNodeAt(position) { return position ? position.nodeId : undefined; }

    getPosition() { return { x: 0, y: 0 }; }

    nodes() { return this.data.nodes ? this.data.nodes.get() : []; }

    edges() { return this.data.edges ? this.data.edges.get() : []; }

    draw() {
        if (!this.container || !this.data.nodes || !this.data.nodes.get) return;
        const visible = items => items.filter(item => !item.hidden);
        const nodes = visible(this.nodes()).map(n => `<li>${n.label}</li>`).join('');
        const edges = visible(this.edges()).map(e => `<li>${e.from} -- ${e.to}</li>`).join('');
        this.container.innerHTML =
            `<ul aria-label="graph nodes">${nodes}</ul><ul aria-label="graph edges">${edges}</ul>`;
    }
}

// The network of the cluster graph, the one drawn into the views' #network div
// (the functionality view's graphs are other networks).
export function clusterNetwork() {
    return networks.filter(n => n.container && n.container.id === 'network').pop();
}

export function resetNetworks() {
    networks.length = 0;
}

// The canvas events, as a user causes them.

export function rightClick(network, nodeId = undefined) {
    act(() => network.emit('oncontext', {
        event: { preventDefault() {}, pageX: 10, pageY: 10 },
        pointer: { DOM: { nodeId } },
    }));
}

export function clickNode(network, nodeId) {
    act(() => network.emit('selectNode', { nodes: [nodeId], edges: [] }));
}

export function doubleClickNode(network, nodeId) {
    act(() => network.emit('doubleClick', { nodes: [nodeId], edges: [] }));
}

export function finishStabilizing(network) {
    act(() => network.emit('stabilizationIterationsDone'));
}

export function stabilized(network) {
    act(() => network.emit('stabilized'));
}
