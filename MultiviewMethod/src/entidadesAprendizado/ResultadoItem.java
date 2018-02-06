package entidadesAprendizado;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import matematica.KendallTau;

import stuctUtil.ItemPerThread;
import stuctUtil.ListaAssociativa;
import stuctUtil.Tupla;
import banco.GerenteBD;

import com.mysql.jdbc.NotImplemented;



public class ResultadoItem implements Comparable<ResultadoItem>, Serializable
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;

	private long id;
	private Long qid = null;
	private float classeReal;
	private float classePrevista;
	private Map<Integer,Float> probPorClasse = new HashMap<Integer,Float>();
	private float confianca;
	private boolean impossibleToPredict = false;

	
	private static HashMap<Long,PreparedStatement> stmtInsereResultPool = new HashMap<Long,PreparedStatement>();
	private static HashMap<Long,PreparedStatement> stmtAtualizaConfiancaResultPool = new HashMap<Long,PreparedStatement>();
	private static HashMap<Long,PreparedStatement> stmtInsereprobResultPool = new HashMap<Long,PreparedStatement>();
	private static HashMap<Long,PreparedStatement> stmtDeleteProbResultPool = new HashMap<Long,PreparedStatement>();
	private static HashMap<Long,PreparedStatement> stmtExisteResultadoPool = new HashMap<Long,PreparedStatement>();
	
	
	
	private static boolean iniatilizedBD = false;
	private Fold fold;
	private int resultOrder;
	
	public ResultadoItem(long id,float classeReal, float classePrevista,
			float[] probPorClasse,boolean gravarNoBanco) throws SQLException, ClassNotFoundException {
		Map<Integer,Float> mapProbPorClasse = new HashMap<Integer,Float>();
		for(int i = 0 ; i<probPorClasse.length ; i++)
		{
			mapProbPorClasse.put(i, probPorClasse[i]);
		}

		inicializa(id, classeReal, classePrevista, mapProbPorClasse, gravarNoBanco);

	}
	public ResultadoItem(long id,float classeReal, float classePrevista,
			Map<Integer,Float> probPorClasse,boolean gravarNoBanco) throws SQLException, ClassNotFoundException {
		super();
		inicializa(id, classeReal, classePrevista, probPorClasse, gravarNoBanco);
	}
	
	public ResultadoItem(long id,float classeReal, float classePrevista,
			float[] probPorClasse) throws SQLException, ClassNotFoundException {
		this(id,classeReal, classePrevista,
				 probPorClasse,true);
	}	
	public ResultadoItem(long id,float classeReal, float classePrevista,
			float confianca) {
		super();
		inicializaThreadsPrepStmt();
		this.classeReal = classeReal;
		this.classePrevista = classePrevista;
		this.confianca = confianca;
		this.id = id;
		
		
	}	
	
	private void inicializa(long id, float classeReal, float classePrevista,
			Map<Integer, Float> probPorClasse, boolean gravarNoBanco)
			throws SQLException, ClassNotFoundException {
		if(gravarNoBanco)
		{
			Long idCurrentThread = Thread.currentThread().getId();
			
			if(!stmtInsereResultPool.containsKey(idCurrentThread))
			{
				inicializaBD();
			}
			inicializaThreadsPrepStmt();
		}

		this.classeReal = classeReal;
		this.classePrevista = classePrevista;
		this.probPorClasse = probPorClasse;
		this.id = id;
		this.confianca = 0;
		if(this.probPorClasse != null)
		{
			for(int classe : probPorClasse.keySet())
			{
				float probClasse = probPorClasse.get(classe);
				if(probClasse>this.confianca)
				{
					this.confianca = probClasse;
				}
			}
		}
	}
	
	public static ListaAssociativa<String,ResultadoItem> agrupaResultPerQID(List<ResultadoItem> lstResultados)
	{
		//agrupa resultados por qid
		ListaAssociativa<String,ResultadoItem> resultPerQid = new ListaAssociativa<String,ResultadoItem>();
		for(ResultadoItem r : lstResultados)
		{
				resultPerQid.put( r.getQID()==null?"null":Long.toString(r.getQID()), r);
		}
		return resultPerQid;
	}
	public int getOrder()
	{
		return this.resultOrder;
	}
	public void setOrder(int order)
	{
		this.resultOrder = order;
	}
	public void setQID(long qid)
	{
		this.qid = qid;
	}
	public Long getQID()
	{
		return this.qid;
	}
	public void setImpossibleToPredict()
	{
		impossibleToPredict = true;
	}
	public boolean isImpossibleToPredict()
	{
		return impossibleToPredict;
	}

	public Fold getFold()
	{
		return this.fold;
	}
	public void setFold(Fold fold)
	{
		this.fold = fold;
	}
	public float getProbHigherOrEqualClass()
	{
		float prob = 0;
		for(int classe : this.probPorClasse.keySet())
		{
			if(this.classePrevista >= classe)
			{
				prob += this.probPorClasse.get(classe);
			}
		}
		return prob;
	}
	public float getProbLowerClass()
	{
		float prob = 0;
		for(int classe : this.probPorClasse.keySet())
		{
			if(this.classePrevista < classe)
			{
				prob += this.probPorClasse.get(classe);
			}
		}
		return prob;
	}
	public int compareForRank(ResultadoItem o2)
	{
		//caso possua probabilidade usa-la cas ocontrario, compara com a classe prevista
		if(probPorClasse.keySet().size()>0)
		{
			if(this.classePrevista != o2.classePrevista)
			{
				return (int) (o2.classePrevista - this.classePrevista);
			}else
			{
				float valO2 = (o2.getProbHigherOrEqualClass()-o2.getProbLowerClass());
				float valThis = (this.getProbHigherOrEqualClass()-this.getProbLowerClass());
				int val = (int) ( 100*valO2 - 100*valThis);
				return val;
				
			}
		}else
		{
			// TODO Auto-generated method stub
			return ((int)(o2.getClassePrevista()*100)-(int)(this.getClassePrevista()*100));
		}
	}
	private void inicializaThreadsPrepStmt()   
	{
		Long idCurrentThread = Thread.currentThread().getId();
		
		PreparedStatement stmtInsereResult = stmtInsereResultPool.get(idCurrentThread);
		if(stmtInsereResult == null )
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
	private  static void inicializaBD()throws SQLException,ClassNotFoundException
	{
		Long idCurrentThread = Thread.currentThread().getId();
		
		//System.out.println("ESQUEMA_RESULT: "+esquemaResult+"\tESQUEMA_AMOSTRA: "+esquemaAmostra);
		Connection conn = GerenteBD.getGerenteBD().obtemConexao(idCurrentThread +"");
		stmtInsereResultPool.put(idCurrentThread,conn.prepareStatement("insert into wiki_results.resultado_regressao " +
														"(page_id," +
														"fold_id,"+
														"nomExperimento," +
														"result," +
														"probResult," +
														"classeReal," +
														"qid"+
														") " +
													"VALUES " +
														"(?," +
														"?,"+
														"?," +
														"?," +
														"?," +
														"?," +
														"?" +
														")"));
		
		stmtInsereprobResultPool.put(idCurrentThread,conn.prepareStatement("insert into wiki_results.prob_result " +
															"(" +
															"fold_id," +
															"nomExperimento,"+
															"page_id,"+
															"class_val," +
															"result"+
															") " +
														"VALUES " +
															"(?," +
															"?,"+
															"?,"+
															"?," +
															"?"+
															")"));
		
		
		stmtDeleteProbResultPool.put(idCurrentThread,conn.prepareStatement("delete from wiki_results.prob_result where fold_id = ? and nomExperimento = ? "));
		stmtAtualizaConfiancaResultPool.put(idCurrentThread,conn.prepareStatement("update wiki_results.resultado_regressao " +
																"	set " +
																"		probResult = ?" +
																"	where" +
																"		nomExperimento = ?" +
																"		and page_id = ?" +
																"		and fold_id = ?"));
		stmtExisteResultadoPool.put(idCurrentThread, conn.prepareStatement("select 1 from wiki_results.resultado_regressao where nomExperimento = ? limit 1"));
	}
	public float calculaEntropia()
	{
		float sum = 0;
		for(float prob : probPorClasse.values())
		{
			//System.out.println("PROB da class: "+ probPorClasse[i]+" LOG:"+Math.log(probPorClasse[i]));
			if(Math.log(prob) ==Float.NEGATIVE_INFINITY)
			{
				sum = Float.NEGATIVE_INFINITY;
				break;
			}
			sum += prob * Math.log(prob);
		}
		
		return -sum;
	}
	
	public long getId()
	{
		return this.id;
	}
	public double getClasseReal() {
		return classeReal;
	}
	public void setClasseReal(float classeReal) {
		this.classeReal = classeReal;
	}
	public double getClassePrevista() {
		return classePrevista;
	}
	public void setClassePrevista(float classePrevista) {
		this.classePrevista = classePrevista;
	}
	public double getConfianca()
	{
		return this.confianca;
	}
	public double getErro()
	{
		return Math.abs(this.classePrevista-this.classeReal);
	}
	 
	public void setConfianca(float confianca)
	{
		this.confianca = confianca;
	}
	public Map<Integer,Float> getProbPorClasseMap() {
		return this.probPorClasse;
	}
	public void setProbPorClasse(Map<Integer,Float> mapProb)
	{
		this.probPorClasse = mapProb;
	}
	public float[] getProbPorClasse() {
		ArrayList<Integer> lstClasses = new ArrayList<Integer>(probPorClasse.keySet());
		Collections.sort(lstClasses);
		float[] fltProbs = new float[lstClasses.size()];
		int pos = 0;
		for(int classe : lstClasses)
		{
			fltProbs[pos] = probPorClasse.get(classe);
			pos++;
		}
		return fltProbs;
	}
	public void setProbPorClasse(float[] probPorClasse) {
		Map<Integer,Float> mapProbPorClasse = new HashMap<Integer,Float>();
		for(int i = 0 ; i<probPorClasse.length ; i++)
		{
			mapProbPorClasse.put(i, probPorClasse[i]);
		}
		this.probPorClasse = mapProbPorClasse;
	}
	public void excluirProbResult(String nomExperimento) throws SQLException, Exception
	{

		PreparedStatement stmtDeleteProbResult = getStmtDeleteProbResult();
		
		
		stmtDeleteProbResult.setInt(1, this.getFold().getId());
		stmtDeleteProbResult.setString(2, nomExperimento);
		stmtDeleteProbResult.execute();
		//stmtDeleteProbResult.close();
	}
	public void gravarProbResult(String nomExperimento) throws Exception
	{
		gravarProbResult(nomExperimento,false);
	}
	private static ItemPerThread<Integer> numProbResultOnBatch = new ItemPerThread<Integer>();
	private static ItemPerThread<Integer> numResultOnBatch = new ItemPerThread<Integer>();
	public static int getNumProbResultOnBatch()
	{
		return numProbResultOnBatch.get(0);
	}
	public static int getNumResultOnBatch()
	{
		return numResultOnBatch.get(0);
	}
	public void gravarProbResult(String nomExperimento,boolean onlyBach) throws Exception
	{
		PreparedStatement stmtInsereprobResult = getStmtInsereprobResult();
		
		if(confianca != 0)
		{
			int pos =0;
			for(int classe : probPorClasse.keySet())
			{
				pos++;
				if(this.getFold()!=null)
				{
					Integer foldId = this.getFold().getId();
					if(foldId != null)
					{
						stmtInsereprobResult.setInt(1, foldId);	
					}else
					{
						stmtInsereprobResult.setInt(1, 0);
					}
						
					
				}else
				{
					stmtInsereprobResult.setInt(1, 0);
				}
				//stmtInsereprobResult.setInt(1, this.getFold().getId());
				stmtInsereprobResult.setString(2, nomExperimento);
				stmtInsereprobResult.setLong(3, this.id);
				stmtInsereprobResult.setInt(4, classe);
				stmtInsereprobResult.setFloat(5, this.probPorClasse.get(classe));
				stmtInsereprobResult.addBatch();
				
				numProbResultOnBatch.set(numProbResultOnBatch.get(0)+1);

				
			}
			if(!onlyBach)
			{
				numProbResultOnBatch.set(0);
				stmtInsereprobResult.executeBatch();
			}
			//stmtInsereprobResult.close();
		}
	}
	public void gravaResultadoBanco(String nomExperimento,boolean onlyBatch) throws Exception
	{
		PreparedStatement stmtInsereResult = getStmtInsereResult();
		
		stmtInsereResult.setLong(1, this.id);
		if(this.getFold()!=null)
		{
			Integer foldId = this.getFold().getId();
			if(foldId != null)
			{
				stmtInsereResult.setInt(2, foldId);	
			}else
			{
				stmtInsereResult.setInt(2, 0);	
			}
		}else
		{
			stmtInsereResult.setInt(2, 0);
		}
		
		stmtInsereResult.setString(3, nomExperimento);
		stmtInsereResult.setFloat(4, this.classePrevista);
		stmtInsereResult.setFloat(5, this.confianca);
		stmtInsereResult.setFloat(6, this.classeReal);
		if(this.qid != null)
		{
			stmtInsereResult.setFloat(7, this.qid);
		}else
		{
			stmtInsereResult.setNull(7, Types.FLOAT);
		}
		if(onlyBatch)
		{
			numResultOnBatch.set(numResultOnBatch.get(0)+1);
			stmtInsereResult.addBatch();
			
		}else
		{
			stmtInsereResult.execute();
		}
		//stmtInsereResult.close();
		this.gravarProbResult(nomExperimento,onlyBatch);
	}
	public void gravaResultadoBanco(String nomExperimento) throws Exception
	{
		gravaResultadoBanco( nomExperimento, false);
	}
	public static void executeBatchResult() throws SQLException
	{
		PreparedStatement stmtInsereResult = getStmtInsereResult();
		stmtInsereResult.executeBatch();
		numResultOnBatch.set(0);

	}
	public static void executeBatchProbResult() throws SQLException
	{
		PreparedStatement stmtInsereprobResult = getStmtInsereprobResult();
		stmtInsereprobResult.executeBatch();
		numProbResultOnBatch.set(0);
	}
	public void atualizaConfianca(String nomExperimento) throws Exception
	{
		/*System.out.println("update wiki_results.resultado_regressao " +
																"	set " +
																"		probResult = " +this.confianca+
																"	where" +
																"		nomExperimento = " +nomExperimento+
																"		and page_id = " +this.id+
																"		and fold_id = "+this.getFold().getId());*/
		PreparedStatement stmtAtualizaConfiancaResult = getStmtAtualizaConfiancaResult();
		stmtAtualizaConfiancaResult.setFloat(1, this.confianca);
		stmtAtualizaConfiancaResult.setString(2, nomExperimento);
		stmtAtualizaConfiancaResult.setLong(3, this.id);
		stmtAtualizaConfiancaResult.setInt(4, this.getFold().getId());
		stmtAtualizaConfiancaResult.execute();
	}

	public String toString()
	{
		String probs = "";
		/*
		if(this.probPorClasse != null)
		{
			for(int i =0 ; i<probPorClasse.length; i++)
			{
				if(probPorClasse[i] != 0)
				{
					probs += "Class"+i+" = "+probPorClasse[i]+"; ";
				}
			}
		}
		*/
		return "ID:"+this.id+" \tResultado previsto: "+this.classePrevista+"\tProbLower:"+this.getProbLowerClass()+"\t probHigher:"+this.getProbHigherOrEqualClass()+"\tReal: "+this.classeReal+" Probs:"+probPorClasse+" Confianca: "+this.confianca;
	}
	public boolean equals(Object ob)
	{
		if(ob instanceof ResultadoItem)
		{
			try {
				ResultadoItem rst = (ResultadoItem) ob;
				if(this.fold != null && rst.fold != null)
				{
					/*
					if(this.fold.getId() != rst.getFold().getId() && this.id == rst.getId())
					{
						System.err.println("Nao podia ser certo aqui");
						throw new RuntimeErrorException(new Error());
					}
					*/
					return this.id == rst.getId();// && this.fold.getId() == rst.getFold().getId();
				}else
				{
					if(this.fold == null && rst.fold == null)
					{
						return this.id == rst.getId();
					}
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return false;
	}
	public static List<Tupla<Integer,ResultadoItem>> getResultsRanking(List<ResultadoItem> lstResults)
	{
		//ordena por classe prevista (qnto maior melhor)
		Collections.sort(lstResults,new Comparator<ResultadoItem>() {

			@Override
			public int compare(ResultadoItem o1, ResultadoItem o2) {
				// TODO Auto-generated method stub
				if(o1.getClassePrevista() < o2.getClassePrevista())
				{
					return 1;
				}
				if(o1.getClassePrevista() > o2.getClassePrevista())
				{
					return -1;
				}
				return 0;
				
			}
		});
		
		//cria o ranking
		int rankPos = 1;
		List<Tupla<Integer,ResultadoItem>> lstRanking = new ArrayList<Tupla<Integer,ResultadoItem>>();
		for(ResultadoItem ri : lstResults)
		{
			lstRanking.add(new Tupla<Integer, ResultadoItem>(rankPos, ri));
			rankPos++;
		}
		
		
		return lstRanking;
		
		
	}
	public static double getKendallTauRanking(List<ResultadoItem> resultX,List<ResultadoItem> resultY)
	{
		List<Tupla<Integer,ResultadoItem>> lstRankResultX = getResultsRanking(resultX);
		List<Tupla<Integer,ResultadoItem>> lstRankResultY = getResultsRanking(resultY);
		
		//ordena os rankings pelo id do resultado 
		Collections.sort(lstRankResultX,new Comparator<Tupla<Integer,ResultadoItem>>() {

			@Override
			public int compare(Tupla<Integer, ResultadoItem> o1,
					Tupla<Integer, ResultadoItem> o2) {
				// TODO Auto-generated method stub
				return (int) (o1.getY().getId()-o2.getY().getId());
			}
			
		});
		
		//ordena os rankings pelo id do resultado 
		Collections.sort(lstRankResultY,new Comparator<Tupla<Integer,ResultadoItem>>() {

			@Override
			public int compare(Tupla<Integer, ResultadoItem> o1,
					Tupla<Integer, ResultadoItem> o2) {
				// TODO Auto-generated method stub
				return (int) (o1.getY().getId()-o2.getY().getId());
			}
			
		});
		
		//cria a entrada
		List<Tupla<Double,Double>> lstRankPos = new ArrayList<Tupla<Double,Double>>();
		for(int i =0 ; i < lstRankResultY.size() ; i++)
		{
			lstRankPos.add(new Tupla<Double,Double>(lstRankResultX.get(i).getX().doubleValue(),
														lstRankResultY.get(i).getX().doubleValue()));
			
		}
		return KendallTau.compareRanking(lstRankPos);
	}
	public static List<Long> getIdsResultadoItem(List<ResultadoItem> result)
	{
		ArrayList<Long> ids = new ArrayList<Long>();
		Iterator<ResultadoItem> i = result.iterator();
		while(i.hasNext())
		{
			ResultadoItem r = i.next();
			ids.add((long)r.getId());
		}
		return ids;
	}
	public  static void main(String[] args) throws Exception
	{
		String nomExperimento = "xxxOi";
		Long idCurrentThread = Thread.currentThread().getId();
		Connection conn = GerenteBD.getGerenteBD().obtemConexao(idCurrentThread +"");
		//System.out.println("ESQUEMA_RESULT: "+esquemaResult+"\tESQUEMA_AMOSTRA: "+esquemaAmostra);
		conn.setAutoCommit(false);
		float[] probPorClasse = {1F,0.1F,0.1F,0.1F};
		for(int i =2 ; i<3000 ; i++)
		{
			System.out.println("Cria resultado");
			ResultadoItem rItem = new ResultadoItem(i,2,3,probPorClasse);
			System.out.println("Manda pra grava");
			rItem.gravaResultadoBanco(nomExperimento,true);
			
			if((i+1)%1000 == 0)
			{
				System.out.println("Execbatch");
				ResultadoItem.executeBatchResult();
				ResultadoItem.executeBatchProbResult();
			}
			
		}
		System.out.println("Fim execbatch final");
		ResultadoItem.executeBatchResult();
		ResultadoItem.executeBatchProbResult();
		
		conn.commit();
		//System.out.println("Entropia: "+rItem.calculaEntropia());
	}
	@Override
	public int compareTo(ResultadoItem o) {
		
		// TODO Auto-generated method stub
		return (int) ((this.classePrevista - o.getClassePrevista())*10000);
	}

	public static PreparedStatement getStmtInsereResult()
	{
		Long idCurrentThread = Thread.currentThread().getId();
		
		PreparedStatement stmtInsereResult = stmtInsereResultPool.get(idCurrentThread);
		
		if(stmtInsereResult == null)
		{
			try {
				inicializaBD();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			stmtInsereResult = stmtInsereResultPool.get(idCurrentThread);
		}
		
		return stmtInsereResult;
	}
	public PreparedStatement getStmtAtualizaConfiancaResult()
	{
		Long idCurrentThread = Thread.currentThread().getId();
		
		PreparedStatement stmtAtualizaConfiancaResult = stmtAtualizaConfiancaResultPool.get(idCurrentThread);
		
		return stmtAtualizaConfiancaResult;
	}
	public static PreparedStatement getStmtInsereprobResult()
	{
		Long idCurrentThread = Thread.currentThread().getId();
		
		PreparedStatement stmtInsereprobResult = stmtInsereprobResultPool.get(idCurrentThread);
		
		return stmtInsereprobResult;
	}
	public PreparedStatement getStmtDeleteProbResult()
	{
		Long idCurrentThread = Thread.currentThread().getId();
		verifyBDInitialization(idCurrentThread);
		PreparedStatement stmtDeleteProbResult = stmtDeleteProbResultPool.get(idCurrentThread);
		
		
		return stmtDeleteProbResult;
	}
	private void verifyBDInitialization(Long idCurrentThread) {
		if(stmtDeleteProbResultPool.get(idCurrentThread) == null)
		{
			try {
				inicializaBD();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
	}
	
	public static boolean existResultado(String nomExperimento) throws NotImplemented
	{
		Long idCurrentThread = Thread.currentThread().getId();
		//stmtExisteResultadoPool.put(idCurrentThread, conn.prepareStatement("select 1 from wiki_results.resultado_regressao where nomExperimento = ? limit 1"));
		throw new NotImplemented();
		
		
		
	}

}
