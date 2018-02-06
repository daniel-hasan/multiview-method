package scriptsUtil;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import stuctUtil.ArrayUtil;
import apredizadoCombinacao.Combinador;
import apredizadoCombinacao.MetaLearning;
import apredizadoCombinacao.SeletorViews;
import apredizadoCombinacao.MetaLearning.TIPO_CONTEUDO_DATASET;
import apredizadoCombinacao.ViewCnf;
import aprendizadoResultado.CNFSimples;
import aprendizadoResultado.ResultadoFromCnfSimples;
import aprendizadoResultado.ValorResultado.MetricaUsada;
import aprendizadoUtils.GenericoLetorLike;
import aprendizadoUtils.GenericoSVMLike;
import aprendizadoUtils.KNN;
import aprendizadoUtils.MetodoAprendizado;
import aprendizadoUtils.SVM;
import arquivo.ArquivoUtil;
import banco.GerenteBD;
import config_tmp.ConfigCustoGama;
import config_tmp.MinMax;
import entidadesAprendizado.Fold;

import entidadesAprendizado.ResultadoItemDelayedRecord;
import entidadesAprendizado.ResultadoViews;
import entidadesAprendizado.View;
import entidadesAprendizado.View.FeatureType;

public class CombinaClassificacaoViews
{
	private static PreparedStatement stmtExists = null; 
	public static List<View> getFoldsIniciaisCNF(File dirCnf,List<CNFSimples> lstViewCNF,int numFolds,boolean utilizarConfianca,int SVM_MODE,String grouperId,boolean doUndersampling) throws Exception
	{
		List<View> lstViews = new ArrayList<View>();
		List<List<Fold>> lstFoldsPorView = new ArrayList<List<Fold>>();
		for(CNFSimples viewCNF : lstViewCNF)
		{
			//instancia o meto do de aprendizado
			GenericoSVMLike metAprendizado = null;
			Fold[] arrFolds = null;
			if(viewCNF.isLetor())
			{
				metAprendizado = ResultadoFromCnfSimples.instanciaLetor(viewCNF.getMetodo(),  viewCNF.getParamTeste(), viewCNF.getParamTeste(), viewCNF.getLstParams(), viewCNF.getNomExperimento());
				//cria os folds a partir do dataset
				arrFolds = metAprendizado.criaFoldsTeste(viewCNF.getDataset(), numFolds, true,	"id", grouperId, false);
				metAprendizado.setMode(SVM.MODE_REGRESSION);
			}else
			{
				metAprendizado = ResultadoFromCnfSimples.instanciaSVM(viewCNF.getMetodo(),  viewCNF.getParamTeste(), viewCNF.getParamTeste(), viewCNF.getLstParams(), viewCNF.getNomExperimento());
				//cria os folds a partir do dataset
				arrFolds = metAprendizado.criaFoldsTeste(viewCNF.getDataset(), numFolds, false,	"id", grouperId, false);
				metAprendizado.setMode(SVM_MODE);
			}

			
			
			
			metAprendizado.setDirExecucaoComando(dirCnf.getAbsolutePath());
			metAprendizado.setGerarProbabilidade(utilizarConfianca);
			
			metAprendizado.setUsarModeloExistente(false);
			
			//define o array de folds 
			
			
			View v = new View(null,arrFolds[0].getOrigem(),metAprendizado);
			v.setFold(arrFolds);
			
			lstFoldsPorView.add(new ArrayUtil<Fold>().toList(arrFolds));
			v.setFeatureType(FeatureType.ARR_GENERIC_VIEW[viewCNF.getNumView()]);
			lstViews.add(v);
		}
		//faz undersampling se necessario
		if(doUndersampling)
		{
			//para cada fold, filtra o treino diminuindo o numero de ids
			View v = lstViews.get(0);
			MetodoAprendizado mView = v.getMetodoAprendizado();
			for(int foldNum = 0; foldNum<numFolds ; foldNum++)
			{
				List<Long> idsUnderSampling = UnderSampleTrain.getIdsUnderSample(mView, lstFoldsPorView.get(0).get(foldNum).getTreino());
				
				//para cada visao, muda o arquivo treino dela para este novo
				for(int idxView = 0 ; idxView < lstFoldsPorView.size() ; idxView++)
				{
					List<Fold> lstFold = lstFoldsPorView.get(idxView);
					File arqTreino = lstFold.get(foldNum).getTreino();
					UnderSampleTrain.gravaTreinoUndersampled(arqTreino, new File(arqTreino.getAbsolutePath()+".undersampled"), lstViews.get(idxView).getMetodoAprendizado(), idsUnderSampling);
					lstFold.get(foldNum).setTreino(new File(arqTreino.getAbsolutePath()+".undersampled"));
				}
			}
			
			
			
		}
		
		
		return lstViews;
	}
	public static List<View> getFoldsIniciais(File dirCnf,List<ViewCnf> lstViewCNF,int numFolds,Integer numClasses,boolean utilizarConfianca,boolean getResult,int SVM_MODE) throws Exception
	{
		
		
		//para cada diretorio (view)
		List<View> lstViews = new ArrayList<View>();

		for(ViewCnf viewCNF : lstViewCNF)
		{
			File dirView = viewCNF.getDiretorio();
			if(dirView.isDirectory())
			{
				Fold[] folds = getFoldsDiretorio(dirView, numFolds, getResult);
				
				
				MetodoAprendizado objMetodoAp = new SVM(SVM_MODE);
				
				boolean isKNN = false; 
				if(viewCNF.getClassificador() == CLASSIFICADOR.ESPECIFICAR)
				{
					objMetodoAp = new GenericoSVMLike(viewCNF.getCmdTreino(),viewCNF.getParamTreino(),viewCNF.getCmdTeste(),viewCNF.getParamTeste());
					((GenericoSVMLike)objMetodoAp).setDirExecucaoComando(dirCnf.getAbsolutePath());
					((GenericoSVMLike)objMetodoAp).setGerarProbabilidade(utilizarConfianca);
					((GenericoSVMLike)objMetodoAp).setMode(SVM_MODE);
					((GenericoSVMLike)objMetodoAp).setUsarModeloExistente(false);
				}
				else
				{
					if(objMetodoAp.foldMatchesFormat(folds[0]))
					{
						SVM objSVM = (SVM) objMetodoAp;
						
						objSVM.setGerarProbabilidade(utilizarConfianca);
						objSVM.setParametros(viewCNF.getCnfCustoGama());
						objSVM.setGetResultPreCalculado(getResult);
						System.out.println("------  Visão no dir: "+folds[0].getTreino().getParentFile().getAbsolutePath()+" é SVM ----Usar pre calc? "+getResult);
					}else
					{
						objMetodoAp = new KNN();
						objMetodoAp.setArquivoOrigem(folds[0].getOrigem());
						
						if(!objMetodoAp.foldMatchesFormat(folds[0]))
						{
							throw new Exception("O fold treino '"+folds[0].getTreino().getAbsolutePath()+"' não tem um formato reconhecido!");
						}
						System.out.println("------ Visão no dir: "+folds[0].getTreino().getParentFile().getAbsolutePath()+" é KNN ---");
						isKNN = true;
						
						for(Fold f : folds)
						{
							CriaTreinoFiltrandoTeste.recriaTreino(objMetodoAp, f);
						}
					}
				}
				
				
				objMetodoAp.setArquivoOrigem(folds[0].getOrigem());
				objMetodoAp.setNumClasses(numClasses);
				objMetodoAp.setNomExperimento("view_"+dirView.getName());
				
				View v = new View(null,folds[0].getOrigem(),objMetodoAp);
				v.setFold(folds);
				
				//System.exit(0);
				//define tipo de visao pelo nome do diretorio 
				String dir = folds[0].getOrigem().getParentFile().getName();
				v.setFeatureType(FeatureType.ARR_GENERIC_VIEW[viewCNF.getNumView()]);

				/******************YOUTUBE**********************/
				/*
				if(dir.equalsIgnoreCase("TITLE"))
				{
					v.setFeatureType(isKNN?FeatureType.YT_KNN_TITLE:FeatureType.YT_TITLE);
				}
				if(dir.equalsIgnoreCase("TAG"))
				{
					v.setFeatureType(isKNN?FeatureType.YT_KNN_TAG:FeatureType.YT_TAG);
				}
				if(dir.equalsIgnoreCase("DESCRIPTION"))
				{
					v.setFeatureType(isKNN?FeatureType.YT_KNN_DESCRIPTION:FeatureType.YT_DESCRIPTION);
				}
				if(dir.equalsIgnoreCase("COMMENT"))
				{
					v.setFeatureType(isKNN?FeatureType.YT_KNN_COMMENT:FeatureType.YT_COMMENT);					
				}
				if(dir.equalsIgnoreCase("BAGOW"))
				{
					v.setFeatureType(isKNN?FeatureType.YT_KNN_BAGOW:FeatureType.YT_BAGOW);
				}
				if(dir.equalsIgnoreCase("CONCAT"))
				{
					v.setFeatureType(isKNN?FeatureType.YT_KNN_CONCAT:FeatureType.YT_CONCAT);
				}
				*/
				
				
				//System.out.println("Folds: "+folds.length+" Numfolds: "+numFolds);
				lstViews.add(v);
				
			}
		}
		//System.exit(0);
		return  lstViews;
	}

	public static boolean existsExperimentoCombinacao(String nomExperimento,List<View> lstViews) throws SQLException, ClassNotFoundException
	{
		String nomViewName = lstViews.get(0).getArquivo().getName();
		String fimNomExp = "";
		String[] arrNomColecoes = {"cook","english","stack"};
		for(String nomColecao : arrNomColecoes)
		{
			if(nomViewName.contains(nomColecao))
			{
				fimNomExp = "__RankLib_combinacao_RandomForest_"+nomColecao+".amostra_TamIgualTreino";
				System.out.println("@@Achou nome! ao procurar a existencia, procurando por '"+nomExperimento+fimNomExp+"' ");
				break;
			}
		}

		if(stmtExists == null)
		{
			stmtExists = GerenteBD.getGerenteBD().obtemConexao("").prepareStatement("select 1 " +
																					"	from wiki_results.resultado_regressao " +
																					"	where " +
																					"		nomExperimento = ?" +
																					"	limit 1");
		}
		stmtExists.setString(1, nomExperimento+fimNomExp);
		ResultSet rst = stmtExists.executeQuery();
		if(rst.next())
		{
			rst.close();
			return true;
		}
		
		rst.close();
		return false;
	}
	public static Fold[] getFoldsDiretorio(File diretorio, int numFolds,boolean getResult) throws Exception {
		Fold[] folds = new Fold[numFolds];
		
		
		//procura origem, treino, teste e testeIds
		File origemFold = getFile(diretorio, ".amostra");
		
		for(int j = 0; j<numFolds ; j++)
		{
			File teste = getFile(diretorio, "Teste"+j); 
			File treino = getFile(diretorio, "Treino"+j);
			File validacao = null;
			File validacaoIds = null;
			File foldIdTeste = getFile(diretorio, "foldIds"+j);
			try{
				validacao = getFile(diretorio, "Validacao"+j);
				validacaoIds = getFile(diretorio, "ValidacaoIds"+j);	
			}catch(Exception e)
			{
				
			}
			
			folds[j] = new Fold(j,origemFold,treino,teste,foldIdTeste,false);
			folds[j].setValidationFiles(validacao, validacaoIds);
			
			if(getResult)
			{
				File predict = getFile(diretorio, "predict"+j);
				folds[j].setPredict(predict);
			}
		}
		return folds;
	}

	

	private static File getFile(File dirView, String nomeContido) throws Exception
	{
		List<File> lstFileAmostra = ArquivoUtil.procuraArqEndsWith(dirView, nomeContido);
		/*
		Iterator<File> iFile = lstFileAmostra.iterator();
		while(iFile.hasNext())
		{
			File fl = iFile.next();
			if(fl.getName().endsWith("Model"))
			{
				iFile.remove();
			}
		}
		*/
		validaArquivo(dirView, lstFileAmostra,nomeContido);
		return lstFileAmostra.get(0);
	}

	private static void validaArquivo(File dirView,  List<File> lstFileAmostra,String nomArquivo) throws Exception
	{
		if(lstFileAmostra.size() == 0)
		{
			throw new Exception("Não foi encontrado o arquivo possuindo a string '"+nomArquivo+"' em: "+dirView.getAbsolutePath());
			
		}
		if(lstFileAmostra.size() > 1)
		{
			throw new Exception("Há mais de um arquivo possuindo a string '"+nomArquivo+"' em: "+dirView.getAbsolutePath());
		
		}
	}
	
	public static void combina(String strPrefixCombinacao,File dirCnf,View[] arrViews,ConfigCustoGama paramCombinacao,String dirResultado,MinMax objMinMax,
			boolean utilizarConfianca,boolean utilizarConfArray,boolean utilizarFeatureSet,boolean featureSetPerFold,boolean usarRankPos,int SVM_MODE,
			ViewCnf cnfCombinacao,File cnfFileComb,File datasetAllFeat,String idGrouper,SeletorViews seletor) throws Exception
	{
		File origem = datasetAllFeat;//getFile(new File(dirResultado), ".amostra");
		SVM objSVM = null;
		objSVM = getMetodoCombinacao(strPrefixCombinacao,dirCnf, paramCombinacao, SVM_MODE,	cnfCombinacao, origem,cnfFileComb,arrViews.length,seletor,utilizarFeatureSet);
		
		//verifica a metrica do seletor
		if(seletor != null)
		{
			if(objSVM.isClassificacao())
			{
				seletor.setMetrica(MetricaUsada.ACURACIA);
			}else
			{
				if(objSVM instanceof GenericoLetorLike)
				{
					seletor.setMetrica(MetricaUsada.NDCG_EXP);
				}else
				{
					seletor.setMetrica(MetricaUsada.MSE);
				}
			}
		}
		
		
		TIPO_CONTEUDO_DATASET[] tpoConteudoDatasetML = getConteudoMetalearning(	utilizarConfianca, utilizarConfArray,utilizarFeatureSet,featureSetPerFold,usarRankPos);
		//System.exit(0);
		//TIPO_CONTEUDO_DATASET[] tpoConteudoDatasetML = 	{};
		MetaLearning mt = new MetaLearning(objSVM, tpoConteudoDatasetML);
		mt.setUsingWiki(false);
		mt.setSeletorViews(seletor);
		/*
		if(objSVM.isClassificacao())
		{
			mt.setMatrizConfusao(new MatrizConfusao((int) (objMinMax.getMax()-objMinMax.getMin()+1)));
		}
		*/
		mt.setMinMaxClasse(objMinMax);
		mt.setDirMetalearning(new File(dirResultado+"/metaLearning"));
		
		Combinador objCombinador = new Combinador("combinacao",mt,arrViews);
		
		objCombinador.executaCombinacao(false, dirResultado,idGrouper);
		
	}
	private static TIPO_CONTEUDO_DATASET[] getConteudoMetalearning(
			boolean utilizarConfianca, boolean utilizarConfArray,boolean utilizarFeatureSet,boolean featureSetPerFold,boolean usarRankPos)
	{
		List<TIPO_CONTEUDO_DATASET> lstTpoConteudoDatasetML = new ArrayList<TIPO_CONTEUDO_DATASET>();
		
		if(utilizarConfArray)
		{
			lstTpoConteudoDatasetML.add(TIPO_CONTEUDO_DATASET.CONFIANCA_ARRAY);
		}
		if(utilizarConfianca)
		{
				lstTpoConteudoDatasetML.add(TIPO_CONTEUDO_DATASET.CONFIANCA);
		}
		if(utilizarFeatureSet)
		{
			lstTpoConteudoDatasetML.add(TIPO_CONTEUDO_DATASET.FEATURES_SET);			
		}
		if(usarRankPos)
		{
			lstTpoConteudoDatasetML.add(TIPO_CONTEUDO_DATASET.RANK_POS);			
		}
		
		TIPO_CONTEUDO_DATASET[] arrTipos = new TIPO_CONTEUDO_DATASET[lstTpoConteudoDatasetML.size()];
		for(int i=0; i< lstTpoConteudoDatasetML.size(); i++)
		{
			arrTipos[i] =  lstTpoConteudoDatasetML.get(i);
			System.out.print("FEAT_VIEW: "+arrTipos[i].toString()+" ");
			
			
		}
		System.out.print("\n");
		
		return arrTipos;
	}
	private static SeletorViews criaSeletorView(String strPrefixCombinacao,File seletorFile,File datasetAllFeat,File cnfFileSeletor,MetricaUsada metrica) throws Exception
	{
		//resgata metodo de selecao
		CNFSimples objCnfComb = ResultadoFromCnfSimples.criaCNF(cnfFileSeletor, datasetAllFeat.getAbsolutePath()).get(0);
		objCnfComb.setSufixExpt("combinacao_"+objCnfComb.getSufixExp());
		
		SVM objSVM = ResultadoFromCnfSimples.instancia(objCnfComb,new HashMap<String,String>(),!objCnfComb.isLetor());
		
		
		//cria seletoro e o retorna
		return new SeletorViews(objSVM, seletorFile, metrica);
	}
	public static void getFeatPorcByNumFeats(int numFeatures,Map<String,String> mapParams)
	{
		if(numFeatures==1)
		{
			mapParams.put("FEAT_PORC", "1.0");
		}else
		{
			if(numFeatures<7)
			{
				mapParams.put("FEAT_PORC", "0.5");
			}
		}
		
	}
	private static SVM getMetodoCombinacao(String strPrefixCombinacao,File dirCnf,ConfigCustoGama paramCombinacao, int SVM_MODE,	ViewCnf cnfCombinacao, File origem,File cnfFileComb,int numViewsComb,SeletorViews seletor,boolean utilizarFeatureSet) throws Exception
	{
		
		SVM objSVM;
		if(cnfFileComb == null)
		{
			boolean isSVM = cnfCombinacao.getClassificador() == CLASSIFICADOR.SVM ||cnfCombinacao.getClassificador() == CLASSIFICADOR.SVR;
			if(isSVM)
			{
				objSVM = new SVM(SVM_MODE);
				
				objSVM.setParametros(paramCombinacao);
				
			}else
			{
				objSVM = new GenericoSVMLike(cnfCombinacao.getCmdTreino(), cnfCombinacao.getParamTreino(), cnfCombinacao.getCmdTeste(), cnfCombinacao.getParamTeste());
				objSVM.setMode(SVM_MODE);
				((GenericoSVMLike)objSVM).setDirExecucaoComando(dirCnf.getAbsolutePath());
				((GenericoSVMLike)objSVM).setUsarModeloExistente(false);
			}
			objSVM.setArquivoOrigem(origem);
			objSVM.setNomExperimento("combinacao");
			return objSVM;
		}else
		{
			CNFSimples objCnfComb = ResultadoFromCnfSimples.criaCNF(cnfFileComb, origem.getAbsolutePath()).get(0);
			objCnfComb.setSufixExpt("combinacao_"+objCnfComb.getSufixExp());
			
			Map<String,String> mapParams = new HashMap<String,String>();
			
			//define parametro de acordo com a porcentagem do numero de features (ie view)
			//para ficar log_2 - ver em qa_multiivew/params_multiview_choice.ods
			if(seletor != null || utilizarFeatureSet)
			{
				mapParams.put("FEAT_PORC", "0.3");
			}else
			{
				getFeatPorcByNumFeats(numViewsComb,mapParams);
				

			}
			objSVM = ResultadoFromCnfSimples.instancia(objCnfComb,mapParams,!objCnfComb.isLetor());
			objSVM.setMode(SVM_MODE);
			
			/*
			if(objCnfComb.isLetor())
			{
				objSVM =  ResultadoFromCnfSimples.instanciaLetor(objCnfComb);
				
			}else
			{
				objSVM =   ResultadoFromCnfSimples.instanciaSVM(objCnfComb);
			}
			*/
			//System.exit(0);
			objSVM.setArquivoOrigem(origem);
			objSVM.setNomExperimento(strPrefixCombinacao+"_"+objSVM.getNomExperimento());
			return objSVM;
		}
		
	}
	public enum CLASSIFICADOR{
		SVM,SVR,LETOR_LIKE,KNN,ESPECIFICAR;
	}
	/**
	 * Cada diretorio de view deve conter os 10 folds (teste e 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception
	{
		
		
		Fold.inicializaBDGeral = false;
		//String errorMsg = "Favor entrar com: Combina.jar <utilizar confianca?0 ou 1> <min_class> <max_class> <num_folds> <custo_combinacao> <gama_combinacao> <dir. view 1> <custo view 1> <gama view 1> ... <dir view n> <custo view n> <gama view n> \nO gama é opcional.";
		String errorMsg = "";
		String regExpCustoGama = "[.0-9e]+";
		String REV_COMB_VIEW = "";
		
		/*String[] argsMan = {"1","0","15","10","2","0.125",
								
								
								
								
								"/home/hasan/views/youtube-sp.iff/knn/concat/","1",
								"/home/hasan/views/youtube-sp.iff/knn/bagow/","1",
									 "/home/hasan/views/youtube-sp.iff/knn/title/","1",
									"/home/hasan/views/youtube-sp.iff/knn/comment/","1",
									"/home/hasan/views/youtube-sp.iff/knn/description/","1",
									"/home/hasan/views/youtube-sp.iff/knn/tag/","1",
									*/
									/*
									"/home/hasan/views/youtube-sp.iff/bagow/","1",
									"/home/hasan/views/youtube-sp.iff/concat/","1",	
									"/home/hasan/views/youtube-sp.iff/title/","1",
									"/home/hasan/views/youtube-sp.iff/comment/","1",
									"/home/hasan/views/youtube-sp.iff/description/","1",
									"/home/hasan/views/youtube-sp.iff/tag/","1",
									*/
									/* 
									
		};*/
		//String[] argsMan = {args[0]};
		//String[] argsMan = {"/data/experimentos/teste/toy_rank/experimento.cnf"};
		
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/starAmostra/starAmostra.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/starVote/starVote.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/muppets/muppets.cnf"};
		//String[] argsMan = {"/home/hasan/Dropbox/ferramentas/combinador_stacking/toy_example/experimento.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/wiki6/wiki6.cnf"};
		
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cook/cook.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cook/cook.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cook/cook_baseline.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/english/english.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cook/cook_just_usr.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/english/english.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/stack/stack.cnf"};
		String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/english/english.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/muppets/muppets.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cook/cook.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cook/cook_viewSelector.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cook/english_selector.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/english/english_feat_viewSelector.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/foursquare/cnf_experimentos/fq_1_run_0.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cook/cook_viewsAn.cnf"};
		//StSring[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/english/english_viewsAn_test.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cook/cook_viewsAn_test.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cook/cook_viewsAn_test.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/stack/stack_viewsAn_test.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cook/cook.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/english/english.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/stack/stack.cnf"};
		//String[] argsMan = {"/data/experimentos/sergio/spambase/experimento.cnf"};
		//String[] argsMan = {"/home/hasan/Dropbox/ferramentas/combinador_stacking/toy_example_rank/experimento.cnf"};

		//String[] argsMan = {"/data/experimentos/teste/toy_qa_forum/experimento.cnf"};
		//String[] argsMan = {"/data/experimentos/teste/toy_qa_forum/experimento_little.cnf"};
		
		
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cook_feat.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cookRankPosFeat.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cook_text.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cook_wo_vote_baseline.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cook_text_baseline.cnf"};
		
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/cook_baseline.cnf"};
		//String[] argsMan = {"/data/experimentos/teste/toy_qa_forum/experimento_real.cnf"};
		//String[] argsMan = {"/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview//stack/stack_allViewChanges.cnf"};		
		args = argsMan;
		for(String fileCnf : args)
		{
			/**
			 * usar_resultado_views_existente =  True | False

			 * utilizar_confianca = True | False
			 * is_classificacao = True | False
			 * min_class = [0-9]
			 * max_class = [0-9]
			 * num_folds = [0-9]
			 * <custo_combinacao> = [0-9].[0-9] 
			 * <gama_combinacao> = [0-9].[0-9] (linear)
			 * <dir_view[1]> <custo view 1> <gama view 1> ... <dir view n> <custo view n> <gama view n>
			 * <classificador_view[1]>
			 * <custo_view[1]>
			 * <gama_view[2]>
			 * <param_view[1]>
			 * <cmd_classificador_view[1]
			 */
			Map<String,String> configMap = ArquivoUtil.leKeyValueFile(new File(fileCnf),true);
			
			String[] chavesObrigatorias = {"usar_result_view_precalculado","utilizar_confianca","num_folds"};
			for(String chave : chavesObrigatorias)
			{
				if(!configMap.containsKey(chave))
				{
					System.err.println("Não foi possivel encontrar a chave "+chave+" no arquivo de  configuração "+fileCnf);
					System.exit(0);
				}
			}
			
			if(! (configMap.get("utilizar_confianca").equalsIgnoreCase("true") || configMap.get("utilizar_confianca").equalsIgnoreCase("false")))
			{
				System.err.println("Confianca invalida! ela deve possuir o valor true ou false Ela possui o value:'"+configMap.get("utilizar_confianca")+"'\n"+errorMsg);
				System.exit(0);
			}
			
			if(! (configMap.get("usar_result_view_precalculado").equalsIgnoreCase("true") || configMap.get("usar_result_view_precalculado").equalsIgnoreCase("false")))
			{
				System.err.println("usar_result_view_precalculado invalido! ela deve possuir o valor true ou false Ela possui o value:'"+configMap.get("usar_result_view_precalculado")+"'\n"+errorMsg);
				System.exit(0);
			}
			if(! (configMap.get("is_classificacao").equalsIgnoreCase("true") || configMap.get("is_classificacao").equalsIgnoreCase("false")))
			{
				System.err.println("parametro is_classificacao invalido! ela deve possuir o valor true ou false \n"+errorMsg);
				System.exit(0);
			}
			if(configMap.containsKey("usar_resultado_views_existente") && !(configMap.get("usar_resultado_views_existente").equalsIgnoreCase("true") || configMap.get("usar_resultado_views_existente").equalsIgnoreCase("false")))
			{
				System.err.println("parametro usar_resultado_views_existente invalido! ela deve possuir o valor true ou false \n"+errorMsg);
				System.exit(0);
			}
			if(configMap.get("min_class") != null &&  !configMap.get("min_class").matches("[0-9]+"))
			{
				System.err.println("Numero da menor classe invalido! \n"+errorMsg);
				System.exit(0);
			}
			if(configMap.get("max_class") != null && !configMap.get("max_class").matches("[0-9]+"))
			{
				System.err.println("Numero da maior classe invalido! \n"+errorMsg);
				System.exit(0);
			}
			
			if(!configMap.get("num_folds").matches("[0-9]+"))
			{
				System.err.println("Numero do fold inválido! \n"+errorMsg);
				System.exit(0);
			}

			
			/*
			if(!configMap.get("custo_combinacao").matches(regExpCustoGama))
			{
				System.err.println("Parametro custo de combinação inválida! \n"+errorMsg);
				System.exit(0);
			}
			if(configMap.containsKey("gama_combinacao") && !configMap.get("gama_combinacao").matches(regExpCustoGama))
			{
				System.err.println("Parametro gama de combinação inválido! \n"+errorMsg);
				System.exit(0);
			}
			*/
			if(!configMap.containsKey("utilizar_feat_set"))
			{
				configMap.put("utilizar_feat_set", "false");
			}
			if(!configMap.containsKey("utilizar_feat_per_fold"))
			{
				configMap.put("utilizar_feat_per_fold", "false");
			}
			if(!configMap.containsKey("utilizar_rank_pos"))
			{
				configMap.put("utilizar_rank_pos", "false");
			}
			
			if(!configMap.containsKey("balancear_seletor"))
			{
				configMap.put("balancear_seletor", "false");
			}
			if(!configMap.containsKey("utilizar_apenas_predictions_seletor"))
			{
				configMap.put("utilizar_apenas_predictions_seletor", "true");
			}
			
			//parametros da combinacao
			String strPrefixExperimento = configMap.containsKey("prefix_experimento")?configMap.get("prefix_experimento"):"";
			String strPrefixCombinacao = configMap.containsKey("prefix_combinacao")?configMap.get("prefix_combinacao")+"_":"";
			String nomArquivoViews = configMap.containsKey("nom_arquivo_views")?configMap.get("nom_arquivo_views"):"";
			boolean doUndersampling = configMap.containsKey("do_undersampling")?configMap.get("do_undersampling").equalsIgnoreCase("true"):false;
			boolean usarResultadoJaExistente = configMap.containsKey("usar_resultado_views_existente")?configMap.get("usar_resultado_views_existente").equalsIgnoreCase("true"):false;
			boolean isClassificacao = configMap.get("is_classificacao").equalsIgnoreCase("true");
			boolean utilizarConfianca = configMap.get("utilizar_confianca").equalsIgnoreCase("true");
			boolean utilizarFeatSet = configMap.get("utilizar_feat_set").equalsIgnoreCase("true");;
			boolean utilizarPerFold = configMap.get("utilizar_feat_per_fold").equalsIgnoreCase("true");;
			boolean utilizarConfArray = configMap.get("utilizar_confianca_array").equalsIgnoreCase("true");
			boolean usarRankPos = configMap.get("utilizar_rank_pos").equalsIgnoreCase("true");
			
			boolean balancearSeletor = configMap.get("balancear_seletor").equalsIgnoreCase("true");
			boolean usarApenasPredictionsInSeletor = configMap.get("utilizar_apenas_predictions_seletor").equalsIgnoreCase("false");
			
			
			Integer minViewsCombinacao = configMap.containsKey("min_views_combinacao")? Integer.parseInt(configMap.get("min_views_combinacao")):null;
			Integer maxViewsCombinacao = configMap.containsKey("maxViewsCombinacao")? Integer.parseInt(configMap.get("maxViewsCombinacao")):null;
			
			String idGrouper = configMap.containsKey("id_grouper_folds")?configMap.get("id_grouper_folds"):"id";
			ResultadoViews.READ_OBJECT = configMap.get("usar_result_view_precalculado").equalsIgnoreCase("true");
			Double custoComb,gamaComb = null;
			MinMax objClassMinMax = configMap.get("min_class")!=null? new MinMax(Double.parseDouble(configMap.get("min_class")), Double.parseDouble(configMap.get("max_class"))):new MinMax();
			Integer numFolds = Integer.parseInt(configMap.get("num_folds"));
			ViewCnf combCnf = new ViewCnf(0,null,CLASSIFICADOR.ESPECIFICAR,configMap.get("cmd_treino_comb"),configMap.get("cmd_teste_comb"),configMap.get("param_treino_comb"),configMap.get("param_teste_comb"));
			 
			/*custoComb = Double.parseDouble(configMap.get("custo_combinacao"));
			
			if(configMap.containsKey("gama_combinacao"))
			{
				gamaComb = Double.parseDouble(configMap.get("gama_combinacao")); 
			}
			*/
			
				
			//separa dir views, de todos os seus parametros
			//List<File> dirViews = new ArrayList<File>();
			//List<ConfigCustoGama> arrParam = new ArrayList<ConfigCustoGama>();
			
			List<ViewCnf> lstViewsCnf = new ArrayList<ViewCnf>();
			List<CNFSimples> lstCnfArqView = new ArrayList<CNFSimples>();
			
			for(String chave : configMap.keySet())
			{
				if(chave.matches("dir_view\\[[0-9]+\\]"))
				{
					File dirView = new File(configMap.get(chave));
					if(!dirView.isDirectory())
					{
						System.err.println("Erro! O diretorio "+dirView.getAbsolutePath()+" para a view '"+chave+"' não foi encontrado \n"+errorMsg);
						System.exit(0);
					}
					String numView = chave.replaceAll("dir_view", "").trim();
					int intNumView = Integer.parseInt(numView.replaceAll("\\[|\\]", "").trim()); 
					
					String className = configMap.get("classificador_view"+numView);
					CLASSIFICADOR metodoAprendizado = getClassificador(numView,	className);
					Double custo = null,gama = null;
					String cmdTreino = "";
					String cmdTeste = "";
					String paramTreino = "";
					String paramTeste = "";
					
					switch(metodoAprendizado)
					{
						case KNN:
							//cmd = configMap.get("cmd_classificador_view"+numView);
							break;
						case SVM:
							
							if(!configMap.containsKey("custo_view"+numView))
							{
								System.out.println("Especificar o parametro custo do svm para a visao "+numView);
							}
							if(!configMap.get("custo_combinacao"+numView).matches(regExpCustoGama))
							{
								System.err.println("Parametro "+"custo_combinacao"+numView+" inválido! \n"+errorMsg);
								System.exit(0);
							}else
							{
								custo = Double.parseDouble(configMap.get("custo_combinacao"+numView));
							}
							if(configMap.containsKey("gama_view"+numView) && !configMap.get("gama_view"+numView).matches(regExpCustoGama))
							{
								System.err.println("Parametro gama_combinacao"+numView+" inválido! \n"+errorMsg);
								System.exit(0);
							}else
							{
								if(configMap.containsKey("gama_view"+numView) )
								{
									gama = Double.parseDouble(configMap.get("gama_combinacao"+numView));
								}
							}
							//cmd = configMap.get("cmd_classificador_view"+numView);
							break;
					
					
						case ESPECIFICAR:
							if(!configMap.containsKey("cmd_treino_view"+numView))
							{
								System.err.println("É necessario especificar o comando do treino para o classificador para a view "+numView+" (parametro "+"cmd_treino_view"+numView+")");
								System.exit(0);
							}
							cmdTreino = configMap.get("cmd_treino_view"+numView);
							
							if(!configMap.containsKey("cmd_teste_view"+numView))
							{
								System.err.println("É necessario especificar o comando do teste para o classificador para a view "+numView+" (parametro "+"cmd_teste_view"+numView+")");
								System.exit(0);
							}
							cmdTeste = configMap.get("cmd_teste_view"+numView);
							
							if(configMap.containsKey("param_treino_view"+numView))
							{
								paramTreino = configMap.get("param_treino_view"+numView);
							}
							
							
							if(configMap.containsKey("param_teste_view"+numView))
							{
								paramTeste = configMap.get("param_teste_view"+numView);
							}
							lstViewsCnf.add(new ViewCnf(intNumView,dirView,CLASSIFICADOR.ESPECIFICAR,cmdTreino,cmdTeste,paramTreino,paramTeste));
							break;
					}
					
					

				}
				if(chave.matches("cnf_view\\[[0-9]+\\]"))
				{
					File arqCnfView = new File(configMap.get(chave));
					String numView = chave.replaceAll("cnf_view", "").trim();
					int intNumView = Integer.parseInt(numView.replaceAll("\\[|\\]", "").trim());
					
					List<CNFSimples> lstCNFs = ResultadoFromCnfSimples.criaCNF(arqCnfView,configMap.get("dataset_view"+numView));
					for(CNFSimples cnf : lstCNFs)
					{
						cnf.setNumView(intNumView);
						cnf.setPrefixExp(configMap.get("prefix_experimento")==null?"multiview":configMap.get("prefix_experimento"));
						lstCnfArqView.add(cnf);
					}
					
					
				}
				
				
			}
			/*
			File[] arrFiles = new File[dirViews.size()];
			ConfigCustoGama[] arrCnfView = new ConfigCustoGama[arrParam.size()];
			for(int j = 0 ; j<dirViews.size() ; j++)
			{
				System.out.println("View: "+dirViews.get(j).getName()+" Params: "+arrParam.get(j).toString());
				arrFiles[j] = dirViews.get(j);
				arrCnfView[j] = arrParam.get(j);
			}
			*/
			if(configMap.containsKey("nom_arquivo_views"))
			{
				ResultadoViews.setNomArquivoView(configMap.get("nom_arquivo_views"));
			}
			
			//cria views com os folds
			custoComb = 1.0;
			MetaLearning.GRAVA_SCALE = false;
			
			List<View> lstViews = new ArrayList<View>();  
					
			lstViews.addAll(getFoldsIniciais(new File(fileCnf).getParentFile(),lstViewsCnf,numFolds,(int) (objClassMinMax.getMax()-objClassMinMax.getMin()+1),utilizarConfianca,usarResultadoJaExistente,isClassificacao?SVM.MODE_CLASSIFICATION:SVM.MODE_REGRESSION));

			lstViews.addAll(getFoldsIniciaisCNF(new File(fileCnf).getParentFile(),lstCnfArqView, numFolds, utilizarConfianca,isClassificacao?SVM.MODE_CLASSIFICATION:SVM.MODE_REGRESSION,idGrouper,doUndersampling)) ;
			
			
			File cnfFileComb = configMap.containsKey("cnf_comb")?new File(configMap.get("cnf_comb")):null;
			File cnfFileSeletor = configMap.containsKey("cnf_seletor")?new File(configMap.get("cnf_seletor")):null;
			File datasetAllFeatures = configMap.containsKey("dataset_all_feat_together")?new File(configMap.get("dataset_all_feat_together")):null;
			File datasetSeletorView = configMap.containsKey("dataset_seletor_view")?new File(configMap.get("dataset_all_feat_together")):null;
			

			
			
			
			List<List<View>> lstCombinacoesViews = new ArrayList<List<View>>();
			
			//caso necessario, faz todas as combinacoes
			if(minViewsCombinacao != null || maxViewsCombinacao != null)
			{
				lstCombinacoesViews = new ArrayUtil<View>().getAllCombinations(lstViews, 
																				minViewsCombinacao != null?minViewsCombinacao:2, 
																				maxViewsCombinacao != null?maxViewsCombinacao:lstViews.size()-1
																				);
			}else
			{
				//caso nao haja combinacao, faz apenas uma com todas
				lstCombinacoesViews.add(lstViews);
			}
			
			for(int i = 0 ; i<lstCombinacoesViews.size() ; i++)
			{
				List<View> lstCombView = lstCombinacoesViews.get(i);
				
				String nomExperimentoCombinacao = "";
				if(lstCombView.size() == lstViews.size())
				{
					nomExperimentoCombinacao = strPrefixExperimento+"_"+strPrefixCombinacao+nomArquivoViews;
				}else
				{
					//resgata cada nome de view...
					nomExperimentoCombinacao = strPrefixExperimento+"_"+REV_COMB_VIEW+"_"+strPrefixCombinacao+"combView_";
					for(View vw : lstCombView)
					{
						nomExperimentoCombinacao += "_"+vw.getArquivo().getName().replaceAll("english", "").replaceAll("cook", "").replaceAll("stack", "").replaceAll("_", "").split("\\.")[0];
					}
				}

				
				
				//verifica a necessidade de criar o seletor de visoes
				SeletorViews seletor = null;
				if(cnfFileSeletor != null)
				{
					seletor = criaSeletorView(nomExperimentoCombinacao+"_seletor", datasetSeletorView, datasetAllFeatures, cnfFileSeletor, null);
					
					seletor.setJustPredictions(usarApenasPredictionsInSeletor);
					seletor.setBalancearSeletor(balancearSeletor);
					
				}
				System.out.println("Experimento combinacao: "+nomExperimentoCombinacao);
				
				//verifica se tem necessidade de realizar esta view
				long time = System.currentTimeMillis();
				/*if(i < 180)
				{*/
				
					if(existsExperimentoCombinacao(nomExperimentoCombinacao,lstViews))
					{
						System.out.println("####> Ja realizou anteriormente Combinacao view: "+nomExperimentoCombinacao+" ("+(i+1)+"/"+lstCombinacoesViews.size()+") tempo de execucao: "+((System.currentTimeMillis()-time)/1000.0)+"s");
					}else
					{
						
							System.out.println("####> Realizando Combinacao view: "+nomExperimentoCombinacao+" ("+(i+1)+"/"+lstCombinacoesViews.size()+") ++Usa trans++");
							
							
							combinaVisoes(nomExperimentoCombinacao,
											new File(fileCnf).getParentFile(),
											utilizarConfianca,
											utilizarFeatSet,
											utilizarPerFold, 
											utilizarConfArray,
											usarRankPos,
											custoComb, 
											gamaComb, 
											objClassMinMax,
											lstCombView.get(0).getArquivo().getParentFile().getAbsolutePath(), 
											lstCombView,
											isClassificacao?SVM.MODE_CLASSIFICATION:SVM.MODE_REGRESSION,
											combCnf,
											cnfFileComb,
											datasetAllFeatures,
											idGrouper,
											seletor);
							
							System.out.println("####> FimView: "+nomExperimentoCombinacao+" ("+(i+1)+"/"+lstCombinacoesViews.size()+") tempo de execucao: "+((System.currentTimeMillis()-time)/1000.0)+"s");					
					
					}
				/*
				}else
				{
					System.out.println("####> Apenas abaixo de 180: "+nomExperimentoCombinacao+" ("+(i+1)+"/"+lstCombinacoesViews.size()+") tempo de execucao: "+((System.currentTimeMillis()-time)/1000.0)+"s");
				}
				*/

				//System.exit(0);
			}
			
			
		}
		ResultadoItemDelayedRecord gravador = ResultadoItemDelayedRecord.getGravador();
		gravador.finish();
		gravador.getThreadGravador().join();
	}

	
	private static CLASSIFICADOR getClassificador(String numView,
			String className)
	{
		CLASSIFICADOR metodoAprendizado = CLASSIFICADOR.ESPECIFICAR;
		String vals = "";
		
		
		for(CLASSIFICADOR cl : CLASSIFICADOR.values())
		{
			if(className.equalsIgnoreCase(cl.toString()))	
			{
				metodoAprendizado = cl;
				break;
			}
			vals += cl.toString().toLowerCase()+"; ";
			
		}
		if(metodoAprendizado == null)
		{
			System.err.println("Erro! Especificar o classficador da visao "+numView+" usando a propriedade classificador_view"+numView+
					
					"\nValores validos: "+vals);
			System.exit(0);
		}
		return metodoAprendizado;
	}


	public static void combinaVisoes(String strPrefixCombinacao,File dirCnf,boolean utilizarConfianca,boolean utilizarFeatureSet,boolean utilizarPerFold,boolean utilizarConfArray,boolean usarRankPos,
			Double custoComb, Double gamaComb, MinMax objClassMinMax,
			String pathToViews, List<View> lstViews,int svmMode,ViewCnf combCnf,File cnfFileComb,File datasetAllFeat,String idGrouper,SeletorViews seletor) throws Exception {
		
		View[] arrViews = new View[lstViews.size()];
		for(int j =0 ; j<arrViews.length ; j++)
		{
			arrViews[j] = lstViews.get(j);	
		}
			
		
		combina(strPrefixCombinacao,dirCnf, arrViews, new ConfigCustoGama(custoComb, gamaComb, 0.1),pathToViews,objClassMinMax,utilizarConfianca,utilizarConfArray,utilizarFeatureSet,utilizarPerFold,usarRankPos,svmMode,combCnf,cnfFileComb,datasetAllFeat,idGrouper,seletor);
	}
}
