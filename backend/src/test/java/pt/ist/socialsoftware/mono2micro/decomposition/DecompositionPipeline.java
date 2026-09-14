package pt.ist.socialsoftware.mono2micro.decomposition;

import pt.ist.socialsoftware.mono2micro.cluster.Cluster;

import java.util.Map;

/**
 * The six steps that turn uploaded representations into a decomposition, as one interface so
 * the same assertions can run against more than one layer of the backend.
 *
 * <p>There are two implementations, and they are not equally valuable:
 *
 * <ul>
 *   <li>{@link HttpDecompositionPipeline} drives the REST API. It is pinned to HTTP paths,
 *       JSON shapes and type strings — the tool's stable contract — so it should survive a
 *       refactor of the backend's internals <em>without edits</em>. This is the regression net.
 *   <li>{@link ServiceDecompositionPipeline} calls the Spring services directly. It is pinned
 *       to class names, method signatures and DI wiring, which is exactly what an internal
 *       refactor moves, so it will need rewriting when that happens. It earns its place as a
 *       diagnostic: when the HTTP test goes red, this one localizes the break — if services
 *       pass and HTTP fails, the fault is in routing or serialization.
 * </ul>
 *
 * <p>A test that must be rewritten as part of the change it is meant to guard has stopped
 * being a safety net, because a red result can no longer distinguish a real break from an
 * edit to the test. That is why the HTTP implementation is the one to keep untouched.
 *
 * <p>Every {@code create*} method on the services returns {@code void}, and the backend
 * generates the names of the similarity and the decomposition itself. Both implementations
 * therefore read the generated name back rather than reconstructing it: the formatting rules
 * live in three separate classes ({@code Strategy}, {@code SimilarityScipyAccessesAndRepositoryDto},
 * {@code AccessesWeights}) and duplicating them here would pin the test to internals it is
 * meant to be free of.
 *
 * <p>Reading the name back is not merely tidier — it is <em>required</em> for at least one
 * variant. {@code SimilarityScipyEntityVectorization}'s constructor appends to the name the
 * DTO already computed, so the stored name is not the one the naming rules produce. Each
 * variant also formats its name differently: the structure DTO orders its parameters
 * {@code (profile, linkage, traceType)} where the accesses DTO uses
 * {@code (linkage, profile, tracesMaxLimit, traceType)}.
 */
interface DecompositionPipeline {

    /** Step 1. Throws if the codebase already exists. */
    void createCodebase(String codebaseName);

    /**
     * Step 2. Uploads every file the case's representation group requires, in one request.
     *
     * <p>It has to be one request: {@code RepresentationService} rejects a second upload of a
     * type the codebase already holds with "Re-sending representations is not allowed."
     */
    void addRepresentations(String codebaseName, StrategyCase strategyCase);

    /** Step 3. Returns the strategy name, which is deterministic — see {@link StrategyCase#strategyName}. */
    String createStrategy(String codebaseName, StrategyCase strategyCase);

    /** Step 4. Returns the generated similarity name, read back from the strategy. */
    String createSimilarity(String strategyName, StrategyCase strategyCase);

    /** Step 5. Returns the generated decomposition name, read back from the similarity. */
    String createDecomposition(String similarityName);

    /** Step 6. The clusters of the finished decomposition. */
    Map<String, Cluster> getClusters(String decompositionName);

    /**
     * How many representations the codebase currently holds.
     *
     * <p>Not part of the pipeline — it guards step 2. {@code RepresentationService} returns
     * <em>silently</em> when both request parameters bind to null, so a malformed upload
     * reports success and fails several steps later with an unrelated error.
     */
    int representationCount(String codebaseName);

    /** Cleanup. Mongo state persists in the {@code mongodata} volume across runs. */
    void deleteCodebase(String codebaseName);

    /** Names the layer under test, for the parameterized test's display name. */
    String layer();
}
