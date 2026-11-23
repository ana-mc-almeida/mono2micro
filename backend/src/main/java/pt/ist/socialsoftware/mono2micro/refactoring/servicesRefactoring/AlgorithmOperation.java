package pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.mongodb.core.mapping.DBRef;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;

/** An operation that clusters a Similarity, such as a run of a clustering algorithm (D-012). */
public abstract class AlgorithmOperation extends Operation {
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
	 * neither saves the output nor sets up its information blocks: the editing chain does.
	 */
	public abstract ClusterRepresentation apply(Similarity input) throws Exception;
}
