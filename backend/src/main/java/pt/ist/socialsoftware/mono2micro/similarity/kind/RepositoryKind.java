package pt.ist.socialsoftware.mono2micro.similarity.kind;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.feature.Requirement;
import pt.ist.socialsoftware.mono2micro.feature.Requires;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AuthorRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CommitRepresentation;

import java.util.Arrays;
import java.util.List;

import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.REPOSITORY_STRATEGY;

@JsonTypeName(REPOSITORY_STRATEGY)
public class RepositoryKind extends SimilarityKind {

	@Override
	public List<Requirement> requirements() {
		return Arrays.asList(
				Requires.of(AuthorRepresentation.class),
				Requires.of(CommitRepresentation.class));
	}
}
