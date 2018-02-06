package apredizadoCombinacao;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import stuctUtil.ListaAssociativa;
import stuctUtil.Tupla;
import aprendizadoResultado.ComparaViews;
import aprendizadoResultado.ValorResultado.MetricaUsada;
import aprendizadoUtils.MetodoAprendizado;
import entidadesAprendizado.Fold;
import entidadesAprendizado.ResultadoItem;

public class SeletorViews {
	public enum DESEMPATE_MODE{
		BEST_VIEW,NEWCLASS,ELIMINATE_FROM_TRAIN;
	}
	private static final String STR_EMPATE = "__empate__";
	private MetodoAprendizado metApSeletor = null;
	private File featSelectorFile = null;
	private MetricaUsada metrica = null;
	private DESEMPATE_MODE desempateMode = DESEMPATE_MODE.BEST_VIEW;
	private boolean balanceClasses = false;
	private boolean justPredictions = false;
	
	private Map<Long,Long> mapPerguntaPorResp = new HashMap<Long,Long>();
	private List<Tupla<File,File>> lstArqTreino = new ArrayList<Tupla<File,File>>();
	private List<Tupla<File,File>> lstArqTeste = new ArrayList<Tupla<File,File>>();
	
	
	private Tupla<Map<String,Double>,Double> mapClassIdxAndLastIdx = new Tupla<Map<String,Double>,Double>(new HashMap<String,Double>(),0.0);
	
	public SeletorViews(MetodoAprendizado metApSeletor,File featSelectorFile,MetricaUsada metrica)
	{
		this.metApSeletor = metApSeletor;
		this.featSelectorFile = featSelectorFile;
		this.metrica =  metrica;
	}
	public DESEMPATE_MODE getDesempateMode()
	{
		return this.desempateMode;
	}
	public Map<Long,Long> getPerguntaPorResp()
	{
		return this.mapPerguntaPorResp;
	}
	public Map<String,Double> getMapClassIdx()
	{
		return this.mapClassIdxAndLastIdx.getX();
	}
	public void setJustPredictions(boolean justPred)
	{
		this.justPredictions = justPred;
	}
	public void setBalancearSeletor(boolean bal)
	{
		this.balanceClasses = bal;
	}
	
	public Map<Long,String> featuresFromResult(Map<Long,ResultadoItem> allResultsPerId,MetodoAprendizado metApCombinacao)
	{
		Map<Long,String> lstFeaturesPerResult = new HashMap<Long,String>();
		
		for(long id : allResultsPerId.keySet())
		{
			ResultadoItem ri = allResultsPerId.get(id);
			
			HashMap<Long,String> features = new HashMap<Long,String>();
			int idxFeat = 0;
			
			//	caso haja um vetor de probabilidade, para cada probabilidade uma feature, caso contrario o resultado eh a classe e a confianca
			//define a classe como feature
			features.put((long)++idxFeat, Double.toString(ri.getClassePrevista()));
			
			
			if(ri.getConfianca()>0)
			{
				features.put((long)++idxFeat, Double.toString(ri.getConfianca()));
				
				for(float prob : ri.getProbPorClasse())
				{
					features.put((long)++idxFeat, Double.toString(prob));	
				}
				
			}
			
			Integer qid = mapPerguntaPorResp.containsKey(id)?mapPerguntaPorResp.get(id).intValue():null;
			if(qid!= null)
			{
				lstFeaturesPerResult.put(id, metApCombinacao.gerarLinhaDataset(ri.getClasseReal(), (int)id, qid, features,new HashMap<String,String>()));
			}else
			{
				lstFeaturesPerResult.put(id, metApCombinacao.gerarLinhaDataset(ri.getClasseReal(), (int)id, features));
			}
				
			
			
		}
		
		return lstFeaturesPerResult;
		
	}
	public List<Tupla<File,File>> getLstTreino()
	{
		return this.lstArqTreino;
	}
	public List<Tupla<File,File>> getLstTeste()
	{
		return this.lstArqTeste;
	}
	public Map<Long,String> selecionaViews(Map<Long,String> mapFeatMetaLearnigPredictions,Map<Long,String> mapFeatMetaLearnig,
			Fold[] resultsPorViewsTreino,Fold[] resultsPorViewsTeste,Fold[] resultPorViewValidacao,
			MetodoAprendizado metApCombinacao) throws Exception
	{
		Map<Long,ResultadoItem> allResultsPerId = new HashMap<Long,ResultadoItem>();
		
		
		//pega features do featSeletorFile
		Map<Long,String> mapfeaturesfSelector = new HashMap<Long,String>();
		if(featSelectorFile != null)
		{
			MetaLearning.getFeaturesFromFile(metApSeletor, featSelectorFile, mapfeaturesfSelector,new HashMap<Long,Double>());
		}
		
		
		
		File dir = new File(resultsPorViewsTeste[0].getOrigem().getParent(),metApCombinacao.getNomExperimento());
		//faz map de treino e teste completos 
		Map<String, Map<Long,ResultadoItem>> mapResultsPorViewsTreino =  getResultsPerView(resultsPorViewsTreino);
		Map<String, Map<Long,ResultadoItem>> mapResultsPorViewsTeste = getResultsPerView(resultsPorViewsTeste);
		Map<String, Map<Long,ResultadoItem>> mapResultsPorViewsValidacao = new HashMap<String,Map<Long,ResultadoItem>>();
		if(resultPorViewValidacao != null)
		{
			getResultsPerView(resultPorViewValidacao);
		}
		
		int numFold = resultsPorViewsTeste[0].getNum();
		Map<String, Map<Long,ResultadoItem>> mapResultsFirstTreinoSubFold = null;
		
		
		//prepara a lista de resultados separados para o treino e teste
		//para o treino, faz por subfold
		for(int subFoldIdx = 0 ; subFoldIdx<resultsPorViewsTreino[0].getSubFolds().length ; subFoldIdx++)
		{
			Fold[] arrFolds = new Fold[resultsPorViewsTreino.length];
			
			Map<String, Map<Long,ResultadoItem>>  mapResultsPorViewsTreinoSubFoldTreino= new HashMap<String,Map<Long,ResultadoItem>>();
			Map<String, Map<Long,ResultadoItem>> mapResultsPorViewsTreinoSubFoldTeste = new HashMap<String,Map<Long,ResultadoItem>>();
			
			
			createMapsResultsSubfold(resultsPorViewsTreino,mapResultsPorViewsTreino, subFoldIdx,mapResultsPorViewsTreinoSubFoldTreino,mapResultsPorViewsTreinoSubFoldTeste);
			File dirSubfold = new File(dir,"subFolds");
			//depois disso, rodar para o teste e treino inserido por fold do treino
			allResultsPerId.putAll(
									selecionaViews(numFold,"subTreino_"+subFoldIdx,dirSubfold,
														this.justPredictions?mapFeatMetaLearnigPredictions:mapFeatMetaLearnig,
														mapfeaturesfSelector,
														mapResultsPorViewsTreinoSubFoldTreino,mapResultsPorViewsTreinoSubFoldTeste,
														metApCombinacao));
			
			//deixa o primeiro treino para ser usado ao plicar o teste no treino
			if(mapResultsFirstTreinoSubFold == null)
			{
				mapResultsFirstTreinoSubFold = mapResultsPorViewsTreinoSubFoldTreino;
			}
			
		}
				
	
		//resgata resultado do teste
		allResultsPerId.putAll(
				selecionaViews(numFold,"",dir,
									this.justPredictions?mapFeatMetaLearnigPredictions:mapFeatMetaLearnig,
									mapfeaturesfSelector,
									mapResultsFirstTreinoSubFold,mapResultsPorViewsTeste,
									metApCombinacao));
		//resgata resultado do validacao
		if(resultPorViewValidacao != null)
		{
			allResultsPerId.putAll(
						selecionaViews(numFold,"validacao_",dir,
											this.justPredictions?mapFeatMetaLearnigPredictions:mapFeatMetaLearnig,
											mapfeaturesfSelector,
											mapResultsFirstTreinoSubFold,
											mapResultsPorViewsValidacao,
											metApCombinacao));
		}
		
		return featuresFromResult(allResultsPerId,metApCombinacao);
	}


	private void createMapsResultsSubfold(
			Fold[] resultsPorViewsTreino,
			Map<String, Map<Long, ResultadoItem>> mapResultsPorViewsTreino,
			int subFoldIdx,
			Map<String, Map<Long, ResultadoItem>> mapResultsPorViewsTreinoSubFoldTreino,
			Map<String, Map<Long, ResultadoItem>> mapResultsPorViewsTreinoSubFoldTeste) {
		
		//adiciona o mapResultsPorViewsTreino e filtra com os resultados de teste deste subfold
		for(String view : mapResultsPorViewsTreino.keySet())
		{
			mapResultsPorViewsTreinoSubFoldTreino.put(view, new HashMap<Long,ResultadoItem>());
			mapResultsPorViewsTreinoSubFoldTreino.get(view).putAll(mapResultsPorViewsTreino.get(view));
		}
		
		Set<Long> lstResultadoIds = resultsPorViewsTreino[0].getSubFolds()[subFoldIdx].getIdsResultado();
		
		//filtra em cada visao o resultado
		for(String view : mapResultsPorViewsTreinoSubFoldTreino.keySet())
		{
			Map<Long,ResultadoItem> mapTreino = mapResultsPorViewsTreinoSubFoldTreino.get(view);
			Map<Long,ResultadoItem> mapTeste = new HashMap<Long,ResultadoItem>();
			mapResultsPorViewsTreinoSubFoldTeste.put(view,mapTeste);
			
			for(long resultId : lstResultadoIds)
			{
				//remove do treino poe no teste
				ResultadoItem ri = mapTreino.remove(resultId);
				mapTeste.put(resultId, ri);
			}
		}
	}
	public Map<Long,ResultadoItem> selecionaViews(int numFold,String prefix,File dir,Map<Long,String> mapFeatMetaLearnig,Map<Long,String> mapfeaturesfSelector,
													Map<String, Map<Long,ResultadoItem>> mapResultsPorViewsTreino,Map<String, Map<Long,ResultadoItem>> mapResultsPorViewsTeste,
													MetodoAprendizado metApCombinacao
													) throws Exception
	{
		//metrica depende da instancia 
		MetricaUsada metrica = this.metrica;
		

		
		
		//cria treino e teste 
		Tupla<File,File>  arqsTreino = criaDataset(numFold,prefix+"treino",dir,mapResultsPorViewsTreino, mapfeaturesfSelector, metApCombinacao,metrica,mapFeatMetaLearnig,true);
		Tupla<File,File>  arqsTeste = criaDataset(numFold,prefix+"teste",dir,mapResultsPorViewsTeste, mapfeaturesfSelector, metApCombinacao,metrica,mapFeatMetaLearnig,false);
		
		lstArqTreino.add(arqsTreino);
		lstArqTeste.add(arqsTeste);
		
		Fold foldSeletor = new Fold(numFold,null,arqsTreino.getX(),arqsTeste.getX(),arqsTeste.getY());
		List<ResultadoItem> lsResultsSeletor = metApSeletor.testar(foldSeletor);
		
		Map<Long,ResultadoItem> mapResultsSeletor = new HashMap<Long,ResultadoItem>();
		for(ResultadoItem ri :lsResultsSeletor)
		{
			mapResultsSeletor.put(ri.getId(), ri);
		}
		
		return mapResultsSeletor;
		
	}
	public Map<Long,Map<Long,String>> adicionaRankPosFeatPerAnswer(ListaAssociativa<Long, Long> mapRespsPorPergunta,Map<String, Map<Long,ResultadoItem>> mapResultsPorViews)
	{
		long featId = 1;
		Map<Long,Map<Long,String>> lstFeaturesPerInstance = new HashMap<Long,Map<Long,String>>();
		
		//para cada view, adiciona uma feature com a posicao do ranking pra cada instancia
		for(String view : mapResultsPorViews.keySet())
		{		
			for(long qId : mapRespsPorPergunta.keySet())
			{
				List<Long> resps = mapRespsPorPergunta.getList(qId);

				List<ResultadoItem> lstResultados = new ArrayList<ResultadoItem>();
				
				//agrupa resultado das respostas para esta view
				for(long resp : resps)
				{
					ResultadoItem ri = mapResultsPorViews.get(view).get(resp);
					lstResultados.add(ri);
					
				}
				//ordena pela classe prevista 
				Collections.sort(lstResultados, new Comparator<ResultadoItem>(){

					@Override
					public int compare(ResultadoItem o1, ResultadoItem o2)
					{
						return (int) Math.round((o2.getClassePrevista()*10000.0)-(o1.getClassePrevista()*10000.0));
					}
					});
				
				//adiciona a feature por instancia (o indice da lista do resultado eh o ranking)
				for(int idx = 0 ; idx< lstResultados.size() ; idx++)
				{
					long instanceId = lstResultados.get(idx).getId();
					if(!lstFeaturesPerInstance.containsKey(instanceId))
					{
						lstFeaturesPerInstance.put(instanceId,new HashMap<Long,String>());
					}
					Map<Long,String> mapFeatures = lstFeaturesPerInstance.get(instanceId);
					
					mapFeatures.put(featId, Integer.toString(idx));
				}
			}
			//muda o id da feature
			featId++;
		}
		return lstFeaturesPerInstance;
	}
	@SuppressWarnings("unchecked")
	public Tupla<File,File> criaDataset(int numFold,String prefixFileName,File dir,Map<String, Map<Long,ResultadoItem>> mapResultsPorViews,	
										Map<Long,String> mapfeaturesfSelector, 	MetodoAprendizado metApCombinacao, MetricaUsada metrica,Map<Long,String> mapFeatMetaLearnig,
										boolean isTrain)
			throws SQLException, ClassNotFoundException, FileNotFoundException,
			IOException {
		
		//resgata a lista de resultado por visao (visao => id_do_resultado => resultado_uitem
		  //=  getResultsPerView(resultPorViews);
		
		//cria map de respostas por pergunta (caso seja ndcg, mapeia as resps por qid caso nao seja, deixa a resp correta
		ListaAssociativa<Long, Long> mapRespsPorPergunta = new ListaAssociativa<Long, Long>();
		
		if(metrica == MetricaUsada.NDCG ||metrica == MetricaUsada.NDCG_EXP)
		{
			String nomView = mapResultsPorViews.keySet().iterator().next();
			for(ResultadoItem ri: mapResultsPorViews.get(nomView).values())
			{
				mapRespsPorPergunta.put(ri.getQID(), ri.getId());
				mapPerguntaPorResp.put(ri.getId(), ri.getQID());
			}
			
		}else
		{
			String nomView = mapResultsPorViews.keySet().iterator().next();
			for(ResultadoItem ri: mapResultsPorViews.get(nomView).values())
			{
				mapRespsPorPergunta.put(ri.getId(), ri.getId());
			}
		}
		
		//adiciona o rank como feats adicionaris 
		Map<Long,Map<Long,String>> lstFeatsAdicionais = new HashMap<Long,Map<Long,String>>();
		
		if(metrica == MetricaUsada.NDCG || metrica == MetricaUsada.NDCG_EXP)
		{
			lstFeatsAdicionais = adicionaRankPosFeatPerAnswer(mapRespsPorPergunta, mapResultsPorViews);
				
		}
		
		
		//cira map de para cada resultado uma view
		double minClassView = ComparaViews.getMinClassView(mapResultsPorViews);
		
		
		Map<String,Integer> mapNumBestPerView = new HashMap<String,Integer>();
		ListaAssociativa<Long,String> bestViewPerQid = new ListaAssociativa<Long,String>(); 
		
		//para cada pergunta,verfica qual view foi melhor
		//int count = 0;
		ComparaViews.getBestResults(metrica, metApCombinacao.getNomExperimento(), mapResultsPorViews,mapRespsPorPergunta, minClassView, false,mapNumBestPerView, bestViewPerQid);
		
		


		
		//faz dataset 
		File dirDataset = new File(dir,"seletor");
		if(!dirDataset.exists())
		{
			dirDataset.mkdirs();
		}
		File arqDataset = new File(dirDataset, prefixFileName+numFold);
		File arqIds = new File(dirDataset, prefixFileName+numFold+".foldids");
		
		//cria classe por id de instancia
		HashMap<Long,Double> classPorInstancia = new HashMap<Long,Double>();
		ListaAssociativa<Double,Long> pergsPorClass = new ListaAssociativa<Double,Long>();
		List<Long> lstQidsToRemove = new ArrayList<Long>();
		for(long qid : mapRespsPorPergunta.keySet())
		{
			
			Double classe  = getClasseInstancia(qid, bestViewPerQid, mapNumBestPerView);
			for(long instanceId : mapRespsPorPergunta.getList(qid))
			{
				
				classPorInstancia.put(instanceId, classe);
			}
			pergsPorClass.put(classe, qid);
			//caso existe empate, remover todas as perguntas que levam a empate caso seja treino
			if(this.mapClassIdxAndLastIdx.getX().containsKey(STR_EMPATE) && this.desempateMode == DESEMPATE_MODE.ELIMINATE_FROM_TRAIN && isTrain)
			{
				double classEmpate = this.mapClassIdxAndLastIdx.getX().get(STR_EMPATE);
				if(classEmpate == classe)
				{
					lstQidsToRemove.add(qid);
					

				}
				
			}
			/*
			if(classe == 2 && isTrain)
			{
				lstQidsToRemove.add(qid);
			}
			*/
		}
		
		if(balanceClasses && isTrain)
		{
			lstQidsToRemove.addAll(balanceClasses(mapRespsPorPergunta, classPorInstancia, pergsPorClass));
		}
		
		for(long qid : lstQidsToRemove){
			mapRespsPorPergunta.removeKey(qid);
		}
		
		
		MetaLearning.fazDataset(metApSeletor,arqDataset,arqIds, mapRespsPorPergunta,mapPerguntaPorResp,classPorInstancia,lstFeatsAdicionais,mapfeaturesfSelector,mapFeatMetaLearnig);
		
		
		return new Tupla<File,File>(arqDataset,arqIds);
	}
	public List<Long> balanceClasses(ListaAssociativa<Long, Long> mapRespsPorPergunta,HashMap<Long,Double> classPorInstancia,ListaAssociativa<Double,Long> pergsPorClass){
		Map<Double,Integer> classDistribuction = new HashMap<Double,Integer>();
		
		//cria distribuicao das classes
		for(Double valClass : classPorInstancia.values())
		{
			if(!classDistribuction.containsKey(valClass))
			{
				classDistribuction.put(valClass, 1);
			}else
			{
				int val = classDistribuction.get(valClass)+1;
				classDistribuction.put(valClass, val);
			}
		}
		
		//e verifica qual eh a classe menor e ve o tamanho dela
		int minTamClass = Integer.MAX_VALUE;
		for(int tamClass : classDistribuction.values())
		{
			 if(minTamClass>tamClass)
			 {
				 minTamClass = tamClass;
			 }
		}
		List<Long> lstPergsToRemove = new ArrayList<Long>();
		
		
		Random rnd = new Random(2309130929323L);
		
		//faz a remocao de classes com valores maior que minTamClass
		for(Double classVal : classDistribuction.keySet())
		{
			int tamClass = classDistribuction.get(classVal);
			List<Long> lstPergsPerClass = pergsPorClass.getList(classVal);
			
			//remove ateh ficar <= minTamClass
			while(tamClass > minTamClass)
			{
				int idxToRemove = (int)Math.floor(rnd.nextDouble()*lstPergsPerClass.size());
				long idPerg = lstPergsPerClass.remove(idxToRemove);
				
				//numero de resps removidas com esta remocao 
				int numRespsRemoved = mapRespsPorPergunta.getList(idPerg).size();
				//remove numRespsRemoved
				tamClass -= numRespsRemoved;
				
				lstPergsToRemove.add(idPerg);
				
				
				
				
				
				
			}
			
			
		}
		
		return lstPergsToRemove;
		
		
		
	}
	public Double getClasseInstancia(long qid,ListaAssociativa<Long,String> bestViewPerQid,Map<String,Integer> mapNumBestPerView)
	{
		List<String> lstViews = bestViewPerQid.getList(qid);
		String selView = "";
		if(lstViews.size() > 1)
		{
			
			if(this.desempateMode == DESEMPATE_MODE.NEWCLASS ||this.desempateMode == DESEMPATE_MODE.ELIMINATE_FROM_TRAIN) 
			{
				selView = STR_EMPATE;
			}else
			{
				//caso haja mais de uma visao empatada, pega a que mais ganha
				String bestView = "";
				int numTimesBestMax = -1;
				for(int i = 0 ; i < lstViews.size() ; i++)
				{
					String view = lstViews.get(i);
					if(view.length()!=0 && mapNumBestPerView.containsKey(view) )
					{
						int numTimesBest = mapNumBestPerView.get(view);
						
						//se essa visao for a que tiver mais vezes best, seleciona ela
						if(numTimesBest >numTimesBestMax)
						{
							numTimesBestMax = numTimesBest;
							bestView = view;
						}
					}
				}
			}
		}else
		{
			if(lstViews.size() == 1)
			{
				selView = lstViews.get(0);
			}
		}
		

		
		//retorna indice double da classe
		Map<String,Double> mapClassIdx = mapClassIdxAndLastIdx.getX();
		Double lastIdxClass = mapClassIdxAndLastIdx.getY();
		if(mapClassIdx.containsKey(selView))
		{
			//caso exista, eh soh retornar
			return mapClassIdx.get(selView);
		}else
		{
			//caso nao exista, cria um novo idx e o retorna
			lastIdxClass ++;
			mapClassIdxAndLastIdx.setY(lastIdxClass);
			Double idxClass = lastIdxClass;
			mapClassIdx.put(selView, idxClass);
			return idxClass;
		}
		/*
		if(bestView.length() == 0)
		{
			//se nao tiver nenhum, retorna classe que nao foi nenhuma visão
			return Integer.toString(mapNumBestPerView.size()+1);
		}
		
		return bestView;
		*/
	}

	

	private Map<String, Map<Long, ResultadoItem>> getResultsPerView(Fold[] resultPorViews) {
		Map<String,Map<Long,ResultadoItem>> mapResultsPerView = new HashMap<String,Map<Long,ResultadoItem>>();
		

		for(int i = 0 ; i<resultPorViews.length ; i++)
		{
			Fold f= resultPorViews[i];
			Map<Long,ResultadoItem> mapResultView = new HashMap<Long,ResultadoItem>();
			mapResultsPerView.put(Integer.toString(i), mapResultView);
			boolean encontrouResult = false;
			for(ResultadoItem ri : f.getResultadosValues())
			{
				encontrouResult = true;
				mapResultView.put(ri.getId(), ri);
			}
			//se nao achou resultado, vai para os subfolds e adiciona  os results de lah
			if(!encontrouResult)
			{
				for(Fold subFold : f.getSubFolds())
				{
					for(ResultadoItem ri : subFold.getResultadosValues())
					{
						mapResultView.put(ri.getId(), ri);
					}		
				}
			}
		}
		return mapResultsPerView;
	}

	public void setMetrica(MetricaUsada metrica) {
		// TODO Auto-generated method stub
		this.metrica = metrica;
	}
}
