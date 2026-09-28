package pt.ist.socialsoftware.mono2micro;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Needs Mongo: @SpringBootTest builds the real context. Excluded from the default
// `mvn test` by surefire's <excludedGroups>; the e2e job opts back in with -Dgroups.
@Tag("integration")
@SpringBootTest
public class Mono2microApplicationTests {

	@Test
	public void contextLoads() {
	}

}
