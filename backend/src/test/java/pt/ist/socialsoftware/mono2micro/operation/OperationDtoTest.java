package pt.ist.socialsoftware.mono2micro.operation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.operation.formCluster.FormClusterOperation;
import pt.ist.socialsoftware.mono2micro.operation.formCluster.FormClusterPartitionsOperation;
import pt.ist.socialsoftware.mono2micro.operation.merge.MergeOperation;
import pt.ist.socialsoftware.mono2micro.operation.merge.MergePartitionsOperation;
import pt.ist.socialsoftware.mono2micro.operation.rename.RenameOperation;
import pt.ist.socialsoftware.mono2micro.operation.rename.RenamePartitionsOperation;
import pt.ist.socialsoftware.mono2micro.operation.split.SplitOperation;
import pt.ist.socialsoftware.mono2micro.operation.split.SplitPartitionsOperation;
import pt.ist.socialsoftware.mono2micro.operation.transfer.TransferOperation;
import pt.ist.socialsoftware.mono2micro.operation.transfer.TransferPartitionsOperation;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The request-shaped side of the operations: type strings, and the copy constructors that turn a
 * deserialized request into something executable.
 *
 * <p>Not setter padding. This is the production path: a controller binds JSON to the base
 * {@code *Operation}, and {@code PartitionsDecomposition} then wraps it in the matching
 * {@code *PartitionsOperation} before executing ({@code PartitionsDecomposition.java:74-101}).
 * If a copy constructor drops a field, the request silently loses part of its meaning — and
 * {@code MergeOperation}'s deliberately drops two, which is why {@code storeState} has to run
 * before the merge rather than being carried over.
 *
 * <p>The type strings are the discriminators Jackson writes into the persisted history
 * ({@code Operation.java:13-20}). They are transcribed as literals here rather than referenced,
 * so that renaming a constant shows up as a red test rather than a silent recompile — the
 * convention {@code StrategyCase} uses for the same reason. Changing one is a migration, since
 * histories already in Mongo carry the old spelling.
 */
@DisplayName("Operation request objects")
class OperationDtoTest {

    @Test
    @DisplayName("type strings match the persisted discriminators")
    void typeStrings() {
        assertThat(new RenameOperation().getOperationType()).isEqualTo("RenameOperation");
        assertThat(new MergeOperation().getOperationType()).isEqualTo("MergeOperation");
        assertThat(new SplitOperation().getOperationType()).isEqualTo("SplitOperation");
        assertThat(new TransferOperation().getOperationType()).isEqualTo("TransferOperation");
        assertThat(new FormClusterOperation().getOperationType()).isEqualTo("FormClusterOperation");

        // The subclasses inherit the discriminator: history records what was requested, not
        // which decomposition flavour executed it.
        assertThat(new RenamePartitionsOperation().getOperationType()).isEqualTo("RenameOperation");
        assertThat(new MergePartitionsOperation().getOperationType()).isEqualTo("MergeOperation");
        assertThat(new SplitPartitionsOperation().getOperationType()).isEqualTo("SplitOperation");
        assertThat(new TransferPartitionsOperation().getOperationType()).isEqualTo("TransferOperation");
        assertThat(new FormClusterPartitionsOperation().getOperationType()).isEqualTo("FormClusterOperation");
    }

    @Test
    @DisplayName("rename copies both names and executes")
    void renameRoundTrip() {
        RenameOperation request = new RenameOperation();
        request.setClusterName("C1");
        request.setNewClusterName("Renamed");

        RenamePartitionsOperation operation = new RenamePartitionsOperation(request);

        assertThat(operation.getClusterName()).isEqualTo("C1");
        assertThat(operation.getNewClusterName()).isEqualTo("Renamed");

        PartitionsDecomposition decomposition = DecompositionFixture.with().cluster("C1", 1).build();
        operation.executeOperation(decomposition);
        assertThat(decomposition.getClusters()).containsOnlyKeys("Renamed");
    }

    @Test
    @DisplayName("split copies all three fields and executes")
    void splitRoundTrip() {
        SplitOperation request = new SplitOperation();
        request.setOriginalCluster("C1");
        request.setNewCluster("C2");
        request.setEntities("1");

        SplitPartitionsOperation operation = new SplitPartitionsOperation(request);

        assertThat(operation.getOriginalCluster()).isEqualTo("C1");
        assertThat(operation.getNewCluster()).isEqualTo("C2");
        assertThat(operation.getEntities()).isEqualTo("1");

        PartitionsDecomposition decomposition = DecompositionFixture.with().cluster("C1", 1, 2).build();
        operation.executeOperation(decomposition);
        assertThat(decomposition.getCluster("C2").getElementsIDs()).containsExactly((short) 1);
    }

    @Test
    @DisplayName("transfer copies all three fields and executes")
    void transferRoundTrip() {
        TransferOperation request = new TransferOperation();
        request.setFromCluster("C1");
        request.setToCluster("C2");
        request.setEntities("1");

        TransferPartitionsOperation operation = new TransferPartitionsOperation(request);

        assertThat(operation.getFromCluster()).isEqualTo("C1");
        assertThat(operation.getToCluster()).isEqualTo("C2");
        assertThat(operation.getEntities()).isEqualTo("1");

        PartitionsDecomposition decomposition = DecompositionFixture.with()
                .cluster("C1", 1)
                .cluster("C2", 2)
                .build();
        operation.executeOperation(decomposition);
        assertThat(decomposition.getCluster("C2").getElementsIDs())
                .containsExactlyInAnyOrder((short) 1, (short) 2);
    }

    @Test
    @DisplayName("form cluster copies the name and the entity map, and executes")
    void formClusterRoundTrip() {
        FormClusterOperation request = new FormClusterOperation();
        request.setNewCluster("NC");
        request.setEntities(Collections.singletonMap("C1", Arrays.asList((short) 1)));

        FormClusterPartitionsOperation operation = new FormClusterPartitionsOperation(request);

        assertThat(operation.getNewCluster()).isEqualTo("NC");
        assertThat(operation.getEntities()).containsOnlyKeys("C1");

        PartitionsDecomposition decomposition = DecompositionFixture.with().cluster("C1", 1, 2).build();
        operation.executeOperation(decomposition);
        assertThat(decomposition.getCluster("NC").getElementsIDs()).containsExactly((short) 1);
    }

    /**
     * Merge's copy constructor deliberately carries only the three names, leaving
     * {@code cluster1Entities} and {@code cluster2Entities} null ({@code MergeOperation.java:19-23}).
     * Those two are not request fields at all — they are undo state, captured by
     * {@code storeState} during {@code execute} against the decomposition as it stands. A copy
     * that carried them over would preserve whatever a previous merge had recorded.
     */
    @Test
    @DisplayName("merge copies the names but not the captured undo state")
    void mergeCopiesNamesOnly() {
        MergeOperation request = new MergeOperation();
        request.setCluster1Name("C1");
        request.setCluster2Name("C2");
        request.setNewName("M");
        request.setCluster1Entities("stale");
        request.setCluster2Entities("stale");

        MergePartitionsOperation operation = new MergePartitionsOperation(request);

        assertThat(operation.getCluster1Name()).isEqualTo("C1");
        assertThat(operation.getCluster2Name()).isEqualTo("C2");
        assertThat(operation.getNewName()).isEqualTo("M");
        assertThat(operation.getCluster1Entities()).isNull();
        assertThat(operation.getCluster2Entities()).isNull();
    }

    @Test
    @DisplayName("merge captures the pre-merge membership on execute, once")
    void mergeCapturesStateOnExecute() {
        PartitionsDecomposition decomposition = DecompositionFixture.with()
                .cluster("C1", 1, 2)
                .cluster("C2", 3)
                .build();

        MergePartitionsOperation operation = new MergePartitionsOperation("C1", "C2", "M");
        operation.execute(decomposition);

        // Captured as a comma-separated entity list, in whatever order the set yields.
        assertThat(operation.getCluster1Entities().split(",")).containsExactlyInAnyOrder("1", "2");
        assertThat(operation.getCluster2Entities()).isEqualTo("3");
    }
}
