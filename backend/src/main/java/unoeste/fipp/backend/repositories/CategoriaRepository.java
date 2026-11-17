package unoeste.fipp.backend.repositories;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



import unoeste.fipp.backend.entities.Categoria;



public interface CategoriaRepository extends JpaRepository<Categoria, Integer> {

    boolean existsByCatNameIgnoreCase(String nome);
    boolean existsByCatNameIgnoreCaseAndCatIdNot(String nome, Integer id);
    @Query(value = "select exists (select 1 from anuncio where cat_id = :id)", nativeQuery = true)
    boolean possuiAnuncios(@Param("id") Integer id);
}

