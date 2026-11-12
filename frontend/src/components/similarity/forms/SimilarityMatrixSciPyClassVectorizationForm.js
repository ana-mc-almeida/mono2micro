import React, {useEffect, useState} from "react";
import Form from "react-bootstrap/Form";
import Row from "react-bootstrap/Row";
import Col from "react-bootstrap/Col";
import Button from "react-bootstrap/Button";
import {SIMILARITY_SCIPY_CLASS_VECTORIZATION} from "../../../models/similarity/SimilarityScipyClassVectorization";
import {UPLOADING} from "./similarityCreationStatus";


export const SimilarityMatrixSciPyClassVectorizationForm = ({softwareSystemId, kindName, status, onSubmit}) => {
    const [linkageType, setLinkageType] = useState("average");

    // Executes when it is informed that there is information to be updated
    useEffect(() => {
    }, [])

    function handleSubmit(event) {
        event.preventDefault();
        onSubmit({
            kindName,
            type: SIMILARITY_SCIPY_CLASS_VECTORIZATION,
            linkageType
        });
    }

    function handleChangeLinkageType(event) {
        setLinkageType(event.target.id);
    }

    return (
        <>
            <Form onSubmit={handleSubmit} className="mt-2 mb-3">
                <Form.Group as={Row} className="align-items-center mb-3">
                    <Form.Label as="legend" column sm={2}>
                        Linkage Type
                    </Form.Label>
                    <Col sm="auto">
                        <Form.Check onClick={handleChangeLinkageType} name="linkageType" label="Average" type="radio" id="average" defaultChecked/>
                    </Col>
                    <Col sm="auto">
                        <Form.Check onClick={handleChangeLinkageType} name="linkageType" label="Single" type="radio" id="single"/>
                    </Col>
                    <Col sm="auto">
                        <Form.Check onClick={handleChangeLinkageType} name="linkageType" label="Complete" type="radio" id="complete"/>
                    </Col>
                </Form.Group>
                <Form.Group as={Row} className="align-items-center">
                    <Col sm={{offset: 2}}>
                        <Button
                            type="submit"
                            disabled={status === UPLOADING || false}
                        >
                            Generate Similarity Distances
                        </Button>
                        <Form.Text className="ms-2">
                            {status}
                        </Form.Text>
                    </Col>
                </Form.Group>
            </Form>
        </>
    );
}