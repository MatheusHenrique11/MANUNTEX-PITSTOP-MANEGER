package com.manutex.pitstop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.MalformedURLException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock S3Client s3Client;
    @Mock S3Presigner presigner;

    StorageService storageService;

    @BeforeEach
    void setUp() throws Exception {
        storageService = new StorageService(s3Client, presigner);
        var bucketField = StorageService.class.getDeclaredField("bucket");
        bucketField.setAccessible(true);
        bucketField.set(storageService, "test-bucket");

        var expiryField = StorageService.class.getDeclaredField("presignedExpiryMinutes");
        expiryField.setAccessible(true);
        expiryField.set(storageService, 5);
    }

    @Test
    void deveArmazenarArquivoComPutObject() {
        byte[] content = "PDF content".getBytes();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());

        storageService.store("docs/file.pdf", content, "application/pdf");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest req = captor.getValue();
        assertThat(req.bucket()).isEqualTo("test-bucket");
        assertThat(req.key()).isEqualTo("docs/file.pdf");
        assertThat(req.contentLength()).isEqualTo(content.length);
    }

    @Test
    void deveArmazenarImagemComMimeTypeCorreto() {
        byte[] imgBytes = new byte[]{(byte) 0xFF, (byte) 0xD8};

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
            .thenReturn(PutObjectResponse.builder().build());

        storageService.storeImage("logos/empresa.jpg", imgBytes, "image/jpeg");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        assertThat(captor.getValue().contentType()).isEqualTo("image/jpeg");
        assertThat(captor.getValue().key()).isEqualTo("logos/empresa.jpg");
    }

    @Test
    void deveRecuperarArquivoExistente() {
        byte[] expected = "conteudo cifrado".getBytes();

        @SuppressWarnings("unchecked")
        ResponseBytes<GetObjectResponse> resp = mock(ResponseBytes.class);
        when(resp.asByteArray()).thenReturn(expected);
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(resp);

        byte[] result = storageService.retrieve("docs/file.pdf");

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void deveLancarDocumentNotFoundExceptionQuandoArquivoNaoExiste() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
            .thenThrow(NoSuchKeyException.builder().message("NoSuchKey").build());

        assertThatThrownBy(() -> storageService.retrieve("docs/inexistente.pdf"))
            .isInstanceOf(StorageService.DocumentNotFoundException.class)
            .hasMessageContaining("docs/inexistente.pdf");
    }

    @Test
    void deveDeletarArquivo() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
            .thenReturn(DeleteObjectResponse.builder().build());

        storageService.delete("docs/arquivo.pdf");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().key()).isEqualTo("docs/arquivo.pdf");
        assertThat(captor.getValue().bucket()).isEqualTo("test-bucket");
    }

    @Test
    void deveGerarUrlPresignadaComExpiracaoCorreta() throws MalformedURLException {
        URL fakeUrl = new URL("https://s3.amazonaws.com/test-bucket/docs/file.pdf?X-Amz-Signature=abc");
        PresignedGetObjectRequest presignedReq = mock(PresignedGetObjectRequest.class);
        when(presignedReq.url()).thenReturn(fakeUrl);
        when(presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedReq);

        URL result = storageService.generatePresignedDownloadUrl("docs/file.pdf");

        assertThat(result).isEqualTo(fakeUrl);
        verify(presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }
}
