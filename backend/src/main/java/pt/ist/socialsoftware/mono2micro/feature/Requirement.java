package pt.ist.socialsoftware.mono2micro.feature;

import java.util.stream.Stream;

/**
 * One thing a {@link VariableFeature} needs before the tool can offer it. A requirement points at a feature or at a family, never at the data
 * behind it, so meeting one is a lookup in the context's set of present features (D-025 point 4).
 *
 * <p>The kinds of requirement are all in this package: {@link Requires#of} names a feature or a family, and {@link Requires#anyOf} is a
 * disjunction over either. The constructor is package-private, so a module declares requirements but writes no requirement classes: a new kind
 * of data is one new {@link FeatureFamily}, not one new requirement.
 */
public abstract class Requirement {

	Requirement() {
	}

	/**
	 * What the derivation record reports when this requirement fails: "requires" and what it names, worded with the label of the named family
	 * (D-025 point 6). The name is generated, never written by the feature.
	 */
	public final String name(FeatureFamilies families) {
		return "requires " + target(families);
	}

	/** What this requirement names, as a noun phrase such as "an Accesses representation". */
	abstract String target(FeatureFamilies families);

	public abstract boolean isMetBy(AvailabilityContext context);

	/** Every class this requirement names, so the boot check can verify that each is a feature or a family (D-025 point 7). */
	abstract Stream<Class<?>> targets();
}
