package com.example.deviceservice.config;

import com.example.deviceservice.common.BaseResponse;
import com.example.deviceservice.exception.ApplicationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 1. Bắt lỗi Validation (Dữ liệu không thỏa mãn các điều kiện @NotBlank, @Size, @Min, @Max...)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String combinedMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        BaseResponse<Object> response = BaseResponse.error(HttpStatus.BAD_REQUEST.value(), combinedMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 2. Bắt lỗi thiếu dữ liệu Request Body (Client quên không gửi Body hoặc sai cấu trúc JSON)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        BaseResponse<Object> response = BaseResponse.error(
                HttpStatus.BAD_REQUEST.value(),
                "Dữ liệu gửi lên không hợp lệ hoặc thiếu Request Body"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 3. Bắt lỗi sai kiểu dữ liệu trên URL (Ví dụ: truyền chữ vào tham số dạng Number/Integer)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<Object>> handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String message = String.format("Tham số '%s' nhận giá trị không hợp lệ: '%s'", ex.getName(), ex.getValue());
        BaseResponse<Object> response = BaseResponse.error(HttpStatus.BAD_REQUEST.value(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 4. Bắt lỗi Logic / Nghiệp vụ tự định nghĩa của hệ thống
    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<BaseResponse<Object>> handleApplicationException(ApplicationException ex) {
        BaseResponse<Object> response = BaseResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 5. BẮT LỖI CUỐI CÙNG: Các lỗi hệ thống không lường trước được (NullPointer, SQL Error...)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<BaseResponse<Object>> handleRuntimeException(RuntimeException ex) {
        // 💡 Ghi lại chi tiết lỗi vào log file để dễ dàng Debug tìm lỗi sau này
        log.error("Hệ thống xảy ra ngoại lệ nghiêm trọng: ", ex);

        BaseResponse<Object> response = BaseResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Có lỗi nội bộ xảy ra, vui lòng thử lại sau!"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}