import { screen, within, waitFor, fireEvent } from '@testing-library/react';
import { fakeBackend, reply } from '../../../testing/fakeBackend';
import { renderAt } from '../../../testing/renderAt';
import { tableRows } from '../../../testing/screenReaders';
import { quietConsole } from '../../../testing/quietConsole';

const URL = '/codebases/ldod/ldod_Accesses/sim1/dec1/functionalityRefactor';

// The refactorization tool is a second service; the fake keeps its full URL.
const REFACTOR_TOOL = 'http://localhost:5001/api/v1';
const VIEW_REFACTOR = `GET ${REFACTOR_TOOL}/refactor/decomposition/dec1`;
const REFACTOR = `POST ${REFACTOR_TOOL}/refactor`;

// Shapes from tools/functionality_refactor/src/app/refactor/values/values.go
const COMPLETED = {
    name: 'lend', status: 'COMPLETED', error: '',
    monolith: { complexity_metrics: { functionality_complexity: 10, system_complexity: 20, invocations_count: 5, accesses_count: 9 } },
    refactor: {
        orchestrator: { name: 'Cluster0', entities: [1, 2] },
        complexity_metrics: { functionality_complexity: 4, system_complexity: 12, invocations_count: 3, accesses_count: 9 },
        execution_metrics: { functionality_complexity_reduction: 6, system_complexity_reduction: 8, invocation_merges: 2 },
        call_graph: [
            { cluster_name: 'Cluster0', accesses: [{ entity_id: 1, type: 'R' }, { entity_id: 2, type: 'W' }] },
            { cluster_name: 'Cluster1', accesses: [{ entity_id: 3, type: 'R' }] },
        ],
    },
};
const FAILED = { name: 'getBook', status: 'FAILED', error: 'functionality is a query' };
const REFACTORING = { name: 'returnBook', status: 'REFACTORING', error: '' };

function refactorization(functionalities) {
    return { decomposition_name: 'dec1', data_dependence_threshold: 0, status: 'COMPLETED', functionalities };
}

describe('the functionality refactorization tool screen', () => {
    let backend;
    // BUG: FunctionalityRefactorToolMenu.js:207-208 log every translated entity
    // (renderAt translates an id as itself, so the lines are bare numbers).
    quietConsole({ log: [/^\d+$/] });
    beforeEach(() => {
        backend = fakeBackend({
            [VIEW_REFACTOR]: refactorization({ lend: COMPLETED, getBook: FAILED }),
            'GET /representation/ldod/getIdToEntity': { 1: 'Author', 2: 'Book', 3: 'Reader' },
            [REFACTOR]: refactorization({ lend: COMPLETED }),
        });
    });

    it('loads the last refactorization and the entity names on opening, and lists one row per functionality', async () => {
        renderAt(URL);

        const table = await screen.findByRole('table');
        // ODDITY: the refactorization is fetched twice, the effect on translateEntity also runs on mount
        expect(backend.sent()).toEqual([VIEW_REFACTOR, 'GET /representation/ldod/getIdToEntity', VIEW_REFACTOR]);
        expect(within(table).getByText('Results of last estimation for the valid Saga functionalities:')).toBeInTheDocument();
        expect(tableRows(table)).toEqual([
            ['Functionality', 'Status', 'Orch.', 'FRC', 'SAC', 'Invocations', 'Accesses', 'FRC Reduction', 'SAC Reduction', 'Merges'],
            ['lend', 'COMPLETED', 'Cluster0', '4', '12', '3', '9', '6', '8', '2'],
            ['getBook', 'FAILED', '', '', '', '', '', '', '', ''],
        ]);
        expect(screen.getByRole('button', { name: 'Update table' })).toBeInTheDocument();

        const nav = screen.getByRole('navigation', { name: 'breadcrumb' });
        expect(within(nav).getAllByRole('listitem').map(i => i.textContent))
            .toEqual(['Home', 'Codebases', 'ldod', 'ldod_Accesses', 'sim1', 'dec1']);
    });

    describe('with the duplicate-key warning held back', () => {
        // BUG: the redesign table is keyed by 'functionality', a field its rows lack
        // (FunctionalityRefactorToolMenu.js:296), so React warns of duplicate keys.
        quietConsole({ error: [/unique "key" prop/] });

        it('expands a completed functionality into its saga redesign and its monolith metrics', async () => {
            const { user } = renderAt(URL);
            const table = await screen.findByRole('table');

            await user.click(within(table).getByText('lend'));

            const redesign = await screen.findByText('Saga redesign proposed:');
            // renderAt translates an entity id as itself
            expect(tableRows(redesign.closest('table'))).toEqual([
                ['Cluster', 'Entity Accesses'],
                ['Cluster0', 'R -> 1    |    W -> 2    |    '],
                ['Cluster1', 'R -> 3    |    '],
            ]);
            expect(screen.getByText('Initial monolith decomposition metrics:')).toBeInTheDocument();
            expect(screen.getByText('Functionality Redesign Complexity:')).toHaveTextContent('Functionality Redesign Complexity: 10');
            expect(screen.getByText('System Added Complexity:')).toHaveTextContent('System Added Complexity: 20');
            expect(screen.getByText('Cluster Invocations:')).toHaveTextContent('Cluster Invocations: 5');
            expect(screen.getByText('Entity Accesses:')).toHaveTextContent('Entity Accesses: 9');
            expect(console.error.mock.calls.filter(([message]) => String(message).includes('unique "key" prop')))
                .not.toHaveLength(0);
        });
    });

    it('shows the error of a failed functionality when expanded', async () => {
        const { user } = renderAt(URL);
        const table = await screen.findByRole('table');

        await user.click(within(table).getByText('getBook'));

        expect((await screen.findByText('Error:')).parentElement).toHaveTextContent('Error: functionality is a query');
        expect(screen.queryByText('Saga redesign proposed:')).not.toBeInTheDocument();
    });

    it('says when no refactorization was made yet, and shows no table', async () => {
        backend.on(VIEW_REFACTOR, reply(404));
        renderAt(URL);

        expect(await screen.findByText('No refactorization information was created previously.')).toBeInTheDocument();
        expect(screen.queryByRole('table')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Update table' })).not.toBeInTheDocument();
    });

    it('says when the refactorization could not be loaded', async () => {
        backend.on(VIEW_REFACTOR, reply(500));
        renderAt(URL);

        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText('Error Message')).toBeInTheDocument();
        expect(within(dialog).getByText('ERROR: Failed to view refactorization of the codebase.')).toBeInTheDocument();
    });

    it('has one field, the data dependence distance from 0 to 5, and Start', async () => {
        renderAt(URL);
        await screen.findByRole('table');

        expect(screen.getByText('Maximum Data Dependence Distance')).toBeInTheDocument();
        const slider = screen.getByRole('slider');
        expect(slider).toHaveValue('0');
        expect(slider).toHaveAttribute('min', '0');
        expect(slider).toHaveAttribute('max', '5');
        expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Start' })).toBeEnabled();
    });

    it('disables Start while some functionality is still being refactored', async () => {
        backend.on(VIEW_REFACTOR, refactorization({ lend: COMPLETED, returnBook: REFACTORING }));
        renderAt(URL);
        await screen.findByRole('table');

        await waitFor(() => expect(screen.getByRole('button', { name: 'Start' })).toBeDisabled());
    });

    it('requests a new refactorization with the chosen distance, and shows its result', async () => {
        backend.on(VIEW_REFACTOR, reply(404));
        const { user } = renderAt(URL);
        await screen.findByText('No refactorization information was created previously.');

        // jsdom does not move a range input by keyboard, so the slider is set directly
        fireEvent.change(screen.getByRole('slider'), { target: { value: 2 } });
        await user.click(screen.getByRole('button', { name: 'Start' }));

        const table = await screen.findByRole('table');
        expect(backend.sent().slice(-1)).toEqual([REFACTOR]);
        // BUG: functionality_names is never filled, so it is always every functionality ([]);
        // the timeout slider is commented out, so the timeout is Number(null) = 0;
        // override_previous is not a field the refactorization tool reads.
        expect(backend.bodyOf(REFACTOR)).toEqual({
            decomposition_name: 'dec1',
            functionality_names: [],
            data_dependence_threshold: 2,
            minimize_sum_of_complexities: false,
            refactor_time_out_secs: 0,
            override_previous: true,
        });
        expect(tableRows(table).slice(1).map(r => r.slice(0, 2))).toEqual([['lend', 'COMPLETED']]);
    });

    it('says when the refactorization request fails', async () => {
        backend.on(REFACTOR, reply(500));
        const { user } = renderAt(URL);
        await screen.findByRole('table');

        await user.click(screen.getByRole('button', { name: 'Start' }));

        const dialog = await screen.findByRole('dialog');
        expect(within(dialog).getByText('ERROR: Failed to request refactorization of the codebase.')).toBeInTheDocument();
    });

    it('reloads the last refactorization on Update table', async () => {
        const { user } = renderAt(URL);
        await screen.findByRole('table');
        const before = backend.sent().length;

        await user.click(screen.getByRole('button', { name: 'Update table' }));

        await waitFor(() => expect(backend.sent().slice(before)).toEqual([VIEW_REFACTOR]));
    });
});
