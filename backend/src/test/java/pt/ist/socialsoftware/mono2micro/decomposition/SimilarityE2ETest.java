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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Similarities created on a software system, without a strategy (D-010, D-013, D-016): the system offers kinds, the backend refuses a kind it
 * does not offer, and a Similarity reads the representations its kind requires.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Similarities on a software system")
class SimilarityE2ETest {

	private static final String CASE = "quizzes-tutor";
	private static final String STRUCTURE_CASE = "quizzes-tutor-structure";

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate rest;

	private final List<Runnable> cleanups = new ArrayList<>();

	@AfterEach
	void removeCreatedSoftwareSystems() {
		cleanups.forEach(Runnable::run);
		cleanups.clear();
	}

	@Test
	@DisplayName("The record lists every kind, and the Accesses uploads offer only the Accesses kind")
	void availableKinds() {
		HttpDecompositionPipeline pipeline = pipeline(CASE);
		String softwareSystem = system(pipeline, CASE, SimilarityCase.accesses());

		Map<String, Boolean> offered = offered(pipeline.getAvailableSimilarityKinds(softwareSystem));

		assertThat(offered).hasSize(8);
		assertThat(offered).containsEntry("Accesses", true).containsEntry("Repository", false).containsEntry("Accesses and Repository", false);
	}

	/**
	 * Accesses+Structure and Repository+Structure were checkbox combinations that crashed or ignored Structure. What is offered is a kind, and
	 * no kind combines Structure with other criteria, so neither is offered, and a request for one is refused (D-013, O-12).
	 */
	@Test
	@DisplayName("The two broken criteria combinations are not offered (D-013)")
	void brokenCombinationsAreNotOffered() {
		HttpDecompositionPipeline pipeline = pipeline(STRUCTURE_CASE);
		String softwareSystem = system(pipeline, STRUCTURE_CASE, SimilarityCase.structure());

		assertThat(offered(pipeline.getAvailableSimilarityKinds(softwareSystem)).keySet())
				.doesNotContain("Accesses and Structure", "Repository and Structure");
		assertThat(pipeline.requestSimilarity(softwareSystem, SimilarityCase.structure(), "Accesses and Structure"))
				.isEqualTo(HttpStatus.BAD_REQUEST);
	}

	/** A crafted request for a kind the uploads do not offer is refused before anything is computed (D-003). */
	@Test
	@DisplayName("A Similarity of a kind the software system does not offer is refused")
	void unavailableKindIsRefused() {
		HttpDecompositionPipeline pipeline = pipeline(CASE);
		String softwareSystem = system(pipeline, CASE, SimilarityCase.accesses());

		assertThat(pipeline.requestSimilarity(softwareSystem, SimilarityCase.repository(), "Accesses and Repository"))
				.isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
		assertThat(pipeline.getSimilarities(softwareSystem)).isEmpty();
	}

	/** The kind fixes the criteria (D-013): Accesses with the Repository weights would compute another kind under its name. */
	@Test
	@DisplayName("A Similarity with weights its kind is not computed with is refused")
	void weightsOfAnotherKindAreRefused() {
		HttpDecompositionPipeline pipeline = pipeline(CASE);
		String softwareSystem = system(pipeline, CASE, SimilarityCase.accesses());

		assertThat(pipeline.requestSimilarity(softwareSystem, SimilarityCase.repository(), "Accesses"))
				.isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(pipeline.getSimilarities(softwareSystem)).isEmpty();
	}

	@Test
	@DisplayName("Two identical requests share one Similarity, which reads the representations its kind requires")
	void identicalRequestsShareOneSimilarity() {
		HttpDecompositionPipeline pipeline = pipeline(CASE);
		String softwareSystem = system(pipeline, CASE, SimilarityCase.accesses());

		String first = pipeline.createSimilarity(softwareSystem, SimilarityCase.accesses());
		String second = pipeline.createSimilarity(softwareSystem, SimilarityCase.accesses());

		assertThat(second).isEqualTo(first);
		JsonNode similarities = pipeline.getSimilarities(softwareSystem);
		assertThat(similarities).hasSize(1);
		assertThat(similarities.get(0).path("id").asText()).isEqualTo(first);
		assertThat(similarities.get(0).path("kindName").asText()).isEqualTo("Accesses");
		assertThat(similarities.get(0).path("name").asText()).startsWith("Accesses ");
		assertThat(similarities.get(0).path("representationIds")).extracting(JsonNode::asText).containsExactly(
				pipeline.representationId(softwareSystem, "IDToEntity"),
				pipeline.representationId(softwareSystem, "Accesses"));
	}

	/** Accesses and Repository is its own kind (D-026): it reads the four representations and its decompositions get both blocks. */
	@Test
	@DisplayName("An Accesses and Repository Similarity is clustered into a decomposition listed with the software system's")
	void accessesAndRepositoryIsClustered() {
		HttpDecompositionPipeline pipeline = pipeline(CASE);
		String softwareSystem = system(pipeline, CASE, SimilarityCase.repository());

		String similarity = pipeline.createSimilarity(softwareSystem, SimilarityCase.repository());
		String decomposition = pipeline.createDecomposition(similarity);

		assertThat(pipeline.getSimilarities(softwareSystem).get(0).path("representationIds")).hasSize(4);
		assertThat(pipeline.getClusters(decomposition)).isNotEmpty();
		assertThat(pipeline.getSoftwareSystemDecompositions(softwareSystem)).extracting(d -> d.path("id").asText()).containsExactly(decomposition);
	}

	@Test
	@DisplayName("Deleting a representation deletes the Similarities that read it")
	void deletingARepresentationDeletesItsReaders() {
		HttpDecompositionPipeline pipeline = pipeline(CASE);
		String softwareSystem = system(pipeline, CASE, SimilarityCase.accesses());
		String similarity = pipeline.createSimilarity(softwareSystem, SimilarityCase.accesses());
		pipeline.createDecomposition(similarity);
		assertThat(pipeline.getSimilarities(softwareSystem)).extracting(s -> s.path("id").asText()).containsExactly(similarity);

		pipeline.deleteRepresentation(pipeline.representationId(softwareSystem, "Accesses"));

		assertThat(pipeline.getSimilarities(softwareSystem)).isEmpty();
		assertThat(pipeline.getSoftwareSystemDecompositions(softwareSystem)).isEmpty();
	}

	private HttpDecompositionPipeline pipeline(String caseName) {
		Stack.assumeDecompositionStack();
		return new HttpDecompositionPipeline(rest, port, caseName);
	}

	private String system(HttpDecompositionPipeline pipeline, String caseName, SimilarityCase uploads) {
		List<String> missing = RepresentationFiles.missing(caseName, uploads.fixtureSuffixes());
		assumeTrue(missing.isEmpty(), "No fixture for " + caseName + ": missing " + String.join(", ", missing));

		String softwareSystem = pipeline.createSoftwareSystem(caseName + "-similarities-" + System.nanoTime());
		cleanups.add(() -> pipeline.deleteSoftwareSystem(softwareSystem));
		pipeline.addRepresentations(softwareSystem, uploads);
		return softwareSystem;
	}

	private static Map<String, Boolean> offered(JsonNode features) {
		Map<String, Boolean> offered = new TreeMap<>();
		for (JsonNode feature : features)
			offered.put(feature.path("name").asText(), feature.path("available").asBoolean());
		return offered;
	}
}
