package pt.ist.socialsoftware.mono2micro.decomposition;

import org.springframework.mock.web.MockMultipartFile;
import pt.ist.socialsoftware.mono2micro.cluster.Cluster;
import pt.ist.socialsoftware.mono2micro.codebase.CodebaseService;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.dto.request.SciPyRequestDto;
import pt.ist.socialsoftware.mono2micro.decomposition.service.DecompositionService;
import pt.ist.socialsoftware.mono2micro.representation.service.RepresentationService;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyAccessesAndRepositoryDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyClassVectorizationDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyEntityVectorizationDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyFunctionalityVectorizationByCallGraphDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyFunctionalityVectorizationBySequenceOfAccessesDto;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyStructureDto;
import pt.ist.socialsoftware.mono2micro.similarity.service.SimilarityService;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.AccessesWeights;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.FunctionalityVectorizationCallGraphWeights;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.FunctionalityVectorizationSequenceOfAccessesWeights;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.RepositoryWeights;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.StructureWeights;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;
import pt.ist.socialsoftware.mono2micro.strategy.service.StrategyService;
import pt.ist.socialsoftware.mono2micro.utils.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Drives the same pipeline by calling the Spring services directly.
 *
 * <p>This is the diagnostic, not the net. It is pinned to class names, method signatures and
 * DI wiring — precisely what an internal refactor moves — so expect to rewrite it when the
 * backend's modularity changes. Its value is localizing a failure: if this passes while
 * {@link HttpDecompositionPipeline} fails, the fault is in routing or serialization rather
 * than in the pipeline itself.
 *
 * <p>Because it constructs DTOs directly, it never exercises the {@code @JsonSubTypes}
 * discriminator dispatch. That gap is deliberate and is the reason the HTTP implementation
 * is the one that must keep working.
 */
class ServiceDecompositionPipeline implements DecompositionPipeline {

    private final CodebaseService codebaseService;
    private final RepresentationService representationService;
    private final StrategyService strategyService;
    private final SimilarityService similarityService;
    private final DecompositionService decompositionService;

    /** The fixture folder to read representation files from, e.g. {@code quizzes-tutor}. */
    private final String caseName;

    ServiceDecompositionPipeline(
            CodebaseService codebaseService,
            RepresentationService representationService,
            StrategyService strategyService,
            SimilarityService similarityService,
            DecompositionService decompositionService,
            String caseName
    ) {
        this.codebaseService = codebaseService;
        this.representationService = representationService;
        this.strategyService = strategyService;
        this.similarityService = similarityService;
        this.decompositionService = decompositionService;
        this.caseName = caseName;
    }

    @Override
    public String layer() {
        return "service";
    }

    @Override
    public void createCodebase(String codebaseName) {
        codebaseService.createCodebase(codebaseName);
    }

    @Override
    public void addRepresentations(String codebaseName, StrategyCase strategyCase) {
        List<String> types = new ArrayList<>();
        List<Object> files = new ArrayList<>();

        for (Map.Entry<String, String> representation : strategyCase.representations().entrySet()) {
            byte[] content = RepresentationFiles.read(caseName, representation.getValue());
            types.add(representation.getKey());
            files.add(new MockMultipartFile(
                    "representations", caseName + representation.getValue(), "application/json", content));
        }

        try {
            representationService.addRepresentations(
                    codebaseName, strategyCase.representationGroup(), types, files);
        } catch (Exception e) {
            throw new AssertionError("Could not add representations to " + codebaseName, e);
        }
    }

    @Override
    public String createStrategy(String codebaseName, StrategyCase strategyCase) {
        strategyService.createStrategy(codebaseName, "SciPy Clustering", strategyCase.strategyTypes());
        return strategyCase.strategyName(codebaseName);
    }

    @Override
    public String createSimilarity(String strategyName, StrategyCase strategyCase) {
        SimilarityDto dto = similarityDto(strategyName, strategyCase);

        try {
            similarityService.createSimilarity(dto);
        } catch (Exception e) {
            throw new AssertionError("Could not create similarity for " + strategyName, e);
        }

        // createSimilarity returns void, and a duplicate request is a silent no-op, so the
        // generated name is read back rather than reconstructed.
        List<Similarity> similarities = strategyService.getStrategySimilarities(strategyName);
        if (similarities.isEmpty())
            throw new AssertionError("No similarity was created for " + strategyName);

        return similarities.get(similarities.size() - 1).getName();
    }

    /**
     * Builds the DTO subclass the variant requires.
     *
     * <p>A switch rather than a shared setter list because the six DTOs genuinely differ:
     * the class and entity vectorization DTOs carry <em>only</em> {@code linkageType}, the
     * structure DTO has no {@code tracesMaxLimit}, and the call-graph DTO adds {@code depth}.
     * Constructing them directly is also why this implementation never exercises the
     * {@code @JsonSubTypes} dispatch that {@link HttpDecompositionPipeline} does.
     */
    private SimilarityDto similarityDto(String strategyName, StrategyCase strategyCase) {
        Map<String, Object> extra = strategyCase.extraSimilarityFields();

        switch (strategyCase.similarityType()) {
            case StrategyCase.SIMILARITY_ACCESSES_REPOSITORY: {
                SimilarityScipyAccessesAndRepositoryDto dto = new SimilarityScipyAccessesAndRepositoryDto();
                dto.setStrategyName(strategyName);
                dto.setLinkageType("average");
                dto.setProfile((String) extra.get("profile"));
                dto.setTracesMaxLimit((Integer) extra.get("tracesMaxLimit"));
                dto.setTraceType(Constants.TraceType.valueOf((String) extra.get("traceType")));
                dto.setWeightsList(weightsList(strategyCase));
                return dto;
            }
            case StrategyCase.SIMILARITY_STRUCTURE: {
                SimilarityScipyStructureDto dto = new SimilarityScipyStructureDto();
                dto.setStrategyName(strategyName);
                dto.setLinkageType("average");
                dto.setProfile((String) extra.get("profile"));
                dto.setTraceType(Constants.TraceType.valueOf((String) extra.get("traceType")));
                dto.setWeightsList(weightsList(strategyCase));
                return dto;
            }
            case StrategyCase.SIMILARITY_CLASS_VECTORIZATION: {
                SimilarityScipyClassVectorizationDto dto = new SimilarityScipyClassVectorizationDto();
                dto.setStrategyName(strategyName);
                dto.setLinkageType("average");
                return dto;
            }
            case StrategyCase.SIMILARITY_ENTITY_VECTORIZATION: {
                SimilarityScipyEntityVectorizationDto dto = new SimilarityScipyEntityVectorizationDto();
                dto.setStrategyName(strategyName);
                dto.setLinkageType("average");
                return dto;
            }
            case StrategyCase.SIMILARITY_FV_CALLGRAPH: {
                SimilarityScipyFunctionalityVectorizationByCallGraphDto dto =
                        new SimilarityScipyFunctionalityVectorizationByCallGraphDto();
                dto.setStrategyName(strategyName);
                dto.setLinkageType("average");
                dto.setDepth((Integer) extra.get("depth"));
                dto.setWeightsList(weightsList(strategyCase));
                return dto;
            }
            case StrategyCase.SIMILARITY_FV_SEQUENCE_ACCESSES: {
                SimilarityScipyFunctionalityVectorizationBySequenceOfAccessesDto dto =
                        new SimilarityScipyFunctionalityVectorizationBySequenceOfAccessesDto();
                dto.setStrategyName(strategyName);
                dto.setLinkageType("average");
                dto.setWeightsList(weightsList(strategyCase));
                return dto;
            }
            default:
                throw new AssertionError("No DTO mapping for similarity type " + strategyCase.similarityType()
                        + ". Add one here and in SimilarityFactory.");
        }
    }

    /**
     * Turns the case's weight maps into domain objects.
     *
     * <p>The maps are the wire shape {@link HttpDecompositionPipeline} posts; here they are
     * rebuilt as the concrete {@code Weights} subclasses, keyed by the same {@code type}
     * literal that {@code WeightsFactory} dispatches on.
     */
    private List<Weights> weightsList(StrategyCase strategyCase) {
        List<Weights> weightsList = new ArrayList<>();

        for (Map<String, Object> weights : strategyCase.weights()) {
            String type = (String) weights.get("type");
            switch (type) {
                case "ACCESSES_WEIGHTS":
                    weightsList.add(new AccessesWeights(
                            number(weights, "accessMetricWeight"),
                            number(weights, "writeMetricWeight"),
                            number(weights, "readMetricWeight"),
                            number(weights, "sequenceMetricWeight")));
                    break;
                case "REPOSITORY_WEIGHTS":
                    weightsList.add(new RepositoryWeights(
                            number(weights, "authorMetricWeight"),
                            number(weights, "commitMetricWeight")));
                    break;
                case "STRUCTURE_WEIGHTS":
                    weightsList.add(new StructureWeights(
                            number(weights, "oneToOneWeight"),
                            number(weights, "oneToManyWeight"),
                            number(weights, "heritageWeight")));
                    break;
                case "FUNCTIONALITY_VECTORIZATION_CALLGRAPH_WEIGHTS":
                    weightsList.add(new FunctionalityVectorizationCallGraphWeights(
                            number(weights, "controllersWeight"),
                            number(weights, "servicesWeight"),
                            number(weights, "intermediateMethodsWeight"),
                            number(weights, "entitiesWeight")));
                    break;
                case "FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS":
                    weightsList.add(new FunctionalityVectorizationSequenceOfAccessesWeights(
                            number(weights, "readMetricWeight"),
                            number(weights, "writeMetricWeight")));
                    break;
                default:
                    throw new AssertionError("No weights mapping for " + type);
            }
        }
        return weightsList;
    }

    private float number(Map<String, Object> weights, String key) {
        return ((Number) weights.get(key)).floatValue();
    }

    @Override
    public String createDecomposition(String similarityName) {
        SciPyRequestDto request = new SciPyRequestDto(similarityName, "N", 3);
        request.setSimilarityName(similarityName);

        try {
            decompositionService.createDecomposition(request);
        } catch (Exception e) {
            throw new AssertionError("Could not create decomposition for " + similarityName, e);
        }

        List<Decomposition> decompositions = similarityService.getDecompositions(similarityName);
        if (decompositions.isEmpty())
            throw new AssertionError("No decomposition was created for " + similarityName);

        return decompositions.get(decompositions.size() - 1).getName();
    }

    @Override
    public Map<String, Cluster> getClusters(String decompositionName) {
        return decompositionService.getDecomposition(decompositionName).getClusters();
    }

    @Override
    public int representationCount(String codebaseName) {
        return codebaseService.getRepresentationTypes(codebaseName).size();
    }

    @Override
    public void deleteCodebase(String codebaseName) {
        codebaseService.deleteCodebase(codebaseName);
    }
}
