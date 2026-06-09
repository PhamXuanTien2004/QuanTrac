package com.example.deviceservice.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class BaseSearchRequest {
    private int page = 0;
    private int size = 10;
    private String sortBy = "createdDate";
    private String sortDir = "desc";
}
