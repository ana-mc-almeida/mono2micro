package pt.ist.socialsoftware.mono2micro.similarity.dto;

import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import java.util.List;

/** What a Similarity kind's form starts from: the kind's name and its default weights (D-029). */
public class SimilarityKindDto {
	private final String name;
	private final List<Weights> defaultWeights;

	public SimilarityKindDto(SimilarityKind kind) {
		this.name = SelfRegistration.nameOf(kind);
		this.defaultWeights = kind.defaultWeights();
	}

	public String getName() {
		return name;
	}

	public List<Weights> getDefaultWeights() {
		return defaultWeights;
	}
}
