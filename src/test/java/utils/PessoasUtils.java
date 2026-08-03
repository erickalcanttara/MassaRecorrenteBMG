package utils;

import com.github.javafaker.Faker;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class PessoasUtils {

    static Faker faker = new Faker(new Locale("pt-BR"));

    public Map<String, String> getIndividualsData(){

        GeradorCpfCnpjRg genarator = new GeradorCpfCnpjRg();

        String name = faker.name().firstName() + " " + faker.name().lastName();
        String cpf = genarator.cpf(false);
        String rg = genarator.rg(false);

        Map<String, String> individualData = new LinkedHashMap<>();
        individualData.put("name", name);
        individualData.put("cpf", cpf);
        individualData.put("rg", rg);

        return individualData;
    }

    public int geraPessoa(String urlSandbox, String pathPessoas, String accessToken) {
        Map<String, String> individualData = getIndividualsData();

        System.out.println("\n Cadastrando dados da Pessoa...");
        Response responseIndividual =
                given().
                        header("Content-Type", "application/json").
                        header("Accept", "*/*").
                        header("access_token", accessToken).
                        queryParam("nome", individualData.get("name")).
                        queryParam("tipo", "PF").
                        queryParam("dataNascimento", "1990-01-01").
                        queryParam("cpf", individualData.get("cpf")).
                        queryParam("numeroIdentidade", individualData.get("rg")).
                        queryParam("orgaoExpedidorIdentidade", "SSP").
                        queryParam("unidadeFederativaIdentidade", "SP").
                when().
                        post(urlSandbox + pathPessoas).
                then().
                        assertThat().
                        statusCode(HttpStatus.SC_OK).
                        extract().response();

        JsonPath jsonPath = new JsonPath(responseIndividual.asString());
        return Integer.parseInt(jsonPath.getString("id"));
    }

}
