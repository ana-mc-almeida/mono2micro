export default class Recommendation {
    id?: string;
    type!: string;
    softwareSystemId!: string;
    kindName!: string;
    name: string;
    isCompleted: boolean;

    constructor(recommendation: any) {
        this.type               = recommendation.type;
        this.id                 = recommendation.id;
        this.softwareSystemId   = recommendation.softwareSystemId;
        this.kindName               = recommendation.kindName;
        this.name               = recommendation.name;
        this.isCompleted        = Boolean(recommendation.completed) || false;
    }
}
