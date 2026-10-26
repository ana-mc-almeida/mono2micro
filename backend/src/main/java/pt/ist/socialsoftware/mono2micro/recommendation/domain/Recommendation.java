package pt.ist.socialsoftware.mono2micro.recommendation.domain;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import pt.ist.socialsoftware.mono2micro.recommendation.dto.RecommendationDto;
import pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge.Profile;
import pt.ist.socialsoftware.mono2micro.recommendation.repository.RecommendationRepository;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;

import java.util.List;

public abstract class Recommendation {
	@Id
	String id = new ObjectId().toHexString();
	String name;
	@DBRef(lazy = true)
	Strategy strategy;
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

	public Strategy getStrategy() {
		return strategy;
	}

	public void setStrategy(Strategy strategy) {
		this.strategy = strategy;
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

	public abstract void createDecompositions(List<Similarity> strategySimilarities, List<String> decompositionNames) throws Exception;
}
