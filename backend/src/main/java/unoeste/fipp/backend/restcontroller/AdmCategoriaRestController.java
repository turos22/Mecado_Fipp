package unoeste.fipp.backend.restcontroller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unoeste.fipp.backend.entities.Categoria;
import unoeste.fipp.backend.entities.Erro;
import unoeste.fipp.backend.services.CategoriaService;

@CrossOrigin
@RestController
@RequestMapping("apis/adm/categoria")
public class AdmCategoriaRestController {
    @Autowired
    private HttpServletRequest request;

    private final CategoriaService categoriaService;

    public AdmCategoriaRestController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    //http://localhost:8080/apis/adm/categoria
    @PostMapping
    public ResponseEntity<Object> novaCategoria(@RequestBody Categoria categoria) {
        try {
            return ResponseEntity.ok(categoriaService.criar(categoria));
        }catch (IllegalArgumentException e)
        {
           return ResponseEntity.badRequest().body(new Erro("Dados invalidos",e.getMessage()));
        }
    }

    //http://localhost:8080/apis/adm/categoria/get-id/2
    @GetMapping("get-id/{id}")
    public ResponseEntity<Object> getCategoriaId(@PathVariable Integer id) {

        return categoriaService.buscarPorId(id).<ResponseEntity<Object>>map(ResponseEntity::ok)
                .orElseGet(() -> naoEncontrado(id));
    }

    private ResponseEntity<Object> naoEncontrado(Integer id)
    {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new Erro("Não encontrado", "categoria "+id+" não existe"));
    }
    @GetMapping("get-all")
    public ResponseEntity<Object> todasCategorias(){

        return ResponseEntity.ok(categoriaService.listarTodas());

    }
    @PutMapping
    public ResponseEntity<Object> alterarCategoria(@RequestBody Categoria categoria)
    {
        try {
            return categoriaService.alterar(categoria).
                    <ResponseEntity<Object>>map(ResponseEntity::ok).orElseGet(() -> naoEncontrado(categoria.getCatId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new Erro("Dados invalidos",e.getMessage()));
        }
    }
    //http://localhost:8080/apis/adm/categoria?id=8;
    @DeleteMapping
    public ResponseEntity<Object> excluirCategoria(@RequestParam Integer id)
    {
        try {
            if(!categoriaService.excluir(id))
            {
                return naoEncontrado(id);
            }else{
                return ResponseEntity.ok("Categoria " + id + " excluída");
            }
        }catch (IllegalArgumentException e)
        {
            return ResponseEntity.badRequest().body(new Erro("Dados invalidos",e.getMessage()));
        }
    }

}
