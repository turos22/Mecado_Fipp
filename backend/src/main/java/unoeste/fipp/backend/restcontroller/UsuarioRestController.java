package unoeste.fipp.backend.restcontroller;

import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

    @PostMapping
    public ResponseEntity<Object> novoAnuncio(@RequestAttribute(AccessFilter.ATRIBUTO_USR_ID) Integer usrId,
                                              @RequestBody EdicaoAnuncio dados){
        return ResponseEntity.ok(anuncioService.criar(usrId, dados));
    }
    @PostMapping("addFoto")
    public ResponseEntity<Object> addFoto(@RequestAttribute(AccessFilter.ATRIBUTO_USR_ID) Integer usrId,
                                          @RequestParam Integer id,
                                          @RequestParam(required = false) List<MultipartFile> fotos) throws IOException {
        return anuncioService.adicionarFotos(usrId, id, fotos)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> naoEncontrado(id));
    }
    @GetMapping("get-id/{id}")
    public ResponseEntity<Object> getId(@PathVariable Integer id){
        return anuncioService.buscarPorId(id)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> naoEncontrado(id));
    }
    @GetMapping("get-filter/{filtro}")
    public ResponseEntity<Object> getFiltro(@PathVariable String filtro){
        return ResponseEntity.ok(anuncioService.filtrar(filtro));
    }
    @GetMapping("meus")
    public ResponseEntity<Object> meus(@RequestAttribute(AccessFilter.ATRIBUTO_USR_ID) Integer usrId){
        return ResponseEntity.ok(anuncioService.listarDoDono(usrId));
    }
    @PutMapping("meus/{id}")
    public ResponseEntity<Object> editar(@RequestAttribute(AccessFilter.ATRIBUTO_USR_ID) Integer usrId,
                                         @PathVariable Integer id, @RequestBody EdicaoAnuncio dados){
        return anuncioService.editar(usrId, id, dados)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> naoEncontrado(id));
    }
    @DeleteMapping
    public ResponseEntity<Object> delete(@RequestAttribute(AccessFilter.ATRIBUTO_USR_ID) Integer usrId,
                                         @RequestParam Integer id){
        if (!anuncioService.excluirDoDono(usrId, id))
            return naoEncontrado(id);
        return ResponseEntity.ok("Anuncio "+id+" deletado");
    }
    @PostMapping("ask")
    public ResponseEntity<Object> ask(@RequestAttribute(AccessFilter.ATRIBUTO_USR_ID) Integer usrId,
                                      @RequestParam Integer id, @RequestParam(required = false) String texto){
        if (!anuncioService.perguntar(usrId, id, texto))
            return naoEncontrado(id);
        return ResponseEntity.ok("Pergunta enviada");
    }
    @PostMapping("respond")
    public ResponseEntity<Object> respond(@RequestAttribute(AccessFilter.ATRIBUTO_USR_ID) Integer usrId,
                                          @RequestParam Integer id, @RequestParam(required = false) String texto){
        if (!anuncioService.responder(usrId, id, texto))
            return ResponseEntity.status(404).body(new Erro("Não encontrado", "Pergunta " + id + " não encontrada nos seus anúncios"));
        return ResponseEntity.ok("Pergunta respondida");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> dadoInvalido(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new Erro("Dados inválidos", e.getMessage()));
    }

    private ResponseEntity<Object> naoEncontrado(Integer id) {
        return ResponseEntity.status(404).body(new Erro("Não encontrado", "Anúncio " + id + " não encontrado"));
    }
}
