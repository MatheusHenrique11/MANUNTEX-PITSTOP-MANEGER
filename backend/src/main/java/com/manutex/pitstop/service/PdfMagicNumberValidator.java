package com.manutex.pitstop.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Valida que o arquivo é um PDF genuíno inspecionando os primeiros bytes (Magic Numbers).
 *
 * Um PDF válido começa com os bytes: 25 50 44 46 ("%PDF").
 * Essa verificação é feita no conteúdo binário do arquivo, não na extensão ou
 * no Content-Type declarado pelo cliente — ambos são trivialmente falsificáveis.
 *
 * Sem isso, um atacante poderia renomear malware.exe para documento.pdf e
 * fazer upload normalmente, executando código no servidor.
 */
@Component
public class PdfMagicNumberValidator {

    // Assinatura binária de todo PDF válido: "%PDF"
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46};

    // Limite de tamanho: 10 MB
    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentException("Arquivo não pode ser vazio");
        }

        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidDocumentException("Arquivo excede o limite de 10 MB");
        }

        if (!hasPdfMagicNumber(file)) {
            throw new InvalidDocumentException(
                "O arquivo não é um PDF válido. Apenas documentos PDF são aceitos."
            );
        }
    }

    private boolean hasPdfMagicNumber(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[PDF_MAGIC.length];
            int bytesRead = is.readNBytes(header, 0, PDF_MAGIC.length);

            if (bytesRead < PDF_MAGIC.length) return false;

            for (int i = 0; i < PDF_MAGIC.length; i++) {
                if (header[i] != PDF_MAGIC[i]) return false;
            }
            return true;
        } catch (IOException e) {
            throw new InvalidDocumentException("Erro ao ler o arquivo: " + e.getMessage());
        }
    }

    public static class InvalidDocumentException extends RuntimeException {
        public InvalidDocumentException(String message) {
            super(message);
        }
    }
}
