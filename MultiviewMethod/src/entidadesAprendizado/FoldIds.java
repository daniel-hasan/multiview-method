package entidadesAprendizado;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import aprendizadoUtils.GenericoLetorLike;
import aprendizadoUtils.MetodoAprendizado;

import stuctUtil.ArrayUtil;
import stuctUtil.Tripla;

public class FoldIds implements Serializable 
{
	

	private Long[] arrTreino = new Long[0];
	private Long[] arrValidacao = new Long[0];
	private Long[] arrTeste = new Long[0];
	private FoldIds[] subFolds = new FoldIds[0];
	private int level = 0;
	
	
	public FoldIds(List<Long> arrTreino,List<Long> arrTeste, List<Long> arrValidacao)
	{
		this(arrTreino.toArray(new Long[arrTreino.size()]),arrTeste.toArray(new Long[arrTeste.size()]),arrValidacao.toArray(new Long[arrValidacao.size()]));
	}
	public FoldIds(Long[] arrTreino,Long[] arrTeste, Long[] arrValidacao)
	{
		this.arrTreino = arrTreino;
		this.arrValidacao = arrValidacao;
		this.arrTeste = arrTeste;
	}
	public void setLevel(int level)
	{
		this.level = level;
	}
	public int getLevel()
	{
		return this.level;
	}
	public void setSubFolds(FoldIds[] fs)
	{
		
		this.subFolds = fs;
		for(FoldIds fids : this.subFolds)
		{
			fids.setLevel(this.level+1);
		}
	}
	public FoldIds[] getSubFolds()
	{
		return this.subFolds;
	}
	public void setArrTreino(Long[] arrTreino) {
		this.arrTreino = arrTreino;
	}
	public void setArrValidacao(Long[] arrValidacao) {
		this.arrValidacao = arrValidacao;
	}
	public void setArrTeste(Long[] arrTeste) {
		this.arrTeste = arrTeste;
	}
	
	/**
	 * particiona o arrTreino de tal forma que cada particao tenha tamParticao
	 * @param tamParticao
	 * @return
	 * @throws IOException 
	 */
	public  static FoldIds[] criarFoldIdsByInstance(File arqFonte,Long[] arrInstancias,MetodoAprendizado metAp,int tamParticao,boolean usarValidacao,long rndSeed) throws IOException
	{
		
		int numFolds = (int)Math.floor(arrInstancias.length/(double)tamParticao);
		
		
		return criarFoldIdsByInstanceAndNumFold(arqFonte,arrInstancias,metAp, numFolds,usarValidacao,rndSeed);
	}
	
	public static FoldIds[] criarFoldIdsByInstanceAndNumFold(File arqFonte,Long[] arrInstancias,MetodoAprendizado metAp, 	int numFolds,boolean usarValidacao,long rndSeed) throws IOException {
		
		
		FoldIds[] arrFoldIds = new FoldIds[ numFolds];
		
		File treino =File.createTempFile("tmpTreino","f");
		File treinoIds =File.createTempFile("tmpTreinoIds","f");
		treinoIds.deleteOnExit();
		treino.deleteOnExit();
		
		//coloca a orgem como o arqFolte
		File oldSource = metAp.getArquivoOrigem();
		metAp.setArquivoOrigem(arqFonte);
		
		
		metAp.filtraArquivoPorIds(new ArrayUtil<Long>().toList(arrInstancias), treino);
		
		
		
		List<Long>[] lstFolds = metAp.divideFileIntoFolds(treino, 
									numFolds,
									"id", 
									metAp instanceof GenericoLetorLike?"qid":"id",  
									false,
									rndSeed,
									false);
		
		
		for(int f =0 ; f<numFolds ; f++)
		{
			arrFoldIds[f] = FoldIds.getFoldIds(f, lstFolds, usarValidacao).getZ();
		}
		
		
		
		metAp.setArquivoOrigem(oldSource);
		return arrFoldIds;
	}
	
	public List<Long> getLstTreino()
	{
		ArrayUtil<Long> arrUtil = new ArrayUtil<Long>();
		return arrUtil.toList(this.arrTreino);
	}
	public List<Long> getLstTeste()
	{
		ArrayUtil<Long> arrUtil = new ArrayUtil<Long>();
		return arrUtil.toList(this.arrTeste);		
	}
	public List<Long> getLstValidacao()
	{
		ArrayUtil<Long> arrUtil = new ArrayUtil<Long>();
		return arrUtil.toList(this.arrValidacao);
	}
	
	public Long[] getArrTreino()
	{
		return this.arrTreino;
	}
	public List<Long> getLstTreinoWithValidacao()
	{
		ArrayUtil<Long> arrUtil = new ArrayUtil<Long>();
		return arrUtil.toList(this.getArrTreinoWithValidacao());
	}
	public Long[] getArrTreinoWithValidacao()
	{
		Long[] arrTreinoValidacao =  new Long[this.arrTreino.length+this.arrValidacao.length];
		
		int idxTreinoValidacao = 0;
		for(int i = 0; i < this.arrTreino.length ; i++)
		{
			arrTreinoValidacao[idxTreinoValidacao] = this.arrTreino[i];
			
			idxTreinoValidacao++;
		}
		for(int i =0 ; i < this.arrValidacao.length ; i++)
		{
			arrTreinoValidacao[idxTreinoValidacao] = this.arrValidacao[i];
			
			idxTreinoValidacao++;
		}
		
		return arrTreinoValidacao;
	}
	public Long[] getArrTeste()
	{
		return this.arrTeste;		
	}
	public Long[] getArrValidacao()
	{
		return this.arrValidacao;
	}
	public static FoldIds extractFoldIds(MetodoAprendizado metAp,Fold f) throws IOException
	{
		List<Long> idsTreino = metAp.getIds(f.getTreino());
		List<Long> idsTeste = metAp.getIds(f.getTeste());
		List<Long> idsValidacao  =  new ArrayList<Long>();
		
		
		if(f.getValidation() != null)
		{
			idsValidacao = metAp.getIds(f.getValidation());
		}
		
		return new FoldIds(idsTreino, idsTeste, idsValidacao);
	}
	private static void getIdsFromFile(File f, List<Long> idsValidacao)
			throws IOException {
		for(Integer id: Fold.getIdsFromIdFile(f))
		{
			idsValidacao.add(id.longValue());
		}
	}
	public static Tripla<Integer,Integer,FoldIds> getFoldIds(int foldNum,List[] idsPerFolds,boolean gerarValidacao)
	{
		//ids do treino, teste e validação 
		List<Long> idsTreino = new ArrayList<Long>();
		List<Long> idsTeste = new ArrayList<Long>();
		List<Long> idsValidacao = new ArrayList<Long>();
		int idxValidacao = -1,idxTeste = -1;
		boolean first = true;
		for(int fj = 0 ; fj<idsPerFolds.length ; fj++)
		{
			//para fj = f, ids de teste
			if(fj == foldNum)
			{
				idsTeste.addAll(idsPerFolds[fj]);
				idxTeste = fj;
			}else
			{
				//o primeiro e  da validacao o resto do treino
				if(first && gerarValidacao)
				{
					idxValidacao = fj;
					idsValidacao.addAll(idsPerFolds[fj]);
					first = false;
				}else
				{
					idsTreino.addAll(idsPerFolds[fj]);
				}
			}
				
		}
		
		return new Tripla<Integer,Integer,FoldIds>(idxValidacao, idxTeste, new FoldIds(idsTreino, idsTeste, idsValidacao));
	}
	
	public Fold getFold(int num,MetodoAprendizado metAp,File arqToCreateFold,boolean temporary,String prefixFold) throws IOException
	{
		
		ArrayUtil<Long> arrUtil = new ArrayUtil<Long>();
		File arqOrigemOld = metAp.getArquivoOrigem();
		metAp.setArquivoOrigem(arqToCreateFold);
		
		Fold f = metAp.criaFoldComIdsFiltrado(num,arrUtil.toList(arrTreino),null,arrUtil.toList(arrValidacao),arrUtil.toList(arrTeste),new ArrayList<ResultadoItemViews>(),new ArrayList<String>());
		
		metAp.setArquivoOrigem(arqOrigemOld);
		
		
		if(temporary)
		{
			f.getTreino().deleteOnExit();
			f.getIdsFile().deleteOnExit();
			f.getTeste().deleteOnExit();
			f.getIdsTreinoFile().deleteOnExit();
			if(f.getValidation() != null)
			{
				f.getValidation().deleteOnExit();
				f.getIdsValidation().deleteOnExit();
			}
			if(prefixFold.length() > 0)
			{
				f.setTreino(changeSufixFile(f.getTreino(),prefixFold));
				f.setIdsTreinoFile(changeSufixFile(f.getIdsTreinoFile(),prefixFold));
				
				f.setIdsFile(changeSufixFile(f.getIdsFile(),prefixFold));
				f.setTeste(changeSufixFile(f.getTeste(),prefixFold));
				if(f.getValidation()!=null)
				{
					f.setValidationFiles(changeSufixFile(f.getValidation(),prefixFold),
										changeSufixFile(f.getIdsValidation(),prefixFold));
				}
				
			}
		}
		return f;
		
				
	}
	public File changeSufixFile(File arq,String prefixFold)
	
	{
		File flRenamed = new File(arq.getParentFile(),prefixFold+"_"+arq.getName());
		arq.renameTo(flRenamed);
		return flRenamed;
		
	}
	
	public void printArrString(StringBuilder str,Long[] arr,int limit)
	{
		
		
		if(arr.length < limit)
		{
			str.append('[');
			for(int i =0 ; i<arr.length ; i++)
			{
				str.append(arr[i]);
				str.append(',');
				str.append(' ');
			}
			str.append(']');
			str.append('\t');
		}			
		str.append("size: ");
		str.append(arr.length);
		
		
		
	}
	public void printAddLevelIdent(StringBuilder str)
	{
		for(int i =0; i<= this.getLevel() ; i++)
		{
			str.append("\t");
		}
	}
	public String toString()
	{
		StringBuilder strB = new StringBuilder();
		
		printAddLevelIdent(strB);
		strB.append("arrTreino: ");
		printArrString(strB, this.arrTreino, 20);
		strB.append("\n");
		printAddLevelIdent(strB);
		strB.append("arrValidacao: ");
		printArrString(strB, this.arrValidacao, 20);
		strB.append("\n");
		printAddLevelIdent(strB);
		strB.append("arrTeste: ");
		printArrString(strB, this.arrTeste, 20);
		strB.append("\n");
		
		
		if(this.subFolds.length > 0)
		{
			printAddLevelIdent(strB);
			strB.append("SUBFOLDS: ");
			strB.append("\n");
			for(int i =0 ; i < this.subFolds.length ; i++)
			{
				printAddLevelIdent(strB);
				strB.append("### FOLD ");
				strB.append(i);
				strB.append(" ###");
				strB.append(this.subFolds[i].toString());
				strB.append("\n");
				strB.append("\n");

			}
		}
		return strB.toString();
	}
}
