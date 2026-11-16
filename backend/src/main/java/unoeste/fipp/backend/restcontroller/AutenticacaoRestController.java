package unoeste.fipp.backend.restcontroller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unoeste.fipp.backend.entities.Erro;
import unoeste.fipp.backend.entities.Token;
import unoeste.fipp.backend.entities.Usuario;
import unoeste.fipp.backend.security.JWTTokenProvider;
import unoeste.fipp.backend.services.AutenticacaoService;

import java.util.Optional;

@CrossOrigin
@RestController
@RequestMapping("apis/autenticacao")
public class AutenticacaoRestController {
    private final AutenticacaoService autenticacaoService;

    public AutenticacaoRestController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    //http://localhost:8080/apis/autenticacao/add-user
    @PostMapping("add-user")
    public ResponseEntity<Object> novoUsuario(@RequestBody Usuario usuario){
        return ResponseEntity.ok("Usuario cadastrado com sucesso");
    }
    //http://localhost:8080/apis/autenticacao/login
    @PostMapping("login")
    public ResponseEntity<Object> login(@RequestParam(required = false) String usuario, @RequestParam(required = false) String senha){
        //autentica o usuário junto ao seu cadastro (tabela usuario)
        Optional<Usuario> cadastro = autenticacaoService.autenticar(usuario, senha);
        if(cadastro.isPresent()) {
            Usuario u = cadastro.get();
            String token=JWTTokenProvider.createToken(u.getUsrName(), AutenticacaoService.nivelDe(u), u.getUsrId());
            return ResponseEntity.ok(new Token(u.getUsrName(),token));
        }
        //mesma resposta para usuário inexistente e senha errada
        return ResponseEntity.badRequest().body(new Erro("Bloqueio de Acesso","Usuário ou senha inválidos"));
    }
}
