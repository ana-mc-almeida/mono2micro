package pt.ist.socialsoftware.mono2micro.decomposition;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One decomposition strategy, described as data.
 *
 * <p>The seven strategies differ only in what they upload, what they name themselves and what
 * the similarity request body looks like — the six pipeline steps are identical. Describing
 * them as data keeps that shared shape in one place instead of six near-copies of the same
 * test, and makes adding the eighth a matter of one static factory.
 *
 * <p><b>Every string here is a literal that the backend dispatches on at runtime</b>, across
 * registries that do not agree with each other. The same concept is spelled three different
 * ways for the sequence-of-accesses variant alone:
 *
 * <ul>
 *   <li>strategy type {@code "Functionality Vectorization Sequence Accesses"}
 *   <li>similarity type {@code "..._FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES"}
 *   <li>weights type {@code "FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS"} — no {@code SEQUENCE}
 * </ul>
 *
 * <p>Getting one wrong fails at runtime rather than compile time, which is the fan-out that
 * this suite exists to catch. The values are transcribed from the constants themselves rather
 * than referenced, so that a renamed constant shows up here as a red test rather than a silent
 * recompile — this test is meant to pin the tool's external contract.
 */
final class StrategyCase {

    // Representation type strings (<pkg>/representation/domain/*.java). Two of these do not
    // resemble the class that defines them: AuthorRepresentation is "Changes Authorship" and
    // CommitRepresentation is "File Changes".
    private static final String ID_TO_ENTITY = "IDToEntity";
    private static final String ENTITY_TO_ID = "EntityToID";
    private static final String ACCESSES = "Accesses";
    private static final String AUTHOR = "Changes Authorship";
    private static final String COMMIT = "File Changes";
    private static final String CODE_EMBEDDINGS = "Code Embeddings";
    private static final String STRUCTURE = "Structure";

    // Representation groups (Representation.java:25-28).
    private static final String ACCESSES_GROUP = "Accesses Based";
    private static final String REPOSITORY_GROUP = "Repository Based";
    private static final String CODE_EMBEDDINGS_GROUP = "Code Embeddings Based";
    private static final String STRUCTURE_GROUP = "Structure Based";

    // Similarity type discriminators (SimilarityDto.java:14-21). Accesses and Repository share
    // one implementation, SimilarityScipyAccessesAndRepository.
    static final String SIMILARITY_ACCESSES_REPOSITORY = "SIMILARITY_SCIPY_ACCESSES_REPOSITORY";
    static final String SIMILARITY_STRUCTURE = "SIMILARITY_SCIPY_STRUCTURE";
    static final String SIMILARITY_CLASS_VECTORIZATION = "SIMILARITY_SCIPY_CLASS_VECTORIZATION";
    static final String SIMILARITY_ENTITY_VECTORIZATION = "SIMILARITY_SCIPY_ENTITY_VECTORIZATION";
    static final String SIMILARITY_FV_CALLGRAPH = "SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_CALLGRAPH";
    static final String SIMILARITY_FV_SEQUENCE_ACCESSES = "SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES";

    private final String label;
    private final List<String> strategyTypes;
    private final String representationGroup;

    /** Representation type → fixture filename suffix, in upload order. */
    private final Map<String, String> representations;

    private final String similarityType;
    private final List<Map<String, Object>> weights;

    /** profile / traceType / tracesMaxLimit / depth, only where the DTO has the field. */
    private final Map<String, Object> extraSimilarityFields;

    private StrategyCase(
            String label,
            List<String> strategyTypes,
            String representationGroup,
            Map<String, String> representations,
            String similarityType,
            List<Map<String, Object>> weights,
            Map<String, Object> extraSimilarityFields
    ) {
        this.label = label;
        this.strategyTypes = strategyTypes;
        this.representationGroup = representationGroup;
        this.representations = representations;
        this.similarityType = similarityType;
        this.weights = weights;
        this.extraSimilarityFields = extraSimilarityFields;
    }

    /**
     * The six cases whose fixtures exist, plus Structure.
     *
     * <p>Structure is last because it is the one strategy with no fixture: the only
     * {@code quizzes-tutor_structure.json} in the repository is a git-lfs pointer stub and
     * {@code git lfs} is not installed here. It is described anyway so that supplying the file
     * is all it takes to cover it.
     */
    static List<StrategyCase> all() {
        return Arrays.asList(
                accesses(),
                repository(),
                classVectorization(),
                entityVectorization(),
                functionalityVectorizationCallGraph(),
                functionalityVectorizationSequenceOfAccesses(),
                structure());
    }

    static StrategyCase accesses() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(ID_TO_ENTITY, RepresentationFiles.ID_TO_ENTITY_SUFFIX);
        files.put(ACCESSES, RepresentationFiles.ACCESSES_SUFFIX);

        return new StrategyCase(
                "accesses",
                Collections.singletonList("Accesses"),
                ACCESSES_GROUP,
                files,
                SIMILARITY_ACCESSES_REPOSITORY,
                Collections.singletonList(accessesWeights(25, 25, 25, 25)),
                accessesProfileFields());
    }

    /**
     * Repository is the only multi-type strategy here: it needs the Accesses representations as
     * well as its own, so it uploads the {@code Repository Based} group, which contains all
     * four files, and declares both strategy types.
     */
    static StrategyCase repository() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(ID_TO_ENTITY, RepresentationFiles.ID_TO_ENTITY_SUFFIX);
        files.put(ACCESSES, RepresentationFiles.ACCESSES_SUFFIX);
        files.put(AUTHOR, RepresentationFiles.AUTHOR_SUFFIX);
        files.put(COMMIT, RepresentationFiles.COMMIT_SUFFIX);

        // Split 17/17/17/17 + 16/16 so the six weights still total 100, as the frontend does
        // for this combination (WeightsFactory.ts:38-42).
        return new StrategyCase(
                "repository",
                Arrays.asList("Accesses", "Repository"),
                REPOSITORY_GROUP,
                files,
                SIMILARITY_ACCESSES_REPOSITORY,
                Arrays.asList(accessesWeights(17, 17, 17, 17), repositoryWeights(16, 16)),
                accessesProfileFields());
    }

    static StrategyCase classVectorization() {
        // Class and Entity Vectorization take no weights at all and their DTOs carry nothing
        // but linkageType.
        return new StrategyCase(
                "class-vectorization",
                Collections.singletonList("Class Vectorization"),
                CODE_EMBEDDINGS_GROUP,
                codeEmbeddingsFiles(),
                SIMILARITY_CLASS_VECTORIZATION,
                Collections.emptyList(),
                Collections.emptyMap());
    }

    static StrategyCase entityVectorization() {
        return new StrategyCase(
                "entity-vectorization",
                Collections.singletonList("Entity Vectorization"),
                CODE_EMBEDDINGS_GROUP,
                codeEmbeddingsFiles(),
                SIMILARITY_ENTITY_VECTORIZATION,
                Collections.emptyList(),
                Collections.emptyMap());
    }

    static StrategyCase functionalityVectorizationCallGraph() {
        Map<String, Object> weights = new LinkedHashMap<>();
        weights.put("type", "FUNCTIONALITY_VECTORIZATION_CALLGRAPH_WEIGHTS");
        weights.put("controllersWeight", 25);
        weights.put("servicesWeight", 25);
        weights.put("intermediateMethodsWeight", 25);
        weights.put("entitiesWeight", 25);

        // depth must be > 0: the form refuses to submit otherwise, and the field's own default
        // is 2 (SimilarityScipyFunctionalityVectorizationByCallGraph.java:35).
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("depth", 2);

        return new StrategyCase(
                "fv-callgraph",
                Collections.singletonList("Functionality Vectorization Call Graph"),
                CODE_EMBEDDINGS_GROUP,
                codeEmbeddingsFiles(),
                SIMILARITY_FV_CALLGRAPH,
                Collections.singletonList(weights),
                extra);
    }

    static StrategyCase functionalityVectorizationSequenceOfAccesses() {
        Map<String, Object> weights = new LinkedHashMap<>();
        weights.put("type", "FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS");
        weights.put("readMetricWeight", 50);
        weights.put("writeMetricWeight", 50);

        return new StrategyCase(
                "fv-sequence-accesses",
                Collections.singletonList("Functionality Vectorization Sequence Accesses"),
                CODE_EMBEDDINGS_GROUP,
                codeEmbeddingsFiles(),
                SIMILARITY_FV_SEQUENCE_ACCESSES,
                Collections.singletonList(weights),
                Collections.emptyMap());
    }

    static StrategyCase structure() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(ENTITY_TO_ID, RepresentationFiles.ENTITY_TO_ID_SUFFIX);
        files.put(ID_TO_ENTITY, RepresentationFiles.ID_TO_ENTITY_SUFFIX);
        files.put(ACCESSES, RepresentationFiles.ACCESSES_SUFFIX);
        files.put(STRUCTURE, RepresentationFiles.STRUCTURE_SUFFIX);

        Map<String, Object> weights = new LinkedHashMap<>();
        weights.put("type", "STRUCTURE_WEIGHTS");
        weights.put("oneToOneWeight", 33);
        weights.put("oneToManyWeight", 33);
        weights.put("heritageWeight", 34);   // 34 so the three total 100

        // SimilarityScipyStructureDto has profile and traceType but, unlike the accesses DTO,
        // no tracesMaxLimit.
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("profile", "Generic");
        extra.put("traceType", "ALL");

        return new StrategyCase(
                "structure",
                Collections.singletonList("Structure"),
                STRUCTURE_GROUP,
                files,
                SIMILARITY_STRUCTURE,
                Collections.singletonList(weights),
                extra);
    }

    private static Map<String, String> codeEmbeddingsFiles() {
        Map<String, String> files = new LinkedHashMap<>();
        files.put(ID_TO_ENTITY, RepresentationFiles.ID_TO_ENTITY_SUFFIX);
        files.put(ENTITY_TO_ID, RepresentationFiles.ENTITY_TO_ID_SUFFIX);
        files.put(ACCESSES, RepresentationFiles.ACCESSES_SUFFIX);
        files.put(CODE_EMBEDDINGS, RepresentationFiles.CODE_EMBEDDINGS_SUFFIX);
        return files;
    }

    private static Map<String, Object> accessesWeights(int access, int write, int read, int sequence) {
        Map<String, Object> weights = new LinkedHashMap<>();
        weights.put("type", "ACCESSES_WEIGHTS");
        weights.put("accessMetricWeight", access);
        weights.put("writeMetricWeight", write);
        weights.put("readMetricWeight", read);
        weights.put("sequenceMetricWeight", sequence);
        return weights;
    }

    private static Map<String, Object> repositoryWeights(int author, int commit) {
        Map<String, Object> weights = new LinkedHashMap<>();
        weights.put("type", "REPOSITORY_WEIGHTS");
        weights.put("authorMetricWeight", author);
        weights.put("commitMetricWeight", commit);
        return weights;
    }

    /** {@code Generic} is the only profile {@code AccessesRepresentation.init} creates. */
    private static Map<String, Object> accessesProfileFields() {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("profile", "Generic");
        extra.put("tracesMaxLimit", 0);
        extra.put("traceType", "ALL");
        return extra;
    }

    /**
     * The strategy name the backend will generate, from {@code Strategy}'s constructor
     * (Strategy.java:76-90): the initial of every word of every strategy type, types joined
     * with {@code +}. So {@code "Class Vectorization"} gives {@code CV} and
     * {@code ["Accesses","Repository"]} gives {@code A+R}.
     *
     * <p>Reproduced rather than read back because it is needed <em>before</em> anything exists
     * to read it from — unlike the similarity and decomposition names, which are read back.
     */
    String strategyName(String codebaseName) {
        StringBuilder shortForm = new StringBuilder();
        for (int i = 0; i < strategyTypes.size(); i++) {
            if (i > 0)
                shortForm.append("+");
            for (String word : strategyTypes.get(i).split(" "))
                shortForm.append(word.charAt(0));
        }
        return codebaseName + " - " + shortForm + " Strategy";
    }

    /** Fixture suffixes this case needs present, for the up-front precondition check. */
    List<String> fixtureSuffixes() {
        return new java.util.ArrayList<>(representations.values());
    }

    String label() {
        return label;
    }

    List<String> strategyTypes() {
        return strategyTypes;
    }

    String representationGroup() {
        return representationGroup;
    }

    Map<String, String> representations() {
        return representations;
    }

    String similarityType() {
        return similarityType;
    }

    List<Map<String, Object>> weights() {
        return weights;
    }

    Map<String, Object> extraSimilarityFields() {
        return extraSimilarityFields;
    }

    @Override
    public String toString() {
        return label;
    }
}
