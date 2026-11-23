package pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/** The underscore walks the output reference, which Spring Data turns into a DBRef. */
public interface OperationRepository extends MongoRepository<Operation, String> {
	/** The operation that produced the representation, if one did. */
	Optional<Operation> findByOutput_Id(String representationId);

	boolean existsByOutput_Id(String representationId);

	void deleteByOutput_Id(String representationId);
}
