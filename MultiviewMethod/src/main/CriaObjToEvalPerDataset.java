package main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import apredizadoCombinacao.FeatureSelectionHelper;
import aprendizadoUtils.FitnessCalculator;
import aprendizadoUtils.GenericoLetorLike;
import aprendizadoUtils.GenericoSVMLike;
import aprendizadoUtils.MatrizFeatures;
import aprendizadoUtils.MetodoAprendizado;
import aprendizadoUtils.SVM;
import arquivo.ArquivoUtil;
import entidadesAprendizado.Fold;
import entidadesAprendizado.XMLMetodoAprendizado;
import stuctUtil.Tupla;


public class CriaObjToEvalPerDataset {
	public static void defineFoldIds(MetodoAprendizado metAp,FeatureSelectionHelper fHelper,List<Long> treino,List<Long> teste,int numSubFoldsTreino) throws Exception
	{
		
		int[] numSubFoldsPerFold=null;
		
		List<Long>[] idsSubFoldsTreino = Fold.divideIntoFolds(numSubFoldsTreino, treino,Fold.SEED_DEFAULT);;
		numSubFoldsPerFold = new int[1];
		numSubFoldsPerFold[0] = numSubFoldsTreino;
		
		System.out.println("Train ids:"+treino);
		System.out.println("test ids:"+teste);
		//id teste = f
		//adiciona ids de teste
		for(long id : teste)
		{
			fHelper.addClassIdAndQidPerTestFold(0, Float.parseFloat(metAp.getClasseRealLinhaMapeada(id)), (int)id, metAp.getQIDLinhaMapeada(id));
		}
			
		//id treino = todos as outras listas de ids menos a f
		for(int subFoldTreino = 0 ; subFoldTreino < idsSubFoldsTreino.length ; subFoldTreino++)
		{
				for(long idTreino : idsSubFoldsTreino[subFoldTreino])
				{
						//adiciona ao treino
						fHelper.addClassIdAndQidPerTrainFold(0, Float.parseFloat(metAp.getClasseRealLinhaMapeada(idTreino)), (int)idTreino, metAp.getQIDLinhaMapeada(idTreino));
						
						//adiciona o test do subtreino 
						fHelper.addClassIdAndQidPerTestPerSubTrainFold(0,subFoldTreino,Float.parseFloat(metAp.getClasseRealLinhaMapeada(idTreino)), (int)idTreino, metAp.getQIDLinhaMapeada(idTreino));
				}
				
				//adiciona o treino do subtreino
				//tdos os ids menos fTreino
				for(int fSubTeste = 0 ; fSubTeste < idsSubFoldsTreino.length ; fSubTeste++)
				{
						if(fSubTeste != subFoldTreino)
						{
							//adiciona os id de teste do fold treino fTreino
							for(long idSubTreinoTreino : idsSubFoldsTreino[fSubTeste])
							{
								//adiciona o trei do subtreino 
								fHelper.addClassIdAndQidPerTrainPerSubTrainFold(0,subFoldTreino,Float.parseFloat(metAp.getClasseRealLinhaMapeada(idSubTreinoTreino)), (int)idSubTreinoTreino, metAp.getQIDLinhaMapeada(idSubTreinoTreino));	
							}
						}
				}
						
			
		}
		
		
		fHelper.setNumSubFoldsPerFold(numSubFoldsPerFold);
		
	}
	public void defineFoldIds(MetodoAprendizado metAp,FeatureSelectionHelper fHelper,List<Long> instancias,int numFolds) throws Exception
	{
		
		int[] numSubFoldsPerFold=null;
		
		List<Long>[] idsTeste = Fold.divideIntoFolds(numFolds, instancias,Fold.SEED_DEFAULT);;
		numSubFoldsPerFold = new int[idsTeste.length];
		//adiciona os ids de cada fold
		
		for(int f = 0; f < idsTeste.length ; f++)
		{
			//id teste = f
			//adiciona ids de teste
			for(long id : idsTeste[f])
			{
				fHelper.addClassIdAndQidPerTestFold(f, Float.parseFloat(metAp.getClasseRealLinhaMapeada(id)), (int)id, null);
			}
			
			
			//id treino = todos as outras listas de ids menos a f
			int subFoldNum = 0;
			numSubFoldsPerFold[f] = idsTeste.length-1;
			for(int fTreino = 0 ; fTreino < idsTeste.length ; fTreino++)
			{
				if(f != fTreino)
				{
					//adiciona o id de treino do fold teste f
					for(long id : idsTeste[fTreino])
					{
						fHelper.addClassIdAndQidPerTrainFold(f, Float.parseFloat(metAp.getClasseRealLinhaMapeada(id)), (int)id, null);
						
						//adiciona o test do subtreino 
						fHelper.addClassIdAndQidPerTestPerSubTrainFold(f,subFoldNum,Float.parseFloat(metAp.getClasseRealLinhaMapeada(id)), (int)id, null);
						
					}
					//adiciona o treino do subtreino
					//tdos os ids menos f e o fTreino
					for(int fSubTeste = 0 ; fSubTeste < idsTeste.length ; fSubTeste++)
					{
						if(fSubTeste != f && fSubTeste != fTreino)
						{
							//adiciona os id de teste do fold treino fTreino
							for(long id : idsTeste[fSubTeste])
							{
								
								//adiciona o trei do subtreino 
								fHelper.addClassIdAndQidPerTrainPerSubTrainFold(f,subFoldNum,Float.parseFloat(metAp.getClasseRealLinhaMapeada(id)), (int)id, null);
								
							}		
						}
					}
						

					subFoldNum++;

				}
			}
		}
		
		
		fHelper.setNumSubFoldsPerFold(numSubFoldsPerFold);
		
	}
	public static int getNumFeats(Map<Long ,Map<Long,String>> map)
	{
		long max = 0;
		for(long id : map.keySet())
		{
			long maxForInst = 0;
			for(long featIdx : map.get(id).keySet())
			{
				if(featIdx > maxForInst)
				{
					maxForInst = featIdx;
				}
			}
			if(maxForInst > max)
			{
				max = maxForInst;
			}
		}
		return (int)max+1;
	}

	public static void addAllFeats(
			FeatureSelectionHelper fHelper,
			Map<Long, Map<Long, String>> mapFeatsPerInstanceLong
			) {
		MatrizFeatures<Short> matFeatures = new MatrizFeatures<Short>(mapFeatsPerInstanceLong.size(), getNumFeats(mapFeatsPerInstanceLong));
		for(long id : mapFeatsPerInstanceLong.keySet())
		{
			
				HashMap<Short,String> mapFeat = new HashMap<Short,String>();
				for(long featIdx : mapFeatsPerInstanceLong.get(id).keySet())
				{
					mapFeat.put((short)featIdx,mapFeatsPerInstanceLong.get(id).get(featIdx));
				}
				matFeatures.adicionaFeature((int)id, mapFeat);
			
		}
		fHelper.addMatrizFeaturesGeral(1, matFeatures);
	}
	public void criaObjToEval(File arqDataset,int numFolds,MetodoAprendizado metAp,File arqOut) throws Exception
	{	
		metAp.setNomExperimento(arqDataset.getName());
		FeatureSelectionHelper fHelper = new FeatureSelectionHelper(metAp, metAp, arqOut);
		
		
		
		metAp.mapeiaIdPorLinha(arqDataset);
		//adiciona todas as feats
		Map<Long ,Map<Long,String>> mapFeatsPerInstanceLong = metAp.mapeiaFeatureInstancias(arqDataset);
		
		addAllFeats(fHelper, mapFeatsPerInstanceLong);
		
		
		defineFoldIds(metAp,fHelper,metAp.getIds(arqDataset),numFolds);
		
		

		//define a classe minima da colecao completa
		fHelper.setMinClasse(-1);
		
		fHelper.indexaFeaturesGlobalmente();
		///grava no diretorio do arquivo
		fHelper.gravaObject();
	}
	public static FeatureSelectionHelper criaObjToEvalFold(File arqTreino, File arqTeste,int numSubfoldsTreino,MetodoAprendizado metAp,File arqOut) throws Exception
	{	
		metAp.setNomExperimento(arqTreino.getName());
		FeatureSelectionHelper fHelper = new FeatureSelectionHelper(metAp, metAp, arqOut);
		
		
		//mapeia id por linha no teste e treino
		int lineOffset = metAp.mapeiaIdPorLinha(arqTreino, true, 1);
		int lastLineTest = metAp.mapeiaIdPorLinha(arqTeste,true,lineOffset+1);
		//treino: 1 até lineOffset (inclusive)
		List<Long> lstTreinoIds = generateSequence(1,lineOffset);
		//teste: lineOffset+1 até lastLineTest (inclusive)
		List<Long> lstTesteIds = generateSequence(lineOffset+1,lastLineTest);
		System.out.println("Treuno:"+lstTreinoIds);
		System.out.println("Teste:"+lstTesteIds);
		System.out.println("Ids Mapeados:"+metAp.getMapIdPorLinha().keySet());
		//System.exit(0);
		
		//adiciona todas as feats
		Map<Long ,Map<Long,String>> mapFeatsPerInstanceLong = metAp.mapeiaFeatureInstancias();
		
		addAllFeats(fHelper, mapFeatsPerInstanceLong);
		
		
		defineFoldIds(metAp,fHelper,lstTreinoIds,lstTesteIds,numSubfoldsTreino);
		
		

		//define a classe minima da colecao completa
		fHelper.setMinClasse(-1);
		
		fHelper.indexaFeaturesGlobalmente();
		///grava no diretorio do arquivo
		fHelper.gravaObject();
		
		return fHelper;
	}
	private static List<Long> generateSequence(int first, int last) {
		// TODO Auto-generated method stub
		List<Long> lstIds = new ArrayList<Long>();
		for(int i = first ; i<=last; i++) {
			lstIds.add((long)i);
		}
		return lstIds;
	}
	public enum ML_MODE{
		L2R,CLASSIFICATION,REGRESSION;
	}
	public static void multiview( File fileTrain,File fileTest, 
								Map<String, String> mapParamTrain, Map<String, String> mapParamTest, File featureSetupCache, boolean use_cache,
								Integer[] arrFeatures, 
								ML_MODE mlMode, String mlMethod,
								boolean withFeatSet,boolean justBase,int numSubfoldsTreino) throws Exception {
		MetodoAprendizado met = null;
		
		if(mlMode == ML_MODE.L2R) {
			met = new GenericoLetorLike(mlMethod,mapParamTrain,mapParamTest);
		}else {
			met = new GenericoSVMLike(mlMethod,mapParamTrain,mapParamTest);
			if(mlMode == ML_MODE.REGRESSION) {
				((GenericoSVMLike)met).setMode(SVM.MODE_REGRESSION);
				
			}else {
				((GenericoSVMLike)met).setMode(SVM.MODE_CLASSIFICATION);
			}
		}
		((GenericoSVMLike)met).setGravarNoBanco(false);
		
		//configure the feature set
		if(!use_cache || !featureSetupCache.exists()) {
			if(use_cache && !featureSetupCache.exists()) {
				System.out.println("The option 'use_feature_cache' is set, however the file '"+featureSetupCache.getAbsolutePath()+"' was not found.");
			
			}
			System.out.println("Creating the file '"+featureSetupCache.getAbsolutePath()+"'");
			criaObjToEvalFold(fileTrain, fileTest,numSubfoldsTreino,met,featureSetupCache);
			
			
		}else {
			System.out.println("Using the feature set file '"+featureSetupCache.getAbsolutePath()+"'");
		}
		
		//run and present the result
		long time = System.currentTimeMillis();
		System.out.println("Result: "+FeatureSelectionHelper.getResultadoConfigView(featureSetupCache,arrFeatures,new FitnessCalculator(),withFeatSet,justBase, new File("/home/profhasan/wiki.out"),mlMode));
		System.out.println("Time (just the evaluation): "+(System.currentTimeMillis()-time)/1000.0+" segundos");
	}
	public static void main(String[] args) throws Exception
	{
		args = new String[3];
		String dirFile = "/home/profhasan/git/multiview/toyExample";
		String dirCnf = "/home/profhasan/git/multiview";
		args[0] = dirFile+"/train_svm.txt";
		args[1] = dirFile+"/test_svm.txt";
		args[2] = dirCnf+"/configExample.cnf";
		if(args.length<2) {
			System.out.println("Usage java -jar multiview.jar <train-file> <test-file> <config-file>");
			return;
		}
		for(int i =0 ; i<args.length ;i++) {
			File arq = new File(args[i]);
			if(!arq.exists()) {
				System.out.println("File '"+arq.getAbsolutePath()+"' does not exist");
			}
		}
		
		
		File train = new File(args[0]);
		File test = new File(args[1]);
		File config = new File(args[2]);
		
		//extract the config params
		Map<String,String> mapConfigFile = ArquivoUtil.leKeyValueFile(new File(args[2]), true);
		Map<String,String> mapConfigFileCaseSensitive = ArquivoUtil.leKeyValueFile(new File(args[2]), false);
		String pathMLTools = mapConfigFile.getOrDefault("mltoolspath",".");
		File featSetfile = new File(mapConfigFile.getOrDefault("featuresetcachefile", train.getAbsolutePath().split("\\.")[0]+"_cache.obj"));
		boolean use_cache = mapConfigFile.getOrDefault("usecache", "false").equals("true");
		int intNumSubFolds = Integer.parseInt(mapConfigFile.getOrDefault("numsubfoldsintrain", "3"));
		
		Integer[] arrFeatures = getArrayFeatures(mapConfigFile,mapConfigFile.get("viewperfeature")); 
		
		
		
		ML_MODE mlMode = getMachineLearningMode(mapConfigFile.getOrDefault("mlmode","classification"));
		String mlMethod = mapConfigFile.get("mlmethod");
		if(mlMethod == null) {
			System.out.println("No machine learning method assigned. Define the machine learning method into the config file '"+config.getAbsolutePath()+"' ");
		}
		boolean withFeatSet = mapConfigFile.getOrDefault("withfeatset", "false").equals("true");
		boolean justBase = mapConfigFile.getOrDefault("justbaselevel", "false").equals("true");
		String strXmlMachineLearningMethods = mapConfigFile.getOrDefault("xmlmachinelearningmethods", "MultiviewMethod/learning_methods.xml");
		
		Map<String,String> mapParamTrain = getParamsMLMethod(mapConfigFileCaseSensitive,"ml_param_train_");
		Map<String,String> mapParamTest = getParamsMLMethod(mapConfigFileCaseSensitive,"ml_param_test_");
		
		//print the config
		System.out.println("You are running multiview with the following configuration:");
		System.out.println("Machine learning tools path:"+pathMLTools);
		System.out.println("Config file:"+config.getAbsolutePath());
		System.out.println("Training file:"+train.getAbsolutePath());
		System.out.println("Test file:"+test.getAbsolutePath());
		System.out.println("Feature set cache file:"+featSetfile.getAbsolutePath());
		System.out.println("Use feature set cache? "+use_cache);
		System.out.println("Machine learning mode:"+mlMode.toString());
		System.out.println("Machine learing method:"+mlMethod);
		System.out.println("Use feature set in the second level? "+withFeatSet);
		System.out.println("Use just first level?"+justBase);
		System.out.println("Training params: "+mapParamTrain);
		System.out.println("Test params: "+mapParamTest);
		GenericoSVMLike.xmlMetodoCnf = new XMLMetodoAprendizado(new File(strXmlMachineLearningMethods));
		
		StringBuffer str = new StringBuffer();
		Set<Integer> setViews = new HashSet<>();
		for(int viewId : arrFeatures) {
			if(viewId != 0) {
				setViews.add(viewId);
			}
		}
		for(int viewId : setViews) {
			str.append("\tview "+viewId+": ");
			for(int i =0; i<arrFeatures.length ; i++) {
				if(viewId == arrFeatures[i]) {
					str.append((i+1)+";");
				}
			}
			str.append("\n");
		}
		System.out.println("features idx per view: \n"+str);
		

		//run multiview
		GenericoSVMLike.PATH_TOOLS = new File(pathMLTools).getAbsolutePath();
		//System.exit(0);
		multiview(train,test,mapParamTrain,mapParamTest,featSetfile,use_cache,arrFeatures,mlMode,mlMethod,withFeatSet,justBase,intNumSubFolds);
		
		
		//CriaObjToEvalPerDataset c = new CriaObjToEvalPerDataset();
		
		/*
		c.criaObjToEval(new File(arqDir,"datasetBOWIrony.svm"), 5, new GenericoSVMLike("SVM"), new File(arqDir,"datasetBOWIrony.obj"));
		c.criaObjToEval(new File(arqDir,"datasetLIWC_irony.svm"), 5, new GenericoSVMLike("SVM"), new File(arqDir,"datasetLIWC_irony.obj"));
		c.criaObjToEval(new File(arqDir,"datasetBOW-LIWCIrony.svm"), 5, new GenericoSVMLike("SVM"), new File(arqDir,"datasetBOW-LIWCIrony.obj"));
		
		c.criaObjToEval(new File(arqDir,"datasetBOWSarcasm.svm"), 5, new GenericoSVMLike("SVM"), new File(arqDir,"datasetBOW_sarcasm.obj"));
		c.criaObjToEval(new File(arqDir,"datasetLIWC_sarcasm.svm"), 5, new GenericoSVMLike("SVM"), new File(arqDir,"datasetLIWC_sarcasm.obj"));
		c.criaObjToEval(new File(arqDir,"datasetBOW-LIWCSarcasm.svm"), 5, new GenericoSVMLike("SVM"), new File(arqDir,"datasetBOW-LIWC_sarcasm.obj"));
		*/
		
		//c.criaObjToEval(new File(arqDir,"68Feat.svm"), 5, new GenericoSVMLike("SVM"), new File(arqDir,"68Feat.obj"));
		//c.criaObjToEval(new File(arqDir,"datasetLIWC_sarcasm_irony.svm"), 5, new GenericoSVMLike("SVM"), new File(arqDir,"datasetLIWC_sarcasm_irony.obj"));
		//c.criaObjToEval(new File(arqDir,"datasetBOW-LIWCSarcasmIrony.svm"), 5, new GenericoSVMLike("SVM"), new File(arqDir,"datasetBOW-LIWCSarcasmIrony.obj"));
	}
	private static Map<String, String> getParamsMLMethod(Map<String, String> mapConfigFile, String prefix) {
		// TODO Auto-generated method stub
		Map<String,String> mapParam = new HashMap<String,String>();
		for(String key : mapConfigFile.keySet()) {
			if(key.toLowerCase().startsWith(prefix.toLowerCase())) {
				mapParam.put(key.substring(prefix.length()), mapConfigFile.get(key));
			}
		}
		return mapParam;
	}
	private static ML_MODE getMachineLearningMode(String mode) throws Exception {
		// TODO Auto-generated method stub
		switch(mode) {
			case "classification":
				return ML_MODE.CLASSIFICATION;
			case "regression":
				return ML_MODE.REGRESSION;
			case "l2r":
			case "learning to rank":
				return ML_MODE.L2R;
			default:
				throw new Exception("The machine learning mode '"+mode+"' is invalid. Please usage: classification, regression or l2r.");
		}
	}
	private static Integer[] getArrayFeatures(Map<String,String> mapConfigFile,String strArrFeatures) throws Exception {
		// TODO Auto-generated method stub
		if(strArrFeatures==null) {
			int view_num = 1;
			String view_idx_range = mapConfigFile.get("view_"+view_num+"_idx_range");
			List<Tupla<Integer,Integer>> lstRanges = new ArrayList<>();
			int firstIdx,lastIdx;
			int maxIdx = 0;
			while(view_idx_range!=null) {
				view_idx_range = view_idx_range.replaceAll(" ", "");
				//get the range from this value
				if(view_idx_range.matches("[0-9]+")) {
					firstIdx = Integer.parseInt(view_idx_range);
					lastIdx = Integer.parseInt(view_idx_range);
					if(lastIdx>maxIdx) {
						maxIdx=lastIdx;
					}
					lstRanges.add(new Tupla<Integer,Integer>(firstIdx,lastIdx));
				}else
				{
					if(view_idx_range.matches("[0-9]+-[0-9]+")) {
						String[] arrIdx = view_idx_range.split("-");
						firstIdx = Integer.parseInt(arrIdx[0]);
						lastIdx = Integer.parseInt(arrIdx[1]);
						if(lastIdx>maxIdx) {
							maxIdx=lastIdx;
						}
						lstRanges.add(new Tupla<Integer,Integer>(firstIdx,lastIdx));
					}else {
						throw new Exception("The range from the view numer "+view_num+" is invalid. It must be: <start_index>-<end_index>. Ex: 5-10");
					}
				}
				
				//next view
				view_num++;
				view_idx_range = mapConfigFile.get("view_"+view_num+"_idx_range");
			}
			if(lstRanges.size()==0) {
			
				throw new Exception("The option 'viewPerFeature' or the ranges per view is not set in the config file");
			}
			
			//cria as features com os ranges
			Integer[] arrFeatures = new Integer[maxIdx];
			for(int view_id=1; view_id<=lstRanges.size() ; view_id++) {
				Tupla<Integer,Integer> range = lstRanges.get(view_id-1);
				for(int i=range.getX()-1; i<range.getY() ; i++) {
					arrFeatures[i] = view_id;
				}
			}
			return arrFeatures;
		}else {
			String[] arrStrFeatures = strArrFeatures.split(",");
			Integer[] arrFeatures = new Integer[arrStrFeatures.length];
			for(int i =0 ; i<arrStrFeatures.length ; i++) {
				arrFeatures[i] = Integer.parseInt(arrStrFeatures[i]);
			}
			return arrFeatures;
		}
	}
	
}
