package pt.ist.socialsoftware.mono2micro.operation.characterization;

import org.springframework.test.util.ReflectionTestUtils;
import pt.ist.socialsoftware.mono2micro.fileManager.ContextManager;
import pt.ist.socialsoftware.mono2micro.functionality.FunctionalityService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Puts a {@link ContextManager} that hands out one bean, a {@link FunctionalityService}, into
 * {@code ContextManager}'s static cache, and takes it out again.
 *
 * <p>{@code ContextManager.get()} returns the cached instance when there is one, and only builds
 * the real, whole-package {@code AnnotationConfigApplicationContext} when there is not. Filling
 * the cache first means the real one is never built, which keeps this package's rule (never let
 * a test reach the real {@code ContextManager}) while letting {@code AccessesInformation}'s hooks
 * run. The stub is a Mockito mock, so no constructor runs and nothing is scanned.
 *
 * <p>Any other bean fails loudly instead of returning {@code null}, so a test that strays into
 * GridFS or a repository says so by name.
 *
 * <p><b>Always pair {@link #install} with {@link #uninstall}</b>, in an {@code @AfterEach}: the
 * cache is a static field, so a stub left behind would leak into every later test in the same
 * surefire fork. {@code uninstall} puts back whatever was there before: {@code null} in an
 * offline run, but possibly a real context when the stack tests share the fork
 * ({@code mvn test -Dexcluded.groups=}).
 */
final class StubContextManager {

	private static final String CACHE_FIELD = "contextManager";

	private static ContextManager previous;
	private static boolean installed;

	private StubContextManager() {
	}

	static void install(FunctionalityService functionalityService) {
		if (installed)
			throw new IllegalStateException("A stub ContextManager is already installed");

		ContextManager stub = mock(ContextManager.class);
		doAnswer(invocation -> {
			Class<?> beanClass = invocation.getArgument(0);
			if (beanClass != FunctionalityService.class)
				throw new AssertionError("Unexpected bean requested from ContextManager: " + beanClass.getName());
			return functionalityService;
		}).when(stub).getBean(any());

		previous = (ContextManager) ReflectionTestUtils.getField(ContextManager.class, CACHE_FIELD);
		ReflectionTestUtils.setField(ContextManager.class, CACHE_FIELD, stub);
		installed = true;
	}

	static void uninstall() {
		if (!installed)
			return;
		ReflectionTestUtils.setField(ContextManager.class, CACHE_FIELD, previous);
		previous = null;
		installed = false;
	}
}
