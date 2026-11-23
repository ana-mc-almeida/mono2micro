package pt.ist.socialsoftware.mono2micro.operation.characterization;

import org.apache.commons.io.IOUtils;
import pt.ist.socialsoftware.mono2micro.refactoring.functionalityRefactoring.FunctionalityService;
import pt.ist.socialsoftware.mono2micro.refactoring.functionalityRefactoring.domain.Functionality;
import pt.ist.socialsoftware.mono2micro.refactoring.functionalityRefactoring.domain.FunctionalityRedesign;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A {@link FunctionalityService} that keeps redesigns in memory instead of in GridFS, and keeps
 * functionalities nowhere.
 *
 * <p>Needed because {@code AccessesInformation}'s two operation hooks,
 * {@code removeFunctionalitiesWithEntityIDs} and {@code renameClusterInFunctionalities}, both
 * open with {@code ContextManager.get().getBean(FunctionalityService.class)}. With this
 * installed through {@link StubContextManager}, the real {@code AccessesInformation} runs offline,
 * so a test can pin what each operation does to functionalities and their redesigns rather than
 * only which hook it called (that is {@code RecordingRepresentationInformation}'s job).
 *
 * <p>Redesigns are stored as the same JSON bytes production writes to GridFS, under the same file
 * name ({@code functionality.getId() + redesign.getName()}), and read back through the inherited
 * {@code getFunctionalityRedesign(InputStream)}. So every read is a fresh object, as it is in
 * production: a hook that edited a redesign and forgot to write it back would show here.
 *
 * <p><b>What this does not stand in for.</b> The {@code FunctionalityRepository} half:
 * {@code saveFunctionality} is a no-op, because a functionality lives in
 * {@code AccessesInformation.functionalities} for as long as these tests look at it. And nothing
 * here recomputes a functionality; that is {@code AccessesInformation.setupFunctionalities}, which
 * reads traces from GridFS and is not reached by any operation.
 */
class InMemoryFunctionalityService extends FunctionalityService {

	/** Redesign file name → its JSON, as GridFS would hold it. Sorted for readable failures. */
	private final Map<String, byte[]> redesignFiles = new TreeMap<>();

	@Override
	public void saveFunctionality(Functionality functionality) {
	}

	@Override
	public void saveFunctionalityRedesign(Functionality functionality, FunctionalityRedesign functionalityRedesign) throws IOException {
		redesignFiles.put(functionality.getId() + functionalityRedesign.getName(), toJson(functionalityRedesign));
	}

	@Override
	public void updateFunctionalityRedesign(Functionality functionality, FunctionalityRedesign functionalityRedesign) throws IOException {
		String fileName = functionality.getId() + functionalityRedesign.getName();
		if (!redesignFiles.containsKey(fileName))
			throw new IllegalStateException("No redesign file to replace: " + fileName);
		redesignFiles.put(fileName, toJson(functionalityRedesign));
	}

	@Override
	public FunctionalityRedesign getFunctionalityRedesign(Functionality functionality, String redesignName) throws IOException {
		String fileName = functionality.getFunctionalityRedesignFileName(redesignName);
		byte[] json = redesignFiles.get(fileName);
		if (json == null)
			throw new IllegalStateException("No redesign file: " + fileName);
		return getFunctionalityRedesign(new ByteArrayInputStream(json));
	}

	/** Deletes the redesign files, as production does; the repository half has nothing to delete. */
	@Override
	public void deleteFunctionalities(Iterable<Functionality> functionalities) {
		for (Functionality functionality : functionalities)
			functionality.getFunctionalityRedesigns().values().forEach(redesignFiles::remove);
	}

	/** The redesign file names still stored. */
	Set<String> redesignFileNames() {
		return redesignFiles.keySet();
	}

	private static byte[] toJson(FunctionalityRedesign functionalityRedesign) throws IOException {
		return IOUtils.toByteArray(getFunctionalityRedesignAsJSON(functionalityRedesign));
	}
}
