package pt.ist.socialsoftware.mono2micro.feature;

import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord.FeatureAvailability;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The one availability check every family goes through: a feature is available exactly when each
 * of its {@link VariableFeature#requirements()} is met by the context.
 *
 * <p>It knows nothing about any family or any kind of requirement, which is what keeps it one
 * check rather than one per hierarchy.
 */
public final class AvailabilityCheck {

	private AvailabilityCheck() {
	}

	public static DerivationRecord derive(Collection<? extends VariableFeature> family, AvailabilityContext context) {
		List<FeatureAvailability> features = family.stream()
				.map(feature -> new FeatureAvailability(
						SelfRegistration.nameOf(feature),
						feature.requirements().stream()
								.filter(requirement -> !requirement.isMetBy(context))
								.map(Requirement::name)
								.collect(Collectors.toList())))
				.collect(Collectors.toList());
		return new DerivationRecord(features);
	}
}
