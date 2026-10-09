package com.arogyalens.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.arogyalens.config.ArogyaLensProperties;
import com.arogyalens.exception.ArogyaLensException;
import com.arogyalens.support.TestProps;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Tests the Gemini client against a mocked HTTP layer; no real API calls are made. */
class GeminiServiceTest {

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";
    private static final String OK_BODY =
            """
            {"candidates":[{"content":{"parts":[
              {"text":"thinking...","thought":true},
              {"text":"```json\\n{\\"answer\\":"},
              {"text":"\\"hi\\"}\\n```","thoughtSignature":"abc"}
            ]}}]}""";

    private MockRestServiceServer server;
    private List<Long> sleeps;

    private GeminiService service(String key, String model, String fallbacks) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        ArogyaLensProperties props = TestProps.withAi(key, model, fallbacks);
        GeminiService s =
                new GeminiService(
                        props,
                        builder,
                        new ObjectMapper(),
                        new AiResponseCache(50, 60_000, Clock.systemUTC()));
        sleeps = new ArrayList<>();
        s.setSleeper(sleeps::add);
        return s;
    }

    @BeforeEach
    void reset() {
        sleeps = new ArrayList<>();
    }

    @Test
    void cleanKeyStripsWhitespaceNewlinesAndQuotes() {
        assertThat(GeminiService.cleanKey("  abc-123 \n")).isEqualTo("abc-123");
        assertThat(GeminiService.cleanKey("\"abc\"")).isEqualTo("abc");
        assertThat(GeminiService.cleanKey(" 'abc' ")).isEqualTo("abc");
        assertThat(GeminiService.cleanKey(null)).isEmpty();
    }

    @Test
    void sendsKeyInHeaderAndJoinsNonThoughtTextParts() {
        GeminiService s = service(" test-key\n", "primary", "");
        server.expect(once(), requestTo(URL.formatted("primary")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "test-key"))
                .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));

        assertThat(s.generateJson("prompt")).isEqualTo("{\"answer\":\"hi\"}");
        server.verify();
    }

    @Test
    void fallsBackToNextModelOnQuotaError() {
        GeminiService s = service("k", "primary", "backup");
        server.expect(once(), requestTo(URL.formatted("primary")))
                .andRespond(
                        withStatus(HttpStatus.TOO_MANY_REQUESTS)
                                .body("{\"error\":{\"code\":429}}"));
        server.expect(once(), requestTo(URL.formatted("backup")))
                .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));

        assertThat(s.generateJson("p")).contains("hi");
        server.verify();
        assertThat(sleeps).isEmpty();
    }

    @Test
    void slowModelIsAbandonedAfterThePerModelTimeout() {
        GeminiService s = service("k", "primary", "backup");
        server.expect(once(), requestTo(URL.formatted("primary")))
                .andRespond(
                        request -> {
                            try {
                                Thread.sleep(2_000);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            return withSuccess(OK_BODY, MediaType.APPLICATION_JSON)
                                    .createResponse(request);
                        });
        server.expect(once(), requestTo(URL.formatted("backup")))
                .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));

        assertThat(s.generateText("slow", java.time.Duration.ofMillis(200)))
                .hasValueSatisfying(v -> assertThat(v).contains("hi"));
        // Second identical request is served from the cache without another call.
        assertThat(s.generateText("slow", java.time.Duration.ofMillis(200))).isPresent();
        server.verify();
    }

    @Test
    void timeCappedTextIsEmptyWhenEveryModelFails() {
        GeminiService s = service("k", "primary", "");
        server.expect(once(), requestTo(URL.formatted("primary")))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{}"));
        assertThat(s.generateText("p", java.time.Duration.ofSeconds(5))).isEmpty();
        assertThat(service("", "m", "").generateText("p", java.time.Duration.ofSeconds(5)))
                .isEmpty();
    }

    @Test
    void retriesOnceWithBackoffOnServerOverload() {
        GeminiService s = service("k", "primary", "");
        server.expect(once(), requestTo(URL.formatted("primary")))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(once(), requestTo(URL.formatted("primary")))
                .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));

        assertThat(s.generateJson("p")).contains("hi");
        assertThat(sleeps).containsExactly(700L);
    }

    @Test
    void invalidKeyFailsFastWithoutTryingFallbacks() {
        GeminiService s = service("bad", "primary", "backup");
        server.expect(once(), requestTo(URL.formatted("primary")))
                .andRespond(
                        withStatus(HttpStatus.BAD_REQUEST)
                                .body("{\"reason\":\"API_KEY_INVALID\"}"));

        assertThatThrownBy(() -> s.generateJson("p"))
                .isInstanceOf(ArogyaLensException.class)
                .extracting("code")
                .isEqualTo(AiErrors.KEY_INVALID);
        server.verify();
    }

    @Test
    void reportsQuotaWhenEveryModelIsExhausted() {
        GeminiService s = service("k", "primary", "backup");
        server.expect(once(), requestTo(URL.formatted("primary")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(once(), requestTo(URL.formatted("backup")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> s.generateJson("p")).extracting("code").isEqualTo(AiErrors.QUOTA);
    }

    @Test
    void emptyAnswerIsReportedAsUnreadable() {
        GeminiService s = service("k", "primary", "");
        server.expect(once(), requestTo(URL.formatted("primary")))
                .andRespond(
                        withSuccess(
                                "{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}",
                                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> s.generateJson("p", new byte[] {1}, "image/png", "medicine label"))
                .isInstanceOf(ArogyaLensException.class)
                .hasMessageContaining("medicine label")
                .extracting("code")
                .isEqualTo(AiErrors.EMPTY);
    }

    @Test
    void identicalRequestsAreServedFromCache() {
        GeminiService s = service("k", "primary", "");
        server.expect(once(), requestTo(URL.formatted("primary")))
                .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));

        assertThat(s.generateJson("same")).isEqualTo(s.generateJson("same"));
        server.verify();
    }

    @Test
    void notConfiguredWhenKeyMissing() {
        GeminiService s = service("   ", "primary", "");
        assertThat(s.isAvailable()).isFalse();
        assertThat(s.generateText("p")).isEmpty();
        assertThatThrownBy(() -> s.generateJson("p"))
                .extracting("code")
                .isEqualTo(AiErrors.NOT_CONFIGURED);
    }

    @Test
    void modelChainIsPrimaryThenDistinctFallbacks() {
        GeminiService s = service("k", " a ", "b, a ,c");
        assertThat(s.modelChain()).containsExactly("a", "b", "c");
    }

    @Test
    void classifiesHttpFailures() {
        assertThat(GeminiService.classify(429, "")).isEqualTo(GeminiService.Kind.QUOTA);
        assertThat(GeminiService.classify(404, "")).isEqualTo(GeminiService.Kind.MODEL_MISSING);
        assertThat(GeminiService.classify(503, "")).isEqualTo(GeminiService.Kind.BUSY);
        assertThat(GeminiService.classify(403, "")).isEqualTo(GeminiService.Kind.KEY_INVALID);
        assertThat(GeminiService.classify(400, "Unable to process input image"))
                .isEqualTo(GeminiService.Kind.BAD_INPUT);
    }

    @Test
    void extractJsonStripsFencesAndProse() {
        GeminiService s = service("k", "m", "");
        assertThat(s.extractJson("Sure! ```json\n{\"a\":1}\n```")).isEqualTo("{\"a\":1}");
        assertThat(s.extractJson("noise {\"a\":{\"b\":2}} tail")).isEqualTo("{\"a\":{\"b\":2}}");
    }

    @Test
    void synthesizedSpeechIsWrappedAsWav() {
        GeminiService s = service("k", "m", "");
        server.expect(once(), requestTo(URL.formatted("tts-model")))
                .andExpect(
                        org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                                        "$.contents[0].parts[0].text")
                                .value(GeminiService.TTS_STYLE + "hello"))
                .andExpect(
                        org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath(
                                        "$.generationConfig.speechConfig.voiceConfig"
                                                + ".prebuiltVoiceConfig.voiceName")
                                .value("Sulafat"))
                .andRespond(
                        withSuccess(
                                "{\"candidates\":[{\"content\":{\"parts\":[{\"inlineData\":{\"data\":\"AAAA\"}}]}}]}",
                                MediaType.APPLICATION_JSON));
        byte[] wav = s.synthesizeSpeech("hello");
        assertThat(new String(wav, 0, 4)).isEqualTo("RIFF");
        assertThat(new String(wav, 8, 4)).isEqualTo("WAVE");
        assertThat(wav).hasSize(44 + 3);
    }
}
