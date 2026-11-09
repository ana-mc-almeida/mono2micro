package pt.ist.socialsoftware.mono2micro.similarity.kind;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.RepresentationInformation;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.AccessesInformation;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.representationInformation.RepositoryInformation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights.RepositoryWeights;
import pt.ist.socialsoftware.mono2micro.feature.Requirement;
import pt.ist.socialsoftware.mono2micro.feature.Requires;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AuthorRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CommitRepresentation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipyAccessesAndRepository.SIMILARITY_SCIPY_ACCESSES_REPOSITORY;

@JsonTypeName(RepositoryKind.REPOSITORY)
public class RepositoryKind extends SimilarityKind {
	public static final String REPOSITORY = "Repository";

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
	public List<String> weightsTypes() {
		return Collections.singletonList(RepositoryWeights.REPOSITORY_WEIGHTS);
	}

	@Override
	public String scopedRepresentationType() {
		return AccessesRepresentation.ACCESSES;
	}
}
