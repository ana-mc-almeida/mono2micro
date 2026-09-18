package pt.ist.socialsoftware.mono2micro.operation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.cluster.Partition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.operation.formCluster.FormClusterOperation;
import pt.ist.socialsoftware.mono2micro.operation.formCluster.FormClusterPartitionsOperation;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code FormClusterPartitionsOperation} — gathering entities from several clusters into a new one.
 *
 * <p>The most branch-heavy of the five, and the only one whose input is a
 * {@code Map<String, List<Short>>} of source cluster name to entity IDs rather than a
 * comma-separated string. That map is the source of its sharpest problem: <b>the keys are
 * ignored on the way forward and authoritative on the way back</b>. {@code formCluster} locates
 * each entity by scanning every cluster ({@code :66-68}) and never reads the key, while
 * {@code undo} dispatches on it to decide whether to split a cluster back out or transfer into
 * an existing one ({@code :42-48}). A caller who supplies a wrong key therefore gets a working
 * operation and a corrupting undo — see {@link Undo#undoWithAWrongKeyMisplacesEntities()}.
 *
 * <p>Unlike {@code split}, this operation removes a source cluster it empties ({@code :78-79}),
 * and {@code undo} depends on that: an absent source means "split it back out", a present one
 * means "transfer back". The same removal is what lets a cluster be re-formed under its own name.
 *
 * <p>Runs entirely in memory — see {@link DecompositionFixture}.
 */
@DisplayName("Form cluster operation")
class FormClusterPartitionsOperationTest {

    @Nested
    @DisplayName("forming")
    class Forming {

        @Test
        @DisplayName("gathers entities from several clusters")
        void gathersFromSeveralClusters() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 3, 4)
                    .build();

            operation("NC", entities("C1", new int[]{1}, "C2", new int[]{3}))
                    .executeOperation(decomposition);

            assertThat(decomposition.getCluster("NC").getElementsIDs())
                    .containsExactlyInAnyOrder((short) 1, (short) 3);
            assertThat(decomposition.getCluster("C1").getElementsIDs()).containsExactly((short) 2);
            assertThat(decomposition.getCluster("C2").getElementsIDs()).containsExactly((short) 4);
        }

        /** Contrast {@code split}, which leaves the emptied cluster in place. */
        @Test
        @DisplayName("removes a source cluster it empties")
        void removesEmptiedSourceCluster() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 2)
                    .build();

            operation("NC", entities("C1", 1)).executeOperation(decomposition);

            assertThat(decomposition.getClusters()).containsOnlyKeys("NC", "C2");
        }

        /**
         * The keys are decorative here: entity 1 lives in C1, the map says {@code "Wrong"}, and
         * the operation succeeds because {@code :66-68} scans instead of looking up. Harmless
         * going forward, corrupting on undo — see {@link Undo#undoWithAWrongKeyMisplacesEntities()}.
         */
        @Test
        @DisplayName("ignores the map keys and finds entities by scanning")
        void ignoresMapKeys() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .build();

            operation("NC", entities("Wrong", 1)).executeOperation(decomposition);

            assertThat(decomposition.getCluster("NC").getElementsIDs()).containsExactly((short) 1);
            assertThat(decomposition.getCluster("C1").getElementsIDs()).containsExactly((short) 2);
        }

        @Test
        @DisplayName("creates an empty cluster when given no entities")
        void emptyEntityMapCreatesEmptyCluster() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .build();

            operation("NC", new LinkedHashMap<>()).executeOperation(decomposition);

            assertThat(decomposition.getCluster("NC").getElements()).isEmpty();
            assertThat(decomposition.getCluster("C1").getElementsIDs()).containsExactly((short) 1);
        }

        @Test
        @DisplayName("throws when no cluster holds a requested entity")
        void throwsOnUnknownEntity() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .build();

            assertThatThrownBy(() -> operation("NC", entities("C1", 99)).executeOperation(decomposition))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("No cluster contains entity 99");
        }
    }

    @Nested
    @DisplayName("re-forming under an existing name")
    class ReForming {

        /**
         * Permitted when the new entity set covers everything the existing cluster held
         * ({@code :59-60}). It works only because the old cluster is emptied and therefore
         * removed at {@code :78-79}, freeing the name before {@code addCluster} at {@code :81} —
         * which would otherwise throw {@code Error}. Two behaviours holding each other up.
         */
        @Test
        @DisplayName("is allowed with a superset of the existing entities")
        void allowsSupersetReForm() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("NC", 1, 2)
                    .cluster("C2", 3)
                    .build();

            operation("NC", entities("NC", new int[]{1, 2}, "C2", new int[]{3}))
                    .executeOperation(decomposition);

            assertThat(decomposition.getClusters()).containsOnlyKeys("NC");
            assertThat(decomposition.getCluster("NC").getElementsIDs())
                    .containsExactlyInAnyOrder((short) 1, (short) 2, (short) 3);
        }

        @Test
        @DisplayName("is rejected with only a subset, changing nothing")
        void rejectsSubsetReForm() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("NC", 1, 2)
                    .cluster("C2", 3)
                    .dependency("C2", "NC", 1)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            assertThatThrownBy(() -> operation("NC", entities("NC", 1)).executeOperation(decomposition))
                    .isInstanceOf(KeyAlreadyExistsException.class)
                    .hasMessageContaining("NC");

            DecompositionSnapshot after = DecompositionSnapshot.of(decomposition);
            assertThat(after.clusters()).isEqualTo(before.clusters());
            assertThat(after.dependencies()).isEqualTo(before.dependencies());
        }
    }

    @Nested
    @DisplayName("coupling dependencies")
    class CouplingDependencies {

        @Test
        @DisplayName("retargets a third party's dependencies onto the new cluster")
        void retargetsThirdPartyDependencies() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 3)
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 1)
                    .dependency("C3", "C2", 3)
                    .build();

            operation("NC", entities("C1", new int[]{1}, "C2", new int[]{3}))
                    .executeOperation(decomposition);

            assertThat(dependenciesOf(decomposition, "C3"))
                    .containsOnlyKeys("NC")
                    .containsEntry("NC", setOf((short) 1, (short) 3));
        }

        @Test
        @DisplayName("leaves dependencies on entities that stayed put")
        void leavesDependenciesOnRemainingEntities() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 1, 2)
                    .build();

            operation("NC", entities("C1", 1)).executeOperation(decomposition);

            assertThat(dependenciesOf(decomposition, "C3"))
                    .containsOnlyKeys("C1", "NC")
                    .containsEntry("C1", Collections.singleton((short) 2))
                    .containsEntry("NC", Collections.singleton((short) 1));
        }

        /**
         * The dependency loop at {@code :73-76} walks {@code getClusters().values()} while the
         * about-to-be-emptied source is still in the map, and the removal at {@code :78-79}
         * happens inside the same entity loop. Guards against a future reordering that would
         * throw {@code ConcurrentModificationException}.
         */
        @Test
        @DisplayName("survives emptying a source cluster mid-iteration")
        void survivesSourceRemovalDuringIteration() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 2)
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 1)
                    .dependency("C3", "C2", 2)
                    .build();

            operation("NC", entities("C1", new int[]{1}, "C2", new int[]{2}))
                    .executeOperation(decomposition);

            assertThat(decomposition.getClusters()).containsOnlyKeys("NC", "C3");
            assertThat(dependenciesOf(decomposition, "C3")).containsOnlyKeys("NC");
        }
    }

    @Nested
    @DisplayName("undo")
    class Undo {

        /** Source survived the forward pass, so undo transfers back. */
        @Test
        @DisplayName("restores the original when the source cluster survived")
        void undoRestoresWhenSourceSurvived() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 1)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            FormClusterPartitionsOperation operation = operation("NC", entities("C1", 1));
            operation.executeOperation(decomposition);
            operation.undo(decomposition);

            DecompositionSnapshot after = DecompositionSnapshot.of(decomposition);
            assertThat(after.clusters()).isEqualTo(before.clusters());
            assertThat(after.dependencies()).isEqualTo(before.dependencies());
        }

        /** Source was emptied and removed, so undo splits it back out. */
        @Test
        @DisplayName("restores the original when the source cluster was removed")
        void undoRestoresWhenSourceWasRemoved() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 1)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            FormClusterPartitionsOperation operation = operation("NC", entities("C1", 1));
            operation.executeOperation(decomposition);
            assertThat(decomposition.getClusters()).doesNotContainKey("C1");

            operation.undo(decomposition);

            DecompositionSnapshot after = DecompositionSnapshot.of(decomposition);
            assertThat(after.clusters()).isEqualTo(before.clusters());
            assertThat(after.dependencies()).isEqualTo(before.dependencies());
        }

        /** Both undo paths in one operation: C1 is emptied and removed, C2 survives. */
        @Test
        @DisplayName("restores the original across both undo paths at once")
        void undoRestoresAcrossBothPaths() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 2, 3)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            FormClusterPartitionsOperation operation =
                    operation("NC", entities("C1", new int[]{1}, "C2", new int[]{2}));
            operation.executeOperation(decomposition);
            operation.undo(decomposition);

            DecompositionSnapshot after = DecompositionSnapshot.of(decomposition);
            assertThat(after.clusters()).isEqualTo(before.clusters());
            assertThat(after.dependencies()).isEqualTo(before.dependencies());
        }

        /**
         * <b>The consequence of the keys being decorative forward and authoritative backward.</b>
         * Entity 1 lives in C1, but the map names C2. The forward pass works — it scans — and
         * moves 1 out of C1. The undo reads the key, finds C2 present, and transfers entity 1
         * into <em>C2</em>. The entity ends up in a cluster it was never in, and no error is
         * raised at any point.
         *
         * <p>Reachable through {@code DecompositionController} with a hand-crafted request body,
         * so this documents corruption rather than endorsing it, and stays enabled so a change
         * in behaviour shows up. Recorded in the gap analysis.
         */
        @Test
        @DisplayName("misplaces entities when the map key names the wrong source")
        void undoWithAWrongKeyMisplacesEntities() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 9)
                    .build();

            FormClusterPartitionsOperation operation = operation("NC", entities("C2", 1));
            operation.executeOperation(decomposition);
            operation.undo(decomposition);

            assertThat(decomposition.getClusters()).containsOnlyKeys("C1", "C2");
            assertThat(decomposition.getCluster("C1").getElementsIDs())
                    .as("entity 1 did not come home")
                    .containsExactly((short) 2);
            assertThat(decomposition.getCluster("C2").getElementsIDs())
                    .as("it landed in the cluster the key named instead")
                    .containsExactlyInAnyOrder((short) 1, (short) 9);
        }

        @Test
        @DisplayName("removes the empty cluster when undoing a form with no entities")
        void undoOfEmptyFormRemovesTheCluster() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .build();

            FormClusterPartitionsOperation operation = operation("NC", new LinkedHashMap<>());
            operation.executeOperation(decomposition);
            operation.undo(decomposition);

            assertThat(decomposition.getClusters()).containsOnlyKeys("C1");
        }

        @Test
        @DisplayName("round-trips again after a redo")
        void redoThenUndoRestoresOriginal() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 3)
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 1)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            FormClusterPartitionsOperation operation =
                    operation("NC", entities("C1", new int[]{1}, "C2", new int[]{3}));
            operation.execute(decomposition);
            DecompositionSnapshot formed = DecompositionSnapshot.of(decomposition);

            operation.undo(decomposition);
            assertThat(DecompositionSnapshot.of(decomposition).clusters()).isEqualTo(before.clusters());

            operation.redo(decomposition);
            assertThat(DecompositionSnapshot.of(decomposition).clusters()).isEqualTo(formed.clusters());
            assertThat(DecompositionSnapshot.of(decomposition).dependencies()).isEqualTo(formed.dependencies());

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

            operation("NC", entities("C1", 1)).executeOperation(decomposition);

            assertThat(decomposition.getCluster("NC").getElementsIDs()).containsExactly((short) 1);
        }

        @Test
        @DisplayName("reports every entity named in the map")
        void reportsEveryMappedEntity() {
            RecordingRepresentationInformation recorder = new RecordingRepresentationInformation();
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 3)
                    .representationInformation(recorder)
                    .build();

            operation("NC", entities("C1", new int[]{1}, "C2", new int[]{3}))
                    .executeOperation(decomposition);

            assertThat(recorder.onlyRemovedEntityIDs()).containsExactly((short) 1, (short) 3);
            assertThat(recorder.renames()).isEmpty();
        }
    }

    /**
     * {@code FormClusterPartitionsOperation} has no {@code (String, Map)} constructor, unlike the
     * other four operations, so it is built through its {@code FormClusterOperation} copy
     * constructor.
     */
    private static FormClusterPartitionsOperation operation(String newCluster, Map<String, List<Short>> entities) {
        FormClusterOperation dto = new FormClusterOperation();
        dto.setNewCluster(newCluster);
        dto.setEntities(entities);
        return new FormClusterPartitionsOperation(dto);
    }

    /** {@code entities("C1", 1)} — one source cluster. */
    private static Map<String, List<Short>> entities(String cluster, int... entityIDs) {
        Map<String, List<Short>> entities = new LinkedHashMap<>();
        entities.put(cluster, shorts(entityIDs));
        return entities;
    }

    /** {@code entities("C1", new int[]{1}, "C2", new int[]{3})} — two source clusters. */
    private static Map<String, List<Short>> entities(String cluster1, int[] ids1, String cluster2, int[] ids2) {
        Map<String, List<Short>> entities = new LinkedHashMap<>();
        entities.put(cluster1, shorts(ids1));
        entities.put(cluster2, shorts(ids2));
        return entities;
    }

    private static List<Short> shorts(int... ids) {
        Short[] boxed = new Short[ids.length];
        for (int i = 0; i < ids.length; i++)
            boxed[i] = (short) ids[i];
        return Arrays.asList(boxed);
    }

    private static Map<String, Set<Short>> dependenciesOf(
            PartitionsDecomposition decomposition, String clusterName) {
        return ((Partition) decomposition.getCluster(clusterName)).getCouplingDependencies();
    }

    private static Set<Short> setOf(short... ids) {
        Set<Short> set = new java.util.TreeSet<>();
        for (short id : ids)
            set.add(id);
        return set;
    }
}
