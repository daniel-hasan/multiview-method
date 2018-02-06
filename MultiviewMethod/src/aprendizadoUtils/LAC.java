package aprendizadoUtils;

import io.Sys;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import arquivo.ArquivoUtil;
import entidadesAprendizado.Fold;
import entidadesAprendizado.ResultadoItem;

public class LAC extends MetodoAprendizado {
	private static int id = 0;
	private static final File DIRETORIO_LAC = new File("/data/ferramentas/lac");
	private int m = 3;
	private double c = 0.8;
	private double s = 1;
	private HashMap<String,Boolean> mapDiscretizedFiles = new HashMap<String, Boolean>();
	
	public LAC() {

	}
	public LAC(File arqOrigem)
	{
		
	}
	public void setM(int m)
	{
		this.m = m;
	}
	
	public void setC(double c)
	{
		this.c = c;
	}

	@Override
	public File filtraIDsArquivo(File arquivoIn, File arquivoOut)
			throws IOException {
		// TODO Auto-generated method stub
		ArquivoUtil.copyfile(arquivoIn, arquivoOut);

		return arquivoOut;
	}

	@Override
	public String gerarLinhaDataset(double classe,HashMap<Long, String> features) {
		// TODO Auto-generated method stub
		return gerarLinhaDataset(classe,features) ;
	}

	@Override
	public StringBuilder gerarLinhaFeatures(HashMap<Long, String> features) {
		// TODO Auto-generated method stub
		StringBuilder strFeatures = new StringBuilder();
		for (long featNum : features.keySet()) {
			strFeatures.append(" w[" + featNum + "]=" + features.get(featNum));
		}
		return strFeatures;
	}

	@Override
	public String gerarLinhaDataset(double classe, int id,
			HashMap<Long, String> features) {

		return id + " CLASS=" + (int) Math.round(classe) + " " + gerarLinhaFeatures(features).toString().trim();
	}

	@Override
	public String getClasseReal(String linha) {
		String[] arrClaseFeat = linha.split(" ");
		for(String colLinha :arrClaseFeat)
		{
			// TODO Auto-generated method stub
			if (colLinha.matches(".*CLASS=[.0-9]+.*")) {
				
				return colLinha.replace("CLASS=", "").trim();
			}	
		}
		
		return null;
	}

	@Override
	public String getFeaturesString(String linha) {
		// TODO Auto-generated method stub
		int idx = linha.indexOf("w[");
		
		if(idx != -1)
		{
			return linha.substring(idx);
		}else
		{
			return "";
		}

	}

	@Override
	public HashMap<Long, String> getFeaturesVector(String linha) {
		// TODO Auto-generated method stub
		String[] arrFeatures = getFeaturesString(linha).split(" |\\t");
		HashMap<Long, String> mapFeatures = new HashMap<Long, String>();
		for (int i = 0; i < arrFeatures.length; i++) {
			if (arrFeatures[i].matches("( )?w\\[[0-9]+\\]=.+")) {
				String strNumFeatures =arrFeatures[i].replace("w[","");
				strNumFeatures =strNumFeatures.substring(0, strNumFeatures.indexOf("]"));
				
				long numFeature = Long.parseLong(strNumFeatures); 
				String valFeature = arrFeatures[i].substring(arrFeatures[i].indexOf("=")+1);

				mapFeatures.put(numFeature, valFeature);
			}
		}

		return mapFeatures;
	}

	@Override
	public Integer getIdPorLinhaArquivo(String linha) {
		// TODO Auto-generated method stub
		if(!linha.split(" ")[0].trim().matches("[0-9]+"))
		{
			
			return null;
		}
			return Integer.parseInt(linha.split(" ")[0].trim());
		
	}
	
	@Override
	public ArrayList<ResultadoItem> testar(Fold fold) throws Exception {
	
		
		//discretiza teste/treino só uma vez durante a execução
		
		System.out.println("LAC: DISCRETIZANDo... Treino: "+fold.getTreino().getAbsolutePath()+" Teste: "+fold.getTeste().getAbsolutePath());
		File treinoDiscreto = new File( fold.getTreino().getAbsolutePath()+".disc");
		File testeDiscreto = new File( fold.getTeste().getAbsolutePath()+".disc");
		WekaFront.discretizar(fold.getTreino(), fold.getTeste(), treinoDiscreto, testeDiscreto, this);
		
		fold.getTreino().deleteOnExit();
		fold.getTeste().deleteOnExit();
		
		//
		//fold.setTreino(treinoDiscreto);
		//fold.setTeste(testeDiscreto);
		
		//testar com o teste e treino discretizado
		String result = Sys.executarComando("./lazy -i "+treinoDiscreto.getAbsolutePath()+" -t "+testeDiscreto.getAbsolutePath()+" -s "+s+" -e 1000000000 -c "+c+" -m "+m, false, DIRETORIO_LAC.getAbsolutePath());
		
		File arqPredict  = new File(fold.getTreino().getParent(),fold.getNomeBase()+".predict"+fold.getNum());
		
		fold.setPredict(arqPredict);
		ArquivoUtil.gravaTexto(result, arqPredict, false);
		
		return parseResult(fold.getTeste(), result);
	} 
	public   ArrayList<ResultadoItem> parseResult(File teste,String result) throws Exception
	{
		String[] linhasTeste = ArquivoUtil.leTexto(teste).split("\n");
		String[] linhasPredict = result.split("\n");
		if(linhasPredict.length < linhasTeste.length)
		{
			throw new Exception("Ocorreu algum erro na predição, o numero de linhas preditas é menor que o arquivo de teste\n"+result);
		}
		ArrayList<ResultadoItem> lstResultado = new ArrayList<ResultadoItem>();
		int idxPrevisto = 0;
		for(int i = 0 ; i<linhasTeste.length ; i++){
			Float classeReal = Float.parseFloat(getClasseReal(linhasTeste[i]));
			long id = getIdPorLinhaArquivo(linhasTeste[i]);
			
			//resgata classe predita
			String linhaPredita = "";
			
			Integer idLinha = null;
			do
			{
				linhaPredita = linhasPredict[idxPrevisto];
				idxPrevisto++;
				
			}while( linhaPredita.matches("[a-zA-Z]+.*"));
			
			Double classePrevista =   getClassePrevista(linhaPredita);
			Double confianca =   getConfiancaClasse(linhaPredita);
			lstResultado.add(new ResultadoItem(id,classeReal,classePrevista.floatValue(), confianca.floatValue()));
		}
		
		return lstResultado;
	}
	public Double getClassePrevista(String linha) throws Exception
	{
		String[] resp = linha.split(" ");
		for(int i =0 ; i< resp.length ; i++)
		{
			if(resp[i].startsWith("prediction"))
			{
				return Double.parseDouble(resp[i+1]);
			}
		}
		throw new Exception("Classe prevista nao encontrada na linha '"+linha+"'");
		
	}
	public Double getConfiancaClasse(String linha)
	{
		return Double.parseDouble(linha.split(" ")[1]); 
	}
	
	public static void main(String[] args) throws Exception
	{
		LAC lac = new LAC();
		
		
		
		System.out.println(lac.testar(new Fold(1,null,new File("/data/experimentos/fonte/LAC/wiki6/teste/wiki6_lac_todas_treino.amostra"),new File("/data/experimentos/fonte/LAC/wiki6/teste/wiki6_lac_todas_treino.amostra"),null)));
		
		boolean bol = "25 CLASS=5.0 w[1]=0.303051 w[4]=0.0149042 ".matches(".*CLASS=[.0-9]+.*");
		
		//System.out.println(lac.getClasseReal("25 CLASS=5.0 w[1]=0.303051 w[4]=0.0149042 "));
		//System.out.println("BOL: "+bol);
		
		
	}
	
	@Override
	public ArrayList<ResultadoItem> parseResult(Fold fold) throws Exception {
		// TODO Auto-generated method stub
		return new ArrayList<ResultadoItem>();
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
	@Override
	public MetodoAprendizado clone() {
		// TODO Auto-generated method stub
		return null;
	}

}
