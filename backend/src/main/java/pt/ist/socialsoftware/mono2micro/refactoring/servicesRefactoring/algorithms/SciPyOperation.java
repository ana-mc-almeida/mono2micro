package pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring.algorithms;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import pt.ist.socialsoftware.mono2micro.refactoring.servicesRefactoring.AlgorithmOperation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.PartitionsClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.domain.SimilarityScipy;

import static pt.ist.socialsoftware.mono2micro.utils.Constants.SCRIPTS_ADDRESS;

/**
 * Hierarchical clustering by the Python service: it cuts a Similarity's dendrogram by a number of clusters ("N") or by a height ("H"), and keeps
 * the run's Silhouette Score.
 */
public class SciPyOperation extends AlgorithmOperation {
	private String cutType;
	private float cutValue;

	public SciPyOperation() {}

	public SciPyOperation(String cutType, float cutValue) {
		this.cutType = cutType;
		this.cutValue = cutValue;
	}

	@Override
	public ClusterRepresentation apply(Similarity input) throws Exception {
		SimilarityScipy similarity = (SimilarityScipy) input;
		PartitionsClusterRepresentation output = new PartitionsClusterRepresentation(similarity);
		output.setName(similarity.getName() + " " + cutType + Float.valueOf(cutValue).toString().replaceAll("\\.?0*$", ""));

		JSONObject response = cut(similarity.getId(), similarity.getLinkageType());
		output.addMetric("Silhouette Score", response.getDouble("silhouetteScore"));
		output.addClusters(new JSONObject(response.getString("clusters")), similarity.getIDToEntityName());
		return output;
	}

	private JSONObject cut(String similarityMatrixName, String linkageType) {
		String response = WebClient.create(SCRIPTS_ADDRESS)
				.get()
				.uri("/scipy/{similarityMatrixName}/{linkageType}/{cutType}/{cutValue}/createDecomposition",
						similarityMatrixName, linkageType, cutType, Float.toString(cutValue))
				.retrieve()
				.onStatus(HttpStatus::isError, clientResponse -> {
					throw new RuntimeException("Error Code:" + clientResponse.statusCode());
				})
				.bodyToMono(String.class)
				.block();
		try {
			return new JSONObject(response);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public String getCutType() {
		return cutType;
	}

	public void setCutType(String cutType) {
		this.cutType = cutType;
	}

	public float getCutValue() {
		return cutValue;
	}

	public void setCutValue(float cutValue) {
		this.cutValue = cutValue;
	}
}
