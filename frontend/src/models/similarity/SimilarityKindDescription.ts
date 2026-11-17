import Weights from "../weights/Weights";

// A setting a Similarity request can take besides its linkage type, its profile and its weights, which every kind's form shows. The backend's
// kind declares which ones it takes (SimilaritySetting.java).
export enum SimilaritySetting {
    TRACES_MAX_LIMIT = "TRACES_MAX_LIMIT",
    TRACE_TYPE = "TRACE_TYPE",
    MAX_DEPTH = "MAX_DEPTH",
}

// What the Similarity form needs to render a kind, as the backend's kind declares it (D-029): the request type that computes it, the
// settings it takes, and the weights it starts from.
export interface SimilarityKindDescription {
    name: string;
    similarityType: string;
    settings: SimilaritySetting[];
    defaultWeights: Weights[];
}
