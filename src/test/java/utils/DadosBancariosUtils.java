package utils;

import io.restassured.response.Response;
import org.apache.http.HttpStatus;

import static io.restassured.RestAssured.given;

public class DadosBancariosUtils {

    public String getBodyDadosBancarios(int idConta , int numeroAgencia, int numeroContaCorrente){
        return "{\n" +
                "  \"idConta\":" + idConta + ",\n" +
                "  \"numeroAgencia\":" + numeroAgencia + ",\n" +
                "  \"numeroContaCorrente\":" + numeroContaCorrente + ",\n" +
                "  \"codigoBanco\": 318,\n" +
                "  \"idTipoContaBancaria\": 3\n" +
                "}";
    }

    public Response cadastraDadosBancarios(String urlSandbox, String pathDadosBancarios, String accessToken,
                                  int idConta, int numeroAgencia, int numeroContaCorrente ) {

        String bodyDadosBancarios = getBodyDadosBancarios(idConta, numeroAgencia, numeroContaCorrente);

        System.out.println("\n Cadastrando dados bancarios...");
        Response responseAccount =
                given().
                        header("Content-Type", "application/json").
                        header("Accept", "*/*").
                        header("access_token", accessToken).
                        body(bodyDadosBancarios).
                when().
                        post(urlSandbox + pathDadosBancarios).
                then().
                        assertThat().
                        statusCode(HttpStatus.SC_OK).
                        extract().response();

        System.out.println("\n Dados bancarios cadastrados com sucesso! \n");
        return responseAccount;
    }
}
