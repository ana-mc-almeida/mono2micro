import { screen, within, waitFor } from '@testing-library/react';
import { fakeBackend, reply } from '../../testing/fakeBackend';
import { renderAt } from '../../testing/renderAt';
import { cardOf } from '../../testing/screenReaders';

const CODEBASES = [
    { name: 'ldod', representations: [] },
    { name: 'quizzes', representations: [] },
];

describe('the codebase list screen', () => {
    let backend;
    beforeEach(() => {
        backend = fakeBackend({
            'GET /codebases': CODEBASES,
            'POST /codebase/create': reply(201),
            'DELETE /codebase/ldod/delete': null,
        });
    });

    it('loads the codebases on opening, and shows one card each', async () => {
        renderAt('/codebases');

        expect(await screen.findByText('ldod')).toBeInTheDocument();
        expect(screen.getByText('quizzes')).toBeInTheDocument();
        expect(backend.sent()).toEqual(['GET /codebases']);

        const card = cardOf('ldod');
        const buttons = within(card).getAllByRole(/button|link/).map(b => b.textContent);
        expect(buttons).toEqual(['Manage Codebase', 'Delete']);
        expect(within(card).getByText('Manage Codebase')).toHaveAttribute('href', '/codebases/ldod');
    });

    it('has one field, the name, and disables Create Codebase until it is filled', async () => {
        const { user } = renderAt('/codebases');
        await screen.findByText('ldod');

        expect(screen.getAllByRole('textbox').map(f => f.placeholder)).toEqual(['Codebase Name']);
        const create = screen.getByRole('button', { name: 'Create Codebase' });
        expect(create).toBeDisabled();

        await user.type(screen.getByPlaceholderText('Codebase Name'), 'newOne');
        expect(create).toBeEnabled();
    });

    it('creates a codebase by its name, then reloads the list', async () => {
        const { user } = renderAt('/codebases');
        await screen.findByText('ldod');

        await user.type(screen.getByPlaceholderText('Codebase Name'), 'newOne');
        await user.click(screen.getByRole('button', { name: 'Create Codebase' }));

        expect(await screen.findByText('Creation completed successfully.')).toBeInTheDocument();
        expect(backend.sent()).toEqual(['GET /codebases', 'POST /codebase/create', 'GET /codebases']);
        expect(backend.bodyOf('POST /codebase/create')).toEqual({ codebaseName: 'newOne' });
    });

    it('says when the name is taken', async () => {
        backend.on('POST /codebase/create', reply(401));
        const { user } = renderAt('/codebases');
        await screen.findByText('ldod');

        await user.type(screen.getByPlaceholderText('Codebase Name'), 'ldod');
        await user.click(screen.getByRole('button', { name: 'Create Codebase' }));

        expect(await screen.findByText('Creation failed. Codebase name already exists.')).toBeInTheDocument();
    });

    it('deletes a codebase by its name, then reloads the list', async () => {
        const { user } = renderAt('/codebases');
        await screen.findByText('ldod');

        const card = cardOf('ldod');
        await user.click(within(card).getByRole('button', { name: 'Delete' }));

        await waitFor(() => expect(backend.sent()).toEqual(
            ['GET /codebases', 'DELETE /codebase/ldod/delete', 'GET /codebases']));
    });
});
