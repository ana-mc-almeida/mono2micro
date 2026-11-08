import {APIService} from "../../../services/APIService";
import {WeightsFactory} from "../../../models/weights/WeightsFactory";

// A form makes its Similarity of a kind on the software system when given kindName, or through the strategy otherwise.
// FIXME (ticket 09): the strategy path goes with Strategy

export function defaultWeights(kindName, strategy) {
    return kindName
        ? WeightsFactory.getWeightListByKind(kindName)
        : WeightsFactory.getWeightListByStrategyType(strategy.strategyTypes);
}

export function submitSimilarity(softwareSystemId, kindName, strategy, request) {
    const service = new APIService();
    return kindName
        ? service.createSimilarityOnSoftwareSystem(softwareSystemId, {...request, kindName})
        : service.createSimilarity({...request, strategyId: strategy.id});
}
