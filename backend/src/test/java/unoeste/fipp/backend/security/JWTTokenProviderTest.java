package unoeste.fipp.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;

import org.junit.jupiter.api.Test;

// A chave que assina os tokens não pode estar escrita no código (o repositório é público).
class JWTTokenProviderTest {

    private static final String SEGREDO = "um-segredo-de-teste-com-mais-de-32-bytes!!";

    private String assinadoCom(SecretKey chave) {
        return Jwts.builder().setSubject("x").signWith(chave).compact();
    }

    @Test
    void semSegredoConfiguradoCadaSubidaSorteiaUmaChaveDiferente() {
        // dois processos diferentes = chaves diferentes: um token de um não vale no outro
        SecretKey a = JWTTokenProvider.resolverChave(null);
        SecretKey b = JWTTokenProvider.resolverChave("");
        assertNotEquals(java.util.Base64.getEncoder().encodeToString(a.getEncoded()), java.util.Base64.getEncoder().encodeToString(b.getEncoded()));
    }

    @Test
    void comSegredoConfiguradoAChaveEDeterministica() {
        SecretKey a = JWTTokenProvider.resolverChave(SEGREDO);
        SecretKey b = JWTTokenProvider.resolverChave(SEGREDO);
        assertEquals(assinadoCom(a), assinadoCom(b));
    }

    @Test
    void segredoDiferenteGeraAssinaturaDiferente() {
        assertNotEquals(assinadoCom(JWTTokenProvider.resolverChave(SEGREDO)), assinadoCom(JWTTokenProvider.resolverChave(SEGREDO + "x")));
    }

    @Test
    void segredoCurtoDemaisFalhaNaSubidaComMensagemClara() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> JWTTokenProvider.resolverChave("curto"));
        assertEquals(true, e.getMessage().contains("JWT_SECRET"));
    }

    @Test
    void tokenAssinadoComOutraChaveNaoEAceito() {
        // quem não conhece a chave do backend não consegue forjar um token de admin
        String forjado = Jwts.builder().setSubject("adm").claim("nivel", "admin").claim("usr_id", 1)
                .setExpiration(new java.util.Date(System.currentTimeMillis() + 60000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("uma-chave-qualquer-que-o-backend-nao-usa-0123456789".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();
        assertEquals(false, JWTTokenProvider.verifyToken(forjado));
    }
}
