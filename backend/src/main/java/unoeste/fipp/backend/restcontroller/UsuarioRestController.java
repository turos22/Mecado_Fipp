package unoeste.fipp.backend.restcontroller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unoeste.fipp.backend.entities.EdicaoAnuncio;
import unoeste.fipp.backend.entities.Erro;
import unoeste.fipp.backend.security.AccessFilter;
import unoeste.fipp.backend.services.AnuncioService;

@CrossOrigin
@RestController
@RequestMapping("apis/usuario")
public class UsuarioRestController {
    private final AnuncioService anuncioService;

    public UsuarioRestController(AnuncioService anuncioService) {
        this.anuncioService = anuncioService;
    }

    //http://localhost:8080/apis/usuario  (o dono vem do token e a data é a de hoje)
    @PostMapping
    public ResponseEntity<Object> novoAnuncio(@RequestAttribute(AccessFilter.ATRIBUTO_USR_ID) Integer usrId,
                                              @RequestBody EdicaoAnuncio dados){
        return ResponseEntity.ok(anuncioService.criar(usrId, dados));
    }
    //http://localhost:8080/apis/usuario/get-id/2
    @GetMapping("get-id/{id}")
    public ResponseEntity<Object> getId(@PathVariable Integer id){
        return anuncioService.buscarPorId(id)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> naoEncontrado(id));
    }
    //http://localhost:8080/apis/usuario/get-filter/bike  ("todos" lista tudo)
    @GetMapping("get-filter/{filtro}")
    public ResponseEntity<Object> getFiltro(@PathVariable String filtro){
        return ResponseEntity.ok(anuncioService.filtrar(filtro));
    }
    //http://localhost:8080/apis/usuario/meus  (o usuário vem do token, nunca da URL)
    @GetMapping("meus")
    public ResponseEntity<Object> meus(@RequestAttribute(AccessFilter.ATRIBUTO_USR_ID) Integer usrId){
        return ResponseEntity.ok(anuncioService.listarDoDono(usrId));
    }
    //http://localhost:8080/apis/usuario/meus/2  (só edita anúncio do próprio usuário)
    @PutMapping("meus/{id}")
    public ResponseEntity<Object> editar(@RequestAttribute(AccessFilter.ATRIBUTO_USR_ID) Integer usrId,
                                         @PathVariable Integer id, @RequestBody EdicaoAnuncio dados){
        return anuncioService.editar(usrId, id, dados)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> naoEncontrado(id));
    }

    //dado inválido vindo do serviço (o texto já é para o usuário)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> dadoInvalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new Erro("Dados inválidos", e.getMessage()));
    }

    private ResponseEntity<Object> naoEncontrado(Integer id) {
        return ResponseEntity.status(404).body(new Erro("Não encontrado", "Anúncio " + id + " não encontrado"));
    }
}
