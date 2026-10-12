package pt.ist.socialsoftware.mono2micro.feature;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Met when at least one of its alternatives is met: the disjunction a flat list of requirements
 * cannot say, as in the model's "Structure requires Static analysis or Model analysis or External".
 *
 * <p>Built now with its tests, and removed later if nothing comes to use it (D-019 point 1).
 */
public class AnyOf implements Requirement {

	private final List<Requirement> alternatives;

	public AnyOf(Requirement... alternatives) {
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
