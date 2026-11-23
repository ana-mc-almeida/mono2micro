package pt.ist.socialsoftware.mono2micro.clusteringAlgorithm;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.web.multipart.MultipartFile;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.ClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.clusterRepresentation.domain.PartitionsClusterRepresentation;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Expert {
	public static final String EXPERT = "Expert Clustering";

	public Expert() {}

	public String getType() {
		return EXPERT;
	}

	public ClusterRepresentation generateClusters(Similarity similarity, String expertName, Optional<MultipartFile> expertFile, Set<String> decompositionNames)
			throws Exception {
		ClusterRepresentation decomposition = new PartitionsClusterRepresentation(similarity);
		decomposition.setExpert(true);

		Map<Short, String> idToEntity;

		String name = similarity.getName() + " " + expertName;
		if (decompositionNames.contains(name))
			throw new KeyAlreadyExistsException();
		decomposition.setName(name);
		decomposition.setExpert(true);

		idToEntity = similarity.getIDToEntityName();

		InputStream is = new BufferedInputStream(expertFile.get().getInputStream());
		JSONObject clustersJSON = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8)).getJSONObject("clusters");
		decomposition.addClusters(clustersJSON, idToEntity);
		is.close();

		return decomposition;
	}
}
