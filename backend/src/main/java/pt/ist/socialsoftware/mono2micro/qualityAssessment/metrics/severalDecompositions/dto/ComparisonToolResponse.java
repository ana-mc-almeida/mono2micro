package pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.severalDecompositions.dto;

import pt.ist.socialsoftware.mono2micro.qualityAssessment.metrics.severalDecompositions.Analysis;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.dto.ClusterRepresentationDto;

import java.util.ArrayList;
import java.util.List;

public abstract class ComparisonToolResponse {
	protected ClusterRepresentationDto decomposition1;
	protected ClusterRepresentationDto decomposition2;

	protected List<Analysis> analysisList = new ArrayList<>();

	public ClusterRepresentationDto getDecomposition2() {
		return decomposition2;
	}

	public void setDecomposition2(ClusterRepresentationDto decomposition2) {
		this.decomposition2 = decomposition2;
	}

	public ClusterRepresentationDto getDecomposition1() {
		return decomposition1;
	}

	public void setDecomposition1(ClusterRepresentationDto decomposition1) {
		this.decomposition1 = decomposition1;
	}

	public List<Analysis> getAnalysisList() {
		return analysisList;
	}

	public void setAnalysisList(List<Analysis> analysisList) {
		this.analysisList = analysisList;
	}

	public void addAnalysis(Analysis analysis) {
		this.analysisList.add(analysis);
	}
}