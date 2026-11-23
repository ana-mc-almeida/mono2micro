package pt.ist.socialsoftware.mono2micro.similarity.dto;

import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendMatrixSciPy;
import pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyStructure;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;
import pt.ist.socialsoftware.mono2micro.utils.Constants;

import java.util.List;
import java.util.stream.Collectors;

import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyStructure.SIMILARITY_SCIPY_STRUCTURE;

@JsonTypeName(SIMILARITY_SCIPY_STRUCTURE)
public class SimilarityScipyStructureDto extends SimilarityDto {
	private List<Weights> weightsList;
	private String linkageType;
	private Constants.TraceType traceType;

	public SimilarityScipyStructureDto() { this.type = SIMILARITY_SCIPY_STRUCTURE; }

	public SimilarityScipyStructureDto(SimilarityScipyStructure similarity) {
		super(similarity);
		this.weightsList = similarity.getWeightsList();
		this.linkageType = similarity.getLinkageType();
		this.traceType = similarity.getTraceType();
	}

	@Override
	public Similarity newSimilarity(List<Representation> representations) {
		return new SimilarityScipyStructure(getKindName(), representations, getName(), this);
	}

	@Override
	public void describeCandidate(RecommendMatrixSciPy recommend, List<Weights> weightsList) {
		copyCandidateSource(recommend);
		this.type = SIMILARITY_SCIPY_STRUCTURE;
		this.weightsList = weightsList;
		copyProfile(recommend.getProfile());
		this.traceType = recommend.getTraceType();
		this.linkageType = recommend.getLinkageType();
	}

	public String getName() {
		if (this.name == null) {
			this.name = namePrefix() + " "
					+ "params" + "(" + profileLabel() + "," + this.linkageType + "," + this.traceType + ") "
					+ this.weightsList.stream()
							.map(weights -> weights.getName())
							.collect(Collectors.joining(", "));
		}

		return this.name;
	}

	@Override
	public List<String> weightsTypes() {
		return typesOf(weightsList);
	}

	public List<Weights> getWeightsList() {
		return weightsList;
	}

	public void setWeightsList(List<Weights> weightsList) {
		this.weightsList = weightsList;
	}

	public String getLinkageType() {
		return linkageType;
	}

	public void setLinkageType(String linkageType) {
		this.linkageType = linkageType;
	}

	public Constants.TraceType getTraceType() {
		return traceType;
	}

	public void setTraceType(Constants.TraceType traceType) {
		this.traceType = traceType;
	}

}
