package pt.ist.socialsoftware.mono2micro.feature;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * What a {@link Requirement} is checked against: the set of present features (D-025 point 2). {@link FeatureFamilies} builds it once for a
 * software system by asking each family which of its features are present, and {@link #withInput(Object)} asks the families again for an
 * input. Every question after that is a lookup in the set.
 *
 * <p>This package does not know where the answers come from: no family keeps facts here, and no software system data reaches it.
 *
 * <p>Immutable: {@link #withInput(Object)} returns a new context, so one software system's context can be reused across the inputs of a family.
 */
public class AvailabilityContext {

	private final FeatureFamilies families;
	private final Set<FeatureId> present;
	private final Set<String> representationKinds;
	private final Object input;

	AvailabilityContext(FeatureFamilies families, Set<FeatureId> present) {
		this(families, present, Collections.emptySet(), null);
	}

	private AvailabilityContext(FeatureFamilies families, Set<FeatureId> present, Set<String> representationKinds, Object input) {
		this.families = families;
		this.present = Collections.unmodifiableSet(new HashSet<>(present));
		this.representationKinds = Collections.unmodifiableSet(new HashSet<>(representationKinds));
		this.input = input;
	}

	public static AvailabilityContext ofRepresentationKinds(Set<String> representationKinds) {
		return new AvailabilityContext(new FeatureFamilies(Collections.emptyList()), Collections.emptySet(), representationKinds, null);
	}

	/** A context for a question that depends only on the input. */
	public static AvailabilityContext forInput(Object input) {
		return ofRepresentationKinds(Collections.emptySet()).withInput(input);
	}

	public AvailabilityContext withInput(Object input) {
		AvailabilityContext asked = families.withInput(this, input);
		return new AvailabilityContext(families, asked.present, representationKinds, input);
	}

	/**
	 * Whether what {@code target} names is present: some feature of the family, if {@code target} is a family's member type, and otherwise the
	 * feature whose class it is.
	 */
	public boolean isPresent(Class<?> target) {
		return families.familyWithMemberType(target)
				.map(family -> family.features().stream().anyMatch(present::contains))
				.orElseGet(() -> present.contains(FeatureId.of(target)));
	}

	public boolean isPresent(FeatureId feature) {
		return present.contains(feature);
	}

	Set<FeatureId> getPresent() {
		return present;
	}

	public Set<String> getRepresentationKinds() {
		return representationKinds;
	}

	public Optional<Object> getInput() {
		return Optional.ofNullable(input);
	}
}
