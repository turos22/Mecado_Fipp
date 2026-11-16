package unoeste.fipp.backend;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import unoeste.fipp.backend.security.JWTTokenProvider;

// Rotas de anúncio do administrador (apis/adm/anuncio): get-id/{id}, get-filter/{filtro} e DELETE ?id=.
// Cada teste grava os próprios dados (ids 9xxx) por SQL e a transação é desfeita no fim (@Transactional):
// o banco mercado_fipp fica exatamente como estava. Os valores esperados são literais escritos à mão.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdmAnuncioRestControllerTest {

    private static final String BASE = "/apis/adm/anuncio";

    @Autowired
    MockMvc mvc;
    @Autowired
    EntityManager em;

    // ---------------------------------------------------------------- apoio

    private ResultActions chamar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requisicao) throws Exception {
        // O AccessFilter exige um token de administrador nas rotas apis/adm/*
        return mvc.perform(requisicao.header("Authorization", JWTTokenProvider.createToken("adm", "admin", 1)));
    }

    private void sql(String comando, Object... parametros) {
        var consulta = em.createNativeQuery(comando);
        for (int i = 0; i < parametros.length; i++) consulta.setParameter(i + 1, parametros[i]);
        consulta.executeUpdate();
    }

    private long contar(String comando) {
        return ((Number) em.createNativeQuery(comando).getSingleResult()).longValue();
    }

    private void categoria(int id, String nome) {
        sql("insert into categoria (cat_id, cat_name) values (?1, ?2)", id, nome);
    }

    private void usuario(int id, String nome) {
        sql("insert into usuario (usr_id, usr_name, usr_pass, usr_level) values (?1, ?2, 'segredo', '2')", id, nome);
    }

    private void anuncio(int id, String titulo, String data, String descricao, String preco, int cat, int usr) {
        sql("insert into anuncio (anu_id, anu_title, anu_date, anu_desc, anu_price, cat_id, usr_id) values (?1, ?2, ?3, ?4, ?5, ?6, ?7)",
                id, titulo, LocalDate.parse(data), descricao, new BigDecimal(preco), cat, usr);
    }

    private void foto(int id, String arquivo, int anuncio) {
        sql("insert into foto_anuncio (fot_id, fot_file, anu_id) values (?1, ?2, ?3)", id, arquivo, anuncio);
    }

    private void pergunta(int id, String texto, String resposta, int anuncio) {
        if (resposta == null) sql("insert into pergunta_anuncio (per_id, per_text, per_resp, anu_id) values (?1, ?2, null, ?3)", id, texto, anuncio);
        else sql("insert into pergunta_anuncio (per_id, per_text, per_resp, anu_id) values (?1, ?2, ?3, ?4)", id, texto, resposta, anuncio);
    }

    // Categoria, vendedor e um anúncio completo (2 fotos, 2 perguntas) com o id 9001
    private void cenarioCompleto() {
        categoria(9001, "cat-teste");
        usuario(9001, "vend-teste");
        anuncio(9001, "Notebook de teste", "2026-11-01", "Descricao do notebook", "123.4", 9001, 9001);
        foto(9002, "b.jpg", 9001); // gravada ANTES da a.jpg, para provar que a ordem vem do id e não da ordem de gravação
        foto(9001, "a.jpg", 9001);
        pergunta(9001, "Tem garantia?", "Sim, 3 meses", 9001);
        pergunta(9002, "Aceita troca?", null, 9001);
        em.flush();
        em.clear();
    }

    // ---------------------------------------------------------------- get-id/{id}

    @Test
    void getIdDevolveOsDadosDoAnuncioComNomeDaCategoriaEDoVendedor() throws Exception {
        cenarioCompleto();

        chamar(get(BASE + "/get-id/9001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anu_id").value(9001))
                .andExpect(jsonPath("$.anu_title").value("Notebook de teste"))
                .andExpect(jsonPath("$.anu_date").value("2026-11-01"))
                .andExpect(jsonPath("$.anu_desc").value("Descricao do notebook"))
                .andExpect(jsonPath("$.anu_price").value(123.4))
                .andExpect(jsonPath("$.cat_id").value(9001))
                .andExpect(jsonPath("$.cat_name").value("cat-teste"))
                .andExpect(jsonPath("$.usr_id").value(9001))
                .andExpect(jsonPath("$.usr_name").value("vend-teste"));
    }

    @Test
    void getIdDevolveAsFotosNaOrdemDoId() throws Exception {
        cenarioCompleto();

        chamar(get(BASE + "/get-id/9001"))
                .andExpect(jsonPath("$.fotos", hasSize(2)))
                .andExpect(jsonPath("$.fotos[0].fot_id").value(9001))
                .andExpect(jsonPath("$.fotos[0].fot_file").value("a.jpg"))
                .andExpect(jsonPath("$.fotos[1].fot_file").value("b.jpg"));
    }

    @Test
    void getIdDevolveAsPerguntasComEmSemResposta() throws Exception {
        cenarioCompleto();

        chamar(get(BASE + "/get-id/9001"))
                .andExpect(jsonPath("$.perguntas", hasSize(2)))
                .andExpect(jsonPath("$.perguntas[0].per_text").value("Tem garantia?"))
                .andExpect(jsonPath("$.perguntas[0].per_resp").value("Sim, 3 meses"))
                .andExpect(jsonPath("$.perguntas[1].per_text").value("Aceita troca?"))
                .andExpect(jsonPath("$.perguntas[1].per_resp").value(nullValue()));
    }

    @Test
    void getIdNuncaExpoeASenhaDoVendedor() throws Exception {
        cenarioCompleto();

        // Primeiro prova que a resposta TEM o anúncio e o nome do vendedor; só assim "sem senha" quer dizer alguma coisa
        String corpo = chamar(get(BASE + "/get-id/9001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usr_name").value("vend-teste"))
                .andReturn().getResponse().getContentAsString();

        assertEquals(false, corpo.contains("usr_pass"), "a resposta não pode ter o campo da senha");
        assertEquals(false, corpo.contains("segredo"), "a resposta não pode ter o valor da senha");
    }

    @Test
    void getIdDeAnuncioInexistenteDevolve404ComMensagemDeErro() throws Exception {
        chamar(get(BASE + "/get-id/9999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.tipo", not(emptyOrNullString())))
                .andExpect(jsonPath("$.texto", not(emptyOrNullString())));
    }

    // ---------------------------------------------------------------- get-filter/{filtro}

    @Test
    void getFilterTodosDevolveTodosOsAnunciosDoBanco() throws Exception {
        cenarioCompleto();
        anuncio(9002, "Outro anuncio", "2026-11-02", "x", "1.0", 9001, 9001);
        em.flush();
        long esperado = contar("select count(*) from anuncio");

        chamar(get(BASE + "/get-filter/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize((int) esperado)))
                .andExpect(jsonPath("$[?(@.anu_id == 9001)].cat_name").value("cat-teste"));
    }

    @Test
    void getFilterProcuraNoTituloSemDiferenciarMaiusculas() throws Exception {
        categoria(9001, "c");
        usuario(9001, "u");
        anuncio(9001, "Calculadora CIENTÍFICA zzt", "2026-11-01", "x", "1.0", 9001, 9001);
        em.flush();

        // O título está em maiúsculas e o usuário digita em maiúsculas: os dois lados são convertidos antes de comparar
        chamar(get(BASE + "/get-filter/CIENTÍFICA ZZT"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].anu_id").value(9001));
    }

    @Test
    void getFilterTambemProcuraNaDescricao() throws Exception {
        categoria(9001, "c");
        usuario(9001, "u");
        anuncio(9001, "Mouse", "2026-11-01", "serve para zzdescricaoxx e mais nada", "1.0", 9001, 9001);
        em.flush();

        chamar(get(BASE + "/get-filter/zzdescricaoxx"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].anu_title").value("Mouse"));
    }

    @Test
    void getFilterSemResultadoDevolveListaVazia() throws Exception {
        chamar(get(BASE + "/get-filter/qqqnaoexisteqqq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getFilterTrataOPorcentoComoTextoComum() throws Exception {
        categoria(9001, "c");
        usuario(9001, "u");
        anuncio(9001, "zzp 100% algodao", "2026-11-01", "x", "1.0", 9001, 9001);
        anuncio(9002, "zzp 1000 algodao", "2026-11-01", "x", "1.0", 9001, 9001);
        em.flush();

        // Se o % valesse como curinga do LIKE, "zzp 100%" também acharia "zzp 1000 algodao"
        chamar(get(BASE + "/get-filter/zzp 100%"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].anu_id").value(9001));
    }

    @Test
    void getFilterTrataOSublinhadoComoTextoComum() throws Exception {
        categoria(9001, "c");
        usuario(9001, "u");
        anuncio(9001, "zzq_a item", "2026-11-01", "x", "1.0", 9001, 9001);
        anuncio(9002, "zzqba item", "2026-11-01", "x", "1.0", 9001, 9001);
        em.flush();

        chamar(get(BASE + "/get-filter/zzq_a"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].anu_id").value(9001));
    }

    @Test
    void getFilterOrdenaDoMaisRecenteParaOMaisAntigoEDesempataPeloId() throws Exception {
        categoria(9001, "c");
        usuario(9001, "u");
        anuncio(9101, "zzord a", "2030-01-01", "x", "1.0", 9001, 9001);
        anuncio(9102, "zzord b", "2030-01-03", "x", "1.0", 9001, 9001);
        anuncio(9103, "zzord c", "2030-01-03", "x", "1.0", 9001, 9001);
        em.flush();

        chamar(get(BASE + "/get-filter/zzord"))
                .andExpect(jsonPath("$[*].anu_id", contains(9103, 9102, 9101)));
    }

    // ---------------------------------------------------------------- DELETE ?id=

    @Test
    void deleteRemoveOAnuncioComSuasFotosEPerguntasSemMexerNosOutros() throws Exception {
        cenarioCompleto();
        anuncio(9002, "Outro anuncio", "2026-11-02", "x", "1.0", 9001, 9001);
        foto(9003, "c.jpg", 9002);
        pergunta(9003, "Pergunta do outro", null, 9002);
        em.flush();
        em.clear();

        chamar(delete(BASE).param("id", "9001"))
                .andExpect(status().isOk());
        em.flush();

        assertEquals(0, contar("select count(*) from anuncio where anu_id = 9001"));
        assertEquals(0, contar("select count(*) from foto_anuncio where anu_id = 9001"));
        assertEquals(0, contar("select count(*) from pergunta_anuncio where anu_id = 9001"));
        assertEquals(1, contar("select count(*) from anuncio where anu_id = 9002"));
        assertEquals(1, contar("select count(*) from foto_anuncio where anu_id = 9002"));
        assertEquals(1, contar("select count(*) from pergunta_anuncio where anu_id = 9002"));
    }

    @Test
    void deleteDeAnuncioInexistenteDevolve404ComMensagemDeErro() throws Exception {
        chamar(delete(BASE).param("id", "9999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.texto", not(emptyOrNullString())));
    }

    @Test
    void deleteSemInformarOIdDevolve400() throws Exception {
        long antes = contar("select count(*) from anuncio");

        chamar(delete(BASE)).andExpect(status().isBadRequest());

        assertEquals(antes, contar("select count(*) from anuncio"));
    }
}
