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
    analysis(decomposition1Name: string, decomposition2Name: string) {
        return this.axios.post<ComparisonToolDto>("/comparison/" + decomposition1Name + "/" + decomposition2Name);
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
    deleteSimilarity(similarityName: string) {
        return this.axios.delete<null>("/similarity/" + similarityName + "/delete");
    }

    getSimilarity(similarityName: string) {
        return this.axios.get("/similarity/" + similarityName + "/getSimilarity").then(
            response => { return SimilarityFactory.getSimilarity(response.data); });
    }

    createSimilarity(similarity: Similarity) {
        return this.axios.post<null>("/similarity/create", similarity);
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

    getEdgeWeights(decompositionName: string, viewType: string) {
        return this.axios.get<Edges>("/decomposition/" + decompositionName + "/" + viewType + "/getEdgeWeights");
    }

    getGraphPositions(decompositionName: string) {
        return this.axios.get("/positionHistory/" + decompositionName + "/getGraphPositions");
    }

    saveGraphPositions(decompositionName: string, graphPositions: any) {
        return this.axios.post<null>("/positionHistory/" + decompositionName + "/saveGraphPositions", graphPositions);
    }

    snapshotDecomposition(decompositionName: string) {
        return this.axios.get<null>("/decomposition/" + decompositionName + "/snapshotDecomposition");
    }

    createDecomposition(decompositionDto: any) {
        return this.axios.post("/similarity/createDecomposition", decompositionDto);
    }

    createExpertDecomposition(
        similarityName: string,
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

        return this.axios.post<null>("/similarity/" + similarityName + "/createExpertDecomposition", data, config);
    }

    //Decomposition

    getDecompositions(
        similarityName: string
    ) {
        return this.axios.get<Decomposition[]>(
            "/similarity/" + similarityName + "/decompositions",
        ).then(responseList => {
            if (responseList.data.length == 0)
                return responseList.data;
            return responseList.data.map((response: any) => {
                return DecompositionFactory.getDecomposition(response);
            });
        });
    }

    getDecomposition(
        decompositionName: string
    ) {
        return this.axios.get<Decomposition>("/decomposition/" + decompositionName)
            .then(response => DecompositionFactory.getDecomposition(response.data));
    }

    getClusters(
        decompositionName: string
    ) {
        return this.axios.get<Record<number, Cluster>>("/decomposition/" + decompositionName + "/getClusters");
    }

    updateDecomposition(
        decompositionName: string
    ) {
        return this.axios.get<Decomposition>(
            "/decomposition/" + decompositionName + "/update"
        );
    }

    async exportDecomposition(decompositionName: string) {
        return await this.axios.get(
            "/decomposition/" + decompositionName + "/export",
            {responseType: 'blob',}
        );
    }

    deleteDecomposition(
        decompositionName: string
    ) {
        return this.axios.delete<null>(
            "/decomposition/" + decompositionName + "/delete"
        );
    }

    getLocalTransactionsGraphForFunctionality(
        decompositionName: string,
        functionalityName: string
    ) {
        return this.axios.get<LocalTransactionsGraph>(
            addSearchParamsToUrl(
                "/accesses/" + decompositionName + "/getLocalTransactionsGraphForFunctionality",
                { functionalityName: functionalityName },
            )
        );
    }

    //Cluster

    mergeClusters(
        decompositionName: string,
        data: any
    ) {
        return this.axios.post<null>(
            "/decomposition/" + decompositionName + "/merge", data);
    }

    renameCluster(
        decompositionName: string,
        data: any
    ) {
        return this.axios.post<null>("/decomposition/" + decompositionName + "/rename", data);
    }

    splitCluster(
        decompositionName: string,
        data: any
    ) {
        return this.axios.post<null>("/decomposition/" + decompositionName + "/split", data);
    }

    formCluster(
        decompositionName: string,
        data: any
    ) {
        return this.axios.post<null>("/decomposition/" + decompositionName + "/formCluster", data);
    }

    transferEntities(
        decompositionName: string,
        data: any
    ) {
        return this.axios.post<null>("/decomposition/" + decompositionName + "/transfer", data);
    }

    undoOperation(
        decompositionName: string
    ) {
        return this.axios.get("/history/" + decompositionName + "/undoOperation");
    }

    redoOperation(
        decompositionName: string
    ) {
        return this.axios.get("/history/" + decompositionName + "/redoOperation");
    }

    canUndoRedo(
        decompositionName: string
    ) {
        return this.axios.get<Record<string, Boolean>>("/history/" + decompositionName + "/canUndoRedo");
    }

    getFunctionalitiesAndFunctionalitiesClusters(
        decompositionName: string,
    ) {
        return this.axios.get<Record<string, any>>(
            "/accesses/" + decompositionName + "/getFunctionalitiesAndFunctionalitiesClusters"
        );
    }

    getClustersAndClustersFunctionalities(
        decompositionName: string
    ) {
        return this.axios.get<Record<string, any>>(
            "/accesses/" + decompositionName + "/getClustersAndClustersFunctionalities"
        );
    }

    getSearchItems(
        decompositionName: string,
        viewType: string
) {
        return this.axios.get<any[]>(
            "/decomposition/" + decompositionName + "/" + viewType + "/getSearchItems"
        );
    }


    //FunctionalityRedesign

    addCompensating(
        decompositionName: string,
        functionalityName: string,
        redesignName: string,
        clusterID: number,
        entities: string,
        fromID: string
    ) {
        return this.axios.post(
            "/decomposition/" + decompositionName + "/functionality/" + functionalityName + "/redesign/" + redesignName + "/addCompensating",
            {
                fromID: fromID,
                cluster : clusterID,
                entities : entities
            }
        );
    }

    sequenceChange(
        decompositionName: string,
        functionalityName: string,
        redesignName: string,
        localTransaction: string,
        newCaller: string
    ) {
        return this.axios.post(
            "/decomposition/" + decompositionName + "/functionality/" + functionalityName + "/redesign/" + redesignName + "/sequenceChange",
            {
                localTransactionID: localTransaction,
                newCaller: newCaller
            }
        );
    }

    dcgi(
        decompositionName: string,
        functionalityName: string,
        redesignName: string,
        fromClusterID: number,
        toClusterID: number,
        localTransactions: string
    ) {
        return this.axios.post(
            "/decomposition/" + decompositionName + "/functionality/" + functionalityName + "/redesign/" + redesignName + "/dcgi",
            {
                fromCluster: fromClusterID,
                toCluster: toClusterID,
                localTransactions: localTransactions
            }
        );
    }

    selectPivotTransaction(
        decompositionName: string,
        functionalityName: string,
        redesignName: string,
        transactionID: string,
        newRedesignName: string
    ) {
        return this.axios.post(
            "/decomposition/" + decompositionName + "/functionality/" + functionalityName + "/redesign/" + redesignName + "/pivotTransaction",
            null,
            { params: {"transactionID" : transactionID, "newRedesignName": newRedesignName}}
        );
    }

    changeLTName(
        decompositionName: string,
        functionalityName: string,
        redesignName: string,
        transactionID: string,
        newName: string
    ) {
        return this.axios.post(
            "/decomposition/" + decompositionName + "/functionality/" + functionalityName + "/redesign/" + redesignName + "/changeLTName",
            null,
            { params: {"transactionID" : transactionID, "newName": newName}}
        );
    }

    deleteRedesign(
        decompositionName: string,
        functionalityName: string,
        redesignName: string
    ){
        return this.axios.delete(
            "/decomposition/" + decompositionName + "/functionality/" + functionalityName + "/redesign/" + redesignName + "/deleteRedesign"
        );
    }

    setUseForMetrics(
        decompositionName: string,
        functionalityName: string,
        redesignName: string
    ){
        return this.axios.post(
            "/decomposition/" + decompositionName + "/functionality/" + functionalityName + "/redesign/" + redesignName + "/useForMetrics"
        );
    }

    refactorSoftwareSystem(
        decompositionName: string,
        functionalityNames: string[],
        dataDependenceThreshold: number,
        minimizeSumOfComplexities: boolean,
        timeOut: number,
        newRefactor: boolean,
    ) {

        const refactorRequest: RefactorSoftwareSystem = {
            decomposition_name: decompositionName,
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
        decompositionName: string,
    ) {

        return this.refactorizationToolAxios.get(
            "/refactor/decomposition/" + decompositionName ,
        );
    }
}