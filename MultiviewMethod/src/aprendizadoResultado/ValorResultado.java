package aprendizadoResultado;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ValorResultado implements Comparable<ValorResultado>
{
	public enum MetricaUsada{
		NDCG(10),NDCG_EXP(10),ERR(10),ACURACIA,MSE,PORC_ERR_NEAR_CLASS;
		
		private Integer defaultK;
		private MetricaUsada()
		{
			
		}
		private MetricaUsada(Integer kDefault)
		{
			this.defaultK = kDefault;
		}
		public Integer getDefaultK()
		{
			return this.defaultK;
		}
	}
	
	private float resultado;
	private List<Double> lstSubResults;
	private Integer k;
	private MetricaUsada metrica = null;
	
	
	public ValorResultado(float resultado,MetricaUsada metrica)
	{
		this(resultado,metrica,null,new ArrayList<Double>());
	}
	public ValorResultado(float resultado,MetricaUsada metrica,Integer k,List<Double> lstSubResults)
	{
		this.resultado = resultado;
		this.metrica = metrica;
		this.k = k;
		this.lstSubResults = lstSubResults;
	}
	public ValorResultado(float resultado,MetricaUsada metrica,Integer k)
	{
		this(resultado,metrica,k,new ArrayList<Double>());
	}
	public void setArrSubResults(List<Double> lstSubResults){
		this.lstSubResults = lstSubResults;
	}
	public List<Double> getArrSubResults()
	{
		return this.lstSubResults;
	}
	public int isBetterThan(ValorResultado arg0)
	{
		// TODO Auto-generated method stub
		if(this.getMetrica() != arg0.getMetrica())
		{
			System.err.println("Não é possivel comparar duas metricas diferentes");
			System.exit(0);
		}
		
		switch(this.metrica)
		{
			case NDCG:
			case NDCG_EXP:
			case ERR:
			case ACURACIA:
			case PORC_ERR_NEAR_CLASS:
				return (this.resultado>arg0.resultado)?1:(this.resultado<arg0.resultado)?-1:0;
			case MSE:
			
				return (this.resultado>arg0.resultado)?-1:(this.resultado<arg0.resultado)?+1:0;
		}
		return 0;
	}
	
	@Override
	public int compareTo(ValorResultado arg0)
	{
		return isBetterThan(arg0);
	}
	public float getResultado()
	{
		return resultado;
	}
	public MetricaUsada getMetrica()
	{
		return metrica;
	}
	public void gravaSubResult(File arquivo) throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(arquivo, false));
		for(Double subResult : lstSubResults)
		{
			out.write(subResult+"\n");
		}
		out.close();
		System.out.println("Gravou em: "+arquivo.getAbsolutePath());
	}
	public String toString()
	{
		switch(metrica)
		{
			case ACURACIA:
				return "Acerto: "+(this.resultado*100)+"%";
			default:
				return metrica.toString().toLowerCase()+(k!=null?"@"+k:"")+": "+this.resultado;
		}
		
	}
	
	
	
}
