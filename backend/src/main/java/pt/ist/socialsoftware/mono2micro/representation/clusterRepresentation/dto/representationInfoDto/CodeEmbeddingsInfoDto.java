package pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.dto.representationInfoDto;

import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.representationInformation.RepresentationInformation;

public class CodeEmbeddingsInfoDto extends RepresentationInfoDto {
	public CodeEmbeddingsInfoDto(RepresentationInformation representationInformation) {
		super(representationInformation.getType());
	}
}
