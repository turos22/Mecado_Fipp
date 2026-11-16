package unoeste.fipp.backend.restcontroller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unoeste.fipp.backend.entities.Erro;
import unoeste.fipp.backend.services.AnuncioService;

@CrossOrigin
@RestController
@RequestMapping("apis/adm/anuncio")
public class AdmAnuncioRestController {
    private final AnuncioService anuncioService;

    public AdmAnuncioRestController(AnuncioService anuncioService) {
        this.anuncioService = anuncioService;
    }

    //http://localhost:8080/apis/adm/anuncio/get-id/2
    @GetMapping("get-id/{id}")
    public ResponseEntity<Object> getId(@PathVariable Integer id){
        return anuncioService.buscarPorId(id)
                .<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> naoEncontrado(id));
    }
    //http://localhost:8080/apis/adm/anuncio/get-filter/computador
    @GetMapping("get-filter/{filtro}")
    public ResponseEntity<Object> getFiltro(@PathVariable String filtro){
        return ResponseEntity.ok(anuncioService.filtrar(filtro));
    }
    //http://localhost:8080/apis/adm/anuncio?id=2
    @DeleteMapping
    public ResponseEntity<Object> delete(@RequestParam Integer id){
        if (!anuncioService.excluir(id))
            return naoEncontrado(id);
        return ResponseEntity.ok("Anuncio "+id+" deletado");
    }

    private ResponseEntity<Object> naoEncontrado(Integer id){
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new Erro("Não encontrado", "Anúncio "+id+" não existe"));
    }
}
