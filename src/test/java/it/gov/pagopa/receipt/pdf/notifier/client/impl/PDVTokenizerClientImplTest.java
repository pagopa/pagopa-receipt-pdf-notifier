package it.gov.pagopa.receipt.pdf.notifier.client.impl;

import it.gov.pagopa.receipt.pdf.notifier.client.PDVTokenizerClient;
import it.gov.pagopa.receipt.pdf.notifier.entity.receipt.enumeration.ReasonErrorCode;
import it.gov.pagopa.receipt.pdf.notifier.exception.PDVTokenizerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class PDVTokenizerClientImplTest {

    private HttpClient clientMock;
    private PDVTokenizerClient sut;

    @BeforeEach
    void setUp() {
        clientMock = mock(HttpClient.class);
        sut = spy(new PDVTokenizerClientImpl(clientMock));
    }

    @Test
    void searchTokenByPIISuccess() throws PDVTokenizerException, IOException, InterruptedException {
        sut.searchTokenByPII(anyString());

        verify(clientMock).send(any(), any());
    }

    @Test
    void findPIIByTokenSuccess() throws PDVTokenizerException, IOException, InterruptedException {
        sut.findPIIByToken(anyString());

        verify(clientMock).send(any(), any());
    }

    @Test
    void createTokenSuccess() throws PDVTokenizerException, IOException, InterruptedException {
        sut.createToken(anyString());

        verify(clientMock).send(any(), any());
    }

    @Test
    void searchTokenByPIIFailThrowsIOException() throws IOException, InterruptedException {
        doThrow(IOException.class).when(clientMock).send(any(), any());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.searchTokenByPII(anyString()));

        assertEquals(ReasonErrorCode.ERROR_PDV_IO.getCode(), e.getStatusCode());

        verify(clientMock).send(any(), any());
    }

    @Test
    void searchTokenByPIIFailThrowsInterruptedException() throws IOException, InterruptedException {
        doThrow(InterruptedException.class).when(clientMock).send(any(), any());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.searchTokenByPII(anyString()));

        assertEquals(ReasonErrorCode.ERROR_PDV_UNEXPECTED.getCode(), e.getStatusCode());

        verify(clientMock).send(any(), any());
    }

    @Test
    void findPIIByTokenFailThrowsIOException() throws IOException, InterruptedException {
        doThrow(IOException.class).when(clientMock).send(any(), any());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.findPIIByToken(anyString()));

        assertEquals(ReasonErrorCode.ERROR_PDV_IO.getCode(), e.getStatusCode());

        verify(clientMock).send(any(), any());
    }

    @Test
    void findPIIByTokenFailThrowsInterruptedException() throws IOException, InterruptedException {
        doThrow(InterruptedException.class).when(clientMock).send(any(), any());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.findPIIByToken(anyString()));

        assertEquals(ReasonErrorCode.ERROR_PDV_UNEXPECTED.getCode(), e.getStatusCode());

        verify(clientMock).send(any(), any());
    }

    @Test
    void createTokenFailThrowsIOException() throws IOException, InterruptedException {
        doThrow(IOException.class).when(clientMock).send(any(), any());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.createToken(anyString()));

        assertEquals(ReasonErrorCode.ERROR_PDV_IO.getCode(), e.getStatusCode());

        verify(clientMock).send(any(), any());
    }

    @Test
    void createTokenFailThrowsInterruptedException() throws IOException, InterruptedException {
        doThrow(InterruptedException.class).when(clientMock).send(any(), any());

        PDVTokenizerException e = assertThrows(PDVTokenizerException.class, () -> sut.createToken(anyString()));

        assertEquals(ReasonErrorCode.ERROR_PDV_UNEXPECTED.getCode(), e.getStatusCode());

        verify(clientMock).send(any(), any());
    }
}