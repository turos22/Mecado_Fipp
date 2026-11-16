package unoeste.fipp.backend.services;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unoeste.fipp.backend.entities.Usuario;
import unoeste.fipp.backend.repositories.UsuarioRepository;

@Service
public class AutenticacaoService {
    private static final String HASH_FALSO = "$2a$10$IY1YXEWgxNvvKcqifsLHau4BXzthArQ0yUWg0P7JXgWHiOdtmNMaO";

    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder();
    private final UsuarioRepository usuarioRepository;

    public AutenticacaoService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Confere nome e senha na tabela usuario; usr_pass guarda o hash BCrypt.
    // Vazio = nome/senha errados, nível desconhecido ou senha fora do formato; o chamador não distingue os casos.
    @Transactional(readOnly = true)
    public Optional<Usuario> autenticar(String nome, String senha) {
        if (nome == null || senha == null) return Optional.empty();
        List<Usuario> candidatos = usuarioRepository.findByUsrName(nome);
        //usuário inexistente também gasta um BCrypt: o tempo da resposta não revela quais nomes existem
        if (candidatos.isEmpty()) codificador.matches(senha, HASH_FALSO);
        return candidatos.stream()
                .filter(u -> u.getUsrPass() != null && codificador.matches(senha, u.getUsrPass()))
                .filter(u -> nivelDe(u) != null)
                .findFirst();
    }

    // "1" -> admin, "2" -> usuario; qualquer outro valor não tem acesso a nada
    public static String nivelDe(Usuario usuario) {
        if ("1".equals(usuario.getUsrLevel())) return "admin";
        if ("2".equals(usuario.getUsrLevel())) return "usuario";
        return null;
    }
}
