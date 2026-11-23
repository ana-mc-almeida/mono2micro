import { screen, within, waitFor } from '@testing-library/react';
import { fakeBackend, reply } from '../../testing/fakeBackend';
import { renderAt } from '../../testing/renderAt';
import { cardOf } from '../../testing/screenReaders';

// Ticket 07: codebases became software systems, addressed by a generated id
// instead of their name (routes, endpoints, and the screen's wording).
const SOFTWARE_SYSTEMS = [
    { id: 'sys1', name: 'ldod', representations: [] },
    { id: 'sys2', name: 'quizzes', representations: [] },
];

describe('the software system list screen', () => {
    let backend;
    beforeEach(() => {
        backend = fakeBackend({
            'GET /softwareSystems': SOFTWARE_SYSTEMS,
            'POST /softwareSystems/create': reply(201),
            'DELETE /softwareSystems/sys1/delete': null,
        });
    });

    it('loads the software systems on opening, and shows one card each', async () => {
        renderAt('/softwareSystems');

        expect(await screen.findByText('ldod')).toBeInTheDocument();
        expect(screen.getByText('quizzes')).toBeInTheDocument();
        expect(backend.sent()).toEqual(['GET /softwareSystems']);

        const card = cardOf('ldod');
        const buttons = within(card).getAllByRole(/button|link/).map(b => b.textContent);
        expect(buttons).toEqual(['Manage Software System', 'Delete']);
        expect(within(card).getByText('Manage Software System')).toHaveAttribute('href', '/softwareSystems/sys1');
    });

    it('has one field, the name, and disables Create Software System until it is filled', async () => {
        const { user } = renderAt('/softwareSystems');
        await screen.findByText('ldod');

        // P1 (ticket 07): the placeholder capitalizes only its first word ("Software system Name").
        expect(screen.getAllByRole('textbox').map(f => f.placeholder)).toEqual(['Software system Name']);
        const create = screen.getByRole('button', { name: 'Create Software System' });
        expect(create).toBeDisabled();

        await user.type(screen.getByPlaceholderText('Software system Name'), 'newOne');
        expect(create).toBeEnabled();
    });

    it('creates a software system by its name, then reloads the list', async () => {
        const { user } = renderAt('/softwareSystems');
        await screen.findByText('ldod');

        await user.type(screen.getByPlaceholderText('Software system Name'), 'newOne');
        await user.click(screen.getByRole('button', { name: 'Create Software System' }));

        expect(await screen.findByText('Creation completed successfully.')).toBeInTheDocument();
        expect(backend.sent()).toEqual(['GET /softwareSystems', 'POST /softwareSystems/create', 'GET /softwareSystems']);
        expect(backend.bodyOf('POST /softwareSystems/create')).toEqual({ softwareSystemName: 'newOne' });
    });

    it('says when the name is taken', async () => {
        backend.on('POST /softwareSystems/create', reply(401));
        const { user } = renderAt('/softwareSystems');
        await screen.findByText('ldod');

        await user.type(screen.getByPlaceholderText('Software system Name'), 'ldod');
        await user.click(screen.getByRole('button', { name: 'Create Software System' }));

        expect(await screen.findByText('Creation failed. Software system name already exists.')).toBeInTheDocument();
    });

    it('deletes a software system by its id, then reloads the list', async () => {
        const { user } = renderAt('/softwareSystems');
        await screen.findByText('ldod');

        const card = cardOf('ldod');
        await user.click(within(card).getByRole('button', { name: 'Delete' }));

        await waitFor(() => expect(backend.sent()).toEqual(
            ['GET /softwareSystems', 'DELETE /softwareSystems/sys1/delete', 'GET /softwareSystems']));
    });
});
