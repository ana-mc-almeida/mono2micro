package pt.ist.socialsoftware.mono2micro.similarity.kind;

import com.fasterxml.jackson.annotation.JsonTypeName;

import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.ACCESSES_STRATEGY;

/** Needs only what every kind needs, which {@link SimilarityKindFamily} shares. */
@JsonTypeName(ACCESSES_STRATEGY)
public class AccessesKind extends SimilarityKind {
}
