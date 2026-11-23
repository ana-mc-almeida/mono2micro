package pt.ist.socialsoftware.mono2micro.decomposition;

import pt.ist.socialsoftware.mono2micro.utils.Constants;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The services an integration test needs, checked before anything is created.
 *
 * <p>A service that is down <b>skips</b> the test, naming the service and the command that
 * brings it up. The suites tagged {@code integration} are not part of a plain {@code mvn test},
 * so a skip here can only happen in a run that asked for them, where the reason is printed.
 *
 * <p>With {@code REQUIRE_STACK=true} in the environment, a service that is down <b>fails</b>
 * the test instead. CI's e2e job sets it, so that a stack that never came up cannot turn the
 * job green with every test skipped.
 */
final class Stack {

	private static final boolean REQUIRED = Boolean.parseBoolean(System.getenv("REQUIRE_STACK"));

	/** Where the functionality refactoring tool listens, as in the frontend's constants.js. */
	static final String REFACTOR_TOOL_ADDRESS = System.getenv().getOrDefault("REFACTOR_TOOL_ADDRESS", "http://localhost:5001");

	private Stack() {}

	/** MongoDB and the scripts service, which every decomposition needs. */
	static void assumeDecompositionStack() {
		// Where spring.data.mongodb.uri points: MONGO_DB, else specific.properties' default.
		// Only the first host of the URI is probed.
		URI mongo = URI.create(System.getenv().getOrDefault("MONGO_DB", "mongodb://localhost:27017").split(",")[0]);
		assumeReachable(mongo.getHost(), mongo.getPort() == -1 ? 27017 : mongo.getPort(), "MongoDB (" + mongo + ")",
				"docker compose up -d mongo scripts");

		// Read from the same static field the production code uses, so the check cannot
		// disagree with where the clustering call will actually go.
		URI scripts = URI.create(Constants.SCRIPTS_ADDRESS);
		int scriptsPort = scripts.getPort() == -1 ? 5002 : scripts.getPort();
		assumeReachable(scripts.getHost(), scriptsPort, "The scripts service (" + Constants.SCRIPTS_ADDRESS + ")",
				"docker compose up -d mongo scripts");
	}

	/**
	 * The functionality refactoring tool, which the export to Context Mapper reads from. It
	 * reads the decomposition straight from MongoDB, so it must share the backend's database.
	 */
	static void assumeRefactorTool() {
		URI tool = URI.create(REFACTOR_TOOL_ADDRESS);
		assumeReachable(tool.getHost(), tool.getPort() == -1 ? 5001 : tool.getPort(),
				"The functionality refactoring tool (" + REFACTOR_TOOL_ADDRESS + ")", "docker compose up -d functionality_refactor");
	}

	private static void assumeReachable(String host, int port, String what, String command) {
		boolean reachable;
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress(host, port), 2000);
			reachable = true;
		} catch (IOException e) {
			reachable = false;
		}

		String reason = what + " is not reachable on " + host + ":" + port + "."
				+ " This test drives the real stack; it does not mock it. Run: " + command;

		if (REQUIRED && !reachable)
			throw new IllegalStateException(reason + " (REQUIRE_STACK is set, so this fails rather than skips.)");
		assumeTrue(reachable, reason);
	}
}
