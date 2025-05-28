package com.example.test.printsv.request;

import com.example.test.printsv.entity.Material;

import io.micrometer.common.lang.NonNull;
import lombok.*;


@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SubZakazRequest {
    @NonNull
    private Material material;
    @NonNull
    private Double lenght;
    @NonNull
    private Double width;
    private String filePath;
    @NonNull
    private Integer cena;
    @NonNull
    private boolean done;

}
