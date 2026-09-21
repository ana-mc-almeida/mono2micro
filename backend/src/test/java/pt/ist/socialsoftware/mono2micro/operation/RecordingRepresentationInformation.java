package pt.ist.socialsoftware.mono2micro.operation;

import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.RepresentationInformation;
import pt.ist.socialsoftware.mono2micro.metrics.decompositionMetrics.DecompositionMetricCalculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * A {@link RepresentationInformation} that records the two hooks the operations call, and does
 * nothing else.
 *
 * <p>Every {@code executeOperation} ends by walking
 * {@code decomposition.getRepresentationInformations()} and calling either
 * {@code removeFunctionalitiesWithEntityIDs} or {@code renameClusterInFunctionalities}. Both are
 * <b>empty non-abstract</b> methods on the base class
 * ({@code RepresentationInformation.java:24,26}), and only {@code AccessesInformation} overrides
 * them — {@code RepositoryInformation} and {@code StructureInformation} inherit the no-ops. So
 * the call site can be exercised offline by a subclass that records its arguments, even though
 * {@code AccessesInformation}'s own implementation reaches for
 * {@code ContextManager.get().getBean(FunctionalityService.class)} and could not be.
 *
 * <p>This is a <b>recorder, not a stand-in</b>: it captures what each operation reports
 * downstream and makes no attempt to reproduce {@code AccessesInformation}'s semantics of
 * deleting functionalities or flagging the decomposition outdated. What it pins is the contract
 * — which entity IDs each operation considers affected — and that contract is not uniform.
 * {@code TransferPartitionsOperation} reports the IDs it was <em>asked</em> to move, including
 * any it silently skipped, while merge and split report actual cluster membership.
 */
class RecordingRepresentationInformation extends RepresentationInformation {

    static final String RECORDING = "RECORDING";

    private final List<Set<Short>> removedEntityIDs = new ArrayList<>();
    private final List<String> renames = new ArrayList<>();

    @Override
    public void removeFunctionalitiesWithEntityIDs(Decomposition decomposition, Set<Short> elements) {
        // Sorted and copied: the caller's set is built from live cluster state.
        removedEntityIDs.add(new TreeSet<>(elements));
    }

    @Override
    public void renameClusterInFunctionalities(String clusterName, String newName) {
        renames.add(clusterName + " -> " + newName);
    }

    /** One entry per call, in call order. */
    List<Set<Short>> removedEntityIDs() {
        return removedEntityIDs;
    }

    /** One {@code "old -> new"} entry per call, in call order. */
    List<String> renames() {
        return renames;
    }

    /** The entity IDs of the single call the operations under test make. */
    Set<Short> onlyRemovedEntityIDs() {
        if (removedEntityIDs.size() != 1)
            throw new AssertionError("Expected exactly one call, got " + removedEntityIDs);
        return removedEntityIDs.get(0);
    }

    @Override
    public String getType() {
        return RECORDING;
    }

    @Override
    public void deleteProperties() {
    }

    @Override
    public void setup(Decomposition decomposition) {
    }

    @Override
    public void update(Decomposition decomposition) {
    }

    @Override
    public void snapshot(Decomposition snapshotDecomposition, Decomposition decomposition) {
    }

    @Override
    public List<DecompositionMetricCalculator> getDecompositionMetrics() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getParameters() {
        return Collections.emptyList();
    }

    @Override
    public String getEdgeWeights(Decomposition decomposition) {
        return "{}";
    }

    @Override
    public String getSearchItems(Decomposition decomposition) {
        return "{}";
    }
}
