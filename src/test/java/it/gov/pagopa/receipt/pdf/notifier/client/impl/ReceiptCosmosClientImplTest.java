package it.gov.pagopa.receipt.pdf.notifier.client.impl;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.util.CosmosPagedIterable;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.IOMessage;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.exception.IoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.exception.ReceiptNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;

class ReceiptCosmosClientImplTest {

    private CosmosClient cosmosClientMock;
    private ReceiptCosmosClientImpl sut;

    @BeforeEach
    void setUp() {
        cosmosClientMock = mock(CosmosClient.class);
        sut = spy(new ReceiptCosmosClientImpl(cosmosClientMock));
    }

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

        CosmosDatabase mockDatabase = mock(CosmosDatabase.class);
        CosmosContainer mockContainer = mock(CosmosContainer.class);
        CosmosPagedIterable mockIterable = mock(CosmosPagedIterable.class);
        Iterator<Receipt> mockIterator = mock(Iterator.class);

        Receipt receipt = new Receipt();
        receipt.setId(receiptId);

        when(cosmosClientMock.getDatabase(any())).thenReturn(mockDatabase);
        when(mockDatabase.getContainer(any())).thenReturn(mockContainer);
        when(mockContainer.queryItems(anyString(), any(), eq(Receipt.class))).thenReturn(mockIterable);
        when(mockIterable.iterator()).thenReturn(mockIterator);
        when(mockIterator.next()).thenReturn(receipt);
        when(mockIterator.hasNext()).thenReturn(true);

        Receipt response = assertDoesNotThrow(() -> sut.getReceiptDocument(receiptId));

        assertNotNull(response);
        assertEquals(receiptId, response.getId());
    }

    @Test
    void getReceiptDocumentFailNotFound() {
        CosmosDatabase mockDatabase = mock(CosmosDatabase.class);
        CosmosContainer mockContainer = mock(CosmosContainer.class);
        CosmosPagedIterable mockIterable = mock(CosmosPagedIterable.class);
        Iterator<Receipt> mockIterator = mock(Iterator.class);

        when(cosmosClientMock.getDatabase(any())).thenReturn(mockDatabase);
        when(mockDatabase.getContainer(any())).thenReturn(mockContainer);
        when(mockContainer.queryItems(anyString(), any(), eq(Receipt.class))).thenReturn(mockIterable);
        when(mockIterable.iterator()).thenReturn(mockIterator);
        when(mockIterator.hasNext()).thenReturn(false);


        assertThrows(ReceiptNotFoundException.class, () -> sut.getReceiptDocument("an invalid receipt id"));
    }

    @Test
    void findIOMessageWithEventIdAndUserTypeSuccess() {
        String messageId = "messageId";

        CosmosDatabase mockDatabase = mock(CosmosDatabase.class);
        CosmosContainer mockContainer = mock(CosmosContainer.class);
        CosmosPagedIterable mockIterable = mock(CosmosPagedIterable.class);
        Iterator<IOMessage> mockIterator = mock(Iterator.class);

        when(cosmosClientMock.getDatabase(any())).thenReturn(mockDatabase);
        when(mockDatabase.getContainer(any())).thenReturn(mockContainer);
        when(mockContainer.queryItems(anyString(), any(), eq(IOMessage.class))).thenReturn(mockIterable);
        when(mockIterable.iterator()).thenReturn(mockIterator);
        when(mockIterator.next()).thenReturn(IOMessage.builder().messageId(messageId).build());
        when(mockIterator.hasNext()).thenReturn(true);

        IOMessage response = assertDoesNotThrow(() -> sut.findIOMessageWithEventIdAndUserType("eventId", UserType.DEBTOR));

        assertNotNull(response);
        assertEquals(messageId, response.getMessageId());
    }

    @Test
    void findIOMessageWithEventIdAndUserTypeFailNotFound() {
        CosmosDatabase mockDatabase = mock(CosmosDatabase.class);
        CosmosContainer mockContainer = mock(CosmosContainer.class);
        CosmosPagedIterable mockIterable = mock(CosmosPagedIterable.class);
        Iterator<IOMessage> mockIterator = mock(Iterator.class);

        when(cosmosClientMock.getDatabase(any())).thenReturn(mockDatabase);
        when(mockDatabase.getContainer(any())).thenReturn(mockContainer);
        when(mockContainer.queryItems(anyString(), any(), eq(IOMessage.class))).thenReturn(mockIterable);
        when(mockIterable.iterator()).thenReturn(mockIterator);
        when(mockIterator.hasNext()).thenReturn(false);


        assertThrows(IoMessageNotFoundException.class, () -> sut.findIOMessageWithEventIdAndUserType("eventId", UserType.DEBTOR));
    }

}