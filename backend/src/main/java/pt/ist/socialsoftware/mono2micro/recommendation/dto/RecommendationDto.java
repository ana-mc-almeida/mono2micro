package pt.ist.socialsoftware.mono2micro.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge.Profile;

import java.util.Collections;
import java.util.List;

/** Its subclasses are JSON subtypes by their {@code @JsonTypeName}, found by the self-registration scan (D-019 point 8). */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
public abstract class RecommendationDto {
	String type;
	String id;
	String softwareSystemId;
	/** The name of the Similarity kind whose weights it explores. */
	String kindName;
	String name;
	boolean isCompleted;
	/** Null for no profile (D-017). */
	String profileId;
	String profileName;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	public String getProfileName() {
		return profileName;
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	/** The types of the weights the request explores, in order, which must be the ones its kind is computed with (D-013). */
	public List<String> weightsTypes() {
		return Collections.emptyList();
	}

	/** Not a setter, so that a request's JSON never binds to it. */
	protected void copyProfile(Profile profile) {
		this.profileId = Profile.idOf(profile);
		this.profileName = Profile.nameOf(profile);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSoftwareSystemId() {
		return softwareSystemId;
	}

	public void setSoftwareSystemId(String softwareSystemId) {
		this.softwareSystemId = softwareSystemId;
	}

	public String getKindName() {
		return kindName;
	}

	public void setKindName(String kindName) {
		this.kindName = kindName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isCompleted() {
		return isCompleted;
	}

	public void setCompleted(boolean completed) {
		isCompleted = completed;
	}
}
