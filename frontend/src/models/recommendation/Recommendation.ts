export default class Recommendation {
    id?: string;
    type!: string;
    strategyId!: string;
    strategyName?: string;
    name: string;
    isCompleted: boolean;

    constructor(recommendation: any) {
        this.type               = recommendation.type;
        this.id                 = recommendation.id;
        this.strategyId         = recommendation.strategyId;
        this.strategyName       = recommendation.strategyName;
        this.name               = recommendation.name;
        this.isCompleted        = Boolean(recommendation.completed) || false;
    }
}
