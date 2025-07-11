package com.example.test.printsv.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubZakazResponse {
    private Long id;
    private String name;
    private Double length;
    private Double width;
    private String filePath;
    private String comment;
    private Integer cena;
    private Boolean done;
    private Long zakazId;
}