package pt.ist.socialsoftware.mono2micro.decomposition;

import org.springframework.mock.web.MockMultipartFile;
import pt.ist.socialsoftware.mono2micro.cluster.Cluster;
import pt.ist.socialsoftware.mono2micro.codebase.CodebaseService;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.dto.request.SciPyRequestDto;
import pt.ist.socialsoftware.mono2micro.decomposition.service.DecompositionService;
import pt.ist.socialsoftware.mono2micro.representation.service.RepresentationService;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityScipyAccessesAndRepositoryDto;
import pt.ist.socialsoftware.mono2micro.similarity.service.SimilarityService;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.AccessesWeights;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;
import pt.ist.socialsoftware.mono2micro.strategy.service.StrategyService;
import pt.ist.socialsoftware.mono2micro.utils.Constants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    ServiceDecompositionPipeline(
            CodebaseService codebaseService,
            RepresentationService representationService,
            StrategyService strategyService,
            SimilarityService similarityService,
            DecompositionService decompositionService
    ) {
        this.codebaseService = codebaseService;
        this.representationService = representationService;
        this.strategyService = strategyService;
        this.similarityService = similarityService;
        this.decompositionService = decompositionService;
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
    public void addAccessesRepresentations(String codebaseName, byte[] idToEntity, byte[] accesses) {
        List<String> types = Arrays.asList("IDToEntity", "Accesses");
        List<Object> files = Arrays.asList(
                new MockMultipartFile("representations", "idToEntity.json", "application/json", idToEntity),
                new MockMultipartFile("representations", "accesses.json", "application/json", accesses));

        try {
            representationService.addRepresentations(codebaseName, "Accesses Based", types, files);
        } catch (Exception e) {
            throw new AssertionError("Could not add representations to " + codebaseName, e);
        }
    }

    @Override
    public String createStrategy(String codebaseName) {
        strategyService.createStrategy(codebaseName, "SciPy Clustering", Collections.singletonList("Accesses"));
        return codebaseName + " - A Strategy";
    }

    @Override
    public String createSimilarity(String strategyName) {
        List<Weights> weightsList = new ArrayList<>();
        weightsList.add(new AccessesWeights(25, 25, 25, 25));

        SimilarityScipyAccessesAndRepositoryDto dto = new SimilarityScipyAccessesAndRepositoryDto();
        dto.setStrategyName(strategyName);
        dto.setProfile("Generic");
        dto.setLinkageType("average");
        dto.setTracesMaxLimit(0);
        dto.setTraceType(Constants.TraceType.ALL);
        dto.setWeightsList(weightsList);

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
