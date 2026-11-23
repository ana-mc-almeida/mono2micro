package pt.ist.socialsoftware.mono2micro.feature;

import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * A group of features that one module owns, such as the representation kinds or the Similarity kinds (D-025). Every feature belongs to exactly
 * one family, and the family alone decides which of its features are present: for a software system, from what the system holds, and for an
 * input, from what the input is. {@link AvailabilityContext} only records the answers, so no rule about any feature lives in it.
 *
 * <p>A family lives in the module that owns its data, and finds its features with the self-registration scan of its member type, so adding a
 * feature is adding its class. Families are found by the same scan, so each one needs a no-arg constructor.
 *
 * <p>A family declares:
 * <ul>
 * <li>its label, which the derivation record's lines use ("an Accesses <em>representation</em>");
 * <li>which of its features are present for a software system ({@link #presentInSystem}) and for an input ({@link #presentInInput}). Both
 * default to none;
 * <li>the requirements all its features share ({@link #sharedRequirements}), which the check adds to each feature's own list.
 * </ul>
 */
public abstract class FeatureFamily<T> {

	private final String label;
	private final Class<T> memberType;
	private final Supplier<Collection<? extends T>> members;

	/** A family whose features are the concrete classes of {@code memberType}, found by the scan. */
	protected FeatureFamily(String label, Class<T> memberType) {
		this.label = label;
		this.memberType = memberType;
		this.members = () -> SelfRegistration.prototypes(memberType).values();
	}

	/**
	 * A family of the given features. Only the core's tests use it: a test-only family on the scan would join the tool's families, so its
	 * features are handed in instead. A module's family scans.
	 */
	protected FeatureFamily(String label, Class<T> memberType, Collection<? extends T> members) {
		this.label = label;
		this.memberType = memberType;
		List<T> copy = new ArrayList<>(members);
		this.members = () -> copy;
	}

	public final String getLabel() {
		return label;
	}

	/** The type every feature of this family is. A requirement names it to require the family: some feature of it is present. */
	public final Class<T> getMemberType() {
		return memberType;
	}

	/** One prototype of each feature of this family. */
	public final Collection<? extends T> members() {
		return members.get();
	}

	public final Set<FeatureId> features() {
		return members().stream().map(FeatureId::ofFeature).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/** Which of this family's features a software system has, given what the system holds, such as its collected representations. */
	public Set<FeatureId> presentInSystem(Collection<?> holdings) {
		return Collections.emptySet();
	}

	/** Which of this family's features an input is, such as the Similarity kind an algorithm would cluster. */
	public Set<FeatureId> presentInInput(Object input) {
		return Collections.emptySet();
	}

	/** Requirements every feature of this family has, before its own. */
	public List<Requirement> sharedRequirements() {
		return Collections.emptyList();
	}

	/** The ids of those {@code objects} that are features of this family, which is the presence rule of a family whose features are objects. */
	protected final Set<FeatureId> featuresAmong(Collection<?> objects) {
		Set<FeatureId> features = features();
		return objects.stream()
				.filter(SelfRegistration::hasName)
				.map(FeatureId::ofFeature)
				.filter(features::contains)
				.collect(Collectors.toSet());
	}
}
