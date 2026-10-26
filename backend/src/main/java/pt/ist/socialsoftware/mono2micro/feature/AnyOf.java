package pt.ist.socialsoftware.mono2micro.feature;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Met when at least one of its alternatives is met: the disjunction a flat list of requirements cannot say, as in the model's "Lexical requires
 * Structure or Call graph or Sequence of accesses", over features and families alike. Built by {@link Requires#anyOf}. It stays although
 * nothing uses it yet, because ticket 09's Profile needs it (D-025 point 4).
 */
public final class AnyOf extends Requirement {

	private final List<Requirement> alternatives;

	AnyOf(Requirement... alternatives) {
		this.alternatives = Arrays.asList(alternatives);
	}

	@Override
	public String name() {
		return alternatives.stream().map(Requirement::name).collect(Collectors.joining(", ", "any of {", "}"));
	}

	@Override
	public boolean isMetBy(AvailabilityContext context) {
		return alternatives.stream().anyMatch(alternative -> alternative.isMetBy(context));
	}
}
