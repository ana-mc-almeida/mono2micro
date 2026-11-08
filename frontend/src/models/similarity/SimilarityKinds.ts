// The names of the Similarity kinds, as the backend's @JsonTypeName gives them. Which kinds a software system offers comes from the backend.
export enum SimilarityKind {
    ACCESSES = 'Accesses',
    REPOSITORY = 'Repository',
    ACCESSES_AND_REPOSITORY = 'Accesses and Repository',
    STRUCTURE = 'Structure',
    CLASS_VECTORIZATION = 'Class Vectorization',
    ENTITY_VECTORIZATION = 'Entity Vectorization',
    FUNCTIONALITY_VECTORIZATION_CALLGRAPH = 'Functionality Vectorization Call Graph',
    FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES = 'Functionality Vectorization Sequence Accesses'
}
