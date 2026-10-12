package pt.ist.socialsoftware.mono2micro.feature;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * What a {@link Requirement} is checked against: the kinds of the representations a System has
 * collected, and the optional input the feature would act on (D-019, D-022).
 *
 * <p>This package does not know where those come from. The module that owns the System builds the
 * context from it, so {@code feature/} never imports how the System stores its uploads.
 *
 * <p>Immutable: {@link #withInput(Object)} returns a new context, so one System's context can be
 * reused across the inputs of a family.
 */
public class AvailabilityContext {

	private final Set<String> representationKinds;
	private final Object input;

	private AvailabilityContext(Set<String> representationKinds, Object input) {
		this.representationKinds = Collections.unmodifiableSet(new HashSet<>(representationKinds));
		this.input = input;
	}

	public static AvailabilityContext ofRepresentationKinds(Set<String> representationKinds) {
		return new AvailabilityContext(representationKinds, null);
	}

	/** A context for a question that depends only on the input. */
	public static AvailabilityContext forInput(Object input) {
		return new AvailabilityContext(Collections.emptySet(), input);
	}

	public AvailabilityContext withInput(Object input) {
		return new AvailabilityContext(representationKinds, input);
	}

	public Set<String> getRepresentationKinds() {
		return representationKinds;
	}

	public Optional<Object> getInput() {
		return Optional.ofNullable(input);
	}
}
