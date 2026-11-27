import React from 'react';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { Main } from '../components/Main';
import { AppProvider } from '../components/AppContext';

// Renders the app's routes at `path`, as a browser opening that URL would, so a
// test names the screen by its address rather than by its component.
export function renderAt(path) {
    const user = userEvent.setup();
    const view = render(
        <AppProvider value={{ translateEntity: id => `${id}`, updateEntityTranslationFile: () => undefined }}>
            <MemoryRouter initialEntries={[path]}>
                <Main />
            </MemoryRouter>
        </AppProvider>
    );
    return { user, ...view };
}
