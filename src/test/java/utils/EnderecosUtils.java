package utils;

import com.github.javafaker.Faker;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class EnderecosUtils {

    static Faker faker = new Faker(new Locale("pt-BR"));

    public Map<String, String> getEnderecosData() {

        String cep = faker.address().zipCode().replaceAll("\\D", "");
        String logradouro = faker.address().streetName();
        String numero = String.valueOf(faker.number().numberBetween(1, 1000));
        String bairro = faker.address().cityName();
        String cidade = faker.address().city();
        String uf = faker.address().stateAbbr();
        String country = faker.address().country();


        Map<String, String> enderecosData = new LinkedHashMap<>();

        enderecosData.put("cep", cep);
        enderecosData.put("logradouro", logradouro);
        enderecosData.put("numero", numero);
        enderecosData.put("bairro", bairro);
        enderecosData.put("cidade", cidade);
        enderecosData.put("uf", uf);
        enderecosData.put("country", country);

        return enderecosData;
    }

    public int geraEndereco(String urlSandbox, String pathEnderecos, String accessToken, int idPessoa) {
        Map<String, String> enderecoDetalheData = getEnderecosData();

        System.out.println("\n Cadastrando endereco...");
        Response responseEndereco =
                given().
                        header("Content-Type", "application/json").
                        header("Accept", "*/*").
                        header("access_token", accessToken).
                        queryParam("idPessoa", idPessoa).
                        queryParam("idTipoEndereco", 1).
                        queryParam("cep", enderecoDetalheData.get("cep")).
                        queryParam("logradouro", enderecoDetalheData.get("logradouro")).
                        queryParam("numero", enderecoDetalheData.get("numero")).
                        queryParam("bairro", enderecoDetalheData.get("bairro")).
                        queryParam("cidade", enderecoDetalheData.get("cidade")).
                        queryParam("uf", enderecoDetalheData.get("uf")).
                        queryParam("country", enderecoDetalheData.get("country")).
                when().
                        post(urlSandbox + pathEnderecos).
                then().
                        assertThat().
                        statusCode(HttpStatus.SC_OK).
                        extract().response();

        JsonPath jsonPath = new JsonPath(responseEndereco.asString());
        return Integer.parseInt(jsonPath.getString("id"));
    }
}
