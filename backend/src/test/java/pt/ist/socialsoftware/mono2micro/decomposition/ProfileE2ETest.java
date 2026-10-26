package pt.ist.socialsoftware.mono2micro.decomposition;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Profiles over the REST API (D-017): a profile is an immutable record, a move offers new versions, and the units no
 * profile selects are listed as Unassigned. Built on the {@code quizzes-tutor} fixture's Accesses representation.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Profiles as immutable Expert knowledge records")
class ProfileE2ETest {

	private static final String CASE = "quizzes-tutor";

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate rest;

	private HttpDecompositionPipeline pipeline;
	private String softwareSystem;
	private String accesses;

	@BeforeEach
	void buildASoftwareSystemWithAccesses() {
		Stack.assumeDecompositionStack();
		StrategyCase strategyCase = StrategyCase.accesses();
		List<String> missing = RepresentationFiles.missing(CASE, strategyCase.fixtureSuffixes());
		assumeTrue(missing.isEmpty(), "No fixture for " + CASE + ": missing " + String.join(", ", missing));

		pipeline = new HttpDecompositionPipeline(rest, port, CASE);
		softwareSystem = pipeline.createSoftwareSystem(CASE + "-profiles-" + System.nanoTime());
		pipeline.addRepresentations(softwareSystem, strategyCase);
		accesses = pipeline.representationId(softwareSystem, "Accesses");
	}

	@AfterEach
	void removeTheSoftwareSystem() {
		if (softwareSystem != null)
			pipeline.deleteSoftwareSystem(softwareSystem);
	}

	@Test
	@DisplayName("A move offers a new version of the profile, and Unassigned is what no profile selects")
	void moveOffersANewVersion() {
		List<String> every = strings(pipeline.getProfiles(accesses).path("unassigned"));
		List<String> moved = every.subList(0, 2);
		String original = pipeline.createProfile(accesses, "Quizzes");

		pipeline.moveUnits(accesses, moved, original);

		JsonNode profiles = pipeline.getProfiles(accesses);
		assertThat(profiles.path("profiles")).hasSize(1);
		JsonNode version = profiles.path("profiles").get(0);
		assertThat(version.path("id").asText()).isNotEqualTo(original);
		assertThat(version.path("name").asText()).isEqualTo("Quizzes");
		assertThat(strings(version.path("units"))).containsExactlyElementsOf(moved);
		assertThat(strings(profiles.path("unassigned"))).containsExactlyElementsOf(every.subList(2, every.size()));
	}

	/** Behavior change allowed by D-017: the deleted profile's functionalities go back to Unassigned. */
	@Test
	@DisplayName("Deleting a profile sends its functionalities to Unassigned")
	void deleteSendsUnitsToUnassigned() {
		List<String> every = strings(pipeline.getProfiles(accesses).path("unassigned"));
		pipeline.moveUnits(accesses, every.subList(0, 2), pipeline.createProfile(accesses, "Quizzes"));

		pipeline.deleteProfile(pipeline.getProfiles(accesses).path("profiles").get(0).path("id").asText());

		JsonNode profiles = pipeline.getProfiles(accesses);
		assertThat(profiles.path("profiles")).isEmpty();
		assertThat(strings(profiles.path("unassigned"))).containsExactlyElementsOf(every);
	}

	@Test
	@DisplayName("A move leaves what an existing Similarity scoped by the profile produces unchanged")
	void moveLeavesAScopedSimilarityUnchanged() {
		List<String> selected = strings(pipeline.getProfiles(accesses).path("unassigned")).subList(0, 20);
		pipeline.moveUnits(accesses, selected, pipeline.createProfile(accesses, "Quizzes"));
		String similarity = scopedSimilarity(profileId("Quizzes"));
		assertThat(pipeline.functionalityNames(pipeline.createDecomposition(similarity))).isEqualTo(selected);

		pipeline.moveUnits(accesses, selected.subList(0, 5), null);

		assertThat(pipeline.functionalityNames(pipeline.createDecomposition(similarity)))
				.as("a decomposition made after the move")
				.isEqualTo(selected);
	}

	@Test
	@DisplayName("Deleting a profile a Similarity uses retires it, and the Similarity keeps working")
	void deleteRetiresAProfileInUse() {
		List<String> every = strings(pipeline.getProfiles(accesses).path("unassigned"));
		List<String> selected = every.subList(0, 20);
		pipeline.moveUnits(accesses, selected, pipeline.createProfile(accesses, "Quizzes"));
		String similarity = scopedSimilarity(profileId("Quizzes"));

		pipeline.deleteProfile(profileId("Quizzes"));

		JsonNode profiles = pipeline.getProfiles(accesses);
		assertThat(profiles.path("profiles")).as("a retired profile is not offered").isEmpty();
		assertThat(strings(profiles.path("unassigned"))).containsExactlyElementsOf(every);
		assertThat(pipeline.functionalityNames(pipeline.createDecomposition(similarity))).isEqualTo(selected);
	}

	@Test
	@DisplayName("A Similarity with no profile is built on every functionality")
	void noProfileMeansEveryFunctionality() {
		List<String> every = strings(pipeline.getProfiles(accesses).path("unassigned"));
		pipeline.moveUnits(accesses, every.subList(0, 20), pipeline.createProfile(accesses, "Quizzes"));
		StrategyCase unscoped = StrategyCase.accesses();

		String similarity = pipeline.createSimilarity(pipeline.createStrategy(softwareSystem, unscoped), unscoped);

		assertThat(pipeline.functionalityNames(pipeline.createDecomposition(similarity))).isEqualTo(every);
	}

	/**
	 * Behavior change allowed by D-017: a vectorization kind takes no profile, so it is built on every functionality.
	 * Before, it read the {@code Generic} profile, which lost every functionality moved out of it.
	 */
	@Test
	@DisplayName("A vectorization Similarity keeps the functionalities moved into a profile")
	void vectorizationKeepsMovedFunctionalities() {
		StrategyCase entityVectorization = StrategyCase.entityVectorization();
		assumeTrue(RepresentationFiles.missing(CASE, entityVectorization.fixtureSuffixes()).isEmpty(), "No code embeddings fixture for " + CASE);
		String system = pipeline.createSoftwareSystem(CASE + "-profiles-vectorization-" + System.nanoTime());
		try {
			pipeline.addRepresentations(system, entityVectorization);
			String systemAccesses = pipeline.representationId(system, "Accesses");
			List<String> every = strings(pipeline.getProfiles(systemAccesses).path("unassigned"));
			pipeline.moveUnits(systemAccesses, every.subList(0, 20), pipeline.createProfile(systemAccesses, "Quizzes"));

			String similarity = pipeline.createSimilarity(pipeline.createStrategy(system, entityVectorization), entityVectorization);

			assertThat(pipeline.functionalityNames(pipeline.createDecomposition(similarity))).isEqualTo(every);
		} finally {
			pipeline.deleteSoftwareSystem(system);
		}
	}

	@Test
	@DisplayName("A Similarity request is a duplicate only under the same profile version")
	void duplicateCheckComparesTheProfileId() {
		List<String> every = strings(pipeline.getProfiles(accesses).path("unassigned"));
		pipeline.moveUnits(accesses, every.subList(0, 20), pipeline.createProfile(accesses, "Quizzes"));
		String version1 = profileId("Quizzes");
		String similarity = scopedSimilarity(version1);
		assertThat(scopedSimilarity(version1)).as("the same request again").isEqualTo(similarity);

		pipeline.moveUnits(accesses, every.subList(0, 5), null);

		assertThat(scopedSimilarity(profileId("Quizzes"))).as("the same name, a new version").isNotEqualTo(similarity);
	}

	@Test
	@DisplayName("A representation holds no profiles: they are Expert knowledge, not representation data")
	void representationsHoldNoProfiles() {
		pipeline.createProfile(accesses, "Quizzes");

		assertThat(pipeline.getRepresentation(accesses).has("profiles")).isFalse();
	}

	private String scopedSimilarity(String profileId) {
		StrategyCase scoped = StrategyCase.accesses().withProfile(profileId);
		return pipeline.createSimilarity(pipeline.createStrategy(softwareSystem, scoped), scoped);
	}

	private String profileId(String profileName) {
		for (JsonNode profile : pipeline.getProfiles(accesses).path("profiles"))
			if (profile.path("name").asText().equals(profileName))
				return profile.path("id").asText();
		throw new AssertionError("No profile " + profileName + " offered.");
	}

	private static List<String> strings(JsonNode array) {
		List<String> strings = new ArrayList<>();
		array.forEach(element -> strings.add(element.asText()));
		return strings;
	}
}
