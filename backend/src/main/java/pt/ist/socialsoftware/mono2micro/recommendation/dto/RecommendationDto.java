package pt.ist.socialsoftware.mono2micro.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge.Profile;

import static pt.ist.socialsoftware.mono2micro.recommendation.domain.RecommendationsType.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
		@JsonSubTypes.Type(value = RecommendMatrixSciPyDto.class, name = RECOMMEND_MATRIX_SCIPY_STRUCTURE),
		@JsonSubTypes.Type(value = RecommendMatrixSciPyDto.class, name = RECOMMEND_MATRIX_SCIPY),
		@JsonSubTypes.Type(value = RecommendMatrixSciPyDto.class, name = RECOMMEND_MATRIX_CLASS_VECTORIZATION),
		@JsonSubTypes.Type(value = RecommendMatrixSciPyDto.class, name = RECOMMEND_MATRIX_ENTITY_VECTORIZATION),
		@JsonSubTypes.Type(value = RecommendMatrixSciPyDto.class, name = RECOMMEND_MATRIX_FUNCTIONALITY_VECTORIZATION_CALLGRAPH),
		@JsonSubTypes.Type(value = RecommendMatrixSciPyDto.class, name = RECOMMEND_MATRIX_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES)
})
public abstract class RecommendationDto {
	String type;
	String id;
	String strategyId;
	String strategyName;
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

	public String getStrategyId() {
		return strategyId;
	}

	public void setStrategyId(String strategyId) {
		this.strategyId = strategyId;
	}

	public String getStrategyName() {
		return strategyName;
	}

	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
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
