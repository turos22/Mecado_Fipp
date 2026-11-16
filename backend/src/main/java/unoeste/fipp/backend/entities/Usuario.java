package unoeste.fipp.backend.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @Column(name = "usr_id")
    @JsonProperty("usr_id")
    private Integer usrId;

    @Column(name = "usr_name", length = 20)
    @JsonProperty("usr_name")
    private String usrName;

    //guarda o hash BCrypt (60 caracteres), nunca a senha; entra pelo JSON mas nunca sai dele
    @Column(name = "usr_pass", length = 60)
    @JsonProperty(value = "usr_pass", access = JsonProperty.Access.WRITE_ONLY)
    private String usrPass;

    //'1' = administrador, '2' = usuário comum
    @Column(name = "usr_level", length = 1)
    @JsonProperty("usr_level")
    private String usrLevel;

    public Usuario() {
    }

    public Integer getUsrId() {
        return usrId;
    }

    public void setUsrId(Integer usrId) {
        this.usrId = usrId;
    }

    public String getUsrName() {
        return usrName;
    }

    public void setUsrName(String usrName) {
        this.usrName = usrName;
    }

    public String getUsrPass() {
        return usrPass;
    }

    public void setUsrPass(String usrPass) {
        this.usrPass = usrPass;
    }

    public String getUsrLevel() {
        return usrLevel;
    }

    public void setUsrLevel(String usrLevel) {
        this.usrLevel = usrLevel;
    }
}
