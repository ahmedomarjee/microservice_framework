package uk.gov.justice.services.core.json;

import static java.lang.String.format;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Service for validating JSON payloads against a schema.
 */
@ApplicationScoped
public class FileBasedJsonSchemaValidator {

    private final Map<String, Schema> schemas = new ConcurrentHashMap<>();

    @Inject
    Logger logger;

    @Inject
    JsonSchemaLoader jsonSchemaLoader;

    @Inject
    PayloadExtractor payloadExtractor;

    DefaultJsonValidationLoggerHelper defaultJsonValidationLoggerHelper = new DefaultJsonValidationLoggerHelper();

    /**
     * Validate a JSON payload against the correct schema for the given message type name. If the
     * JSON contains metadata, this is removed first. Schemas are cached for reuse.
     *
     * @param envelopeJson the payload to validate
     * @param actionName   the message type name
     */
    public void validateWithoutSchemaCatalog(final String envelopeJson, final String actionName) {
        logger.debug("Falling back to file based schema lookup, no catalog schema found for: {}", actionName);
        final JSONObject payload = payloadExtractor.extractPayloadFrom(envelopeJson);

        try {
            schemaOf(actionName).validate(payload);
        } catch (final ValidationException ex) {

            final String errorMessage = format("Message not valid against schema: %s",
                    defaultJsonValidationLoggerHelper.toValidationTrace(ex));

            throw new JsonSchemaValidationException(errorMessage, ex);
        }

    }

    private Schema schemaOf(final String actionName) {
        return schemas.computeIfAbsent(actionName, jsonSchemaLoader::loadSchema);
    }
}
