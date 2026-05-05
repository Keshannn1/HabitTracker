# Room DAO SOP

## Objective
Establish atomicity for 1-to-N relationships and define sequential task fetching using the `orderIndex`.

## The `RoutineWithTasks` POJO
To retrieve a `Routine` and its associated `Task`s simultaneously, we use a Room `@Relation` within a plain class (`RoutineWithTasks`). 
We apply the `@Transaction` annotation to DAO methods returning this class. This guarantees atomic reads, avoiding data inconsistency if the database is modified mid-query.

## Sequential Execution using `orderIndex`
Tasks within a routine must execute sequentially. We maintain this sequence via the `orderIndex` integer property.

### How it works:
1. When Task N starts, its status transitions to `ACTIVE`.
2. Upon completion, Task N is marked `COMPLETED`.
3. To determine the next task, the DAO queries the `tasks` table for the given `routineId` filtering for a `PENDING` status.
4. The key is to sort by `orderIndex ASC` and enforce a `LIMIT 1`.

### SQL Implementation
```sql
SELECT * FROM tasks 
WHERE routineId = :routineId AND status = 'PENDING' 
ORDER BY orderIndex ASC 
LIMIT 1
```

**Advantage**: This approach pushes the sequence logic directly into SQLite, making it highly robust. If a user closes the app, the sequence is preserved, and the next query organically yields the correct next step.
