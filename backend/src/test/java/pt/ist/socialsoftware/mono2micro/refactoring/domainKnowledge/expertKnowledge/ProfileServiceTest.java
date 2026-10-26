package pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.analytical.domain.CodeEmbeddingsRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.repository.RepresentationRepository;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * The profile rules of D-017, at the {@link ProfileService} seam. No Spring and no Mongo: the profiles live in
 * {@link InMemoryProfiles}, and the Accesses representation's file is a three-functionality stub.
 */
public class ProfileServiceTest {
	private static final String ACCESSES_FILE = "{\"addQuiz\": {}, \"getQuiz\": {}, \"removeQuiz\": {}}";

	@Mock
	private ProfileRepository profileRepository;
	@Mock
	private RepresentationRepository representationRepository;
	@Mock
	private GridFsService gridFsService;

	@InjectMocks
	private ProfileService profileService;

	private AccessesRepresentation accesses;

	@BeforeEach
	public void givenAnAccessesRepresentationWithThreeFunctionalities() throws Exception {
		MockitoAnnotations.initMocks(this);
		InMemoryProfiles.backing(profileRepository);
		accesses = new AccessesRepresentation();
		when(representationRepository.getById(accesses.getId())).thenReturn(accesses);
		when(gridFsService.getFile(anyString()))
				.thenAnswer(invocation -> new ByteArrayInputStream(ACCESSES_FILE.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void aNewProfileIsEmptyAndEveryFunctionalityIsUnassigned() throws Exception {
		profileService.createProfile(accesses.getId(), "Quizzes");

		ProfilesDto profiles = profileService.getProfiles(accesses.getId());

		assertThat(profiles.getProfiles()).extracting(ProfileDto::getName).containsExactly("Quizzes");
		assertThat(profiles.getProfiles().get(0).getUnits()).isEmpty();
		assertThat(profiles.getUnassigned()).containsExactly("addQuiz", "getQuiz", "removeQuiz");
	}

	@Test
	public void movingFunctionalitiesIntoAProfileOffersANewVersionAndLeavesTheOldOneAsItWas() throws Exception {
		Profile original = profileService.createProfile(accesses.getId(), "Quizzes");

		profileService.moveUnits(accesses.getId(), units("addQuiz", "getQuiz"), original.getId());

		ProfilesDto profiles = profileService.getProfiles(accesses.getId());
		assertThat(profiles.getProfiles()).hasSize(1);
		ProfileDto version = profiles.getProfiles().get(0);
		assertThat(version.getId()).isNotEqualTo(original.getId());
		assertThat(version.getName()).isEqualTo("Quizzes");
		assertThat(version.getUnits()).containsExactly("addQuiz", "getQuiz");
		assertThat(profiles.getUnassigned()).containsExactly("removeQuiz");
		assertThat(original.getUnits()).isEmpty();
	}

	@Test
	public void movingBetweenProfilesTakesTheFunctionalitiesOutOfTheSourceAndVersionsBoth() throws Exception {
		Profile quizzes = profileService.createProfile(accesses.getId(), "Quizzes");
		profileService.createProfile(accesses.getId(), "Admin");
		profileService.moveUnits(accesses.getId(), units("addQuiz", "removeQuiz"), quizzes.getId());

		profileService.moveUnits(accesses.getId(), units("removeQuiz"), idOf("Admin"));

		ProfilesDto profiles = profileService.getProfiles(accesses.getId());
		assertThat(unitsOf(profiles, "Quizzes")).containsExactly("addQuiz");
		assertThat(unitsOf(profiles, "Admin")).containsExactly("removeQuiz");
		assertThat(profiles.getUnassigned()).containsExactly("getQuiz");
	}

	@Test
	public void movingWithNoTargetSendsTheFunctionalitiesToUnassigned() throws Exception {
		Profile quizzes = profileService.createProfile(accesses.getId(), "Quizzes");
		profileService.moveUnits(accesses.getId(), units("addQuiz", "getQuiz"), quizzes.getId());

		profileService.moveUnits(accesses.getId(), units("getQuiz"), null);

		ProfilesDto profiles = profileService.getProfiles(accesses.getId());
		assertThat(unitsOf(profiles, "Quizzes")).containsExactly("addQuiz");
		assertThat(profiles.getUnassigned()).containsExactly("getQuiz", "removeQuiz");
	}

	/**
	 * Behavior change allowed by D-017: today, deleting a profile takes its functionalities out of every profile,
	 * including {@code Generic}. Now they go back to Unassigned.
	 */
	@Test
	public void deletingAProfileSendsItsFunctionalitiesToUnassigned() throws Exception {
		Profile quizzes = profileService.createProfile(accesses.getId(), "Quizzes");
		profileService.moveUnits(accesses.getId(), units("addQuiz", "getQuiz"), quizzes.getId());

		profileService.deleteProfile(idOf("Quizzes"));

		ProfilesDto profiles = profileService.getProfiles(accesses.getId());
		assertThat(profiles.getProfiles()).isEmpty();
		assertThat(profiles.getUnassigned()).containsExactly("addQuiz", "getQuiz", "removeQuiz");
	}

	@Test
	public void aRepresentationWithNoUnitsTakesNoProfile() {
		CodeEmbeddingsRepresentation codeEmbeddings = new CodeEmbeddingsRepresentation();
		when(representationRepository.getById(codeEmbeddings.getId())).thenReturn(codeEmbeddings);

		assertThatThrownBy(() -> profileService.createProfile(codeEmbeddings.getId(), "Quizzes"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void aProfileNameIsUniqueAmongTheRepresentationsProfiles() throws Exception {
		profileService.createProfile(accesses.getId(), "Quizzes");

		assertThatThrownBy(() -> profileService.createProfile(accesses.getId(), "Quizzes"))
				.isInstanceOf(KeyAlreadyExistsException.class);
	}

	private String idOf(String profileName) throws Exception {
		return profileNamed(profileService.getProfiles(accesses.getId()), profileName).getId();
	}

	private static List<String> unitsOf(ProfilesDto profiles, String profileName) {
		return profileNamed(profiles, profileName).getUnits();
	}

	private static ProfileDto profileNamed(ProfilesDto profiles, String profileName) {
		return profiles.getProfiles().stream()
				.filter(profile -> profile.getName().equals(profileName))
				.findFirst()
				.orElseThrow(() -> new AssertionError("No profile " + profileName + " offered."));
	}

	private static Set<String> units(String... names) {
		return new HashSet<>(Arrays.asList(names));
	}
}
