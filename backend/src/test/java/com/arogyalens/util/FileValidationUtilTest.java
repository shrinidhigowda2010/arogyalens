package com.arogyalens.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arogyalens.exception.ArogyaLensException;
import com.arogyalens.support.TestProps;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileValidationUtilTest {

    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0};
    static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7'};
    static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'};

    private final FileValidationUtil util = new FileValidationUtil(TestProps.defaults());

    @Test
    void detectsTypeFromContentNotFromClaims() {
        assertThat(
                        util.validate(
                                new MockMultipartFile(
                                        "file", "x.bin", "application/octet-stream", PNG)))
                .isEqualTo("image/png");
        assertThat(util.validate(new MockMultipartFile("file", "a.jpg", "image/jpeg", JPEG)))
                .isEqualTo("image/jpeg");
        assertThat(util.validate(new MockMultipartFile("file", "a.pdf", "application/pdf", PDF)))
                .isEqualTo("application/pdf");
        assertThat(util.validate(new MockMultipartFile("file", "a.webp", "image/webp", WEBP)))
                .isEqualTo("image/webp");
    }

    @Test
    void rejectsSpoofedContentType() {
        MockMultipartFile html =
                new MockMultipartFile("file", "evil.png", "image/png", "<html>".getBytes());
        assertThatThrownBy(() -> util.validate(html))
                .isInstanceOf(ArogyaLensException.class)
                .extracting("code")
                .isEqualTo("UNSUPPORTED_FORMAT");
    }

    @Test
    void rejectsEmpty() {
        MockMultipartFile file =
                new MockMultipartFile("file", "report.png", "image/png", new byte[] {});
        assertThatThrownBy(() -> util.validate(file)).extracting("code").isEqualTo("EMPTY_FILE");
    }

    @Test
    void rejectsOversized() {
        byte[] big = new byte[15 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        assertThatThrownBy(
                        () ->
                                util.validate(
                                        new MockMultipartFile("file", "a.png", "image/png", big)))
                .extracting("code")
                .isEqualTo("FILE_TOO_LARGE");
    }
}
