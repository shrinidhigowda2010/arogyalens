package com.arogyalens.util;

import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.exception.ArogyaLensException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

@Component
public class FileValidationUtil {

    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf");

    private final ArogyaLensProperties properties;

    public FileValidationUtil(ArogyaLensProperties properties) {
        this.properties = properties;
    }

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ArogyaLensException(
                    "EMPTY_FILE",
                    "Empty upload",
                    "We couldn't find a document in your upload. Please choose a clearer image or PDF."
            );
        }

        long maxBytes = properties.files().maxSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new ArogyaLensException(
                    "FILE_TOO_LARGE",
                    "File too large",
                    "The file is too large. Please upload a file under " + properties.files().maxSizeMb() + " MB."
            );
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1) : "";

        boolean typeOk = properties.files().allowedTypeList().stream()
                .anyMatch(t -> t.equalsIgnoreCase(contentType));
        boolean extOk = EXTENSIONS.contains(ext);

        if (!typeOk && !extOk) {
            throw new ArogyaLensException(
                    "UNSUPPORTED_FORMAT",
                    "Unsupported format: " + contentType,
                    "Unsupported file format. Please upload a JPG, PNG, or PDF."
            );
        }
    }
}
