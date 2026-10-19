package pt.ist.socialsoftware.mono2micro.similarity.domain;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.Clustering;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;

import java.util.Map;

public abstract class Similarity {
	@Id
	protected String id = new ObjectId().toHexString();
	protected String name;
	@DBRef
	private Strategy strategy;

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

	public abstract Clustering getClustering();

	public abstract Map<Short, String> getIDToEntityName() throws Exception;

	public abstract void generate() throws Exception;

	public abstract void removeProperties();

	public abstract boolean equalsDto(SimilarityDto dto);
}
