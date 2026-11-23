package pt.ist.socialsoftware.mono2micro.refactoring.domainKnowledge.expertKnowledge;

import java.util.List;

/** A representation's profiles, and the units none of them selects (D-017's computed, read-only Unassigned). */
public class ProfilesDto {
	private List<ProfileDto> profiles;
	private List<String> unassigned;

	public ProfilesDto() {}

	public ProfilesDto(List<ProfileDto> profiles, List<String> unassigned) {
		this.profiles = profiles;
		this.unassigned = unassigned;
	}

	public List<ProfileDto> getProfiles() {
		return profiles;
	}

	public List<String> getUnassigned() {
		return unassigned;
	}
}
