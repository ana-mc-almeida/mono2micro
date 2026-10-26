import {TraceType} from "../../type-declarations/types";
import Recommendation from "./Recommendation";

export default class RecommendMatrixSciPy extends Recommendation {
    profileId: string | null;
    profileName: string | null;
    linkageType: string[];
    traceType: TraceType[];
    tracesMaxLimit: number;
    weightsList: any[];

    constructor(recommendation: any) {
        super(recommendation);
        this.profileId =                recommendation.profileId            ||     null;
        this.profileName =              recommendation.profileName          ||     null;
        this.tracesMaxLimit =           recommendation.tracesMaxLimit       ||     0;
        this.traceType =                recommendation.traceType            ||     TraceType.ALL;
        this.linkageType =              recommendation.linkageType          ||     "average";
        this.weightsList =              recommendation.weightsList;
    }
}