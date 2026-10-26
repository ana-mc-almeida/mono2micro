package pt.ist.socialsoftware.mono2micro.feature;

import java.util.stream.Stream;

/**
 * Met when what it names is present (D-025 point 4). A feature names what it needs by class and nothing else: a feature's own class, as in
 * {@code Requires.of(AccessesRepresentation.class)}, or a family's member type, as in {@code Requires.of(SimilarityKind.class)} ("some
 * Similarity kind"). The context decides which of the two the class is, so a feature never knows.
 *
 * <p>A disjunction is {@link #anyOf}, over either.
 */
public final class Requires extends Requirement {

	private final Class<?> target;

	private Requires(Class<?> target) {
		this.target = target;
	}

	public static Requirement of(Class<?> target) {
		return new Requires(target);
	}

	/** Met when at least one alternative is met. */
	public static Requirement anyOf(Requirement... alternatives) {
		return new AnyOf(alternatives);
	}

	@Override
	String target(FeatureFamilies families) {
		return families.describe(target);
	}

	@Override
	public boolean isMetBy(AvailabilityContext context) {
		return context.isPresent(target);
	}

	@Override
	Stream<Class<?>> targets() {
		return Stream.of(target);
	}
}
