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

	private static List<String> strings(JsonNode array) {
		List<String> strings = new ArrayList<>();
		array.forEach(element -> strings.add(element.asText()));
		return strings;
	}
}
