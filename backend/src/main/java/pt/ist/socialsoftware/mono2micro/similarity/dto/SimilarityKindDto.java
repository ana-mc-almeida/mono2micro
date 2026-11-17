package pt.ist.socialsoftware.mono2micro.similarity.dto;

import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilaritySetting;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

import java.util.List;

/**
 * What the Similarity form needs to render a kind (D-029): the request type that computes it, the settings it takes, and the weights it starts
 * from.
 */
public class SimilarityKindDto {
	private final String name;
	private final String similarityType;
	private final List<SimilaritySetting> settings;
	private final List<Weights> defaultWeights;

	public SimilarityKindDto(SimilarityKind kind) {
		this.name = SelfRegistration.nameOf(kind);
		this.similarityType = kind.similarityType();
		this.settings = kind.settings();
		this.defaultWeights = kind.defaultWeights();
	}

	public String getName() {
		return name;
	}

	public String getSimilarityType() {
		return similarityType;
	}

	public List<SimilaritySetting> getSettings() {
		return settings;
	}

	public List<Weights> getDefaultWeights() {
		return defaultWeights;
	}
}
