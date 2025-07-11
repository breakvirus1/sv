package com.example.test.printsv.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubZakazRequest {
    private String name;
    private Double length;
    private Double width;
    private String filePath;
    private String comment;
    private Integer cena;
    private Boolean done;
}