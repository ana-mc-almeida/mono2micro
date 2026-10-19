package pt.ist.socialsoftware.mono2micro.softwareSystem.domain;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;

import java.util.*;
import java.util.stream.Collectors;

@Document("softwareSystem")
public class SoftwareSystem {
	@Id
	private String id = new ObjectId().toHexString();
	private String name;
	@DBRef(lazy = true)
	private List<Strategy> strategies;

	public SoftwareSystem() {}

	public SoftwareSystem(String name) {
		this.name = name;
		strategies = new ArrayList<>();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Strategy> getStrategies() {
		return strategies;
	}

	public boolean containsStrategyName(String name) {
		return this.strategies.stream().anyMatch(strategy -> strategy.getName().equals(name));
	}

	public void setStrategies(List<Strategy> strategies) {
		this.strategies = strategies;
	}

	public void addStrategy(Strategy strategy) {
		this.strategies.add(strategy);
	}

	public void removeStrategy(String strategyName) {
		this.strategies = this.strategies.stream().filter(strategy -> !strategy.getName().equals(strategyName)).collect(Collectors.toList());
	}
}