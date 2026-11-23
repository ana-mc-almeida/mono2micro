package pt.ist.socialsoftware.mono2micro.similarity.kind;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.representationInformation.AccessesInformation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.representationInformation.RepositoryInformation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.representationInformation.RepresentationInformation;
import pt.ist.socialsoftware.mono2micro.feature.Requirement;
import pt.ist.socialsoftware.mono2micro.feature.Requires;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AuthorRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CommitRepresentation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.AccessesWeights;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.RepositoryWeights;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.Weights;

import java.util.Arrays;
import java.util.List;

import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyAccessesAndRepository.SIMILARITY_SCIPY_ACCESSES_REPOSITORY;

/**
 * The access and repository criteria together, a kind of its own beside Repository alone (D-026). It needs
 * what {@link RepositoryKind} needs, and its Similarity weighs both.
 */
@JsonTypeName(AccessesAndRepositoryKind.ACCESSES_AND_REPOSITORY)
public class AccessesAndRepositoryKind extends SimilarityKind {
	public static final String ACCESSES_AND_REPOSITORY = "Accesses and Repository";

	@Override
	public List<Requirement> requirements() {
		return Arrays.asList(
				Requires.of(AuthorRepresentation.class),
				Requires.of(CommitRepresentation.class));
	}

	@Override
	public String similarityType() {
		return SIMILARITY_SCIPY_ACCESSES_REPOSITORY;
	}

	@Override
	public List<RepresentationInformation> informationBlocks() {
		return Arrays.asList(new AccessesInformation(), new RepositoryInformation());
	}

	@Override
	public List<Weights> defaultWeights() {
		return Arrays.asList(new AccessesWeights(17, 17, 17, 17), new RepositoryWeights(16, 16));
	}

	@Override
	public List<SimilaritySetting> settings() {
		return Arrays.asList(SimilaritySetting.TRACES_MAX_LIMIT, SimilaritySetting.TRACE_TYPE);
	}

	@Override
	public String scopedRepresentationType() {
		return AccessesRepresentation.ACCESSES;
	}
}
