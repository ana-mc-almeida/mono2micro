package pt.ist.socialsoftware.mono2micro.feature;

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
 * Pins D-022: {@code feature/} is the shared contract every family imports, so it imports no
 * module of the tool back, only {@code utils/}. A violation would form a package cycle.
 */
public class FeaturePackageDependencyTest {

	private static final Path FEATURE_SOURCES = Paths.get("src/main/java/pt/ist/socialsoftware/mono2micro/feature");
	private static final String TOOL = "pt.ist.socialsoftware.mono2micro.";

	@Test
	public void featureImportsNoModuleOfTheTool() throws IOException {
		List<String> imports;
		try (Stream<Path> files = Files.list(FEATURE_SOURCES)) {
			imports = files.flatMap(FeaturePackageDependencyTest::lines)
					.map(String::trim)
					.filter(line -> line.startsWith("import " + TOOL) || line.startsWith("import static " + TOOL))
					.collect(Collectors.toList());
		}

		assertThat(imports).isNotEmpty();
		assertThat(imports).allMatch(line -> line.contains(TOOL + "feature.") || line.contains(TOOL + "utils."), "imports only feature/ or utils/");
	}

	private static Stream<String> lines(Path file) {
		try {
			return Files.readAllLines(file).stream();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
