package pt.ist.socialsoftware.mono2micro.decomposition;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Compares JSON from the REST API with a file recorded from today's tool.
 *
 * <p>The recorded files live under {@code src/test/resources/characterization/} and hold only
 * what the API returns: paths, JSON and type strings, never a class name. They are the
 * characterization the restructure must keep. A later ticket that ports an endpoint changes
 * how the JSON is fetched, not these files, so a mismatch after a port is a regression.
 *
 * <p>On a mismatch, or when no file is recorded yet, the actual JSON is written to
 * {@code target/characterization-actual/} under the same name and the assertion fails with
 * both paths. Recording a new value is copying that file over, after reading it.
 *
 * <p>Numbers compare within {@link #TOLERANCE}, so a different last bit from another
 * machine's floating point is not a failure. An integer and a decimal never match, however
 * close. The backend already rounds most metrics to three
 * decimals.
 */
final class Golden {

	private static final double TOLERANCE = 1e-6;
	private static final String ROOT = "characterization/";
	private static final Path ACTUAL_ROOT = Paths.get("target", "characterization-actual");

	private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	private Golden() {}

	/** Fails unless {@code actual} matches the recorded {@code name}, e.g. {@code clusters/quizzes-tutor/accesses.json}. */
	static void assertMatches(String name, JsonNode actual) {
		JsonNode sorted = sorted(actual);
		JsonNode expected = read(name);

		if (expected == null)
			throw new AssertionError("Nothing is recorded for " + ROOT + name + ". Today's answer was written to "
					+ writeActual(name, sorted) + "; read it, then copy it to src/test/resources/" + ROOT + name + ".");

		List<String> differences = new ArrayList<>();
		compare("$", expected, sorted, differences);

		if (!differences.isEmpty())
			throw new AssertionError(differences.size() + " difference(s) from " + ROOT + name + ":\n  "
					+ String.join("\n  ", differences.subList(0, Math.min(differences.size(), 20)))
					+ "\nThe actual JSON was written to " + writeActual(name, sorted) + ".");
	}

	static JsonNode toJson(Object value) {
		return MAPPER.valueToTree(value);
	}

	private static JsonNode read(String name) {
		try (InputStream in = Golden.class.getClassLoader().getResourceAsStream(ROOT + name)) {
			return in == null ? null : MAPPER.readTree(in);
		} catch (IOException e) {
			throw new UncheckedIOException("Could not read " + ROOT + name, e);
		}
	}

	private static Path writeActual(String name, JsonNode actual) {
		Path path = ACTUAL_ROOT.resolve(name);
		try {
			Files.createDirectories(path.getParent());
			Files.write(path, (MAPPER.writeValueAsString(actual) + "\n").getBytes("UTF-8"));
		} catch (IOException e) {
			throw new UncheckedIOException("Could not write " + path, e);
		}
		return path.toAbsolutePath();
	}

	private static void compare(String path, JsonNode expected, JsonNode actual, List<String> differences) {
		if (expected.isNumber() && actual.isNumber()) {
			// An integer that comes back as a decimal, or the reverse, is a contract change.
			if (expected.isIntegralNumber() != actual.isIntegralNumber())
				differences.add(path + ": expected " + expected + " but was " + actual + ", a different kind of number");
			else if (Math.abs(expected.asDouble() - actual.asDouble()) > TOLERANCE)
				differences.add(path + ": expected " + expected + " but was " + actual);

		} else if (expected.isObject() && actual.isObject()) {
			for (Iterator<String> names = expected.fieldNames(); names.hasNext();) {
				String field = names.next();
				if (!actual.has(field))
					differences.add(path + "." + field + ": missing");
				else compare(path + "." + field, expected.get(field), actual.get(field), differences);
			}
			for (Iterator<String> names = actual.fieldNames(); names.hasNext();) {
				String field = names.next();
				if (!expected.has(field))
					differences.add(path + "." + field + ": unexpected, " + actual.get(field));
			}

		} else if (expected.isArray() && actual.isArray()) {
			if (expected.size() != actual.size())
				differences.add(path + ": expected " + expected.size() + " elements but was " + actual.size());
			else for (int i = 0; i < expected.size(); i++)
				compare(path + "[" + i + "]", expected.get(i), actual.get(i), differences);

		} else if (!expected.equals(actual)) {
			differences.add(path + ": expected " + expected + " but was " + actual);
		}
	}

	/** Object fields in name order, so the written file diffs cleanly between recordings. */
	private static JsonNode sorted(JsonNode node) {
		if (node.isObject()) {
			List<String> names = new ArrayList<>();
			node.fieldNames().forEachRemaining(names::add);
			Collections.sort(names);

			ObjectNode sorted = MAPPER.createObjectNode();
			for (String name : names)
				sorted.set(name, sorted(node.get(name)));
			return sorted;
		}
		if (node.isArray()) {
			ArrayNode sorted = MAPPER.createArrayNode();
			node.forEach(element -> sorted.add(sorted(element)));
			return sorted;
		}
		return node;
	}
}
