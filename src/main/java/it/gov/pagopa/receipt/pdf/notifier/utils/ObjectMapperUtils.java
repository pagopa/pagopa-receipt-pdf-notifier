package it.gov.pagopa.receipt.pdf.notifier.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Hide from public usage.
     */
    private ObjectMapperUtils() {
    }

    /**
     * Encodes an object to a string
     *
     * @param value Object to be encoded
     * @return encoded string
     */
    public static String writeValueAsString(Object value) throws JsonProcessingException {
            return objectMapper.writeValueAsString(value);
    }

    /**
     * Maps string to object of defined Class
     *
     * @param string   String to map
     * @param outClass Class to be mapped to
     * @param <T>      Defined Class
     * @return object of the defined Class
     */
    public static <T> T mapString(final String string, Class<T> outClass) throws JsonProcessingException {
        return objectMapper.readValue(string, outClass);
    }
}
