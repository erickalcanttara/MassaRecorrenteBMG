package utils;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;

import static io.restassured.RestAssured.given;

public class CartoesUtils {

    public String getBodyCards(int idConta, int idPessoa){
        return "{\n" +
                "  \"idConta\": "+ idConta + ",\n" +
                "  \"id_pessoa\": "+ idPessoa + "\n" +
                "}";
    }

    public int geraCartao(String urlSandbox, String pathCartoes, String accessToken, int idConta, int idPessoa) {
        String bodyCards = getBodyCards(idConta, idPessoa);
        String cartoesPath = pathCartoes.replace("{id}", String.valueOf(idConta));

        System.out.println("\n Gerando cartao...");
        Response responseCartao =
                given().
                        header("Content-Type", "application/json").
                        header("Accept", "*/*").
                        header("access_token", accessToken).
                        body(bodyCards).
                when().
                        post(urlSandbox + cartoesPath).
                then().
                        assertThat().
                        statusCode(HttpStatus.SC_OK).
                        extract().response();

        System.out.println("\n Cartao gerado com sucesso! \n");

        JsonPath jsonPath = new JsonPath(responseCartao.asString());
        return Integer.parseInt(jsonPath.getString("idCartao"));
    }

    public String getBodyAlterarEstagio() {
        return "{\n" +
                "  \"id\": 4\n" +
                "}";
    }

    public void alterarEstagioCartao(String urlSandbox, String pathCartoesAlterarEstagio, String accessToken, int idCartao) {
        String bodyAlterarEstagio = getBodyAlterarEstagio();
        String cartoesPath = pathCartoesAlterarEstagio.replace("{id}", String.valueOf(idCartao));

        System.out.println("\n Alterando estagio do cartao...");
        Response responseAlterarEstagio =
                given().
                        header("Content-Type", "application/json").
                        header("Accept", "*/*").
                        header("access_token", accessToken).
                        body(bodyAlterarEstagio).
                when().
                        post(urlSandbox + cartoesPath).
                then().
                        assertThat().
                        statusCode(HttpStatus.SC_OK).
                        extract().response();

        System.out.println("\n Estagio do cartao alterado com sucesso! \n");
    }

    public void desbloquearCartao(String urlSandbox, String pathDesbloqueioCartao, String accessToken, int idCartao) {
        String cartoesPath = pathDesbloqueioCartao.replace("{id}", String.valueOf(idCartao));

        System.out.println("\n Desbloqueando cartao...");
        given().
                header("Content-Type", "application/json").
                header("Accept", "*/*").
                header("access_token", accessToken).
        when().
                post(urlSandbox + cartoesPath).
        then().
                assertThat().
                statusCode(HttpStatus.SC_OK);

        System.out.println("\n Cartao desbloqueado com sucesso! \n");
    }

}
