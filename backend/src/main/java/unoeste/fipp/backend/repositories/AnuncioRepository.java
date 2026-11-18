package unoeste.fipp.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import unoeste.fipp.backend.entities.Anuncio;

@Repository
public interface AnuncioRepository extends JpaRepository<Anuncio, Integer> {
    List<Anuncio> findAllByOrderByAnuDateDescAnuIdDesc();
    @Query("""
            select a from Anuncio a
            where lower(a.anuTitle) like :padrao escape '!'
               or lower(a.anuDesc) like :padrao escape '!'
            order by a.anuDate desc, a.anuId desc
            """)
    List<Anuncio> buscarPorTexto(@Param("padrao") String padrao);

    List<Anuncio> findByUsrIdOrderByAnuDateDescAnuIdDesc(Integer usrId);
    Optional<Anuncio> findByAnuIdAndUsrId(Integer anuId, Integer usrId);
    // o anúncio que contém a pergunta, SE for do usuário (só o dono responde)
    @Query("select a from Anuncio a join a.perguntas p where p.perId = :perId and a.usrId = :usrId")
    Optional<Anuncio> buscarPorPerguntaEDono(@Param("perId") Integer perId, @Param("usrId") Integer usrId);
    @Query(value = "select exists (select 1 from categoria where cat_id = :id)", nativeQuery = true)
    boolean categoriaExiste(@Param("id") Integer id);
}
