package pt.ist.socialsoftware.mono2micro.decomposition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * A recommendation runs its candidates through the SciPy operation and saves a chosen one as a cluster representation, on the Similarity of
 * the candidate's weights (D-030). The Repository kind explores two weights, so its run is short: 11 matrices, each cut into 3 to 5
 * clusters on {@code quizzes-tutor}'s 13 entities.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Recommendations through the SciPy operation")
class RecommendationE2ETest {

	private static final String CASE = "quizzes-tutor";
	private static final String KIND = "Repository";
	private static final long TIMEOUT_MILLIS = 300_000;

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
	@DisplayName("A saved candidate is a cluster representation of its number of clusters, on a Similarity of the recommendation's kind")
	void aSavedCandidateIsAClusterRepresentationOfItsSize() throws InterruptedException {
		HttpDecompositionPipeline pipeline = pipeline();
		String softwareSystem = system(pipeline);
		Map<String, Object> request = recommendationRequest(pipeline, softwareSystem);

		String recommendation = completed(pipeline, request).path("id").asText();
		JsonNode candidates = pipeline.getRecommendationResult(recommendation);
		assertThat(candidates).hasSize(11 * 3);

		JsonNode chosen = candidates.get(0);
		pipeline.saveRecommendationCandidates(recommendation, Collections.singletonList(chosen.path("name").asText()));

		JsonNode similarities = pipeline.getSimilarities(softwareSystem);
		assertThat(similarities).hasSize(1);
		assertThat(similarities.get(0).path("kindName").asText()).isEqualTo(KIND);
		JsonNode outputs = pipeline.getDecompositions(similarities.get(0).path("id").asText());
		assertThat(outputs).hasSize(1);
		assertThat(outputs.get(0).path("clusters").size()).isEqualTo(chosen.path("numberOfClusters").asInt());
	}

	/** The request the recommendation screen sends for the kind: its default weights, average linkage, every trace. */
	@SuppressWarnings("unchecked")
	private static Map<String, Object> recommendationRequest(HttpDecompositionPipeline pipeline, String softwareSystem) {
		Map<String, Object> request = new LinkedHashMap<>();
		request.put("type", "RECOMMEND_MATRIX_SCIPY");
		request.put("softwareSystemId", softwareSystem);
		request.put("kindName", KIND);
		request.put("weightsList", new ObjectMapper().convertValue(pipeline.getSimilarityKind(KIND).path("defaultWeights"), List.class));
		request.put("profileId", null);
		request.put("linkageType", "average");
		request.put("tracesMaxLimit", 0);
		request.put("traceType", "ALL");
		return request;
	}

	/** Asks for the recommendation until it has run: an equal request returns the existing one, with its completion flag. */
	private static JsonNode completed(HttpDecompositionPipeline pipeline, Map<String, Object> request) throws InterruptedException {
		long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
		JsonNode recommendation = pipeline.requestRecommendation(request);
		while (!recommendation.path("completed").asBoolean() && !recommendation.path("isCompleted").asBoolean()) {
			if (System.currentTimeMillis() > deadline)
				throw new AssertionError("The recommendation did not complete in " + TIMEOUT_MILLIS / 1000 + "s: " + recommendation);
			Thread.sleep(1000);
			recommendation = pipeline.requestRecommendation(request);
		}
		return recommendation;
	}

	private HttpDecompositionPipeline pipeline() {
		Stack.assumeDecompositionStack();
		return new HttpDecompositionPipeline(rest, port, CASE);
	}

	private String system(HttpDecompositionPipeline pipeline) {
		List<String> missing = RepresentationFiles.missing(CASE, SimilarityCase.repository().fixtureSuffixes());
		assumeTrue(missing.isEmpty(), "No fixture for " + CASE + ": missing " + String.join(", ", missing));

		String softwareSystem = pipeline.createSoftwareSystem(CASE + "-recommendations-" + System.nanoTime());
		cleanups.add(() -> pipeline.deleteSoftwareSystem(softwareSystem));
		pipeline.addRepresentations(softwareSystem, SimilarityCase.repository());
		return softwareSystem;
	}
}
