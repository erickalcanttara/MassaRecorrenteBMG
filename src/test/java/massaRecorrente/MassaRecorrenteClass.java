package massaRecorrente;

import org.junit.Test;
import org.testng.SkipException;
import queries.MassaRecorrenteQueries;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Objects;

public class MassaRecorrenteClass {

    private final MassaRecorrenteQueries mr = new MassaRecorrenteQueries();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private Map<String, Object> getAccount;

    @Test
    public void getAccount() {
        getAccount = mr.searchAccounts();

        if (Objects.isNull(getAccount)) {
            throw new SkipException("Não existe massa válida na base de dados do emissor.");
        }
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(getAccount.get("Id_Conta"));
            System.out.println(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Erro ao converter conta para JSON", e);
        }
    }
}

