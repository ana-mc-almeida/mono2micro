import { format } from 'util';

// Drops the console messages a suite knows to be noise, so whatever else the
// screens print still shows. `patterns` maps a console method to the regular
// expressions its noise matches, tested against the message as printed:
//   quietConsole({ log: [/^\[LOG\] - /] });
// Call it at the top of a test file or in a describe. It holds for the whole
// block, not per test: setupTests' afterEach, which flushes the toast timers
// whose warnings are the usual noise, runs after a describe's own afterEach.
export function quietConsole(patterns) {
    const spies = [];
    beforeAll(() => {
        Object.entries(patterns).forEach(([method, noise]) => {
            const original = console[method];
            spies.push(jest.spyOn(console, method).mockImplementation((...args) => {
                const message = format(...args);
                if (!noise.some(pattern => pattern.test(message))) original(...args);
            }));
        });
    });
    afterAll(() => {
        spies.splice(0).forEach(spy => spy.mockRestore());
    });
}
