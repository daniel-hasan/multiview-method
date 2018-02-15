package aprendizadoUtils;

import io.Sys;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import string.StringUtil;
import stuctUtil.ListaAssociativa;
import stuctUtil.Tripla;
import aprendizadoResultado.CalculaResultados;
import aprendizadoResultado.ResultadoAnalyser;
import aprendizadoResultado.ValorResultado;
import aprendizadoResultado.ValorResultado.MetricaUsada;
import aprendizadoResultado.ValorResultadoIteracoes;
import arquivo.ArquivoUtil;
import banco.GerenteBD;
import config_tmp.Colecao;
import entidadesAprendizado.Fold;
import entidadesAprendizado.FoldIds;
import entidadesAprendizado.Resultado;
import entidadesAprendizado.ResultadoItem;
import entidadesAprendizado.ResultadoItemViews;
import featSelector.ValorResultadoMultiplo;

public abstract class MetodoAprendizado implements Serializable
{ 
	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;
	private static final String DIRETORIO_TENFOLD = "/usr/ferramentas/tenfold";
	public abstract ArrayList<ResultadoItem> testar(Fold fold) throws  Exception;
	private static HashMap<Long,PreparedStatement> stmtSelectResult = new HashMap<Long,PreparedStatement>();
	private static HashMap<Long,PreparedStatement> stmtSelectFoldInicial = new HashMap<Long,PreparedStatement>();
	private static HashMap<Long,PreparedStatement> stmtSelectProbResult = new HashMap<Long,PreparedStatement>();
	private static HashMap<Long,PreparedStatement> stmtSelectClasses = new HashMap<Long,PreparedStatement>();
	
	
	
	protected String nomExperimento;
	protected File arqFonte;
	private boolean dividirFoldsBanco = false;
	private boolean getResultPreCalculado = false;
	
	private Colecao colecao;
	private HashMap<Long,String> mapIdPorLinha  = new HashMap<Long, String>();
	private String[] linhasLida;
	public MetodoAprendizado()
	{
		try
		{
			inicializaBD();
			dividirFoldsBanco = true;
		} catch (SQLException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public MetodoAprendizado(boolean inicializaBD)
	{
		if(inicializaBD)
		{
			try
			{
				inicializaBD();
			} catch (SQLException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void setGetResultPreCalculado(boolean getResult)
	{
		this.getResultPreCalculado = getResult;
	}
	public boolean isGetResultPreCalculado()
	{
		return this.getResultPreCalculado;
	}
	public MetodoAprendizado(Colecao col,boolean gravarNoBanco)
	{
		this(gravarNoBanco);
		this.colecao = col;
	}
	public MetodoAprendizado(Colecao col)
	{
		this();
		this.colecao = col;
		
	}
	public void setDividirFoldBanco(boolean dividirFoldsBanco)
	{
		this.dividirFoldsBanco = dividirFoldsBanco;
	}
	public Fold[] testar(File arquivo,int numFolds) throws Exception
	{
		return testar(arquivo,numFolds,false,"id","",false);
	}
	public Fold[] testar(File arquivo,int numFolds,boolean usarValidacao,String id_name,String group_id,boolean groupPerLength) throws Exception
	{
		
		Fold[] folds = criaFoldsTeste(arquivo, numFolds, usarValidacao,	id_name, group_id, groupPerLength);
			//dividePerFoldsByIds(ids, arquivo.getName().split("\\.")[0], "folds"+arquivo.getName().split("\\.")[0], numFolds);
		System.out.println("Testando....");
		return testar(folds);
	}
	public Fold[] criaFoldsTeste(File arquivo, int numFolds,boolean usarValidacao, String id_name, String group_id,boolean groupPerLength) throws IOException
	{
		List<Long>[] idsPerFold = divideFileIntoFolds(arquivo, numFolds,id_name, group_id, groupPerLength,Fold.SEED_DEFAULT);
		/*
		for(Fold f : folds)
		{
			f.setValidationFiles(validation, validationIds);
		}
		*/
		Fold[] folds = criaFolds(idsPerFold,"folds_"+this.nomExperimento,new ArrayList<ResultadoItemViews>(),new ArrayList<String>(),usarValidacao);
		return folds;
	}
	public List<Long>[] divideFileIntoFolds(File arquivo, int numFolds,String id_name, String group_id, boolean groupPerLength,long seed) throws IOException
	{
		return divideFileIntoFolds( arquivo,  numFolds, id_name,  group_id,  groupPerLength, seed, true);
	}
	public List<Long>[] divideFileIntoFolds(File arquivo, int numFolds,String id_name, String group_id, boolean groupPerLength,long seed,boolean defineFoldsInFile)
			throws IOException {
		File arqFonteAntigo = this.arqFonte;
		this.arqFonte = arquivo;
		
		

		//cria o fold de validação se necessario
		/*
		File validation = new File(arquivo.getAbsoluteFile()+".validation");
		File validationIds = new File(arquivo.getAbsoluteFile()+".validationIds");
		if(porcValidationExterna >0)
		{
			System.out.println("Dividindo folds validação... (divide into folds)");
			List<Long>[] idsPerFold = Fold.divideIntoFolds(2,ids, (int) Math.round(ids.size()*porcValidationExterna));
			System.out.println("Cria folds ids filtrado");
			criaArquivoIdsFiltrado(idsPerFold[0],validation,validationIds);
			ids = idsPerFold[1];
		}
		*/
		System.out.println("Get ids...");
		List<Long>[] idsPerFold  = null;
		
		/************************************** se tiver folds pre-definidos, usa-los ***********************************/
		List<Long> foldsPerLine = null;
		try{
			foldsPerLine = defineFoldsInFile?this.getIds(arqFonte, "fold_id"):new ArrayList<Long>();
		}catch(IdNotFoundException e) {
			foldsPerLine = new ArrayList<Long>();
		}
		List<Long> ids = new ArrayList<Long>();
		Set<Long> setFoldsNum = new HashSet<Long>();
		Map<Long,Integer> idxPerFoldNum = new HashMap<Long,Integer>();
		if(foldsPerLine.size()!= 0)
		{
			ids= this.getIds(arqFonte);
			
			
			//mapeia um indice por fold
			setFoldsNum = new HashSet<Long>(foldsPerLine);
			setFoldsNum.addAll(foldsPerLine);
			
			//ordena lista e deixa cada fold num (do menor para o menor) com um indice sequencial de 0 a n sendo que n é o numero de folds
			List<Long> lstFoldsNum = new ArrayList<Long>(setFoldsNum);
			Collections.sort(lstFoldsNum);
			
			int idx = 0;
			for(long foldNum : lstFoldsNum)
			{
				idxPerFoldNum.put(foldNum, idx);
				idx++;
			}
		}
		
		//se tiver, resgata dele o folds per por linha e agrupa no fods
		if(foldsPerLine.size()!= 0 && foldsPerLine.size()==ids.size() && idxPerFoldNum.keySet().size() == numFolds)
		{
			idsPerFold = new ArrayList[numFolds];
			for(int i =0 ; i<foldsPerLine.size() ; i++)
			{
				int foldNum = foldsPerLine.get(i).intValue();
				int idxVal = idxPerFoldNum.get((long)foldNum); 
				if(idsPerFold[idxVal] == null)
				{
					idsPerFold[idxVal] = new ArrayList<Long>();	
				}
				idsPerFold[idxVal].add(ids.get((int)i));
			}
		}else
		{
		
			/*************************** Nao tem fold pre definido e nao tem grupo ***********************/
			if(group_id.length() == 0 || id_name.equals(group_id))
			{
				if(ids.size()==0)
				{
					ids= this.getIds(arqFonte);
				}
				System.out.println("Dividindo folds...");
				idsPerFold = Fold.divideIntoFolds(numFolds, ids,seed); 
						
			}
			else
			{
				/*************************** Nao tem fold pre definido e tem grupo ***********************/
				idsPerFold = getIdsFoldsPerGroup(numFolds, this.arqFonte, group_id, id_name,groupPerLength,seed);
			}
		}
		if(arqFonteAntigo != null)
		{
			this.arqFonte = arqFonteAntigo;
		}
		return idsPerFold;
	}
	public ValorResultado getResultadoGeral(Fold[] foldResults) throws SQLException
	{
		List<ResultadoItem> lstResultados = new ArrayList<ResultadoItem>();
		for(int i =0 ; i<foldResults.length ; i++)
		{
			lstResultados.addAll(foldResults[i].getResultadosValues());
		}
		
		return getResultado(lstResultados);
		
	}
	public ValorResultado getResultadoGeral(Fold[] foldResults,MetricaUsada metrica,Integer k,double minClasse) throws SQLException
	{
		List<ResultadoItem> lstResultados = new ArrayList<ResultadoItem>();
		
		for(int i =0 ; i<foldResults.length ; i++)
		{
			lstResultados.addAll(foldResults[i].getResultadosValues());
		}
		System.out.println("Numero de instancias com resultado: "+lstResultados.size());
		return CalculaResultados.getResultado(lstResultados,metrica,k,minClasse);
		
	}
	public ValorResultadoMultiplo getResultadoPorIteracao(Fold[] foldResults,MetricaUsada metrica,Integer k,double minClasse) throws SQLException
	{
		return getResultadoPorIteracao( foldResults, metrica, k, minClasse,null);
	}
	public ValorResultadoMultiplo getResultadoPorIteracao(Fold[] foldResults,MetricaUsada metrica,Integer k,double minClasse,ResultadoAnalyser resultAnalyser) throws SQLException
	{
		
		ValorResultado[] valsPorFold = new ValorResultado[foldResults.length];
		for(int i =0 ; i<foldResults.length ; i++)
		{
			valsPorFold[i] = CalculaResultados.getResultado(foldResults[i].getResultadosValues(),metrica,k,minClasse,resultAnalyser);
			
		}
		
		return new ValorResultadoMultiplo(valsPorFold);
	}
	public ValorResultadoIteracoes getResultadoPorIteracao(Fold[] foldResults) throws SQLException
	{
		double[] vals = new double[foldResults.length];
		MetricaUsada metrica = null;
		for(int i =0 ; i<foldResults.length ; i++)
		{
			ValorResultado vResult = getResultado(foldResults[i].getResultadosValues());
			vals[i] = vResult.getResultado();
			metrica = vResult.getMetrica();
		}
		return new ValorResultadoIteracoes(vals, metrica);
	}
	public ValorResultado getResultado(List<ResultadoItem> lstResults) throws SQLException
	{
		return CalculaResultados.getResultado(lstResults,MetricaUsada.ACURACIA,null,0);
	} 
	public Fold[] testar(File arquivo) throws Exception
	{
		/*
		this.arqFonte = arquivo;
		Fold[] folds = criaTenFolds(arquivo,arquivo.getName().replaceAll("\\..*", ""));
		
		folds = testar(folds);
		*/
		return testar(arquivo,10);
	}
	public void setArquivoOrigem(File arqOrigem)
	{
		this.arqFonte = arqOrigem;
	}
	public File getArquivoOrigem()
	{
		return this.arqFonte;
	}

	private class ExecutarMetodoAprendizado implements Runnable
	{
		private MetodoAprendizado objMetodo;
		private Fold fold;
		
		public ExecutarMetodoAprendizado(MetodoAprendizado objMetodo,Fold fld)
		{
			this.objMetodo = objMetodo;
			this.fold = fld;
		}
		
		@Override
		public void run() {
			try {
				fold.setResultados(objMetodo.testar(fold));
			} catch (Exception e) {
				e.printStackTrace();
			}
			/*
			try {
				//fecha conexao da thread atual
				Long idCurrentThread = Thread.currentThread().getId();
				Connection conn = GerenteBD.getGerenteBD().obtemConexao(idCurrentThread +"");
				conn.close();
			} catch (ClassNotFoundException e) {

				e.printStackTrace();
				System.exit(0);
			} catch (SQLException e) {
				System.out.println("Erro SQL!");
			}
			*/
		}
		
		public Fold getFold()
		{
			return this.fold;
		}
		
	}
	private boolean UTILIZAR_MULTITHREAD = false;
	private int LIMITE_THREADS = 3;
	
	public boolean isUsingMultithread()
	
	{
		return this.UTILIZAR_MULTITHREAD;
	}
	public Fold[] testar(Fold[] folds) throws Exception{
		
		this.arqFonte = folds[0].getOrigem();
		Fold[] foldsCalculado = getResultsPorFoldBanco();

		if(foldsCalculado != null)
		{
			System.out.println(">>>>>>FOLD  do exp "+this.nomExperimento+" Calculado!");
			for(int i = 0; i < foldsCalculado.length ; i++)
			{
				foldsCalculado[i].addIdsToLstSemClasse(folds[i].getIdsSemClasse());
			}
			
			return foldsCalculado;
		}/*else
		{
			System.out.println("Nao foi possivel achar o fold :( ");
			System.exit(0);
		}
		*/
		 
		if(UTILIZAR_MULTITHREAD)
		{
			//cria threads por fold e manda executar
			Thread[] arrThreads = new Thread[folds.length];
			int numThreads = 0;
			for(int i = 0; i<folds.length ; i++)
			{
				arrThreads[i] = new Thread(new ExecutarMetodoAprendizado(this,folds[i]));
				arrThreads[i].start();
				numThreads++;
				if(numThreads >= LIMITE_THREADS)
				{
					for(int j=0 ; j<=i ; j++)
					{
						arrThreads[j].join();
					}
					numThreads  = 0;
				}
				
			}
			
			//aguarda finalização das threads
			for(int i = 0; i<arrThreads.length ; i++)
			{
				arrThreads[i].join();
			}
			System.out.println("Terminou threads!");
			

		}else
		{
			for(int i = 0; i<folds.length ; i++)
			{
				//System.out.println("Testando fold#"+i);
				long tempo = System.currentTimeMillis();
				
				folds[i].setResultados(testar(folds[i]));
			}	
		}
		
		return folds;
	}
	public void treinaParametros(File arquivo) throws IOException
	{
		
	}
	public abstract  Integer getIdPorLinhaArquivo(String linha,String nomIdentificador);
	public abstract  Integer getIdPorLinhaArquivo(String linha);

	public String gerarLinhaDataset(double classe,String[] features)
	{
		HashMap<Long, String> map = stringArrayToMapFeatures(features);
		return gerarLinhaDataset(classe,map);
		
	}
	public String gerarLinhaDataset(double classe,int id, Integer qid,HashMap<Long,String> features,String params)
	{
		
		String linha = this.gerarLinhaDataset(classe, id, qid, features);
		
		return linha+" #"+params;
		//metAp.gerarLinhaDataset(classe, id, features);
	}
	
	public String gerarLinhaDataset(double classe,int id,int qid,HashMap<Long,String> features,Map<String,String> idsParamComment)
	{
		idsParamComment.put("qid", Integer.toString(qid));
		return gerarLinhaDataset(classe,id,features,idsParamComment);
	}
	
	public String gerarLinhaDataset(double classe,int id,Integer qid,HashMap<Long,String> features)
	{
		return gerarLinhaDataset(classe,id,features);
	}
	public String gerarLinhaDataset(double classe,int id,String[] features)
	{
		HashMap<Long, String> map = stringArrayToMapFeatures(features);
		return gerarLinhaDataset(classe,id,map);
	}
	public HashMap<Long, String> stringArrayToMapFeatures(String[] features) {
		HashMap<Long ,String> map = new HashMap<Long,String>();
		
		for(int i = 0 ; i<features.length ; i++)
		{
			map.put((long) i, features[i]);
		}
		return map;
	}
	public StringBuilder gerarLinhaFeatures(String[] features,int firstFeatureNum)
	{
		HashMap<Long ,String> map = new HashMap<Long,String>();
		
		for(int i = 0 ; i<features.length ; i++)
		{
			map.put((long) i+firstFeatureNum, features[i]);
		}
		return gerarLinhaFeatures(map);
	}
	public boolean foldMatchesFormat(Fold fold) throws IOException
	{
		File f = fold.getTreino();
		return fileMatchesFormat(f);
	}
	public boolean fileMatchesFormat(File f) throws IOException {
		String[] strTexto = ArquivoUtil.leTexto(f).split("\n");
		return linhaMatchesFormat(strTexto[0]);
	}
	public boolean linhaMatchesFormat(String linha)
	{
		return false;
	}
	public boolean ignoreFirstLine()
	{
		return false;
	}
	public abstract  String gerarLinhaDataset(double classe,int id,HashMap<Long,String> features,Map<String,String> paramsComment);
	public abstract  String gerarLinhaDataset(double classe,int id,HashMap<Long,String> features);
	public abstract  String gerarLinhaDataset(double classe,HashMap<Long,String> features);
	
	public abstract StringBuilder gerarLinhaFeatures(HashMap<Long,String> features);

	public abstract String gerarCabecalhoResultado(List<String> lstClasses);
	public abstract String gerarLinhaResultado(ResultadoItem ri);
	public String gerarCabecalhoDataset(String[] arrNomsFeatures)
	{
		return "";
	}
	public String gerarCabecalhoDataset(Map<Integer,String> idToNom)
	{
		//max feat q se obtem
		int maxFeat = Integer.MIN_VALUE;
		for(int featId : idToNom.keySet())
		{
			if(maxFeat < featId)
			{
				maxFeat = featId;
			}
		}
		
		//vai de 1 a last feat (o 0 é a classe) e transforma num array de string passando para o metodo implementado
		String[] arrStrCabecalho = null;
		if(maxFeat != Integer.MIN_VALUE)
		{
			arrStrCabecalho = new String[maxFeat+1];
			
			for(int i = 1 ; i<= maxFeat ; i++)
			{
				if(idToNom.containsKey(i))
				{
					arrStrCabecalho[i] = idToNom.get(i);	
				}else
				{
					arrStrCabecalho[i] = "atr_"+i;
				}
				
			}
		}else
		{
			
			arrStrCabecalho = new String[1];
		}
		arrStrCabecalho[0] = "classe";
		return gerarCabecalhoDataset(arrStrCabecalho);
			
		
	}
	
	public abstract HashMap<Long,String> getFeaturesVector(String linha);
	public abstract String getFeaturesString(String linha);
	public abstract String getClasseReal(String linha);
	public abstract ArrayList<ResultadoItem> parseResult(Fold fold) throws Exception;
	public abstract MetodoAprendizado clone();
	private Integer numClasses = null;
	
	public void gerarResultadoArq(File arquivoResultado,List<String> lstClasses,List<ResultadoItem> rstItem) throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(arquivoResultado, false));
		
		//gera cabecalho, se necessario
		if(lstClasses.size()>0)
		{
			out.write(gerarCabecalhoResultado(lstClasses)+"\n");
		}
		
		//gera as linhas do resultado
		Iterator<ResultadoItem> i = rstItem.iterator();
		while(i.hasNext())
		{
			ResultadoItem ri = i.next();
			
			out.write(gerarLinhaResultado(ri));
			if(i.hasNext())
			{
				out.write("\n");	
			}
		}
		
		
		
		
		out.close();
		
		
	}
	
	public void substituiFeatSet(File arquivoIn,File arquivoOut,List<Map<Long, String>> newFeatSet) throws IOException
	{
		System.out.println("SUBSTITUINDO Feat set: "+arquivoIn.getAbsolutePath()+"  "+newFeatSet.size());
		 String[] linhaDataset = ArquivoUtil.leTexto(arquivoIn).split("\n");
		 StringBuilder strArquivo = new StringBuilder();
		 
		 int i =0;
		 for(String linha : linhaDataset)
		 {
			 //resgata dados
			 Double classe = Double.parseDouble(this.getClasseReal(linha));
			 Integer id = this.getIdPorLinhaArquivo(linha);
			 
			 //cria a linha nova
			 String linhaNova = this.gerarLinhaDataset(classe, id, (HashMap<Long, String>) newFeatSet.get(i));
			 strArquivo.append(linhaNova+"\n");
			 
			 i++;
		 }
		 ArquivoUtil.gravaTexto(strArquivo.toString(), arquivoOut, false);
	}
	 public List<Map<Long, String>> getFeaturesData(File arquivo) throws IOException
	 {
		 String[] linhaDataset = ArquivoUtil.leTexto(arquivo).split("\n");
		 List<Map<Long, String>> lstFeatData = new ArrayList<Map<Long, String>>();
		 
		 for(String linha : linhaDataset)
		 {
			 lstFeatData.add(getFeaturesVector(linha));
		 }
		 linhasLida = linhaDataset;
		 return lstFeatData;
		 
	 }
	 public void loadLinhasLidas(File arquivo) throws IOException
	 {
		 linhasLida = ArquivoUtil.leTexto(arquivo).split("\n");
	 }
	 public String getClasseFromLinha(int linhaIdx)
	 {
		 return this.getClasseReal(linhasLida[linhaIdx]);
	 }
	
	public void setNumClasses(int numClasses)
	{
		this.numClasses = numClasses;
	}
	public Integer getNumClasses() throws Exception
	{
		
		return this.numClasses;
	}
	public boolean isClassificacao()
	{
		return true;
	}
	public ArrayList<Long> getIds(File arquivo) throws IOException
	{
		return getIds(arquivo,"id");
	}
	public ArrayList<Long> getIds(File arquivo,String nomId) throws IOException{
		return getIds(arquivo,nomId,false);
	}
	public ArrayList<Long> getIds(File arquivo,String nomId,boolean generateAutomaticIds) throws IOException
	{
		System.out.println("arquivo a resgatar a linha:"+arquivo.getAbsolutePath());
		
		ArrayList<Long> ids = new ArrayList<Long>();
		ArrayList<Integer> linhas = new ArrayList<Integer>();
		BufferedReader in = new BufferedReader(new FileReader(arquivo));
		String linhaTexto = "";
		int i =0;
		while ((linhaTexto = in.readLine()) != null)
		{
			Integer idVal = getIdPorLinhaArquivo(linhaTexto,nomId);
			if(idVal!= null)
			{
				ids.add((long)idVal);
			}else
			{
				if(generateAutomaticIds) {
					linhas.add(i);					
				}else {
					throw new IdNotFoundException("Id not found in line "+(i+1)+" of file "+arquivo.getAbsolutePath());
				}

				
			}
			i++;
		}
		in.close();
		if(linhas.size() > 0)
		{
			
			if(ids.size()==0)
			{
				for(long id: this.mapIdPorLinha.keySet()) {
					ids.add(id);
				}
				if(ids.size()==0) {
					System.err.println("NAO FOI POSSIVEL ACHAR ID "+nomId+" nem todas as linhas do arquivo "+arquivo.getAbsolutePath());	
				}
			}else
			{
				System.err.println("NAO FOI POSSIVEL ACHAR ID "+nomId+" nas linhas "+linhas+" do arquivo "+arquivo.getAbsolutePath());
			}
		}
		return ids;
	}
	
	public List<Long>[] getIdsFoldsPerGroup(int numFolds,File arquivo,String nomGroupId,String nomId,boolean groupPerLength,long randSeed) throws IOException
	{
		ListaAssociativa<Long,Long> mapIdsPerGroup = new ListaAssociativa<Long,Long>(getIdsPerGroup( arquivo, nomGroupId,nomId));
		//agrupa os ids pelo seu nome...o id que se diz aqui "nomId" pode ser o id que é considerado a unidade, que nao necessariamente
		//eh uam instancia por ex: Uma pergunta é a unidade, podendo ter varias instancias....
		ListaAssociativa<Long,Long> mapIdsPerNomId = new ListaAssociativa<Long,Long>(getIdsPerGroup( arquivo, nomId,"id"));
		
		
		//ordena o grupo (usuario por ex)
		List<Long> lstGroupKeys = null;
		if(groupPerLength)
		{
			lstGroupKeys = mapIdsPerGroup.getKeysOrderedByListLength();
		}else
		{
			lstGroupKeys = Fold.divideIntoFolds(1, mapIdsPerGroup.keySet(), mapIdsPerGroup.keySet().size(),randSeed)[0];
		}
		
		//armazena todas as instancias de um grupo, o grupo deve ficar no mesmo fold
		//folds de forma round robin...
		int numFold =0;
		List<Long>[] arrlstFolds = new List[numFolds];
		
		//inicializa
		for(int i = 0 ; i<arrlstFolds.length ; i++)
		{
			arrlstFolds[i] = new ArrayList<Long>();
		}
		for(int i = lstGroupKeys.size()-1; i>=0 ; i--)
		{
			Long grpKey = lstGroupKeys.get(i);
			List<Long> lstInstanciasGrp = mapIdsPerGroup.getList(grpKey);
			for(Long id : lstInstanciasGrp)
			{
				arrlstFolds[numFold].addAll(mapIdsPerNomId.getList(id));
			}
			numFold ++;
			if(numFold>=numFolds)
			{
				numFold = 0;
			}
		}
		return arrlstFolds;
		
	}
	public Map<Long,List<Long>> getIdsPerGroup(File arquivo,String nomGroupId,String nomId) throws IOException
	{
		Map<Long,List<Long>> mapIdsPerGroup = new HashMap<Long,List<Long>>();
		
		
		BufferedReader in = new BufferedReader(new FileReader(arquivo));
		String linhaTexto = "";
		while ((linhaTexto = in.readLine()) != null)
		{
			long id = (long)getIdPorLinhaArquivo(linhaTexto,nomId);
			long gid = (long)getIdPorLinhaArquivo(linhaTexto,nomGroupId);
			
			if(!mapIdsPerGroup.containsKey(gid))
			{
				mapIdsPerGroup.put(gid, new ArrayList<Long>());
				
			}
			mapIdsPerGroup.get(gid).add(id);
		}
		in.close();
		return mapIdsPerGroup;
		
		
		
	}
	public void filtraArquivoPorIds(Collection<Long> ids,File novoArquivo,List<String> lstLinhasAdicionais,Map<String,String> paramsSubst) throws IOException
	{
		//StringBuilder textoFiltrado = new StringBuilder();
				//cria arquivo de saida
				if(!novoArquivo.getParentFile().exists())
				{
					novoArquivo.getParentFile().mkdirs();
				}
				File tmpNovoArquivo = File.createTempFile("amostraNovoArq",".amostra");
				tmpNovoArquivo.deleteOnExit();
				
				BufferedWriter out = new BufferedWriter(new FileWriter(tmpNovoArquivo, false));
				
				//verifica se contem na lstLinhaAdicionais
				for(String linhaAdd : lstLinhasAdicionais)
				{
					long id = getIdPorLinhaArquivo(linhaAdd);
					if(ids.contains(id))
					{
						for(String paramKey : paramsSubst.keySet())
						{
							if(paramKey.contains(paramKey))
							{
								linhaAdd = linhaAdd.replace(paramKey, paramsSubst.get(paramKey));
							}
						}
						
						out.write(linhaAdd+"\n");
					}
				}
				
				
				//verifica se contem no arquivo fonte original
				BufferedReader in = new BufferedReader(new FileReader(arqFonte));
				
				String strLine;
				while ((strLine = in.readLine()) != null)
				{
					Integer id = getIdPorLinhaArquivo(strLine);

					if(ids.contains((long)id))
					{
						out.write(strLine+"\n");
					}
				}
				in.close();
				out.close();
				//move para o arquivo original
				novoArquivo.delete();
				Sys.executarComando("cp "+tmpNovoArquivo.getAbsolutePath()+" "+novoArquivo.getAbsolutePath(),false);
				if(!tmpNovoArquivo.exists())
				{
					throw new IOException("Não foi possivel criar o arquivo "+tmpNovoArquivo.getAbsolutePath());
				}

				
				System.out.println("Gravado: "+novoArquivo.getAbsolutePath());
	}
	public void filtraArquivoPorIds(Collection<Long> ids,File novoArquivo,List<String> lstLinhasAdicionais) throws IOException
	{
		filtraArquivoPorIds(ids, novoArquivo,lstLinhasAdicionais,new HashMap<String,String>());
		//ArquivoUtil.gravaTexto(textoFiltrado.toString(), novoArquivo, false);
	}
	/**
	 * Filtra id e grava em um novo arquivo
	 * @param ids
	 * @param novoArquivo
	 * @throws IOException
	 */
	public void filtraArquivoPorIds(Collection<Long> ids,File novoArquivo) throws IOException
	{
		filtraArquivoPorIds( ids, novoArquivo,new ArrayList<String>());
		
	}
	/**
	 * Cria arquivo duplicando instancias de determinados id
	 * @param ids
	 * @param novoArquivo
	 * @throws IOException
	 */
	public List<String> criaArquivoDuplicandoInstanciasDeIds(List<Long> ids,File arqAntigo,File novoArquivo) throws IOException
	{
		//resgata o id maximo
		List<Long> lstIdsFonte = this.getIds(arqFonte);
		long maxId = Long.MIN_VALUE;
		for(Long idFonte : lstIdsFonte)
		{
			if(idFonte > maxId)
			{
				maxId = idFonte;
			}
		}
		
		//duplica ids caso necessario
		List<String> newLines = new ArrayList<String>();
		String[] linhaTexto = ArquivoUtil.leTexto(arqAntigo).split("\n");
		StringBuilder textoFiltrado = new StringBuilder();
		for(int i = 0; i<linhaTexto.length ; i++)
		{
			int id = getIdPorLinhaArquivo(linhaTexto[i]);
			//adiciona sempre a linha atual
			textoFiltrado = textoFiltrado.append(linhaTexto[i]+"\n");
			
			//adiciona ela novamente, com id diferente, caso ela pertenca a ids 
			if(ids.contains((long)id))
			{
				
				String linhaNova = this.gerarLinhaDataset(Double.parseDouble(this.getClasseReal(linhaTexto[i])), (int)++maxId, this.getFeaturesVector(linhaTexto[i]));
				textoFiltrado = textoFiltrado.append(linhaNova+"\n");
				newLines.add(linhaNova);
			}
		}
		if(!novoArquivo.getParentFile().exists())
		{
			novoArquivo.getParentFile().mkdirs();
		}
		System.out.println("Gravado: "+novoArquivo.getAbsolutePath());
		ArquivoUtil.gravaTexto(textoFiltrado.toString(), novoArquivo, false);
		
		return newLines;
	}
	/**
	 * Substitiu classes de ids definidos por lstSubstituir
	 * @param ids
	 * @param novoArquivo
	 * @throws IOException
	 */
	public void substituiClasseArquivo(List<ResultadoItemViews> lstSubstituir,File arqAntigo,File novoArquivo) throws IOException
	{
		if(lstSubstituir.size() == 0)
		{
			return;
		}
		//System.out.println("Lendo arquivo: "+arqAntigo);
		String[] linhaTexto = ArquivoUtil.leTexto(arqAntigo).split("\n");
		StringBuilder textoFiltrado = new StringBuilder();
		StringBuilder substituicoes = new StringBuilder();
		for(int i = 0; i<linhaTexto.length ; i++)
		{
			int id = getIdPorLinhaArquivo(linhaTexto[i]);
			ResultadoItemViews rvEncontrado = null;
			for(ResultadoItemViews rv : lstSubstituir)
			{
				if(rv.getId() == id)
				{
					rvEncontrado = rv; 
					break;
				}
			}
			if(rvEncontrado != null)
			{
				substituicoes = substituicoes.append("LINHA #"+i+" id: "+rvEncontrado.getId()+"\tClasse mudou de "+rvEncontrado.getClasseReal()+" para "+rvEncontrado.getMeanClasseView()+"\t"+rvEncontrado.toString()+" \n");
				//System.out.println("LINHA #"+i+" id: "+rvEncontrado.getId()+"\tClasse mudou pra "+rvEncontrado.getMeanClasseView());
				//substitui classe
				String featuresVector = linhaTexto[i].substring(linhaTexto[i].indexOf(" "));
				textoFiltrado.append(rvEncontrado.getMeanClasseView()+" "+featuresVector+"\n");
			}
			else
			{
				textoFiltrado.append(linhaTexto[i]+"\n");
			}
		}
		ArquivoUtil.gravaTexto(textoFiltrado.toString(), novoArquivo, false); 
		ArquivoUtil.gravaTexto(substituicoes.toString(), new File(novoArquivo.getParentFile(),novoArquivo.getName()+"_"+this.nomExperimento+"_ruidosEliminados.log"), false);
	}
	public Fold criaFoldComIdsFiltrado(int numFold,List<Long> idsTreino,List<Long> idsTeste) throws IOException
	{
		return criaFoldComIdsFiltrado( numFold, idsTreino, "foldsFiltro", idsTeste);
	}
	public List<Long> getSourceIds() throws IOException
	{
		String[] linhaTexto = ArquivoUtil.leTexto(arqFonte).split("\n");
		List<Long> lstIds = new ArrayList<Long>();
		for(int i = 0; i<linhaTexto.length ; i++)
		{
			long id = getIdPorLinhaArquivo(linhaTexto[i]);
			lstIds.add(id);
		}
		return lstIds;
	}
	public List<IdClass> getSourceIdClass() throws IOException
	{
		String[] linhaTexto = ArquivoUtil.leTexto(arqFonte).split("\n");
		List<IdClass> lstIds = new ArrayList<IdClass>();
		for(int i = 0; i<linhaTexto.length ; i++)
		{
			long id = getIdPorLinhaArquivo(linhaTexto[i]);
			String classe = getClasseReal(linhaTexto[i]);
			lstIds.add(new IdClass(id, classe));
		}
		return lstIds;
	
	}
	public Fold[] dividePerFoldsByIds(List<Long> ids,String prefixName,String subDir,int numFolds,Integer numItens) throws IOException
	{
		return dividePerFoldsByIds( ids,prefixName, subDir, numFolds, numItens, new ArrayList<String>());
	}
	public Fold[] dividePerFoldsByIds(List<Long> ids,String prefixName,String subDir,int numFolds,Integer numItens,List<String> idsAdicional) throws IOException
	{
		Fold[] folds = new Fold[numFolds];
		List<Long> lstIdsFolds = new ArrayList<Long>();
		lstIdsFolds.addAll(ids);
		
		
		for(String linhaAdd : idsAdicional)
		{
			lstIdsFolds.add((long)this.getIdPorLinhaArquivo(linhaAdd));
		}
		//divide into folds
		List<Long>[] idsPerFold = null;
		
		if(numItens == null)
		{
			idsPerFold = Fold.divideIntoFolds(numFolds, lstIdsFolds);
		}else
		{
			idsPerFold = Fold.divideIntoFolds(numFolds, lstIdsFolds,numItens);
		}
		
		
		
		gravaIdsFold(lstIdsFolds, subDir, folds, idsPerFold,idsAdicional);
		return folds;
	}
	public Fold[] dividePerFoldsByIdsClasse(List<IdClass> idsClass,String prefixName,String subDir,int numFolds,Integer numItens) throws IOException, SQLException
	{
		Fold[] folds = new Fold[numFolds];
		List<Long> ids = new ArrayList<Long>();
		
		//divide into folds
		@SuppressWarnings("unchecked")
		List<Long>[] idsPerFold = new ArrayList[numFolds];
		
		Map<Integer,List<Long>> classPerPage = new HashMap<Integer,List<Long>>();
		
		
		//mapeia classes por id das paginas
		Map<Long,Integer> objMapPageClass = new HashMap<Long,Integer>();
		
		try {
				iniciaCountClasses();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		PreparedStatement objStmtSelectClasses =  stmtSelectClasses.get(Thread.currentThread().getId());
		ResultSet rst =objStmtSelectClasses.executeQuery();
		while(rst.next())
		{
			
			objMapPageClass.put(rst.getLong(1),rst.getInt(2));
			
		}
		rst.close();
			

		
		//adiciona as classes do fold em um hashmap
		for(IdClass id : idsClass)
		{
			List<Long> lstClass = null;
			if(!objMapPageClass.containsKey(id.getId()))
			{
				System.err.println("NAO FOI ENCONTRADO O ID: "+id.getId());
				System.exit(0);
			}
			int intClasse = objMapPageClass.get(id.getId());
			
			if(!classPerPage.containsKey(intClasse))
			{
				classPerPage.put(intClasse, new ArrayList<Long>());
			}
			lstClass = classPerPage.get(intClasse);
			lstClass.add(id.getId());
			ids.add(id.getId());
			
		}
		
		//para cada classe, extrai uma quantidade por fold
		int numItemsPorClasse = (int) Math.ceil(numItens/(double) classPerPage.size());
		
		//System.out.println("Itens: "+numItens+" Num por classe: "+numItemsPorClasse);
		for(Integer classe : classPerPage.keySet())
		{
			List<Long> lstIdsClass = classPerPage.get(classe);
			//Collections.sort(lstIdsClass);
			System.out.println("NUM DE ITENS NA CLASSE "+classe+":"+lstIdsClass.size()+" Começando com: "+lstIdsClass.get(0)+","+lstIdsClass.get(1)+","+lstIdsClass.get(2));
			
			List<Long>[] idsPerFoldClass = Fold.divideIntoFolds(numFolds, lstIdsClass,numItemsPorClasse);
			
			
			for(int i = 0; i<idsPerFoldClass.length ; i++)
			{
				if(idsPerFold[i] == null)
				{
					idsPerFold[i] = new ArrayList<Long>();
				}
				
				idsPerFold[i].addAll(idsPerFoldClass[i]);
				
				if(classe == 0 && i == 0)
				{
					System.out.println("FOLD # "+i+"Classe:"+classe+" id inicial: "+idsPerFoldClass[i].get(0)+","+idsPerFoldClass[i].get(1)+","+idsPerFoldClass[i].get(2));
				}
			}
		}
		
		
		gravaIdsFold(ids, subDir, folds, idsPerFold);
		return folds;
	}
	
	
	private void gravaIdsFold(List<Long> ids, String subDir, Fold[] folds,	List<Long>[] idsPerFold) throws IOException {
		gravaIdsFold( ids, subDir,  folds,	 idsPerFold, new ArrayList<String>());
	}
	private void gravaIdsFold(List<Long> ids, String subDir, Fold[] folds,	List<Long>[] idsPerFold,List<String> linhasAdicionais) throws IOException {
		for(int f = 0; f< idsPerFold.length ; f++)
		{
			List<Long> lstIdsTeste = idsPerFold[f];
			
			//create fold com ids de treio e teste
			List<Long> lstIdsTreino = new ArrayList<Long>(ids);
			lstIdsTreino.removeAll(lstIdsTeste);
			System.out.println("****************FOLD #"+f+"\tTeste:"+lstIdsTeste.size()+" Treino"+lstIdsTreino.size());
			
			folds[f] = criaFoldComIdsFiltrado(f,lstIdsTreino,subDir,lstIdsTeste,new ArrayList<ResultadoItemViews>(),linhasAdicionais);
		}
	}
	
	public Fold[] dividePerFoldsByIds(List<Long> lstIds, String prefixName,
			String subDir, int numFoldsView, List<String> linhasIdsAdicional) throws IOException {
		// TODO Auto-generated method stub
		return dividePerFoldsByIds( lstIds, prefixName, subDir, numFoldsView,null,linhasIdsAdicional);
	}
	
	public Fold[] dividePerFoldsByIds(List<Long> ids,String prefixName,String subDir,int numFolds) throws IOException
	{
		return dividePerFoldsByIds( ids, prefixName, subDir, numFolds,null,new ArrayList<String>());
	}
	public Fold criaFoldComIdsFiltrado(int numFold,List<Long> idsTreino,String foldSubdir,List<Long> idsTeste) throws IOException
	{
		return criaFoldComIdsFiltrado(numFold,idsTreino,foldSubdir,idsTeste,new ArrayList<ResultadoItemViews>());
	}
	public Fold criaFoldComIdsFiltrado(int numFold,List<Long> idsTreino,String foldSubdir,List<Long> idsTeste,List<ResultadoItemViews> lstMudarClasse) throws IOException
	{
		return criaFoldComIdsFiltrado( numFold, idsTreino, foldSubdir, idsTeste, lstMudarClasse, new ArrayList<String>());
	}
	public File criaArquivoIdsFiltrado(List<Long> lstIds,File arquivoDado,File arquivoIds) throws IOException
	{
		this.filtraArquivoPorIds(lstIds, arquivoDado);
		criaArquivoIds(arquivoDado, arquivoIds);
		this.filtraIDsArquivo(arquivoDado, arquivoDado);
		
		return arquivoDado;
	}
	public Fold criaFoldComIdsFiltrado(int numFold,List<Long> idsTreino,String foldSubdir,List<Long> idsTeste,List<ResultadoItemViews> lstMudarClasse,List<String> linhasTreinoAdicional) throws IOException
	{
		return criaFoldComIdsFiltrado(numFold,idsTreino,foldSubdir,new ArrayList<Long>(), idsTeste, lstMudarClasse, linhasTreinoAdicional);
	}

	public Fold[] criaFolds(List<Long>[] idsPerFolds,String foldSubdir,List<ResultadoItemViews> lstMudarClasse,List<String> linhasTreinoAdicional,boolean gerarValidacao) throws IOException
	{
		Fold[] arrFolds =new Fold[idsPerFolds.length];
		for(int f = 0 ; f<idsPerFolds.length ; f++)
		{

			Tripla<Integer,Integer,FoldIds> tplFold = FoldIds.getFoldIds(f,idsPerFolds,gerarValidacao);
			FoldIds fIds = tplFold.getZ();
			int idxValidacao = tplFold.getX();
			int idxTeste = tplFold.getY();
			
			//armazena o fold com os ids
			arrFolds[f] = criaFoldComIdsFiltrado(f,fIds.getLstTreino(),foldSubdir,fIds.getLstValidacao(), fIds.getLstTeste(),lstMudarClasse,linhasTreinoAdicional);
			arrFolds[f].setIdsFold(idsPerFolds, idxValidacao, idxTeste);
		}
		return arrFolds;
	}
	/**
	 * Cria um fold atraves de filtros do arquivo da fonte de todos os folds
	 * @param numFold
	 * @param idsTreino
	 * @param idsTeste
	 * @return
	 * @throws IOException 
	 */
	public Fold criaFoldComIdsFiltrado(int numFold,List<Long> idsTreino,String foldSubdir,List<Long> idsValidacao,List<Long> idsTeste,List<ResultadoItemViews> lstMudarClasse,List<String> linhasTreinoAdicional) throws IOException
	{
		//System.out.println("Oioi");
		File diretorio = new File(arqFonte.getParentFile().getAbsolutePath()+"/"+(foldSubdir != null?foldSubdir:"") );
		if(!diretorio.exists())
		{
			diretorio.mkdir();
		}
		String caminhoArquivoBase = diretorio.getAbsolutePath()+"/"+arqFonte.getName();
		File foldTreino = new File(caminhoArquivoBase+".treino"+numFold);
		File foldTeste = new File(caminhoArquivoBase+".teste"+numFold);
		File foldValidacao = new File(caminhoArquivoBase+".validacao"+numFold);
		File idsPredict = new File(caminhoArquivoBase+".foldIds"+numFold);
		File idsValidacaoFl = new File(caminhoArquivoBase+".validacaoIds"+numFold);
		File idsTreinoFile = new File(caminhoArquivoBase+".foldTreinoIds"+numFold);
		
		//cria arquivo treino e teste e validacao
		HashMap<String,String> mapParamSubst = new HashMap<String,String>();
		mapParamSubst.put("{{arrTeste}}", idsTeste.toString().replaceAll("\\[\\]", "").trim());
		this.filtraArquivoPorIds(idsTreino, foldTreino ,linhasTreinoAdicional,mapParamSubst);
		this.filtraArquivoPorIds(idsTeste, foldTeste,new ArrayList<String>(),mapParamSubst);
		if(idsValidacao.size()>0)
		{
			this.filtraArquivoPorIds(idsValidacao, foldValidacao);
		}
		
		//substitui classes do arquivo de treino se necessario
		substituiClasseArquivo(lstMudarClasse,foldTreino,foldTreino);
		if(idsValidacao.size()>0)
		{
			substituiClasseArquivo(lstMudarClasse,foldValidacao,foldValidacao);
		}
		
		//cria arquivo com ids do teste
		criaArquivoIds(foldTeste, idsPredict);
		criaArquivoIds(foldTreino, idsTreinoFile);
		if(idsValidacao.size()>0)
		{
			criaArquivoIds(foldValidacao, idsValidacaoFl);
		}
		
		//limpa ids do treino e do teste
		this.filtraIDsArquivo(foldTreino, foldTreino);
		this.filtraIDsArquivo(foldTeste, foldTeste);
		if(idsValidacao.size()>0)
		{
			this.filtraIDsArquivo(foldValidacao,foldValidacao);
		}
		
		//System.out.println("tchau");
		Fold f= new Fold(numFold,
						this.getArquivoOrigem(),
						foldTreino,
						foldTeste,
						idsPredict,
						idsTreinoFile);
		if(idsValidacao.size()>0)		
		{
			f.setValidationFiles(foldValidacao, idsValidacaoFl);
			
		}
		return f;
	}
	public void criaArquivoIds(File fileTeste, File fileIds)
			throws IOException
	{
		StringBuilder strIdsTeste = new StringBuilder();
		String[] arqTeste = ArquivoUtil.leTexto(fileTeste).split("\n");
		for(int i =0 ; i<arqTeste.length ; i++)
		{
			//System.out.println("linha "+i+": "+arqTeste[i].length());
			strIdsTeste.append(getIdPorLinhaArquivo(arqTeste[i])+"\n");
		}
		ArquivoUtil.gravaTexto(strIdsTeste.toString(), fileIds, false);
	}
	public String getNomBase(File arquivo)
	{
		return arquivo.getName().replaceAll("\\..*", "");
	}
	public Fold[] criaTenFolds(File arquivo) throws Exception {
		// TODO Auto-generated method stub
		return criaTenFolds(arquivo,getNomBase(arquivo));
	}
	public Fold[] criaTenFolds(File arquivo,String prefixoNomSaida) throws Exception
	{
		long threadId = Thread.currentThread().getId();
		
		this.arqFonte = arquivo;
		String pathDiretorio = arquivo.getParentFile().getAbsolutePath();
		File diretorioFolds = new File(pathDiretorio+"/folds");
		if(!diretorioFolds.exists())
		{
			diretorioFolds.mkdir();
		}
		Fold[] folds = new Fold[10];
		
		if(!this.dividirFoldsBanco)
		{
			if(new File(DIRETORIO_TENFOLD+"/tenfold_id.pl").exists())
			{
				Sys.executarComando("cp "+DIRETORIO_TENFOLD+"/tenfold_id.pl "+pathDiretorio+"/.",false,pathDiretorio);
				String resp = Sys.executarComando("perl "+pathDiretorio+"/tenfold_id.pl "+arquivo.getName(),false,pathDiretorio);
			}
			//System.out.println("SAIU!\n"+resp);
			
			for(int i =0 ; i < folds.length ; i++)
			{
				folds[i] = new Fold(i,
									arquivo,
									new File(diretorioFolds.getAbsolutePath()+"/"+prefixoNomSaida+".treino"+i),
									new File(diretorioFolds.getAbsolutePath()+"/"+prefixoNomSaida+".teste"+i),
									new File(diretorioFolds.getAbsolutePath()+"/"+prefixoNomSaida+".pageId"+i)
									);

				
			}
		}else
		{
			
			//testa para ver se select ja foi preparado
			prepareFoldSelect();
			
			
			//mapeia folds e ids via banco
			ResultSet rst = stmtSelectFoldInicial.get(threadId).executeQuery();
			Map<Integer,List<Long>> mapFoldsPage = new HashMap<Integer,List<Long>>();
			Set<Long> idsTodasPages = new HashSet<Long>();
			System.out.println("Organizando os pageIds...");
			while(rst.next())
			{
				int foldId = rst.getInt(1);
				long pageId = rst.getLong(2);
				
				
				if(!mapFoldsPage.containsKey(foldId))
				{
					mapFoldsPage.put(foldId,new ArrayList<Long>());
				}
				List<Long> pageIds = mapFoldsPage.get(foldId);
				
				idsTodasPages.add(pageId);
				pageIds.add(pageId);
				
			}
			
			//cria folds com ids apropriados
			for(int foldId : mapFoldsPage.keySet())
			{
				
				List<Long> idsTeste = mapFoldsPage.get(foldId);
				List<Long> idsTreino = new ArrayList<Long>(idsTodasPages);
				idsTreino.removeAll(idsTeste);
				
				System.out.println("Criando fold "+foldId+"\tNumTeste: "+idsTeste.size()+"\tNumTreino: "+idsTreino.size()+" Fonte: "+this.arqFonte.getAbsolutePath());
				folds[foldId] = criaFoldComIdsFiltrado(foldId, idsTreino, idsTeste);	
			}
			
			
		}
		return folds;
	}
	private synchronized void prepareFoldSelect() throws ClassNotFoundException,
			SQLException
	{
		String filtroFold = colecao.getFiltroFold();
		if(filtroFold.length() > 0)
		{
			filtroFold = " where "+filtroFold;
		}
		long threadId = Thread.currentThread().getId();
			Connection conn = GerenteBD.getGerenteBD().obtemConexao(""+threadId);
			String sql = "select "+
					"	fld.fold_id,"+
					"	fld.page_id	page_id "+
					"from "+
					"	"+colecao.getEsquemaAmostra()+".fold fld "+filtroFold;
			stmtSelectFoldInicial.put(threadId,conn.prepareStatement(sql
																		
																	));
			System.out.println(sql);
			
	}
	private synchronized boolean iniciaCountClasses()throws SQLException,ClassNotFoundException
	{
		long threadId = Thread.currentThread().getId();
		Connection conn = GerenteBD.getGerenteBD().obtemConexao(""+threadId);
		
		
		if(this.getColecao() == null)
		{
			return false;
		}
		if(this.getColecao() == Colecao.STARWARS_VOTE
				|| this.getColecao() == Colecao.MUPPETS)
		{
			stmtSelectClasses.put(threadId,conn.prepareStatement("select page_id,round(page_class) from "+this.getColecao().getEsquemaAmostra()+".page"));
			
		}else
		{
			stmtSelectClasses.put(threadId,conn.prepareStatement("select page_id,round(num_page_class) from "+this.getColecao().getEsquemaAmostra()+".page"));
			
		}
		return true;
	}
	public synchronized void inicializaBD()throws SQLException,ClassNotFoundException
	{
		//System.out.println("ESQUEMA_RESULT: "+esquemaResult+"\tESQUEMA_AMOSTRA: "+esquemaAmostra);
		long threadId = Thread.currentThread().getId();
		Connection conn = GerenteBD.getGerenteBD().obtemConexao(""+threadId);
		
		if(stmtSelectResult.get(threadId) == null)
		{
			/*
			stmtSelectResult.put(threadId,conn.prepareStatement("select " +
																	"rrg.page_id," +
																	"fld.fold_num ," +
																	"fld.id fold_id,"+
																	"rrg.classeReal,"+
																	"rrg.result," +
																	"rrg.probResult," +
																	"fld.end_treino," +
																	"fld.end_teste," +
																	"fld.end_ids,"+
																	"fld.end_modelo_treino," +
																	"fld.end_resultado," +
																	"fld.end_origem "+
																"from " +
																	"wiki_results.resultado_regressao rrg," +
																	" wiki_results.fold fld " +
																" where " +
																"	nomExperimento = ? " +
																"	and rrg.fold_id = fld.id" +
																" order by fold_num desc "
																));*/

			
			stmtSelectResult.put(threadId,conn.prepareStatement("select " +
																	"rrg.page_id," +
																	"fld.fold_num ," +
																	"fld.id fold_id,"+
																	"rrg.classeReal,"+
																	"rrg.result," +
																	"rrg.probResult," +
																	"fld.end_treino," +
																	"fld.end_teste," +
																	"fld.end_ids,"+
																	"fld.end_modelo_treino," +
																	"fld.end_resultado," +
																	"fld.end_origem," +
																	"fld.end_validacao," +
																	"fld.end_validacao_ids, " +
																	"rrg.qid " +
																	
																"from " +
																	"wiki_results.resultado_regressao rrg," +
																	" wiki_results.fold fld " +
																" where " +
																"	nomExperimento = ? " +
																"	and rrg.fold_id = fld.id" +
																" order by fold_num desc "
																));
			/*System.out.println("select " +
					"rrg.page_id," +
					"fld.fold_num ," +
					"fld.id fold_id,"+
					"rrg.classeReal,"+
					"rrg.result," +
					"rrg.probResult," +
					"fld.end_treino," +
					"fld.end_teste," +
					"fld.end_ids,"+
					"fld.end_modelo_treino," +
					"fld.end_resultado," +
					"fld.end_origem "+
				"from " +
					"wiki_results.resultado_regressao rrg," +
					" wiki_results.fold fld " +
				" where " +
				"	nomExperimento = ? " +
				"	and rrg.fold_id = fld.id" +
				" order by fold_num desc ");*/
			stmtSelectProbResult.put(threadId,conn.prepareStatement("select " +
															"page_id,"+
															"fold_id," +
															"nomExperimento,"+
															"class_val," +
															"result "+
													"from " +
														" wiki_results.prob_result pr " +
													" where " +
													"	nomExperimento = ? " +
													"order by class_val desc" 
													)
													);
			

		}
	}
	public Colecao getColecao()
	{
		return this.colecao;
	}
	public void setColecao(Colecao col)
	{
		this.colecao = col;
	}
	public String getNomExperimento()
	{
		return this.nomExperimento;
	}
	public void setNomExperimento(String nomExperimento)
	{
		this.nomExperimento = nomExperimento;
	}
	public Fold[] getResultsPorFoldBanco() throws Exception
	{
		/*
		if(1==1)
		{
			return null;
		}
		*/
		
		
		
		/* resgata o probResult de cada item */
		
		PreparedStatement stmtProbResult =  stmtSelectProbResult.get(Thread.currentThread().getId());
		
		if(stmtProbResult != null)
		{
			HashMap<String,Map<Integer,Float>> probResultset = new HashMap<String,Map<Integer, Float>>();
			
			
			stmtProbResult.setString(1, this.nomExperimento);
			ResultSet rstProbResult = stmtProbResult.executeQuery();
			while(rstProbResult.next())
			{
				int pageId = rstProbResult.getInt(1);
				int foldId = rstProbResult.getInt(2);
				int classVal = rstProbResult.getInt(4);
				float probResult = rstProbResult.getFloat(5);
				
				Map<Integer, Float> probResults = new HashMap<Integer, Float>();
				if(probResultset.get(foldId+"_"+pageId)==null)
				{
					probResultset.put(foldId+"_"+pageId, probResults);
				}else
				{
					probResults = probResultset.get(foldId+"_"+pageId);
				}
				probResults.put(classVal, probResult);
				
			}
			rstProbResult.close();
			
		
			/* Busca resultados*/ 
			PreparedStatement stmtResult =  stmtSelectResult.get(Thread.currentThread().getId());
			stmtResult.setString(1, this.nomExperimento);
			System.out.println("Procura no banco : "+this.nomExperimento);
			ResultSet stmtResultSet = stmtResult.executeQuery();
		
			
			
			
			Fold[] fs = null;
			while(stmtResultSet.next())
			{
				int pageId = stmtResultSet.getInt(1);
				int foldNum = stmtResultSet.getInt(2);
				int foldId = stmtResultSet.getInt(3);
				
				float classeReal = stmtResultSet.getFloat(4);
				float result = stmtResultSet.getFloat(5);
				float probResult = stmtResultSet.getFloat(6);
				String endTreino = stmtResultSet.getString(7);
				String endTeste = stmtResultSet.getString(8);
				String endIds = stmtResultSet.getString(9);
				String endModeloTreino = stmtResultSet.getString(10);
				String endResultado = stmtResultSet.getString(11);
				String endOrigem = stmtResultSet.getString(12);
				String endValidacao = stmtResultSet.getString(13);
				String endValidacaoIds = stmtResultSet.getString(14);
				
				Long qId = stmtResultSet.getLong(15);
				//povoa um resultado de fold
				ResultadoItem rItem = new ResultadoItem(pageId,classeReal,result,probResult);
				if(qId != null)
				{
					rItem.setQID(qId);
				}
				
				//adiciona arrays com a probabilidade de cada result, se encontrado
				if(probResultset.get(foldId+"_"+pageId)!=null)
				{
					Map<Integer, Float> probResults = probResultset.get(foldId+"_"+pageId);
					rItem.setProbPorClasse(probResults);
				}
				
				
				//inicaliza fold atual se necessario
				if(fs == null)
				{
					fs = new Fold[foldNum+1];
				}
				if(fs[foldNum] == null)
				{
					fs[foldNum] = new Fold(foldNum,new File(endOrigem),new File(endTreino),new File(endTeste),new File(endIds),new File(endResultado),endModeloTreino != null?new File(endModeloTreino):null);
					fs[foldNum].setId(foldId);
					if(endValidacao != null && endValidacaoIds != null)
					{
						fs[foldNum].setValidationFiles(new File(endValidacao), new File(endValidacaoIds));
					}
					fs[foldNum].buscaParams();
				}
				
				
				fs[foldNum].adicionaResultado(rItem);
				
			}
			stmtResultSet.close();
			return fs;
		}
		
		return null;
		
	}

	public static ArrayList<Resultado> testarTodosDiretorio(File diretorio, HashMap<String,MetodoAprendizado> metodos) throws Exception
	{
		ArrayList<Resultado> results = new ArrayList<Resultado>();
		if(diretorio.isDirectory())
		{
			File[] lstArquivos = diretorio.listFiles();
			for(int i = 0 ; i<lstArquivos.length ; i++)
			{
				if(lstArquivos[i].getName().endsWith(".amostra"))
				{
					String nomAmostra = lstArquivos[i].getName().replaceAll("\\.amostra", "");
					
					MetodoAprendizado mt = metodos.get(nomAmostra);

					if(mt != null)
					{
						System.out.println("AMOSTRA - "+nomAmostra);
						Fold[] folds = mt.getResultsPorFoldBanco();
						if(folds == null)
						{
							folds = mt.testar(lstArquivos[i]);
						}
						
						Resultado result = new Resultado(mt.getNomExperimento(),folds);
						results.add(result);
					}else
					{
						throw new Exception("Nao foi possivel achar o metodo para a amostra "+nomAmostra);
					}
				}
			}
		}else
		{
			System.out.println("NAO É UM DIRETORIO");
		}
		return results; 
	}

	public abstract File filtraIDsArquivo(File arquivoIn, File arquivoOut) throws IOException;
	public String filtraIdLinha(String strLinha){
		return StringUtil.removeDoubleSpace(strLinha.replaceAll("( |\t)[0-9]+( |\t)", " "));
	}
	
	public Map<String,String> getParams()
	{
		return new HashMap<String,String>();
	}
	public HashMap<Long, String> getFeaturesVectorPorId(String[] arrLinhas,Long id)
	{
		return this.getFeaturesVector(this.getLinhaPorId(arrLinhas, id));
	}
	public String getLinhaPorId(String[] arrLinhas,long id)
	{
		for(String linha : arrLinhas)
		{
			if(this.getIdPorLinhaArquivo(linha) == (int)id)
			{
				return linha;
			}
		}
		return null;
	}
	/**
	 * Mapeia cada feature de cada isntancia em um hashmap
	 * @param arquivo
	 * @return
	 * @throws IOException
	 */
	public Map<Long,Map<Long,String>> mapeiaFeatureInstancias(File arquivo) throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(arquivo));
		String linha;
		StringBuilder texto = new StringBuilder();
		Map<Long,Map<Long,String>> mapFeatPerInstance = new HashMap<Long,Map<Long,String>>(10000,0.75F);
		while ((linha = in.readLine()) != null)
		{
			long id = (long) this.getIdPorLinhaArquivo(linha);
			HashMap<Long,String>  featVector = this.getFeaturesVector(linha);
			
			mapFeatPerInstance.put(id, featVector);
		}
		in.close();
		return mapFeatPerInstance;
		
	}
	/**
	 * Mapeia cada feature de cada isntancia em um hashmap usando o map 
	 * @param arquivo
	 * @return
	 * @throws Exception 
	 */
	public Map<Long,Map<Long,String>> mapeiaFeatureInstancias() throws Exception
	{
		Map<Long,Map<Long,String>> mapFeatPerInstance = new HashMap<Long,Map<Long,String>>(10000,0.75F);
		for(long id : mapIdPorLinha.keySet())
		{
			String linha = this.mapIdPorLinha.get(id);
			if(linha == null)
			{
				throw new Exception("Nao achou a linha do id: "+id);
			}
			
			HashMap<Long,String>  featVector = this.getFeaturesVector(linha);
			
			mapFeatPerInstance.put(id, featVector);
		}
		return mapFeatPerInstance;
		
	}
	public Map<Long,Map<Long,String>> mapeiaIdsPorLinhaFold(Fold[] foldPorView) throws IOException
	{
		 
		Map<Long,Map<Long,String>>  mapIdsPorLinha = new HashMap<Long,Map<Long,String>>();
		Map<String,Long> arqFoldToFeatIdx = new HashMap<String,Long>();
		long lastIdx = 0;
		int numArq = 0;
		//para cada arquivo, adiciona no map de cada artigo todas suas features
		for(Fold f : foldPorView)
		{
			
			Map<Long,Map<Long,String>> mapArquivo = mapeiaFeatureInstancias(f.getTreino());
			mapArquivo.putAll(mapeiaFeatureInstancias(f.getTeste()));
			if(f.getValidation() != null)
			{
				mapArquivo.putAll(mapeiaFeatureInstancias(f.getValidation()));
			}
			
			//armazena tudo no vetor
			for(Long instanceId : mapArquivo.keySet())
			{
				//resgata feature desta view
				Map<Long,String> mapFeat = mapArquivo.get(instanceId);
				Map<Long,String> mapFeatGeral = mapIdsPorLinha.get(instanceId);
				if(mapFeatGeral == null)
				{
					mapFeatGeral = new HashMap<Long,String>();
					mapIdsPorLinha.put(instanceId,mapFeatGeral);
				}
				
				//para cada feature, armazena a feature no map geral analisando o numero da view neste map
				for(Long featViewId : mapFeat.keySet())
				{
					long idxGeral = 0;
					String idxGeralFinderKey = numArq+"_"+featViewId;
					//procura o idx geral no map geral
					if(arqFoldToFeatIdx.containsKey(idxGeralFinderKey))
					{
						idxGeral = arqFoldToFeatIdx.get(idxGeralFinderKey);
					}else
					{
						//caso nao acha, cria um idx geral para ela
						lastIdx++;
						idxGeral = lastIdx;
						arqFoldToFeatIdx.put(idxGeralFinderKey, idxGeral);
						
					}
					//coloca a feature em questao no map geral
					mapFeatGeral.put(idxGeral, mapFeat.get(featViewId));
				}
			}
			
			numArq++;
		}
		return mapIdsPorLinha;
	}
	
	public int mapeiaIdPorLinha(File arquivo,boolean useLineNumAsId,int lineNumOffset) throws IOException
	{
		System.out.println("--------------------------");
		System.out.println(">>>>>>>>>>>>>>>>>>>>>>>> MAPEANDO.... <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		//System.exit(0);
		
		String[] arrLinha = ArquivoUtil.leTexto(arquivo).split("\n");
		String linha = "";
		for(int lineNum=0 ; lineNum<arrLinha.length ; lineNum++)
		{
			linha = arrLinha[lineNum];
			long id = 0;
			if(!useLineNumAsId) {
				id = (long) this.getIdPorLinhaArquivo(linha);
			}else {
				id = lineNum+lineNumOffset;
			}
			mapIdPorLinha.put(id, linha);
		}
		return arrLinha.length+lineNumOffset-1;
	}
	public int mapeiaIdPorLinha(File arquivo) throws IOException
	{
		return mapeiaIdPorLinha(arquivo,false,0);
	}
	public Map<Long,String> getMapIdPorLinha()
	{
		return mapIdPorLinha;
	}
	public String[] getFeatVectorLinhaMapeada(Long id) throws Exception
	{
		String linha = this.mapIdPorLinha.get(id);
		if(linha == null)
		{
			throw new Exception("Nao achou a linha do id: "+id);
		}
		
		HashMap<Long,String> map =  this.getFeaturesVector(linha);
		String[] arrFeat = mapToStringArrayFeatures(map);
		return arrFeat;
	}
	public String[] mapToStringArrayFeatures(HashMap<Long, String> map) {
		
		//resgata o maximo 
		long maxIndex = 0; 
		for(long key : map.keySet())
		{
			if(maxIndex < key)
			{
				maxIndex = key;
			}
			
		}
		String[] arrFeat = new String[(int)maxIndex];
		for(int i = 0 ; i<arrFeat.length ; i++)
		{
			if(map.containsKey((long)i))
			{
				arrFeat[i]= map.get((long)i);
			}else
			{
				arrFeat[i] = "";
			}
		}
		return arrFeat;
	}
	public String getClasseRealLinhaMapeada(Long id) throws Exception
	{
		String linha = this.mapIdPorLinha.get(id);
		if(linha == null)
		{
			throw new Exception("Nao achou a linha do id: "+id);
		}
		return this.getClasseReal(linha);
	}
	public Integer getQIDLinhaMapeada(Long id) throws Exception
	{
		
		if(this instanceof GenericoLetorLike) {
			String linha = this.mapIdPorLinha.get(id);
			return ((GenericoLetorLike)this).getQIDFeatureString(linha);
		}
		return null;
	}
	public String getCommentarioLinha(String linha)
	{
		return linha.split("\\#")[1];
		
	}
	/**
	 * Resgata o fileIn, extrai todos os dados dele atraves de seu metodo aprendizado (metApIn)
	 * e grava em fileOut substituindo as classes de cada id (classPerId)
	 * @param fileIn
	 * @param fileOut
	 * @param metApIn
	 * @param metApOut
	 * @param classPerId
	 * @param append
	 * @throws IOException 
	 */
	public static void convertArquivo(File fileIn,File fileOut,MetodoAprendizado metApIn,MetodoAprendizado metApOut,Map<Integer,Double> classPerId,Set<Integer> idsToUse,Set<Long> featsToUse,Map<Integer,String> mapIdToNomFeats,boolean append) throws IOException
	{
		BufferedWriter out = new BufferedWriter(new FileWriter(fileOut, append),100);
		
		
		BufferedReader in = new BufferedReader(new FileReader(fileIn));
		String strDataLine;
		
		
		//gera cabecalho (se existir)
		String cabecalho = metApOut.gerarCabecalhoDataset(mapIdToNomFeats);
		if(cabecalho.length()>0){
			out.write(cabecalho+"\n");
		}
		
		while ((strDataLine = in.readLine()) != null)
		{
			HashMap<Long,String> mapFeatVector = metApIn.getFeaturesVector(strDataLine);
			
			//filtra features pelos seus ids
			if(featsToUse.size() > 0)
			{
				HashMap<Long,String> mapFeatVectorFiltered = new HashMap<Long,String>();
				for(long featId : featsToUse)
				{
					mapFeatVectorFiltered.put(featId, mapFeatVector.get(featId));
				}
				mapFeatVector = mapFeatVectorFiltered;
			}
			
			//grava se necessario
			Integer qid = metApIn.getIdPorLinhaArquivo(strDataLine, "qid");
			Integer id = metApIn.getIdPorLinhaArquivo(strDataLine);
			Double classe = classPerId.containsKey(id)?classPerId.get(id):Double.parseDouble(metApIn.getClasseReal(strDataLine));
			
			//apenas escreve se existir
			if(idsToUse.size()==0 || idsToUse.contains(id))
			{
				out.write(metApOut.gerarLinhaDataset(classe, id.intValue(),qid,mapFeatVector));	
			}
			
		}
		in.close();
		out.close();
	}
	public static void main(String[] args) throws Exception
	{

			
			MetodoAprendizado mt = new SVM("decision_three",2F,0.5F,0.1F,SVM.MODE_CLASSIFICATION,true,false);
			System.out.println("Veio aqui!");
			Fold f = new Fold(1,null,new File("/data/experimentos/sigir_2013/datasets/foldseval_predict_logScore/eval_predict_logScore.amostra.treino4"),
					new File("/data/experimentos/sigir_2013/datasets/foldseval_predict_logScore/eval_predict_logScore.amostra.teste4"),
							new File("/data/experimentos/sigir_2013/datasets/foldseval_predict_logScore/eval_predict_logScore.amostra.foldIds4"),
									false);
			f.setPredict(new File("/data/experimentos/sigir_2013/datasets/foldseval_predict_logScore/eval_predict_logScore.predict4"));
			mt.parseResult(f);
			
			
			//Fold[] f = mt.testar(new File("/usr/ferramentas/tenfold/testes/wiki6.amostra"));
			//Fold[] f = mt.testar(new File("/home/hasan/decision_three.amostra"),5,0.1,"","");
			
			
			/*
			System.out.println("Veio aqui!");
			
			for(int i = 0 ; i<f.length ; i++)
			{
				System.out.println("FOLD #"+i);
				Iterator<ResultadoItem> j = f[i].getResultados().iterator();
				while(j.hasNext())
				{
					System.out.println(j.next());
				}
			}*/
			
			//System.out.println("MSE: "+result.getMSE());
			
	
		
	}


}
