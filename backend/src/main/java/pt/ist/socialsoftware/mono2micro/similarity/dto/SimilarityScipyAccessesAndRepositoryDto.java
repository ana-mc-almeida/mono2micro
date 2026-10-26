package pt.ist.socialsoftware.mono2micro.similarity.dto;

import pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendMatrixSciPy;
import pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyAccessesAndRepository;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;
import pt.ist.socialsoftware.mono2micro.utils.Constants;

import java.util.List;
import java.util.stream.Collectors;

import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyAccessesAndRepository.SIMILARITY_SCIPY_ACCESSES_REPOSITORY;

public class SimilarityScipyAccessesAndRepositoryDto extends SimilarityDto {
	private List<Weights> weightsList;
	private String linkageType;
	private int tracesMaxLimit;
	private Constants.TraceType traceType;

	public SimilarityScipyAccessesAndRepositoryDto() { this.type = SIMILARITY_SCIPY_ACCESSES_REPOSITORY; }

	public SimilarityScipyAccessesAndRepositoryDto(SimilarityScipyAccessesAndRepository similarity) {
		this.id = similarity.getId();
		this.softwareSystemId = similarity.getStrategy().getSoftwareSystem().getId();
		this.strategyId = similarity.getStrategy().getId();
		this.strategyName = similarity.getStrategy().getName();
		this.name = similarity.getName();
		this.type = similarity.getType();
		this.weightsList = similarity.getWeightsList();
		copyProfile(similarity.getProfile());
		this.linkageType = similarity.getLinkageType();
		this.tracesMaxLimit = similarity.getTracesMaxLimit();
		this.traceType = similarity.getTraceType();
	}

	public SimilarityScipyAccessesAndRepositoryDto(RecommendMatrixSciPy recommend, List<Weights> weightsList) {
		this.strategyId = recommend.getStrategy().getId();
		this.strategyName = recommend.getStrategy().getName();
		this.type = SIMILARITY_SCIPY_ACCESSES_REPOSITORY;
		this.weightsList = weightsList;
		copyProfile(recommend.getProfile());
		this.linkageType = recommend.getLinkageType();
		this.tracesMaxLimit = recommend.getTracesMaxLimit();
		this.traceType = recommend.getTraceType();
	}

	public String getName() {
		if (this.name == null) {
			this.name = this.strategyName + " "
					+ "params" + "(" + this.linkageType + "," + profileLabel() + "," + this.tracesMaxLimit + "," + this.traceType + ") "
					+ this.weightsList.stream()
							.map(weights -> weights.getName())
							.collect(Collectors.joining(", "));
		}

		return this.name;
	}

	public List<Weights> getWeightsList() {
		return weightsList;
	}

	public void setWeightsList(List<Weights> weightsList) {
		this.weightsList = weightsList;
	}

	public int getTracesMaxLimit() {
		return tracesMaxLimit;
	}

	public void setTracesMaxLimit(int tracesMaxLimit) {
		this.tracesMaxLimit = tracesMaxLimit;
	}

	public Constants.TraceType getTraceType() {
		return traceType;
	}

	public void setTraceType(Constants.TraceType traceType) {
		this.traceType = traceType;
	}

	public String getLinkageType() {
		return linkageType;
	}

	public void setLinkageType(String linkageType) {
		this.linkageType = linkageType;
	}

}
