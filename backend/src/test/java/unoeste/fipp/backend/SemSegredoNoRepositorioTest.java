package unoeste.fipp.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

// O repositório é público: senha do banco e chave do JWT não podem estar escritas em arquivo versionado.
class SemSegredoNoRepositorioTest {

    private static final Path FONTES = Path.of("src/main");

    @Test
    void applicationPropertiesNaoTemSenhaDoBanco() throws IOException {
        List<String> linhas = Files.readAllLines(FONTES.resolve("resources/application.properties"));
        for (String linha : linhas) {
            String l = linha.trim();
            if (l.startsWith("spring.datasource.password")) {
                String valor = l.substring(l.indexOf('=') + 1).trim();
                assertTrue(valor.isEmpty() || valor.startsWith("${"), "senha literal no application.properties: " + l);
            }
        }
    }

    @Test
    void asSenhasVemDeArquivoLocalForaDoGit() throws IOException {
        String conteudo = Files.readString(FONTES.resolve("resources/application.properties"));
        assertTrue(conteudo.contains("application-local.properties"), "o application.properties deve importar o arquivo local");
        assertTrue(Files.readString(Path.of("../.gitignore")).contains("backend/application-local.properties"),
                "o arquivo local precisa estar no .gitignore");
    }

    @Test
    void aChaveAntigaDoJwtNaoEstaEmNenhumArquivoDeProducao() throws IOException {
        try (Stream<Path> arquivos = Files.walk(FONTES)) {
            long achados = arquivos.filter(Files::isRegularFile).filter(p -> {
                try {
                    return Files.readString(p).contains("MINHA" + "CHAVESECRETA");
                } catch (IOException e) {
                    return false;
                }
            }).count();
            assertEquals(0, achados);
        }
    }
}
