package it.gov.pagopa.receipt.pdf.notifier.client.impl;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.exception.ReceiptNotFoundException;
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
    private CosmosClient cosmosClientMock;
    @Mock
    private CosmosDatabase mockDatabase;
    @Mock
    private CosmosContainer mockContainer;
    @Mock
    private CosmosPagedIterable<Receipt> mockIterable;
    @Mock
    private CosmosPagedIterable<IOMessage> mockIOMessageIterable;
    @Mock
    private Iterable<FeedResponse<Receipt>> mockReceiptIterableByPage;
    @Mock
    private Stream<Receipt> mockReceiptStream;
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
        ).execute(() -> assertThrows(IllegalArgumentException.class, ReceiptCosmosClientImpl::getInstance)
        );
    }

    @Test
    void getReceiptDocumentSuccess() {
        String receiptId = "a valid receipt id";
        Receipt receipt = new Receipt();
        receipt.setId(receiptId);

        when(cosmosClientMock.getDatabase(any())).thenReturn(mockDatabase);
        when(mockDatabase.getContainer(any())).thenReturn(mockContainer);
        when(mockContainer.queryItems(any(SqlQuerySpec.class), any(), eq(Receipt.class))).thenReturn(mockIterable);
        when(mockIterable.stream()).thenReturn(mockReceiptStream);
        when(mockReceiptStream.findFirst()).thenReturn(Optional.of(receipt));

        Receipt response = assertDoesNotThrow(() -> sut.getReceiptDocument(receiptId));

        assertNotNull(response);
        assertEquals(receiptId, response.getId());
    }

    @Test
    void getReceiptDocumentFailNotFound() {
        when(cosmosClientMock.getDatabase(any())).thenReturn(mockDatabase);
        when(mockDatabase.getContainer(any())).thenReturn(mockContainer);
        when(mockContainer.queryItems(any(SqlQuerySpec.class), any(), eq(Receipt.class))).thenReturn(mockIterable);
        when(mockIterable.stream()).thenReturn(mockReceiptStream);
        when(mockReceiptStream.findFirst()).thenReturn(Optional.empty());

        assertThrows(ReceiptNotFoundException.class,
                () -> sut.getReceiptDocument("an invalid receipt id")
        );
    }

    @Test
    void findIOMessageWithEventIdAndUserTypeSuccess() {
        String messageId = "messageId";
        IOMessage ioMessage = IOMessage.builder().messageId(messageId).build();

        when(cosmosClientMock.getDatabase(any())).thenReturn(mockDatabase);
        when(mockDatabase.getContainer(any())).thenReturn(mockContainer);
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
        when(cosmosClientMock.getDatabase(any())).thenReturn(mockDatabase);
        when(mockDatabase.getContainer(any())).thenReturn(mockContainer);
        when(mockContainer.queryItems(any(SqlQuerySpec.class), any(), eq(IOMessage.class)))
                .thenReturn(mockIOMessageIterable);
        when(mockIOMessageIterable.stream()).thenReturn(mockIOMessageStream);
        when(mockIOMessageStream.findFirst()).thenReturn(Optional.empty());

        assertThrows(IoMessageNotFoundException.class,
                () -> sut.findIOMessageWithEventIdAndUserType("eventId", UserType.DEBTOR)
        );
    }

}