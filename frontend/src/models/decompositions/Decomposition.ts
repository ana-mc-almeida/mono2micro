export default abstract class Decomposition {
    softwareSystemId: string;
    softwareSystemName: string;
    strategyId: string;
    strategyName: string;
    similarityId: string;
    similarityName: string;
    name: string;
    type: string;
    expert: boolean;
    clusters: any;
    metrics: Record<string, any>;
    representationInformations: string[];

    protected constructor(decomposition: any) {
        this.softwareSystemId = decomposition.softwareSystemId;
        this.softwareSystemName = decomposition.softwareSystemName;
        this.strategyId = decomposition.strategyId;
        this.strategyName = decomposition.strategyName;
        this.similarityId = decomposition.similarityId;
        this.similarityName = decomposition.similarityName;
        this.name = decomposition.name;
        this.type = decomposition.type;
        this.expert = decomposition.expert;
        this.clusters = decomposition.clusters;
        this.metrics = decomposition.metrics;
        this.representationInformations = decomposition.representationInformations;
    }

    abstract printCard(
        reloadDecompositions: () => void,
        handleDeleteDecomposition: (collector: string) => void,
        handleExportDecomposition: (collector: string) => void
    ): JSX.Element;
}