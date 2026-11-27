import '@testing-library/jest-dom';
import { act, cleanup } from '@testing-library/react';

// react-toastify defers its bookkeeping with timers: let pending toast
// updates land before unmounting, then let the unmounted container
// deregister, or its toasts and timers leak into the next test.
afterEach(async () => {
    await act(() => new Promise(resolve => setTimeout(resolve, 20)));
    cleanup();
    await new Promise(resolve => setTimeout(resolve, 0));
});
