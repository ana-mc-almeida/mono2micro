package pt.ist.socialsoftware.mono2micro.feature;

import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord.FeatureAvailability;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The one availability check every family goes through: a feature is available exactly when each
 * of its {@link VariableFeature#requirements()}, and each requirement its family shares, is met by the context.
 *
 * <p>It knows nothing about any family or any kind of requirement, which is what keeps it one
 * check rather than one per hierarchy.
 */
public final class AvailabilityCheck {

	private AvailabilityCheck() {
	}

	public static DerivationRecord derive(Collection<? extends VariableFeature> features, AvailabilityContext context) {
		return new DerivationRecord(features.stream()
				.map(feature -> new FeatureAvailability(
						SelfRegistration.nameOf(feature),
						requirementsOf(feature, context.getFamilies()).stream()
								.filter(requirement -> !requirement.isMetBy(context))
								.map(Requirement::name)
								.collect(Collectors.toList())))
				.collect(Collectors.toList()));
	}

	/** The requirements its family shares, then the feature's own (D-025 point 5). */
	private static List<Requirement> requirementsOf(VariableFeature feature, FeatureFamilies families) {
		List<Requirement> requirements = new ArrayList<>();
		families.familyOf(FeatureId.ofFeature(feature)).ifPresent(family -> requirements.addAll(family.sharedRequirements()));
		requirements.addAll(feature.requirements());
		return requirements;
	}
}
