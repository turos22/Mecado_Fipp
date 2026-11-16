package unoeste.fipp.backend;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import unoeste.fipp.backend.security.JWTTokenProvider;

// Rotas do usuário logado (apis/usuario): ver anúncios (get-id, get-filter) e gerir os PRÓPRIOS (meus, PUT meus/{id}).
// Dados 9xxx por SQL, desfeitos no fim.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioAnuncioRestControllerTest {

    private static final String BASE = "/apis/usuario";
    private static final int DONO = 9101, OUTRO = 9102, CAT = 9101, CAT2 = 9102;

    @Autowired
    MockMvc mvc;
    @Autowired
    EntityManager em;

    private void sql(String comando, Object... p) {
        var q = em.createNativeQuery(comando);
        for (int i = 0; i < p.length; i++) q.setParameter(i + 1, p[i]);
        q.executeUpdate();
    }

    @BeforeEach
    void dados() {
        sql("insert into categoria (cat_id, cat_name) values (?1, 'Tst Um'), (?2, 'Tst Dois')", CAT, CAT2);
        sql("insert into usuario (usr_id, usr_name, usr_pass, usr_level) values (?1, 'tstdono', 'x', '2'), (?2, 'tstoutro', 'x', '2')", DONO, OUTRO);
        sql("insert into anuncio (anu_id, anu_title, anu_date, anu_desc, anu_price, cat_id, usr_id) values "
                + "(9101, 'Meu velho', '2099-01-01', 'zzt desc', 10.0, ?1, ?2),"
                + "(9102, 'Meu novo', '2099-01-02', 'zzt desc', 20.0, ?1, ?2),"
                + "(9103, 'Do outro', '2099-01-03', 'zzt desc', 30.0, ?1, ?3)", CAT, DONO, OUTRO);
        sql("insert into foto_anuncio (fot_file, anu_id) values ('a.jpg', 9101)");
    }

    private ResultActions como(int usrId, MockHttpServletRequestBuilder r) throws Exception {
        return mvc.perform(r.header("Authorization", JWTTokenProvider.createToken("t" + usrId, "usuario", usrId)));
    }

    private MockHttpServletRequestBuilder editar(int id, String json) {
        return put(BASE + "/meus/" + id).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private String coluna(String sql) {
        return String.valueOf(em.createNativeQuery(sql).getSingleResult());
    }

    // ---- leitura geral

    @Test
    void usuarioComumListaTodosOsAnuncios() throws Exception {
        como(DONO, get(BASE + "/get-filter/zzt")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].anu_id", contains(9103, 9102, 9101)));
    }

    @Test
    void usuarioComumVeUmAnuncioPorId() throws Exception {
        como(OUTRO, get(BASE + "/get-id/9101")).andExpect(status().isOk())
                .andExpect(jsonPath("$.anu_title").value("Meu velho"))
                .andExpect(jsonPath("$.usr_name").value("tstdono"))
                .andExpect(jsonPath("$.fotos", hasSize(1)))
                .andExpect(content().string(not(containsString("usr_pass"))));
    }

    @Test
    void anuncioInexistenteDa404() throws Exception {
        como(DONO, get(BASE + "/get-id/99999")).andExpect(status().isNotFound());
    }

    // ---- meus

    @Test
    void meusDevolveSoOsAnunciosDeQuemEstaLogado() throws Exception {
        como(DONO, get(BASE + "/meus")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.anu_title=='Meu novo' || @.anu_title=='Meu velho')]", hasSize(2)))
                .andExpect(jsonPath("$[?(@.usr_id != " + DONO + ")]", hasSize(0)));
        como(OUTRO, get(BASE + "/meus")).andExpect(jsonPath("$[*].anu_id", contains(9103)));
    }

    @Test
    void meusNaoAceitaUsuarioPorParametro() throws Exception {
        // a identidade vem do token; ?usr_id= na URL não pode trocar de dono
        como(DONO, get(BASE + "/meus").param("usr_id", String.valueOf(OUTRO)).param("usrId", String.valueOf(OUTRO)))
                .andExpect(jsonPath("$[?(@.usr_id == " + OUTRO + ")]", hasSize(0)));
    }

    // ---- editar

    @Test
    void donoEditaTituloDescricaoPrecoECategoria() throws Exception {
        como(DONO, editar(9101, "{\"anu_title\":\"Novo título\",\"anu_desc\":\"Nova desc\",\"anu_price\":55.5,\"cat_id\":" + CAT2 + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anu_id").value(9101))
                .andExpect(jsonPath("$.anu_title").value("Novo título"))
                .andExpect(jsonPath("$.anu_price").value(55.5))
                .andExpect(jsonPath("$.cat_id").value(CAT2))
                .andExpect(jsonPath("$.cat_name").value("Tst Dois"));

        assertEquals("Novo título", coluna("select anu_title from anuncio where anu_id = 9101"));
        assertEquals("Nova desc", coluna("select anu_desc from anuncio where anu_id = 9101"));
        assertEquals("55.5", coluna("select anu_price from anuncio where anu_id = 9101"));
        assertEquals(String.valueOf(CAT2), coluna("select cat_id from anuncio where anu_id = 9101"));
    }

    @Test
    void editarNaoMexeEmDonoDataFotosNemPerguntas() throws Exception {
        como(DONO, editar(9101, "{\"anu_title\":\"X\",\"anu_desc\":\"Y\",\"anu_price\":1,\"cat_id\":" + CAT + ","
                + "\"usr_id\":" + OUTRO + ",\"anu_date\":\"2000-01-01\",\"fotos\":[],\"perguntas\":[{\"per_text\":\"inventada\"}]}"))
                .andExpect(status().isOk());

        assertEquals(String.valueOf(DONO), coluna("select usr_id from anuncio where anu_id = 9101"));
        assertEquals("2099-01-01", coluna("select anu_date from anuncio where anu_id = 9101"));
        assertEquals("1", coluna("select count(*) from foto_anuncio where anu_id = 9101"));
        assertEquals("0", coluna("select count(*) from pergunta_anuncio where anu_id = 9101"));
    }

    @Test
    void ninguemEditaAnuncioDeOutroEResponde404SemMudarNada() throws Exception {
        como(DONO, editar(9103, "{\"anu_title\":\"Roubado\",\"anu_desc\":\"d\",\"anu_price\":1,\"cat_id\":" + CAT + "}"))
                .andExpect(status().isNotFound());
        assertEquals("Do outro", coluna("select anu_title from anuncio where anu_id = 9103"));
    }

    @Test
    void editarAnuncioInexistenteDa404() throws Exception {
        como(DONO, editar(99999, "{\"anu_title\":\"a\",\"anu_desc\":\"d\",\"anu_price\":1,\"cat_id\":" + CAT + "}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void tituloVazioOuLongoDemaisOuPrecoInvalidoOuCategoriaInexistenteDao400SemMudarNada() throws Exception {
        String ok = "\"anu_desc\":\"d\",\"anu_price\":1,\"cat_id\":" + CAT;
        como(DONO, editar(9101, "{\"anu_title\":\"  \"," + ok + "}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.tipo").exists()).andExpect(jsonPath("$.texto").exists());
        como(DONO, editar(9101, "{\"anu_title\":\"" + "a".repeat(81) + "\"," + ok + "}")).andExpect(status().isBadRequest());
        como(DONO, editar(9101, "{\"anu_title\":\"t\",\"anu_desc\":\"d\",\"anu_price\":-1,\"cat_id\":" + CAT + "}")).andExpect(status().isBadRequest());
        como(DONO, editar(9101, "{\"anu_title\":\"t\",\"anu_desc\":\"d\",\"anu_price\":1000000000,\"cat_id\":" + CAT + "}")).andExpect(status().isBadRequest());
        como(DONO, editar(9101, "{\"anu_title\":\"t\",\"anu_desc\":\"d\",\"cat_id\":" + CAT + "}")).andExpect(status().isBadRequest());
        como(DONO, editar(9101, "{\"anu_title\":\"t\",\"anu_desc\":\"d\",\"anu_price\":1,\"cat_id\":987654}")).andExpect(status().isBadRequest());
        como(DONO, editar(9101, "{\"anu_title\":\"t\",\"anu_desc\":\"d\",\"anu_price\":1}")).andExpect(status().isBadRequest());
        como(DONO, editar(9101, "isto não é json")).andExpect(status().isBadRequest());

        assertEquals("Meu velho", coluna("select anu_title from anuncio where anu_id = 9101"));
    }

    @Test
    void tituloEEspacoNasPontasSaiAparado() throws Exception {
        como(DONO, editar(9101, "{\"anu_title\":\"  Aparado  \",\"anu_desc\":\"d\",\"anu_price\":1,\"cat_id\":" + CAT + "}"))
                .andExpect(jsonPath("$.anu_title").value("Aparado"));
    }

    @Test
    void semTokenNaoEditaNemListaMeus() throws Exception {
        mvc.perform(put(BASE + "/meus/9101").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mvc.perform(get(BASE + "/meus")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminTambemSoEditaOQueEDele() throws Exception {
        mvc.perform(editar(9101, "{\"anu_title\":\"a\",\"anu_desc\":\"d\",\"anu_price\":1,\"cat_id\":" + CAT + "}")
                .header("Authorization", JWTTokenProvider.createToken("adm", "admin", 1))).andExpect(status().isNotFound());
    }
}
