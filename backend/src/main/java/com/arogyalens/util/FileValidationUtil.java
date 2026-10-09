package com.arogyalens.util;

import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.exception.ArogyaLensException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validates uploads by size and by their actual content (magic bytes), not just the client-supplied
 * Content-Type or file extension, which are trivially spoofed.
 */
@Component
public class FileValidationUtil {

    private final ArogyaLensProperties properties;

    public FileValidationUtil(ArogyaLensProperties properties) {
        this.properties = properties;
    }

    /**
     * Validates an upload.
     *
     * @return the detected MIME type (image/jpeg, image/png, image/webp or application/pdf)
     * @throws ArogyaLensException EMPTY_FILE, FILE_TOO_LARGE or UNSUPPORTED_FORMAT
     */
    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ArogyaLensException(
                    "EMPTY_FILE",
                    "Empty upload",
                    "We couldn't find a document in your upload. Please choose a clearer image or PDF.");
        }

        long maxBytes = properties.files().maxSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new ArogyaLensException(
                    "FILE_TOO_LARGE",
                    "File too large",
                    "The file is too large. Please upload a file under "
                            + properties.files().maxSizeMb()
                            + " MB.",
                    HttpStatus.PAYLOAD_TOO_LARGE);
        }

        byte[] head;
        try (InputStream in = file.getInputStream()) {
            head = in.readNBytes(16);
        } catch (IOException e) {
            throw unsupported();
        }
        String detected = detectMime(head).orElseThrow(this::unsupported);
        if (!properties.files().allowedTypeList().contains(detected)) {
            throw unsupported();
        }
        return detected;
    }

    /** Detects JPEG, PNG, WEBP or PDF from the first bytes of a file. */
    public static Optional<String> detectMime(byte[] b) {
        if (b == null) {
            return Optional.empty();
        }
        if (b.length >= 3
                && (b[0] & 0xFF) == 0xFF
                && (b[1] & 0xFF) == 0xD8
                && (b[2] & 0xFF) == 0xFF) {
            return Optional.of("image/jpeg");
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') {
            return Optional.of("image/png");
        }
        if (b.length >= 12
                && b[0] == 'R'
                && b[1] == 'I'
                && b[2] == 'F'
                && b[3] == 'F'
                && b[8] == 'W'
                && b[9] == 'E'
                && b[10] == 'B'
                && b[11] == 'P') {
            return Optional.of("image/webp");
        }
        if (b.length >= 5
                && b[0] == '%'
                && b[1] == 'P'
                && b[2] == 'D'
                && b[3] == 'F'
                && b[4] == '-') {
            return Optional.of("application/pdf");
        }
        return Optional.empty();
    }

    private ArogyaLensException unsupported() {
        return new ArogyaLensException(
                "UNSUPPORTED_FORMAT",
                "Unsupported upload content",
                "Unsupported file format. Please upload a JPG, PNG, WEBP, or PDF.",
                HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }
}
