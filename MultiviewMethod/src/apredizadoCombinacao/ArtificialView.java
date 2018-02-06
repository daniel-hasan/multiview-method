package apredizadoCombinacao;

import io.Sys;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import string.StringUtil;
import stuctUtil.Tripla;
import stuctUtil.Tupla;
import utilAprendizado.params.ParamUtilSVMFilter;
import aprendizadoResultado.ValorResultado;
import aprendizadoUtils.FitnessCalculator;
import aprendizadoUtils.GenericoLetorLike;
import aprendizadoUtils.GenericoSVMLike;
import aprendizadoUtils.MetodoAprendizado;
import config_tmp.ConfigViewColecao;
import entidadesAprendizado.Fold;
import entidadesAprendizado.Resultado;
import entidadesAprendizado.ResultadoItem;
import entidadesAprendizado.View;
import featSelector.ViewCreatorHelper;

public class ArtificialView extends View {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5106684686938097851L;
	//private static final long serialVersionUID = 9011104286793362375L;//Antigo
	 
	private Resultado artResultValidacao = null;
	private Resultado artResultTeste = null;
	private Resultado artResultTreino = null;
	private List<Integer> lstIdxFeatures = new ArrayList<Integer>();
	private Map<Integer,Integer> mapGlobalToLocalFeature = new HashMap<Integer,Integer>();
	private boolean COMPUTE_VIEW = true;
	
	private int lastLocalFeatIdx = 0;
	private Fold[] arrFolds;
	private Fold[] arrFoldsValidacao;
	private Fold[] arrFoldsTreino;
	private File dir = null;
	private int idxView = 0;
	private Double minClasse = null;
	private String nomArquivoView;
	
	public ArtificialView(String nomArquivoView,int idxView,File dir,ConfigViewColecao cnf,MetodoAprendizado metAp,List<Integer> lstIdxFeatures,FeatureType objType,Double minClasse)
	{
		super(cnf,null,metAp,objType);
		this.lstIdxFeatures = lstIdxFeatures;
		this.nomArquivoView = nomArquivoView;
		this.dir = dir;
		this.idxView = idxView;
		this.minClasse = minClasse;
		
		//mapea features globais x locais 
		for(Integer idxFeature : lstIdxFeatures) 
		{
			lastLocalFeatIdx++;
			mapGlobalToLocalFeature.put(idxFeature, lastLocalFeatIdx);
			
		}
		
	}

	
	
	public void criaResultadosView(ViewCreatorHelper viewCreatableHelper ,int numFolds, int[] numSubFoldPerView) throws Exception 
	{
		
		MetodoAprendizado metAp = this.getMetodoAprendizado();
		long time = System.currentTimeMillis();
		System.out.println("Criacao de view (antes) - Memoria usada:"+Sys.getUsedMemory()/(1024.0*1024.0)+" MB");
		this.arrFolds = createFoldsView(viewCreatableHelper, numFolds, numSubFoldPerView);
		System.out.println("Tempo criação dos folds: "+(System.currentTimeMillis()-time)/1000.0+" segundos");
		System.out.println("Criacao de view - Memoria usada:"+Sys.getUsedMemory()/(1024.0*1024.0)+" MB");
		System.gc();
		System.out.println("Criacao de view - Memoria usada: (depois gc)"+Sys.getUsedMemory()/(1024.0*1024.0)+" MB");
		this.arrFoldsValidacao = new Fold[this.arrFolds.length];
		this.arrFoldsTreino = new Fold[this.arrFolds.length];
		List<Tupla<Tupla<Map<String, String>, Map<String, String>>,ValorResultado>> lstParamOrederedPerBestResult = null;
		if(COMPUTE_VIEW)
		{
			ResultadoViewCache rCached = CacheResultadoView.getResultadoViewCached(nomArquivoView,this.lstIdxFeatures);
			if(rCached != null)
			{
				System.out.println("*************************** Usando Cache **************************** ArqView: "+nomArquivoView);
				this.arrFoldsTreino = rCached.getArrFoldTreino();
				this.arrFoldsValidacao = rCached.getArrFoldValidacao();
				this.arrFolds = rCached.getArrFoldTeste();
			}else
			{
				System.out.println("*************************** Calculando view **************************** ArqView: "+nomArquivoView);
				//computa parametros da visao se necessario
				if(metAp instanceof GenericoSVMLike)
				{
					if( /*(((GenericoSVMLike)metAp).getNomeMetodo().equalsIgnoreCase("SVM_LINEAR")) ||  
							(((GenericoSVMLike)metAp).getNomeMetodo().equalsIgnoreCase("SVM")) || 	
							((GenericoSVMLike)metAp).getNomeMetodo().equalsIgnoreCase("SVMRank") ||
							((GenericoSVMLike)metAp).getNomeMetodo().equalsIgnoreCase("SVMLibLinear")
							)*/false)
					{
						Set<Integer> qids = new HashSet<Integer>();
						List<Tripla<Float,Integer,Integer>> lst = viewCreatableHelper.getClassIdAndQidTrain(0, 0);
						if(lst == null)
						{
							lst = viewCreatableHelper.getClassIdAndQidTrain(0, null);
						}
						for(Tripla<Float,Integer,Integer> tpl : lst)
						{
							qids.add(tpl.getZ());
						}
						
						///1 Map<String, String>: parametros do treino
						///2 Map<String, String>: parametros do teste
						
						Tupla<GenericoSVMLike,List<Tupla<Tupla<Map<String, String>, Map<String, String>>,ValorResultado>>> metApAndResults =ParamUtilSVMFilter.computeViewParameterSVM(metAp instanceof GenericoSVMLike  && this.arrFolds[0].getSubFolds() != null && this.arrFolds[0].getSubFolds().length > 1 
																																																	? this.arrFolds[0].getSubFolds()
																																																	:null,
																																																this.arrFolds[0].getTreino(),
																																																(GenericoSVMLike) metAp,
																																																true,
																																																qids.size(),
																																																this.lstIdxFeatures.size());
						
						//Tupla<GenericoSVMLike,List<Tupla<Tupla<Map<String, String>, Map<String, String>>,ValorResultado>>> metApAndResults =ParamUtilSVMFilter.computeViewParameterSVM(null,this.arrFolds[0].getTreino(),(GenericoSVMLike) metAp,true,qids.size(),this.lstIdxFeatures.size());
						
						
						
						metAp = metApAndResults.getX();
						lstParamOrederedPerBestResult = metApAndResults.getY();
						
						//metAp = ParamUtilSVMFilter.getParameterDefaultWikipedia(this.idxView,(GenericoSVMLike) metAp);
						//metAp = ParamUtilSVMFilter.getParameterDefaultMuppets(this.idxView,(GenericoSVMLike) metAp);
						
					} 
				}
				
				
				metAp = calculaFolds(metAp, lstParamOrederedPerBestResult);
				
				
				//grava na cache
				CacheResultadoView.addResultadoView(this.nomArquivoView,lstIdxFeatures, new ResultadoViewCache(this.arrFoldsTreino, this.arrFoldsValidacao, this.arrFolds));
			}
			//define resultado de cada
			if(arrFoldsValidacao[0] != null)
			{
				artResultValidacao = new Resultado(metAp.getNomExperimento(),arrFoldsValidacao);
			}else
			{
				arrFoldsValidacao = null;
			}
			artResultTeste = new Resultado(metAp.getNomExperimento(),arrFolds);
			
			artResultTreino =new Resultado(metAp.getNomExperimento(),arrFoldsTreino);
			
			System.out.println("RESULTADO Visão:");
			System.out.println("view_"+this.idxView+"_ndcg@10:"+FitnessCalculator.getResultado(metAp,artResultTeste,this.minClasse,null));

			//System.out.println("view_"+this.idxView+"_fitness:"+FitnessCalculator.getResultado(metAp,artResultTeste)/(lstIdxFeatures.size()/186.0));
			
			//System.exit(0);
			View[] v = {this};
			
			artResultTeste.setView(v);
			artResultTreino.setView(v);
			if(artResultValidacao != null)
			{
				artResultValidacao.setView(v);
			}
		}
		

		
		
		
		
		
	}

	public static void changeSerialId(long id) throws IllegalArgumentException, IllegalAccessException, SecurityException, NoSuchFieldException
	{
		Field field = ArtificialView.class.getDeclaredField("serialVersionUID");
	    field.setAccessible(true);

	    // remove final modifier from field
	    Field modifiersField = Field.class.getDeclaredField("modifiers");
	    modifiersField.setAccessible(true);
	    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
	    System.out.println(field);
	    
	    
	    field.setLong(null, id);
	    
	    //recoloca como final
	    /*modifiersField.setAccessible(false);
	    modifiersField.setInt(field, field.getModifiers() & Modifier.FINAL);
	    
	    //seta como nao acessivel novamente
	    field.setAccessible(false);
	    */
	}

	public MetodoAprendizado calculaFolds(
			MetodoAprendizado metAp,
			List<Tupla<Tupla<Map<String, String>, Map<String, String>>, ValorResultado>> lstParamOrederedPerBestResult)
			throws Exception {
		/*
		boolean isSVMRank = lstParamOrederedPerBestResult != null && metAp instanceof GenericoSVMLike && ((GenericoSVMLike)metAp).getNomeMetodo().equalsIgnoreCase("SVMRank");
		if(isSVMRank)
		{
			((GenericoSVMLike)metAp).setTimeout(30000);
		}
		*/
		boolean isSVMRank = false;
		boolean endedWithTimeout = false;
		do
		{
			endedWithTimeout = false;	
			Exception exp = null;
			try {

				
				calculaFolds(metAp);
				
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				exp = e;
			}
			
			if(((GenericoSVMLike)metAp).endedWithTimeout() && lstParamOrederedPerBestResult != null)
			{
				endedWithTimeout = true;
				//o primeiro eh o atual, remove ele
				lstParamOrederedPerBestResult.remove(0);
				
				//tenta rodar com o melhor anterior
				Tupla<Map<String,String>,Map<String,String>> mapParamNew = lstParamOrederedPerBestResult.get(0).getX();
				metAp = new GenericoLetorLike(((GenericoSVMLike)metAp).getNomeMetodo(),mapParamNew.getX(),mapParamNew.getY());
				((GenericoSVMLike)metAp).setTimeout(30000);
				
				//limpa os resultados dos folds (se existiu)
				for(Fold f : arrFolds)
				{
					f.limpaResultados();
				}
			}else
			{
				if(exp != null)
				{
					throw exp;
				}
			}
		}
		while(isSVMRank &&  endedWithTimeout);
		return metAp;
	}



	public void calculaFolds(MetodoAprendizado metAp) throws Exception {
		long time;
		int foldIdx = 0;
		int subFoldIdx = 0;
		for(Fold f : arrFolds)
		{

			
			subFoldIdx = 0;
			//cria fold do treino
			Fold fTreino = new Fold(f.getNum(), metAp.getArquivoOrigem(),f.getTreino(), f.getTeste(), f.getIdsFile());
			fTreino.setValidationFiles(f.getValidation(), f.getIdsValidation());
			
			for(Fold subFold : f.getSubFolds())
			{
				
				//aplica teste no treino  e a validacao no treino (subfold 0) 
				if(subFoldIdx==0)
				{
					time = System.currentTimeMillis();
					//aplica no teste do fold 0
					f.setTreino(subFold.getTreino());
					fTreino.setTreino(subFold.getTreino());
					ArrayList<ResultadoItem> lstResults = metAp.testar(f);
					f.setResultados(lstResults);
					
					/*metAp = testaWithParams(metAp,
							lstParamOrederedPerBestResult,
							f).getX();*/
					System.out.println("Tempo criacao do metateste fold #"+foldIdx+": "+(System.currentTimeMillis()-time)/1000.0+" segundos");
					
					time = System.currentTimeMillis();
					//aplica na validacao do fold 0
					if(f.getValidation() != null)
					{
						Fold fValidacao = new Fold(f.getNum(), metAp.getArquivoOrigem(),subFold.getTreino(), f.getValidation(), f.getIdsValidation());
						//fValidacao.setPredict(new File(f.getValidation().getAbsolutePath()+"subTreino.predict"));
						/*metAp = testaWithParams(metAp,
								lstParamOrederedPerBestResult,
								fValidacao).getX();*/
						fValidacao.setResultados(metAp.testar(fValidacao));
						this.arrFoldsValidacao[f.getNum()] = fValidacao;
					}
					
					System.out.println("Tempo criacao do metavalidacao fold #"+foldIdx+": "+(System.currentTimeMillis()-time)/1000.0+" segundos");
					
					
				}
				
				time = System.currentTimeMillis();
				//aplica subtestes nos subtreinos
				ArrayList<ResultadoItem> lstResultsTrain = metAp.testar(subFold);
				subFold.setResultados(lstResultsTrain);
				/*Tupla<MetodoAprendizado,List<ResultadoItem>> tplMethodAndResult = testaWithParams(metAp,
																								lstParamOrederedPerBestResult,
																								subFold);*/
				//metAp = tplMethodAndResult.getX();
				//subFold.setResultados(lstResults);
				//adiciona subfold no fTreino
				fTreino.adicionaTodosResultados(lstResultsTrain);
				
				
				System.out.println("Tempo criacao do subTreino fold #"+foldIdx+" subFold #"+subFoldIdx+":  "+(System.currentTimeMillis()-time)/1000.0+" segundos");
				
				
				subFoldIdx++;
			}
			
			//caso nao existe subfold, aplica o teste apenas
			if(f.getSubFolds().length == 0)
			{
				time = System.currentTimeMillis();
				//aplica no teste do fold 0
				ArrayList<ResultadoItem> lstResults = metAp.testar(f);
				f.setResultados(lstResults);
				
				/*metAp = testaWithParams(metAp,
						lstParamOrederedPerBestResult,
						f).getX();*/
				System.out.println("Tempo criacao do metateste fold #"+foldIdx+": "+(System.currentTimeMillis()-time)/1000.0+" segundos");
			}
			fTreino.setSubFolds(f.getSubFolds());
			this.arrFoldsTreino[f.getNum()] = fTreino;
			
			foldIdx++;
		}
	}


/*
	public Tupla<MetodoAprendizado,List<ResultadoItem>> testaWithParams(
			MetodoAprendizado metAp,
			List<Tupla<Tupla<Map<String, String>, Map<String, String>>, ValorResultado>> lstParamOrederedPerBestResult,
			Fold objFold) throws Exception {
		ArrayList<ResultadoItem> lstResults = new ArrayList<ResultadoItem>();
		boolean isSVMRank = lstParamOrederedPerBestResult != null && metAp instanceof GenericoSVMLike && ((GenericoSVMLike)metAp).getNomeMetodo().equalsIgnoreCase("SVMRank");
		if(isSVMRank)
		{
			((GenericoSVMLike)metAp).setTimeout(30000);
		}
		boolean endedWithTimeout = false;
		do
		{
			endedWithTimeout = false;	
			Exception exp = null;
			try {
				lstResults = metAp.testar(objFold);
				objFold.setResultados(lstResults);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				exp = e;
			}
			
			if(((GenericoSVMLike)metAp).endedWithTimeout() && lstParamOrederedPerBestResult != null)
			{
				endedWithTimeout = true;
				//o primeiro eh o atual, remove ele
				lstParamOrederedPerBestResult.remove(0);
				
				//tenta rodar com o melhor anterior
				Tupla<Map<String,String>,Map<String,String>> mapParamNew = lstParamOrederedPerBestResult.get(0).getX();
				metAp = new GenericoLetorLike(((GenericoSVMLike)metAp).getNomeMetodo(),mapParamNew.getX(),mapParamNew.getY());
				((GenericoSVMLike)metAp).setTimeout(30000);
			}else
			{
				if(exp != null)
				{
					throw exp;
				}
			}
		}
		while(isSVMRank &&  endedWithTimeout);
		return new Tupla<MetodoAprendizado,List<ResultadoItem>>(metAp,lstResults);
	}
	*/
	public List<Tripla<Float,Integer,Integer>> sortInstancesByQID(List<Tripla<Float,Integer,Integer>> lstInstances)
	{
		//return lstInstances;
		
		if(lstInstances == null || lstInstances.size() == 0)
		{
			return lstInstances;
		}
		Collections.sort(lstInstances,new Comparator<Tripla<Float,Integer,Integer>>() {

			@Override
			public int compare(Tripla<Float, Integer, Integer> o1,
					Tripla<Float, Integer, Integer> o2) {
				// TODO Auto-generated method stub
				if(o1.getZ()!= null)
				{
					return o1.getZ()-o2.getZ();
				}else
				{
					return 0;
				}
			}
		
		});
		return lstInstances;
		
	}
	public boolean verifyContainSameQID(List<Tripla<Float,Integer,Integer>> lstInstances1,List<Tripla<Float,Integer,Integer>> lstInstances2)
	{
		Set<Integer> setQIDsInstances1 = new HashSet<Integer>();
		Set<Integer> setQIDsInstances2 = new HashSet<Integer>();
		boolean justNull = true;
		for(Tripla<Float,Integer,Integer> instance : lstInstances1)
		{
			if(instance.getZ() != null)
			{
				justNull = false;
			}
			setQIDsInstances1.add(instance.getZ());
		}
		for(Tripla<Float,Integer,Integer> instance : lstInstances2)
		{
			if(instance.getZ() != null)
			{
				justNull = false;
			}
			setQIDsInstances2.add(instance.getZ());
		}
		
		setQIDsInstances2.retainAll(setQIDsInstances1);
		return !justNull && setQIDsInstances2.size() > 0;
	}
	public Fold[] createFoldsView(ViewCreatorHelper viewCreatableHelper ,int numFolds, int[] numSubFoldPerView ) throws Exception
	{
		MetodoAprendizado metAp = this.getMetodoAprendizado();
		arrFolds = new Fold[numFolds];
		File dirView = new File(this.dir,this.getFeatureType().toString().toLowerCase());
		if(!dirView.exists())
		{
			dirView.mkdirs();
		}
		
		for(int f = 0 ; f<numFolds ; f++)
		{
			System.out.println("Criando para o fold: "+f);
			
			//coloca o treino no subfold0 (nao faz mta diferenca)
			if(!dirView.exists()){
				dirView.mkdirs();
			}
			

			List<Tripla<Float,Integer,Integer>> lstInstancesTreino = sortInstancesByQID(viewCreatableHelper.getClassIdAndQidTrain(f, null));
			List<Tripla<Float,Integer,Integer>> lstInstancesTeste = sortInstancesByQID(viewCreatableHelper.getClassIdAndQidTest(f, null));
			List<Tripla<Float,Integer,Integer>> lstInstancesVal = sortInstancesByQID(viewCreatableHelper.getClassIdAndQidValidation(f, null));
			if(this.getMetodoAprendizado() instanceof GenericoLetorLike)
			{
				if(verifyContainSameQID(lstInstancesTreino,lstInstancesTeste))
				{
					System.out.println("========================== ATENCAO! Ha QIDs iguais no treino e no teste");
				}
				if(lstInstancesVal != null && verifyContainSameQID(lstInstancesVal,lstInstancesTeste))
				{
					System.out.println("========================== ATENCAO! Ha QIDs iguais na validação e no teste");
				}
				if(lstInstancesVal != null && verifyContainSameQID(lstInstancesTreino,lstInstancesVal))
				{
					System.out.println("========================== ATENCAO! Ha QIDs iguais no treino e na validação");
				}
			}
			File arqTreino = createFile(dirView,"treino",viewCreatableHelper,lstInstancesTreino,f, 0);
			File arqTeste = createFile(dirView,"teste",viewCreatableHelper,lstInstancesTeste,f, null);
			
			
			File arqValidacao = null;
			if(lstInstancesVal != null && lstInstancesVal.size()>0)
			{
				arqValidacao = createFile(dirView,"validacao",viewCreatableHelper,lstInstancesVal,f, null);
			}
			
			File idsDir = new File(dirView,"ids");
			if(!idsDir.exists())
			{
				idsDir.mkdirs();
			}
			File arqTesteIds = File.createTempFile("teste"+f+"ids", "", idsDir);;
			File arqValidacaoIds = null;
			if(arqValidacao != null)
			{
				arqValidacaoIds = File.createTempFile("validacao"+f+"ids", "", idsDir);;
				
			}
			
			metAp.criaArquivoIds(arqTeste, arqTesteIds);
			if(arqValidacao != null)
			{
				metAp.criaArquivoIds(arqValidacao, arqValidacaoIds);
			}
			
			Fold[] subFolds = new Fold[numSubFoldPerView[f]];
			
			for(int subF =0 ; subF < subFolds.length ; subF++)
			{
				System.out.println("Sub fold "+subF+" do fold: "+f);
				List<Tripla<Float,Integer,Integer>> lstInstancesSubTreino = sortInstancesByQID(viewCreatableHelper.getClassIdAndQidTrain(f, subF));
				List<Tripla<Float,Integer,Integer>> lstInstancesSubTeste = sortInstancesByQID(viewCreatableHelper.getClassIdAndQidTest(f, subF));
				
				if(verifyContainSameQID(lstInstancesSubTreino,lstInstancesSubTeste))
				{
					System.out.println("========================== ATENCAO! Ha QIDs iguais no treino e no teste");
				}
				
				
				
				File arqSubTreino = createFile(dirView,"subtreino"+f+"_"+subF+"_",viewCreatableHelper,lstInstancesSubTreino,f, subF);
				File arqSubTeste = createFile(dirView,"subteste"+f+"_"+subF+"_",viewCreatableHelper,lstInstancesSubTeste,f, subF);
				

				//File arqSubValidacao = createFile(dirView,"subvalidacao"+f,viewCreatableHelper,viewCreatableHelper.getClassIdAndQidValidation(f, null),f, null);
				
				File arqSubTesteIds = File.createTempFile("subTeste"+f+"_"+subF+"_ids", "", idsDir);
				//File arqSubValidacaoIds = arqSubValidacao!=null?File.createTempFile("subValidacao"+subF+"ids", "",idsDir):null;
				
				metAp.criaArquivoIds(arqSubTeste, arqSubTesteIds);
				/*
				if(arqSubValidacao!=null)
				{
					metAp.criaArquivoIds(arqSubValidacao, arqSubValidacaoIds);
				}
				*/
				
				subFolds[subF] = new Fold(subF, arqTreino, arqSubTreino, arqSubTeste, arqSubTesteIds);
				//validacao sempre pega do fold principalç
				subFolds[subF].setValidationFiles(arqValidacao, arqValidacaoIds);
			}
			arrFolds[f] = new Fold(f, metAp.getArquivoOrigem(), arqTreino, arqTeste, arqTesteIds);
			arrFolds[f].setValidationFiles(arqValidacao, arqValidacaoIds);
			arrFolds[f].setSubFolds(subFolds);
		}
		
		return arrFolds;
	}

	
	public File createFile(File dir,String prefix,ViewCreatorHelper viewCreatableHelper,List<Tripla<Float,Integer,Integer>> lstInstances,Integer foldNum, Integer subFoldNum) throws Exception
	{
		if(lstInstances.size()==0)
		{
			return null;
		}
		//System.out.println("Dir: "+dir.getAbsolutePath());
		File arqOut = File.createTempFile(prefix, "__"+foldNum+(subFoldNum!=null?"_"+subFoldNum:"")+".data", dir);
		

		
		//para cada instancia
		BufferedWriter out = new BufferedWriter(new FileWriter(arqOut, false),1024*1024*50);
		MetodoAprendizado metAp = this.getMetodoAprendizado();

		
		//gera dataset
		for(Tripla<Float,Integer,Integer> instance : lstInstances)
		{
			

			//cria o feature set
			HashMap<Long,String> mapFeatures = new HashMap<Long,String>();
			for(Integer idxFeature : lstIdxFeatures)
			{
				
				//regata o valor
				String val = viewCreatableHelper.getFeatureVal(foldNum, subFoldNum, instance.getY().longValue(), idxFeature);
				
				//coloca o valor na instancia local
				if(val != null)
				{
					//regata idx da feature local
					int idxLocalIdxFeature = mapGlobalToLocalFeature.get(idxFeature);
					
					
					
					mapFeatures.put((long)idxLocalIdxFeature, val);
				}
				
			}
			
			//grava no arquivo
			String linha = "";
			if(metAp instanceof GenericoLetorLike)
			{
				linha = metAp.gerarLinhaDataset(instance.getX(), instance.getY(), instance.getZ(), mapFeatures,new HashMap<String,String>());
			}else
			{
				linha = metAp.gerarLinhaDataset(instance.getX(),instance.getY(), mapFeatures,new HashMap<String,String>());
			}
			
			out.write(StringUtil.removeDoubleSpace(linha)+"\n");
		}
		
		out.close();
		
		return arqOut;
	}

	@Override
	public Resultado getResultValidacao() {
		// TODO Auto-generated method stub
		return this.artResultValidacao;
	}

	@Override
	public Resultado getResultTeste() {
		// TODO Auto-generated method stub
		return this.artResultTeste;
	}

	@Override
	public Resultado getResultTreino() {
		// TODO Auto-generated method stub
		return this.artResultTreino;
	}
	public String toString()
	{
		return this.getFeatureType().toString().toLowerCase();
	}
	public static void main(String[] args)
	{
		Integer[] idv1 = {2,2,2,2,2,2,2};
		Integer[] idv2 = {1,1,1,1,1,1,1};
		Random rnd = new Random();
		Integer[] resp = {0,0,0,0,0,0,0};
		for(int i =0 ; i<idv1.length ; i++)
		{
			float prob = rnd.nextFloat();
			
			if(prob<0.5)
			{
				resp[i] = idv1[i];
			}else
			{
				resp[i] = idv2[i];
			}
		}
		for(int i =0 ; i<resp.length ; i++)
		{
			System.out.print(resp[i]+";");
		}
	}
	
}
