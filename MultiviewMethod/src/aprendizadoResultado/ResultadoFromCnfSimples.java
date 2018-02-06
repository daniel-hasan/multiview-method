package aprendizadoResultado;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import stuctUtil.ListaAssociativa;
import aprendizadoUtils.GenericoLetorLike;
import aprendizadoUtils.GenericoSVMLike;
import aprendizadoUtils.SVM;
import arquivo.ArquivoUtil;
import entidadesAprendizado.Fold;
import entidadesAprendizado.MatrizConfusao;
import entidadesAprendizado.ResultadoItem;



public class ResultadoFromCnfSimples {
	
	public static void gerarMetodo(String prefixExp,String metodo,File dataset,Map<String,String> paramTreino,Map<String,String> paramTeste,File dirResultTxt,String sufixExp,ListaAssociativa<String, String> lstParams) throws Exception
	{ 
		String nomExperimento  = prefixExp+"_"+metodo+"_"+sufixExp+"_"+dataset.getName();
		System.out.println("Experimento: "+nomExperimento);
		
		GenericoSVMLike svmLike = instanciaSVM(metodo, paramTreino, paramTeste,
				lstParams, nomExperimento);
		
		Fold[] folds = svmLike.testar(dataset, 5, true,"id","qid",true);
		System.out.println("Resultado geral: "+svmLike.getResultadoGeral(folds));
		List<ResultadoItem> lstResultados = new ArrayList<ResultadoItem>();
		for(int i =0 ; i<folds.length ; i++)
		{
			lstResultados.addAll(folds[i].getResultadosValues());
		}
		
		File dirExp = new File(dirResultTxt,nomExperimento);
		if(!dirExp.exists())
		{
			dirExp.mkdirs();
		}
		
		MatrizConfusao mt = MatrizConfusao.getMatrizConfusao(lstResultados);
		ArquivoUtil.gravaTexto(mt.toString(), new File(dirExp,"resultado.txt"), false);
		
		ArquivoUtil.gravaTexto(svmLike.getSaidaTreino(), new File(dirExp,"saidaTreino.txt"), false);
		ArquivoUtil.gravaTexto(svmLike.getSaidaTeste()+"\n\n"+mt.toString(), new File(dirExp,"saidaTeste.txt"), false);
	}
	public static GenericoSVMLike instancia(CNFSimples objCNF,Map<String,String> params,boolean isSVM) throws SQLException, ClassNotFoundException, Exception
	{
		//adiciona parametros hardcoded
		Map<String,String> mapParamsTeste = objCNF.getParamTeste();
		Map<String,String> mapParamsTreino = objCNF.getParamTreino();
		for(String key : params.keySet())
		{
			mapParamsTreino.put(key, params.get(key));
			mapParamsTeste.put(key, params.get(key));
		}
		
		if(isSVM)
		{
			return instanciaSVM(objCNF.getMetodo(),mapParamsTreino,mapParamsTeste ,objCNF.getLstParams(), objCNF.getNomExperimento());
		}else
		{
			return instanciaLetor(objCNF.getMetodo(),mapParamsTreino,mapParamsTeste ,objCNF.getLstParams(), objCNF.getNomExperimento());
		}
	}
	public static GenericoSVMLike instanciaSVM(CNFSimples objCNF) throws SQLException, ClassNotFoundException, Exception
	{
		return instanciaSVM(objCNF.getMetodo(),objCNF.getParamTreino(), objCNF.getParamTeste(),objCNF.getLstParams(), objCNF.getNomExperimento()); 
	}
	public static GenericoSVMLike instanciaSVM(String metodo,
			Map<String, String> paramTreino, Map<String, String> paramTeste,
			ListaAssociativa<String, String> lstParams, String nomExperimento)
			throws Exception, SQLException, ClassNotFoundException
	{
		GenericoSVMLike svmLike = new GenericoSVMLike(metodo,paramTreino,paramTeste);
		svmLike.setMode(SVM.MODE_CLASSIFICATION);
		svmLike.setNomExperimento(nomExperimento);
		svmLike.setGravarNoBanco(lstParams.getFirst("gravar_no_banco", "false").equalsIgnoreCase("true"));
		svmLike.setNomExperimento(nomExperimento);
		svmLike.setGetResultPreCalculado(lstParams.getFirst("get_result_pre_calculado", "false").equalsIgnoreCase("true"));
		svmLike.setUsarModeloExistente(lstParams.getFirst("usar_modelo_existente", "false").equalsIgnoreCase("true"));
		return svmLike;
	}

	public static void gerarMetodoRank(String prefixExp,String metodo,File dataset,Map<String,String> paramTreino,Map<String,String> paramTeste,File dirResultTxt,String sufixExp,ListaAssociativa<String, String> lstParams) throws Exception
	{
		
		String nomExperimento  = prefixExp+"_"+metodo+"_"+sufixExp+"_"+dataset.getName();
		File dirExp = new File(dirResultTxt,nomExperimento);
		if(!dirExp.exists())
		{
			dirExp.mkdirs();
		}
		System.out.println("Dir expResult: "+dirExp.getAbsolutePath());
		//System.exit(0);
		System.out.println("Experimento: "+nomExperimento);
		
		GenericoSVMLike svmLike = instanciaLetor(metodo, paramTreino,
				paramTeste, lstParams, nomExperimento);
		
		
		
		Fold[] folds = svmLike.testar(dataset, 5, true,"id","qid",true);
		svmLike.setNomExperimento(nomExperimento);
		
		
		List<ResultadoItem> lstResultados = new ArrayList<ResultadoItem>();
		for(int i =0 ; i<folds.length ; i++)
		{
			lstResultados.addAll(folds[i].getResultadosValues());
		}
		ValorResultado vResult = svmLike.getResultado(lstResultados);
		String resultGeral = "============================== Resultado geral =========================";
		resultGeral +=  "Resultado geral: "+vResult.toString();
		System.out.println(resultGeral);
		
		ArquivoUtil.gravaTexto(vResult.toString(), new File(dirExp,"resultado.txt"), false);
		
		ArquivoUtil.gravaTexto(svmLike.getSaidaTreino(), new File(dirExp,"saidaTreino.txt"), false);
		ArquivoUtil.gravaTexto(svmLike.getSaidaTeste()+"\n\n"+resultGeral, new File(dirExp,"saidaTeste.txt"), false);
	}
	public static GenericoSVMLike instanciaLetor(CNFSimples objCNF) throws SQLException, ClassNotFoundException, Exception
	{
		return instanciaLetor(objCNF.getMetodo(),objCNF.getParamTeste(), objCNF.getParamTeste(),objCNF.getLstParams(), objCNF.getNomExperimento());
	}

	public static GenericoSVMLike instanciaLetor(String metodo,
			Map<String, String> paramTreino, Map<String, String> paramTeste,
			ListaAssociativa<String, String> lstParams, String nomExperimento)
			throws Exception, SQLException, ClassNotFoundException
	{
		GenericoSVMLike svmLike = new GenericoLetorLike(metodo,paramTreino,paramTeste);
		svmLike.setGravarNoBanco(lstParams.getFirst("gravar_no_banco", "false").equalsIgnoreCase("true"));
		svmLike.setNomExperimento(nomExperimento);
		svmLike.setGetResultPreCalculado(lstParams.getFirst("get_result_pre_calculado", "false").equalsIgnoreCase("true"));
		svmLike.setUsarModeloExistente(lstParams.getFirst("usar_modelo_existente", "false").equalsIgnoreCase("true"));
		return svmLike;
	}
	
	public static List<CNFSimples> criaCNF(File arqCNF) throws Exception
	{
		return criaCNF(arqCNF,null);
	}
	public static List<CNFSimples> criaCNF(File arqCNF,String strEndDataset) throws Exception
	{
		ListaAssociativa<String,String>  mapValues = ArquivoUtil.leKeyValueListFile(arqCNF, false);
		List<CNFSimples> lstCNFSimples = new ArrayList<CNFSimples>();
		//verifica o arquivo (dataset), caso necessario adiciona mais
		
		List<String> lstEndDatasets = new ArrayList<String>();
		if(strEndDataset == null)
		{
			lstEndDatasets.addAll(mapValues.getList("end_dataset"));
		}else
		{
			lstEndDatasets.add(strEndDataset);
		}
		
		for(String endDataset : lstEndDatasets)
		{
			if(endDataset.contains("*") || endDataset.contains("+"))
			{
				File end = new File(endDataset);
				String arqName = end.getName();
				
				//System.out.println("NAME: "+arqName+" PARENT: "+end.getParent());
				File dir = end.getParentFile();
				if(dir != null)
				{
					for(File arq : dir.listFiles())
					{
						String regExp = arqName;
						if(arq.getName().matches(regExp))
						{
							mapValues.getList("end_dataset").add(arq.getAbsolutePath());
						}
					}
				}
				mapValues.removeValue("end_dataset", endDataset);
			}
		}
		for(Map<String,String> paramsValues : mapValues.combineAll())
		{
			boolean isLetor = paramsValues.get("is_letor")!=null?paramsValues.get("is_letor").equalsIgnoreCase("true"):false;
			
			String metodoNome = getParamValue(paramsValues, "nome_metodo");
			String prefixExp = paramsValues.containsKey("prefix_experimento")?getParamValue(paramsValues, "prefix_experimento"):"";
			File dataset = paramsValues.containsKey("end_dataset")?new File(getParamValue(paramsValues,"end_dataset")):new File(strEndDataset);
			File dirResult = new File(getParamValue(paramsValues,"dir_result"));
			
			//coloca o sufixo de cada parametro "geral"
			String sufixExp = "";
			
			//resgata param de treino e teste
			Map<String,String> paramTeste = new HashMap<String,String>();
			for(String paramName : paramsValues.keySet())
			{
				if(paramName.startsWith("param_"))
				{
					paramTeste.put(paramName.replaceAll("param_", ""), getParamValue(paramsValues, paramName) ) ;
				}				
				//para todo os parametros, verificar nome do experimento
				sufixExp += getNomExperimento(paramsValues,paramName);
			}
			
			if(sufixExp.length()>0)
			{
				sufixExp = sufixExp.substring(1);
			}
			System.out.println("Metodo: "+metodoNome+" \nprefixExp: "+prefixExp+"\ndataset: "+dataset.getAbsolutePath()+"\ndirResult: "+dirResult.getAbsolutePath()+
								"\nSufixExp: "+sufixExp+"\nparams: "+paramTeste+"\nisLetor: "+isLetor+"\n");
			System.out.println("=========================================================\n");
			CNFSimples cnf = new CNFSimples(prefixExp, metodoNome, dataset, paramTeste, paramTeste, dirResult, sufixExp, new ListaAssociativa<String, String>((HashMap<String,String>)paramsValues), isLetor);
			lstCNFSimples.add(cnf);
		}
		
		return lstCNFSimples;
	}
		
	public static void executaCNF(File arqCNF) throws Exception
	{
		
		List<CNFSimples> lstCNFSimples = criaCNF(arqCNF);
		for(CNFSimples cnf : lstCNFSimples)
		{
			
			if(cnf.isLetor())
			{
				gerarMetodoRank(cnf.getPrefixExp(),cnf.getMetodo(),cnf.getDataset(),cnf.getParamTreino(), cnf.getParamTeste(), cnf.getDirResult(), cnf.getSufixExp(),cnf.getLstParams());
			}else
			{
				gerarMetodo(cnf.getPrefixExp(),cnf.getMetodo(),cnf.getDataset(),cnf.getParamTreino(), cnf.getParamTeste(), cnf.getDirResult(), cnf.getSufixExp(),cnf.getLstParams());
			}
		}	
		
	}

	private static String getNomExperimento(Map<String, String> paramsValues,
			String paramName)
	{
		String paramValue = paramsValues.get(paramName);
		String paramExp = "";
		if( paramValue.indexOf("(") >= 0)
		{
			int idxAbrePar = paramValue.indexOf("(");
			int idxFechaPar = paramValue.indexOf(")");
			
			paramExp =  "_"+paramValue.substring(idxAbrePar+1,idxFechaPar );
			paramValue = paramValue.substring(0, idxAbrePar);
			
			
		}
		return paramExp;
	}
	 
	private static String getParamValue(Map<String, String> paramsValues,
			String paramName)
	{
		String paramValue = paramsValues.get(paramName);
		if( paramValue.indexOf("(") >= 0)
		{
			paramValue = paramValue.substring(0, paramValue.indexOf("("));			
		}
		return paramValue;
	}
	
	public static void main(String[] args) throws Exception
	{
		String dir = "/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/execucao/multiview/";
		
		/*
		String[] argsNew = {"/local/user.cnf"};
		args = argsNew;
		*/
		//executaCNF(new File(dir+"/"+args[0]));
		//executaCNF(new File(dir+"/.cnf"));
		//executaCNF(new File(dir+"/ex_qd.cnf"));
		
		//executaCNF(new File(dir+"/AdTree.cnf"));
		//executaCNF(new File(args[0]));
		//executaCNF(new File(dir+"/textualFeatStudy_2.cnf"));
		//executaCNF(new File(dir+"/ex_qd.cnf"));
		
		executaCNF(new File("/home/hasan/data/experimentos/qa_multiview/cnf_experimentos/multiview/teste_base.cnf"));
		
		System.exit(0);
		Map<String,String> paramTesteY = new HashMap<String,String>();
		
		
		
		/*
		paramTesteY.put("SIM_METHOD","tag");
		paramTesteY.put("ANSWER_IMPORTANCE","0.2");
		gerarMetodoRank("ExpertiseQualityDependent", new File("/data/experimentos/sigir/datasets/ex_qd.amostra"),new HashMap<String,String>(),paramTesteY,new File("/data/experimentos/sigir_2013/resultados"),"");
		*/
		
		/*
		paramTesteY.put("BOOSTING_TREES","20");
		paramTesteY.put("EXP_NODES","-3");
		gerarMetodo("sigir_2013","ADTree", new File("/data/experimentos/sigir_2013/datasets/decision_three.amostra"),new HashMap<String,String>(),paramTesteY,new File("/data/experimentos/sigir_2013/resultados"),"",new ListaAssociativa<String,String>());
		
		gerarMetodo("sigir_2013","LogRegression", new File("/data/experimentos/sigir_2013/datasets/eval_predict_logScore.amostra"),new HashMap<String,String>(),new HashMap<String,String>(),new File("/data/experimentos/sigir_2013/resultados"),"",new ListaAssociativa<String,String>());
		*/

		/*
		gerarMetodo("LogRegression", new File("/data/experimentos/jcdl_2013/datasets/eval_predict.amostra"),new HashMap<String,String>(),new HashMap<String,String>(),new File("/data/experimentos/jcdl_2013/resultados"),"");
		Map<String,String> paramTesteY = new HashMap<String,String>();
		paramTesteY.put("BOOSTING_TREES","20");
		paramTesteY.put("EXP_NODES","-3");
		gerarMetodo("ADTree", new File("/data/experimentos/jcdl_2013/datasets/decision_three.amostra"),new HashMap<String,String>(),paramTesteY,new File("/data/experimentos/jcdl_2013/resultados"),"");
		*/
		/*
		String[] arrTypes = {/*"8","7","6",*///"5","4","3","2","1"};
		//String[] arrMethods = {/*"RandomForests","ListNet","LambdaMart",*/"CoordinateAscent","AdaRank","RankBoost","RankNet","Mart"};
		
		/*String[] argsNew = {"amostra.txt"};
		args = argsNew;
		*/
		//Map<String,String> paramTesteX = new HashMap<String,String>();
		/*paramTesteX.put("RANKER","8");
		gerarMetodoRank("RankLib", new File(args[0]),paramTesteX,paramTesteX,new File("/data/experimentos/jcdl_2013/resultados"),"RandomForests");
		*/
		/*
		paramTesteX.put("METRIC","ERR@10");
		gerarMetodoRank("RankLib", new File(args[0]),paramTesteX,paramTesteX,new File("/data/experimentos/jcdl_2013/resultados"),"RandomForests_ERR");
		*/
		/*
		for(int i = 0 ; i <arrTypes.length ; i++)
		{
			Map<String,String> paramTeste = new HashMap<String,String>();
			paramTeste.put("RANKER",arrTypes[i]);
			//paramTeste.put("Rank",arrTypes[i]);
			//gerarMetodo("MaxEnt", new File("/home/hasan/Dropbox/ferramentas/toy/colecao.amostra"));
			//gerarMetodo("ADTree", new File("/data/experimentos/jcdl_2013/datasets/decision_three.amostra"),new HashMap<String,String>(),paramTeste,new File("/data/experimentos/jcdl_2013/resultados"));
			gerarMetodoRank("RankLib", new File("/data/experimentos/jcdl_2013/datasets/main.amostra"),paramTeste,paramTeste,new File("/data/experimentos/jcdl_2013/resultados"),arrMethods[i]);
			//return;
		}
		*/
		//gerarMetodo("LambdaMart", new File("/home/hasan/Dropbox/ferramentas/toy/colecaoRank.amostra"));
		
		//gerarMetodoRank("LambdaMart", new File("/home/hasan/Dropbox/ferramentas/toy/colecaoRank.amostra"));
		//gerarMetodo("SVM", new File("/home/hasan/Dropbox/ferramentas/toy/colecaoRank.amostra"));
		
		//gerarMetodo("ADTree", new File("/data/experimentos/jcdl_2013/datasets/decision_three.amostra"));
		
		//gerarMetodo("ADTree", new File("/data/experimentos/jcdl_2013/dataset/decision_three.amostra"));
		//System.out.println(Sys.executarComando("/tmp/train_ADTree1273304642418386719.sh ",true,"/tmp"));
	}
}