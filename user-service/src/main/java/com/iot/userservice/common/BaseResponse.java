package com.iot.userservice.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BaseResponse <T>{
    private T data;

    private String message;

    public static <T> BaseResponse<T> success(T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setData(data);
        response.setMessage("Success");
        return response;
    }

    public static <T> BaseResponse<T> success(T data, String message) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setData(data);
        response.setMessage(message);
        return response;
    }
}

