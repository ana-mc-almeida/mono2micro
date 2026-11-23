package pt.ist.socialsoftware.mono2micro.clusteringAlgorithm;

import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.dto.request.DecompositionRequest;
import pt.ist.socialsoftware.mono2micro.feature.VariableFeature;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;

import java.util.Set;

public abstract class Clustering implements VariableFeature {
	public abstract ClusterRepresentation generateDecomposition(Similarity similarity, DecompositionRequest request, Set<String> decompositionNames) throws Exception;

	public abstract String getType();
}
