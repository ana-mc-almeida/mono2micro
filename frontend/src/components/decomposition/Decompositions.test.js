import { screen, within, waitFor } from '@testing-library/react';
import { fakeBackend, reply } from '../../testing/fakeBackend';
import { renderAt } from '../../testing/renderAt';
import { cardOf } from '../../testing/screenReaders';

const SIMILARITY = {
    type: 'SIMILARITY_SCIPY_ACCESSES_REPOSITORY',
    name: 'sim1',
    codebaseName: 'ldod',
    strategyName: 's1',
    profile: 'Generic',
    linkageType: 'average',
    tracesMaxLimit: 0,
    traceType: 'ALL',
    weightsList: [
        { type: 'ACCESSES_WEIGHTS', accessMetricWeight: 25, writeMetricWeight: 25, readMetricWeight: 25, sequenceMetricWeight: 25 },
    ],
};

function decomposition(name, overrides = {}) {
    return {
        name,
        codebaseName: 'ldod',
        strategyName: 's1',
        similarityName: 'sim1',
        type: 'Partitions Decomposition',
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

const OPENING = ['GET /similarity/sim1/getSimilarity', 'GET /similarity/sim1/decompositions'];
const RELOAD = 'GET /similarity/sim1/decompositions';
const URL_PATH = '/codebases/ldod/s1/sim1/decomposition';

function buttonsOf(card) {
    return within(card).getAllByRole(/button|link/).map(b => b.textContent.trim());
}

describe('the decomposition screen', () => {
    let backend;
    beforeEach(() => {
        backend = fakeBackend({
            'GET /similarity/sim1/getSimilarity': SIMILARITY,
            'GET /similarity/sim1/decompositions': [decomposition('d1')],
            'POST /similarity/createDecomposition': reply(200),
            'POST /similarity/sim1/createExpertDecomposition': reply(200),
            'DELETE /decomposition/d1/delete': reply(200),
            'GET /decomposition/d1/update': decomposition('d1'),
            'GET /decomposition/d1/export': 'zip-bytes',
        });
    });

    async function open() {
        const view = renderAt(URL_PATH);
        await screen.findByText('d1');
        return view;
    }

    it('loads the similarity, then its decompositions, on opening', async () => {
        await open();

        expect(backend.sent()).toEqual(OPENING);
        expect(backend.unhandled).toEqual([]);
        const crumbs = screen.getByRole('navigation', { name: 'breadcrumb' });
        expect(within(crumbs).getAllByRole('listitem').map(i => i.textContent.trim()))
            .toEqual(['Home', 'Codebases', 'ldod', 's1', 'sim1']);
        expect(within(crumbs).getAllByRole('link').map(a => a.getAttribute('href')))
            .toEqual(['/', '/codebases', '/codebases/ldod', '/codebases/ldod/s1/similarity']);
        expect(screen.getByRole('heading', { name: 'Decomposition Creation Method' })).toBeInTheDocument();
        expect(screen.getByRole('heading', { name: 'Decompositions' })).toBeInTheDocument();
    });

    it('offers the two creation methods, and shows neither form until one is chosen', async () => {
        await open();

        expect(screen.getByRole('button', { name: 'Generate Decomposition' })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Add Expert Decomposition' })).toBeInTheDocument();
        expect(screen.queryByLabelText('Height')).toBeNull();
        expect(screen.queryByLabelText('Expert Name')).toBeNull();
    });

    it.each([
        'SIMILARITY_SCIPY_ACCESSES_REPOSITORY',
        'SIMILARITY_SCIPY_ENTITY_VECTORIZATION',
        'SIMILARITY_SCIPY_CLASS_VECTORIZATION',
        'SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_CALLGRAPH',
        'SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES',
        'SIMILARITY_SCIPY_STRUCTURE',
    ])('offers the SciPy creation methods for a %s similarity', async type => {
        backend.on('GET /similarity/sim1/getSimilarity', { ...SIMILARITY, type, weightsList: [] });
        await open();

        expect(screen.getAllByRole('button', { name: /Decomposition$/ }).map(b => b.textContent))
            .toEqual(['Generate Decomposition', 'Add Expert Decomposition']);
    });

    it('shows no Decompositions heading when the similarity has none', async () => {
        backend.on('GET /similarity/sim1/decompositions', []);
        renderAt(URL_PATH);

        await screen.findByRole('button', { name: 'Generate Decomposition' });
        await waitFor(() => expect(backend.sent()).toEqual(OPENING));
        expect(screen.queryByRole('heading', { name: 'Decompositions' })).toBeNull();
    });

    describe('generating a decomposition by cutting the dendrogram', () => {
        it('shows the dendrogram, then a Height field OR a Number of Clusters field', async () => {
            const { user } = await open();

            await user.click(screen.getByRole('button', { name: 'Generate Decomposition' }));

            // Oddity: the image URL doubles the slash after /mono2micro, and
            // carries a timestamp to defeat the cache.
            expect(screen.getByRole('img', { name: 'Strategy' }).getAttribute('src'))
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

        it('cuts by height, then reloads the decompositions and closes the form', async () => {
            const { user } = await open();
            await user.click(screen.getByRole('button', { name: 'Generate Decomposition' }));

            await user.type(screen.getByLabelText('Height'), '1.5');
            await user.click(screen.getByRole('button', { name: 'Generate' }));

            await waitFor(() => expect(backend.sent()).toEqual([...OPENING, 'POST /similarity/createDecomposition', RELOAD]));
            expect(backend.bodyOf('POST /similarity/createDecomposition')).toEqual({
                type: 'SciPy Clustering', similarityName: 'sim1', cutType: 'H', cutValue: 1.5,
            });
            await waitFor(() => expect(screen.queryByLabelText('Height')).toBeNull());
        });

        it('cuts by number of clusters', async () => {
            const { user } = await open();
            await user.click(screen.getByRole('button', { name: 'Generate Decomposition' }));

            await user.type(screen.getByLabelText('Number of Clusters'), '4');
            await user.click(screen.getByRole('button', { name: 'Generate' }));

            await waitFor(() => expect(backend.sent()).toContain(RELOAD + ''));
            expect(backend.bodyOf('POST /similarity/createDecomposition')).toEqual({
                type: 'SciPy Clustering', similarityName: 'sim1', cutType: 'N', cutValue: 4,
            });
        });

        it.each([
            ['the name is taken', 401],
            ['the cut fails', 400],
        ])('says the creation failed when %s, and does not reload', async (_, status) => {
            backend.on('POST /similarity/createDecomposition', reply(status));
            const { user } = await open();
            await user.click(screen.getByRole('button', { name: 'Generate Decomposition' }));

            await user.type(screen.getByLabelText('Height'), '1.5');
            await user.click(screen.getByRole('button', { name: 'Generate' }));

            expect(await screen.findByText('Failed to create decomposition.')).toBeInTheDocument();
            expect(backend.sent()).toEqual([...OPENING, 'POST /similarity/createDecomposition']);
            expect(screen.getByLabelText('Height')).toHaveValue(1.5);
        });

        it('treats any success other than 200 as a failure', async () => {
            // Oddity: SciPyDecompositionForm.js:59 accepts only status 200.
            backend.on('POST /similarity/createDecomposition', reply(201));
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
            // Oddity: the file is labelled Optional (and the backend takes it as
            // Optional), but SciPyDecompositionForm.js:199 requires it.
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
                [...OPENING, 'POST /similarity/sim1/createExpertDecomposition', RELOAD]));
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
        it('summarise the clusters and list the metrics', async () => {
            await open();

            expect(cardOf('d1')).toHaveTextContent(
                'd1Type: Partitions Decomposition Number of Clusters: 3 Number of Entities: 5 ' +
                'Singleton Clusters: 2 Maximum Cluster Size: 3 Complexity: 12.5 Cohesion: 0.75 Coupling: 0.25');
        });

        it('link to the accesses views and the refactorization tool for an accesses-based decomposition', async () => {
            await open();

            const card = cardOf('d1');
            expect(buttonsOf(card)).toEqual(['View Accesses', 'Refactorization Tool', 'Delete']);
            expect(within(card).getByText('View Accesses').closest('a'))
                .toHaveAttribute('href', '/codebases/ldod/s1/sim1/d1/accessesViews');
            expect(within(card).getByText('Refactorization Tool').closest('a'))
                .toHaveAttribute('href', '/codebases/ldod/s1/sim1/d1/functionalityRefactor');
        });

        it('link to the repository view for a repository-based decomposition', async () => {
            backend.on('GET /similarity/sim1/decompositions', [
                decomposition('d1', { representationInformations: [{ type: 'Accesses Based' }, { type: 'Repository Based' }] }),
            ]);
            await open();

            const card = cardOf('d1');
            expect(buttonsOf(card)).toEqual(['View Accesses', 'View Repository', 'Refactorization Tool', 'Delete']);
            expect(within(card).getByText('View Repository').closest('a'))
                .toHaveAttribute('href', '/codebases/ldod/s1/sim1/d1/repositoryView');
        });

        it('link to the structure view and offer a CML export for a structure-based decomposition', async () => {
            backend.on('GET /similarity/sim1/decompositions', [
                decomposition('d1', { representationInformations: [{ type: 'Structure Based' }] }),
            ]);
            await open();

            const card = cardOf('d1');
            expect(buttonsOf(card)).toEqual(['View Structure', 'Export to CML', 'Delete']);
            expect(within(card).getByText('View Structure').closest('a'))
                .toHaveAttribute('href', '/codebases/ldod/s1/sim1/d1/structureView');
        });

        it('offer Update Metrics first when the decomposition is outdated', async () => {
            backend.on('GET /similarity/sim1/decompositions', [decomposition('d1', { outdated: true })]);
            await open();

            expect(buttonsOf(cardOf('d1'))).toEqual(['Update Metrics', 'View Accesses', 'Refactorization Tool', 'Delete']);
        });

        it('update the metrics by name, then reload the decompositions', async () => {
            backend.on('GET /similarity/sim1/decompositions', [decomposition('d1', { outdated: true })]);
            const { user } = await open();

            await user.click(within(cardOf('d1')).getByRole('button', { name: 'Update Metrics' }));

            await waitFor(() => expect(backend.sent()).toEqual([...OPENING, 'GET /decomposition/d1/update', RELOAD]));
            expect(await screen.findByText('Success updating decomposition!')).toBeInTheDocument();
        });

        it('delete a decomposition by name, then reload the decompositions', async () => {
            const { user } = await open();

            await user.click(within(cardOf('d1')).getByRole('button', { name: 'Delete' }));

            await waitFor(() => expect(backend.sent()).toEqual([...OPENING, 'DELETE /decomposition/d1/delete', RELOAD]));
            expect(await screen.findByText('Decomposition deleted.')).toBeInTheDocument();
        });

        describe('exporting to CML', () => {
            let clicked;
            beforeEach(() => {
                backend.on('GET /similarity/sim1/decompositions', [
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
