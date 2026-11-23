package pt.ist.socialsoftware.mono2micro.decomposition;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.singleDecomposition.CohesionMetricCalculator;
import pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.singleDecomposition.CouplingMetricCalculator;
import pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring.EditingChainService;
import pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring.Operation;
import pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring.OperationRepository;
import pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring.algorithms.SciPyOperation;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Running an algorithm operation on a Similarity (D-010, D-012, D-016): a Similarity lists the operations it can run, a run is saved as the
 * operation and its output, and the output is produced, not collected.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Algorithm operations on a Similarity")
class AlgorithmOperationE2ETest {

	private static final String CASE = "quizzes-tutor";

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate rest;

	@Autowired
	private OperationRepository operationRepository;

	@Autowired
	private RepresentationRepository representationRepository;

	@Autowired
	private EditingChainService editingChainService;

	private final List<Runnable> cleanups = new ArrayList<>();

	@AfterEach
	void removeCreatedSoftwareSystems() {
		cleanups.forEach(Runnable::run);
		cleanups.clear();
	}

	@Test
	@DisplayName("The record lists every algorithm operation, and SciPy is offered for a Similarity")
	void availableOperations() {
		HttpDecompositionPipeline pipeline = pipeline();
		String similarity = pipeline.createSimilarity(system(pipeline), SimilarityCase.accesses());

		JsonNode features = pipeline.availableOperations(similarity);

		assertThat(features).hasSize(1);
		assertThat(features.get(0).path("name").asText()).isEqualTo(SciPyOperation.SCIPY);
		assertThat(features.get(0).path("available").asBoolean()).isTrue();
		assertThat(features.get(0).path("failedRequirements")).isEmpty();
	}

	@Test
	@DisplayName("A run is saved as a SciPy operation whose output is a produced cluster representation of the Similarity")
	void aRunIsSavedAsTheOperationAndItsOutput() {
		HttpDecompositionPipeline pipeline = pipeline();
		String similarity = pipeline.createSimilarity(system(pipeline), SimilarityCase.accesses());

		String output = pipeline.createDecomposition(similarity);

		Operation operation = operationRepository.findByOutput_Id(output).orElseThrow(AssertionError::new);
		assertThat(operation).isInstanceOf(SciPyOperation.class);
		assertThat(((SciPyOperation) operation).getCutType()).isEqualTo("N");
		assertThat(((SciPyOperation) operation).getCutValue()).isEqualTo(3);
		assertThat(((SciPyOperation) operation).getSimilarity().getId()).isEqualTo(similarity);
		assertThat(operation.isUndone()).isFalse();
		assertThat(editingChainService.isCollected(representationRepository.getById(output))).isFalse();
		JsonNode listed = pipeline.getDecompositions(similarity);
		assertThat(listed).extracting(d -> d.path("id").asText()).containsExactly(output);
		assertThat(listed.get(0).path("metrics").has("Silhouette Score")).as("the cut's score, from the operation").isTrue();
		assertThat(listed.get(0).path("metrics").has(CohesionMetricCalculator.COHESION)).as("set up: metrics calculated").isTrue();
		assertThat(listed.get(0).path("metrics").has(CouplingMetricCalculator.COUPLING)).as("set up: metrics calculated").isTrue();
	}

	@Test
	@DisplayName("Deleting a Similarity deletes the operations that produced its cluster representations")
	void deletingTheSimilarityDeletesItsOperations() {
		HttpDecompositionPipeline pipeline = pipeline();
		String similarity = pipeline.createSimilarity(system(pipeline), SimilarityCase.accesses());
		String output = pipeline.createDecomposition(similarity);

		pipeline.deleteSimilarity(similarity);

		assertThat(operationRepository.findByOutput_Id(output)).isEmpty();
	}

	@Test
	@DisplayName("Deleting a cluster representation deletes the operation that produced it, and only that one")
	void deletingTheOutputDeletesItsOperation() {
		HttpDecompositionPipeline pipeline = pipeline();
		String similarity = pipeline.createSimilarity(system(pipeline), SimilarityCase.accesses());
		String output = pipeline.createDecomposition(similarity);
		String other = pipeline.createDecomposition(similarity);

		pipeline.deleteDecomposition(output);

		assertThat(operationRepository.findByOutput_Id(output)).isEmpty();
		assertThat(operationRepository.findByOutput_Id(other)).as("the other run's operation").isPresent();
		assertThat(pipeline.getDecompositions(similarity)).extracting(d -> d.path("id").asText()).containsExactly(other);
	}

	@Test
	@DisplayName("A request naming no algorithm operation is refused")
	void anUnknownOperationIsRefused() {
		HttpDecompositionPipeline pipeline = pipeline();
		String similarity = pipeline.createSimilarity(system(pipeline), SimilarityCase.accesses());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", "No Such Algorithm");

		assertThat(pipeline.runOperation(similarity, body).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(pipeline.getDecompositions(similarity)).isEmpty();
	}

	private HttpDecompositionPipeline pipeline() {
		Stack.assumeDecompositionStack();
		return new HttpDecompositionPipeline(rest, port, CASE);
	}

	private String system(HttpDecompositionPipeline pipeline) {
		List<String> missing = RepresentationFiles.missing(CASE, SimilarityCase.accesses().fixtureSuffixes());
		assumeTrue(missing.isEmpty(), "No fixture for " + CASE + ": missing " + String.join(", ", missing));

		String softwareSystem = pipeline.createSoftwareSystem(CASE + "-operations-" + System.nanoTime());
		cleanups.add(() -> pipeline.deleteSoftwareSystem(softwareSystem));
		pipeline.addRepresentations(softwareSystem, SimilarityCase.accesses());
		return softwareSystem;
	}
}
