package aprendizadoUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import entidadesAprendizado.CnfMetodoAprendizado;
import entidadesAprendizado.Fold;
import entidadesAprendizado.Param;
import entidadesAprendizado.ResultadoItem;
import entidadesAprendizado.XMLMetodoAprendizado;
import io.Sys;
import string.StringUtil;
import stuctUtil.Tupla;
import utilAprendizado.params.ParamUtil;

public class GenericoSVMLike extends SVM
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1520463802356674747L;
	public static XMLMetodoAprendizado xmlMetodoCnf = null;
	public static String PATH_TOOLS = "";
	public static String DIR_CNF_METODOS = "";
	private String cmdTeste = "";
	private String cmdTreino = "";
	private String paramTeste = "";
	private String paramTreino = ""; 
	private String dir = "~";
	private CnfMetodoAprendizado cnfMetodo = null;
	private Map<String,String> mapParamTreino = new HashMap<String,String>();
	private Map<String,String> mapParamTeste = new HashMap<String,String>();
	private String nomeMetodo;
	private String saidaTeste = "";
	private String saidaTreino = "";
	private Long timeout;
	private boolean bolEndedWithTimeout = false;
	private boolean useSubfoldInTest = true;
	
	public GenericoSVMLike()
	{
		
	}
	public GenericoSVMLike(String cmdTreino,String paramTreino, String cmdTeste,String paramTeste)
	{
		this.cmdTeste = cmdTeste;
		this.cmdTreino = cmdTreino;
		this.paramTreino = paramTreino;
		this.paramTeste = paramTeste;
	}
	
	public GenericoSVMLike(String nomeMetodo) throws Exception
	{
		this(nomeMetodo,new HashMap<String,String>(),new HashMap<String,String>());
	}
	public GenericoSVMLike(String nomeMetodo,Map<String,String> mapParamTreino,Map<String,String> mapParamTeste) throws Exception
	{
		this.nomeMetodo = nomeMetodo;
		if(xmlMetodoCnf == null)
		{
			xmlMetodoCnf = new XMLMetodoAprendizado(new File(DIR_CNF_METODOS+"/metodo_aprendizado.xml"));
		}
		cnfMetodo = xmlMetodoCnf.getCNFMetodo(nomeMetodo);
		if(cnfMetodo == null){
			
			System.err.println("Could not find the method name: "+nomeMetodo+" in the XML. Valid names are:  "+xmlMetodoCnf.getCNFMetodoNames());
			System.exit(0);
		}
		boolean isClassificacao = false;
		if(mapParamTeste != null && mapParamTeste.containsKey("SVM_TYPE"))
		{
			if(mapParamTeste.get("SVM_TYPE").equals("1") || mapParamTeste.get("SVM_TYPE").equals("0"))
			{
				
				this.setMode(SVM.MODE_CLASSIFICATION);
				isClassificacao = true;
			}else
			{
				this.setMode(SVM.MODE_REGRESSION);
				isClassificacao = false;
			}
		}
		if(mapParamTreino != null && mapParamTreino.containsKey("SVM_TYPE"))
		{
			if(mapParamTeste.get("SVM_TYPE").equals("1") || mapParamTeste.get("SVM_TYPE").equals("0"))
			{
				this.setMode(SVM.MODE_CLASSIFICATION);
				isClassificacao = true;
			}else
			{
				this.setMode(SVM.MODE_REGRESSION);
				isClassificacao = false;
			}
		}else {
			this.setMode(SVM.MODE_CLASSIFICATION);
			isClassificacao = true;
		}
		//System.out.println("oioi");
		String strModeVal = "";
		if(isClassificacao)
		{
			strModeVal = "0";
		}else
		{
			strModeVal = "3";
		}
		if(mapParamTeste != null) {
			mapParamTeste.put("PATH_TOOLS", PATH_TOOLS);
		}
		
		if(mapParamTreino != null) {
			mapParamTreino.put("PATH_TOOLS", PATH_TOOLS);
		}
		
		if(mapParamTeste != null)
		{
			//o valor do modo caso a classificacao for = 1 é 0
			mapParamTeste.put("IS_CLASSIFICACAO", strModeVal);
		}
		if(mapParamTreino != null)
		{
			//o valor do modo caso a classificacao for = 1 é 0
			mapParamTreino.put("IS_CLASSIFICACAO", strModeVal);
		}
		
		createTrainTestScripts();
		
		this.mapParamTreino = mapParamTreino;
		this.mapParamTeste  = mapParamTeste;
		

		
		//transforma parametros em string
		resetParams(mapParamTreino, mapParamTeste);
	}
	private void resetParams(Map<String, String> mapParamTreino,
			Map<String, String> mapParamTeste) throws Exception {
		if(cnfMetodo.getLstParamsTreino() != null)
		{
			paramTreino = paramsToString(cnfMetodo.getName(),mapParamTreino,cnfMetodo.getLstParamsTreino());
		}else
		{
			paramTreino = "";
		}
		paramTeste = paramsToString(cnfMetodo.getName(),mapParamTeste,cnfMetodo.getLstParamsTeste());
	}
	public void setUseSubFoldInTest(boolean setSubfold) {
		this.useSubfoldInTest = setSubfold;
	}
	public boolean useSubFoldInTest() {
		return this.useSubfoldInTest;
	}
	public void createTrainTestScripts() throws IOException {
		//cria arquivos
		File arqCmdTreino = cnfMetodo.createTrainFile();
		File arqCmdTeste = cnfMetodo.createTestFile();
		
		//transforma em string
		cmdTreino = arqCmdTreino.getAbsolutePath();
		cmdTeste = arqCmdTeste.getAbsolutePath();
		dir = arqCmdTeste.getParentFile().getAbsolutePath();
	}
	public static XMLMetodoAprendizado getXMLMetodo()
	{
		if(xmlMetodoCnf == null)
		{
			try {
				xmlMetodoCnf = new XMLMetodoAprendizado(new File(DIR_CNF_METODOS+"/metodo_aprendizado.xml"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return GenericoSVMLike.xmlMetodoCnf;
	}
	public String getNomeMetodo()
	{
		return this.nomeMetodo;
	}
	public GenericoSVMLike instancia(String nomeMetodo,Map<String,String> mapParamTreino,Map<String,String> mapParamTeste) throws Exception
	{

		
		GenericoSVMLike gLike =  new GenericoSVMLike(nomeMetodo,mapParamTreino,mapParamTeste);
		gLike.setMode(this.isClassificacao()?SVM.MODE_CLASSIFICATION:SVM.MODE_REGRESSION);
		return gLike;
		
	}
	public void setTimeout(long timeout)
	{
		this.timeout = timeout;
	}
	public ArrayList<ResultadoItem> testar(Fold fold) throws Exception
	{
		if(xmlMetodoCnf == null)
		{
			xmlMetodoCnf = new XMLMetodoAprendizado(new File(DIR_CNF_METODOS+"/metodo_aprendizado.xml"));
		}
		
		//resgata os melhores parametros aplicando atraves do fold de validacao (caso exista um fold de validacao
		//e casoexista ocnfMetodo
		Map<String, String> bestParamTreino = new HashMap<String,String>() ;
		Map<String, String> bestParamTeste = new HashMap<String,String>() ;
		if(cnfMetodo != null)// && fold.getValidation() != null && fold.getValidation().exists())
		{
			System.out.println("calculando parametros fold ...");
			//caso haja algum parametro "*" variar este parametro e pegar o melhor resultado
			Fold[] arrFoldTesteParams = null;
			File arqFonteAntigo = this.arqFonte;
			if(fold.getValidation() != null)
			{
				Fold f = new Fold(1,this.arqFonte,fold.getTreino(),fold.getValidation(),fold.getIdsValidation());
				arrFoldTesteParams = new Fold[1];
				arrFoldTesteParams[1] = f;
			}else
			{
				this.arqFonte = fold.getTreino();
			}
			Tupla<Map<String,String>,Map<String,String>> bestParamTreinoTeste = getBestParam(cnfMetodo.getName(),mapParamTreino,mapParamTeste,fold.getTreino(),arrFoldTesteParams);
			bestParamTreino = bestParamTreinoTeste.getX();
			bestParamTeste = bestParamTreinoTeste.getY();
			paramTreino = paramsToString(cnfMetodo.getName(),bestParamTreinoTeste.getX(),cnfMetodo.getLstParamsTreino());
			paramTeste = paramsToString(cnfMetodo.getName(),bestParamTreinoTeste.getY(),cnfMetodo.getLstParamsTeste());
			this.arqFonte = arqFonteAntigo;
		}else
		{
			bestParamTreino = mapParamTreino;
			bestParamTeste = mapParamTeste;
		}
		
		//testa o fold
		System.out.println("teste fold ...");
		//adiciona params
		if(mapParamTreino != null)
		{
			for(String keyParam : bestParamTreino.keySet())
			{
				fold.adicionaParam("tr_"+keyParam, bestParamTreino.get(keyParam));
			}
		}
		if(mapParamTeste != null)
		{
			for(String keyParam : bestParamTeste.keySet())
			{
				fold.adicionaParam("ts_"+keyParam, bestParamTeste.get(keyParam));
			}
		}
		
		
		//testa o fold
		try
		{
			ArrayList<ResultadoItem> lstResultFold = super.testar(fold);
			String strResultLine = "Resultado do fold: "+this.getResultado(lstResultFold);
			//System.out.println(strResultLine);
			saidaTeste += "\n"+strResultLine;
			
			return this.bolEndedWithTimeout?new ArrayList<ResultadoItem>(): lstResultFold;
		}
		catch(IOException ex)
		{
			if(this.bolEndedWithTimeout)
			{
				return new ArrayList<ResultadoItem>();
			}else
			{
				throw ex;
			}
		}
	}
	public Map<String,String> getParamTrain()
	{
		return this.mapParamTreino;
	}
	public Map<String,String> getParamTest()
	{
		return this.mapParamTeste;
	}
	public String getParamTrain(String key)
	{
		return this.mapParamTreino.get(key);
	}
	public void setParamTrain(String key,String val) throws Exception
	{
		this.mapParamTreino.put(key, val);
		this.resetParams(this.mapParamTreino, this.mapParamTeste);
	}

	private Tupla<Map<String, String>, Map<String, String>> getBestParam(String nomeMetodo, Map<String, String> mapParamTreino,	Map<String, String> mapParamTeste,File arquivo,Fold[] arrFoldTesteParam) throws Exception {
		
		
		return ParamUtil.getBestParam(this,  mapParamTreino, mapParamTeste, arquivo,arrFoldTesteParam);
	}
	/*
	 * IMPLEMENTADO EM: ParamUtil
	private ListaAssociativa<String,String> getParamsVariar(Map<String, String> mapParam,List<Param> lstParams) throws Exception {
		// TODO Auto-generated method stub

		ListaAssociativa<String,String> lstParamVariar = new ListaAssociativa<String,String>();
		if(lstParams == null)
		{
			return lstParamVariar;
		}
		for(Param p : lstParams)
		{
		
			if(mapParam.containsKey(p.getName()) && mapParam.get(p.getName()).equals("*")){
				if(p.getValuesVariation().size() == 0)
				{
					throw new Exception("Erro o parametro "+p.getName()+" nao possui variacao!");
				}
				lstParamVariar.put(p.getName(), p.getValuesVariation());
			}
		}
		
		return lstParamVariar;
	}
	*/
	public static String paramsToString(String methodName,Map<String,String> mapParams,List<Param> lstParams) throws Exception
	{
		boolean first = true;
		String strParamComplete = "";
		if(lstParams == null)
		{
			return strParamComplete;
		}
		for(Param p : lstParams)
		{
			if(!first)
			{
				strParamComplete += " ";
				
			}else
			{
				first = false;
			}
			
			String strParam = "";
			if(mapParams.containsKey(p.getName()) )
			{
				strParam = mapParams.get(p.getName());
			}else
			{
				if(p.getDefaultValue() != null)
				{
					strParam = p.getDefaultValue();
				}else
				{
					strParam = "{"+p.getName()+"}";
				}
			}
			
			strParamComplete += strParam;
		}
		//System.out.println("PARAMS: "+strParamComplete);
		return strParamComplete;
	}
	public void setDirExecucaoComando(String dir)
	{
		
		this.dir = dir;
	}
	public boolean endedWithTimeout()
	{
		return this.bolEndedWithTimeout;
	}
	
	public void setParamUnsetedTrain(String paramKey,String paramVal)
	{
		this.paramTreino.replaceAll("\\{\\{"+paramKey+"\\}\\}", paramVal);
	}
	public void setParamUnsetedTest(String paramKey,String paramVal)
	{
		this.paramTeste.replaceAll("\\{\\{"+paramKey+"\\}\\}", paramVal);
	}
	
	@Override
	public File testar(Fold fold, String nomeBase, String treino,String teste, String pathDiretorio) throws IOException {
		String resp;
		bolEndedWithTimeout = false;
		
		
		String nomeResult = UUID.randomUUID().toString();
		String resultFile = pathDiretorio+"/result_"+nomeResult+".predict"+fold.getNum();
		File arqPredict = new File(resultFile);
		boolean existe = arqPredict.exists();
		String saidaErro = "";
		if(this.isGetResultPreCalculado() && arqPredict.exists())
		{
			System.err.println("ATENCAO! Usando predict ja existente");
		}else
		{
			//fold.setPredict(new File(pathDiretorio+"/"+nomeBase+".predict"+fold.getNum()) );
			
			String modeloFile = pathDiretorio+"/"+nomeBase+".model"+fold.getNum();
			File arqModel = new File(modeloFile);
			//System.out.println("IDS: "+ fold.getIdsFile().getAbsolutePath());
			String fmtParamTreino = formataStringParametros(paramTreino,treino,teste,resultFile,modeloFile,
															fold.getValidation() != null?fold.getValidation().getAbsolutePath():"",
															fold.getIdsFile().getAbsolutePath(),new ArrayList<Integer>(fold.getIdsSemClasse()),
															fold.getIdsFile().getParentFile());

			String fmtParamTeste = formataStringParametros(paramTeste,treino,teste,resultFile,modeloFile,fold.getValidation() != null?fold.getValidation().getAbsolutePath():"",fold.getIdsFile().getAbsolutePath(),new ArrayList<Integer>(fold.getIdsSemClasse()),fold.getIdsFile().getParentFile());
			
			if(this.isUsarModeloExistent() && arqModel.exists())
			{
				System.err.println("Atencao! usando modelo ja existente");
			}else
			{
				long treinoInicio = System.currentTimeMillis();
				saidaTreino += "======================== Treinando FOLD "+fold.getNum()+" "+treino+" validaccao:"+(fold.getValidation() != null?fold.getValidation().getAbsolutePath():"")+" Parametros: "+fmtParamTreino+"=======================\n";
				String cmd = cmdTreino+" "+ fmtParamTreino;
				if(this.timeout==null)
				{
						saidaTreino += Sys.executarComando(cmdTreino+" "+ fmtParamTreino,false,dir);
				}else
				{
					System.out.println("Executando com timeout de "+this.timeout+" ms");
					try {
						Tupla<String,String> objSaidaTreino = Sys.executarComandoWithErrorAndStdInput(cmd, false, true, this.timeout);
						saidaTreino += saidaErro;
						saidaErro = objSaidaTreino.getY();
					} catch (TimeoutException e) {
						// TODO Auto-generated catch block
						bolEndedWithTimeout = true;
						return null;
					}
				}
				saidaTreino += "\n\n Executado em: "+(System.currentTimeMillis()-treinoInicio)/(double)1000+"segundos";
				saidaTreino += "\n\n";				
				
			}

			long testeInicio = System.currentTimeMillis();
			saidaTeste += "======================== Testando FOLD "+fold.getNum()+": "+teste+" Predict:"+resultFile+" Parametros: "+fmtParamTeste+"=======================\n";
			try {
			Tupla<String,String> objSaidaTreino = Sys.executarComandoWithErrorAndStdInput(cmdTeste+" "+ fmtParamTeste, false, true, Long.MAX_VALUE);
			saidaTeste += objSaidaTreino.getX();
			//saidaTeste += Sys.executarComando(cmdTeste+" "+ fmtParamTeste,false,dir);
			saidaTeste += "\n\n Executado em: "+(System.currentTimeMillis()-testeInicio)/(double)1000+"segundos";
			saidaTeste += "\n\n";
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				bolEndedWithTimeout = true;
				return null;
			}

		    
		}    
	    fold.setPredict(new File(resultFile) );
	    if(!fold.getPredict().exists())
	    {
	    	System.out.println("O arquivo de resultado nao foi criado!");
	    	System.out.println("=========================Saida do treino: ");
	    	System.out.println(saidaTreino);
	    	System.out.println(saidaErro);
	    	
	    	System.out.println("=================Saida do teste: ");
	    	System.out.println(saidaTeste);
	    	
	    }
	    System.out.println("Predict file: "+fold.getPredict());
		return null;
	}
	public String formataStringParametros(String param,String treino,String teste,String result,String modelo,String validacao,String foldIdsTeste,List<Integer> idsSemClasse,File foldDir) throws IOException
	{
		String formatedStr = param;
		formatedStr = formatedStr.replaceAll("\\{ARQ_MODELO\\}", modelo);
		formatedStr = formatedStr.replaceAll("\\{ARQ_RESULTADO\\}", result);
		formatedStr = formatedStr.replaceAll("\\{ARQ_TREINO\\}", treino);
		formatedStr = formatedStr.replaceAll("\\{ARQ_TESTE\\}", teste);
		formatedStr = formatedStr.replaceAll("\\{FOLD_IDS_TESTE\\}", foldIdsTeste);
		
		if(formatedStr.contains("{FOLD_IDS_SEM_CLASSE}"))
		{
			formatedStr = formatedStr.replaceAll("\\{FOLD_IDS_SEM_CLASSE\\}", Fold.criaIdsFile(foldDir,idsSemClasse,".filterHiddenClasses").getAbsolutePath());
		}
		
		
		formatedStr = formatedStr.replaceAll("\\{ARQ_VALIDATION\\}", validacao);
		
		return formatedStr;
		
		
	}
	
	public File filtraIDsArquivo(File arquivoIn, File arquivoOut) throws IOException
	{
		//se for letor, nao ha necessidade de filtrar
		if(!(this instanceof GenericoLetorLike))
		{
		
		
			//System.out.println("oi");
			BufferedReader in = new BufferedReader(new FileReader(arquivoIn));
			//arquivo temporario do filtro id
			File tmpOut = File.createTempFile("filtroID","");
			tmpOut.deleteOnExit();
			BufferedWriter out = new BufferedWriter(new FileWriter(tmpOut, false));
			
			String strLinha;
			
			while ((strLinha = in.readLine()) != null)
			{
				String textoSaida = strLinha.replaceAll("( |\t)[0-9]+( |\t)", " ");
				textoSaida = StringUtil.removeDoubleSpace(textoSaida);
				textoSaida = textoSaida.replaceAll("#qid", "@qid");
				out.write(textoSaida+"\n");
			}
			in.close();
			out.close();
			
			Sys.executarComando("cp "+tmpOut.getAbsolutePath()+" "+arquivoOut.getAbsolutePath(), true);
			//ArquivoUtil.gravaTexto(textoSaida, arquivoOut, false);
			return arquivoOut;
		}else
		{
			Sys.executarComando("cp "+arquivoIn.getAbsolutePath()+" "+arquivoOut.getAbsolutePath(), true);
			//ArquivoUtil.gravaTexto(textoSaida, arquivoOut, false);
			return arquivoOut;
		}
		
	}
	public String getSaidaTreino()
	{
		return this.saidaTreino;
	}
	public String getSaidaTeste()
	{
		return this.saidaTeste;
	}
	public String toString()
	{
		return "GenericoSVM Metodo: "+this.nomeMetodo+" Params Treino: "+this.mapParamTreino.toString()+" ParamsTeste:"+this.mapParamTeste.toString();
	}
	
	public static void  main(String[] args) throws Exception
	{
		List<String> lst = new ArrayList<String>();
		
		lst.add("ola");
		lst.add("ola!");
		lst.add("oiii");
		lst.add("otimo");
		lst.add("allala");
		lst.add("alalla2");
		lst.add("oioioi");
		
		for(int i = 0; i < lst.size() ; i++){
			if(lst.get(i).equals("ola")){
				lst.remove(i);
			}
		}
		System.out.println(lst);
		
		
		//GenericoSVMLike g = new GenericoSVMLike();
		//g.filtraIDsArquivo(new File("/home/hasan/Desktop/oioi.txt"), new File("/home/hasan/Desktop/oioi.txt"));
		
		//String strLine = "3 qid:40 1:1 2:0 3:0 4:0 5:1 6:0 7:0 8:0 9:0 10:0";
				/*"11:0.0 12:0.0 13:0.0 14:0.0 15:654.0 16:654.0 17:0.0 18:654.0 19:0.0 20:0.0 " +
				"21:0.0 22:0.0 23:0.0 24:0.0 25:5.0 26:128.0 27:23.0 28:1023 29:3.0 30:0.0 " +
				"31:0.0 32:4.0 33:14.0 34:4.0 35:2.0 36:6.0 37:5.0 38:14.0 39:0.0 40:1.0 " +
				"41:0.0 42:2.0 43:0.0 44:0.0 45:3.0 46:0.169473 47:0.020202 48:8.0 49:3.0";*/
				/*"50:18.0 51:1.99752 52:0.472503 53:3.65942 54:1.0E33 55:5.52794 56:5.79835 " +
				"57:4.0 58:0.0 59:5.0 60:4.2 61:12.7 62:69.0 63:7.2 64:24.3 65:7.6 66:0.561213 " +
				"67:3.14393 68:0.00623459 69:0.00451132 70:14.0 71:50.0 72:3.0 73:3.0 74:2.0 " +
				"75:3.0 76:34.0 77:21.0 78:16.0 79:34.0 80:21.0 81:15.0 82:4.0 83:5.0 84:1.0 " +
				"85:1.0 86:3.0 87:5.0 88:0.0 89:0.0 90:0.0 91:0.0 92:9.2437 93:8.38727 ";*/
				/*"94:2.48862 97:9.89902 98:8.38727 99:2.48862 102:3.0 103:5.0 104:0.0 " +
				"105:0.0 106:0.0 107:0.0 108:2.0 109:2.0 110:6.0 111:1.5 " +
				"112:0.666666666666667 113:1.66666666666667 114:0.25 115:0.977272727272727 116:1.0 117:9.0 118:0.0 119:17.0 120:3.0 121:29.0 122:3.0 123:0.0 124:0.0 " +
				"125:5.0 126:2.0 127:960.0 128:950.0 129:7.0 130:3.0 131:2.0 132:3.0 133:1.0 134:1.0 135:1.0 136:5.0 137:1.0 138:0.0 139:0.0 140:0.0 " +
				"141:0.909090909090909 142:1.0 143:0.0 144:0.0 145:93.0 146:43.0 147:0.288435 148:0.0 149:0.310613 150:1.0 151:960.325 152:960.327 153:1 154:0 155:10 " +
				"156:0 157:0 158:0 159:2 160:3 161:0 162:0 163:1 164:0 165:0 166:2"*/;
		//GenericoLetorLike letor = new GenericoLetorLike();
		//System.out.println(letor.linhaMatchesFormat(strLine));
		
				
			
	}
}
