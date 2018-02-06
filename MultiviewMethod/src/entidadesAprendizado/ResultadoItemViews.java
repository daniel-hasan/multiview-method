package entidadesAprendizado;

import matematica.Estatistica;

public class ResultadoItemViews implements Comparable<ResultadoItemViews>
{
	public enum ComparationType
	{
		VARIANCIA_ERRO,VARIANCIA_CLASSE, MSE, MEAN, DISCORDANTES;
		
		public String toString()
		{
			switch(this)
			{
				case  VARIANCIA_ERRO: return "VARIANCIA_ERRO";
				case  MSE: return "MSE";
				case  MEAN: return "MEAN";
				case VARIANCIA_CLASSE: return "VARIANCIA_CLASSE";
				case DISCORDANTES: return "DISCORDANTES";
				default: return "";
			}
		}
	}

	private long id;
	private ResultadoItem[] resultPorView;
	private ComparationType compType = ComparationType.MSE;

	public ResultadoItemViews(long id, Fold[] views)
	{
		this.id = id;
		resultPorView = new ResultadoItem[views.length];

		// procura cada resultado item com id
		for (int v = 0; v < views.length; v++)
		{
			resultPorView[v] = views[v].getResultadoPorId(id);
		}
	}
	public ResultadoItem[] getResultPorView()
	{
		return this.resultPorView;
	}
	public double getMeanClasseView()
	{
		double valueClass =0;
		for(ResultadoItem r : resultPorView)
		{
			valueClass += r.getClassePrevista();
		}
		return valueClass/(double)resultPorView.length;
	}
	public long getId()
	{
		return this.id;
	}
	public String toString()
	{
		
		return this.id+"\t"+this.getValueToCompare()+
				"\t"+getResultString()+
				"\tVariancia (erro): "+this.getVarianciaError()+
				"\tVariancia (classe): "+this.getVarianciaClass()+
				"\tMSE: "+this.getMSE()+
				"\tMedia:"+this.getMeanError()+" Media das classes:"+this.getMeanClasseView();
	}
	public String getResultString()
	{
		StringBuilder str = new StringBuilder();
		str.append(resultPorView[0].getClasseReal()+"=>");
		for(ResultadoItem r : resultPorView)
		{
			str.append(r.getClassePrevista()+";");
		}
		
		return str.toString();
	}
	public double getClasseReal()
	{
		return this.resultPorView[0].getClasseReal();
	}
	public void setCompType(ComparationType comp)
	{
		this.compType = comp;
	}
	/**
	 * Retorna MSE
	 * @return
	 */
	public double getMSE()
	{
		double sumQuadrado = 0;
		for(ResultadoItem r : resultPorView)
		{
			sumQuadrado += r.getErro()*r.getErro();
		}
		
		return sumQuadrado/(double)resultPorView.length;
	}
	/**
	 * Retorna media do erro
	 * @return
	 */
	public double getMeanError()
	{
		double sum = 0;
		for(ResultadoItem r : resultPorView)
		{
			sum += r.getErro();
		}
		
		return sum/(double)resultPorView.length;
	}
	/**
	 * Retorna variancia das classes
	 * @return
	 */
	public double getVarianciaClass()
	{
		double[] valores = new double[resultPorView.length+1];
		
		for(int i =0; i<valores.length-1 ; i++)
		{
			valores[i] = resultPorView[i].getClassePrevista();
		}
		valores[valores.length-1] = resultPorView[0].getClasseReal();
		
		return Estatistica.variancia(valores);
	}
	
	/**
	 * Retorna variancia do erro
	 * @return
	 */
	public double getVarianciaError()
	{
		double[] valores = new double[resultPorView.length];
		
		for(int i =0; i<valores.length ; i++)
		{
			valores[i] = resultPorView[i].getErro();
		}
		
		return Estatistica.variancia(valores);
	}
	public double getValueToCompare()
	{
		
		//adiciona mais um valor para priorizar os nao concordantes
		double valPriorizaDiscordante = 0;
		boolean achouConcordante = false;
		//procura discordante
		for(int vA=0 ; vA<this.resultPorView.length ; vA++)
		{
			for(int vB=0 ; vB<this.resultPorView.length ; vB++)
			{
				if(vA != vB && Math.abs(resultPorView[vA].getClassePrevista()-resultPorView[vB].getClassePrevista())<0.5)
				{
					achouConcordante = true;
					break;
				}
			}	
		}
		if(this.resultPorView.length>0 && !achouConcordante)
		{
			valPriorizaDiscordante = 100000;
		}
		
		switch (compType)
		{
			case MSE:
				return this.getMSE();
				
			case VARIANCIA_ERRO:
				return this.getVarianciaError();
				
			case VARIANCIA_CLASSE:
				return this.getVarianciaClass();
				
			case MEAN:
				return this.getMeanError();
			case DISCORDANTES:
				return valPriorizaDiscordante;
			default:
				return this.getVarianciaClass()+valPriorizaDiscordante;
		}
	}
	@Override
	public int compareTo(ResultadoItemViews o)
	{
		// TODO Auto-generated method stub
		double result = this.getValueToCompare()-o.getValueToCompare();
		return (int) (result*10000);
	}

}
