package com.comanda.exception;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String, Object>> api(ApiException e) { return corpo(e.getStatus(), e.getMessage()); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validacao(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).collect(Collectors.joining("; "));
        return corpo(HttpStatus.BAD_REQUEST, msg);
    }

    // Parâmetro da URL com valor que não existe (ex.: ?status=ABC ou ?dataInicio=ontem)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Map<String, Object>> parametro(MethodArgumentTypeMismatchException e) {
        return corpo(HttpStatus.BAD_REQUEST, "Valor inválido para o parâmetro '" + e.getName() + "'.");
    }

    // Corpo da requisição que não é um JSON válido (ex.: {"status": "ABC"})
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> corpoInvalido(HttpMessageNotReadableException e) {
        return corpo(HttpStatus.BAD_REQUEST, "Dados enviados em formato inválido.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> negado(AccessDeniedException e) { return corpo(HttpStatus.FORBIDDEN, "Sem permissão para esta ação."); }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> geral(Exception e) {
        log.error("Erro inesperado", e); // stack trace só no log do servidor
        return corpo(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno. Tente novamente.");
    }

    private ResponseEntity<Map<String, Object>> corpo(HttpStatus s, String msg) {
        return ResponseEntity.status(s).body(Map.of("status", s.value(), "message", msg, "timestamp", LocalDateTime.now().toString()));
    }
}
