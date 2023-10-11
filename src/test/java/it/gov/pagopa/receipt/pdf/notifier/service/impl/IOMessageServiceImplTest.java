package it.gov.pagopa.receipt.pdf.notifier.service.impl;

import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.CartItem;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.EventData;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.Receipt;
import it.gov.pagopa.receipt.pdf.notifier.exception.MissingFieldsForNotificationException;
import it.gov.pagopa.receipt.pdf.notifier.generated.model.NewMessage;
import it.gov.pagopa.receipt.pdf.notifier.model.enumeration.UserType;
import it.gov.pagopa.receipt.pdf.notifier.service.IOMessageService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SystemStubsExtension.class)
class IOMessageServiceImplTest {

    private static final String VALID_PAYER_CF = "a valid payer fiscal code";
    private static final String VALID_DEBTOR_CF = "a valid debtor fiscal code";
    private static final String EVENT_ID = "123";
    private static final String SUBJECT_PAYER = "Ricevuta del pagamento a payee";
    private static final String SUBJECT_DEBTOR = "Ricevuta del pagamento a payee";
    private static final String MARKDOWN_PAYER = "Hai pagato **200** € a **payee** per **subject**.\n\nEcco la ricevuta con i dettagli.";
    private static final String MARKDOWN_DEBTOR = "È stato effettuato il pagamento di un avviso intestato a te:\n\n**Importo**: 200 €\n**Oggetto:** subject\n**Ente creditore**: payee\n\nEcco la ricevuta con i dettagli.";


    private IOMessageService sut;

    @SystemStub
    private final EnvironmentVariables environmentVariables = new EnvironmentVariables(
            "SUBJECT_PAYER", "Ricevuta del pagamento a {cart.items[0].payee.name}",
            "SUBJECT_DEBTOR", "Ricevuta del pagamento a {cart.items[0].payee.name}",
            "MARKDOWN_PAYER", "Hai pagato **{transaction.amount}** € a **{cart.items[0].payee.name}** per **{cart.items[0].subject}**.\n\nEcco la ricevuta con i dettagli.",
            "MARKDOWN_DEBTOR", "È stato effettuato il pagamento di un avviso intestato a te:\n\n**Importo**: {transaction.amount} €\n**Oggetto:** {cart.items[0].subject}\n**Ente creditore**: {cart.items[0].payee.name}\n\nEcco la ricevuta con i dettagli.");

    @BeforeEach
    void setUp() {
        sut = new IOMessageServiceImpl();
    }

    @Test
    @SneakyThrows
    void buildMessageDebtorWithSuccess() {
        Receipt receipt = buildReceipt();

        NewMessage message = sut.buildNewMessage(VALID_DEBTOR_CF, receipt, UserType.DEBTOR);

        assertNotNull(message);
        assertEquals(VALID_DEBTOR_CF, message.getFiscalCode());
        assertEquals("ADVANCED", message.getFeatureLevelType());
        assertNotNull(message.getContent());
        assertEquals(SUBJECT_DEBTOR, message.getContent().getSubject());
        assertEquals(MARKDOWN_DEBTOR, message.getContent().getMarkdown());
        assertNotNull(message.getContent().getThirdPartyData());
        assertEquals(EVENT_ID, message.getContent().getThirdPartyData().getId());
        assertEquals(Boolean.TRUE, message.getContent().getThirdPartyData().getHasAttachments());
    }

    @Test
    @SneakyThrows
    void buildMessageDebtorWithException() {
        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        assertThrows(MissingFieldsForNotificationException.class, () -> sut.buildNewMessage(VALID_DEBTOR_CF, receipt, UserType.DEBTOR));
    }

    @Test
    @SneakyThrows
    void buildMessagePayerWithSuccess() {
        Receipt receipt = buildReceipt();

        NewMessage message = sut.buildNewMessage(VALID_PAYER_CF, receipt, UserType.PAYER);

        assertNotNull(message);
        assertEquals(VALID_PAYER_CF, message.getFiscalCode());
        assertEquals("ADVANCED", message.getFeatureLevelType());
        assertNotNull(message.getContent());
        assertEquals(SUBJECT_PAYER, message.getContent().getSubject());
        assertEquals(MARKDOWN_PAYER, message.getContent().getMarkdown());
        assertNotNull(message.getContent().getThirdPartyData());
        assertEquals(EVENT_ID, message.getContent().getThirdPartyData().getId());
        assertEquals(Boolean.TRUE, message.getContent().getThirdPartyData().getHasAttachments());
    }

    private Receipt buildReceipt() {
        Receipt receipt = new Receipt();
        receipt.setEventId(EVENT_ID);

        EventData eventData = new EventData();
        eventData.setAmount("200");
        CartItem cartItem = new CartItem();
        cartItem.setPayeeName("payee");
        cartItem.setSubject("subject");
        eventData.setCart(Collections.singletonList(cartItem));
        receipt.setEventData(eventData);
        return receipt;
    }
}