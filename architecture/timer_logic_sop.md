# Timer Logic SOP
1. ViewModel holds the active Routine and current active Task index.
2. `StateFlow` exposes the current remaining seconds and task status.
3. Coroutine `delay(1000)` loop updates the timer.
4. When task completes, status updates, and timer moves to the next task.
5. On cleared, coroutine cancels (timer pauses).
