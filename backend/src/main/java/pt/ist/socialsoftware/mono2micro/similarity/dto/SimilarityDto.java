package pt.ist.socialsoftware.mono2micro.similarity.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge.Profile;

import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyClassVectorization.SIMILARITY_SCIPY_CLASS_VECTORIZATION;
import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyEntityVectorization.SIMILARITY_SCIPY_ENTITY_VECTORIZATION;
import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyFunctionalityVectorizationBySequenceOfAccesses.SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES;
import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyAccessesAndRepository.SIMILARITY_SCIPY_ACCESSES_REPOSITORY;
import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyFunctionalityVectorizationByCallGraph.SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_CALLGRAPH;
import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyStructure.SIMILARITY_SCIPY_STRUCTURE;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = SimilarityScipyAccessesAndRepositoryDto.class, name = SIMILARITY_SCIPY_ACCESSES_REPOSITORY),
		@JsonSubTypes.Type(value = SimilarityScipyClassVectorizationDto.class, name = SIMILARITY_SCIPY_CLASS_VECTORIZATION),
		@JsonSubTypes.Type(value = SimilarityScipyEntityVectorizationDto.class, name = SIMILARITY_SCIPY_ENTITY_VECTORIZATION),
		@JsonSubTypes.Type(value = SimilarityScipyFunctionalityVectorizationByCallGraphDto.class, name = SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_CALLGRAPH),
		@JsonSubTypes.Type(value = SimilarityScipyFunctionalityVectorizationBySequenceOfAccessesDto.class, name = SIMILARITY_SCIPY_FUNCTIONALITY_VECTORIZATION_SEQUENCE_ACCESSES),
		@JsonSubTypes.Type(value = SimilarityScipyStructureDto.class, name = SIMILARITY_SCIPY_STRUCTURE)
})
public abstract class SimilarityDto {
	String id;
	String softwareSystemId;
	String strategyId;
	String strategyName;
	String name;
	String type;
	/** Null for no profile: the Similarity is built on every unit (D-017). */
	String profileId;
	String profileName;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStrategyName() {
		return strategyName;
	}

	public void setStrategyName(String strategyName) {
		this.strategyName = strategyName;
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

	/**
	 * The representation kind whose units a profile of this Similarity selects, or null when the kind takes no profile.
	 * Not a getter, so that it never appears in the JSON.
	 */
	public String scopedRepresentationType() {
		return null;
	}

	/** How the generated name shows the profile. */
	protected String profileLabel() {
		return profileName == null ? "No profile" : profileName;
	}

	public String getSoftwareSystemId() {
		return softwareSystemId;
	}

	public void setSoftwareSystemId(String softwareSystemId) {
		this.softwareSystemId = softwareSystemId;
	}
}
