package aprendizadoUtils;

import io.Sys;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import string.RegExpsConst;
import arquivo.ArquivoUtil;
import entidadesAprendizado.Fold;
import entidadesAprendizado.ResultadoItem;

public class KNN extends MetodoAprendizado {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int lastId = 1;
	private static final String dirKNN = "/data/ferramentas/knn/";
	private int k = 30;
	
	
	@Override
	public StringBuilder gerarLinhaFeatures(HashMap<Long, String> features) {
		// TODO Auto-generated method stub
		
		
		StringBuilder strLinhaFeatures = new StringBuilder();
		List<Long> lstKeys = new ArrayList<Long>(features.keySet());
		Collections.sort(lstKeys);
		
		for(long featNum : lstKeys)
		{
			String featValue = features.get(featNum);
			if(strLinhaFeatures.length()>0)
			{
				strLinhaFeatures.append(";");	
			}
			strLinhaFeatures.append(featNum+";"+featValue);
			
		}
		return strLinhaFeatures;
	}
	public void setK(int k)
	{
		this.k = k;
	}
	@Override
	public String gerarLinhaDataset(double classe,HashMap<Long, String> features) {
		// TODO Auto-generated method stub
		return (lastId++)+";1;CLASS="+classe+";"+gerarLinhaFeatures(features);
	}

	@Override
	public String gerarLinhaDataset(double classe, int id, HashMap<Long, String> features) {
		// TODO Auto-generated method stub
		
		return id+";1;CLASS="+classe+";"+gerarLinhaFeatures(features);
	}

	@Override
	public HashMap<Long,String> getFeaturesVector(String linha) {
		// TODO Auto-generated method stub
		//get the max index
		String[] arrLinha = linha.split(";");
		
		
		HashMap<Long,String> mapedFeatures = new HashMap<Long, String>();
		//add the features
		for(int i = 3 ; i<arrLinha.length ; i+=2)
		{
			Long index = Long.parseLong(arrLinha[i]);
			String featValue = arrLinha[i+1];
			
			mapedFeatures.put(index, featValue);
		}
		
		
		return mapedFeatures;
	}

	@Override
	public String getFeaturesString(String linha) {
		// TODO Auto-generated method stub
		int index = linha.indexOf(';');
		index = linha.indexOf(';', index+1);
		index = linha.indexOf(';', index+1);
		
		return linha.substring(index+1);
	}

	@Override
	public String getClasseReal(String linha) {
		// TODO Auto-generated method stub
		int index = linha.indexOf("CLASS=");
		int indexFim = linha.indexOf(";",index);
		return linha.substring(index,indexFim).replaceAll("CLASS=", "");
	}

	@Override
	public File filtraIDsArquivo(File arquivoIn, File arquivoOut)
			throws IOException {
		// TODO Auto-generated method stub
		ArquivoUtil.copyfile(arquivoIn, arquivoOut);
		return arquivoOut;
	}


	@Override
	public Integer getIdPorLinhaArquivo(String linha) {
		// TODO Auto-generated method stub
		return Integer.parseInt(linha.split(";")[0]);
	}

	
	/*********************** Realiza teste ******************************************/
	@Override
	public ArrayList<ResultadoItem> testar(Fold fold) throws Exception {
		// TODO Auto-generated method stub
		
		/**Executar teste **/
		
		//define arquivos
		File pathDiretorio = fold.getTreino().getParentFile();
		String nomeBase = fold.getTreino().getName().replaceAll("\\..*", "");
		
		fold.setPredict(new File(pathDiretorio,nomeBase+".predict"+fold.getNum()));
		
		
		//executa arquivos
		String cmd  = dirKNN+"/fastKNN -d "+fold.getTreino().getAbsolutePath()+" -t "+fold.getTeste().getAbsolutePath()+" -k "+k; 
		String resp = Sys.executarComando(cmd, false);
		
		//grava predição no arquivo
		if(resp.contains("CLASS="))		
		{
			ArquivoUtil.gravaTexto(resp, fold.getPredict(), false);
		}else
		{
			throw new Exception("ERRO, a reposta foi: "+resp);
		}
		fold.adicionaParam("k", Integer.toString(k));
		
		
		
		//retorna o parse do resultado
		return parseResult(fold);
	}
	public float getClasseParser(String itemResult)
	{	
		String classe = itemResult.replaceAll(":[-.e0-9nanNAN]+", "");
		classe = classe.replaceAll("CLASS=", "");
		return Float.parseFloat(classe);
	}
	
	public float getPesoParser(String itemResult)
	{	
			String peso = itemResult.replaceAll("CLASS="+RegExpsConst.DIGITO_FLOAT_OPCIONAL+":", "");
			if(peso.contains("nan"))
			{
				return 0;
			}
			return Float.parseFloat(peso);
	}
	
	public ArrayList<ResultadoItem> parseResult(Fold fold) throws IOException
	{
		String[] arrResult = ArquivoUtil.leTexto(fold.getPredict()).split("\n");
		
		ArrayList<ResultadoItem> lstResultados = new ArrayList<ResultadoItem>();
		
		List<Float> lstPesos = new ArrayList<Float>();
		
		//para cada linha do resultado... 
		for(String linhaResult : arrResult)
		{
			if(linhaResult.startsWith("#"))
			{
				continue;
			}
			ResultadoItem rItem = null;
			Float maxClass = 0F;
			Float maxPeso = 0F;
			String[] itemResult = linhaResult.split(" ");
			
			int id = Integer.parseInt(itemResult[0]);
			if(itemResult.length>2)
			{
				//verifica a classe maxima e o peso mximo
				for(int i = 2; i < itemResult.length ; i++)
				{
					Float classe = getClasseParser(itemResult[i]);
					
					if(classe > maxClass)
					{
						maxClass = classe;
					}
					
					
					Float peso = getPesoParser(itemResult[i]);
					lstPesos.add(peso);
					/*
					if(peso > maxPesoTotal)
					{
						maxPesoTotal = peso;
					}
					*/
							
				}
				//obtem classe real e prevista
				float classeReal = getClasseParser(itemResult[1]);
				float classePrevista = getClasseParser(itemResult[2]);
				
				//cria array de pesos das notas
				float[] pesos = new float[Math.round(maxClass)+1];
				
				
				//adiciona os pesos de cada nota
				for(int i = 2; i < itemResult.length ; i++)
				{
					Float classe = getClasseParser(itemResult[i]);
					Float peso = getPesoParser(itemResult[i]);
			
					
					pesos[(int) Math.round(classe)] = peso;
					
				}
				

				
				//cria resultado
				try {
					rItem = new ResultadoItem(id,classeReal,classePrevista,pesos);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else
			{
				float classeReal = getClasseParser(itemResult[1]);
				rItem = new ResultadoItem(id,classeReal,0,0);
				rItem.setImpossibleToPredict();
			}
			
			lstResultados.add(rItem);
			
		}
		//Collections.sort(lstPesos);
		//System.out.println("Peso total: "+lstPesos);
		//System.exit(0);
		return lstResultados;
	}
	public boolean linhaMatchesFormat(String linha)
	{
		String regExp = RegExpsConst.DIGITO+";"+RegExpsConst.DIGITO+";CLASS="+RegExpsConst.DIGITO_FLOAT_OPCIONAL+"(;"+RegExpsConst.DIGITO+";"+RegExpsConst.DIGITO_FLOAT_OPCIONAL+")+";
		System.out.println(regExp);
		return linha.matches(regExp);
	}
	
	public static void main(String[] args) throws Exception
	{
		KNN objKNN = new KNN();
		
		String linha = "23;1;CLASS=812;1260781;0.000000000000000;1881674;0.000000000000000;1881741;0.293905068310986;1904607;0.583436485994321;2157723;0.000000000000000;2222146;0.535360475582440;2299586;0.535360475582440";
		String featString = objKNN.getFeaturesString(linha);
		HashMap<Long,String> mapFeat = objKNN.getFeaturesVector(linha);
		
		System.out.println("MATCHES? "+objKNN.linhaMatchesFormat(linha));
		

		System.out.println("ID: "+objKNN.getIdPorLinhaArquivo(linha)); //ok
		System.out.println("Classe Real: "+objKNN.getClasseReal(linha)); //ok
		System.out.println("FEAT STRING: "+featString);//ok
		System.out.println("FEAT VECTOR: "+mapFeat); //ok
		System.out.println("Linha feat pelo vetor: "+objKNN.gerarLinhaFeatures(mapFeat)); //ok
		
		System.out.println("Linha pelo vetor: "+objKNN. gerarLinhaDataset(155,mapFeat)); //ok
		System.out.println("Linha pelo vetor id: "+objKNN. gerarLinhaDataset(155,11,mapFeat)); //ok
		
		Fold f = new Fold(1,null,new File("/home/hasan/views/youtube-sp.iff/knn_method/TAG.train0"),new File("/home/hasan/views/youtube-sp.iff/knn_method/TAG.test0"),null,null);
		
		System.out.println(objKNN.testar(f));
	}
	@Override
	public MetodoAprendizado clone() {
		// TODO Auto-generated method stub
		KNN objKnn = new KNN();
		objKnn.setK(objKnn.k);
		objKnn.setArquivoOrigem(this.getArquivoOrigem());
		objKnn.setNomExperimento(this.getNomExperimento());
		return objKnn;
	}
	@Override
	public Integer getIdPorLinhaArquivo(String linha, String nomIdentificador) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String gerarLinhaDataset(double classe, int id,
			HashMap<Long, String> features, Map<String, String> paramsComment) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String gerarCabecalhoResultado(List<String> lstClasses) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String gerarLinhaResultado(ResultadoItem ri) {
		// TODO Auto-generated method stub
		return null;
	}


}
