package pt.ist.socialsoftware.mono2micro.clusteringAlgorithm;

import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.dto.request.DecompositionRequest;
import pt.ist.socialsoftware.mono2micro.feature.VariableFeature;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;

import java.util.Set;

public abstract class Clustering implements VariableFeature {
	public abstract Decomposition generateDecomposition(Similarity similarity, DecompositionRequest request, Set<String> decompositionNames) throws Exception;

	public abstract String getType();
}
