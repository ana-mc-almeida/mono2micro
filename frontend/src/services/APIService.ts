import axios, { AxiosInstance } from 'axios';
import { 
    URL,
    REFACTORIZATION_TOOL_URL,
} from '../constants/constants';
import {
    ComparisonToolDto,
    Cluster,
    LocalTransactionsGraph,
    RefactorSoftwareSystem,
    Edges
} from "../type-declarations/types";
import { addSearchParamsToUrl } from "../utils/url";
import {RepresentationFactory} from "../models/representation/RepresentationFactory";
import Similarity from "../models/similarity/Similarity";
import {SimilarityFactory} from "../models/similarity/SimilarityFactory";
import {DecompositionFactory} from "../models/decompositions/DecompositionFactory";
import Decomposition from "../models/decompositions/Decomposition";
import SoftwareSystem from "../models/softwareSystem/SoftwareSystem";
import {RecommendationFactory} from "../models/recommendation/RecommendationFactory";
import Recommendation from "../models/recommendation/Recommendation";
import Representation from "../models/representation/Representation";
import Strategy from "../models/strategy/Strategy";

export class APIService {
    axios: AxiosInstance;

    refactorizationToolAxios: AxiosInstance;

    constructor() {
        const headers = {
            'X-Custom-Header': 'Mono2Micro',
        };

        this.axios = axios.create({
            baseURL: URL,
            timeout: 0,
            headers: headers,
        });

        this.refactorizationToolAxios = axios.create({
            baseURL: REFACTORIZATION_TOOL_URL,
            timeout: 0,
            headers: headers,
        });
    }

    //Analysis
    analysis(decomposition1Id: string, decomposition2Id: string) {
        return this.axios.post<ComparisonToolDto>("/comparison/" + decomposition1Id + "/" + decomposition2Id);
    }

    // Recommendation
    recommendation(
        requestedStrategyRecommendation: Recommendation
    ) {
        return this.axios.put("recommendation/createRecommendation", requestedStrategyRecommendation)
            .then((response) => {return RecommendationFactory.getRecommendation(response.data)});
    }

    getRecommendationResult(
        recommendationName: string
    ) {
        return this.axios.get("/recommendation/" + recommendationName + "/getRecommendationResult")
            .then(response => {
                if (response.data === "")
                    return [];
                else return response.data;
            });
    }

    createRecommendationDecompositions(
        recommendationName: string,
        decompositionNames: string[]
    ) {
        const data = new FormData();
        for (const name of decompositionNames)
            data.append('decompositionNames', name);

        // unfortunately, this is needed so that data.append does not interpret the commas as an array separation
        data.append('decompositionNames', "");

        return this.axios.post<null>("/recommendation/" + recommendationName + "/createDecompositions", data);
    }

    //Software Systems
    getSoftwareSystems() {
        return this.axios.get("/softwareSystems").then(response => response.data.map((softwareSystem:any) => new SoftwareSystem(softwareSystem)));
    }

    getSoftwareSystem(softwareSystemId: string) {
        return this.axios.get("/softwareSystems/" + softwareSystemId).then(response => new SoftwareSystem(response.data));
    }

    getSoftwareSystemStrategies(softwareSystemId: string) {
        return this.axios.get("/softwareSystems/" + softwareSystemId + "/getSoftwareSystemStrategies")
            .then(response => {return response.data.map((strategy:any) => new Strategy(strategy))});
    }

    getSoftwareSystemDecompositions(softwareSystemId: string) {
        return this.axios.get<Decomposition[]>("/softwareSystems/" + softwareSystemId + "/getSoftwareSystemDecompositions");
    }

    deleteSoftwareSystem(softwareSystemId: string) {
        return this.axios.delete<null>("/softwareSystems/" + softwareSystemId + "/delete");
    }

    createSoftwareSystem(softwareSystemName: string) {
        const data = new FormData();
        data.append('softwareSystemName', softwareSystemName);
        return this.axios.post("/softwareSystems/create", data);
    }

    getSoftwareSystemRepresentationGroups(softwareSystemId: string) {
        return this.axios.get("/softwareSystems/" + softwareSystemId + "/getSoftwareSystemRepresentationGroups");
    }

    getRepresentationGroups() {
        return this.axios.get("/softwareSystems/getRepresentationGroups");
    }

    // Profiles
    addAccessesProfile(representationId: string, profile: string) {
        return this.axios.post<null>(
            "/representation/" + representationId + "/addAccessesProfile",
            null,
            {
                params: {
                    "profile" : profile
                }
            });
    }

    moveAccessesFunctionalities(
        representationId: string,
        functionalities: string[],
        targetProfile: string,
    ) {
        return this.axios.post<null>(
            "/representation/" + representationId + "/moveAccessesFunctionalities",
            functionalities,
            {
                params: {
                    "targetProfile" : targetProfile
                }
            }
        );
    }

    deleteAccessesProfile(representationId: string, profile: string) {
        return this.axios.delete<null>(
            "/representation/" + representationId + "/deleteAccessesProfile",
            {
                params: {
                    "profile" : profile
                }
            }
        );
    }

    //Representation

    addRepresentations(
        softwareSystemId: string,
        selectedRepresentationType: string,
        representations: Map<string, File>
    ) {
        const config = {
            headers: {
                'content-type': 'multipart/form-data'
            }
        }
        const data = new FormData();
        Object.entries(representations).forEach((entry) => {data.append("representationTypes", entry[0]); data.append("representations", entry[1])});

        return this.axios.post<null>("/softwareSystems/" + softwareSystemId + "/addRepresentations/" + selectedRepresentationType, data, config);
    }

    getRepresentations(softwareSystemId: string) {
        return this.axios.get<Representation[]>("/softwareSystems/" + softwareSystemId + "/getRepresentations")
            .then((response) => RepresentationFactory.getRepresentations(response.data));
    }

    getSoftwareSystemRepresentation(softwareSystemId: string, representationType: string) {
        return this.axios.get("/softwareSystems/" + softwareSystemId + "/representation/" + representationType + "/getSoftwareSystemRepresentation")
            .then((response) => RepresentationFactory.getRepresentation(response.data));
    }

    getRepresentation(representationId: string) {
        return this.axios.get("/representation/" + representationId + "/getRepresentation")
            .then((response) => RepresentationFactory.getRepresentation(response.data));
    }

    deleteRepresentation(representationId: string) {
        return this.axios.delete<null>("/representation/" + representationId + "/delete");
    }

    getIdToEntity(softwareSystemId: string) {
        // FIXME (ticket 07): takes a software system name under /representation; move with the backend route
        return this.axios.get<string>("/representation/" + softwareSystemId + "/getIdToEntity");
    }


    //Similarities
    deleteSimilarity(similarityId: string) {
        return this.axios.delete<null>("/similarity/" + similarityId + "/delete");
    }

    getSimilarity(similarityId: string) {
        return this.axios.get("/similarity/" + similarityId + "/getSimilarity").then(
            response => { return SimilarityFactory.getSimilarity(response.data); });
    }

    createSimilarity(similarity: Similarity) {
        return this.axios.post("/similarity/create", similarity);
    }

    getAllowableSoftwareSystemStrategyTypes(softwareSystemId: string) {
        return this.axios.get("/softwareSystems/" + softwareSystemId + "/getAllowableSoftwareSystemStrategyTypes");
    }

    getAlgorithmSupportedStrategyTypes(algorithmType: string) {
        return this.axios.get("/clustering/" + algorithmType + "/getAlgorithmSupportedStrategyTypes");
    }


    //Strategies
    createStrategy(
        softwareSystemId: string,
        strategyRepresentations: string[],
        strategyAlgorithm: string
    ) {
        const config = {
            headers: {
                'content-type': 'multipart/form-data'
            }
        }
        const data = new FormData();
        strategyRepresentations.forEach(repr => data.append("strategyTypes", repr));
        data.append("algorithmType", strategyAlgorithm);

        return this.axios.post("/softwareSystems/" + softwareSystemId + "/createStrategy", data, config);
    }

    getAlgorithms() {
        return this.axios.get("/strategy/getAlgorithms");
    }

    getStrategySimilarities(strategyId: string) {
        return this.axios.get("/strategy/" + strategyId + "/getStrategySimilarities").then(
            response => response.data.map((similarity: any) => SimilarityFactory.getSimilarity(similarity)));
    }

    getStrategyDecompositions(strategyId: string) {
        return this.axios.get("/strategy/" + strategyId + "/getStrategyDecompositions").then(
            response => response.data.map((decomposition: any) => DecompositionFactory.getDecomposition(decomposition)));
    }

    getStrategy(strategyId: string) {
        return this.axios.get("/strategy/" + strategyId + "/getStrategy").then(
            response => new Strategy(response.data));
    }

    deleteStrategy(strategyId: string) {
        return this.axios.delete<null>("/strategy/" + strategyId + "/delete");
    }

    getEdgeWeights(decompositionId: string, viewType: string) {
        return this.axios.get<Edges>("/decomposition/" + decompositionId + "/" + viewType + "/getEdgeWeights");
    }

    getGraphPositions(decompositionId: string) {
        return this.axios.get("/positionHistory/" + decompositionId + "/getGraphPositions");
    }

    saveGraphPositions(decompositionId: string, graphPositions: any) {
        return this.axios.post<null>("/positionHistory/" + decompositionId + "/saveGraphPositions", graphPositions);
    }

    snapshotDecomposition(decompositionId: string) {
        return this.axios.get<null>("/decomposition/" + decompositionId + "/snapshotDecomposition");
    }

    createDecomposition(decompositionDto: any) {
        return this.axios.post("/similarity/createDecomposition", decompositionDto);
    }

    createExpertDecomposition(
        similarityId: string,
        expertName: string,
        expertFile: any
    ) {
        const config = {
            headers: {
                'content-type': 'multipart/form-data'
            }
        }
        const data = new FormData();
        data.append('expertName', expertName);
        data.append('expertFile', expertFile);

        return this.axios.post<null>("/similarity/" + similarityId + "/createExpertDecomposition", data, config);
    }

    //Decomposition

    getDecompositions(
        similarityId: string
    ) {
        return this.axios.get<Decomposition[]>(
            "/similarity/" + similarityId + "/decompositions",
        ).then(responseList => {
            if (responseList.data.length == 0)
                return responseList.data;
            return responseList.data.map((response: any) => {
                return DecompositionFactory.getDecomposition(response);
            });
        });
    }

    getDecomposition(
        decompositionId: string
    ) {
        return this.axios.get<Decomposition>("/decomposition/" + decompositionId)
            .then(response => DecompositionFactory.getDecomposition(response.data));
    }

    getClusters(
        decompositionId: string
    ) {
        return this.axios.get<Record<number, Cluster>>("/decomposition/" + decompositionId + "/getClusters");
    }

    updateDecomposition(
        decompositionId: string
    ) {
        return this.axios.get<Decomposition>(
            "/decomposition/" + decompositionId + "/update"
        );
    }

    async exportDecomposition(decompositionId: string) {
        return await this.axios.get(
            "/decomposition/" + decompositionId + "/export",
            {responseType: 'blob',}
        );
    }

    deleteDecomposition(
        decompositionId: string
    ) {
        return this.axios.delete<null>(
            "/decomposition/" + decompositionId + "/delete"
        );
    }

    getLocalTransactionsGraphForFunctionality(
        decompositionId: string,
        functionalityName: string
    ) {
        return this.axios.get<LocalTransactionsGraph>(
            addSearchParamsToUrl(
                "/accesses/" + decompositionId + "/getLocalTransactionsGraphForFunctionality",
                { functionalityName: functionalityName },
            )
        );
    }

    //Cluster

    mergeClusters(
        decompositionId: string,
        data: any
    ) {
        return this.axios.post<null>(
            "/decomposition/" + decompositionId + "/merge", data);
    }

    renameCluster(
        decompositionId: string,
        data: any
    ) {
        return this.axios.post<null>("/decomposition/" + decompositionId + "/rename", data);
    }

    splitCluster(
        decompositionId: string,
        data: any
    ) {
        return this.axios.post<null>("/decomposition/" + decompositionId + "/split", data);
    }

    formCluster(
        decompositionId: string,
        data: any
    ) {
        return this.axios.post<null>("/decomposition/" + decompositionId + "/formCluster", data);
    }

    transferEntities(
        decompositionId: string,
        data: any
    ) {
        return this.axios.post<null>("/decomposition/" + decompositionId + "/transfer", data);
    }

    undoOperation(
        decompositionId: string
    ) {
        return this.axios.get("/history/" + decompositionId + "/undoOperation");
    }

    redoOperation(
        decompositionId: string
    ) {
        return this.axios.get("/history/" + decompositionId + "/redoOperation");
    }

    canUndoRedo(
        decompositionId: string
    ) {
        return this.axios.get<Record<string, Boolean>>("/history/" + decompositionId + "/canUndoRedo");
    }

    getFunctionalitiesAndFunctionalitiesClusters(
        decompositionId: string,
    ) {
        return this.axios.get<Record<string, any>>(
            "/accesses/" + decompositionId + "/getFunctionalitiesAndFunctionalitiesClusters"
        );
    }

    getClustersAndClustersFunctionalities(
        decompositionId: string
    ) {
        return this.axios.get<Record<string, any>>(
            "/accesses/" + decompositionId + "/getClustersAndClustersFunctionalities"
        );
    }

    getSearchItems(
        decompositionId: string,
        viewType: string
) {
        return this.axios.get<any[]>(
            "/decomposition/" + decompositionId + "/" + viewType + "/getSearchItems"
        );
    }


    //FunctionalityRedesign

    addCompensating(
        decompositionId: string,
        functionalityName: string,
        redesignName: string,
        clusterID: number,
        entities: string,
        fromID: string
    ) {
        return this.axios.post(
            "/decomposition/" + decompositionId + "/functionality/" + functionalityName + "/redesign/" + redesignName + "/addCompensating",
            {
                fromID: fromID,
                cluster : clusterID,
                entities : entities
            }
        );
    }

    sequenceChange(
        decompositionId: string,
        functionalityName: string,
        redesignName: string,
        localTransaction: string,
        newCaller: string
    ) {
        return this.axios.post(
            "/decomposition/" + decompositionId + "/functionality/" + functionalityName + "/redesign/" + redesignName + "/sequenceChange",
            {
                localTransactionID: localTransaction,
                newCaller: newCaller
            }
        );
    }

    dcgi(
        decompositionId: string,
        functionalityName: string,
        redesignName: string,
        fromClusterID: number,
        toClusterID: number,
        localTransactions: string
    ) {
        return this.axios.post(
            "/decomposition/" + decompositionId + "/functionality/" + functionalityName + "/redesign/" + redesignName + "/dcgi",
            {
                fromCluster: fromClusterID,
                toCluster: toClusterID,
                localTransactions: localTransactions
            }
        );
    }

    selectPivotTransaction(
        decompositionId: string,
        functionalityName: string,
        redesignName: string,
        transactionID: string,
        newRedesignName: string
    ) {
        return this.axios.post(
            "/decomposition/" + decompositionId + "/functionality/" + functionalityName + "/redesign/" + redesignName + "/pivotTransaction",
            null,
            { params: {"transactionID" : transactionID, "newRedesignName": newRedesignName}}
        );
    }

    changeLTName(
        decompositionId: string,
        functionalityName: string,
        redesignName: string,
        transactionID: string,
        newName: string
    ) {
        return this.axios.post(
            "/decomposition/" + decompositionId + "/functionality/" + functionalityName + "/redesign/" + redesignName + "/changeLTName",
            null,
            { params: {"transactionID" : transactionID, "newName": newName}}
        );
    }

    deleteRedesign(
        decompositionId: string,
        functionalityName: string,
        redesignName: string
    ){
        return this.axios.delete(
            "/decomposition/" + decompositionId + "/functionality/" + functionalityName + "/redesign/" + redesignName + "/deleteRedesign"
        );
    }

    setUseForMetrics(
        decompositionId: string,
        functionalityName: string,
        redesignName: string
    ){
        return this.axios.post(
            "/decomposition/" + decompositionId + "/functionality/" + functionalityName + "/redesign/" + redesignName + "/useForMetrics"
        );
    }

    refactorSoftwareSystem(
        decompositionId: string,
        functionalityNames: string[],
        dataDependenceThreshold: number,
        minimizeSumOfComplexities: boolean,
        timeOut: number,
        newRefactor: boolean,
    ) {

        const refactorRequest: RefactorSoftwareSystem = {
            decomposition_id: decompositionId,
            functionality_names: functionalityNames,
            data_dependence_threshold: dataDependenceThreshold,
            minimize_sum_of_complexities: minimizeSumOfComplexities,
            refactor_time_out_secs: timeOut,
            override_previous: newRefactor,
        };

        return this.refactorizationToolAxios.post(
            "/refactor",
            refactorRequest
        );
    }

    viewRefactor(
        decompositionId: string,
    ) {

        return this.refactorizationToolAxios.get(
            "/refactor/decomposition/" + decompositionId ,
        );
    }
}