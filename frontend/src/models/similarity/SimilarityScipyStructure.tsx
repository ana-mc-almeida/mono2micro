import Similarity from "./Similarity";
import Card from "react-bootstrap/Card";
import Button from "react-bootstrap/Button";
import {URL} from "../../constants/constants"
import React from "react";
import {WeightsFactory} from "../weights/WeightsFactory";
import Weights from "../weights/Weights";

const SIMILARITY_SCIPY_STRUCTURE = 'SIMILARITY_SCIPY_STRUCTURE';
export {SIMILARITY_SCIPY_STRUCTURE};    

export default class SimilarityScipyStructure extends Similarity {
    profileId: string | null;
    profileName: string | null;
    weightsList: Weights[];

    constructor(similarity: any) {
        super(similarity);
        this.profileId =            similarity.profileId               ||     null;
        this.profileName =          similarity.profileName             ||     null;
        this.weightsList = WeightsFactory.getWeightsList(similarity.weightsList);
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