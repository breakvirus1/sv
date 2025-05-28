
package com.example.test.printsv.response;

import com.example.test.printsv.entity.Material;

import io.micrometer.common.lang.NonNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SubZakazResponse {
    @NonNull
    private Material material;
    @NonNull
    private Double lenght;
    @NonNull
    private Double width;
    private String filePath;
    @NonNull
    private Integer cena;
    private String comment;

    private boolean done;
}
