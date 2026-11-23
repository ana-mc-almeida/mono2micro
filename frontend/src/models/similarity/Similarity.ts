export default abstract class Similarity {
    id?: string;
    softwareSystemId?: string;
    type!: string;
    name?: string;
    // The name of its Similarity kind, and the ids of the representations it reads.
    kindName!: string;
    representationIds?: string[];

    protected constructor(similarity: any) {
        this.id = similarity.id;
        this.softwareSystemId = similarity.softwareSystemId;
        this.type = similarity.type;
        this.name = similarity.name;
        this.kindName = similarity.kindName;
        this.representationIds = similarity.representationIds;
    }

    // This function is used to display the decomposition
    abstract printCard(handleDeleteSimilarity: (similarity: Similarity) => void): JSX.Element;
}

