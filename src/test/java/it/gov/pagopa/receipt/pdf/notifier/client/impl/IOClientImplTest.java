package it.gov.pagopa.receipt.pdf.notifier.client.impl;

import it.gov.pagopa.receipt.pdf.notifier.client.IOClient;
import it.gov.pagopa.receipt.pdf.notifier.exception.IOAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class IOClientImplTest {

    private HttpClient clientMock;
    private IOClient sut;

    @BeforeEach
    void setUp() {
        clientMock = mock(HttpClient.class);
        sut = spy(new IOClientImpl(clientMock));
    }

    @Test
    void getProfileSuccess() throws IOAPIException, IOException, InterruptedException {
        sut.getProfile(anyString());

        verify(clientMock).send(any(), any());
    }

    @Test
    void submitMessageSuccess() throws IOAPIException, IOException, InterruptedException {
        sut.submitMessage(anyString());

        verify(clientMock).send(any(), any());
    }

    @Test
    void getProfileFailThrowsIOException() throws IOException, InterruptedException {
        doThrow(IOException.class).when(clientMock).send(any(), any());

        assertThrows(IOAPIException.class, () -> sut.getProfile(anyString()));

        verify(clientMock).send(any(), any());
    }

    @Test
    void getProfileFailThrowsInterruptedException() throws IOException, InterruptedException {
        doThrow(InterruptedException.class).when(clientMock).send(any(), any());

        assertThrows(IOAPIException.class, () -> sut.getProfile(anyString()));

        verify(clientMock).send(any(), any());
    }

    @Test
    void submitMessageFailThrowsIOException() throws IOException, InterruptedException {
        doThrow(IOException.class).when(clientMock).send(any(), any());

        assertThrows(IOAPIException.class, () -> sut.submitMessage(anyString()));

        verify(clientMock).send(any(), any());
    }

    @Test
    void submitMessageFailThrowsInterruptedException() throws IOException, InterruptedException {
        doThrow(InterruptedException.class).when(clientMock).send(any(), any());

        assertThrows(IOAPIException.class, () -> sut.submitMessage(anyString()));

        verify(clientMock).send(any(), any());
    }
}