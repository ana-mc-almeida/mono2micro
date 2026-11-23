package pt.ist.socialsoftware.mono2micro.similarity.dto;

import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendMatrixSciPy;
import pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyEntityVectorization;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;

import java.util.List;
import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyEntityVectorization.SIMILARITY_SCIPY_ENTITY_VECTORIZATION;

@JsonTypeName(SIMILARITY_SCIPY_ENTITY_VECTORIZATION)
public class SimilarityScipyEntityVectorizationDto extends SimilarityDto {
	private String linkageType;

	public SimilarityScipyEntityVectorizationDto() { this.type = SIMILARITY_SCIPY_ENTITY_VECTORIZATION; }

	public SimilarityScipyEntityVectorizationDto(SimilarityScipyEntityVectorization similarity) {
		super(similarity);
		this.linkageType = similarity.getLinkageType();
	}

	@Override
	public Similarity newSimilarity(List<Representation> representations) {
		return new SimilarityScipyEntityVectorization(getKindName(), representations, getName(), this);
	}

	public SimilarityScipyEntityVectorizationDto(RecommendMatrixSciPy recommend, List<Weights> weightsList) {
		copyCandidateSource(recommend);
		this.name = recommend.getName();
		this.type = SIMILARITY_SCIPY_ENTITY_VECTORIZATION;
		this.linkageType = recommend.getLinkageType();
	}

	public String getName() {
		if (this.name == null) {
			this.name = namePrefix() + " " + "params" + "(" + this.linkageType + ")";
		}

		return this.name;
	}

	public String getLinkageType() {
		return linkageType;
	}

	public void setLinkageType(String linkageType) {
		this.linkageType = linkageType;
	}

}
