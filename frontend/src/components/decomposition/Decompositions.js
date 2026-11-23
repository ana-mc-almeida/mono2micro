import React, {useEffect, useState} from 'react';
import { APIService } from '../../services/APIService';
import Row from 'react-bootstrap/Row';
import Breadcrumb from 'react-bootstrap/Breadcrumb';
import {useParams} from "react-router-dom";
import {SciPyDecompositionForm} from "./forms/SciPyDecompositionForm";
import {toast, ToastContainer} from "react-toastify";

/** The form of each algorithm operation, by the operation's name. A new operation needs its form here and nothing else. */
const operationForms = {
    "SciPy Clustering": SciPyDecompositionForm,
};

export const Decompositions = () => {
    let { softwareSystemId, similarityId } = useParams();
    const [softwareSystemName, setSoftwareSystemName] = useState("");
    const [similarity, setSimilarity] = useState({});
    const [decompositions, setDecompositions] = useState([]);
    const [operations, setOperations] = useState([]);

    //Executed on mount
    useEffect(() => {
        const service = new APIService();
        service.getSoftwareSystem(softwareSystemId).then(response => setSoftwareSystemName(response.name));
        service.getSimilarity(similarityId).then(response => {
            setSimilarity(response);
            loadDecompositions();
        });
        service.getAvailableOperationNames(similarityId).then(setOperations);
    }, []);

    function loadDecompositions() {
        const service = new APIService();
        const toastId = toast.loading("Fetching Decompositions...");
        service.getDecompositions(
            similarityId
        ).then(response => {
            setDecompositions(response);
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
        }).catch((error) => {
            if (error.response && error.response.status === 404) {
                toast.update(toastId, {type: toast.TYPE.INFO, render: "Please use the Refactorization Tool first to create the data to export.", isLoading: false})
            } else {
                toast.update(toastId, {type: toast.TYPE.ERROR, render: "Error exporting " + decompositionName + ".", isLoading: false});
            }
            setTimeout(() => {toast.dismiss(toastId)}, 5000);
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
                <Breadcrumb.Item href="/">
                    Home
                </Breadcrumb.Item>
                <Breadcrumb.Item href="/softwareSystems">
                    Software Systems
                </Breadcrumb.Item>
                <Breadcrumb.Item href={`/softwareSystems/${softwareSystemId}`}>
                    {softwareSystemName}
                </Breadcrumb.Item>
                <Breadcrumb.Item active>
                    {similarity.name}
                </Breadcrumb.Item>
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

            <h4 style={{color: "#666666"}}>
                Decomposition Creation Method
            </h4>

            {operations.filter(name => operationForms[name]).map(name => {
                const OperationForm = operationForms[name];
                return <OperationForm key={name} loadDecompositions={loadDecompositions}/>;
            })}

            {decompositions.length !== 0 &&
                <h4 style={{color: "#666666", marginTop: "16px"}}>
                    Decompositions
                </h4>
            }

            <Row className={"d-flex flex-wrap mw-100"} style={{gap: '1rem 1rem'}}>
                {decompositions.map(decomposition => decomposition.printCard(loadDecompositions, handleDeleteDecomposition, handleExportDecomposition))}
            </Row>
        </div>
    );
}