package unoeste.fipp.backend.security;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

// O AccessFilter roda ao redor das rotas apis/adm/* e apis/usuario/*. Aqui ele é chamado direto, sem Spring nem banco.
class AccessFilterTest {

    private final AccessFilter filtro = new AccessFilter();

    private MockHttpServletRequest requisicao(String rota, String token) {
        MockHttpServletRequest requisicao = new MockHttpServletRequest("GET", rota);
        if (token != null) requisicao.addHeader("Authorization", token);
        return requisicao;
    }

    @Test
    void tokenDeAdminNaRotaAdmDeixaPassarEnaoMexeNaRespostaDaRota() throws Exception {
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        // A rota responde 200 com um JSON, como o Jackson faz: sem tamanho conhecido de antemão
        FilterChain rota = (req, res) -> {
            ((HttpServletResponse) res).setStatus(200);
            res.getOutputStream().write("[{\"a\":1}]".getBytes(UTF_8));
        };

        filtro.doFilter(requisicao("/apis/adm/anuncio/get-filter/todos", JWTTokenProvider.createToken("adm", "admin", 1)), resposta, rota);

        assertEquals(200, resposta.getStatus(), "o filtro não pode trocar o status que a rota deu");
        assertEquals("[{\"a\":1}]", resposta.getContentAsString(UTF_8), "o filtro não pode escrever nada depois da rota");
    }

    @Test
    void semTokenNaoChegaARota() throws Exception {
        AtomicBoolean rotaFoiChamada = new AtomicBoolean(false);

        filtro.doFilter(requisicao("/apis/adm/anuncio/get-filter/todos", null), new MockHttpServletResponse(),
                (req, res) -> rotaFoiChamada.set(true));

        assertFalse(rotaFoiChamada.get());
    }

    @Test
    void tokenComAssinaturaAdulteradaNaoChegaARota() throws Exception {
        AtomicBoolean rotaFoiChamada = new AtomicBoolean(false);
        String token = JWTTokenProvider.createToken("adm", "admin", 1);
        String adulterado = token.substring(0, token.length() - 4) + "AAAA";

        filtro.doFilter(requisicao("/apis/adm/anuncio/get-filter/todos", adulterado), new MockHttpServletResponse(),
                (req, res) -> rotaFoiChamada.set(true));

        assertFalse(rotaFoiChamada.get());
    }

    @Test
    void tokenDeUsuarioComumNaoChegaARotaDeAdministrador() throws Exception {
        AtomicBoolean rotaFoiChamada = new AtomicBoolean(false);

        filtro.doFilter(requisicao("/apis/adm/anuncio/get-filter/todos", JWTTokenProvider.createToken("jao", "usuario", 2)),
                new MockHttpServletResponse(), (req, res) -> rotaFoiChamada.set(true));

        assertFalse(rotaFoiChamada.get());
    }

    // ------------------------------------------------ níveis, status e identidade

    private MockHttpServletResponse passar(String rota, String token, AtomicBoolean chamada) throws Exception {
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        filtro.doFilter(requisicao(rota, token), resposta, (req, res) -> chamada.set(true));
        return resposta;
    }

    @Test
    void usuarioComumAcessaAsRotasDeUsuario() throws Exception {
        AtomicBoolean chamada = new AtomicBoolean(false);
        passar("/apis/usuario/get-filter/todos", JWTTokenProvider.createToken("jao", "usuario", 2), chamada);
        assertEquals(true, chamada.get());
    }

    @Test
    void rotaDeUsuarioComAdmNoFimDoCaminhoNaoViraRotaDeAdmin() throws Exception {
        // contains("adm") confundia isto com rota de administrador
        AtomicBoolean chamada = new AtomicBoolean(false);
        passar("/apis/usuario/get-filter/adm", JWTTokenProvider.createToken("jao", "usuario", 2), chamada);
        assertEquals(true, chamada.get());
    }

    @Test
    void rotaFalsaQueSoTemAdmNoNomeNaoLiberaNada() throws Exception {
        AtomicBoolean chamada = new AtomicBoolean(false);
        passar("/apis/administrador/x", JWTTokenProvider.createToken("adm", "admin", 1), chamada);
        assertFalse(chamada.get(), "só /apis/adm/ e /apis/usuario/ existem");
    }

    @Test
    void adminAcessaTambemAsRotasDeUsuario() throws Exception {
        AtomicBoolean chamada = new AtomicBoolean(false);
        passar("/apis/usuario/meus", JWTTokenProvider.createToken("adm", "admin", 1), chamada);
        assertEquals(true, chamada.get());
    }

    @Test
    void semTokenDevolve401ComJson() throws Exception {
        MockHttpServletResponse r = passar("/apis/usuario/meus", null, new AtomicBoolean(false));
        assertEquals(401, r.getStatus());
        assertEquals("application/json;charset=UTF-8", r.getContentType());
        assertEquals("{\"tipo\":\"Não autorizado\",\"texto\":\"Token ausente, inválido ou expirado\"}", r.getContentAsString(UTF_8));
    }

    @Test
    void tokenAdulteradoDevolve401() throws Exception {
        String t = JWTTokenProvider.createToken("jao", "usuario", 2);
        MockHttpServletResponse r = passar("/apis/usuario/meus", t.substring(0, t.length() - 4) + "AAAA", new AtomicBoolean(false));
        assertEquals(401, r.getStatus());
    }

    @Test
    void usuarioComumNaRotaAdmDevolve403() throws Exception {
        AtomicBoolean chamada = new AtomicBoolean(false);
        MockHttpServletResponse r = passar("/apis/adm/anuncio?id=1", JWTTokenProvider.createToken("jao", "usuario", 2), chamada);
        assertFalse(chamada.get());
        assertEquals(403, r.getStatus());
        assertEquals("{\"tipo\":\"Acesso negado\",\"texto\":\"Seu nível não permite acessar esta rota\"}", r.getContentAsString(UTF_8));
    }

    @Test
    void tokenSemClaimNivelNaoDaNullPointerE403() throws Exception {
        String semNivel = JWTTokenProvider.createToken("x", null, 1, 5); //claim nulo = claim ausente
        MockHttpServletResponse r = passar("/apis/usuario/meus", semNivel, new AtomicBoolean(false));
        assertEquals(403, r.getStatus());
    }

    @Test
    void tokenExpiradoDevolve401() throws Exception {
        String expirado = JWTTokenProvider.createToken("jao", "usuario", 2, -1);
        assertEquals(401, passar("/apis/usuario/meus", expirado, new AtomicBoolean(false)).getStatus());
    }

    @Test
    void identidadeDoTokenVaiParaARequisicaoEnaoParaOQueOClienteMandar() throws Exception {
        MockHttpServletRequest req = requisicao("/apis/usuario/meus", JWTTokenProvider.createToken("jao", "usuario", 2));
        req.setAttribute(AccessFilter.ATRIBUTO_USR_ID, 999); // cliente não pode semear isto
        filtro.doFilter(req, new MockHttpServletResponse(), (a, b) -> { });
        assertEquals(2, req.getAttribute(AccessFilter.ATRIBUTO_USR_ID));
    }

    @Test
    void tokenSemUsrIdNaoEntraEmRotaDeUsuario() throws Exception {
        // token antigo (sem identidade) não pode chegar a controllers que dependem dela
        MockHttpServletResponse r = passar("/apis/usuario/meus", JWTTokenProvider.createToken("jao", "usuario"), new AtomicBoolean(false));
        assertEquals(401, r.getStatus());
    }

    @Test
    void tokenDoLoginDuraMaisQueDoisMinutos() {
        java.util.Date exp = JWTTokenProvider.getAllClaimsFromToken(JWTTokenProvider.createToken("jao", "usuario", 2)).getExpiration();
        assertEquals(true, exp.getTime() - System.currentTimeMillis() > 30 * 60 * 1000L);
    }
}
