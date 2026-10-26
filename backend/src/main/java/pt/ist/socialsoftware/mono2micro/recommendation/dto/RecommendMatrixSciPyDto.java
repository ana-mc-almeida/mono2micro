package pt.ist.socialsoftware.mono2micro.recommendation.dto;

import pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendMatrixSciPy;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;
import pt.ist.socialsoftware.mono2micro.utils.Constants;

import java.util.List;

import static pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendationsType.RECOMMEND_MATRIX_SCIPY;
import static pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendationsType.RECOMMEND_MATRIX_SCIPY_STRUCTURE;
import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation.ACCESSES;
import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.StructureRepresentation.STRUCTURE;

public class RecommendMatrixSciPyDto extends RecommendationDto {
	private String linkageType;
	private Constants.TraceType traceType;
	private int tracesMaxLimit;
	private List<Weights> weightsList;

	public RecommendMatrixSciPyDto() {}

	@Override
	public String scopedRepresentationType() {
		switch (getType()) {
		case RECOMMEND_MATRIX_SCIPY:
			return ACCESSES;
		case RECOMMEND_MATRIX_SCIPY_STRUCTURE:
			return STRUCTURE;
		default:
			return null;
		}
	}

	public RecommendMatrixSciPyDto(RecommendMatrixSciPy recommendation) {
		this.setType(recommendation.getType());
		this.setId(recommendation.getId());
		this.setStrategyId(recommendation.getStrategy().getId());
		this.setStrategyName(recommendation.getStrategy().getName());
		this.name = recommendation.getName();
		copyProfile(recommendation.getProfile());
		this.linkageType = recommendation.getLinkageType();
		this.tracesMaxLimit = recommendation.getTracesMaxLimit();
		this.isCompleted = recommendation.isCompleted();
		this.traceType = recommendation.getTraceType();
		this.weightsList = recommendation.getWeightsList();
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