package pt.ist.socialsoftware.mono2micro.operation;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pt.ist.socialsoftware.mono2micro.cluster.Partition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.operation.merge.MergePartitionsOperation;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code MergePartitionsOperation} — combining two clusters into one.
 *
 * <p>Merge is the operation with the most state to get right, and two findings here matter more
 * than the rest:
 *
 * <ul>
 *   <li><b>The merged cluster loses its outgoing coupling dependencies.</b> {@code merge}
 *       rewrites dependencies in a loop over {@code getClusters().values()} and only then adds
 *       the merged cluster ({@code MergePartitionsOperation.java:68-76}), which is besides a
 *       fresh {@code Partition} ({@code :62}). Dependencies the two inputs held are therefore
 *       dropped rather than carried over, and {@code undo} cannot bring them back. Since the
 *       thesis minimizes exactly this quantity, it is recorded as a defect — see
 *       {@link CouplingDependencies#mergePreservesOutgoingDependencies()}.
 *   <li><b>{@code undo} is correct in all three name shapes, and only just.</b> It branches on
 *       {@code cluster1Name.equals(newName)} ({@code :47}) to choose which side to split back
 *       off, so that the subsequent rename always has a free target. When the merge kept
 *       cluster2's name, that rename is a <em>self-rename</em>, which only works because
 *       {@code RenamePartitionsOperation}'s duplicate-name guard exempts it
 *       ({@code RenamePartitionsOperation.java:42}). Tighten that guard and merge undo breaks.
 *       See {@link Undo#undoRestoresBothClusters(String)}.
 * </ul>
 *
 * <p>Merge is also the one operation whose {@code execute} does more than
 * {@code executeOperation}: it first calls {@code storeState} to capture the pre-merge
 * membership that {@code undo} needs ({@code :29}). So unlike the other four,
 * {@code executeOperation} followed by {@code undo} does not work — see
 * {@link Undo#undoWithoutExecuteFails()}.
 */
@DisplayName("Merge operation")
class MergePartitionsOperationTest {

    @Nested
    @DisplayName("merging")
    class Merging {

        @Test
        @DisplayName("combines both clusters under a new name")
        void mergesIntoNewName() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 3)
                    .cluster("Bystander", 9)
                    .build();

            new MergePartitionsOperation("C1", "C2", "M").executeOperation(decomposition);

            assertThat(decomposition.getClusters()).containsOnlyKeys("M", "Bystander");
            assertThat(decomposition.getCluster("M").getElementsIDs())
                    .containsExactlyInAnyOrder((short) 1, (short) 2, (short) 3);
        }

        @ParameterizedTest(name = "keeping the name \"{0}\"")
        @CsvSource({"C1", "C2"})
        @DisplayName("may keep either input's name")
        void mergesIntoAnInputName(String newName) {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 3)
                    .build();

            new MergePartitionsOperation("C1", "C2", newName).executeOperation(decomposition);

            assertThat(decomposition.getClusters()).containsOnlyKeys(newName);
            assertThat(decomposition.getCluster(newName).getElementsIDs())
                    .containsExactlyInAnyOrder((short) 1, (short) 2, (short) 3);
        }

        /**
         * {@code merge} builds a fresh {@code Partition} rather than growing one of the inputs,
         * so any metrics already computed for either input are discarded. Recorded in the gap
         * analysis; {@link DecompositionSnapshot} deliberately ignores metrics so that this does
         * not fail every merge round-trip for an unrelated reason.
         */
        @Test
        @DisplayName("discards the metrics of both inputs")
        void discardsInputMetrics() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 2)
                    .build();
            decomposition.getCluster("C1").addMetric("cohesion", 0.5);
            decomposition.getCluster("C2").addMetric("cohesion", 0.25);

            new MergePartitionsOperation("C1", "C2", "M").executeOperation(decomposition);

            assertThat(decomposition.getCluster("M").getMetrics()).isEmpty();
        }
    }

    @Nested
    @DisplayName("coupling dependencies")
    class CouplingDependencies {

        @Test
        @DisplayName("redirects a third party's dependencies onto the merged name")
        void redirectsThirdPartyDependencies() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 3)
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 1)
                    .dependency("C3", "C2", 3)
                    .build();

            new MergePartitionsOperation("C1", "C2", "M").executeOperation(decomposition);

            // Both source keys collapse into one, since both now name the same cluster.
            assertThat(dependenciesOf(decomposition, "C3"))
                    .containsOnlyKeys("M")
                    .containsEntry("M", setOf((short) 1, (short) 3));
        }

        @Test
        @DisplayName("leaves a third party's unrelated dependencies alone")
        void leavesUnrelatedDependencies() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 3)
                    .cluster("C3", 5)
                    .cluster("C4", 9)
                    .dependency("C3", "C1", 1)
                    .dependency("C3", "C4", 9)
                    .build();

            new MergePartitionsOperation("C1", "C2", "M").executeOperation(decomposition);

            assertThat(dependenciesOf(decomposition, "C3"))
                    .containsOnlyKeys("M", "C4")
                    .containsEntry("C4", Collections.singleton((short) 9));
        }

        /**
         * <b>The defect, asserted as it should behave.</b> Disabled so the suite stays green;
         * enable it the day {@code merge} is fixed.
         *
         * <p>C1 reaches entity 9 in the untouched cluster C4. After merging C1 and C2 into M,
         * M ought to inherit that reach — the entity is still in another cluster, so it is still
         * a distributed transaction. Instead {@code merge} constructs a fresh {@code Partition}
         * and runs its transfer loop before adding it to the decomposition, so the dependency is
         * silently dropped and the decomposition under-reports its own coupling.
         *
         * <p>See {@code ../implementation/docs/gap-analysis.md}. The companion test below pins
         * what the code does today, so either a fix or a regression shows up as a red test.
         */
        @Test
        @Disabled("Known defect: merge() drops the merged cluster's outgoing coupling "
                + "dependencies. See ../implementation/docs/gap-analysis.md")
        @DisplayName("carries the inputs' outgoing dependencies onto the merged cluster")
        void mergePreservesOutgoingDependencies() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 3)
                    .cluster("C4", 9)
                    .dependency("C1", "C4", 9)
                    .build();

            new MergePartitionsOperation("C1", "C2", "M").executeOperation(decomposition);

            assertThat(dependenciesOf(decomposition, "M"))
                    .containsEntry("C4", Collections.singleton((short) 9));
        }

        /** Documents the current, lossy output of the case above. Not an endorsement. */
        @Test
        @DisplayName("in fact drops the inputs' outgoing dependencies")
        void mergeDropsOutgoingDependencies() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 3)
                    .cluster("C4", 9)
                    .dependency("C1", "C4", 9)
                    .build();

            new MergePartitionsOperation("C1", "C2", "M").executeOperation(decomposition);

            assertThat(dependenciesOf(decomposition, "M")).isEmpty();
        }

        /**
         * The same loss, for a dependency between the two clusters being merged. Here it is
         * arguably the right outcome — an intra-cluster reach is no longer a distributed
         * transaction — but it is reached by accident rather than by the self-edge suppression
         * in {@code Partition.transferCouplingDependencies}, which never gets to run for the
         * merged cluster.
         */
        @Test
        @DisplayName("drops a dependency between the two merged clusters")
        void dropsDependencyBetweenMergedClusters() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 3)
                    .dependency("C1", "C2", 3)
                    .build();

            new MergePartitionsOperation("C1", "C2", "M").executeOperation(decomposition);

            assertThat(dependenciesOf(decomposition, "M")).isEmpty();
        }
    }

    @Nested
    @DisplayName("guards")
    class Guards {

        @Test
        @DisplayName("rejects a merged name belonging to a third cluster, changing nothing")
        void rejectsExistingThirdClusterName() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 2)
                    .cluster("C3", 3)
                    .dependency("C3", "C1", 1)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            assertThatThrownBy(() -> new MergePartitionsOperation("C1", "C2", "C3").executeOperation(decomposition))
                    .isInstanceOf(KeyAlreadyExistsException.class)
                    .hasMessageContaining("C3");

            DecompositionSnapshot after = DecompositionSnapshot.of(decomposition);
            assertThat(after.clusters()).isEqualTo(before.clusters());
            assertThat(after.dependencies()).isEqualTo(before.dependencies());
        }

        @ParameterizedTest(name = "merging {0} and {1}")
        @CsvSource({"Absent,C2", "C1,Absent"})
        @DisplayName("fails with Error when either cluster does not exist")
        void failsOnUnknownCluster(String cluster1, String cluster2) {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1)
                    .cluster("C2", 2)
                    .build();

            assertThatThrownBy(() -> new MergePartitionsOperation(cluster1, cluster2, "M").executeOperation(decomposition))
                    .isInstanceOf(Error.class)
                    .hasMessageContaining("Absent");
        }
    }

    @Nested
    @DisplayName("undo")
    class Undo {

        /**
         * All three name shapes, which is the point: the {@code :47} branch picks which side to
         * split back off so the following rename has a free target, and for
         * {@code newName == "C2"} that rename is a self-rename. The three cases exercise the
         * else-branch with a fresh name, the if-branch, and the else-branch's self-rename path.
         */
        @ParameterizedTest(name = "merged into \"{0}\"")
        @CsvSource({"M", "C1", "C2"})
        @DisplayName("restores both original clusters")
        void undoRestoresBothClusters(String newName) {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 3)
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 1)
                    .dependency("C3", "C2", 3)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            MergePartitionsOperation operation = new MergePartitionsOperation("C1", "C2", newName);
            operation.execute(decomposition);
            operation.undo(decomposition);

            DecompositionSnapshot after = DecompositionSnapshot.of(decomposition);
            assertThat(after.clusters()).isEqualTo(before.clusters());
            assertThat(after.dependencies()).isEqualTo(before.dependencies());
        }

        /**
         * {@code storeState} is reached only from {@code execute} ({@code :29}), so for merge
         * alone the {@code executeOperation}-then-{@code undo} pairing the other four operations
         * support does not work: {@code cluster1Entities} is still null and
         * {@code SplitPartitionsOperation} dereferences it.
         */
        @Test
        @DisplayName("fails when the merge was applied without execute")
        void undoWithoutExecuteFails() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 3)
                    .build();

            MergePartitionsOperation operation = new MergePartitionsOperation("C1", "C2", "M");
            operation.executeOperation(decomposition);

            assertThat(operation.getCluster1Entities()).isNull();
            assertThatThrownBy(() -> operation.undo(decomposition))
                    .isInstanceOf(NullPointerException.class);
        }

        /**
         * {@code storeState}'s {@code if (cluster1Entities == null)} guard
         * ({@code MergeOperation.java:31}) is what makes this work: {@code redo} goes through
         * {@code executeOperation} and so never re-captures, and even if it did,
         * {@code execute} would decline to overwrite. Were the capture repeated on the
         * post-undo state, the third-name shape would throw on the second undo.
         */
        @Test
        @DisplayName("round-trips again after a redo, reusing the state captured first time")
        void redoThenUndoRestoresOriginal() {
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 3)
                    .cluster("C3", 5)
                    .dependency("C3", "C1", 1)
                    .build();
            DecompositionSnapshot before = DecompositionSnapshot.of(decomposition);

            MergePartitionsOperation operation = new MergePartitionsOperation("C1", "C2", "M");
            operation.execute(decomposition);
            DecompositionSnapshot merged = DecompositionSnapshot.of(decomposition);
            String capturedCluster1 = operation.getCluster1Entities();
            String capturedCluster2 = operation.getCluster2Entities();

            operation.undo(decomposition);
            assertThat(DecompositionSnapshot.of(decomposition).clusters()).isEqualTo(before.clusters());

            operation.redo(decomposition);
            assertThat(DecompositionSnapshot.of(decomposition).clusters()).isEqualTo(merged.clusters());

            // The redo did not re-capture, so the second undo has the original membership.
            assertThat(operation.getCluster1Entities()).isEqualTo(capturedCluster1);
            assertThat(operation.getCluster2Entities()).isEqualTo(capturedCluster2);

            operation.undo(decomposition);
            assertThat(DecompositionSnapshot.of(decomposition).clusters()).isEqualTo(before.clusters());
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

            new MergePartitionsOperation("C1", "C2", "M").executeOperation(decomposition);

            assertThat(decomposition.getClusters()).containsOnlyKeys("M");
        }

        /** Merge reports the whole merged cluster, including entities that never moved. */
        @Test
        @DisplayName("reports every entity in the merged cluster")
        void reportsWholeMergedCluster() {
            RecordingRepresentationInformation recorder = new RecordingRepresentationInformation();
            PartitionsDecomposition decomposition = DecompositionFixture.with()
                    .cluster("C1", 1, 2)
                    .cluster("C2", 3)
                    .representationInformation(recorder)
                    .build();

            new MergePartitionsOperation("C1", "C2", "M").executeOperation(decomposition);

            assertThat(recorder.onlyRemovedEntityIDs())
                    .containsExactly((short) 1, (short) 2, (short) 3);
        }
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
