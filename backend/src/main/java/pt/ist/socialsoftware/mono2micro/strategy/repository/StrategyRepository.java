package pt.ist.socialsoftware.mono2micro.strategy.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy;

import java.util.List;

public interface StrategyRepository extends MongoRepository<Strategy, String> {
	List<Strategy> findBySoftwareSystemId(String softwareSystemId);
}
