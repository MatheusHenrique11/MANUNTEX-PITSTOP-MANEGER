package com.manutex.pitstop.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.*;

class PdfMagicNumberValidatorTest {

    private PdfMagicNumberValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PdfMagicNumberValidator();
    }

    @Test
    void deveAceitarPdfValido() {
        byte[] pdfBytes = "%PDF-1.4 conteudo qualquer".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", pdfBytes);

        assertThatNoException().isThrownBy(() -> validator.validate(file));
    }

    @Test
    void deveRejeitarArquivoVazio() {
        MockMultipartFile file = new MockMultipartFile("file", "vazio.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(PdfMagicNumberValidator.InvalidDocumentException.class)
            .hasMessageContaining("vazio");
    }

    @Test
    void deveRejeitarExeDisfarçadoDePdf() {
        // MZ = cabeçalho de executável Windows
        byte[] exeBytes = new byte[]{0x4D, 0x5A, 0x00, 0x00, 0x50, 0x45};
        MockMultipartFile file = new MockMultipartFile("file", "malware.pdf", "application/pdf", exeBytes);

        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(PdfMagicNumberValidator.InvalidDocumentException.class)
            .hasMessageContaining("PDF válido");
    }

    @Test
    void deveRejeitarArquivoMaiorQue10Mb() {
        byte[] grande = new byte[11 * 1024 * 1024];
        grande[0] = 0x25; grande[1] = 0x50; grande[2] = 0x44; grande[3] = 0x46; // %PDF
        MockMultipartFile file = new MockMultipartFile("file", "grande.pdf", "application/pdf", grande);

        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(PdfMagicNumberValidator.InvalidDocumentException.class)
            .hasMessageContaining("10 MB");
    }

    @Test
    void deveRejeitarImagemJpegDisfarçadaDePdf() {
        // FF D8 FF = cabeçalho JPEG
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};
        MockMultipartFile file = new MockMultipartFile("file", "foto.pdf", "application/pdf", jpegBytes);

        assertThatThrownBy(() -> validator.validate(file))
            .isInstanceOf(PdfMagicNumberValidator.InvalidDocumentException.class);
    }
}
