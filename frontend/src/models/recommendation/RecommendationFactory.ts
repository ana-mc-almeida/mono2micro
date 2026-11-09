import Recommendation from "./Recommendation";
import {RecommendationType} from "./RecommendationTypes";
import RecommendMatrixSciPy from "./RecommendMatrixSciPy";

export abstract class RecommendationFactory {
    static getRecommendation(recommendation: any) : Recommendation {
        switch (recommendation.type) {
            case RecommendationType.RECOMMEND_MATRIX_SCIPY:
                return new RecommendMatrixSciPy(recommendation);
            default:
                throw new Error('Type ' + recommendation.type + ' unknown.');
        }
    }
}
