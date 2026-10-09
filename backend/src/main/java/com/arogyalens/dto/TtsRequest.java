package com.arogyalens.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Text to read aloud with Gemini TTS when the device has no voice for the language. */
public record TtsRequest(
        @NotBlank @Size(max = 800) String text,
        @Pattern(regexp = "^[a-z]{2}$") String language
) {
}
