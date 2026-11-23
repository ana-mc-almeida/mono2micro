import React, {useEffect, useState} from 'react';
import { APIService } from '../../services/APIService';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Card from 'react-bootstrap/Card';
import Button from 'react-bootstrap/Button';
import Form from 'react-bootstrap/Form';
import FormControl from 'react-bootstrap/FormControl';
import Breadcrumb from 'react-bootstrap/Breadcrumb';
import {toast, ToastContainer} from "react-toastify";

const HttpStatus = require('http-status-codes');

export const SoftwareSystems = () => {
    const [newSoftwareSystemName, setNewSoftwareSystemName] = useState("");
    const [softwareSystems, setSoftwareSystems] = useState([]);
    const [isCreated, setIsCreated] = useState("");

    //Executed on mount
    useEffect(() => loadSoftwareSystems(), []);

    function loadSoftwareSystems() {
        const service = new APIService();
        service.getSoftwareSystems().then(response => {
            setSoftwareSystems(response);
        });
    }

    function handleChangeNewSoftwareSystemName(event) {
        setNewSoftwareSystemName(event.target.value);
    }

    function handleDeleteSoftwareSystem(softwareSystemId) {
        let toastId = toast.loading("Deleting software system information...", {type: toast.TYPE.INFO});
        const service = new APIService();
        service.deleteSoftwareSystem(softwareSystemId).then(() => {
            loadSoftwareSystems();
            toast.update(toastId, {type: toast.TYPE.SUCCESS, render: "Software System deleted.", isLoading: false});
            setTimeout(() => {toast.dismiss(toastId)}, 1000);
        });
    }

    function renderSoftwareSystems() {
        return (
            <Row>
                {
                    softwareSystems.map(softwareSystem =>
                        <Col key={softwareSystem.id} md="auto">
                            <Card className={"text-center"} style={{ width: '15rem', marginBottom: "16px" }}>
                                <Card.Header>{softwareSystem.name}</Card.Header>
                                <Card.Body>
                                    <Button
                                        href={`/softwareSystems/${softwareSystem.id}`}
                                        className="mb-2"
                                        variant={"primary"}
                                    >
                                        Manage Software System
                                    </Button>
                                    <br/>
                                    <Button
                                        onClick={() => handleDeleteSoftwareSystem(softwareSystem.id)}
                                        variant="danger"
                                    >
                                        Delete
                                    </Button>
                                </Card.Body>
                            </Card>
                        </Col>
                    )
                }
            </Row>
        );
    }

    function handleSubmit() {
        event.preventDefault()

        setIsCreated("Creating...");

        const service = new APIService();
        service.createSoftwareSystem(newSoftwareSystemName)
            .then(response => {
                if (response.status === HttpStatus.CREATED) {
                    loadSoftwareSystems();
                    setIsCreated("Creation completed successfully.");
                } else {
                    setIsCreated("Creation failed.");
                }
            })
            .catch(error => {
                if (error.response !== undefined && error.response.status === HttpStatus.UNAUTHORIZED) {
                    setIsCreated("Creation failed. Software system name already exists.");
                }
                else setIsCreated("Creation failed.");
            });
    }

    function renderCreateSoftwareSystemForm() {
        return (
            <Form onSubmit={handleSubmit}>
                <Form.Group as={Row} controlId="newSoftwareSystemName" className="mb-3 align-items-center">
                    <Form.Label column sm={2}>
                        Software System Name
                    </Form.Label>
                    <Col sm={5}>
                        <FormControl
                            type="text"
                            maxLength="256"
                            placeholder="Software system Name"
                            value={newSoftwareSystemName}
                            onChange={handleChangeNewSoftwareSystemName}
                        />
                    </Col>
                </Form.Group>

                <Form.Group as={Row} className="mb-3">
                    <Col sm={{ span: 5, offset: 2 }}>
                        <Button
                            type="submit"
                            disabled={
                                isCreated === "Creating..." ||
                                newSoftwareSystemName === ""
                            }
                        >
                            Create Software System
                        </Button>
                        <Form.Text className="ms-3">
                            {isCreated}
                        </Form.Text>
                    </Col>
                </Form.Group>
            </Form>
        );
    }


    function renderBreadCrumbs() {
        return (
            <Breadcrumb>
                <Breadcrumb.Item href="/">
                    Home
                </Breadcrumb.Item>
                <Breadcrumb.Item active>
                    Software Systems
                </Breadcrumb.Item>
            </Breadcrumb>
        );
    }


    return (
        <>
            <ToastContainer
                position="top-center"
                theme="colored"
            />

            <div style={{ paddingLeft: "2rem" }}>
                {renderBreadCrumbs()}

                <h4 style={{color: "#666666"}}>
                    Create Software System
                </h4>
                {renderCreateSoftwareSystemForm()}

                <h4 style={{color: "#666666"}}>
                    Software Systems
                </h4>
                {renderSoftwareSystems()}
            </div>
        </>
    );
}
