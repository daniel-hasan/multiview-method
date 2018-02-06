package entidadesAprendizado;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import arquivo.ArquivoUtil;
import banco.GerenteBD;

public class ResultadoItemDelayedRecord implements Runnable 
{
	private static final int NUM_BATCH_INSERT_BD = 10000;
	
	private static ResultadoItemDelayedRecord gravador = null;
	private static Thread tGravador;
	private List<ResultadoItemToRecord> lstResultsToRecord = Collections.synchronizedList(new ArrayList<ResultadoItemToRecord>());
	
	private boolean finish = false;
	private boolean onlyBatch = true;
	private static PreparedStatement pstmtInsertMulti;
	
	private File errorFile = new File("/home/hasan/error_file_"+ new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime()) );
	
	private Set<String> setAlreadyDoneForExps = new HashSet<String>();
	
	
	public synchronized void addResult(ResultadoItemToRecord ri)
	{
		lstResultsToRecord.add(ri);
	}
	public synchronized ResultadoItemToRecord popResult()
	{
		
		return lstResultsToRecord.remove(0);
	}
	private ResultadoItemDelayedRecord() throws ClassNotFoundException, SQLException{
		inicializaBD();
	}
	
	

	public synchronized static ResultadoItemDelayedRecord getGravador()
	{
		if(gravador == null)
		{
			try {
				gravador = new ResultadoItemDelayedRecord();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			tGravador = new Thread(gravador);
			tGravador.start();
		}
		return gravador;
	}
	public Thread getThreadGravador()
	{
		return this.tGravador;
	}
	
	public void finish()
	{
		this.finish = true;
	}
	

	
	private synchronized void waitResultItem()
	{
		try {
			wait(2000L);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void gravaResultados()
	{
		
		while(!finish)
		{
			ResultadoItemToRecord ri = null;
				
					try {
						//gravaExperimento(nomExperimento, result);
						insertResults(true);
						
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						String stackTrace = "";
						for (StackTraceElement stackT : e.getStackTrace())
						{
							stackTrace += stackT.toString()+"\n";
						}
							String msg = ">>>> Ocorreu um erro ao tentar gravar resultado \n"+e.getMessage()+"\n"+stackTrace+"\n\n\n";
						try {
							ArquivoUtil.gravaTexto(msg, errorFile, true);
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						
					}	
					
				waitResultItem();

 
			
			
			
		}
		
		//grava ultimos elementos
		try {
			System.out.println(">BD: Gravando ultimos elementso.... ");
			insertResults(false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			String stackTrace = "";
			for (StackTraceElement stackT : e.getStackTrace())
			{
				stackTrace += stackT.toString()+"\n";
			}
			String msg = ">>>> Ocorreu um erro ao tentar gravar resultado :\n"+e.getMessage()+"\n"+stackTrace+"\n\n";
			try {
				ArquivoUtil.gravaTexto(msg, errorFile, true);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}

	}
	public static int getNumResults()
	{
		return ResultadoItem.getNumResultOnBatch();
	}
	private void gravaResultadoOnBatch(int numMinimo) throws SQLException {
		if(ResultadoItem.getNumResultOnBatch() >= numMinimo)
		{
			long time = System.currentTimeMillis();
			System.out.println(">BD: Gravando "+ResultadoItem.getNumResultOnBatch() +" resultados... ");
			ResultadoItem.executeBatchResult();
			System.out.println(">BD: "+ResultadoItem.getNumResultOnBatch()+" results gravados em: "+((System.currentTimeMillis()-time)/1000.0)+" seg");
		}else
		{
			if(ResultadoItem.getNumResultOnBatch() % 100 == 0)
			{
				System.out.println(">BD: Num itens pra gravar: "+ResultadoItem.getNumResultOnBatch());
			}
		}
		if(ResultadoItem.getNumProbResultOnBatch() >= numMinimo)
		{
			System.out.println(">BD: Gravando "+ResultadoItem.getNumProbResultOnBatch() +" PROB resultados... ");
			ResultadoItem.executeBatchProbResult();
		}
	}
	
	private void gravaExperimento(String nomExperimento, ResultadoItem result)
			throws SQLException, Exception {
		
		//se necessario, remove probResultDeste experimento
		if(!setAlreadyDoneForExps.contains(nomExperimento))
		{
			if(result.getProbPorClasse().length > 0 && result.getConfianca()>0)
			{
				result.excluirProbResult(nomExperimento);//ok (sem close ok)
			}
		}
		setAlreadyDoneForExps.add(nomExperimento);
		
		//manda gravar este resultado
		result.gravaResultadoBanco(nomExperimento, onlyBatch);
		
		
		gravaResultadoOnBatch(NUM_BATCH_INSERT_BD);
	}
	private void inicializaBD() throws ClassNotFoundException, SQLException
	{

		
		pstmtInsertMulti = createInsertStmt(NUM_BATCH_INSERT_BD);
	}
	private PreparedStatement createInsertStmt(int numInsertLines) throws ClassNotFoundException, SQLException
	{
		Connection conn = GerenteBD.getGerenteBD().obtemConexao("");
		
		String sql = createInsertStatementWithLines(numInsertLines);
		return conn.prepareStatement(sql);
	}
	public String createInsertStatementWithLines(int nLines)
	{
		String sqlInsert = "insert into wiki_results.resultado_regressao " +
				"(page_id," +
				"fold_id,"+
				"nomExperimento," +
				"result," +
				"probResult," +
				"classeReal," +
				"qid"+
				") " +
			"VALUES ";
		String strVals = 
				"(?," +
				"?,"+
				"?," +
				"?," +
				"?," +
				"?," +
				"?" +
				")";
		String strInsertMulti = sqlInsert;
		for(int i =0 ; i<nLines ; i++)
		{
			strInsertMulti +=strVals;
			if(i+1<nLines)
			{
				strInsertMulti += ",";
			}
		}
		return strInsertMulti;
	}
	private void insertResults(boolean onlyInLimit) throws Exception
	{
		if(lstResultsToRecord.size() >= NUM_BATCH_INSERT_BD && onlyInLimit)
		{
			insertResults(pstmtInsertMulti, NUM_BATCH_INSERT_BD);
			
			
		}else
		{
			if(!onlyInLimit)
			{
				//envia  de NUM_BATCH_INSERT_BD em NUM_BATCH_INSERT_BD ate ficar com o valor maximo
				while(lstResultsToRecord.size() >= NUM_BATCH_INSERT_BD)
				{
					insertResults(pstmtInsertMulti, NUM_BATCH_INSERT_BD);
				}
				//envia o restante...
				int numElements = lstResultsToRecord.size();
				PreparedStatement pStmt = createInsertStmt(numElements);
				insertResults(pStmt,numElements);
				pStmt.close();
			}
		}
	}
	private void insertResults(PreparedStatement stmtInsereResult,int limit) throws Exception
	{
		System.out.println(">BD: Gravando "+limit+" elementos.... ");
		int numResults = 0;
		long time= System.currentTimeMillis();
		while(numResults < limit)
		{
			ResultadoItemToRecord ri = popResult();
			insertResults(stmtInsereResult,ri.getNomExperimento(),ri.getRi(),numResults);
			//System.out.println(">BD: Inserido "+numResults+" elementos em ");
			numResults++;
			
			
			
		}
		if(numResults>0)
		{
			int num = stmtInsereResult.executeUpdate();
			System.out.println(">BD: Inserido "+num+" elementos em "+((System.currentTimeMillis()-time)/1000.0)+" segundos.... ");
		}
	}
	private void insertResults(PreparedStatement stmtInsereResult,String nomExperimento,ResultadoItem ri,int offset) throws Exception
	{
		offset = offset*7;
		stmtInsereResult.setLong(offset+1, ri.getId());
		if(ri.getFold()!=null)
		{
			Integer foldId = ri.getFold().getId();
			if(foldId != null)
			{
				stmtInsereResult.setInt(offset+2, foldId);	
			}else
			{
				stmtInsereResult.setInt(offset+2, 0);	
			}
		}else
		{
			stmtInsereResult.setInt(offset+2, 0);
		}
		
		stmtInsereResult.setString(offset+3, nomExperimento);
		stmtInsereResult.setDouble(offset+4, ri.getClassePrevista());
		stmtInsereResult.setDouble(offset+5, ri.getConfianca());
		stmtInsereResult.setDouble(offset+6, ri.getClasseReal());
		if(ri.getQID() != null)
		{
			stmtInsereResult.setFloat(offset+7, ri.getQID());
		}else
		{
			stmtInsereResult.setNull(offset+7, Types.FLOAT);
		}
	}
	
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		gravaResultados();
	}

}
