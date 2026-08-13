package massaRecorrente;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import queries.MassaRecorrenteQueries;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class VencimentoMassaScreen extends Application {

    private static final DateTimeFormatter DATA_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final MassaRecorrenteQueries queries = new MassaRecorrenteQueries();
    private final TextField dataVencimentoField = new TextField();
    private final TextArea outputArea = new TextArea();

    @Override
    public void start(Stage primaryStage) {
        Label tituloLabel = new Label("Massa Recorrente");
        tituloLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label dataLabel = new Label("Data vencimento (yyyy-MM-dd):");
        dataVencimentoField.setPromptText("2026-09-01");

        Button gerarButton = new Button("Gerar");
        gerarButton.setOnAction(event -> gerar());

        HBox inputBox = new HBox(10, dataLabel, dataVencimentoField, gerarButton);
        inputBox.setPadding(new Insets(10));

        outputArea.setEditable(false);
        outputArea.setWrapText(true);
        outputArea.setPrefRowCount(12);

        VBox content = new VBox(10, tituloLabel, inputBox, outputArea);
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

        String sql = queries.buildInsertControleProcessosProcedures(dataVencimento);
        outputArea.setText(sql);
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

    public static void main(String[] args) {
        VencimentoMassaLauncher.main(args);
    }
}
