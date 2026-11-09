import {SimilarityKind} from "../../../models/similarity/SimilarityKinds";
import {APIService} from "../../../services/APIService";
import {WeightsFactory} from "../../../models/weights/WeightsFactory";

// A form makes its Similarity of a kind on the software system when given kindName, or through the strategy otherwise.
// FIXME (ticket 09): the strategy path goes with Strategy

export function defaultWeights(kindName, strategy) {
    return kindName
        ? WeightsFactory.getWeightListByKind(kindName)
        : WeightsFactory.getWeightListByStrategyType(strategy.strategyTypes);
}

// The kind the profile selector asks about: the form's kind, or the one the strategy stands for (D-026).
export function profileKindName(kindName, strategy) {
    if (kindName)
        return kindName;
    // Hard-coded on purpose: Accesses and Repository is the only kind a strategy holds as two types. This copies a backend rule, which is
    // acceptable only because the strategy screen is removed in slice 10.
    return strategy.strategyTypes.length === 1 ? strategy.strategyTypes[0] : SimilarityKind.ACCESSES_AND_REPOSITORY;
}

export function submitSimilarity(softwareSystemId, kindName, strategy, request) {
    const service = new APIService();
    return kindName
        ? service.createSimilarityOnSoftwareSystem(softwareSystemId, {...request, kindName})
        : service.createSimilarity({...request, strategyId: strategy.id});
}
