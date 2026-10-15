package pt.ist.socialsoftware.mono2micro.decomposition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Characterizes what the REST API does after the algorithm runs: manual editing, undo and redo,
 * metrics, comparison of two decompositions, expert import and the export to Context Mapper.
 *
 * <p>This is the regression net for the part of the pipeline the Phase 1 restructure rebuilds
 * (seam 1 of the restructure spec). Every assertion is on paths, JSON and type strings, through
 * {@link HttpDecompositionPipeline}. When a ticket changes an endpoint, it changes the path or
 * payload there and nothing here, so a red test after a port is a regression.
 *
 * <p>Each test builds its own software system from the {@code quizzes-tutor} fixture with the
 * Accesses strategy, whose algorithm run (average linkage, cut N=3) gives:
 *
 * <pre>
 * Cluster0 Course, Image, Option, Question, Topic
 * Cluster1 CourseExecution, QuestionAnswer, Quiz, QuizAnswer, QuizQuestion, User
 * Cluster2 Assessment, TopicConjunction
 * </pre>
 *
 * <p>{@link DecompositionE2ETest} pins that answer for every strategy. The edits below start
 * from it. The export needs Structure data, so it uses {@code quizzes-tutor-structure}.
 *
 * <p>A stack or fixture that is missing skips the test with the reason. The export also needs
 * the functionality refactoring tool, and skips on its own when only that is down.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("The REST pipeline after the algorithm run, characterized")
class PipelineCharacterizationE2ETest {

	private static final String CASE = "quizzes-tutor";
	private static final String STRUCTURE_CASE = "quizzes-tutor-structure";

	/** How long the refactoring tool may take before the export is given up on. */
	private static final long REFACTOR_DEADLINE_MILLIS = 120_000;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate rest;

	private final List<Runnable> cleanups = new ArrayList<>();

	/**
	 * Per test, not {@code @BeforeAll}: surefire reports a class aborted there as "Tests run: 0"
	 * with no reason, while a test aborted here is counted as skipped and keeps its message.
	 */
	@BeforeEach
	void requireStack() {
		Stack.assumeDecompositionStack();
	}

	@AfterEach
	void removeCreatedSoftwareSystems() {
		for (Runnable cleanup : cleanups) {
			try {
				cleanup.run();
			} catch (Exception e) {
				System.err.println("Cleanup failed: " + e.getMessage());
			}
		}
		cleanups.clear();
	}

	// Manual editing.

	@Test
	@DisplayName("Merge joins two clusters under a new name")
	void merge() {
		Built built = build(CASE, StrategyCase.accesses());

		built.pipeline.merge(built.decomposition, "Cluster0", "Cluster2", "Merged");

		assertThat(built.clusters()).isEqualTo(clusters(
				"Cluster1", "CourseExecution, QuestionAnswer, Quiz, QuizAnswer, QuizQuestion, User",
				"Merged", "Assessment, Course, Image, Option, Question, Topic, TopicConjunction"));
	}

	@Test
	@DisplayName("Split moves the chosen entities into a new cluster")
	void split() {
		Built built = build(CASE, StrategyCase.accesses());

		built.pipeline.split(built.decomposition, "Cluster0", "Media", built.ids("Image", "Option"));

		assertThat(built.clusters()).isEqualTo(clusters(
				"Cluster0", "Course, Question, Topic",
				"Cluster1", "CourseExecution, QuestionAnswer, Quiz, QuizAnswer, QuizQuestion, User",
				"Cluster2", "Assessment, TopicConjunction",
				"Media", "Image, Option"));
	}

	@Test
	@DisplayName("Transfer moves the chosen entities to an existing cluster")
	void transfer() {
		Built built = build(CASE, StrategyCase.accesses());

		built.pipeline.transfer(built.decomposition, "Cluster1", "Cluster2", built.ids("User"));

		assertThat(built.clusters()).isEqualTo(clusters(
				"Cluster0", "Course, Image, Option, Question, Topic",
				"Cluster1", "CourseExecution, QuestionAnswer, Quiz, QuizAnswer, QuizQuestion",
				"Cluster2", "Assessment, TopicConjunction, User"));
	}

	@Test
	@DisplayName("Rename changes a cluster's name and keeps its entities")
	void rename() {
		Built built = build(CASE, StrategyCase.accesses());

		built.pipeline.rename(built.decomposition, "Cluster2", "Assessments");

		assertThat(built.clusters()).isEqualTo(clusters(
				"Assessments", "Assessment, TopicConjunction",
				"Cluster0", "Course, Image, Option, Question, Topic",
				"Cluster1", "CourseExecution, QuestionAnswer, Quiz, QuizAnswer, QuizQuestion, User"));
	}

	@Test
	@DisplayName("Form cluster gathers entities from several clusters into a new one")
	void formCluster() {
		Built built = build(CASE, StrategyCase.accesses());

		Map<String, List<Short>> entities = new LinkedHashMap<>();
		entities.put("Cluster0", built.ids("Course"));
		entities.put("Cluster1", built.ids("CourseExecution", "User"));
		built.pipeline.formCluster(built.decomposition, "Core", entities);

		assertThat(built.clusters()).isEqualTo(clusters(
				"Cluster0", "Image, Option, Question, Topic",
				"Cluster1", "QuestionAnswer, Quiz, QuizAnswer, QuizQuestion",
				"Cluster2", "Assessment, TopicConjunction",
				"Core", "Course, CourseExecution, User"));
	}

	// Undo and redo.

	@Test
	@DisplayName("Undo and redo step back and forth through the edits")
	void undoAndRedo() {
		Built built = build(CASE, StrategyCase.accesses());
		Map<String, List<String>> original = built.clusters();

		assertThat(built.pipeline.canUndoRedo(built.decomposition)).as("before any edit").isEqualTo(undoRedo(false, false));
		assertThat(built.pipeline.undo(built.decomposition)).as("undo with nothing to undo").isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(built.pipeline.redo(built.decomposition)).as("redo with nothing to redo").isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(built.clusters()).as("after the empty undo and redo").isEqualTo(original);

		built.pipeline.merge(built.decomposition, "Cluster0", "Cluster2", "Merged");
		Map<String, List<String>> merged = built.clusters();
		built.pipeline.rename(built.decomposition, "Merged", "Content");
		Map<String, List<String>> renamed = clusters(
				"Cluster1", "CourseExecution, QuestionAnswer, Quiz, QuizAnswer, QuizQuestion, User",
				"Content", "Assessment, Course, Image, Option, Question, Topic, TopicConjunction");
		assertThat(built.clusters()).as("after merge and rename").isEqualTo(renamed);

		assertThat(built.pipeline.undo(built.decomposition)).isEqualTo(HttpStatus.OK);
		assertThat(built.clusters()).as("after undoing the rename").isEqualTo(merged);
		assertThat(built.pipeline.canUndoRedo(built.decomposition)).isEqualTo(undoRedo(true, true));

		assertThat(built.pipeline.undo(built.decomposition)).isEqualTo(HttpStatus.OK);
		assertThat(built.clusters()).as("after undoing the merge too").isEqualTo(original);
		assertThat(built.pipeline.canUndoRedo(built.decomposition)).isEqualTo(undoRedo(false, true));

		assertThat(built.pipeline.redo(built.decomposition)).isEqualTo(HttpStatus.OK);
		assertThat(built.clusters()).as("after redoing the merge").isEqualTo(merged);

		assertThat(built.pipeline.redo(built.decomposition)).isEqualTo(HttpStatus.OK);
		assertThat(built.clusters()).as("after redoing the rename").isEqualTo(renamed);
		assertThat(built.pipeline.canUndoRedo(built.decomposition)).isEqualTo(undoRedo(true, false));
	}

	@Test
	@DisplayName("A new edit after an undo discards the undone edits")
	void newEditAfterUndoDiscardsTheUndoneBranch() {
		Built built = build(CASE, StrategyCase.accesses());

		built.pipeline.merge(built.decomposition, "Cluster0", "Cluster2", "Merged");
		built.pipeline.undo(built.decomposition);
		built.pipeline.rename(built.decomposition, "Cluster2", "Assessments");

		Map<String, List<String>> renamed = clusters(
				"Assessments", "Assessment, TopicConjunction",
				"Cluster0", "Course, Image, Option, Question, Topic",
				"Cluster1", "CourseExecution, QuestionAnswer, Quiz, QuizAnswer, QuizQuestion, User");

		assertThat(built.clusters()).isEqualTo(renamed);
		assertThat(built.pipeline.canUndoRedo(built.decomposition)).as("the merge can no longer be redone").isEqualTo(undoRedo(true, false));

		assertThat(built.pipeline.redo(built.decomposition)).as("redo after the branch was discarded").isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(built.clusters()).as("a redo has nothing to bring back").isEqualTo(renamed);

		assertThat(built.pipeline.undo(built.decomposition)).isEqualTo(HttpStatus.OK);
		assertThat(built.clusters()).as("undo returns to the algorithm's clusters, not to the merge").isEqualTo(clusters(
				"Cluster0", "Course, Image, Option, Question, Topic",
				"Cluster1", "CourseExecution, QuestionAnswer, Quiz, QuizAnswer, QuizQuestion, User",
				"Cluster2", "Assessment, TopicConjunction"));
		assertThat(built.pipeline.canUndoRedo(built.decomposition)).isEqualTo(undoRedo(false, true));
	}

	// Metrics.

	@Test
	@DisplayName("The algorithm run's decomposition, cluster and functionality metrics")
	void metricsOfTheAlgorithmRun() {
		Built built = build(CASE, StrategyCase.accesses());

		JsonNode decomposition = built.pipeline.getDecomposition(built.decomposition);

		// Silhouette Score is written by the algorithm run itself, beside the calculated metrics.
		assertThat(decomposition.path("metrics").has("Silhouette Score")).isTrue();

		Golden.assertMatches("metrics/" + CASE + "/accesses/decomposition.json", metricsOf(decomposition));
		Golden.assertMatches("metrics/" + CASE + "/accesses/functionalities.json",
				functionalityMetrics(built.pipeline.getFunctionalities(built.decomposition)));
	}

	@Test
	@DisplayName("An edit makes the metrics outdated, and an update recalculates them")
	void metricsAfterAnEdit() {
		Built built = build(CASE, StrategyCase.accesses());

		built.pipeline.merge(built.decomposition, "Cluster0", "Cluster2", "Merged");

		JsonNode stored = built.pipeline.getDecomposition(built.decomposition);
		assertThat(stored.path("outdated").asBoolean()).as("outdated after the merge").isTrue();

		JsonNode updated = built.pipeline.updateDecomposition(built.decomposition);
		assertThat(updated.path("outdated").asBoolean()).as("outdated after the update").isFalse();

		Golden.assertMatches("metrics/" + CASE + "/accesses/decomposition-after-merge.json", metricsOf(updated));
	}

	// Each representation contributes its own decomposition metrics, so the metric set depends on
	// the strategy. Accesses is pinned above; these pin the two strategies that bring others.

	@Test
	@DisplayName("The Repository strategy's algorithm run adds TSR to the metrics")
	void metricsOfTheRepositoryRun() {
		Built built = build(CASE, StrategyCase.repository());

		JsonNode decomposition = built.pipeline.getDecomposition(built.decomposition);
		assertThat(decomposition.path("metrics").has("TSR")).as("TSR, from the Repository representation").isTrue();

		Golden.assertMatches("metrics/" + CASE + "/repository/decomposition.json", metricsOf(decomposition));
	}

	@Test
	@DisplayName("The Structure strategy's algorithm run, whose metrics come from the Structure representation")
	void metricsOfTheStructureRun() {
		Built built = build(STRUCTURE_CASE, StrategyCase.structure());

		Golden.assertMatches("metrics/" + STRUCTURE_CASE + "/structure/decomposition.json",
				metricsOf(built.pipeline.getDecomposition(built.decomposition)));
	}

	// Comparison.

	@Test
	@DisplayName("MoJoFM and Purity between the algorithm run and an expert clustering")
	void comparison() {
		Built built = build(CASE, StrategyCase.accesses());
		String expert = built.pipeline.createExpertDecomposition(built.similarity, "Expert", expertClustering(built));

		JsonNode comparison = built.pipeline.compare(built.decomposition, expert);

		ObjectNode analyses = MAPPER.createObjectNode();
		for (JsonNode analysis : comparison.path("analysisList")) {
			ObjectNode copy = analysis.deepCopy();
			// The pairs MoJo got wrong are listed in an order nothing promises; their count is
			// already pinned by falsePositive and falseNegative.
			copy.remove("falsePairs");
			analyses.set(analysis.path("type").asText(), copy);
		}
		assertThat(analyses.has("MOJO")).as("a MoJo analysis").isTrue();
		assertThat(analyses.has("PURITY")).as("a Purity analysis").isTrue();

		Golden.assertMatches("comparison/" + CASE + "/accesses-vs-expert.json", analyses);
	}

	// Expert import.

	@Test
	@DisplayName("An expert import creates a decomposition with the uploaded clusters")
	void expertImport() {
		Built built = build(CASE, StrategyCase.accesses());

		String expert = built.pipeline.createExpertDecomposition(built.similarity, "Expert", expertClustering(built));

		assertThat(built.pipeline.getClusters(expert)).isEqualTo(clusters(
				"Content", "Course, Image, Option, Question, Topic, TopicConjunction",
				"Quizzes", "Assessment, CourseExecution, QuestionAnswer, Quiz, QuizAnswer, QuizQuestion, User"));

		JsonNode decomposition = built.pipeline.getDecomposition(expert);
		assertThat(decomposition.path("expert").asBoolean()).isTrue();
		assertThat(decomposition.path("metrics").has("Silhouette Score")).as("no algorithm ran, so no Silhouette Score").isFalse();

		Golden.assertMatches("metrics/" + CASE + "/accesses/expert-decomposition.json", metricsOf(decomposition));
	}

	// Export to Context Mapper.

	@Test
	@DisplayName("Export is refused for a decomposition without Structure data")
	void exportWithoutStructure() {
		Built built = build(CASE, StrategyCase.accesses());

		// Today the frontend hides the button and the backend fails with a 400 (D-018 moves the
		// gate to the backend).
		assertThat(built.pipeline.export(built.decomposition).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	@DisplayName("Export is refused until the functionalities have been refactored")
	void exportBeforeRefactoring() {
		Built built = build(STRUCTURE_CASE, StrategyCase.structure());

		assertThat(built.pipeline.export(built.decomposition).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	@DisplayName("Export to Context Mapper, for a decomposition with Structure data and refactored functionalities")
	void export() throws IOException, InterruptedException {
		Stack.assumeRefactorTool();
		Built built = build(STRUCTURE_CASE, StrategyCase.structure());

		built.pipeline.refactorFunctionalities(Stack.REFACTOR_TOOL_ADDRESS, built.decomposition);
		ResponseEntity<byte[]> response = exportOnceRefactored(built);

		JsonNode contract = zipEntry(response.getBody(), "m2m_decomposition.json");

		assertThat(contract.path("name").asText()).as("the contract is named after the software system").isEqualTo(built.softwareSystem);
		assertThat(contract.path("entities").size()).as("entities, from the Structure data").isGreaterThan(0);
		assertThat(contract.path("functionalities").size()).as("functionalities, from the refactoring").isGreaterThan(0);

		assertThat(HttpDecompositionPipeline.clusterNames(contract.path("clusters"))).as("the exported clusters are the decomposition's")
				.isEqualTo(built.clusters());
	}

	/**
	 * Polls the export until the refactoring tool has written every functionality. Until then
	 * the export fails in one of two ways: a 404 until the tool has stored its first results,
	 * then a 400 while a functionality has no orchestrator yet. Any other status is not "not
	 * yet", so it stops the polling and fails at once.
	 */
	private ResponseEntity<byte[]> exportOnceRefactored(Built built) throws InterruptedException {
		long deadline = System.currentTimeMillis() + REFACTOR_DEADLINE_MILLIS;
		ResponseEntity<byte[]> response = built.pipeline.export(built.decomposition);

		while (notYetRefactored(response.getStatusCode()) && System.currentTimeMillis() < deadline) {
			Thread.sleep(1000);
			response = built.pipeline.export(built.decomposition);
		}

		assertThat(response.getStatusCode())
				.as("export within %d s of refactoring %s", REFACTOR_DEADLINE_MILLIS / 1000, built.decomposition)
				.isEqualTo(HttpStatus.OK);
		return response;
	}

	private static boolean notYetRefactored(HttpStatus status) {
		return status == HttpStatus.NOT_FOUND || status == HttpStatus.BAD_REQUEST;
	}

	// Building blocks.

	/** A software system with one algorithm run, removed after the test. */
	private Built build(String caseName, StrategyCase strategyCase) {
		List<String> missing = RepresentationFiles.missing(caseName, strategyCase.fixtureSuffixes());
		assumeTrue(missing.isEmpty(), "No fixture for " + caseName + " / " + strategyCase.label() + ": missing "
				+ String.join(", ", missing) + " under representations/" + caseName + "/. See that folder's README.md.");

		HttpDecompositionPipeline pipeline = new HttpDecompositionPipeline(rest, port, caseName);
		String softwareSystem = caseName + "-characterization-" + System.nanoTime();

		pipeline.createSoftwareSystem(softwareSystem);
		cleanups.add(() -> pipeline.deleteSoftwareSystem(softwareSystem));
		pipeline.addRepresentations(softwareSystem, strategyCase);

		String strategy = pipeline.createStrategy(softwareSystem, strategyCase);
		String similarity = pipeline.createSimilarity(strategy, strategyCase);
		String decomposition = pipeline.createDecomposition(similarity);

		return new Built(pipeline, softwareSystem, similarity, decomposition);
	}

	/** Two clusters that cut across the algorithm's three, so the comparison has something to find. */
	private static byte[] expertClustering(Built built) {
		Map<String, List<Short>> clusters = new LinkedHashMap<>();
		clusters.put("Content", built.ids("Course", "Image", "Option", "Question", "Topic", "TopicConjunction"));
		clusters.put("Quizzes", built.ids("Assessment", "CourseExecution", "QuestionAnswer", "Quiz", "QuizAnswer", "QuizQuestion", "User"));

		Map<String, Object> file = new LinkedHashMap<>();
		file.put("clusters", clusters);
		try {
			return MAPPER.writeValueAsBytes(file);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Decomposition metrics, and each cluster's metrics by cluster name. */
	private static JsonNode metricsOf(JsonNode decomposition) {
		ObjectNode metrics = MAPPER.createObjectNode();
		metrics.set("decomposition", decomposition.path("metrics"));

		ObjectNode clusters = metrics.putObject("clusters");
		for (Iterator<Map.Entry<String, JsonNode>> it = decomposition.path("clusters").fields(); it.hasNext();) {
			Map.Entry<String, JsonNode> cluster = it.next();
			clusters.set(cluster.getKey(), cluster.getValue().path("metrics"));
		}
		return metrics;
	}

	/** Each functionality's metrics, and those of each of its redesigns, by name. */
	private static JsonNode functionalityMetrics(JsonNode functionalitiesAndClusters) {
		ObjectNode metrics = MAPPER.createObjectNode();
		for (JsonNode functionality : functionalitiesAndClusters.path("functionalities")) {
			ObjectNode entry = metrics.putObject(functionality.path("name").asText());
			entry.set("metrics", functionality.path("metrics"));

			ObjectNode redesigns = entry.putObject("redesigns");
			for (JsonNode redesign : functionality.path("functionalityRedesigns"))
				redesigns.set(redesign.path("name").asText(), redesign.path("metrics"));
		}
		return metrics;
	}

	private static JsonNode zipEntry(byte[] zip, String name) throws IOException {
		try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
			for (ZipEntry entry = in.getNextEntry(); entry != null; entry = in.getNextEntry()) {
				if (entry.getName().equals(name))
					return MAPPER.readTree(new String(readAll(in), StandardCharsets.UTF_8));
			}
		}
		throw new AssertionError("The export has no " + name);
	}

	private static byte[] readAll(ZipInputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		for (int read = in.read(buffer); read != -1; read = in.read(buffer))
			out.write(buffer, 0, read);
		return out.toByteArray();
	}

	/** Alternating cluster name and comma-separated entity names. */
	private static Map<String, List<String>> clusters(String... nameThenEntities) {
		Map<String, List<String>> clusters = new TreeMap<>();
		for (int i = 0; i < nameThenEntities.length; i += 2) {
			List<String> entities = new ArrayList<>(Arrays.asList(nameThenEntities[i + 1].split(", ")));
			Collections.sort(entities);
			clusters.put(nameThenEntities[i], entities);
		}
		return clusters;
	}

	private static JsonNode undoRedo(boolean undo, boolean redo) {
		ObjectNode node = MAPPER.createObjectNode();
		node.put("undo", undo);
		node.put("redo", redo);
		return node;
	}

	/** One software system's names, and the pipeline that built it. */
	private static final class Built {
		final HttpDecompositionPipeline pipeline;
		final String softwareSystem;
		final String similarity;
		final String decomposition;

		private Map<String, Short> ids;

		Built(HttpDecompositionPipeline pipeline, String softwareSystem, String similarity, String decomposition) {
			this.pipeline = pipeline;
			this.softwareSystem = softwareSystem;
			this.similarity = similarity;
			this.decomposition = decomposition;
		}

		Map<String, List<String>> clusters() {
			return pipeline.getClusters(decomposition);
		}

		/** Entity ids by name, from the algorithm run. Ids do not change with edits. */
		List<Short> ids(String... entityNames) {
			if (ids == null)
				ids = pipeline.entityIds(decomposition);

			List<Short> result = new ArrayList<>();
			for (String name : entityNames) {
				Short id = ids.get(name);
				if (id == null)
					throw new IllegalArgumentException("No entity " + name + " in " + decomposition + "; it has " + ids.keySet());
				result.add(id);
			}
			return result;
		}
	}
}
