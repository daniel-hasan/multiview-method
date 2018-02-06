package apredizadoCombinacao;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import matematica.Kappa;
import scriptsUtil.DatasetUtil;
import string.StringUtil;
import stuctUtil.ListaAssociativa;
import stuctUtil.Tupla;
import utilAprendizado.params.ParamUtilSVMFilter;
import aprendizadoResultado.ResultadosWikiMultiviewMetodos;
import aprendizadoResultado.ValorResultado;
import aprendizadoUtils.GenericoLetorLike;
import aprendizadoUtils.GenericoSVMLike;
import aprendizadoUtils.IdClass;
import aprendizadoUtils.KNN;
import aprendizadoUtils.MetodoAprendizado;
import aprendizadoUtils.SVM;
import arquivo.ArquivoUtil;
import banco.GerenteBD;
import config_tmp.Colecao;
import config_tmp.ConfigViewColecao;
import config_tmp.MinMax;
import entidadesAprendizado.FeatureNormalizer;
import entidadesAprendizado.Fold;
import entidadesAprendizado.MatrizConfusao;
import entidadesAprendizado.Resultado;
import entidadesAprendizado.ResultadoItem;
import entidadesAprendizado.View;
import entidadesAprendizado.View.FeatureType;
import entidadesAprendizado.View.TIPO_DIVISAO_TREINO;
import featSelector.ViewCreatorHelper;

public class MetaLearning extends AbordagemCombinacao 
{
	private static  boolean GRAVAR_KAPPA = false;
	public static  boolean COMPUTAR_METALEARNING = true;
	public static  boolean GRAVA_SCALE = true;
	
	private static MinMax objMinMaxNorm = new MinMax(0,1);
	private boolean utilizarConfianca = false;
	private boolean utilizarConfiancaArray = false;
	private boolean utilizarFeaturesSet = false;
	private boolean utilizarFeatPertenceView = false;
	private boolean utilizarFeatKappa = false;
	private boolean utilizarRankPos = false;
	private MetodoAprendizado metCombinacao = null;
	private MatrizConfusao mConfusao = new MatrizConfusao(6);
	private boolean ignoraLinhaSemResult = false;
	private TIPO_CONTEUDO_DATASET[] conteudoDatasetMetalearning;
	private File dirMetaLearning = null;
	private File dirParamPrimTreino = null;
	private TipoFeatureCombinacao objTipoCombinacao = null;
	private View[] arrViews;
	private boolean usingWiki = true;
	private List<Integer> lstFeaturesIdxToInclude = new ArrayList<Integer>();
	
	private static HashMap<Long, PreparedStatement> stmtInsereParamMetalearning = new HashMap<Long, PreparedStatement>();
	private HashMap<Long,Kappa> kappaPorId = new HashMap<Long, Kappa>();
	private boolean[] arrClassesExistentes = new boolean[0];
	private SeletorViews seletorView =null;
	private Map<Long,String> mapFeatMetaLearnigPredictions = new HashMap<Long,String>();
	private int idxColecaoCol = 0;
	private MinMax objMinMaxClasse;
	private ViewCreatorHelper objViewCreatorHelper = null;
	public enum TIPO_CONTEUDO_DATASET {
		CONFIANCA,
		CONFIANCA_ARRAY,
		FEATURES_SET,
		PERTENCE_VIEW,
		RANK_POS,
		KAPPA
	}
	public MetaLearning(MetodoAprendizado metCombinacao,TIPO_CONTEUDO_DATASET[] conteudoDatasetMetalearning) throws IOException
	{
		this.conteudoDatasetMetalearning = conteudoDatasetMetalearning;
		this.metCombinacao = metCombinacao;
		
		for(TIPO_CONTEUDO_DATASET tpoMeta : conteudoDatasetMetalearning)
		{
			if(tpoMeta == TIPO_CONTEUDO_DATASET.CONFIANCA)
			{
				this.utilizarConfianca = true;
			}

			if(tpoMeta == TIPO_CONTEUDO_DATASET.FEATURES_SET)
			{
				this.utilizarFeaturesSet = true;
			}
			if(tpoMeta == TIPO_CONTEUDO_DATASET.PERTENCE_VIEW)
			{
				this.utilizarFeatPertenceView = true;
			}
			if(tpoMeta == TIPO_CONTEUDO_DATASET.CONFIANCA_ARRAY)
			{
				this.utilizarConfiancaArray = true;
			}
			if(tpoMeta == TIPO_CONTEUDO_DATASET.KAPPA)
			{
				this.utilizarFeatKappa = true;
			}
			if(tpoMeta == TIPO_CONTEUDO_DATASET.RANK_POS)
			{
				this.utilizarRankPos = true;
			}
		}
		
		if(this.utilizarFeaturesSet)
		{
			if(this.metCombinacao.getColecao() != null)
			{
				File origem = this.metCombinacao.getColecao().getArquivoOrigem();
				this.metCombinacao.mapeiaIdPorLinha(origem);
			}
			
			
			
		}
		
	}
	public void setViewCreatorHelper(ViewCreatorHelper objViewCreatorHelper)
	{
		this.objViewCreatorHelper = objViewCreatorHelper;
	}
	public void setSeletorViews(SeletorViews seletorView)
	{
		this.seletorView = seletorView;
	}
	public void setUsingWiki(boolean usingWiki)
	{
		this.usingWiki = usingWiki;
	}
	public static void inicializaBD() throws ClassNotFoundException, SQLException
	{
		Long idCurrentThread = Thread.currentThread().getId();
		Connection conn = GerenteBD.getGerenteBD().obtemConexao(Thread.currentThread().getName());
		
		if(!stmtInsereParamMetalearning.containsKey(idCurrentThread) )
		{
			stmtInsereParamMetalearning.put(idCurrentThread, conn
					.prepareStatement("insert into wiki_results.metalearning_kappa_params (page_id,nomExperimento,kappa,pa,pe,inputKappa) values (?,?,?,?,?,?)"));
		}
		
	}

	public void setTipoCombinacao(TipoFeatureCombinacao objTipo)
	{
		//System.err.println("MUDOU COMBINAÇÂO! "+objTipo);
		objTipoCombinacao = objTipo;
	}
	
	public void setMatrizConfusao(MatrizConfusao mt)
	{
		this.mConfusao = mt;
	}
	public String getNomExperimento()
	{
		return this.metCombinacao.getNomExperimento();
	}
	
	public File criaArquivo(String sufixo,Resultado result)
	{
		String sigla = getSiglaColecao();
		String nomeBase = sigla;

		File diretorio = getDirMetalearning();
		return new File(diretorio.getAbsoluteFile()+"/"+nomeBase+sufixo);
	}
	private String getSiglaColecao()
	{
		String sigla = "combinacao";
		if(metCombinacao.getColecao() != null)
		{
			sigla = metCombinacao.getColecao().getSigla();
		}
		return sigla;
	}
	public void setMinMaxClasse(MinMax obj)
	{
		this.objMinMaxClasse = obj;
	}
	public void setDirMetalearning(File objDir)
	{
		this.dirMetaLearning = objDir;
	}
	public void setDirParamPrimTreino(File objDir)
	{
		dirParamPrimTreino = objDir;
	}
	public void setIgnoraLinhaSemResult(boolean ignora)
	{
		ignoraLinhaSemResult = ignora;
	}
	public void setFeaturesIdxToInclude(List<Integer> lstFeatsIdx)
	{
		this.lstFeaturesIdxToInclude = lstFeatsIdx;
	}
private static long[] preprocessaFeatures(MetodoAprendizado metAp,	Set<Long> idsInstances,ListaAssociativa<Long, Map<Long, String>> lstMapFeaturesPerId,Map<Long, String>... arrMapfeaturesPerInstance) {
		
		long[] arrMaxIdFeatPerVector = new long[arrMapfeaturesPerInstance.length];
		for(int i = 0 ; i < arrMaxIdFeatPerVector.length ; i++)
		{
			arrMaxIdFeatPerVector[i] = 0;
		}

		
		for(long idInstance : idsInstances)
		{
			//for(long idInstance : arrMapfeaturesPerInstance[i].keySet())
			//{
			for(int i = 0; i < arrMapfeaturesPerInstance.length ; i++)
			{
				if(arrMapfeaturesPerInstance[i].containsKey(idInstance))
				{
					String linha = arrMapfeaturesPerInstance[i].get(idInstance);
					MetodoAprendizado metApToUse = null;
					metApToUse = chooseMetodoToUse(metAp, linha);
					HashMap<Long,String> map =  metApToUse.getFeaturesVector(linha);
					
					lstMapFeaturesPerId.put(idInstance, map);
					
					//navega nas chaves deste map e verifica se ha algum maior que arrMaxIdFeatPerVector
					for(long featId : map.keySet())
					{
						if(arrMaxIdFeatPerVector[i] < featId)
						{
							arrMaxIdFeatPerVector[i] = featId;
						}
					}
				}else{
					
					lstMapFeaturesPerId.put(idInstance, new HashMap<Long,String>());
				}
			}
			//}
		}

		return arrMaxIdFeatPerVector;
	}
private static MetodoAprendizado chooseMetodoToUse(MetodoAprendizado metAp,
		String linha) {
	MetodoAprendizado metApToUse = null;
	GenericoSVMLike gSVM = new GenericoSVMLike();
	GenericoLetorLike gLetor = new GenericoLetorLike();
	if(metAp.linhaMatchesFormat(linha))
	{
		metApToUse = metAp;
	}else
	{
		if(gLetor.linhaMatchesFormat(linha))
		{
			metApToUse = gLetor;
		}else
		{
			if(gSVM.linhaMatchesFormat(linha))
			{
				metApToUse = gSVM;
			}
		}
	}
	return metApToUse;
}
	private static Map<Long, String> gerarMapFeaturesGeral(long[] arrMaxIdFeatPerVector, List<Map<Long, String>> lstFeatures) {
		Map<Long,String> mapFeaturesGeral = new HashMap<Long,String>();
		//adiciona o primeiro completo e os proximos relativos ao primeiro
		mapFeaturesGeral.putAll(lstFeatures.get(0));
		long offSet = arrMaxIdFeatPerVector[0]+1L;
		
		// TODO Auto-generated method stub
		for(int i = 1 ; i < lstFeatures.size() ; i++)
		{
			Map<Long,String> mapFeaturesAtual = lstFeatures.get(i);
			for(long featId : mapFeaturesAtual.keySet())
			{
				mapFeaturesGeral.put(featId+offSet, mapFeaturesAtual.get(featId));
			}
			offSet += arrMaxIdFeatPerVector[i]+1;
			
		}
		return mapFeaturesGeral;
		
	}

	
	public static void fazDataset(
									MetodoAprendizado metAp,
									File arqDataset,File arqIds,
									ListaAssociativa<Long, Long> mapRespsPorPergunta,
									Map<Long,Long> pergPerResp,
									Map<Long,Double> mapClassPerInstance,
									Map<Long,Map<Long,String>> lstFeatsAdicionais,
									Map<Long,String> ... arrMapfeaturesPerInstance
									) throws IOException
	{
		//para cada id vc listas de conjuntos de features
		ListaAssociativa<Long,Map<Long,String>> lstMapFeaturesPerId = new ListaAssociativa<Long,Map<Long,String>>();
		
		
		
		//preprocessa features resgatando o max id por feat vector
		long[] arrMaxIdFeatPerVector = preprocessaFeatures(metAp,mapClassPerInstance.keySet(),lstMapFeaturesPerId,arrMapfeaturesPerInstance);
		
		long maxIdAdicional= 0;
		System.out.println("oasdioi");
		for(long instanceId : lstFeatsAdicionais.keySet())
		{
			lstMapFeaturesPerId.put(instanceId, lstFeatsAdicionais.get(instanceId));
			for(long featId : lstFeatsAdicionais.get(instanceId).keySet())
			{
				if(maxIdAdicional<featId)
				{
					maxIdAdicional = featId;
				}
			}
			
		}
		long[] arrNewMaxIdFeatPerVector =  new long[arrMaxIdFeatPerVector.length+1];
		for(int i =0 ; i<arrMaxIdFeatPerVector.length ; i++)
		{
			arrNewMaxIdFeatPerVector[i] = arrMaxIdFeatPerVector[i];
		}
		arrNewMaxIdFeatPerVector[arrNewMaxIdFeatPerVector.length-1] = maxIdAdicional;
		arrMaxIdFeatPerVector = arrNewMaxIdFeatPerVector;
		//caso jah exista arquivo de ids, le ele e guarda os ids na ordem. para gerar um arquivo de id na mesma ordem deste
		List<Long> lstInstances = new ArrayList<Long>();
		if(arqIds.exists())
		{
			BufferedReader in = new BufferedReader(new FileReader(arqIds));
			String str;
			StringBuilder texto = new StringBuilder();
			while ((str = in.readLine()) != null)
			{
				lstInstances.add(Long.parseLong(str));
			}
			in.close();

		}
		else
		{
			//caso nao exista, a ordem é aleatoria
			List<Long> lstIds = new ArrayList<Long>();
			for(long qid : mapRespsPorPergunta.keySet())
			{
				for(long instanceId : mapRespsPorPergunta.getList(qid))
				{
					lstIds.add(instanceId);
				}
			}
			List<Long>[] lstFolds = Fold.divideIntoFolds(1,lstIds, lstIds.size()+10);
			lstInstances = lstFolds[0];
		}
		
		
		//cria arquivo de features de acordoc com o formato do met de aprendizado
		BufferedWriter out = new BufferedWriter(new FileWriter(arqDataset, false),100);
		BufferedWriter outIds = new BufferedWriter(new FileWriter(arqIds, false),100);
		
		//cria o dataset por instancia 
		for(long instanceId : lstInstances)
		{
				List<Map<Long,String>> lstFeatures = lstMapFeaturesPerId.getList(instanceId);
				
				HashMap<Long,String> mapFeaturesGeral = (HashMap<Long,String>)gerarMapFeaturesGeral(arrMaxIdFeatPerVector,lstFeatures);
				
				String linha = "";
				if(metAp instanceof GenericoLetorLike)
				{
					long qid = pergPerResp.get(instanceId);
					linha = ((GenericoLetorLike)metAp).gerarLinhaDataset(mapClassPerInstance.get(instanceId),(int)instanceId,(int)qid,mapFeaturesGeral,new HashMap<String,String>());
				}else
				{
					linha = metAp.gerarLinhaDataset(mapClassPerInstance.get(instanceId), (int) instanceId, mapFeaturesGeral);
				}
				linha = StringUtil.removeDoubleSpace(linha);
				
				out.write(linha+"\n");
				outIds.write(instanceId+"\n");
		}
		out.close();
		outIds.close();
		
		
	}
	private File getDirMetalearning()
	{
		if(dirMetaLearning != null)
		{
			if(!dirMetaLearning.exists())
			{
				dirMetaLearning.mkdirs();
			}
			return dirMetaLearning;
		}
		
		String subViews = ResultadosWikiMultiviewMetodos.SUB_VIEW_NAME.length()>0?"_"+ResultadosWikiMultiviewMetodos.SUB_VIEW_NAME:"";
		if(objTipoCombinacao != null)
		{
			subViews += "_"+objTipoCombinacao.getName();
		}
		File diretorio = null;
		String sglColecao = "";
		if(metCombinacao.getColecao() != null)
		{
			sglColecao = metCombinacao.getColecao().getArquivoOrigem().getParentFile().getAbsolutePath();
		}else
		{
			sglColecao = arrViews[0].getResultTeste().getFolds()[0].getTeste().getParentFile().getParentFile().getAbsolutePath();
		}
		diretorio = new File(sglColecao+"/metaLearning/"+(this.utilizarConfianca?"confiancaView":"")
																															+(this.utilizarConfiancaArray?"confiancaViewArray":"")
																															+(this.utilizarFeatPertenceView?"featPertView":"")
																															+this.metCombinacao.getNomExperimento()+
																															//+"LAC_SVM_METALEARNING"+
																															subViews);
		/*
		if(result.getFolds()[0].getTreino() != null)
		{
			//diretorio = new File(result.getFolds()[0].getTreino().getParentFile().getAbsolutePath()+"/metaLearning"+(this.utilizarConfianca?"confiancaView":"")+"/");
			diretorio = new File(metCombinacao.getColecao().getArquivoOrigem().getParentFile().getAbsolutePath()+"/metaLearning"+(this.utilizarConfianca?"confiancaView":"")+"/");
		}else
		{
			diretorio = new File("metaLearning"+(this.utilizarConfianca?"_confiancaView":"")+"/");
		}
		*/
		if(!diretorio.exists())
		{
			diretorio.mkdirs();
		}
		return diretorio;
	}

	public int getViewColectionIndex(View[] arrViews)
	{
		int idxViewColecao = 0;
		for(int i=0 ; i< arrViews.length; i++)
		{
			if(arrViews[i].getColecaoDatasetView() == arrViews[i].getColecao())
			{
				idxViewColecao = i;
			}
		}
		
		return idxViewColecao;
	}
	public void calculoPreTreino(View[] views) throws Exception
	{
		arrViews = views;
		super.calculoPreTreino(views);
		
		int numFolds = views[0].getResultTeste().getFolds().length;
		
		File fonte = criaArquivo(".fonte",views[0].getResultTeste());
		File fonteIds = criaArquivo(".idFonte",views[0].getResultTeste());
		if(fonte.exists())
		{
			fonte.delete();
		}
		if(fonteIds.exists())
		{
			fonteIds.delete();
		}
		
		HashSet<Integer> setClasses = new HashSet<Integer>();
		int maxClasse = 0;
		
		
		for(int i= 0 ; i<numFolds ; i++)
		{
			Fold[] resultPorView = new Fold[views.length];
			for(int j = 0 ; j<views.length ; j++)
			{
				Fold foldTeste = views[j].getResultTeste().getFolds()[i];
				Fold foldTreino = views[j].getResultTreino().getFolds()[i];
				resultPorView[j] = foldTeste;
				
				List<ResultadoItem> lstItens = new ArrayList<ResultadoItem>();
				lstItens.addAll(foldTeste.getResultadosValues());
				lstItens.addAll(foldTreino.getResultadosValues());
				
				for(ResultadoItem r : lstItens)
				{
					int classeReal =(int) Math.round(r.getClasseReal()); 
					setClasses.add(classeReal);
					if(maxClasse < classeReal)
					{
						maxClasse = classeReal;
					}
				}
			}
			
			
			//gerarFold(resultPorView,fonte,fonteIds,true);
		}
		
		//adiciona as classes existentes no array de classes
		System.out.println("Classes possiveis: "+setClasses);
		arrClassesExistentes = new boolean[maxClasse];
		for(int i = 0 ; i<arrClassesExistentes.length ; i++)
		{
			arrClassesExistentes[i] = setClasses.contains(i);
			System.out.println(i+ " => "+arrClassesExistentes[i]);
		}
		

	}
	
	public synchronized void gravarKappa(int pageId,String nomExperimento,Kappa k)
	{	
		try {
			inicializaBD();
			Long idCurrentThread = Thread.currentThread().getId();
			PreparedStatement stmtInsere = stmtInsereParamMetalearning.get(idCurrentThread);
			
			stmtInsere.setInt(1, pageId);
			stmtInsere.setString(2, nomExperimento);
			stmtInsere.setDouble(3, k.getKappa());
			stmtInsere.setDouble(4, k.getP());
			stmtInsere.setDouble(5, k.getPe());
			stmtInsere.setString(6, k.getInput());
			
			stmtInsere.execute();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	 public static Kappa calculaPaKappa(double[] arrPredictions,MinMax minMaxClass)
	{

		int numClasses = (int) (Math.round(minMaxClass.getMax()-minMaxClass.getMin()))+1;
		
		int[] arrPredictionsPerClass = new int[numClasses];
		
		for(int i = 0 ; i<arrPredictionsPerClass.length ; i++)
		{
			arrPredictionsPerClass[i] = 0;
		}
		for(double prediction : arrPredictions)
		{
			int classPredicted = (int) Math.round(prediction);
			
			
			
			//classe máxima
			if(classPredicted > minMaxClass.getMax())
			{
				classPredicted = (int) Math.round(minMaxClass.getMax());
			}
			if(classPredicted < minMaxClass.getMin())
			{
				classPredicted = (int) Math.round(minMaxClass.getMin());
			}

			
			arrPredictionsPerClass[classPredicted-(int)minMaxClass.getMin()]++;  			
		}
		
		String paramsExp = "";
		for(int i = 0 ; i< arrPredictionsPerClass.length ; i++)
		{
			paramsExp +=  arrPredictionsPerClass[i]+" ";
			
			
		}
		int[][] arrCasos = {arrPredictionsPerClass};
		Kappa k = Kappa.calculaKappa(arrCasos);
		k.setInput(paramsExp);
		return k;
	}
	 
	 public Fold combinarResultadoFold(Fold[] resultPorViewTreino, Fold[] resultPorViewTeste) throws Exception {
		return this.combinarResultadoFold( resultPorViewTreino,  resultPorViewTeste, null); 
	 }
	 
	 public static ListaAssociativa<Long, Long>  getFeaturesFromFile(MetodoAprendizado metAp,File feats,Map<Long, String> mapfeaturesfSelector,Map<Long,Double> mapClassPerInstance)
				throws FileNotFoundException, IOException {
		 	ListaAssociativa<Long, Long>  lstIds = new ListaAssociativa<Long,Long>();
			BufferedReader in = new BufferedReader(new FileReader(feats));
			String strLine;
			while ((strLine = in.readLine()) != null)
			{
				long id = metAp.getIdPorLinhaArquivo(strLine);
				Integer qid = metAp.getIdPorLinhaArquivo(strLine, "qid");
				if(qid != null)
				{
					lstIds.put((long)qid, id);	
				}else
				{
					lstIds.put(id, id);
				}
				
				mapClassPerInstance.put(id, Double.parseDouble(metAp.getClasseReal(strLine)));
				mapfeaturesfSelector.put(id, strLine);
			}
			in.close();
			return lstIds;
		}
	/**
	 * Combina resultado dos folds  resultPorViewTeste retornando um fold como resultado
	 */
	@SuppressWarnings("unchecked")
	public Fold combinarResultadoFold(Fold[] resultPorViewTreino, Fold[] resultPorViewTeste,Fold[] resultPorViewValidacao) throws Exception {
		
		
		if(resultPorViewTreino.length==0)
		{
			throw new Exception("Sem view de treino");
		}
		if(resultPorViewTeste.length==0)
		{
			throw new Exception("Sem view de teste");
		}
		
		//coloca a combinação no tipo combinação do meta learning
		if(resultPorViewTreino[0].getView()[0].getFeatureType() != null && objTipoCombinacao == null)
		{
			FeatureType[] arrTypesOfFeat = new FeatureType[resultPorViewTreino.length];
			for(int i =0 ; i <resultPorViewTreino.length ; i++)
			{
				
				arrTypesOfFeat[i] = resultPorViewTreino[i].getView()[0].getFeatureType();
				
				
				if(resultPorViewTreino[i].getView()[0].getMetodoAprendizado() instanceof KNN)
				{
					switch(arrTypesOfFeat[i])
					{
						case YT_TITLE:
							arrTypesOfFeat[i] = FeatureType.YT_KNN_TITLE;
							break;
						case YT_COMMENT:
							arrTypesOfFeat[i] = FeatureType.YT_KNN_COMMENT;
							break;
						case YT_DESCRIPTION:
							arrTypesOfFeat[i] = FeatureType.YT_KNN_DESCRIPTION;
							break;
						case YT_TAG:
							arrTypesOfFeat[i] = FeatureType.YT_KNN_TAG;
							break;
					}
				}
				System.out.println("Met. Aprend. View "+i+": "+resultPorViewTreino[i].getView()[0].getMetodoAprendizado().getClass()+"   View Type:"+arrTypesOfFeat[i]);
			}
			objTipoCombinacao = new TipoFeatureCombinacao(arrTypesOfFeat);
				
		}else
		{
			if(objTipoCombinacao == null && this.usingWiki)
			{
				System.err.println("Nao achou combincação fazendo por padrão "+resultPorViewTreino.length);
				switch(resultPorViewTreino.length)
				{
					case 12:
						objTipoCombinacao = new TipoFeatureCombinacao(FeatureType.STRUCTURE,FeatureType.STYLE,FeatureType.LENGTH,FeatureType.READ,FeatureType.HISTORY,FeatureType.NETWORK,
																	FeatureType.STRUCTURE_LOCAL,FeatureType.STYLE_LOCAL,FeatureType.LENGTH_LOCAL,FeatureType.READ_LOCAL,FeatureType.HISTORY_LOCAL,FeatureType.NETWORK_LOCAL);
						
						break;
					case 6:
						objTipoCombinacao = new TipoFeatureCombinacao(FeatureType.STRUCTURE,FeatureType.STYLE,FeatureType.LENGTH,FeatureType.READ,FeatureType.HISTORY,FeatureType.NETWORK);
						break;
					case 3:
						objTipoCombinacao = new TipoFeatureCombinacao(FeatureType.TEXT,FeatureType.HISTORY,FeatureType.NETWORK);
						break;
				}
				
			}else
			{
				//colcoa views genericas
				System.out.println("Atribuindo views genericas....");
				FeatureType[] arrTypesOfFeat = new FeatureType[resultPorViewTreino.length];
				for(int i =0 ; i<resultPorViewTreino.length ; i++)
				{
					View v = resultPorViewTreino[i].getView()[0];
					arrTypesOfFeat[i] = FeatureType.ARR_GENERIC_VIEW[i];
					v.setFeatureType(arrTypesOfFeat[i]);
				}
				objTipoCombinacao = new TipoFeatureCombinacao(arrTypesOfFeat);
			}
		}
		System.out.println("=>>>> Tamanho treino: "+resultPorViewTreino[0].getNumResults());
		System.out.println("=>>>> Tamanho teste: "+resultPorViewTeste[0].getNumResults());
		if(resultPorViewValidacao != null && resultPorViewValidacao.length >0)
		{
			System.out.println("=>>>> Tamanho validacao: "+resultPorViewValidacao[0].getNumResults());
		}
		

		File arqDiretorio =  new File(resultPorViewTeste[idxColecaoCol].getTeste().getParentFile().getParentFile().getAbsolutePath()+"/metaLearning");
		if(!arqDiretorio.exists())
		{
			arqDiretorio.mkdir();
		}
		
		//dados do fold novo
		
		int foldNum = resultPorViewTreino[0].getNum();
		System.err.println("FOLD NUM : "+foldNum);
		
		String nomeBaseArquivo = getDirMetalearning().getAbsolutePath()+"/"+this.getSiglaColecao();
		
		System.out.println("Arquivo base:"+nomeBaseArquivo);
		File arqFonte = new File(nomeBaseArquivo+".fonte");
		File arqTreino = new File(nomeBaseArquivo+foldNum+".treino");
		File arqTreinoIds = new File(nomeBaseArquivo+foldNum+".treinoIds");
		File arqValidacao = new File(nomeBaseArquivo+foldNum+".validacao");
		File arqValidacaoIds = new File(nomeBaseArquivo+foldNum+".validacaoIds");
		File arqTeste = new File(nomeBaseArquivo+foldNum+".teste");
		File arqPageIds = new File(nomeBaseArquivo+foldNum+".pageId");
		
		mapFeatMetaLearnigPredictions = new HashMap<Long,String>();
		//grava texto do meta-treino (colocando o resultado de cada fold) 
		MetodoAprendizado metApView1 = resultPorViewTeste[0].getView()[0].getMetodoAprendizado();

		gerarFold(resultPorViewTreino,arqTreino,arqTreinoIds,false,true,false);
		System.out.println("Criando arquivo fold: "+arqTreino.getAbsolutePath());
		
		
		//se necessario, faz um cross fold no resultado de treino, pega o melhor treino como arquivo para aplicar no teste
		/*
		if(View.tpoDivisao == TIPO_DIVISAO_TREINO.FOLD_VALIDACAO_NOVE_FOLDS_BALANCEADO_TESTE_IGUAL)
		{	
			//arqTreino = executaCrossFoldTreino(resultPorViewTreino,resultPorViewTeste, arqTreino, foldNum);
			File arqOrigemAntigo =  metCombinacao.getArquivoOrigem();
					
			//gera o fonte apenas com o trei		
			File arqTreinoFonte = new File(arqTreino.getAbsolutePath()+".amostraTreino");
			gerarFold(resultPorViewTreino,arqTreinoFonte,null,false,true,false);
			
			metCombinacao.setArquivoOrigem(arqTreinoFonte);
			
			
			//todos os ids)
			List<Long> lstIdsCompleta = new ArrayList<Long>(metCombinacao.getIds(metCombinacao.getArquivoOrigem()));
			
			//excluindo ids teste
			lstIdsCompleta.removeAll(resultPorViewTeste[0].getIdsResultado());
			
			//ids do resultado do teste do treino
			List<Long> lstIds = new ArrayList<Long>(resultPorViewTreino[0].getSubFolds()[0].getIdsResultado());
			
			//ids do treino filtrado
			lstIdsCompleta.removeAll(lstIds);
			List<Long> lstIdsTreino = lstIdsCompleta;
			System.out.println("IDS Treino: "+lstIdsTreino.size());
			
			//System.err.println("Treino amostra: "+resultPorViewTreino[0].getOrigem().getAbsolutePath());
			//pega os ids de treino
			metCombinacao.filtraArquivoPorIds(lstIdsTreino, arqTreinoFonte);
			ArquivoUtil.copyfile(arqTreinoFonte, arqTreino);
			//metCombinacao.filtraIDsArquivo(arqTreinoFonte, arqTreino);
			
			//para testes, faz arquivo com ids
			/*StringBuffer strIds = new StringBuffer();
			for(Long id : lstIds)
			{
				strIds.append(id+"\n");
			}*/
			//ArquivoUtil.gravaTexto(strIds.toString(), new File(arqTreino.getAbsolutePath()+".pageIdsTreino"), false);
			//System.out.println("Arquivo: "+arqTreino.getAbsolutePath()+".pageIdsTreino\n");
			/*
			
			System.out.println("Arquivo de treino do metalearning: "+arqTreino.getAbsolutePath());
			if(foldNum == 0)
			{
				System.err.println("\n\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> VEIO AKIIIIIIIIIIIIIIIIIIIIIII\n\n");
				//metCombinacao.setNomExperimento(metCombinacao.getNomExperimento()+"_Kappa_TamIgualTreino");
				metCombinacao.setNomExperimento(metCombinacao.getNomExperimento()+"_TamIgualTreino");
			}
			
			metCombinacao.setArquivoOrigem(arqOrigemAntigo);
		}
		*/
		
		//cria arquivo validacao (caso exista)
		if(resultPorViewValidacao != null)
		{
			if(resultPorViewValidacao[0] != null)
			{
				gerarFold(resultPorViewValidacao,arqValidacao ,arqValidacaoIds,false,true,true);	
			}
		}else
		{
			arqValidacao = null;
		}
		
		
		//grava texto do meta-teste (colocando o resultado de cada fold)
		gerarFold(resultPorViewTeste,arqTeste ,arqPageIds,false,true,true);
		System.out.println("Criando arquivo fold: "+arqTeste.getAbsolutePath());

		//caso necessario, gera o seletor
		
		if(this.seletorView != null)
		{
			System.out.println("============================================  Criando seletor de visão =========================================================== ");
			Map<Long,String>  mapFeaturesMetalearning = new HashMap<Long,String>();
			Map<Long,Double> mapClassPerInstance = new HashMap<Long,Double>();
			ListaAssociativa<Long, Long> mapRespsPorPerguntaTreino = getFeaturesFromFile(this.metCombinacao,arqTreino,mapFeaturesMetalearning,mapClassPerInstance);
			ListaAssociativa<Long, Long> mapRespsPorPerguntaTeste = getFeaturesFromFile(this.metCombinacao,arqTeste,mapFeaturesMetalearning,mapClassPerInstance);
			ListaAssociativa<Long, Long> mapRespsPorPerguntaValidacao = new ListaAssociativa<Long,Long>();
			if(arqValidacao!=null)
			{
				mapRespsPorPerguntaValidacao = getFeaturesFromFile(this.metCombinacao,arqValidacao,mapFeaturesMetalearning,mapClassPerInstance);
			}
			Map<Long,String> arrMapfeaturesSeletor = this.seletorView.selecionaViews(this.mapFeatMetaLearnigPredictions,mapFeaturesMetalearning,	
																						resultPorViewTreino,resultPorViewTeste,resultPorViewValidacao,this.metCombinacao);
			//System.exit(0);
			//adiciona tudo no dataset
			fazDataset(
					this.metCombinacao,
					arqTreino,arqTreinoIds,
					mapRespsPorPerguntaTreino,
					seletorView.getPerguntaPorResp(),
					mapClassPerInstance,
					new HashMap<Long,Map<Long,String>>(),
					arrMapfeaturesSeletor,
					mapFeaturesMetalearning
					);
			
			fazDataset(
					this.metCombinacao,
					arqTeste,arqPageIds,
					mapRespsPorPerguntaTeste,
					seletorView.getPerguntaPorResp(),
					mapClassPerInstance,
					new HashMap<Long,Map<Long,String>>(),
					arrMapfeaturesSeletor,
					mapFeaturesMetalearning
					);
			if(arqValidacao != null)	
			{
				fazDataset(
						this.metCombinacao,
						arqValidacao,arqValidacaoIds,
						mapRespsPorPerguntaValidacao,
						seletorView.getPerguntaPorResp(),
						mapClassPerInstance,
						new HashMap<Long,Map<Long,String>>(),
						arrMapfeaturesSeletor,
						mapFeaturesMetalearning
						);
			}
			
			
		}
		//System.out.println("--- Apenas seletor e metalearning --- ");
		//System.exit(0);
		//criaSeletorView(arqTreino,resultPorViewTreino,arqTeste,resultPorViewTeste);
		
		//cria um novo fold com o meta-treino e meta- teste	
		Fold foldResult = new Fold(foldNum,arqFonte,arqTreino,arqTeste,arqPageIds);
		if(arqValidacao != null)
		{
			foldResult.setValidationFiles(arqValidacao, arqValidacaoIds);
		}
		
		
		
		
		//calcula o teste e treino
		if(COMPUTAR_METALEARNING)
		{
			computarMetalearning(resultPorViewTreino, resultPorViewTeste,
					foldResult);
		}else
		{
			foldResult.setResultados(resultPorViewTeste[0].getResultadosValues());	
		}
		//
		
		
		///System.exit(0);
		
		gravaTreinoPrimParam(resultPorViewTreino, foldResult);
		
		//imprime indice de cada visão
		if(resultPorViewTreino[0].getNum() == 0 && objTipoCombinacao != null)
		{
			int num = 1;
			for(int i = 0; i<resultPorViewTreino.length ; i++)
			{
				FeatureType objFetType = resultPorViewTreino[i].getView()[0].getFeatureType();
				if(objTipoCombinacao.contains(objFetType))
				{
					
					System.out.println("Indice "+num+": "+objFetType);
					num++;
				}
			}
		}
		
		
		Iterator<ResultadoItem> i = foldResult.getResultadosValues().iterator();
		while(i.hasNext())
		{
			ResultadoItem r = i.next();
			mConfusao.novaPredicao((int)Math.round(r.getClassePrevista()),(int)Math.round(r.getClasseReal()));
		}
		
		// TODO Auto-generated method stub
		return foldResult;
	}
	public void computarMetalearning(Fold[] resultPorViewTreino,
			Fold[] resultPorViewTeste, Fold foldResult) throws Exception,
			IOException {
		//computa os parametros se necessario
		List<Tupla<Tupla<Map<String, String>, Map<String, String>>,ValorResultado>> lstParamOrederedPerBestResult = null;
		/*if(resultPorViewTeste[0].getNum() == 0)
		{
			System.out.println("===== Calculando parametros do metalearning ====");
			if(this.metCombinacao instanceof GenericoSVMLike)
			{
				if( (((GenericoSVMLike)metCombinacao).getNomeMetodo().equalsIgnoreCase("SVM")) || 	((GenericoSVMLike)metCombinacao).getNomeMetodo().equalsIgnoreCase("SVMRank"))
				{
					Tupla<GenericoSVMLike,List<Tupla<Tupla<Map<String, String>, Map<String, String>>,ValorResultado>>> tplParamMetAp  = ParamUtilSVMFilter.computeViewParameterSVM(foldResult.getSubFolds(),foldResult.getTreino(),(GenericoSVMLike) this.metCombinacao,true,getNumQueries(resultPorViewTreino),resultPorViewTreino.length);
					this.metCombinacao = tplParamMetAp.getX();
					lstParamOrederedPerBestResult = tplParamMetAp.getY();
					//this.metCombinacao = ParamUtilSVMFilter.getParameterDefaultWikipedia(0,(GenericoSVMLike) this.metCombinacao);
				}
			}else
			{
				this.metCombinacao.treinaParametros(resultPorViewTeste[0].getTreino());
			}
		}*/
		
		//realiza o metalearning para este fold
		
		//boolean isSVMRank = lstParamOrederedPerBestResult != null && this.metCombinacao instanceof GenericoSVMLike && ((GenericoSVMLike)metCombinacao).getNomeMetodo().equalsIgnoreCase("SVMRank");
		boolean isSVMRank = false;
		if(isSVMRank)
		{
			((GenericoSVMLike)this.metCombinacao).setTimeout(30000);
		}
		boolean endedWithTimeout = false;
		do
		{
			endedWithTimeout = false;	
			Exception exp = null;
			try {

				foldResult.setResultados(this.metCombinacao.testar(foldResult));
				
				
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				exp = e;
			}
			
			if(((GenericoSVMLike)this.metCombinacao).endedWithTimeout() && lstParamOrederedPerBestResult != null)
			{
				endedWithTimeout = true;
				//o primeiro eh o atual, remove ele
				lstParamOrederedPerBestResult.remove(0);
				
				//tenta rodar com o melhor anterior
				Tupla<Map<String,String>,Map<String,String>> mapParamNew = lstParamOrederedPerBestResult.get(0).getX();
				this.metCombinacao = new GenericoLetorLike(((GenericoSVMLike)this.metCombinacao).getNomeMetodo(),mapParamNew.getX(),mapParamNew.getY());
				((GenericoSVMLike)this.metCombinacao).setTimeout(30000);
				
				//limpa os resultados dos folds (se existiu)
				foldResult.limpaResultados();

			}else
			{
				if(exp != null)
				{
					throw exp;
				}
			}
		}
		while(isSVMRank &&  endedWithTimeout);
		
		
	}
	public int getNumQueries(Fold[] resultPerView)
	{
		Set<Long> setQueryIds = new HashSet<Long>();
		for(ResultadoItem ri : resultPerView[0].getResultadosValues())
		{
			setQueryIds.add(ri.getQID());
		}
		return setQueryIds.size();
	}
	/**
	 * Seletor de views, a classe será as visoes.
	 * Caso seja classificacao, a classe alvo sera a visao com que acertou com maior confianca 
	 * caso nenhuma tenha acertado, tera uma classe para isso
	 * Caso seja regressao, a classe alvo sera a visao que obteve um maior acerto
	 * @param arqMetaLearning
	 * @param resultPorView
	 * @throws IOException 
	 */
	private void criaSeletorView(File arqMetaLearning, Fold[] resultPorView,File arqSeletor,File arqPageIds) throws IOException
	{
		
		BufferedWriter outSeletor = new BufferedWriter(new FileWriter(arqSeletor, false));
		BufferedWriter outSeletorIds = new BufferedWriter(new FileWriter(arqPageIds, false));
		
		BufferedReader in = new BufferedReader(new FileReader(arqMetaLearning));
		String strLinhaMetaLearning;
		StringBuilder texto = new StringBuilder();
		while ((strLinhaMetaLearning = in.readLine()) != null)
		{
			//resgata os dados da instancia no 2o nivel
			Integer id = this.metCombinacao.getIdPorLinhaArquivo(strLinhaMetaLearning);
			String strFeatures = this.metCombinacao.getFeaturesString(strLinhaMetaLearning);
			double classeReal = Double.parseDouble(this.metCombinacao.getClasseReal(strLinhaMetaLearning));
			
			
			/*procua a classe alvo do seletor 
			 * Caso seja classificacao, a classe alvo sera a visao com que acertou com maior confianca 
			 * caso nenhuma tenha acertado, tera uma classe para isso
			 * Caso seja regressao, a classe alvo sera a visao que obteve um maior acerto
			 */
			int classeAlvoSeletor =  resultPorView.length;
			double minDistClasseReal = Double.MAX_VALUE;
			double maxProbClasseReal = 0;
			for(int v = 0 ; v < resultPorView.length ; v++)
			{
				ResultadoItem r = resultPorView[v].getResultadoPorId(id.longValue());
				
				if(!this.metCombinacao.isClassificacao())
				{
					//distancia da classe real pela regressao das classe prevista com a real
					double distClasseReal = Math.abs(r.getClassePrevista()-r.getClasseReal());
					if(minDistClasseReal > distClasseReal)
					{
						minDistClasseReal = distClasseReal;
						classeAlvoSeletor = v;
					}
				}else
				{
					if(r.getClassePrevista() == classeReal) 
					{
						if(r.getConfianca()>maxProbClasseReal)
						{
							maxProbClasseReal = r.getConfianca();
							classeAlvoSeletor = v;
						}
					}
				}

				//grava linha com resultado e ids
				outSeletor.write(classeAlvoSeletor+" "+strFeatures);
				outSeletorIds.write(id);
			}
		}
		
		//out.close();
//		//in.close();
	}
	/**
	 * faz um cross fold no resultado de treino, pega o melhor treino como arquivo para treino do metalearning
	 * @param resultPorViewTreino
	 * @param resultPorViewTeste
	 * @param foldNum
	 * @return
	 * @throws IOException
	 * @throws SQLException
	 * @throws Exception
	 */
	private File executaCrossFoldTreino(Fold[] resultPorViewTreino,	Fold[] resultPorViewTeste,File arqTreino, int foldNum) throws IOException,
			SQLException, Exception {
		
		File arqFonteAntigo = metCombinacao.getArquivoOrigem(); 
		String nomExperimentoAntigo = metCombinacao.getNomExperimento();
		
		//gera arqTreino com ids para ser definido como  fonte
		File arqTreinoFonte = new File(arqTreino.getAbsolutePath()+".amostra");
		gerarFold(resultPorViewTreino,arqTreinoFonte,null,false);
		
		//o fonte é o treino completo
		metCombinacao.setArquivoOrigem(arqTreinoFonte);
		
		//cria o fold com o cross fold desejado
		int tamAmostra = resultPorViewTreino[0].getNumResults()+resultPorViewTeste[0].getNumResults();
		List<IdClass> lstIdClass = new ArrayList();
		for(ResultadoItem objResult : resultPorViewTreino[0].getResultadosValues())
		{
			lstIdClass.add(new IdClass(objResult.getId(), Double.toString(objResult.getClasseReal())));
		}

		Fold[] foldsTreinoFiltrado = metCombinacao.dividePerFoldsByIdsClasse(lstIdClass, "_"+metCombinacao.getNomExperimento()+"_validation_"+foldNum, "foldsTreinoMetaLearning/fold"+foldNum,9, (int) (tamAmostra/(double)10));
		
		//faz um cross fold validation
		
		System.out.println("Fazendo cross validação no treino....");
		metCombinacao.setNomExperimento("_"+metCombinacao.getNomExperimento()+"_validation_"+foldNum);
		foldsTreinoFiltrado = metCombinacao.testar(foldsTreinoFiltrado);
		
		
		//verifica dos folds, o fold zero. Este será o fold de treino do metalerning
		Fold foldMin = foldsTreinoFiltrado[0];//Fold.getMinMSEFold(foldsTreinoFiltrado);

		//volta rtesultados anteriores
		metCombinacao.setArquivoOrigem(arqFonteAntigo);
		metCombinacao.setNomExperimento(nomExperimentoAntigo);
		
		return foldMin.getTreino();
	}

	public static File getFileNameParam(File dirParamPrimTreino,ConfigViewColecao cnf,TipoFeatureCombinacao objTipoCombinacao)
	{
		String nomArquivo =cnf.getColecao().getSigla() ;;

		int iniPrioDepoisUm = 2;
		int lastPrio = 6;
		switch(cnf)
		{
			case WIKIPEDIA_CULTURE:
				nomArquivo += "_culture";
				iniPrioDepoisUm = 4;
				lastPrio = 7;
				break;
			case WIKIPEDIA_GEOGRAPHY:
				nomArquivo += "_geography";
				iniPrioDepoisUm = 4;
				lastPrio = 7;
				break;
			case WIKIPEDIA_HISTORY:
				nomArquivo += "_history";
				iniPrioDepoisUm = 4;
				lastPrio = 7;
				break;
			case WIKIPEDIA_SCIENCE:
				nomArquivo += "_science";
				iniPrioDepoisUm = 4;
				lastPrio = 7;
				break;
		}
		if(objTipoCombinacao.getNumViewsGlobal() != 0)
		{
			iniPrioDepoisUm = 2;
			lastPrio = 6;
		}
		String nomDir = nomArquivo;
		
		//define o nome de acordo com o tipo de combinação e da prioridade de acordo com a combinação
		if(objTipoCombinacao != null)
		{
			nomArquivo += "_"+objTipoCombinacao.getName();
		
			//prioridades: todos depois (5/11 e 1 - 7 (um local e 6 global)) isolados depois de texto e depois o resto
			if(objTipoCombinacao.getNumViewsGlobal() == 6 && objTipoCombinacao.getNumViewsLocal() == 0
					|| objTipoCombinacao.getNumViewsGlobal() == 6 && objTipoCombinacao.getNumViewsLocal() == 6)
			{
				nomArquivo = "1_"+nomArquivo;
			}else
			{
				if( (objTipoCombinacao.getNumViewsGlobal() == 5 && objTipoCombinacao.getNumViewsLocal() == 0) ||
					(objTipoCombinacao.getNumViewsGlobal() == 6 && objTipoCombinacao.getNumViewsLocal() == 5) ||
					(objTipoCombinacao.getNumViewsGlobal() == 6 && objTipoCombinacao.getNumViewsLocal() == 1) ||
					(objTipoCombinacao.getNumViewsGlobal() == 1)
					)
						
				{
					nomArquivo = iniPrioDepoisUm+"_"+nomArquivo;	
				}else
				{
					if(objTipoCombinacao.isOnlyTextual() || (objTipoCombinacao.getNumViewsLocal() >= 1 && objTipoCombinacao.isOnlyTextualLocal()))
					{
						nomArquivo = (iniPrioDepoisUm+1)+"_"+nomArquivo;	
					}else
					{
						nomArquivo = lastPrio+"_"+nomArquivo;
						
					}
				}
					
			}
		}
		File subDirColecao = new File(dirParamPrimTreino,nomDir);
		if(!subDirColecao.exists())
		{
			subDirColecao.mkdirs();
			
		}
		return new File(subDirColecao,nomArquivo+".out");
		
	}
	public File getFileParam(Fold[] resultPorViewTreino,
			Fold foldResult)
	{
			
			return getFileNameParam(dirParamPrimTreino,resultPorViewTreino[0].getCnfViewColecao(),objTipoCombinacao);
		
	}
	private void gravaTreinoPrimParam(Fold[] resultPorViewTreino,Fold foldResult) {
		
		if(dirParamPrimTreino != null && foldResult.getNum() == 0)
		{
			File arquivoFinal = getFileParam(resultPorViewTreino, foldResult);
			System.out.println("Gravado para parametros: "+arquivoFinal.getAbsolutePath());
			File arquivoTreino = foldResult.getTreino();
			ArquivoUtil.copyfile(arquivoTreino, arquivoFinal);
		}
		
	}
	
	private boolean normClasse = true;
	private void gerarFold(Fold[] resultPorView,File arquivo,File arqPageIds,boolean append) throws Exception
	{
		 gerarFold(resultPorView,arquivo,arqPageIds,append,false,false);
	}
	/*
	private void gerarFold(Fold[] resultPorView,File arquivoSaida,File arqPageIds,boolean append,boolean gerarId,boolean isTeste)
	{
		gerarFold( resultPorView, arquivoSaida, arqPageIds, append, gerarId, isTeste, null);
	}
	*/
	private void gerarFold(Fold[] resultPorView,File arquivoSaida,File arqPageIds,boolean append,boolean gerarId,boolean isTeste) throws Exception
	{
		//if(this.metCombinacao.getColecao() != null && resultPorView.length > 0)
		if(utilizarFeaturesSet && this.metCombinacao.getMapIdPorLinha().keySet().size()==0 && this.objViewCreatorHelper == null)
		{
			File origem = this.metCombinacao.getArquivoOrigem();
			this.metCombinacao.mapeiaIdPorLinha(origem);
		}
		
		
		//System.out.println("ArquivoFolds: "+arquivo.getAbsolutePath()+" Arquivo IDs: "+arqPageIds.getAbsolutePath());
		FeatureNormalizer[] normViews = null;
		if(normClasse)
		{
		    
			normViews = new FeatureNormalizer[resultPorView.length];
			for(int i =0; i<normViews.length ; i++)
			{
					//System.out.println("oioi");
				Colecao col = resultPorView[i].getView()[0].getColecaoDatasetView();
				if(col == null)
				{
					col = resultPorView[i].getView()[0].getColecao();
				}
				if(col != null)
				{
					normViews[i] = new FeatureNormalizer(col.getMinMaxClass());
				}else
				{
					normViews[i] = new FeatureNormalizer(objMinMaxClasse);
				}
			}
		}
		
		StringBuffer strArquivo = new StringBuffer();
		StringBuffer strArquivoIds = new StringBuffer();
		/*
		System.out.println("**************VIEW 1********************");
		for(ResultadoItem rItem : resultPorView[0].getResultadosValues())
		{
			System.out.println(rItem);
		}
		
		System.out.println("**************VIEW 2********************");
		for(ResultadoItem rItem : resultPorView[1].getResultadosValues())
		{
			System.out.println(rItem);
		}
		*/
		
		//cria por linha
		boolean ignoraLinha = false;
		int numResults = resultPorView[0].getNumResults();
		
		
		ConfigViewColecao cnfAtual =resultPorView[0].getCnfViewColecao();
		List<ConfigViewColecao> lstCnfViewFilhas = new ArrayList<ConfigViewColecao>();
		Map<ConfigViewColecao,Integer> idxPorCnfFilhas = new HashMap<ConfigViewColecao, Integer>();
		int numFilhas = 0;
		//mapeia as filhas caso necessario
		if(cnfAtual != null)
		{

			lstCnfViewFilhas = cnfAtual.getLstCnfViewFilhas();
			Iterator<ConfigViewColecao> iCnfView = lstCnfViewFilhas.iterator();
			numFilhas = lstCnfViewFilhas.size();
	
			int i =0 ;
			while(iCnfView.hasNext())
			{
				ConfigViewColecao cnf = iCnfView.next();
				idxPorCnfFilhas.put( cnf, i);
				//System.out.println((i*3+4)+","+(i*3+5)+","+(i*3+6)+": "+cnf);
				i++;
			}
		}
		Map<Long,Integer>[] arrFoldsRankPerResultId = new Map[resultPorView.length]; 
		//imprime resultado
		//System.out.println("COMBINAÇÂO:::::::::::::: "+objTipoCombinacao);
		//resgata o resultado da primeira visao (para ser a ordem de resgate dos resultados)
		List<ResultadoItem> lstResults = resultPorView[0].getResultadosValues();
		
		
		
		//ordena resultados por uma ordem aleatoria fixa (caso nao seja ranksvm) caso seja, eh pela ordem de seus qids
		
			final Map<Long,Integer> mapInstancePosIdx = gerarOrdemIdsFile(lstResults);
			final MetodoAprendizado metAtualFinal = this.metCombinacao;
			Collections.sort(lstResults,new Comparator<ResultadoItem>() {
	
				@Override
				public int compare(ResultadoItem o1, ResultadoItem o2) {
					
					if(metAtualFinal instanceof GenericoLetorLike && ((GenericoLetorLike)metAtualFinal).getNomeMetodo().equals("SVMRank") )
					{
						return (int) (o1.getQID()-o2.getQID());
					}
					
					//Se nao for svmrank, retorna a ordem fixa aleatoria
					Integer idxO1 = mapInstancePosIdx.get(o1.getId());
					Integer idxO2 = mapInstancePosIdx.get(o2.getId());
					
					if(idxO1 != null && idxO2 != null)
					{
						return idxO1-idxO2;
					}else
					{
						System.err.println("Nao foi possivel achar a ordem do id: "+(idxO1 == null?o1.getId():o2.getId()));
						return 0;
					}
					
				}
				
			});
		
		
		for(int r = 0; r<numResults ; r++)
		{
			String classeReal = "";
			String features = "";
			long id = -1;
			long idResult = lstResults.get(r).getId();
			int foldNumResult = lstResults.get(r).getFold().getNum();
			Long qid = lstResults.get(r).getQID();
			if(idResult == 6320532L)
			{
			//	System.out.println("OI!");
			}
			//prepara array de features por filha
			int[] arrPertenceView = new int[numFilhas];
			if(utilizarFeatPertenceView)
			{
				for(int j = 0 ; j<arrPertenceView.length ; j++)
				{
					arrPertenceView[j] = 0;
				}
					
			}
			//armazena views globais em um hashmap
			Integer idxText = null;
			Integer idxRead = null;
			Integer idxTam = null;
			Integer idxStyle = null;
			Integer idxStruct = null;
			Integer idxGrafo = null;
			Integer idxHist = null;
			Double[] arrClsPrevistaGlobal = new Double[objTipoCombinacao == null?12:objTipoCombinacao.getNumViewsGlobal()+objTipoCombinacao.getNumViewsLocal()];
			
			double[] arrPredictions = new double[objTipoCombinacao == null?12:objTipoCombinacao.getNumViewsGlobal()+objTipoCombinacao.getNumViewsLocal()];
			
			//System.out.println("RESULT VIEW 1: "+lstItens0.size()+" Result view 2:"+lstItens1.size());
			int numFeature = 1;
			int viewIdx = 0;
			for(int v = 0 ; v<resultPorView.length; v++)
			{
				
				View objView = resultPorView[v].getView()[0];
				FeatureType objTypeFeature = objView.getFeatureType();
				
				//ignora visão se ela nao fizer parte da combinacao
				if(objTipoCombinacao != null && !this.objTipoCombinacao.contains(objTypeFeature))
				{
					continue;
				}
				

				
				
				//adiciona os hashs para serem usados nas views locais se necessario
					if(objTypeFeature == FeatureType.TEXT)
					{
						idxText = v;
					}
					if(objTypeFeature == FeatureType.READ)
					{
						idxRead = v;
					}
					if(objTypeFeature == FeatureType.LENGTH)
					{
						idxTam = v;
					}
					if(objTypeFeature == FeatureType.STYLE)
					{
						idxStyle = v;
					}
					if(objTypeFeature == FeatureType.STRUCTURE)
					{
						idxStruct = v;
					}
					
					if(objTypeFeature == FeatureType.NETWORK)
					{
						idxGrafo = v;
					}
					if(objTypeFeature == FeatureType.HISTORY)
					{
						idxHist = v;
					}
					
				
				//cria features, classe real
				//System.out.println("View: "+v+" ID: "+idResult);
				ResultadoItem result =  resultPorView[v].getResultadoPorId(idResult);
				
				if(ignoraLinhaSemResult && result == null)
				{
					ignoraLinha = true;
				}
				if(result != null)
				{									//prepara a classe prevista e a armazena como resultado
					double classePrevista = result.getClassePrevista();
					classeReal = Double.toString(result.getClasseReal());
					/*
					if(normViews!=null && normViews.length>0  && objMinMaxNorm != null && objMinMaxFeature != null)
					{
						classePrevista = normViews[v].normValue(classePrevista, objMinMaxNorm);
						//System.out.println("View: "+v+" classe prevista: "+result.getClassePrevista()+" Classe prevista norm: "+classePrevista);
					}
					*/
					if(objTypeFeature != null)
					{
						if((!objTypeFeature.isLocal()) && !ignoraLinhaSemResult)
						{
							arrClsPrevistaGlobal[v] = classePrevista;
							
						}
					}
					if(!ignoraLinha)
					{
						arrPredictions[viewIdx] = result.getClassePrevista();;
					}
					
					//armazena feature da classe	
					features += " "+numFeature+":"+classePrevista;//result.getClassePrevista();
					numFeature++;
					
					
					//feature da confiança, se necessario
					if(this.utilizarConfianca && objView.getMetodoAprendizado().isClassificacao())
					{
						features += " "+numFeature+":"+result.getConfianca();
						numFeature++;
					}
					if(this.utilizarConfiancaArray)
					{
						float[] arrProbPorClasse = result.getProbPorClasse();
						//para cada classe existente, verificar sua confianca (se existir, caso contrario é zero)
						for(int j=0 ;j<arrProbPorClasse.length ; j++)
						{
							features += " "+numFeature+":"+arrProbPorClasse[j];
							numFeature++;							
						}
						
					}
					
					//utilizar rankPOs?
					if(this.utilizarRankPos)
					{
						//povoa o map do rank per result id
						if(arrFoldsRankPerResultId[v] == null)
						{
							
							arrFoldsRankPerResultId[v] =  resultPorView[v].getRankPorResultId();
						}
						if(arrFoldsRankPerResultId[v].containsKey(idResult))
						{
							features += " "+numFeature+":"+arrFoldsRankPerResultId[v].get(idResult);
						}
						numFeature++;
					}
					
					//verifica se o id esta ok
					if(id == -1)
					{
						id = result.getId();
					}else
					{
						if(id != result.getId())
						{
							throw new Exception("Erro: ID de views incompativeis");
						}
					}
					

				}
				
				//adiciona a view caso necessario
				ConfigViewColecao cnfViewColecaoResult = resultPorView[v].getCnfViewColecao();
				if(cnfViewColecaoResult != null && idxPorCnfFilhas.containsKey(cnfViewColecaoResult) && !ignoraLinha)
				{
					int idxCnf = idxPorCnfFilhas.get(cnfViewColecaoResult);
					arrPertenceView[idxCnf] =  (result != null)?1:0;
					
					//caso seja a filha, e ela nao existe, zerar a features
					if(result == null)
					{

						double classePrevista = 0;
						
						//armazena feature da classe
						if(resultPorView[v].getView()[0].getFeatureType() == FeatureType.TEXT_LOCAL)
						{
							classePrevista = arrClsPrevistaGlobal[idxText];
						}
						if(resultPorView[v].getView()[0].getFeatureType() == FeatureType.READ_LOCAL)
						{
							classePrevista = arrClsPrevistaGlobal[idxRead];
						}
						if(resultPorView[v].getView()[0].getFeatureType() == FeatureType.LENGTH_LOCAL)
						{
							classePrevista = arrClsPrevistaGlobal[idxTam];
						}
						if(resultPorView[v].getView()[0].getFeatureType() == FeatureType.STRUCTURE_LOCAL)
						{
							classePrevista = arrClsPrevistaGlobal[idxStruct];
						}
						if(resultPorView[v].getView()[0].getFeatureType() == FeatureType.STYLE_LOCAL)
						{
							classePrevista = arrClsPrevistaGlobal[idxStyle];
						}
						
						if(resultPorView[v].getView()[0].getFeatureType() == FeatureType.NETWORK_LOCAL)
						{
							classePrevista = arrClsPrevistaGlobal[idxGrafo];
						}
						
						if(resultPorView[v].getView()[0].getFeatureType() == FeatureType.HISTORY_LOCAL)
						{
							classePrevista = arrClsPrevistaGlobal[idxHist];
						}
						features += " "+numFeature+":"+classePrevista;//result.getClassePrevista();

						//features += " "+numFeature+":0";//result.getClassePrevista();
						numFeature++;
					}
					/*
					if(this.utilizarConfianca)
					{
						//armazena feature da classe	
						features += " "+numFeature+":0";//result.getClassePrevista();
						numFeature++;	
					}
					*/
				}
				
				viewIdx++;
			}
			String featJustPredictions = features;
			if(utilizarFeatPertenceView)
			{
				for(int j = 0 ; j<arrPertenceView.length ; j++)
				{
					features += " "+numFeature+":"+arrPertenceView[j];
					numFeature++;
				}
					
			}

			//utilizar kappa?
			Kappa k = null;
			if(this.metCombinacao.getColecao() != null)
			{
				k =calculaPaKappa(arrPredictions, this.metCombinacao.getColecao().getMinMaxClass());
				if(utilizarFeatKappa)
				{
					
					
					features += " "+numFeature+":"+k.getP();
					
					/*
					for(int i = 0; i<arrPredictions.length; i++)
					{
						MinMax objMinMax = this.metCombinacao.getColecao().getMinMaxClass();
						if(arrPredictions[i] > objMinMax.getMax())
						{
							arrPredictions[i] = objMinMax.getMax();
						}
						if(arrPredictions[i] < objMinMax.getMin())
						{
							arrPredictions[i] = objMinMax.getMin();
						}
						features += " "+numFeature+":"+(arrPredictions[i]/(objMinMax.getMax()-objMinMax.getMin()+1));
						numFeature++;
					}
					*/
					
					
	
				}
			}
			if(isTeste && GRAVAR_KAPPA)
			{
				
				gravarKappa((int)id, metCombinacao.getNomExperimento(), k);
				
			}
			
			//verifica se é necessário criar as features do artigo
			if(utilizarFeaturesSet)
			{
				String[] featVector = null; 
				if(this.objViewCreatorHelper == null)
				{
					featVector = this.metCombinacao.getFeatVectorLinhaMapeada(idResult);
				}else
				{
					
					featVector = new String[this.lstFeaturesIdxToInclude.size()];
					int idxLocalFeat = 0;
					for(Integer idxFeat : this.lstFeaturesIdxToInclude)
					//for(Integer idxFeat =1  ; idxFeat<=183 ;idxFeat++)
					{
						featVector[idxLocalFeat] = this.objViewCreatorHelper.getFeatureVal(foldNumResult, isTeste?null:0, (long)id, idxFeat);
						idxLocalFeat++;
					}
				}
				features += " "+this.metCombinacao.gerarLinhaFeatures(featVector, numFeature);
			}
			//qid
			String strAntesFeatures = "";
			String commentQID = "";
			if(metCombinacao instanceof GenericoLetorLike)
			{
				strAntesFeatures = " qid:"+qid;
				commentQID = " @qid:"+qid;
			}
			if(!ignoraLinha)
			{
				if(gerarId)
				{
					strArquivo = strArquivo.append(StringUtil.removeDoubleSpace(classeReal+strAntesFeatures+" "+features+" #id: "+id+commentQID+"\n"));
					

				}else
				{
					strArquivo = strArquivo.append(classeReal+strAntesFeatures+" "+features+"\n");
					
				}
				
				//coloca tb soh as predicoes no hashmap
				mapFeatMetaLearnigPredictions.put(id, StringUtil.removeDoubleSpace(classeReal+strAntesFeatures+" "+featJustPredictions+" #id: "+id+commentQID));
				
			}

			
			
			strArquivoIds = strArquivoIds.append(id+"\n");
		}

		
		//grava
		if(!arquivoSaida.getParentFile().exists())
		{
			arquivoSaida.getParentFile().mkdirs();
		}
		ArquivoUtil.gravaTexto(strArquivo.toString(), arquivoSaida, append);
		if(metCombinacao instanceof SVM && !(metCombinacao instanceof GenericoSVMLike) && !(metCombinacao instanceof GenericoLetorLike))
		{
			if(GRAVA_SCALE && gerarId)
			{
				DatasetUtil.calculaScale(arquivoSaida.toString(),arquivoSaida.toString());	
			}
			
		}
		
		
		if(arqPageIds != null)
		{
			ArquivoUtil.gravaTexto(strArquivoIds.toString(), arqPageIds, append);
		}
		
		
		
	}
	/**
	 * Gera a ordem de exibicao das linhas no arquivo 
	 * Coloca tudo em ordem crescente e depois retorna os ids de forma aleatoria
	 * pageid => pos_idx
	 * @param arq
	 * @return
	 * @throws IOException 
	 */
	private Map<Long,Integer> gerarOrdemIdsFile(List<ResultadoItem> rVals) throws IOException {
		//ordena de forma crescente
		List<Long> lstIds = new ArrayList<Long>();
		for(ResultadoItem ri : rVals)
		{
			lstIds.add(ri.getId());
		}
		Collections.sort(lstIds);
		
		//recria em ids
		lstIds = Fold.divideIntoFolds(1,lstIds, lstIds.size())[0];
		
		//mapeia os ids da forma que estah agora
		Map<Long,Integer> mapPageIdIdx = new HashMap<Long,Integer>();
		
		for(int idx =0; idx<lstIds.size() ; idx++)
		{
			mapPageIdIdx.put(lstIds.get(idx), idx);
		}
		
		
		return mapPageIdIdx;
	}
	private Map<Long,Integer> gerarMapIdxFromDatasetFile(File arq,MetodoAprendizado metAp) throws IOException {
		
		Map<Long,Integer> mapIntLong = new HashMap<Long,Integer>();
		
		BufferedReader in = new BufferedReader(new FileReader(arq));
		String str;
		int idxPos  = 0;

		while ((str = in.readLine()) != null)
		{
			mapIntLong.put(metAp.getIdPorLinhaArquivo(str, "id").longValue(), idxPos);
			idxPos ++;
		}
		in.close();
		return mapIntLong;
	}
	public static MetaLearning criaAbordagemSVM(boolean gravarNoBanco,ConfigViewColecao colecao,String nomExperimento) throws IOException
	{
		TIPO_CONTEUDO_DATASET[] tpoConteudoDatasetML = {};
		return new MetaLearning(new SVM(nomExperimento+"_metaLearning",ConfigViewColecao.getEPSLON(),ConfigViewColecao.MODE,true,gravarNoBanco),tpoConteudoDatasetML);	
	}
	public static void main(String[] args)
	{
		try {
			ConfigViewColecao cViewColecao = ConfigViewColecao.MUPPETS;
			String nomExperimento = "wikiMultiview_"+cViewColecao.getColecao().getSigla();
			MetaLearning abComb = criaAbordagemSVM(true,cViewColecao,nomExperimento);
			
			View[] vs= cViewColecao.getViews(nomExperimento, true);
			Combinador c = new Combinador("Combinacao-"+nomExperimento,abComb,vs);
			c.executaCombinacao(false,"teste");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public MatrizConfusao getMatrizCombinacao() {
		// TODO Auto-generated method stub
		return this.mConfusao;
	}
	
	

}
