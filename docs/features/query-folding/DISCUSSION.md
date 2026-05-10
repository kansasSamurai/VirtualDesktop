# Query Folding — Feature Discussion

**Date:** 2026-05-10  
**Status:** Early concept — captured for future design work.  
**Related:** SmartGrid roadmap — "Structured Filter Expressions" (Phase 14)

---

## Context

Database query results will be a primary datasource for SmartGrid in the
VirtualDesktop workflow. A key goal is to allow one dataset to become the basis
for the next query — i.e. the result set of query 1 becomes the "table" for
query 2. Queries should be saveable individually and the app should be able to
derive a single consolidated query representing the entire chain.

---

## The Core Concept: Query Folding

Tools like Power Query (Excel/PowerBI), LINQ-to-SQL, and Hibernate's Criteria API
all do this — you compose a chain of transformations (filter, sort, join, project)
in application code, and the system derives a single optimal query from the chain
rather than executing each step independently. The "consolidated single query" is
the SQL equivalent of that folded result.

## The SmartGrid Implication That Matters Most

The current `GridModelFilter` is a Java lambda — it's opaque. You can evaluate it
but you can't inspect it, serialize it, or translate it to SQL.
`row -> row.get("salary").toString().contains("70")` is a black box.

For query folding to work, filters need to be **structured predicates** alongside
the lambda:

```java
// Instead of (or in addition to) a lambda:
new FilterExpression("salary", Operator.GREATER_THAN, 70000)
```

A `FilterExpression` can be both evaluated in Java (for client-side display
filtering) AND rendered to SQL (`WHERE salary > 70000`). The lambda remains for
fast local filtering; the structured form enables translation. These coexist — the
structured form generates the lambda, essentially.

Sort state is already structured (`currentSortKey` + `SortOrder`) and trivially
translates to `ORDER BY salary DESC`.

## The "Dataset as Next Query's Table" Concept

In SQL terms, each result set becomes a derived table or CTE:

```sql
WITH level1 AS (
    SELECT * FROM employees WHERE dept = 'Engineering'
),
level2 AS (
    SELECT * FROM level1 WHERE salary > 70000
)
SELECT * FROM level2 JOIN projects ON ...
```

The consolidated query is the accumulated CTE chain. Each "grid" in the workflow
corresponds to one CTE level. This is standard SQL — any database supports it, and
the result can be pasted into any query tool.

## What `ColumnDef` Needs for This

Currently `ColumnDef` carries display metadata only. For database-backed grids it
also needs:
- The source table/alias (for join disambiguation)
- The actual SQL column name (may differ from the display header)
- The SQL data type (for predicate generation — string vs. number vs. date affects
  operator choices)
- Whether it's a key column (needed to generate the `sourceObject` identity
  reference)

This suggests `SqlColumnDef extends ColumnDef` rather than polluting the base
class — keep SmartGrid generic, let the database layer extend it.

## On Saving Queries vs. Data

Saving the query chain rather than the results is the correct approach — it's
smaller, it stays current, and it's composable. The persistence format is
essentially a sequence of `QueryStep` objects:

```
{ source: "employees", filters: [...], sorts: [...], selectedKeyValues: [...] }
→ { joinTable: "projects", on: "emp_id", filters: [...] }
→ ...
```

The `selectedKeyValues` at each step is what propagates the selection from grid N
to the WHERE clause of grid N+1's query — and this is exactly why `sourceObject`
(or at minimum, a `getKeyValues()` method on `GridRow`) is necessary. Without row
identity you can't express "WHERE emp_id IN (the rows I selected in grid 1)."

## The Most Important Gap This Reveals in SmartGrid

**Structured filter expressions.** Everything else (column metadata, `sourceObject`,
async loading) is additive. But if filters stay as opaque lambdas, query folding is
impossible and the consolidated query can never be derived.

The lambda approach is fine for interactive use, but a structured `FilterExpression`
model sitting alongside it would be the hook that enables the whole workflow. Worth
keeping in mind as the filter implementation evolves — the two forms coexist: the
structured form generates the lambda, and the structured form is what gets
serialized and translated to SQL.

---

## Open Questions for Future Design

1. What is the persistence format for a `QueryStep` chain? JSON? A custom DSL?
2. How does the "consolidated query" handle aggregation (GROUP BY) across levels?
3. Should `FilterExpression` support compound predicates (AND / OR trees)?
4. How does column aliasing propagate through CTE levels?
5. What is the UI for "use this selection as next query's input"? A toolbar button?
   A right-click context menu? A drag-to-new-grid gesture?
6. Should the grid expose a `getQueryState()` method returning the structured
   filter/sort state, or should that live in a wrapper layer above SmartGrid?
