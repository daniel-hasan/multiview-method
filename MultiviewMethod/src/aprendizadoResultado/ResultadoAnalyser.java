package aprendizadoResultado;

import java.sql.SQLException;

import entidadesAprendizado.ResultadoItem;

public abstract class ResultadoAnalyser {
	public abstract void analyseResult(ResultadoItem resultItem, double resultValue) throws SQLException ;
	
	public abstract void analyseResultGroup(int k,int groupId,double resultValue,String metricGrouper,Double[] arrPredictionsOrder) throws SQLException ;
	
}
