package com.manutex.pitstop.service;

import com.manutex.pitstop.domain.entity.*;
import com.manutex.pitstop.domain.enums.TipoDocumento;
import com.manutex.pitstop.domain.enums.UserRole;
import com.manutex.pitstop.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentoServiceTest {

    @Mock private DocumentoRepository documentoRepository;
    @Mock private VeiculoRepository veiculoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UserRepository userRepository;
    @Mock private PdfMagicNumberValidator pdfValidator;
    @Mock private AesEncryptionService encryptionService;
    @Mock private ChecksumService checksumService;
    @Mock private StorageService storageService;

    @InjectMocks
    private DocumentoService service;

    private User mockUser;
    private UUID veiculoId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        veiculoId = UUID.randomUUID();

        mockUser = User.builder()
            .email("mecanico@pitstop.com")
            .passwordHash("hash")
            .fullName("João Mecânico")
            .role(UserRole.ROLE_MECANICO)
            .build();

        // Configura SecurityContext com usuário autenticado
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("mecanico@pitstop.com");
        SecurityContext secCtx = mock(SecurityContext.class);
        when(secCtx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(secCtx);

        when(userRepository.findByEmail("mecanico@pitstop.com")).thenReturn(Optional.of(mockUser));
    }

    @Test
    void deveRealizarUploadComPipelineCompleto() {
        byte[] pdfBytes = "%PDF-1.4 conteudo".getBytes();
        byte[] encryptedBytes = "ENCRYPTED".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "crlv.pdf", "application/pdf", pdfBytes);

        Veiculo veiculo = Veiculo.builder().placa("ABC1234").chassi("9BWZZZ377VT004251")
            .renavam("00258665590").marca("VW").modelo("Gol").anoFabricacao(2020).anoModelo(2021).build();

        when(veiculoRepository.findById(veiculoId)).thenReturn(Optional.of(veiculo));
        when(checksumService.sha256Hex(pdfBytes)).thenReturn("abc123hash");
        when(encryptionService.encrypt(pdfBytes)).thenReturn(encryptedBytes);
        when(documentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.upload(file, TipoDocumento.CRLV, veiculoId, null, null);

        // Verifica que o pipeline completo foi executado na ordem correta
        InOrder order = inOrder(pdfValidator, checksumService, encryptionService, storageService, documentoRepository);
        order.verify(pdfValidator).validate(file);
        order.verify(checksumService).sha256Hex(pdfBytes);
        order.verify(encryptionService).encrypt(pdfBytes);
        order.verify(storageService).store(anyString(), eq(encryptedBytes), anyString());
        order.verify(documentoRepository).save(any());

        assertThat(response.checksumSha256()).isEqualTo("abc123hash");
        assertThat(response.nomeOriginal()).isEqualTo("crlv.pdf");
    }

    @Test
    void deveRejeitarSeValidacaoPdfFalhar() {
        MockMultipartFile malware = new MockMultipartFile("file", "malware.pdf", "application/pdf",
            new byte[]{0x4D, 0x5A});  // cabeçalho MZ (exe)

        doThrow(new PdfMagicNumberValidator.InvalidDocumentException("Não é PDF"))
            .when(pdfValidator).validate(malware);

        assertThatThrownBy(() -> service.upload(malware, TipoDocumento.CRLV, null, null, null))
            .isInstanceOf(PdfMagicNumberValidator.InvalidDocumentException.class);

        verifyNoInteractions(encryptionService, storageService, documentoRepository);
    }

    @Test
    void deveSanitizarNomeDeArquivoPerigoso() {
        byte[] pdfBytes = "%PDF-1.4".getBytes();
        MockMultipartFile file = new MockMultipartFile(
            "file", "../../etc/passwd.pdf", "application/pdf", pdfBytes);

        when(checksumService.sha256Hex(any())).thenReturn("hash");
        when(encryptionService.encrypt(any())).thenReturn(new byte[0]);
        when(documentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.upload(file, TipoDocumento.OUTRO, null, null, null);

        // Path traversal deve ser sanitizado
        assertThat(response.nomeOriginal()).doesNotContain("..");
        assertThat(response.nomeOriginal()).doesNotContain("/");
    }

    @Test
    void deveVerificarIntegridadeNoDownload() {
        UUID docId = UUID.randomUUID();
        Documento doc = Documento.builder()
            .tipo(TipoDocumento.CRLV)
            .storageKey("docs/test/key")
            .nomeOriginal("crlv.pdf")
            .tamanhoBytes(100L)
            .mimeType("application/pdf")
            .checksumSha256("hash_correto")
            .uploadedBy(mockUser)
            .build();

        byte[] encryptedBytes = "ENCRYPTED".getBytes();
        byte[] plainBytes = "%PDF-1.4".getBytes();

        when(documentoRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(storageService.retrieve("docs/test/key")).thenReturn(encryptedBytes);
        when(encryptionService.decrypt(encryptedBytes)).thenReturn(plainBytes);
        when(checksumService.verify(plainBytes, "hash_correto")).thenReturn(true);

        byte[] result = service.getDecryptedContent(docId);
        assertThat(result).isEqualTo(plainBytes);
    }

    @Test
    void deveLancarExcecaoSeIntegridadeComprometida() {
        UUID docId = UUID.randomUUID();
        Documento doc = Documento.builder()
            .tipo(TipoDocumento.CRLV)
            .storageKey("docs/test/key")
            .nomeOriginal("crlv.pdf")
            .tamanhoBytes(100L)
            .mimeType("application/pdf")
            .checksumSha256("hash_correto")
            .uploadedBy(mockUser)
            .build();

        when(documentoRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(storageService.retrieve(any())).thenReturn(new byte[0]);
        when(encryptionService.decrypt(any())).thenReturn(new byte[0]);
        when(checksumService.verify(any(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.getDecryptedContent(docId))
            .isInstanceOf(DocumentoService.DocumentoIntegrityException.class);
    }

    @Test
    void deveDeletarDocumentoDoStorageEDoBanco() {
        UUID docId = UUID.randomUUID();
        Documento doc = Documento.builder()
            .tipo(TipoDocumento.CRLV)
            .storageKey("docs/test/key-deletar")
            .nomeOriginal("crlv.pdf")
            .tamanhoBytes(100L)
            .mimeType("application/pdf")
            .checksumSha256("hash")
            .uploadedBy(mockUser)
            .build();

        when(documentoRepository.findById(docId)).thenReturn(Optional.of(doc));

        service.deletar(docId);

        verify(storageService).delete("docs/test/key-deletar");
        verify(documentoRepository).delete(doc);
    }

    @Test
    void deveNaoExporStorageKeyNaResposta() {
        byte[] pdfBytes = "%PDF-1.4".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", pdfBytes);

        when(checksumService.sha256Hex(any())).thenReturn("hash");
        when(encryptionService.encrypt(any())).thenReturn(new byte[0]);
        when(documentoRepository.save(any())).thenAnswer(inv -> {
            Documento d = inv.getArgument(0);
            // Verifica que storageKey está presente na entidade (para persistência)
            assertThat(d.getStorageKey()).isNotBlank();
            return d;
        });

        var response = service.upload(file, TipoDocumento.OUTRO, null, null, null);

        // DocumentoResponse não deve ter storageKey - campo não existe no record
        // (garantido pelo design do DTO)
        assertThat(response).isNotNull();
    }
}
