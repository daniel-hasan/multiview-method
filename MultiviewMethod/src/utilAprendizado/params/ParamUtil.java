package utilAprendizado.params;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import stuctUtil.ListaAssociativa;
import stuctUtil.Tripla;
import stuctUtil.Tupla;
import aprendizadoResultado.ValorResultado;
import aprendizadoResultado.ValorResultado.MetricaUsada;
import aprendizadoUtils.GenericoLetorLike;
import aprendizadoUtils.GenericoSVMLike;
import aprendizadoUtils.MetodoAprendizado;
import aprendizadoUtils.SVM;
import arquivo.ArquivoUtil;
import entidadesAprendizado.Fold;
import entidadesAprendizado.Param;
import featSelector.ValorResultadoMultiplo;

public class ParamUtil {
	
	public static boolean GERAR_ESTATISTCA_EXCEL = true;

	
	public static GenericoSVMLike instancia(GenericoSVMLike met,String nomeMetodo,Map<String,String> mapParamTreino,Map<String,String> mapParamTeste,ParamUtilFilter paramFilter) throws Exception
	{
		GenericoSVMLike gLike =  null;
		if(met instanceof GenericoLetorLike)
		{
			gLike = new GenericoLetorLike(nomeMetodo,mapParamTreino,mapParamTeste);
		}else
		{
			gLike = new GenericoSVMLike(nomeMetodo,mapParamTreino,mapParamTeste);
		}
		if(paramFilter != null && paramFilter.getTimeoutExecution()!=null)
		{
			gLike.setTimeout(paramFilter.getTimeoutExecution());
		}
		
		gLike.setMode(met.isClassificacao()?SVM.MODE_CLASSIFICATION:SVM.MODE_REGRESSION);
		return gLike;
		
	}
	public static List<String> getOrderedParams(Map<String,String> mapParams)
	{
		List<String> lstParams = new ArrayList<String>();
		
		//adiciona parametros numa lista
		for(String param : mapParams.keySet())
		{
			lstParams.add(param);
		}
		
		//ordena
		Collections.sort(lstParams);
		return lstParams;
	}
	public static StringBuffer paramColumns(Map<String,String> mapParams)
	{
		List<String> lstParams = getOrderedParams(mapParams);
		
		//gera string ordenada com resultado
		StringBuffer strLine = new StringBuffer();
		for(int i = 0; i < lstParams.size() ; i++)
		{
			strLine.append(lstParams.get(i));
			if(i+1 < lstParams.size())
			{
				strLine.append('\t');
			}
		}
		
		return strLine;
	}
	public static StringBuffer paramToExcelLine(Map<String,String> mapParams)
	{
		List<String> lstParams = getOrderedParams(mapParams);
		
		//gera string ordenada com resultado
		StringBuffer strLine = new StringBuffer();
		for(int i = 0; i < lstParams.size() ; i++)
		{
			strLine.append(mapParams.get(lstParams.get(i)));
			if(i+1 < lstParams.size())
			{
				strLine.append('\t');
			}
		}
		
		return strLine;
		
		
	}
	public static StringBuffer paramToExcelLine(boolean gerarColTitulo,Map<String,String> paramTreino,Map<String,String> paramTeste,float tempo,float resultado,List<Float> lstResults)
	{ 
		StringBuffer strParams = new StringBuffer();
		
		//coluna de titulo
		if(gerarColTitulo)
		{
			StringBuffer colParamTreino = paramColumns(paramTreino);
			StringBuffer colParamTeste = paramColumns(paramTeste);
			
			strParams.append(colParamTreino);
			if(strParams.length() > 0 )
			{
				strParams.append('\t');
				
			}
			if(colParamTeste.length()>0)
			{
				strParams.append(colParamTeste);
				strParams.append('\t');
			}
			
			strParams.append("Tempo\tResultado\tPorFold\n");
		}
		//dados por parametro  treino/teste
		StringBuffer strValParamTreino = paramToExcelLine(paramTreino);
		StringBuffer strValParamTeste = paramToExcelLine(paramTeste);
		strParams.append(strValParamTreino);
		if(strParams.length() > 0 )
		{
			strParams.append('\t');
			
		}
		if(strValParamTeste.length()>0)
		{
			strParams.append(strValParamTeste);
			strParams.append('\t');
		}
		strParams.append(tempo);
		strParams.append('\t');
		strParams.append(resultado);
		strParams.append('\t');
		strParams.append(lstResults.toString());
		return strParams;
		
	}
	public static Tupla<Map<String, String>, Map<String, String>> getBestParam(GenericoSVMLike met, Map<String, String> mapParamTreino,	Map<String, String> mapParamTeste,File arquivo,Fold[] arrFoldsToTest) throws NumberFormatException, Exception
	{
		return getBestParam(null , met, mapParamTreino,	mapParamTeste, arquivo,arrFoldsToTest);
	}
	public static Tupla<Map<String, String>, Map<String, String>> getBestParam(ParamUtilFilter paramFilter ,GenericoSVMLike met, Map<String, String> mapParamTreino,	Map<String, String> mapParamTeste,File arquivo,Fold[] arrFoldToTest) throws NumberFormatException, Exception
	{
		
		Tupla<Map<String, String>, Map<String, String>> bestParameterTreinoTeste = getAllParamResults(
				paramFilter, met, mapParamTreino, mapParamTeste, arquivo,arrFoldToTest).getX();
		
		return bestParameterTreinoTeste;
	}
	public static List<Tupla<Tupla<Map<String, String>, Map<String, String>>,ValorResultado>> getParamsOrderedByImportance(ParamUtilFilter paramFilter ,GenericoSVMLike met, Map<String, String> mapParamTreino,	Map<String, String> mapParamTeste,File arquivo,Fold[] arrFoldsToTestParam) throws NumberFormatException, Exception
	{
		
		List<Tupla<Tupla<Map<String, String>, Map<String, String>>,ValorResultado>> lstParamPerResult = getAllParamResults(paramFilter, met, mapParamTreino, mapParamTeste, arquivo,arrFoldsToTestParam).getY();
		Collections.sort(lstParamPerResult,new Comparator<Tupla<Tupla<Map<String, String>, Map<String, String>>,ValorResultado>>() {

			@Override
			public int compare(
					Tupla<Tupla<Map<String, String>, Map<String, String>>, ValorResultado> o1,
					Tupla<Tupla<Map<String, String>, Map<String, String>>, ValorResultado> o2) {
				// TODO Auto-generated method stub
				return -o1.getY().isBetterThan(o2.getY());
				
				
			}
			
		});
		
		
		return lstParamPerResult;
	}
	public static Tupla<Tupla<Map<String, String>, Map<String, String>>,List<Tupla<Tupla<Map<String, String>, Map<String, String>>,ValorResultado>>> 
	
		getAllParamResults(
			ParamUtilFilter paramFilter, GenericoSVMLike met,
			Map<String, String> mapParamTreino,
			Map<String, String> mapParamTeste, File arquivo, Fold[] arrFoldsToTestParam) throws Exception,
			IOException, SQLException {
		ListaAssociativa<String,String>  paramsVariarTreino = ParamUtil.getParamsVariar(mapParamTreino,GenericoSVMLike.getXMLMetodo().getCNFMetodo(met.getNomeMetodo()).getLstParamsTreino());
		ListaAssociativa<String,String>  paramsVariarTeste = ParamUtil.getParamsVariar(mapParamTeste,GenericoSVMLike.getXMLMetodo().getCNFMetodo(met.getNomeMetodo()).getLstParamsTeste());
		List<Tupla<Tupla<Map<String, String>, Map<String, String>>,ValorResultado>> lstResultsPerParam = new ArrayList<Tupla<Tupla<Map<String, String>, Map<String, String>>,ValorResultado>>();
		
		//caso nao haja parametro para variar, retorne a funcao
		if(paramsVariarTeste.keySet().size() ==0 && paramsVariarTreino.keySet().size() == 0)
		{
			
			return new Tupla<Tupla<Map<String, String>, Map<String, String>>,List<Tupla<Tupla<Map<String, String>, Map<String, String>>,ValorResultado>>>(new Tupla<Map<String,String>,Map<String,String>>(mapParamTreino,mapParamTeste),lstResultsPerParam);
		}
		
		//cria lista de opções de parametros
		List<Map<String,String>> lstCombinacoesParamsTreino = paramsVariarTreino.combineAll();
		List<Map<String,String>> lstCombinacoesParamsTeste = paramsVariarTeste.combineAll();
		
		//caso o teste/treino esteja sem parametros, adicionar um hashmap vazio para ele 
		if(lstCombinacoesParamsTreino.size() == 0)
		{
			lstCombinacoesParamsTreino.add(new HashMap<String,String>());
		}
		if(lstCombinacoesParamsTeste.size() == 0)
		{
			lstCombinacoesParamsTeste.add(new HashMap<String,String>());
		}
		
		//realiza combinacao
		int numCobinacoesTotal = lstCombinacoesParamsTreino.size()*lstCombinacoesParamsTeste.size();
		System.out.println("Numero de combinacoes a serem feitas: "+numCobinacoesTotal);
		
		//cria folds
		String nomExperimentoAnt = met.getNomExperimento();
		String nomExperimento = met.getNomExperimento()==null?"":met.getNomExperimento();
		long start_global = System.currentTimeMillis();
		met.setNomExperimento("best_param_compute_"+nomExperimento);
		Fold[] foldsToTest = arrFoldsToTestParam!=null?arrFoldsToTestParam:
														met.criaFoldsTeste(arquivo, Integer.parseInt(mapParamTreino.containsKey("val_num_folds")?mapParamTreino.get("val_num_folds"):"5"), false,	
																				mapParamTreino.containsKey("val_id_name")?mapParamTreino.get("val_id_name"):"id", 
																						met instanceof GenericoLetorLike?"qid":"",
																						false);
		
		//met.setNomExperimento("best_param_compute_"+nomExperimento);
		int combNum = 0;
		ValorResultado vResultBest = null;
		Tupla<Map<String, String>, Map<String, String>> bestParameterTreinoTeste = null;
		boolean firstTime = true;
		File arqEstatistica = new File(arquivo.getParentFile(),arquivo.getName()+"_params.xls");
		
		
		//reornena combinacao de parametros se necessario
		/*if(paramFilter != null)
		{
			lstCombinacoesParamsTreino = paramFilter.getOrderCombinacaoTreino(lstCombinacoesParamsTreino);
			lstCombinacoesParamsTeste = paramFilter.getOrderCombinacaoTeste(lstCombinacoesParamsTeste);
		}*/
		//define parametros para a geracao do resulado de acordo com o tipo de metodo de aprendizado
		Integer k = null;
		MetricaUsada metricaAvaliacao = null;
		
		if(met instanceof GenericoLetorLike)
		{
			metricaAvaliacao = MetricaUsada.NDCG_EXP;
			k = 10;
		}else
		{
			if(met.isClassificacao())
			{
				metricaAvaliacao = MetricaUsada.ACURACIA;
			}else
			{
				metricaAvaliacao = MetricaUsada.MSE;
			}
		}
		if(paramFilter!=null)
		{
			paramFilter.setMetricToEvaluate(metricaAvaliacao);	
		}
		
		
		
		for(Map<String,String> mapParamsCompTreino : lstCombinacoesParamsTreino)
		{
			
			for(Map<String,String> mapParamsCompTeste : lstCombinacoesParamsTeste)
			{
				combNum++;
				//cria novos parametros
				Map<String,String> newParamTreino = new HashMap<String,String>();
				Map<String,String> newParamTeste = new HashMap<String,String>();
				
				//adiciona todos os parametros (que foram passados nesta funcao) 
				for(String key : mapParamTreino.keySet())
				{
					newParamTreino.put(key, mapParamTreino.get(key));
				}
				for(String key : mapParamTeste.keySet())
				{
					newParamTeste.put(key, mapParamTeste.get(key));
				}
				
				//sobrepoe os "*" com a variacao atual
				for(String key : mapParamsCompTreino.keySet())
				{
					newParamTreino.put(key, mapParamsCompTreino.get(key));
				}
				for(String key : mapParamsCompTeste.keySet())
				{
					newParamTeste.put(key, mapParamsCompTeste.get(key));
				}
				System.out.println("Variacao atual, treino: "+mapParamsCompTreino+"\tTeste:"+mapParamsCompTeste+"  ("+combNum+"/"+numCobinacoesTotal+")");
				
				
				if(paramFilter == null || paramFilter.allowComputeParams(newParamTreino,newParamTeste))
				{
					//cria o metodo atual e o executa
					long start = System.currentTimeMillis();
					if(paramFilter != null)
					{
						paramFilter.runCalculatedParams(newParamTreino,newParamTeste);
					}
					System.out.println("Rodando: "+newParamTreino.toString()+" "+newParamTeste.toString());
					GenericoSVMLike gMetodoAp = ParamUtil.instancia(met,met.getNomeMetodo(), newParamTreino, newParamTeste,paramFilter);
					Fold[] fResult = null;
					boolean timedOut = false;
					if(!gMetodoAp.isUsingMultithread())
					{
						
						for(int i = 0; i<foldsToTest.length ; i++)
						{
							System.out.println("Testando fold#"+i);
							long tempo = System.currentTimeMillis();
							
							foldsToTest[i].setResultados(gMetodoAp.testar(foldsToTest[i]));
							if(gMetodoAp.endedWithTimeout())
							{
								paramFilter.setTimedOut();
								timedOut = true;
								
								System.out.println("Setou o timeout!");
								break;
							}
							System.out.println("Fold "+i+" em "+tempo/1000.0+" seg");
						}
						fResult = foldsToTest;
					}else
					{
						fResult = gMetodoAp.testar(foldsToTest);
					}
					long end = System.currentTimeMillis();
					
					//pega a menor classe
					/*
					double minClasse = Integer.MAX_VALUE;
					for(Fold f : fResult)
					{
						
						for(ResultadoItem ri : f.getResultadosValues())
						{
							if(ri.getClasseReal()<minClasse)
							{
								minClasse = ri.getClasseReal();
							}
						}
					}
					*/
					if(!timedOut)
					{
						//resgata resultado e verifica se eh o melhor
						ValorResultadoMultiplo vResultado = gMetodoAp.getResultadoPorIteracao(fResult, metricaAvaliacao, k, 0);//getResultadoGeral(fResult);
						ValorResultado vResultadoGeral = new ValorResultado((float)vResultado.getAvgResults(), metricaAvaliacao);
						
						lstResultsPerParam.add(new Tupla<Tupla<Map<String,String>,Map<String,String>>,ValorResultado>(new Tupla<Map<String,String>,Map<String,String>>(newParamTreino,newParamTeste),vResultadoGeral));
						System.out.println("----------------------------------------------------------");
						System.out.println("----------------------------------------------------------");
						if(vResultBest == null || vResultBest.isBetterThan(vResultadoGeral) == -1)
						{
							vResultBest = vResultadoGeral;
							bestParameterTreinoTeste = new Tupla<Map<String,String>,Map<String,String>>(newParamTreino,newParamTeste);
							System.out.println(vResultadoGeral+", resultado atual é o melhor resultado! ");
						}else
						{
							System.out.println(vResultado+" melhor resultado:"+vResultBest+"  params: "+bestParameterTreinoTeste);
						}
						
						System.out.println("Resultado executado em: "+(end-start)/1000.0+" segundos");
						
						if(paramFilter  != null)
						{
							paramFilter.addResultParam(mapParamsCompTreino, mapParamsCompTeste, vResultado.getAllResults(), (float)(end-start));
						}
						//gera estatistica de empo de execucao deste parametro
						if(GERAR_ESTATISTCA_EXCEL)
						{
							ArquivoUtil.gravaTexto(paramToExcelLine(firstTime,mapParamsCompTreino,mapParamsCompTeste,(end-start)/1000F,(float)vResultado.getAvgResults(),vResultado.getAllResults()).append('\n').toString(), arqEstatistica, !firstTime);
						}
						System.out.println("----------------------------------------------------------");
						System.out.println("----------------------------------------------------------");
					}
					
					
					//limpa os fodls
					for(Fold f : fResult)
					{
						f.limpaResultados();
					}
					
				}else
				{
					System.out.println("----------------------------------------------------------");
					System.out.println("----------------------------------------------------------");
					System.out.println("Parametro "+bestParameterTreinoTeste+" não foi necessario computar");
					if(GERAR_ESTATISTCA_EXCEL)
					{
						ArquivoUtil.gravaTexto(paramToExcelLine(firstTime,mapParamsCompTreino,mapParamsCompTeste,0,0,new ArrayList<Float>()).append('\n').toString(), arqEstatistica, !firstTime);
					}
					System.out.println("----------------------------------------------------------");
					System.out.println("----------------------------------------------------------");
				}
				

				firstTime = false;
			}
		}
		System.out.println("Tempo total de execucao: "+(System.currentTimeMillis()-start_global)/1000.0+" segundos");
		met.setNomExperimento(nomExperimentoAnt);
		return new Tupla<Tupla<Map<String, String>, Map<String, String>>,List<Tupla<Tupla<Map<String, String>, Map<String, String>>,ValorResultado>>>(bestParameterTreinoTeste,lstResultsPerParam);
	}
	
	
	private static ListaAssociativa<String,String> getParamsVariar(Map<String, String> mapParam,List<Param> lstParams) throws Exception {
		// TODO Auto-generated method stub

		ListaAssociativa<String,String> lstParamVariar = new ListaAssociativa<String,String>();
		if(lstParams == null)
		{
			return lstParamVariar;
		}
		for(Param p : lstParams)
		{
		
			if(mapParam.containsKey(p.getName()) && mapParam.get(p.getName()).equals("*")){
				if(p.getValuesVariation().size() == 0)
				{
					throw new Exception("Erro o parametro "+p.getName()+" nao possui variacao!");
				}
				lstParamVariar.put(p.getName(), p.getValuesVariation());
			}
		}
		
		return lstParamVariar;
	}
	
	public static void main(String[] args) throws NumberFormatException, Exception
	{

		
		HashMap<String, String> mapParam = new HashMap<String,String>();
		mapParam.put("IS_CLASSIFICACAO", "3");
		mapParam.put("CUSTO", "*");
		mapParam.put("GAMA", "*");
		
		
		GenericoSVMLike metRegressao = new GenericoSVMLike("SVM",mapParam,mapParam);
		metRegressao.setMode(SVM.MODE_REGRESSION);
		metRegressao.setGravarNoBanco(false);
		
		getBestParam(new ParamUtilSVMFilter(false,1000),metRegressao, mapParam,	mapParam, new File("/data/experimentos/qa_multiview/datasets/wiki6/wiki6_structure.amostra"),null);
		//getBestParam(metRegressao, mapParam,	mapParam, new File("/data/experimentos/teste/toy.data"));
		//getBestParam(new ParamUtilSVMFilter(false),metRegressao, mapParam,	mapParam, new File("/data/experimentos/teste/svr/starVote_structure.amostra"));
		
		
		//getBestParam(new ParamUtilSVMFilter(false),metRegressao, mapParam,	mapParam, new File("/home/hasan/teste.amostra"));
	}
}
