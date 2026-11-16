package unoeste.fipp.backend.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pergunta_anuncio")
public class PerguntaAnuncio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "per_id")
    @JsonProperty("per_id")
    private Integer perId;

    @Column(name = "per_text")
    @JsonProperty("per_text")
    private String perText;

    @Column(name = "per_resp")
    @JsonProperty("per_resp")
    private String perResp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anu_id")
    @JsonIgnore
    private Anuncio anuncio;

    public PerguntaAnuncio() {
    }

    public Integer getPerId() {
        return perId;
    }

    public void setPerId(Integer perId) {
        this.perId = perId;
    }

    public String getPerText() {
        return perText;
    }

    public void setPerText(String perText) {
        this.perText = perText;
    }

    public String getPerResp() {
        return perResp;
    }

    public void setPerResp(String perResp) {
        this.perResp = perResp;
    }

    public Anuncio getAnuncio() {
        return anuncio;
    }

    public void setAnuncio(Anuncio anuncio) {
        this.anuncio = anuncio;
    }
}
