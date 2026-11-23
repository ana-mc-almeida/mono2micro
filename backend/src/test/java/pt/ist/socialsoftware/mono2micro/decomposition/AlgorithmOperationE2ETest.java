package pt.ist.socialsoftware.mono2micro.decomposition;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring.OperationRepository;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Running an algorithm operation on a Similarity (D-010, D-012, D-016): a run is saved as the operation and its output. */
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

	private final List<Runnable> cleanups = new ArrayList<>();

	@AfterEach
	void removeCreatedSoftwareSystems() {
		cleanups.forEach(Runnable::run);
		cleanups.clear();
	}

	@Test
	@DisplayName("Deleting a Similarity deletes the operations that produced its cluster representations")
	void deletingTheOutputDeletesItsOperation() {
		HttpDecompositionPipeline pipeline = pipeline();
		String similarity = pipeline.createSimilarity(system(pipeline), SimilarityCase.accesses());
		String output = pipeline.createDecomposition(similarity);
		assertThat(operationRepository.findByOutput_Id(output)).as("the run saved its operation").isPresent();

		pipeline.deleteSimilarity(similarity);

		assertThat(operationRepository.findByOutput_Id(output)).isEmpty();
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
