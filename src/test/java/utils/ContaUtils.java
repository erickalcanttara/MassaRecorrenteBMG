package utils;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;

import static io.restassured.RestAssured.given;

public class ContaUtils {

    public String getBodyAccount(int idPessoa, int idOrigemComercial, int idProduto, int diaVencimento, int idEnderecoCorrespondencia){
        return "{\n" +
                "  \"idPessoa\":" + idPessoa + ",\n" +
                "  \"idOrigemComercial\":" + idOrigemComercial + ",\n" +
                "  \"idProduto\":" + idProduto + ",\n" +
                "  \"diaVencimento\": " + diaVencimento + ",\n" +
                "  \"valorRenda\": 10000,\n" +
                "  \"idEnderecoCorrespondencia\":" + idEnderecoCorrespondencia + ",\n" +
                "  \"limiteGlobal\": 10000,\n" +
                "  \"limiteMaximo\": 10000,\n" +
                "  \"limiteParcelas\": 10000,\n" +
                "  \"limiteConsignado\": 200,\n" +
                "  \"flagFaturaPorEmail\": 0,\n" +
                "  \"idStatusConta\": 0,\n" +
                "  \"behaviorScore\": 0,\n" +
                "  \"valorPontuacao\": 0,\n" +
                "  \"funcaoAtiva\": \"DEBITOCREDITO\"\n" +
                "}";
    }

    public int cadastraConta(String urlSandbox, String pathContas, String accessToken,
                             int idPessoa, int idEndereco, int idOrigemComercial,
                             int idProduto, int diaVencimento) {
        String bodyAccount = getBodyAccount(idPessoa, idOrigemComercial, idProduto, diaVencimento, idEndereco);

        System.out.println("\n Cadastrando conta...");
        Response responseAccount =
                given().
                        header("Content-Type", "application/json").
                        header("Accept", "*/*").
                        header("access_token", accessToken).
                        body(bodyAccount).
                when().
                        post(urlSandbox + pathContas).
                then().
                        assertThat().
                        statusCode(HttpStatus.SC_OK).
                        extract().response();

        System.out.println("\n Conta cadastrada com sucesso! \n");

        JsonPath jsonPath = new JsonPath(responseAccount.asString());
        return Integer.parseInt(jsonPath.getString("id"));
    }
}
