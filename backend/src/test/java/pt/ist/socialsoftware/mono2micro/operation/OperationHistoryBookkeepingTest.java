package pt.ist.socialsoftware.mono2micro.operation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.history.domain.History;
import pt.ist.socialsoftware.mono2micro.operation.rename.RenamePartitionsOperation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code Operation.execute}'s history bookkeeping, independent of any one operation.
 *
 * <p>Every {@code *PartitionsOperation.execute} ends in {@code super.execute}, which does four
 * things ({@code Operation.java:26-36}): increment the history's depth cursor, stamp that depth
 * on the operation, ask the history to drop operations the new one overrides, and append. Rename
 * is the vehicle here because its {@code execute} adds nothing of its own — merge's, by
 * contrast, also captures pre-merge state.
 *
 * <p>Depth is <b>1-based</b>: {@code getCurrentHistoryOperation} indexes the list at
 * {@code depth - 1} ({@code History.java:66}), and an empty history sits at depth 0. The
 * invariant across undo and redo is {@code currentHistoryOperationDepth <= list.size()}, with
 * undone operations left in the list above the cursor until a new operation prunes them.
 *
 * <p><b>What this does not cover.</b> The history here is an {@link InMemoryHistory}, not the
 * production {@code PositionHistory}, whose {@code removeOverriddenOperations} also deletes the
 * GridFS blobs holding graph positions for the discarded depths. The filter predicate is
 * transcribed rather than shared, so a divergence in {@code PositionHistory} will <em>not</em>
 * turn these tests red — that is the cost of the stand-in, and the reason it is needed is
 * recorded on {@link InMemoryHistory}. Undo and redo are driven directly here rather than
 * through {@code HistoryService}, which adds only two repository saves.
 */
@DisplayName("Operation history bookkeeping")
class OperationHistoryBookkeepingTest {

    @Test
    @DisplayName("stamps an incrementing depth on each executed operation")
    void stampsIncrementingDepths() {
        PartitionsDecomposition decomposition = threeClusters();

        RenamePartitionsOperation first = new RenamePartitionsOperation("C1", "A");
        RenamePartitionsOperation second = new RenamePartitionsOperation("C2", "B");
        RenamePartitionsOperation third = new RenamePartitionsOperation("C3", "C");
        first.execute(decomposition);
        second.execute(decomposition);
        third.execute(decomposition);

        assertThat(first.getHistoryDepth()).isEqualTo(1L);
        assertThat(second.getHistoryDepth()).isEqualTo(2L);
        assertThat(third.getHistoryDepth()).isEqualTo(3L);

        History history = decomposition.getHistory();
        assertThat(history.getCurrentHistoryOperationDepth()).isEqualTo(3L);
        assertThat(history.getMaxHistoryDepth()).isEqualTo(3);
        assertThat(history.getHistoryOperationList()).containsExactly(first, second, third);
    }

    @Test
    @DisplayName("resolves the current operation and one at a given depth, 1-based")
    void resolvesOperationsByDepth() {
        PartitionsDecomposition decomposition = threeClusters();

        RenamePartitionsOperation first = new RenamePartitionsOperation("C1", "A");
        RenamePartitionsOperation second = new RenamePartitionsOperation("C2", "B");
        first.execute(decomposition);
        second.execute(decomposition);

        History history = decomposition.getHistory();
        assertThat(history.getCurrentHistoryOperation()).isSameAs(second);
        assertThat(history.getHistoryOperationByDepth(1L)).isSameAs(first);
        assertThat(history.getHistoryOperationByDepth(2L)).isSameAs(second);
    }

    /**
     * The branch-after-undo case: two undos rewind the cursor, then a new operation discards the
     * operations it overrides.
     *
     * <p>The two {@code decrementCurrentHistoryDepth} calls stand in for
     * {@code HistoryService.undoOperation}, which decrements after delegating to
     * {@code operation.undo} ({@code HistoryService.java:38-40}).
     */
    @Test
    @DisplayName("drops the operations a new one overrides")
    void dropsOverriddenOperations() {
        PartitionsDecomposition decomposition = threeClusters();

        RenamePartitionsOperation first = new RenamePartitionsOperation("C1", "A");
        RenamePartitionsOperation second = new RenamePartitionsOperation("C2", "B");
        RenamePartitionsOperation third = new RenamePartitionsOperation("C3", "C");
        first.execute(decomposition);
        second.execute(decomposition);
        third.execute(decomposition);

        History history = decomposition.getHistory();
        third.undo(decomposition);
        history.decrementCurrentHistoryDepth();
        second.undo(decomposition);
        history.decrementCurrentHistoryDepth();
        assertThat(history.getCurrentHistoryOperationDepth()).isEqualTo(1L);

        // The two undone operations are still in the list, above the cursor.
        assertThat(history.getHistoryOperationList()).containsExactly(first, second, third);

        RenamePartitionsOperation branch = new RenamePartitionsOperation("C2", "Branch");
        branch.execute(decomposition);

        assertThat(branch.getHistoryDepth()).isEqualTo(2L);
        assertThat(history.getHistoryOperationList()).containsExactly(first, branch);
        assertThat(history.getMaxHistoryDepth()).isEqualTo(2);
    }

    @Test
    @DisplayName("redo re-applies the operation without touching history")
    void redoDoesNotRestampOrAppend() {
        PartitionsDecomposition decomposition = threeClusters();

        RenamePartitionsOperation operation = new RenamePartitionsOperation("C1", "A");
        operation.execute(decomposition);
        operation.undo(decomposition);

        History history = decomposition.getHistory();
        int sizeBeforeRedo = history.getHistoryOperationList().size();

        // Operation.redo delegates to executeOperation, never to execute — so the depth stamp
        // and the list are HistoryService's business, not the operation's.
        operation.redo(decomposition);

        assertThat(operation.getHistoryDepth()).isEqualTo(1L);
        assertThat(history.getHistoryOperationList()).hasSize(sizeBeforeRedo);
        assertThat(decomposition.getClusters()).containsKey("A");
    }

    /**
     * Documents the seam the rest of this suite relies on: {@code execute} needs a wired history
     * and {@code executeOperation} does not, which is what lets the per-operation tests run
     * without any {@code History} at all.
     */
    @Test
    @DisplayName("execute requires a history; executeOperation does not")
    void executeRequiresHistory() {
        PartitionsDecomposition decomposition = DecompositionFixture.with()
                .cluster("C1", 1)
                .noHistory()
                .build();

        assertThatThrownBy(() -> new RenamePartitionsOperation("C1", "A").execute(decomposition))
                .isInstanceOf(NullPointerException.class);

        // The mutation still happened: execute applies the operation, then records it.
        assertThat(decomposition.getClusters()).containsOnlyKeys("A");

        new RenamePartitionsOperation("A", "B").executeOperation(decomposition);
        assertThat(decomposition.getClusters()).containsOnlyKeys("B");
    }

    private static PartitionsDecomposition threeClusters() {
        return DecompositionFixture.with()
                .cluster("C1", 1)
                .cluster("C2", 2)
                .cluster("C3", 3)
                .build();
    }
}
