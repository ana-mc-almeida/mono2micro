package pt.ist.socialsoftware.mono2micro.similarity.dto;

import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyClassVectorization;

import java.util.List;

import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyClassVectorization.SIMILARITY_SCIPY_CLASS_VECTORIZATION;

@JsonTypeName(SIMILARITY_SCIPY_CLASS_VECTORIZATION)
public class SimilarityScipyClassVectorizationDto extends SimilarityDto {
	private String linkageType;

	public SimilarityScipyClassVectorizationDto() { this.type = SIMILARITY_SCIPY_CLASS_VECTORIZATION; }

	public SimilarityScipyClassVectorizationDto(SimilarityScipyClassVectorization similarity) {
		super(similarity);
		this.linkageType = similarity.getLinkageType();
	}

	@Override
	public Similarity newSimilarity(List<Representation> representations) {
		return new SimilarityScipyClassVectorization(getKindName(), representations, getName(), this);
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
