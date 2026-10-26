export default abstract class Similarity {
    id?: string;
    softwareSystemId?: string;
    strategyId!: string;
    strategyName?: string;
    type!: string;
    name?: string;

    protected constructor(similarity: any) {
        this.id = similarity.id;
        this.softwareSystemId = similarity.softwareSystemId;
        this.strategyId = similarity.strategyId;
        this.strategyName = similarity.strategyName;
        this.type = similarity.type;
        this.name = similarity.name;
    }

    // This function is used to display the decomposition
    abstract printCard(handleDeleteSimilarity: (similarity: Similarity) => void): JSX.Element;
}

