package pt.ist.socialsoftware.mono2micro.comparisonTool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.cluster.Partition;
import pt.ist.socialsoftware.mono2micro.comparisonTool.domain.Purity;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.element.DomainEntity;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * {@code Purity} — how much of each proposed cluster comes from a single expert cluster.
 *
 * <p>For every cluster in the <em>proposed</em> decomposition, purity finds the expert cluster it
 * overlaps most and scores {@code maxOverlap / proposedSize}; the decomposition's purity is the
 * total overlap over the total proposed size ({@code Purity.java:43-70}). Two consequences are
 * worth pinning, because they decide what the number can be used to argue:
 *
 * <ul>
 *   <li><b>Splitting is free.</b> Cutting an expert cluster into pieces leaves every piece
 *       perfectly pure, so purity stays 1.0. Purity alone cannot detect over-decomposition —
 *       see {@link Splitting#splittingAnExpertClusterIsNotPenalised()}.
 *   <li><b>Merging is what it punishes.</b> Pulling two expert clusters into one proposed
 *       cluster scores only the larger half.
 * </ul>
 *
 * <p>The class needs no Spring context, no Mongo and no Docker: {@code analyse} reads nothing but
 * {@code getClusters()}, and unlike its sibling {@code MoJoFM} it never touches
 * {@code Constants}, whose static initializer builds an {@code ApplicationContext}. That is the
 * whole reason this test is cheap and {@code MoJoFM}'s is not.
 *
 * <p>Note {@code Purity}'s constructor <em>is</em> the computation — {@code analyse} returns
 * {@code void} and results come back through getters.
 */
class PurityTest {

    /** Float comparison tolerance. Purity divides small ints, so the slack only absorbs binary representation. */
    private static final float TOLERANCE = 1e-6f;

    @Nested
    @DisplayName("agreement")
    class Agreement {

        @Test
        @DisplayName("scores 1.0 when both decompositions are identical")
        void identicalDecompositionsArePure() throws IOException {
            PartitionsDecomposition expert = decomposition(cluster("C1", 1, 2, 3), cluster("C2", 4, 5));
            PartitionsDecomposition proposed = decomposition(cluster("C1", 1, 2, 3), cluster("C2", 4, 5));

            Purity purity = new Purity(expert, proposed);

            assertThat(purity.getPurity()).isEqualTo(1.0f, within(TOLERANCE));
            assertThat(purity.getClusterPurityMap())
                    .containsEntry("C1", 1.0f)
                    .containsEntry("C2", 1.0f);
            assertThat(purity.getClusterMapping())
                    .containsEntry("C1", "C1")
                    .containsEntry("C2", "C2");
        }

        @Test
        @DisplayName("matches clusters by overlap, not by name")
        void matchesByOverlapNotName() throws IOException {
            // The same partition of the entities, with the two cluster names swapped. Purity
            // compares contents, so each proposed cluster maps to the differently-named expert
            // cluster holding its entities.
            PartitionsDecomposition expert = decomposition(cluster("C1", 1, 2), cluster("C2", 3, 4));
            PartitionsDecomposition proposed = decomposition(cluster("C2", 1, 2), cluster("C1", 3, 4));

            Purity purity = new Purity(expert, proposed);

            assertThat(purity.getPurity()).isEqualTo(1.0f, within(TOLERANCE));
            assertThat(purity.getClusterMapping())
                    .containsEntry("C2", "C1")
                    .containsEntry("C1", "C2");
        }
    }

    @Nested
    @DisplayName("splitting")
    class Splitting {

        @Test
        @DisplayName("is not penalised: every piece of a split cluster is perfectly pure")
        void splittingAnExpertClusterIsNotPenalised() throws IOException {
            // Expert keeps 1..4 together; the proposal cuts it in half. Each half draws entirely
            // from one expert cluster, so each scores 1.0 and so does the whole decomposition.
            // This is the metric's blind spot, asserted deliberately: purity cannot argue against
            // over-decomposition, which is why it is reported alongside MoJo rather than alone.
            PartitionsDecomposition expert = decomposition(cluster("C1", 1, 2, 3, 4));
            PartitionsDecomposition proposed = decomposition(cluster("P1", 1, 2), cluster("P2", 3, 4));

            Purity purity = new Purity(expert, proposed);

            assertThat(purity.getPurity()).isEqualTo(1.0f, within(TOLERANCE));
            assertThat(purity.getClusterPurityMap())
                    .containsEntry("P1", 1.0f)
                    .containsEntry("P2", 1.0f);
            assertThat(purity.getClusterMapping())
                    .containsEntry("P1", "C1")
                    .containsEntry("P2", "C1");
        }
    }

    @Nested
    @DisplayName("merging")
    class Merging {

        @Test
        @DisplayName("scores a merged cluster as its largest expert contribution")
        void mergingTwoExpertClustersCostsTheSmallerHalf() throws IOException {
            // Proposed "M" holds 1,2,3 from C1 and 4 from C2: maxOverlap 3 of 4 entities.
            PartitionsDecomposition expert = decomposition(cluster("C1", 1, 2, 3), cluster("C2", 4));
            PartitionsDecomposition proposed = decomposition(cluster("M", 1, 2, 3, 4));

            Purity purity = new Purity(expert, proposed);

            assertThat(purity.getPurity()).isEqualTo(0.75f, within(TOLERANCE));
            assertThat(purity.getClusterPurityMap()).containsEntry("M", 0.75f);
            assertThat(purity.getClusterMapping()).containsEntry("M", "C1");
        }

        @Test
        @DisplayName("scores an evenly split cluster at 0.5")
        void halfOverlapScoresAHalf() throws IOException {
            // 2 of 4 entities from each expert cluster. Ties go to the first cluster reaching
            // maxOverlap, since the loop uses a strict > (Purity.java:56).
            PartitionsDecomposition expert = decomposition(cluster("C1", 1, 2), cluster("C2", 3, 4));
            PartitionsDecomposition proposed = decomposition(cluster("M", 1, 2, 3, 4));

            Purity purity = new Purity(expert, proposed);

            assertThat(purity.getPurity()).isEqualTo(0.5f, within(TOLERANCE));
            assertThat(purity.getClusterPurityMap()).containsEntry("M", 0.5f);
        }

        @Test
        @DisplayName("weights each cluster by size, not one vote per cluster")
        void overallPurityIsWeightedByClusterSize() throws IOException {
            // Pure "P1" holds 4 entities; impure "P2" holds 2, one from each expert cluster.
            // Per-cluster purities are 1.0 and 0.5, but the total is 5/6, not their mean 0.75 --
            // Purity.java:67-70 sums overlaps and sizes rather than averaging the ratios.
            PartitionsDecomposition expert = decomposition(cluster("C1", 1, 2, 3, 4, 5), cluster("C2", 6));
            PartitionsDecomposition proposed = decomposition(cluster("P1", 1, 2, 3, 4), cluster("P2", 5, 6));

            Purity purity = new Purity(expert, proposed);

            assertThat(purity.getPurity()).isEqualTo(5.0f / 6.0f, within(TOLERANCE));
            assertThat(purity.getClusterPurityMap())
                    .containsEntry("P1", 1.0f)
                    .containsEntry("P2", 0.5f);
        }
    }

    @Nested
    @DisplayName("degenerate inputs")
    class DegenerateInputs {

        @Test
        @DisplayName("scores an empty proposed cluster 0 rather than dividing by zero")
        void emptyProposedClusterScoresZero() throws IOException {
            // Guards the size == 0 ternary at Purity.java:62. An empty cluster is the state
            // `split` leaves behind, so this is reachable in practice, not only in theory.
            PartitionsDecomposition expert = decomposition(cluster("C1", 1, 2));
            PartitionsDecomposition proposed = decomposition(cluster("P1", 1, 2), emptyCluster("P2"));

            Purity purity = new Purity(expert, proposed);

            assertThat(purity.getClusterPurityMap()).containsEntry("P2", 0.0f);
            // P2 contributes nothing to either total, so the decomposition stays perfectly pure.
            assertThat(purity.getPurity()).isEqualTo(1.0f, within(TOLERANCE));
        }

        @Test
        @DisplayName("scores an empty proposed decomposition 0 rather than dividing by zero")
        void emptyProposedDecompositionScoresZero() throws IOException {
            // Guards the totalEntities == 0 ternary at Purity.java:70.
            PartitionsDecomposition expert = decomposition(cluster("C1", 1, 2));
            PartitionsDecomposition proposed = decomposition();

            Purity purity = new Purity(expert, proposed);

            assertThat(purity.getPurity()).isEqualTo(0.0f, within(TOLERANCE));
            assertThat(purity.getClusterPurityMap()).isEmpty();
            assertThat(purity.getClusterMapping()).isEmpty();
        }

        @Test
        @DisplayName("maps a cluster sharing no entities to null")
        void disjointEntitiesMapToNull() throws IOException {
            // maxOverlap never exceeds 0, so bestMatchingExpertCluster is never assigned and the
            // mapping holds a null VALUE (Purity.java:53-60). Callers that iterate clusterMapping
            // -- a report, or the frontend -- must expect that, so it is pinned rather than left
            // to be discovered. Note containsEntry(k, null) needs the key present, which is the
            // distinction being asserted: present-and-null, not absent.
            PartitionsDecomposition expert = decomposition(cluster("C1", 1, 2));
            PartitionsDecomposition proposed = decomposition(cluster("P1", 8, 9));

            Purity purity = new Purity(expert, proposed);

            assertThat(purity.getPurity()).isEqualTo(0.0f, within(TOLERANCE));
            assertThat(purity.getClusterPurityMap()).containsEntry("P1", 0.0f);
            assertThat(purity.getClusterMapping()).containsKey("P1").containsEntry("P1", null);
        }
    }

    /**
     * A {@code PartitionsDecomposition} holding the given clusters.
     *
     * <p>{@code operation.DecompositionFixture} does this job already but is package-private, and
     * widening it for one caller would trade a real boundary for a little duplication. What is
     * needed here is also narrower: purity reads only names and entity IDs, never dependencies or
     * history.
     */
    private static PartitionsDecomposition decomposition(Partition... clusters) {
        PartitionsDecomposition decomposition = new PartitionsDecomposition();
        for (Partition cluster : clusters)
            decomposition.addCluster(cluster);
        return decomposition;
    }

    /** A cluster holding the given entity IDs. {@code int} to spare every call site a cast. */
    private static Partition cluster(String name, int... entityIDs) {
        Partition partition = new Partition(name);
        for (int entityID : entityIDs)
            partition.addElement(new DomainEntity((short) entityID, "E" + entityID));
        return partition;
    }

    private static Partition emptyCluster(String name) {
        return new Partition(name);
    }
}
