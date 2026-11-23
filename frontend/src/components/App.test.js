import React from 'react';
import { render, screen } from '@testing-library/react';
import { fakeBackend } from '../testing/fakeBackend';
import App from './App';

describe('the app', () => {
    it('opens on the home page, with the header links', () => {
        const backend = fakeBackend();
        render(<App />);

        expect(screen.getByRole('heading', { name: 'Mono2Micro' })).toBeInTheDocument();
        expect(screen.getByText('© Software Engineering Group - INESC-ID 2022')).toBeInTheDocument();

        const links = screen.getAllByRole('link').map(l => [l.textContent, l.getAttribute('href')]);
        expect(links).toEqual([
            ['Mono2Micro', '/'],
            ['Software Systems', '/softwareSystems'],
            ['Comparison Tool', '/comparisonTool'],
        ]);
        expect(backend.sent()).toEqual([]);
    });
});
