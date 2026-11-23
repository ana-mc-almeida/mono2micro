package pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;

/**
 * A refactoring step that reads one input and produces one new {@link ClusterRepresentation}, leaving its input unchanged (D-010, D-016). Each
 * family of operations types its input, and each operation keeps its parameters as plain fields.
 *
 * <p>The operation holds both ends, so a representation needs no link back to what produced it: a representation is collected when no operation
 * has it as {@link #getOutput() output}, and produced otherwise.
 *
 * <p>The id, output and undone flag are the editing chain's, never a request's, so JSON ignores them.
 */
@Document("operation")
public abstract class Operation {
	@Id
	@JsonIgnore
	private String id = new ObjectId().toHexString();
	/** Eager: a lazy reference to an abstract type cannot be downcast (D-024). */
	@DBRef
	@JsonIgnore
	private ClusterRepresentation output;
	@JsonIgnore
	private boolean undone;

	public String getId() {
		return id;
	}

	public ClusterRepresentation getOutput() {
		return output;
	}

	void setOutput(ClusterRepresentation output) {
		this.output = output;
	}

	public boolean isUndone() {
		return undone;
	}

	void setUndone(boolean undone) {
		this.undone = undone;
	}
}
