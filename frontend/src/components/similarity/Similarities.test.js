import { screen, within, waitFor, act } from '@testing-library/react';
import { fakeBackend, held, reply } from '../../testing/fakeBackend';
import { renderAt } from '../../testing/renderAt';
import { cardOf } from '../../testing/screenReaders';
import { quietConsole } from '../../testing/quietConsole';

// The similarity screen (`Similarities.js`) and the six forms it picks from by
// the strategy's types. The forms are tested through the screen, one describe
// per form, each pinning the form's rows in order (label, controls, defaults),
// when Generate is enabled, the exact body it posts, and the card that a
// similarity of its kind shows.

const AT = '/codebases/ldod/strat1/similarity';
const STRATEGY_URL = 'GET /strategy/strat1/getStrategy';
const SIMILARITIES_URL = 'GET /strategy/strat1/getStrategySimilarities';
const ACCESSES_URL = 'GET /codebase/ldod/representation/Accesses/getCodebaseRepresentation';
const STRUCTURE_URL = 'GET /codebase/ldod/representation/Structure/getCodebaseRepresentation';
const GENERATE = 'Generate Similarity Distances';

function strategy(strategyTypes, algorithmType = 'SciPy Clustering') {
    return { codebaseName: 'ldod', name: 'strat1', algorithmType, strategyTypes, parameterTypes: [] };
}

// AccessesRepresentationDto / StructureRepresentationDto: profiles are
// Map<String, Set<String>>, profile name -> functionalities.
function representation(type) {
    return {
        name: 'ldod ' + type, type, codebaseName: 'ldod',
        profiles: { Generic: ['LoginController.login', 'BookController.list'], 'Big Functionalities': ['BookController.list'] },
    };
}

function similarity(type, name, fields) {
    return { codebaseName: 'ldod', strategyName: 'strat1', name, type, ...fields };
}

// The form's rows, one string each, in screen order: the row's label, then its
// controls. `(x)`/`( )` radios, `[value|placeholder]` number fields,
// `▾title` dropdowns and `<text>` buttons.
function formRows() {
    const form = screen.getByRole('button', { name: GENERATE }).closest('form');
    return Array.from(form.children).map(group => {
        const head = group.firstElementChild;
        const label = ['LABEL', 'LEGEND'].includes(head.tagName) ? head.textContent.trim() + ': ' : '';
        return label + Array.from(group.querySelectorAll('input, button')).map(describeControl).join(' ');
    });
}

function describeControl(control) {
    if (control.type === 'radio') return (control.checked ? '(x) ' : '( ) ') + control.labels[0].textContent;
    if (control.type === 'number') return `[${control.value}|${control.placeholder}]`;
    if (control.hasAttribute('aria-expanded')) return '▾' + control.textContent;
    return `<${control.textContent.trim()}${control.disabled ? ' (disabled)' : ''}>`;
}

function field(label) {
    return screen.getByLabelText(label);
}

// The lines of a card's text, split at its <br>s.
function cardLines(card) {
    const lines = [''];
    const walk = node => node.childNodes.forEach(child => {
        if (child.nodeName === 'BR') lines.push('');
        else if (child.nodeType === Node.TEXT_NODE) lines[lines.length - 1] += child.textContent;
        else walk(child);
    });
    walk(card.querySelector('p'));
    return lines.map(line => line.trim()).filter(line => line !== '');
}

// What every similarity card has besides its text: the dendrogram image, the
// Decomposition Generation link and Delete.
function expectCardFrame(card, name) {
    // Upstream oddity: URL already ends in "/", so the image URL has "//similarity".
    expect(card.querySelector('img').getAttribute('src'))
        .toMatch(new RegExp(`^http://localhost:8080/mono2micro//similarity/${name}/image\\?\\d+$`));
    expect(within(card).getAllByRole(/button|link/).map(b => b.textContent)).toEqual(['Decomposition Generation', 'Delete']);
    expect(within(card).getByText('Decomposition Generation'))
        .toHaveAttribute('href', `/codebases/ldod/strat1/${name}/decomposition`);
}

// Types a weight the way a browser delivers it. Upstream bug: the forms'
// changeMetricWeight reads `event.target.value` inside a setWeightsList updater,
// which React may run lazily; under act() (so with userEvent or fireEvent) it
// runs after React has restored the controlled input, reads the old value, and
// the edit is lost. A browser flushes the update before that restore, so the
// event is dispatched natively here, outside act, and act only flushes effects.
async function enterWeight(input, value) {
    Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(input, value);
    input.dispatchEvent(new Event('input', { bubbles: true }));
    await act(() => Promise.resolve());
}

async function setNumber(user, input, value) {
    await user.clear(input);
    await user.type(input, value);
}

// The screen logs every load (`[LOG] - ...`).
quietConsole({ log: [/^\[LOG\] - /] });

let backend;
afterEach(() => {
    expect(backend.unhandled).toEqual([]);
});

const LOADED = [STRATEGY_URL, SIMILARITIES_URL];
const RELOADED = ['POST /similarity/create', STRATEGY_URL, SIMILARITIES_URL];

describe('the similarity screen', () => {
    const SIMILARITIES = [
        similarity('SIMILARITY_SCIPY_CLASS_VECTORIZATION', 'sim1', { linkageType: 'average' }),
        similarity('SIMILARITY_SCIPY_CLASS_VECTORIZATION', 'sim2', { linkageType: 'single' }),
    ];

    beforeEach(() => {
        backend = fakeBackend({
            [STRATEGY_URL]: strategy(['Class Vectorization']),
            [SIMILARITIES_URL]: SIMILARITIES,
            'POST /similarity/create': reply(201),
            'DELETE /similarity/sim1/delete': null,
        });
    });

    it('loads the strategy, then its similarities, and shows the breadcrumbs', async () => {
        renderAt(AT);

        expect(await screen.findByText('sim1')).toBeInTheDocument();
        expect(backend.sent()).toEqual(LOADED);

        const crumbs = screen.getByRole('navigation', { name: 'breadcrumb' });
        expect(within(crumbs).getAllByRole('listitem').map(i => i.textContent)).toEqual(['Home', 'Codebases', 'ldod', 'strat1']);
        expect(within(crumbs).getAllByRole('link').map(l => l.getAttribute('href'))).toEqual(['/', '/codebases', '/codebases/ldod']);
    });

    it('shows the similarities under a heading, one card each, in the order received', async () => {
        renderAt(AT);
        await screen.findByText('sim1');

        expect(screen.getByRole('heading', { name: 'Similarity Distances' })).toBeInTheDocument();
        const titles = screen.getAllByRole('button', { name: 'Delete' }).map(b => within(b.closest('.card')).getByText(/^sim/).textContent);
        expect(titles).toEqual(['sim1', 'sim2']);
    });

    it('shows neither heading nor cards when the strategy has no similarities', async () => {
        backend.on(SIMILARITIES_URL, []);
        renderAt(AT);
        await screen.findByRole('button', { name: GENERATE });
        await waitFor(() => expect(backend.sent()).toEqual(LOADED));

        expect(screen.queryByRole('heading', { name: 'Similarity Distances' })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument();
    });

    it('shows no form when the strategy is not a SciPy Clustering one', async () => {
        backend.on(STRATEGY_URL, strategy(['Accesses'], 'Other Algorithm'));
        renderAt(AT);

        expect(await screen.findByText('sim1')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: GENERATE })).not.toBeInTheDocument();
    });

    it('deletes a similarity by its name, then reloads only the similarities', async () => {
        const { user } = renderAt(AT);
        await screen.findByText('sim1');

        await user.click(within(cardOf('sim1')).getByRole('button', { name: 'Delete' }));

        expect(await screen.findByText('Similarity deleted.')).toBeInTheDocument();
        expect(backend.sent()).toEqual([...LOADED, 'DELETE /similarity/sim1/delete', SIMILARITIES_URL]);
    });

    it('says when a delete fails, and does not reload', async () => {
        backend.on('DELETE /similarity/sim1/delete', reply(400));
        const { user } = renderAt(AT);
        await screen.findByText('sim1');

        await user.click(within(cardOf('sim1')).getByRole('button', { name: 'Delete' }));

        expect(await screen.findByText('Error deleting sim1.')).toBeInTheDocument();
        expect(backend.sent()).toEqual([...LOADED, 'DELETE /similarity/sim1/delete']);
    });

    it('after a similarity is created, reloads the strategy and the similarities and shows the new card', async () => {
        let created = false;
        backend.on('POST /similarity/create', () => { created = true; return reply(201); });
        backend.on(SIMILARITIES_URL, () => created ? [...SIMILARITIES, similarity('SIMILARITY_SCIPY_CLASS_VECTORIZATION', 'sim3', { linkageType: 'average' })] : SIMILARITIES);
        const { user } = renderAt(AT);
        await screen.findByText('sim1');

        await user.click(screen.getByRole('button', { name: GENERATE }));

        expect(await screen.findByText('sim3')).toBeInTheDocument();
        expect(backend.sent()).toEqual([...LOADED, ...RELOADED]);
    });

    it('says "Uploading..." and disables Generate while the request is pending', async () => {
        let answer;
        backend.on('POST /similarity/create', () => new Promise(resolve => { answer = resolve; }));
        const { user } = renderAt(AT);
        await screen.findByText('sim1');

        await user.click(screen.getByRole('button', { name: GENERATE }));

        expect(await screen.findByText('Uploading...')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: GENERATE })).toBeDisabled();

        answer(reply(201));
        await waitFor(() => expect(screen.queryByText('Uploading...')).not.toBeInTheDocument());
        expect(screen.getByRole('button', { name: GENERATE })).toBeEnabled();
        await waitFor(() => expect(backend.sent()).toEqual([...LOADED, ...RELOADED]));
    });

    it('says the upload failed when the backend refuses it (the backend answers 400), and does not reload', async () => {
        backend.on('POST /similarity/create', reply(400));
        const { user } = renderAt(AT);
        await screen.findByText('sim1');

        await user.click(screen.getByRole('button', { name: GENERATE }));

        expect(await screen.findByText('Upload failed.')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: GENERATE })).toBeEnabled();
        expect(backend.sent()).toEqual([...LOADED, 'POST /similarity/create']);
    });

    it('says the name already exists on a 401', async () => {
        // Upstream oddity: SimilarityController.createSimilarity never answers
        // 401 (any failure is 400), so this message is unreachable today.
        backend.on('POST /similarity/create', reply(401));
        const { user } = renderAt(AT);
        await screen.findByText('sim1');

        await user.click(screen.getByRole('button', { name: GENERATE }));

        expect(await screen.findByText('Upload failed. Name already exists.')).toBeInTheDocument();
    });

    // react-toastify drops an update to a toast its container has not shown yet,
    // so these failures answer only once the loading toast is up.
    it('says when the strategy cannot be loaded, and shows no form nor similarities', async () => {
        const strategy = held(reply(400));
        backend.on(STRATEGY_URL, strategy.route);
        renderAt(AT);
        await screen.findByText('Fetching Strategy...');
        strategy.release();

        expect(await screen.findByText('Error Loading Strategy.')).toBeInTheDocument();
        expect(backend.sent()).toEqual([STRATEGY_URL]);
        expect(screen.queryByRole('button', { name: GENERATE })).not.toBeInTheDocument();
    });

    it('says when the similarities cannot be loaded, but still shows the form', async () => {
        const similarities = held(reply(400));
        backend.on(SIMILARITIES_URL, similarities.route);
        renderAt(AT);
        await screen.findByText('Fetching Similarities...');
        similarities.release();

        expect(await screen.findByText("Error while loading strategy's similarity.")).toBeInTheDocument();
        expect(screen.getByRole('button', { name: GENERATE })).toBeInTheDocument();
    });
});

// SimilarityMatrixSciPyForm: the form for Accesses and/or Repository strategies.
describe('the similarity form for an Accesses strategy', () => {
    beforeEach(() => {
        backend = fakeBackend({
            [STRATEGY_URL]: strategy(['Accesses']),
            [SIMILARITIES_URL]: [],
            [ACCESSES_URL]: representation('Accesses'),
            'POST /similarity/create': reply(201),
        });
    });

    async function open() {
        const view = renderAt(AT);
        await screen.findByLabelText('Access Weight (%)');
        await waitFor(() => expect(backend.sent()).toEqual([STRATEGY_URL, ACCESSES_URL, SIMILARITIES_URL]));
        return view;
    }

    it('loads the profiles of the codebase\'s Accesses representation', async () => {
        await open();
    });

    it('shows profile, amount of traces, type of traces, linkage type and four weights of 25, in that order', async () => {
        await open();

        expect(formRows()).toEqual([
            'Select Codebase Profile: ▾Generic',
            'Amount of Traces per Functionality: [|0 by default]',
            'Type of traces: (x) All ( ) Longest ( ) With more different accesses',
            'Linkage Type: (x) Average ( ) Single ( ) Complete',
            'Access Weight (%): [25|0-100]',
            'Write Weight (%): [25|0-100]',
            'Read Weight (%): [25|0-100]',
            'Sequence Weight (%): [25|0-100]',
            '<Generate Similarity Distances>',
        ]);
        expect(screen.getByText('If no number is inserted, 0 is assumed to be the default value meaning the maximum number of traces')).toBeInTheDocument();
    });

    it('lists the representation\'s profiles in the profile dropdown', async () => {
        const { user } = await open();

        await user.click(screen.getByRole('button', { name: 'Generic' }));

        const items = screen.getAllByRole('button').filter(b => b.closest('.dropdown-menu'));
        expect(items.map(i => i.textContent)).toEqual(['Generic', 'Big Functionalities']);
        // Upstream bug: `active={profile === profile}` compares the map variable
        // with itself, so every profile is shown as the active one.
        expect(items.map(i => i.classList.contains('active'))).toEqual([true, true]);
    });

    it('posts the defaults: Generic profile, average linkage, 0 traces of type ALL, and the four weights', async () => {
        const { user } = await open();

        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toEqual([STRATEGY_URL, ACCESSES_URL, SIMILARITIES_URL, ...RELOADED]));
        expect(backend.bodyOf('POST /similarity/create')).toEqual({
            strategyName: 'strat1',
            type: 'SIMILARITY_SCIPY_ACCESSES_REPOSITORY',
            weightsList: [{
                type: 'ACCESSES_WEIGHTS', numberOfWeights: 4,
                weightsLabel: { accessMetricWeight: 'Access Weight', writeMetricWeight: 'Write Weight', readMetricWeight: 'Read Weight', sequenceMetricWeight: 'Sequence Weight' },
                accessMetricWeight: 25, writeMetricWeight: 25, readMetricWeight: 25, sequenceMetricWeight: 25,
            }],
            profile: 'Generic',
            linkageType: 'average',
            tracesMaxLimit: 0,
            traceType: 'ALL',
        });
    });

    it('posts the chosen profile, amount of traces, type of traces, linkage and weights', async () => {
        const { user } = await open();

        await user.click(screen.getByRole('button', { name: 'Generic' }));
        await user.click(screen.getByRole('button', { name: 'Big Functionalities' }));
        await user.type(field('Amount of Traces per Functionality'), '5');
        await user.click(screen.getByLabelText('With more different accesses'));
        await user.click(screen.getByLabelText('Complete'));
        await enterWeight(field('Access Weight (%)'), '40');
        await enterWeight(field('Write Weight (%)'), '10');

        expect(formRows()).toEqual([
            'Select Codebase Profile: ▾Big Functionalities',
            'Amount of Traces per Functionality: [5|0 by default]',
            'Type of traces: ( ) All ( ) Longest (x) With more different accesses',
            'Linkage Type: ( ) Average ( ) Single (x) Complete',
            'Access Weight (%): [40|0-100]',
            'Write Weight (%): [10|0-100]',
            'Read Weight (%): [25|0-100]',
            'Sequence Weight (%): [25|0-100]',
            '<Generate Similarity Distances>',
        ]);
        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toContain('POST /similarity/create'));
        expect(backend.bodyOf('POST /similarity/create')).toEqual(expect.objectContaining({
            profile: 'Big Functionalities',
            linkageType: 'complete',
            tracesMaxLimit: 5,
            traceType: 'WITH_MORE_DIFFERENT_ACCESSES',
        }));
        expect(backend.bodyOf('POST /similarity/create').weightsList[0]).toEqual(expect.objectContaining({
            accessMetricWeight: 40, writeMetricWeight: 10, readMetricWeight: 25, sequenceMetricWeight: 25,
        }));
    });

    it('posts the Longest type of traces and single linkage', async () => {
        const { user } = await open();

        await user.click(screen.getByLabelText('Longest'));
        await user.click(screen.getByLabelText('Single'));
        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toContain('POST /similarity/create'));
        expect(backend.bodyOf('POST /similarity/create')).toEqual(expect.objectContaining({ linkageType: 'single', traceType: 'LONGEST' }));
    });

    it('enables Generate only while the weights sum to 100', async () => {
        await open();
        const generate = screen.getByRole('button', { name: GENERATE });
        expect(generate).toBeEnabled();

        await enterWeight(field('Access Weight (%)'), '30');
        expect(generate).toBeDisabled();

        await enterWeight(field('Write Weight (%)'), '20');
        expect(generate).toBeEnabled();
    });

    it('shows a cleared weight as 0, not empty', async () => {
        // Upstream bug: `event.target.value === 0 ? -1 : ...` compares a string
        // with a number, so clearing never yields the -1 that renders as empty.
        await open();

        await enterWeight(field('Access Weight (%)'), '');

        expect(field('Access Weight (%)')).toHaveValue(0);
        expect(screen.getByRole('button', { name: GENERATE })).toBeDisabled();
    });

    it('shows each similarity as a card with profile, traces, trace type, linkage and weights', async () => {
        backend.on(SIMILARITIES_URL, [similarity('SIMILARITY_SCIPY_ACCESSES_REPOSITORY', 'sim1', {
            profile: 'Generic', linkageType: 'average', tracesMaxLimit: 0, traceType: 'ALL',
            weightsList: [{ type: 'ACCESSES_WEIGHTS', accessMetricWeight: 25, writeMetricWeight: 25, readMetricWeight: 25, sequenceMetricWeight: 25 }],
        })]);
        renderAt(AT);

        await screen.findByText('sim1');
        const card = cardOf('sim1');
        expect(cardLines(card)).toEqual([
            'Profile: Generic',
            'AmountOfTraces: 0',
            'Type of traces: ALL',
            'Linkage Type: average',
            'Access Weight: 25%',
            'Write Weight: 25%',
            'Read Weight: 25%',
            'Sequence Weight: 25%',
        ]);
        expectCardFrame(card, 'sim1');
    });
});

describe('the similarity form for a Repository strategy', () => {
    beforeEach(() => {
        backend = fakeBackend({
            [STRATEGY_URL]: strategy(['Repository']),
            [SIMILARITIES_URL]: [],
            [ACCESSES_URL]: representation('Accesses'),
            'POST /similarity/create': reply(201),
        });
    });

    it('shows the same rows as for Accesses, with an author and a commit weight of 50', async () => {
        renderAt(AT);
        await screen.findByLabelText('Author Weight (%)');
        // Upstream oddity: a Repository-only strategy still loads the profiles
        // of the Accesses representation (and has no catch if there is none).
        await waitFor(() => expect(backend.sent()).toEqual([STRATEGY_URL, ACCESSES_URL, SIMILARITIES_URL]));

        expect(formRows()).toEqual([
            'Select Codebase Profile: ▾Generic',
            'Amount of Traces per Functionality: [|0 by default]',
            'Type of traces: (x) All ( ) Longest ( ) With more different accesses',
            'Linkage Type: (x) Average ( ) Single ( ) Complete',
            'Author Weight (%): [50|0-100]',
            'Commit Weight (%): [50|0-100]',
            '<Generate Similarity Distances>',
        ]);
    });

    it('posts the repository weights', async () => {
        const { user } = renderAt(AT);
        await screen.findByLabelText('Author Weight (%)');

        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toContain('POST /similarity/create'));
        expect(backend.bodyOf('POST /similarity/create')).toEqual({
            strategyName: 'strat1',
            type: 'SIMILARITY_SCIPY_ACCESSES_REPOSITORY',
            weightsList: [{
                type: 'REPOSITORY_WEIGHTS', numberOfWeights: 2,
                weightsLabel: { authorMetricWeight: 'Author Weight', commitMetricWeight: 'Commit Weight' },
                authorMetricWeight: 50, commitMetricWeight: 50,
            }],
            profile: 'Generic',
            linkageType: 'average',
            tracesMaxLimit: 0,
            traceType: 'ALL',
        });
    });
});

describe('the similarity form for an Accesses and Repository strategy', () => {
    beforeEach(() => {
        backend = fakeBackend({
            [STRATEGY_URL]: strategy(['Accesses', 'Repository']),
            [SIMILARITIES_URL]: [],
            [ACCESSES_URL]: representation('Accesses'),
            'POST /similarity/create': reply(201),
        });
    });

    it('shows the six weights, the accesses ones at 17 and the repository ones at 16', async () => {
        renderAt(AT);
        await screen.findByLabelText('Author Weight (%)');

        expect(formRows()).toEqual([
            'Select Codebase Profile: ▾Generic',
            'Amount of Traces per Functionality: [|0 by default]',
            'Type of traces: (x) All ( ) Longest ( ) With more different accesses',
            'Linkage Type: (x) Average ( ) Single ( ) Complete',
            'Access Weight (%): [17|0-100]',
            'Write Weight (%): [17|0-100]',
            'Read Weight (%): [17|0-100]',
            'Sequence Weight (%): [17|0-100]',
            'Author Weight (%): [16|0-100]',
            'Commit Weight (%): [16|0-100]',
            '<Generate Similarity Distances>',
        ]);
    });

    it('posts both weights objects, accesses first', async () => {
        const { user } = renderAt(AT);
        await screen.findByLabelText('Author Weight (%)');

        await enterWeight(field('Commit Weight (%)'), '6');
        expect(screen.getByRole('button', { name: GENERATE })).toBeDisabled();
        await enterWeight(field('Author Weight (%)'), '26');
        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toContain('POST /similarity/create'));
        expect(backend.bodyOf('POST /similarity/create').weightsList).toEqual([
            {
                type: 'ACCESSES_WEIGHTS', numberOfWeights: 4,
                weightsLabel: { accessMetricWeight: 'Access Weight', writeMetricWeight: 'Write Weight', readMetricWeight: 'Read Weight', sequenceMetricWeight: 'Sequence Weight' },
                accessMetricWeight: 17, writeMetricWeight: 17, readMetricWeight: 17, sequenceMetricWeight: 17,
            },
            {
                type: 'REPOSITORY_WEIGHTS', numberOfWeights: 2,
                weightsLabel: { authorMetricWeight: 'Author Weight', commitMetricWeight: 'Commit Weight' },
                authorMetricWeight: 26, commitMetricWeight: 6,
            },
        ]);
    });

    it('shows both weights on the card', async () => {
        backend.on(SIMILARITIES_URL, [similarity('SIMILARITY_SCIPY_ACCESSES_REPOSITORY', 'sim1', {
            profile: 'Big Functionalities', linkageType: 'complete', tracesMaxLimit: 10, traceType: 'LONGEST',
            weightsList: [
                { type: 'ACCESSES_WEIGHTS', accessMetricWeight: 17, writeMetricWeight: 17, readMetricWeight: 17, sequenceMetricWeight: 17 },
                { type: 'REPOSITORY_WEIGHTS', authorMetricWeight: 16, commitMetricWeight: 16 },
            ],
        })]);
        renderAt(AT);

        await screen.findByText('sim1');
        const card = cardOf('sim1');
        expect(cardLines(card)).toEqual([
            'Profile: Big Functionalities',
            'AmountOfTraces: 10',
            'Type of traces: LONGEST',
            'Linkage Type: complete',
            'Access Weight: 17%',
            'Write Weight: 17%',
            'Read Weight: 17%',
            'Sequence Weight: 17%',
            'Author Weight: 16%',
            'Commit Weight: 16%',
        ]);
        expectCardFrame(card, 'sim1');
    });
});

// SimilarityMatrixSciPyStructureForm
describe('the similarity form for a Structure strategy', () => {
    beforeEach(() => {
        backend = fakeBackend({
            [STRATEGY_URL]: strategy(['Structure']),
            [SIMILARITIES_URL]: [],
            [STRUCTURE_URL]: representation('Structure'),
            'POST /similarity/create': reply(201),
        });
    });

    async function open() {
        const view = renderAt(AT);
        await screen.findByLabelText('One to One Weight (%)');
        await waitFor(() => expect(backend.sent()).toEqual([STRATEGY_URL, STRUCTURE_URL, SIMILARITIES_URL]));
        return view;
    }

    it('loads the profiles of the codebase\'s Structure representation', async () => {
        await open();
    });

    it('shows profile, type of traces, linkage type and three weights 33/33/34, with no amount of traces', async () => {
        await open();

        expect(formRows()).toEqual([
            'Select Codebase Profile: ▾Generic',
            'Type of traces: (x) All ( ) Longest ( ) With more different accesses',
            'Linkage Type: (x) Average ( ) Single ( ) Complete',
            'One to One Weight (%): [33|0-100]',
            'One to Many Weight (%): [33|0-100]',
            'Inheritance Weight (%): [34|0-100]',
            '<Generate Similarity Distances>',
        ]);
    });

    it('lists the representation\'s profiles in the profile dropdown', async () => {
        const { user } = await open();

        await user.click(screen.getByRole('button', { name: 'Generic' }));

        const items = screen.getAllByRole('button').filter(b => b.closest('.dropdown-menu'));
        expect(items.map(i => i.textContent)).toEqual(['Generic', 'Big Functionalities']);
        // Upstream bug: `active={profile === profile}`, every profile is active.
        expect(items.map(i => i.classList.contains('active'))).toEqual([true, true]);
    });

    it('posts the defaults: Generic profile, average linkage, ALL traces and the three weights', async () => {
        const { user } = await open();

        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toEqual([STRATEGY_URL, STRUCTURE_URL, SIMILARITIES_URL, ...RELOADED]));
        expect(backend.bodyOf('POST /similarity/create')).toEqual({
            strategyName: 'strat1',
            type: 'SIMILARITY_SCIPY_STRUCTURE',
            weightsList: [{
                type: 'STRUCTURE_WEIGHTS', numberOfWeights: 3,
                weightsLabel: { oneToOneWeight: 'One to One Weight', oneToManyWeight: 'One to Many Weight', heritageWeight: 'Inheritance Weight' },
                oneToOneWeight: 33, oneToManyWeight: 33, heritageWeight: 34,
            }],
            profile: 'Generic',
            linkageType: 'average',
            traceType: 'ALL',
        });
    });

    it('posts the chosen profile, type of traces, linkage and weights', async () => {
        const { user } = await open();

        await user.click(screen.getByRole('button', { name: 'Generic' }));
        await user.click(screen.getByRole('button', { name: 'Big Functionalities' }));
        await user.click(screen.getByLabelText('Longest'));
        await user.click(screen.getByLabelText('Single'));
        await enterWeight(field('One to One Weight (%)'), '50');
        expect(screen.getByRole('button', { name: GENERATE })).toBeDisabled();
        await enterWeight(field('Inheritance Weight (%)'), '17');
        expect(screen.getByRole('button', { name: GENERATE })).toBeEnabled();
        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toContain('POST /similarity/create'));
        const body = backend.bodyOf('POST /similarity/create');
        expect(body).toEqual(expect.objectContaining({ profile: 'Big Functionalities', linkageType: 'single', traceType: 'LONGEST' }));
        expect(body.weightsList[0]).toEqual(expect.objectContaining({ oneToOneWeight: 50, oneToManyWeight: 33, heritageWeight: 17 }));
    });

    it('shows each similarity as a card with only its weights', async () => {
        // Upstream oddity: the card shows neither the profile, the linkage type
        // nor the type of traces the similarity was made with.
        backend.on(SIMILARITIES_URL, [similarity('SIMILARITY_SCIPY_STRUCTURE', 'sim1', {
            profile: 'Generic', linkageType: 'average', traceType: 'ALL',
            weightsList: [{ type: 'STRUCTURE_WEIGHTS', oneToOneWeight: 33, oneToManyWeight: 33, heritageWeight: 34 }],
        })]);
        renderAt(AT);

        await screen.findByText('sim1');
        const card = cardOf('sim1');
        expect(cardLines(card)).toEqual([
            'One to One Weight: 33%',
            'One to Many Weight: 33%',
            'Inheritance Weight: 34%',
        ]);
        expectCardFrame(card, 'sim1');
    });
});

// SimilarityMatrixSciPyClassVectorizationForm
describe('the similarity form for a Class Vectorization strategy', () => {
    beforeEach(() => {
        backend = fakeBackend({
            [STRATEGY_URL]: strategy(['Class Vectorization']),
            [SIMILARITIES_URL]: [],
            'POST /similarity/create': reply(201),
        });
    });

    it('shows only the linkage type, and Generate enabled, loading no representation', async () => {
        renderAt(AT);
        await screen.findByRole('button', { name: GENERATE });
        await waitFor(() => expect(backend.sent()).toEqual(LOADED));

        expect(formRows()).toEqual([
            'Linkage Type: (x) Average ( ) Single ( ) Complete',
            '<Generate Similarity Distances>',
        ]);
    });

    it('posts the strategy, type and default average linkage', async () => {
        const { user } = renderAt(AT);
        await screen.findByRole('button', { name: GENERATE });

        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toEqual([...LOADED, ...RELOADED]));
        expect(backend.bodyOf('POST /similarity/create')).toEqual({
            strategyName: 'strat1', type: 'SIMILARITY_SCIPY_CLASS_VECTORIZATION', linkageType: 'average',
        });
    });

    it('posts the chosen linkage', async () => {
        const { user } = renderAt(AT);
        await screen.findByRole('button', { name: GENERATE });

        await user.click(screen.getByLabelText('Complete'));
        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toContain('POST /similarity/create'));
        expect(backend.bodyOf('POST /similarity/create')).toEqual({
            strategyName: 'strat1', type: 'SIMILARITY_SCIPY_CLASS_VECTORIZATION', linkageType: 'complete',
        });
    });

    it('shows each similarity as a card with its linkage type', async () => {
        backend.on(SIMILARITIES_URL, [similarity('SIMILARITY_SCIPY_CLASS_VECTORIZATION', 'sim1', { linkageType: 'single' })]);
        renderAt(AT);

        await screen.findByText('sim1');
        const card = cardOf('sim1');
        expect(cardLines(card)).toEqual(['Linkage Type: single']);
        expectCardFrame(card, 'sim1');
    });
});

// SimilarityMatrixSciPyEntityVectorizationForm
describe('the similarity form for an Entity Vectorization strategy', () => {
    beforeEach(() => {
        backend = fakeBackend({
            [STRATEGY_URL]: strategy(['Entity Vectorization']),
            [SIMILARITIES_URL]: [],
            'POST /similarity/create': reply(201),
        });
    });

    it('shows only the linkage type, and Generate enabled, loading no representation', async () => {
        renderAt(AT);
        await screen.findByRole('button', { name: GENERATE });
        await waitFor(() => expect(backend.sent()).toEqual(LOADED));

        expect(formRows()).toEqual([
            'Linkage Type: (x) Average ( ) Single ( ) Complete',
            '<Generate Similarity Distances>',
        ]);
    });

    it('posts the strategy, type and default average linkage', async () => {
        const { user } = renderAt(AT);
        await screen.findByRole('button', { name: GENERATE });

        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toEqual([...LOADED, ...RELOADED]));
        expect(backend.bodyOf('POST /similarity/create')).toEqual({
            strategyName: 'strat1', type: 'SIMILARITY_SCIPY_ENTITY_VECTORIZATION', linkageType: 'average',
        });
    });

    it('posts the chosen linkage', async () => {
        const { user } = renderAt(AT);
        await screen.findByRole('button', { name: GENERATE });

        await user.click(screen.getByLabelText('Single'));
        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toContain('POST /similarity/create'));
        expect(backend.bodyOf('POST /similarity/create')).toEqual({
            strategyName: 'strat1', type: 'SIMILARITY_SCIPY_ENTITY_VECTORIZATION', linkageType: 'single',
        });
    });

    it('shows each similarity as a card with its linkage type', async () => {
        backend.on(SIMILARITIES_URL, [similarity('SIMILARITY_SCIPY_ENTITY_VECTORIZATION', 'sim1', { linkageType: 'complete' })]);
        renderAt(AT);

        await screen.findByText('sim1');
        const card = cardOf('sim1');
        expect(cardLines(card)).toEqual(['Linkage Type: complete']);
        expectCardFrame(card, 'sim1');
    });
});

// SimilarityMatrixSciPyFunctionalityVectorizationCallGraphForm
describe('the similarity form for a Functionality Vectorization Call Graph strategy', () => {
    beforeEach(() => {
        backend = fakeBackend({
            [STRATEGY_URL]: strategy(['Functionality Vectorization Call Graph']),
            [SIMILARITIES_URL]: [],
            'POST /similarity/create': reply(201),
        });
    });

    async function open() {
        const view = renderAt(AT);
        await screen.findByLabelText('Controllers Weight (%)');
        await waitFor(() => expect(backend.sent()).toEqual(LOADED));
        return view;
    }

    it('shows linkage type, an empty max depth and four weights of 25, loading no representation', async () => {
        await open();

        expect(formRows()).toEqual([
            'Linkage Type: (x) Average ( ) Single ( ) Complete',
            'Max Depth: [|1-N]',
            'Controllers Weight (%): [25|0-100]',
            'Services Weight (%): [25|0-100]',
            'Intermediate Methods Weight (%): [25|0-100]',
            'Entities Weight (%): [25|0-100]',
            '<Generate Similarity Distances (disabled)>',
        ]);
    });

    it('enables Generate only once the depth is above 0 and the weights sum to 100', async () => {
        const { user } = await open();
        const generate = screen.getByRole('button', { name: GENERATE });
        // Max Depth's legend is not tied to its field, so the field is found by placeholder.
        const depth = screen.getByPlaceholderText('1-N');

        await user.type(depth, '0');
        expect(generate).toBeDisabled();

        await setNumber(user, depth, '3');
        expect(generate).toBeEnabled();

        await enterWeight(field('Services Weight (%)'), '30');
        expect(generate).toBeDisabled();
    });

    it('posts the strategy, type, linkage, depth and the four weights', async () => {
        const { user } = await open();

        await user.type(screen.getByPlaceholderText('1-N'), '3');
        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toEqual([...LOADED, ...RELOADED]));
        expect(backend.bodyOf('POST /similarity/create')).toEqual({
            strategyName: 'strat1',
            type: 'SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_CALLGRAPH',
            linkageType: 'average',
            depth: 3,
            weightsList: [{
                type: 'FUNCTIONALITY_VECTORIZATION_CALLGRAPH_WEIGHTS', numberOfWeights: 4,
                weightsLabel: { controllersWeight: 'Controllers Weight', servicesWeight: 'Services Weight', intermediateMethodsWeight: 'Intermediate Methods Weight', entitiesWeight: 'Entities Weight' },
                controllersWeight: 25, servicesWeight: 25, intermediateMethodsWeight: 25, entitiesWeight: 25,
            }],
        });
    });

    it('posts the chosen linkage and weights', async () => {
        const { user } = await open();

        await user.type(screen.getByPlaceholderText('1-N'), '2');
        await user.click(screen.getByLabelText('Single'));
        await enterWeight(field('Controllers Weight (%)'), '40');
        await enterWeight(field('Entities Weight (%)'), '10');
        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toContain('POST /similarity/create'));
        const body = backend.bodyOf('POST /similarity/create');
        expect(body).toEqual(expect.objectContaining({ linkageType: 'single', depth: 2 }));
        expect(body.weightsList[0]).toEqual(expect.objectContaining({
            controllersWeight: 40, servicesWeight: 25, intermediateMethodsWeight: 25, entitiesWeight: 10,
        }));
    });

    it('shows each similarity as a card with linkage type, depth and weights', async () => {
        backend.on(SIMILARITIES_URL, [similarity('SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_CALLGRAPH', 'sim1', {
            linkageType: 'average', depth: 3,
            weightsList: [{ type: 'FUNCTIONALITY_VECTORIZATION_CALLGRAPH_WEIGHTS', controllersWeight: 25, servicesWeight: 25, intermediateMethodsWeight: 25, entitiesWeight: 25 }],
        })]);
        renderAt(AT);

        await screen.findByText('sim1');
        const card = cardOf('sim1');
        expect(cardLines(card)).toEqual([
            'Linkage Type: average',
            'Depth: 3',
            'Controllers Weight: 25%',
            'Services Weight: 25%',
            'Intermediate Methods Weight: 25%',
            'Entities Weight: 25%',
        ]);
        expectCardFrame(card, 'sim1');
    });
});

// SimilarityMatrixScipyFunctionalityVectorizationSequenceOfAccessesForm
describe('the similarity form for a Functionality Vectorization Sequence Accesses strategy', () => {
    beforeEach(() => {
        backend = fakeBackend({
            [STRATEGY_URL]: strategy(['Functionality Vectorization Sequence Accesses']),
            [SIMILARITIES_URL]: [],
            'POST /similarity/create': reply(201),
        });
    });

    async function open() {
        const view = renderAt(AT);
        await screen.findByLabelText('Read Weight (%)');
        await waitFor(() => expect(backend.sent()).toEqual(LOADED));
        return view;
    }

    it('shows linkage type and a read and a write weight of 50, loading no representation', async () => {
        await open();

        expect(formRows()).toEqual([
            'Linkage Type: (x) Average ( ) Single ( ) Complete',
            'Read Weight (%): [50|0-100]',
            'Write Weight (%): [50|0-100]',
            '<Generate Similarity Distances>',
        ]);
    });

    it('enables Generate only while the weights sum to 100', async () => {
        await open();
        const generate = screen.getByRole('button', { name: GENERATE });

        await enterWeight(field('Read Weight (%)'), '70');
        expect(generate).toBeDisabled();

        await enterWeight(field('Write Weight (%)'), '30');
        expect(generate).toBeEnabled();
    });

    it('posts the strategy, type, linkage and the two weights', async () => {
        const { user } = await open();

        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toEqual([...LOADED, ...RELOADED]));
        expect(backend.bodyOf('POST /similarity/create')).toEqual({
            strategyName: 'strat1',
            type: 'SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES',
            linkageType: 'average',
            weightsList: [{
                type: 'FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS', numberOfWeights: 2,
                weightsLabel: { readMetricWeight: 'Read Weight', writeMetricWeight: 'Write Weight' },
                readMetricWeight: 50, writeMetricWeight: 50,
            }],
        });
    });

    it('posts the chosen linkage and weights', async () => {
        const { user } = await open();

        await user.click(screen.getByLabelText('Complete'));
        await enterWeight(field('Read Weight (%)'), '80');
        await enterWeight(field('Write Weight (%)'), '20');
        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toContain('POST /similarity/create'));
        const body = backend.bodyOf('POST /similarity/create');
        expect(body.linkageType).toBe('complete');
        expect(body.weightsList[0]).toEqual(expect.objectContaining({ readMetricWeight: 80, writeMetricWeight: 20 }));
    });

    it('shows each similarity as a card with linkage type and weights', async () => {
        backend.on(SIMILARITIES_URL, [similarity('SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES', 'sim1', {
            linkageType: 'single',
            weightsList: [{ type: 'FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS', readMetricWeight: 60, writeMetricWeight: 40 }],
        })]);
        renderAt(AT);

        await screen.findByText('sim1');
        const card = cardOf('sim1');
        expect(cardLines(card)).toEqual([
            'Linkage Type: single',
            'Read Weight: 60%',
            'Write Weight: 40%',
        ]);
        expectCardFrame(card, 'sim1');
    });
});
