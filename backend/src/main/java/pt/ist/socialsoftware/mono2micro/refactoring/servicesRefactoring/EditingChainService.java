package pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityCheck;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityContext;
import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord;
import pt.ist.socialsoftware.mono2micro.feature.FeatureFamilies;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.repository.ClusterRepresentationRepository;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.service.ClusterRepresentationService;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.repository.SimilarityRepository;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;

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
	SimilarityRepository similarityRepository;

	@Autowired
	RepresentationRepository representationRepository;

	@Autowired
	ClusterRepresentationRepository clusterRepresentationRepository;

	@Autowired
	ClusterRepresentationService clusterRepresentationService;

	/** Every algorithm operation, offered or withheld for the Similarity, with what each failed: the derivation record (D-013). */
	public DerivationRecord getAvailableAlgorithmOperations(String similarityId) {
		return AvailabilityCheck.derive(SelfRegistration.prototypes(AlgorithmOperation.class).values(),
				contextOf(similarityRepository.getById(similarityId)));
	}

	/**
	 * Runs {@code operation} on the Similarity and returns its saved output.
	 *
	 * @throws pt.ist.socialsoftware.mono2micro.feature.UnavailableFeatureException
	 * if the operation is not available for the Similarity, before anything is computed (D-003)
	 */
	public ClusterRepresentation runAlgorithmOperation(String similarityId, AlgorithmOperation operation) throws Exception {
		Similarity similarity = similarityRepository.getById(similarityId);
		AvailabilityCheck.requireAvailable(operation, contextOf(similarity));
		return run(operation, similarity);
	}

	/**
	 * Applies {@code operation} to {@code input}, then sets up and saves its output under a name its Similarity's other cluster representations do
	 * not use, and saves the operation linked to it. The caller has checked the operation is available.
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

	/** The context of the Similarity's software system, with the Similarity as input. */
	private AvailabilityContext contextOf(Similarity similarity) {
		return FeatureFamilies.scanned().contextFor(representationRepository.findBySoftwareSystemId(similarity.getSoftwareSystem().getId()))
				.withInput(similarity);
	}
}
