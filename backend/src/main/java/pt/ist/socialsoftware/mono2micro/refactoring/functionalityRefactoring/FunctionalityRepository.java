package pt.ist.socialsoftware.mono2micro.refactoring.functionalityRefactoring;

import org.springframework.data.mongodb.repository.MongoRepository;
import pt.ist.socialsoftware.mono2micro.refactoring.functionalityRefactoring.domain.Functionality;

public interface FunctionalityRepository extends MongoRepository<Functionality, String> {
}
