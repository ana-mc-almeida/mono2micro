package pt.ist.socialsoftware.mono2micro.recommendation.domain;

import org.bson.types.ObjectId;
import pt.ist.socialsoftware.mono2micro.softwareSystem.domain.SoftwareSystem;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import pt.ist.socialsoftware.mono2micro.recommendation.dto.RecommendationDto;
import pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge.Profile;
import pt.ist.socialsoftware.mono2micro.recommendation.repository.RecommendationRepository;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;

import java.util.List;
import java.util.stream.Collectors;

public abstract class Recommendation {
	@Id
	String id = new ObjectId().toHexString();
	String name;
	@DBRef(lazy = true)
	SoftwareSystem softwareSystem;
	/** The name of the Similarity kind whose weights it explores. */
	String kindName;
	/** The collected representations its Similarities read, which are those its kind requires. */
	@DBRef
	List<Representation> representations;
	/** The profile that scopes the recommended Similarities, or null for none (D-017). */
	@DBRef(lazy = true)
	Profile profile;
	String recommendationResultName;
	boolean isCompleted; // true when all the decompositions are calculated

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public SoftwareSystem getSoftwareSystem() {
		return softwareSystem;
	}

	public void setSoftwareSystem(SoftwareSystem softwareSystem) {
		this.softwareSystem = softwareSystem;
	}

	public String getKindName() {
		return kindName;
	}

	public void setKindName(String kindName) {
		this.kindName = kindName;
	}

	public List<Representation> getRepresentations() {
		return representations;
	}

	public void setRepresentations(List<Representation> representations) {
		this.representations = representations;
	}

	public List<String> getRepresentationIds() {
		return representations.stream().map(Representation::getId).collect(Collectors.toList());
	}

	public Profile getProfile() {
		return profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}

	public String getProfileId() {
		return Profile.idOf(profile);
	}

	public abstract String getType();

	public abstract void deleteProperties();

	public abstract boolean equalsDto(RecommendationDto dto);

	public String getRecommendationResultName() {
		return recommendationResultName;
	}

	public void setRecommendationResultName(String recommendationResultName) {
		this.recommendationResultName = recommendationResultName;
	}

	public boolean isCompleted() {
		return isCompleted;
	}

	public void setCompleted(boolean completed) {
		isCompleted = completed;
	}

	public abstract void generateRecommendation(RecommendationRepository recommendationRepository);

	public abstract void createDecompositions(List<Similarity> similarities, List<String> decompositionNames) throws Exception;
}
