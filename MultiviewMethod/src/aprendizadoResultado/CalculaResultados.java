package aprendizadoResultado;

import io.Sys;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import matematica.Estatistica;
import matematica.FuncMath;
import stuctUtil.ListaAssociativa;
import stuctUtil.Tupla;
import aprendizadoResultado.ValorResultado.MetricaUsada;
import aprendizadoUtils.MetodoAprendizado;
import aprendizadoUtils.MetricUtils;
import aprendizadoUtils.SVM;
import arquivo.ArquivoUtil;
import banco.GerenteBD;
import config_tmp.Colecao;
import entidadesAprendizado.Confianca;
import entidadesAprendizado.Confianca.Z;
import entidadesAprendizado.Fold;
import entidadesAprendizado.MatrizConfusao;
import entidadesAprendizado.MatrizConfusaoCalculos;
import entidadesAprendizado.Resultado;
import entidadesAprendizado.ResultadoItem;
import entidadesAprendizado.ResultadoItemVoto;
import featSelector.ValorResultadoMultiplo;

public class CalculaResultados {
	
	private static boolean NDCG_EXP = true;
	public static double getMSE(Fold[] folds) throws SQLException
	{ 
		double media = 0;
		for(int i = 0; i<folds.length ; i++)
		{
			media += getMSEMedio(folds[i]);
		}
		return media/folds.length;
	}
	
	public static double getMSEMedio(Fold f) throws SQLException
	{ 
		 
		 return getMSE(f.getResultadosValues());
	}
	public static double getMSE(Collection<ResultadoItem> lstResultados) throws SQLException
	{
		return getMSE(lstResultados,null);
	}
	public static double getMSE(Collection<ResultadoItem> lstResultados,ResultadoAnalyser resultAn) throws SQLException
	{
		Iterator<ResultadoItem> j = lstResultados.iterator();

		double sum = 0;
		while (j.hasNext())
		{
			ResultadoItem r = j.next();
			sum += Math.pow(r.getErro(), 2);
			if(resultAn!= null)
			{
				resultAn.analyseResult(r, Math.pow(r.getErro(), 2));
			}
		}
		return sum/(float)lstResultados.size();
	}
	public static double getPorcErrNearClass(Collection<ResultadoItem> lstResultados,ResultadoAnalyser resultAn,double maxError) throws SQLException
	{
		Iterator<ResultadoItem> j = lstResultados.iterator();

		double numElementsNear = 0;
		
		while (j.hasNext())
		{
			ResultadoItem r = j.next();
			
			if(r.getErro() <= maxError)
			{
				numElementsNear++;
			}
			
		}
		return numElementsNear/(float)lstResultados.size();
	}
	public static double getAcurracia(Collection<ResultadoItem> lstResultados) throws SQLException
	{
		return getAcurracia( lstResultados,null);
	}
	public static double getAcurracia(Collection<ResultadoItem> lstResultados,ResultadoAnalyser resultAn) throws SQLException
	{
		Iterator<ResultadoItem> j = lstResultados.iterator();
		int numErros = 0;
		while (j.hasNext())
		{
			ResultadoItem r = j.next();
			
			if(r.getClasseReal() != r.getClassePrevista())
			{
				if(resultAn!=null)
				{
					resultAn.analyseResult(r, 0);
				}
				numErros ++;
			}else
			{
				if(resultAn!=null)
				{
					resultAn.analyseResult(r, 1);
				}
			}
			
		}
		
		return  (lstResultados.size()-numErros)/(double) lstResultados.size();
	}
	public static Tupla<Double,List<Double>> getPerQueryMetric(List<ResultadoItem> lstResultados,Integer k,MetricaUsada metric,double minResultado) throws SQLException
	{
		return getPerQueryMetric( lstResultados, k, metric,minResultado,null);
	}
	public static Tupla<Double,List<Double>> getPerQueryMetric(List<ResultadoItem> lstResultados,Integer k,MetricaUsada metric,double minResultado,ResultadoAnalyser resultAn) throws SQLException
	{
		double maxScore = -Double.MIN_VALUE;
		
		for(ResultadoItem ri : lstResultados)
		{
			double classeReal = ri.getClasseReal();
			if(maxScore<classeReal)
			{
				maxScore = classeReal;
			}
			
		}
		if(Math.abs(minResultado) < 0.01)
		{
			minResultado = 0;
		}
		//System.out.println("Classe real minima: "+minResultado);
		
		
		ListaAssociativa<String, ResultadoItem> lstResultadoPorQID = ResultadoItem.agrupaResultPerQID(lstResultados);
		
		
		
		List<Double> arrValues = new ArrayList<Double>();
		int pos = 0;
		//para cada pergutna... 
		for(String key : lstResultadoPorQID.keySet())
		{
			List<ResultadoItem> lstRi = lstResultadoPorQID.getList(key);
			Double[] lstResults = getDoublePredictionArray(lstRi, k,minResultado);
			//analisa o resultado por item
			if(resultAn != null)
			{
				for(int i = 0 ; i<lstResults.length ; i++)
				{
					resultAn.analyseResult(lstRi.get(i), lstRi.get(i).getClassePrevista());
				}
			}
				
			//computa a metrica por grupo
			switch(metric)
			{
				case NDCG:
				case NDCG_EXP:					
					//System.out.println(new ArrayUtil<Double>().toString(lstResults));
					//agrupo por qid
					List<ResultadoItem> lstResultadosItem = lstResultadoPorQID.getList(key);
					
					//gera o array de classes
					Collections.sort(lstResultadosItem, new Comparator<ResultadoItem>(){

						@Override
						public int compare(ResultadoItem o1, ResultadoItem o2)
						{
							return (int) Math.round((o2.getClasseReal()*10000.0)-(o1.getClasseReal()*10000.0));
						}
						});
						
					Double[] arrClasses = new Double[lstResultadosItem.size()>k?k:lstResultadosItem.size()];

					for(int i = 0 ; i< arrClasses.length ; i++)
					{
						arrClasses[i] = lstResultadosItem.get(i).getClasseReal()-minResultado;
						//System.out.print(arrClasses[i]+";");
					}
					
					
					//resgata ndcg
					double valNDCG = MetricUtils.ndcg(lstResults,arrClasses,NDCG_EXP);
					
					if(Double.isInfinite(valNDCG))
					{
						System.out.println("EH INFINITO!");
					}
					
					if(Double.isNaN(valNDCG))
					{
						//arrValues[pos] = 1;
						System.out.println("Um resultado deu NAN!");
					}else
					{
						arrValues.add(valNDCG);
					}
					//adiciona valor do grupo para ser analisado
					if(resultAn!=null)
					{
						resultAn.analyseResultGroup(k, Integer.parseInt(key), valNDCG,"NDCG",lstResults);		
					}
					break;
				case ERR:
					

					double valERR = MetricUtils.err(lstResults,maxScore);
					if(Double.isNaN(valERR) || maxScore == 0)
					{
						//arrValues[pos] = 1;
						System.out.println("Um resultado deu NAN!");
					}else
					{
						arrValues.add(valERR);
					}
					
					//adiciona valor do grupo para ser analisado
					if(resultAn!=null)
					{
						resultAn.analyseResultGroup(k, Integer.parseInt(key), valERR,"ERR",lstResults);
					}
					break;
					
			}
				
			pos++;
		}
		
		return new Tupla<Double,List<Double>>(Estatistica.media(arrValues),arrValues);
		
	}


	private static Double[] getDoublePredictionArray(List<ResultadoItem> lstResultados, Integer k,double minResultado)
	{
		List<ResultadoItem> newLstResultados = new ArrayList<ResultadoItem>(lstResultados);
		
		
		
		//somatorio dos késimos dcgs
		Collections.sort(newLstResultados, new Comparator<ResultadoItem>(){

			@Override
			public int compare(ResultadoItem o1, ResultadoItem o2)
			{
				
				//caso possua probabilidade usa-la cas ocontrario, compara com a classe prevista
				/*
				if(o1.getProbPorClasseMap().keySet().size()>0)
				{
					float probHighClass = 0;
					float classePrevista = 
				}else
				{
					// TODO Auto-generated method stub
					return ((int)(o2.getClassePrevista()*100)-(int)(o1.getClassePrevista()*100));
				}
				*/
				return o1.compareForRank(o2);
			}
			
		});
		double sumRank = 0;
		Double[] lstResults = new Double[k<newLstResultados.size()?k:newLstResultados.size()];
		for(int i= 0 ; i<lstResults.length;i++)
		{
			lstResults[i] = newLstResultados.get(i).getClasseReal()-minResultado;
		}
		return lstResults;
	}
	public static ValorResultado getResultado(List<ResultadoItem> lstResultados,MetricaUsada metrica,Integer k,double minResultado) throws SQLException
	{
		return getResultado( lstResultados, metrica, k, minResultado,null);
	}
	public static ValorResultado getResultado(List<ResultadoItem> lstResultados,MetricaUsada metrica,Integer k,double minResultado,ResultadoAnalyser resultAnalyser) throws SQLException
	{
		Double resultado = null;
		List<Double> lstSubResults = new ArrayList<Double>();
		switch(metrica)
		{
			case NDCG:
			case ERR:
			case NDCG_EXP:				
				Tupla<Double,List<Double>> tpResult = getPerQueryMetric(lstResultados,k,metrica,minResultado,resultAnalyser);
				resultado = tpResult.getX();
				lstSubResults = tpResult.getY();
				//System.out.println("Resultado: "+resultado);
				break;
			case ACURACIA:
				resultado = getAcurracia(lstResultados,resultAnalyser);
				break;
			case MSE: 
				resultado = getMSE(lstResultados,resultAnalyser);
				break;
			case PORC_ERR_NEAR_CLASS:
				resultado = getPorcErrNearClass(lstResultados,resultAnalyser,0.5);
				break;
		}
		
		
		return new ValorResultado(resultado.floatValue(),metrica,k,lstSubResults);
	}
	public static Confianca getMseConfiancaArquivo(Fold[] f,String nomExperimento,File dirResult) throws IOException, SQLException
	{
		
		File diretorio = new File(dirResult.getAbsolutePath()+"/"+nomExperimento);
		if(!diretorio.exists())
		{
			diretorio.mkdirs();
		}
		int maxValue = 6;
		if(f.length>0 && f[0].getResultadosValues().size()>0)
		{
			maxValue =f[0].getResultadosValues().get(0).getProbPorClasse().length; 
		}
		if(maxValue <= 1)
		{
			maxValue = 6;
		}
		BufferedWriter[] out = new BufferedWriter[maxValue]; 
		
		for(int i=0 ; i<out.length ; i++)
		{
			out[i] =new BufferedWriter(new FileWriter(new File(diretorio.getAbsoluteFile()+"/"+nomExperimento+"."+i+".resultErro"))); 
		}

		BufferedWriter outGeral = new BufferedWriter(new FileWriter(new File(diretorio.getAbsoluteFile()+"/"+nomExperimento+".geral.resultErro")));
		
		double erro = 0;
		int numResultados = 0;
		
		for(int i = 0 ; i<f.length ; i++)
		{
			erro += f[i].getSomaErroQuadratico();
			numResultados += f[i].getNumResults();

		}
		double media = erro/(double) numResultados;
		
		double sumDesvio = 0;
		int n = 0;
		for(int i = 0 ; i<f.length ; i++)
		{
			Iterator<ResultadoItem> j = f[i].getResultadosValues().iterator();
			while(j.hasNext())
			{
				/* Iterador que representa cada instancia. */
				ResultadoItem r = j.next();
				
				sumDesvio += Math.pow(Math.pow(r.getErro(),2) - media,2);

				n++;
				out[(int) Math.round(r.getClasseReal())].write(r.getErro()+"\n");
				outGeral.write(r.getErro()+"\n");
			}
		}
		
		for(int i=0; i<out.length ; i++)
		{
			out[i].close();
		}
		outGeral.close();
		
		double desvio = Math.sqrt(sumDesvio/(n-1));
		return new Confianca(media,desvio,n);
		
	}
	public static Confianca getMseConfianca(Fold[] f) throws IOException, SQLException
	{
			
		double erro = 0;
		int numResultados = 0; 
		
		for(int i = 0 ; i<f.length ; i++)
		{
			erro += f[i].getSomaErroQuadratico();
			numResultados += f[i].getNumResults();

		}
		double media = erro/(double) numResultados;
		
		double sumDesvio = 0;
		int n = 0;
		for(int i = 0 ; i<f.length ; i++)
		{
			Iterator<ResultadoItem> j = f[i].getResultadosValues().iterator();
			while(j.hasNext())
			{
				/* Iterador que representa cada instancia. */
				ResultadoItem r = j.next();
				
				sumDesvio += Math.pow(Math.pow(r.getErro(),2) - media,2);

				n++;
			}
		}
		
		
		double desvio = Math.sqrt(sumDesvio/(n-1));
		return new Confianca(media,desvio,n);
		
	}
	public static String resultadoClassificacaoToString(Resultado r,Integer numClasses,File arq) throws IOException
	{
		
			
		
		
		
		HashSet<Integer> classesNum = new HashSet<Integer>();
		Fold[] arrFolds = r.getFolds();
		MatrizConfusao mcGeral = null;
		
		//resgata num de classes se necessario 
		for(int i = 0; i<arrFolds.length ; i++)
		{
			Iterator<ResultadoItem> j = arrFolds[i].getResultadosValues().iterator();
			
			while(j.hasNext())
			{
				ResultadoItem ri = j.next();
				classesNum.add((int)Math.round((ri.getClasseReal())));
			}
		}
		mcGeral = new MatrizConfusao(classesNum.size());
		numClasses = classesNum.size();
/*		if(numClasses == null)
		{

		}else
		{
			mcGeral = new MatrizConfusao(numClasses);
		}*/
		
		
		 
		
		
		
		StringBuilder strResult = new StringBuilder();
		double numTotalAcuracia = 0.0;
		double numMacroF1 = 0.0;
		
		for(int i = 0; i<arrFolds.length ; i++)
		{
			//strResult.append("================Fold #"+i+"=================");
			//System.out.println("Numero de classes:"+numClasses);
			MatrizConfusao mc = new MatrizConfusaoCalculos(numClasses);
			Iterator<ResultadoItem> j = arrFolds[i].getResultadosValues().iterator();
			
			while(j.hasNext())
			{
				ResultadoItem ri = j.next();
				mc.novaPredicao((int)ri.getClassePrevista(), (int)ri.getClasseReal());
				mcGeral.novaPredicao((int)ri.getClassePrevista(), (int)ri.getClasseReal());
			}
			numTotalAcuracia += mc.getAcuracia();
			numMacroF1 += mc.getMacroF1();
			
			//strResult.append("\n"+mc.toString()+"\n\n");
		}
		
		strResult.append("Confusion Matrix\n"+mcGeral.toString());
		strResult.append("\n\nMacro F1: "+numMacroF1/arrFolds.length);
		strResult.append("\nAcuracy: "+numTotalAcuracia/arrFolds.length);
		if(arq != null)
		{
			if(!arq.getParentFile().exists())
			{
				arq.getParentFile().mkdirs();
			}
			ArquivoUtil.gravaTexto(strResult.toString(), arq, false);
			
			System.out.println("Result written:"+arq.toString());
		}else
		{
			System.err.println("Arquivo result não gravado!");
		}
		return strResult.toString();
	}
	public static String resultadoRegressaoToString(Resultado r,File arqResult) throws Exception
	{
		//Fold[] folds = r.getFolds();
		
		//imprime cabecalho
		String result =  "";
		
		//resultado matriz confusao (se existir)
		/*
		MatrizConfusao m = r.getMatrizConfusao();
		if(m!=null)
		{
			result += "\n\n***Matriz Confusão**\n"+m.toString();
		}
		*/
		//result por fold geral
		//result = "\n\n\n================================================================";
		//result += "\nExperimento: "+r.getNomExperimento()+"\n";
		result += getResultPorFold(r.getFolds(),arqResult,r.getNomExperimento());
		
		//result por fold de votacao (se necessario
		if(r.getFolds()[0].getResultadosValues().size()>0 && r.getFolds()[0].getResultadosValues().get(0) instanceof ResultadoItemVoto)
		{
			result += "\n\n*****************************Resultado da votação***************************";
			Fold[] foldsResultado = r.getFolds();
			Fold[] foldTodosConcordantes = new Fold[r.getFolds().length];
			int totalViews = r.getViews().length;
			//adiciona todos os concordantes
			for(int i = 0 ; i<foldTodosConcordantes.length ; i++)
			{
				foldTodosConcordantes[i] = new Fold(foldsResultado[i].getNum(),foldsResultado[i].getNomeBase(),new ArrayList<ResultadoItem>());
				
				Iterator<ResultadoItem> j = foldsResultado[i].getResultadosValues().iterator();
				while(j.hasNext())
				{
					ResultadoItem rVoto = j.next();
					if(rVoto instanceof ResultadoItemVoto)
					{
						ResultadoItemVoto rVotoIntanciado = (ResultadoItemVoto)rVoto;
						if(rVotoIntanciado.getNumConcordantes() == totalViews)
						{
							foldTodosConcordantes[i].adicionaResultado(new ResultadoItem(rVoto.getId(),(float)rVoto.getClasseReal(),(float)rVotoIntanciado.getMediaResultado(),0F));
						}						
					}

				}
				
			}
			result += getResultPorFold(foldTodosConcordantes,arqResult,r.getNomExperimento());
		}
		
		if(arqResult!=null)
		{
			ArquivoUtil.gravaTexto(result, arqResult, true);
		}
		return result;
	}
	public static String getResultPorFold(Fold[] folds, File arqResult,String nomExperimento)throws IOException, SQLException
	{
		
		String result = "";
		
		/*result += "\n\n*****FOLDS MSE e Tempo de execução****\n";
		//imprime resultado por folds
		for(int i = 0; i<folds.length ; i++)
		{
			result += "#"+i+"\t";
		}
		result += "\n";
		*/
		//imprime totalizacoes
		long sumTempo = 0;
		int totalInstancias = 0;
		for(int i = 0 ; i<folds.length ; i++)
		{
			result += Double.toString(Math.round(getMSEMedio(folds[i])*10000)/10000.0)+"\t";
			sumTempo += folds[i].getTempoExecucaoTeste();
			totalInstancias += folds[i].getResultadosValues().size();
		}
		result += "\n";
		
		

		//calcula desvio padrao do tempo
		double mediaTempo = sumTempo/folds.length;
		long sumDesvioTempo = 0;
		for(int i = 0 ; i<folds.length ; i++)
		{
			result += Double.toString(folds[i].getTempoExecucaoTeste()/1000.0)+"s\t";
			sumDesvioTempo += Math.pow(folds[i].getTempoExecucaoTeste() - mediaTempo,2);
		}
		double desvioTempo = Math.sqrt(sumDesvioTempo/(folds.length));
		Confianca tempoConfianca = new Confianca(mediaTempo,desvioTempo,folds.length);
		result += "\n";
		
		//confianca
		if(arqResult != null)
		{
			/*
			Confianca mseConfianca = getMseConfiancaArquivo(folds,nomExperimento,arqResult.getParentFile()!=null?arqResult.getParentFile():new File(""));
			double[] intConfiancaMSE = mseConfianca.calculaIntervalo(Confianca.Z.C_090);
			double[] intConfiancaTempo = tempoConfianca.calculaIntervalo(Confianca.T.T_090);
			result += "\nMédia MSE:"+mseConfianca.getMedia()+" Int. Confianca MSE: ["+intConfiancaMSE[0]+","+intConfiancaMSE[1]+"]";
			result += "\nMédia Tempo:"+tempoConfianca.getMedia()+"ms Int. Confianca Tempo: ["+intConfiancaTempo[0]+","+intConfiancaTempo[1]+"]";
			*/
		}

		result += "\nMSE: "+getMSE(folds);
		result += "\n# of itens: "+totalInstancias;
		
		return result;
	}
	public static void imprimeTabelaPorAgrupamento(HashSet<String> valoresColuna,HashSet<String> valoresLinha,HashMap<String,Resultado> result,File arqOut) throws IOException, SQLException
	{
		String strResult = "  ";
		for(String nomColuna : valoresColuna)
		{
				strResult += "\t"+nomColuna;
		}
		
		strResult += "\n";
		for(String nomLinha : valoresLinha)
		{
			strResult += nomLinha+"\t";
			for(String nomColuna : valoresColuna)
			{
				for(String nomExperimento : result.keySet())
				{
					if(nomExperimento.contains(nomLinha) && nomExperimento.contains(nomColuna))
					{
						Resultado r = result.get(nomExperimento);
						Fold[] f = r.getFolds();
						
						double erro = 0;
						int numResultados = 0;
						for(int i = 0 ; i<f.length ; i++)
						{
							erro += f[i].getSomaErroQuadratico();
							numResultados += f[i].getNumResults();

						}
						double media = erro/(double) numResultados;
						strResult += media;
					}
				}
				strResult += "\t";
			}
			strResult += "\n";
		}
		System.out.println(strResult);
		//ArquivoUtil.gravaTexto(strResult, arqOut, false);
	}
	
	public static void imprimeTabelaResultadMSEExcel(HashMap<Colecao,HashMap<String,Resultado>> resultado,boolean imprimeDelta,File arqOut,int numCasas,boolean calculaMediaPorFold) throws IOException, SQLException
	{ 
		String delimitador = "\t"; 
		String fimLinha = "\n";
		imprimeTabelaResultadoMSE(resultado, delimitador, fimLinha, imprimeDelta, arqOut,numCasas,calculaMediaPorFold);
	}
	public static void imprimeTabelaResultadMSELatex(HashMap<Colecao,HashMap<String,Resultado>> resultado,boolean imprimeDelta,File arqOut,int numCasas,boolean calculaMediaPorFold) throws IOException, SQLException
	{ 
		String delimitador = "\t&\t"; 
		String fimLinha = "\t\\\\\n";
		imprimeTabelaResultadoMSE(resultado, delimitador, fimLinha, imprimeDelta, arqOut,numCasas,calculaMediaPorFold);
	}
	public static void imprimeTabelaResultadoMSEPorFold(HashMap<Colecao,HashMap<String,Resultado>> resultado,String delimitador,String fimLinha,File arqOut,int numCasasDecimais,int numFolds) throws IOException, SQLException
	{
		boolean imprimeTitulo = true;
		StringBuilder strTabela = new StringBuilder();
		Iterator<Colecao> colecoesIt = resultado.keySet().iterator();
		while(colecoesIt.hasNext())
		{
			Colecao colecao = colecoesIt.next();
			HashMap<String,Resultado> resultColecao = resultado.get(colecao);
			
			Iterator<String> resultsIt = resultColecao.keySet().iterator();
			
			/* Imprime cabeçalho*/
			if(imprimeTitulo)
			{
				strTabela = strTabela.append("Experimento");
				for(int i =0; i<numFolds ; i++)
				{
					strTabela = strTabela.append(i);
					strTabela = strTabela.append(delimitador);	
				}
				strTabela = strTabela.append(fimLinha);	
				imprimeTitulo = false;
			}
			
			/* Imprime resultados por experimento */
			//strTabela = strTabela.append(colecao.getSigla());
			while(resultsIt.hasNext())
			{
				String nomExperimento = resultsIt.next();
				Resultado  resultColecaoExp = resultColecao.get(nomExperimento);
				
				//imprime expermento/colecao
				strTabela = strTabela.append(colecao.getSigla()+"-"+nomExperimento);
				
				//delimitador
				strTabela = strTabela.append(delimitador);
				
				//imprime folds
				Fold[] folds = resultColecaoExp.getFolds();
				double sumMSE = 0;
				
				for(int i = 0 ; i<folds.length ; i++)
				{
					double mse = folds[i].getSomaErroQuadratico()/(double)folds[i].getNumResults(); 
					sumMSE += mse;
					
					//cada MSE do fold
					strTabela = strTabela.append(FuncMath.cortaCasasDecimais(mse, numCasasDecimais));
					strTabela = strTabela.append(delimitador);
					
				}
				double media = sumMSE/(double) folds.length;
					
				strTabela = strTabela.append(FuncMath.cortaCasasDecimais(media, numCasasDecimais));

				strTabela = strTabela.append(fimLinha);	

				
			}

			
		}
		
		ArquivoUtil.gravaTexto(strTabela.toString(), arqOut, false);
	}
	public static void imprimeTabelaResultadoMSE(HashMap<Colecao,HashMap<String,Resultado>> resultado,String delimitador,String fimLinha,boolean imprimeDelta,File arqOut,int numCasasDecimais,boolean calculaMediaPorFold) throws IOException, SQLException
	{
		boolean imprimeTitulo = true;
		StringBuilder strTabela = new StringBuilder();
		Iterator<Colecao> colecoesIt = resultado.keySet().iterator();
		while(colecoesIt.hasNext())
		{
			Colecao colecao = colecoesIt.next();
			HashMap<String,Resultado> resultColecao = resultado.get(colecao);
			
			Iterator<String> resultsIt = resultColecao.keySet().iterator();
			
			/* Imprime titulo das colunas*/
			if(imprimeTitulo)
			{
				strTabela = strTabela.append("Coleção");
				while(resultsIt.hasNext())
				{
					strTabela = strTabela.append(delimitador);
					String coluna = resultsIt.next();
					strTabela = strTabela.append(coluna);
				}
				strTabela = strTabela.append(fimLinha);
				imprimeTitulo = false;
				resultsIt = resultColecao.keySet().iterator();
			}
			
			/* Imprime resultados por colecao */
			//colecao 
			strTabela = strTabela.append(colecao.getSigla());
			while(resultsIt.hasNext())
			{
				String coluna = resultsIt.next();
				Resultado  resultColecaoExp = resultColecao.get(coluna);
				Confianca c = getMseConfianca(resultColecaoExp.getFolds());
				
				//delimitador
				strTabela = strTabela.append(delimitador);
				
				//media
				if(calculaMediaPorFold)
				{
					Fold[] folds = resultColecaoExp.getFolds();
					double erro = 0;
					int numResultados = 0;
					for(int i = 0 ; i<folds.length ; i++)
					{
						erro += folds[i].getSomaErroQuadratico();
						numResultados += folds[i].getNumResults();

					}
					double media = erro/(double) numResultados;
					
					strTabela = strTabela.append(FuncMath.cortaCasasDecimais(media, numCasasDecimais));
				}else
				{
					strTabela = strTabela.append(FuncMath.cortaCasasDecimais(c.getMedia(), numCasasDecimais));
					//delta da IC
					if(imprimeDelta)
					{
						strTabela = strTabela.append(" $\\pm$"+FuncMath.cortaCasasDecimais(c.getDelta(Z.C_090),numCasasDecimais));
					}
				}
				

				
			}
			
			//fim da linha
			strTabela = strTabela.append(fimLinha);
			
		}
		
		ArquivoUtil.gravaTexto(strTabela.toString(), arqOut, false);
	}
	public enum TIPO_RESULTADO{
		ACURACIA,ERRO,ORDENACAO;
	}
	
	public static Fold[] getResultadoItemBanco(String nomExperimento) throws Exception
	{
		SVM svm =new  SVM();
		
		ResultadoAnalyser r = null/*new ResultadoAnalyserRank(nomExperimento)*/;
		svm.setNomExperimento(nomExperimento);
		svm.setGravarNoBanco(true);
		return svm.getResultsPorFoldBanco();
	}
	public static void getResultadoBanco(MetodoAprendizado met,String nomExperimentoClasseReal,TIPO_RESULTADO tpoResult,File diretorio,ResultadoAnalyser resultAnalyser) throws Exception
	{
		Fold[] arrFolds = met.getResultsPorFoldBanco();
		String nomExpAnt = met.getNomExperimento();
		met.setNomExperimento(nomExperimentoClasseReal);
		Fold[] arrFoldsClasseReal = met.getResultsPorFoldBanco();
		met.setNomExperimento(nomExpAnt);
		
		getResults(met, nomExperimentoClasseReal, tpoResult, diretorio,arrFolds,arrFoldsClasseReal,resultAnalyser);
	}

	public static void getResults(MetodoAprendizado met,String nomExperimentoClasseReal, TIPO_RESULTADO tpoResult,File diretorio, Fold[] arrFolds,Fold[] arrFoldsClasseReal,ResultadoAnalyser resultAnalyser) throws Exception, IOException
	{
		File dirExperimento = new File(diretorio,met.getNomExperimento());
		//System.out.println("Dir experimento: "+dirExperimento.getAbsolutePath());
		if(!dirExperimento.exists())
		{
			dirExperimento.mkdirs();
		}
		
		//met.parseResult(arrFolds[4]);
		double minClasse = Double.MAX_VALUE;
		double maxClasse = Double.MIN_VALUE;
		
		int resultEncontrados = 0;
		Map<Long,Double> mapClasseRealPorId = new HashMap<Long,Double>();
		for(int i = 0 ; i<arrFolds.length ; i++)
		{
			Fold fold = arrFolds[i];
			Fold fClasseReal  = arrFoldsClasseReal[i];
			
			for(ResultadoItem r : fClasseReal.getResultadosValues())
			{
				mapClasseRealPorId.put(r.getId(), r.getClasseReal());
			}



		}
		
		for(int i = 0 ; i<arrFolds.length ; i++)
		{
			Fold fold = arrFolds[i];
			for(ResultadoItem r : fold.getResultadosValues())
			{
				if(mapClasseRealPorId.containsKey(r.getId()))
				{
					float classNew = mapClasseRealPorId.get(r.getId()).floatValue();
					r.setClasseReal(classNew);
					if(minClasse>classNew)
					{
						minClasse = classNew;
					}
					if(maxClasse < classNew)
					{
						maxClasse = classNew;
					}
					resultEncontrados++;
					
				}else
				{
					fold.removeResultado(r.getId());
				}
			}
		}
		
		System.out.println("Classe menor: "+minClasse+" maxClasse:"+maxClasse+" Resultados: "+resultEncontrados);
		MetricaUsada[] metricas = new MetricaUsada[0];
		String geralClassificacao = "";
		Integer[] ks = new Integer[0];
		File tmp = File.createTempFile("result", "xx");
		tmp.deleteOnExit();
		
		
		switch(tpoResult)
		{
			case ACURACIA:
				metricas = new MetricaUsada[1];
				metricas[0] = MetricaUsada.ACURACIA;
				geralClassificacao = resultadoClassificacaoToString(new Resultado(met.getNomExperimento(),arrFolds),(int) (maxClasse+1),tmp);
				break;
			case ERRO:
				metricas = new MetricaUsada[1];
				metricas[0] = MetricaUsada.MSE;
				break;
			case ORDENACAO:
				metricas = new MetricaUsada[10];
				ks = new Integer[10];
				for(int i =0 ; i <10 ; i++)
				{
					metricas[i] = MetricaUsada.NDCG;
					ks[i] = i+1;
				}
				
				break;
		}

		
		
		//calcula metricas
		String strGeral = "";
		String strPorFold = "";
		String avgPorK = "";
		for(int i =0 ; i<metricas.length ; i++)
		{
			ValorResultado vGeral  = met.getResultadoGeral(arrFolds,metricas[i],i<ks.length?ks[i]:null,minClasse);
			
			ValorResultadoMultiplo vr = met.getResultadoPorIteracao(arrFolds,metricas[i],i<ks.length?ks[i]:null,minClasse,resultAnalyser);
			
			//gera resultado geral
			strGeral += vGeral.toString()+"\n";
			
			//gera resultado por fold
			if(ks.length>0)
			{
				strPorFold += "\n\n============= k = "+ks[i]+"=======================\n";
				strPorFold += vr.toString();
				avgPorK += ks[i]+"\t"+vr.getAvgResults()+"\n";
			}else
			{
				strPorFold += "\n\n====================================\n";
				strPorFold += vr.toString();
			}
			
			
			
			//vGeral.gravaSubResult(new File(dirExperimento,metricas[i].toString()+"_"+ks[i]+"_PorQuery_"+met.getNomExperimento()));
			
			
		}
		File result = new File(dirExperimento,"resultado_"+met.getNomExperimento());
		ArquivoUtil.gravaTexto(strGeral+"\n\n======Por fold =====\n"+strPorFold+geralClassificacao, result, false);
		ArquivoUtil.gravaTexto(avgPorK, new File(dirExperimento,"avg_porK_"+met.getNomExperimento()), false);
		System.out.println("Resultado gravado em: "+result.getAbsolutePath());
	}
	public static ListaAssociativa<Integer,Float> getListResult(File arq) throws IOException
	{
		ListaAssociativa<Integer,Float> lstResult = new ListaAssociativa<Integer, Float>();
		
		
		String[] arrLinhasResult = ArquivoUtil.leTexto(arq).split("\n");
		Pattern kPatt = Pattern.compile(".*k[ ]*=[ ]*([0-9]*).*");
		Pattern ndcgPatt = Pattern.compile("\\[0\\]: ndcg@[0-9]+:[ ]*([.e0-9]+).*");
		
		int k = 0;
		for(String linha : arrLinhasResult)
		{
			Matcher objK = kPatt.matcher(linha);
			Matcher objNdcg = ndcgPatt.matcher(linha);
			if(objK.matches())
			{
				k = Integer.parseInt(objK.group(1));
			}
			
			if(objNdcg.matches())
			{
				lstResult.put(k, Float.parseFloat(objNdcg.group(1)));
			}
		}
		
		return lstResult;
		
		
	}
	/**
	 * um diretorio, resgatar seus resultados como se o diretorio fosse o nome do experimento e o dir pai a colecao
	 * @throws IOException 
	 */
	public static ListaAssociativa<Integer,Float> getListResultPerDir(File diretorio) throws IOException
	{
		//String colecao = diretorio.getParentFile().getName();
		
		String nomExp = diretorio.getName();
		File arqResult = new File(new File(diretorio,nomExp),"resultado_"+nomExp);
		
		if(arqResult.exists())
		{
			//return getListResult(new File("/data/experimentos/qa_multiview/resultados/"+colecao+"/"+nomExp+"/"+nomExp+"/resultado_"+nomExp));
			return getListResult(arqResult);
		}else
		{
			return null;
		}
	}
	/**
	 * Dado um diretorio de uma colecao com seus resultados, retorna todas as listas de experimentos possiveis
	 * @throws IOException 
	 */
	public static Map<String,ListaAssociativa<Integer,Float>> getExperimentosFromColecaoDir(File dir) throws IOException
	{
		Map<String,ListaAssociativa<Integer,Float>> mapListMap = new HashMap<String,ListaAssociativa<Integer,Float>>();
		for(File arq : dir.listFiles())
		{
			if(arq.isDirectory())
			{
				ListaAssociativa<Integer,Float> lstResults = getListResultPerDir(arq);
				if(lstResults != null)
				{
					mapListMap.put(arq.getName(), lstResults);
				}
			}
		}
		
		
		return mapListMap;
	}
	/**
	 * Faz grid de ndcg dados os resultados em Map<String,ListaAssociativa<Integer,Float>> para um determinado k
	 * retornando uma matriz em string
	 * @param dir
	 */
	public static String fazGridNdcg(Map<String,ListaAssociativa<Integer,Float>> lstResults, int ndcgK)
	{
		List<String> lstVals = new ArrayList<String>();
		StringBuffer strText = new StringBuffer();
		//colunas sao os experimentos
		int numFolds = 0;
		List<String> lstExps = new ArrayList<String>(lstResults.keySet());
		for(int i =0 ; i<lstExps.size() ; i++)
		{
			String exp = lstExps.get(i);
			numFolds = lstResults.get(exp).getList(ndcgK).size();
			strText.append(exp);
			if(i+1<lstExps.size())
			{
				strText.append("\t");
			}
		}

		//linha sao os valores
		for(int foldId =0 ; foldId < numFolds ; foldId++ )
		{
			strText.append("\n");
			for(int i =0 ; i<lstExps.size() ; i++)
			{
				String exp = lstExps.get(i);
				ListaAssociativa<Integer,Float> resultExp = lstResults.get(exp);
				strText.append(resultExp.getList(ndcgK).get(foldId));
				if(i+1<lstExps.size())
				{
					strText.append("\t");	
				}
			}
		}
		return strText.toString();
	}
	
	public static void calculaPValueAllColecaoDir(File dir) throws IOException
	{
		Map<String,ListaAssociativa<Integer,Float>> mapExperimentos = getExperimentosFromColecaoDir(dir);
		
		for(int ndcgK = 1 ; ndcgK <= 10 ; ndcgK++)
		{
			System.out.println("NDCG@"+ndcgK);
			//faz o grid 
			String gridNDCG = fazGridNdcg(mapExperimentos,  ndcgK);
			
			//System.out.println(gridNDCG);
			
			//grava num arq tmp
			File objFile = File.createTempFile("ndcgVal", ".txt");
			objFile.deleteOnExit();
			ArquivoUtil.gravaTexto(gridNDCG, objFile, false);
			
			//roda o pvalue (imprime na tela o resultado
			pValueOnAll(objFile.getPath());
			
			System.out.println();
		}
	}
	
	public static List<String> getExperimentos(String strNomExps) throws SQLException, ClassNotFoundException
	{
		String sql = "select distinct nomExperimento from wiki_results.resultado_regressao where nomExperimento like '"+strNomExps+"'";
		ResultSet rst = GerenteBD.getGerenteBD().obtemConexao("").createStatement().executeQuery(sql);
		System.out.println(sql);
		List<String> lstExps = new ArrayList<String>();
		while(rst.next())
		{
			lstExps.add(rst.getString(1));
		}
		rst.close();
		return lstExps;
		
	}
	public static void gerarExcelExperimentosDir(Map<String,ListaAssociativa<Integer,Float>> mapRespsPorExp,File arqOut) throws IOException
	{
		 
		 
		BufferedWriter out = new BufferedWriter(new FileWriter(arqOut, false),100);
		
		
		 //coloca a primeira coluna dos labels
		out.write(" \t NDCG@K \n");
		out.write("Experimentos");
		
		//imprime os "k"s
		List<Integer> lstKsNDCG = new ArrayList<Integer>();
		if( mapRespsPorExp.values().size() > 0)
		{
			ListaAssociativa<Integer,Float> firstVal = mapRespsPorExp.values().iterator().next();
			for(int k : firstVal.keySet())
			{
				out.write("\t"+k);
				lstKsNDCG.add(k);
			}
		}
		
		//imprime os valores por experimento
		//out.write(texto);
		for(String nomExperimento : mapRespsPorExp.keySet())
		{
			out.write("\n");
			//imprime nome do experimento
			out.write(nomExperimento+"\t");
			
			//imprime resultados por experimento
			ListaAssociativa<Integer,Float> lstResultPerNdcg = mapRespsPorExp.get(nomExperimento);
			for(int k : lstKsNDCG)
			{
				if(lstResultPerNdcg.containsKey(k))
				{
					float avg = 0;
					List<Float> lstResultPorFold = lstResultPerNdcg.getList(k, true);
					for(float resultPorFold : lstResultPorFold)
					{
						avg+=resultPorFold;
					}
					out.write(Float.toString(avg/(float)lstResultPorFold.size())+"\t");
				}else
				{
					out.write("--\t");	
				}
				
			}
			
			
		}
		out.write("\n");
		out.close(); 
	}
	public static void calculaExperimentos(String amostra,String resultDirName,String nomExperimentos) throws Exception
	{
		File dir = gerarDirResultadosPorExperimento(amostra, resultDirName,			nomExperimentos,TIPO_RESULTADO.ORDENACAO);
				
		
			
		System.out.println("resgatando resultados....");
		
		gerarExcelFromDir(dir);
		
	}

	private static void gerarExcelFromDir(File dir) throws IOException {
		Map<String,ListaAssociativa<Integer,Float>> mapRespsPorExp = getExperimentosFromColecaoDir(dir);
		
		System.out.println("Gravando resultado em xls....");
		gerarExcelExperimentosDir(mapRespsPorExp,new File(dir,"resultGeral.xls"));
	}

	private static File gerarDirResultadosPorExperimento(String amostra,String resultDirName, String nomExperimentos,TIPO_RESULTADO tpoResult) throws SQLException,
			ClassNotFoundException, Exception {
		
		
		//resgata nome dos experimentos
		List<String> lstExperimentos = getExperimentos(nomExperimentos);
		SVM svm =new  SVM();
		
		//cria diretorio se nao existir
				File dir = new File("/data/experimentos/qa_multiview/resultados/"+resultDirName+"/"+amostra);
				if(!dir.exists())
				{
					dir.mkdirs();
				}
				
		//gera diretorios de resultado para cada um
		for(int i =0 ; i<lstExperimentos.size() ; i++)
		{
			String nomExperimento = lstExperimentos.get(i);
			System.out.println("Gravando no diretorio experimento "+nomExperimento+" ("+i+"/"+lstExperimentos.size()+")");
			File dirResult = new File(dir,nomExperimento);
			if(!dirResult.exists())
			{
				String nomExperimentoClasseReal = nomExperimento;
				ResultadoAnalyser r = null/*new ResultadoAnalyserRank(nomExperimento)*/;
				svm.setNomExperimento(nomExperimento);
				svm.setGravarNoBanco(true);
				
				
				getResultadoBanco(svm,nomExperimentoClasseReal,tpoResult,dirResult,r);
			}
		}
		return dir;
	}
	
	public static Map<String, Map<String,Double>> agrupaResultado(List<Tupla<String,Double>> lstTplNomExpResult,String regExp)
	{
		//linha, coluna ->valor
		Map<String, Map<String,Double>> lstAllResults = new HashMap<String, Map<String,Double>>();
		
		//navega em cada resultado e armazena ela apropriadamente em lstAllResults
		Pattern expPatt = Pattern.compile(regExp);
		for(Tupla<String,Double> tplNomExpResult : lstTplNomExpResult)
		{
			String nomExperimento = tplNomExpResult.getX();
			Double valResult = tplNomExpResult.getY();
			
			
			
			Matcher m = expPatt.matcher(nomExperimento);
			String nomColExperimento = "";
			String nomLineExperimento = nomExperimento;
			if (m.matches()) {
				int qtdRemoved = 0;
				for(int groupNum = 1 ; groupNum<=m.groupCount() ; groupNum++)
				{
					
					//pega o inicio e o fim de cada grupo como coluna atual
					int idxIniGroup = m.start(groupNum);
					int idxEndGroup = m.end(groupNum);
					String strGroup = nomExperimento.substring(idxIniGroup,idxEndGroup);
					
					//incrementa coluna
					if(nomColExperimento.length()>0)
					{
						nomColExperimento += "_";	
					}
					nomColExperimento += nomColExperimento;
					
					//altera linha removendo a parte desta coluna
					nomLineExperimento = nomLineExperimento.substring(0,idxIniGroup-qtdRemoved)+nomLineExperimento.substring(idxEndGroup-qtdRemoved);
					
					
					//armazena qtde removida do nomLineExperimento
					qtdRemoved += strGroup.length();
					
					
				}
			  
			}else
			{
				System.err.println("Reg exp: "+regExp+" nao deu matcj em :"+nomExperimento);
			}
			//adiciona linha
			if(!lstAllResults.containsKey(nomLineExperimento))
			{
				lstAllResults.put(nomLineExperimento, new HashMap<String,Double>());
			}
			Map<String,Double> mapResultLinha = lstAllResults.get(nomLineExperimento);
			
			//adiciona coluna/valor na linha
			mapResultLinha.put(nomColExperimento, valResult);
			
		}
		
		return lstAllResults;
		
	}
	/*
	public static gerarResults(String[] arrExps,File dir,String colecao)
	{
		String[] argsNew = {"qa_multiview_"+colecao+"_multiview__RankLib_combinacao_RandomForest_"+colecao+".amostra_TamIgualTreino","/data/experimentos/qa_multiview/resultados/"+colecao+"/viewChange/"};
		SVM svm =new  SVM();
		
		
	}
	*/
	public static void main(String[] args) throws Exception
	{
		//System.out.println("args: "+args[0]);
		//gerarExcelFromDir(new File("/data/experimentos/qa_multiview/resultados/viewChange"));
		//System.exit(0);
		//String[] arrMetComb = {"MaxEnt_noGaussian","SVM_WITH_EASY","SVM",};
		/*
		String[] arrcol = {"cook","stack","english"};
		for(String metComb: arrcol)
		{
			gerarDirResultadosPorExperimento("forsquare_dataset1_"+metComb, "resultGeral",	"fq_d1r%"+metComb+"_combinacao%",TIPO_RESULTADO.ACURACIA);
			gerarDirResultadosPorExperimento("forsquare_dataset2_"+metComb, "resultGeral",	"fq_d2r%"+metComb+"_combinacao%",TIPO_RESULTADO.ACURACIA);
		}
		*
		/*
		for(String metComb: arrMetComb)
		{
			gerarDirResultadosPorExperimento("forsquare_dataset1_"+metComb, "resultGeral",	"fq_d1r%"+metComb+"_combinacao%",TIPO_RESULTADO.ACURACIA);
			gerarDirResultadosPorExperimento("forsquare_dataset2_"+metComb, "resultGeral",	"fq_d2r%"+metComb+"_combinacao%",TIPO_RESULTADO.ACURACIA);
		}
		*/
		
		//calculaExperimentos("stack","rev02Completo","qa\\_multiview\\_rev02combView\\_%combinacao%stack%");
		//calculaExperimentos("stack","rev02Completo","qa\\_multiview\\_rev02combView\\_%combinacao%stack%");
		//calculaExperimentos("english","rev02Completo","qa\\_multiview\\_rev02combView\\_%combinacao%english%");
		//calculaExperimentos("cook","rev02Completo","qa\\_multiview\\_rev02combView\\_%combinacao%cook%");
		//System.exit(0);
		//gerarDirResultadosPorExperimento("forsquare_dataset2", "resultGeral",	"fq_d2r%combinacao%",TIPO_RESULTADO.ACURACIA);
		//calculaExperimentos("cook","teste_sem_bug","qa_multiview_tst%");
		//calculaExperimentos("cook","rev02Teste","qa_multiview_rev02combView_%");
		//gerarDirResultadosPorExperimento(String amostra,String resultDirName, String nomExperimentos,TIPO_RESULTADO tpoResult)
		//calculaExperimentos("cook","rev02Completo","qa_multiview_rev02combView_%_cook.amostra_TamIgualTreino");
		
		//calculaExperimentos("english","perViewResult","qa\\_multiview\\_RankLib%\\_RandomForest\\_english\\_%.amostra\\_TamIgualTreino");
		//calculaExperimentos("cook","perViewResult","qa\\_multiview\\_RankLib%\\_RandomForest\\_cook\\_%.amostra\\_TamIgualTreino");
		/*
		gerarDirResultadosPorExperimento("stack",   "resultViews",  "qa\\_multiview\\_RankLib%\\_RandomForest\\_stack\\_%.amostra\\_TamIgualTreino",TIPO_RESULTADO.ORDENACAO);
		gerarDirResultadosPorExperimento("cook",    "resultViews",	"qa\\_multiview\\_RankLib%\\_RandomForest\\_cook\\_%.amostra\\_TamIgualTreino",TIPO_RESULTADO.ORDENACAO);
		gerarDirResultadosPorExperimento("english", "resultViews",	"qa\\_multiview\\_RankLib%\\_RandomForest\\_english\\_%.amostra\\_TamIgualTreino",TIPO_RESULTADO.ORDENACAO);
		*/
		//gerarDirResultadosPorExperimento("stack",   "resultViews",  "qa\\_multiview\\_RankLib%\\_RandomForest\\_stack\\_%.amostra\\_TamIgualTreino",TIPO_RESULTADO.ORDENACAO);
		//System.exit(0);
		/*
		String amostra = "cook";
		//calculaExperimentos(amostra,"viewAnalysis","qa_multiview_combView%"+amostra+".amostra_TamIgualTreino");
		calculaExperimentos(amostra,"perViewResult","qa_multiview_RankLib_RandomForest_"+amostra+"_%.amostra_TamIgualTreino");
		
		
*/
		
		//System.exit(0);
		//pValuesNdcg("/data/experimentos/sigir_2013/resultados/ndcg_exp/resultado_anFeat/allExcept/ndcg_");
		//pValuesNdcg("/home/hasan/data/experimentos/qa_multiview/resultados/cook/ndcg_");

		
		/*
		calculaPValueAllColecaoDir(new File("/data/experimentos/qa_multiview/resultados/english"));
		System.exit(0);

		 //String[] argsNew = {"sigir2013_RankLib_RandomForests_main_wo_user_log_score.amostra","/data/experimentos/sigir_2013/resultados/"};


		
		String nomExp = "qa_multiview_"+colecao+"_allFeats__RankLibDinamicFeatures_combinacao_RandomForest_"+colecao+".amostra_TamIgualTreino";
		System.out.println( getListResult(new File("/data/experimentos/qa_multiview/resultados/"+colecao+"/"+nomExp+"/"+nomExp+"/resultado_"+nomExp)));
		System.exit(0);
		*/
		String colecao = "cook";
		//String feat = args[0];
		String feat = "length";
		
		//String[] argsNew = {"qa_multiview_"+colecao+"_multiview__RankLib_combinacao_RandomForest_"+colecao+".amostra_TamIgualTreino","/data/experimentos/qa_multiview/resultados/resultadoMultiviewRev04/"+colecao+"/"};
		//String[] argsNew = {"qa_multiview_rev04LowBag_RankLib_RandomForest_"+colecao+"_"+feat+".amostra_TamIgualTreino","/data/experimentos/qa_multiview/resultados/resultadoMultiviewRev04/"+colecao+"/"};
		//String[] argsNew = {"qa_multiview_rev05LowBag_RankLibDinamicFeatures_RandomForest_"+colecao+"_user_wo_vote.amostra_TamIgualTreino","/data/experimentos/qa_multiview/resultados/resultadoMultiviewRev05_lowBag/"+colecao+"/"};
		String[] argsNew = {"qa_multiview_featSelFinal3_cook_multiview__RankLib_combinacao_RandomForest_cook.amostra_TamIgualTreino","/data/experimentos/qa_multiview/resultados/resultadoMultiviewFeatSel_lowBag/"+colecao+"/"};
		
		
		args = argsNew;
		SVM svm =new  SVM();
		String resultado =args[1];
		
		String[] arrNomExperimento = { 
				
				/*********** USO DO SELETOR ************/
				
				//"wiki_multiview_with_seletor_allFeats_starVote_multiview__SVM_combinacao__starVote.amostra_TamIgualTreino",
				//"wiki_multiview_with_seletor_starVote_multiview__SVM_combinacao__starVote.amostra_TamIgualTreino",
				//"qa_multiview_with_seletorRev2_stack_multiview__RankLib_combinacao_RandomForest_stack.amostra_TamIgualTreino",
				//"qa_multiview_allFeats_with_seletorRev2_"+colecao+"_multiview__RankLibDinamicFeatures_combinacao_RandomForest_"+colecao+".amostra_TamIgualTreino",
				//"qa_multiview_with_seletorRev2_"+colecao+"_multiview__RankLib_combinacao_RandomForest_"+colecao+".amostra_TamIgualTreino",
				//"qa_multiview_allFeats_with_seletor_"+colecao+"_multiview__RankLibDinamicFeatures_combinacao_RandomForest_"+colecao+".amostra_TamIgualTreino",
				//"qa_multiview_with_seletor_"+colecao+"_multiview__RankLib_combinacao_RandomForest_"+colecao+".amostra_TamIgualTreino",
				
				/*  MUDANCA DAS VIEWS
				"qa_multiview_style_read_"+colecao+"_multiview_style_read__RankLib_combinacao_RandomForest_"+colecao+".amostra_TamIgualTreino",
				"qa_multiview_allUserInSingleView_"+colecao+"_multiview_allUser__RankLib_combinacao_RandomForest_"+colecao+".amostra_TamIgualTreino",
				"qa_multiview_sixViews_"+colecao+"_multiview_sixView__RankLib_combinacao_RandomForest_"+colecao+".amostra_TamIgualTreino"
				*/
				"qa_multiview_featSelFinal3_cook_multiview__RankLib_combinacao_RandomForest_cook.amostra_TamIgualTreino",
				//"qa_multiview_cook_multiview_usergraph_style__RankLib_combinacao_RandomForest_cook.amostra_TamIgualTreino",
				//Multiview:
				//"qa_multiview_RankLib_combinacao_RandomForest_stack.amostra_TamIgualTreino",
				//"qa_multiview_"+colecao+"_multiview__RankLib_combinacao_RandomForest_"+colecao+".amostra_TamIgualTreino",
				//"combinacao_1a13ba4f-812a-476f-874e-f061ae43bf84",
				//"qa_multiview_english_multiview__RankLib_combinacao_RandomForest_english.amostra_TamIgualTreino",
				
				//Multiview + allFeats 
				//"qa_multiview_"+colecao+"_allFeats__RankLibDinamicFeatures_combinacao_RandomForest_"+colecao+".amostra_TamIgualTreino",
				
				//multiview
				//"qa_multiview_rev05LowBag_"+colecao+"_multiview__RankLib_combinacao_RandomForest_cook.amostra_TamIgualTreino"
				//"qa_multiview_rev05LowBag_cook_multiview_low_5__RankLib_combinacao_RandomForest_cook.amostra_TamIgualTreino",
				
				//"qa_multiview_rev05LowBagFim_cook_multiview_low_5__RankLib_combinacao_RandomForest_cook.amostra_TamIgualTreino",
				
				
				//"qa_multiview_baseline_rev03_RankLibDinamicFeatures_RandomForest_cook.amostra_TamIgualTreino",
				
				//Visoes
				//"qa_multiview_rev04_RankLib_RandomForest_"+colecao+"_relevance.amostra_TamIgualTreino",
				//"qa_multiview_rev04LowBag_RankLib_RandomForest_"+colecao+"_relevance.amostra_TamIgualTreino",
				//"qa_multiview_rev05LowBag_final2_cook_multiview_low_5__RankLib_combinacao_RandomForest_cook.amostra_TamIgualTreino",
				//"qa_multiview_featSelFinal_cook_multiview__RankLib_combinacao_RandomForest_cook.amostra_TamIgualTreino",
				//"qa_multiview_rev05LowBag_RankLibDinamicFeatures_RandomForest_"+colecao+"_user_wo_vote.amostra_TamIgualTreino",
				//"qa_multiview_rev05LowBag_RankLib_RandomForest_"+colecao+"_"+feat+".amostra_TamIgualTreino",
				//"qa_multiview_rev04LowBag_RankLib_RandomForest_"+colecao+"_"+args[0]+".amostra_TamIgualTreino",
				//"qa_multiview_rev04LowBag_RankLibDinamicFeatures_RandomForest_"+colecao+"_user_wo_vote.amostra_TamIgualTreino",
				
				//Baselines do multiview:
				//"qa_multiview_baseline_RankLibDinamicFeatures_RandomForest_cook.amostra_TamIgualTreino",
				
				//"qa_multiview_baseline_RankLibDinamicFeatures_RandomForest_stack.amostra_TamIgualTreino",
				//"qa_multiview_baseline_RankLibDinamicFeatures_RandomForest_"+colecao+".amostra_TamIgualTreino",
				
				//"qa_multiview_baseline_RankLibDinamicFeatures_RandomForest_cook.amostra_TamIgualTreino",
				//"qa_multiview_allFeats_allFeats_with_seletorRev2_stack_multiview__RankLibDinamicFeatures_combinacao_RandomForest_stack.amostra_TamIgualTreino",
				
				
				//"qa_multiview_baseline_RankLibDinamicFeatures_RandomForest_"+colecao+".amostra_TamIgualTreino",
				//"qa_multiview_baseline_RankLibDinamicFeatures_RandomForest_cook.amostra_TamIgualTreino",
				//"qa_multiview_cook_multiview__RankLib_combinacao_RandomForest_cook.amostra_TamIgualTreino",
				//"qa_multiview_cook_text__RankLib_combinacao_RandomForest_cook.amostra_TamIgualTreino",
				//"qa_multiview_cook_multiview_wo_vote__RankLib_combinacao_RandomForest_cook.amostra_TamIgualTreino",
				//"qa_multiview_baseline_wo_vote_cook_multiview_wo_vote_baseline__RankLib_combinacao_RandomForest_cook.amostra_TamIgualTreino",
				//"qa_multiview_baseline_wo_vote_RankLib_RandomForest_cook.amostra_TamIgualTreino",
				//"qa_multiview_RankLib_combinacao_RandomForest_stack.amostra_TamIgualTreino",
				//"qa_multiview_baseline_RankLibDinamicFeatures_RandomForest_stack.amostra_TamIgualTreino"
				//"qa_multiview_cook_text__RankLib_combinacao_RandomForest_cook.amostra_TamIgualTreino",
				//"qa_multiview_textBaseline_RankLib_RandomForest_cook_text.amostra_TamIgualTreino",
				//"qa_multiview_baseline_RankLibDinamicFeatures_RandomForest_cook.amostra_TamIgualTreino"
				//"qa_multiview_allFeats_cook_allFeats__RankLibDinamicFeatures_combinacao_RandomForest_cook.amostra_TamIgualTreino",
				//"qa_multiview_allFeats_stack_allFeats__RankLibDinamicFeatures_combinacao_RandomForest_stack.amostra_TamIgualTreino",
				//"qa_multiview_rankPosAllFeats_cook_multiviewRankPosAllFeats__RankLibDinamicFeatures_combinacao_RandomForest_cook.amostra_TamIgualTreino",
				//"qa_multiview_rankPos_cook_multiviewRankPos__RankLib_combinacao_RandomForest_cook.amostra_TamIgualTreino",
				
				
				
				//Parametro primeiro nivel
				/*
				"qa_mview_RankLibDinamicFeatures_f1_RandomForest_"+colecao+"_user_wo_vote.amostra",
				"qa_mview_RankLibDinamicFeatures_f2_RandomForest_"+colecao+"_user_wo_vote.amostra",
				"qa_mview_RankLibDinamicFeatures_f4_RandomForest_"+colecao+"_user_wo_vote.amostra",
				"qa_mview_RankLibDinamicFeatures_f6_RandomForest_"+colecao+"_user_wo_vote.amostra",
				"qa_mview_RankLibDinamicFeatures_f8_RandomForest_"+colecao+"_user_wo_vote.amostra",
				
				
				"qa_mview_RankLib_f1_RandomForest_"+colecao+"_history.amostra",
				"qa_mview_RankLib_f2_RandomForest_"+colecao+"_history.amostra",
				"qa_mview_RankLib_f4_RandomForest_"+colecao+"_history.amostra",
				"qa_mview_RankLib_f6_RandomForest_"+colecao+"_history.amostra",
				"qa_mview_RankLib_f8_RandomForest_"+colecao+"_history.amostra",
				
				
				"qa_mview_RankLib_f1_RandomForest_"+colecao+"_relevance.amostra",
				"qa_mview_RankLib_f2_RandomForest_"+colecao+"_history.amostra",
				"qa_mview_RankLib_f4_RandomForest_"+colecao+"_history.amostra",
				"qa_mview_RankLib_f6_RandomForest_"+colecao+"_history.amostra",
				"qa_mview_RankLib_f8_RandomForest_"+colecao+"_history.amostra",
				
				
				"qa_mview_RankLib_f1_RandomForest_"+colecao+"_structure.amostra",
				"qa_mview_RankLib_f2_RandomForest_"+colecao+"_structure.amostra",
				"qa_mview_RankLib_f4_RandomForest_"+colecao+"_structure.amostra",
				"qa_mview_RankLib_f6_RandomForest_"+colecao+"_structure.amostra",
				"qa_mview_RankLib_f8_RandomForest_"+colecao+"_structure.amostra",
				
				
				"qa_mview_RankLib_f1_RandomForest_"+colecao+"_style.amostra",
				"qa_mview_RankLib_f2_RandomForest_"+colecao+"_style.amostra",
				"qa_mview_RankLib_f4_RandomForest_"+colecao+"_style.amostra",
				"qa_mview_RankLib_f6_RandomForest_"+colecao+"_style.amostra",
				"qa_mview_RankLib_f8_RandomForest_"+colecao+"_style.amostra",
				
				
				
				"qa_mview_RankLib_f2_RandomForest_"+colecao+"_readbility.amostra",
				"qa_mview_RankLib_f4_RandomForest_"+colecao+"_readbility.amostra",
				"qa_mview_RankLib_f6_RandomForest_"+colecao+"_readbility.amostra",
				"qa_mview_RankLib_f8_RandomForest_"+colecao+"_readbility.amostra",
				
				"qa_mview_RankLib_f4_RandomForest_"+colecao+"_user_graph.amostra",
				"qa_mview_RankLib_f8_RandomForest_"+colecao+"_user_graph.amostra",
				*/
				
				
		};
		
		gerarResultadoDIr(argsNew, svm, resultado, arrNomExperimento);
	}

	private static void gerarResultadoDIr(String[] argsNew, SVM svm,
			String resultado, String[] arrNomExperimento) throws SQLException,
			ClassNotFoundException, Exception {
		if(new File(resultado).exists())
		{
			System.out.println("Arquivo existe");
		}else
		{
			System.out.println("Arquivo NAO existe");
		}
		
		
		//System.exit(0);
		for(String nomExperimento : arrNomExperimento)
		{
			String nomExperimentoClasseReal = nomExperimento;
			ResultadoAnalyser r = null/*new ResultadoAnalyserRank(nomExperimento)*/;
			svm.setNomExperimento(nomExperimento);
			svm.setGravarNoBanco(true);
			
			  
			//getResultadoBanco(svm,"sigir_2013_FeatStudy_RankLibDinamicFeatures_RandomForest_main_score.amostra",TIPO_RESULTADO.ORDENACAO,new File("/data/experimentos/sigir_2013/resultados/ndcg_exp/resultado_anFeat/"+nomExperimento),r);
			getResultadoBanco(svm,nomExperimentoClasseReal,TIPO_RESULTADO.ORDENACAO,new File(argsNew[1]+nomExperimento),r);
		}
	}

	private static void pValuesNdcg(String file) throws IOException
	{
		for(int i = 1 ; i <= 10 ;i++)
		 {
			 System.out.println("NDCG@"+i);
			 pValueOnAll(file+i);
			 System.out.println();
			 //System.exit(0);
		 }
	}  
	public static double calculateConcordance(Integer[] arrPredictedClasses,int numClasses)
	{
		double norm = 1/(numClasses*(numClasses-1));
		double sumProb = 0;
		for(int i =0 ; i<arrPredictedClasses.length ; i++)
		{
			sumProb += arrPredictedClasses[i]*(arrPredictedClasses[i]-1);
		}
		return sumProb/norm;
	}
	public static String pValueOnAll(String arquivoDados) throws IOException
	{
		String matrixToString = "";
		String primLinha = ArquivoUtil.leTexto(new File(arquivoDados)).split("\n")[0];
		String[] arrColunas =primLinha.split(" |\t");
		for(int i =0; i<arrColunas.length ;i++)
		{
			arrColunas[i] = arrColunas[i].replaceAll("\\-", ".");
			matrixToString += "\t"+arrColunas[i];
		}
		Double[][] arrMatrixPValue = new Double[arrColunas.length][arrColunas.length];
		for(int i = 0; i < arrColunas.length ; i++)
		{
			for(int j = 0; j < arrColunas.length ; j++)
			{
				String colunaI = arrColunas[i];
				String colunaJ = arrColunas[j];
				
				if(colunaI.length()>0 && colunaJ.length()>0 && !colunaI.equalsIgnoreCase(colunaJ))
				{
					
					String strScript = "v <- read.table(\""+new File(arquivoDados).getAbsolutePath()+"\", header = TRUE)\n" +
									/*"t.test(v$"+colunaI+",v$"+colunaJ+", paired=T)$p.value"*/
									"wilcox.test(v$"+colunaI+",v$"+colunaJ+", paired=T, alternative = \"greater\")$p.value";
					File script = File.createTempFile("scriptR", ".R");
					script.deleteOnExit();
					ArquivoUtil.gravaTexto(strScript, script, false);
					Sys.executarComando("chmod 700 "+script.getAbsolutePath(),true);
					String resp = Sys.executarComando("R --file="+script.getAbsolutePath(), false);
					//System.out.println(resp);
					String[] arrResult = resp.split("\n");
					arrMatrixPValue[i][j] = Double.parseDouble(arrResult[arrResult.length-2].replaceAll("\\[1\\]","").trim());
					//System.exit(0);
				}
			}
		}
		
		 
		for(int j = 0; j<arrMatrixPValue.length ; j++)
		{
			matrixToString += "\n"+arrColunas[j];
			for(int i = 0; i<arrMatrixPValue.length ; i++)
			{
				matrixToString += "\t"+arrMatrixPValue[j][i];
			}
		}
		System.out.println(matrixToString);
		return matrixToString;
		
	}
	
}

