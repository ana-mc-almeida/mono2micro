package pt.ist.socialsoftware.mono2micro.decomposition.dto.decomposition;

import pt.ist.socialsoftware.mono2micro.cluster.Cluster;
import pt.ist.socialsoftware.mono2micro.decomposition.dto.representationInfoDto.RepresentationInfoDto;

import java.util.List;
import java.util.Map;

public abstract class DecompositionDto {
	String name;
	String softwareSystemId;
	String softwareSystemName;
	String strategyId;
	String strategyName;
	String similarityName;
	String type;
	Map<String, Object> metrics;
	Map<String, Cluster> clusters;
	List<RepresentationInfoDto> representationInformations;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSoftwareSystemId() {
		return softwareSystemId;
	}

	public void setSoftwareSystemId(String softwareSystemId) {
		this.softwareSystemId = softwareSystemId;
	}

	public String getSoftwareSystemName() {
		return softwareSystemName;
	}

	public void setSoftwareSystemName(String softwareSystemName) {
		this.softwareSystemName = softwareSystemName;
	}

	public String getStrategyId() {
		return strategyId;
	}

	public void setStrategyId(String strategyId) {
		this.strategyId = strategyId;
	}

	public String getStrategyName() {
		return strategyName;
	}

	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
	}

	public String getSimilarityName() {
		return similarityName;
	}

	public void setSimilarityName(String similarityName) {
		this.similarityName = similarityName;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, Object> getMetrics() {
		return metrics;
	}

	public void setMetrics(Map<String, Object> metrics) {
		this.metrics = metrics;
	}

	public Map<String, Cluster> getClusters() {
		return clusters;
	}

	public void setClusters(Map<String, Cluster> clusters) {
		this.clusters = clusters;
	}

	public List<RepresentationInfoDto> getRepresentationInformations() {
		return representationInformations;
	}

	public void setRepresentationInformations(List<RepresentationInfoDto> representationInformations) {
		this.representationInformations = representationInformations;
	}
}
