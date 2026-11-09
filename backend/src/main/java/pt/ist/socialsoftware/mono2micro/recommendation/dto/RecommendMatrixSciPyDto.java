package pt.ist.socialsoftware.mono2micro.recommendation.dto;

import pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendMatrixSciPy;
import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;
import pt.ist.socialsoftware.mono2micro.utils.Constants;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendationsType.RECOMMEND_MATRIX_SCIPY;

@JsonTypeName(RECOMMEND_MATRIX_SCIPY)
public class RecommendMatrixSciPyDto extends RecommendationDto {
	private String linkageType;
	private Constants.TraceType traceType;
	private int tracesMaxLimit;
	private List<Weights> weightsList;

	public RecommendMatrixSciPyDto() {}

	/** The representation a profile of its kind selects, as the kind declares it. */
	@Override
	public String scopedRepresentationType() {
		return SimilarityKind.getByName(getKindName()).scopedRepresentationType();
	}

	public RecommendMatrixSciPyDto(RecommendMatrixSciPy recommendation) {
		this.setType(recommendation.getType());
		this.setId(recommendation.getId());
		this.setSoftwareSystemId(recommendation.getSoftwareSystem().getId());
		this.setKindName(recommendation.getKindName());
		this.name = recommendation.getName();
		copyProfile(recommendation.getProfile());
		this.linkageType = recommendation.getLinkageType();
		this.tracesMaxLimit = recommendation.getTracesMaxLimit();
		this.isCompleted = recommendation.isCompleted();
		this.traceType = recommendation.getTraceType();
		this.weightsList = recommendation.getWeightsList();
	}

	@Override
	public List<String> weightsTypes() {
		return weightsList == null ? Collections.emptyList() : weightsList.stream().map(Weights::getType).collect(Collectors.toList());
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

	public int getTracesMaxLimit() {
		return tracesMaxLimit;
	}

	public void setTracesMaxLimit(int tracesMaxLimit) {
		this.tracesMaxLimit = tracesMaxLimit;
	}

	public List<Weights> getWeightsList() {
		return weightsList;
	}

	public void setWeightsList(List<Weights> weightsList) {
		this.weightsList = weightsList;
	}
}