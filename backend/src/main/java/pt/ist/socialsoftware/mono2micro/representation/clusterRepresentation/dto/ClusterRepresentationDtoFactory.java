package pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.dto;

import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.*;

import java.util.ArrayList;
import java.util.List;

import static pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.PartitionsClusterRepresentation.PARTITIONS_CLUSTER_REPRESENTATION;

public class ClusterRepresentationDtoFactory {
	public static ClusterRepresentationDto getDecompositionDto(ClusterRepresentation decomposition) {
		if (decomposition == null)
			return null;
		switch (decomposition.getType()) {
		case PARTITIONS_CLUSTER_REPRESENTATION:
			return new PartitionsClusterRepresentationDto((PartitionsClusterRepresentation) decomposition);
		default:
			throw new RuntimeException("The type \"" + decomposition.getType() + "\" is not a valid decomposition type.");
		}
	}

	public static List<ClusterRepresentationDto> getDecompositionDtos(List<ClusterRepresentation> decompositions) {
		List<ClusterRepresentationDto> decompositionDtos = new ArrayList<>();
		for (ClusterRepresentation decomposition : decompositions)
			decompositionDtos.add(getDecompositionDto(decomposition));
		return decompositionDtos;
	}
}