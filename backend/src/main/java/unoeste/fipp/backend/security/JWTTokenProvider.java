package unoeste.fipp.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class JWTTokenProvider {
    //a chave vem da variável de ambiente JWT_SECRET (32+ caracteres); sem ela, é sorteada a cada subida
    //do backend (os tokens antigos deixam de valer ao reiniciar: é só entrar de novo). Nunca escrever a chave aqui.
    private static final SecretKey CHAVE = resolverChave(System.getenv("JWT_SECRET"));

    static SecretKey resolverChave(String segredo) {
        if (segredo == null || segredo.isBlank())
            return Jwts.SIG.HS256.key().build();
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32)
            throw new IllegalArgumentException("JWT_SECRET precisa ter pelo menos 32 caracteres (tem " + bytes.length + ")");
        return Keys.hmacShaKeyFor(bytes);
    }

    private static final long DURACAO_MINUTOS = 60L;

    //token sem identidade (usr_id): o AccessFilter recusa, serve só para testar tokens antigos
    static public String createToken(String usuario, String nivel)
    {
        return createToken(usuario, nivel, null);
    }

    static public String createToken(String usuario, String nivel, Integer usrId)
    {
        return createToken(usuario, nivel, usrId, DURACAO_MINUTOS);
    }

    //validade em minutos (negativa = já expirado); visível só ao pacote, para os testes
    static String createToken(String usuario, String nivel, Integer usrId, long minutos)
    {       
        String jwtToken = Jwts.builder()
            .setSubject(usuario)
            .setIssuer("localhost:8080")
            .claim("nivel", nivel)
            .claim("usr_id", usrId)
            .setIssuedAt(new Date())
            .setExpiration(Date.from(LocalDateTime.now().plusMinutes(minutos)
                .atZone(ZoneId.systemDefault()).toInstant()))
            .signWith(CHAVE)
            .compact();
        return jwtToken;        
    }

    static public boolean verifyToken(String token)
    {
        try {
            Jwts.parser()
                .setSigningKey(CHAVE)
                .build()
                .parseClaimsJws(token).getSignature();
                return true;
       } catch (Exception e) {
                System.out.println(e);
       }
       return false;       
    }

    static public Claims getAllClaimsFromToken(String token) 
    {
        Claims claims=null;
        try {
            claims = Jwts.parser()
            .setSigningKey(CHAVE)
            .build()
            .parseClaimsJws(token)
            .getBody();
        } catch (Exception e) {
            System.out.println("Erro ao recuperar as informações (claims)");
        }
        return claims;        
    }
    static public String getClaimFromToken(String token, String chave)
    {
        Claims claims=null;
        try {
            claims = Jwts.parser()
                    .setSigningKey(CHAVE)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            System.out.println("Erro ao recuperar as informações (claims)");
        }
        return claims.get(chave).toString();
    }

}
