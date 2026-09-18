package pt.ist.socialsoftware.mono2micro.operation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.ist.socialsoftware.mono2micro.cluster.Partition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.element.Element;
import pt.ist.socialsoftware.mono2micro.operation.transfer.TransferPartitionsOperation;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code TransferPartitionsOperation} — moving entities between two existing clusters.
 *
 * <p>The simplest operation by membership arithmetic, and the one with the sharpest
 * dependency-bookkeeping problem. {@code Partition.transferCouplingDependencies} suppresses a
 * dependency that would point at the partition itself ({@code Partition.java:47}), which is
 * right going forward — once the entity lives in the cluster that reached for it, there is no
 * distributed transaction left. But nothing records that the dependency <em>was</em> there, so
 * {@code undo} finds no key to move back and returns early. The reach is gone for good.
 * {@link Undo#undoDoesNotRestoreASuppressedSelfDependency()} pins it.
 *
 * <p>A second, smaller asymmetry: membership changes and dependency changes are driven from
 * different inputs. Entities are moved only if they are actually in the source cluster
 * ({@code :53}), while the dependency loop runs over the <em>parsed request</em>
 * ({@code :49,59-62}) regardless. An ID that did not move still has its dependencies retargeted.
 *
 * <p>Runs entirely in memory — see {@link DecompositionFixture}.
 */
@DisplayName("Transfer operation")
class TransferPartitionsOperationTest {

    @Nested
    @DisplayName("transferring")
    class Transferring {

        @Test
        @DisplayName("moves the listed entities to the target cluster")
        void movesListedEntities() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2, 3)
                    .cluster("C2", 4)
                    .build();
            Element entity1 = decomposition.getCluster("C1").getElementByID((short) 1);

            new TransferPartitionsOperation("C1", "C2", "1,2").executeOperation(decomposition);

            assertThat(decomposition.getCluster("C1").getElementsIDs()).containsExactly((short) 3);
            assertThat(decomposition.getCluster("C2").getElementsIDs())
                    .containsExactlyInAnyOrder((short) 1, (short) 2, (short) 4);
            assertThat(decomposition.getCluster("C2").getElementByID((short) 1)).isSameAs(entity1);
        }

        @Test
        @DisplayName("silently skips entities absent from the source cluster")
        void silentlySkipsAbsentEntities() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 4)
                    .build();

            new TransferPartitionsOperation("C1", "C2", "1,99").executeOperation(decomposition);

            assertThat(decomposition.getCluster("C2").getElementsIDs())
                    .containsExactlyInAnyOrder((short) 1, (short) 4);
        }

        @Test
        @DisplayName("leaves the source cluster in place when every entity moves out")
        void leavesEmptySourceCluster() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 4)
                    .build();

            new TransferPartitionsOperation("C1", "C2", "1").executeOperation(decomposition);

            assertThat(decomposition.getClusters()).containsOnlyKeys("C1", "C2");
            assertThat(decomposition.getCluster("C1").getElements()).isEmpty();
        }

        /**
         * The membership half and the dependency half disagree about what the request means:
         * entity 4 is already in the target, so nothing moves, but its dependencies are
         * retargeted anyway because the loop at {@code :59-62} uses the parsed request.
         */
        @Test
        @DisplayName("retargets dependencies for an entity that did not move")
        void retargetsDependenciesForUnmovedEntity() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 4)
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 4)
                    .build();

            // Entity 4 lives in C2 already; the transfer moves nothing.
            new TransferPartitionsOperation("C1", "C2", "4").executeOperation(decomposition);

            assertThat(decomposition.getCluster("C2").getElementsIDs()).containsExactly((short) 4);
            assertThat(dependenciesOf(decomposition, "C3"))
                    .as("retargeted despite no entity moving")
                    .containsOnlyKeys("C2");
        }
    }

    @Nested
    @DisplayName("coupling dependencies")
    class CouplingDependencies {

        @Test
        @DisplayName("retargets a third party's dependency on the moved entities")
        void retargetsThirdPartyDependency() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 3)
                    .cluster("C2", 4)
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 1, 3)
                    .build();

            new TransferPartitionsOperation("C1", "C2", "1").executeOperation(decomposition);

            assertThat(dependenciesOf(decomposition, "C3"))
                    .containsOnlyKeys("C1", "C2")
                    .containsEntry("C1", Collections.singleton((short) 3))
                    .containsEntry("C2", Collections.singleton((short) 1));
        }

        /**
         * The self-edge suppression at {@code Partition.java:47}. C2 reached entity 1 while it
         * lived in C1; once the entity moves into C2, the reach is internal and is dropped
         * rather than becoming {@code C2 -> C2}.
         */
        @Test
        @DisplayName("drops the dependency when the entity moves into the cluster that reached it")
        void dropsDependencyOnSelf() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 4)
                    .dependency("C2", "C1", 1)
                    .build();

            new TransferPartitionsOperation("C1", "C2", "1").executeOperation(decomposition);

            assertThat(dependenciesOf(decomposition, "C2"))
                    .as("no self-loop, and no leftover key")
                    .isEmpty();
        }

        @Test
        @DisplayName("leaves dependencies naming other clusters alone")
        void leavesUnrelatedDependencies() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 4)
                    .cluster("C3", 5)
                    .cluster("C4", 9)
                    .dependency("C3", "C1", 1)
                    .dependency("C3", "C4", 9)
                    .build();

            new TransferPartitionsOperation("C1", "C2", "1").executeOperation(decomposition);

            assertThat(dependenciesOf(decomposition, "C3"))
                    .containsOnlyKeys("C2", "C4")
                    .containsEntry("C4", Collections.singleton((short) 9));
        }
    }

    @Nested
    @DisplayName("guards")
    class Guards {

        @Test
        @DisplayName("fails with Error when the source cluster does not exist")
        void failsOnUnknownSource() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .build();

            assertThatThrownBy(() -> new TransferPartitionsOperation("Absent", "C1", "1").executeOperation(decomposition))
                    .isInstanceOf(Error.class)
                    .hasMessageContaining("Absent");
        }

        @Test
        @DisplayName("fails with Error when the target cluster does not exist")
        void failsOnUnknownTarget() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .build();

            assertThatThrownBy(() -> new TransferPartitionsOperation("C1", "Absent", "1").executeOperation(decomposition))
                    .isInstanceOf(Error.class)
                    .hasMessageContaining("Absent");
        }

        @ParameterizedTest(name = "entities=\"{0}\"")
        @ValueSource(strings = {"abc", "1,abc", "", "1,,2"})
        @DisplayName("fails on a malformed entity list")
        void rejectsMalformedEntityIds(String entities) {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 2)
                    .build();

            assertThatThrownBy(() -> new TransferPartitionsOperation("C1", "C2", entities).executeOperation(decomposition))
                    .isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("undo")
    class Undo {

        @Test
        @DisplayName("restores the original clusters and dependencies")
        void undoRestoresOriginal() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 4)
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 1, 2)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            TransferPartitionsOperation operation = new TransferPartitionsOperation("C1", "C2", "1");
            operation.executeOperation(decomposition);
            operation.undo(decomposition);

            DecompositionSnapshot after = DecompositionSnapshot.of(decomposition);
            assertThat(after.clusters()).isEqualTo(before.clusters());
            assertThat(after.dependencies()).isEqualTo(before.dependencies());
        }

        /**
         * <b>Transfer is not reversible in general.</b> The forward pass suppressed the
         * {@code C2 -> C1} dependency on entity 1 rather than turning it into a self-loop, and
         * kept no record of it; on the way back, {@code transferCouplingDependencies} looks up
         * key {@code "C2"} in C2's own map, finds nothing, and returns at
         * {@code Partition.java:44}. Membership round-trips, coupling does not.
         *
         * <p>Documented current behaviour, recorded in the gap analysis. It matters because
         * coupling is the quantity the tool exists to minimize: a user who transfers an entity
         * and then undoes it is left with a decomposition that under-reports its own coupling.
         */
        @Test
        @DisplayName("does not restore a dependency that the forward pass suppressed")
        void undoDoesNotRestoreASuppressedSelfDependency() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 4)
                    .dependency("C2", "C1", 1)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            TransferPartitionsOperation operation = new TransferPartitionsOperation("C1", "C2", "1");
            operation.executeOperation(decomposition);
            operation.undo(decomposition);

            DecompositionSnapshot after = DecompositionSnapshot.of(decomposition);

            // Membership is restored...
            assertThat(after.clusters()).isEqualTo(before.clusters());

            // ...but the dependency is gone, where it started as {"C1": {1}}.
            assertThat(after.dependencies()).isNotEqualTo(before.dependencies());
            assertThat(dependenciesOf(decomposition, "C2")).isEmpty();
        }

        @Test
        @DisplayName("round-trips again after a redo")
        void redoThenUndoRestoresOriginal() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 4)
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 1)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            TransferPartitionsOperation operation = new TransferPartitionsOperation("C1", "C2", "1");
            operation.execute(decomposition);
            DecompositionSnapshot transferred = DecompositionSnapshot.of(decomposition);

            operation.undo(decomposition);
            assertThat(DecompositionSnapshot.of(decomposition).clusters()).isEqualTo(before.clusters());

            operation.redo(decomposition);
            assertThat(DecompositionSnapshot.of(decomposition).clusters()).isEqualTo(transferred.clusters());
            assertThat(DecompositionSnapshot.of(decomposition).dependencies()).isEqualTo(transferred.dependencies());

            operation.undo(decomposition);
            assertThat(DecompositionSnapshot.of(decomposition).clusters()).isEqualTo(before.clusters());
            assertThat(DecompositionSnapshot.of(decomposition).dependencies()).isEqualTo(before.dependencies());
        }
    }

    @Nested
    @DisplayName("representation information hook")
    class RepresentationInformationHook {

        @Test
        @DisplayName("does nothing when no representation information is attached")
        void noRepresentationInformation() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 2)
                    .build();

            assertThat(decomposition.getRepresentationInformations()).isEmpty();

            new TransferPartitionsOperation("C1", "C2", "1").executeOperation(decomposition);

            assertThat(decomposition.getCluster("C2").getElementsIDs())
                    .containsExactlyInAnyOrder((short) 1, (short) 2);
        }

        /**
         * Transfer reports the parsed request rather than what moved, so a skipped ID still
         * reaches representation information — unlike merge and split, which report actual
         * cluster membership.
         */
        @Test
        @DisplayName("reports the requested entities, including ones that did not move")
        void reportsRequestedEntitiesNotMovedOnes() {
            RecordingRepresentationInformation recorder = new RecordingRepresentationInformation();
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 2)
                    .representationInformation(recorder)
                    .build();

            new TransferPartitionsOperation("C1", "C2", "1,99").executeOperation(decomposition);

            assertThat(recorder.onlyRemovedEntityIDs()).containsExactly((short) 1, (short) 99);
            assertThat(recorder.renames()).isEmpty();
        }
    }

    private static Map<String, Set<Short>> dependenciesOf(
            PartitionsDecomposition decomposition, String clusterName) {
        return ((Partition) decomposition.getCluster(clusterName)).getCouplingDependencies();
    }
}
