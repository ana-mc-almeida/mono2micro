package pt.ist.socialsoftware.mono2micro.similarity.kind;

import pt.ist.socialsoftware.mono2micro.feature.FeatureFamily;
import pt.ist.socialsoftware.mono2micro.feature.FeatureId;
import pt.ist.socialsoftware.mono2micro.feature.Requirement;
import pt.ist.socialsoftware.mono2micro.feature.Requires;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.entityDictionary.IDToEntityRepresentation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The 8 Similarity kinds (D-021, D-025). Every kind reads the ID-to-entity map and the Accesses representation, so the family shares those two
 * requirements, and each kind declares only what it needs on top.
 *
 * <p>No kind is present in a software system. For an input, the family answers with the input's kind: a Similarity's, read from its
 * {@code kindName}, such as the one an algorithm operation would cluster, or the kind itself, such as the one a profile would scope.
 */
public class SimilarityKindFamily extends FeatureFamily<SimilarityKind> {

	public SimilarityKindFamily() {
		super("Similarity kind as input", SimilarityKind.class);
	}

	@Override
	public Set<FeatureId> presentInInput(Object input) {
		Object kind = input instanceof Similarity ? SimilarityKind.getByName(((Similarity) input).getKindName()) : input;
		return featuresAmong(Collections.singleton(kind));
	}

	@Override
	public List<Requirement> sharedRequirements() {
		return Arrays.asList(
				Requires.of(IDToEntityRepresentation.class),
				Requires.of(AccessesRepresentation.class));
	}
}
