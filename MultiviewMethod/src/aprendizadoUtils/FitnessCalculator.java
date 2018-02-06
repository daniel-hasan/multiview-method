package aprendizadoUtils;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;


import aprendizadoResultado.ResultadoAnalyser;
import aprendizadoResultado.ResultadoToCSV;
import aprendizadoResultado.ValorResultado.MetricaUsada;
import entidadesAprendizado.Fold;
import entidadesAprendizado.Resultado;
import entidadesAprendizado.ResultadoItem;
import featSelector.ValorResultadoMultiplo;

public class FitnessCalculator {


	public FitnessCalculator()
	{
		
	}
	public static double getResultado(MetodoAprendizado metCombinacao,Resultado result,ResultadoAnalyser resAnal) throws SQLException
	{
		double minClasse = getMinClass(result);
		return getResultado( metCombinacao, result, minClasse,resAnal);
	}
	private static double getMinClass(Resultado result) {
		double minClasse = Integer.MAX_VALUE;
		//procura min class
		for(Fold f : result.getFolds())
		{
			for(ResultadoItem ri : f.getResultadosValues())
			{
				if(ri.getClasseReal() < minClasse)
				{
					minClasse = ri.getClasseReal();
				}
			}
		}
		return minClasse;
	}
	public static double getResultado(MetodoAprendizado metCombinacao,Resultado result,Double minClasse,ResultadoAnalyser resAnal) throws SQLException
	{ 
		if(minClasse == null)
		{
			minClasse = getMinClass(result);
		}
		System.out.println("MIN CLASSE: "+minClasse);
		
		Integer k = 1; 
		MetricaUsada metrica = null;
		if(metCombinacao.isClassificacao())
		{
			metrica = MetricaUsada.ACURACIA;
		}else
		{
			if(metCombinacao instanceof GenericoLetorLike)
			{
				k=10;
				metrica = MetricaUsada.NDCG_EXP;
			}
			else
			{
				
				metrica = MetricaUsada.MSE;
			}
		}
		//metrica = MetricaUsada.NDCG_EXP;
		//k=10;
		double dblresult = 0;
		for(int kVal= 1 ; kVal<=k ; kVal++)
		{
			switch(metrica)
			{
				case NDCG_EXP:
				case NDCG:
					System.out.println("\nNDCG@"+kVal+":");
					break;
				case MSE:
					System.out.println("\nMSE por Fold:");
					break;
				case ACURACIA:
					System.out.println("\nAcuracia por Fold:");
					break;
			}
			
			ValorResultadoMultiplo vrMult = metCombinacao.getResultadoPorIteracao(result.getFolds(), metrica, kVal, minClasse,resAnal);
			double sum = 0;
			for(double resultFold : vrMult.getAllResults())
			{
				System.out.println("==> "+resultFold);
				sum += resultFold;
			}
			System.out.println("Avg: "+sum/vrMult.getAllResults().size());
			if(kVal == k)
			{
				dblresult = vrMult.getAvgResults();
			}
		}
		

		return dblresult;
	}
	public double calculaCusto(Integer[] arrFeatures)
	{
		int numNonZero = 0;
		for(int i =0 ; i<arrFeatures.length ; i++)
		{
			if(arrFeatures[i] != 0)
			{
				numNonZero++;
			}
		}
		return numNonZero/(double)arrFeatures.length;
		//return numNonZero/arrFeatures.length;
	}
	public double calculaFitness(Integer[] arrFeatures,MetodoAprendizado metCombinacao,Resultado result,ResultadoAnalyser resAnal) throws SQLException
	{
		double resultPredicao = this.getResultado(metCombinacao,result, resAnal);
		double custo = this.calculaCusto(arrFeatures);
		
		return calculaFitness(metCombinacao,resultPredicao,custo);
		
	}
	private double calculaFitness(MetodoAprendizado metCombinacao,double resultPredicao, double custo) {
		// TODO Auto-generated method stub
		//caso seja regressao, o resultado eh o erro entao eh qnto maior pior
		if((! (metCombinacao instanceof GenericoLetorLike)) && !metCombinacao.isClassificacao())
		{
			return custo*resultPredicao;
		}else
		{
			//caso contrario ou eh classificacao ou ordenacao (com ndcg) ou seja, qnto maior melhor
			return resultPredicao/custo;
		}
	}
	
}
