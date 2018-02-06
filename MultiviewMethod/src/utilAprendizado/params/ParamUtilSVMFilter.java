package utilAprendizado.params;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.inference.TTest;
import org.apache.commons.math.stat.inference.TTestImpl;

import stuctUtil.ListaAssociativa; 
import stuctUtil.Tupla;
import aprendizadoResultado.ValorResultado;
import aprendizadoResultado.ValorResultado.MetricaUsada;
import aprendizadoUtils.GenericoLetorLike;
import aprendizadoUtils.GenericoSVMLike;
import entidadesAprendizado.Fold;

public class ParamUtilSVMFilter extends ParamUtilFilter 
{
	public static final boolean ALLOW_ALL = false; 
	//para um determinado gama, a tupla <custo, resultado>  de cada resultado obtido (lista por fold)
	private ListaAssociativa<Double, Tupla<Double,List<Float>>> lstResultPerGamaCusto = new ListaAssociativa<Double, Tupla<Double,List<Float>>>();
	
	private Set<Tupla<Double,Double>> filtroTupla = new HashSet<Tupla<Double,Double>>();
	private boolean isClassificacao = false;
	private Double maxCost = null; 
	
	private Integer k = null;
	private int numQueries = 0;
	private Long timeoutExecution = null;
	private boolean bolTimedOut=  false;
	private Double lastGama = null;
	public ParamUtilSVMFilter(boolean isClassificacao,int numQueries) {
		super();
		this.isClassificacao = isClassificacao;
		this.numQueries = numQueries;

	}
	public void setTimeout(Long timeout)
	{
		this.timeoutExecution = timeout;
	}
	public Long getTimeoutExecution()
	{
		return timeoutExecution;
	}
	public void setTimedOut()
	{
		bolTimedOut = true;
	}
	class CompareParam implements Comparator<Map<String, String>>{
		
		@Override
		public int compare(Map<String, String> o1, Map<String, String> o2) {
			//custo do menor pro maior, gama do maior pro menor 
			double custo1 = Double.parseDouble(o1.get("CUSTO"));
			double custo2 = Double.parseDouble(o2.get("CUSTO"));
			
			Double gama1 = o1.get("GAMA") != null?Double.parseDouble(o1.get("GAMA")):null;
			Double gama2 = o2.get("GAMA") != null?Double.parseDouble(o2.get("GAMA")):null;
			
			
			if(gama1 != gama2)
			{
				return (int) Math.round((float)(100*gama2)-(100*gama1));
			}else
			{
				return (int)Math.round((float)(100*custo1)-(100*custo2));
			}
			
		}
	}
	public void addTuplaFiltrar(Tupla<Double,Double> ... arrTplCustoGamaFiltro)
	{
		for(Tupla<Double,Double> tplCustoGama : arrTplCustoGamaFiltro)
		{
			this.filtroTupla.add(tplCustoGama);
		}
	}
	public void setMaxCost(double max)
	{
		this.maxCost = max;
	}
	public static GenericoSVMLike getParameterDefaultWikipedia(Integer viewIdx,GenericoSVMLike metAp) throws Exception
	{
		Map<String, String> mapParam = new HashMap<String,String>();
		mapParam.put("IS_CLASSIFICACAO", "3");

		
		
		metAp.setGravarNoBanco(false);
		switch(viewIdx)
		{
			
			case 1://history (ou o baseline)
				
				//history
				mapParam.put("CUSTO", "32");
				mapParam.put("GAMA", "2");
				mapParam.put("EPSLON", "0.5");

				
				//baseline
				/*
				 
				mapParam.put("CUSTO", "8");
				mapParam.put("GAMA", "0.5");
				mapParam.put("EPSLON", "0.5");
				 */
				break;
			case 2://Readability
				mapParam.put("CUSTO", "128.0");
				mapParam.put("GAMA", "8.0");
				mapParam.put("EPSLON", "0.0625");
				break;
			case 3://style
				mapParam.put("CUSTO", "8");
				mapParam.put("GAMA", "2");
				mapParam.put("EPSLON", "0.1");
				break;
				
			case 4://length
				mapParam.put("CUSTO", "32");
				mapParam.put("GAMA", "8");
				mapParam.put("EPSLON", "0.5");
				break;
			case 5://graph
				mapParam.put("CUSTO", "8");
				mapParam.put("GAMA", "8");
				mapParam.put("EPSLON", "0.5");
				break;
				
			case 6://structure
				mapParam.put("CUSTO", "8");
				mapParam.put("GAMA", "2");
				mapParam.put("EPSLON", "0.5");
				break;
			default://combincacao
				//simples
				/*
				mapParam.put("CUSTO", "512");
				mapParam.put("GAMA", "0.03125");
				mapParam.put("EPSLON", "0.5");
				*/
				
				//com feature set
				
				mapParam.put("CUSTO", "2");
				mapParam.put("GAMA", "0.125");
				mapParam.put("EPSLON", "0.5");
				
				break;
		}

		

		
		
		
		return new GenericoSVMLike(metAp.getNomeMetodo(),mapParam,mapParam);
	}
	
	public static GenericoSVMLike getParameterDefaultMuppets(Integer viewIdx,GenericoSVMLike metAp) throws Exception
	{
		Map<String, String> mapParam = new HashMap<String,String>();
		mapParam.put("IS_CLASSIFICACAO", "3");

		
		
		metAp.setGravarNoBanco(false);
		switch(viewIdx)
		{
			
			case 1://history (ou o baseline)
				
				//history
				mapParam.put("CUSTO", "0.5");
				mapParam.put("GAMA", "0.125");
				mapParam.put("EPSLON", "0.1");

				
				//baseline
				/*
				 
				mapParam.put("CUSTO", "8");
				mapParam.put("GAMA", "0.5");
				mapParam.put("EPSLON", "0.5");
				 */
				break;
			case 2://Readability
				mapParam.put("CUSTO", "128.0");
				mapParam.put("GAMA", "8.0");
				mapParam.put("EPSLON", "0.0625");
				break;
			case 3://style
				mapParam.put("CUSTO", "8");
				mapParam.put("GAMA", "2");
				mapParam.put("EPSLON", "0.1");
				break;
				
			case 4://length
				mapParam.put("CUSTO", "32");
				mapParam.put("GAMA", "8");
				mapParam.put("EPSLON", "0.5");
				break;
			case 5://graph
				mapParam.put("CUSTO", "8");
				mapParam.put("GAMA", "8");
				mapParam.put("EPSLON", "0.5");
				break;
				
			case 6://structure
				mapParam.put("CUSTO", "8");
				mapParam.put("GAMA", "2");
				mapParam.put("EPSLON", "0.5");
				break;
			default://combincacao
				//simples
				/*
				mapParam.put("CUSTO", "512");
				mapParam.put("GAMA", "0.03125");
				mapParam.put("EPSLON", "0.5");
				*/
				
				//com feature set
				
				mapParam.put("CUSTO", "2");
				mapParam.put("GAMA", "0.125");
				mapParam.put("EPSLON", "0.5");
				
				break;
		}

		

		
		
		
		return new GenericoSVMLike(metAp.getNomeMetodo(),mapParam,mapParam);
	}
	
	@SuppressWarnings("unchecked")
	public static Tupla<GenericoSVMLike,List<Tupla<Tupla<Map<String, String>, Map<String, String>>,ValorResultado>>> computeViewParameterSVM(Fold[] arrFoldToTest,File arq,GenericoSVMLike metAp,boolean filterBigCost,Integer numQueries,int numFeatures) throws Exception
	{
		Map<String, String> mapParam = new HashMap<String,String>();
		
		//verifica se eh svm ou rank svm para definicao dos parametros
		ParamUtilSVMFilter paramFilter = new ParamUtilSVMFilter(false,numQueries);
		
		if(metAp instanceof GenericoLetorLike )
		{
			mapParam.put("CUSTO", "*");
			mapParam.put("CUSTO_PER_QUERY", "1");
			System.out.println("Filtra custo");
			if(metAp.getNomeMetodo().equalsIgnoreCase("SVMRank") && numFeatures <= 2)
			{
				System.out.println("Maximo 128");
				paramFilter.setMaxCost(128);
				
			}
			paramFilter.setTimeout(30000L);
			
		}else
		{
			mapParam.put("IS_CLASSIFICACAO", metAp.isClassificacao()?"0":"3");
			mapParam.put("CUSTO", "*");
			if(metAp.getParamTrain("GERAR_CONFIANCA") != null)
			{
				mapParam.put("GERAR_CONFIANCA", metAp.getParamTrain("GERAR_CONFIANCA"));
			}
			if(!metAp.getNomeMetodo().equalsIgnoreCase("SVM_LINEAR"))
			{
				mapParam.put("GAMA", "*");	
			}
			
			filterBigCost = false;
			if(filterBigCost)
			{//qnto mais baixo, mais rapido
				//qnto mais alto o custo, mais demorado
				paramFilter.addTuplaFiltrar(
											new Tupla<Double,Double>(2048.0,8.0),
											new Tupla<Double,Double>(8192.0,8.0),
											new Tupla<Double,Double>(32768.0,8.0),
											
											new Tupla<Double,Double>(2048.0,2.0),
											new Tupla<Double,Double>(8192.0,2.0),
											new Tupla<Double,Double>(32768.0,2.0),
											
											new Tupla<Double,Double>(8192.0,0.5),
											new Tupla<Double,Double>(32768.0,0.5),
											
											new Tupla<Double,Double>(8192.0,0.125),
											new Tupla<Double,Double>(32768.0,0.125),
											
											new Tupla<Double,Double>(8192.0,0.03125),
											new Tupla<Double,Double>(32768.0,0.03125),
											
											
											new Tupla<Double,Double>(8192.0,0.03125),
											new Tupla<Double,Double>(32768.0,0.0078125)
											
											);
			}
			
			
		}

		
		
		metAp.setGravarNoBanco(false);
		

		
		List<Tupla<Tupla<Map<String, String>, Map<String, String>>,ValorResultado>> lstParamsResults = ParamUtil.getParamsOrderedByImportance(paramFilter,metAp, mapParam,	mapParam, arq,arrFoldToTest);
		Tupla<Map<String, String>, Map<String, String>> mapParamFim = lstParamsResults.get(0).getX();
		//Tupla<Map<String, String>, Map<String, String>> mapParamFim = lstParamsResults.get(lstParamsResults.size()-1).getX();
		GenericoSVMLike newMetAp = null; 
		if(metAp instanceof GenericoLetorLike )
		{
			newMetAp = new GenericoLetorLike(metAp.getNomeMetodo(),mapParamFim.getX(),mapParamFim.getY());
		}else
		{
			newMetAp = new GenericoSVMLike(metAp.getNomeMetodo(),mapParamFim.getX(),mapParamFim.getY());
		}
		//limpa os resultados 
		if(arrFoldToTest != null)
		{
			for(Fold f : arrFoldToTest)
					{
						f.limpaResultados();
					}
		}
		
		return new Tupla<GenericoSVMLike,List<Tupla<Tupla<Map<String, String>, Map<String, String>>,ValorResultado>>>(newMetAp,lstParamsResults);
	}
	
	@Override
	public void addResultParam(Map<String, String> paramTreino,Map<String, String> paramTeste, List<Float> resultado, float tempo) {
		// TODO Auto-generated method stub
		Tupla<Double, List<Float>> tpl = new Tupla<Double,  List<Float>>(Double.parseDouble(paramTreino.get("CUSTO")), resultado);
		lstResultPerGamaCusto.put(paramTreino.containsKey("GAMA")?Double.parseDouble(paramTreino.get("GAMA")):0, tpl);
		
	}
	public Tupla<Float,double[]> getResultAndAvg(Tupla<Double,List<Float>>  tplCustoResultado)
	{
		float resultAvg = 0;
		double[] result = new double[tplCustoResultado.getY().size()];
		for(int i =0; i< tplCustoResultado.getY().size() ; i++)
		{
			result[i] = tplCustoResultado.getY().get(i);
			resultAvg += result[i];
		}
		resultAvg /= (float)result.length;
		return new Tupla<Float,double[]>(resultAvg,result);
	}
	public void runCalculatedParams(Map<String,String> paramTreino,Map<String,String> paramTeste)
	{
		if(paramTreino.containsKey("CUSTO_PER_QUERY"))
		{
			double custo = Double.parseDouble(paramTreino.get("CUSTO"));
			paramTreino.put("CUSTO_PER_QUERY", Double.toString(custo/this.numQueries));
		}
	}
	public boolean allowComputeParams(Map<String,String> paramTreino,Map<String,String> paramTeste) throws Exception
	{
		if(ALLOW_ALL)
		{
			return true;
		}
		//gama eh opcional (caso for linear ou ranksvm)
		Double gama = paramTreino.containsKey("GAMA") && !paramTreino.get("GAMA").equals("*")?Double.parseDouble(paramTreino.get("GAMA")):0;
		double custo = Double.parseDouble(paramTreino.get("CUSTO"));
		if(!gama.equals(lastGama)){
			bolTimedOut = false;
		}
		
		//nao calcula caso seja maior que o maximo
		if(this.maxCost != null && custo > this.maxCost)
		{
			lastGama = gama;
			return false;
		}
		//caso tenha no filtro, retorna falso
		if(this.filtroTupla.contains(new Tupla<Double,Double>(custo,gama)))
		{
			lastGama = gama;
			return false;
		}
		//caso tenha dado timout, entao retorna false
		if(bolTimedOut)
		{
			lastGama = gama;
			return false;
		}
		
		List<Tupla<Double,List<Float>>> lstPastResults = lstResultPerGamaCusto.getList(gama);
		//até o segundo pode ser calculado
		if(lstPastResults == null || lstPastResults.size() <= 1)
		{
			lastGama = gama;
			return true;
		}
		
		//ordena do maior pro menor custo
		Collections.sort(lstPastResults, new Comparator<Tupla<Double,List<Float>>>() {

			@Override
			public int compare(Tupla<Double, List<Float>> o1, Tupla<Double, List<Float>> o2) {
				// TODO Auto-generated method stub
				double custo1 = o1.getX();
				double custo2 = o2.getX();
				
				return (int)Math.round((float)(10000*custo1)-(10000*custo2));
			}
		});
		
		//se o ultimo resultado for pior do que o melhor resultado, nao permite computar 
		float bestResultAvg = isClassificacao?0:Float.MAX_VALUE;
		double[] bestResult = null;
		for(Tupla<Double,List<Float>> tplCustoResultado : lstPastResults)
		{
			//resgata resultado (media e valor total
			Tupla<Float,double[]> resultAndAvg = getResultAndAvg(tplCustoResultado);
			float resultAvg = resultAndAvg.getX();
			double[] result = resultAndAvg.getY();
			
			if(isClassificacao)
			{
				if(resultAvg>bestResultAvg)
				{
					bestResultAvg = resultAvg;
					bestResult = result;
				}
						
			}else
			{
				//regressao (qnto menor melhor)
				if(resultAvg<bestResultAvg)
				{
					bestResultAvg = resultAvg;
					bestResult = result;
				}
			}
		}
		
		//resgata o last result e faz a comparacao
		Tupla<Float,double[]> resultAndAvg = getResultAndAvg(lstPastResults.get(lstPastResults.size()-1));
		float lastResultAvg = resultAndAvg.getX();
		double[] lastResult = resultAndAvg.getY();
		
		//varifica se ha diferenca significativa entre o melhor resutlado e o ultimo e se o ultimo
		//for estatisticamente pior do que o melhor, nao permite mais executar
		boolean isWorse = false;
		if(this.getMetricToEvaluate() == MetricaUsada.ACURACIA || this.getMetricToEvaluate() == MetricaUsada.NDCG_EXP || this.getMetricToEvaluate() == MetricaUsada.NDCG)
		{
			isWorse = lastResultAvg<bestResultAvg;
		}else
		{
			isWorse = lastResultAvg >bestResultAvg;
		}
		//para ser pior, tem q ter um valor pior e a piora tem que ser significativa
		if(isWorse)
		{
			TTest test = new TTestImpl();
			double alpha = test.pairedTTest(bestResult, lastResult);
			if(!Double.isNaN(alpha))
			{
				//caso seja pior com 90% de confianca retorna false (pq nao permite executar mais)
				return  !(alpha <= 0.1);
			}
		}
		return true;
		
		
	}
	
	/**
	 * Ordena gama do menor pro maior, custo do maior pro menor 
	 */
	@Override
	public List<Map<String,String>> getOrderCombinacaoTreino(List<Map<String,String>> mapParamTreino)
	{
		Collections.sort(mapParamTreino, new CompareParam());
		
		return mapParamTreino;
	}
	
	public static void main(String[] args) throws IllegalArgumentException, MathException
	{
		double[] bestResulta = {1.8637948, 1.8545204, 1.9011335, 1.9337348, 1.9361671};
		double[] lastResult1a = {1.8688571, 1.9210829, 1.9158264, 1.948405, 1.9723511};
		double[] lastResult2a = {1.8747442, 1.8877649, 1.9098955, 1.9411199, 1.9356201};
		
		double[] bestResultb = {1.7017295, 1.7740388, 1.7716669, 1.7681906, 1.736601};
		double[] lastResult1b = {1.697165, 1.7971245, 1.8209765, 1.7186631, 1.7589974};//ok ser igual
		double[] lastResult2b = {1.7071744, 1.8040346, 1.845617, 1.7254006, 1.7294788};//iok ser igual
		double[] lastResult3b = {1.7070986, 2.1031797, 1.8523924, 1.71391, 1.8881938};//nao eh igual
		
		TTest test = new TTestImpl();
		System.out.println("1a: "+test.pairedTTest(bestResulta, lastResult1a));
		System.out.println("2a: "+test.pairedTTest(bestResulta, lastResult2a));
		
		System.out.println("2a: "+test.pairedTTest(bestResultb, lastResult1b));
		System.out.println("2a: "+test.pairedTTest(bestResultb, lastResult2b));
		System.out.println("2a: "+test.pairedTTest(bestResultb, lastResult3b));
	}

}
