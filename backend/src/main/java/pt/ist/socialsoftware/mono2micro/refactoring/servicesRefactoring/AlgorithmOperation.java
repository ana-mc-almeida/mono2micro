package pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.data.mongodb.core.mapping.DBRef;
import pt.ist.socialsoftware.mono2micro.feature.VariableFeature;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;

/**
 * An operation that clusters a Similarity, such as a run of a clustering algorithm (D-012). Each one is a variable feature that declares which
 * Similarities it can cluster, and registers itself: its {@code @JsonTypeName} is its name, its JSON type and the type of the request that runs
 * it, so a new algorithm needs no factory or endpoint of its own.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class AlgorithmOperation extends Operation implements VariableFeature {
	/** The Similarity it clustered, which its output is measured with (D-015). Eager, as an abstract type (D-024). */
	@DBRef
	@JsonIgnore
	private Similarity similarity;

	public Similarity getSimilarity() {
		return similarity;
	}

	void setSimilarity(Similarity similarity) {
		this.similarity = similarity;
	}

	/**
	 * Clusters {@code input} into a new cluster representation measured with it, named after the Similarity and this operation's parameters. It
	 * neither saves the output nor sets up its information blocks: the editing chain does, or a recommendation measures it without keeping it.
	 */
	public abstract ClusterRepresentation apply(Similarity input) throws Exception;
}
