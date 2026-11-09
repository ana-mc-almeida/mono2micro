package pt.ist.socialsoftware.mono2micro.similarity.domain.similarityMatrix.weights;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import pt.ist.socialsoftware.mono2micro.recommendation.domain.Recommendation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;

import java.util.List;
import java.util.Set;

/** Its subclasses are JSON subtypes by their {@code @JsonTypeName}, found by the self-registration scan (D-019 point 8). */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class Weights {
	public abstract String getType();

	public abstract int getNumberOfWeights();

	public abstract float[] getWeights();

	public abstract List<String> getWeightsNames();

	public abstract String getName();

	public abstract void setWeightsFromArray(float[] weightsArray);

	public abstract void fillMatrix(GridFsService gridFsService, Similarity similarity, float[][][] rawMatrix, Set<Short> elements, int fillFromIndex)
			throws Exception;

	public abstract void fillMatrix(GridFsService gridFsService, Recommendation recommendation, float[][][] rawMatrix, Set<Short> elements, int fillFromIndex)
			throws Exception;

	@Override
	public abstract boolean equals(Object object);
}
