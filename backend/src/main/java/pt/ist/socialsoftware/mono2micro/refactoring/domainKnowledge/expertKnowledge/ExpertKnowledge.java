package pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge;

import pt.ist.socialsoftware.mono2micro.feature.VariableFeature;

/**
 * The model's Expert knowledge: what the architect decides about the decomposition, such as which units a Similarity is scoped to
 * ({@link Profile}, D-017) or, in the model's own example, the maximum number of clusters. Each kind of expert knowledge is a feature of
 * {@link ExpertKnowledgeFamily}, found by the scan, so a new one joins by implementing this type.
 */
public interface ExpertKnowledge extends VariableFeature {
}
