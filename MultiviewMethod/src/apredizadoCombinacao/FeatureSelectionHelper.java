package apredizadoCombinacao;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.MalformedParametersException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import apredizadoCombinacao.MetaLearning.TIPO_CONTEUDO_DATASET;
import aprendizadoResultado.CalculaResultados;
import aprendizadoResultado.ResultadoAnalyser;
import aprendizadoUtils.FitnessCalculator;
import aprendizadoUtils.GenericoLetorLike;
import aprendizadoUtils.GenericoSVMLike;
import aprendizadoUtils.MatrizFeatures;
import aprendizadoUtils.MetodoAprendizado;
import aprendizadoUtils.SVM;
import arquivo.ArquivoUtil;
import arquivo.TempFiles;
import entidadesAprendizado.Fold;
import entidadesAprendizado.Resultado;
import entidadesAprendizado.ResultadoItem;
import entidadesAprendizado.ResultadoViews;
import entidadesAprendizado.View;
import entidadesAprendizado.View.FeatureType;
import featSelector.ViewCreatorHelper;
import io.Sys;
import main.CriaObjToEvalPerDataset.ML_MODE;
import matematica.MathUtil;
import scriptsUtil.CombinaClassificacaoViews;
import stuctUtil.ListaAssociativa;
import stuctUtil.Tripla;
import stuctUtil.Tupla;

public class FeatureSelectionHelper implements Serializable,ViewCreatorHelper
{
	/** 
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final boolean DELETE_FILES_ON_EXIT = true ;
	private static final boolean FORCE_SVR = false;
	private boolean runMetalearningWithFeatSet = false;
	private boolean runJustBase = false;
	
	//global_feat_idx => view_idx,idxFeat 
	private Map<Short,Tupla<Short,Short>> mapIdxFeatGlobalToViewsFeatIdx = new HashMap<Short,Tupla<Short,Short>>(200);
	
	
     //view_idx => (instance_id => (feat_id => feat_val))
	//private Map<Integer,Map<Long,Map<Short,String>>> mapFeatsPerView = new HashMap<Integer,Map<Long,Map<Short,String>>>(10);
	private Map<Integer,MatrizFeatures<Short>> mapFeatsPerView = new HashMap<Integer,MatrizFeatures<Short>>();
	 
	 
	 // Features especificas por cada particao de treino  
	 //view_idx => (<instance_id,fold_num,treino_subFold_num>  => (feat_id => feat_val))
	 //private Map<Integer,Map<Tripla<Long,Integer,Integer>,Map<Short,String>>> mapFeatsPerViewTreino = new HashMap<Integer,Map<Tripla<Long,Integer,Integer>,Map<Short,String>>>();
	//private Map<Integer,Map<Tripla<Long,Integer,Integer>,Map<Short,String>>> mapFeatsPerViewTreino = new HashMap<Integer,Map<Tripla<Long,Integer,Integer>,Map<Short,String>>>();
	private Map<Tripla<Integer,Integer,Integer>,MatrizFeatures<Short>> mapFeatsPerViewTreino = new HashMap<Tripla<Integer,Integer,Integer>,MatrizFeatures<Short>>();
	
	 
	 
	 
	 //fold_idx => [instances]
	 private ListaAssociativa<Integer, Tripla<Float,Integer,Integer>> instancesTrainPerFold = new ListaAssociativa<Integer, Tripla<Float,Integer,Integer>>();

	private ListaAssociativa<Integer, Tripla<Float,Integer,Integer>> instancesValidationPerFold = new ListaAssociativa<Integer, Tripla<Float,Integer,Integer>>();
	 private ListaAssociativa<Integer, Tripla<Float,Integer,Integer>> instancesTestPerFold = new ListaAssociativa<Integer, Tripla<Float,Integer,Integer>>();
	 
	 
	 //fold_idx,sub_fold_idx => [instances]
	 private ListaAssociativa<Tupla<Integer,Integer>, Tripla<Float,Integer,Integer>> instancesTrainPerSubTrainFold = new ListaAssociativa<Tupla<Integer,Integer>,Tripla<Float,Integer,Integer>>();
	 private ListaAssociativa<Tupla<Integer,Integer>, Tripla<Float,Integer,Integer>> instancesTestPerSubTrainFold = new ListaAssociativa<Tupla<Integer,Integer>,Tripla<Float,Integer,Integer>>();
	 private ListaAssociativa<Tupla<Integer,Integer>, Tripla<Float,Integer,Integer>> instancesValidationPerSubTrainFold = new ListaAssociativa<Tupla<Integer,Integer>,Tripla<Float,Integer,Integer>>();
	 
	 private int[] numSubFoldsPerFold=null;
	 private MetodoAprendizado metApCombinacao = null;
	 private MetodoAprendizado metApViews = null;
	 private MetodoAprendizado[] arrMetApViews = null;
	 private File arqResultadoViews = null;
	 private Double minClasse = null;
	 private String nomArquivo = "";
	 private String methodName = "";
	 private Map<String,String> mapParams = new HashMap<String,String>();
	 
	 private List<Resultado> lstResultPorView = new ArrayList<Resultado>();
	 
	 

	 
	 /********************** Matriz de features (substitui mapFeatsPerView mapIdxFeatGlobalToViewsFeatIdx ao mandar executar por questoes de otimizacao de memoria) **/
	 
	 
	 public FeatureSelectionHelper(MetodoAprendizado metApView,MetodoAprendizado metApCombinacao,File arqResultadoViews)
	 {
		 this.metApViews = metApView;
		 this.metApCombinacao = metApCombinacao;
		 this.arqResultadoViews = arqResultadoViews;
	 }
	 public MetodoAprendizado getMetodoCombinacao()
	 {
		 return this.metApCombinacao;
	 }
	 public void setMinClasse(double minClasse)
	 {
		 this.minClasse = minClasse;
	 }
	 public int[] getNumFoldsPerFold()
	 {
		 return this.numSubFoldsPerFold;
	 }
	 public void setNumFoldsPerFold(int[] numSubFoldsPerFold)
	 {
		this.numSubFoldsPerFold = numSubFoldsPerFold;
	 }
	 public MetodoAprendizado getMetodoViews()
	 {
		 return this.metApViews;
	 }
	 public void setNomArquivo(String nomArquivo)
	 {
		 this.nomArquivo = nomArquivo;
	 }
	 public void setNumSubFoldsPerFold(int[] numSubFoldsPerFold)
	 {
		 this.numSubFoldsPerFold = numSubFoldsPerFold;
	 }
	 
	 public String getNomArquivo()
	 {
		 return this.nomArquivo;
	 }
	 public String getMethodName()
	 {
		 return this.methodName;
	 }
	 public void setMethodName(String methodName)
	 {
		 this.methodName = methodName;
	 }
	 public void addParamTeste(String paramKey,String paramVal)
	 {
		 if(this.metApViews instanceof GenericoSVMLike)
		 {
			((GenericoSVMLike) (this.metApViews)).setParamUnsetedTest(paramKey,paramVal);
		 }
	 }
	 public void addParamTreino(String paramKey,String paramVal)
	 {
		 if(this.metApViews instanceof GenericoSVMLike)
		 {
			((GenericoSVMLike) (this.metApViews)).setParamUnsetedTrain(paramKey,paramVal);
		 }
	 }
	 
	 public Map<Short,Tupla<Short,Short>> getMapLocalFeatIdxPerGlobal()
	 {
		 return this.mapIdxFeatGlobalToViewsFeatIdx;
	 }
	 public void carregaFeatureSelection() throws Exception
	 {
		 ResultadoViews rv = this.getViewsResult(new File(this.arqResultadoViews.getParent(),"views_"+this.arqResultadoViews.getName()));
		 View[] arrViews = rv.getViews();
		 Fold[][] arrTreinoPerView = new Fold[rv.getViews().length][];
		 Fold[][] arrTestePerView = new Fold[rv.getViews().length][];
		 for(int idxView =0 ; idxView < rv.getViews().length ; idxView++)
		 {
			 arrTreinoPerView[idxView] = arrViews[idxView].getResultTreino().getFolds();
			 arrTestePerView[idxView] = arrViews[idxView].getResultTeste().getFolds();
		 }
		 carregaFeatureSelection(rv,arrTreinoPerView,arrTestePerView);
	 }
	 public boolean isRunJustBase()
	 {
		 return this.runJustBase;
	 }
	 public boolean isRunMetaLearningWithFeatSet()
	 {
		 return this.runMetalearningWithFeatSet;
	 }
	 public void setMetaLearningWithFeatSet(boolean runMetawithFeat)
	 { 
		 this.runMetalearningWithFeatSet = runMetawithFeat;
	 }
	 public void setRunJustBase(boolean runJustBase)
	 {
		 this.runJustBase = runJustBase;
	 }
	 public List<Resultado> getLastResultViewResult()
	 {
		 return this.lstResultPorView;
	 }
	 public ListaAssociativa<Integer, Tripla<Float, Integer, Integer>> getInstancesTrainPerFold() {
		return instancesTrainPerFold;
	}
	public void setInstancesTrainPerFold(
			ListaAssociativa<Integer, Tripla<Float, Integer, Integer>> instancesTrainPerFold) {
		this.instancesTrainPerFold = instancesTrainPerFold;
	}
	public ListaAssociativa<Integer, Tripla<Float, Integer, Integer>> getInstancesValidationPerFold() {
		return instancesValidationPerFold;
	}
	public void setInstancesValidationPerFold(
			ListaAssociativa<Integer, Tripla<Float, Integer, Integer>> instancesValidationPerFold) {
		this.instancesValidationPerFold = instancesValidationPerFold;
	}
	public ListaAssociativa<Integer, Tripla<Float, Integer, Integer>> getInstancesTestPerFold() {
		return instancesTestPerFold;
	}
	public void setInstancesTestPerFold(
			ListaAssociativa<Integer, Tripla<Float, Integer, Integer>> instancesTestPerFold) {
		this.instancesTestPerFold = instancesTestPerFold;
	}
	public ListaAssociativa<Tupla<Integer, Integer>, Tripla<Float, Integer, Integer>> getInstancesTrainPerSubTrainFold() {
		return instancesTrainPerSubTrainFold;
	}
	public void setInstancesTrainPerSubTrainFold(
			ListaAssociativa<Tupla<Integer, Integer>, Tripla<Float, Integer, Integer>> instancesTrainPerSubTrainFold) {
		this.instancesTrainPerSubTrainFold = instancesTrainPerSubTrainFold;
	}
	public ListaAssociativa<Tupla<Integer, Integer>, Tripla<Float, Integer, Integer>> getInstancesTestPerSubTrainFold() {
		return instancesTestPerSubTrainFold;
	}
	public void setInstancesTestPerSubTrainFold(
			ListaAssociativa<Tupla<Integer, Integer>, Tripla<Float, Integer, Integer>> instancesTestPerSubTrainFold) {
		this.instancesTestPerSubTrainFold = instancesTestPerSubTrainFold;
	}
	public ListaAssociativa<Tupla<Integer, Integer>, Tripla<Float, Integer, Integer>> getInstancesValidationPerSubTrainFold() {
		return instancesValidationPerSubTrainFold;
	}
	public void setInstancesValidationPerSubTrainFold(
			ListaAssociativa<Tupla<Integer, Integer>, Tripla<Float, Integer, Integer>> instancesValidationPerSubTrainFold) {
		this.instancesValidationPerSubTrainFold = instancesValidationPerSubTrainFold;
	}
	 public void carregaFeatureSelection(ResultadoViews rv,Fold[][] arrFoldsPerViewTreino,Fold[][] arrFoldsPerViewTeste) throws Exception
	 {
		 if(rv != null)
		 {
			 extraiFeaturesPorView(arrFoldsPerViewTreino,arrFoldsPerViewTeste,rv);
		 }else
		 {
			 extraiFeaturesPorView(arrFoldsPerViewTreino, arrFoldsPerViewTreino, this.metApViews);
		 }
		 indexaFeaturesGlobalmente();
			
			//String x=  this.getFeatureVal(0,0,25L, 180);
			System.out.println("Gravando objeto...");
			gravaObject();
			
			countFeatures();
			//
			
	}
	 
	public void gravaObject() throws IOException {
		
		//ArquivoUtil.gravaObjectWithKryo(new File(this.arqResultadoViews.getParentFile(),"feat_study_"+this.arqResultadoViews.getName()), this);
		File arqObj = this.arqResultadoViews;//new File(this.arqResultadoViews.getParentFile(),"feat_study_"+this.arqResultadoViews.getName());
		gravaObject(arqObj);
	}
	public void gravaObject(File arqObj) throws IOException {
		ArquivoUtil.gravaObject(arqObj, this);
		this.setNomArquivo(arqObj.getAbsolutePath());
		
		System.out.println("Gravado em: "+arqObj.getAbsolutePath());
	}

	private void countFeatures() {
		Map<Integer,Integer> featViewCounter  = new HashMap<Integer,Integer>();
		int total = 0;
		for(Tupla<Short,Short> tplViewIdxFeat : mapIdxFeatGlobalToViewsFeatIdx.values())
		{
			if(!featViewCounter.containsKey(tplViewIdxFeat.getX().intValue()))
			{
				featViewCounter.put(tplViewIdxFeat.getX().intValue(), 1);
			}else
			{
				int count = featViewCounter.get(tplViewIdxFeat.getX().intValue())+1;
				featViewCounter.put(tplViewIdxFeat.getX().intValue(), count);
			}
			total++;
		}
		
		System.out.println("Numero de features: "+total);
		System.out.println("Por view: "+featViewCounter);
		
		printFeaturesVector();
		
		
	}
	public void printFeaturesVector() {
		System.out.println("Modelo array: ");
		System.out.print("[");
		for(int idxFeatGlobal = 0; idxFeatGlobal<mapIdxFeatGlobalToViewsFeatIdx.size() ; idxFeatGlobal++)
		{
			Tupla<Short,Short> tplViewIdxFeat = mapIdxFeatGlobalToViewsFeatIdx.get((short)idxFeatGlobal);
			System.out.print(tplViewIdxFeat.getX()+",");
		}
		System.out.print("]");

		System.out.println("Global(view): ");
		System.out.print("[");
		for(int idxFeatGlobal = 0; idxFeatGlobal<mapIdxFeatGlobalToViewsFeatIdx.size() ; idxFeatGlobal++)
		{
			Tupla<Short,Short> tplViewIdxFeat = mapIdxFeatGlobalToViewsFeatIdx.get((short)idxFeatGlobal);
			System.out.print(idxFeatGlobal+"("+tplViewIdxFeat.getY()+"),");
		}
		System.out.print("]");
	}
	public Integer[] getArrAllFeatures()
	{
		Integer[] arrFeats = new Integer[mapIdxFeatGlobalToViewsFeatIdx.size()];
		for(int idxFeatGlobal = 0; idxFeatGlobal<mapIdxFeatGlobalToViewsFeatIdx.size() ; idxFeatGlobal++)
		{
			Tupla<Short,Short> tplViewIdxFeat = mapIdxFeatGlobalToViewsFeatIdx.get((short)idxFeatGlobal);
			arrFeats[idxFeatGlobal] = tplViewIdxFeat.getX().intValue();
		}
		return arrFeats;
	}
	public int getNumViews()
	{
		return this.mapFeatsPerView.size();
	}
	public static void printInstancesPerFold(String tipo,ListaAssociativa<Integer, Tripla<Float,Integer,Integer>> lst)
	{
		for(Integer foldId: lst.keySet())
		{
			System.out.print(tipo+" no fold "+foldId+": {");	
			for(Tripla tpl : lst.getList(foldId))
			{
				System.out.print(tpl.getY()+";");
			}
			System.out.println("}");
		}
	}
	public Resultado retornaFoldsFiltrado(Integer[] arrFiltro) throws Exception{
		Set<Integer> idxViews = new HashSet<Integer>();
		for(int viewIdx : arrFiltro) {
			idxViews.add(viewIdx);
		}
		//MetodoAprendizado metAp = getMetodoAprendizado(methodViewName,metApViewAtual,lstFeatures.size());
		
		GenericoSVMLike gSVM = (GenericoSVMLike) this.metApCombinacao;
		Map<Integer,String> mapMethodPerView = new HashMap<Integer,String>();
		Map<Integer,Map<String,String>> mapParamsViewTreino = new HashMap<Integer,Map<String,String>>(); 
		Map<Integer,Map<String,String>> mapParamsViewTeste = new HashMap<Integer,Map<String,String>>(); 
		for(int idxView: idxViews) {
			mapParamsViewTreino.put(idxView, gSVM.getParamTrain());
			mapParamsViewTeste.put(idxView, gSVM.getParamTest());
			mapMethodPerView.put(idxView, gSVM.getNomeMetodo());
		}
		return retornaFoldsFiltrado(arrFiltro,gSVM.getParamTrain(),gSVM.getParamTest(),
									mapParamsViewTreino,mapParamsViewTeste,mapMethodPerView);
	}
		/**
		 * Dado um array de inteiros com o filtro, retorna folds com os ids de features a ser filtrado
		 * Assume-se que as features ja foram extraidas e indexadas
		 * @param arrFiltro
		 * @return
		 * @throws Exception 
		 */
		public Resultado retornaFoldsFiltrado(Integer[] arrFiltro,
				Map<String,String> mapTrainParamMultiview, Map<String,String> mapTestParamMultiview,
				Map<Integer,Map<String,String>> mapTrainParamPerView, Map<Integer,Map<String,String>> mapTestParamPerView,Map<Integer,String> methodPerView) throws Exception
		{
			/*
			printInstancesPerFold("Teste",this.instancesTestPerFold);
			printInstancesPerFold("Treino",this.instancesTrainPerFold);
			printInstancesPerFold("Validacao",this.instancesValidationPerFold);
			System.exit(0);
			*/
			
			/********************* Imprime uso da memoria **********************/
			
			System.out.println("Memoria usada:"+Sys.getUsedMemory()/(1024.0*1024.0)+" MB");
			System.gc();
			System.out.println("Memoria usada: (depois gc)"+Sys.getUsedMemory()/(1024.0*1024.0)+" MB");
			//resgata conjunto de features por visao a ser criada dado o arrFiltro (view_idx => [ids_features]
			ListaAssociativa<Integer, Integer> mapViewsFeatSet = resgataMapViewsFeatSet(arrFiltro);

			
			File dirViews = new File("/tmp/viewsCreators/cnf_"+UUID.randomUUID());
			this.lstResultPorView = new ArrayList<Resultado>(); 
			
			//cria visoes artificiais
			View[] arrViews = new View[mapViewsFeatSet.keySet().size()];
			int idxView = 0;
			long time = System.currentTimeMillis();
			List<Integer> lstIdxAllFeatures = new ArrayList<Integer>();
			MetodoAprendizado metApViewAtual = null;
			String methodViewName= this.methodName;
			for(int idxNewView : mapViewsFeatSet.keySet())
			{
				System.out.println("--------------------- View #"+idxNewView+" ----------------------");
				metApViewAtual = this.arrMetApViews == null?this.metApViews:this.arrMetApViews[idxNewView-1];
				long subtime = System.currentTimeMillis();
				List<Integer> lstFeatures = mapViewsFeatSet.getList(idxNewView);
			
				
				lstIdxAllFeatures.addAll(lstFeatures);
				
				/**
				 * Verifica qntos atributos este metodo possui, casso possua pouco, altera o metodo da visao (caso seja random forets)
				 */
				MetodoAprendizado metApView = getMetodoAprendizado(methodViewName,metApViewAtual,lstFeatures.size());
				Collections.sort(lstFeatures);
				
				/** Define os parametros especificos desta visao **/
				Map<String,String> mapTreino = mapTrainParamPerView.get(idxNewView);
				Map<String,String> mapTeste = mapTestParamPerView.get(idxNewView);
				methodViewName = this.methodName;
				if(methodPerView.containsKey(idxNewView)) {
					methodViewName = methodPerView.get(idxNewView);
				}
				setMethodParams(metApView, mapTreino, mapTeste,methodViewName);
				if(metApViewAtual instanceof GenericoSVMLike)
				{
					((GenericoSVMLike) metApViewAtual).setGravarNoBanco(false);
					((GenericoSVMLike) metApViewAtual).createTrainTestScripts();
					methodViewName =((GenericoSVMLike) metApViewAtual).getNomeMetodo(); 
				}
				
				/** Cria visao **/
				ArtificialView view = new ArtificialView(this.getNomArquivo(),idxNewView,dirViews,null, metApView, lstFeatures, FeatureType.ARR_GENERIC_VIEW[idxNewView-1],this.minClasse);
				
				view.criaResultadosView(this, this.numSubFoldsPerFold.length, this.numSubFoldsPerFold);
				
				arrViews[idxView] = view;		
				lstResultPorView.add(view.getResultTeste());
				
				idxView++;
				System.out.println("Tempo de execucao da view #"+idxNewView+": "+(System.currentTimeMillis()-subtime)/1000.0);
			}
			System.out.println("Tempo de execucao das views: "+(System.currentTimeMillis()-time)/1000.0);
			
			if(!runJustBase)
			{
				System.out.println("--------------------- Metalearning ----------------------");
				//imprime metodos por visao
				if(this.arrMetApViews != null)
				{
					System.out.println("Metodo por visao: ");
					for (int i = 0; i < arrMetApViews.length; i++) {
						System.out.println("View: "+i+":"+arrMetApViews[i]);
						
					}
				}
				
				
				//cria objeto com resultado de visoes
				ResultadoViews rv = new ResultadoViews(null, null);
				rv.preparaFoldsPorView(arrViews);
				
				/**
				 * Dependendo do numero de visoes, altera o parametro se for random forests
				 */
				
				//realiza combinacao com o metalearning
				time = System.currentTimeMillis();
				
				TIPO_CONTEUDO_DATASET[] arrTipos = {};
				if(runMetalearningWithFeatSet)
				{
					arrTipos = new TIPO_CONTEUDO_DATASET[1];
					arrTipos[0] = TIPO_CONTEUDO_DATASET.FEATURES_SET;
					
				}
				String methodName = this.methodName;
				if(this.metApCombinacao instanceof GenericoSVMLike) {
					methodName = ((GenericoSVMLike) (this.metApCombinacao)).getNomeMetodo();
				}
				MetodoAprendizado metApCombinacao = getMetodoAprendizado(this.methodName,this.metApCombinacao,mapViewsFeatSet.keySet().size());
				setMethodParams(metApCombinacao, mapTrainParamMultiview, mapTestParamMultiview,methodName);
				if(metApCombinacao instanceof GenericoSVMLike)
				{
					((GenericoSVMLike) metApCombinacao).setGravarNoBanco(false);
					((GenericoSVMLike) metApCombinacao).createTrainTestScripts();
				}
				
				//String nomExperimentoComb = metApCombinacao.getNomExperimento();
				metApCombinacao.setNomExperimento("combinacao");//+UUID.randomUUID());
				MetaLearning mt = new MetaLearning(metApCombinacao, arrTipos);
				mt.setViewCreatorHelper(this);
				if(runMetalearningWithFeatSet)
				{
					mt.setFeaturesIdxToInclude(lstIdxAllFeatures);
				}
				
				Combinador cmb = new Combinador("combinacao_"+UUID.randomUUID(), mt, arrViews,rv);
				Resultado r = cmb.executaCombinacao("combinacao_"+UUID.randomUUID(), arrViews,false);
				r.setView(arrViews);
				
				System.out.println("Tempo de execucao comvbinacao: "+(System.currentTimeMillis()-time)/1000.0);
				
				if(DELETE_FILES_ON_EXIT)
				{
					dirViews.deleteOnExit();
					TempFiles.getTempFiles().addFile(dirViews);
				}
				
				return r;
			}else
			{
				return arrViews[0].getResultTeste();
			}
			
		}
		private void setMethodParams(MetodoAprendizado metApView, Map<String, String> mapTreino,
				Map<String, String> mapTeste,String nomMethod) throws Exception {
			if(metApView instanceof GenericoSVMLike) {
				
				
				GenericoSVMLike genSVM = (GenericoSVMLike) metApView;
				genSVM.setMethodName(nomMethod);
				genSVM.clearAllParams();
				if(mapTreino != null) {
					for(String key : mapTreino.keySet()) {
						genSVM.setParamTrain(key,mapTreino.get(key));
					}
				}
				if(mapTeste != null) {
					for(String key : mapTeste.keySet()) {
						genSVM.setParamTrain(key,mapTeste.get(key));
					}
				}
			}
		}
		
		public Fold[][] createFoldsViews(Integer[] arrFiltro) throws Exception
		{
			File dirViews = new File("/tmp/viewsCreators/cnf_"+UUID.randomUUID());
			ListaAssociativa<Integer, Integer> mapViewsFeatSet = resgataMapViewsFeatSet(arrFiltro);
			Fold[][] arrFoldsPerView = new Fold[mapViewsFeatSet.keySet().size()][];
			int idxView = 0; 
			for(int idxNewView : mapViewsFeatSet.keySet())
			{
				long subtime = System.currentTimeMillis();
				List<Integer> lstFeatures = mapViewsFeatSet.getList(idxNewView);
				((GenericoSVMLike) this.metApViews).setGravarNoBanco(false);
				
				/**
				 * Verifica qntos atributos este metodo possui, casso possua pouco, altera o metodo da visao
				 */
				MetodoAprendizado metApView = getMetodoAprendizado(this.methodName,this.metApViews,lstFeatures.size());
				Collections.sort(lstFeatures);
				
				/** Cria visao **/
				ArtificialView view = new ArtificialView(this.nomArquivo,idxNewView,dirViews,null, metApView, lstFeatures, FeatureType.ARR_GENERIC_VIEW[idxView],this.minClasse);
				
				arrFoldsPerView[idxView] = view.createFoldsView(this ,this.numSubFoldsPerFold.length, this.numSubFoldsPerFold);
				
				idxView++;
			}
			
			return arrFoldsPerView;
		}
		private ListaAssociativa<Integer, Integer> resgataMapViewsFeatSet(
				Integer[] arrFiltro) {
			ListaAssociativa<Integer,Integer> mapViewsFeatSet = new ListaAssociativa<Integer,Integer>();
			for(int idxFeat = 0 ; idxFeat<arrFiltro.length ; idxFeat++)
			{
				int idxView = arrFiltro[idxFeat];
				
				if(idxView != 0)
				{
					//Tupla<Integer,Integer> tplIdxOriginalFeat =mapIdxFeatGlobalToViewsFeatIdx.get(idxFeat); 
					mapViewsFeatSet.put(idxView,idxFeat);
				}
			}
			return mapViewsFeatSet;
		}
		public void addParams(Map<String,String> mapParams)
		{
			if(this.mapParams == null)
			{
				this.mapParams = new HashMap<String,String>();
			}
			this.mapParams.putAll(mapParams);
		}
		public Map<String,String> getParams(MetodoAprendizado metAtual,int numFeatures)
		{
			Map<String,String> mapParam = new HashMap<String,String>();//metApAtual.getParams();
			if(this.mapParams != null)
			{
				mapParam.putAll(this.mapParams);
			}
			if( metAtual instanceof GenericoLetorLike)	
			{
				CombinaClassificacaoViews.getFeatPorcByNumFeats(numFeatures,mapParam);
				//mapParam.put("FEAT_PORC", featPorcParam.toString());
				//mapParam.put("FEAT_PORC", featPorcParam.toString());
				mapParam.put("BAG", "30");	
			}
			return mapParam; 
		}
		public void criaSubFold(int fold_id,List<Tupla<Integer, Float>> lstTreino,int numFolds) {
			resetFoldInstancesTrainSubTrainFold(fold_id);
			resetFoldInstancesTrainSubTestFold(fold_id);
			
			//transforma numa lista de ids
			List<Long> lstIds = new ArrayList<Long>();
			
			Map<Long,Tupla<Integer,Float>> mapTuplaPerId = new HashMap<Long,Tupla<Integer,Float>>();
			List<Long> lstAll = new ArrayList<Long>();
			
			//cria os folds 
			for(Tupla<Integer,Float> tpl : lstTreino)
			{
				lstIds.add(tpl.getX().longValue());
				mapTuplaPerId.put(tpl.getX().longValue(),tpl);
				
			}
			
			//faz crossvalidation
			List<Long>[] arrLstPerFold = Fold.divideIntoFolds(numFolds,lstIds);
			Tupla<Integer, Float> tpl = null;
			//armazzena os folds por subids
			for(int subFoldNum = 0 ; subFoldNum < arrLstPerFold.length; subFoldNum++)
			{
				//para cada foldNum, foldNum é o teste 
				for(Long id : arrLstPerFold[subFoldNum])
				{
					tpl = mapTuplaPerId.get(id);
					//subteste
					this.addClassIdAndQidPerTestPerSubTrainFold(fold_id, subFoldNum,tpl.getY() , tpl.getX(), null);
				}
				//e todos que nao sao foldNum é o treino
				for(int foldNumTreino = 0 ; foldNumTreino < arrLstPerFold.length; foldNumTreino++)
				{
					if(foldNumTreino != subFoldNum)
					{
						for(Long id : arrLstPerFold[foldNumTreino])
						{
							tpl = mapTuplaPerId.get(id);
							//subteste
							this.addClassIdAndQidPerTrainPerSubTrainFold(fold_id, subFoldNum,tpl.getY() , tpl.getX(), null);
						}
						
					}
				}
			}
			
		}
		/**
		 * Dado o numero de features, retorna o metodo de apredizaco parametrizado (se necessario)
		 * @param numFeatures
		 * @return
		 * @throws Exception
		 */
		public MetodoAprendizado getMetodoAprendizado(String nomMetodo,MetodoAprendizado metApAtual,int numFeatures)
				throws Exception {
			

			MetodoAprendizado metApView = metApAtual;
			
			/*
			Map<String,String> mapParam = getParams(metApView,numFeatures);
			if(metApView instanceof GenericoLetorLike && !FORCE_SVR)
			{
				metApView =  getMetodoApredizadoL2R(nomMetodo, mapParam);	
			}else
			{
				
				//metApView =  getMetodoAprendizadoRegressao(nomMetodo,mapParam);
				metApView =  metApAtual;
			}
			*/
			return metApView;
		}
	 
	private void adicionaMapInSpecific(Integer foldNum,Integer subFoldNum,
										Map<Tripla<Long,Integer,Integer>,Map<Short,String>> mapFeaturesPorInstancia,
										File arquivo,MetodoAprendizado metAp,MatrizFeatures<Short> mFeatures) throws IOException
	{
		Map<Long,Map<Long,String>> mapInstancePerId =  metAp.mapeiaFeatureInstancias(arquivo);
		for(Long intanceId : mapInstancePerId.keySet())
		{
			HashMap<Short,String> mapFeats = new HashMap<Short,String>();
			for(Long featIdx : mapInstancePerId.get(intanceId).keySet())
			{
				mapFeats.put(featIdx.shortValue(), mapInstancePerId.get(intanceId).get(featIdx));
			}
			mapFeaturesPorInstancia.put(new Tripla<Long,Integer,Integer>(intanceId,foldNum,subFoldNum), mapFeats);
			mFeatures.adicionaFeature(intanceId.intValue(), mapFeats);
			
		}
	}
	
	public enum COLECAO_EXP{
		COOK("cook_amostra","cook"),ENGLISH("english_amostra","english"),STACK("so_amostra","default"),OUTROS("","");
		String amostra;
		String colecao;
		
		
		private COLECAO_EXP(String amostra, String colecao)
		{
			this.amostra = amostra;
			this.colecao = colecao;
		}
		
		public String getAmostra(){
			return this.amostra;
		}
		public String getColecao()
		{
			return this.colecao;
		}
	}
	private COLECAO_EXP getColecaoByExperimento(String nomExperimento)
	{
		if(nomExperimento.contains("cook"))
		{
			return COLECAO_EXP.COOK;
		}
		if(nomExperimento.contains("english"))
		{
			return COLECAO_EXP.ENGLISH;
		}
		if(nomExperimento.contains("stack"))
		{
			return COLECAO_EXP.STACK;
		}
		return COLECAO_EXP.OUTROS;
	}
	public List<Tripla<Float,Integer,Integer>> getClassIdAndQids(File arq,MetodoAprendizado metAp) throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(arq));
		String linhaTexto = "";
		List<Tripla<Float,Integer,Integer>> lstResult = new ArrayList<Tripla<Float,Integer,Integer>>();
		
		while ((linhaTexto = in.readLine()) != null)
		{
			Float classe = Float.parseFloat(metAp.getClasseReal(linhaTexto));
			Integer idVal = metAp.getIdPorLinhaArquivo(linhaTexto,"id");
			Integer qidVal = metAp.getIdPorLinhaArquivo(linhaTexto,"qid");
			lstResult.add(new Tripla<Float, Integer, Integer>(classe, idVal, qidVal));
		}
		in.close();
		
		return lstResult;
	}
	public static Tupla<Integer,Integer> getNumInstancesAndFeatures(MetodoAprendizado metAp,Fold[] arrFold) throws IOException
	{
		int maxFeatId = 0;
		Set<Long> setInstances = new HashSet<Long>();
		for(Fold objFold : arrFold)
		{
			File[] arrFiles = {objFold.getTreino(),objFold.getValidation(),objFold.getTeste()};
			for(File objF : arrFiles)
			{
				if(objF != null && objF.exists())
				{
					Map<Long,Map<Long,String>> mapInstancePerId =  metAp.mapeiaFeatureInstancias(objF);
					
					for(long instId : mapInstancePerId.keySet())
					{
						setInstances.add(instId);
						for(long featId : mapInstancePerId.get(instId).keySet())
						{
							if(featId > maxFeatId)
							{
								maxFeatId = (int)featId;
							}
						}
					}
					
				}
			}
			
		}
		return new Tupla<Integer,Integer>(setInstances.size(),maxFeatId);
	}
	public Tupla<Integer,Integer> getNumInstancesAndFeatures(MetodoAprendizado metAp,File f) throws IOException
	{
		
		if(f != null && f.exists())
		{
			Map<Long,Map<Long,String>> mapInstancePerId =  metAp.mapeiaFeatureInstancias(f);
			
			int instances = mapInstancePerId.keySet().size();
			int maxFeatId = 0;
			for(long instId : mapInstancePerId.keySet())
			{
				for(long featId : mapInstancePerId.get(instId).keySet())
				{
					if(featId > maxFeatId)
					{
						maxFeatId = (int)featId;
					}
				}
			}
			return new Tupla<Integer,Integer>(instances,maxFeatId);
		}
		return new Tupla<Integer,Integer>(0,0);
	}
	public void addClassIdAndQidPerTrainFold(int fold, Float classe,Integer id, Integer qid)
	{
		instancesTrainPerFold.put(fold,new Tripla<Float,Integer,Integer>(classe,id,qid));
	}
	public void resetFoldInstancesTrain(int fold)
	{
		instancesTrainPerFold.reset(fold);
	}
	public void resetFoldInstancesTrainSubTrainFold(int fold)
	{

		resetSubfoldsFromFold(instancesTrainPerSubTrainFold, fold);
	}
	public void resetFoldInstancesTrainSubTestFold(int fold)
	{
		resetSubfoldsFromFold(instancesTestPerSubTrainFold,fold);
	}
	public void printSubFoldsLengthFromFold(int fold)
	{
		System.out.print("Sub Treino fold #"+fold+":");
		printSubfoldsLengthFromFold(instancesTrainPerSubTrainFold,fold);
		System.out.print("\n");
		System.out.print("Sub Teste fold #"+fold+":");
		printSubfoldsLengthFromFold(instancesTestPerSubTrainFold,fold);
		System.out.print("\n");
	}
	public void resetSubfoldsFromFold(ListaAssociativa<Tupla<Integer,Integer>, Tripla<Float,Integer,Integer>> lstInstancesPerSubFold,int fold)
	{
		List<Tupla<Integer,Integer>> resetableValues = new ArrayList<Tupla<Integer,Integer>>();
		//procura todos os com o fold especifico
		for(Tupla<Integer,Integer> tplId : lstInstancesPerSubFold.keySet())
		{
			if(tplId.getX() == fold)
			{
				resetableValues.add(tplId);
			}
		}
		
		//deleta todos os sub folds desse fold especifico
		for(Tupla<Integer,Integer> tplId : resetableValues)
		{
			lstInstancesPerSubFold.reset(tplId);
		}
	}

	public void printSubfoldsLengthFromFold(ListaAssociativa<Tupla<Integer,Integer>, Tripla<Float,Integer,Integer>> lstInstancesPerSubFold,int fold)
	{
		//procura todos os com o fold especifico
		System.out.print("\n");
		System.out.print("Fold #"+fold+": subfolds:");
		for(Tupla<Integer,Integer> tplId : lstInstancesPerSubFold.keySet())
		{
			if(tplId.getX() == fold)
			{
				System.out.print("#"+tplId.getY()+": "+lstInstancesPerSubFold.getList(tplId).size()+"\t");
			}
		}
		
		
	}
	
	public void addClassIdAndQidPerTestFold(int fold, Float classe,Integer id, Integer qid)
	{
		instancesTestPerFold.put(fold,new Tripla<Float,Integer,Integer>(classe,id,qid));
	}
	public void addClassIdAndQidPerTrainPerSubTrainFold(int fold,int subFold, Float classe,Integer id, Integer qid)
	{
		instancesTrainPerSubTrainFold.put(new Tupla<Integer,Integer>(fold,subFold), new Tripla<Float,Integer,Integer>(classe,id,qid) );	
	}
	
	public void addClassIdAndQidPerTestPerSubTrainFold(int fold,int subFold, Float classe,Integer id, Integer qid)
	{
		instancesTestPerSubTrainFold.put(new Tupla<Integer,Integer>(fold,subFold), new Tripla<Float,Integer,Integer>(classe,id,qid) );	
	}
	
	
	public void addMatrizFeaturesGeral(int viewIdx, MatrizFeatures<Short> matFeatures)
	{
		mapFeatsPerView.put(viewIdx, matFeatures);
	}

	private Map<Long,Map<Short,String>> getFeaturesPerView(Fold[] arrFoldTreino,Fold[] arrFoldTeste,MetodoAprendizado metAp,int idxView) throws IOException
	{
		//MetodoAprendizado metAp = v.getMetodoAprendizado();
		Map<Tripla<Long,Integer,Integer>,Map<Short,String>> mapFeaturesPorInstanciaTreino = new HashMap<Tripla<Long,Integer,Integer>,Map<Short,String>>();
		
		Map<Long,Map<Short,String>> mapFeaturesPorInstancia = new HashMap<Long,Map<Short,String>>();
		
		COLECAO_EXP col = getColecaoByExperimento(metAp.getNomExperimento());
		String amostra = col.getAmostra();
		String colecao = col.getColecao();
		
		
		//resgata os ids por fold (apenas quando nao estiver setado
		if(instancesTrainPerFold.keySet().size() == 0)
		{
			numSubFoldsPerFold = new int[arrFoldTeste.length];
			for(int i = 0; i<arrFoldTeste.length ; i++)
			{
				Fold fold = arrFoldTeste[i];
				
				//o treino eh o fold 0 do subfold
				instancesTrainPerFold.put(i,getClassIdAndQids(arrFoldTreino[i].getSubFolds()[0].getTreino(),metAp));
				instancesTestPerFold.put(i, getClassIdAndQids(fold.getTeste(),metAp));
				
				System.out.println("#Instances fold "+i+": "+instancesTrainPerFold.getList(i,true).size());
				System.out.println("#Instances fold "+i+": "+instancesTestPerFold.getList(i,true).size());
				if(fold.getValidation() != null)
				{
					instancesValidationPerFold.put(i, getClassIdAndQids(fold.getValidation(),metAp));
				}
				
				
				
			}
			for(int i = 0; i<arrFoldTreino.length ; i++)
			{
				Fold fold = arrFoldTreino[i];
				numSubFoldsPerFold[i] = fold.getSubFolds().length;
				for(int j = 0 ; j<fold.getSubFolds().length ; j++)
				{
					Fold subFold = fold.getSubFolds()[j];
					instancesTrainPerSubTrainFold.put(new Tupla<Integer,Integer>(i,j), getClassIdAndQids(subFold.getTreino(),metAp));
					instancesTestPerSubTrainFold.put(new Tupla<Integer,Integer>(i,j), getClassIdAndQids(subFold.getTeste(),metAp));
					
					
					System.out.println("#Instances fold "+i+" subfold  "+j+": "+instancesTrainPerSubTrainFold.getList(new Tupla<Integer,Integer>(i,j),true).size());
					System.out.println("#Instances fold "+i+" subfold "+j+": "+instancesTestPerSubTrainFold.getList(new Tupla<Integer,Integer>(i,j),true).size());
					
					if(subFold.getValidation() != null)
					{
						instancesValidationPerSubTrainFold.put(new Tupla<Integer,Integer>(i,j), getClassIdAndQids(subFold.getValidation(),metAp));	
					}
					
				}
			}
		}
		

		
		
		
		//extrai todas as features de testes de todos os folds e todas as features de treino dos folds especificos que necessitam disso
		//features do treino eh apenas a user_wo_vote.amostra
		
		//if(false)
		if(metAp.getNomExperimento().contains("user_wo_vote.amostra"))
		{
			for(int i = 0; i<arrFoldTreino.length ; i++)
			{
				Fold fold =arrFoldTreino[i];
				

				
				
				//coloca subFolds se existir
				for(int j = 0 ; j<fold.getSubFolds().length ; j++)
				{
					 
					Fold subFoldFinal = fold.getSubFolds()[j];
					
					
					//resgata features dinamicas
					subFoldFinal = transformDinamicFeatures(subFoldFinal, amostra, colecao);
					
					
					
					MatrizFeatures<Short> mFeats = criaMatrizFeature(metAp,	subFoldFinal);
					
					if(subFoldFinal.getTreino() != null)
					{
						
						adicionaMapInSpecific(i, j, mapFeaturesPorInstanciaTreino, subFoldFinal.getTreino(), metAp,mFeats);
					}
					if(subFoldFinal.getTeste() != null)
					{
						
						adicionaMapInSpecific(i, j, mapFeaturesPorInstanciaTreino, subFoldFinal.getTeste(), metAp,mFeats);
					}
					if(subFoldFinal.getValidation() != null)
					{
						adicionaMapInSpecific(i, j, mapFeaturesPorInstanciaTreino, subFoldFinal.getValidation(), metAp,mFeats);
					}
					Tripla<Integer,Integer,Integer> tplFold = new Tripla<Integer,Integer,Integer>(idxView,i,j);
					mapFeatsPerViewTreino.put(tplFold, mFeats);
				}
			}
			
			for(int i = 0; i<arrFoldTeste.length ; i++)
			{
				Fold fFinal = arrFoldTeste[i];
				
				//adiciona testTreinoIds, validacao ids e testIds em  - nao precisa pq ja  foi gravado assimmm
				/*Fold subFoldTreinoFinal = v.getResultTreino().getFolds()[i].getSubFolds()[0];
				fFinal.addIdsToLstSemClasse(subFoldTreinoFinal.getIdsFile());
				fFinal.addIdsToLstSemClasse(fFinal.getIdsFile());
				fFinal.addIdsToLstSemClasse(fFinal.getIdsValidation());
				*/
				
				//executa features dinamicas em fFinal
				fFinal = transformDinamicFeatures(fFinal, amostra, colecao);
				
				//armazena no array de features
				MatrizFeatures<Short> mFeats = criaMatrizFeature(metAp,	fFinal);
				
				adicionaMapInSpecific(i, null, mapFeaturesPorInstanciaTreino, fFinal.getTeste(), metAp,mFeats);
				if( fFinal.getValidation()  != null)
				{
					adicionaMapInSpecific(i, null, mapFeaturesPorInstanciaTreino, fFinal.getValidation(), metAp,mFeats);
				}
				addMatrizFeaturesPerTrain(idxView, i, mFeats);
				
			}
			
			
			
			
			//mapFeatsPerViewTreinoTmpAux.put(idxView, mapFeaturesPorInstanciaTreino);
			/*
			if(v.getResultValidacao()!= null)
			{
				for(int i = 0; i<v.getResultValidacao().getFolds().length ; i++)
				{
					Fold fFinal = transformDinamicFeatures(v.getResultValidacao().getFolds()[i], amostra, colecao);
					adicionaMapInSpecific(i, null, mapFeaturesPorInstanciaTreino, fFinal.getValidation(), metAp);
				}
			}
			*/
			
		}else
		{
			//extrai todas as features da origem
			MatrizFeatures<Short> mFeatsGeral = criaMatrizFeature(metAp,	arrFoldTeste);
			Set<Long> setIdsAmostra = new HashSet<Long>();
			for(Fold f : arrFoldTeste)
			{
				Map<Long ,Map<Long,String>> mapFeatsPerInstanceLong = metAp.mapeiaFeatureInstancias(f.getTeste());
				mapFeatsPerInstanceLong.putAll(metAp.mapeiaFeatureInstancias(f.getTreino()));
				if(f.getValidation() != null)
				{
					mapFeatsPerInstanceLong.putAll(metAp.mapeiaFeatureInstancias(f.getValidation()));
				}
				
				
				for(long id : mapFeatsPerInstanceLong.keySet())
				{
					if(!setIdsAmostra.contains(id))
					{
						HashMap<Short,String> mapFeat = new HashMap<Short,String>();
						for(long featIdx : mapFeatsPerInstanceLong.get(id).keySet())
						{
							mapFeat.put((short)featIdx,mapFeatsPerInstanceLong.get(id).get(featIdx));
						}
						mFeatsGeral.adicionaFeature((int)id, mapFeat);
						
						setIdsAmostra.add(id);
					}

					//mapFeaturesPorInstancia.put(id,mapFeat );
				}
				//Tupla<Integer,Integer> tplFold = new Tupla<Integer,Integer>(idxView,f.getNum());
				
			}
			mapFeatsPerView.put(idxView, mFeatsGeral);
		}
		
		return mapFeaturesPorInstancia;

	}
	public void addMatrizFeaturesPerTrain(int idxView, int fold_id,
			MatrizFeatures<Short> mFeats) {
		Tripla<Integer,Integer,Integer> tplFold = new Tripla<Integer,Integer,Integer>(idxView,fold_id,null);
		mapFeatsPerViewTreino.put(tplFold, mFeats);
	}
	public MatrizFeatures<Short> criaMatrizFeature(MetodoAprendizado metAp,
			Fold subFoldFinal) throws IOException {
		//verifica qntas instancias e features existem
		Tupla<Integer,Integer> t1 = getNumInstancesAndFeatures(metAp,subFoldFinal.getTreino());
		Tupla<Integer,Integer> t2 = getNumInstancesAndFeatures(metAp,subFoldFinal.getTeste());
		Tupla<Integer,Integer> t3 = getNumInstancesAndFeatures(metAp,subFoldFinal.getValidation());
		int numInstances = MathUtil.sum(t1.getX(),t2.getX(),t3.getX());
		int numFeatures = MathUtil.max(t1.getY(),t2.getY(),t3.getY());
		
		MatrizFeatures<Short> mFeats = new MatrizFeatures<Short>(numInstances, numFeatures);
		System.out.println("# de instancias:"+numInstances+"\t# de features:"+numFeatures);
		return mFeats;
	}
	public MatrizFeatures<Short> criaMatrizFeature(MetodoAprendizado metAp,
			Fold[] arrFolds) throws IOException {
		//verifica qntas instancias e features existem

		Tupla<Integer,Integer> t1 = FeatureSelectionHelper.getNumInstancesAndFeatures(metAp,arrFolds);

		
		MatrizFeatures<Short> mFeats = new MatrizFeatures<Short>(t1.getX(), t1.getY());
		System.out.println("# de instancias ao todo nos folds:"+t1.getX()+"\t# de features:"+t1.getY());
		return mFeats;
	}
	public Fold transformDinamicFeatures(Fold fold,String amostra,String colecao) throws IOException
	{
		File foldIdsTeste = fold.getIdsFile();
		File arqTreino = fold.getTreino();
		File arqValidacao = fold.getValidation();
		File arqTeste = fold.getTeste();
		File arqSemClasse = Fold.criaIdsFile(arqTeste.getParentFile(),new ArrayList<Integer>(fold.getIdsSemClasse()),".filterHiddenClasses");
		TempFiles.getTempFiles().addFile(arqSemClasse);
		
		Sys.executarComando("ipython /home/hasan/Dropbox/projetos_eclipse/forum_qa_python/src/run_dinamicFeatures.py "+arqSemClasse.getAbsolutePath()+" "
																													  +(arqTreino!=null?arqTreino.getAbsolutePath()+" ":"")
																													  +arqTeste.getAbsolutePath()+" "
																													  +(arqValidacao!=null?arqValidacao.getAbsolutePath()+" ":"")
																													  +" ^"+amostra+" ~"+colecao+" @allUserFeatures", true);
		
		Fold newF = new Fold(fold.getNum(),fold.getOrigem(),new File(arqTreino.getAbsolutePath()+".feats"),new File(arqTeste.getAbsolutePath()+".feats"),fold.getIdsFile(),fold.getIdsTreinoFile());
		if(arqValidacao != null)
		{
			newF.setValidationFiles(new File(arqValidacao.getAbsolutePath()+".feats"), fold.getIdsValidation());
		}
		newF.addIdsToLstSemClasse(fold.getIdsSemClasse());
		return newF;
	}
	
	public ResultadoViews getViewsResult(File arq)throws FileNotFoundException, IOException, ClassNotFoundException
	{
		ObjectInputStream arqInput = new ObjectInputStream(new FileInputStream(arq));
		ResultadoViews resultadoViews = (ResultadoViews) arqInput.readObject();
		arqInput.close();
	
		
		return resultadoViews;
	}
	
	/**
	 * Extrai features por visão
	 * @return
	 * @throws IOException 
	 */
	public void extraiFeaturesPorView(Fold[][] arrFoldTreino,Fold[][] arrFoldTeste,ResultadoViews rv) throws IOException
	{
		View[] arrViews = rv.getViews();
		for(int idxView = 0 ; idxView < rv.getViews().length ; idxView++) 
		{
			System.out.println("carregando features da visão: "+arrViews[idxView].getNomExperimento());
			getFeaturesPerView(arrFoldTreino[idxView],arrFoldTeste[idxView],arrViews[idxView].getMetodoAprendizado(),idxView+1);
			//mapFeatsPerView.put(idxView+1, getFeaturesPerView(arrViews[idxView],idxView+1));
		}
	}
	/**
	 * Extrai features por visão
	 * @return
	 * @throws IOException 
	 */
	public void extraiFeaturesPorView(Fold[][] arrFoldTreino,Fold[][] arrFoldTeste,MetodoAprendizado metAp) throws IOException
	{
		System.out.println("carregando features  ** Visao unica **");
		for(int idxView = 0 ; idxView < arrFoldTeste.length ; idxView++)
		{
			System.out.println("carregando features da visão: "+arrFoldTeste[idxView][0].getTeste().getName());
			getFeaturesPerView(arrFoldTreino[idxView], arrFoldTeste[idxView], metAp,idxView);
		}
		
	}
	public void indexaFeaturesGlobalmente() throws Exception
	{
		System.out.println("Indexando features");
		//view idx, idx_feat
		Set<Tupla<Short,Short>> indexedFeatures = new HashSet<Tupla<Short,Short>>();
		
		//reinicia o indexador
		mapIdxFeatGlobalToViewsFeatIdx = new HashMap<Short,Tupla<Short,Short>>();
		

		int lastIdx = 0;
		

		for(int idxView : mapFeatsPerView.keySet())
		{
			lastIdx = indexaFeatures(indexedFeatures, lastIdx, idxView,	mapFeatsPerView.get(idxView));	
		}
		
		
		//indexa todas as features
		for(Tripla<Integer, Integer, Integer> tplMap :  mapFeatsPerViewTreino.keySet() )
		{
			lastIdx = indexaFeatures(indexedFeatures, lastIdx, tplMap.getX(),	mapFeatsPerViewTreino.get(tplMap));
		}
		
		/*
		for(int idxView : mapFeatsPerViewTreinoTmpAux.keySet())
		{
			System.out.println("Indexando features visao especifica: "+idxView);

			//lastIdx = indexaFeatures(indexedFeatures, lastIdx, tplMap.getX(),	mapFeatsPerViewTreino.get(tplMap));
			
			
			for(Map<Short,String> mapFeat : mapFeatsPerViewTreinoTmpAux.get(idxView).values())
			{
				for(short featIdx : mapFeat.keySet())
				{
					lastIdx = indexaFeatIdx(indexedFeatures, lastIdx, idxView,featIdx);	
				}
					
			}
			
			
	
			
		}
		*/
		//mapFeatsPerViewTreinoTmpAux = null;
		System.gc();
		//for(int idxView : mapFeatsPerViewTreino.keySet())
		/*
		for(Tripla<Integer,Integer,Integer> tplMap : mapFeatsPerViewTreino.keySet())
		{
			System.out.println("Indexando features (que possui treino diferenciado) : "+tplMap.getX());
			lastIdx = indexaFeatures(indexedFeatures, lastIdx, tplMap.getX(),	mapFeatsPerViewTreino.get(tplMap));
			//lastIdx = indexaFeatures(indexedFeatures, lastIdx, idxView,	mapFeatsPerViewTreino.get(idxView).values());
			
		}
		*/
	}

	
	
	private int indexaFeatures(Set<Tupla<Short,Short>> indexedFeatures,
			int lastIdx, int idxView, MatrizFeatures<Short> matFeat) {
		
		//for(short featIdx = 1 ;  featIdx <= matFeat.getFeatureListOrderedyByFeatIdx(false).size() ; featIdx++)
		for(short featIdx :  matFeat.getFeatureListOrderedyByFeatIdx(false))
		{
			/*
			for(long featIdx : featVal.keySet())
			{*/
				//se nao tiver indexado, indexar 
				lastIdx = indexaFeatIdx(indexedFeatures, lastIdx, idxView,featIdx);
			//}
		}
		return lastIdx;
	}
	private int indexaFeatIdx(Set<Tupla<Short, Short>> indexedFeatures,
			int lastIdx, int idxView, short featIdx) {
		Tupla<Short,Short> idxFeatureView =new Tupla<Short,Short>((short)idxView,(short)featIdx); 
		if(!indexedFeatures.contains(idxFeatureView))
		{
			
			indexedFeatures.add(idxFeatureView);
			mapIdxFeatGlobalToViewsFeatIdx.put((short)lastIdx, idxFeatureView);
			lastIdx++;
		}
		return lastIdx;
	}
	
	

	


	@Override
	public String getFeatureVal(Integer foldId, Integer subFoldId,Long instanceId, Integer featGlobalIdx)throws Exception 
	{
		
		
		//resgata view_idx e local feat idx
		Tupla<Short,Short> featIdxLocal = mapIdxFeatGlobalToViewsFeatIdx.get(featGlobalIdx.shortValue());
		if(featIdxLocal == null)
		{
			System.out.println("Got a wrong feature numeber: "+featGlobalIdx+"\n map featIdx: "+mapIdxFeatGlobalToViewsFeatIdx);
			
		}
		int viewIdx = featIdxLocal.getX();
		short featLocalIdx = featIdxLocal.getY().shortValue();
		
		
		
		
		return getFeatureVal(foldId, subFoldId, instanceId, viewIdx,
				featLocalIdx);

		
	}
	
	@Override
	public String getFeatureVal(Integer foldId, Integer subFoldId,
			Long instanceId, int viewIdx, short featLocalIdx) throws Exception {
		//procura esta tupla no hashmap geral
		//Tupla<Integer,Integer> tplFeatIdx = new Tupla<Integer,Integer>(viewIdx,foldId);
		MatrizFeatures<Short> matrizGeral = mapFeatsPerView.get(viewIdx);
		if(matrizGeral != null)
		{
			if(matrizGeral.hasInstance(instanceId.intValue()))
			{
				Float fVal = matrizGeral.getFeature(instanceId.intValue(), featLocalIdx);
				return fVal != null? Float.toString(fVal): "";
			}
		}
		
		//nao encontrou, procura no especifico
		Tripla<Integer,Integer,Integer> trlFeatIdx = new Tripla<Integer,Integer,Integer>(viewIdx,foldId,subFoldId);
		MatrizFeatures<Short> matrizEsp = mapFeatsPerViewTreino.get(trlFeatIdx);
		if(matrizEsp != null)
		{
			//System.out.println("Possui visao (instancia "+instanceId+")");
			if(matrizEsp.hasInstance(instanceId.intValue()))
			{
				//System.out.println("Possui (instancia "+instanceId+")");
				//System.out.println("Feature:"+featLocalIdx);
				Float fVal = matrizEsp.getFeature(instanceId.intValue(), featLocalIdx);
				return fVal != null? Float.toString(fVal) : "";
			}
		}
		throw new Exception("A visao: "+viewIdx+" não possui a instancia "+instanceId+" (fold_id: "+foldId+" subFoldId: "+subFoldId);
	}
	
	public List<Tripla<Float, Integer, Integer>> getListPerFold(	Integer foldId, Integer subFoldId,
																		ListaAssociativa<Integer, Tripla<Float,Integer,Integer>> lstFold,
																		ListaAssociativa<Tupla<Integer,Integer>, Tripla<Float,Integer,Integer>> lstSubFold
																		)
    {
		if(subFoldId == null)
		{
			return lstFold.getList(foldId);
		}else
		{
			return lstSubFold.getList(new Tupla<Integer,Integer>(foldId,subFoldId));
		}
	}
	public int getNumFoldsTrain()
	{
		return this.instancesTrainPerFold.keySet().size();
	}
	public int getNumSubfoldsPerFoldTrain(int foldid)
	{
		int numSubFolds = 0;
		for(Tupla<Integer,Integer> tplFold : this.instancesTestPerSubTrainFold.keySet())
		{
			if(tplFold.getX() == foldid)
			{
				numSubFolds++;
			}
		}
		return numSubFolds;
	}
	
	@Override
	public List<Tripla<Float, Integer, Integer>> getClassIdAndQidTrain(	Integer foldId, Integer subFoldId) {
		// TODO Auto-generated method stub
		return getListPerFold(foldId, subFoldId, this.instancesTrainPerFold, this.instancesTrainPerSubTrainFold);
	}
	
	@Override
	public List<Tripla<Float, Integer, Integer>> getClassIdAndQidTest(Integer foldId, Integer subFoldId) {
		// TODO Auto-generated method stub
		return getListPerFold(foldId, subFoldId, this.instancesTestPerFold, this.instancesTestPerSubTrainFold);
	}
	
	@Override
	public List<Tripla<Float, Integer, Integer>> getClassIdAndQidValidation(
			Integer foldId, Integer subFoldId) {
		// TODO Auto-generated method stub
		//as instancias de validacao estao sempre no fold principal
		return getListPerFold(foldId, null, this.instancesValidationPerFold, this.instancesValidationPerSubTrainFold);
	}
	public Double getMinClasse()
	{
		return this.minClasse;
	}
	 public static double getResultadoConfigView(File arqIndexadorFeatHelper,Integer[] arrFiltro,FitnessCalculator fc,boolean featSet,boolean justBase) throws Exception{
		 return getResultadoConfigView( arqIndexadorFeatHelper, arrFiltro, fc, featSet, justBase, new File("/home/profhasan/wikiTest.out"), ML_MODE.CLASSIFICATION);
	 }
	 
	 public static void writeResult(GenericoSVMLike mAp,Fold fResult, File arqOrder,File arquivoOutput) throws IOException{
		 //busca a ordem do arquivo
		 List<Long> lstIdsOrder = new ArrayList<Long>();
		 BufferedReader in = new BufferedReader(new FileReader(arqOrder));
		 String line;
		 StringBuilder texto = new StringBuilder();
		 while ((line = in.readLine()) != null)
			{
				Integer id = mAp.getIdPorLinhaArquivo(line);
				//if id==null, put in sortedOrder
				if(id==null){
					lstIdsOrder = new ArrayList<Long>(fResult.getIdsResultado());
					lstIdsOrder.sort(new Comparator<Long>() {
						@Override
						public int compare(Long o1, Long o2) {
							// TODO Auto-generated method stub
							return o1>o2? 1 : (o1<o2?-1:0);
						}
					});
					break;
				}
				lstIdsOrder.add((long)id);
			}
			in.close();
			
		//com a lista de ids, cria o novo arquivo
		BufferedWriter out = new BufferedWriter(new FileWriter(arquivoOutput, false));
		for(Long idList : lstIdsOrder){
			out.write(fResult.getResultadoPorId(idList).getClassePrevista()+"\n");	
		}
		out.close();
	 }
	 public static double getResultadoConfigView(File arqIndexadorFeatHelper,Integer[] arrFiltro,FitnessCalculator fc,boolean featSet,boolean justBase,File outputDir, ML_MODE mlMode) throws Exception{
		 return getResultadoConfigView(arqIndexadorFeatHelper, arrFiltro, fc, featSet, justBase, outputDir,  mlMode, null,null,null,null,null,null);
	 }
	 public static double getResultadoConfigView(File arqIndexadorFeatHelper,Integer[] arrFiltro,FitnessCalculator fc,boolean featSet,boolean justBase,File outputDir, ML_MODE mlMode,
			 File arqTest) throws Exception {
		 return getResultadoConfigView(arqIndexadorFeatHelper, arrFiltro, fc, featSet, justBase, outputDir,  mlMode, arqTest,null,null,null,null,null);
	 }
	 public static double getResultadoConfigView(File arqIndexadorFeatHelper,Integer[] arrFiltro,FitnessCalculator fc,boolean featSet,boolean justBase,File outputDir, ML_MODE mlMode,
			 File arqTest,Map<String,String> mapTrainParamMultiview, Map<String,String> mapTestParamMultiview,
				Map<Integer,Map<String,String>> mapTrainParamPerView, Map<Integer,Map<String,String>> mapTestParamPerView,
				Map<Integer,String> methodPerView) throws Exception
	 {
		 long time = System.currentTimeMillis();
		 FeatureSelectionHelper fr = (FeatureSelectionHelper)ArquivoUtil.leObject(arqIndexadorFeatHelper);
		 
		 //fr.printFeaturesVector();
		 //System.exit(0);
		 //fr.setMethodName("SVMRank");
		 //FeatureSelectionHelper fr = (FeatureSelectionHelper)ArquivoUtil.leObjectWithKryo(arqIndexadorFeatHelper,FeatureSelectionHelper.class);
		 System.out.println("Tempo de leitura do objeto: "+(System.currentTimeMillis()-time)/1000.0);
		 fr.setMetaLearningWithFeatSet(featSet);
		 fr.setRunJustBase(justBase);
		 
		 Resultado r = mapTrainParamMultiview==null?fr.retornaFoldsFiltrado(arrFiltro):fr.retornaFoldsFiltrado(arrFiltro,mapTrainParamMultiview,mapTestParamMultiview,
				 																													mapTrainParamPerView,mapTestParamPerView,
				 																													methodPerView);
		 Resultado[] resultTesteView =  new Resultado[r.getViews().length];
		for(int i = 0; i<r.getViews().length ; i++){
			resultTesteView[i] = r.getViews()[i].getResultTeste();
		}
		if(!outputDir.exists()) {
			outputDir.mkdirs();
		}
		 //verifica a quantidade de classes do resultado
		 int maxClass = 0;
		 File[] finalResulPath = new File[r.getFolds().length];
		 for(int i =0 ; i<r.getFolds().length; i++){
			 Fold f = r.getFolds()[i];
			 for(ResultadoItem ri : f.getResultadosValues()){
				 if(maxClass > ri.getClasseReal()){
					 maxClass = (int) ri.getClasseReal();
				 }
			 }
			 //move o predict do fold para a pasta de resultado
			 finalResulPath[i] = new File(outputDir,"final_result.predict");
			 File arqOrder = arqTest!=null?arqTest:f.getTeste();
			 writeResult((GenericoSVMLike)fr.getMetodoCombinacao(), f, arqOrder, finalResulPath[i]);
			 //ArquivoUtil.copyfile(f.getPredict(), finalResulPath[i]); 
		 }
		 
		 switch(mlMode) {
			 case CLASSIFICATION:
			 case REGRESSION:

				 System.out.println("========================= Per view Result ========================");
				 for(int i = 0 ; i<resultTesteView.length ; i++){
					 System.out.println("......................................................................");
					 System.out.println("VIEW: "+r.getViews()[i].toString());
					 switch(mlMode) {
					 	case CLASSIFICATION:
					 		//System.out.println("CLASSIFICCACAO");
					 		System.out.println(CalculaResultados.resultadoClassificacaoToString(resultTesteView[i], maxClass, new File(outputDir,"result_"+r.getViews()[i].toString()+".txt")));
					 		break;
					 	case REGRESSION:
					 		 //System.out.println("REGRESSAO");
							 System.out.println(CalculaResultados.resultadoRegressaoToString(resultTesteView[i], new File(outputDir,"result_"+r.getViews()[i].getSglView()+".txt")));
					 		break;
					 }
					 //System.out.println(CalculaResultados.resultadoClassificacaoToString(resultTesteView[i], maxClass, new File(outputDir,"result_"+r.getViews()[i].toString()+".txt")));
					 write_view_output(i,outputDir, arqTest, fr, r, resultTesteView);
				 }
				 
				 System.out.println("====================== Final Result ==============================");
				 switch(mlMode) {
				 	case CLASSIFICATION:
				 		System.out.println(CalculaResultados.resultadoClassificacaoToString(r, maxClass, new File(outputDir,"final_result.txt")));
				 		break;
				 	case REGRESSION:
						 System.out.println(CalculaResultados.resultadoRegressaoToString(r, new File(outputDir,"final_result.txt")));
				 		break;
				 }
				 
				 break;
			 case L2R:
				 System.out.println("========================= Per view Result ========================");
				 for(int i = 0 ; i<resultTesteView.length ; i++){
					 System.out.println("......................................................................");
					 System.out.println("VIEW: "+r.getViews()[i].toString());
					 FitnessCalculator.getResultado(fr.getMetodoCombinacao(), resultTesteView[i],0.0,null);
					 
					 //System.out.println(CalculaResultados.resultadoClassificacaoToString(resultTesteView[i], maxClass, new File(outputDir,"result_"+r.getViews()[i].toString()+".txt")));
					 //grava predict de cada visao
					 write_view_output(i,outputDir, arqTest, fr, r, resultTesteView);
					 
				 }
				 System.out.println("================ Final Result ==========================");
				 FitnessCalculator.getResultado(fr.getMetodoCombinacao(), r,0.0,null);

		 }
		 
		 for(int i =0; i<finalResulPath.length; i++) {
			 
			 System.out.println("Final predicts output file:"+finalResulPath[i].getAbsolutePath() );
		 }		
		return 0;
		 
		 
	 }
	private static void write_view_output(int i,File outputDir, File arqTest, FeatureSelectionHelper fr, Resultado r,
			Resultado[] resultTesteView) throws IOException {
		
			 for(int j = 0; j<resultTesteView[i].getFolds().length ; j++) {
				 Fold f = resultTesteView[i].getFolds()[j];
				 File fViewResult = new File(outputDir,"result_"+r.getViews()[i].toString()+".predict");
				 
				 //grava reusltado
				 File arqOrder = arqTest!=null?arqTest:f.getTeste();	 
				 writeResult((GenericoSVMLike)fr.getMetodoViews(), f, arqOrder, fViewResult);
				 
				 //ArquivoUtil.copyfile(f.getPredict(), fViewResult);  
				 System.out.println(" predicts output file:"+fViewResult.getAbsolutePath() );
			 }
			 
		 
	}
	 public static MetodoAprendizado getMetodoApredizadoL2R(String nomMetodo,Map<String,String> mapParam) throws Exception
	 {
		mapParam.put("RANKER", "8");
		
		if(!mapParam.containsKey("BAG"))
		{
			mapParam.put("BAG", "30");
		}
		System.out.println("Nome do metodo: "+nomMetodo);
		GenericoLetorLike metLetor = new GenericoLetorLike(nomMetodo != null && nomMetodo.length()>0?nomMetodo:"RankLib",mapParam,mapParam);
		
		metLetor.setGravarNoBanco(false);
		return metLetor;
	 }
	 public static MetodoAprendizado getMetodoAprendizadoRegressao(String nomMetodo,Map<String,String> mapParam) throws Exception
	 {
		 
			
			mapParam.put("IS_CLASSIFICACAO", "3");
			
			GenericoSVMLike metRegressao = new GenericoSVMLike(nomMetodo != null && nomMetodo.length()>0?nomMetodo:"SVM",mapParam,mapParam);
			metRegressao.setMode(SVM.MODE_REGRESSION);
			metRegressao.setGravarNoBanco(false);
		return metRegressao;
	 }
	 
		public static void carregaTodasColecoesL2R() throws Exception
		{
			String dirProj = "/home/hasan/data/projetos_eclipse/Util/objResultadoViews/";
			String dirHome = "/home/hasan/objResultadoViews/";
			String[] colsObjs = {
									//dirProj+"toy_rank_test.obj",
									//dirHome+"testNoDinamic.obj"
									//dirHome+"cook_multiview.obj",
									//dirHome+"english_multiview.obj",								
									dirHome+"stack_multiview.obj",
								};
			long time = System.currentTimeMillis();
			for(String colObjFile : colsObjs)
			{
				System.out.println("=====================+++ Colecao: "+colObjFile+"=================================");
				Map<String,String> mapParam = new HashMap<String,String>();
				MetodoAprendizado metLetor = getMetodoApredizadoL2R("",mapParam);
				
				FeatureSelectionHelper fs = new FeatureSelectionHelper(metLetor,metLetor,new File(colObjFile));
				
				fs.carregaFeatureSelection();
			}
			
			System.out.println("\nTime:"+((System.currentTimeMillis()-time)/1000.0)+" segs");
		} 
		public static void carregaTodasColecoesRegressao() throws Exception
		{
			System.out.println("Criando starvote.... ");
			String dirProj = "/home/hasan/data/projetos_eclipse/Util/objResultadoViews/";
			String dirHome = "/home/hasan/objResultadoViews/";
			String[] colsObjs = {
									//dirHome+"toy_regression_multiview.obj",
									//dirHome+"muppets_multiview.obj",
									//dirHome+"wiki6_multiview.obj",
									dirHome+"starVote_multiview.obj",
									//dirHome+"starAmostra_multiview.obj",
			};
			
			for(String colObjFile : colsObjs)
			{
				System.out.println("Carregando colecao wiki: "+colObjFile);
				Map<String,String> mapParam = new HashMap<String,String>();
				MetodoAprendizado metRegressao = getMetodoAprendizadoRegressao("",mapParam);
				FeatureSelectionHelper fs = new FeatureSelectionHelper(metRegressao,metRegressao,new File(colObjFile));
				
				fs.carregaFeatureSelection();
			}
		
		}
	public Map<Integer, MatrizFeatures<Short>> getMapFeatsPerView()
	{
		return this.mapFeatsPerView;
	}
	
	public Map<Tripla<Integer, Integer, Integer>, MatrizFeatures<Short>> getMapFeatsPerViewTrain()
	{
		return this.mapFeatsPerViewTreino;
	}
	private void addViewsFrom(FeatureSelectionHelper objFSH)
	{
		Map<Integer,Integer> originalViewIdToAdded = new HashMap<Integer,Integer>();
		
		Map<Integer,MatrizFeatures<Short>> mapViewFeat = objFSH.getMapFeatsPerView();
		
		for(int idxView : mapViewFeat.keySet())
		{
			//adiciona proxima visao com o proximo id
			int nextViewId = mapFeatsPerView.size()+1;
			
			//adiciona no mapeamento
			originalViewIdToAdded.put(idxView, nextViewId);
			
			//adiciona o map com no next view id
			this.mapFeatsPerView.put(nextViewId, mapViewFeat.get(idxView));
			
		}
		
		//adiciona os especificos de treino
		Map<Tripla<Integer, Integer, Integer>, MatrizFeatures<Short>> mapViewPerTrain = objFSH.getMapFeatsPerViewTrain();
		int viewIdx = 0; 
		Tripla<Integer,Integer,Integer> trpNewView = null;
		for(Tripla<Integer, Integer, Integer> trpView : mapViewPerTrain.keySet())
		{
			//cria id mapeado (usando o view id certo)
			viewIdx = originalViewIdToAdded.get(trpView.getX());
			trpNewView = new Tripla<Integer,Integer,Integer>(viewIdx,trpView.getY(),trpView.getZ());
			
			//adiciona a view especifica
			this.mapFeatsPerViewTreino.put(trpNewView, mapViewPerTrain.get(trpView));
		}
		
		
		
		
	}
	public void addViewsFrom(FeatureSelectionHelper[] arrFSH) throws Exception
	{
		for(FeatureSelectionHelper objFSH : arrFSH )
		{
			this.addViewsFrom(objFSH);
		}
		this.indexaFeaturesGlobalmente();
	}
	
	public static void main(String[] args) throws Exception
	{

		//System.out.println("Rodando ranklib cook - ORDENANDO SUBTREINO"  );
		
		/*
		Random r = new Random(13);
		for(int i =0 ; i <10 ; i++)
		{
			System.out.println(r.nextFloat());
		}
		System.exit(0);
		*/
		
		/*
		carregaTodasColecoesRegressao();
		System.exit(0);
		*/
		
		
		/*
		carregaTodasColecoesL2R(); 
		System.exit(0);
		
		*/
		
		
		/*
		ArquivoUtil.leObjectWithKryo (new File("/home/hasan/objResultadoViews/feat_study_english_multiview.obj"), 
									FeatureSelectionHelper.class);
		System.exit(0);
		*/
		//Integer[] arrFeatures = {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,3,3,3,3,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,5,5,5,5,5,5,5,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,7,7,7,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8,8};
		
		/*
		if(args.length<1)
		{
			System.err.println("Entrar com <nome do arqivo de features> [feat array separado por espca]");
			System.exit(0);
		}
		*/ 
		//args = qaTests(args,"testNoDinamic",2,5);
		
		//sargs = wikiTests(args,"wiki6",4,0);
		//args = qaTests(args,"cook",0,0);
		//args = qaTests(args,"toy_rank",0,0);
		
		//args = wikiTests(args,"toy_regression",0,0);
		
		
		/*
		Integer[] arrFeatures = new Integer[args.length-1];
		for(int i =0 ; i<arrFeatures.length ; i++)
		{
			arrFeatures[i] = Integer.parseInt(args[i+1]);
		}
		System.out.print("Array de features: [");
		for(int i =0 ; i<arrFeatures.length ; i++)
		{
			System.out.print(arrFeatures[i]+",");
		}
		System.out.println("]\n\n");
		*/
		boolean justBase = true;
		boolean withFeatSet = false;
		int numFeatures = 68;
		Integer[] arrFeatures = new Integer[68];
		for(int i =0 ; i<numFeatures ; i++){
			arrFeatures[i] = 1;
		}
		File arq = new File("/home/profhasan/68Feat.obj");
		//Integer[] arrFeatures = {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,2,2,2,2,2,2,3,3,3,3};
		
		long time = System.currentTimeMillis();
		//System.out.println("Resultado: "+FeatureSelectionHelper.getResultadoConfigView(new File("/home/hasan/objResultadoViews/feat_study_cook_multiview.obj"),arrFeatures,new FitnessCalculator()));
		System.out.println("Resultado: "+FeatureSelectionHelper.getResultadoConfigView(arq,arrFeatures,new FitnessCalculator(),withFeatSet,justBase));
		System.out.println("Tempo gasto: "+(System.currentTimeMillis()-time)/1000.0+" segundos");
		
		if(FeatureSelectionHelper.DELETE_FILES_ON_EXIT)
		{
			TempFiles.getTempFiles().deleteFiles();
		}
		
		
		//carregaTodasColecoesL2R();
	}
	
	private static void printStatisticObj(File arqObj) throws FileNotFoundException, IOException, ClassNotFoundException
	{
		FeatureSelectionHelper fr = (FeatureSelectionHelper)ArquivoUtil.leObject(arqObj);
		
		List<Tripla<Float, Integer, Integer>>  lstClIdQidTreino = fr.getClassIdAndQidTrain(0, null);
		List<Tripla<Float, Integer, Integer>>  lstClIdQidTest = fr.getClassIdAndQidTest(0, null);
		List<Tripla<Float, Integer, Integer>>  lstClIdQidValidation = fr.getClassIdAndQidValidation(0, null);
		
		System.out.println("Treino size: "+lstClIdQidTreino.size());
		System.out.println("Teste size: "+lstClIdQidTest.size());
		System.out.println("Validacao size: "+lstClIdQidValidation.size());
	}
	private static String[] qaTests(String[] args,String colecao,int numFold,int holdOutNum) 
	{
		
		String endObj = "/home/hasan/objResultadoViews/"+colecao+"/fold_"+numFold+"/feat_study_holdout_"+holdOutNum+".obj";
		//String endObj = "/home/hasan/objResultadoViews/"+colecao+"/fold_"+numFold+"/feat_study_fold_ga.obj";
		
		//String endObj = "/home/hasan/objResultadoViews/feat_study_"+colecao+"_multiview.obj";
		
		//soh usuario
		//String[] argsNew = ("/home/hasan/objResultadoViews/feat_study_"+colecao+"_multiview.obj 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8").split(" ");
		
		if(colecao.equals("toy"))
		{
			//return (endObj+" 1 1 1 1 2 2 2 2 3 3 3 3").split(" ");
			return (endObj+" 1 1 1 1 2 2").split(" ");
		}else
		{
			//teste
			//String[] argsNew = (endObj+" 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 8 0 0 8 0 0 0 0 0 0 0 0").split(" ");
			///String[] argsNew = (endObj+" 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8").split(" ");
			//String[] argsNew = (endObj+" 1 1 1 1 1 1").split(" ");
			//usuario (sem features dinamicas)
			//String[] argsNew = (endObj+" 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8").split(" ");
			
			//usuario		
			String[] argsNew = (endObj+" 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8").split(" ");
			
			//String[] argsNew = (endObj+" 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 7 7 7 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8").split(" ");
			//multiview
			//String[] argsNew = (endObj+" 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 2 3 3 3 3 4 4 4 4 4 4 4 4 4 4 4 4 4 4 4 4 4 4 5 5 5 5 5 5 5 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 7 7 7 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8 8").split(" ");
			//String[] argsNew=(endObj+" 0 0 1 0 1 2 2").split(" ");
			 
			//String[] argsNew = (endObj+" 0 0 1 0 0 0 1 0 1 0 0 0 1 0 0 0 0 1 0 0 0 1 0 1 1 1 0 0 0 0 1 0 0 0 0 0 0 0 1 0 2 0 0 2 0 2 2 2 0 0 2 0 0 0 0 0 0 0 0 0 2 0 0 0 0 0 0 0 0 0 0 0 3 0 0 0 0 0 0 4 0 0 4 0 4 0 4 0 4 0 0 0 0 0 5 0 0 0 5 0 0 6 0 6 0 0 0 0 6 6 0 6 6 0 0 6 6 0 0 0 0 6 6 0 7 7 0 0 0 0 8 0 8 0 8 0 0 0 0 0 0 0 0 8 0 0 0 0 0 0 8 8 0 0 8 8 0 0 0 0 8 0 8 0 0 0 0 0 0 8 0 0 8 0 0 8 0 8 0 0 0 0 8").split(" ");
			//String[] argsNew = (endObj+" 1 1 1 ").split(" ");
			 
			//best idv
			//String[] argsNew = (endObj+" 0 0 0 1 1 1 0 1 0 0 0 0 1 0 0 0 1 1 0 0 0 1 0 1 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 2 0 0 0 0 2 0 2 0 0 0 2 2 0 0 2 2 0 2 2 0 0 0 3 0 0 0 4 0 0 4 0 0 0 0 0 0 4 4 0 0 0 4 4 4 0 0 0 0 0 0 0 6 6 0 6 6 0 0 0 0 0 0 6 0 0 0 0 0 6 0 0 0 6 0 6 6 0 0 0 0 8 8 0 0 0 0 0 0 8 0 8 0 0 8 0 0 0 0 0 8 0 8 0 0 8 0 0 0 0 0 8 0 8 0 0 8 8 8 0 0 8 0 0 8 0 0 8 0 0 8 8 8 8 0 0 0 8 0").split(" ");
			//idv lerdasso
			//String[] argsNew = (endObj+" 1 0 0 0 0 1 1 1 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 2 0 2 0 0 2 2 0 0 0 0 2 2 0 0 0 0 0 0 0 0 0 2 0 0 0 0 3 0 0 0 0 4 0 0 0 0 4 0 4 0 0 4 0 0 4 0 5 0 0 0 0 0 0 6 0 0 0 6 6 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 6 0 0 6 0 7 0 8 0 0 0 0 0 8 0 0 8 0 8 0 0 0 8 8 0 0 0 0 0 0 8 0 8 0 0 0 0 0 0 0 0 8 0 0 8 8 0 0 0 8 0 8 8 0 0 0 8 0 0 8 8 0 0 0 0 8").split(" ");
			
			//testes
			//String[] argsNew = (endObj+" 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0").split(" ");
			//baseline
			//String[] argsNew = (endObj+" 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1").split(" ");
			
	
			//simples 
			//String[] argsNew = (endObj+" 1 1 1 1").split(" ");
			
			//melhor resultado do english		
			//String[] argsNew = endObj+" 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0 2 0 0 0 0 0 0 0 0 0 0 0 2 2 2 2 2 2 0 0 0 0 0 0 0 0 0 0 0 0 0 3 0 0 0 0 0 0 0 0 0 4 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 6 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 6 0 0 0 0 0 8 0 0 0 8 8 0 0 0 0 8 0 0 0 0 0 0 0 0 0 0 8 0 0 0 8 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 8 0".split(" ");
			
			//english com o melhor do cook
			//String[] argsNew = endObj+" 0 0 0 0 0 1 0 0 0 1 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 2 0 0 0 0 2 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 5 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 8 0 0 0 0 8 8 0 0 0 8 0 0 0 0 0 0 8 0 0 0 0 8 0 0 0 0 0 0 0 0 0 8 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 8 0 0 0 0 0".split(" ");
			
			
			//melhor resultado do cook
			//String[] argsNew = endObj+" 0 0 0 0 0 1 0 0 0 1 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 2 0 0 0 0 2 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 5 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 8 0 0 0 0 8 8 0 0 0 8 0 0 0 0 0 0 8 0 0 0 0 8 0 0 0 0 0 0 0 0 0 8 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 8 0 0 0 0 0".split(" ");
			
			
			
	
			//melhor baseline (cook)
			//String[] argsNew = endObj+" 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 1 0 0 0 0 0 1 0 0 0 0 0 0 0 1 0 0 0 1 1 1 0 1 1 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 1 1 0 0 0 1 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0".split(" ");
			
			//String[] argsNew = endObj+" 1 1 1 1 0 0 0 0 0 8 0 0 0 0 8 0 0 0 0 0 0 0 0 0 8 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 8 0 0 0 0 0".split(" ");
			
			//args = argsNew;
			
			return argsNew;
		}
	}
	
	
	
	private static String[] wikiTests(String[] args,String colecao,int numFold,int holdOutNum)
	{
		//String endObj = "/home/hasan/objResultadoViews/"+colecao+"/fold_"+numFold+"/feat_study_holdout_"+holdOutNum+".obj";
		//String endObj = "/home/hasan/objResultadoViews/"+colecao+"/fold_"+numFold+"/feat_study_fold_ga.obj";
		String endObj = "/home/hasan/objResultadoViews/feat_study_"+colecao+"_multiview.obj";
		if(colecao.startsWith("toy"))
		{
			String[] argsNew = (endObj+" 1 1 1 1 2 2 2").split(" ");
			return argsNew;			
		}else
		{
			//multiview
			//String[] argsNew = (endObj+" 1 1 1 1 1 1 1 1 1 1 1 1 1 1 2 2 2 2 2 2 2 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 3 4 4 4 5 5 5 5 5 5 5 5 5 5 5 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6 6").split(" ");
			String[] argsNew = (endObj+" 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1").split(" ");
			//14, 7, 16, 3, 11 (grafo), 17 (historico?)
			return argsNew;
		}
		
		
	}
	
	public static void testePerFold(File[] arrObjFiles,String[] arrRepresentations) 
	{
		
	}
	public void setMetodoAprendizadoViews(MetodoAprendizado metApViews2) {
		this.metApViews = metApViews2;
		
	}
	public void setMetodoAprendizadoViews(MetodoAprendizado[] metApViews2) {
		this.arrMetApViews = metApViews2;
		
	}
	public void setMetodoAprendizadoCombinacao(MetodoAprendizado metCombinacao)
	{
		this.metApCombinacao = metCombinacao;
	}
	public int getTotalFeats() {
		// TODO Auto-generated method stub
		return this.mapIdxFeatGlobalToViewsFeatIdx.keySet().size();
	}

	

}




