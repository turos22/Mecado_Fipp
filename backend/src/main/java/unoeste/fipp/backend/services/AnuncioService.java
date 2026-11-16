package unoeste.fipp.backend.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import unoeste.fipp.backend.entities.Anuncio;
import unoeste.fipp.backend.entities.EdicaoAnuncio;
import unoeste.fipp.backend.repositories.AnuncioRepository;

@Service
public class AnuncioService {
    public static final String FILTRO_TODOS = "todos";

    private static final BigDecimal PRECO_MAXIMO = new BigDecimal("999999999.9"); //numeric(10,1)

    private final AnuncioRepository anuncioRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public AnuncioService(AnuncioRepository anuncioRepository) {
        this.anuncioRepository = anuncioRepository;
    }

    @Transactional(readOnly = true)
    public List<Anuncio> filtrar(String filtro) {
        String texto = filtro == null ? "" : filtro.trim();
        List<Anuncio> anuncios = texto.isEmpty() || texto.equalsIgnoreCase(FILTRO_TODOS)
                ? anuncioRepository.findAllByOrderByAnuDateDescAnuIdDesc()
                : anuncioRepository.buscarPorTexto(padraoDeBusca(texto));
        anuncios.forEach(this::carregarFotosEPerguntas);
        return anuncios;
    }

    @Transactional(readOnly = true)
    public Optional<Anuncio> buscarPorId(Integer id) {
        Optional<Anuncio> anuncio = anuncioRepository.findById(id);
        anuncio.ifPresent(this::carregarFotosEPerguntas);
        return anuncio;
    }

    @Transactional(readOnly = true)
    public List<Anuncio> listarDoDono(Integer usrId) {
        List<Anuncio> anuncios = anuncioRepository.findByUsrIdOrderByAnuDateDescAnuIdDesc(usrId);
        anuncios.forEach(this::carregarFotosEPerguntas);
        return anuncios;
    }

    // Edita um anúncio SE ele for do usuário. Vazio = não existe ou é de outro (o chamador responde igual nos dois casos).
    // Dado inválido: IllegalArgumentException com o texto para o usuário.
    @Transactional
    public Optional<Anuncio> editar(Integer usrId, Integer anuId, EdicaoAnuncio dados) {
        Optional<Anuncio> encontrado = anuncioRepository.findByAnuIdAndUsrId(anuId, usrId);
        if (encontrado.isEmpty()) return encontrado;
        String titulo = dados.titulo() == null ? "" : dados.titulo().trim();
        if (titulo.isEmpty()) throw new IllegalArgumentException("Informe o título do anúncio");
        if (titulo.length() > 80) throw new IllegalArgumentException("O título pode ter no máximo 80 caracteres");
        if (dados.preco() == null) throw new IllegalArgumentException("Informe o preço");
        BigDecimal preco = dados.preco().setScale(1, RoundingMode.HALF_UP); //a coluna guarda 1 casa decimal
        if (preco.signum() < 0 || preco.compareTo(PRECO_MAXIMO) > 0)
            throw new IllegalArgumentException("Preço fora do limite permitido");
        if (dados.categoria() == null || !anuncioRepository.categoriaExiste(dados.categoria()))
            throw new IllegalArgumentException("Categoria inexistente");

        Anuncio anuncio = encontrado.get();
        anuncio.setAnuTitle(titulo);
        anuncio.setAnuDesc(dados.descricao());
        anuncio.setAnuPrice(preco);
        anuncio.setCatId(dados.categoria());
        anuncioRepository.flush();
        entityManager.refresh(anuncio); //recalcula cat_name (@Formula)
        carregarFotosEPerguntas(anuncio);
        return Optional.of(anuncio);
    }

    @Transactional
    public boolean excluir(Integer id) {
        if (!anuncioRepository.existsById(id)) return false;
        anuncioRepository.deleteById(id);
        return true;
    }

    private void carregarFotosEPerguntas(Anuncio anuncio) {
        Hibernate.initialize(anuncio.getFotos());
        Hibernate.initialize(anuncio.getPerguntas());
    }

    private String padraoDeBusca(String texto) {
        String protegido = texto.toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + protegido + "%";
    }
}
