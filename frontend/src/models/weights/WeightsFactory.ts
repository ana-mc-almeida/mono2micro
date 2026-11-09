import AccessesWeights, {ACCESSES_WEIGHTS} from "./AccessesWeights";
import Weights from "./Weights";
import RepositoryWeights, {REPOSITORY_WEIGHTS} from "./RepositoryWeights";
import ClassVectorizationWeights, {CLASS_VECTORIZATION_WEIGHTS} from "./ClassVectorizationWeights";
import EntityVectorizationWeights, {ENTITY_VECTORIZATION_WEIGHTS} from "./EntityVectorizationWeights";
import FunctionalityVectorizationCallGraphWeights, {FUNCTIONALITY_VECTORIZATION_CALLGRAPH_WEIGHTS} from "./FunctionalityVectorizationCallGraphWeights";
import FunctionalityVectorizationAccessesWeights, {FUNCTIONALITY_VECTORIZATION_ACCESSES_WEIGHTS} from "./FunctionalityVectorizationAccessesWeights";
import {SimilarityKind} from "../similarity/SimilarityKinds";
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

    // The default weights of each Similarity kind's form: one lookup from the kind's name.
    static getWeightListByKind(kind: string) : Weights[] {
        const defaults: {[kind: string]: () => Weights[]} = {
            [SimilarityKind.ACCESSES]: () => [new AccessesWeights({accessMetricWeight: 25, writeMetricWeight: 25, readMetricWeight: 25, sequenceMetricWeight: 25})],
            [SimilarityKind.REPOSITORY]: () => [new RepositoryWeights({authorMetricWeight: 50, commitMetricWeight: 50})],
            [SimilarityKind.ACCESSES_AND_REPOSITORY]: () => [
                new AccessesWeights({ accessMetricWeight: 17, writeMetricWeight: 17, readMetricWeight: 17, sequenceMetricWeight: 17}),
                new RepositoryWeights({authorMetricWeight: 16, commitMetricWeight: 16})
            ],
            [SimilarityKind.CLASS_VECTORIZATION]: () => [],
            [SimilarityKind.ENTITY_VECTORIZATION]: () => [],
            [SimilarityKind.FUNCTIONALITY_VECTORIZATION_CALLGRAPH]: () => [new FunctionalityVectorizationCallGraphWeights({controllersWeight: 25, servicesWeight: 25, intermediateMethodsWeight: 25, entitiesWeight: 25})],
            [SimilarityKind.FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES]: () => [new FunctionalityVectorizationAccessesWeights({readMetricWeight: 50, writeMetricWeight: 50})],
            [SimilarityKind.STRUCTURE]: () => [new StructureWeights({oneToOneWeight: 33, oneToManyWeight: 33, heritageWeight: 34})],
        };
        if (defaults[kind] === undefined)
            throw new Error('No default weights for the Similarity kind ' + kind + '.');
        return defaults[kind]();
    }
}
