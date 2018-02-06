package aprendizadoResultado;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import matematica.KendallTau;
import stuctUtil.ListaAssociativa;
import stuctUtil.Tupla;
import aprendizadoResultado.ValorResultado.MetricaUsada;
import banco.GerenteBD;
import entidadesAprendizado.Fold;
import entidadesAprendizado.ResultadoItem;

public class ComparaViews {
	private static PreparedStatement stmtInsertBestResult ;
	private static PreparedStatement deleteAllBestResult;
	private static PreparedStatement stmtInsertCorrelacao;
	private static PreparedStatement deleteAllCorrelacao;
	public static void inicializaBD() throws SQLException, ClassNotFoundException
	{
		Connection conn = GerenteBD.getGerenteBD().obtemConexao("");
		stmtInsertBestResult = conn.prepareStatement("INSERT INTO wiki_results.best_result_view " +
											"	(" +
											"page_id," +
											"result," +
											"nomExperimento," +
											"nomExperimentoCombinacao," +
											"nomExperimento_2nd," +
											"result_2nd" +
										")" +
										"values " +
										"(?,?,?,?,?,?)");
		deleteAllBestResult = conn.prepareStatement("DELeTE FROM wiki_results.best_result_view  where nomExperimentoCombinacao = ?");
		
		stmtInsertCorrelacao = conn.prepareStatement("INSERT INTO wiki_results.correlacao_views " +
																		"	(" +
																		"nomExperimentoView1," +
																		"nomExperimentoView2," +
																		"qid," +
																		"correlacao" +
																	")" +
																	"values " +
																	"(?,?,?,?)");
		
		deleteAllCorrelacao = conn.prepareStatement("DELeTE FROM wiki_results.correlacao_views  where nomExperimentoView1 = ? and nomExperimentoView2 = ?");
	}
	public static void deleteCorrelacao(String view1,String view2) throws SQLException
	{
		deleteAllCorrelacao.setString(1, view1);
		deleteAllCorrelacao.setString(2, view2);
		deleteAllCorrelacao.executeUpdate();
	}
	
	public static void insertCorrelacao(String view1, String view2,long qid,float correlacao) throws SQLException
	{
		stmtInsertCorrelacao.setString(1, view1);
		stmtInsertCorrelacao.setString(2, view2);
		stmtInsertCorrelacao.setLong(3, qid);
		stmtInsertCorrelacao.setFloat(4, correlacao);
		
		stmtInsertCorrelacao.executeUpdate();
	}
	public static void deleteCombinacaBestResult(String nomExpCombinacao) throws SQLException
	{
		deleteAllBestResult.setString(1, nomExpCombinacao);
		deleteAllBestResult.executeUpdate();
	}
	public static void insertBest(long itemId,double result,String nomExperimento,String nomExperimentoComb,String nomExperimento2nd,double result2nd) throws SQLException
	{
		stmtInsertBestResult.setLong(1, itemId);
		stmtInsertBestResult.setDouble(2, result);
		stmtInsertBestResult.setString(3, nomExperimento);
		stmtInsertBestResult.setString(4, nomExperimentoComb);
		stmtInsertBestResult.setString(5, nomExperimento2nd);
		stmtInsertBestResult.setDouble(6, result2nd);
		
		stmtInsertBestResult.executeUpdate();
	}
	public static ListaAssociativa<Tupla<String,String>,Tupla<Long,Double>>  kendallTauBetweenAllViews(String ... arrNomExperimentoViews) throws Exception
	{
		inicializaBD();
		//resgata a lista de resultado por visao (visao => id_do_resultado => resultado_uitem
		Map<String, Map<Long,ResultadoItem>> mapResultsPorViews  =  getResultsPerView(arrNomExperimentoViews);
		
		
		//mapa auxiliar para sabermos quais perguntas precisam ser ordenadas (a ordenacao eh por pergunta) id_pergunta => [lista resps]
		ListaAssociativa<Long, Long> mapRespsPorPergunta = getRespsPorPergunta(arrNomExperimentoViews);
		
		
		
		
		//faz todas as combinacoes dois a dois de visoes e executa a comparação por pergunta, retornando num excel com sua cdf
		ListaAssociativa<Tupla<String,String>,Tupla<Long,Double>> resultComb = new ListaAssociativa<Tupla<String,String>,Tupla<Long,Double>>();
		for(int i = 0 ; i < arrNomExperimentoViews.length ; i++)
		{
			for(int j = i+1 ; j<arrNomExperimentoViews.length ; j++)
			{
				
				System.out.println("Comparando com kendall tau: "+arrNomExperimentoViews[i]+" versus "+arrNomExperimentoViews[j]);
				deleteCorrelacao(arrNomExperimentoViews[i],arrNomExperimentoViews[j]);
				//para cada pergunta, comparar o ranking e dar o resultado
				for(long qId : mapRespsPorPergunta.keySet())
				{
					List<Long> idsRespPergunta = mapRespsPorPergunta.getList(qId);
					String viewX = arrNomExperimentoViews[i];
					String viewY = arrNomExperimentoViews[j];
					Map<Long,ResultadoItem> resultViewX = mapResultsPorViews.get(viewX);
					Map<Long,ResultadoItem> resultViewY = mapResultsPorViews.get(viewY);
					
					
					//cria tuplas por id de resposta
					List<Tupla<Double,Double>> lstTuplaResult = new ArrayList<Tupla<Double,Double>>();
					for(long idResp : idsRespPergunta)
					{
						lstTuplaResult.add(new Tupla<Double,Double>(resultViewX.get(idResp).getClassePrevista(),resultViewY.get(idResp).getClassePrevista()));
					}
					
					double kendall = KendallTau.compareRanking(lstTuplaResult);
					insertCorrelacao(arrNomExperimentoViews[i],arrNomExperimentoViews[j], qId,(float)kendall);
					resultComb.put(new Tupla<String,String>(viewX,viewY), new Tupla<Long,Double>(qId,kendall));
					
					
					
					
				}
			}
		}
		return resultComb;
		
		
		
		
	}
	public static String getBestView(long qid,Map<String, Map<Long,ResultadoItem>> mapResultsPorViews,
			List<Long> resps,
			MetricaUsada metrica,String nomExperimentoComb,
			double minClassView,
			boolean inserir) throws SQLException, ClassNotFoundException
	{

		double maxResult = 0;
		double minResult = Double.MAX_VALUE;
		String viewBestResult = "";
		String view2ndBestResult = "";
		double bestResult = 0;
		double val2ndBestResult = 0;
		//procura por view o resultado
		for(String view : mapResultsPorViews.keySet())
		{
			List<ResultadoItem> lstResultados = new ArrayList<ResultadoItem>();
			/*
			double minClassView = Double.MAX_VALUE;
			
			//agrupa resultado das respostas para esta view
			for(long resp : resps)
			{
				ResultadoItem ri = mapResultsPorViews.get(view).get(resp);
				lstResultados.add(ri);
				//decobre a menor classe (para o ndcg)
				if(ri.getClasseReal() < minClassView)
				{
					minClassView = ri.getClasseReal();
				}
			}
			*/
			//agrupa resultado das respostas para esta view
			for(long resp : resps)
			{
				ResultadoItem ri = mapResultsPorViews.get(view).get(resp);
				lstResultados.add(ri);
				
			}
			//verifica se o resultado desta view eh o melhor de acordo com a metrica
			boolean isEqual = false;
			boolean isBetter = false;
			switch(metrica)
			{
				case NDCG_EXP:
				case NDCG:
					double ndcg = CalculaResultados.getResultado(lstResultados,metrica,lstResultados.size(),minClassView).getResultado();
					isEqual = ndcg == maxResult;
					isBetter = ndcg > maxResult;
					if(isBetter)
					{
						val2ndBestResult = maxResult;
						maxResult = ndcg;
						bestResult = maxResult;
					}
					
				break;
				case MSE:
					//double result = Math.round(lstResultados.get(0).getClassePrevista());
					double erro = lstResultados.get(0).getErro();
					
					isEqual = erro == minResult;
					isBetter = erro < minResult;
					
					if(isBetter)
					{
						val2ndBestResult = minResult;
						minResult = erro;
						bestResult = minResult;
					}
				break;
				case ACURACIA:
					boolean acerto = lstResultados.get(0).getClasseReal() == lstResultados.get(0).getClassePrevista();
					if(acerto)
					{
						//se acertou, verifica a probabilidade do acerto
						double prob =  lstResultados.get(0).getConfianca();
						isEqual = prob == maxResult;
						isBetter = prob > maxResult;
						if(isBetter)
						{
							val2ndBestResult = maxResult;
							maxResult = prob;
							bestResult = maxResult;
						}
					}
						
			}
			//coloca a melhor visao caso necessario
			if(isBetter)
			{
				view2ndBestResult = viewBestResult;
				viewBestResult =view;
			}else
			{
				if(isEqual)
				{
					viewBestResult +=";"+view;
				}
			}
			
		}
		if(inserir)
		{
			insertBest(qid,bestResult,viewBestResult,nomExperimentoComb,view2ndBestResult,val2ndBestResult);
		}
		return viewBestResult;
	}
	
	public static Map<String,Integer> agrupaMelhorResultado(MetricaUsada metrica,String nomExpComb,String ... arrNomExperimentoViews) throws Exception
	{
		inicializaBD();
		//resgata a lista de resultado por visao (visao => id_do_resultado => resultado_uitem
		Map<String, Map<Long,ResultadoItem>> mapResultsPorViews  =  getResultsPerView(arrNomExperimentoViews);
		//mapa auxiliar para sabermos quais perguntas precisam ser ordenadas (a ordenacao eh por pergunta) id_pergunta => [lista resps]
		ListaAssociativa<Long, Long> mapRespsPorPergunta = new ListaAssociativa<Long, Long>();
		if(metrica == MetricaUsada.NDCG ||metrica == MetricaUsada.NDCG_EXP)
		{
			mapRespsPorPergunta = getRespsPorPergunta(arrNomExperimentoViews);
		}else
		{
			for(ResultadoItem ri: mapResultsPorViews.get(arrNomExperimentoViews[0]).values())
			{
				mapRespsPorPergunta.put(ri.getId(), ri.getId());
			}
		}
		
		//resgata menor classe
		double minClassView = getMinClassView(mapResultsPorViews);
		System.out.println("MENOR CLASSE: "+minClassView);
		deleteCombinacaBestResult(nomExpComb);
		boolean bolInserir = true; 
		
		Map<String,Integer> mapNumBestPerView = new HashMap<String,Integer>();
		ListaAssociativa<Long,String> bestViewPerQid = new ListaAssociativa<Long,String>(); 
		//para cada pergunta,verfica qual view foi melhor
		//int count = 0;
		getBestResults(metrica, nomExpComb, mapResultsPorViews,
				mapRespsPorPergunta, minClassView, bolInserir,
				mapNumBestPerView, bestViewPerQid);
		
		return mapNumBestPerView;
	}
	public static void getBestResults(MetricaUsada metrica, String nomExpComb,
			Map<String, Map<Long, ResultadoItem>> mapResultsPorViews,
			ListaAssociativa<Long, Long> mapRespsPorPergunta,
			double minClassView, boolean bolInserir,
			Map<String, Integer> mapNumBestPerView,
			ListaAssociativa<Long, String> bestViewPerQid) throws SQLException,
			ClassNotFoundException {
		
		for(long qId : mapRespsPorPergunta.keySet())
		{
			//System.out.println("oioi");
			String viewBestResult = getBestView(qId,mapResultsPorViews,mapRespsPorPergunta.getList(qId),metrica,nomExpComb,minClassView-1,bolInserir);
			int numBestWithThisView = 1;
			if(viewBestResult.length()>0)
			{
				if(mapNumBestPerView.containsKey(viewBestResult))
				{
					numBestWithThisView = mapNumBestPerView.get(viewBestResult)+1;
				}
				mapNumBestPerView.put(viewBestResult,numBestWithThisView);
			}
			bestViewPerQid.put(qId, viewBestResult.split(";"));
		}
	}
	public static double getMinClassView(
			Map<String, Map<Long, ResultadoItem>> mapResultsPorViews) {
		double minClassView = Float.MAX_VALUE;
		for(Map<Long,ResultadoItem> mapresult : mapResultsPorViews.values())
		{
					for(ResultadoItem ri : mapresult.values())
					{
						if(ri.getClasseReal()< minClassView)
						{
							minClassView = ri.getClasseReal();
						}
					}
					
					//soh uma visao eh o suficiente
					break;
		}
		return minClassView;
	}
	
	
	public static Map<Long,ResultadoItem> agrupaResultPerAnswer(Fold f)
	{
		Map<Long,ResultadoItem> resultPerAnswer = new HashMap<Long,ResultadoItem>();
		for(ResultadoItem ri : f.getResultadosValues())
		{
			resultPerAnswer.put(ri.getId(), ri);
		}
		return resultPerAnswer;
	}
	public static Map<String, Map<Long, ResultadoItem>>  getResultsPerView(String... arrNomExperimentoViews) throws Exception
	{
		Map<String, Map<Long, ResultadoItem>> mapRespsPorPergunta = new HashMap<String,Map<Long,ResultadoItem>>();
		
		
		for(String nomExperimento : arrNomExperimentoViews)
		{
			
			for(Fold f : CalculaResultados.getResultadoItemBanco(nomExperimento))
			{
				Map<Long, ResultadoItem> mapResultPerAnswer = agrupaResultPerAnswer(f);
				if(!mapRespsPorPergunta.containsKey(nomExperimento))
				{
					mapRespsPorPergunta.put(nomExperimento, mapResultPerAnswer);	
				}else
				{
					Map<Long, ResultadoItem> atualResultPerAnwer = mapRespsPorPergunta.get(nomExperimento);
					atualResultPerAnwer.putAll(mapResultPerAnswer);
				}
					
			}
			
		}
		return mapRespsPorPergunta;
	}
	
	/**
	 * Retorna um mapa, dado um pergunta, suas espostas
	 **/
	private static ListaAssociativa<Long, Long> getRespsPorPergunta(String... arrNomExperimentoViews) throws Exception {
		
		ListaAssociativa<Long, Long> mapRespsPorPergunta = new ListaAssociativa<Long, Long>();
		
		
		String nomExperimento = arrNomExperimentoViews[0];
			
			for(Fold f : CalculaResultados.getResultadoItemBanco(nomExperimento))
			{
				for(ResultadoItem ri : f.getResultadosValues())
				{
					mapRespsPorPergunta.put(ri.getQID(), ri.getId());
				}
				
			}
			
		
		//soh precisa do primeiro experimento
		return mapRespsPorPergunta;
		
	}
	
	
	public static void main(String[] args) throws Exception
	{	
		String[] arrQAForumcols = {"cook","stack","english"};
		String[] arrWikicols = {"wiki6_balanceada","starVote","starAmostra","muppets"};
		
		//siglas por view
		Map<String,String> viewSigla = new HashMap<String,String>();
		
		boolean compute_kendall = false;
		
		//wiki
		
		for(String wikiCollection : arrWikicols)
		{
			
			
			
			System.out.println("Comparação coleção: "+wikiCollection);
					
			String combWiki = "'jcdl12_6viewsBal_"+wikiCollection+"_all_metalearning_view_simples_fold_validacao_nove_folds_balanceado'";
			String[] wikiViews = {"jcdl12_6viewsBal_"+wikiCollection+"_grafo_TamIgualTreino",
					"jcdl12_6viewsBal_"+wikiCollection+"_hist_TamIgualTreino",
					"jcdl12_6viewsBal_"+wikiCollection+"_read_TamIgualTreino",
					"jcdl12_6viewsBal_"+wikiCollection+"_struct_TamIgualTreino",
					"jcdl12_6viewsBal_"+wikiCollection+"_style_TamIgualTreino",
					"jcdl12_6viewsBal_"+wikiCollection+"_tam_TamIgualTreino"};
			
			//ComparaViews.kendallTauBetweenAllViews(qaForumViews);
			if(compute_kendall)
			{
				ComparaViews.kendallTauBetweenAllViews(wikiViews);
			}else
			{
				ComparaViews.agrupaMelhorResultado(MetricaUsada.MSE,combWiki,wikiViews);	
			}
			
						
			
		}
		
		/*
		//forum qa
		for(String qaForumCollection : arrQAForumcols)
		{
			System.out.println("Comparação coleção: "+qaForumCollection);
			String[] qaForumViews = {"qa_multiview_RankLibDinamicFeatures_RandomForest_"+qaForumCollection+"_user_wo_vote.amostra_TamIgualTreino",
					"qa_multiview_RankLib_RandomForest_"+qaForumCollection+"_history.amostra_TamIgualTreino",
					"qa_multiview_RankLib_RandomForest_"+qaForumCollection+"_length.amostra_TamIgualTreino",
					"qa_multiview_RankLib_RandomForest_"+qaForumCollection+"_readbility.amostra_TamIgualTreino",
					"qa_multiview_RankLib_RandomForest_"+qaForumCollection+"_relevance.amostra_TamIgualTreino",
					"qa_multiview_RankLib_RandomForest_"+qaForumCollection+"_structure.amostra_TamIgualTreino",
					"qa_multiview_RankLib_RandomForest_"+qaForumCollection+"_style.amostra_TamIgualTreino",
					"qa_multiview_RankLib_RandomForest_"+qaForumCollection+"_user_graph.amostra_TamIgualTreino"};
			String combQA = "qa_multiview_RankLib_combinacao_RandomForest_"+qaForumCollection+".amostra_TamIgualTreino";
			
			
			if(compute_kendall)
			{
				ComparaViews.kendallTauBetweenAllViews(qaForumViews);
			}else
			{
				ComparaViews.agrupaMelhorResultado(MetricaUsada.NDCG_EXP,combQA,qaForumViews);
			}
			
			
		}
		*/

	}
}
