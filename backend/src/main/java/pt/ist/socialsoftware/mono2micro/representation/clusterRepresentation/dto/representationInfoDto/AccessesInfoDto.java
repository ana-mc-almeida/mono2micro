package pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.dto.representationInfoDto;

import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.representationInformation.RepresentationInformation;

public class AccessesInfoDto extends RepresentationInfoDto {
	public AccessesInfoDto(RepresentationInformation representationInformation) {
		super(representationInformation.getType());
	}
}
