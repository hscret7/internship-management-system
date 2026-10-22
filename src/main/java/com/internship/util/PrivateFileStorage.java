package com.internship.util;

import java.io.IOException;
import java.nio.file.*;

/** Files live outside the WAR. Legacy files are readable only via authorized endpoints. */
public final class PrivateFileStorage {
    private PrivateFileStorage() { }
    public static Path root() {
        String configured = System.getProperty("internship.uploadDir", System.getenv("INTERNSHIP_UPLOAD_DIR"));
        return Path.of(configured == null || configured.isBlank()
                ? Path.of(System.getProperty("user.home"), "internship_uploads").toString() : configured)
                .toAbsolutePath().normalize();
    }
    public static Path resolve(String stored, String legacyWebRoot) throws IOException {
        if (stored == null) throw new IOException("Missing file path");
        String relative = stored.replace('\\', '/');
        if (relative.startsWith("uploads/")) relative = relative.substring(8);
        Path privatePath = within(root(), relative);
        if (Files.isRegularFile(privatePath)) return privatePath;
        if (legacyWebRoot != null) {
            Path oldRoot = Path.of(legacyWebRoot, "uploads").toAbsolutePath().normalize();
            Path oldFile = within(oldRoot, relative);
            if (Files.isRegularFile(oldFile)) return oldFile;
        }
        // Old fallback deployments used uploads/documents inside the external directory.
        Path oldFallback = within(root(), "uploads/" + relative);
        return Files.isRegularFile(oldFallback) ? oldFallback : privatePath;
    }
    private static Path within(Path base, String relative) throws IOException {
        Path resolved = base.resolve(relative).normalize();
        if (Path.of(relative).isAbsolute() || !resolved.startsWith(base)) throw new IOException("Invalid storage path");
        if (Files.exists(resolved) && !resolved.toRealPath().startsWith(base.toRealPath())) throw new IOException("Invalid storage link");
        return resolved;
    }
}
