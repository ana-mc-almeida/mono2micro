package pt.ist.socialsoftware.mono2micro.utils.mojoCalculator.src.main.java;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * {@code MoJoCalculator} — the distance between two partitions of the same objects.
 *
 * <p>MoJo counts the cheapest sequence of <b>Move</b> (one object to another cluster) and
 * <b>Join</b> (two clusters into one) operations turning partition A into partition B; MoJoFM
 * reports that as a percentage of the worst case, so 100 means identical
 * ({@code mojofmValue}, {@code MoJoCalculator.java:331-335}). It is the backend's answer to
 * "how close is this decomposition to the expert one", reached through
 * {@code MoJoFM.getMojoValue} ({@code MoJoFM.java:394}).
 *
 * <p>This is vendored third-party code from 2004 — 660 lines that were at 0% coverage, and the
 * single biggest drag on the module's coverage gate. It is also, conveniently, pure computation:
 * no Spring, no Mongo, no Docker, just two files in and a number out. The tests therefore go
 * wide across the entry points on purpose, because each one reaches a different private block
 * ({@code tagAssignment} modes, {@code calculateEvoCost}, {@code edgeCost},
 * {@code readSourceBunchFile}) that nothing else in the suite touches.
 *
 * <p><b>Expected values are derived by hand</b>, from the cost formula
 * {@code moves + numberOfClustersInA - nonEmptyGroups} ({@code calculateCost}, {@code :376-403}),
 * and each carries the operation sequence it stands for. Fixtures are kept tiny so that stays
 * possible. A disagreement between a hand-derived value and the implementation is a finding to
 * investigate, not a number to overwrite.
 *
 * <p>Two defects are pinned below rather than fixed: this file is vendored, and changing it
 * would diverge the fork. See {@link Defects}.
 */
class MoJoCalculatorTest {

    @TempDir
    Path tmp;

    @Nested
    @DisplayName("agreement")
    class Agreement {

        @Test
        @DisplayName("costs nothing and scores 100 when the partitions are identical")
        void identicalPartitionsCostNothing() throws IOException {
            // No object is in the wrong place and no two clusters need joining, so the cheapest
            // sequence is empty: moves 0, and every cluster in A maps onto its own group, so
            // numberOfClustersInA - nonEmptyGroups is 0 too.
            String source = rsf("src.rsf", "C1 1", "C1 2", "C2 3");
            String target = rsf("tgt.rsf", "C1 1", "C1 2", "C2 3");

            assertThat(new MoJoCalculator(source, target, null).mojo()).isZero();
            assertThat(new MoJoCalculator(source, target, null).mojofm()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("ignores cluster names, comparing only the grouping")
        void clusterNamesDoNotMatter() throws IOException {
            // Same partition, different names on both sides. MoJo is defined over groupings, so
            // the distance is still 0 -- worth pinning because MoJoFM feeds it generated cluster
            // names that need not agree between the two decompositions.
            String source = rsf("src.rsf", "Alpha 1", "Alpha 2", "Beta 3");
            String target = rsf("tgt.rsf", "X 1", "X 2", "Y 3");

            assertThat(new MoJoCalculator(source, target, null).mojo()).isZero();
            assertThat(new MoJoCalculator(source, target, null).mojofm()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("the two operations")
    class Operations {

        @Test
        @DisplayName("charges one Move for a single misplaced object")
        void oneMisplacedObjectCostsOneMove() throws IOException {
            // A: {1,2} {3}   B: {1} {2,3}
            // Cluster {1,2} is tagged for B's first group (1) and B's second (2), so its
            // maxtag is 1 and it pays gettotalTags() - getMaxtag() = 1 Move. Both A-clusters
            // land in distinct non-empty groups, so no Join is charged. Total 1.
            String source = rsf("src.rsf", "A1 1", "A1 2", "A2 3");
            String target = rsf("tgt.rsf", "B1 1", "B2 2", "B2 3");

            assertThat(new MoJoCalculator(source, target, null).mojo()).isEqualTo(1L);
        }

        @Test
        @DisplayName("charges one Join for two clusters that belong together")
        void twoClustersThatBelongTogetherCostOneJoin() throws IOException {
            // A: {1} {2}   B: {1,2}
            // No object is misplaced, so moves is 0. Both A-clusters map to B's single group,
            // leaving numberOfClustersInA(2) - nonEmptyGroups(1) = 1 Join. This is the case that
            // distinguishes the two operations: a Move-only reading would score it 0.
            String source = rsf("src.rsf", "A1 1", "A2 2");
            String target = rsf("tgt.rsf", "B1 1", "B1 2");

            assertThat(new MoJoCalculator(source, target, null).mojo()).isEqualTo(1L);
        }

        @Test
        @DisplayName("assigns each cluster a distinct group, so a split cluster costs only its Move")
        void aSplitClusterCostsOnlyItsMove() throws IOException {
            // A: {1,2} {3,4}   B: {1,2,3} {4}
            // A1={1,2} sits entirely in B1: 0 moves. A2={3,4} is split between B1 and B2, keeps
            // its majority tag and pays 1 Move. No Join: groups are assigned by maximum bipartite
            // matching (maxbipartiteMatching, :299-325), not by each cluster taking its own
            // maxtag, so A1 takes B1 and A2 takes B2 rather than both claiming B1. That leaves
            // numberOfClustersInA(2) - nonEmptyGroups(2) = 0 and a total of 1.
            //
            // Derived as 2 on first writing, by assuming both clusters claim B1. The matching
            // step exists precisely to stop that, and it is why MoJo does not double-charge a
            // cluster that merely straddles a boundary.
            String source = rsf("src.rsf", "A1 1", "A1 2", "A2 3", "A2 4");
            String target = rsf("tgt.rsf", "B1 1", "B1 2", "B1 3", "B2 4");

            assertThat(new MoJoCalculator(source, target, null).mojo()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("MoJoFM as a percentage")
    class MoJoFmScale {

        @Test
        @DisplayName("stays within 0 and 100")
        void alwaysWithinRange() throws IOException {
            // The worst partition available for these objects: everything in one cluster against
            // an all-singletons target.
            String source = rsf("src.rsf", "A1 1", "A1 2", "A1 3", "A1 4");
            String target = rsf("tgt.rsf", "B1 1", "B2 2", "B3 3", "B4 4");

            double value = new MoJoCalculator(source, target, null).mojofm();

            assertThat(value).isBetween(0.0, 100.0);
        }

        @Test
        @DisplayName("scores a closer partition strictly higher than a further one")
        void isMonotoneInDistance() throws IOException {
            // The property that makes the number usable for ranking decompositions, which is
            // exactly what the comparison tool does with it.
            String target = rsf("tgt.rsf", "B1 1", "B1 2", "B1 3", "B2 4", "B2 5", "B2 6");
            String close = rsf("close.rsf", "A1 1", "A1 2", "A1 3", "A2 4", "A2 5", "A3 6");
            String far = rsf("far.rsf", "A1 1", "A2 2", "A3 3", "A4 4", "A5 5", "A6 6");

            double closeValue = new MoJoCalculator(close, target, null).mojofm();
            double farValue = new MoJoCalculator(far, target, null).mojofm();

            assertThat(closeValue).isGreaterThan(farValue);
        }

        @Test
        @DisplayName("rounds to two decimal places")
        void roundsToTwoDecimals() throws IOException {
            // mojofmValue multiplies by 10000, rint()s and divides by 100 (:334), so the result
            // carries at most two decimals.
            //
            // A: {1} {2} {3,4}   B: {1,2,3,4}. Collapsing 3 clusters into 1 costs 2 Joins, and
            // the worst case for 4 objects against a single target cluster is 3, so
            // 1 - 2/3 = 0.3333 -> 33.33.
            String source = rsf("src.rsf", "A1 1", "A2 2", "A3 3", "A3 4");
            String target = rsf("tgt.rsf", "B1 1", "B1 2", "B1 3", "B1 4");

            double value = new MoJoCalculator(source, target, null).mojofm();

            assertThat(value).isEqualTo(Math.rint(value * 100) / 100);
            assertThat(value).isEqualTo(33.33, within(1e-9));
        }
    }

    @Nested
    @DisplayName("the other entry points")
    class OtherEntryPoints {

        @Test
        @DisplayName("mojoplus tags with the MoJoPlus rule")
        void mojoPlusRunsItsOwnTagAssignment() throws IOException {
            // Same inputs as the single-Move case; mojoplus() takes tagAssignment("MoJoPlus"),
            // a branch mojo() never reaches.
            String source = rsf("src.rsf", "A1 1", "A1 2", "A2 3");
            String target = rsf("tgt.rsf", "B1 1", "B2 2", "B2 3");

            assertThat(new MoJoCalculator(source, target, null).mojoplus()).isEqualTo(1L);
        }

        @Test
        @DisplayName("mojoev scores identical partitions at 100")
        void mojoEvScoresIdenticalPartitions() throws IOException {
            // Reaches calculateEvoCost/evoMaxDistanceTo, which nothing else exercises.
            String source = rsf("src.rsf", "C1 1", "C1 2", "C2 3");
            String target = rsf("tgt.rsf", "C1 1", "C1 2", "C2 3");

            assertThat(new MoJoCalculator(source, target, null).mojoev()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("edgemojo adds a cost for edges crossing clusters")
        void edgeMojoAddsEdgeCost() throws IOException {
            // The relation file's first token is the relation name and is discarded (:628); the
            // remaining pair is the edge. Identical partitions cost 0 in MoJo terms, so whatever
            // edgemojo returns here is the edge cost alone -- and it must not be negative.
            String source = rsf("src.rsf", "C1 1", "C2 2");
            String target = rsf("tgt.rsf", "C1 1", "C2 2");
            String relations = write("rel.rsf", "call 1 2");

            double value = new MoJoCalculator(source, target, relations).edgemojo();

            assertThat(value).isGreaterThanOrEqualTo(0.0);
        }

        @Test
        @DisplayName("reads the Bunch format when the file is named .bunch")
        void readsBunchFormat() throws IOException {
            // isBunch() switches on the extension alone (:819-827), so the .bunch readers are
            // unreachable unless a fixture is named for them. Format is `cluster = a,b,c`.
            String source = write("src.bunch", "A1 = 1,2", "A2 = 3");
            String target = write("tgt.bunch", "B1 = 1,2", "B2 = 3");

            assertThat(new MoJoCalculator(source, target, null).mojo()).isZero();
        }

        @Test
        @DisplayName("mixes formats, taking each side's reader from its own extension")
        void mixesRsfAndBunch() throws IOException {
            // Source and target are read independently (:209-225), so the pairing is legal.
            String source = write("src.bunch", "A1 = 1,2", "A2 = 3");
            String target = rsf("tgt.rsf", "B1 1", "B1 2", "B2 3");

            assertThat(new MoJoCalculator(source, target, null).mojo()).isZero();
        }

        @Test
        @DisplayName("setVerbose(true) logs without changing the result")
        void verboseDoesNotChangeTheResult() throws IOException {
            // put() is guarded by the verbose flag (:829-832), so the logging lines stay dark
            // otherwise. The extra objects on each side also trigger the "were not found"
            // warnings, which is where most of those lines live.
            String source = rsf("src.rsf", "A1 1", "A1 2", "A2 9");
            String target = rsf("tgt.rsf", "B1 1", "B1 2", "B2 8");

            MoJoCalculator quiet = new MoJoCalculator(source, target, null);
            MoJoCalculator loud = new MoJoCalculator(source, target, null);
            loud.setVerbose(true);

            assertThat(loud.mojo()).isEqualTo(quiet.mojo());
        }
    }

    @Nested
    @DisplayName("input handling")
    class InputHandling {

        @Test
        @DisplayName("ignores lines whose first token is not 'contain'")
        void ignoresNonContainLines() throws IOException {
            // The reader skips any 3-token line not starting with `contain` (:494-496), so a
            // relation line mixed into a partition file is dropped rather than misread.
            String source = write("src.rsf", "contain A1 1", "depends A1 2", "contain A2 3");
            String target = rsf("tgt.rsf", "B1 1", "B2 3");

            assertThat(new MoJoCalculator(source, target, null).mojo()).isZero();
        }

        @Test
        @DisplayName("ignores objects missing from the other partition")
        void ignoresObjectsMissingFromTheOtherSide() throws IOException {
            // Object 9 exists only in A and 8 only in B; both are dropped with a warning, and
            // the distance is computed over the shared objects. MoJoFM relies on this -- it
            // filters to shared entities itself (MoJoFM.java:356), so the two must agree.
            String source = rsf("src.rsf", "A1 1", "A1 2", "A1 9");
            String target = rsf("tgt.rsf", "B1 1", "B1 2", "B1 8");

            assertThat(new MoJoCalculator(source, target, null).mojo()).isZero();
        }

        @Test
        @DisplayName("strips quotes from target cluster names")
        void stripsQuotedTargetClusterNames() throws IOException {
            // The target reader unquotes names (:702-709); the source reader does not. Quoted
            // and unquoted spellings of one name must therefore be the same cluster.
            String source = rsf("src.rsf", "A1 1", "A1 2");
            String target = write("tgt.rsf", "contain \"B1\" 1", "contain B1 2");

            assertThat(new MoJoCalculator(source, target, null).mojo()).isZero();
        }

        @Test
        @DisplayName("rejects a malformed line")
        void rejectsMalformedLine() throws IOException {
            // Any line without exactly 3 tokens is fatal (:489-493).
            String source = write("src.rsf", "contain A1");
            String target = rsf("tgt.rsf", "B1 1");

            assertThatThrownBy(() -> new MoJoCalculator(source, target, null).mojo())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Incorrect RSF format");
        }

        @Test
        @DisplayName("reports a missing file by name")
        void reportsMissingFile() throws IOException {
            String target = rsf("tgt.rsf", "B1 1");
            String missing = tmp.resolve("absent.rsf").toString();

            assertThatThrownBy(() -> new MoJoCalculator(missing, target, null).mojo())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Could not open");
        }
    }

    /**
     * Defects in the vendored code, pinned so a change shows up as a red test.
     *
     * <p>Following this repository's convention (see {@code MergePartitionsOperationTest}), a
     * known defect gets two tests: a {@code @Disabled} one asserting what it <em>should</em> do,
     * and an enabled one documenting what it does today. The NPE below is the exception, and the
     * reason is given there.
     */
    @Nested
    @DisplayName("defects")
    class Defects {

        /**
         * <b>Characterization only — deliberately without a {@code @Disabled} twin.</b>
         *
         * <p>{@code commonPrep} handles a null {@code targetFile} by reading {@code targetStream}
         * and skipping the file readers with {@code else if} ({@code :209-216}). The source
         * branch ten lines below is missing that {@code else} ({@code :219-225}): it calls
         * {@code readSourceStream()} and then falls into {@code isBunch(sourceFile)}, which
         * dereferences the null at {@code :820}.
         *
         * <p>There is no companion test asserting the fix, because the correct behaviour is not
         * decidable yet. Adding the missing {@code else} alone would leave the stream path
         * reading a source that {@code readSourceStream} populates and {@code readSourceRSFfile}
         * would then populate again — a double read that inflates {@code numberOfObjectsInA} and
         * silently changes the score. The NPE is currently preventing a wrong number rather than
         * merely being one, so "what it should do" is a design decision, not a missing keyword.
         * Writing that guess into a {@code @Disabled} test would dress it up as a specification.
         *
         * <p>The practical consequence: the {@code sourceStream}/{@code targetStream} fields are
         * not a usable in-memory alternative to files for {@code mojo()} or {@code mojofm()}, and
         * these tests write real fixtures instead. {@code readSourceStream} even builds its error
         * messages from {@code sourceFile}, always null on the only path that reaches it, which
         * suggests the path has never run.
         */
        @Test
        @DisplayName("in fact throws when the source is supplied as a stream")
        void streamOnlySourceThrows() throws IOException {
            String target = rsf("tgt.rsf", "B1 1");
            MoJoCalculator calculator = new MoJoCalculator(null, target, null);
            calculator.sourceStream = new java.io.ByteArrayOutputStream();
            calculator.sourceStream.write("contain A1 1\n".getBytes());

            assertThatThrownBy(calculator::mojofm).isInstanceOf(NullPointerException.class);
        }

        /**
         * <b>The defect, asserted as it should behave.</b> Disabled so the suite stays green.
         *
         * <p>{@code MoJo.executeMojo} swallows every {@code RuntimeException} to stdout and falls
         * through to {@code return 0.0} ({@code MoJo.java:89-94}). That is coherent for the CLI
         * the class was written as — {@code main} prints, a human reads. It is wrong at
         * {@code MoJoFM.java:394}, which calls it as a library inside a Spring service where
         * nothing reads stdout, because <b>0.0 is also a legitimate MoJoFM value</b> meaning
         * maximally dissimilar. A missing file and a genuinely terrible decomposition are
         * therefore indistinguishable.
         *
         * <p>This matters beyond tidiness: {@code Constants.MOJO_RESOURCES_PATH} is a
         * <em>relative</em> path, so every comparison silently scores 0.0 whenever the working
         * directory is not {@code backend/}.
         *
         * <p>The fix belongs at the call site, not in this vendored file — detect the failure, or
         * return -1 as {@code MoJoFM} already does for its NaN guards ({@code MoJoFM:278-300}).
         * Enable this test the day that happens.
         */
        @Test
        @Disabled("Known defect: MoJo.executeMojo reports a missing file as 0.0, which is also a "
                + "valid score. Fix belongs at MoJoFM.java:394. See ../implementation/docs/decisions.md")
        @DisplayName("distinguishes a failed run from a legitimate score of 0.0")
        void missingFileIsDistinguishableFromZero() throws IOException {
            String target = rsf("tgt.rsf", "B1 1");
            String missing = tmp.resolve("absent.rsf").toString();

            double value = new MoJo().executeMojo(new String[]{missing, target, "-fm"});

            assertThat(value).isNegative();
        }

        /** Documents the current, silent output of the case above. Not an endorsement. */
        @Test
        @DisplayName("in fact returns 0.0 for a missing file, exactly as for the worst score")
        void missingFileReturnsZero() throws IOException {
            String target = rsf("tgt.rsf", "B1 1");
            String missing = tmp.resolve("absent.rsf").toString();

            double value = new MoJo().executeMojo(new String[]{missing, target, "-fm"});

            assertThat(value).isEqualTo(0.0);
        }

        @Test
        @DisplayName("returns the MoJoFM value for a well-formed -fm run")
        void executeMojoReturnsMojoFm() throws IOException {
            // The one argument shape MoJoFM uses. Note only -fm and the no-flag case return a
            // value at all; -b, -b+, -m+ and -e print and return 0.0 (MoJo.java:32-77), so they
            // are not exercised here. Nor are malformed argument lists: showerrormsg() ends in
            // System.exit(0) (MoJo.java:113), which would kill the surefire fork.
            String source = rsf("src.rsf", "C1 1", "C2 2");
            String target = rsf("tgt.rsf", "C1 1", "C2 2");

            assertThat(new MoJo().executeMojo(new String[]{source, target, "-fm"})).isEqualTo(100.0);
        }
    }

    /**
     * Writes an RSF partition file, one {@code contain <cluster> <object>} line per entry.
     *
     * <p>Entries are given as {@code "<cluster> <object>"} so the fixtures read as the partition
     * they describe; the {@code contain} prefix that {@code MoJoFM.getMojoValue} emits
     * ({@code MoJoFM.java:358-362}) is added here.
     */
    private String rsf(String name, String... clusterObjectPairs) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String pair : clusterObjectPairs)
            lines.add("contain " + pair);
        return write(name, lines.toArray(new String[0]));
    }

    /** Writes a fixture verbatim into the per-test temp directory and returns its path. */
    private String write(String name, String... lines) throws IOException {
        Path file = tmp.resolve(name);
        Files.write(file, String.join("\n", lines).concat("\n").getBytes());
        return file.toString();
    }
}
