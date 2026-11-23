package pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * Backs a mocked {@link ProfileRepository} with a map, so a test reads back what the service saved through the same
 * queries the service uses. Only the methods {@link ProfileService} calls are backed.
 */
final class InMemoryProfiles {
	private InMemoryProfiles() {}

	static void backing(ProfileRepository profileRepository) {
		Map<String, Profile> profiles = new LinkedHashMap<>();

		when(profileRepository.save(any(Profile.class))).thenAnswer(invocation -> {
			Profile profile = invocation.getArgument(0);
			profiles.put(profile.getId(), profile);
			return profile;
		});
		when(profileRepository.getById(anyString())).thenAnswer(invocation -> Optional.ofNullable(profiles.get(invocation.<String>getArgument(0)))
				.orElseThrow(() -> new RuntimeException("No profile " + invocation.getArgument(0) + " found.")));
		when(profileRepository.findByRepresentationIdAndRetiredFalse(anyString())).thenAnswer(invocation -> profiles.values().stream()
				.filter(profile -> !profile.isRetired() && profile.getRepresentation().getId().equals(invocation.getArgument(0)))
				.collect(Collectors.toList()));
		doAnswer(invocation -> profiles.remove(invocation.<Profile>getArgument(0).getId())).when(profileRepository).delete(any(Profile.class));
	}
}
