package pt.ist.socialsoftware.mono2micro.decomposition.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import static pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.SciPyClustering.SCIPY;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
		@JsonSubTypes.Type(value = SciPyRequestDto.class, name = SCIPY),
})
public abstract class DecompositionRequest {
	String similarityId;

	public String getSimilarityId() {
		return similarityId;
	}

	public void setSimilarityId(String similarityId) {
		this.similarityId = similarityId;
	}
}
