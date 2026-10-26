package pt.ist.socialsoftware.mono2micro.similarity.dto;

import pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendMatrixSciPy;
import pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyStructure;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;
import pt.ist.socialsoftware.mono2micro.utils.Constants;

import java.util.List;
import java.util.stream.Collectors;

import static pt.ist.socialsoftware.mono2micro.representation.analytical.domain.StructureRepresentation.STRUCTURE;
import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyStructure.SIMILARITY_SCIPY_STRUCTURE;

public class SimilarityScipyStructureDto extends SimilarityDto {
	private List<Weights> weightsList;
	private String linkageType;
	private Constants.TraceType traceType;

	public SimilarityScipyStructureDto() { this.type = SIMILARITY_SCIPY_STRUCTURE; }

	public SimilarityScipyStructureDto(SimilarityScipyStructure similarity) {
		this.id = similarity.getId();
		this.softwareSystemId = similarity.getStrategy().getSoftwareSystem().getId();
		this.strategyId = similarity.getStrategy().getId();
		this.strategyName = similarity.getStrategy().getName();
		this.name = similarity.getName();
		this.type = similarity.getType();
		this.weightsList = similarity.getWeightsList();
		copyProfile(similarity.getProfile());
		this.linkageType = similarity.getLinkageType();
		this.traceType = similarity.getTraceType();

	}

	public SimilarityScipyStructureDto(RecommendMatrixSciPy recommend, List<Weights> weightsList) {
		this.strategyId = recommend.getStrategy().getId();
		this.strategyName = recommend.getStrategy().getName();
		this.type = SIMILARITY_SCIPY_STRUCTURE;
		this.weightsList = weightsList;
		copyProfile(recommend.getProfile());
		this.traceType = recommend.getTraceType();
		this.linkageType = recommend.getLinkageType();
	}

	@Override
	public String scopedRepresentationType() {
		return STRUCTURE;
	}

	public String getName() {
		if (this.name == null) {
			this.name = this.strategyName + " "
					+ "params" + "(" + profileLabel() + "," + this.linkageType + "," + this.traceType + ") "
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
