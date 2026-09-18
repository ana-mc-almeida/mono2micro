package pt.ist.socialsoftware.mono2micro.decomposition;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

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
 *
 * <p><b>The suffixes below are the spellings on disk, and they are not consistent</b> —
 * {@code _IDToEntity.json} capitalizes the leading I while {@code _entityToID.json} does not.
 * They are collector output, so the test matches the collectors rather than tidying them.
 */
final class RepresentationFiles {

    private static final String ROOT = "representations";

    /** The two files the {@code Accesses Based} representation group requires. */
    static final String ID_TO_ENTITY_SUFFIX = "_IDToEntity.json";
    static final String ACCESSES_SUFFIX = "_accesses.json";

    /** Adds the {@code Repository Based} group to the two above. */
    static final String AUTHOR_SUFFIX = "_author.json";
    static final String COMMIT_SUFFIX = "_commit.json";

    /** Adds the {@code Code Embeddings Based} group. Note the lowercase {@code e}. */
    static final String ENTITY_TO_ID_SUFFIX = "_entityToID.json";
    static final String CODE_EMBEDDINGS_SUFFIX = "_code_embeddings.json";

    /** The {@code Structure Based} group. */
    static final String STRUCTURE_SUFFIX = "_structure.json";

    private RepresentationFiles() {}

    static byte[] idToEntity(String caseName) {
        return read(caseName, ID_TO_ENTITY_SUFFIX);
    }

    static byte[] accesses(String caseName) {
        return read(caseName, ACCESSES_SUFFIX);
    }

    /**
     * Fails with the download instructions if any required file is absent. Called from the
     * test's precondition check so that a missing fixture is reported once, up front, rather
     * than as a confusing failure partway through the pipeline.
     */
    static void requirePresent(String caseName, List<String> suffixes) {
        for (String suffix : suffixes)
            read(caseName, suffix);
    }

    /**
     * The requested fixtures that are absent, as bare filenames, in the order given.
     *
     * <p>Used to decide whether a case/strategy combination can run at all. Every other
     * precondition in this suite fails loudly; this one is deliberately narrow — it answers
     * "are the files there", never "did the pipeline work". A file that exists but cannot be
     * read is <em>not</em> reported here, so that a broken fixture reaches
     * {@link #requirePresent} and fails rather than quietly skipping.
     */
    static List<String> missing(String caseName, List<String> suffixes) {
        List<String> absent = new ArrayList<>();
        for (String suffix : suffixes)
            if (!isPresent(caseName, suffix))
                absent.add(caseName + suffix);
        return absent;
    }

    /** Whether a fixture exists on the classpath, without failing when it does not. */
    static boolean isPresent(String caseName, String suffix) {
        String path = path(caseName, suffix);
        try (InputStream stream = RepresentationFiles.class.getClassLoader().getResourceAsStream(path)) {
            return stream != null;
        } catch (IOException e) {
            return false;
        }
    }

    static byte[] read(String caseName, String suffix) {
        String path = path(caseName, suffix);

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

    private static String path(String caseName, String suffix) {
        return ROOT + "/" + caseName + "/" + caseName + suffix;
    }

    private static String missingMessage(String path) {
        return "Test fixture not found on the classpath: " + path
                + "\n\nThese files are not committed. Download them into"
                + "\n  backend/src/test/resources/" + ROOT + "/"
                + "\nand see that folder's README.md for the link and the expected layout.";
    }
}
