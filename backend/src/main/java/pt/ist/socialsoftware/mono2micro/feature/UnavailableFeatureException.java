package pt.ist.socialsoftware.mono2micro.feature;

import java.util.Collections;
import java.util.List;

/** A request asked for a feature the availability check withholds: the backend rejects it rather than fail halfway (D-003). */
public class UnavailableFeatureException extends RuntimeException {

	private final List<String> failedRequirements;

	public UnavailableFeatureException(String feature, List<String> failedRequirements) {
		super(feature + " is not available: " + String.join(", ", failedRequirements));
		this.failedRequirements = Collections.unmodifiableList(failedRequirements);
	}

	public List<String> getFailedRequirements() {
		return failedRequirements;
	}
}
