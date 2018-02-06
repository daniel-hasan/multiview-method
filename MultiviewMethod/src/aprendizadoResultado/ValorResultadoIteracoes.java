package aprendizadoResultado;

import matematica.Estatistica;
import aprendizadoResultado.ValorResultado.MetricaUsada;

public class ValorResultadoIteracoes
{
	private double[] values;
	private MetricaUsada metrica;
	
	
	public ValorResultadoIteracoes(double[] values,MetricaUsada metrica)
	{
		this.values = values;
		this.metrica =  metrica;
	}
	
	public double getMedia()
	{
		return Estatistica.media(values);
	}
	public double getDesvioPadrao()
	{
		return Estatistica.desvioPadrao(values);
	}
}
