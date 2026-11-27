import { screen, within, waitFor } from '@testing-library/react';
import { fakeBackend, reply } from '../../testing/fakeBackend';
import { renderAt } from '../../testing/renderAt';
import { cardOf } from '../../testing/screenReaders';

// Ticket 07: everything is addressed by id. Ticket 09: a Similarity belongs to
// the software system, with no strategy between them. Ticket 10: decompositions
// are cluster representations, made by an algorithm operation the Similarity
// offers (D-013), through its own endpoint.
const SOFTWARE_SYSTEM = { id: 'sys1', name: 'ldod', representations: [] };

const SIMILARITY = {
    id: 'sim1',
    softwareSystemId: 'sys1',
    type: 'SIMILARITY_SCIPY_ACCESSES_REPOSITORY',
    name: 'sim1',
    linkageType: 'average',
    tracesMaxLimit: 0,
    traceType: 'ALL',
    weightsList: [
        { type: 'ACCESSES_WEIGHTS', accessMetricWeight: 25, writeMetricWeight: 25, readMetricWeight: 25, sequenceMetricWeight: 25 },
    ],
};

const OPERATIONS = { features: [{ name: 'SciPy Clustering', available: true, failedRequirements: [] }] };

function decomposition(name, overrides = {}) {
    return {
        id: name,
        name,
        softwareSystemId: 'sys1',
        similarityId: 'sim1',
        type: 'Partitions Cluster Representation',
        metrics: { Complexity: 12.5, Cohesion: 0.75, Coupling: 0.25 },
        clusters: {
            C0: { name: 'C0', elements: [{ id: 1, name: 'A' }, { id: 2, name: 'B' }, { id: 3, name: 'C' }], metrics: {} },
            C1: { name: 'C1', elements: [{ id: 4, name: 'D' }], metrics: {} },
            C2: { name: 'C2', elements: [{ id: 5, name: 'E' }], metrics: {} },
        },
        representationInformations: [{ type: 'Accesses Based' }],
        outdated: false,
        expert: false,
        ...overrides,
    };
}

const LOAD_DECOMPOSITIONS = 'GET /similarities/sim1/clusterRepresentations';
const OPENING = [
    'GET /softwareSystems/sys1',
    'GET /similarity/sim1/getSimilarity',
    'GET /similarities/sim1/operations/available',
    LOAD_DECOMPOSITIONS,
];
const RUN = 'POST /similarities/sim1/operations';
const URL_PATH = '/softwareSystems/sys1/similarities/sim1/decomposition';
const PAGES = '/softwareSystems/sys1/similarities/sim1/d1';

function buttonsOf(card) {
    return within(card).getAllByRole(/button|link/).map(b => b.textContent.trim());
}

describe('the decomposition screen', () => {
    let backend;
    beforeEach(() => {
        backend = fakeBackend({
            'GET /softwareSystems/sys1': SOFTWARE_SYSTEM,
            'GET /similarity/sim1/getSimilarity': SIMILARITY,
            'GET /similarities/sim1/operations/available': OPERATIONS,
            [LOAD_DECOMPOSITIONS]: [decomposition('d1')],
            [RUN]: reply(201),
            'POST /similarity/sim1/createExpertDecomposition': reply(200),
            'DELETE /decomposition/d1/delete': reply(200),
            'GET /decomposition/d1/update': decomposition('d1'),
            'GET /decomposition/d1/export': 'zip-bytes',
        });
    });

    async function open() {
        const view = renderAt(URL_PATH);
        await screen.findByText('d1');
        await screen.findByRole('button', { name: 'Generate Decomposition' });
        return view;
    }

    // Ticket 09: no strategy crumb, and the Similarity's crumb is the current
    // page, not a link to a Similarity page of the strategy.
    it('loads the software system, the similarity and its operations, then its decompositions, on opening', async () => {
        await open();

        expect(backend.sent()).toEqual(OPENING);
        expect(backend.unhandled).toEqual([]);
        const crumbs = screen.getByRole('navigation', { name: 'breadcrumb' });
        expect(within(crumbs).getAllByRole('listitem').map(i => i.textContent.trim()))
            .toEqual(['Home', 'Software Systems', 'ldod', 'sim1']);
        expect(within(crumbs).getAllByRole('link').map(a => a.getAttribute('href')))
            .toEqual(['/', '/softwareSystems', '/softwareSystems/sys1']);
        expect(screen.getByRole('heading', { name: 'Decomposition Creation Method' })).toBeInTheDocument();
        expect(screen.getByRole('heading', { name: 'Decompositions' })).toBeInTheDocument();
    });

    it('offers the two creation methods, and shows neither form until one is chosen', async () => {
        await open();

        expect(screen.getAllByRole('button', { name: /Decomposition$/ }).map(b => b.textContent))
            .toEqual(['Generate Decomposition', 'Add Expert Decomposition']);
        expect(screen.queryByLabelText('Height')).toBeNull();
        expect(screen.queryByLabelText('Expert Name')).toBeNull();
    });

    // Ticket 10: the creation methods follow the operations the Similarity
    // offers, not its type (upstream offered them for every type).
    it('offers no creation method when the Similarity does not offer SciPy Clustering', async () => {
        backend.on('GET /similarities/sim1/operations/available',
            { features: [{ name: 'SciPy Clustering', available: false, failedRequirements: ['some requirement'] }] });
        renderAt(URL_PATH);

        await screen.findByText('d1');
        await waitFor(() => expect(backend.sent()).toEqual(OPENING));
        expect(screen.queryByRole('button', { name: /Decomposition$/ })).toBeNull();
    });

    // Starts from one decomposition so the heading is there to lose: an empty
    // list on opening would look the same whether or not it was read.
    it('shows no Decompositions heading once the similarity has none', async () => {
        const { user } = await open();
        expect(screen.getByRole('heading', { name: 'Decompositions' })).toBeInTheDocument();

        backend.on(LOAD_DECOMPOSITIONS, []);
        await user.click(within(cardOf('d1')).getByRole('button', { name: 'Delete' }));

        await waitFor(() => expect(screen.queryByRole('heading', { name: 'Decompositions' })).toBeNull());
        expect(screen.queryByText('d1')).toBeNull();
    });

    describe('generating a decomposition by cutting the dendrogram', () => {
        it('shows the dendrogram, then a Height field OR a Number of Clusters field', async () => {
            const { user } = await open();

            await user.click(screen.getByRole('button', { name: 'Generate Decomposition' }));

            // U11: the image URL doubles the slash after /mono2micro, and
            // carries a timestamp to defeat the cache. Ticket 09 renamed its
            // alt text from "Strategy" to "Dendrogram".
            expect(screen.getByRole('img', { name: 'Dendrogram' }).getAttribute('src'))
                .toMatch(/^http:\/\/localhost:8080\/mono2micro\/\/similarity\/sim1\/image\?\d+$/);
            const form = screen.getByRole('button', { name: 'Generate' }).closest('form');
            expect(within(form).getAllByRole('spinbutton').map(f => [f.labels[0].textContent.trim(), f.placeholder, f.value]))
                .toEqual([['Height', 'Cut Height', ''], ['Number of Clusters', 'Number of Clusters in Cut', '']]);
            expect(within(form).getByText('OR')).toBeInTheDocument();
        });

        it('enables Generate only when exactly one of the two fields is filled', async () => {
            const { user } = await open();
            await user.click(screen.getByRole('button', { name: 'Generate Decomposition' }));
            const generate = screen.getByRole('button', { name: 'Generate' });

            expect(generate).toBeDisabled();
            await user.type(screen.getByLabelText('Height'), '1.5');
            expect(generate).toBeEnabled();
            await user.type(screen.getByLabelText('Number of Clusters'), '4');
            expect(generate).toBeDisabled();
            await user.clear(screen.getByLabelText('Height'));
            expect(generate).toBeEnabled();
        });

        // Ticket 10: the cut runs as the SciPy Clustering operation on the
        // Similarity's own endpoint; the body names no similarity.
        it('cuts by height, then reloads the decompositions and closes the form', async () => {
            const { user } = await open();
            await user.click(screen.getByRole('button', { name: 'Generate Decomposition' }));

            await user.type(screen.getByLabelText('Height'), '1.5');
            await user.click(screen.getByRole('button', { name: 'Generate' }));

            await waitFor(() => expect(backend.sent()).toEqual([...OPENING, RUN, LOAD_DECOMPOSITIONS]));
            expect(backend.bodyOf(RUN)).toEqual({ type: 'SciPy Clustering', cutType: 'H', cutValue: 1.5 });
            await waitFor(() => expect(screen.queryByLabelText('Height')).toBeNull());
        });

        it('cuts by number of clusters', async () => {
            const { user } = await open();
            await user.click(screen.getByRole('button', { name: 'Generate Decomposition' }));

            await user.type(screen.getByLabelText('Number of Clusters'), '4');
            await user.click(screen.getByRole('button', { name: 'Generate' }));

            await waitFor(() => expect(backend.sent()).toEqual([...OPENING, RUN, LOAD_DECOMPOSITIONS]));
            expect(backend.bodyOf(RUN)).toEqual({ type: 'SciPy Clustering', cutType: 'N', cutValue: 4 });
        });

        it.each([
            ['the name is taken', 401, 'Failed to create decomposition.'],
            ['the cut fails', 400, 'Failed to create decomposition.'],
            // Ticket 10: an operation the Similarity does not offer is refused with 422.
            ['the Similarity does not offer the operation', 422, 'This Similarity does not offer the SciPy algorithm.'],
        ])('says the creation failed when %s, and does not reload', async (_, status, message) => {
            backend.on(RUN, reply(status));
            const { user } = await open();
            await user.click(screen.getByRole('button', { name: 'Generate Decomposition' }));

            await user.type(screen.getByLabelText('Height'), '1.5');
            await user.click(screen.getByRole('button', { name: 'Generate' }));

            expect(await screen.findByText(message)).toBeInTheDocument();
            expect(backend.sent()).toEqual([...OPENING, RUN]);
            expect(screen.getByLabelText('Height')).toHaveValue(1.5);
        });

        // U23, moved by ticket 10: only 201 Created counts as success now
        // (upstream: only 200), as the operation endpoint answers.
        it('treats any success other than 201 as a failure', async () => {
            backend.on(RUN, reply(200));
            const { user } = await open();
            await user.click(screen.getByRole('button', { name: 'Generate Decomposition' }));

            await user.type(screen.getByLabelText('Height'), '1.5');
            await user.click(screen.getByRole('button', { name: 'Generate' }));

            expect(await screen.findByText('Failed to create decomposition.')).toBeInTheDocument();
        });
    });

    describe('adding an expert decomposition', () => {
        const expertFile = () => new File(['{"clusters": {}}'], 'expert.json', { type: 'application/json' });

        it('has a name field and a file field', async () => {
            const { user } = await open();

            await user.click(screen.getByRole('button', { name: 'Add Expert Decomposition' }));

            const name = screen.getByLabelText('Expert Name');
            expect(name).toHaveAttribute('type', 'text');
            expect(name).toHaveAttribute('placeholder', 'Expert Name');
            expect(name).toHaveAttribute('maxLength', '30');
            expect(name).toHaveValue('');
            expect(screen.getByLabelText('Expert File (Optional)')).toHaveAttribute('type', 'file');
            expect(screen.queryByLabelText('Height')).toBeNull();
        });

        it('enables Create Expert only once both a name and a file are given', async () => {
            const { user } = await open();
            await user.click(screen.getByRole('button', { name: 'Add Expert Decomposition' }));
            const create = screen.getByRole('button', { name: 'Create Expert' });

            expect(create).toBeDisabled();
            await user.type(screen.getByLabelText('Expert Name'), 'exp1');
            // U24: the file is labelled Optional (and the backend takes it as
            // optional), but the button requires it.
            expect(create).toBeDisabled();
            await user.upload(screen.getByLabelText('Expert File (Optional)'), expertFile());
            expect(create).toBeEnabled();
        });

        it('uploads the name and file as a form, then reloads the decompositions and closes the form', async () => {
            const { user } = await open();
            await user.click(screen.getByRole('button', { name: 'Add Expert Decomposition' }));

            await user.type(screen.getByLabelText('Expert Name'), 'exp1');
            await user.upload(screen.getByLabelText('Expert File (Optional)'), expertFile());
            await user.click(screen.getByRole('button', { name: 'Create Expert' }));

            await waitFor(() => expect(backend.sent()).toEqual(
                [...OPENING, 'POST /similarity/sim1/createExpertDecomposition', LOAD_DECOMPOSITIONS]));
            expect(backend.bodyOf('POST /similarity/sim1/createExpertDecomposition'))
                .toEqual({ expertName: 'exp1', expertFile: 'File(expert.json)' });
            await waitFor(() => expect(screen.queryByLabelText('Expert Name')).toBeNull());
        });

        it.each([
            [401, 'Upload failed. Expert name already exists.'],
            [400, 'Upload failed.'],
        ])('on a %i, says %j', async (status, message) => {
            backend.on('POST /similarity/sim1/createExpertDecomposition', reply(status));
            const { user } = await open();
            await user.click(screen.getByRole('button', { name: 'Add Expert Decomposition' }));

            await user.type(screen.getByLabelText('Expert Name'), 'd1');
            await user.upload(screen.getByLabelText('Expert File (Optional)'), expertFile());
            await user.click(screen.getByRole('button', { name: 'Create Expert' }));

            expect(await screen.findByText(message)).toBeInTheDocument();
            expect(backend.sent()).toEqual([...OPENING, 'POST /similarity/sim1/createExpertDecomposition']);
        });

        it('clears the status message when switching methods', async () => {
            backend.on('POST /similarity/sim1/createExpertDecomposition', reply(400));
            const { user } = await open();
            await user.click(screen.getByRole('button', { name: 'Add Expert Decomposition' }));
            await user.type(screen.getByLabelText('Expert Name'), 'exp1');
            await user.upload(screen.getByLabelText('Expert File (Optional)'), expertFile());
            await user.click(screen.getByRole('button', { name: 'Create Expert' }));
            await screen.findByText('Upload failed.');

            await user.click(screen.getByRole('button', { name: 'Generate Decomposition' }));

            expect(screen.queryByText('Upload failed.')).toBeNull();
            expect(screen.getByLabelText('Height')).toBeInTheDocument();
        });
    });

    describe('the decomposition cards', () => {
        // Ticket 10 renamed the type to "Partitions Cluster Representation".
        it('summarise the clusters and list the metrics', async () => {
            await open();

            expect(cardOf('d1')).toHaveTextContent(
                'd1Type: Partitions Cluster Representation Number of Clusters: 3 Number of Entities: 5 ' +
                'Singleton Clusters: 2 Maximum Cluster Size: 3 Complexity: 12.5 Cohesion: 0.75 Coupling: 0.25');
        });

        it('link to the accesses views and the refactorization tool for an accesses-based decomposition', async () => {
            await open();

            const card = cardOf('d1');
            expect(buttonsOf(card)).toEqual(['View Accesses', 'Refactorization Tool', 'Delete']);
            expect(within(card).getByText('View Accesses').closest('a'))
                .toHaveAttribute('href', `${PAGES}/accessesViews`);
            expect(within(card).getByText('Refactorization Tool').closest('a'))
                .toHaveAttribute('href', `${PAGES}/functionalityRefactor`);
        });

        it('link to the repository view for a repository-based decomposition', async () => {
            backend.on(LOAD_DECOMPOSITIONS, [
                decomposition('d1', { representationInformations: [{ type: 'Accesses Based' }, { type: 'Repository Based' }] }),
            ]);
            await open();

            const card = cardOf('d1');
            expect(buttonsOf(card)).toEqual(['View Accesses', 'View Repository', 'Refactorization Tool', 'Delete']);
            expect(within(card).getByText('View Repository').closest('a'))
                .toHaveAttribute('href', `${PAGES}/repositoryView`);
        });

        it('link to the structure view and offer a CML export for a structure-based decomposition', async () => {
            backend.on(LOAD_DECOMPOSITIONS, [
                decomposition('d1', { representationInformations: [{ type: 'Structure Based' }] }),
            ]);
            await open();

            const card = cardOf('d1');
            expect(buttonsOf(card)).toEqual(['View Structure', 'Export to CML', 'Delete']);
            expect(within(card).getByText('View Structure').closest('a'))
                .toHaveAttribute('href', `${PAGES}/structureView`);
        });

        it('offer Update Metrics first when the decomposition is outdated', async () => {
            backend.on(LOAD_DECOMPOSITIONS, [decomposition('d1', { outdated: true })]);
            await open();

            expect(buttonsOf(cardOf('d1'))).toEqual(['Update Metrics', 'View Accesses', 'Refactorization Tool', 'Delete']);
        });

        it('update the metrics by id, then reload the decompositions', async () => {
            backend.on(LOAD_DECOMPOSITIONS, [decomposition('d1', { outdated: true })]);
            const { user } = await open();

            await user.click(within(cardOf('d1')).getByRole('button', { name: 'Update Metrics' }));

            await waitFor(() => expect(backend.sent()).toEqual([...OPENING, 'GET /decomposition/d1/update', LOAD_DECOMPOSITIONS]));
            expect(await screen.findByText('Success updating decomposition!')).toBeInTheDocument();
        });

        it('delete a decomposition by id, then reload the decompositions', async () => {
            const { user } = await open();

            await user.click(within(cardOf('d1')).getByRole('button', { name: 'Delete' }));

            await waitFor(() => expect(backend.sent()).toEqual([...OPENING, 'DELETE /decomposition/d1/delete', LOAD_DECOMPOSITIONS]));
            expect(await screen.findByText('Decomposition deleted.')).toBeInTheDocument();
        });

        describe('exporting to CML', () => {
            let clicked;
            beforeEach(() => {
                backend.on(LOAD_DECOMPOSITIONS, [
                    decomposition('d1', { representationInformations: [{ type: 'Structure Based' }] }),
                ]);
                clicked = [];
                window.URL.createObjectURL = jest.fn(() => 'blob:zip');
                window.URL.revokeObjectURL = jest.fn();
                jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(function () {
                    clicked.push([this.getAttribute('href'), this.getAttribute('download')]);
                });
            });
            afterEach(() => {
                jest.restoreAllMocks();
                delete window.URL.createObjectURL;
                delete window.URL.revokeObjectURL;
            });

            it('downloads the exported data as a zip', async () => {
                const { user } = await open();

                await user.click(within(cardOf('d1')).getByRole('button', { name: 'Export to CML' }));

                expect(await screen.findByText('Decomposition exported.')).toBeInTheDocument();
                expect(backend.sent()).toEqual([...OPENING, 'GET /decomposition/d1/export']);
                expect(clicked).toEqual([['blob:zip', 'm2m_decomposition_data.zip']]);
                expect(window.URL.createObjectURL.mock.calls[0][0].type).toBe('application/zip');
            });

            it('asks to use the refactorization tool first when there is nothing to export', async () => {
                backend.on('GET /decomposition/d1/export', reply(404));
                const { user } = await open();

                await user.click(within(cardOf('d1')).getByRole('button', { name: 'Export to CML' }));

                expect(await screen.findByText('Please use the Refactorization Tool first to create the data to export.'))
                    .toBeInTheDocument();
                expect(clicked).toEqual([]);
            });

            it('says when the export fails', async () => {
                backend.on('GET /decomposition/d1/export', reply(400));
                const { user } = await open();

                await user.click(within(cardOf('d1')).getByRole('button', { name: 'Export to CML' }));

                expect(await screen.findByText('Error exporting d1.')).toBeInTheDocument();
            });
        });
    });
});
