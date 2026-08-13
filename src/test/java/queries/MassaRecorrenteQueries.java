package queries;

import org.testng.SkipException;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MassaRecorrenteQueries {

    private final BDconnect bd = new BDconnect();

    public String buildVencimentoMassa(String dataVencimento) {
        return "select top 10 c.ProximoVencimentoPadrao, c.dataprevistacorte, c.DataRealizacaoCorte, c.DataVencimento,\n" +
                "cp.VencimentoPadraoCortar, cp.DataMovimento as 'criar na ControleProcessosProcedures', cpp.DataMovimento\n" +
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

    public String getInformacoesVencimento(String dataVencimento) {
        Map<String, Object> buscaInformacoesVencimento = executaQuery(buildVencimentoMassa(dataVencimento));

        if (Objects.isNull(buscaInformacoesVencimento)) {
            throw new SkipException("Não existe massa válida na base de dados do emissor.");
        }
        Object dataMovimento = buscaInformacoesVencimento.get("criar na ControleProcessosProcedures");
        return extrairDataSomente(dataMovimento);
    }

    public String buildInsertControleProcessosProcedures(String dataVencimento) {
        String dataInsert = getInformacoesVencimento(dataVencimento);
        return "INSERT INTO ControleProcessosProcedures(Id_Processo, DataMovimento, DataUltProc, FlagExecutado) Select 9, '" + dataInsert + "', getdate(), 2";
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

    private Map<String, Object> resultSetToMap(ResultSet rs) throws SQLException {
        Map<String, Object> row = new HashMap<>();
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
