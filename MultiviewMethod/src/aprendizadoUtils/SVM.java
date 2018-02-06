package aprendizadoUtils;

import io.Sys;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import scriptsUtil.DatasetUtil;
import string.PadraoString;
import string.RegExpsConst;
import string.StringUtil;
import aprendizadoResultado.CalculaResultados;
import aprendizadoResultado.ValorResultado;
import aprendizadoResultado.ValorResultado.MetricaUsada;
import banco.GerenteBD;
import config_tmp.Colecao;
import config_tmp.ConfigCustoGama;
import entidadesAprendizado.Fold;
import entidadesAprendizado.ResultadoItem;
import entidadesAprendizado.ResultadoItemDelayedRecord;
import entidadesAprendizado.ResultadoItemToRecord;

public class SVM extends MetodoAprendizado
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static final int MODE_REGRESSION =3;
	public static final int MODE_CLASSIFICATION = 1;
	public static final int NUM_FOLDS_CUSTO_GAMA = 5;
	
	public static int NUM_BATCH_INSERT_BD = 2000;
	
	private ConfigCustoGama parametros = null;
	private int mode;
	private boolean gerarProbabilidade;
	private boolean gravarNoBanco;
	private boolean usarModeloExistente = false;
	private boolean calculaCustoGama = false;
	private boolean normalizar = false;
	//private String dirSVM = "/usr/ferramentas/libsvm/";
	private String dirSVM = "/ferramentas/";
	

	public SVM(int mode)
	{
		super(false);
		this.mode = mode;
	}
	public SVM()
	{
		super(false);
	}
	public void setNormalizar(boolean norm)
	{
		this.normalizar = norm;
	}
	public void setParametros(ConfigCustoGama parametros)
	{
		this.parametros = parametros;
		calculaCustoGama = false;
	}
	public void setDirSVM(String dirSVM)
	{
		this.dirSVM = dirSVM;
	}
	public void setGerarProbabilidade(boolean gerar)
	{
		this.gerarProbabilidade = gerar;
	}
	public ValorResultado getResultado(List<ResultadoItem> lstResults) throws SQLException
	{
		if(this.mode == MODE_CLASSIFICATION) 
		{ 
			return CalculaResultados.getResultado(lstResults,MetricaUsada.ACURACIA,null,0);
		}else
		{
			if(this instanceof GenericoLetorLike)
			{
				return CalculaResultados.getResultado(lstResults,MetricaUsada.NDCG_EXP,null,10);
			}else
			{
				return CalculaResultados.getResultado(lstResults,MetricaUsada.MSE,null,0);
			}
		}
	} 
	
	/**
	 * Para usar sem o banco de dados 
	 * @param gravarNoBanco
	 * @param nomExperimento
	 * @param custo
	 * @param gama
	 * @param epslon
	 * @param mode
	 * @param gerarProbabilidade
	 * @param col
	 */
	public SVM(boolean gravarNoBanco,String nomExperimento,float custo,float gama,float epslon,int mode,boolean gerarProbabilidade,Colecao col)
	{
		super(gravarNoBanco);
		parametros = new ConfigCustoGama((double) custo,(double) gama,(double) epslon);

		this.gerarProbabilidade = gerarProbabilidade;
		this.gravarNoBanco = gravarNoBanco;
		this.mode = mode;
		this.nomExperimento = nomExperimento;
		calculaCustoGama = false;
	}
	
	/**
	 * Como o bd
	 * @param nomExperimento
	 * @param custo
	 * @param gama
	 * @param epslon
	 * @param mode
	 * @param gerarProbabilidade
	 * @param gravarNoBanco
	 * @param col
	 */
	public SVM(String nomExperimento,float custo,float gama,float epslon,int mode,boolean gerarProbabilidade,boolean gravarNoBanco,Colecao col)
	{
		super(col,gravarNoBanco);
		parametros = new ConfigCustoGama((double) custo,(double) gama,(double) epslon);

		this.gerarProbabilidade = gerarProbabilidade;
		this.gravarNoBanco = gravarNoBanco;
		this.mode = mode;
		this.nomExperimento = nomExperimento;
		calculaCustoGama = false;
	}
	public SVM(String nomExperimento,float custo,float gama,float epslon,int mode,boolean gerarProbabilidade,boolean gravarNoBanco)
	{
		this(nomExperimento,custo,gama,epslon,mode,gerarProbabilidade,gravarNoBanco,null);
		calculaCustoGama = false;
	}
	public SVM(String nomExperimento,float epslon,int mode,boolean gerarProbabilidade,boolean gravarNoBanco,Colecao col)
	{
		this(nomExperimento,-1,-1,epslon,mode,gerarProbabilidade,gravarNoBanco,col);
		calculaCustoGama = true;
	}
	public SVM(String nomExperimento,float epslon,int mode,boolean gerarProbabilidade,boolean gravarNoBanco)
	{
		this(nomExperimento,-1,-1,epslon,mode,gerarProbabilidade,gravarNoBanco,null);
		calculaCustoGama = true;
	}
	public void setMode(int mode)
	{
		this.mode = mode;
	}
	public File filtraIDsArquivo(File arquivoIn, File arquivoOut) throws IOException
	{
		//System.out.println("oi");
		/*
		String textoSaida = ArquivoUtil.leTexto(arquivoIn);
		ArquivoUtil.gravaTexto(textoSaida, arquivoOut, false);
		*/
		
		File arqTmp = File.createTempFile("met", ".amostra");
		arqTmp.deleteOnExit();
		
		BufferedReader in = new BufferedReader(new FileReader(arquivoIn));
		BufferedWriter out = new BufferedWriter(new FileWriter(arqTmp, false),100);
		String strLinha;
		StringBuilder texto = new StringBuilder();
		while ((strLinha = in.readLine()) != null)
		{
			//strLinha = strLinha.replaceAll("#id: [0-9]+", " ");
			strLinha = strLinha.replaceAll(":[ ]+", ":");
			strLinha = strLinha.replaceAll("( |\t)[0-9]+( |\t)", " ");
			out.write(strLinha+"\n");
		}
		in.close();
		out.close();
		
		Sys.executarComando("cp "+arqTmp.getAbsolutePath()+" "+arquivoOut.getAbsolutePath(),false);
		
		return arquivoOut;
		
	}

	public boolean isClassificacao()
	{
		//System.out.println("Classificacao? "+(this.mode == MODE_CLASSIFICATION));
		return this.mode == MODE_CLASSIFICATION;
	}
	public Map<String,String> getParams()
	{
		HashMap<String,String> mapParams =  new HashMap<String,String>();
		mapParams.put("c",  Double.toString(this.parametros.getCusto()));
		mapParams.put("g",  Double.toString(this.parametros.getGama()));
		if(mode == MODE_REGRESSION)
		{
			mapParams.put("e",  Double.toString(this.parametros.getEpslon()));
			mapParams.put("mode",  "regression");
		}else
		{
			mapParams.put("mode",  "classification");
		}
		
		return mapParams;
	}
	
	public void treinaParametros(File arquivo) throws IOException
	{
		if(calculaCustoGama)
		{
			calcularCustoGama(arquivo,NUM_FOLDS_CUSTO_GAMA);
		}
	}
	public void calcularCustoGama(File arquivo,int numFolds) throws IOException
	{
		File pathParam = new File(arquivo.getParentFile().getAbsolutePath()+"/parametros/");
		if(!pathParam.exists())
		{
			pathParam.mkdir();
		}
		File arquivoResultadoCustoGama = new File(pathParam.getAbsoluteFile()+"/"+arquivo.getName()+"_param"+this.mode+".outJava");
		parametros = ConfigCustoGama.leArquivo(arquivoResultadoCustoGama); 

		if(parametros == null)
		{
			double custo,gama,epslon;
			File arquivoFiltrado = filtraIDsArquivo(arquivo, new File(pathParam.getAbsoluteFile()+"/"+arquivo.getName()+"_param"));
			String nomGrid = "grid_classification.sh";
			if(mode == MODE_REGRESSION)
			{
				nomGrid = "grid_regression.sh";
			}
			
			String result = Sys.executarComando("/usr/ferramentas/libsvm/"+nomGrid+" "+numFolds+"  "+arquivoFiltrado.getAbsolutePath(),true);
			String[] results = result.split(" ");
			System.out.println("Linha do resultado: "+result);
			custo = Float.parseFloat(results[0]);
			gama = Float.parseFloat(results[1]);
			if(mode == MODE_REGRESSION)
			{
				epslon = Float.parseFloat(results[2]);
			}else
			{
				epslon = 0;
			}
			parametros = new ConfigCustoGama(custo,gama,epslon,arquivoFiltrado,null);
			System.out.println(parametros.toString());
			parametros.gravaArquivo(arquivoResultadoCustoGama);
		}else
		{
			System.out.println("Ja computou:"+parametros.toString());
		}
	}
	public void setUsarModeloExistente(boolean usarModeloExistente )
	{
		this.usarModeloExistente = usarModeloExistente;
	}
	public boolean isUsarModeloExistent()
	{
		return this.usarModeloExistente;
	}
	
	
	@Override
	public ArrayList<ResultadoItem> testar(Fold fold) throws Exception {
		
		String nomeBase = fold.getTreino().getName().replaceAll("\\..*", "");
	    String treino = fold.getTreino().getAbsolutePath();
	    String teste = fold.getTeste().getAbsolutePath();
	    String pathDiretorio = fold.getTreino().getParentFile().getAbsolutePath();

	    
    	String resp ="";
    	File modeloTreino = testar(fold, nomeBase, treino, teste, pathDiretorio);
    	fold.setModeloTreino(modeloTreino);
    	
    	if(!fold.existeResultado())
    	{
    		throw new Exception("Arquivo de resultado inexistente");
    	}
    	
    	
		// TODO Auto-generated method stub
		return parseResult(fold);
	}
	public File testar(Fold fold, String nomeBase, String treino,
			String teste, String pathDiretorio) throws IOException {
		String resp;

		
		
		File modeloTreino = new File(treino+"Model");
		//fold.setPredict(new File(pathDiretorio+"/"+nomeBase+".predict"+fold.getNum()) );
		System.out.println("Predict file: "+fold.getPredict()+"  Get pre calculado? "+this.isGetResultPreCalculado());
    	if( (!this.isGetResultPreCalculado()) || fold.getPredict() == null || !fold.getPredict().exists())
    	{
	    	/******************Executa treino*************************/
	    	if(fold.getModeloTreino() == null || (modeloTreino.exists() && !usarModeloExistente))
	    	{
	    		if(calculaCustoGama)
	    		{
	    			this.calcularCustoGama(fold.getOrigem(),NUM_FOLDS_CUSTO_GAMA);
	    		}
	    		resp = executaTreino(fold, treino);
	   			
	    	}else
	    	{
	    		System.err.println("ATENCAO! usando modelo de treino jah existente");
	    	}
    	
	    	/******************Testa************************/
	    	long tempo = System.currentTimeMillis();
	    	if(this.normalizar)
	    	{
	    		DatasetUtil.calculaScale(teste, teste);
	    	}
	    	resp=Sys.executarComando(dirSVM+"testar.sh "+(gerarProbabilidade?"1":"0")+" "+teste+" "+modeloTreino+" "+fold.getTreino().getParent()+"/"+nomeBase+".predict"+fold.getNum(),false,fold.getTreino().getParent());
    	
	    	fold.setPredict(new File(pathDiretorio+"/"+nomeBase+".predict"+fold.getNum()) );
	    	fold.setTempoExecucaoTeste(System.currentTimeMillis()-tempo);
    	}else
    	{
    		System.err.println("ATENCAO! usando Teste/Treino já existente");
    	}
		return modeloTreino;
	}
	private String executaTreino(Fold fold, String treino) throws IOException {
		String resp;
		File treinoExec = File.createTempFile("treino", ""); 
		treinoExec.deleteOnExit();
		this.filtraIDsArquivo(new File(treino), treinoExec);
		if(this.normalizar)
    	{
    		DatasetUtil.calculaScale(treinoExec.toString(), treinoExec.toString());
    	}
		Double epslon = 0.1;
		Double custo = 1.0;
		if(parametros != null)
		{
			epslon = parametros.getEpslon();
			custo = parametros.getCusto();
		}
		if(parametros == null || parametros.getGama() == null)
		{
			
			resp = Sys.executarComando(dirSVM+"treinar_linear.sh "+(gerarProbabilidade?"1":"0")+" "+epslon+" "+(mode==MODE_CLASSIFICATION?"0":mode)+" "+custo+" "+treinoExec.getAbsolutePath()+" "+treino+"Model",false,fold.getTreino().getParent());	
		}else
		{
			resp = Sys.executarComando(dirSVM+"treinar.sh "+(gerarProbabilidade?"1":"0")+" "+this.parametros.getEpslon()+" "+(mode==MODE_CLASSIFICATION?"0":mode)+" "+this.parametros.getCusto()+" "+parametros.getGama()+" "+treinoExec.getAbsolutePath()+" "+treino+"Model",false,fold.getTreino().getParent());
			
		}
		return resp;
	}
	public ArrayList<ResultadoItem> parseResult(Fold fold) throws Exception
	{
        //grava fold no banco
		Connection conn = null;
		
        if(gravarNoBanco) 
        {
        	conn = GerenteBD.getGerenteBD().obtemConexao("");
        	conn.setAutoCommit(false);
        	fold.inserir();//ok (sem close ok)
        }	
		
		ArrayList<ResultadoItem> resultados = new ArrayList<ResultadoItem>(); 
		
		//le arquivos e contabliza o resultado na matriz
		BufferedReader arqResult = new BufferedReader(new FileReader(fold.getPredict()));
		BufferedReader arqTeste = new BufferedReader(new FileReader(fold.getTeste()));
		BufferedReader arqPageIds = new BufferedReader(new FileReader(fold.getIdsFile()));
		

        String pageId;

        String result;
        int[] ordemProbs = null;
        int count = 0;
        int maxClass = 0;
        while ((result = arqResult.readLine()) != null) 
        {
        	if(result.startsWith("LABEL") || result.startsWith("label"))
        	{
        		//ordena o label
        		String[] strOrdemProbs =  StringUtil.removeDoubleSpace(result.toLowerCase().replaceAll("labels ", "")).split(" |\t");
        		ordemProbs = new int[strOrdemProbs.length];
        		for(int i =0; i<strOrdemProbs.length ; i++)
        		{
        			int idxFloatVal = strOrdemProbs[i].indexOf(".");
        			if(idxFloatVal > 0)
        			{
        				strOrdemProbs[i] = strOrdemProbs[i].substring(0,idxFloatVal);
        			}
        			if(maxClass < Integer.parseInt(strOrdemProbs[i]))
        			{
        				maxClass = Integer.parseInt(strOrdemProbs[i]);
        			}
        			ordemProbs[i] = Integer.parseInt(strOrdemProbs[i]);
        		}
        		
        		continue;//ignora linha inicial se necessario
        	}
        	
        	String[] resultArray = StringUtil.removeDoubleSpace(result).split(" |\t");
        	
        	//System.out.println("Result: "+result+" Result Split: "+resultArray[0]);
        	Map<Integer,Float> probs = new HashMap<Integer,Float>();
        	
        	//procura a probabilidade dele ser do resultado obtido
        	for(int i = 1 ; i<resultArray.length ; i++)
        	{
        		
        		//probs[ordemProbs[i-1]] = Float.parseFloat(resultArray[i]);
        		probs.put(ordemProbs[i-1], Float.parseFloat(resultArray[i]));        		
        	}
        	String linhaTeste = arqTeste.readLine();
        	pageId = arqPageIds.readLine(); 
        	float classeReal = Float.parseFloat(linhaTeste.split(" |\t")[0]);
        	float classePrevista = Float.parseFloat(resultArray[0]);
        	
        	ResultadoItem rItem = new ResultadoItem(Integer.parseInt(pageId),classeReal,classePrevista,probs,gravarNoBanco);
        	if(linhaTeste.contains("qid"))
        	{
        		Integer qid = this.getIdPorLinhaArquivo(linhaTeste, "qid");
        		if(qid != null)
        		{
        			rItem.setQID(qid);
        		}
        	}
        	rItem.setOrder(count);
        	rItem.setFold(fold);
        	
        	if(gravarNoBanco)
        	{
        		/*
        		if(count == 0)
        		{
        			rItem.excluirProbResult(nomExperimento);//ok (sem close ok)
        		}
        		rItem.gravaResultadoBanco(this.nomExperimento,true);//ok (sem close ok)
        		
        		if(ResultadoItem.getNumProbResultOnBatch() > NUM_BATCH_INSERT_BD)
        		{
        			ResultadoItem.executeBatchResult();
        		}
        		if(ResultadoItem.getNumProbResultOnBatch() > NUM_BATCH_INSERT_BD)
        		{
        			ResultadoItem.executeBatchProbResult();
        		}
        		*/
        		ResultadoItemDelayedRecord.getGravador().addResult(new ResultadoItemToRecord(rItem, nomExperimento));
        	}
        	resultados.add(rItem);        	
        	count++;
        }
        //ResultadoItem.executeBatchResult();
        //ResultadoItem.executeBatchProbResult();
        arqPageIds.close();
        arqResult.close();
        arqTeste.close();
        
        //grava parametros utilizados
        if(gravarNoBanco)
        {
        	if(this.parametros != null)
        	{
	        	fold.adicionaParam("c", Double.toString(this.parametros.getCusto()));
	        	fold.adicionaParam("g", Double.toString(this.parametros.getGama()));
	        	fold.adicionaParam("epslon", Double.toString(this.parametros.getEpslon()));
        	}
        	System.out.println(">BD: Results pra adicionar: "+ResultadoItemDelayedRecord.getNumResults());
        	fold.inserirParams(); //(sem close ok)
        	conn.commit();
        	conn.setAutoCommit(true);
        }
        
        return resultados;
	}
	public void setGravarNoBanco(boolean gravarNoBanco) throws SQLException, ClassNotFoundException
	{
		this.gravarNoBanco = gravarNoBanco;
		if(gravarNoBanco)
		{
			super.inicializaBD();
		}
	}
	public static void main(String[] args) throws Exception
	{
		SVM s = new SVM("qa_multiview_allFeats_stack_allFeats__RankLibDinamicFeatures_combinacao_RandomForest_stack.amostra_TamIgualTreino",0.1F,1,true,true);
		Fold[] fs = s.getResultsPorFoldBanco();
		
		for(int i = 10 ; i < 5000 ; i *= 5)
		{
			s.setNomExperimento("teste_batch_"+i);
			SVM.NUM_BATCH_INSERT_BD = i;
			long time = System.currentTimeMillis();
			
			s.parseResult(fs[0]);
			
			System.out.println("GASTO: "+ (System.currentTimeMillis()-time)/1000 +" segundos. TESTANDO COM i = "+i);
		}
		System.exit(0);
		
		
		
		//for(int i =1 ;i<=3 ; i++)
		//{
			SVM metodo = new SVM("xx",1,1,false,false);
			metodo.setArquivoOrigem(new File("/home/hasan/Dropbox/ferramentas/combinador_stacking/toy_example_rank/colecao.amostra"));
			
			metodo.testar(new File("/home/hasan/Dropbox/ferramentas/combinador_stacking/toy_example_rank/view3/colecao.amostra"),3,true,"id","qid",false);
		//}
		
		/*
		Fold f = new Fold(1,new File(diretorio+"muppets_grafo.treino0"),new File(diretorio+"muppets_grafo.treino0"),new File(diretorio+"muppets_grafo.teste0"),new File(diretorio+"muppets_grafo.pageId0"));
		
		try {
			SVM metodo = new SVM("novoTesteFinal_classification",0.125F,0.5F,1,SVM.MODE_CLASSIFICATION,true,true);
			metodo.treinaParametros(f.getTreino());
			//metodo.calcularCustoGama(new File(diretorio+"teste.amostra"),10);

			Iterator<ResultadoItem> i = metodo.testar(f).iterator();
			while(i.hasNext())
			{
				System.out.println(i.next());
			}
			System.out.println("oia! chegou aqui!");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		*/                                      
	}
	@Override
	public Integer getIdPorLinhaArquivo(String linha, String nomIdentificador)
	{

		//System.out.println(linha);
		String id = "";
		if(nomIdentificador.equalsIgnoreCase("id"))
		{
			id = PadraoString.resgataPadrao("(\t| )[0-9]+(\t| )", linha).replaceAll("\t| ", "").trim();
		}
		if(id.replaceAll("\t| ", "").length() == 0)
		{
			id = PadraoString.resgataPadrao("(#|@)"+nomIdentificador+":( )*[0-9]+", linha);
			id = id.replaceAll("(#|@)"+nomIdentificador+":( )*", "").trim();
		}
		
		//System.out.println("id: "+id);
		if(id.length() == 0 || id.replaceAll("\t| ", "").length() == 0)
		{
			
			return null;
		}
		Integer intId =Integer.parseInt(id);  
		return intId;

	}
	
	@Override
	public Integer getIdPorLinhaArquivo(String linha) 
	{
		
		return getIdPorLinhaArquivo(linha,"id");
	}
	
	@Override
	public String gerarLinhaDataset(double classe, String[] features) {
		// TODO Auto-generated method stub
		StringBuilder linhaFeatures = gerarLinhaFeatures(features,1);
		
		return StringUtil.removeDoubleSpace(classe+linhaFeatures.toString());
	}
	public StringBuilder gerarLinhaFeatures(String[] features,int firstFeatureNum)
	{
		StringBuilder linhaFeatures = new StringBuilder("");
		for(int i = 0 ; i<features.length ; i++)
		{
			if(features[i].length() >0)
			{
				linhaFeatures.append(" "+firstFeatureNum+":"+features[i]);
			}
			firstFeatureNum++;
		}
		return linhaFeatures;
	}
	
	@Override
	public String getClasseReal(String linha)
	{
		// TODO Auto-generated method stub
		String classReal = linha.substring(0,getInitFeatures(linha)).trim();
		if(!classReal.matches("[-.0-9]+"))
		{
			System.err.println("Class not match as real number: "+classReal);
		}
		return classReal;
	}
	
	@Override
	public String getFeaturesString(String linha)
	{
		// TODO Auto-generated method stub
		int idxIniFeat = getInitFeatures(linha);
		return linha.replaceAll("( |\t)[0-9]+( |\t)", " ").replaceAll("#.*","").substring(idxIniFeat).trim();
	}
	private int getInitFeatures(String linha) {
		int idxPrimSpace = linha.indexOf(" ");
		int idxPrimTab = linha.indexOf("\t");
		int idxIniFeat = -1;
		if(idxPrimTab < 0 || idxPrimSpace < 0)
		{
			idxIniFeat = (idxPrimTab<0)?idxPrimSpace:idxPrimTab;
		}else
		{
			idxIniFeat = idxPrimTab<idxPrimSpace?idxPrimTab:idxPrimSpace;
		}
		return idxIniFeat;
	}
	public Integer getIndexFeature(String nameValue)
	{
		return Integer.parseInt(nameValue.substring(0, nameValue.indexOf(":")).trim());
	}
	
	@Override
	public HashMap<Long, String> getFeaturesVector(String linha)
	{
		// TODO Auto-generated method stub
		String[] arrFeatures = getFeaturesString(linha).split(" |\\t");
		
		
		return getFeaturesVector(arrFeatures);
	}
	private HashMap<Long, String> getFeaturesVector(String[] arrFeatures)
	{
		HashMap<Long, String> mapFeaturesValues = new HashMap<Long, String>();
		if(arrFeatures.length == 0 || (arrFeatures.length == 1 && arrFeatures[0].length()==0))
		{
			return mapFeaturesValues;
		}
		
		int lastFeature = getIndexFeature(arrFeatures[arrFeatures.length-1]);
		/*
		//blank features
		for(int i =0; i<arrLstFeaturesValues.length ; i++)
		{
			arrLstFeaturesValues[i] = "";
		}
		*/
		
		//fill then
		List<String> lstFeaturesVal = new ArrayList<String>();
		for(int i=0; i< arrFeatures.length ; i++)
		{
			
			if(arrFeatures[i].matches("( )?[0-9]+:.+"))
			{
				int featNum = getIndexFeature(arrFeatures[i]);
				mapFeaturesValues.put((long)featNum, arrFeatures[i].replaceAll("[0-9]+:", "").trim());
				
			}else
			{
				System.err.println("NOT MATCH : "+arrFeatures[i]);
			}
		}
		
		return mapFeaturesValues;
	}
	
	@Override
	public String gerarLinhaDataset(double classe, int id, String[] features)
	{
		// TODO Auto-generated method stub
		return this.gerarLinhaDataset(classe, features)+" #id:"+Integer.toString(id);
	}
	
	
	@Override
	public String gerarLinhaDataset(double classe, int id,	HashMap<Long, String> features) {
		// TODO Auto-generated method stub
		return StringUtil.removeDoubleSpace(classe+" "+gerarLinhaFeatures(features)+" #id:"+Integer.toString(id));
	}
	
	@Override
	public String gerarLinhaDataset(double classe, int id,HashMap<Long, String> features, Map<String, String> paramsComment)
	{
		String params = criaCommentString(id, paramsComment);
		// TODO Auto-generated method stub
		return classe+" "+gerarLinhaFeatures(features).toString().trim()+" "+params.trim();
	}
	public String criaCommentString(int id, Map<String, String> paramsComment)
	{
		paramsComment.put("id", Integer.toString(id));
		
		String params = "";
		boolean isFirst = true;
		for(String paramId : paramsComment.keySet())
		{
			String val = (paramId.toLowerCase().contains("id")?paramsComment.get(paramId):"\"\""+paramsComment.get(paramId)+"\"\"")+" ";
			if(isFirst)
			{
				params += "#"+paramId+":"+val;
				isFirst = false;
			}else
			{
				params += "@"+paramId+":"+val;
			}
		}
		return params;
	}
	
	@Override
	public String gerarLinhaDataset(double classe,HashMap<Long, String> features) {
		// TODO Auto-generated method stub
		return classe+" "+gerarLinhaFeatures(features);
	}
	
	@Override
	public StringBuilder gerarLinhaFeatures(HashMap<Long, String> features) {
		// TODO Auto-generated method stub
		
		StringBuilder linhaFeatures = new StringBuilder("");
		List<Long> lstFeatIndex = new ArrayList<Long>(features.keySet());
		Collections.sort(lstFeatIndex);
		
		for(long key : lstFeatIndex)
		{
			String featVal = features.get(key);
			if(featVal != null && featVal.length()>0)
			{
				linhaFeatures.append(" "+key+":"+features.get(key));
			}
			
		}
		return linhaFeatures;
	}
//
	public boolean linhaMatchesFormat(String linha)
	{
		//System.out.println(linha);
		//System.out.println(RegExpsConst.DIGITO_FLOAT_OPCIONAL+"(( )+"+RegExpsConst.DIGITO+")?(( )+[0-9]+:"+RegExpsConst.DIGITO_FLOAT_OPCIONAL+")+( )*");
		return linha.matches(RegExpsConst.DIGITO_FLOAT_OPCIONAL+"(( )+"+RegExpsConst.DIGITO+")?(( )+[0-9]+:"+RegExpsConst.DIGITO_FLOAT_OPCIONAL+")+( )*(#.*)");
	}
	@Override
	public MetodoAprendizado clone() {
		// TODO Auto-generated method stub
		SVM objSVM = new SVM(nomExperimento,parametros.getEpslon().floatValue(),mode,this.gerarProbabilidade,gravarNoBanco);
		objSVM.setColecao(objSVM.getColecao());
		objSVM.setGerarProbabilidade(this.gerarProbabilidade);
		objSVM.setParametros(parametros);
		objSVM.setUsarModeloExistente(usarModeloExistente);
		objSVM.setArquivoOrigem(this.getArquivoOrigem());
		objSVM.setNomExperimento(this.getNomExperimento());
		objSVM.setNormalizar(this.normalizar);
		return objSVM;
	
	
	}
	@Override
	public String gerarCabecalhoResultado(List<String> lstClasses) {
		// TODO Auto-generated method stub
		String cabecalho = "LABELS";
		for(String classe : lstClasses)
		{
			cabecalho += " "+classe;
		}
		return cabecalho;
	}
	@Override
	public String gerarLinhaResultado(ResultadoItem ri) {
		// TODO Auto-generated method stub
		String resultPorClasse = "";
		if(ri.getProbPorClasse() != null)
		{
			for(float prob : ri.getProbPorClasse())
			{
				resultPorClasse += " "+prob;
			}
		}
		return ri.getClassePrevista()+" "+resultPorClasse;
	}


	
	
}
