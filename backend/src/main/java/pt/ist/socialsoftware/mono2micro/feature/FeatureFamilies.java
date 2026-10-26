package pt.ist.socialsoftware.mono2micro.feature;

import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Every {@link FeatureFamily} of the tool, and the one place an {@link AvailabilityContext} is built from (D-025 point 2). It holds no rule of
 * its own: building a context asks each family which of its features are present, and keeps only the answers.
 */
public final class FeatureFamilies {

	private final List<FeatureFamily<?>> families;

	public FeatureFamilies(Collection<? extends FeatureFamily<?>> families) {
		this.families = Collections.unmodifiableList(new ArrayList<>(families));
	}

	/** The tool's families, found by the self-registration scan, so adding a family is adding its class. */
	public static FeatureFamilies scanned() {
		List<FeatureFamily<?>> families = new ArrayList<>();
		for (FeatureFamily<?> family : SelfRegistration.instances(FeatureFamily.class))
			families.add(family);
		return new FeatureFamilies(families);
	}

	public List<FeatureFamily<?>> getFamilies() {
		return families;
	}

	/** The family that owns {@code feature}, if any does. */
	public Optional<FeatureFamily<?>> familyOf(FeatureId feature) {
		return families.stream().filter(family -> family.features().contains(feature)).findFirst();
	}

	/** The family whose member type is {@code memberType}, if one is registered. */
	public Optional<FeatureFamily<?>> familyWithMemberType(Class<?> memberType) {
		return families.stream().filter(family -> family.getMemberType().equals(memberType)).findFirst();
	}

	/**
	 * Fails when the families do not add up, so the boot stops instead of an availability request misreporting (D-025 point 7): two families
	 * claim one feature, one of {@code variableFeatures} belongs to no family, or a requirement or exclusion names a class that is neither a
	 * feature of some family nor a family's member type. The requirements checked are each family's shared ones and its features' own.
	 *
	 * @throws IllegalStateException
	 * naming the first such problem
	 */
	public void verify(Collection<Class<? extends VariableFeature>> variableFeatures) {
		Map<FeatureId, FeatureFamily<?>> owners = new HashMap<>();
		for (FeatureFamily<?> family : families)
			for (FeatureId feature : family.features()) {
				FeatureFamily<?> owner = owners.putIfAbsent(feature, family);
				if (owner != null)
					throw new IllegalStateException(
							"The families \"" + owner.getLabel() + "\" and \"" + family.getLabel() + "\" both claim the feature " + feature);
			}
		for (Class<? extends VariableFeature> type : variableFeatures)
			if (!isFeature(type))
				throw new IllegalStateException(type.getName() + " is a variable feature that no family claims");
		for (FeatureFamily<?> family : families) {
			verifyTargets("The family \"" + family.getLabel() + "\"", family.sharedRequirements());
			for (Object member : family.members())
				if (member instanceof VariableFeature)
					verifyTargets((VariableFeature) member);
		}
	}

	private void verifyTargets(VariableFeature feature) {
		String name = SelfRegistration.nameOf(feature);
		verifyTargets(name, feature.requirements());
		for (FeatureId excluded : feature.excludes())
			if (!familyOf(excluded).isPresent())
				throw new IllegalStateException(name + " excludes " + excluded.getType().getName() + ", which no family claims");
	}

	private void verifyTargets(String owner, List<Requirement> requirements) {
		requirements.stream().flatMap(Requirement::targets)
				.filter(target -> !familyWithMemberType(target).isPresent() && !isFeature(target))
				.findFirst()
				.ifPresent(target -> {
					throw new IllegalStateException(
							owner + " requires " + target.getName() + ", which is neither a feature of a family nor a family's member type");
				});
	}

	private boolean isFeature(Class<?> type) {
		return SelfRegistration.hasNamedType(type) && familyOf(FeatureId.of(type)).isPresent();
	}

	/**
	 * {@code target} as a noun phrase: a family's member type by the family's label ("a Similarity kind as input"), and a feature by its name
	 * and its family's label ("an Accesses representation"), so two features with one name read differently (D-025 point 6).
	 */
	String describe(Class<?> target) {
		return familyWithMemberType(target)
				.map(family -> withArticle(family.getLabel()))
				.orElseGet(() -> describe(FeatureId.of(target)));
	}

	/** A feature no family claims is named by itself; the boot check keeps the tool from having one. */
	String describe(FeatureId feature) {
		return familyOf(feature)
				.map(family -> withArticle(feature.getName() + " " + family.getLabel()))
				.orElse(feature.getName());
	}

	private static String withArticle(String phrase) {
		return ("AEIOUaeiou".indexOf(phrase.charAt(0)) >= 0 ? "an " : "a ") + phrase;
	}

	/** The context of a software system that holds {@code holdings}, such as its collected representations: each family is asked once. */
	public AvailabilityContext contextFor(Collection<?> holdings) {
		Set<FeatureId> present = new HashSet<>();
		for (FeatureFamily<?> family : families)
			present.addAll(family.presentInSystem(holdings));
		return new AvailabilityContext(this, present);
	}

	/** {@code context} with the features {@code input} makes present: each family is asked again, for the input. */
	AvailabilityContext withInput(AvailabilityContext context, Object input) {
		Set<FeatureId> present = new HashSet<>(context.getPresent());
		for (FeatureFamily<?> family : families)
			present.addAll(family.presentInInput(input));
		return new AvailabilityContext(this, present);
	}
}
