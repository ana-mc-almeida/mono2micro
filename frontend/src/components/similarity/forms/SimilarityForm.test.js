import { screen, within, waitFor, act } from '@testing-library/react';
import { fakeBackend, held, reply } from '../../../testing/fakeBackend';
import { renderAt } from '../../../testing/renderAt';
import { cardOf } from '../../../testing/screenReaders';

// Upstream's similarity screen (`Similarities.js`) and its six forms, carried onto
// Phase 1: ticket 09 moved the Similarities onto the software system's page, picked
// by kind instead of by the strategy's types, and ticket 23 replaced the six forms
// with one `SimilarityForm` that renders whatever the backend's kind declares. One
// describe per upstream form, each pinning the form's rows in order (label,
// controls, defaults), when Generate is enabled, the exact body it posts, and the
// card a Similarity of its kind shows: so these are ticket 23's "screens unchanged"
// check, and every difference from upstream is named by the ticket that made it.

const AT = '/softwareSystems/sys1';
const SIMILARITIES_URL = 'GET /softwareSystems/sys1/similarities';
const CREATE = 'POST /softwareSystems/sys1/similarities/create';
const GENERATE = 'Generate Similarity Distances';

const ON_OPENING = [
    'GET /softwareSystems/sys1',
    'GET /softwareSystems/sys1/getRepresentations',
    'GET /softwareSystems/sys1/profiles/representationTypes',
    SIMILARITIES_URL,
    'GET /softwareSystems/sys1/similarities/available',
    'GET /softwareSystems/sys1/getSoftwareSystemRepresentationGroups',
    'GET /softwareSystems/getRepresentationGroups',
];

const KINDS = [
    'Accesses', 'Repository', 'Accesses and Repository', 'Structure', 'Class Vectorization', 'Entity Vectorization',
    'Functionality Vectorization Call Graph', 'Functionality Vectorization Sequence Accesses',
];

// Ticket 23: the backend's kind declares what its form shows (SimilarityKindDto, the
// kinds under similarity/kind/): the request type, the settings, the default weights.
const TRACES = ['TRACES_MAX_LIMIT', 'TRACE_TYPE'];
const ACCESSES_WEIGHTS = (w, x, r, s) =>
    ({ type: 'ACCESSES_WEIGHTS', accessMetricWeight: w, writeMetricWeight: x, readMetricWeight: r, sequenceMetricWeight: s });
const REPOSITORY_WEIGHTS = (a, c) => ({ type: 'REPOSITORY_WEIGHTS', authorMetricWeight: a, commitMetricWeight: c });
const KIND_DESCRIPTIONS = {
    'Accesses': ['SIMILARITY_SCIPY_ACCESSES_REPOSITORY', TRACES, [ACCESSES_WEIGHTS(25, 25, 25, 25)]],
    'Repository': ['SIMILARITY_SCIPY_ACCESSES_REPOSITORY', TRACES, [REPOSITORY_WEIGHTS(50, 50)]],
    'Accesses and Repository': ['SIMILARITY_SCIPY_ACCESSES_REPOSITORY', TRACES, [ACCESSES_WEIGHTS(17, 17, 17, 17), REPOSITORY_WEIGHTS(16, 16)]],
    'Structure': ['SIMILARITY_SCIPY_STRUCTURE', ['TRACE_TYPE'],
        [{ type: 'STRUCTURE_WEIGHTS', oneToOneWeight: 33, oneToManyWeight: 33, heritageWeight: 34 }]],
    'Class Vectorization': ['SIMILARITY_SCIPY_CLASS_VECTORIZATION', [], []],
    'Entity Vectorization': ['SIMILARITY_SCIPY_ENTITY_VECTORIZATION', [], []],
    'Functionality Vectorization Call Graph': ['SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_CALLGRAPH', ['MAX_DEPTH'],
        [{ type: 'FUNCTIONALITY_VECTORIZATION_CALLGRAPH_WEIGHTS', controllersWeight: 25, servicesWeight: 25, intermediateMethodsWeight: 25, entitiesWeight: 25 }]],
    'Functionality Vectorization Sequence Accesses': ['SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES', [],
        [{ type: 'FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS', readMetricWeight: 50, writeMetricWeight: 50 }]],
};

// The kinds that take a profile (their scopedRepresentationType), and so the profile
// routes each asks on choosing it. Ticket 09: whether the selector shows is the
// backend's derivation record, and which representation's profiles it lists is the
// backend's call from the kind.
const PROFILED = ['Accesses', 'Repository', 'Accesses and Repository', 'Structure'];
const kindQuery = kind => kind.replace(/ /g, '+');
const PROFILES_OFFERED = kind => `GET /softwareSystems/sys1/profiles/available?kindName=${kindQuery(kind)}`;
const PROFILES_LIST = kind => `GET /softwareSystems/sys1/profiles?kindName=${kindQuery(kind)}`;
const KIND_URL = kind => `GET /similarityKinds/${encodeURIComponent(kind)}`;
function choosing(kind) {
    return PROFILED.includes(kind)
        ? [PROFILES_OFFERED(kind), KIND_URL(kind), PROFILES_LIST(kind)]
        : [PROFILES_OFFERED(kind), KIND_URL(kind)];
}

// Ticket 08: profiles are records, addressed by id; "Generic" is gone.
const PROFILES = [{ id: 'p1', name: 'Login' }, { id: 'p2', name: 'Big Functionalities' }];

// SimilarityDto: ticket 07 gives it an id; ticket 09 a kindName instead of a strategy;
// ticket 08 a profileId and profileName instead of a profile name.
function similarity(type, name, fields) {
    return { id: name, name, softwareSystemId: 'sys1', type, kindName: 'Accesses', ...fields };
}

function routes(kind) {
    const offered = available => ({ features: [{ name: 'Profile', available, failedRequirements: [] }] });
    return {
        'GET /softwareSystems/sys1': { id: 'sys1', name: 'ldod', representations: [] },
        'GET /softwareSystems/sys1/getRepresentations': [],
        'GET /softwareSystems/sys1/profiles/representationTypes': [],
        [SIMILARITIES_URL]: [],
        'GET /softwareSystems/sys1/similarities/available':
            { features: KINDS.map(name => ({ name, available: true, failedRequirements: [] })) },
        'GET /softwareSystems/sys1/getSoftwareSystemRepresentationGroups': [],
        'GET /softwareSystems/getRepresentationGroups': {},
        'GET /softwareSystems/sys1/profiles/available': offered(PROFILED.includes(kind)),
        'GET /softwareSystems/sys1/profiles': PROFILES,
        ...(kind && {
            [KIND_URL(kind)]: {
                name: kind, similarityType: KIND_DESCRIPTIONS[kind][0], settings: KIND_DESCRIPTIONS[kind][1], defaultWeights: KIND_DESCRIPTIONS[kind][2],
            },
        }),
        [CREATE]: reply(201),
    };
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

// What every Similarity card has besides its text: the dendrogram image, the
// Decomposition Generation link and Delete.
function expectCardFrame(card, id) {
    // U11: URL already ends in "/", so the image URL has "//similarity" (by id, ticket 07).
    expect(card.querySelector('img').getAttribute('src'))
        .toMatch(new RegExp(`^http://localhost:8080/mono2micro//similarity/${id}/image\\?\\d+$`));
    expect(within(card).getAllByRole(/button|link/).map(b => b.textContent)).toEqual(['Decomposition Generation', 'Delete']);
    // Tickets 07 and 09: the decomposition screen lives under the software system's Similarities.
    expect(within(card).getByText('Decomposition Generation'))
        .toHaveAttribute('href', `/softwareSystems/sys1/similarities/${id}/decomposition`);
}

// Types a weight the way a browser delivers it. U5, moved by ticket 23 into
// SimilarityForm: changeMetricWeight reads `event.target.value` inside a
// setWeightsList updater, which React may run lazily; under act() (so with
// userEvent or fireEvent) it runs after React has restored the controlled input,
// reads the old value, and the edit is lost. A browser flushes the update before
// that restore, so the event is dispatched natively here, outside act, and act
// only flushes effects.
async function enterWeight(input, value) {
    Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(input, value);
    input.dispatchEvent(new Event('input', { bubbles: true }));
    await act(() => Promise.resolve());
}

async function setNumber(user, input, value) {
    await user.clear(input);
    await user.type(input, value);
}

// Opens the page and chooses the kind; resolves once its form has loaded.
async function openKind(kind) {
    const view = renderAt(AT);
    await view.user.click(await screen.findByRole('radio', { name: kind }));
    await waitFor(() => expect(backend.sent()).toEqual([...ON_OPENING, ...choosing(kind)]));
    await waitFor(() => expect(screen.getByRole('button', { name: GENERATE }).closest('form').textContent).toContain('Linkage Type'));
    if (PROFILED.includes(kind)) await screen.findByRole('button', { name: 'No profile (all units)' });
    return view;
}

let backend;
afterEach(() => {
    expect(backend.unhandled).toEqual([]);
});

// Upstream reloaded the strategy and its similarities after a create; ticket 09: only the Similarities.
const RELOADED = [CREATE, SIMILARITIES_URL];

describe('the Similarities of the software system page', () => {
    const SIMILARITIES = [
        similarity('SIMILARITY_SCIPY_CLASS_VECTORIZATION', 'sim1', { kindName: 'Class Vectorization', linkageType: 'average' }),
        similarity('SIMILARITY_SCIPY_CLASS_VECTORIZATION', 'sim2', { kindName: 'Class Vectorization', linkageType: 'single' }),
    ];

    beforeEach(() => {
        backend = fakeBackend({
            ...routes('Class Vectorization'),
            [SIMILARITIES_URL]: SIMILARITIES,
            'DELETE /similarity/sim1/delete': null,
        });
    });

    // Ticket 09: one card each, in the order received, under the Similarities heading
    // that also holds the kinds (upstream: their own "Similarity Distances" heading).
    it('shows the Similarities, one card each, in the order received', async () => {
        renderAt(AT);
        await screen.findByText('sim1');

        expect(screen.queryByRole('heading', { name: 'Similarity Distances' })).not.toBeInTheDocument();
        const titles = screen.getAllByRole('button', { name: 'Delete' }).map(b => within(b.closest('.card')).getByText(/^sim/).textContent);
        expect(titles).toEqual(['sim1', 'sim2']);
    });

    it('shows no cards when the software system has no Similarities', async () => {
        backend.on(SIMILARITIES_URL, []);
        renderAt(AT);
        await screen.findByRole('radio', { name: 'Accesses' });
        await waitFor(() => expect(backend.sent()).toEqual(ON_OPENING));

        expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument();
    });

    // Ticket 09: no strategy, so no form until a kind is chosen (upstream showed the
    // strategy's form at once, and none for a non-SciPy strategy).
    it('shows no form until a kind is chosen', async () => {
        renderAt(AT);
        await screen.findByText('sim1');

        expect(screen.queryByRole('button', { name: GENERATE })).not.toBeInTheDocument();
    });

    it('deletes a Similarity by its id, then reloads only the Similarities', async () => {
        const { user } = renderAt(AT);
        await screen.findByText('sim1');

        await user.click(within(cardOf('sim1')).getByRole('button', { name: 'Delete' }));

        // P3 fixed: the page renders a ToastContainer, so the deletion's toasts show, as upstream's did.
        expect(await screen.findByText('Similarity deleted.')).toBeInTheDocument();
        expect(backend.sent()).toEqual([...ON_OPENING, 'DELETE /similarity/sim1/delete', SIMILARITIES_URL]);
    });

    it('says when a delete fails, and does not reload', async () => {
        backend.on('DELETE /similarity/sim1/delete', reply(400));
        const { user } = renderAt(AT);
        await screen.findByText('sim1');

        await user.click(within(cardOf('sim1')).getByRole('button', { name: 'Delete' }));

        expect(await screen.findByText('Error deleting sim1.')).toBeInTheDocument();
        expect(backend.sent()).toEqual([...ON_OPENING, 'DELETE /similarity/sim1/delete']);
    });

    it('after a Similarity is created, reloads the Similarities and shows the new card', async () => {
        let created = false;
        backend.on(CREATE, () => { created = true; return reply(201); });
        backend.on(SIMILARITIES_URL, () => created
            ? [...SIMILARITIES, similarity('SIMILARITY_SCIPY_CLASS_VECTORIZATION', 'sim3', { kindName: 'Class Vectorization', linkageType: 'average' })]
            : SIMILARITIES);
        const { user } = await openKind('Class Vectorization');

        await user.click(screen.getByRole('button', { name: GENERATE }));

        expect(await screen.findByText('sim3')).toBeInTheDocument();
        expect(backend.sent()).toEqual([...ON_OPENING, ...choosing('Class Vectorization'), ...RELOADED]);
    });

    it('says "Uploading..." and disables Generate while the request is pending', async () => {
        let answer;
        backend.on(CREATE, () => new Promise(resolve => { answer = resolve; }));
        const { user } = await openKind('Class Vectorization');

        await user.click(screen.getByRole('button', { name: GENERATE }));

        expect(await screen.findByText('Uploading...')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: GENERATE })).toBeDisabled();

        answer(reply(201));
        await waitFor(() => expect(screen.queryByText('Uploading...')).not.toBeInTheDocument());
        expect(screen.getByRole('button', { name: GENERATE })).toBeEnabled();
        await waitFor(() => expect(backend.sent()).toEqual([...ON_OPENING, ...choosing('Class Vectorization'), ...RELOADED]));
    });

    // Ticket 23: the page maps every status once; a 401, which the backend never
    // answers, is a plain failure now (U12, upstream: "Name already exists.").
    it.each([
        [400, 'Upload failed.'],
        [401, 'Upload failed.'],
        [422, 'Upload failed. The software system does not offer this kind.'],
    ])('on a %i reply, says "%s", and does not reload', async (status, message) => {
        backend.on(CREATE, reply(status));
        const { user } = await openKind('Class Vectorization');

        await user.click(screen.getByRole('button', { name: GENERATE }));

        expect(await screen.findByText(message)).toBeInTheDocument();
        expect(screen.getByRole('button', { name: GENERATE })).toBeEnabled();
        expect(backend.sent()).toEqual([...ON_OPENING, ...choosing('Class Vectorization'), CREATE]);
    });

    // Ticket 23: what the form shows when the kind cannot be loaded.
    it('says when the kind could not be loaded, and keeps Generate disabled', async () => {
        backend.on(KIND_URL('Class Vectorization'), reply(400));
        const { user } = renderAt(AT);

        await user.click(await screen.findByRole('radio', { name: 'Class Vectorization' }));

        expect(await screen.findByText("Could not load this kind's settings.")).toBeInTheDocument();
        expect(screen.getByRole('button', { name: GENERATE })).toBeDisabled();
    });

    // Ticket 23: no weights must not read as "not loaded": Generate waits for the kind.
    it('keeps Generate disabled until the kind loads, even for a kind without weights', async () => {
        const description = held(routes('Class Vectorization')[KIND_URL('Class Vectorization')]);
        backend.on(KIND_URL('Class Vectorization'), description.route);
        const { user } = renderAt(AT);

        await user.click(await screen.findByRole('radio', { name: 'Class Vectorization' }));
        expect(await screen.findByRole('button', { name: GENERATE })).toBeDisabled();

        description.release();
        await waitFor(() => expect(screen.getByRole('button', { name: GENERATE })).toBeEnabled());
    });

    // P4 fixed: as upstream's screen did, a failed load says so (ticket 09: of "the
    // Similarities"; upstream: "strategy's similarity"), and the kinds are still offered.
    // react-toastify drops an update to a toast its container has not shown yet, so the
    // failure answers only once the loading toast is up.
    it('says when the Similarities cannot be loaded, but still offers the kinds', async () => {
        const similarities = held(reply(400));
        backend.on(SIMILARITIES_URL, similarities.route);
        renderAt(AT);
        await screen.findByText('Fetching Similarities...');
        similarities.release();

        expect(await screen.findByText('Error while loading the Similarities.')).toBeInTheDocument();
        expect(screen.getByRole('radio', { name: 'Accesses' })).toBeInTheDocument();
    });
});

// Upstream's SimilarityMatrixSciPyForm, for Accesses and/or Repository strategies.
describe('the Similarity form for the Accesses kind', () => {
    beforeEach(() => {
        backend = fakeBackend(routes('Accesses'));
    });

    // Ticket 23: the form asks the backend for the kind; ticket 09: whether a profile
    // is offered, then the profiles the backend says the kind scopes (upstream: the
    // codebase's Accesses representation).
    it('loads the kind, and the profiles the backend offers for it', async () => {
        await openKind('Accesses');
    });

    // Ticket 08: the profile row reads "Profile" (upstream: "Select Codebase Profile"),
    // and defaults to no profile (upstream: Generic).
    it('shows profile, amount of traces, type of traces, linkage type and four weights of 25, in that order', async () => {
        await openKind('Accesses');

        expect(formRows()).toEqual([
            'Profile: ▾No profile (all units)',
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

    // U3, resolved by ticket 08: the shared ProfileSelect marks only the chosen profile.
    it('lists no profile, then the offered profiles, in the profile dropdown, marking only the chosen one', async () => {
        const { user } = await openKind('Accesses');

        await user.click(screen.getByRole('button', { name: 'No profile (all units)' }));

        const items = screen.getAllByRole('button').filter(b => b.closest('.dropdown-menu'));
        expect(items.map(i => i.textContent)).toEqual(['No profile (all units)', 'Login', 'Big Functionalities']);
        expect(items.map(i => i.classList.contains('active'))).toEqual([true, false, false]);
    });

    // Ticket 09: the kind's name replaces the strategy's; ticket 08: profileId, null for
    // no profile, replaces the profile's name.
    it('posts the defaults: no profile, average linkage, 0 traces of type ALL, and the four weights', async () => {
        const { user } = await openKind('Accesses');

        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toEqual([...ON_OPENING, ...choosing('Accesses'), ...RELOADED]));
        expect(backend.bodyOf(CREATE)).toEqual({
            kindName: 'Accesses',
            type: 'SIMILARITY_SCIPY_ACCESSES_REPOSITORY',
            weightsList: [{
                type: 'ACCESSES_WEIGHTS', numberOfWeights: 4,
                weightsLabel: { accessMetricWeight: 'Access Weight', writeMetricWeight: 'Write Weight', readMetricWeight: 'Read Weight', sequenceMetricWeight: 'Sequence Weight' },
                accessMetricWeight: 25, writeMetricWeight: 25, readMetricWeight: 25, sequenceMetricWeight: 25,
            }],
            profileId: null,
            linkageType: 'average',
            tracesMaxLimit: 0,
            traceType: 'ALL',
        });
    });

    it('posts the chosen profile, amount of traces, type of traces, linkage and weights', async () => {
        const { user } = await openKind('Accesses');

        await user.click(screen.getByRole('button', { name: 'No profile (all units)' }));
        await user.click(screen.getByRole('button', { name: 'Big Functionalities' }));
        await user.type(field('Amount of Traces per Functionality'), '5');
        await user.click(screen.getByLabelText('With more different accesses'));
        await user.click(screen.getByLabelText('Complete'));
        await enterWeight(field('Access Weight (%)'), '40');
        await enterWeight(field('Write Weight (%)'), '10');

        expect(formRows()).toEqual([
            'Profile: ▾Big Functionalities',
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

        await waitFor(() => expect(backend.sent()).toContain(CREATE));
        expect(backend.bodyOf(CREATE)).toEqual(expect.objectContaining({
            profileId: 'p2',
            linkageType: 'complete',
            tracesMaxLimit: 5,
            traceType: 'WITH_MORE_DIFFERENT_ACCESSES',
        }));
        expect(backend.bodyOf(CREATE).weightsList[0]).toEqual(expect.objectContaining({
            accessMetricWeight: 40, writeMetricWeight: 10, readMetricWeight: 25, sequenceMetricWeight: 25,
        }));
    });

    it('posts the Longest type of traces and single linkage', async () => {
        const { user } = await openKind('Accesses');

        await user.click(screen.getByLabelText('Longest'));
        await user.click(screen.getByLabelText('Single'));
        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toContain(CREATE));
        expect(backend.bodyOf(CREATE)).toEqual(expect.objectContaining({ linkageType: 'single', traceType: 'LONGEST' }));
    });

    it('enables Generate only while the weights sum to 100', async () => {
        await openKind('Accesses');
        const generate = screen.getByRole('button', { name: GENERATE });
        expect(generate).toBeEnabled();

        await enterWeight(field('Access Weight (%)'), '30');
        expect(generate).toBeDisabled();

        await enterWeight(field('Write Weight (%)'), '20');
        expect(generate).toBeEnabled();
    });

    it('shows a cleared weight as 0, not empty', async () => {
        // U6, moved by ticket 23 into SimilarityForm: `event.target.value === 0 ? -1 : ...`
        // compares a string with a number, so clearing never yields the -1 that renders as empty.
        await openKind('Accesses');

        await enterWeight(field('Access Weight (%)'), '');

        expect(field('Access Weight (%)')).toHaveValue(0);
        expect(screen.getByRole('button', { name: GENERATE })).toBeDisabled();
    });

    // Ticket 08: the card names the profile, or "No profile" (upstream: the profile's name).
    it('shows each Similarity as a card with profile, traces, trace type, linkage and weights', async () => {
        backend.on(SIMILARITIES_URL, [
            similarity('SIMILARITY_SCIPY_ACCESSES_REPOSITORY', 'sim1', {
                profileId: null, profileName: null, linkageType: 'average', tracesMaxLimit: 0, traceType: 'ALL',
                weightsList: [ACCESSES_WEIGHTS(25, 25, 25, 25)],
            }),
            similarity('SIMILARITY_SCIPY_ACCESSES_REPOSITORY', 'sim2', {
                profileId: 'p1', profileName: 'Login', linkageType: 'average', tracesMaxLimit: 0, traceType: 'ALL',
                weightsList: [ACCESSES_WEIGHTS(25, 25, 25, 25)],
            }),
        ]);
        renderAt(AT);

        await screen.findByText('sim1');
        const card = cardOf('sim1');
        expect(cardLines(card)).toEqual([
            'Profile: No profile',
            'AmountOfTraces: 0',
            'Type of traces: ALL',
            'Linkage Type: average',
            'Access Weight: 25%',
            'Write Weight: 25%',
            'Read Weight: 25%',
            'Sequence Weight: 25%',
        ]);
        expect(cardLines(cardOf('sim2'))[0]).toBe('Profile: Login');
        expectCardFrame(card, 'sim1');
    });
});

describe('the Similarity form for the Repository kind', () => {
    beforeEach(() => {
        backend = fakeBackend(routes('Repository'));
    });

    // U13, moved by ticket 09 to the backend: the Repository kind is still scoped by the
    // Accesses representation (RepositoryKind.scopedRepresentationType), but the form
    // only asks, and ProfileSelect catches a failure.
    it('shows the same rows as for Accesses, with an author and a commit weight of 50', async () => {
        await openKind('Repository');

        expect(formRows()).toEqual([
            'Profile: ▾No profile (all units)',
            'Amount of Traces per Functionality: [|0 by default]',
            'Type of traces: (x) All ( ) Longest ( ) With more different accesses',
            'Linkage Type: (x) Average ( ) Single ( ) Complete',
            'Author Weight (%): [50|0-100]',
            'Commit Weight (%): [50|0-100]',
            '<Generate Similarity Distances>',
        ]);
    });

    it('posts the repository weights', async () => {
        const { user } = await openKind('Repository');

        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toContain(CREATE));
        expect(backend.bodyOf(CREATE)).toEqual({
            kindName: 'Repository',
            type: 'SIMILARITY_SCIPY_ACCESSES_REPOSITORY',
            weightsList: [{
                type: 'REPOSITORY_WEIGHTS', numberOfWeights: 2,
                weightsLabel: { authorMetricWeight: 'Author Weight', commitMetricWeight: 'Commit Weight' },
                authorMetricWeight: 50, commitMetricWeight: 50,
            }],
            profileId: null,
            linkageType: 'average',
            tracesMaxLimit: 0,
            traceType: 'ALL',
        });
    });
});

describe('the Similarity form for the Accesses and Repository kind', () => {
    beforeEach(() => {
        backend = fakeBackend(routes('Accesses and Repository'));
    });

    it('shows the six weights, the accesses ones at 17 and the repository ones at 16', async () => {
        await openKind('Accesses and Repository');

        expect(formRows()).toEqual([
            'Profile: ▾No profile (all units)',
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
        const { user } = await openKind('Accesses and Repository');

        await enterWeight(field('Commit Weight (%)'), '6');
        expect(screen.getByRole('button', { name: GENERATE })).toBeDisabled();
        await enterWeight(field('Author Weight (%)'), '26');
        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toContain(CREATE));
        expect(backend.bodyOf(CREATE).weightsList).toEqual([
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
            kindName: 'Accesses and Repository', profileId: 'p2', profileName: 'Big Functionalities',
            linkageType: 'complete', tracesMaxLimit: 10, traceType: 'LONGEST',
            weightsList: [ACCESSES_WEIGHTS(17, 17, 17, 17), REPOSITORY_WEIGHTS(16, 16)],
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

// Upstream's SimilarityMatrixSciPyStructureForm
describe('the Similarity form for the Structure kind', () => {
    beforeEach(() => {
        backend = fakeBackend(routes('Structure'));
    });

    it('loads the kind, and the profiles the backend offers for it', async () => {
        await openKind('Structure');
    });

    it('shows profile, type of traces, linkage type and three weights 33/33/34, with no amount of traces', async () => {
        await openKind('Structure');

        expect(formRows()).toEqual([
            'Profile: ▾No profile (all units)',
            'Type of traces: (x) All ( ) Longest ( ) With more different accesses',
            'Linkage Type: (x) Average ( ) Single ( ) Complete',
            'One to One Weight (%): [33|0-100]',
            'One to Many Weight (%): [33|0-100]',
            'Inheritance Weight (%): [34|0-100]',
            '<Generate Similarity Distances>',
        ]);
    });

    // U3, resolved by ticket 08.
    it('lists no profile, then the offered profiles, marking only the chosen one', async () => {
        const { user } = await openKind('Structure');

        await user.click(screen.getByRole('button', { name: 'No profile (all units)' }));

        const items = screen.getAllByRole('button').filter(b => b.closest('.dropdown-menu'));
        expect(items.map(i => i.textContent)).toEqual(['No profile (all units)', 'Login', 'Big Functionalities']);
        expect(items.map(i => i.classList.contains('active'))).toEqual([true, false, false]);
    });

    it('posts the defaults: no profile, average linkage, ALL traces and the three weights', async () => {
        const { user } = await openKind('Structure');

        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toEqual([...ON_OPENING, ...choosing('Structure'), ...RELOADED]));
        expect(backend.bodyOf(CREATE)).toEqual({
            kindName: 'Structure',
            type: 'SIMILARITY_SCIPY_STRUCTURE',
            weightsList: [{
                type: 'STRUCTURE_WEIGHTS', numberOfWeights: 3,
                weightsLabel: { oneToOneWeight: 'One to One Weight', oneToManyWeight: 'One to Many Weight', heritageWeight: 'Inheritance Weight' },
                oneToOneWeight: 33, oneToManyWeight: 33, heritageWeight: 34,
            }],
            profileId: null,
            linkageType: 'average',
            traceType: 'ALL',
        });
    });

    it('posts the chosen profile, type of traces, linkage and weights', async () => {
        const { user } = await openKind('Structure');

        await user.click(screen.getByRole('button', { name: 'No profile (all units)' }));
        await user.click(screen.getByRole('button', { name: 'Big Functionalities' }));
        await user.click(screen.getByLabelText('Longest'));
        await user.click(screen.getByLabelText('Single'));
        await enterWeight(field('One to One Weight (%)'), '50');
        expect(screen.getByRole('button', { name: GENERATE })).toBeDisabled();
        await enterWeight(field('Inheritance Weight (%)'), '17');
        expect(screen.getByRole('button', { name: GENERATE })).toBeEnabled();
        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toContain(CREATE));
        const body = backend.bodyOf(CREATE);
        expect(body).toEqual(expect.objectContaining({ profileId: 'p2', linkageType: 'single', traceType: 'LONGEST' }));
        expect(body.weightsList[0]).toEqual(expect.objectContaining({ oneToOneWeight: 50, oneToManyWeight: 33, heritageWeight: 17 }));
    });

    it('shows each Similarity as a card with only its weights', async () => {
        // U14, still present (models/similarity/SimilarityScipyStructure.tsx): the card shows
        // neither the profile, the linkage type nor the type of traces it was made with.
        backend.on(SIMILARITIES_URL, [similarity('SIMILARITY_SCIPY_STRUCTURE', 'sim1', {
            kindName: 'Structure', profileId: 'p1', profileName: 'Login', linkageType: 'average', traceType: 'ALL',
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

// Upstream's SimilarityMatrixSciPyClassVectorizationForm and
// SimilarityMatrixSciPyEntityVectorizationForm: the same form for two kinds.
describe.each([
    ['Class Vectorization', 'SIMILARITY_SCIPY_CLASS_VECTORIZATION', 'complete'],
    ['Entity Vectorization', 'SIMILARITY_SCIPY_ENTITY_VECTORIZATION', 'single'],
])('the Similarity form for the %s kind', (kind, type, otherLinkage) => {
    beforeEach(() => {
        backend = fakeBackend(routes(kind));
    });

    // Ticket 09: the backend offers no profile for the kind, so the selector does not
    // show, and nothing else loads but the kind itself (ticket 23).
    it('shows only the linkage type, and Generate enabled', async () => {
        await openKind(kind);

        expect(formRows()).toEqual([
            'Linkage Type: (x) Average ( ) Single ( ) Complete',
            '<Generate Similarity Distances>',
        ]);
    });

    // Ticket 08: profileId is sent, null, even for a kind that takes no profile.
    it('posts the kind, type and default average linkage', async () => {
        const { user } = await openKind(kind);

        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toEqual([...ON_OPENING, ...choosing(kind), ...RELOADED]));
        expect(backend.bodyOf(CREATE)).toEqual({ kindName: kind, type, profileId: null, linkageType: 'average' });
    });

    it('posts the chosen linkage', async () => {
        const { user } = await openKind(kind);

        await user.click(screen.getByLabelText(otherLinkage[0].toUpperCase() + otherLinkage.slice(1)));
        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toContain(CREATE));
        expect(backend.bodyOf(CREATE)).toEqual({ kindName: kind, type, profileId: null, linkageType: otherLinkage });
    });

    it('shows each Similarity as a card with its linkage type', async () => {
        backend.on(SIMILARITIES_URL, [similarity(type, 'sim1', { kindName: kind, linkageType: otherLinkage })]);
        renderAt(AT);

        await screen.findByText('sim1');
        const card = cardOf('sim1');
        expect(cardLines(card)).toEqual([`Linkage Type: ${otherLinkage}`]);
        expectCardFrame(card, 'sim1');
    });
});

// Upstream's SimilarityMatrixSciPyFunctionalityVectorizationCallGraphForm
describe('the Similarity form for the Functionality Vectorization Call Graph kind', () => {
    const KIND = 'Functionality Vectorization Call Graph';
    beforeEach(() => {
        backend = fakeBackend(routes(KIND));
    });

    it('shows linkage type, an empty max depth and four weights of 25', async () => {
        await openKind(KIND);

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
        const { user } = await openKind(KIND);
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

    it('posts the kind, type, linkage, depth and the four weights', async () => {
        const { user } = await openKind(KIND);

        await user.type(screen.getByPlaceholderText('1-N'), '3');
        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toEqual([...ON_OPENING, ...choosing(KIND), ...RELOADED]));
        expect(backend.bodyOf(CREATE)).toEqual({
            kindName: KIND,
            type: 'SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_CALLGRAPH',
            profileId: null,
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
        const { user } = await openKind(KIND);

        await user.type(screen.getByPlaceholderText('1-N'), '2');
        await user.click(screen.getByLabelText('Single'));
        await enterWeight(field('Controllers Weight (%)'), '40');
        await enterWeight(field('Entities Weight (%)'), '10');
        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toContain(CREATE));
        const body = backend.bodyOf(CREATE);
        expect(body).toEqual(expect.objectContaining({ linkageType: 'single', depth: 2 }));
        expect(body.weightsList[0]).toEqual(expect.objectContaining({
            controllersWeight: 40, servicesWeight: 25, intermediateMethodsWeight: 25, entitiesWeight: 10,
        }));
    });

    it('shows each Similarity as a card with linkage type, depth and weights', async () => {
        backend.on(SIMILARITIES_URL, [similarity('SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_CALLGRAPH', 'sim1', {
            kindName: KIND, linkageType: 'average', depth: 3,
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

// Upstream's SimilarityMatrixScipyFunctionalityVectorizationSequenceOfAccessesForm
describe('the Similarity form for the Functionality Vectorization Sequence Accesses kind', () => {
    const KIND = 'Functionality Vectorization Sequence Accesses';
    beforeEach(() => {
        backend = fakeBackend(routes(KIND));
    });

    it('shows linkage type and a read and a write weight of 50', async () => {
        await openKind(KIND);

        expect(formRows()).toEqual([
            'Linkage Type: (x) Average ( ) Single ( ) Complete',
            'Read Weight (%): [50|0-100]',
            'Write Weight (%): [50|0-100]',
            '<Generate Similarity Distances>',
        ]);
    });

    it('enables Generate only while the weights sum to 100', async () => {
        await openKind(KIND);
        const generate = screen.getByRole('button', { name: GENERATE });

        await enterWeight(field('Read Weight (%)'), '70');
        expect(generate).toBeDisabled();

        await enterWeight(field('Write Weight (%)'), '30');
        expect(generate).toBeEnabled();
    });

    it('posts the kind, type, linkage and the two weights', async () => {
        const { user } = await openKind(KIND);

        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toEqual([...ON_OPENING, ...choosing(KIND), ...RELOADED]));
        expect(backend.bodyOf(CREATE)).toEqual({
            kindName: KIND,
            type: 'SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES',
            profileId: null,
            linkageType: 'average',
            weightsList: [{
                type: 'FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS', numberOfWeights: 2,
                weightsLabel: { readMetricWeight: 'Read Weight', writeMetricWeight: 'Write Weight' },
                readMetricWeight: 50, writeMetricWeight: 50,
            }],
        });
    });

    it('posts the chosen linkage and weights', async () => {
        const { user } = await openKind(KIND);

        await user.click(screen.getByLabelText('Complete'));
        await enterWeight(field('Read Weight (%)'), '80');
        await enterWeight(field('Write Weight (%)'), '20');
        await user.click(screen.getByRole('button', { name: GENERATE }));

        await waitFor(() => expect(backend.sent()).toContain(CREATE));
        const body = backend.bodyOf(CREATE);
        expect(body.linkageType).toBe('complete');
        expect(body.weightsList[0]).toEqual(expect.objectContaining({ readMetricWeight: 80, writeMetricWeight: 20 }));
    });

    it('shows each Similarity as a card with linkage type and weights', async () => {
        backend.on(SIMILARITIES_URL, [similarity('SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES', 'sim1', {
            kindName: KIND, linkageType: 'single',
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
