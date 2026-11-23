import React, {useEffect, useState} from 'react';
import { APIService } from '../../services/APIService';
import Row from 'react-bootstrap/Row';
import Col from 'react-bootstrap/Col';
import Form from 'react-bootstrap/Form';
import FormControl from 'react-bootstrap/FormControl';
import ButtonToolbar from 'react-bootstrap/ButtonToolbar';
import Button from 'react-bootstrap/Button';
import ButtonGroup from 'react-bootstrap/ButtonGroup';
import Breadcrumb from 'react-bootstrap/Breadcrumb';
import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';
import {useParams} from "react-router-dom";

const HttpStatus = require('http-status-codes');

export const Profiles = () => {
    let { softwareSystemId, representationId } = useParams();
    const [softwareSystemName, setSoftwareSystemName] = useState("");
    const [representation, setRepresentation] = useState({});
    const [profiles, setProfiles] = useState(undefined);
    const [newProfileName, setNewProfileName] = useState("");
    const [moveTo, setMoveTo] = useState(undefined); // a profile, or null for Unassigned
    const [selectedUnits, setSelectedUnits] = useState([]);
    const [isUploaded, setIsUploaded] = useState("");

    useEffect(() => {
        const service = new APIService();
        service.getSoftwareSystem(softwareSystemId).then(response => setSoftwareSystemName(response.name));
        service.getRepresentation(representationId).then(response => {
            setRepresentation(response === null ? {} : response);
        });
        loadProfiles();
    }, []);

    function loadProfiles() {
        const service = new APIService();
        service.getProfiles(representationId).then(response => setProfiles(response.data));
    }

    function handleChangeNewProfileName(event) {
        setNewProfileName(event.target.value);
    }

    function handleNewProfileSubmit(event) {
        event.preventDefault();

        setIsUploaded("Uploading...");

        const service = new APIService();
        service.addProfile(representationId, newProfileName).then(response => {
            if (response.status === HttpStatus.CREATED) {
                loadProfiles();
                setIsUploaded("Upload completed successfully.");
            } else {
                setIsUploaded("Upload failed.");
            }
        })
        .catch(error => {
            if (error.response?.status === HttpStatus.UNAUTHORIZED) {
                setIsUploaded("Upload failed. Profile name already exists.");
            } else {
                setIsUploaded("Upload failed.");
            }
        });
    }

    // A move saves new versions of the profiles it changes, so the selected target is cleared: its id is gone.
    function handleMoveUnitsSubmit() {
        const service = new APIService();

        service.moveUnits(
            representationId,
            selectedUnits,
            moveTo === null ? null : moveTo.id
        ).then(() => {
            setSelectedUnits([]);
            setMoveTo(undefined);
            loadProfiles();
        });
    }

    function handleDeleteProfile(profile) {
        const service = new APIService();

        service.deleteProfile(profile.id).then(() => {
            setMoveTo(undefined);
            loadProfiles();
        });
    }

    function handleSelectUnit(event) {
        const eventTargetId = event.target.id;

        if (selectedUnits.includes(eventTargetId)) {
            setSelectedUnits(selectedUnits.filter(c => c !== eventTargetId));
            return;
        }

        setSelectedUnits([...selectedUnits, eventTargetId]);
    }

    const renderUnits = (unitNames) => unitNames.map(unit =>
        <Form.Check
            id={unit}
            key={unit}
            type="checkbox"
            label={unit}
            checked={selectedUnits.includes(unit)}
            onChange={handleSelectUnit}
            style={{ paddingLeft: "3em" }}
        />
    );

    const renderBreadCrumbs = () => {
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
                <Breadcrumb.Item href={`/softwareSystems/${softwareSystemId}`}>
                    {representation.name}
                </Breadcrumb.Item>
                <Breadcrumb.Item active>
                    Profiles
                </Breadcrumb.Item>
            </Breadcrumb>
        );
    }

    return (
        <div style={{ paddingLeft: "2rem" }}>
            {renderBreadCrumbs()}
            <h4 style={{color: "#666666"}}>
                Create Profile
            </h4>

            <Form onSubmit={handleNewProfileSubmit}>
                <Form.Group className="mb-3" as={Row} controlId="newProfileName">
                    <Form.Label column sm={2}>
                        Profile Name
                    </Form.Label>
                    <Col sm={5}>
                        <FormControl
                            type="text"
                            maxLength="30"
                            placeholder="Profile Name"
                            value={newProfileName}
                            onChange={handleChangeNewProfileName}
                        />
                    </Col>
                </Form.Group>

                <Form.Group as={Row}>
                    <Col sm={{ span: 5, offset: 2 }}>
                        <Button
                            type="submit"
                            disabled={isUploaded === "Uploading..." || newProfileName === ""}
                        >
                            Create Profile
                        </Button>
                        <Form.Text className={"ms-2"}>
                            {isUploaded}
                        </Form.Text>
                    </Col>
                </Form.Group>
            </Form>

            <h4 style={{color: "#666666"}}>
                Profiles
            </h4>
            {profiles !== undefined &&
                <div>
                    <ButtonToolbar>
                        <Button className="me-1">Move selected units to</Button>

                        <DropdownButton
                            as={ButtonGroup}
                            title={moveTo === undefined ? "Profile" : moveTo === null ? "Unassigned" : moveTo.name}
                            className="me-1"
                        >
                            {profiles.profiles.map(profile =>
                                <Dropdown.Item
                                    key={profile.id}
                                    onClick={() => setMoveTo(profile)}
                                >
                                        {profile.name}
                                </Dropdown.Item>
                            )}
                            <Dropdown.Divider />
                            <Dropdown.Item onClick={() => setMoveTo(null)}>
                                Unassigned
                            </Dropdown.Item>
                        </DropdownButton>

                        <Button
                            disabled={selectedUnits.length === 0 || moveTo === undefined}
                            onClick={handleMoveUnitsSubmit}
                        >
                            Submit
                        </Button>
                    </ButtonToolbar>

                    {profiles.profiles.map(profile =>
                        <div key={profile.id}>
                            <div style={{fontSize: '25px'}}>
                                {profile.name}
                                <Button
                                    onClick={() => handleDeleteProfile(profile)}
                                    className="ms-2"
                                    variant="danger"
                                    size="sm"
                                >
                                    -
                                </Button>
                            </div>

                            {renderUnits(profile.units)}
                        </div>
                    )}

                    <div style={{fontSize: '25px'}}>
                        Unassigned
                    </div>
                    <Form.Text className="ms-2">
                        The units no profile selects. This list is computed and cannot be edited or chosen for a Similarity.
                    </Form.Text>
                    {renderUnits(profiles.unassigned)}
                </div>
            }
        </div>
    );
}