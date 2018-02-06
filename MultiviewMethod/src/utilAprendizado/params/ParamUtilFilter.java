package utilAprendizado.params;

import java.util.List;
import java.util.Map;

import aprendizadoResultado.ValorResultado.MetricaUsada;

public abstract class ParamUtilFilter {
	
	private MetricaUsada metToEvaluate = null;
	public abstract void addResultParam(Map<String,String> paramTreino,Map<String,String> paramTeste,List<Float> resultado ,float tempo);
	
	
	public void setMetricToEvaluate(MetricaUsada met)
	{
		this.metToEvaluate = met;
	}
	public MetricaUsada getMetricToEvaluate()
	{
		return this.metToEvaluate;
	}
	public boolean allowComputeParams(Map<String,String> paramTreino,Map<String,String> paramTeste)throws Exception
	{
		return true;
	}
	
	public List<Map<String,String>> getOrderCombinacaoTreino(List<Map<String,String>> mapParamTreino)
	{
		return mapParamTreino;
	}
	public List<Map<String,String>> getOrderCombinacaoTeste(List<Map<String,String>> mapParamTeste)
	{
		return mapParamTeste;
	}
	public Long getTimeoutExecution()
	{
		return null;
	}
	public void setTimedOut()
	{
		
	}
	
	public abstract void runCalculatedParams(Map<String,String> paramTreino,Map<String,String> paramTeste);
}
