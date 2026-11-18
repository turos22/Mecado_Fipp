package unoeste.fipp.backend;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import unoeste.fipp.backend.security.JWTTokenProvider;

// Rotas do usuário logado (apis/usuario): ver anúncios (get-id, get-filter) e gerir os PRÓPRIOS (meus, PUT meus/{id}).
// Dados 9xxx por SQL, desfeitos no fim.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioAnuncioRestControllerTest {

    private static final String BASE = "/apis/usuario";
    private static final Path PASTA_FOTOS = Path.of("uploads", "fotos");
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

    // fotos gravadas pelos testes saem do disco (o banco é desfeito pelo @Transactional, o disco não)
    @AfterEach
    void limparFotos() throws Exception {
        for (Object nome : em.createNativeQuery("select fot_file from foto_anuncio where anu_id in (9101, 9102, 9103)").getResultList())
            Files.deleteIfExists(PASTA_FOTOS.resolve(String.valueOf(nome)));
    }

    private ResultActions como(int usrId, MockHttpServletRequestBuilder r) throws Exception {
        return mvc.perform(r.header("Authorization", JWTTokenProvider.createToken("t" + usrId, "usuario", usrId)));
    }

    // no Spring 7 o builder de multipart não herda de MockHttpServletRequestBuilder
    private ResultActions como(int usrId, MockMultipartHttpServletRequestBuilder r) throws Exception {
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

    // ---- POST de anúncio

    private MockHttpServletRequestBuilder novo(String json) {
        return post(BASE).contentType(MediaType.APPLICATION_JSON).content(json);
    }

    @Test
    void criaAnuncioDoUsuarioDoTokenComDataDeHoje() throws Exception {
        como(DONO, novo("{\"anu_title\":\"  Criado  \",\"anu_desc\":\"zzt novo\",\"anu_price\":12.34,\"cat_id\":" + CAT + ","
                + "\"usr_id\":" + OUTRO + ",\"anu_date\":\"2000-01-01\",\"fotos\":[],\"perguntas\":[{\"per_text\":\"inventada\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anu_id").isNumber())
                .andExpect(jsonPath("$.anu_title").value("Criado"))
                .andExpect(jsonPath("$.anu_price").value(12.3))
                .andExpect(jsonPath("$.usr_id").value(DONO))
                .andExpect(jsonPath("$.usr_name").value("tstdono"))
                .andExpect(jsonPath("$.cat_name").value("Tst Um"))
                .andExpect(jsonPath("$.anu_date").value(java.time.LocalDate.now().toString()))
                .andExpect(jsonPath("$.fotos", hasSize(0)))
                .andExpect(jsonPath("$.perguntas", hasSize(0)));

        assertEquals("1", coluna("select count(*) from anuncio where anu_title = 'Criado' and usr_id = " + DONO));
        assertEquals("0", coluna("select count(*) from pergunta_anuncio where per_text = 'inventada'"));
    }

    @Test
    void criarComDadoInvalidoDa400SemGravar() throws Exception {
        como(DONO, novo("{\"anu_title\":\" \",\"anu_desc\":\"d\",\"anu_price\":1,\"cat_id\":" + CAT + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.tipo").exists()).andExpect(jsonPath("$.texto").exists());
        como(DONO, novo("{\"anu_title\":\"t\",\"anu_desc\":\"d\",\"anu_price\":-1,\"cat_id\":" + CAT + "}")).andExpect(status().isBadRequest());
        como(DONO, novo("{\"anu_title\":\"t\",\"anu_desc\":\"d\",\"anu_price\":1,\"cat_id\":987654}")).andExpect(status().isBadRequest());
        como(DONO, novo("isto não é json")).andExpect(status().isBadRequest());
        assertEquals("0", coluna("select count(*) from anuncio where anu_title = 't'"));
    }

    // ---- POST fotos (addFoto)

    private MockMultipartHttpServletRequestBuilder fotos(String id, MockMultipartFile... arquivos) {
        var r = multipart(BASE + "/addFoto").param("id", id);
        for (MockMultipartFile a : arquivos) r.file(a);
        return r;
    }

    private MockMultipartFile arquivo(String tipo) {
        return new MockMultipartFile("fotos", "../../x.bin", tipo, new byte[]{1, 2, 3});
    }

    @Test
    void donoEnviaFotosEElasFicamNoDiscoENoAnuncio() throws Exception {
        como(DONO, fotos("9102", arquivo("image/jpeg"), arquivo("image/png"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.anu_id").value(9102))
                .andExpect(jsonPath("$.fotos", hasSize(2)))
                .andExpect(jsonPath("$.fotos[0].fot_file").value(org.hamcrest.Matchers.endsWith(".jpg")))
                .andExpect(jsonPath("$.fotos[1].fot_file").value(org.hamcrest.Matchers.endsWith(".png")));

        List<?> nomes = em.createNativeQuery("select fot_file from foto_anuncio where anu_id = 9102").getResultList();
        assertEquals(2, nomes.size());
        for (Object nome : nomes) assertTrue(Files.exists(PASTA_FOTOS.resolve(String.valueOf(nome))));
    }

    @Test
    void fotoDeAnuncioDeOutroOuInexistenteDa404SemGravar() throws Exception {
        como(DONO, fotos("9103", arquivo("image/jpeg"))).andExpect(status().isNotFound());
        como(DONO, fotos("99999", arquivo("image/jpeg"))).andExpect(status().isNotFound());
        assertEquals("0", coluna("select count(*) from foto_anuncio where anu_id = 9103"));
    }

    @Test
    void fotoInvalidaDa400SemGravarNada() throws Exception {
        como(DONO, fotos("9102", arquivo("image/jpeg"), arquivo("text/html"))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.texto").exists());
        como(DONO, fotos("9102")).andExpect(status().isBadRequest());
        como(DONO, fotos("9102", new MockMultipartFile("fotos", "v.jpg", "image/jpeg", new byte[0]))).andExpect(status().isBadRequest());
        assertEquals("0", coluna("select count(*) from foto_anuncio where anu_id = 9102"));
    }

    // ---- DELETE do próprio anúncio

    @Test
    void donoExcluiOProprioAnuncioComFotosEArquivos() throws Exception {
        como(DONO, fotos("9101", arquivo("image/gif"))).andExpect(status().isOk());
        String nome = coluna("select fot_file from foto_anuncio where anu_id = 9101 and fot_file <> 'a.jpg'");
        assertTrue(Files.exists(PASTA_FOTOS.resolve(nome)));

        como(DONO, delete(BASE).param("id", "9101")).andExpect(status().isOk());
        em.flush();

        assertEquals("0", coluna("select count(*) from anuncio where anu_id = 9101"));
        assertEquals("0", coluna("select count(*) from foto_anuncio where anu_id = 9101"));
        assertFalse(Files.exists(PASTA_FOTOS.resolve(nome)));
        assertEquals("1", coluna("select count(*) from anuncio where anu_id = 9102"));
    }

    @Test
    void ninguemExcluiAnuncioDeOutroNemInexistente() throws Exception {
        como(DONO, delete(BASE).param("id", "9103")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.tipo").exists());
        como(DONO, delete(BASE).param("id", "99999")).andExpect(status().isNotFound());
        mvc.perform(delete(BASE).param("id", "9101").header("Authorization", JWTTokenProvider.createToken("adm", "admin", 1)))
                .andExpect(status().isNotFound()); //admin por esta rota também só exclui o que é dele
        assertEquals("3", coluna("select count(*) from anuncio where anu_id in (9101, 9102, 9103)"));
    }

    // ---- POST pergunta (ask)

    private MockHttpServletRequestBuilder ask(String id, String texto) {
        var r = post(BASE + "/ask").param("id", id);
        return texto == null ? r : r.param("texto", texto);
    }

    @Test
    void perguntaEmAnuncioDeOutroFicaGravada() throws Exception {
        como(OUTRO, ask("9101", "  Aceita troca?  ")).andExpect(status().isOk());
        assertEquals("Aceita troca?", coluna("select per_text from pergunta_anuncio where anu_id = 9101"));
        assertEquals("null", coluna("select per_resp from pergunta_anuncio where anu_id = 9101"));
        como(OUTRO, get(BASE + "/get-id/9101")).andExpect(jsonPath("$.perguntas", hasSize(1)));
    }

    @Test
    void naoPerguntaNoProprioAnuncioNemEmInexistenteNemSemTexto() throws Exception {
        como(DONO, ask("9101", "Pergunta?")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.texto").exists());
        como(OUTRO, ask("99999", "Pergunta?")).andExpect(status().isNotFound());
        como(OUTRO, ask("9101", "   ")).andExpect(status().isBadRequest());
        como(OUTRO, ask("9101", null)).andExpect(status().isBadRequest());
        assertEquals("0", coluna("select count(*) from pergunta_anuncio where anu_id = 9101"));
    }

    // ---- POST resposta (respond)

    private MockHttpServletRequestBuilder respond(String id, String texto) {
        var r = post(BASE + "/respond").param("id", id);
        return texto == null ? r : r.param("texto", texto);
    }

    @Test
    void donoRespondePerguntaDoProprioAnuncio() throws Exception {
        sql("insert into pergunta_anuncio (per_id, per_text, anu_id) values (9101, 'Faz desconto?', 9101)");
        como(DONO, respond("9101", "  Só no PIX  ")).andExpect(status().isOk());
        assertEquals("Só no PIX", coluna("select per_resp from pergunta_anuncio where per_id = 9101"));
        como(OUTRO, get(BASE + "/get-id/9101")).andExpect(jsonPath("$.perguntas[0].per_resp").value("Só no PIX"));
    }

    @Test
    void ninguemRespondePerguntaDeAnuncioDeOutroNemInexistenteNemSemTexto() throws Exception {
        sql("insert into pergunta_anuncio (per_id, per_text, anu_id) values (9102, 'Ainda tem?', 9103)");
        sql("insert into pergunta_anuncio (per_id, per_text, anu_id) values (9103, 'Faz desconto?', 9101)");
        como(DONO, respond("9102", "Tenho sim")).andExpect(status().isNotFound()).andExpect(jsonPath("$.tipo").exists());
        como(DONO, respond("99999", "Tenho sim")).andExpect(status().isNotFound());
        como(DONO, respond("9103", "  ")).andExpect(status().isBadRequest());
        como(DONO, respond("9103", null)).andExpect(status().isBadRequest());
        assertEquals("0", coluna("select count(*) from pergunta_anuncio where per_resp is not null"));
    }

    @Test
    void semTokenAsRotasNovasDao401() throws Exception {
        mvc.perform(novo("{}")).andExpect(status().isUnauthorized());
        mvc.perform(fotos("9101", arquivo("image/jpeg"))).andExpect(status().isUnauthorized());
        mvc.perform(delete(BASE).param("id", "9101")).andExpect(status().isUnauthorized());
        mvc.perform(ask("9101", "x")).andExpect(status().isUnauthorized());
        mvc.perform(respond("9101", "x")).andExpect(status().isUnauthorized());
    }
}
