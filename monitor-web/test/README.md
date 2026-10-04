# Frontend event optimization tests

Run `npm test` from `monitor-web`, using Node's built-in test runner. No extra packages are needed.

The fake clock advances deterministically without real sleeps. Tests cover trailing debounce, leading/trailing throttle, cancellation on unmount, argument/receiver preservation, delay validation, and case-insensitive service filtering.

In the page, enter a service name/key/group/URL in the service directory search. Results update after a 300ms quiet period. Scroll down beyond 500px to reveal the back-to-top button; scroll position updates are throttled to 150ms and include the latest trailing position. Component unmount cancels pending callbacks and removes its listener. A reduced-motion preference disables smooth scrolling.

The helpers are unit-tested; these tests do not claim a full browser end-to-end test. Search filters the already loaded catalog and does not call a backend search endpoint.

Manual browser acceptance on 2026-10-04 verified one matching result out of two temporary catalog fixtures, the settled search indicator, the back-to-top action, and no observed console errors. Fixtures were removed after checking their IDs/keys. The local preview remains at http://127.0.0.1:5200/; development API proxy targets the local default backend on port 8080, whereas the Docker gateway on 8088 uses its Docker-profile backend.
