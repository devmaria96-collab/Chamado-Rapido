package br.com.chamadorapido;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
@RestControllerAdvice
public class ErrorHandler {
  private static final Logger LOG=LoggerFactory.getLogger(ErrorHandler.class);
  @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Não foi possível concluir.":e.getReason()));}
  @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e){return ResponseEntity.badRequest().body(Map.of("message","Confira os campos obrigatórios e os limites informados.","fields",e.getBindingResult().getFieldErrors().stream().map(f->f.getField()+": "+f.getDefaultMessage()).toList()));}
  @ExceptionHandler({IllegalArgumentException.class,org.springframework.http.converter.HttpMessageNotReadableException.class,org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class}) ResponseEntity<?> invalid(Exception e){return ResponseEntity.badRequest().body(Map.of("message","Dados inválidos. Confira sua solicitação."));}
  @ExceptionHandler(OptimisticLockingFailureException.class) ResponseEntity<?> conflict(Exception e){return ResponseEntity.status(409).body(Map.of("message","Este registro foi atualizado por outra pessoa. Reabra o chamado antes de salvar."));}
  @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> duplicate(Exception e){return ResponseEntity.status(409).body(Map.of("message","Registro duplicado ou referência inválida. Confira os dados."));}
  @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<?> large(Exception e){return ResponseEntity.status(413).body(Map.of("message","O arquivo deve ter no máximo 5 MB."));}
  @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class) ResponseEntity<?> notFound(Exception e){return ResponseEntity.status(404).body(Map.of("message","Recurso nao encontrado."));}
  @ExceptionHandler(Exception.class) ResponseEntity<?> unexpected(Exception e){LOG.error("Falha inesperada no atendimento",e);return ResponseEntity.status(500).body(Map.of("message","Não foi possível concluir. Tente novamente ou consulte o administrador."));}
}
