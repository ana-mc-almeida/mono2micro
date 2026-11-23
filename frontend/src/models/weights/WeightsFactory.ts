import AccessesWeights, {ACCESSES_WEIGHTS} from "./AccessesWeights";
import Weights from "./Weights";
import RepositoryWeights, {REPOSITORY_WEIGHTS} from "./RepositoryWeights";
import ClassVectorizationWeights, {CLASS_VECTORIZATION_WEIGHTS} from "./ClassVectorizationWeights";
import EntityVectorizationWeights, {ENTITY_VECTORIZATION_WEIGHTS} from "./EntityVectorizationWeights";
import FunctionalityVectorizationCallGraphWeights, {FUNCTIONALITY_VECTORIZATION_CALLGRAPH_WEIGHTS} from "./FunctionalityVectorizationCallGraphWeights";
import FunctionalityVectorizationAccessesWeights, {FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS} from "./FunctionalityVectorizationAccessesWeights";
import StructureWeights, {STRUCTURE_WEIGHTS} from "./StructureWeights";

export abstract class WeightsFactory {
    static getWeights(weights: any) : Weights {
        switch (weights.type) {
            case ACCESSES_WEIGHTS:
                return new AccessesWeights(weights);
            case REPOSITORY_WEIGHTS:
                return new RepositoryWeights(weights);
            case STRUCTURE_WEIGHTS:
                return new StructureWeights(weights);
            case CLASS_VECTORIZATION_WEIGHTS:
                return new ClassVectorizationWeights(weights);
            case ENTITY_VECTORIZATION_WEIGHTS:
                return new EntityVectorizationWeights(weights);
            case FUNCTIONALITY_VECTORIZATION_CALLGRAPH_WEIGHTS:
                return new FunctionalityVectorizationCallGraphWeights(weights);
            case FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS:
                return new FunctionalityVectorizationAccessesWeights(weights);
            default:
                throw new Error('Type ' + weights.type + ' unknown.');
        }
    }

    static getWeightsList(weightsList: any[]) : Weights[] {
        return weightsList.map(weights => WeightsFactory.getWeights(weights));
    }
}
