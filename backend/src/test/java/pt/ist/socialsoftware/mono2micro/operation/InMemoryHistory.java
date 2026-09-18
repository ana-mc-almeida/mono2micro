package pt.ist.socialsoftware.mono2micro.operation;

import pt.ist.socialsoftware.mono2micro.history.domain.History;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * A {@link History} that keeps its operations in memory and touches nothing else.
 *
 * <p>Needed because {@code Operation.execute} calls
 * {@link History#removeOverriddenOperations(Long)}, and the only concrete {@code History} in the
 * production code is {@code PositionHistory}, whose implementation of that method opens with
 * {@code ContextManager.get().getBean(GridFsService.class)}. {@code ContextManager.get()}
 * lazily constructs an {@code AnnotationConfigApplicationContext} that scans the whole
 * {@code pt.ist.socialsoftware.mono2micro} package and <b>caches it in a static field</b>, so a
 * single unit test that reached it would either fail on missing Mongo configuration or boot a
 * full context and leak it into every later test in the same JVM fork. Using the real
 * {@code PositionHistory} would make this suite the very thing it exists to complement.
 *
 * <p><b>What this stands in for, and what it therefore does not test.</b>
 * {@code PositionHistory.removeOverriddenOperations} does two unrelated things: it filters the
 * operation list, and it deletes the GridFS blobs holding graph positions for the discarded
 * depths. Only the first half is replicated here. So tests built on this fake pin
 * <em>{@code Operation.execute}'s contract</em> — increment the depth, stamp it on the
 * operation, ask the history to drop overridden operations, append — and say nothing about
 * whether {@code PositionHistory} cleans up after itself. That belongs to an integration test.
 *
 * <p>That the two concerns cannot be separated without a fake is itself the finding: a pure list
 * operation is welded to blob garbage collection, so neither can be exercised alone.
 *
 * <p>The filter predicate below is transcribed from {@code PositionHistory.java:54-56} rather
 * than shared with it, following the convention {@code StrategyCase} uses for type strings: a
 * divergence should surface here as a red test rather than a silent recompile. It will not
 * surface on its own, though — see the class javadoc of {@link OperationHistoryBookkeepingTest}.
 */
class InMemoryHistory extends History {

    static final String IN_MEMORY_HISTORY = "IN_MEMORY_HISTORY";

    /**
     * Initializes both fields, which {@code History} leaves null.
     *
     * <p>Neither has an inline initializer and only {@code PositionHistory(Decomposition)} sets
     * them, so a bare subclass would NPE twice over: in {@code addOperation} on the null list,
     * and in {@code incrementCurrentHistoryDepth}, where {@code ++} unboxes a null {@code Long}.
     */
    InMemoryHistory() {
        setName("in-memory-history");
        setHistoryOperationsList(new ArrayList<>());
        setCurrentHistoryOperationDepth(0L);
    }

    @Override
    public String getType() {
        return IN_MEMORY_HISTORY;
    }

    /** No persisted properties to delete, unlike {@code PositionHistory}'s GridFS blobs. */
    @Override
    public void deleteProperties() {
    }

    /** The list-filtering half of {@code PositionHistory.removeOverriddenOperations}. */
    @Override
    public void removeOverriddenOperations(Long newHistoryOperationDepth) {
        setHistoryOperationsList(getHistoryOperationList().stream()
                .filter(operation -> operation.getHistoryDepth() < newHistoryOperationDepth)
                .collect(Collectors.toList()));
    }
}
