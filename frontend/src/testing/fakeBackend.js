import axios from 'axios';

// A fake of the HTTP layer, installed as axios' default adapter. Every
// `axios.create()` instance made after `fakeBackend()` (APIService makes one per
// screen action) inherits it, so the screens run unchanged and every request
// they send is recorded here, readable as `METHOD /path?query` plus a body.
//
// Routes are keyed "METHOD /path", the path relative to the backend's base URL
// (`/mono2micro`) and without its query string, URL-encoded as sent (a name
// "ldod & Accesses" is `ldod%20&%20Accesses`; query values are form-encoded,
// `profile=Reader+Only`). A value is the response body, or
// a function of the recorded request returning one; `reply(status, body)` sets
// a status. A request no route answers is recorded in `unhandled` and fails
// with 404, so a renamed endpoint shows up instead of hanging the screen.

const BACKEND = 'http://localhost:8080/mono2micro';

export function reply(status, data) {
    return { __reply: true, status, data };
}

// A route whose response is held back until `release()`, so a test decides
// when, and so in which order, requests answer.
export function held(value) {
    let release;
    const released = new Promise(resolve => { release = resolve; });
    return { route: () => released.then(() => value), release };
}

export function fakeBackend(routes = {}) {
    const backend = {
        routes: { ...routes },
        requests: [],
        unhandled: [],

        on(key, response) {
            this.routes[key] = response;
            return this;
        },

        // The requests sent, as "METHOD /path?query" lines, in order.
        sent() {
            return this.requests.map(r => r.request);
        },

        // The body of the last request matching "METHOD /path?query".
        bodyOf(request) {
            const found = this.requests.filter(r => r.request === request);
            if (found.length === 0)
                throw new Error(`No request "${request}". Sent:\n${this.sent().join('\n')}`);
            return found[found.length - 1].body;
        },
    };

    axios.defaults.adapter = async config => {
        const { path, query } = splitUrl(config);
        const method = config.method.toUpperCase();
        const request = method + ' ' + path + (query ? '?' + query : '');
        const recorded = { request, method, path, query, body: readBody(config.data) };
        backend.requests.push(recorded);

        const route = backend.routes[method + ' ' + path];
        if (route === undefined) {
            backend.unhandled.push(request);
            return reject(config, 404, { message: `fakeBackend: no route for ${request}` });
        }
        let result = typeof route === 'function' ? await route(recorded) : route;
        if (result && result.__reply) {
            if (result.status >= 400) return reject(config, result.status, result.data);
            return respond(config, result.status, result.data);
        }
        return respond(config, method === 'POST' ? 201 : 200, result);
    };

    return backend;
}

function splitUrl(config) {
    const base = (config.baseURL || '').replace(/\/+$/, '');
    const relative = (config.url || '').replace(/^\/+/, '');
    let full = base + '/' + relative;
    const url = new URL(full);
    const params = new URLSearchParams(url.search);
    Object.entries(config.params || {}).forEach(([k, v]) => params.append(k, v));
    let path = url.origin + url.pathname;
    path = path.startsWith(BACKEND) ? path.substring(BACKEND.length) : path;
    return { path, query: params.toString() };
}

function readBody(data) {
    if (data === undefined || data === null) return undefined;
    if (typeof FormData !== 'undefined' && data instanceof FormData) {
        const body = {};
        for (const [key, value] of data.entries()) {
            const shown = typeof File !== 'undefined' && value instanceof File ? `File(${value.name})` : value;
            body[key] = key in body ? [].concat(body[key], shown) : shown;
        }
        return body;
    }
    if (typeof data === 'string') {
        try { return JSON.parse(data); } catch (e) { return data; }
    }
    return data;
}

function respond(config, status, data) {
    return { data: data === undefined ? '' : data, status, statusText: String(status), headers: {}, config, request: {} };
}

function reject(config, status, data) {
    const error = new Error(`Request failed with status code ${status}`);
    error.config = config;
    error.response = respond(config, status, data);
    error.isAxiosError = true;
    return Promise.reject(error);
}
