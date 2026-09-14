package pt.ist.socialsoftware.mono2micro.decomposition;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

/**
 * Reads representation fixtures from the test classpath.
 *
 * <p>The files are downloaded rather than committed (see
 * {@code src/test/resources/representations/README.md}), so a fresh clone has an empty
 * fixture folder. Every failure here therefore names the download, because the alternative
 * is a {@code NullPointerException} out of {@code getResourceAsStream} that tells a new
 * contributor nothing.
 *
 * <p>Filenames are derived from the case name: a case {@code quizzes-tutor} is expected at
 * {@code representations/quizzes-tutor/quizzes-tutor_accesses.json}.
 */
final class RepresentationFiles {

    private static final String ROOT = "representations";

    /** The two files the {@code Accesses Based} representation group requires. */
    static final String ID_TO_ENTITY_SUFFIX = "_IDToEntity.json";
    static final String ACCESSES_SUFFIX = "_accesses.json";

    private RepresentationFiles() {}

    static byte[] idToEntity(String caseName) {
        return read(caseName, ID_TO_ENTITY_SUFFIX);
    }

    static byte[] accesses(String caseName) {
        return read(caseName, ACCESSES_SUFFIX);
    }

    /**
     * Fails with the download instructions if either required file is absent. Called from the
     * test's precondition check so that a missing fixture is reported once, up front, rather
     * than as a confusing failure partway through the pipeline.
     */
    static void requirePresent(String caseName) {
        read(caseName, ID_TO_ENTITY_SUFFIX);
        read(caseName, ACCESSES_SUFFIX);
    }

    private static byte[] read(String caseName, String suffix) {
        String path = ROOT + "/" + caseName + "/" + caseName + suffix;

        try (InputStream stream = RepresentationFiles.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null)
                throw new IllegalStateException(missingMessage(path));

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = stream.read(chunk)) != -1)
                buffer.write(chunk, 0, read);

            byte[] content = buffer.toByteArray();
            if (content.length == 0)
                throw new IllegalStateException("Fixture is empty: " + path + "\n" + missingMessage(path));

            return content;

        } catch (IOException e) {
            throw new IllegalStateException("Could not read fixture: " + path, e);
        }
    }

    private static String missingMessage(String path) {
        return "Test fixture not found on the classpath: " + path
                + "\n\nThese files are not committed. Download them into"
                + "\n  backend/src/test/resources/" + ROOT + "/"
                + "\nand see that folder's README.md for the link and the expected layout.";
    }
}
