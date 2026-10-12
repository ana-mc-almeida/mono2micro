package pt.ist.socialsoftware.mono2micro.feature;

import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Met when the input is one of the given Similarity kinds, compared by feature name (its
 * {@code @JsonTypeName}). Today the input is the kind itself; it becomes a Similarity holding its
 * kind when Similarities stop hanging off a strategy.
 */
public class SimilarityKindIn implements Requirement {

	private final List<String> kinds;

	public SimilarityKindIn(List<String> kinds) {
		this.kinds = new ArrayList<>(kinds);
	}

	@Override
	public String name() {
		return "Similarity kind in {" + String.join(", ", kinds) + "}";
	}

	@Override
	public boolean isMetBy(AvailabilityContext context) {
		return context.getInput()
				.filter(input -> SelfRegistration.hasName(input) && kinds.contains(SelfRegistration.nameOf(input)))
				.isPresent();
	}
}
