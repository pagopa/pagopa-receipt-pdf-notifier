package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartForReceipt;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.CartPayment;
import it.gov.pagopa.receipt.pdf.notifier.entity.cart.Payload;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.CartItem;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.EventData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.exception.MissingFieldsForNotificationException;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.model.io.message.MessagePayload;
import it.gov.pagopa.receipt.pdf.notifier.service.NotificationMessageBuilder;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SystemStubsExtension.class)
class NotificationMessageBuilderImplTest {

    private static final String VALID_PAYER_CF = "a valid payer fiscal code";
    private static final String VALID_DEBTOR_CF = "a valid debtor fiscal code";
    private static final String EVENT_ID = "123";
    private static final String SUBJECT_PAYER = "Ricevuta del pagamento a payee";
    private static final String SUBJECT_PAYER_CART = "Ricevuta di pagamento";
    private static final String SUBJECT_DEBTOR = "Ricevuta del pagamento a payee";
    private static final String MARKDOWN_PAYER = "Hai effettuato il pagamento di un avviso:\\n\\n**Importo**: 2.300,55 €\\n\\n**Oggetto:** subject\\n\\n**Ente creditore**: payee\\n\\nEcco la ricevuta con i dettagli.";
    private static final String MARKDOWN_PAYER_WITHOUT_SUBJECT = "Hai effettuato il pagamento di un avviso:\\n\\n**Importo**: 2.300,55 €\\n\\n**Oggetto:** -\\n\\n**Ente creditore**: payee\\n\\nEcco la ricevuta con i dettagli.";
    private static final String MARKDOWN_PAYER_CART = "Hai effettuato il pagamento di 3 avvisi:\n\n# Avviso 1\n\n**Importo:** 10,00 €\n**Oggetto:** subject1\n**Ente creditore:** payee1\n\n# Avviso 2\n\n**Importo:** 13,50 €\n**Oggetto:** subject2\n**Ente creditore:** payee2\n\n# Avviso 3\n\n**Importo:** 33,90 €\n**Oggetto:** subject3\n**Ente creditore:** payee3\n\n\nEcco la ricevuta con i dettagli.";
    private static final String MARKDOWN_PAYER_CART_WITHOUT_SUBJECT = "Hai effettuato il pagamento di 3 avvisi:\n\n# Avviso 1\n\n**Importo:** 10,00 €\n**Oggetto:** -\n**Ente creditore:** payee1\n\n# Avviso 2\n\n**Importo:** 13,50 €\n**Oggetto:** -\n**Ente creditore:** payee2\n\n# Avviso 3\n\n**Importo:** 33,90 €\n**Oggetto:** -\n**Ente creditore:** payee3\n\n\nEcco la ricevuta con i dettagli.";
    private static final String MARKDOWN_DEBTOR = "È stato effettuato il pagamento di un avviso intestato a te:\n\n**Importo**: 2.300,55 €\n**Oggetto:** subject\n**Ente creditore**: payee\n\nEcco la ricevuta con i dettagli.";
    private static final String MARKDOWN_DEBTOR_WITHOUT_SUBJECT = "È stato effettuato il pagamento di un avviso intestato a te:\n\n**Importo**: 2.300,55 €\n**Oggetto:** -\n**Ente creditore**: payee\n\nEcco la ricevuta con i dettagli.";
    public static final String CART_TP_ID_SUFFIX = "_CART_";
    public static final String BIZ_EVENT_ID = "bizEventId";


    private NotificationMessageBuilder sut;

    @SystemStub
    private final EnvironmentVariables environmentVariables = new EnvironmentVariables(
            "IO_CONFIGURATION_ID", "asdfasdfafewqrfasdfadsvfadsfgg",
            "SUBJECT_PAYER", "Ricevuta del pagamento a {cart.item.payee.name}",
            "SUBJECT_DEBTOR", "Ricevuta del pagamento a {cart.item.payee.name}",
            "MARKDOWN_PAYER", "Hai effettuato il pagamento di un avviso:\\n\\n**Importo**: {transaction.amount} €\\n\\n**Oggetto:** {cart.item.subject}\\n\\n**Ente creditore**: {cart.item.payee.name}\\n\\nEcco la ricevuta con i dettagli.",
            "MARKDOWN_DEBTOR", "È stato effettuato il pagamento di un avviso intestato a te:\n\n**Importo**: {transaction.amount} €\n**Oggetto:** {cart.item.subject}\n**Ente creditore**: {cart.item.payee.name}\n\nEcco la ricevuta con i dettagli.",
            "MARKDOWN_PAYER_CART", "Hai effettuato il pagamento di {notice.total} avvisi:\n\n{notices}\nEcco la ricevuta con i dettagli.",
            "MARKDOWN_PAYER_CART_ITEM", "# Avviso {notice.index}\n\n**Importo:** {transaction.amount} €\n**Oggetto:** {cart.item.subject}\n**Ente creditore:** {cart.item.payee.name}\n\n",
            "SUBJECT_PAYER_CART", "Ricevuta di pagamento"
    );

    @BeforeEach
    void setUp() {
        sut = new NotificationMessageBuilderImpl();
    }

    @Test
    @SneakyThrows
    void buildMessageDebtorSuccess() {
        Receipt receipt = buildReceipt(true);

        MessagePayload message = sut.buildMessagePayload(VALID_DEBTOR_CF, receipt, UserType.DEBTOR);

        assertNotNull(message);
        assertEquals(VALID_DEBTOR_CF, message.getFiscalCode());
        assertEquals("ADVANCED", message.getFeatureLevelType());
        assertNotNull(message.getContent());
        assertEquals(SUBJECT_DEBTOR, message.getContent().getSubject());
        assertEquals(MARKDOWN_DEBTOR, message.getContent().getMarkdown());
        assertNotNull(message.getContent().getThirdPartyData());
        assertEquals(EVENT_ID, message.getContent().getThirdPartyData().getId());
        assertTrue(message.getContent().getThirdPartyData().getHasAttachments());
        assertFalse(message.getContent().getThirdPartyData().getHasRemoteContent());
        assertNotNull(message.getContent().getThirdPartyData().getConfigurationId());
    }

    @Test
    @SneakyThrows
    void buildMessageDebtorWithoutSubjectSuccess() {
        Receipt receipt = buildReceipt(false);

        MessagePayload message = sut.buildMessagePayload(VALID_DEBTOR_CF, receipt, UserType.DEBTOR);

        assertNotNull(message);
        assertEquals(VALID_DEBTOR_CF, message.getFiscalCode());
        assertEquals("ADVANCED", message.getFeatureLevelType());
        assertNotNull(message.getContent());
        assertEquals(SUBJECT_DEBTOR, message.getContent().getSubject());
        assertEquals(MARKDOWN_DEBTOR_WITHOUT_SUBJECT, message.getContent().getMarkdown());
        assertNotNull(message.getContent().getThirdPartyData());
        assertEquals(EVENT_ID, message.getContent().getThirdPartyData().getId());
        assertTrue(message.getContent().getThirdPartyData().getHasAttachments());
        assertFalse(message.getContent().getThirdPartyData().getHasRemoteContent());
        assertNotNull(message.getContent().getThirdPartyData().getConfigurationId());
    }

    @Test
    @SneakyThrows
    void buildMessageDebtorFailThrowsMissingFieldsForNotificationException() {
        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        assertThrows(MissingFieldsForNotificationException.class, () -> sut.buildMessagePayload(VALID_DEBTOR_CF, receipt, UserType.DEBTOR));
    }

    @Test
    @SneakyThrows
    void buildMessagePayerSuccess() {
        Receipt receipt = buildReceipt(true);

        MessagePayload message = sut.buildMessagePayload(VALID_PAYER_CF, receipt, UserType.PAYER);

        assertNotNull(message);
        assertEquals(VALID_PAYER_CF, message.getFiscalCode());
        assertEquals("ADVANCED", message.getFeatureLevelType());
        assertNotNull(message.getContent());
        assertEquals(SUBJECT_PAYER, message.getContent().getSubject());
        assertEquals(MARKDOWN_PAYER, message.getContent().getMarkdown());
        assertNotNull(message.getContent().getThirdPartyData());
        assertEquals(EVENT_ID, message.getContent().getThirdPartyData().getId());
        assertTrue(message.getContent().getThirdPartyData().getHasAttachments());
        assertFalse(message.getContent().getThirdPartyData().getHasRemoteContent());
        assertNotNull(message.getContent().getThirdPartyData().getConfigurationId());
    }

    @Test
    @SneakyThrows
    void buildMessagePayerWithoutSubjectSuccess() {
        Receipt receipt = buildReceipt(false);

        MessagePayload message = sut.buildMessagePayload(VALID_PAYER_CF, receipt, UserType.PAYER);

        assertNotNull(message);
        assertEquals(VALID_PAYER_CF, message.getFiscalCode());
        assertEquals("ADVANCED", message.getFeatureLevelType());
        assertNotNull(message.getContent());
        assertEquals(SUBJECT_PAYER, message.getContent().getSubject());
        assertEquals(MARKDOWN_PAYER_WITHOUT_SUBJECT, message.getContent().getMarkdown());
        assertNotNull(message.getContent().getThirdPartyData());
        assertEquals(EVENT_ID, message.getContent().getThirdPartyData().getId());
        assertTrue(message.getContent().getThirdPartyData().getHasAttachments());
        assertFalse(message.getContent().getThirdPartyData().getHasRemoteContent());
        assertNotNull(message.getContent().getThirdPartyData().getConfigurationId());
    }

    @Test
    @SneakyThrows
    void buildCartPayerMessagePayloadSuccess() {
        CartForReceipt cart = CartForReceipt.builder()
                .cartId(EVENT_ID)
                .payload(
                        Payload.builder()
                                .totalNotice(3)
                                .cart(
                                        List.of(
                                                CartPayment.builder()
                                                        .debtorFiscalCode(VALID_DEBTOR_CF)
                                                        .subject("subject1")
                                                        .payeeName("payee1")
                                                        .amount("10,00")
                                                        .build(),
                                                CartPayment.builder()
                                                        .debtorFiscalCode("valid cf 2")
                                                        .subject("subject2")
                                                        .payeeName("payee2")
                                                        .amount("13,50")
                                                        .build(),
                                                CartPayment.builder()
                                                        .debtorFiscalCode("valid cf 3")
                                                        .subject("subject3")
                                                        .payeeName("payee3")
                                                        .amount("33,90")
                                                        .build()
                                        )
                                )
                                .build()
                )
                .build();

        MessagePayload message = assertDoesNotThrow(() -> sut.buildCartPayerMessagePayload(VALID_PAYER_CF, cart));

        assertNotNull(message);
        assertEquals(VALID_PAYER_CF, message.getFiscalCode());
        assertEquals("ADVANCED", message.getFeatureLevelType());
        assertNotNull(message.getContent());
        assertEquals(SUBJECT_PAYER_CART, message.getContent().getSubject());
        assertEquals(MARKDOWN_PAYER_CART, message.getContent().getMarkdown());
        assertNotNull(message.getContent().getThirdPartyData());
        assertEquals(EVENT_ID + CART_TP_ID_SUFFIX, message.getContent().getThirdPartyData().getId());
        assertTrue(message.getContent().getThirdPartyData().getHasAttachments());
        assertTrue(message.getContent().getThirdPartyData().getHasRemoteContent());
        assertNotNull(message.getContent().getThirdPartyData().getConfigurationId());
    }

    @Test
    @SneakyThrows
    void buildCartPayerMessagePayloadWithoutSubjectSuccess() {
        CartForReceipt cart = CartForReceipt.builder()
                .cartId(EVENT_ID)
                .payload(
                        Payload.builder()
                                .totalNotice(3)
                                .cart(
                                        List.of(
                                                CartPayment.builder()
                                                        .debtorFiscalCode(VALID_DEBTOR_CF)
                                                        .payeeName("payee1")
                                                        .amount("10,00")
                                                        .build(),
                                                CartPayment.builder()
                                                        .debtorFiscalCode("valid cf 2")
                                                        .payeeName("payee2")
                                                        .amount("13,50")
                                                        .build(),
                                                CartPayment.builder()
                                                        .debtorFiscalCode("valid cf 3")
                                                        .payeeName("payee3")
                                                        .amount("33,90")
                                                        .build()
                                        )
                                )
                                .build()
                )
                .build();

        MessagePayload message = assertDoesNotThrow(() -> sut.buildCartPayerMessagePayload(VALID_PAYER_CF, cart));

        assertNotNull(message);
        assertEquals(VALID_PAYER_CF, message.getFiscalCode());
        assertEquals("ADVANCED", message.getFeatureLevelType());
        assertNotNull(message.getContent());
        assertEquals(SUBJECT_PAYER_CART, message.getContent().getSubject());
        assertEquals(MARKDOWN_PAYER_CART_WITHOUT_SUBJECT, message.getContent().getMarkdown());
        assertNotNull(message.getContent().getThirdPartyData());
        assertEquals(EVENT_ID + CART_TP_ID_SUFFIX, message.getContent().getThirdPartyData().getId());
        assertTrue(message.getContent().getThirdPartyData().getHasAttachments());
        assertTrue(message.getContent().getThirdPartyData().getHasRemoteContent());
        assertNotNull(message.getContent().getThirdPartyData().getConfigurationId());
    }

    @Test
    @SneakyThrows
    void buildCartPayerMessagePayloadFail() {
        CartForReceipt cart = CartForReceipt.builder()
                .cartId(EVENT_ID)
                .payload(
                        Payload.builder()
                                .totalNotice(3)
                                .build()
                )
                .build();

        assertThrows(
                MissingFieldsForNotificationException.class,
                () -> sut.buildCartPayerMessagePayload(VALID_PAYER_CF, cart)
        );
    }

    @Test
    @SneakyThrows
    void buildCartDebtorMessagePayloadSuccess() {
        CartPayment cartPayment = CartPayment.builder()
                .debtorFiscalCode(VALID_DEBTOR_CF)
                .bizEventId(BIZ_EVENT_ID)
                .subject("subject")
                .payeeName("payee")
                .amount("2.300,55")
                .build();

        String expectedTPId = String.format("%s%s%s", EVENT_ID, CART_TP_ID_SUFFIX, BIZ_EVENT_ID);

        MessagePayload message = assertDoesNotThrow(() -> sut.buildCartDebtorMessagePayload(VALID_DEBTOR_CF, cartPayment, EVENT_ID));

        assertNotNull(message);
        assertEquals(VALID_DEBTOR_CF, message.getFiscalCode());
        assertEquals("ADVANCED", message.getFeatureLevelType());
        assertNotNull(message.getContent());
        assertEquals(SUBJECT_DEBTOR, message.getContent().getSubject());
        assertEquals(MARKDOWN_DEBTOR, message.getContent().getMarkdown());
        assertNotNull(message.getContent().getThirdPartyData());
        assertEquals(expectedTPId, message.getContent().getThirdPartyData().getId());
        assertTrue(message.getContent().getThirdPartyData().getHasAttachments());
        assertTrue(message.getContent().getThirdPartyData().getHasRemoteContent());
        assertNotNull(message.getContent().getThirdPartyData().getConfigurationId());
    }

    @Test
    @SneakyThrows
    void buildCartDebtorMessagePayloadWithoutSubjectSuccess() {
        CartPayment cartPayment = CartPayment.builder()
                .debtorFiscalCode(VALID_DEBTOR_CF)
                .bizEventId(BIZ_EVENT_ID)
                .payeeName("payee")
                .amount("2.300,55")
                .build();

        String expectedTPId = String.format("%s%s%s", EVENT_ID, CART_TP_ID_SUFFIX, BIZ_EVENT_ID);

        MessagePayload message = assertDoesNotThrow(() -> sut.buildCartDebtorMessagePayload(VALID_DEBTOR_CF, cartPayment, EVENT_ID));

        assertNotNull(message);
        assertEquals(VALID_DEBTOR_CF, message.getFiscalCode());
        assertEquals("ADVANCED", message.getFeatureLevelType());
        assertNotNull(message.getContent());
        assertEquals(SUBJECT_DEBTOR, message.getContent().getSubject());
        assertEquals(MARKDOWN_DEBTOR_WITHOUT_SUBJECT, message.getContent().getMarkdown());
        assertNotNull(message.getContent().getThirdPartyData());
        assertEquals(expectedTPId, message.getContent().getThirdPartyData().getId());
        assertTrue(message.getContent().getThirdPartyData().getHasAttachments());
        assertTrue(message.getContent().getThirdPartyData().getHasRemoteContent());
        assertNotNull(message.getContent().getThirdPartyData().getConfigurationId());
    }

    @Test
    @SneakyThrows
    void buildCartDebtorMessagePayloadFail() {
        assertThrows(
                MissingFieldsForNotificationException.class,
                () -> sut.buildCartDebtorMessagePayload(VALID_DEBTOR_CF, new CartPayment(), EVENT_ID)
        );
    }

    private Receipt buildReceipt(boolean withSubject) {
        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        EventData eventData = new EventData();
        eventData.setAmount("2.300,55");
        CartItem cartItem = new CartItem();
        cartItem.setPayeeName("payee");
        cartItem.setSubject(withSubject ? "subject" : null);
        eventData.setCart(Collections.singletonList(cartItem));
        receipt.setEventData(eventData);
        return receipt;
    }
}