package pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.dto;

import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.PartitionsClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.dto.representationInfoDto.RepresentationInfoDtoFactory;

public class PartitionsClusterRepresentationDto extends ClusterRepresentationDto {
	private boolean outdated;
	private boolean expert;

	public PartitionsClusterRepresentationDto(PartitionsClusterRepresentation decomposition) {
		this.setId(decomposition.getId());
		this.setSoftwareSystemId(decomposition.getSoftwareSystem().getId());
		this.setSoftwareSystemName(decomposition.getSoftwareSystem().getName());
		this.setSimilarityId(decomposition.getSimilarity().getId());
		this.setSimilarityName(decomposition.getSimilarity().getName());
		this.setName(decomposition.getName());
		this.type = decomposition.getType();
		this.setMetrics(decomposition.getMetrics());
		this.outdated = decomposition.isOutdated();
		this.expert = decomposition.isExpert();
		this.clusters = decomposition.getClusters();
		this.representationInformations = RepresentationInfoDtoFactory.getRepresentationInfoDtos(decomposition.getRepresentationInformations());
	}

	public boolean isOutdated() {
		return outdated;
	}

	public void setOutdated(boolean outdated) {
		this.outdated = outdated;
	}

	public boolean isExpert() {
		return expert;
	}

	public void setExpert(boolean expert) {
		this.expert = expert;
	}
}
