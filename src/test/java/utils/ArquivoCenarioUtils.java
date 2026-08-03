package utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.stream.Stream;

public class ArquivoCenarioUtils {

    private Path getDiretorioCenario(String nomeCenario) {
        return Paths.get("src", "test", "resources", nomeCenario);
    }

    private void limparConteudoDiretorio(Path diretorio) {
        try {
            if (!Files.exists(diretorio)) {
                Files.createDirectories(diretorio);
                return;
            }

            try (Stream<Path> paths = Files.walk(diretorio)) {
                paths
                        .filter(path -> !path.equals(diretorio))
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                throw new RuntimeException("Erro ao remover arquivo da pasta " + diretorio, e);
                            }
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao limpar pasta " + diretorio, e);
        }
    }

    public void limparPastasResultadosEmResources() {
        Path resources = Paths.get("src", "test", "resources");

        try {
            Files.createDirectories(resources);

            try (Stream<Path> paths = Files.walk(resources)) {
                paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().startsWith("ids-conta-cartao"))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                throw new RuntimeException("Erro ao remover arquivo de ids em resources: " + path, e);
                            }
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao varrer pastas de resultados em resources", e);
        }
    }

    public void limparArquivosCenario(String nomeCenario) {
        Path diretorioCenario = getDiretorioCenario(nomeCenario);
        limparConteudoDiretorio(diretorioCenario);
    }

    public void gravaIdsContaECartao(String nomeCenario, int idConta, int idCartao) {
        Path diretorioCenario = getDiretorioCenario(nomeCenario);
        Path arquivoIds = diretorioCenario.resolve("ids-conta-cartao-" + nomeCenario + ".csv");

        try {
            Files.createDirectories(diretorioCenario);

            if (!Files.exists(arquivoIds)) {
                Files.write(
                        arquivoIds,
                        "id_conta,id_cartao\n".getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE
                );
            }

            String linha = idConta + "," + idCartao + System.lineSeparator();
            Files.write(
                    arquivoIds,
                    linha.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new RuntimeException("Erro ao gravar ids no arquivo do " + nomeCenario, e);
        }
    }
}

