package pt.ist.socialsoftware.mono2micro.similarity.domain;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.Clustering;
import pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge.Profile;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;

import java.util.Map;

public abstract class Similarity {
	@Id
	protected String id = new ObjectId().toHexString();
	protected String name;
	@DBRef(lazy = true)
	private Strategy strategy;
	/** The profile that scopes this Similarity, or null for none: then it is built on every unit (D-017). */
	@DBRef(lazy = true)
	private Profile profile;

	public Similarity() {}

	public Similarity(Strategy strategy, String name) {
		this.strategy = strategy;
		this.name = name;
	}

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

	public abstract String getType();

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
		return profile == null ? null : profile.getId();
	}

	public abstract Clustering getClustering();

	public abstract Map<Short, String> getIDToEntityName() throws Exception;

	public abstract void generate() throws Exception;

	public abstract void removeProperties();

	public abstract boolean equalsDto(SimilarityDto dto);
}
