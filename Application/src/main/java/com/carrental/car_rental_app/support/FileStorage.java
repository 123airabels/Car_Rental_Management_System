package com.carrental.car_rental_app.support;

import com.carrental.car_rental_app.web.ApiError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Saves uploaded images under app.upload-dir. The database stores only the relative path. */
@Component
public class FileStorage {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private final Path root;

    public FileStorage(@Value("${app.upload-dir:uploads}") String dir) {
        this.root = Path.of(dir).toAbsolutePath().normalize();
    }

    /** Accepts JPG and PNG only. The type is checked from the file's own bytes, and the name is generated here. */
    public String save(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) throw ApiError.bad("A photo is required.");
        if (file.getSize() > MAX_BYTES) throw ApiError.bad("Each image must be 5 MB or smaller.");
        try {
            byte[] b = file.getBytes();
            String ext = isJpeg(b) ? "jpg" : isPng(b) ? "png" : null;
            if (ext == null) throw ApiError.bad("Only JPG or PNG images are accepted.");
            Path dir = root.resolve(folder).normalize();
            if (!dir.startsWith(root)) throw ApiError.bad("Invalid upload location.");
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + "." + ext;
            Files.write(dir.resolve(name), b);
            return folder + "/" + name;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The file couldn't be saved. Try again.");
        }
    }

    public byte[] read(String relativePath) {
        try {
            Path p = root.resolve(relativePath).normalize();
            if (!p.startsWith(root) || !Files.isRegularFile(p)) throw ApiError.notFound("File not found.");
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw ApiError.notFound("File not found.");
        }
    }

    public static String contentType(String relativePath) {
        return relativePath.endsWith(".png") ? "image/png" : "image/jpeg";
    }

    private static boolean isJpeg(byte[] b) {
        return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] b) {
        return b.length > 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }
}
