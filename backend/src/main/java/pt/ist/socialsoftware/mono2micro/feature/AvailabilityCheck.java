package pt.ist.socialsoftware.mono2micro.feature;

import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord.FeatureAvailability;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The one availability check every family goes through: a feature is available exactly when each
 * of its {@link VariableFeature#requirements()}, and each requirement its family shares, is met by the context, and no feature it
 * {@link VariableFeature#excludes()} is present.
 *
 * <p>It knows nothing about any family or any kind of requirement, which is what keeps it one
 * check rather than one per hierarchy.
 */
public final class AvailabilityCheck {

	private AvailabilityCheck() {
	}

	public static DerivationRecord derive(Collection<? extends VariableFeature> features, AvailabilityContext context) {
		return new DerivationRecord(features.stream()
				.map(feature -> new FeatureAvailability(SelfRegistration.nameOf(feature), failuresOf(feature, context)))
				.collect(Collectors.toList()));
	}

	/**
	 * Fails when {@code feature} is not available in {@code context}, so that a request for a withheld feature is rejected by the same check
	 * that withholds it (D-003).
	 *
	 * @throws UnavailableFeatureException
	 * naming the feature and what it failed
	 */
	public static void requireAvailable(VariableFeature feature, AvailabilityContext context) {
		List<String> failures = failuresOf(feature, context);
		if (!failures.isEmpty())
			throw new UnavailableFeatureException(SelfRegistration.nameOf(feature), failures);
	}

	/**
	 * The features {@code feature} requires by name, its family's shared requirements first, such as the representations a Similarity kind
	 * reads. A requirement naming a family names no one feature, and the alternatives of an {@link AnyOf} are needed only one at a time, so
	 * neither is included.
	 */
	public static Set<FeatureId> requiredFeatures(VariableFeature feature, FeatureFamilies families) {
		return requirementsOf(feature, families).stream()
				.flatMap(Requirement::conjunctTargets)
				.filter(target -> !families.familyWithMemberType(target).isPresent())
				.map(FeatureId::of)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/** The names of the requirements the feature fails, then of the present features it excludes. */
	private static List<String> failuresOf(VariableFeature feature, AvailabilityContext context) {
		FeatureFamilies families = context.getFamilies();
		List<String> failures = requirementsOf(feature, families).stream()
				.filter(requirement -> !requirement.isMetBy(context))
				.map(requirement -> requirement.name(families))
				.collect(Collectors.toList());
		feature.excludes().stream()
				.filter(context::isPresent)
				.map(excluded -> "excludes " + families.describe(excluded))
				.forEach(failures::add);
		return failures;
	}

	/** The requirements its family shares, then the feature's own (D-025 point 5). */
	private static List<Requirement> requirementsOf(VariableFeature feature, FeatureFamilies families) {
		List<Requirement> requirements = new ArrayList<>();
		families.familyOf(FeatureId.ofFeature(feature)).ifPresent(family -> requirements.addAll(family.sharedRequirements()));
		requirements.addAll(feature.requirements());
		return requirements;
	}
}
