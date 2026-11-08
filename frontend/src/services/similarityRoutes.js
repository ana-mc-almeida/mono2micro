// Where a Similarity's pages live. One made through a strategy lives under the strategy; one made on the software system, under
// "similarities" (ticket 09).
// FIXME (ticket 09): the strategy segment goes with Strategy

export function similarityBase(softwareSystemId, strategyId) {
    return strategyId
        ? `/softwareSystems/${softwareSystemId}/${strategyId}`
        : `/softwareSystems/${softwareSystemId}/similarities`;
}

// The page that lists the Similarity: its strategy's page, or the software system's.
export function similaritiesPage(softwareSystemId, strategyId) {
    return strategyId
        ? `/softwareSystems/${softwareSystemId}/${strategyId}/similarity`
        : `/softwareSystems/${softwareSystemId}`;
}
