package unoeste.fipp.backend.services;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import unoeste.fipp.backend.entities.Anuncio;
import unoeste.fipp.backend.entities.EdicaoAnuncio;
import unoeste.fipp.backend.entities.FotoAnuncio;
import unoeste.fipp.backend.entities.PerguntaAnuncio;
import unoeste.fipp.backend.repositories.AnuncioRepository;

@Service
public class AnuncioService {
    public static final String FILTRO_TODOS = "todos";

    private static final BigDecimal PRECO_MAXIMO = new BigDecimal("999999999.9"); //numeric(10,1)

    //as fotos ficam aqui (relativo à pasta do backend) e saem em /fotos/{fot_file}, ver spring.web.resources.static-locations
    private static final Path PASTA_FOTOS = Path.of("uploads", "fotos");
    private static final Map<String, String> EXTENSOES = Map.of(
            "image/jpeg", ".jpg", "image/png", ".png", "image/gif", ".gif", "image/webp", ".webp");

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

    // Cria um anúncio do usuário (a data é a de hoje). Dado inválido: IllegalArgumentException com o texto para o usuário.
    @Transactional
    public Anuncio criar(Integer usrId, EdicaoAnuncio dados) {
        Anuncio anuncio = new Anuncio();
        anuncio.setUsrId(usrId);
        anuncio.setAnuDate(LocalDate.now());
        aplicar(anuncio, dados);
        anuncioRepository.saveAndFlush(anuncio);
        entityManager.refresh(anuncio); //preenche cat_name e usr_name (@Formula)
        carregarFotosEPerguntas(anuncio);
        return anuncio;
    }

    // Edita um anúncio SE ele for do usuário. Vazio = não existe ou é de outro (o chamador responde igual nos dois casos).
    // Dado inválido: IllegalArgumentException com o texto para o usuário.
    @Transactional
    public Optional<Anuncio> editar(Integer usrId, Integer anuId, EdicaoAnuncio dados) {
        Optional<Anuncio> encontrado = anuncioRepository.findByAnuIdAndUsrId(anuId, usrId);
        if (encontrado.isEmpty()) return encontrado;
        Anuncio anuncio = encontrado.get();
        aplicar(anuncio, dados);
        anuncioRepository.flush();
        entityManager.refresh(anuncio); //recalcula cat_name (@Formula)
        carregarFotosEPerguntas(anuncio);
        return Optional.of(anuncio);
    }

    // Grava as fotos no disco e no anúncio, SE ele for do usuário. Vazio = não existe ou é de outro.
    // Foto inválida (vazia ou que não é JPG/PNG/GIF/WEBP): IllegalArgumentException; nesse caso nada é gravado.
    @Transactional
    public Optional<Anuncio> adicionarFotos(Integer usrId, Integer anuId, List<MultipartFile> fotos) throws IOException {
        Optional<Anuncio> encontrado = anuncioRepository.findByAnuIdAndUsrId(anuId, usrId);
        if (encontrado.isEmpty()) return encontrado;
        List<MultipartFile> arquivos = fotos == null ? List.of() : fotos.stream().filter(f -> !f.isEmpty()).toList();
        if (arquivos.isEmpty()) throw new IllegalArgumentException("Envie ao menos uma foto");
        for (MultipartFile arquivo : arquivos)
            if (extensao(arquivo) == null) throw new IllegalArgumentException("Só são aceitas fotos JPG, PNG, GIF ou WEBP");

        Anuncio anuncio = encontrado.get();
        Files.createDirectories(PASTA_FOTOS);
        List<String> gravados = new ArrayList<>();
        try {
            for (MultipartFile arquivo : arquivos) {
                //nome sorteado (cabe em fot_file, 20): o nome enviado pelo cliente nunca vira caminho no disco
                String nome = UUID.randomUUID().toString().replace("-", "").substring(0, 12) + extensao(arquivo);
                try (InputStream entrada = arquivo.getInputStream()) {
                    Files.copy(entrada, PASTA_FOTOS.resolve(nome));
                }
                gravados.add(nome);
                FotoAnuncio foto = new FotoAnuncio();
                foto.setFotFile(nome);
                foto.setAnuncio(anuncio);
                anuncio.getFotos().add(foto);
            }
            anuncioRepository.flush();
        } catch (IOException | RuntimeException e) {
            gravados.forEach(this::apagarArquivo); //não deixa foto órfã no disco
            throw e;
        }
        carregarFotosEPerguntas(anuncio);
        return Optional.of(anuncio);
    }

    // Pergunta num anúncio de OUTRO usuário. false = o anúncio não existe.
    // Texto vazio ou anúncio do próprio usuário: IllegalArgumentException com o texto para o usuário.
    @Transactional
    public boolean perguntar(Integer usrId, Integer anuId, String texto) {
        Optional<Anuncio> encontrado = anuncioRepository.findById(anuId);
        if (encontrado.isEmpty()) return false;
        Anuncio anuncio = encontrado.get();
        if (usrId.equals(anuncio.getUsrId()))
            throw new IllegalArgumentException("Você não pode fazer perguntas no seu próprio anúncio");
        PerguntaAnuncio pergunta = new PerguntaAnuncio();
        pergunta.setPerText(textoObrigatorio(texto, "Informe o texto da pergunta"));
        pergunta.setAnuncio(anuncio);
        anuncio.getPerguntas().add(pergunta);
        anuncioRepository.flush();
        return true;
    }

    // Responde uma pergunta SE o anúncio dela for do usuário. false = a pergunta não existe ou é de anúncio de outro.
    // Texto vazio: IllegalArgumentException com o texto para o usuário.
    @Transactional
    public boolean responder(Integer usrId, Integer perId, String texto) {
        Optional<Anuncio> encontrado = anuncioRepository.buscarPorPerguntaEDono(perId, usrId);
        if (encontrado.isEmpty()) return false;
        String resposta = textoObrigatorio(texto, "Informe o texto da resposta");
        encontrado.get().getPerguntas().stream()
                .filter(p -> perId.equals(p.getPerId()))
                .forEach(p -> p.setPerResp(resposta));
        anuncioRepository.flush();
        return true;
    }

    @Transactional
    public boolean excluir(Integer id) {
        return apagar(anuncioRepository.findById(id));
    }

    // Exclui um anúncio SE ele for do usuário. false = não existe ou é de outro.
    @Transactional
    public boolean excluirDoDono(Integer usrId, Integer id) {
        return apagar(anuncioRepository.findByAnuIdAndUsrId(id, usrId));
    }

    // fotos e perguntas saem junto (cascade); os arquivos das fotos também
    private boolean apagar(Optional<Anuncio> encontrado) {
        encontrado.ifPresent(anuncio -> {
            List<String> arquivos = anuncio.getFotos().stream().map(FotoAnuncio::getFotFile).toList();
            anuncioRepository.delete(anuncio);
            anuncioRepository.flush();
            arquivos.forEach(this::apagarArquivo);
        });
        return encontrado.isPresent();
    }

    // valida os dados e passa para o anúncio; nada é alterado se algo estiver inválido
    private void aplicar(Anuncio anuncio, EdicaoAnuncio dados) {
        String titulo = dados.titulo() == null ? "" : dados.titulo().trim();
        if (titulo.isEmpty()) throw new IllegalArgumentException("Informe o título do anúncio");
        if (titulo.length() > 80) throw new IllegalArgumentException("O título pode ter no máximo 80 caracteres");
        if (dados.preco() == null) throw new IllegalArgumentException("Informe o preço");
        BigDecimal preco = dados.preco().setScale(1, RoundingMode.HALF_UP); //a coluna guarda 1 casa decimal
        if (preco.signum() < 0 || preco.compareTo(PRECO_MAXIMO) > 0)
            throw new IllegalArgumentException("Preço fora do limite permitido");
        if (dados.categoria() == null || !anuncioRepository.categoriaExiste(dados.categoria()))
            throw new IllegalArgumentException("Categoria inexistente");

        anuncio.setAnuTitle(titulo);
        anuncio.setAnuDesc(dados.descricao());
        anuncio.setAnuPrice(preco);
        anuncio.setCatId(dados.categoria());
    }

    private String textoObrigatorio(String texto, String mensagem) {
        String aparado = texto == null ? "" : texto.trim();
        if (aparado.isEmpty()) throw new IllegalArgumentException(mensagem);
        return aparado;
    }

    //null = tipo de arquivo não aceito
    private String extensao(MultipartFile arquivo) {
        String tipo = arquivo.getContentType();
        return tipo == null ? null : EXTENSOES.get(tipo.toLowerCase(Locale.ROOT));
    }

    private void apagarArquivo(String nome) {
        try {
            Files.deleteIfExists(PASTA_FOTOS.resolve(Path.of(nome).getFileName()));
        } catch (IOException e) {
            //arquivo que não saiu não pode impedir a exclusão do anúncio
        }
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
