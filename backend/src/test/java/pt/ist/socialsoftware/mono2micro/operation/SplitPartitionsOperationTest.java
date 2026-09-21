package pt.ist.socialsoftware.mono2micro.operation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.ist.socialsoftware.mono2micro.cluster.Partition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.element.Element;
import pt.ist.socialsoftware.mono2micro.operation.split.SplitPartitionsOperation;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code SplitPartitionsOperation} — moving entities out of a cluster into a new one.
 *
 * <p>Two behaviours here are worth knowing before changing anything:
 *
 * <ul>
 *   <li><b>An emptied origin cluster is left in place.</b> {@code split} never removes it,
 *       while {@code formCluster} removes sources it empties
 *       ({@code FormClusterPartitionsOperation.java:78-79}). The inconsistency is not free to
 *       fix: {@code undo} merges the new cluster back into the origin <em>by name</em>, so if
 *       split removed the empty cluster, undo would throw. See {@link Undo}.
 *   <li><b>Dependencies stay with the origin cluster, not with the entities.</b> The new
 *       cluster is added to the decomposition only after the dependency-transfer loop
 *       ({@code SplitPartitionsOperation.java:62-67}), so it never receives outgoing
 *       dependencies of its own. Ownership is per-cluster, not per-entity. See
 *       {@link CouplingDependencies}.
 * </ul>
 *
 * <p>Runs entirely in memory — see {@link DecompositionFixture}.
 */
@DisplayName("Split operation")
class SplitPartitionsOperationTest {

    @Nested
    @DisplayName("splitting")
    class Splitting {

        @Test
        @DisplayName("moves the listed entities into the new cluster")
        void movesListedEntities() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2, 3)
                    .build();
            Element entity1 = decomposition.getCluster("C1").getElementByID((short) 1);

            new SplitPartitionsOperation("C1", "C2", "1,2").executeOperation(decomposition);

            assertThat(decomposition.getCluster("C1").getElementsIDs()).containsExactly((short) 3);
            assertThat(decomposition.getCluster("C2").getElementsIDs())
                    .containsExactlyInAnyOrder((short) 1, (short) 2);

            // Moved, not copied.
            assertThat(decomposition.getCluster("C2").getElementByID((short) 1)).isSameAs(entity1);
        }

        /**
         * {@code if (entity != null)} at {@code SplitPartitionsOperation.java:57} makes a
         * mistyped entity ID a no-op rather than an error, so a request naming entities that
         * are not in the origin cluster half-succeeds.
         */
        @Test
        @DisplayName("silently skips entity IDs absent from the origin cluster")
        void silentlySkipsAbsentEntities() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("Other", 9)
                    .build();

            new SplitPartitionsOperation("C1", "C2", "1,99,9").executeOperation(decomposition);

            // 99 does not exist; 9 exists but lives in another cluster. Both are ignored.
            assertThat(decomposition.getCluster("C2").getElementsIDs()).containsExactly((short) 1);
            assertThat(decomposition.getCluster("C1").getElementsIDs()).containsExactly((short) 2);
            assertThat(decomposition.getCluster("Other").getElementsIDs()).containsExactly((short) 9);
        }

        /**
         * Pins the inconsistency with {@code formCluster}, which removes a source it empties.
         * Load-bearing for {@link Undo#undoAfterFullSplitRestoresOriginal()}.
         */
        @Test
        @DisplayName("leaves the origin cluster in place when every entity moves out")
        void leavesEmptyOriginCluster() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .build();

            new SplitPartitionsOperation("C1", "C2", "1,2").executeOperation(decomposition);

            assertThat(decomposition.getClusters()).containsOnlyKeys("C1", "C2");
            assertThat(decomposition.getCluster("C1").getElements()).isEmpty();
        }

        @Test
        @DisplayName("splitting off nothing still creates an empty cluster")
        void splitOffNothingCreatesEmptyCluster() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .build();

            new SplitPartitionsOperation("C1", "C2", "99").executeOperation(decomposition);

            assertThat(decomposition.getCluster("C2").getElements()).isEmpty();
            assertThat(decomposition.getCluster("C1").getElementsIDs()).containsExactly((short) 1);
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
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 1, 3)
                    .build();

            new SplitPartitionsOperation("C1", "C2", "1").executeOperation(decomposition);

            // Entity 1 moved to C2, entity 3 stayed — so the origin key survives alongside.
            assertThat(dependenciesOf(decomposition, "C3"))
                    .containsOnlyKeys("C1", "C2")
                    .containsEntry("C1", Collections.singleton((short) 3))
                    .containsEntry("C2", Collections.singleton((short) 1));
        }

        @Test
        @DisplayName("drops the origin key when every entity it named moves out")
        void dropsOriginKeyWhenFullyMoved() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 1)
                    .build();

            new SplitPartitionsOperation("C1", "C2", "1").executeOperation(decomposition);

            assertThat(dependenciesOf(decomposition, "C3")).containsOnlyKeys("C2");
        }

        /**
         * Dependency ownership is per-cluster, not per-entity. The new cluster is created at
         * {@code :52} but only added to the decomposition at {@code :67}, after the transfer
         * loop at {@code :62-65} has walked {@code getClusters().values()} — so it is never a
         * candidate to receive dependencies, and the origin keeps every outgoing dependency
         * even when the entity that justified it has moved away.
         *
         * <p>Documented current behaviour, recorded in the gap analysis. Under the thesis'
         * coupling metric this misattributes the reach of the split halves.
         */
        @Test
        @DisplayName("leaves the origin's outgoing dependencies behind, even for moved entities")
        void newClusterReceivesNoOutgoingDependencies() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C4", 7)
                    .dependency("C1", "C4", 7)
                    .build();

            new SplitPartitionsOperation("C1", "C2", "1").executeOperation(decomposition);

            assertThat(dependenciesOf(decomposition, "C1"))
                    .as("the origin keeps the dependency")
                    .containsEntry("C4", Collections.singleton((short) 7));
            assertThat(dependenciesOf(decomposition, "C2"))
                    .as("the new cluster gets none")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("guards")
    class Guards {

        @Test
        @DisplayName("rejects a new cluster name that already exists, changing nothing")
        void rejectsExistingNewClusterName() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 3)
                    .dependency("C2", "C1", 1)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            assertThatThrownBy(() -> new SplitPartitionsOperation("C1", "C2", "1").executeOperation(decomposition))
                    .isInstanceOf(KeyAlreadyExistsException.class)
                    .hasMessageContaining("C2");

            DecompositionSnapshot after = DecompositionSnapshot.of(decomposition);
            assertThat(after.clusters()).isEqualTo(before.clusters());
            assertThat(after.dependencies()).isEqualTo(before.dependencies());
        }

        @Test
        @DisplayName("fails with Error when the origin cluster does not exist")
        void failsOnUnknownOriginCluster() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .build();

            assertThatThrownBy(() -> new SplitPartitionsOperation("Absent", "C2", "1").executeOperation(decomposition))
                    .isInstanceOf(Error.class)
                    .hasMessageContaining("Absent");
        }

        /**
         * {@code Short.parseShort} at {@code :55} is unguarded, so a malformed entity list
         * escapes as {@code NumberFormatException}. {@code DecompositionController} catches
         * {@code Exception}, so this one does reach the client as a 400 — unlike the
         * {@code Error} thrown for an unknown cluster name.
         */
        @ParameterizedTest(name = "entities=\"{0}\"")
        @ValueSource(strings = {"abc", "1,abc", "", "1,,2", "99999"})
        @DisplayName("fails on a malformed entity list")
        void rejectsMalformedEntityIds(String entities) {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .build();

            assertThatThrownBy(() -> new SplitPartitionsOperation("C1", "C2", entities).executeOperation(decomposition))
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
                    .cluster("C1", 1, 2, 3)
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 1, 3)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            SplitPartitionsOperation operation = new SplitPartitionsOperation("C1", "C2", "1");
            operation.executeOperation(decomposition);
            operation.undo(decomposition);

            DecompositionSnapshot after = DecompositionSnapshot.of(decomposition);
            assertThat(after.clusters()).isEqualTo(before.clusters());
            assertThat(after.dependencies()).isEqualTo(before.dependencies());
        }

        /**
         * Holds only because {@code split} left the emptied origin cluster in place:
         * {@code undo} delegates to {@code MergePartitionsOperation(origin, new, origin)}, whose
         * {@code merge} opens with {@code decomposition.getCluster(origin)}. Remove the empty
         * cluster in {@code split} and this throws {@code Error}.
         */
        @Test
        @DisplayName("restores the original after every entity was split off")
        void undoAfterFullSplitRestoresOriginal() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            SplitPartitionsOperation operation = new SplitPartitionsOperation("C1", "C2", "1,2");
            operation.executeOperation(decomposition);
            operation.undo(decomposition);

            assertThat(decomposition.getClusters()).containsOnlyKeys("C1");
            assertThat(DecompositionSnapshot.of(decomposition).clusters()).isEqualTo(before.clusters());
        }

        @Test
        @DisplayName("round-trips again after a redo")
        void redoThenUndoRestoresOriginal() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2, 3)
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 1)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            SplitPartitionsOperation operation = new SplitPartitionsOperation("C1", "C2", "1");
            operation.execute(decomposition);
            DecompositionSnapshot split = DecompositionSnapshot.of(decomposition);

            operation.undo(decomposition);
            assertThat(DecompositionSnapshot.of(decomposition).clusters()).isEqualTo(before.clusters());

            operation.redo(decomposition);
            assertThat(DecompositionSnapshot.of(decomposition).clusters()).isEqualTo(split.clusters());
            assertThat(DecompositionSnapshot.of(decomposition).dependencies()).isEqualTo(split.dependencies());

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
                    .cluster("C1", 1, 2)
                    .build();

            assertThat(decomposition.getRepresentationInformations()).isEmpty();

            new SplitPartitionsOperation("C1", "C2", "1").executeOperation(decomposition);

            assertThat(decomposition.getCluster("C2").getElementsIDs()).containsExactly((short) 1);
        }

        /** Split reports the <em>new</em> cluster's membership, so a skipped ID is not reported. */
        @Test
        @DisplayName("reports the new cluster's entities, not the requested ones")
        void reportsNewClusterMembership() {
            RecordingRepresentationInformation recorder = new RecordingRepresentationInformation();
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .representationInformation(recorder)
                    .build();

            new SplitPartitionsOperation("C1", "C2", "1,99").executeOperation(decomposition);

            assertThat(recorder.onlyRemovedEntityIDs()).containsExactly((short) 1);
            assertThat(recorder.renames()).isEmpty();
        }
    }

    private static Map<String, Set<Short>> dependenciesOf(
            PartitionsDecomposition decomposition, String clusterName) {
        return ((Partition) decomposition.getCluster(clusterName)).getCouplingDependencies();
    }
}
