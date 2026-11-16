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
@Table(name = "foto_anuncio")
public class FotoAnuncio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fot_id")
    @JsonProperty("fot_id")
    private Integer fotId;

    @Column(name = "fot_file", length = 20)
    @JsonProperty("fot_file")
    private String fotFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anu_id")
    @JsonIgnore
    private Anuncio anuncio;

    public FotoAnuncio() {
    }

    public Integer getFotId() {
        return fotId;
    }

    public void setFotId(Integer fotId) {
        this.fotId = fotId;
    }

    public String getFotFile() {
        return fotFile;
    }

    public void setFotFile(String fotFile) {
        this.fotFile = fotFile;
    }

    public Anuncio getAnuncio() {
        return anuncio;
    }

    public void setAnuncio(Anuncio anuncio) {
        this.anuncio = anuncio;
    }
}
