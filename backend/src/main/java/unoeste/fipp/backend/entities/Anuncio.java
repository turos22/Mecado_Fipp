package unoeste.fipp.backend.entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Formula;

@Entity
@Table(name = "anuncio")
public class Anuncio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "anu_id")
    @JsonProperty("anu_id")
    private Integer anuId;

    @Column(name = "anu_title", length = 80)
    @JsonProperty("anu_title")
    private String anuTitle;

    @Column(name = "anu_date")
    @JsonProperty("anu_date")
    private LocalDate anuDate;

    @Column(name = "anu_desc")
    @JsonProperty("anu_desc")
    private String anuDesc;

    @Column(name = "anu_price", precision = 10, scale = 1)
    @JsonProperty("anu_price")
    private BigDecimal anuPrice;

    @Column(name = "cat_id")
    @JsonProperty("cat_id")
    private Integer catId;

    @Column(name = "usr_id")
    @JsonProperty("usr_id")
    private Integer usrId;

    @Formula("(select c.cat_name from categoria c where c.cat_id = cat_id)")
    @JsonProperty(value = "cat_name", access = JsonProperty.Access.READ_ONLY)
    private String catName;

    @Formula("(select u.usr_name from usuario u where u.usr_id = usr_id)")
    @JsonProperty(value = "usr_name", access = JsonProperty.Access.READ_ONLY)
    private String usrName;

    @OneToMany(mappedBy = "anuncio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fotId")
    @Fetch(FetchMode.SUBSELECT)
    @JsonProperty("fotos")
    private List<FotoAnuncio> fotos = new ArrayList<>();

    @OneToMany(mappedBy = "anuncio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("perId")
    @Fetch(FetchMode.SUBSELECT)
    @JsonProperty("perguntas")
    private List<PerguntaAnuncio> perguntas = new ArrayList<>();

    public Anuncio() {
    }

    public Integer getAnuId() {
        return anuId;
    }

    public void setAnuId(Integer anuId) {
        this.anuId = anuId;
    }

    public String getAnuTitle() {
        return anuTitle;
    }

    public void setAnuTitle(String anuTitle) {
        this.anuTitle = anuTitle;
    }

    public LocalDate getAnuDate() {
        return anuDate;
    }

    public void setAnuDate(LocalDate anuDate) {
        this.anuDate = anuDate;
    }

    public String getAnuDesc() {
        return anuDesc;
    }

    public void setAnuDesc(String anuDesc) {
        this.anuDesc = anuDesc;
    }

    public BigDecimal getAnuPrice() {
        return anuPrice;
    }

    public void setAnuPrice(BigDecimal anuPrice) {
        this.anuPrice = anuPrice;
    }

    public Integer getCatId() {
        return catId;
    }

    public void setCatId(Integer catId) {
        this.catId = catId;
    }

    public Integer getUsrId() {
        return usrId;
    }

    public void setUsrId(Integer usrId) {
        this.usrId = usrId;
    }

    public String getCatName() {
        return catName;
    }

    public String getUsrName() {
        return usrName;
    }

    public List<FotoAnuncio> getFotos() {
        return fotos;
    }

    public void setFotos(List<FotoAnuncio> fotos) {
        this.fotos = fotos;
    }

    public List<PerguntaAnuncio> getPerguntas() {
        return perguntas;
    }

    public void setPerguntas(List<PerguntaAnuncio> perguntas) {
        this.perguntas = perguntas;
    }
}
