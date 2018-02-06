package featSelector;

import java.util.ArrayList;
import java.util.List;

import stuctUtil.ArrayUtil;
import aprendizadoResultado.ValorResultado;

public class ValorResultadoMultiplo extends ValorResultado
{
	private List<ValorResultado> lstResults = new ArrayList<ValorResultado>();
	

	public ValorResultadoMultiplo(ValorResultado ... arrValores)
	{
		super(arrValores[0].getResultado(),arrValores[0].getMetrica());
		lstResults = new ArrayUtil<ValorResultado>().toList(arrValores);
	}
	public ValorResultado getValorResultadoMetrica(MetricaUsada m)
	{
		for(ValorResultado v : lstResults)
		{
			if(v.getMetrica() == m)
			{
				return v;
			}
		}
		return null;
	}
	public List<ValorResultado> getLstResults()
	{
		return lstResults;
	}
	public double getAvgResults()
	{
		float result = 0;
		for(ValorResultado v : lstResults)
		{
			result += v.getResultado();
			
		}
		return result/(double)lstResults.size();
	}
	public List<Float> getAllResults()
	{
		List<Float> lstResultsDbl = new ArrayList<Float>();
		for(ValorResultado v : lstResults)
		{
			lstResultsDbl.add(v.getResultado());
			
		}
		return lstResultsDbl;
	}
	
	public String toString()
	{
		String strMetricas = "";
		int i = 0;
		for(ValorResultado v : lstResults)
		{
			strMetricas += "["+i+"]: "+v.toString()+"\n";
		}
		return strMetricas;
	}
}
