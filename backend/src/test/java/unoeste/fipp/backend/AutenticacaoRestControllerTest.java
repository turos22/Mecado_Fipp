package unoeste.fipp.backend;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import unoeste.fipp.backend.security.JWTTokenProvider;

// Login (apis/autenticacao/login) conferido contra a tabela usuario. Dados 9xxx criados por SQL e desfeitos no fim.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AutenticacaoRestControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    EntityManager em;

    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder();

    // grava a senha como o sistema guarda: hash BCrypt
    private void usuario(int id, String nome, String senha, String nivel) {
        usuarioComSenhaCrua(id, nome, codificador.encode(senha), nivel);
    }

    private void usuarioComSenhaCrua(int id, String nome, String guardada, String nivel) {
        em.createNativeQuery("insert into usuario (usr_id, usr_name, usr_pass, usr_level) values (?1, ?2, ?3, ?4)")
                .setParameter(1, id).setParameter(2, nome).setParameter(3, guardada).setParameter(4, nivel).executeUpdate();
    }

    private ResultActions login(String usuario, String senha) throws Exception {
        var req = post("/apis/autenticacao/login");
        if (usuario != null) req.param("usuario", usuario);
        if (senha != null) req.param("senha", senha);
        return mvc.perform(req);
    }

    private String token(ResultActions r) throws Exception {
        String corpo = r.andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(corpo, "$.token");
    }

    @Test
    void usuarioComumLogaEOTokenTraNivelUsuarioEOSeuId() throws Exception {
        usuario(9001, "tstvend", "senha1", "2");

        ResultActions r = login("tstvend", "senha1").andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("tstvend"));

        String token = token(r);
        assertEquals("usuario", JWTTokenProvider.getClaimFromToken(token, "nivel"));
        assertEquals("9001", JWTTokenProvider.getClaimFromToken(token, "usr_id"));
        assertEquals("tstvend", JWTTokenProvider.getAllClaimsFromToken(token).getSubject());
    }

    @Test
    void nivel1VirraAdmin() throws Exception {
        usuario(9002, "tstadm", "senha1", "1");

        String token = token(login("tstadm", "senha1").andExpect(status().isOk()));

        assertEquals("admin", JWTTokenProvider.getClaimFromToken(token, "nivel"));
    }

    @Test
    void senhaErradaEhRecusadaSemDarToken() throws Exception {
        usuario(9003, "tstvend", "senha1", "2");

        login("tstvend", "senha2").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.tipo").value("Bloqueio de Acesso"))
                .andExpect(content().string(not(containsString("token"))));
    }

    @Test
    void usuarioInexistenteRecebeAMesmaRespostaDeSenhaErrada() throws Exception {
        usuario(9004, "tstvend", "senha1", "2");
        String semUsuario = login("naoexiste", "senha1").andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        String semSenha = login("tstvend", "errada").andReturn().getResponse().getContentAsString();

        assertEquals(semSenha, semUsuario, "mensagens diferentes revelam quais usuários existem");
    }

    @Test
    void oAntigoAdm123FixoNaoFuncionaMais() throws Exception {
        // no banco o adm tem outra senha; "123" só passava por estar escrita no código
        login("adm", "123").andExpect(status().isBadRequest());
    }

    @Test
    void admDoBancoEntraComASuaSenhaDoBanco() throws Exception {
        usuario(9005, "tstadm2", "abc", "1");
        login("tstadm2", "abc").andExpect(status().isOk());
    }

    @Test
    void senhaEhSensivelAMaiusculas() throws Exception {
        usuario(9006, "tstvend", "Senha1", "2");
        login("tstvend", "senha1").andExpect(status().isBadRequest());
    }

    @Test
    void nomeEhSensivelAMaiusculas() throws Exception {
        usuario(9007, "tstvend", "senha1", "2");
        login("TSTVEND", "senha1").andExpect(status().isBadRequest());
    }

    @Test
    void semParametrosEhRequisicaoInvalidaENaoErro500() throws Exception {
        login(null, null).andExpect(status().isBadRequest());
        login("tstvend", null).andExpect(status().isBadRequest());
        login(null, "x").andExpect(status().isBadRequest());
    }

    @Test
    void senhaVaziaNaoCasaComUsuarioSemSenha() throws Exception {
        em.createNativeQuery("insert into usuario (usr_id, usr_name, usr_pass, usr_level) values (9008, 'tstvazio', null, '2')").executeUpdate();
        login("tstvazio", "").andExpect(status().isBadRequest());
    }

    @Test
    void usuarioSemNivelValidoNaoLoga() throws Exception {
        usuario(9009, "tstsemnivel", "senha1", "9");
        login("tstsemnivel", "senha1").andExpect(status().isBadRequest());
    }

    @Test
    void doisUsuariosComOMesmoNomeEntramCadaUmComASuaSenha() throws Exception {
        usuario(9010, "tstigual", "aaa", "2");
        usuario(9011, "tstigual", "bbb", "2");

        assertEquals("9011", JWTTokenProvider.getClaimFromToken(token(login("tstigual", "bbb").andExpect(status().isOk())), "usr_id"));
        assertEquals("9010", JWTTokenProvider.getClaimFromToken(token(login("tstigual", "aaa").andExpect(status().isOk())), "usr_id"));
    }

    @Test
    void senhaGuardadaEmTextoPuroNaoLogaMaisNemComATextoPuro() throws Exception {
        usuarioComSenhaCrua(9012, "tstcru", "senha1", "2");
        login("tstcru", "senha1").andExpect(status().isBadRequest());
    }

    @Test
    void hashEmSiNaoServeDeSenha() throws Exception {
        String hash = codificador.encode("senha1");
        usuarioComSenhaCrua(9013, "tsthash", hash, "2");
        login("tsthash", hash).andExpect(status().isBadRequest());
    }

    @Test
    void ousuariosDoSeedEntramComAsSenhasDocumentadas() throws Exception {
        login("adm", "adm123").andExpect(status().isOk());
        login("jao", "jao123").andExpect(status().isOk());
        login("marina", "mari123").andExpect(status().isOk());
    }

    @Test
    void nenhumaSenhaDoBancoFicouEmTextoPuro() {
        Number cruas = (Number) em.createNativeQuery("select count(*) from usuario where usr_pass not like '$2a$%' and usr_pass not like '$2b$%' and usr_pass not like '$2y$%'").getSingleResult();
        assertEquals(0L, cruas.longValue());
    }
}
