package pt.ist.socialsoftware.mono2micro.similarity.kind;

import com.fasterxml.jackson.annotation.JsonTypeName;
import pt.ist.socialsoftware.mono2micro.feature.HasRepresentation;
import pt.ist.socialsoftware.mono2micro.feature.Requirement;

import java.util.Arrays;
import java.util.List;

import static pt.ist.socialsoftware.mono2micro.representation.domain.AccessesRepresentation.ACCESSES;
import static pt.ist.socialsoftware.mono2micro.representation.domain.AuthorRepresentation.AUTHOR;
import static pt.ist.socialsoftware.mono2micro.representation.domain.CommitRepresentation.COMMIT;
import static pt.ist.socialsoftware.mono2micro.representation.domain.IDToEntityRepresentation.ID_TO_ENTITY;
import static pt.ist.socialsoftware.mono2micro.strategy.domain.Strategy.REPOSITORY_STRATEGY;

@JsonTypeName(REPOSITORY_STRATEGY)
public class RepositoryKind extends SimilarityKind {

	@Override
	public List<Requirement> requirements() {
		return Arrays.asList(
				new HasRepresentation(ID_TO_ENTITY),
				new HasRepresentation(ACCESSES),
				new HasRepresentation(AUTHOR),
				new HasRepresentation(COMMIT));
	}
}
