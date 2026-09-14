package pt.ist.socialsoftware.mono2micro.decomposition;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import pt.ist.socialsoftware.mono2micro.cluster.Cluster;
import pt.ist.socialsoftware.mono2micro.codebase.CodebaseService;
import pt.ist.socialsoftware.mono2micro.decomposition.service.DecompositionService;
import pt.ist.socialsoftware.mono2micro.representation.service.RepresentationService;
import pt.ist.socialsoftware.mono2micro.similarity.service.SimilarityService;
import pt.ist.socialsoftware.mono2micro.strategy.service.StrategyService;
import pt.ist.socialsoftware.mono2micro.utils.Constants;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts that the tool produces <em>a</em> decomposition from uploaded representations.
 *
 * <p>Deliberately says nothing about decomposition quality — no cohesion, coupling or
 * complexity thresholds. The question is whether the pipeline still runs end to end, so that
 * an upcoming refactor of the backend's modularity (and the deferred Spring Boot upgrade) has
 * a net underneath it.
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
 * <p>Preconditions <b>fail</b> rather than skip. A skipped test that reports green is the
 * exact failure mode this suite exists to replace — the pre-existing {@code contextLoads()}
 * smoke test passes with Mongo switched off.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Decomposition generation, end to end")
class DecompositionE2ETest {

    /** Fixture folders under {@code src/test/resources/representations/}. */
    private static final List<String> CASES = Arrays.asList("quizzes-tutor");

    private static final int MONGO_PORT = 27017;

    @LocalServerPort
    private int port;

    @Autowired private TestRestTemplate rest;
    @Autowired private CodebaseService codebaseService;
    @Autowired private RepresentationService representationService;
    @Autowired private StrategyService strategyService;
    @Autowired private SimilarityService similarityService;
    @Autowired private DecompositionService decompositionService;

    private final List<Runnable> cleanups = new ArrayList<>();

    @BeforeAll
    static void requireStackAndFixtures() {
        requireReachable("localhost", MONGO_PORT, "MongoDB");
        requireScriptsService();
        CASES.forEach(RepresentationFiles::requirePresent);
    }

    static Stream<String> cases() {
        return CASES.stream();
    }

    @ParameterizedTest(name = "{0} produces a decomposition")
    @MethodSource("cases")
    void generatesDecomposition(String caseName) {
        for (DecompositionPipeline pipeline : pipelines()) {
            // Unique per run and per layer: re-uploading a representation type to an existing
            // codebase throws "Re-sending representations is not allowed."
            String codebaseName = caseName + "-" + pipeline.layer() + "-" + System.currentTimeMillis();

            pipeline.createCodebase(codebaseName);
            cleanups.add(() -> pipeline.deleteCodebase(codebaseName));

            pipeline.addAccessesRepresentations(
                    codebaseName,
                    RepresentationFiles.idToEntity(caseName),
                    RepresentationFiles.accesses(caseName));

            // Guards a silent failure: RepresentationService returns without doing anything
            // when both request parameters bind to null, which would otherwise surface several
            // steps later as an unrelated error.
            assertThat(pipeline.representationCount(codebaseName))
                    .as("representations uploaded via %s", pipeline.layer())
                    .isEqualTo(2);

            String strategyName = pipeline.createStrategy(codebaseName);
            String similarityName = pipeline.createSimilarity(strategyName);
            String decompositionName = pipeline.createDecomposition(similarityName);

            Map<String, Cluster> clusters = pipeline.getClusters(decompositionName);

            assertThat(clusters)
                    .as("clusters of %s via %s", decompositionName, pipeline.layer())
                    .isNotEmpty();

            assertThat(clusters.values())
                    .as("at least one cluster holds entities (via %s)", pipeline.layer())
                    .anyMatch(cluster -> !cluster.getElements().isEmpty());
        }
    }

    @AfterEach
    void removeCreatedCodebases() {
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

    private List<DecompositionPipeline> pipelines() {
        return Arrays.asList(
                new HttpDecompositionPipeline(rest, port),
                new ServiceDecompositionPipeline(
                        codebaseService, representationService, strategyService,
                        similarityService, decompositionService));
    }

    private static void requireReachable(String host, int port, String what) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
        } catch (IOException e) {
            throw new IllegalStateException(
                    what + " is not reachable on " + host + ":" + port + "."
                            + "\nThis test drives the real stack; it does not mock it."
                            + "\n\nRun: docker compose up -d mongo scripts", e);
        }
    }

    private static void requireScriptsService() {
        // Read from the same static field the production code uses, so the check cannot
        // disagree with where the clustering call will actually go.
        URI scripts = URI.create(Constants.SCRIPTS_ADDRESS);
        int scriptsPort = scripts.getPort() == -1 ? 5002 : scripts.getPort();
        requireReachable(scripts.getHost(), scriptsPort, "The scripts service (" + Constants.SCRIPTS_ADDRESS + ")");
    }
}
