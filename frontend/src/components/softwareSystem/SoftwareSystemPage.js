import React, {useEffect, useState} from 'react';
import Breadcrumb from 'react-bootstrap/Breadcrumb';
import Button from 'react-bootstrap/Button';
import {useParams} from "react-router-dom";
import Form from "react-bootstrap/Form";
import Row from "react-bootstrap/Row";
import Col from "react-bootstrap/Col";
import DropdownButton from "react-bootstrap/DropdownButton";
import Dropdown from "react-bootstrap/Dropdown";
import OverlayTrigger from "react-bootstrap/OverlayTrigger";
import Popover from "react-bootstrap/Popover";
import {APIService} from "../../services/APIService";
import HttpStatus from "http-status-codes";
import {Modal, ModalBody, ModalFooter, ModalTitle} from "react-bootstrap";
import {toast} from "react-toastify";
import FormControl from "react-bootstrap/FormControl";
import {AddCircleOutline, CheckCircle} from "@mui/icons-material";
import MUIButton from "@mui/material/Button";
import Card from "react-bootstrap/Card";
import {similarityForms} from "../similarity/forms/similarityForms";
import {creationFailure, UPLOADING} from "../similarity/forms/similarityCreationStatus";

function renderBreadCrumbs(softwareSystemName) {
    return (
        <Breadcrumb>
            <Breadcrumb.Item href="/">
                Home
            </Breadcrumb.Item>
            <Breadcrumb.Item href="/softwareSystems">
                Software Systems
            </Breadcrumb.Item>
            <Breadcrumb.Item active>
                {softwareSystemName}
            </Breadcrumb.Item>
        </Breadcrumb>
    );
}

export function SoftwareSystemPage() {
    let { softwareSystemId } = useParams();
    const [softwareSystemName, setSoftwareSystemName] = useState("");
    const [showPopup, setShowPopup] = useState(false);
    const [representationToDelete, setRepresentationToDelete] = useState(undefined);
    const [representations, setRepresentations] = useState([]);
    const [requiredRepresentations, setRequiredRepresentations] = useState([]);
    const [obtainedRepresentations, setObtainedRepresentations] = useState([]);
    const [selectedRepresentationType, setSelectedRepresentationType] = useState(undefined);
    const [addedRepresentations, setAddedRepresentations] = useState({});
    const [isUploaded, setIsUploaded] = useState("");
    const [canSubmit, setCanSubmit] = useState(false);
    const [add, setAdd] = useState("");
    const [representationGroups, setRepresentationGroups] = useState([]);
    const [softwareSystemRepresentationGroups, setSoftwareSystemRepresentationGroups] = useState([]);
    const [similarities, setSimilarities] = useState([]);
    const [offeredKinds, setOfferedKinds] = useState([]);
    const [profiledRepresentationTypes, setProfiledRepresentationTypes] = useState([]);
    const [selectedKind, setSelectedKind] = useState(undefined);
    const [similarityStatus, setSimilarityStatus] = useState("");

    //Executed on mount
    useEffect(() => {
        const service = new APIService();
        service.getSoftwareSystem(softwareSystemId).then(response => setSoftwareSystemName(response.name));
        loadRepresentations();
        loadSoftwareSystemRepresentationGroups();
        service.getRepresentationGroups().then(response => setRepresentationGroups(response.data));
    }, []);

    function loadSoftwareSystemRepresentationGroups() {
        const service = new APIService();
        service.getSoftwareSystemRepresentationGroups(softwareSystemId).then(response => setSoftwareSystemRepresentationGroups(response.data));
    }

    // The Similarities, the offered kinds and the cards that link to profiles follow the representations, so they reload with them.
    function loadRepresentations() {
        const service = new APIService();
        service.getRepresentations(softwareSystemId).then(response => {
            setRepresentations(response);
            setObtainedRepresentations(response.map(representation => representation.type));
        });
        service.getProfiledRepresentationTypes(softwareSystemId).then(response => setProfiledRepresentationTypes(response));
        loadSimilarities();
        // Only the kinds the backend offers for this software system's representations (D-013).
        service.getOfferedSimilarityKinds(softwareSystemId).then(setOfferedKinds);
    }

    function loadSimilarities() {
        const service = new APIService();
        service.getSimilarities(softwareSystemId).then(response => setSimilarities(response));
    }

    function handleDeleteSimilarity(similarity) {
        let toastId = toast.loading("Deleting " + similarity.name + "...", {type: toast.TYPE.INFO});
        const service = new APIService();
        service.deleteSimilarity(similarity.id).then(() => {
            loadSimilarities();
            toast.update(toastId, {type: toast.TYPE.SUCCESS, render: "Similarity deleted.", isLoading: false});
            setTimeout(() => {toast.dismiss(toastId)}, 1000);
        }).catch(() => {
            toast.update(toastId, {type: toast.TYPE.ERROR, render: "Error deleting " + similarity.name + ".", isLoading: false});
        });
    }

    // The one place a Similarity is created: the chosen kind's form only collects its settings into the request.
    function handleCreateSimilarity(request) {
        setSimilarityStatus(UPLOADING);
        new APIService().createSimilarity(softwareSystemId, request)
            .then(() => {
                setSimilarityStatus("");
                loadSimilarities();
            })
            .catch(error => setSimilarityStatus(creationFailure(error)));
    }

    function handleSelectKind(kind) {
        setSelectedKind(kind);
        setSimilarityStatus("");
    }

    // The kinds the software system offers, and the form of the chosen one: one lookup from the kind's name.
    function renderSimilarities() {
        const SimilarityForm = selectedKind !== undefined ? similarityForms[selectedKind] : undefined;
        return (
            <>
                <h4 className="mt-4" style={{color: "#666666"}}>
                    Similarities
                    <Button className="ms-3" size="sm" href={`/softwareSystems/${softwareSystemId}/recommendation`}>
                        Recommendation
                    </Button>
                </h4>
                {offeredKinds.length === 0 ?
                    <p>Upload representations to be offered a Similarity kind.</p> :
                    <Form.Group as={Row} className="align-items-center mb-3">
                        <Form.Label as="legend" column sm={2}>
                            Kind
                        </Form.Label>
                        <Col sm={10}>
                            <Row>
                                {offeredKinds.map(kind =>
                                    <Col key={kind} sm="auto">
                                        <Form.Check onClick={() => handleSelectKind(kind)} name="similarityKind" label={kind} type="radio" id={"kind-" + kind}/>
                                    </Col>
                                )}
                            </Row>
                        </Col>
                    </Form.Group>
                }
                {SimilarityForm !== undefined &&
                    <SimilarityForm
                        key={selectedKind}
                        softwareSystemId={softwareSystemId}
                        kindName={selectedKind}
                        status={similarityStatus}
                        onSubmit={handleCreateSimilarity}
                    />
                }
                <div className={"d-flex flex-wrap mw-100 mt-3"} style={{gap: '1rem 1rem'}}>
                    {similarities.map(similarity => similarity.printCard(handleDeleteSimilarity))}
                </div>
            </>
        );
    }

    function handleRepresentationsSubmit(event) {
        event.preventDefault()

        setIsUploaded("Uploading...");

        const service = new APIService();
        service.addRepresentations(softwareSystemId, selectedRepresentationType, addedRepresentations)
            .then(response => {
                if (response.status === HttpStatus.CREATED) {
                    loadSoftwareSystemRepresentationGroups();
                    loadRepresentations();
                    setIsUploaded("");
                } else setIsUploaded("Upload failed.");

                setSelectedRepresentationType(undefined);
                setRequiredRepresentations([]);
                setAddedRepresentations({});
                setAdd("");
            })
            .catch(error => {
                if (error.response !== undefined && error.response.status === HttpStatus.UNAUTHORIZED) {
                    setIsUploaded("Upload failed. These representations were already submitted.");
                }
                else if (error.response !== undefined && error.response.status === HttpStatus.NOT_FOUND) {
                    setIsUploaded("Upload failed. Invalid datafile path.");
                }
                else {
                    setIsUploaded("Upload failed.");
                }
            });
        setCanSubmit(false);
    }

    const getHelpText = (
        <Popover id="helpPopover">
            <div> Select the decomposition type and add the required files, then proceed to the similarities or customize the representations</div>
        </Popover>
    );

    function handleSelectedRepresentation(representationType) {
        setSelectedRepresentationType(representationType);
        setRequiredRepresentations(representationGroups[representationType]);
        let representationTypes = representations.map(rep => rep.type);
        setCanSubmit(representationGroups[representationType].reduce((p, c) => {return p? representationTypes.includes(c) : false}, true));
    }

    function addRepresentation(representation, event) {
        setAddedRepresentations(prev => {
            prev[representation] = event.target.files[0];
            let totalRepresentations = [...representations.map(r => r.type), ...Object.keys(prev)];
            if (requiredRepresentations.reduce((p, c) => {return p? totalRepresentations.includes(c) : false}, true))
                setCanSubmit(true);
            return prev;
        });
    }

    function handleDeleteRepresentation(representation) {
        setRepresentationToDelete(representation);
        setShowPopup(true);
    }

    function confirmRepresentationToDelete() {
        setShowPopup(false);

        let toastId = toast.loading("Deleting representation...", {type: toast.TYPE.INFO});
        const service = new APIService();
        service.deleteRepresentation(representationToDelete.id).then(() => {
            loadSoftwareSystemRepresentationGroups();
            loadRepresentations();
            toast.dismiss(toastId);
        });
    }

    function renderRepresentations() {
        return (
            <Form onSubmit={handleRepresentationsSubmit}>
                <Form.Group as={Row} controlId="selectRepresentationInfo" className="align-items-center mb-3">
                    <Col sm={2}>
                        <DropdownButton title={selectedRepresentationType === undefined? "Choose Representation Type" : selectedRepresentationType}>
                            {representationGroups !== undefined && Object.keys(representationGroups).filter(representationType => !softwareSystemRepresentationGroups.includes(representationType))
                                .map(representationType =>
                                    <Dropdown.Item
                                        key={representationType}
                                        onClick={() => handleSelectedRepresentation(representationType)}
                                    >
                                        {representationType}
                                    </Dropdown.Item>
                            )}
                        </DropdownButton>
                    </Col>
                </Form.Group>
                {requiredRepresentations.map(representationType =>
                    <React.Fragment key={representationType}>
                        {!obtainedRepresentations.includes(representationType) &&
                            <Form.Group as={Row} className="mb-3 mt-2 align-items-center">
                                <Form.Label column sm={2}>
                                    {representationType + " File"}
                                </Form.Label>
                                <Col sm={5}>
                                    <FormControl
                                        type="file"
                                        onChange={event => addRepresentation(representationType, event)}
                                    />
                                </Col>
                            </Form.Group>
                        }
                        {obtainedRepresentations.includes(representationType) &&
                            <Form.Group as={Row} className="mb-3 mt-2 align-items-center">
                                <Form.Label column sm={2}>
                                    {representationType + " File"}
                                </Form.Label>
                                <Col sm={5}>
                                    <MUIButton variant="contained" disableRipple={true} disableElevation={true} color="success" sx={{ bgcolor: "success.dark" }} endIcon={<CheckCircle/>}>
                                        Already Added
                                    </MUIButton>
                                </Col>
                            </Form.Group>
                        }
                    </React.Fragment>
                )}

                {selectedRepresentationType !== undefined &&
                    <Form.Group as={Row} className="align-items-center mb-4">
                        <Col sm={{offset: 2}}>
                            <Button type="submit" disabled={!canSubmit}>Submit</Button>
                            <Form.Text className="ms-2">
                                {isUploaded}
                            </Form.Text>
                        </Col>
                    </Form.Group>
                }
            </Form>
        );
    }

    function addCard(add) {
        return (
            <Card className={"text-center"} key="add" style={{cursor: "pointer", borderStyle: "dashed", borderWidth: "thick", backgroundColor: "#00000000", width: '13rem'}} onClick={() => setAdd(add)}>
                <div style={{ position: "relative", marginTop: "50%", marginBottom: "40%", marginLeft: "50%", transform: "translate(-50%,-50%)" }}>
                    <AddCircleOutline style={{transform: "scale(3)", color: "#00000022" }}/>
                </div>
            </Card>
        );
    }

    function renderDeleteModal() {
        const closePopup = function () {setShowPopup(false); setRepresentationToDelete(undefined)};

        return <Modal
                show={showPopup}
                onHide={() => closePopup()}
                backdrop="static"
            >
                <ModalTitle>&ensp;Representation Deletion</ModalTitle>
                <ModalBody>
                    Similarities that read this representation will be deleted.<br/>Are you sure you want to delete this Representation?
                </ModalBody>
                <ModalFooter>
                    <Button variant="secondary" onClick={() => closePopup()}>
                        Close
                    </Button>
                    <Button variant="primary" onClick={() => confirmRepresentationToDelete()}>Delete</Button>
                </ModalFooter>
            </Modal>;
    }

    function renderAddModal() {
        const closeAddPopup = function () {
            setAdd("");
        };

        return <Modal
            show={add !== ""}
            onHide={() => closeAddPopup()}
            backdrop="static"
            size='lg'
        >
            <ModalTitle>&ensp;Add {add}</ModalTitle>
            <ModalBody>
                {add === "Representation" && renderRepresentations()}
            </ModalBody>
            <ModalFooter>
                <Button variant="secondary" onClick={() => closeAddPopup()}>
                    Close
                </Button>
            </ModalFooter>
        </Modal>;
    }

    return (
        <div style={{ paddingLeft: "2rem" }}>
            { renderDeleteModal() }

            { renderAddModal() }

            <Row className="mt-4">
                <Col>
                    { renderBreadCrumbs(softwareSystemName) }
                </Col>
                <Col className="me-5">
                    <OverlayTrigger trigger="click" placement="left" overlay={getHelpText}>
                        <Button className="float-end" variant="success">Help</Button>
                    </OverlayTrigger>
                </Col>
            </Row>

            <h4 className="mt-4" style={{ color: "#666666" }}>
                Representation Files
            </h4>
            <div className={"d-flex flex-wrap mw-100"} style={{gap: '1rem 1rem'}}>
                {[...representations.map(representation => representation.printCard(handleDeleteRepresentation, profiledRepresentationTypes.includes(representation.type))), addCard("Representation")]}
            </div>

            { renderSimilarities() }
        </div>
    );
}