import {SimilarityKind} from "../../../models/similarity/SimilarityKinds";
import {SimilarityMatrixSciPyForm} from "./SimilarityMatrixSciPyForm";
import {SimilarityMatrixSciPyStructureForm} from "./SimilarityMatrixSciPyStructureForm";
import {SimilarityMatrixSciPyClassVectorizationForm} from "./SimilarityMatrixSciPyClassVectorizationForm";
import {SimilarityMatrixSciPyEntityVectorizationForm} from "./SimilarityMatrixSciPyEntityVectorizationForm";
import {SimilarityMatrixSciPyFunctionalityVectorizationCallGraphForm} from "./SimilarityMatrixSciPyFunctionalityVectorizationCallGraphForm";
import {SimilarityMatrixScipyFunctionalityVectorizationSequenceOfAccessesForm} from "./SimilarityMatrixScipyFunctionalityVectorizationSequenceOfAccessesForm";

// The form for each Similarity kind, by the kind's name (D-013). Which kinds are offered comes from the backend. A new kind needs its form here,
// its name in SimilarityKinds.ts and its default weights in WeightsFactory.getWeightListByKind (AGENT.md, "New Similarity kind").
export const similarityForms = {
    [SimilarityKind.ACCESSES]: SimilarityMatrixSciPyForm,
    [SimilarityKind.REPOSITORY]: SimilarityMatrixSciPyForm,
    [SimilarityKind.ACCESSES_AND_REPOSITORY]: SimilarityMatrixSciPyForm,
    [SimilarityKind.STRUCTURE]: SimilarityMatrixSciPyStructureForm,
    [SimilarityKind.CLASS_VECTORIZATION]: SimilarityMatrixSciPyClassVectorizationForm,
    [SimilarityKind.ENTITY_VECTORIZATION]: SimilarityMatrixSciPyEntityVectorizationForm,
    [SimilarityKind.FUNCTIONALITY_VECTORIZATION_CALLGRAPH]: SimilarityMatrixSciPyFunctionalityVectorizationCallGraphForm,
    [SimilarityKind.FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES]: SimilarityMatrixScipyFunctionalityVectorizationSequenceOfAccessesForm,
};
