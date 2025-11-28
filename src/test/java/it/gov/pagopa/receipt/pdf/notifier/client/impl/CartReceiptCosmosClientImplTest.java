package it.gov.pagopa.receipt.pdf.notifier.client.impl;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.util.CosmosPagedIterable;
import it.gov.pagopa.receipt.pdf.notifier.entity.message.CartIOMessage;
import it.gov.pagopa.receipt.pdf.notifier.exception.CartIoMessageNotFoundException;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;

@ExtendWith(MockitoExtension.class)
class CartReceiptCosmosClientImplTest {

    private static final String CART_ID = "1";
    private static final String BIZ_EVENT_ID = "1";

    @Mock
    private CosmosClient cosmosClientMock;

    @Mock
    private CosmosDatabase mockDatabase;
    @Mock
    private CosmosContainer mockContainer;
    @Mock
    private CosmosPagedIterable<CartIOMessage> mockIterable;
    @Mock
    private Iterator<CartIOMessage> mockIterator;

    @InjectMocks
    private CartReceiptCosmosClientImpl sut;

    @Test
    void testSingletonConnectionError() throws Exception {
        String mockKey = "mockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeymockKeyMK==";
        withEnvironmentVariables(
                "COSMOS_RECEIPT_KEY", mockKey,
                "COSMOS_RECEIPT_SERVICE_ENDPOINT", ""
        ).execute(() -> assertThrows(IllegalArgumentException.class, CartReceiptCosmosClientImpl::getInstance));
    }

    @Test
    void findIOMessageWithCartIdAndEventIdAndUserTypeSuccessForPayer() {

        when(cosmosClientMock.getDatabase(any())).thenReturn(mockDatabase);
        when(mockDatabase.getContainer(any())).thenReturn(mockContainer);
        when(mockContainer.queryItems(anyString(), any(), eq(CartIOMessage.class)))
                .thenReturn(mockIterable);
        when(mockIterable.iterator()).thenReturn(mockIterator);
        when(mockIterator.hasNext()).thenReturn(true);
        when(mockIterator.next()).thenReturn(new CartIOMessage());

        CartIOMessage result = assertDoesNotThrow(
                () -> sut.findIOMessageWithCartIdAndEventIdAndUserType(CART_ID, null, UserType.PAYER)
        );

        assertNotNull(result);
    }

    @Test
    void findIOMessageWithCartIdAndEventIdAndUserTypeSuccessForDebtor() {

        when(cosmosClientMock.getDatabase(any())).thenReturn(mockDatabase);
        when(mockDatabase.getContainer(any())).thenReturn(mockContainer);
        when(mockContainer.queryItems(anyString(), any(), eq(CartIOMessage.class)))
                .thenReturn(mockIterable);
        when(mockIterable.iterator()).thenReturn(mockIterator);
        when(mockIterator.hasNext()).thenReturn(true);
        when(mockIterator.next()).thenReturn(new CartIOMessage());

        CartIOMessage result = assertDoesNotThrow(
                () -> sut.findIOMessageWithCartIdAndEventIdAndUserType(CART_ID, BIZ_EVENT_ID, UserType.DEBTOR)
        );

        assertNotNull(result);
    }

    @Test
    void getCartItemFail() {
        when(cosmosClientMock.getDatabase(any())).thenReturn(mockDatabase);
        when(mockDatabase.getContainer(any())).thenReturn(mockContainer);
        when(mockContainer.queryItems(anyString(), any(), eq(CartIOMessage.class)))
                .thenReturn(mockIterable);
        when(mockIterable.iterator()).thenReturn(mockIterator);
        when(mockIterator.hasNext()).thenReturn(false);

        assertThrows(
                CartIoMessageNotFoundException.class,
                () -> sut.findIOMessageWithCartIdAndEventIdAndUserType(CART_ID, null, UserType.PAYER)
        );
    }
}