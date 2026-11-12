package pt.ist.socialsoftware.mono2micro.similarity.service;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.decomposition.repository.DecompositionRepository;
import pt.ist.socialsoftware.mono2micro.decomposition.service.DecompositionService;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import pt.ist.socialsoftware.mono2micro.feature.AvailabilityCheck;
import pt.ist.socialsoftware.mono2micro.feature.DerivationRecord;
import pt.ist.socialsoftware.mono2micro.feature.FeatureFamilies;
import pt.ist.socialsoftware.mono2micro.feature.FeatureId;
import pt.ist.socialsoftware.mono2micro.representation.domain.Representation;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;
import pt.ist.socialsoftware.mono2micro.similarity.kind.SimilarityKind;
import pt.ist.socialsoftware.mono2micro.utils.SelfRegistration;
import pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge.Profile;
import pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge.ProfileService;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityFactory;
import pt.ist.socialsoftware.mono2micro.similarity.dto.SimilarityDto;
import pt.ist.socialsoftware.mono2micro.similarity.repository.SimilarityRepository;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SimilarityService {

	@Autowired
	SimilarityRepository similarityRepository;

	@Autowired
	RepresentationRepository representationRepository;

	@Autowired
	DecompositionRepository decompositionRepository;

	@Autowired
	DecompositionService decompositionService;

	@Autowired
	ProfileService profileService;

	@Autowired
	GridFsService gridFsService;

	/** Every Similarity kind, offered or withheld, with what each failed, for the software system's collected representations (D-013). */
	public DerivationRecord getAvailableKinds(String softwareSystemId) {
		return AvailabilityCheck.derive(SelfRegistration.prototypes(SimilarityKind.class).values(),
				FeatureFamilies.scanned().contextFor(representationRepository.findBySoftwareSystemId(softwareSystemId)));
	}

	/**
	 * The representations a Similarity of {@code kind} reads in the software system: one of each representation kind the Similarity kind
	 * requires, its family's shared requirements first (D-016). The order is fixed, because two requests are equal only if they list the same
	 * representation ids in the same order.
	 *
	 * @throws pt.ist.socialsoftware.mono2micro.feature.UnavailableFeatureException
	 * if the software system's representations do not offer the kind
	 */
	public List<Representation> resolveRepresentations(SimilarityKind kind, String softwareSystemId) {
		List<Representation> systemExistingRepresentations = representationRepository.findBySoftwareSystemId(softwareSystemId);
		AvailabilityCheck.requireAvailable(kind, FeatureFamilies.scanned().contextFor(systemExistingRepresentations));
		return AvailabilityCheck.requiredFeatures(kind, FeatureFamilies.scanned()).stream()
				.map(required -> systemExistingRepresentations.stream()
						.filter(representation -> FeatureId.ofFeature(representation).equals(required))
						.findFirst()
						// Unreachable while requireAvailable and requiredFeatures agree: the kind passed the check, so each representation it
						// requires exists. If this fires, the two disagree, which is a bug in feature/, not a bad request.
						.orElseThrow(() -> new IllegalStateException("Bug: the " + SelfRegistration.nameOf(kind) + " kind passed the availability "
								+ "check, but software system " + softwareSystemId + " has no " + required
								+ " representation; requiredFeatures and the check disagree.")))
				.collect(Collectors.toList());
	}

	/** The Similarities created on the software system. */
	public List<Similarity> getSimilarities(String softwareSystemId) {
		return similarityRepository.findByRepresentationsIn(representationRepository.findBySoftwareSystemId(softwareSystemId));
	}

	/** The Similarities of the kind created on the software system, where an equal request finds the one it reuses. */
	public List<Similarity> getSimilaritiesOfKind(String softwareSystemId, String kindName) {
		return getSimilarities(softwareSystemId).stream()
				.filter(similarity -> similarity.getKindName().equals(kindName))
				.collect(Collectors.toList());
	}

	/**
	 * Returns the new Similarity of the requested kind on the software system, or the existing one equal to the request.
	 *
	 * @throws IllegalArgumentException
	 * if no kind has the requested name, or the request's type or weights are not the ones its kind is computed with
	 * @throws pt.ist.socialsoftware.mono2micro.feature.UnavailableFeatureException
	 * if the software system's representations do not offer the requested kind
	 */
	public Similarity createSimilarity(String softwareSystemId, SimilarityDto similarityDto) throws Exception {
		SimilarityKind kind = SimilarityKind.getByName(similarityDto.getKindName());
		if (!kind.similarityType().equals(similarityDto.getType()))
			throw new IllegalArgumentException("The " + similarityDto.getKindName() + " kind is computed by " + kind.similarityType()
					+ ", not " + similarityDto.getType() + ".");
		kind.requireWeights(similarityDto.weightsTypes());
		List<Representation> representations = resolveRepresentations(kind, softwareSystemId);
		similarityDto.setSoftwareSystemId(softwareSystemId);
		similarityDto.setRepresentationIds(representations.stream().map(Representation::getId).collect(Collectors.toList()));
		Profile profile = profileService.getScopingProfile(similarityDto.getProfileId(), softwareSystemId, kind);
		similarityDto.setProfileName(Profile.nameOf(profile)); // the generated name shows the profile
		for (Similarity existing : getSimilaritiesOfKind(softwareSystemId, similarityDto.getKindName()))
			if (existing.equalsDto(similarityDto))
				return existing;
		Similarity similarity = SimilarityFactory.createSimilarity(representations, similarityDto);
		similarity.setProfile(profile);

		similarity.generate();

		similarityRepository.save(similarity);
		return similarity;
	}

	/** Deletes the Similarities that read the representation, with what was clustered from them, as deleting a representation must. */
	public void deleteSimilaritiesReading(String representationId) {
		for (Similarity similarity : similarityRepository.findByRepresentations_Id(representationId))
			deleteSimilarity(similarity);
	}

	public void deleteSingleSimilarity(String similarityId) {
		deleteSimilarity(getSimilarityById(similarityId));
	}

	public void deleteSimilarity(Similarity similarity) {
		for (Decomposition decomposition : decompositionRepository.findBySimilarityId(similarity.getId()))
			decompositionService.deleteDecomposition(decomposition);

		similarity.removeProperties();
		similarityRepository.deleteById(similarity.getId());
	}

	public List<Decomposition> getDecompositions(String similarityId) {
		return decompositionRepository.findBySimilarityId(similarityId);
	}

	public Similarity getSimilarityById(String similarityId) {
		return similarityRepository.getById(similarityId);
	}

	public byte[] getDendrogramImage(String similarityId) throws IOException {
		Similarity similarity = getSimilarityById(similarityId);
		InputStream inputStream = gridFsService.getFile(similarity.getId() + "_image");
		return IOUtils.toByteArray(inputStream);
	}
}