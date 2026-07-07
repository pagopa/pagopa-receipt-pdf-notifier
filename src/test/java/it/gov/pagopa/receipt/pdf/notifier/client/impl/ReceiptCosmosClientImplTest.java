package it.gov.pagopa.receipt.pdf.notifier.client.impl;

import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;

@ExtendWith(MockitoExtension.class)
class ReceiptCosmosClientImplTest {

    @Mock
    private CosmosContainer mockContainer;
    @Mock
    private CosmosPagedIterable<IOMessage> mockIOMessageIterable;
    @Mock
    private Stream<IOMessage> mockIOMessageStream;

    @InjectMocks
    private ReceiptCosmosClientImpl sut;

    @Test
    void testSingletonConnectionError() throws Exception {
        String mockKey = "mockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeyMK==";
        withEnvironmentVariables(
                "COSMOS_RECEIPT_KEY", mockKey,
                "COSMOS_RECEIPT_SERVICE_ENDPOINT", ""
        ).execute(() -> assertThrows(IllegalArgumentException.class, ReceiptCosmosClientImpl::getInstance));
    }

    @Test
    void findIOMessageWithEventIdAndUserTypeSuccess() {
        String messageId = "messageId";
        IOMessage ioMessage = IOMessage.builder().messageId(messageId).build();

        when(mockContainer.queryItems(any(SqlQuerySpec.class), any(), eq(IOMessage.class)))
                .thenReturn(mockIOMessageIterable);
        when(mockIOMessageIterable.stream()).thenReturn(mockIOMessageStream);
        when(mockIOMessageStream.findFirst()).thenReturn(Optional.of(ioMessage));

        IOMessage response = assertDoesNotThrow(
                () -> sut.findIOMessageWithEventIdAndUserType("eventId", UserType.DEBTOR));

        assertNotNull(response);
        assertEquals(messageId, response.getMessageId());
    }

    @Test
    void findIOMessageWithEventIdAndUserTypeFailNotFound() {
        when(mockContainer.queryItems(any(SqlQuerySpec.class), any(), eq(IOMessage.class)))
                .thenReturn(mockIOMessageIterable);
        when(mockIOMessageIterable.stream()).thenReturn(mockIOMessageStream);
        when(mockIOMessageStream.findFirst()).thenReturn(Optional.empty());

        assertThrows(IoMessageNotFoundException.class,
                () -> sut.findIOMessageWithEventIdAndUserType("eventId", UserType.DEBTOR)
        );
    }
}