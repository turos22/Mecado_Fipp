package unoeste.fipp.backend.services;



import org.springframework.stereotype.Service;

import unoeste.fipp.backend.entities.Categoria;
import unoeste.fipp.backend.repositories.CategoriaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class CategoriaService {
    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository){
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listarTodas(){
        List<Categoria> categorias = categoriaRepository.findAll();
        return categorias;
    }

    @Transactional(readOnly = true)
    public Optional<Categoria> buscarPorId(Integer id)
    {
        Optional<Categoria> categoriaId = categoriaRepository.findById(id);
        return categoriaId;
    }

    @Transactional
    public Categoria criar(Categoria categoria)
    {
        if(categoria.getCatName() == null)
            categoria.setCatName("");
        else{
            categoria.setCatName(categoria.getCatName().trim());
        }
        if(categoria.getCatName().isEmpty()) throw new IllegalArgumentException("Informe o nome da categoria");
        if(categoria.getCatName().length()>20) throw new IllegalArgumentException("o Nome excedeu o limite de 20 caracteres");
        if(categoriaRepository.existsByCatNameIgnoreCase(categoria.getCatName())) throw new IllegalArgumentException("Nome de categoria ja existente!");
        categoria.setCatId(null);
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public Optional<Categoria> alterar(Categoria categoria)
    {
        if(categoria.getCatId()==null) throw new IllegalArgumentException("Informe o ID da categoria");
        if(!categoriaRepository.existsById(categoria.getCatId())) return Optional.empty();
        if(categoria.getCatName() == null)
            categoria.setCatName("");
        else{
            categoria.setCatName(categoria.getCatName().trim());
        }
        if(categoria.getCatName().isEmpty()) throw new IllegalArgumentException("Informe o nome da categoria");
        if(categoria.getCatName().length()>20) throw new IllegalArgumentException("o Nome excedeu o limite de 20 caracteres");
        if(categoriaRepository.existsByCatNameIgnoreCaseAndCatIdNot(categoria.getCatName(),categoria.getCatId()))throw new IllegalArgumentException("nome ja existente");
        return Optional.of(categoriaRepository.save(categoria));
    }
    @Transactional
    public boolean excluir(Integer id)
    {
        if(!categoriaRepository.existsById(id))return false;
        if(categoriaRepository.possuiAnuncios(id)) throw new IllegalArgumentException(("Categoria possui anuncios e nao pode ser excluida"));
        categoriaRepository.deleteById(id);
        return true;
    }


}
