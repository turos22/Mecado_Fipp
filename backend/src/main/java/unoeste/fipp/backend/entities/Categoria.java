package unoeste.fipp.backend.entities;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "categoria")
public class Categoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cat_id")
    @JsonProperty("cat_id")
    private Integer catId;

    @Column(name = "cat_name")
    @JsonProperty("cat_name")
    private String catName;

    public Categoria(Integer catId, String catName) {
        this.catId = catId;
        this.catName = catName;
    }
    public Categoria() {

    }

    public Integer getCatId() {
        return catId;
    }

    public void setCatId(Integer catId) {
        this.catId = catId;
    }

    public String getCatName() {
        return catName;
    }

    public void setCatName(String catName) {
        this.catName = catName;
    }
}
