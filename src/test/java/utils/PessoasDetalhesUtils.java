package utils;

import com.github.javafaker.Faker;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class PessoasDetalhesUtils {

    static Faker faker = new Faker(new Locale("pt-BR"));

    public Map<String, String> getPessoasDetalhesData(){

        String nomeMae = faker.name().firstName() + " " + faker.name().lastName();
        String email = faker.internet().emailAddress();

        Map<String, String> pessoasDetalhesData = new LinkedHashMap<>();

        pessoasDetalhesData.put("nomeMae", nomeMae);
        pessoasDetalhesData.put("email", email);

        return pessoasDetalhesData;
    }

    public int geraPessoaDetalhes(String urlSandbox, String pathPessoasDetalhes, String accessToken,
                                  int idPessoa, int randomNumeroAgencia, int randomContaCorrente) {
        Map<String, String> pessoaDetalhesData = getPessoasDetalhesData();

        System.out.println("\n Cadastrando pessoas detalhes...");
        Response responsePessoasDetalhes =
                given().
                        header("Content-Type", "application/json").
                        header("Accept", "*/*").
                        header("access_token", accessToken).
                        queryParam("idPessoa", idPessoa).
                        queryParam("idNacionalidade", 1).
                        queryParam("numeroBanco", 318).
                        queryParam("numeroAgencia", randomNumeroAgencia).
                        queryParam("numeroContaCorrente", randomContaCorrente).
                        queryParam("email", pessoaDetalhesData.get("email")).
                        queryParam("salario", 10000).
                        queryParam("patrimonioTotal", 10000).
                        queryParam("grauInstrucao", 0).
                        queryParam("numeroDependentes", 0).
                        queryParam("pessoaPoliticamenteExposta", false).
                        queryParam("flagNomePaiNaoInformado", false).
                        queryParam("flagSemEnderecoComercialFixo", false).
                when().
                        post(urlSandbox + pathPessoasDetalhes).
                then().
                        assertThat().
                        statusCode(HttpStatus.SC_OK).
                        extract().response();

        JsonPath jsonPath = new JsonPath(responsePessoasDetalhes.asString());
        return Integer.parseInt(jsonPath.getString("idPessoa"));
    }

}
