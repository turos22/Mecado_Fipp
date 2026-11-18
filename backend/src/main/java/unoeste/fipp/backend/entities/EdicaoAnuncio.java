package unoeste.fipp.backend.entities;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EdicaoAnuncio(
        @JsonProperty("anu_title") String titulo,
        @JsonProperty("anu_desc") String descricao,
        @JsonProperty("anu_price") BigDecimal preco,
        @JsonProperty("cat_id") Integer categoria) {
}
