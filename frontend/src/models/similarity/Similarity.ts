export default abstract class Similarity {
    softwareSystemId?: string;
    strategyName!: string;
    type!: string;
    name?: string;

    protected constructor(similarity: any) {
        this.softwareSystemId = similarity.softwareSystemId;
        this.strategyName = similarity.strategyName;
        this.type = similarity.type;
        this.name = similarity.name;
    }

    // This function is used to display the decomposition
    abstract printCard(handleDeleteSimilarity: (similarity: Similarity) => void): JSX.Element;
}

