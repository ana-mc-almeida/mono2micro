package pt.ist.socialsoftware.mono2micro.operation;

import pt.ist.socialsoftware.mono2micro.cluster.Partition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.PartitionsDecomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.RepresentationInformation;
import pt.ist.socialsoftware.mono2micro.element.DomainEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds a {@link PartitionsDecomposition} for a unit test, as data.
 *
 * <pre>
 * PartitionsDecomposition decomposition = DecompositionFixture.with()
 *         .cluster("C1", 1, 2)
 *         .cluster("C2", 3)
 *         .dependency("C2", "C1", 1)   // C2 reaches entity 1, which lives in C1
 *         .build();
 * </pre>
 *
 * <p>The operations under test need no Spring context, no Mongo and no Docker, because
 * everything they touch is a plain object: {@code PartitionsDecomposition}'s no-argument
 * constructor only sets a type string, and {@code clusters}, {@code metrics} and
 * {@code representationInformations} all have inline initializers, so a bare instance has
 * non-null empty collections.
 *
 * <p>The one thing it does not have is a {@code History}, which {@code Operation.execute}
 * dereferences — so {@link #build()} attaches an {@link InMemoryHistory}. Tests that want the
 * opposite (a decomposition that fails loudly if anything reaches for history) use
 * {@link #noHistory()}.
 *
 * <p>The production types are used directly rather than wrapped: the point is to exercise
 * {@code Partition} and {@code DomainEntity} as the operations see them.
 */
final class DecompositionFixture {

    private final PartitionsDecomposition decomposition = new PartitionsDecomposition();

    /**
     * One {@link DomainEntity} instance per ID, shared across clusters.
     *
     * <p>{@code Element} declares no {@code equals}/{@code hashCode}, so a {@code HashSet} of
     * elements uses identity while {@code Cluster}'s own lookups all compare {@code getId()}.
     * Memoizing here means a test can assert that an operation <em>moved</em> an element rather
     * than copying it, by checking instance identity — which would catch a refactor that
     * rebuilt elements instead of transferring them.
     */
    private final Map<Short, DomainEntity> entities = new HashMap<>();

    private boolean withHistory = true;

    private DecompositionFixture() {
    }

    static DecompositionFixture with() {
        return new DecompositionFixture();
    }

    /** A cluster holding the given entity IDs. {@code int} to spare every call site a cast. */
    DecompositionFixture cluster(String name, int... entityIDs) {
        Partition partition = new Partition(name);
        for (int entityID : entityIDs)
            partition.addElement(entity((short) entityID));
        decomposition.addCluster(partition);
        return this;
    }

    /** A cluster with no entities — the state {@code split} leaves behind. */
    DecompositionFixture emptyCluster(String name) {
        decomposition.addCluster(new Partition(name));
        return this;
    }

    /**
     * Records that {@code from} reaches the given entities, which live in {@code to}.
     *
     * <p>Deliberately the singular {@code addCouplingDependency}, never
     * {@code addCouplingDependencies}: the plural form stores the caller's {@code Set} by
     * reference when the key is absent, and a fixture that seeded through it could alias two
     * clusters' dependency sets and make an unrelated test fail in a baffling way.
     */
    DecompositionFixture dependency(String from, String to, int... entityIDs) {
        Partition partition = (Partition) decomposition.getCluster(from);
        for (int entityID : entityIDs)
            partition.addCouplingDependency(to, (short) entityID);
        return this;
    }

    /**
     * Attaches a representation information, which every {@code executeOperation} calls into.
     *
     * <p>Left empty by default, matching the production default and keeping
     * {@code AccessesInformation}'s {@code ContextManager} lookup out of reach. See
     * {@link RecordingRepresentationInformation}.
     */
    DecompositionFixture representationInformation(RepresentationInformation representationInformation) {
        decomposition.addRepresentationInformation(representationInformation);
        return this;
    }

    /** Leaves {@code history} null, so {@code execute} NPEs and {@code executeOperation} does not. */
    DecompositionFixture noHistory() {
        withHistory = false;
        return this;
    }

    PartitionsDecomposition build() {
        if (withHistory)
            decomposition.setHistory(new InMemoryHistory());
        return decomposition;
    }

    /** The shared instance for an ID, named {@code E<id>} so failures are readable. */
    private DomainEntity entity(short entityID) {
        return entities.computeIfAbsent(entityID, id -> new DomainEntity(id, "E" + id));
    }
}
