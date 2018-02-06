package entidadesAprendizado;

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import scriptsUtil.DatasetUtil;
import stuctUtil.ListaAssociativa;

import aprendizadoResultado.CalculaResultados;
import aprendizadoUtils.MetodoAprendizado;
import arquivo.ArquivoUtil;
import banco.GerenteBD;
import config_tmp.ConfigViewColecao;

public class Fold implements Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final static int[] FOLD_ID_DEFAULT = 	{ 833, 834, 835, 836, 837, 838, 839, 840, 841, 842 };
	public static final long SEED_DEFAULT = 38486178929823L;
	private int id = -1;
	private int num;
	private File arqOrigem;
	private File treino;
	private File teste;
	private File idsFile;
	private File idsTreinoFile;
	private File predict;
	private File modeloTreino;
	private File validation;
	private File idsValidation;
	private long tempoExecucaoFoldTeste;
	private Resultado resultado;
	
	private HashMap<String, String> params = new HashMap<String, String>();
	private List<String> lstLinhasAdicionaisTreino = new ArrayList<String>();
	
	private Set<Integer> setIdsSemClasse = new HashSet<Integer>();
	
	private String nomeBase = "";
	private HashMap<Long, ResultadoItem> resultados = new HashMap<Long, ResultadoItem>();
	
	private List<Long>[] idsPerFolds;
	private int idxValidacao;
	private int idxTeste;
	
	private Fold[] subFolds = null;
	public static boolean inicializaBDGeral = true;
	private static HashMap<Long, PreparedStatement> stmtBuscaIdFoldPool = new HashMap<Long, PreparedStatement>();
	private static HashMap<Long, PreparedStatement> stmtBuscaParamFoldPool = new HashMap<Long, PreparedStatement>();
	private static HashMap<Long, PreparedStatement> deletaParamsPool = new HashMap<Long, PreparedStatement>();
	private static HashMap<Long, PreparedStatement> insereFoldPool = new HashMap<Long, PreparedStatement>();
	private static HashMap<Long, PreparedStatement> insereParamPool = new HashMap<Long, PreparedStatement>();

	static
	{ 
		/*
		try
		{
			inicializaBD();	

			
		} catch (SQLException e)
		{
			// TODO Auto-generated catch block
			//e.printStackTrace();
		} catch (ClassNotFoundException e)
		{
			// TODO Auto-generated catch block
			///e.printStackTrace();
		}
		*/
	}

	public Fold(int num, String nomeBase, List<ResultadoItem> resultados)
			throws Exception
	{
		inicializaThreadsPrepStmt();
		this.num = num;
		this.setResultados(resultados);
		this.nomeBase = nomeBase;
	}
	
	public Fold(int num, File arquivoOrigem, File treino, File teste, File idsTeste,File idsTreino)
	{
		this( num,  arquivoOrigem,  treino,  teste,  idsTeste);
		this.idsTreinoFile = idsTreino;
		
	}
	
	public Fold(int num, File arquivoOrigem, File treino, File teste, File ids,boolean gravarNoBanco)
	{
		super();
		/*
		if(gravarNoBanco)
		{
			inicializaThreadsPrepStmt();
		}
		*/
		this.num = num;
		this.arqOrigem = arquivoOrigem;
		this.treino = treino;
		this.teste = teste;
		this.idsFile = ids;
	}
	public Fold(int num, File arquivoOrigem, File treino, File teste, File ids)
	{
		this(num, arquivoOrigem, treino, teste, ids,true);
	}
	
	public Fold(int num, File arquivoOrigem, File treino, File teste, File ids,
			File predict, File modeloTreino)
	{
		this(num, arquivoOrigem, treino, teste, ids);
		this.predict = predict;
		this.modeloTreino = modeloTreino;
	}
	public void setNum(int numFold)
	{
		this.num = numFold;
	}
	public void setValidationFiles(File validation,File ids)
	{
		this.validation = validation;
		this.idsValidation = ids;
	}
	public List<Long>[] getIdsFold()
	{
		return this.idsPerFolds;
	}
	public void setIdsFold(List<Long>[] idsPerFolds,int idxValidacao, int idxTeste)
	{
		this.idsPerFolds = idsPerFolds;
		this.idxValidacao = idxValidacao;
		this.idxTeste = idxTeste;
	}
	public int getIdxValidacao()
	{
		return this.idxValidacao;
	}
	public int getIdxTeste()
	{
		return this.idxTeste;
	}
	public File getValidation()
	{
		return this.validation;
	}
	public File getIdsValidation()
	{
		return this.idsValidation;
	}
	public List<String> getLstLinhasAdicionaisTreino()
	{
		return this.lstLinhasAdicionaisTreino;
	}
	public void addLstLinhasAdicionaisTreino(List<String> lstAdd)
	{
		this.lstLinhasAdicionaisTreino.addAll(lstAdd);
	}
	public void setIdsTreinoFile(File foldsIdsTreino)
	{
		this.idsTreinoFile = foldsIdsTreino;
	}
	public void limpaResultados()
	{
		this.resultados = new HashMap<Long, ResultadoItem>();
		if(this.subFolds!=null)
		{
			for(Fold f : this.subFolds)
			{
				f.limpaResultados();
			}
		}
	}
	public File getIdsTreinoFile()
	{
		return this.idsTreinoFile;
	}
	public String getNomeBase()
	{
		if (this.nomeBase.length() > 0)
		{
			return this.nomeBase;
		}
		return this.getTreino().getName().replaceAll("\\..*", "");
	}
	/**
	 * Get which view this result pertence
	 * @return
	 */
	public View[] getView()
	{
		return this.resultado.getViews();
	}
	public void setResultado(Resultado result)
	{
		this.resultado = result;
	}
	public ConfigViewColecao getCnfViewColecao()
	{
		return this.resultado.getViews()[0].getCnfView();
	}
	private synchronized void inicializaThreadsPrepStmt()
	{
		Long idCurrentThread = Thread.currentThread().getId();

		PreparedStatement stmtBuscaIdFold = stmtBuscaIdFoldPool.get(idCurrentThread);
		if (stmtBuscaIdFold == null)
		{
			try
			{
				inicializaBD();
			} catch (SQLException e)
			{
				// TODO Auto-generated catch block
				//e.printStackTrace();
			} catch (ClassNotFoundException e)
			{
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
		}
		// System.out.println("CURRENT THREAD LOOKUP: "+idCurrentThread);
	}
	
	public static List<Integer> getIdsFromIdFile(File ids) throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(ids));
		String strId;
		List<Integer> lstIds = new ArrayList<Integer>();
		while ((strId = in.readLine()) != null)
		{
			lstIds.add(Integer.parseInt(strId));
		}
		in.close();

		return lstIds;		
	}
	public Set<Integer> getIdsSemClasse() throws IOException
	{
		
		if(this.setIdsSemClasse.size() == 0 && this.idsFile != null)
		{
			for(int id : Fold.getIdsFromIdFile(this.idsFile))
			{
				this.setIdsSemClasse.add(id);
			}
			

		}
		
		return this.setIdsSemClasse;
	}
	public static File criaIdsFile(File dir,List<Integer> lstIds,String strSufix) throws IOException
	{
		File f = File.createTempFile("tmp", strSufix,dir);
		f.deleteOnExit();
		BufferedWriter out = new BufferedWriter(new FileWriter(f, false),100);
		for(Integer id : lstIds)
		{
			out.write(Integer.toString(id));
			out.write("\n");
		}
		
		
		out.close();
		
		
		return f;
	}
	public void addIdsToLstSemClasse(File ids) throws IOException
	{
		for(int id : Fold.getIdsFromIdFile(ids))
		{
			this.setIdsSemClasse.add(id);
		}
			
	}
	public void addIdsToLstSemClasse(Set<Integer> lstIds) throws IOException
	{
		for(long id : lstIds)
		{
			this.setIdsSemClasse.add((int)id);
		}
			
	}
	public void addIdsToLstSemClasse(List<Long> lstIds) throws IOException
	{
		for(long id : lstIds)
		{
			this.setIdsSemClasse.add((int)id);
		}
			
	}
	public void addIdsToLstSemClasse(MetodoAprendizado metAp,File fileDataset) throws IOException
	{
			
			for(long id : metAp.getIds(fileDataset))
			{
				this.setIdsSemClasse.add((int) id);
			}
}
	private static void inicializaBD() throws SQLException,
			ClassNotFoundException
	{
		Long idCurrentThread = Thread.currentThread().getId();
		// System.out.println("CURRENT THREAD ADD: "+idCurrentThread);
		// System.out.println("ESQUEMA_RESULT: "+esquemaResult+"\tESQUEMA_AMOSTRA: "+esquemaAmostra);

		Connection conn = GerenteBD.getGerenteBD().obtemConexao(idCurrentThread +"");
		stmtBuscaParamFoldPool.put(idCurrentThread, conn
				.prepareStatement("select " + "fld.param_name,"
						+ "fld.param_value\n" + "from "
						+ " wiki_results.params_fold fld " + " where "
						+ "	fold_id = ? "));
		stmtBuscaIdFoldPool.put(idCurrentThread, conn
				.prepareStatement("select " + "fld.id " + "from "
						+ " wiki_results.fold fld " + " where "
						+ "	end_ids = ? " + " order by fold_num desc "));
		deletaParamsPool
				.put(
						idCurrentThread,
						conn
								.prepareStatement("delete from wiki_results.params_fold where fold_id = ?"));

		insereFoldPool.put(idCurrentThread, conn
				.prepareStatement("insert  wiki_results.fold " + " ("
						+ "	fold_num," + "	end_origem," + "	end_treino,"
						+ "	end_modelo_treino," + "	end_teste,"
						+ "	end_resultado," + "	end_ids" + ")" + "	values"
						+ "(" + "	?," + "	?," + "	?," + "	?," + "	?," + "	?,"
						+ "	?" + ")"));
		insereParamPool.put(idCurrentThread, conn
				.prepareStatement("insert wiki_results.params_fold " + " ("
						+ "	fold_id," + "	param_name," + "	param_value" + ")"
						+ "	values" + "(" + "	?," + "	?," + "	?" + ")"));
	}

	public void adicionaParam(String name, String value)
	{
		this.params.put(name, value);
	}

	public long getTempoExecucaoTeste()
	{
		return this.tempoExecucaoFoldTeste;
	}

	public void setTempoExecucaoTeste(long tempo)
	{
		this.tempoExecucaoFoldTeste = tempo;
	}

	public Integer getId() throws Exception
	{
		if (this.idsFile == null && this.id <= 0)
		{
			return FOLD_ID_DEFAULT[this.num];
		}
		if (this.id <= 0)
		{
			this.id = this.buscarId();
		}
		return this.id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public int getNum()
	{
		return this.num;
	}

	public File getTreino()
	{
		return treino;
	}

	public void setTreino(File treino)
	{
		this.treino = treino;
	}

	public File getTeste()
	{
		return teste;
	}

	public File getOrigem()
	{
		return this.arqOrigem;
	}

	public void setOrigem(File arqOrigem)
	{
		this.arqOrigem = arqOrigem;
	}

	public void setTeste(File teste)
	{

		this.teste = teste;
	}

	public File getPredict()
	{
		return predict;
	}

	public void setPredict(File predict)
	{
		this.predict = predict;
	}

	public File getIdsFile()
	{
		return idsFile;
	}

	public void setIdsFile(File ids)
	{
		this.idsFile = ids;
	}

	public Set<Long> getIdsResultado()
	{
		return this.resultados.keySet();
	}

	public void adicionaResultado(ResultadoItem r)
	{
		r.setFold(this);
		this.resultados.put(r.getId(), r);
	}

	public void adicionaTodosResultados(List<ResultadoItem> rs)
			throws Exception
	{
		Iterator<ResultadoItem> i = rs.iterator();
		while (i.hasNext())
		{
			ResultadoItem r = i.next();
			r.setFold(this);
			if (this.resultados.containsKey(r.getId()))
			{
				throw new Exception("O fold ja possui o id:" + r.getId());
			}
			this.resultados.put(r.getId(), r);
		}

	}
	public static Fold getMinMSEFold(Fold[] subFoldTreino) throws SQLException {
		double mseMin = Double.MAX_VALUE;
		Fold objSubFoldMseMin = null;
		for(int j =0 ; j<subFoldTreino.length ; j++)
		{
			double mse = CalculaResultados.getMSEMedio(subFoldTreino[j]);
			if(mse < mseMin)
			{
				mseMin = mse;
				objSubFoldMseMin = subFoldTreino[j];
			}
		}
		return objSubFoldMseMin;
	}
	public boolean existeTreinoTeste()
	{
		return this.treino.exists() && this.teste.exists()
				&& this.idsFile.exists();
	}

	public boolean existeResultado()
	{
		return this.predict !=null && this.predict.exists() && existeTreinoTeste();
	}

	public void setResultados(List<ResultadoItem> resultados)
			throws Exception
	{
		setResultados(resultados,true);

	}

	public void setResultados(List<ResultadoItem> resultados,boolean associarFold)
			throws Exception
	{
		Iterator<ResultadoItem> i = resultados.iterator();
		while (i.hasNext())
		{
			ResultadoItem r = i.next();
			if(associarFold)
			{
				r.setFold(this);
			}
			if (this.resultados.containsKey(r.getId()))
			{
				throw new Exception("O fold ja possui o id:" + r.getId());
			}
			this.resultados.put(r.getId(), r);
		}

	}

	public File getModeloTreino()
	{
		return modeloTreino;
	}

	public void setModeloTreino(File modeloTreino)
	{
		this.modeloTreino = modeloTreino;
		
	}
	public void removeResultado(long id)
	{
		this.resultados.remove(id);
	}
	public Map<Long,Integer> getRankPorResultId()
	{
		//if(mapResultPorQid.isEmpty() || !onlyIfIsEmpty)
		//{
			//ordena resultado dentro do seu qid e depois armazena em posPorResultado
			List<ResultadoItem> lstResults = new ArrayList<ResultadoItem>(this.resultados.values());
			//gera o array de classes
			Collections.sort(lstResults, new Comparator<ResultadoItem>(){

				@Override
				public int compare(ResultadoItem o1, ResultadoItem o2)
				{
					//se for do mesmo qid, ordena pelo resultado predito caso contrario pelo qid
					if(o1.getQID() == o2.getQID() && o1.getQID() != null &&  o2.getQID() != null)
						
					{
						return (int) Math.round((o2.getClassePrevista()*10000.0)-(o1.getClassePrevista()*10000.0));
					}
					return (int) Math.round((o2.getQID()*10000.0)-(o1.getQID()*10000.0)); 
				}
				});
				
			
			int pos = 0;
			long lastQid = Long.MIN_VALUE;
			
			Map<Long,Integer> mapPosPorResult = new HashMap<Long,Integer>();
			for(ResultadoItem ri : lstResults)
			{
				//cada vez q mudar o qid, reiniciar pos 
				if(ri.getQID() != lastQid)
				{
					pos = 0;
					 
				}
				//coloca a poscao para este resulto
				mapPosPorResult.put(ri.getId(), pos);
				
				
				pos++;
				lastQid = ri.getQID();
			}
			
			return mapPosPorResult;
		//}
	}
	public ArrayList<ResultadoItem> getResultadosValues()
	{
		return new ArrayList<ResultadoItem>(this.resultados.values());
	}
	public ListaAssociativa<Long, ResultadoItem> getResultadoItemPerQID()
	{
		ListaAssociativa<Long, ResultadoItem> lstResultPerQID = new ListaAssociativa<Long, ResultadoItem>();
		
		for(ResultadoItem ri : this.resultados.values())
		{
			lstResultPerQID.put(ri.getQID() != null?ri.getQID():0L, ri);
		}
		
		return lstResultPerQID;
	}

	public ResultadoItem getResultadoPorId(long id)
	{
		return this.resultados.get(id);
	}

	private int buscarId() throws Exception
	{
		inicializaThreadsPrepStmt();
		PreparedStatement stmtBuscaIdFold = getStmtBuscaIdFold();
		stmtBuscaIdFold.setString(1, this.idsFile.getAbsolutePath());
		ResultSet rst = stmtBuscaIdFold.executeQuery();
		if (rst.next())
		{
			int id = rst.getInt(1);
			rst.close();
			return id;
		}
		throw new Exception("Item nao encontrado");
	}

	public void buscaParams() throws SQLException, Exception
	{
		inicializaThreadsPrepStmt();
		PreparedStatement stmtBuscaParamFold = getStmtBuscaParamFold();
		stmtBuscaParamFold.setInt(1, this.getId());
		ResultSet rst = stmtBuscaParamFold.executeQuery();

		while (rst.next())
		{
			String nome = rst.getString(1);
			String valor = rst.getString(1);
			this.adicionaParam(nome, valor);
		}
		rst.close();
	}

	public void inserir() throws SQLException
	{
		inicializaThreadsPrepStmt();
		PreparedStatement insereFold = getInsereFold();
		if(insereFold == null)
		{
			this.inicializaThreadsPrepStmt();
			insereFold = getInsereFold();
		}
		if (this.idsFile == null)
		{
			return;
		}
		insereFold.setInt(1, this.num);
		insereFold.setString(2, this.arqOrigem!= null?this.arqOrigem.getAbsolutePath(): "no_path");
		insereFold.setString(3, this.treino != null ? this.treino
				.getAbsolutePath() : null);
		insereFold.setString(4, this.modeloTreino != null ? this.modeloTreino
				.getAbsolutePath() : null);
		insereFold.setString(5, this.teste != null ? this.teste
				.getAbsolutePath() : null);
		insereFold.setString(6, this.predict != null ? this.predict
				.getAbsolutePath() : null);
		insereFold.setString(7, this.idsFile != null ? this.idsFile
				.getAbsolutePath() : null);
		insereFold.execute();
		//insereFold.close();

	}

	public double getSomaErroQuadratico() throws SQLException
	{
		Collection<ResultadoItem> lstResultados =this.resultados.values(); 
		return CalculaResultados.getMSE(lstResultados);
	}
	
	public double getAcuracia() throws SQLException
	{
		Collection<ResultadoItem> lstResultados =this.resultados.values();
		return CalculaResultados.getAcurracia(lstResultados);
	}

	public String getParam(String chave)
	{
		return this.params.get(chave);
	}

	public Map<String,String> getParams()
	{
		return this.params;
	}
	public int getNumResults()
	{
		return this.resultados.size();
	}

	private void deletaParams() throws Exception
	{
		PreparedStatement deletaParams = getDeletaParams();
		int foldId = this.getId();
		if (id > 0)
		{
			deletaParams.setInt(1, foldId);
			deletaParams.execute();
			//deletaParams.close();
		}
		
	}

	public void inserirParams() throws SQLException, Exception
	{
		PreparedStatement insereParam = getInsereParam();
		Iterator<String> i = this.params.keySet().iterator();
		int foldId = this.getId();
		// deletaParams();
		while (i.hasNext())
		{
			String name = i.next();
			String value = this.params.get(name);

			insereParam.setInt(1, foldId);
			insereParam.setString(2, name);
			insereParam.setString(3, value);
			insereParam.execute();
		}
		//insereParam.close();
	}

	public Fold clonaFoldFiltrandoResultados(	ArrayList<ResultadoItem> resultsFold, String sufixoNomeBase)
			throws Exception
	{
		Fold foldResultadoNovo = new Fold(this.getNum(), sufixoNomeBase
				+ this.getNomeBase(), new ArrayList<ResultadoItem>());

		ArrayList<ResultadoItem> results = new ArrayList<ResultadoItem>(
				resultsFold);
		results.retainAll(resultsFold);
		foldResultadoNovo.setResultados(results);
		foldResultadoNovo.setOrigem(this.getOrigem());

		foldResultadoNovo.setIdsFile(this.getIdsFile());
		foldResultadoNovo.setModeloTreino(this.getModeloTreino());
		foldResultadoNovo.setTreino(this.getTreino());
		foldResultadoNovo.setTeste(this.getTeste());
		foldResultadoNovo.setPredict(this.getPredict());

		return foldResultadoNovo;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException
	{
		Fold f = (Fold) super.clone();
		try
		{
			f.setResultados(new ArrayList<ResultadoItem>());
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO Auto-generated method stub
		return f;
	}
	public static Fold[] divideIntoFolds(File arq,String nome,int numFolds,MetodoAprendizado met) throws IOException
	{
		List<Long> lstIds = met.getIds(arq);
		
		return met.dividePerFoldsByIds(lstIds,nome,nome+"Folds", numFolds,lstIds.size()/numFolds);
	}
	public static List<Long>[] divideIntoFolds(int numFolds, List<Long> lstIds)
	{
		
		
		
		return divideIntoFolds( numFolds, lstIds,Fold.SEED_DEFAULT);

	}
	public static List<Long>[] divideIntoFolds(int numFolds, List<Long> lstIds,long randSeed)
	{
		int itensPerFold = Math.round(lstIds.size() / numFolds);
		
		return divideIntoFolds(numFolds, lstIds, itensPerFold,randSeed);		
	}
	public static List<Long>[] divideIntoFolds(int numFolds,List<Long> lstIdsFolds,int numItens)
	{
		return divideIntoFolds( numFolds,lstIdsFolds,  numItens,Fold.SEED_DEFAULT);		
	}
	public static List<Long>[] divideIntoFolds(int numFolds,Collection<Long> lstIds, int itensPerFold,long randSeed)
	{
		
		List<Long> lstIdsSobrando = new ArrayList<Long>(lstIds);
		
		List<Long>[] lstByFolds = new List[numFolds];
		
		Random randomGenerator = new Random(randSeed);
		
		for (int i = 0; i < numFolds; i++)
		{
			// add a random id to the fold
			lstByFolds[i] = new ArrayList<Long>();
			for (int j = 0; j < itensPerFold && lstIdsSobrando.size()>0; j++)
			{
				int num = randomGenerator.nextInt(lstIdsSobrando.size());
				Long idSelected = lstIdsSobrando.get(num);
				lstIdsSobrando.remove(num);
				lstByFolds[i].add(idSelected);
			}

		}
		
		// put on the missing ones on the last fold
		for (Long id : lstIdsSobrando)
		{
			lstByFolds[numFolds - 1].add(id);
		}
		
		return lstByFolds;
	}
	
	public static List<Long>[] divideIntoFoldsPerGroup(int numFolds, Map<Long,List<Long>> idsPerGroup)
	{
		long randSeed = 8971346546L;
		List<Long> lstUsers = new ArrayList<Long>();
		lstUsers.addAll(idsPerGroup.keySet());
		
		//deixa os usuarios de forma aleatoria
		lstUsers = divideIntoFolds(1,lstUsers, lstUsers.size(),randSeed)[0];
		
		
		//folds
		List<Long>[] lstByFolds = new List[numFolds];
		
		//numero do fold
		int numFold = 0;
		
		for(long usr : lstUsers)
		{
			//adiciona no fold
			lstByFolds[numFold].addAll(idsPerGroup.get(numFold));
			
			if(numFold < numFolds)
			{
				numFold++;
			}else
			{
				numFold = 0;
			}
		}
		
		return lstByFolds;
		
	} 
	
	
	public static List<Long>[] divideIntoFolds(int numFolds,Collection<Long> lstIds, int itensPerFold)
	{
		//System.out.println("Oioi!");
		return divideIntoFolds(numFolds,lstIds,  itensPerFold, SEED_DEFAULT);
	}
	public static double getResultFold(Fold f,boolean isClassificacao) throws SQLException
	{
		if(isClassificacao)
		{
			return f.getAcuracia();
		}else
		{
			return f.getSomaErroQuadratico()/(double)f.getNumResults();
		}
	}
	public static double getResultFold(Fold[] arrF,boolean isClassificacao) throws SQLException
	{
		
			double sumResults = 0;
			for(Fold f : arrF)
			{
				if(isClassificacao)
				{
					sumResults += f.getAcuracia();
				}else
				{
					sumResults += f.getSomaErroQuadratico()/(double)f.getNumResults();
				}
				
			}
			return sumResults/arrF.length;
	}
	public ArrayList<Long> getIdsTeste() throws IOException
	{
		if (this.idsFile != null)
		{
			String[] texto = ArquivoUtil.leTexto(this.idsFile).split("\n");
			return gerarIdsFrom(texto);
		}
		return null;
	}
	public ArrayList<Long> getIdsTreino() throws IOException
	{
		if (this.idsTreinoFile != null)
		{
			String[] texto = ArquivoUtil.leTexto(this.idsTreinoFile).split("\n");
			return gerarIdsFrom(texto);
		}
		return null;
	}
	private ArrayList<Long> gerarIdsFrom(String[] texto) {
		ArrayList<Long> idsInteiro = new ArrayList<Long>();
		for (int i = 0; i < texto.length; i++)
		{
			idsInteiro.add(Long.parseLong(texto[i]));
		}
		return idsInteiro;
	}


	public static void main(String[] args)
	{
		List<Long> lstIds = new ArrayList<Long>();
		lstIds.add(1L);
		lstIds.add(2L);
		lstIds.add(3L);
		lstIds.add(4L);
		lstIds.add(5L);
		lstIds.add(6L);
		lstIds.add(7L);
		lstIds.add(8L);
		lstIds.add(9L);
		lstIds.add(10L);
		lstIds.add(11L);
		lstIds.add(12L);
		lstIds.add(13L);

		List<Long>[] lstArrFolds = divideIntoFolds(3, lstIds);
		for (int i = 0; i < lstArrFolds.length; i++)
		{
			System.out.println("FOLD #" + i + ": " + lstArrFolds[i]);
		}
	}

	public synchronized PreparedStatement getStmtBuscaIdFold()
	{
		Long idCurrentThread = Thread.currentThread().getId();

		PreparedStatement stmtBuscaIdFold = stmtBuscaIdFoldPool.get(idCurrentThread);
		return stmtBuscaIdFold;
	}


	public synchronized PreparedStatement getStmtBuscaParamFold()
	{
		Long idCurrentThread = Thread.currentThread().getId();

		PreparedStatement stmtBuscaParamFold = stmtBuscaParamFoldPool.get(idCurrentThread);
		return stmtBuscaParamFold;
	}



	public synchronized PreparedStatement getDeletaParams()
	{
		Long idCurrentThread = Thread.currentThread().getId();

		PreparedStatement deletaParams = deletaParamsPool.get(idCurrentThread);
		return deletaParams;
	}

	public synchronized PreparedStatement getInsereFold()
	{
		Long idCurrentThread = Thread.currentThread().getId();

		PreparedStatement insereFold = insereFoldPool.get(idCurrentThread);
		
		return insereFold;
	}


	public synchronized PreparedStatement getInsereParam()
	{
		Long idCurrentThread = Thread.currentThread().getId();

		PreparedStatement insereParam = insereParamPool.get(idCurrentThread);
		
		return insereParam;
	}
	public Fold[] getSubFolds() {
		return subFolds;
	}
	public void setSubFolds(Fold[] subFolds) {
		this.subFolds = subFolds;
	}


}
