import Similarity from "./Similarity";
import {TraceType} from "../../type-declarations/types";
import Card from "react-bootstrap/Card";
import Button from "react-bootstrap/Button";
import {URL} from "../../constants/constants"
import React from "react";
import {WeightsFactory} from "../weights/WeightsFactory";
import Weights from "../weights/Weights";

const SIMILARITY_SCIPY_ACCESSES_REPOSITORY = 'SIMILARITY_SCIPY_ACCESSES_REPOSITORY';
export {SIMILARITY_SCIPY_ACCESSES_REPOSITORY};

export default class SimilarityScipyAccessesAndRepository extends Similarity {
    profileId: string | null;
    profileName: string | null;
    linkageType: string;
    tracesMaxLimit: number;
    traceType: TraceType;
    weightsList: Weights[];

    constructor(similarity: any) {
        super(similarity);
        // Initializes default values if no previous value is provided
        this.profileId =            similarity.profileId               ||     null;
        this.profileName =          similarity.profileName             ||     null;
        this.linkageType =          similarity.linkageType             ||     "average";
        this.tracesMaxLimit =       similarity.tracesMaxLimit          ||     0;
        this.traceType =            similarity.traceType               ||     TraceType.ALL;
        this.weightsList =          WeightsFactory.getWeightsList(similarity.weightsList);
    }

    printCard(handleDeleteSimilarity: (similarity: Similarity) => void): JSX.Element {
        return <Card key={this.id} className="mb-4" style={{ width: '20rem' }}>
            <Card.Img
                variant="top"
                src={URL + "/similarity/" + this.id + "/image?" + new Date().getTime()}
            />
            <Card.Body>
                <Card.Title>{this.name}</Card.Title>
                <Card.Text>
                    Profile: {this.profileName === null ? "No profile" : this.profileName} <br />
                    AmountOfTraces: {this.tracesMaxLimit} <br />
                    Type of traces: {this.traceType} <br />
                    Linkage Type: {this.linkageType} < br />
                    {this.weightsList.flatMap((weights: any) =>
                        Object.entries(weights.weightsLabel).map(([key, value]) => <span key={key}>{value + ": " + weights[key] + "%"} <br/></span>)
                    )}
                </Card.Text>
                <Button href={`/softwareSystems/${this.softwareSystemId}/${this.strategyId}/${this.id}/decomposition`}
                        variant={"success"}
                        className="mb-2">
                    Decomposition Generation
                </Button>
                <br />
                <Button
                    onClick={() => handleDeleteSimilarity(this)}
                    variant="danger"
                >
                    Delete
                </Button>
            </Card.Body>
        </Card>;
    }
}