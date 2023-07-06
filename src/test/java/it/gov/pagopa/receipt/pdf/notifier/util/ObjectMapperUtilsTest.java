package it.gov.pagopa.receipt.pdf.notifier.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.receipt.pdf.notifier.utils.ObjectMapperUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

class ObjectMapperUtilsTest {

    @Test
    void returnNullAfterException() {

        Assertions.assertThrows(JsonProcessingException.class, () -> ObjectMapperUtils.writeValueAsString(InputStream.nullInputStream()));
        Assertions.assertThrows(JsonProcessingException.class, () -> ObjectMapperUtils.mapString("", InputStream.class));

    }
}
