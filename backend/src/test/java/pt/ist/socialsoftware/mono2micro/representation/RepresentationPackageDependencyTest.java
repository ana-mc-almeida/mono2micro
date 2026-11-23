package pt.ist.socialsoftware.mono2micro.representation;

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
 * Pins D-016's rule as ticket 10 reads it: an operation holds both its input and its output, so {@code representation/} never imports the
 * operations, and a new operation never touches the Representation group. Nor does it import {@code representationCollection/}: collecting
 * depends on representations, not the other way round (D-018).
 *
 * <p>The rule is narrower than "never imports {@code refactoring/}". The information blocks, which D-011 keeps with the cluster representation,
 * read functionalities (Functionality Refactoring) and profiles (Expert knowledge); those imports predate the restructure.
 */
public class RepresentationPackageDependencyTest {

	private static final Path REPRESENTATION_SOURCES = Paths.get("src/main/java/pt/ist/socialsoftware/mono2micro/representation");
	private static final String TOOL = "pt.ist.socialsoftware.mono2micro.";

	@Test
	public void representationImportsNeitherTheOperationsNorRepresentationCollection() throws IOException {
		List<String> imports;
		try (Stream<Path> files = Files.walk(REPRESENTATION_SOURCES)) {
			imports = files.filter(file -> file.toString().endsWith(".java"))
					.flatMap(RepresentationPackageDependencyTest::lines)
					.map(String::trim)
					.filter(line -> line.startsWith("import " + TOOL) || line.startsWith("import static " + TOOL))
					.collect(Collectors.toList());
		}

		assertThat(imports).isNotEmpty();
		assertThat(imports).noneMatch(line -> line.contains(TOOL + "refactoring.servicesRefactoring"));
		assertThat(imports).noneMatch(line -> line.contains(TOOL + "representationCollection"));
	}

	private static Stream<String> lines(Path file) {
		try {
			return Files.readAllLines(file).stream();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}
}
