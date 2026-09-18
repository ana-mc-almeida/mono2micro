package pt.ist.socialsoftware.mono2micro.operation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.cluster.Cluster;
import pt.ist.socialsoftware.mono2micro.cluster.Partition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.operation.rename.RenamePartitionsOperation;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code RenamePartitionsOperation} — renaming a cluster and the dependencies pointing at it.
 *
 * <p>The simplest of the five operations, and the one whose quirk matters most elsewhere:
 * renaming a cluster to the name it already has is <b>allowed</b>, because the duplicate-name
 * guard carries {@code && !clusterName.equals(newClusterName)}
 * ({@code RenamePartitionsOperation.java:42}). That escape hatch looks permissive in isolation
 * but is load-bearing — {@code MergePartitionsOperation.undo} finishes with a self-rename in
 * two of its three branches, so tightening this guard would silently break merge's undo. See
 * {@link MergePartitionsOperationTest} and {@link SelfRename} below.
 *
 * <p>Runs entirely in memory: no Spring context, no Mongo, no Docker. See
 * {@link DecompositionFixture} for why that is possible and {@link InMemoryHistory} for the one
 * stand-in involved.
 */
@DisplayName("Rename operation")
class RenamePartitionsOperationTest {

    @Nested
    @DisplayName("renaming")
    class Renaming {

        @Test
        @DisplayName("moves the cluster to its new name, keeping the same instance")
        void renamesClusterKeyAndName() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 3)
                    .build();
            Cluster original = decomposition.getCluster("C1");

            new RenamePartitionsOperation("C1", "Renamed").executeOperation(decomposition);

            assertThat(decomposition.getClusters()).containsOnlyKeys("Renamed", "C2");
            assertThat(decomposition.getCluster("Renamed").getElementsIDs())
                    .containsExactlyInAnyOrder((short) 1, (short) 2);

            // rename() re-inserts the very object it removed rather than rebuilding it, so the
            // map key and cluster.getName() stay in step. Unlike merge, which builds a fresh
            // Partition and drops the metrics of both inputs.
            assertThat(decomposition.getCluster("Renamed")).isSameAs(original);
            assertThat(original.getName()).isEqualTo("Renamed");
        }

        @Test
        @DisplayName("leaves the other clusters untouched")
        void leavesOtherClustersAlone() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 2, 3)
                    .build();

            new RenamePartitionsOperation("C1", "Renamed").executeOperation(decomposition);

            assertThat(decomposition.getCluster("C2").getElementsIDs())
                    .containsExactlyInAnyOrder((short) 2, (short) 3);
        }
    }

    @Nested
    @DisplayName("coupling dependencies")
    class CouplingDependencies {

        @Test
        @DisplayName("rewrites the keys of dependencies pointing at the renamed cluster")
        void rewritesDependencyKeys() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 2)
                    .cluster("C3", 3)
                    .dependency("C2", "C1", 1)
                    .dependency("C3", "C1", 1)
                    .build();

            new RenamePartitionsOperation("C1", "Renamed").executeOperation(decomposition);

            assertThat(dependenciesOf(decomposition, "C2"))
                    .containsOnlyKeys("Renamed")
                    .containsEntry("Renamed", Collections.singleton((short) 1));
            assertThat(dependenciesOf(decomposition, "C3")).containsOnlyKeys("Renamed");
        }

        @Test
        @DisplayName("leaves dependencies pointing elsewhere alone")
        void leavesUnrelatedDependencies() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 2)
                    .cluster("C3", 3)
                    .dependency("C3", "C1", 1)
                    .dependency("C3", "C2", 2)
                    .build();

            new RenamePartitionsOperation("C1", "Renamed").executeOperation(decomposition);

            assertThat(dependenciesOf(decomposition, "C3"))
                    .containsOnlyKeys("Renamed", "C2")
                    .containsEntry("C2", Collections.singleton((short) 2));
        }

        /**
         * A dependency key can outlive the cluster it names — see
         * {@link SplitPartitionsOperationTest} on per-cluster dependency ownership — so renaming
         * onto a stale key has to merge rather than overwrite.
         *
         * <p>This is also the one place {@code addCouplingDependencies}' {@code containsKey}
         * branch is reached from production code. Its other branch stores the caller's
         * {@code Set} by reference ({@code Partition.java:33-39}); {@code rename} is safe only
         * because the set it hands over is one it has just orphaned, which is worth knowing
         * before writing a second caller.
         */
        @Test
        @DisplayName("merges into an existing dependency key rather than replacing it")
        void mergesIntoExistingDependencyKey() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 2)
                    .dependency("C2", "C1", 1)
                    .dependency("C2", "Stale", 9)
                    .build();

            // No cluster is named "Stale", so the guard permits the rename.
            new RenamePartitionsOperation("C1", "Stale").executeOperation(decomposition);

            assertThat(dependenciesOf(decomposition, "C2"))
                    .containsOnlyKeys("Stale")
                    .containsEntry("Stale", new java.util.HashSet<>(java.util.Arrays.asList((short) 9, (short) 1)));
        }
    }

    @Nested
    @DisplayName("renaming a cluster to its own name")
    class SelfRename {

        /**
         * Permitted, and depended upon by {@code MergePartitionsOperation.undo}: its
         * else-branch finishes with {@code rename(newName, cluster2Name)}, which is a self-rename
         * whenever the merge kept cluster2's name. Remove the {@code !clusterName.equals(...)}
         * half of the guard at {@code RenamePartitionsOperation.java:42} and merge undo starts
         * throwing {@code KeyAlreadyExistsException}.
         */
        @Test
        @DisplayName("is allowed and changes nothing")
        void selfRenameIsANoOp() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 3)
                    .dependency("C2", "C1", 1)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            new RenamePartitionsOperation("C1", "C1").executeOperation(decomposition);

            DecompositionSnapshot after = DecompositionSnapshot.of(decomposition);
            assertThat(after.clusters()).isEqualTo(before.clusters());
            assertThat(after.dependencies()).isEqualTo(before.dependencies());
        }

        @Test
        @DisplayName("undoes to itself")
        void undoOfSelfRenameIsANoOp() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 2)
                    .dependency("C2", "C1", 1)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            RenamePartitionsOperation operation = new RenamePartitionsOperation("C1", "C1");
            operation.executeOperation(decomposition);
            operation.undo(decomposition);

            DecompositionSnapshot after = DecompositionSnapshot.of(decomposition);
            assertThat(after.clusters()).isEqualTo(before.clusters());
            assertThat(after.dependencies()).isEqualTo(before.dependencies());
        }
    }

    @Nested
    @DisplayName("guards")
    class Guards {

        @Test
        @DisplayName("rejects renaming onto another existing cluster, changing nothing")
        void rejectsExistingName() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 2)
                    .dependency("C2", "C1", 1)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            assertThatThrownBy(() -> new RenamePartitionsOperation("C1", "C2").executeOperation(decomposition))
                    .isInstanceOf(KeyAlreadyExistsException.class)
                    .hasMessageContaining("C2");

            // The guard runs before any mutation.
            DecompositionSnapshot after = DecompositionSnapshot.of(decomposition);
            assertThat(after.clusters()).isEqualTo(before.clusters());
            assertThat(after.dependencies()).isEqualTo(before.dependencies());
        }

        /**
         * {@code Decomposition.removeCluster} throws {@code java.lang.Error} rather than an
         * exception ({@code Decomposition.java:147-153}). {@code DecompositionController}
         * catches {@code Exception}, so this reaches the client as a 500 rather than a 400 —
         * recorded in the gap analysis, pinned here so the behaviour is not changed by accident.
         */
        @Test
        @DisplayName("fails with Error when the cluster does not exist")
        void failsOnUnknownCluster() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .build();

            assertThatThrownBy(() -> new RenamePartitionsOperation("Absent", "Renamed").executeOperation(decomposition))
                    .isInstanceOf(Error.class)
                    .hasMessageContaining("Absent");
        }
    }

    @Nested
    @DisplayName("undo")
    class Undo {

        @Test
        @DisplayName("restores the original name and dependency keys")
        void undoRestoresOriginal() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 3)
                    .dependency("C2", "C1", 1)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            RenamePartitionsOperation operation = new RenamePartitionsOperation("C1", "Renamed");
            operation.executeOperation(decomposition);
            operation.undo(decomposition);

            DecompositionSnapshot after = DecompositionSnapshot.of(decomposition);
            assertThat(after.clusters()).isEqualTo(before.clusters());
            assertThat(after.dependencies()).isEqualTo(before.dependencies());
        }

        @Test
        @DisplayName("round-trips again after a redo")
        void redoThenUndoRestoresOriginal() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 3)
                    .dependency("C2", "C1", 1)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            RenamePartitionsOperation operation = new RenamePartitionsOperation("C1", "Renamed");
            operation.execute(decomposition);
            DecompositionSnapshot renamed = DecompositionSnapshot.of(decomposition);

            operation.undo(decomposition);
            assertThat(DecompositionSnapshot.of(decomposition).clusters()).isEqualTo(before.clusters());

            // redo() is Operation's default: executeOperation, with no history bookkeeping.
            operation.redo(decomposition);
            assertThat(DecompositionSnapshot.of(decomposition).clusters()).isEqualTo(renamed.clusters());
            assertThat(DecompositionSnapshot.of(decomposition).dependencies()).isEqualTo(renamed.dependencies());

            operation.undo(decomposition);
            assertThat(DecompositionSnapshot.of(decomposition).clusters()).isEqualTo(before.clusters());
            assertThat(DecompositionSnapshot.of(decomposition).dependencies()).isEqualTo(before.dependencies());
        }
    }

    @Nested
    @DisplayName("representation information hook")
    class RepresentationInformationHook {

        /**
         * The production default, and the reason this suite can exist at all: with no
         * representation information attached, {@code executeOperation}'s {@code forEach} body
         * never runs, so {@code AccessesInformation.renameClusterInFunctionalities} — which
         * opens with {@code ContextManager.get().getBean(FunctionalityService.class)} — is never
         * reached. A decomposition carrying a real {@code AccessesInformation} could not be unit
         * tested at all.
         */
        @Test
        @DisplayName("does nothing when no representation information is attached")
        void noRepresentationInformation() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .build();

            assertThat(decomposition.getRepresentationInformations()).isEmpty();

            new RenamePartitionsOperation("C1", "Renamed").executeOperation(decomposition);

            assertThat(decomposition.getClusters()).containsOnlyKeys("Renamed");
        }

        @Test
        @DisplayName("passes the old and new names downstream")
        void passesNamesDownstream() {
            RecordingRepresentationInformation recorder = new RecordingRepresentationInformation();
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .representationInformation(recorder)
                    .build();

            new RenamePartitionsOperation("C1", "Renamed").executeOperation(decomposition);

            assertThat(recorder.renames()).containsExactly("C1 -> Renamed");
            assertThat(recorder.removedEntityIDs()).isEmpty();
        }
    }

    private static java.util.Map<String, java.util.Set<Short>> dependenciesOf(
            PartitionsDecomposition decomposition, String clusterName) {
        return ((Partition) decomposition.getCluster(clusterName)).getCouplingDependencies();
    }
}
