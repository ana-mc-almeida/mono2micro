import React, {useEffect, useState} from 'react';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Form from 'react-bootstrap/Form';
import Breadcrumb from 'react-bootstrap/Breadcrumb';

import {useParams} from "react-router-dom";
import {APIService} from "../../services/APIService";
import {toast, ToastContainer} from "react-toastify";
import {RecommendMatrixSciPy} from "./forms/RecommendMatrixSciPy";
import {DecompositionFactory} from "../../models/decompositions/DecompositionFactory";

export const Recommendations = () => {
    let { softwareSystemId } = useParams();

    const [softwareSystemName, setSoftwareSystemName] = useState("");
    const [offeredKinds, setOfferedKinds] = useState([]);
    const [selectedKind, setSelectedKind] = useState(undefined);
    const [decompositions, setDecompositions] = useState([]);

    useEffect(() => {
        const service = new APIService();
        service.getSoftwareSystem(softwareSystemId).then(response => setSoftwareSystemName(response.name));
        // Only the kinds the backend offers for this software system's representations (D-013).
        service.getOfferedSimilarityKinds(softwareSystemId).then(setOfferedKinds);
        loadDecompositions();
    }, []);

    function loadDecompositions() {
        const service = new APIService();
        const toastId = toast.loading("Fetching Decompositions...");
        service.getSoftwareSystemDecompositions(
            softwareSystemId
        ).then(response => {
            setDecompositions(response.data.map(decomposition => DecompositionFactory.getDecomposition(decomposition)));
            toast.update(toastId, {type: toast.TYPE.SUCCESS, render: "Decompositions Loaded.", isLoading: false});
            setTimeout(() => {toast.dismiss(toastId)}, 1000);
        }).catch(() => {
            toast.update(toastId, {type: toast.TYPE.ERROR, render: "Error Loading Decompositions.", isLoading: false});
        });
    }

    function handleDeleteDecomposition(decomposition) {
        const decompositionName = decomposition.name;
        const toastId = toast.loading("Deleting " + decompositionName + "...");
        const service = new APIService();
        service.deleteDecomposition(decomposition.id).then(() => {
            loadDecompositions();
            toast.update(toastId, {type: toast.TYPE.SUCCESS, render: "Decomposition deleted.", isLoading: false});
            setTimeout(() => {toast.dismiss(toastId)}, 1000);
        }).catch(() => {
            toast.update(toastId, {type: toast.TYPE.ERROR, render: "Error deleting " + decompositionName + ".", isLoading: false});
        });
    }

    function handleExportDecomposition(decomposition) {
        const decompositionName = decomposition.name;
        const toastId = toast.loading("Exporting " + decompositionName + "...");
        const service = new APIService();
        service.exportDecomposition(decomposition.id).then(response => {
            downloadDecompositionData(response);
            toast.update(toastId, {type: toast.TYPE.SUCCESS, render: "Decomposition exported.", isLoading: false});
            setTimeout(() => {toast.dismiss(toastId)}, 1000);
        }).catch(() => {
            toast.update(toastId, {type: toast.TYPE.ERROR, render: "Error exporting " + decompositionName + ".", isLoading: false});
        });
    }

    function downloadDecompositionData(response) {
        const url = window.URL.createObjectURL(
            new Blob([response.data], {type: "application/zip"})
        );

        const link = document.createElement('a');
        link.href = url;
        link.setAttribute('download', 'm2m_decomposition_data.zip');
        document.body.appendChild(link);
        link.click();

        link.parentNode.removeChild(link);
        URL.revokeObjectURL(url);
    }

    function renderBreadCrumbs() {
        return (
            <Breadcrumb>
                <Breadcrumb.Item href="/">Home</Breadcrumb.Item>
                <Breadcrumb.Item href="/softwareSystems">Software Systems</Breadcrumb.Item>
                <Breadcrumb.Item href={`/softwareSystems/${softwareSystemId}`}>{softwareSystemName}</Breadcrumb.Item>
                <Breadcrumb.Item active>Recommendation</Breadcrumb.Item>
            </Breadcrumb>
        );
    }

    return (
        <div style={{ paddingLeft: "2rem" }}>
            <ToastContainer
                position="top-center"
                theme="colored"
            />

            {renderBreadCrumbs()}

            <Form.Group as={Row} className="align-items-center mb-3">
                <Form.Label as="legend" column sm={2}>
                    Kind
                </Form.Label>
                <Col sm={8} style={{ paddingLeft: 0 }}>
                    {offeredKinds.map(kind =>
                        <Col key={kind} sm="auto">
                            <Form.Check onClick={() => setSelectedKind(kind)} name="kind" label={kind} type="radio" id={kind} value={kind}/>
                        </Col>
                    )}
                </Col>
            </Form.Group>

            {selectedKind !== undefined &&
                <>
                    <RecommendMatrixSciPy
                        key={selectedKind}
                        softwareSystemId={softwareSystemId}
                        kind={selectedKind}
                        onDecompositionsCreated={loadDecompositions}
                    />

                    {decompositions.length !== 0 &&
                        <h4 style={{color: "#666666", marginTop: "16px"}}>
                            Decompositions
                        </h4>
                    }

                    <Row className={"d-flex flex-wrap mw-100"} style={{gap: '1rem 1rem'}}>
                        {decompositions.map(decomposition => decomposition.printCard(loadDecompositions, handleDeleteDecomposition, handleExportDecomposition))}
                    </Row>
                </>
            }
        </div>
    )
}
