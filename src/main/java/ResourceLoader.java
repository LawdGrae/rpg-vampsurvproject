import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.imageio.ImageIO;

public final class ResourceLoader {
    private static final String RESOURCE_PREFIX = "main/resources/";

    private ResourceLoader() {
    }

    public static BufferedImage loadImage(String resourcePath) {
        for (String candidate : classpathCandidates(resourcePath)) {
            try (InputStream input = ResourceLoader.class.getClassLoader()
                    .getResourceAsStream(candidate)) {
                if (input != null) {
                    return readImage(input, resourcePath);
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Could not load " + resourcePath, exception);
            }
        }

        for (Path candidate : fileCandidates(resourcePath)) {
            if (Files.isRegularFile(candidate)) {
                try {
                    BufferedImage image = ImageIO.read(candidate.toFile());
                    if (image != null) {
                        return image;
                    }
                } catch (IOException exception) {
                    throw new IllegalStateException("Could not load " + resourcePath, exception);
                }
            }
        }

        throw new IllegalStateException("Could not load " + resourcePath);
    }

    private static BufferedImage readImage(InputStream input, String resourcePath)
            throws IOException {
        BufferedImage image = ImageIO.read(input);
        if (image == null) {
            throw new IOException("Unsupported image format: " + resourcePath);
        }
        return image;
    }

    private static Set<String> classpathCandidates(String resourcePath) {
        String normalized = normalize(resourcePath);
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(normalized);
        candidates.add(stripResourcePrefix(normalized));
        return candidates;
    }

    private static Set<Path> fileCandidates(String resourcePath) {
        String normalized = normalize(resourcePath);
        Set<Path> candidates = new LinkedHashSet<>();
        candidates.add(Path.of(normalized));
        candidates.add(Path.of("src", normalized));
        candidates.add(Path.of("src", RESOURCE_PREFIX, stripResourcePrefix(normalized)));
        return candidates;
    }

    private static String normalize(String resourcePath) {
        String normalized = resourcePath.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static String stripResourcePrefix(String resourcePath) {
        if (resourcePath.startsWith(RESOURCE_PREFIX)) {
            return resourcePath.substring(RESOURCE_PREFIX.length());
        }
        return resourcePath;
    }
}
