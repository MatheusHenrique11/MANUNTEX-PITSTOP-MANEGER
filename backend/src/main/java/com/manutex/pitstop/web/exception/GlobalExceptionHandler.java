package com.manutex.pitstop.web.exception;

import com.manutex.pitstop.service.AuthService;
import com.manutex.pitstop.service.AesEncryptionService;
import com.manutex.pitstop.service.DocumentoService;
import com.manutex.pitstop.service.PdfMagicNumberValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centraliza o mapeamento de exceções para respostas HTTP seguindo RFC 7807 (ProblemDetail).
 *
 * IMPORTANTE: mensagens de erro nunca expõem stack traces ou detalhes internos ao cliente.
 * Logs completos ficam no servidor.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "inválido",
                (a, b) -> a
            ));

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problem.setTitle("Dados inválidos");
        problem.setType(URI.create("https://pitstop.manutex.com/errors/validation"));
        problem.setProperty("fields", errors);
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.unprocessableEntity().body(problem);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(EntityNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Recurso não encontrado");
        problem.setDetail(ex.getMessage());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problem.setTitle("Acesso negado");
        problem.setDetail("Você não tem permissão para acessar este recurso.");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    @ExceptionHandler(AuthService.InvalidCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleInvalidCredentials(AuthService.InvalidCredentialsException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setTitle("Não autorizado");
        problem.setDetail(ex.getMessage());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    @ExceptionHandler(PdfMagicNumberValidator.InvalidDocumentException.class)
    public ResponseEntity<ProblemDetail> handleInvalidDocument(PdfMagicNumberValidator.InvalidDocumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        problem.setTitle("Documento inválido");
        problem.setDetail(ex.getMessage());
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.unprocessableEntity().body(problem);
    }

    @ExceptionHandler(DocumentoService.DocumentoIntegrityException.class)
    public ResponseEntity<ProblemDetail> handleIntegrity(DocumentoService.DocumentoIntegrityException ex) {
        // Log crítico no servidor, mensagem genérica para o cliente
        log.error("ALERTA DE SEGURANÇA - falha de integridade de documento: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Erro de integridade");
        problem.setDetail("Não foi possível processar o documento. Contate o suporte.");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.internalServerError().body(problem);
    }

    @ExceptionHandler(AesEncryptionService.EncryptionException.class)
    public ResponseEntity<ProblemDetail> handleEncryption(AesEncryptionService.EncryptionException ex) {
        log.error("Falha de criptografia: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Erro interno");
        problem.setDetail("Não foi possível processar o documento.");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.internalServerError().body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex) {
        log.error("Erro não tratado: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setTitle("Erro interno do servidor");
        problem.setDetail("Ocorreu um erro inesperado. Tente novamente.");
        problem.setProperty("timestamp", Instant.now());
        return ResponseEntity.internalServerError().body(problem);
    }
}
