package unoeste.fipp.backend.entities;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

// Só os campos que o dono pode alterar. Dono, data, fotos e perguntas que vierem no JSON são ignorados.
public record EdicaoAnuncio(
        @JsonProperty("anu_title") String titulo,
        @JsonProperty("anu_desc") String descricao,
        @JsonProperty("anu_price") BigDecimal preco,
        @JsonProperty("cat_id") Integer categoria) {
}
