import { screen, within } from '@testing-library/react';

// Reading helpers shared by the screen tests: each turns a part of the screen
// into plain values a test can compare.

// The card holding `text`; `options` are getByText's, e.g. a selector when the
// text also appears outside the cards.
export function cardOf(text, options) {
    return screen.getByText(text, options).closest('.card');
}

// A table as rows of cell texts, the header row first. `container` is the table
// or anything holding just one.
export function tableRows(container) {
    return within(container).getAllByRole('row').map(row => [...row.cells].map(cell => cell.textContent));
}

// The actions the views' speed dial offers, by their accessible names.
export function speedDialActions() {
    return screen.queryAllByRole('menuitem').map(item => item.getAttribute('aria-label'));
}

// The views' right-click menu: a react-bootstrap ListGroup, with no role of its
// own, whose options are its action items.
export function menuOptions() {
    return [...document.querySelectorAll('.list-group-item-action')].map(item => item.textContent);
}
