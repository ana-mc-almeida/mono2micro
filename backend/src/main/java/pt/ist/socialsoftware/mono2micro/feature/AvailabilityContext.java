package pt.ist.socialsoftware.mono2micro.feature;

import pt.ist.socialsoftware.mono2micro.codebase.domain.Codebase;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * What a {@link Requirement} is checked against: the System, the kinds of its collected
 * representations, and the optional input the feature would act on (D-019).
 *
 * <p>Immutable: {@link #withInput(Object)} returns a new context, so one System's context can be
 * reused across the inputs of a family.
 */
public class AvailabilityContext {

	private final Codebase system;
	private final Set<String> representationKinds;
	private final Object input;

	private AvailabilityContext(Codebase system, Set<String> representationKinds, Object input) {
		this.system = system;
		this.representationKinds = Collections.unmodifiableSet(new HashSet<>(representationKinds));
		this.input = input;
	}

	public static AvailabilityContext of(Codebase system) {
		Set<String> kinds = system.getRepresentations().stream()
				.map(Representation::getType)
				.collect(Collectors.toSet());
		return new AvailabilityContext(system, kinds, null);
	}

	/** A context with no System, for a question that depends only on which kinds were collected. */
	public static AvailabilityContext ofRepresentationKinds(Set<String> representationKinds) {
		return new AvailabilityContext(null, representationKinds, null);
	}

	/** A context with no System, for a question that depends only on the input. */
	public static AvailabilityContext forInput(Object input) {
		return new AvailabilityContext(null, Collections.emptySet(), input);
	}

	public AvailabilityContext withInput(Object input) {
		return new AvailabilityContext(system, representationKinds, input);
	}

	public Codebase getSystem() {
		return system;
	}

	public Set<String> getRepresentationKinds() {
		return representationKinds;
	}

	public Optional<Object> getInput() {
		return Optional.ofNullable(input);
	}
}
