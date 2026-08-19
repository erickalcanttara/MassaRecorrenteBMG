package massaRecorrente;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.concurrent.Task;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import individuals.GenarateAccountAndCardTest;
import queries.MassaRecorrenteQueries;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VencimentoMassaScreen extends Application {

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HORA_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final MassaRecorrenteQueries queries = new MassaRecorrenteQueries();
    private final TextField dataVencimentoField = new TextField();
    private final TextArea outputArea = new TextArea();
    private final TableView<Map<String, Object>> tableView = new TableView<Map<String, Object>>();

    @Override
    public void start(Stage primaryStage) {
        Label tituloLabel = new Label("Massa Recorrente");
        tituloLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label dataLabel = new Label("Data vencimento (yyyy-MM-dd):");
        dataVencimentoField.setPromptText("2026-09-01");

        Button gerarButton = new Button("Gerar Massa");
        gerarButton.setOnAction(event -> gerar());

        HBox inputBox = new HBox(10, dataLabel, dataVencimentoField, gerarButton);
        inputBox.setPadding(new Insets(10));

        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setPrefRowCount(12);

        tableView.setPlaceholder(new Label("As informações de vencimento serão exibidas aqui."));
        tableView.setPrefHeight(180);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        VBox content = new VBox(10, tituloLabel, inputBox, tableView, outputArea);
        content.setPadding(new Insets(15));

        BorderPane root = new BorderPane(content);
        Scene scene = new Scene(root, 900, 550);

        primaryStage.setTitle("Massa Recorrente");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void gerar() {
        String dataVencimento = dataVencimentoField.getText().trim();

        if (dataVencimento.isEmpty()) {
            mostrarErro("Informe a data de vencimento.");
            return;
        }

        if (!dataEhValida(dataVencimento)) {
            mostrarErro("Data inválida. Informe a data no formato yyyy-MM-dd.");
            return;
        }

        int diaVencimento = LocalDate.parse(dataVencimento, DATA_FORMATTER).getDayOfMonth();
        outputArea.setText("Executando insert...\n");
        outputArea.appendText("Hora inicio do processamento de gerar massa: " + horaAtual() + "\n");
        executarInsertEConfirmar(dataVencimento, diaVencimento);
    }

    private boolean dataEhValida(String dataVencimento) {
        try {
            LocalDate.parse(dataVencimento, DATA_FORMATTER);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private void mostrarErro(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro de validação");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }

    private void executarInsertEConfirmar(String dataVencimento, int diaVencimento) {
        Task<MassaRecorrenteQueries.InsercaoVencimentoContexto> task = new Task<MassaRecorrenteQueries.InsercaoVencimentoContexto>() {
            @Override
            protected MassaRecorrenteQueries.InsercaoVencimentoContexto call() {
                return queries.buildInsertControleProcessosProcedures(dataVencimento);
            }
        };

        task.setOnSucceeded(event -> {
            MassaRecorrenteQueries.InsercaoVencimentoContexto contexto = task.getValue();
            Map<String, Object> informacaoVencimento = contexto.getInformacaoVencimento();
            outputArea.appendText("Insert executado com sucesso.\n");
            outputArea.appendText("Informacoes de vencimento carregadas na tabela.\n");
            preencherTabela(informacaoVencimento);

            Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacao.setTitle("Confirmacao");
            confirmacao.setHeaderText(null);
            confirmacao.setContentText("As informacoes de vencimento estao corretas?");
            ButtonType sim = ButtonType.YES;
            ButtonType nao = ButtonType.NO;
            confirmacao.getButtonTypes().setAll(sim, nao);

            Optional<ButtonType> resposta = confirmacao.showAndWait();
            if (!resposta.isPresent() || resposta.get() != sim) {
                contexto.rollback();
                outputArea.appendText("Execucao cancelada.\n");
                return;
            }

            contexto.commit();
            GenarateAccountAndCardTest.setDiaVencimento(diaVencimento);
            GenarateAccountAndCardTest.setDataVencimento(dataVencimento);
            String gerarComprasAte = queries.getGerarComprasAte(dataVencimento);
            GenarateAccountAndCardTest.setGerarComprasAte(gerarComprasAte);
            outputArea.appendText("Data gerarComprasAte: " + gerarComprasAte + "\n");
            outputArea.appendText("\nCriando contas e cartões...\n");
            executarGenarateAccountAndCardTest(diaVencimento);
        });

        task.setOnFailed(event -> mostrarErro("Erro ao executar insert: " + task.getException().getMessage()));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void preencherTabela(Map<String, Object> informacaoVencimento) {
        tableView.getItems().clear();
        tableView.getColumns().clear();

        if (informacaoVencimento == null || informacaoVencimento.isEmpty()) {
            return;
        }

        ObservableList<Map<String, Object>> itens = FXCollections.observableArrayList();
        itens.add(informacaoVencimento);
        tableView.setItems(itens);

        for (String chave : informacaoVencimento.keySet()) {
            TableColumn<Map<String, Object>, String> coluna = new TableColumn<Map<String, Object>, String>(chave);
            coluna.setText(chave);
            coluna.setCellValueFactory(cellData -> new ReadOnlyStringWrapper(
                    String.valueOf(cellData.getValue().get(chave))
            ));
            coluna.setMinWidth(90);
            coluna.setPrefWidth(120);
            tableView.getColumns().add(coluna);
        }
    }

    private void executarGenarateAccountAndCardTest(int diaVencimento) {
        Task<String> task = new Task<String>() {
            @Override
            protected String call() {
                GenarateAccountAndCardTest.setDiaVencimento(diaVencimento);
                GenarateAccountAndCardTest.setOutputConsumer(VencimentoMassaScreen.this::appendOutput);
                Set<String> cenariosReportados = ConcurrentHashMap.newKeySet();
                SummaryGeneratingListener listener = new SummaryGeneratingListener();
                TestExecutionListener progressoListener = new TestExecutionListener() {
                    @Override
                    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
                        if (!testIdentifier.isContainer()) {
                            return;
                        }

                        String mensagem = obterMensagemCenario(testIdentifier.getDisplayName());
                        if (mensagem == null) {
                            return;
                        }

                        if (cenariosReportados.add(mensagem)) {
                            appendOutput(mensagem + "\n");
                        }
                    }
                };
                LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                        .selectors(DiscoverySelectors.selectClass(GenarateAccountAndCardTest.class))
                        .build();

                Launcher launcher = LauncherFactory.create();
                launcher.execute(request, progressoListener, listener);
                appendOutput("Hora fim do processamento de gerar massa: " + horaAtual() + "\n");

                TestExecutionSummary summary = listener.getSummary();
                StringBuilder resultado = new StringBuilder();
                resultado.append("Tests found: ").append(summary.getTestsFoundCount()).append('\n');
                resultado.append("Tests succeeded: ").append(summary.getTestsSucceededCount()).append('\n');
                resultado.append("Tests failed: ").append(summary.getTestsFailedCount()).append('\n');

                if (summary.getTotalFailureCount() > 0) {
                    for (TestExecutionSummary.Failure failure : summary.getFailures()) {
                        resultado.append('\n')
                                .append(failure.getTestIdentifier().getDisplayName())
                                .append(" -> ")
                                .append(failure.getException().getMessage())
                                .append('\n');
                    }
                }

                return resultado.toString();
            }
        };

        task.setOnSucceeded(event -> outputArea.appendText(task.getValue()));
        task.setOnFailed(event -> mostrarErro("Erro ao executar GenarateAccountAndCardTest: " + task.getException().getMessage()));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private String obterMensagemCenario(String displayName) {
        if (displayName.contains("Cenario1")) {
            return "Cenario 1 finalizado.";
        }
        if (displayName.contains("Cenario2")) {
            return "Cenario 2 finalizado.";
        }
        if (displayName.contains("Cenario3")) {
            return "Cenario 3 finalizado.";
        }
        if (displayName.contains("Cenario4")) {
            return "Cenario 4 finalizado.";
        }
        if (displayName.contains("Cenario5")) {
            return "Cenario 5 finalizado.";
        }
        return null;
    }

    private void appendOutput(String mensagem) {
        Platform.runLater(() -> outputArea.appendText(mensagem));
    }

    private String horaAtual() {
        return LocalDateTime.now().format(HORA_FORMATTER);
    }

    public static void main(String[] args) {
        VencimentoMassaLauncher.main(args);
    }
}
