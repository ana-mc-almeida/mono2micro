package pt.ist.socialsoftware.mono2micro.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** One feature's line in a {@link DerivationRecord}. */
public class FeatureAvailability {

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
