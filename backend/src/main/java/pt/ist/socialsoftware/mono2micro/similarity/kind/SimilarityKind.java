package pt.ist.socialsoftware.mono2micro.similarity.kind;

import pt.ist.socialsoftware.mono2micro.feature.VariableFeature;

/**
 * One of the seven Similarity kinds the tool offers (today's strategy types), as a feature that
 * declares what a System must have collected before the kind can be offered.
 *
 * <p>A kind is not a {@code SimilarityScipy*} class. Those six classes compute the matrices and
 * stay fused (Gap 5, D-009), and one of them, {@code SimilarityScipyAccessesAndRepository},
 * computes two kinds. So each kind is its own small class, named by its {@code @JsonTypeName} and
 * found by the self-registration scan (D-021).
 */
public abstract class SimilarityKind implements VariableFeature {
}
