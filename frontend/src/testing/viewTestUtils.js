import { screen, within, waitFor } from '@testing-library/react';
import userEvent, { PointerEventsCheckLevel } from '@testing-library/user-event';

// Reading helpers shared by the view tests.

// The node labels / edges the graph currently shows (see fakeVisNetwork.js).
export function graphNodes() {
    const list = screen.queryByRole('list', { name: 'graph nodes' });
    return list ? within(list).queryAllByRole('listitem').map(i => i.textContent) : [];
}

export function graphEdges() {
    const list = screen.queryByRole('list', { name: 'graph edges' });
    return list ? within(list).queryAllByRole('listitem').map(i => i.textContent) : [];
}

// Leaving the cluster view unmounts its ToastContainer; when a toast's timer
// fires after that, React warns of an update on an unmounted component. It says
// nothing about the screen, so the view suites drop it (see quietConsole.js).
export const UNMOUNTED_TOAST_WARNING = /on an unmounted component[\s\S]*ToastContainer/;

// The speed dial's actions only take clicks once it is open; it opens on hover.
// (user-event's move from the dial to the action reads, in jsdom, as leaving the
// dial, which closes it again; so the action is clicked without that check.)
export async function chooseFromSpeedDial(user, action) {
    const dial = screen.getByRole('button', { name: 'Search Dial' });
    if (dial.getAttribute('aria-expanded') !== 'true') {
        await user.hover(dial);
        await waitFor(() => expect(dial).toHaveAttribute('aria-expanded', 'true'));
    }
    const unchecked = userEvent.setup({ pointerEventsCheck: PointerEventsCheckLevel.Never });
    await unchecked.click(screen.getByRole('menuitem', { name: action }));
}
