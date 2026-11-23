package pt.ist.socialsoftware.mono2micro.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Every feature of a family, offered and withheld alike, with the names of the requirements each
 * one failed (D-013, D-019). Computed on request and never stored.
 */
public class DerivationRecord {

	private final List<FeatureAvailability> features;

	public DerivationRecord(List<FeatureAvailability> features) {
		this.features = Collections.unmodifiableList(new ArrayList<>(features));
	}

	public List<FeatureAvailability> getFeatures() {
		return features;
	}

	public List<String> availableNames() {
		return features.stream()
				.filter(FeatureAvailability::isAvailable)
				.map(FeatureAvailability::getName)
				.collect(Collectors.toList());
	}

	/** One feature's line in the record. */
	public static class FeatureAvailability {

		private final String name;
		private final boolean available;
		private final List<String> failedRequirements;

		public FeatureAvailability(String name, List<String> failedRequirements) {
			this.name = name;
			this.failedRequirements = Collections.unmodifiableList(new ArrayList<>(failedRequirements));
			this.available = failedRequirements.isEmpty();
		}

		public String getName() {
			return name;
		}

		public boolean isAvailable() {
			return available;
		}

		public List<String> getFailedRequirements() {
			return failedRequirements;
		}
	}
}
