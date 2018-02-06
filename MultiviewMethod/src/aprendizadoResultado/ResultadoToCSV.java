package aprendizadoResultado;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import arquivo.ArquivoUtil;
import entidadesAprendizado.ResultadoItem;

public class ResultadoToCSV extends ResultadoAnalyser {
	private File csvArq = null;
	
	public ResultadoToCSV(File csvArq) throws IOException
	{
		this.csvArq = csvArq;
		ArquivoUtil.gravaTexto("QID;Resultado;Predicoes\n", csvArq, false);
	}
	@Override
	public void analyseResult(ResultadoItem resultItem, double resultValue)
			throws SQLException {
		// TODO Auto-generated method stub

	}

	@Override
	public void analyseResultGroup(int k, int groupId, double resultValue,
			String metricGrouper, Double[] arrPredictionsOrder)
			throws SQLException {
		// TODO Auto-generated method stub
		try {
			if(k == 10)
			{
				List<Double> lstResultados = new ArrayList<Double>();
				for(Double r : arrPredictionsOrder)
				{
					lstResultados.add(r);
				}
				
				ArquivoUtil.gravaTexto(groupId+";"+resultValue+";"+lstResultados+"\n", csvArq, true);	
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
