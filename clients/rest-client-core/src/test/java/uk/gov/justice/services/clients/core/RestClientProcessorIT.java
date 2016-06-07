package uk.gov.justice.services.clients.core;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static javax.json.JsonValue.NULL;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.common.http.HeaderConstants.CLIENT_CORRELATION_ID;
import static uk.gov.justice.services.common.http.HeaderConstants.SESSION_ID;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;

import uk.gov.justice.services.clients.core.exception.InvalidResponseException;
import uk.gov.justice.services.common.converter.StringToJsonObjectConverter;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjectEnvelopeConverter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.skyscreamer.jsonassert.JSONAssert;

@RunWith(MockitoJUnitRunner.class)
public class RestClientProcessorIT {

    private static final String REQUEST_PARAM_A_PARAM_B_FILE_NAME = "request-envelope-a-b";
    private static final String REQUEST_PARAM_A_PARAM_C_FILE_NAME = "request-envelope-a-c";
    private static final String RESPONSE_WITH_METADATA_FILE_NAME = "response-with-metadata";
    private static final String RESPONSE_WITHOUT_METADATA_FILE_NAME = "response-without-metadata";
    private static final String POST_REQUEST_WITH_METADATA_FILE_NAME = "post-request-with-metadata";
    private static final String POST_REQUEST_BODY_ONLY_FILE_NAME = "post-request-body-only";

    private static final String BASE_PATH = "/example-command-api/rest/command/api";
    private static final String REMOTE_BASE_PATH = "/external-command-api/rest/command/api";
    private static final String BASE_URI = "http://localhost:8089" + REMOTE_BASE_PATH;
    private static final String BASE_URI_WITH_DIFFERENT_PORT = "http://localhost:8080" + REMOTE_BASE_PATH;
    private static final String LOCAL_BASE_URI_WITH_DIFFERENT_PORT = "http://localhost:8080" + BASE_PATH;
    private static final String APP_NAME = "example-command-api";

    private static final String QUERY_NAME = "context.query.myquery";
    private static final String COMMAND_NAME = "context.my-command";
    private static final String PAYLOAD_ID_NAME = "payloadId";
    private static final String PAYLOAD_ID_VALUE = "c3f7182b-bd20-4678-ba8b-e7e5ea8629c3";
    private static final String PAYLOAD_VERSION_NAME = "payloadVersion";
    private static final int PAYLOAD_VERSION_VALUE = 0;
    private static final String PAYLOAD_NAME_NAME = "payloadName";
    private static final String PAYLOAD_NAME_VALUE = "Name of the Payload";
    private static final String METADATA_ID_VALUE = "861c9430-7bc6-4bf0-b549-6534394b8d65";
    private static final String METADATA_ID = "CPPID";
    private static final String MOCK_SERVER_PORT = "mock.server.port";
    private static final String CLIENT_CORRELATION_ID_VALUE = "d51597dc-2526-4c71-bd08-5031c79f11e1";
    private static final String USER_ID_VALUE = "72251abb-5872-46e3-9045-950ac5bae399";
    private static final String SESSION_ID_VALUE = "45b0c3fe-afe6-4652-882f-7882d79eadd9";

    @Rule
    public WireMockRule wireMock8089 = new WireMockRule(8089);

    @Rule
    public WireMockRule wireMock8080 = new WireMockRule(8080);

    private RestClientProcessor restClientProcessor;

    private String envelopeWithMetadataAsJson;
    private String envelopeWithoutMetadataAsJson;

    @Before
    public void setup() throws IOException {
        System.clearProperty(MOCK_SERVER_PORT);
        configureFor(8089);
        initialiseRestClientProcessor();
        envelopeWithMetadataAsJson = responseWithMetadata();
        envelopeWithoutMetadataAsJson = jsonFromFile(RESPONSE_WITHOUT_METADATA_FILE_NAME);
    }

    @Test
    public void shouldDoGetWithNoParameters() throws Exception {

        final String path = "/my/resource";
        final String mimetype = format("application/vnd.%s+json", QUERY_NAME);

        stubFor(get(urlEqualTo(REMOTE_BASE_PATH + path))
                .withHeader(ACCEPT, WireMock.equalTo(mimetype))
                .withHeader(CLIENT_CORRELATION_ID, WireMock.equalTo(CLIENT_CORRELATION_ID_VALUE))
                .withHeader(USER_ID, WireMock.equalTo(USER_ID_VALUE))
                .withHeader(SESSION_ID, WireMock.equalTo(SESSION_ID_VALUE))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, mimetype)
                        .withBody(responseWithMetadata())));

        EndpointDefinition endpointDefinition = new EndpointDefinition(BASE_URI, path, emptySet(), emptySet());

        validateResponse(restClientProcessor.get(endpointDefinition, requestEnvelopeParamAParamB()), envelopeWithMetadataAsJson);
    }


    @Test
    public void shouldDoGetWithPathParameters() throws Exception {
        final String path = "/my/resource/{paramA}/{paramB}";
        final String mimetype = format("application/vnd.%s+json", QUERY_NAME);

        stubFor(get(urlEqualTo(REMOTE_BASE_PATH + "/my/resource/valueA/valueB"))
                .withHeader(ACCEPT, WireMock.equalTo(mimetype))
                .withHeader(CLIENT_CORRELATION_ID, WireMock.equalTo(CLIENT_CORRELATION_ID_VALUE))
                .withHeader(USER_ID, WireMock.equalTo(USER_ID_VALUE))
                .withHeader(SESSION_ID, WireMock.equalTo(SESSION_ID_VALUE))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, mimetype)
                        .withBody(responseWithMetadata())));

        EndpointDefinition endpointDefinition = new EndpointDefinition(BASE_URI, path, ImmutableSet.of("paramA", "paramB"), emptySet());

        validateResponse(restClientProcessor.get(endpointDefinition, requestEnvelopeParamAParamB()), envelopeWithMetadataAsJson);
    }

    @Test
    public void shouldDoRemoteGetWithPortFromSystemProperty() throws Exception {
        System.setProperty(MOCK_SERVER_PORT, "8089");
        initialiseRestClientProcessor();

        final String path = "/my/resource";
        final String mimetype = format("application/vnd.%s+json", QUERY_NAME);

        stubFor(get(urlEqualTo(REMOTE_BASE_PATH + path))
                .withHeader(ACCEPT, WireMock.equalTo(mimetype))
                .withHeader(CLIENT_CORRELATION_ID, WireMock.equalTo(CLIENT_CORRELATION_ID_VALUE))
                .withHeader(USER_ID, WireMock.equalTo(USER_ID_VALUE))
                .withHeader(SESSION_ID, WireMock.equalTo(SESSION_ID_VALUE))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, mimetype)
                        .withBody(responseWithMetadata())));

        EndpointDefinition endpointDefinition = new EndpointDefinition(BASE_URI_WITH_DIFFERENT_PORT, path, emptySet(), emptySet());

        validateResponse(restClientProcessor.get(endpointDefinition, requestEnvelopeParamAParamB()), envelopeWithMetadataAsJson);
    }

    @Test
    public void shouldDoLocalGetIgnoringPortFromSystemProperty() throws Exception {
        System.setProperty(MOCK_SERVER_PORT, "10000");
        configureFor(8080);
        initialiseRestClientProcessor();

        final String path = "/my/resource";
        final String mimetype = format("application/vnd.%s+json", QUERY_NAME);

        stubFor(get(urlEqualTo(BASE_PATH + path))
                .withHeader(ACCEPT, WireMock.equalTo(mimetype))
                .withHeader(CLIENT_CORRELATION_ID, WireMock.equalTo(CLIENT_CORRELATION_ID_VALUE))
                .withHeader(USER_ID, WireMock.equalTo(USER_ID_VALUE))
                .withHeader(SESSION_ID, WireMock.equalTo(SESSION_ID_VALUE))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, mimetype)
                        .withBody(responseWithMetadata())));

        EndpointDefinition endpointDefinition = new EndpointDefinition(LOCAL_BASE_URI_WITH_DIFFERENT_PORT, path, emptySet(), emptySet());

        validateResponse(restClientProcessor.get(endpointDefinition, requestEnvelopeParamAParamB()), envelopeWithMetadataAsJson);
    }

    @Test
    public void shouldDoPostWithPathParameters() throws Exception {

        final String path = "/my/resource/{paramA}/{paramB}";
        final String mimetype = format("application/vnd.%s+json", COMMAND_NAME);
        final String bodyWithoutParams = jsonFromFile(POST_REQUEST_BODY_ONLY_FILE_NAME);

        stubFor(post(urlEqualTo(REMOTE_BASE_PATH + "/my/resource/valueA/valueB"))
                .withHeader(CONTENT_TYPE, WireMock.equalTo(mimetype))
                .withHeader(CLIENT_CORRELATION_ID, WireMock.equalTo(CLIENT_CORRELATION_ID_VALUE))
                .withHeader(USER_ID, WireMock.equalTo(USER_ID_VALUE))
                .withHeader(SESSION_ID, WireMock.equalTo(SESSION_ID_VALUE))
                .withRequestBody(equalToJson(bodyWithoutParams))
                .willReturn(aResponse()
                        .withStatus(ACCEPTED.getStatusCode())));

        EndpointDefinition endpointDefinition = new EndpointDefinition(BASE_URI, path, ImmutableSet.of("paramA", "paramB"), emptySet());

        restClientProcessor.post(endpointDefinition, postRequestEnvelope());

        verify(postRequestedFor(urlEqualTo(REMOTE_BASE_PATH + "/my/resource/valueA/valueB"))
                .withHeader(CONTENT_TYPE, WireMock.equalTo(mimetype))
                .withRequestBody(equalToJson(bodyWithoutParams)));
    }

    @Test
    public void shouldHandleRemoteResponseWithMetadata() throws Exception {

        final String path = "/my/resource";
        final String mimetype = format("application/vnd.%s+json", QUERY_NAME);

        stubFor(get(urlPathEqualTo(REMOTE_BASE_PATH + path))
                .withHeader(ACCEPT, WireMock.equalTo(mimetype))
                .withHeader(CLIENT_CORRELATION_ID, WireMock.equalTo(CLIENT_CORRELATION_ID_VALUE))
                .withHeader(USER_ID, WireMock.equalTo(USER_ID_VALUE))
                .withHeader(SESSION_ID, WireMock.equalTo(SESSION_ID_VALUE))
                .withQueryParam("paramA", WireMock.equalTo("valueA"))
                .withQueryParam("paramC", WireMock.equalTo("valueC"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, mimetype)
                        .withBody(responseWithMetadata())));

        Set<QueryParam> queryParams = ImmutableSet.of(new QueryParam("paramA", true), new QueryParam("paramB", false), new QueryParam("paramC", true));

        EndpointDefinition endpointDefinition = new EndpointDefinition(BASE_URI, path, emptySet(), queryParams);

        validateResponse(restClientProcessor.get(endpointDefinition, requestEnvelopeParamAParamC()), envelopeWithMetadataAsJson);
    }

    @Test
    public void shouldHandleRemoteResponseWithoutMetadata() throws Exception {

        final String path = "/my/resource";
        final String mimetype = format("application/vnd.%s+json", QUERY_NAME);

        stubFor(get(urlPathEqualTo(REMOTE_BASE_PATH + path))
                .withHeader(ACCEPT, WireMock.equalTo(mimetype))
                .withHeader(CLIENT_CORRELATION_ID, WireMock.equalTo(CLIENT_CORRELATION_ID_VALUE))
                .withHeader(USER_ID, WireMock.equalTo(USER_ID_VALUE))
                .withHeader(SESSION_ID, WireMock.equalTo(SESSION_ID_VALUE))
                .withQueryParam("paramA", WireMock.equalTo("valueA"))
                .withQueryParam("paramC", WireMock.equalTo("valueC"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, mimetype)
                        .withHeader(METADATA_ID, METADATA_ID_VALUE)
                        .withBody(envelopeWithoutMetadataAsJson)));

        Set<QueryParam> queryParams = ImmutableSet.of(new QueryParam("paramA", true), new QueryParam("paramB", false), new QueryParam("paramC", true));

        EndpointDefinition endpointDefinition = new EndpointDefinition(BASE_URI, path, emptySet(), queryParams);

        validateResponse(restClientProcessor.get(endpointDefinition, requestEnvelopeParamAParamC()), envelopeWithMetadataAsJson);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionOnQueryParamMissing() throws Exception {

        final String path = "/my/resource";
        final String mimetype = format("application/vnd.%s+json", QUERY_NAME);

        stubFor(get(urlPathEqualTo(REMOTE_BASE_PATH + path))
                .withHeader(ACCEPT, WireMock.equalTo(mimetype))
                .withHeader(CLIENT_CORRELATION_ID, WireMock.equalTo(CLIENT_CORRELATION_ID_VALUE))
                .withHeader(USER_ID, WireMock.equalTo(USER_ID_VALUE))
                .withHeader(SESSION_ID, WireMock.equalTo(SESSION_ID_VALUE))
                .withQueryParam("paramA", WireMock.equalTo("valueA"))
                .withQueryParam("paramC", WireMock.equalTo("valueC"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, mimetype)
                        .withBody(responseWithMetadata())));

        Set<QueryParam> queryParams = ImmutableSet.of(new QueryParam("paramA", true), new QueryParam("paramC", true));

        EndpointDefinition endpointDefinition = new EndpointDefinition(BASE_URI, path, emptySet(), queryParams);

        restClientProcessor.get(endpointDefinition, requestEnvelopeParamAParamB());
    }

    @Test(expected = InvalidResponseException.class)
    public void shouldThrowExceptionWhenMissingCPPID() throws Exception {

        final String path = "/my/resource";
        final String mimetype = format("application/vnd.%s+json", QUERY_NAME);

        stubFor(get(urlPathEqualTo(REMOTE_BASE_PATH + path))
                .withHeader("Accept", WireMock.equalTo(mimetype))
                .withHeader(CLIENT_CORRELATION_ID, WireMock.equalTo(CLIENT_CORRELATION_ID_VALUE))
                .withHeader(USER_ID, WireMock.equalTo(USER_ID_VALUE))
                .withHeader(SESSION_ID, WireMock.equalTo(SESSION_ID_VALUE))
                .withQueryParam("paramA", WireMock.equalTo("valueA"))
                .withQueryParam("paramC", WireMock.equalTo("valueC"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader(CONTENT_TYPE, mimetype)
                        .withBody(envelopeWithoutMetadataAsJson)));

        Set<QueryParam> queryParams = ImmutableSet.of(new QueryParam("paramA", true), new QueryParam("paramB", false), new QueryParam("paramC", true));

        EndpointDefinition endpointDefinition = new EndpointDefinition(BASE_URI, path, emptySet(), queryParams);

        restClientProcessor.get(endpointDefinition, requestEnvelopeParamAParamC());
    }

    @Test
    public void shouldReturnJsonNullPayloadFor404ResponseCode() throws Exception {

        final String path = "/my/resource";
        final String mimetype = format("application/vnd.%s+json", QUERY_NAME);

        stubFor(get(urlEqualTo(path))
                .withHeader(ACCEPT, WireMock.equalTo(mimetype))
                .withHeader(CLIENT_CORRELATION_ID, WireMock.equalTo(CLIENT_CORRELATION_ID_VALUE))
                .withHeader(USER_ID, WireMock.equalTo(USER_ID_VALUE))
                .withHeader(SESSION_ID, WireMock.equalTo(SESSION_ID_VALUE))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader(CONTENT_TYPE, mimetype)
                        .withBody(responseWithMetadata())));

        restClientProcessor.enveloper = new Enveloper();

        EndpointDefinition endpointDefinition = new EndpointDefinition(BASE_URI, path, emptySet(), emptySet());

        JsonEnvelope response = restClientProcessor.get(endpointDefinition, requestEnvelopeParamAParamB());

        assertThat(response, notNullValue());
        assertThat(response.payload(), equalTo(NULL));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionFor500ResponseCode() throws Exception {

        final String path = "/my/resource";
        final String mimetype = format("application/vnd.%s+json", QUERY_NAME);

        stubFor(get(urlEqualTo(REMOTE_BASE_PATH + path))
                .withHeader(ACCEPT, WireMock.equalTo(mimetype))
                .withHeader(CLIENT_CORRELATION_ID, WireMock.equalTo(CLIENT_CORRELATION_ID_VALUE))
                .withHeader(USER_ID, WireMock.equalTo(USER_ID_VALUE))
                .withHeader(SESSION_ID, WireMock.equalTo(SESSION_ID_VALUE))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader(CONTENT_TYPE, mimetype)
                        .withBody(responseWithMetadata())));

        EndpointDefinition endpointDefinition = new EndpointDefinition(BASE_URI, path, emptySet(), emptySet());

        restClientProcessor.get(endpointDefinition, requestEnvelopeParamAParamB());
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionFor500ResponseCodeToPost() throws Exception {

        final String path = "/my/resource/{paramA}/{paramB}";
        final String mimetype = format("application/vnd.%s+json", COMMAND_NAME);
        final String bodyWithoutParams = jsonFromFile(POST_REQUEST_BODY_ONLY_FILE_NAME);

        stubFor(post(urlEqualTo(REMOTE_BASE_PATH + "/my/resource/valueA/valueB"))
                .withHeader(CONTENT_TYPE, WireMock.equalTo(mimetype))
                .withHeader(CLIENT_CORRELATION_ID, WireMock.equalTo(CLIENT_CORRELATION_ID_VALUE))
                .withHeader(USER_ID, WireMock.equalTo(USER_ID_VALUE))
                .withHeader(SESSION_ID, WireMock.equalTo(SESSION_ID_VALUE))
                .withRequestBody(equalToJson(bodyWithoutParams))
                .willReturn(aResponse()
                        .withStatus(INTERNAL_SERVER_ERROR.getStatusCode())));

        EndpointDefinition endpointDefinition = new EndpointDefinition(BASE_URI, path, ImmutableSet.of("paramA", "paramB"), emptySet());

        restClientProcessor.post(endpointDefinition, postRequestEnvelope());
    }

    private void initialiseRestClientProcessor() {
        restClientProcessor = new RestClientProcessor();
        restClientProcessor.stringToJsonObjectConverter = new StringToJsonObjectConverter();
        restClientProcessor.jsonObjectEnvelopeConverter = new JsonObjectEnvelopeConverter();
        restClientProcessor.enveloper = new Enveloper();
        restClientProcessor.appName = APP_NAME;
    }

    private String jsonFromFile(String jsonFileName) throws IOException {
        return Resources.toString(Resources.getResource(String.format("json/%s.json", jsonFileName)), Charset.defaultCharset());
    }

    private JsonEnvelope requestEnvelopeParamAParamB() throws IOException {
        return new JsonObjectEnvelopeConverter().asEnvelope(new StringToJsonObjectConverter().convert(jsonFromFile(REQUEST_PARAM_A_PARAM_B_FILE_NAME)));
    }

    private JsonEnvelope requestEnvelopeParamAParamC() throws IOException {
        return new JsonObjectEnvelopeConverter().asEnvelope(new StringToJsonObjectConverter().convert(jsonFromFile(REQUEST_PARAM_A_PARAM_C_FILE_NAME)));
    }

    private String responseWithMetadata() throws IOException {
        return jsonFromFile(RESPONSE_WITH_METADATA_FILE_NAME);
    }

    private JsonEnvelope postRequestEnvelope() throws IOException {
        return new JsonObjectEnvelopeConverter().asEnvelope(new StringToJsonObjectConverter().convert(jsonFromFile(POST_REQUEST_WITH_METADATA_FILE_NAME)));
    }

    private void validateResponse(JsonEnvelope response, String expectedResponseJson) {
        assertThat(response.metadata(), notNullValue());
        assertThat(response.metadata().id().toString(), equalTo(METADATA_ID_VALUE));
        assertThat(response.metadata().name(), equalTo(QUERY_NAME));

        JSONAssert.assertEquals(expectedResponseJson, new JsonObjectEnvelopeConverter().fromEnvelope(response).toString(), false);

        assertThat(response.payloadAsJsonObject().getString(PAYLOAD_ID_NAME), equalTo(PAYLOAD_ID_VALUE));
        assertThat(response.payloadAsJsonObject().getInt(PAYLOAD_VERSION_NAME), equalTo(PAYLOAD_VERSION_VALUE));
        assertThat(response.payloadAsJsonObject().getString(PAYLOAD_NAME_NAME), equalTo(PAYLOAD_NAME_VALUE));
    }

}
