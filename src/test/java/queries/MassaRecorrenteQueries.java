package queries;

import org.testng.SkipException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MassaRecorrenteQueries {

    private final BDconnect bd = new BDconnect();

    public static class InsercaoVencimentoContexto {
        private final Connection connection;
        private final Map<String, Object> informacaoVencimento;

        InsercaoVencimentoContexto(Connection connection, Map<String, Object> informacaoVencimento) {
            this.connection = connection;
            this.informacaoVencimento = informacaoVencimento;
        }

        public Map<String, Object> getInformacaoVencimento() {
            return informacaoVencimento;
        }

        public void commit() {
            fecharConexao(true);
        }

        public void rollback() {
            fecharConexao(false);
        }

        private void fecharConexao(boolean commit) {
            try {
                if (commit) {
                    connection.commit();
                } else {
                    connection.rollback();
                }
            } catch (SQLException e) {
                throw new RuntimeException("Erro ao finalizar transação: " + e.getMessage(), e);
            } finally {
                try {
                    connection.close();
                } catch (SQLException e) {
                    throw new RuntimeException("Erro ao fechar conexão: " + e.getMessage(), e);
                }
            }
        }
    }

    public String buildVencimentoMassa(String dataVencimento) {
        return "select top 10 c.ProximoVencimentoPadrao, c.dataprevistacorte, c.DataRealizacaoCorte, c.DataVencimento,\n" +
                "cp.VencimentoPadraoCortar, cp.DataMovimento as DataMovimento, cp.DataMovimento as 'criar na ControleProcessosProcedures'\n" +
                "from ControleVencimentos c (nolock)\n" +
                "inner join ControleProcessos cp (nolock) \n" +
                "    on convert(varchar, c.DataVencimento, 103) = cp.ProximoVencimentoPadrao\n" +
                "    left join ControleProcessosProcedures cpp (NOLOCK) \n" +
                "    on cp.DataMovimento = cpp.DataMovimento\n" +
                "AND cpp.FlagExecutado IN (1, 2) \n" +
                "AND cpp.Id_Processo = 9 -- codigo Procedure = 'SPR_Corte'\n" +
                "where 0=0   \n" +
                "and c.DataVencimento = '" + dataVencimento + "' --DATA DO VENCIMENTO DA MASSA\n" +
                "ORDER BY CONVERT(smalldatetime,cp.VencimentoPadraoCortar,103) DESC";
    }

    public String GetDataCompra(int diaVencimento) {
        return "declare @diaVencimento int =" + diaVencimento + " -- DIA DO VENCIMENTO DESEJADO\n" +
                "--\n" +
                "drop table if exists #vencimentos\n" +
                "\n" +
                "\n" +
                "----vencimento com colunas id_controleVencimentos, dataVencimento, dataprevistacorte\n" +
                "select Id_ControleVencimentos, DataVencimento, dataprevistacorte, DataRealizacaoCorte, DataRealizacaoFaturamento\n" +
                "into #vencimentos\n" +
                "from controlevencimentos (nolock) --where DataVencimento ='2023-06-11'\n" +
                "where convert(varchar, DataVencimento, 103) = (\n" +
                "    SELECT TOP (1)  cp.ProximoVencimentoPadrao\n" +
                "    FROM ControleProcessos cp (NOLOCK),\n" +
                "         ControleProcessosProcedures cpp (NOLOCK),\n" +
                "         ProcessosProcedures pp (NOLOCK)\n" +
                "   WHERE 0=0\n" +
                "        and LEFT(cp.VencimentoPadraoCortar,2) = ISNULL(@diaVencimento,1)\n" +
                "\t\tAND cp.DataMovimento = cpp.DataMovimento\n" +
                "        AND cpp.FlagExecutado IN (1, 2) \n" +
                "\t\tAND cpp.Id_Processo = pp.Id_Processo \n" +
                "\t\tAND pp.NomeProcedure = 'SPR_Corte'\n" +
                "    ORDER BY CONVERT(smalldatetime,cp.VencimentoPadraoCortar,103) DESC\n" +
                ")\n" +
                "--\n" +
                "select 'controlevencimentos' as tabela,* from #vencimentos\n" +
                "--\n" +
                "select c.Id_ControleProcesso, c.DataMovimento, c.DataUltimoMovimento, c.DataUltimoMovimento - 3 as 'gerarComprasAte', \n" +
                "\t   DATEDIFF(day, c.DataUltimoMovimento - 3, GETDATE()) as 'Dias anteriores'\n" +
                "from ControleProcessos c\n" +
                "inner join #vencimentos v on c.VencimentoPadraoFaturar = convert(varchar, v.DataVencimento, 103) ";
    }

    public void processarCompras(String DataProcessamentoCompras, int PrimeiroIdContaCenario1, int ultimoIdContaCenario3){
        try (Connection conn = bd.getConnection()) {
            System.out.println("Executando processarCompras com DataProcessamentoCompras = " + DataProcessamentoCompras);
            executarProceduresCompras(conn, DataProcessamentoCompras, PrimeiroIdContaCenario1, ultimoIdContaCenario3);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao executar processarCompras: " + e.getMessage(), e);
        }
    }

    private void executarProceduresCompras(Connection conn, String dataProcessamentoCompras, int primeiroIdContaCenario1, int ultimoIdContaCenario3) throws SQLException {
        final String[] procedures = {
                "SPR_PROCESSACOMPRAS",
                "SPR_PROCESSACOMPRAS_INSERETRANSACOES",
                "SPR_LANCAPARCELAS"
        };

        StringBuilder sqlBuilder = new StringBuilder("set nocount on; ");
        for (String procedure : procedures) {
            sqlBuilder.append("exec ").append(procedure).append(" ?, ?, ?, ?; ");
        }

        try (PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {
            int index = 1;
            for (int i = 0; i < procedures.length; i++) {
                stmt.setString(index++, dataProcessamentoCompras);
                stmt.setString(index++, "");
                stmt.setInt(index++, primeiroIdContaCenario1);
                stmt.setInt(index++, ultimoIdContaCenario3);
            }
            stmt.execute();
        }
    }

    public void processarCorte(String dataVencimento, int primeiroIdContaCenario1, int ultimoIdContaCenario5) {
        final String buscaDataMovimentoSql = "select top 1 dataprevistacorte from ControleVencimentos " +
                "where datavencimento = ? and DataRealizacaoCorte is null";
        final String[] procedures = {
                "spr_corte",
                "SPR_IniciaCiclo",
                "SPR_FlagEmiteExtrato",
                "SPR_DataVencimentoCobranca",
                "SPR_IdEndereco",
                "SPR_EncerraFaturamento",
                "SPR_Vencimento",
                "SPR_VencimentoTransacoes",
                "SPR_CalculaValorMinimo",
                "SPR_ProcessamentoPagamentos",
                "SPR_Gerar_Campanha_ParcelamentoFatura",
                "SPR_GerarOfertaParcelamento",
                "SPR_Gerar_ParcelFaturaRotConsig"
        };

        try (Connection conn = bd.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Timestamp dataMovimento = buscarDataMovimento(conn, buscaDataMovimentoSql, dataVencimento);
                executarProceduresCorteParametrizadas(conn, procedures, dataMovimento, primeiroIdContaCenario1, ultimoIdContaCenario5);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao executar processamento de corte: " + e.getMessage(), e);
        }
    }

    private void executarProceduresCorteParametrizadas(Connection conn, String[] procedures, Timestamp dataMovimento, int primeiroIdContaCenario1, int ultimoIdContaCenario5) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder("set nocount on; ");
        for (String procedure : procedures) {
            sqlBuilder.append("exec ").append(procedure).append(" ?, ?, ?, ?; ");
        }

        String sql = sqlBuilder.toString();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int index = 1;
            for (int i = 0; i < procedures.length; i++) {
                stmt.setTimestamp(index++, dataMovimento);
                stmt.setString(index++, "");
                stmt.setInt(index++, primeiroIdContaCenario1);
                stmt.setInt(index++, ultimoIdContaCenario5);
            }
            stmt.execute();
        }
    }

    public void atualizaIdsContasCenario4() {
        atualizaIdsContasCenario4DoArquivo();
    }

    public void atualizaIdsContasCenario4DoArquivo() {
        Path arquivoIds = Paths.get("src", "test", "resources", "cenario4", "ids-conta-cartao-cenario4.csv");
        List<Integer> idsConta = lerIdsContaCenario4(arquivoIds);

        if (idsConta.isEmpty()) {
            throw new RuntimeException("Nenhum id_conta encontrado no arquivo: " + arquivoIds);
        }

        for (int i = 0; i < idsConta.size(); i++) {
            int posicao = i + 1;
            int idConta = idsConta.get(i);
            int status = (posicao % 2 == 0) ? 48 : 8;
            atualizarStatusConta(idConta, status);
            System.out.println("Conta atualizada. posicao = " + posicao + ", id_conta = " + idConta + ", status = " + status);
        }
    }

    private List<Integer> lerIdsContaCenario4(Path arquivoIds) {
        try {
            if (!Files.exists(arquivoIds)) {
                throw new RuntimeException("Arquivo nao encontrado: " + arquivoIds);
            }

            List<String> linhas = Files.readAllLines(arquivoIds);
            List<Integer> idsConta = new ArrayList<>();
            for (int i = 1; i < linhas.size(); i++) {
                String linha = linhas.get(i).trim();
                if (linha.isEmpty()) {
                    continue;
                }

                String[] colunas = linha.split(",");
                if (colunas.length == 0 || colunas[0].trim().isEmpty()) {
                    continue;
                }

                idsConta.add(Integer.parseInt(colunas[0].trim()));
            }
            return idsConta;
        } catch (IOException e) {
            throw new RuntimeException("Erro ao ler arquivo de ids do cenario 4: " + e.getMessage(), e);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Arquivo de ids do cenario 4 possui id_conta invalido: " + e.getMessage(), e);
        }
    }

    private void atualizarStatusConta(int idConta, int status) {
        final String updateSql = "update contas set status = ? where id_conta = ?";
        try (Connection conn = bd.getConnection();
             PreparedStatement stmt = conn.prepareStatement(updateSql)) {
            stmt.setInt(1, status);
            stmt.setInt(2, idConta);
            int linhasAfetadas = stmt.executeUpdate();
            if (linhasAfetadas == 0) {
                throw new RuntimeException("Nenhuma conta encontrada para id_conta = " + idConta);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar status da conta do cenario 4: " + e.getMessage(), e);
        }
    }

    private Timestamp buscarDataMovimento(Connection conn, String buscaDataMovimentoSql, String dataVencimento) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(buscaDataMovimentoSql)) {
            stmt.setTimestamp(1, parseDataVencimento(dataVencimento));
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SkipException("Nao foi encontrada dataprevistacorte para dataVencimento " + dataVencimento + " com DataRealizacaoCorte nula.");
                }
                Timestamp dataMovimento = rs.getTimestamp("dataprevistacorte");
                if (dataMovimento == null) {
                    throw new SkipException("A dataprevistacorte retornou nula para dataVencimento " + dataVencimento + ".");
                }
                return dataMovimento;
            }
        }
    }

    private Timestamp parseDataVencimento(String dataVencimento) {
        String dataNormalizada = dataVencimento.trim();
        if (dataNormalizada.length() == 10) {
            dataNormalizada = dataNormalizada + " 00:00:00";
        } else if (dataNormalizada.length() == 16) {
            dataNormalizada = dataNormalizada + ":00";
        }

        try {
            return Timestamp.valueOf(dataNormalizada);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Data de vencimento invalida. Use yyyy-MM-dd ou yyyy-MM-dd HH:mm:ss. Valor recebido: " + dataVencimento,
                    e
            );
        }
    }

    public String getGerarComprasAte(String dataVencimento) {
        int diaVencimento = extrairDiaVencimento(dataVencimento);
        Map<String, Object> resultado = executaUltimoResultSet(GetDataCompra(diaVencimento));
        imprimirResultadoQuery("GetDataCompra", resultado);
        if (Objects.isNull(resultado)) {
            throw new SkipException("Não foi possível obter a data para gerar compras.");
        }
        return extrairDataSomente(resultado.get("gerarComprasAte"));
    }

    private int extrairDiaVencimento(String dataVencimento) {
        String dataSomente = extrairDataSomente(dataVencimento);
        if (dataSomente == null || dataSomente.length() < 10) {
            throw new IllegalArgumentException("Data de vencimento inválida: " + dataVencimento);
        }
        return Integer.parseInt(dataSomente.substring(8, 10));
    }

    private Map<String, Object> getInformacoesVencimento(Connection conn, String dataVencimento) {
        Map<String, Object> buscaInformacoesVencimento = executaQuery(conn, buildVencimentoMassa(dataVencimento));

        if (Objects.isNull(buscaInformacoesVencimento)) {
            throw new SkipException("Não existe massa válida na base de dados do emissor.");
        }
        return buscaInformacoesVencimento;
    }

    public InsercaoVencimentoContexto buildInsertControleProcessosProcedures(String dataVencimento) {
        String insertSql = "INSERT INTO ControleProcessosProcedures(Id_Processo, DataMovimento, DataUltProc, FlagExecutado) Select 9, ?, getdate(), 2";
        Connection conn = null;
        try {
            conn = bd.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                Map<String, Object> informacaoVencimento = getInformacoesVencimento(conn, dataVencimento);
                String dataInsert = extrairDataSomente(informacaoVencimento.get("criar na ControleProcessosProcedures"));
                stmt.setDate(1, java.sql.Date.valueOf(dataInsert));
                stmt.executeUpdate();

                return new InsercaoVencimentoContexto(conn, informacaoVencimento);
            }
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    conn.close();
                } catch (SQLException ex) {
                    throw new RuntimeException("Erro ao encerrar transação: " + ex.getMessage(), ex);
                }
            }
            throw new RuntimeException("Erro ao executar insert: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    conn.close();
                } catch (SQLException ex) {
                    throw new RuntimeException("Erro ao encerrar transação: " + ex.getMessage(), ex);
                }
            }
            throw e;
        }
    }

    private String extrairDataSomente(Object valorData) {
        if (valorData == null) {
            return null;
        }

        if (valorData instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) valorData;
            return timestamp.toLocalDateTime().toLocalDate().toString();
        }

        String valorComoTexto = valorData.toString();
        if (valorComoTexto.length() >= 10) {
            return valorComoTexto.substring(0, 10);
        }
        return valorComoTexto;
    }

    public Map<String, Object> executaQuery(String query) {
        try (Connection conn = bd.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (!rs.next()) {
                return null;
            }
            return resultSetToMap(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao executar query: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> executaUltimoResultSet(String query) {
        try (Connection conn = bd.getConnection();
             Statement stmt = conn.createStatement()) {

            boolean hasResultSet = stmt.execute(query);
            Map<String, Object> ultimoResultado = null;

            while (hasResultSet || stmt.getUpdateCount() != -1) {
                if (hasResultSet) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        if (rs != null && rs.next()) {
                            ultimoResultado = resultSetToMap(rs);
                        }
                    }
                }
                hasResultSet = stmt.getMoreResults();
            }

            return ultimoResultado;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao executar query com múltiplos resultados: " + e.getMessage(), e);
        }
    }

    private void imprimirResultadoQuery(String nomeQuery, Map<String, Object> resultado) {
        System.out.println("Resultado da query " + nomeQuery + ":");
        if (resultado == null) {
            System.out.println("  <sem resultado>");
            return;
        }
        for (Map.Entry<String, Object> entry : resultado.entrySet()) {
            System.out.println("  " + entry.getKey() + " = " + entry.getValue());
        }
    }

    private Map<String, Object> executaQuery(Connection conn, String query) {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (!rs.next()) {
                return null;
            }
            return resultSetToMap(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao executar query na transação: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> resultSetToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        ResultSetMetaData meta = rs.getMetaData();
        int cols = meta.getColumnCount();
        for (int i = 1; i <= cols; i++) {
            String colName = meta.getColumnLabel(i);
            Object value = rs.getObject(i);
            row.put(colName, value);
        }
        return row;
    }

}
