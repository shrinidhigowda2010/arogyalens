package com.arogyalens.ai;

import com.arogyalens.exception.ArogyaLensException;
import org.springframework.http.HttpStatus;

/**
 * Factory for the distinct, user-facing AI failure modes so the UI can tell "busy, try again" apart
 * from "invalid key" or "unreadable image".
 */
public final class AiErrors {

    public static final String BUSY = "AI_BUSY";
    public static final String QUOTA = "AI_QUOTA";
    public static final String KEY_INVALID = "AI_KEY_INVALID";
    public static final String EMPTY = "AI_EMPTY";
    public static final String TIMEOUT = "AI_TIMEOUT";
    public static final String NOT_CONFIGURED = "AI_NOT_CONFIGURED";
    public static final String FAILURE = "AI_FAILURE";

    private AiErrors() {}

    public static ArogyaLensException busy() {
        return new ArogyaLensException(
                BUSY,
                "AI provider overloaded",
                "The AI service is busy right now. Please try again in a minute.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    public static ArogyaLensException quota() {
        return new ArogyaLensException(
                QUOTA,
                "AI quota exhausted",
                "Today's free AI limit has been reached. Please try again later.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    public static ArogyaLensException keyInvalid() {
        return new ArogyaLensException(
                KEY_INVALID,
                "AI key rejected",
                "The AI service is not set up correctly (the API key was rejected). Please contact the site owner.",
                HttpStatus.BAD_GATEWAY);
    }

    public static ArogyaLensException timeout() {
        return new ArogyaLensException(
                TIMEOUT,
                "AI request timed out",
                "The AI service took too long to respond. Please try again.",
                HttpStatus.GATEWAY_TIMEOUT);
    }

    public static ArogyaLensException unreadable(String what) {
        return new ArogyaLensException(
                EMPTY,
                "Could not read " + what,
                "We couldn't confidently read this "
                        + what
                        + ". Try a clearer, well-lit photo with the whole page visible.",
                HttpStatus.UNPROCESSABLE_ENTITY);
    }

    public static ArogyaLensException notConfigured() {
        return new ArogyaLensException(
                NOT_CONFIGURED,
                "Gemini API key missing",
                "Image analysis needs the AI service, which is not configured on this server. Text PDFs can still be analyzed.",
                HttpStatus.SERVICE_UNAVAILABLE);
    }

    public static ArogyaLensException failure() {
        return new ArogyaLensException(
                FAILURE,
                "AI provider failure",
                "The AI service is temporarily unavailable. Please try again.",
                HttpStatus.BAD_GATEWAY);
    }
}
