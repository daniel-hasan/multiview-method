/*
 * Created on 06/08/2008
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package scriptsUtil;

import io.Sys;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import string.StringUtil;
import stuctUtil.ListaAssociativa;
import stuctUtil.Tupla;
import aprendizadoUtils.GenericoLetorLike;
import aprendizadoUtils.GenericoSVMLike;
import aprendizadoUtils.MetodoAprendizado;
import arquivo.ArquivoUtil;

/**
 * @author Daniel Hasan Dalip
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DatasetUtil
{

	/**
	 * 
	 */
	public DatasetUtil()
	{
		super();
		// TODO Auto-generated constructor stub
	}
	public static void  agruparIds(String arquivoAmostra,String arquivoIds,String arquivoOut)throws IOException
	{
		 agruparIds(arquivoAmostra,arquivoIds,arquivoOut,false);
	}
	public static void  agruparIds(String arquivoAmostra,String arquivoIds,String arquivoOut,boolean possuiCabecalho)throws IOException
	{
		BufferedWriter arqOut = new BufferedWriter(new FileWriter(arquivoOut));
		BufferedReader arqId = new BufferedReader(new FileReader(arquivoIds));
		BufferedReader arqAmostra = new BufferedReader(new FileReader(arquivoAmostra));
		String linhaAmostra ="";
		String linhaId = "";
		//se possui cabecalho, ignora primeira linha da amostra
		if(possuiCabecalho)
		{
			arqAmostra.readLine();
		}
		while((linhaAmostra=arqAmostra.readLine())!=null)
		{
			linhaId = arqId.readLine();
			String novaLinha = linhaAmostra.substring(0,linhaAmostra.indexOf(" "))+//classe
								" "+linhaId+
								linhaAmostra.substring(linhaAmostra.indexOf(" "),linhaAmostra.length());//features
			arqOut.write(novaLinha+"\n");
		}
		arqOut.close();
		arqId.close();
		arqAmostra.close();
	}
	public static void scaleAllFilesWithPrefix(File dir,String  prefix,String sufix,String sufixOutput) throws IOException
	{
		for(File arq : dir.listFiles())
		{
			if(!arq.isDirectory() && arq.getName().startsWith(prefix) &&  arq.getName().endsWith(sufix))
			{
				System.out.println("Calculando scale: "+arq.getAbsolutePath());
				calculaScaleMaintainComment(arq.getAbsolutePath(),arq.getAbsolutePath()+sufixOutput);
			}
		}
	}
	public static void calculaScaleMaintainComment(String arquivoFonte,String arquivoDestino)throws IOException
	{
		List<String> lstIdsPerLine = new ArrayList<String>();
		
		//le o texto e armazena os ids por linha
		BufferedReader in = new BufferedReader(new FileReader(arquivoFonte));
		String str;
		
		while ((str = in.readLine()) != null)
		{
			String comment = str.split("\\#")[1];
			lstIdsPerLine.add(comment);
		}
		in.close();
		
		//faz o scale  
		String[] arrTxtArquivoLines = Sys.executarComando("/usr/ferramentas/libsvm/svm-scale -l 0 -u 1 "+arquivoFonte+"",false).split("\n");//"I:\\libsvm-2.86\\tools\\computarParametros \""+caminhoArquivo+"\"");
		
		//organiza o arquivo e o grava
		BufferedWriter out = new BufferedWriter(new FileWriter(arquivoDestino));
		for(int i =0 ; i < arrTxtArquivoLines.length ; i++)
		{
			out.write(StringUtil.removeDoubleSpace(arrTxtArquivoLines[i]+" #"+lstIdsPerLine.get(i))+"\n");
		}
		out.close();
		
		
		
	}
	public static void calculaScale(String arquivoFonte,String arquivoDestino)throws IOException
	{
		System.out.println("/usr/ferramentas/libsvm/svm-scale -l 0 -u 1 "+arquivoFonte);
		String txtArquivo = Sys.executarComando("/usr/ferramentas/libsvm/svm-scale -l 0 -u 1 "+arquivoFonte+"",false);//"I:\\libsvm-2.86\\tools\\computarParametros \""+caminhoArquivo+"\"");
		BufferedWriter out = new BufferedWriter(new FileWriter(arquivoDestino));
	    out.write(txtArquivo);
	    out.close();
	}
	public static void removeComments(File arqEntrada,File arqSaida) throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(arqEntrada));
		BufferedWriter out = new BufferedWriter(new FileWriter(arqSaida));
		String str;
		
		while ((str = in.readLine()) != null)
		{
			String data = str.split("\\#")[0];
			out.write(StringUtil.removeDoubleSpace(data));
			out.write('\n');
		}
		in.close();
		
		out.close();
	}
	public static void writeClass(File arqEntrada,File arqSaida) throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(arqEntrada));
		BufferedWriter out = new BufferedWriter(new FileWriter(arqSaida));
		String str;
		
		while ((str = in.readLine()) != null)
		{
			String data = str.split(" ")[0];
			out.write(StringUtil.removeDoubleSpace(data));
			out.write('\n');
		}
		in.close();
		
		out.close();
	}
	
	public static void joinFeatDataset(File[] arqs, File arqOut, MetodoAprendizado met) throws Exception
	{
		if(arqs.length == 0)
		{
			System.err.println("No arqs");
			return;
		}
		StringBuffer finalArq = new StringBuffer();
		String[][] linhasPerArq = null;
		//resgata linhas por arquivo
		for(int i = 0 ; i < arqs.length ; i++)
		{
			File arq = arqs[i];
			String[] linhasArq = ArquivoUtil.leTexto(arq).split("\n");
			
			if(linhasPerArq == null)
			{
				linhasPerArq = new String[arqs.length][linhasArq.length];
			}else
			{
				if(linhasPerArq[i].length < linhasArq.length)
				{
					throw new Exception("Arquivos de entradas com linhas diferentes ("+arq.getAbsolutePath()+" com menos linhas) ");
					
				}
			}
			linhasPerArq[i] = linhasArq;
		}
		
		//faz junção das linhas
		HashMap<Tupla<Integer,Long>,Integer> mapFeatArqToFeatGlobal = new HashMap<Tupla<Integer,Long>,Integer>();
		int lastIdxFeature = 0;
		int lastIdxInstance = 0;
		for(int linha = 0 ; linha< linhasPerArq[0].length ; linha++)
		{
			Integer id = null;
			String classeReal = null;
			HashMap<Long,String> featSetLinhaGlobal = new HashMap<Long,String>();
			
			for(int arqIdx = 0 ; arqIdx < linhasPerArq.length ; arqIdx++)
			{
				//resgata features do arquivo arqIdx
				String strLinha = linhasPerArq[arqIdx][linha];
				HashMap<Long,String> featLinha = met.getFeaturesVector(strLinha);
				classeReal = met.getClasseReal(strLinha); 
				
				Integer idAtual = met.getIdPorLinhaArquivo(strLinha);
				if(id != null &&idAtual != null && id != idAtual){
					throw new Exception("Ids incompativeis! Id geral: "+id+" idAtual: "+idAtual);
				}
				id = idAtual;
				
				//adiciona as features
				for(long featArqIdx : featLinha.keySet())
				{
					//resgata id da feature no featureset global
					Tupla<Integer,Long> tuplaArqFeatIdxnew =  new Tupla<Integer,Long>(arqIdx,featArqIdx);
					Integer featArqGlobalId = mapFeatArqToFeatGlobal.get(tuplaArqFeatIdxnew);
							
					//caso ela nao exista mapeia, para cada feature do arquivo uma feature global
					if(featArqGlobalId == null)
					{
						
						mapFeatArqToFeatGlobal.put(tuplaArqFeatIdxnew, ++lastIdxFeature);
						featArqGlobalId = mapFeatArqToFeatGlobal.get(tuplaArqFeatIdxnew);
					}
					
					//adiciona a feature no hash de features globais
					featSetLinhaGlobal.put(featArqGlobalId.longValue(), featLinha.get(featArqIdx));
				}
				
			}
			
			//armazena nova linha com todas as features
			if(linha != 0)
			{
				finalArq.append("\n");	
			}
			finalArq.append(met.gerarLinhaDataset(Double.parseDouble(classeReal),(int)(id!=null?id:++lastIdxInstance), featSetLinhaGlobal));
			
		}
		ArquivoUtil.gravaTexto(finalArq.toString(), arqOut, false);
	}

	public static List<LinhaDataset> nomalizeFeaturesDataset(File arqIn,File arqOut,MetodoAprendizado metAp,int minVal,int maxVal,File arqParamMaxMin) throws IOException, ClassNotFoundException 
	{
		Map<Long,Tupla<Double,Double>> lstMaxAndMin = new HashMap<Long,Tupla<Double,Double>>();
		if(arqParamMaxMin != null)
		{
			lstMaxAndMin = (Map<Long,Tupla<Double,Double>>) ArquivoUtil.leObject(arqParamMaxMin);
		}
		
		
		//le texto, transforma numa lista de Hashmap de features por id
		BufferedReader in = new BufferedReader(new FileReader(arqIn),1024*1024*200);
		String strLinha;
		
		
		//agrupa as linhas pelo id que ela eh ordenado
		ListaAssociativa<Integer, LinhaDataset> lstDataset = new ListaAssociativa<Integer, LinhaDataset>();
		
		while ((strLinha = in.readLine()) != null)
		{
			//regata a linha 
			String classeReal = metAp.getClasseReal(strLinha);
			int id = metAp.getIdPorLinhaArquivo(strLinha);
			Integer qid = metAp.getIdPorLinhaArquivo(strLinha, "qid");
			Integer lineOrderBy = 0;//orderBy != null && orderBy.length() > 0?metAp.getIdPorLinhaArquivo(strLinha, orderBy):0;
			HashMap<Long,String> mapIdFeat = metAp.getFeaturesVector(strLinha);
			String paramComment = metAp.getCommentarioLinha(strLinha);
			
			//para cada feature, resgata seu minimo e maximo se necessario
			if(arqParamMaxMin == null)
			{
				for(long idxFeat : mapIdFeat.keySet())
				{
					double val = Double.parseDouble(mapIdFeat.get(idxFeat));
					if(!lstMaxAndMin.containsKey(idxFeat))
					{
						lstMaxAndMin.put(idxFeat, new Tupla<Double,Double>(val,val));
					}else
					{
						Tupla<Double,Double> maxMin = lstMaxAndMin.get(idxFeat);
						if(val>maxMin.getX())
						{
							maxMin.setX(val);
						}
						if(val<maxMin.getY())
						{
							maxMin.setY(val);
						}
					}
				}
			}
			
			//adiciona na lista
			lstDataset.put(lineOrderBy,new LinhaDataset(classeReal, id, qid, mapIdFeat, paramComment));
			
			
		}
		
		
		
		
		
		
		//ordena a lista por esse id e retorna o dataset 
		List<Integer> lstIntOrderBy = new ArrayList<Integer>(lstDataset.keySet()); 
		Collections.sort(lstIntOrderBy);
		List<LinhaDataset> lstDatasetData  = new ArrayList<LinhaDataset>();
		BufferedWriter out = arqOut!=null?new BufferedWriter(new FileWriter(arqOut, false),100):null;;
		//BufferedWriter outId = arqIdsOut!=null?new BufferedWriter(new FileWriter(arqIdsOut, false),100):null;;
		
		
		for(int key : lstIntOrderBy)
		{
			for(LinhaDataset objLinhaDataset : lstDataset.getList(key))
			{
				/*
				//insere o id
				if(outId!=null)
				{
					if(!firstLine)
					{
						outId.append("\n");
						
					}
					
					outId.append(objLinhaDataset.getId().toString());
				}
				*/
				
				
				//atualiza o dataset
				lstDatasetData.add(objLinhaDataset);

					//atualiza feature set de acordo com a sua escala
					for(long idxFeat : objLinhaDataset.getMapFetures().keySet())
					{
						double max = lstMaxAndMin.get(idxFeat).getX();
						double min = lstMaxAndMin.get(idxFeat).getY();
						float valOriginal = Float.parseFloat(objLinhaDataset.getMapFetures().get(idxFeat));
						
						Double val = normalizeVal(minVal, maxVal, max, min,	valOriginal);
						
						if(val != 0 )
						{
							/*
							if(val> 0.00001)
							{
								objLinhaDataset.setIdxFeatVal(idxFeat, new DecimalFormat("#.#####").format(val));
							}
							else
							{
								objLinhaDataset.setIdxFeatVal(idxFeat, new DecimalFormat("##0.#####E0").format(val));
							}
							*/
							
							objLinhaDataset.setIdxFeatVal(idxFeat, Double.toString(val));
						}else
						{
							objLinhaDataset.setIdxFeatVal(idxFeat, null);
						}
					}
					
				if(out != null)
				{			
					String linha = metAp.gerarLinhaDataset(Double.parseDouble(objLinhaDataset.getClasse()),
															objLinhaDataset.getId(), 
															objLinhaDataset.getQid(),
															objLinhaDataset.getMapFetures(),
															objLinhaDataset.getComentario());
					linha = metAp.filtraIdLinha(linha);
					//grava no dataset
					out.write(linha+"\n");	
				}
			}
			
		}
		if(out!=null)
		{
			out.close();
		}
		/*
		if(outId != null)
		{
			outId.close();
		}
		*/
		if(arqParamMaxMin == null)
		{
			//grava o arquivo de parametros
			ArquivoUtil.gravaObject(new File(arqOut.getAbsolutePath()+".param"), lstMaxAndMin);
		
		}
		

		return lstDatasetData;
	}
	public static double normalizeVal(int minVal, int maxVal, double max,
			double min, double valOriginal) {
		if((max-min)==0)
		{
			return 0;
		}
		
		double zeroOne =((valOriginal-min)/(max-min));//garante que ele fica de zero a 1 - o zero eh o min 
		double zeroOneWithRange = zeroOne*Math.abs(minVal-maxVal);//faz com que ele fique com a "range" da normalizacao

		return zeroOneWithRange+minVal;//offset para ficar entre minVal e maxVal
	}
	public static void normalizeFeaturesDatasetTrain(File arqTrain,File arqOutTrain) throws IOException, ClassNotFoundException
	{
		DatasetUtil.nomalizeFeaturesDataset(arqTrain,//arquivo de entrada 
				arqOutTrain, //arquivo de saida
				new GenericoLetorLike(), //metodo aprendizado
				0,//valor minimo
				1, //valor maximo
				null
				);
	}
	public static void normalizeFeaturesDataset(String[] args) throws IOException, ClassNotFoundException
	{
		DatasetUtil.nomalizeFeaturesDataset(new File(args[0]),//arquivo de entrada 
				new File(args[1]), //arquivo de saida
				new GenericoLetorLike(), //metodo aprendizado
				0,//valor minimo
				1, //valor maximo
				args.length >=3?new File(args[2]):null //arquivo de param (opcional)
				);
	}
	public static void main(String[] args) throws Exception
	{
		/*
		String nomTrain = "train_unnorm";
		String nomTest = "test_unnorm";
		String[] argsNew = {"/home/hasan/data/ferramentas/svmrank/example3/"+nomTrain+".dat",
							"/home/hasan/data/ferramentas/svmrank/example3/"+nomTrain+".dat.norm",
							"/home/hasan/data/ferramentas/svmrank/example3/"+nomTrain+".dat.ids"
							};
		args = argsNew;
		*/
		/*
		System.out.println("Args size: "+args.length);
		DatasetUtil.normalizeFeaturesDatasetTrain(new File(args[0]), //treino
												new File(args[1]));//treino saida
		*/
		
		
		/*
		String[] argsNew2 = {"/home/hasan/data/ferramentas/svmrank/example3/"+nomTest+".dat",
							"/home/hasan/data/ferramentas/svmrank/example3/"+nomTest+".dat.norm",
							"/home/hasan/data/ferramentas/svmrank/example3/"+nomTest+".dat.ids",
							"/home/hasan/data/ferramentas/svmrank/example3/"+nomTrain+".dat.norm.param",};
		args = argsNew2;
		*/
		
		DatasetUtil.normalizeFeaturesDataset(args);
		
		
		System.exit(0);
			//DatasetUtil.agruparIds("/usr/wikipedia/combinacoes/6classes/amostra.out","/usr/wikipedia/combinacoes/6classes/grafo_hist_read_txt.pageIds","/usr/wikipedia/combinacoes/6classes/amostra.txt");
			
			//Toy ex
		/*
			String arq1	= "1	1	1:5	3:5.3	4:7.0\n" +
						  "1	2	1:1	2:12	5:99\n"+
						  "1	3	1:3	2:12	8:99";
			
			String arq2	= "1	1	2:20 3:5.3	4:17.0\n" +
					  "1	2	1:2	2:50	5:199	6:58\n"+
					  "1	3	1:13	2:120	8:990";
			
			File a1 = File.createTempFile("x1Arq","");
			File a2 = File.createTempFile("x2Arq","");
			File[] arqs = {a1,a2};
			File saida = File.createTempFile("x2Saida","");
			a1.deleteOnExit();
			a2.deleteOnExit();
			saida.deleteOnExit();
			ArquivoUtil.gravaTexto(arq1, a1,false);
			ArquivoUtil.gravaTexto(arq2, a2,false);
			joinFeatDataset(arqs, saida, new SVM());
			System.out.println(ArquivoUtil.leTexto(saida));
			*/
		//DatasetUtil.scaleAllFilesWithPrefix(new File("/data/experimentos/qa_multiview/datasets/wiki6"), "wiki6",".amostra", ".scaled");
		//DatasetUtil.writeClass(new File(args[0]), new File(args[1]));
		//DatasetUtil.removeComments(new File(args[0]), new File(args[1]));
		File arqIds = new File("/data/experimentos/fonte/wiki6/views_wiki6_balanceada/foldsView/jcdl12_r02_wikiMVTransfer_wiki6_balanceada_treino_fold0.amostra.foldTreinoIds0");
		File arqFonte = new File("/data/experimentos/qa_multiview/datasets/wiki6/wiki6.amostra.scaled");
		File arqSaida = new File("/home/hasan/tmp/treino_atual_2");
		
		MetodoAprendizado metAp = new GenericoSVMLike();
		
		filtraIdsArquivo(arqIds, arqFonte, arqSaida, metAp);
		
		arqIds = new File("/data/experimentos/fonte/wiki6/foldsFiltro/wiki6_todos.amostra.foldIds0");
		arqFonte = new File("/data/experimentos/qa_multiview/datasets/wiki6/wiki6.amostra.scaled");
		arqSaida = new File("/home/hasan/tmp/teste_atual_2");
		
		filtraIdsArquivo(arqIds, arqFonte, arqSaida, metAp);
		
		
		
		/*
		File[] arrFiles = {new File("/home/hasan/tmp/treino_atual_2"),new File("/home/hasan/tmp/teste_atual")} ;
		
		
		MetodoAprendizado metAp = new GenericoSVMLike();
		for(File f : arrFiles)
		{
			
			BufferedWriter out = new BufferedWriter(new FileWriter(f.getAbsolutePath()+".featRemoved", false),100);
			//cria por lina um novo arquivo baseano nas linhas atuais removendo detemrinadas features
			String[] arrLinhas = ArquivoUtil.leTexto(f).split("\n");
			Long[] arrFeatsToRemove = {};//{48L,51L,55L,67L};
			for(String linha : arrLinhas)
			{
				HashMap<Long,String> mapFeatures = metAp.getFeaturesVector(linha);
				for(Long featId : arrFeatsToRemove)
				{
					mapFeatures.remove(featId);
				}
				out.write(metAp.gerarLinhaDataset(Double.parseDouble(metAp.getClasseReal(linha)), mapFeatures)+"\n");	
			}
			
			out.close();
			
		}
		*/
	}
	private static void filtraIdsArquivo(File arqIds, File arqFonte,
			File arqSaida, MetodoAprendizado metAp) throws IOException {
		List<Long> lstTreino = new ArrayList<Long>();
		//treinoIds antigo
		String[] strIdsTreino = ArquivoUtil.leTexto(arqIds).split("\n");
		for(String stId : strIdsTreino)
		{
			lstTreino.add(Long.valueOf(stId));
		}
		metAp.setArquivoOrigem(arqFonte);
		metAp.filtraArquivoPorIds(lstTreino, arqSaida);
	}
}
