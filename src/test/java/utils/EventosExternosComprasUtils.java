package utils;

import io.restassured.response.Response;
import org.apache.http.HttpStatus;

import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;

public class EventosExternosComprasUtils {

    private static final String SUFIXO_DATA_COMPRA = "T01:30:00.000-05:00";

    private String montarDataCompra(String dataBase) {
        return dataBase + SUFIXO_DATA_COMPRA;
    }

    public String getBodyEventoCompraAvista(int idConta, int idCartao, double valor, String dataCompra) {
        return "{\n" +
                "  \"idEstabelecimento\": 1,\n" +
                "  \"idConta\": " + idConta + ",\n" +
                "  \"idCartao\": " + idCartao + ",\n" +
                "  \"dataCompra\": \"" + montarDataCompra(dataCompra) + "\",\n" +
                "  \"idOperacao\": 10552,\n" + // 10552 à vista Elo - 270 parcelado logista Mastercard
                "  \"numeroParcelas\": 1,\n" +
                "  \"valorParcela\": " + valor + ",\n" +
                "  \"valorContrato\": " + valor + ",\n" +
                "  \"valorCompra\": " + valor + ",\n" +
                "  \"valorEncargosTotais\": 0,\n" +
                "  \"taxaJuros\": 0,\n" +
                "  \"valorIOF\": 0,\n" +
                "  \"valorTAC\": 0,\n" +
                "  \"origem\": \"TST\",\n" +
                "  \"carencia\": 0,\n" +
                "  \"nomeEstabelecimento\": \"TESTE\",\n" +
                "  \"valorPrimeiraParcela\": 0\n" +
                "}";
    }

    public String getBodyEventoCompraParcelado(int idConta, int idCartao, int qtdParcelas, int valorParcela, String dataCompra) {
        return "{\n" +
                "  \"idEstabelecimento\": 1,\n" +
                "  \"idConta\": " + idConta + ",\n" +
                "  \"idCartao\": " + idCartao + ",\n" +
                "  \"dataCompra\": \"" + montarDataCompra(dataCompra) + "\",\n" +
                "  \"idOperacao\": 10557,\n" + // 10557 parcelado logista elo - 28 parcelado logista Mastercard
                "  \"numeroParcelas\": " + qtdParcelas + ",\n" +
                "  \"valorParcela\": " + valorParcela + ",\n" +
                "  \"valorContrato\": " + (qtdParcelas * valorParcela) + ",\n" +
                "  \"valorCompra\": " + (qtdParcelas * valorParcela) + ",\n" +
                "  \"valorEncargosTotais\": 0,\n" +
                "  \"taxaJuros\": 0,\n" +
                "  \"valorIOF\": 0,\n" +
                "  \"valorTAC\": 0,\n" +
                "  \"origem\": \"TST\",\n" +
                "  \"carencia\": 0,\n" +
                "  \"nomeEstabelecimento\": \"TESTE\",\n" +
                "  \"valorPrimeiraParcela\": " + valorParcela + "\n" +
                "}";
    }

    public Response gerarCompraAvista(String urlSandbox, String pathEventosCompras, String accessToken,
                                int idConta, int idCartao, int valorCompraMinimo, int valorCompraMaximo, String dataCompra) {
        int valorCompra = ThreadLocalRandom.current().nextInt(valorCompraMinimo, valorCompraMaximo + 1);
        String bodyEventoCompra = getBodyEventoCompraAvista(idConta, idCartao, valorCompra, dataCompra);

        System.out.println("\n Gerando evento externo de compra...");
        Response responseEventoCompra =
                given().
                        header("Content-Type", "application/json").
                        header("Accept", "*/*").
                        header("access_token", accessToken).
                        body(bodyEventoCompra).
                when().
                        post(urlSandbox + pathEventosCompras).
                then().
                        assertThat().
                        statusCode(HttpStatus.SC_OK).
                        extract().response();

        System.out.println("\n Evento externo de compra gerado com sucesso! \n");
        return responseEventoCompra;
    }

    public Response gerarCompraParcelada(String urlSandbox, String pathEventosCompras, String accessToken,
                                      int idConta, int idCartao, int valorCompraMinimo, int valorCompraMaximo, String dataCompra) {

        int valorCompra = ThreadLocalRandom.current().nextInt(valorCompraMinimo, valorCompraMaximo + 1);
        int randomQtdParcelas = ThreadLocalRandom.current().nextInt(3, 13);
        int limiteValorParcela = Math.max(1, valorCompra / randomQtdParcelas);
        int randomValorParcela = ThreadLocalRandom.current().nextInt(1, limiteValorParcela + 1);

        String bodyEventoCompra = getBodyEventoCompraParcelado(idConta, idCartao, randomQtdParcelas, randomValorParcela, dataCompra);

        System.out.println("\n Gerando evento externo de compra...");
        Response responseEventoCompra =
                given().
                        header("Content-Type", "application/json").
                        header("Accept", "*/*").
                        header("access_token", accessToken).
                        body(bodyEventoCompra).
                        when().
                        post(urlSandbox + pathEventosCompras).
                        then().
                        assertThat().
                        statusCode(HttpStatus.SC_OK).
                        extract().response();

        System.out.println("\n Evento externo de compra gerado com sucesso! \n");
        return responseEventoCompra;
    }
}
