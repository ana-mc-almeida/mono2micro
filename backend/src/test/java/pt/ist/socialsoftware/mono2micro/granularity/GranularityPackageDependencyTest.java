package pt.ist.socialsoftware.mono2micro.granularity;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins ticket 04 of the target architecture: {@code granularity/} is the shared vocabulary both representation subtrees import, so it imports no module
 * of the tool back, subpackages included. A violation would form a package cycle.
 */
public class GranularityPackageDependencyTest {

	private static final Path GRANULARITY_SOURCES = Paths.get("src/main/java/pt/ist/socialsoftware/mono2micro/granularity");
	private static final String TOOL = "pt.ist.socialsoftware.mono2micro.";

	@Test
	public void granularityImportsNoModuleOfTheTool() throws IOException {
		List<String> imports;
		try (Stream<Path> files = Files.walk(GRANULARITY_SOURCES)) {
			imports = files.filter(file -> file.toString().endsWith(".java")).flatMap(GranularityPackageDependencyTest::lines)
					.map(String::trim)
					.filter(line -> line.startsWith("import " + TOOL) || line.startsWith("import static " + TOOL))
					.collect(Collectors.toList());
		}

		assertThat(Files.exists(GRANULARITY_SOURCES.resolve("Element.java"))).as("granularity/ holds Element").isTrue();
		assertThat(imports).allMatch(line -> line.contains(TOOL + "granularity."), "imports only granularity/");
	}

	private static Stream<String> lines(Path file) {
		try {
			return Files.readAllLines(file).stream();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
