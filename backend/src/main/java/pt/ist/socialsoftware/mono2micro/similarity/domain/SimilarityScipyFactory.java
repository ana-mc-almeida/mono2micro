package pt.ist.socialsoftware.mono2micro.similarity.domain;

import pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendMatrixSciPy;

import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;

import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyAccessesAndRepository.SIMILARITY_SCIPY_ACCESSES_REPOSITORY;
import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyClassVectorization.SIMILARITY_SCIPY_CLASS_VECTORIZATION;
import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyEntityVectorization.SIMILARITY_SCIPY_ENTITY_VECTORIZATION;
import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyFunctionalityVectorizationByCallGraph.SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_CALLGRAPH;
import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyFunctionalityVectorizationBySequenceOfAccesses.SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES;
import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyStructure.SIMILARITY_SCIPY_STRUCTURE;

public class SimilarityScipyFactory {

	public static SimilarityScipy getSimilarityScipy(RecommendMatrixSciPy recommendation) {
		switch (SimilarityKind.getByName(recommendation.getKindName()).similarityType()) {
		case SIMILARITY_SCIPY_STRUCTURE:
			return new SimilarityScipyStructure(recommendation);
		case SIMILARITY_SCIPY_ACCESSES_REPOSITORY:
			return new SimilarityScipyAccessesAndRepository(recommendation);
		case SIMILARITY_SCIPY_CLASS_VECTORIZATION:
			return new SimilarityScipyClassVectorization(recommendation);
		case SIMILARITY_SCIPY_ENTITY_VECTORIZATION:
			return new SimilarityScipyEntityVectorization(recommendation);
		case SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_CALLGRAPH:
			return new SimilarityScipyFunctionalityVectorizationByCallGraph(recommendation);
		case SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES:
			return new SimilarityScipyFunctionalityVectorizationBySequenceOfAccesses(recommendation);
		default:
			throw new RuntimeException("The " + recommendation.getKindName() + " Similarity kind is computed by "
					+ SimilarityKind.getByName(recommendation.getKindName()).similarityType() + ", which builds no recommendation candidate.");
		}
	}

}
