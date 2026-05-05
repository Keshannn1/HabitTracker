# Navigation SOP

## Objective
Define the Compose Navigation routes and state transitions to support the flow between routines, active execution, and analytics.

## NavHost Structure
- `NavHost` will manage the backstack.
- Route definitions:
  - `Dashboard`: Lists all user routines and available templates.
  - `Timer/{routineId}`: The active timer screen for a specific routine. The `routineId` is passed as a string argument.
  - `Editor/{routineId}`: Screen to modify routine name and reorder/add tasks. The `routineId` is passed as a string argument.
  - `Explore`: A marketplace/list of all templates to clone.
  - `Analytics`: Displays history and completion stats.

## Transitions
- Cloned Template -> Redirects to `Dashboard` (or directly to `Editor/{id}`).
- Start Routine -> Redirects to `Timer/{id}`.
- Complete/Exit Routine -> Redirects to `Dashboard`.
