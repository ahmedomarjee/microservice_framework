package uk.gov.justice.subscription.yaml.parser;

import static java.lang.String.format;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;

import java.io.IOException;
import java.net.URL;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class YamlParser {

    public <T> T parseYamlFrom(final URL yamlUrl, final TypeReference typeReference) {
        try {
            return getObjectMapper().readValue(yamlUrl, typeReference);
        } catch (final IOException e) {
            throw new YamlParserException(format("Failed to read YAML file %s ", yamlUrl), e);
        }
    }

    public <T> T parseYamlFrom(final URL yamlUrl, final Class<T> classType) {
        try {
            return getObjectMapper().readValue(yamlUrl, classType);
        } catch (final IOException e) {
            throw new YamlParserException(format("Failed to read YAML file %s ", yamlUrl), e);
        }
    }

    private ObjectMapper getObjectMapper() {
        return new ObjectMapperProducer().objectMapperWith(new YAMLFactory())
                .setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
    }
}
