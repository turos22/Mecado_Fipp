package unoeste.fipp.backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class AccessFilter implements Filter {
    //quem passou pelo filtro tem aqui o usr_id do TOKEN (nunca o que o cliente mandou); os controllers leem daqui
    public static final String ATRIBUTO_USR_ID = "usr_id";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String token = request.getHeader("Authorization");
        Claims claims = token == null ? null : JWTTokenProvider.getAllClaimsFromToken(token); //nulo se assinatura ruim ou expirado
        if (claims == null) {
            negar(response, 401, "Não autorizado", "Token ausente, inválido ou expirado");
            return;
        }
        Object nivel = claims.get("nivel");
        if (!permitido(nivel == null ? null : nivel.toString(), request.getRequestURI())) {
            negar(response, 403, "Acesso negado", "Seu nível não permite acessar esta rota");
            return;
        }
        Object usrId = claims.get("usr_id");
        if (!(usrId instanceof Integer)) {
            negar(response, 401, "Não autorizado", "Token ausente, inválido ou expirado");
            return;
        }
        request.setAttribute(ATRIBUTO_USR_ID, usrId); //sobrescreve qualquer valor que já estivesse na requisição
        filterChain.doFilter(servletRequest, servletResponse);
    }

    // admin: /apis/adm e /apis/usuario; usuario: só /apis/usuario; o resto é negado
    private boolean permitido(String nivel, String rota) {
        if (nivel == null) return false;
        if (nivel.equals("admin") && daRota(rota, "/apis/adm")) return true;
        return (nivel.equals("admin") || nivel.equals("usuario")) && daRota(rota, "/apis/usuario");
    }

    private boolean daRota(String rota, String prefixo) {
        return rota.equals(prefixo) || rota.startsWith(prefixo + "/");
    }

    private void negar(HttpServletResponse response, int status, String tipo, String texto) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getOutputStream().write(("{\"tipo\":\"" + tipo + "\",\"texto\":\"" + texto + "\"}").getBytes(StandardCharsets.UTF_8));
    }
}
