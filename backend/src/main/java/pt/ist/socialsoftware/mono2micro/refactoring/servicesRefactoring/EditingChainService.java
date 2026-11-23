package pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.repository.ClusterRepresentationRepository;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.service.ClusterRepresentationService;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;

import java.util.Set;

/**
 * Runs operations: it applies an operation to its input, saves the output and links it on the operation (D-012, D-016, D-018). Manual operations
 * and undo still go through {@code History} until tickets 11 and 12.
 */
@Service
public class EditingChainService {

	@Autowired
	OperationRepository operationRepository;

	@Autowired
	ClusterRepresentationRepository clusterRepresentationRepository;

	@Autowired
	ClusterRepresentationService clusterRepresentationService;

	/**
	 * Applies {@code operation} to {@code input}, then sets up and saves its output under a name its Similarity's other cluster representations do
	 * not use, and saves the operation linked to it.
	 */
	public ClusterRepresentation run(AlgorithmOperation operation, Similarity input) throws Exception {
		ClusterRepresentation output = operation.apply(input);
		output.setName(uniqueName(output.getName(), clusterRepresentationRepository.findNamesBySimilarityId(input.getId())));
		clusterRepresentationService.setupDecomposition(output);

		operation.setSimilarity(input);
		operation.setOutput(output);
		operationRepository.save(operation);
		return output;
	}

	/** Whether the representation was collected: no operation has it as output (D-016). Otherwise an operation produced it. */
	public boolean isCollected(Representation representation) {
		return !operationRepository.existsByOutput_Id(representation.getId());
	}

	/** {@code name}, or the first of "name(2)", "name(3)", … that is not among {@code taken}. */
	static String uniqueName(String name, Set<String> taken) {
		if (!taken.contains(name))
			return name;
		int i = 2;
		while (taken.contains(name + "(" + i + ")"))
			i++;
		return name + "(" + i + ")";
	}
}
