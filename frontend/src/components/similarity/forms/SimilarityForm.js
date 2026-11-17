import React, {useEffect, useState} from "react";
import Form from "react-bootstrap/Form";
import Row from "react-bootstrap/Row";
import Col from "react-bootstrap/Col";
import FormControl from "react-bootstrap/FormControl";
import Button from "react-bootstrap/Button";
import {ProfileSelect} from "../../profile/ProfileSelect";
import {TraceType} from "../../../type-declarations/types";
import {SimilaritySetting} from "../../../models/similarity/SimilarityKindDescription";
import {UPLOADING} from "./similarityCreationStatus";
import {APIService} from "../../../services/APIService";

/**
 * The form of any Similarity kind (D-029). It shows what the backend's kind declares: the settings it takes and the weights it starts from,
 * besides the linkage type every kind takes and the profile, which shows only when the backend offers one for the kind. It only collects them
 * into a request; the page creates the Similarity.
 */
export const SimilarityForm = ({softwareSystemId, kindName, status, onSubmit}) => {
    const [kind, setKind] = useState(undefined);
    const [profileId, setProfileId] = useState(null);
    const [tracesMaxLimit, setTracesMaxLimit] = useState(0);
    const [traceType, setTraceType] = useState(TraceType.ALL);
    const [linkageType, setLinkageType] = useState("average");
    const [depth, setDepth] = useState(-1);
    const [weightsList, setWeightsList] = useState([]);
    const [weightSum, setWeightSum] = useState(0);

    useEffect(() => {
        new APIService().getSimilarityKind(kindName).then(description => {
            setKind(description);
            setWeightsList(description.defaultWeights);
        });
    }, [kindName]);

    useEffect(() => {
        let sum = 0;
        weightsList.forEach(weight => Object.keys(weight.weightsLabel).forEach(key => sum += weight[key]));
        setWeightSum(sum);
    }, [weightsList]);

    const takes = setting => kind !== undefined && kind.settings.includes(setting);
    const hasWeights = weightsList.length > 0;

    function handleSubmit(event) {
        event.preventDefault();
        onSubmit({
            kindName,
            type: kind.similarityType,
            profileId,
            linkageType,
            ...(takes(SimilaritySetting.TRACES_MAX_LIMIT) && {tracesMaxLimit}),
            ...(takes(SimilaritySetting.TRACE_TYPE) && {traceType}),
            ...(takes(SimilaritySetting.MAX_DEPTH) && {depth}),
            ...(hasWeights && {weightsList}),
        });
    }

    function changeMetricWeight(event, weightName) {
        setWeightsList(prev => prev.map(weight => {
            if (weight[weightName] || weight[weightName] === 0)
                weight[weightName] = event.target.value === 0 ? -1 : Number(event.target.value);
            return weight;
        }));
    }

    const complete = kind !== undefined
        && (!hasWeights || weightSum === 100)
        && (!takes(SimilaritySetting.TRACE_TYPE) || traceType !== "")
        && (!takes(SimilaritySetting.MAX_DEPTH) || depth > 0);

    return (
        <Form onSubmit={handleSubmit} className="mt-2 mb-3">
            <ProfileSelect
                softwareSystemId={softwareSystemId}
                kindName={kindName}
                profileId={profileId}
                setProfileId={setProfileId}
            />
            {takes(SimilaritySetting.TRACES_MAX_LIMIT) &&
                <Form.Group as={Row} controlId="amountOfTraces" className="mb-3">
                    <Form.Label column sm={2}>
                        Amount of Traces per Functionality
                    </Form.Label>
                    <Col sm={2}>
                        <FormControl
                            type="number"
                            placeholder="0 by default"
                            value={tracesMaxLimit === 0 ? '' : tracesMaxLimit}
                            onChange={event => setTracesMaxLimit(Number(event.target.value))}
                        />
                        <Form.Text className="text-muted">
                            If no number is inserted, 0 is assumed to be the default value meaning the maximum number of traces
                        </Form.Text>
                    </Col>
                </Form.Group>
            }
            {takes(SimilaritySetting.TRACE_TYPE) &&
                <Form.Group as={Row} className="align-items-center mb-3">
                    <Form.Label as="legend" column sm={2}>
                        Type of traces
                    </Form.Label>
                    <Col sm={3} style={{paddingLeft: 0}}>
                        <Col sm="auto">
                            <Form.Check onClick={event => setTraceType(event.target.value)} name="traceType" label="All" type="radio" id="allTraces" value="ALL" defaultChecked/>
                        </Col>
                        <Col sm="auto">
                            <Form.Check onClick={event => setTraceType(event.target.value)} name="traceType" label="Longest" type="radio" id="longest" value="LONGEST"/>
                        </Col>
                        <Col sm="auto">
                            <Form.Check onClick={event => setTraceType(event.target.value)} name="traceType" label="With more different accesses" type="radio" id="withMoreDifferentTraces" value="WITH_MORE_DIFFERENT_ACCESSES"/>
                        </Col>
                    </Col>
                </Form.Group>
            }
            <Form.Group as={Row} className="align-items-center mb-3">
                <Form.Label as="legend" column sm={2}>
                    Linkage Type
                </Form.Label>
                <Col sm="auto">
                    <Form.Check onClick={event => setLinkageType(event.target.id)} name="linkageType" label="Average" type="radio" id="average" defaultChecked/>
                </Col>
                <Col sm="auto">
                    <Form.Check onClick={event => setLinkageType(event.target.id)} name="linkageType" label="Single" type="radio" id="single"/>
                </Col>
                <Col sm="auto">
                    <Form.Check onClick={event => setLinkageType(event.target.id)} name="linkageType" label="Complete" type="radio" id="complete"/>
                </Col>
            </Form.Group>
            {takes(SimilaritySetting.MAX_DEPTH) &&
                <Form.Group as={Row} className="align-items-center mb-3">
                    <Form.Label as="legend" column sm={2}>
                        Max Depth
                    </Form.Label>
                    <Col sm={2}>
                        <FormControl
                            type="number"
                            placeholder="1-N"
                            value={depth === -1 ? '' : depth}
                            onChange={event => setDepth(Number(event.target.value))}/>
                    </Col>
                </Form.Group>
            }
            {weightsList.flatMap(weight => Object.entries(weight.weightsLabel).map(([key, value]) =>
                <Form.Group as={Row} key={key} controlId={key} className="align-items-center mb-3">
                    <Form.Label column sm={2}>
                        {value} (%)
                    </Form.Label>
                    <Col sm={2}>
                        <FormControl
                            type="number"
                            placeholder="0-100"
                            value={weight[key] === -1 ? '' : weight[key]}
                            onChange={event => changeMetricWeight(event, key)}/>
                    </Col>
                </Form.Group>)
            )}
            <Form.Group as={Row} className="align-items-center">
                <Col sm={{offset: 2}}>
                    <Button type="submit" disabled={status === UPLOADING || !complete}>
                        Generate Similarity Distances
                    </Button>
                    <Form.Text className="ms-2">
                        {status}
                    </Form.Text>
                </Col>
            </Form.Group>
        </Form>
    );
}
