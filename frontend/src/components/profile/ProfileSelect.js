import React, {useEffect, useState} from "react";
import Form from "react-bootstrap/Form";
import Row from "react-bootstrap/Row";
import Col from "react-bootstrap/Col";
import DropdownButton from "react-bootstrap/DropdownButton";
import Dropdown from "react-bootstrap/Dropdown";
import {APIService} from "../../services/APIService";

/**
 * Chooses the profile that scopes a Similarity or a recommendation. The default, a null profileId, is no profile: every
 * unit (D-017). Only offered profiles are listed; Unassigned is not a profile and cannot be chosen. It shows only when the
 * backend offers a profile for the kind (the Profile feature's derivation record), and otherwise keeps no profile. Which
 * representation's profiles it lists is the backend's call, from the kind.
 */
export const ProfileSelect = ({softwareSystemId, kindName, profileId, setProfileId}) => {
    const [profiles, setProfiles] = useState([]);
    const [offered, setOffered] = useState(false);
    const noProfile = "No profile (all units)";

    useEffect(() => {
        const service = new APIService();
        service.isProfileOffered(softwareSystemId, kindName)
            .then(isOffered => {
                setOffered(isOffered);
                if (!isOffered) {
                    setProfileId(null);
                    return [];
                }
                return service.getOfferedProfiles(softwareSystemId, kindName);
            })
            .then(offeredProfiles => setProfiles(offeredProfiles))
            .catch(() => {
                setOffered(false);
                setProfiles([]);
            });
    }, [softwareSystemId, kindName]);

    const selected = profiles.find(profile => profile.id === profileId);

    if (!offered)
        return null;

    return (
        <Form.Group as={Row} controlId="selectProfile" className="align-items-center mb-3">
            <Form.Label column sm={2}>
                Profile
            </Form.Label>
            <Col sm={2}>
                <DropdownButton title={selected === undefined ? noProfile : selected.name}>
                    <Dropdown.Item onClick={() => setProfileId(null)} active={profileId === null}>
                        {noProfile}
                    </Dropdown.Item>
                    {profiles.map(profile =>
                        <Dropdown.Item
                            key={profile.id}
                            onClick={() => setProfileId(profile.id)}
                            active={profile.id === profileId}
                        >
                            {profile.name}
                        </Dropdown.Item>
                    )}
                </DropdownButton>
            </Col>
        </Form.Group>
    );
}
