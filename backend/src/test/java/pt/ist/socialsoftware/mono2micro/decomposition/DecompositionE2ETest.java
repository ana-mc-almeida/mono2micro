package pt.ist.socialsoftware.mono2micro.decomposition;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import pt.ist.socialsoftware.mono2micro.softwareSystem.SoftwareSystemService;
import pt.ist.socialsoftware.mono2micro.decomposition.service.DecompositionService;
import pt.ist.socialsoftware.mono2micro.representation.service.RepresentationService;
import pt.ist.socialsoftware.mono2micro.similarity.service.SimilarityService;
import pt.ist.socialsoftware.mono2micro.strategy.service.StrategyService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Asserts which clusters the tool produces from uploaded representations, for every strategy
 * it supports.
 *
 * <p>The clusters are compared with those today's tool produces, recorded under
 * {@code characterization/clusters/<case>/<strategy>.json} (see {@link Golden}), so that the
 * restructure of the backend's modularity (and the deferred Spring Boot upgrade) has a net
 * underneath it that catches a changed answer, not only a missing one. The rest of the pipeline
 * — editing, undo and redo, metrics, comparison, expert import and export — is characterized
 * by {@link PipelineCharacterizationE2ETest}.
 *
 * <p>Covering every strategy matters because each one is registered by <em>type string</em>
 * across five registries that do not agree on spelling. Miss one and the variant breaks at
 * runtime rather than compile time, so a suite covering one strategy would leave the other six
 * free to rot. The strategies are described as data in {@link StrategyCase}.
 *
 * <p>A case is covered only for the strategies whose fixtures it has; the rest are skipped and
 * listed after the run. No case has all of them. See {@link #requireFixtures}.
 *
 * <p>Each case runs twice, once per {@link DecompositionPipeline} implementation. The HTTP
 * implementation is the net and should survive that refactor untouched; the service
 * implementation is a diagnostic that localizes failures. See {@link DecompositionPipeline}.
 *
 * <p><b>Requires the real stack.</b> Mocking is impractical here: {@code ContextManager}
 * builds a second ApplicationContext that domain objects fetch beans from, so {@code @MockBean}
 * is invisible below the service layer, and clustering genuinely happens in the Python service.
 *
 * <pre>
 * docker compose up -d mongo scripts
 * </pre>
 *
 * <p>A stack that is down <b>skips</b> the suite with the command that brings it up (see
 * {@link Stack}), and so does an absent fixture, which {@link #reportSkips()} also lists. A
 * fixture that is present but broken still fails.
 */
// Excluded from the default `mvn test` by surefire's <excludedGroups>, since the
// stack requirement above cannot be met offline; the e2e job opts back in with -Dexcluded.groups=.
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Decomposition generation, end to end")
class DecompositionE2ETest {

	/** Fixture folders under {@code src/test/resources/representations/}. */
	private static final List<String> CASES = Arrays.asList("quizzes-tutor", "quizzes-tutor-structure", "spring-petclinic");

	/** {@link #CASES} × every strategy — the full matrix this suite covers. */
	static Stream<Arguments> strategies() {
		return CASES.stream().flatMap(caseName -> StrategyCase.all().stream().map(strategy -> Arguments.of(caseName, strategy)));
	}

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate rest;
	@Autowired
	private SoftwareSystemService softwareSystemService;
	@Autowired
	private RepresentationService representationService;
	@Autowired
	private StrategyService strategyService;
	@Autowired
	private SimilarityService similarityService;
	@Autowired
	private DecompositionService decompositionService;

	private final List<Runnable> cleanups = new ArrayList<>();

	/** Case/strategy combinations skipped for want of fixtures, reported by {@link #reportSkips()}. */
	private static final Set<String> skipped = new LinkedHashSet<>();

	/**
	 * Per test, not {@code @BeforeAll}: surefire reports a class aborted there as "Tests run: 0"
	 * with no reason, while a test aborted here is counted as skipped and keeps its message.
	 */
	@BeforeEach
	void requireStack() {
		Stack.assumeDecompositionStack();
	}

	@ParameterizedTest(name = "{1} produces today's clusters for {0}")
	@MethodSource("strategies")
	void generatesDecomposition(String caseName, StrategyCase strategyCase) {
		requireFixtures(caseName, strategyCase);

		for (DecompositionPipeline pipeline : pipelines(caseName)) {
			// Unique per run, per layer and per strategy: re-uploading a representation type to
			// an existing software system throws "Re-sending representations is not allowed."
			String softwareSystemName = caseName + "-" + strategyCase.label() + "-"
					+ pipeline.layer() + "-" + System.currentTimeMillis();

			String softwareSystemId = pipeline.createSoftwareSystem(softwareSystemName);
			cleanups.add(() -> pipeline.deleteSoftwareSystem(softwareSystemId));

			pipeline.addRepresentations(softwareSystemId, strategyCase);

			// Guards a silent failure: RepresentationService returns without doing anything
			// when both request parameters bind to null, which would otherwise surface several
			// steps later as an unrelated error.
			assertThat(pipeline.representationCount(softwareSystemId))
					.as("representations uploaded via %s", pipeline.layer())
					.isEqualTo(strategyCase.representations().size());

			String strategyId = pipeline.createStrategy(softwareSystemId, strategyCase);
			String similarityId = pipeline.createSimilarity(strategyId, strategyCase);
			String decompositionId = pipeline.createDecomposition(similarityId);

			Map<String, List<String>> clusters = pipeline.getClusters(decompositionId);

			assertThat(clusters.values())
					.as("at least one cluster holds entities (via %s)", pipeline.layer())
					.anyMatch(entities -> !entities.isEmpty());

			Golden.assertMatches("clusters/" + caseName + "/" + strategyCase.label() + ".json", Golden.toJson(clusters));
		}
	}

	@AfterEach
	void removeCreatedSoftwareSystems() {
		for (Runnable cleanup : cleanups) {
			try {
				cleanup.run();
			} catch (Exception e) {
				// Mongo state lives in a volume across runs; a failed cleanup should not mask
				// the assertion that actually ran.
				System.err.println("Cleanup failed: " + e.getMessage());
			}
		}
		cleanups.clear();
	}

	/**
	 * Prints the coverage gap, so that a run skipping much of its matrix is not read as a run
	 * that covered it. Build tools report a skip far more quietly than a failure, and this
	 * suite's whole point is that an uncovered strategy should be visible.
	 */
	@AfterAll
	static void reportSkips() {
		if (skipped.isEmpty())
			return;

		System.err.println("\n" + skipped.size() + " case/strategy combination(s) skipped for want of fixtures:");
		for (String skip : skipped)
			System.err.println("  - " + skip);
		System.err.println("See backend/src/test/resources/representations/README.md.\n");
	}

	private List<DecompositionPipeline> pipelines(String caseName) {
		return Arrays.asList(
				new HttpDecompositionPipeline(rest, port, caseName),
				new ServiceDecompositionPipeline(
						softwareSystemService, representationService, strategyService,
						similarityService, decompositionService, caseName));
	}

	/**
	 * Checks the fixtures this strategy needs, before anything is created.
	 *
	 * <p>A case whose fixtures are not all present is <b>skipped</b>, naming the files that are
	 * missing. No case holds every fixture: {@code quizzes-tutor} has no structure file, the
	 * structure case has no repository or code-embeddings files, and no case but
	 * {@code quizzes-tutor} has code embeddings — those are collector output that not every
	 * software system can currently produce. Dropping the uncovered combinations from {@link #CASES}
	 * would hide that they are uncovered; failing the build for files nobody can obtain would
	 * make the suite useless.
	 *
	 * <p>The skip is deliberately narrow, which is what keeps it from becoming the green-but-
	 * vacuous run this suite exists to replace. It asks only whether the files are on the
	 * classpath, never whether the pipeline works — supply them and the case runs and fails
	 * loudly. A fixture that is present but unreadable or empty still fails.
	 *
	 * <p>{@link #reportSkips()} prints the resulting coverage gap after the run, so that a
	 * suite skipping half its matrix cannot be mistaken for a suite that passed.
	 */
	private void requireFixtures(String caseName, StrategyCase strategyCase) {
		List<String> missing = RepresentationFiles.missing(caseName, strategyCase.fixtureSuffixes());

		if (!missing.isEmpty()) {
			skipped.add(caseName + " / " + strategyCase.label() + " — missing " + String.join(", ", missing));

			assumeTrue(false,
					"No fixture for " + caseName + " / " + strategyCase.label() + ": missing "
							+ String.join(", ", missing) + " under representations/" + caseName + "/."
							+ " That strategy is not covered for this case."
							+ " See that folder's README.md.");
		}

		// Present but empty or unreadable is a broken fixture, not an absent one: fail.
		RepresentationFiles.requirePresent(caseName, strategyCase.fixtureSuffixes());
	}
}
