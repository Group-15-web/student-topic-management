package vn.edu.detaihub.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class ErrorAdvice {
  @ExceptionHandler({
    RuleException.class,
    AccessDeniedException.class,
    DataIntegrityViolationException.class,
    MethodArgumentNotValidException.class,
    MaxUploadSizeExceededException.class,
    org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
  })
  public Object error(Exception ex, HttpServletRequest req, HttpServletResponse res) {
    int status =
        ex instanceof RuleException r
            ? r.getStatus()
            : ex instanceof AccessDeniedException
                ? 403
                : ex instanceof DataIntegrityViolationException ? 409 : 400;
    String message =
        ex instanceof RuleException || ex instanceof AccessDeniedException
            ? ex.getMessage()
            : ex instanceof DataIntegrityViolationException
                ? "Không thể lưu/xóa: dữ liệu bị trùng hoặc đã có liên kết. Hãy khóa/lưu trữ thay vì xóa."
                : "Dữ liệu không hợp lệ hoặc file vượt quá 20 MB.";
    if (req.getRequestURI().startsWith("/api/"))
      return ResponseEntity.status(status).body(Map.of("status", status, "message", message));
    res.setStatus(status);
    var mv = new ModelAndView("error");
    mv.addObject("code", status);
    mv.addObject("message", message);
    return mv;
  }

  @ExceptionHandler(Exception.class)
  public Object generalError(Exception ex, HttpServletRequest req, HttpServletResponse res) {
    if (req.getRequestURI().startsWith("/api/"))
      return ResponseEntity.status(500).body(Map.of("status", 500, "message", "Lỗi máy chủ nội bộ: " + ex.getMessage()));
    res.setStatus(500);
    var mv = new ModelAndView("error");
    mv.addObject("code", 500);
    mv.addObject("message", ex.getMessage() != null && !ex.getMessage().isBlank()
        ? ex.getMessage()
        : "Đã xảy ra lỗi hệ thống. Vui lòng quay lại và thử lại.");
    return mv;
  }
}