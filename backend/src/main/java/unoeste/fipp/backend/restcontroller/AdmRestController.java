package unoeste.fipp.backend.restcontroller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unoeste.fipp.backend.entities.Categoria;
import unoeste.fipp.backend.entities.Erro;
import unoeste.fipp.backend.entities.Usuario;
import unoeste.fipp.backend.security.JWTTokenProvider;

@CrossOrigin
@RestController
@RequestMapping("apis/adm/categoria")
public class AdmRestController {
    @Autowired
    private HttpServletRequest request;

    //http://localhost:8080/apis/adm/categoria
    @PostMapping
    public ResponseEntity<Object> novaCategoria(@RequestBody Categoria categoria){
        return ResponseEntity.ok("Categoria cadastrada com sucesso");
    }
    //http://localhost:8080/apis/adm/categoria/2
    @GetMapping("{id}")
    public ResponseEntity<Object> getCategoriaId(@PathVariable Long id){
        //String token=request.getHeader("Authorization");
        //System.out.println(token);
        //if(JWTTokenProvider.verifyToken(token)) {
            //token validado
       //     String nivel=JWTTokenProvider.getClaimFromToken(token,"nivel");
       //     if(nivel.equals("admin")){
                return ResponseEntity.ok("Categoria "+id);
        //    }
        //}
        //return ResponseEntity.badRequest().body(new Erro("Acesso bloqueado","Usuário não possui acesso a esse endpoint"));
    }
}
