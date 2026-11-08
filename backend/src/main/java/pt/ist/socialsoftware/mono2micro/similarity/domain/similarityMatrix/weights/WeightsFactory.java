package pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights;

import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import java.util.ArrayList;
import java.util.List;

public class WeightsFactory {
	/** A new Weights of the type named {@code type}, found by the self-registration scan. */
	public static Weights getWeights(String type) {
		return SelfRegistration.newMember(Weights.class, type);
	}

	public static List<Weights> getWeightsList(List<String> types) {
		List<Weights> weightsList = new ArrayList<>();
		for (String weightsType : types)
			weightsList.add(getWeights(weightsType));
		return weightsList;
	}
}
