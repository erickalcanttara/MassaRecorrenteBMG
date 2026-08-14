package individuals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import utils.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class GenarateAccountAndCardTest {

    static final String NOME_CENARIO1 = "cenario1";
    static final String NOME_CENARIO2 = "cenario2";
    static final String NOME_CENARIO3 = "cenario3";
    static final String NOME_CENARIO4 = "cenario4";
    static final String NOME_CENARIO5 = "cenario5";

    RestProperties restProperties = new RestProperties();
    final String URL_SANDBOX = restProperties.getUrlSandbox();
    final String accessToken = restProperties.getAccessToken();

    final String PATH_PESSOAS = "pessoas";
    final String PATH_PESSOAS_DETALHES = "pessoas-detalhes";
    final String PATH_ENDERECOS = "enderecos";
    final String PATH_CONTAS = "contas";
    final String PATH_DADOS_BANCARIOS = "dados-bancarios-conta";
    final String PATH_CARTOES = "contas/{id}/gerar-cartao-grafica";
    final String PATH_CARTOES_ALTERAR_ESTAGIO = "cartoes/{id}/alterar-estagio";
    final String PATH_DESBLOQUEIO_CARTAO = "cartoes/{id}/desbloquear";
    final String PATH_EVENTOS_COMPRAS = "eventos-externos-compras";

    /*
        Dados a serem modificados para que se gere uma conta com algumas informações específicas
        Por padrão, já serão gerados com os valores padrão abaixo.
     */
    int idOrigemComercial = 11;
    private static volatile Integer diaVencimento;
    private static volatile String gerarComprasAte;

    PessoasUtils pessoa = new PessoasUtils();
    PessoasDetalhesUtils pessoasDetalhesUtils = new PessoasDetalhesUtils();
    EnderecosUtils enderecosUtils = new EnderecosUtils();
    ContaUtils contaUtils = new ContaUtils();
    DadosBancariosUtils dadosBancariosUtils = new DadosBancariosUtils();
    CartoesUtils cartoesUtils = new CartoesUtils();
    ArquivoCenarioUtils arquivoCenarioUtils = new ArquivoCenarioUtils();
    EventosExternosComprasUtils eventosExternosComprasUtils = new EventosExternosComprasUtils();

    public static void setDiaVencimento(int diaVencimento) {
        GenarateAccountAndCardTest.diaVencimento = diaVencimento;
    }

    public static void setGerarComprasAte(String gerarComprasAte) {
        GenarateAccountAndCardTest.gerarComprasAte = gerarComprasAte;
    }

    private int getDiaVencimento() {
        if (diaVencimento == null) {
            throw new IllegalStateException("Dia de vencimento não informado pela tela.");
        }
        return diaVencimento;
    }

    private String getGerarComprasAte() {
        if (gerarComprasAte == null || gerarComprasAte.isEmpty()) {
            throw new IllegalStateException("Data limite para compras não informada pela tela.");
        }
        return gerarComprasAte;
    }

    private String getGerarComprasAteMenosCincoDias() {
        LocalDate dataBase = LocalDate.parse(getGerarComprasAte());
        return dataBase.minusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static class DadosContaBase {
        final int idPessoa;
        final int idEndereco;
        final int idConta;
        final int idCartao;

        DadosContaBase(int idPessoa, int idEndereco, int idConta, int idCartao) {
            this.idPessoa = idPessoa;
            this.idEndereco = idEndereco;
            this.idConta = idConta;
            this.idCartao = idCartao;
        }
    }

    @BeforeAll
    static void beforeTest() {
        new ArquivoCenarioUtils().limparPastasResultadosEmResources();
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/cenario1/cenario-1.csv", delimiter = ';')
    public void genareteIndividualAccountAndCardCenario1Test(int idProduto, int produtoVinculado, int valorCompra) {
        DadosContaBase dadosBase = executarFluxoPrincipal(idProduto, NOME_CENARIO1);
        cadastrarContaVinculada(dadosBase.idPessoa, dadosBase.idEndereco, produtoVinculado);
        eventosExternosComprasUtils.gerarCompraAvista(URL_SANDBOX, PATH_EVENTOS_COMPRAS, accessToken, dadosBase.idConta, dadosBase.idCartao, valorCompra, getGerarComprasAteMenosCincoDias());
    }

    //@ParameterizedTest
    //@CsvFileSource(resources = "/cenario2/cenario-2.csv", delimiter = ';')
    public void genareteIndividualAccountAndCardCenario2Test(int idProduto, int produtoVinculado) {
        DadosContaBase dadosBase = executarFluxoPrincipal(idProduto, NOME_CENARIO2);
        cadastrarContaVinculada(dadosBase.idPessoa, dadosBase.idEndereco, produtoVinculado);
    }

    //@ParameterizedTest
    //@CsvFileSource(resources = "/cenario4/cenario-4.csv", delimiter = ';')
    public void genareteIndividualAccountAndCardCenario4Test(int idProduto1, int produtoVinculado) {
        DadosContaBase dadosBase = executarFluxoPrincipal(idProduto1, NOME_CENARIO4);
        cadastrarContaVinculada(dadosBase.idPessoa, dadosBase.idEndereco, produtoVinculado);
    }

    //@ParameterizedTest
    //@CsvFileSource(resources = "/cenario5/cenario-5.csv")
    public void genareteIndividualAccountAndCardCenario5Test(int idProduto) {
        executarFluxoPrincipal(idProduto, NOME_CENARIO5);
    }

    //@ParameterizedTest
    //@CsvFileSource(resources = "/cenario3/cenario-3.csv", delimiter = ';')
    public void genareteIndividualAccountAndCardCenario3Test(int idProduto, int produtoVinculado1, int produtoVinculado2, int produtoVinculado3) {
        DadosContaBase dadosBase = executarFluxoPrincipal(idProduto, NOME_CENARIO3);
        int[] produtosVinculados = {produtoVinculado1, produtoVinculado2, produtoVinculado3};

        for (int produto : produtosVinculados) {
            if (produto == 0) {
                continue;
            }
            cadastrarContaVinculada(dadosBase.idPessoa, dadosBase.idEndereco, produto);
        }
    }

    private DadosContaBase executarFluxoPrincipal(int idProduto, String nomeCenario) {
        int randomNumeroAgencia = gerarNumeroAgencia();
        int randomContaCorrente = gerarContaCorrente();

        int idPessoa = pessoa.geraPessoa(URL_SANDBOX, PATH_PESSOAS, accessToken);
        pessoasDetalhesUtils.geraPessoaDetalhes(URL_SANDBOX, PATH_PESSOAS_DETALHES, accessToken, idPessoa, randomNumeroAgencia, randomContaCorrente);
        int idEndereco = enderecosUtils.geraEndereco(URL_SANDBOX, PATH_ENDERECOS, accessToken, idPessoa);
        int idConta = contaUtils.cadastraConta(URL_SANDBOX, PATH_CONTAS, accessToken, idPessoa, idEndereco, idOrigemComercial, idProduto, getDiaVencimento());

        dadosBancariosUtils.cadastraDadosBancarios(URL_SANDBOX, PATH_DADOS_BANCARIOS, accessToken, idConta, randomNumeroAgencia, randomContaCorrente);
        int idCartao = cartoesUtils.geraCartao(URL_SANDBOX, PATH_CARTOES, accessToken, idConta, idPessoa);
        cartoesUtils.alterarEstagioCartao(URL_SANDBOX, PATH_CARTOES_ALTERAR_ESTAGIO, accessToken, idCartao);
        cartoesUtils.desbloquearCartao(URL_SANDBOX, PATH_DESBLOQUEIO_CARTAO, accessToken, idCartao);

        arquivoCenarioUtils.gravaIdsContaECartao(nomeCenario, idConta, idCartao);
        return new DadosContaBase(idPessoa, idEndereco, idConta, idCartao);
    }

    private void cadastrarContaVinculada(int idPessoa, int idEndereco, int idProdutoVinculado) {
        int randomNumeroAgenciaVinculado = gerarNumeroAgencia();
        int randomContaCorrenteVinculado = gerarContaCorrente();

        int idContaVinculado = contaUtils.cadastraConta(URL_SANDBOX, PATH_CONTAS, accessToken, idPessoa, idEndereco, idOrigemComercial, idProdutoVinculado, getDiaVencimento());
        dadosBancariosUtils.cadastraDadosBancarios(URL_SANDBOX, PATH_DADOS_BANCARIOS, accessToken, idContaVinculado, randomNumeroAgenciaVinculado, randomContaCorrenteVinculado);
    }

    private int gerarNumeroAgencia() {
        return 1000 + (int) (Math.random() * 9000);
    }

    private int gerarContaCorrente() {
        return 10000 + (int) (Math.random() * 90000);
    }

}
