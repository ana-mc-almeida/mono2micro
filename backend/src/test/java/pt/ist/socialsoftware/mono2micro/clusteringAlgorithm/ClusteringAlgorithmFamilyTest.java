package pt.ist.socialsoftware.mono2micro.clusteringAlgorithm;

import org.junit.jupiter.api.Test;
import pt.ist.socialsoftware.mono2micro.feature.FeatureFamilies;
import pt.ist.socialsoftware.mono2micro.feature.FeatureId;

import static org.assertj.core.api.Assertions.assertThat;

/** The clustering algorithms are a family (D-025), so the boot check finds every algorithm claimed. No algorithm is ever present. */
public class ClusteringAlgorithmFamilyTest {

	@Test
	public void everyAlgorithmIsAFeatureOfTheFamily() {
		assertThat(FeatureFamilies.scanned().familyWithMemberType(Clustering.class)).hasValueSatisfying(family -> assertThat(family.features())
				.extracting(FeatureId::getName)
				.containsExactlyInAnyOrderElementsOf(ClusteringFactory.algorithmTypes));
	}
}
