package entidadesAprendizado;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


import aprendizadoResultado.CalculaResultados;
import aprendizadoUtils.GenericoLetorLike;
import aprendizadoUtils.MetodoAprendizado;
import banco.GerenteBD;
import config_tmp.Colecao;
import config_tmp.ConfigViewColecao;



public class View implements Serializable 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final int NUM_FOLDS_VIEW = 3;
	private static boolean TESTAR_TREINO = true;
	
	private boolean usarBanco = true; 
	private String sglView=null;
	private File arquivo;
	private MetodoAprendizado metodoAprendizado;
	private String nomExperimento;
	private Resultado resultTreino;
	private Resultado resultTeste;
	private Resultado resultValidacao;
	private Colecao colView;
	private ConfigViewColecao cnfView;
	private ArrayList<Long> filtroId = new ArrayList<Long>();
	private double porcfiltroTreinoAleatorio = 0;
	private long seedTreinoAleatorio = System.currentTimeMillis();
	public static TIPO_DIVISAO_TREINO tpoDivisao = TIPO_DIVISAO_TREINO.FOLD_VALIDACAO_NOVE_FOLDS_BALANCEADO_TESTE_IGUAL;
	private Fold[] foldsTeste;
	private Fold[] foldsTreino;
	private FeatureType featureType;
	private boolean boostView = false;
	private View parentView = null;
	private List<View> childViews = new ArrayList<View>();
	
	
	public enum TIPO_DIVISAO_TREINO{
		THREE_FOLD,FOLD_VALIDACAO_NOVE_FOLDS,FOLD_VALIDACAO_NOVE_FOLDS_BALANCEADO,FOLD_VALIDACAO_NOVE_FOLDS_BALANCEADO_TESTE_IGUAL,FOLD_VALIDACAO_NOVE_FOLDS_TESTE_IGUAL;
	}
	public void setSglView(String sgl)
	{
		this.sglView = sgl;
	}
	public String getSglView()
	{
		return this.sglView;
	}
	public enum FeatureType {
		
		
		VIEW_1,VIEW_2,VIEW_3,VIEW_4,VIEW_5,VIEW_6,VIEW_7,VIEW_8,VIEW_9,VIEW_10,VIEW_11,VIEW_12,VIEW_13,VIEW_14,VIEW_15,VIEW_16,VIEW_17,VIEW_18,VIEW_19,VIEW_20,
		 
		WIKIPEDIA,SW_VOTE,SW_LABEL,MUPPETS, //coleções que são visões
		WIKIPEDIA_LOCAL,//colecao local
		
		TEXT,STRUCTURE,STYLE,LENGTH,READ,HISTORY,NETWORK,//visões (globais) por grupo de atributo
		LAC_TEXT,LAC_STRUCTURE,LAC_STYLE,LAC_LENGTH,LAC_READ,LAC_HISTORY,LAC_NETWORK,//visões (globais) por grupo de atributo
		
		TEXT_LOCAL,STRUCTURE_LOCAL,STYLE_LOCAL,LENGTH_LOCAL,READ_LOCAL,HISTORY_LOCAL,NETWORK_LOCAL,//visões (local) por grupo de atributo
		
		/*******YOUTUBE***********/
		YT_TITLE,YT_COMMENT,YT_TAG,YT_DESCRIPTION,//youtube views
		YT_KNN_TITLE,YT_KNN_COMMENT,YT_KNN_TAG,YT_KNN_DESCRIPTION, //KNN
		YT_BAGOW,YT_CONCAT,YT_KNN_BAGOW,YT_KNN_CONCAT;//baselines
		
		
		public static FeatureType[] ARR_GENERIC_VIEW = {VIEW_1,VIEW_2,VIEW_3,VIEW_4,VIEW_5,VIEW_6,VIEW_7,VIEW_8,VIEW_9,VIEW_10,VIEW_11,VIEW_12,VIEW_13,VIEW_14,VIEW_15,VIEW_16,VIEW_17,VIEW_18,VIEW_19,VIEW_20};
		
		public boolean isTextual()
		{
			return this == TEXT || this == STRUCTURE || this == STYLE || this == LENGTH || this == READ ||
					this == TEXT_LOCAL || this == STRUCTURE_LOCAL || this == STYLE_LOCAL || this == LENGTH || this == READ_LOCAL;
				
		}
		
		public boolean isLocal()
		{
			return this.toString().toUpperCase().endsWith("_LOCAL");
		}

		public String getName()
		{
			switch (this) {
				case TEXT:
				case TEXT_LOCAL:
					return "Text";
				case STRUCTURE:
				case STRUCTURE_LOCAL:					
					return "Est";
				case STYLE:
				case STYLE_LOCAL:					
					return "Style";
				case LENGTH:
				case LENGTH_LOCAL:					
					return "Tam";
				case READ:
				case READ_LOCAL:
					return "Read";
				case HISTORY:
				case HISTORY_LOCAL:
					return "Hist";
				case NETWORK:
				case NETWORK_LOCAL:
					return "Grafo";
					
					
			}
			
			return this.toString().toLowerCase();
		}
		
		public String getAbvName()
		{
			switch (this) {
				case TEXT:
				case TEXT_LOCAL:
					return "T";
				case STRUCTURE:
				case STRUCTURE_LOCAL:					
					return "E";
				case STYLE:
				case STYLE_LOCAL:					
					return "Y";
				case LENGTH:
				case LENGTH_LOCAL:					
					return "L";
				case READ:
				case READ_LOCAL:
					return "R";
				case HISTORY:
				case HISTORY_LOCAL:
					return "H";
				case NETWORK:
				case NETWORK_LOCAL:
					return "N";
					
					
			}
			
			return this.toString().toLowerCase();
		}
		
	}
	
	
	public View(ConfigViewColecao cnf, File arquivo,MetodoAprendizado metodoAprendizado,Colecao colView)
	{
		this( cnf,  arquivo,metodoAprendizado);
		this.colView = colView;
		
		switch(this.colView)
		{
			case	WIKI_CULTURE:
			case	WIKI_GEOGRAPHY:
			case 	WIKI_HISTORY:
			case    WIKI_SCIENCE:
			case 	WIKI_RANDOM_1:
			case 	WIKI_RANDOM_2:
			case 	WIKI_RANDOM_3:
			case 	WIKI_RANDOM_5:				
				this.featureType = FeatureType.WIKIPEDIA_LOCAL;
				break;
			
			case WIKIPEDIA:
			case WIKIPEDIA_2011:
			case WIKIPEDIA_PT:
			case WIKIPEDIA_TEMPORAL:
			case WIKIPEDIA_TEMPORAL_ULTAV:
			case WIKIPEDIA_CONTROLE_TEMPORAL:
			case WIKIPEDIA_FA:
				this.featureType = FeatureType.WIKIPEDIA;
				break;
			case STARWARS_LABEL:
				this.featureType = FeatureType.SW_LABEL;
				break;
			case STARWARS_VOTE:
				this.featureType = FeatureType.SW_VOTE;
				break;
			case MUPPETS:
				this.featureType = FeatureType.MUPPETS;
				break;
				
		}
	}
	public View(ConfigViewColecao cnf, File arquivo,MetodoAprendizado metodoAprendizado,FeatureType objType)
	{
		this( cnf,  arquivo,metodoAprendizado);
		this.featureType = objType;
	}
	public View(ConfigViewColecao cnf, File arquivo,MetodoAprendizado metodoAprendizado)
	{
		this.arquivo = arquivo;
		this.metodoAprendizado = metodoAprendizado;
		this.nomExperimento = metodoAprendizado.getNomExperimento();
		this.cnfView =  cnf;
	}
	public void setUsarBanco(boolean usarBanco)
	{
		this.usarBanco = usarBanco;
	}
	public boolean isUsarBanco()
	{
		return this.usarBanco;
	}
	public void setAsLocal()
	{
		switch(featureType)
		{
			case TEXT:
					this.featureType = FeatureType.TEXT_LOCAL;
					break;
			case STRUCTURE:
					this.featureType = FeatureType.STRUCTURE_LOCAL;
					break;
			case STYLE:
					this.featureType = FeatureType.STYLE_LOCAL;
					break;
			case LENGTH:
					this.featureType = FeatureType.LENGTH_LOCAL;
					break;
			case READ:
					this.featureType = FeatureType.READ_LOCAL;
					break;
			case HISTORY:
					this.featureType = FeatureType.HISTORY_LOCAL;					
					break;
			case NETWORK:
					this.featureType = FeatureType.NETWORK_LOCAL;
					break;	
		}
	}
	public Resultado getResultValidacao()
	{
		return resultValidacao;
	}
	public Resultado getResultTeste() {
		return resultTeste;
	}
	public boolean isViewLocal() 
	{
		if(this.featureType == null)
		{
			return false;
		}
		return this.featureType.isLocal();
	}
	public FeatureType getFeatureType()
	{
		return this.featureType;
	}
	public void setFeatureType(FeatureType objFetType)
	{
		this.featureType = objFetType;
	}
	public void setFold(Fold[] foldsTeste)
	{
		this.foldsTeste = foldsTeste;
	}
	/**
	 * Filtro de ids de fold - caso exista, apenas estes ids seram retornados
	 * @param idsFiltro
	 */
	public void setFiltroId(ArrayList<Long> idsFiltro)
	{
		this.filtroId = idsFiltro;
	}
	/**
	 * Coleção na qual o dataset para treinar pertence
	 */
	public Colecao getColecaoDatasetView()
	{
		return this.colView;
	}
	/**
	 * Coleção na qual esta sendo testado
	 * @return
	 */
	public Colecao getColecao()
	{
		if(this.cnfView == null)
		{
			return null;
		}
		return this.cnfView.getColecao();
	}
	public ConfigViewColecao getCnfView()
	{
		return this.cnfView;
	}
	public File getArquivo()
	{
		return this.arquivo;
	}
	public MetodoAprendizado getMetodoAprendizado() {
		return metodoAprendizado;
	}	
	public String getNomExperimento() {
		return nomExperimento;
	}

	public Resultado getResultTreino() {
		return resultTreino;
	}
	public void calculaResultadoViewValidacao() throws Exception
	{
		if(resultTeste.getFolds()[0].getValidation() == null)
		{
			return;
		}
		System.out.println("************************Calcula Validacao ******************************");
		String nomExpValidacao = nomExperimento+"_validacao";
		String nomExpAnt = metodoAprendizado.getNomExperimento();
		metodoAprendizado.setNomExperimento(nomExpValidacao);
		
		//para cada visao, resgata o resultado usando o mesmo sub-treino 0 (usado no teste)
		Fold[] foldTreino = resultTreino.getFolds();
		Fold[] foldTeste = resultTeste.getFolds();
		Fold[] foldResultValidacao = new Fold[foldTreino.length];
		for(int i =0 ; i<foldTreino.length ; i++)
		{
			//cria fold
			File origem = foldTeste[i].getOrigem();
			File treino = foldTreino[i].getSubFolds()[0].getTreino();
			File teste = foldTeste[i].getValidation();
			File testeIds = foldTeste[i].getIdsValidation();
			foldResultValidacao[i] = new Fold(i,origem,treino,teste,testeIds,false);
			foldResultValidacao[i].setPredict(new File(teste.getAbsoluteFile()+"Validacao.predict"));
			
			//adiciona ids do teste, ids do teste do treino e ids da validacao
			foldResultValidacao[i].addIdsToLstSemClasse(testeIds);//ids da validacao
			foldResultValidacao[i].addIdsToLstSemClasse(foldTreino[i].getSubFolds()[0].getIdsFile());//do subtreino
			foldResultValidacao[i].addIdsToLstSemClasse(foldsTeste[i].getIdsFile());//do teste

			
			//
						
		}
		//define resultado
		foldResultValidacao = metodoAprendizado.testar(foldResultValidacao);
		
		
		countResults(foldResultValidacao,"validacao");
		//System.out.println(CalculaResultados.(resultTeste,16,null));
		resultValidacao = new Resultado(nomExpAnt,foldResultValidacao);
		//System.exit(0);
		View[] v = {this};
		resultValidacao.setView(v);
		metodoAprendizado.setNomExperimento(nomExpValidacao);
	}
	public void calculaResultadoViewTeste() throws Exception
	{
		System.out.println("************************Calcula teste ******************************");
		Colecao colecaoAnt = metodoAprendizado.getColecao();
		
		
		Fold[] folds = null;
		if(this.foldsTeste == null)
		{
			folds =  criaFoldsTeste();
		}else{
			folds = this.foldsTeste;
		}
		
		
		//caso a colecao da view atual seja diferente do dataset usar a colecao da view atual como treino (mudar o teste para a view do dataset)
		if(colView != null && !cnfView.getColecao().equals(colView))
		{
			//folds = this.metodoAprendizado.criaTenFolds(arquivo,strPrefixo+"_transf_"+colDatasetView.getSigla());
			metodoAprendizado.setColecao(cnfView.getColecao());
			
			//System.out.println("Usando dataset view diferente usando tenfold (para teste): "+cnfView.getArquivoColecao().getName());
			//cria folds com o arquivo da colecao 
			Fold[] foldsColecao = this.metodoAprendizado.criaTenFolds(cnfView.getArquivoColecao());
				
			//coloca o teste como o arquivo da colecao
			for(int i =0 ; i<folds.length ; i++)
			{
				File arqTesteColecao = foldsColecao[i].getTeste();
				File arqIDTesteColecao = foldsColecao[i].getIdsFile();
				
				//System.out.println(arqTesteColecao.getName()+" => "+folds[i].getTeste().getAbsolutePath());
				//System.out.println(arqIDTesteColecao.getName()+" => "+folds[i].getIdsFile().getAbsolutePath());
				arqTesteColecao.renameTo(folds[i].getTeste());
				arqIDTesteColecao.renameTo(folds[i].getIdsFile());
			}

			metodoAprendizado.setColecao(this.getColecaoDatasetView());
			
		}else
		{
			System.out.println("Usando dataset view igual");
		}
		
		//filtra fold se necessario
		if(this.filtroId.size()>0 || this.porcfiltroTreinoAleatorio > 0)
		{
			folds = filtraFold(folds);
		}
		
		//executa o teste
		if(View.tpoDivisao == TIPO_DIVISAO_TREINO.FOLD_VALIDACAO_NOVE_FOLDS_BALANCEADO_TESTE_IGUAL || View.tpoDivisao == TIPO_DIVISAO_TREINO.FOLD_VALIDACAO_NOVE_FOLDS_TESTE_IGUAL )
		{
			//para cada fold procurar o subfold do treino que obteve um resultado menor
			Fold[] foldTreino = resultTreino.getFolds();
			for(int i =0 ; i<folds.length ; i++)
			{
				Fold objFoldTreino = foldTreino[i];
				Fold[] subFoldTreino = objFoldTreino.getSubFolds();
				
				//resgata o fold treino 0
				Fold objSubFoldMseMin = subFoldTreino[0];//Fold.getMinMSEFold(subFoldTreino);
				

				
				
				//troca arquivo de fold treino
				folds[i].setTreino(objSubFoldMseMin.getTreino());
				folds[i].setIdsTreinoFile(objSubFoldMseMin.getIdsTreinoFile());
				
				//adiciona ids do teste, ids do teste do treino e ids da validacao
				if(folds[i].getIdsValidation() != null)
				{
					folds[i].addIdsToLstSemClasse(folds[i].getIdsValidation());
				}
				
				folds[i].addIdsToLstSemClasse(folds[i].getIdsFile());
				folds[i].addIdsToLstSemClasse(objSubFoldMseMin.getIdsFile());
				//System.out.println("Arquivo treino do teste no fold "+i+": "+objSubFoldMseMin.getTreino().getAbsolutePath());
			}
			metodoAprendizado.setNomExperimento(nomExperimento+"_TamIgualTreino");
			resultTeste = calculaResultadoTesteFold(colecaoAnt, folds,metodoAprendizado,nomExperimento+"_TamIgualTreino");
			
			
		}else
		{
			resultTeste = calculaResultadoTesteFold(colecaoAnt, folds,metodoAprendizado,nomExperimento);	
		}
		
		
		
		//System.exit(0);
		View[] v = {this};
		resultTeste.setView(v);
		if(this.metodoAprendizado.isClassificacao())
		{
			System.out.println(CalculaResultados.resultadoClassificacaoToString(resultTeste,16,null));
		}else
		{
			if(! (this.metodoAprendizado instanceof GenericoLetorLike))
			{
				System.out.println(CalculaResultados.resultadoRegressaoToString(resultTeste,null));	
			}
		}
		
		
		
		
	}
	public boolean isBoostView()
	{
		return this.boostView;
	}
	
	public void setBoostView(boolean boost)
	{
		this.boostView = boost;
	}
	private Fold[] criaFoldsTeste() throws Exception {
		Fold[] folds = null;
		
		this.metodoAprendizado.setNomExperimento(nomExperimento);
		String strPrefixo = this.metodoAprendizado.getNomBase(arquivo);
		//cria folds de teste
		if(this.getColecaoDatasetView() != null)
		{
			metodoAprendizado.setColecao(this.getColecaoDatasetView());	
		}
		folds = this.metodoAprendizado.criaTenFolds(arquivo,strPrefixo);
		
		this.foldsTeste = folds;
		return folds;
	}
	public static Resultado calculaResultadoTesteFold(Colecao colecaoAnt, Fold[] folds,MetodoAprendizado metodoAprendizado,String nomExperimento)
			throws Exception
	{
		Fold[] foldsAntigo = folds;

		
		folds = metodoAprendizado.testar(folds);
		
		//atualiza o fold caso necessario
		PreparedStatement pmtUpdate = null;
		for(int i =0 ; i<folds.length ; i++)
		{
			if(folds[i].getValidation() == null && foldsAntigo[i].getValidation() != null)
			{
				folds[i].setValidationFiles(foldsAntigo[i].getValidation(), foldsAntigo[i].getIdsValidation());
				
				//grava no banco
				try {
					if(pmtUpdate == null)
					{
						pmtUpdate = GerenteBD.getGerenteBD().obtemConexao("").prepareStatement("update wiki_results.fold set end_validacao = ?, end_validacao_ids = ? where id = ?");
					}
					pmtUpdate.setString(1, foldsAntigo[i].getValidation().getAbsolutePath());
					pmtUpdate.setString(2, foldsAntigo[i].getIdsValidation().getAbsolutePath());
					pmtUpdate.setInt(3, folds[i].getId());
					pmtUpdate.executeUpdate();
					
					
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
			

		}
		
		metodoAprendizado.setColecao(colecaoAnt);
		
		//coloca resultado
		Resultado resultTeste = new Resultado(nomExperimento,folds);
		
		countResults(folds,"teste");
		return resultTeste;
	}
	private static void countResults(Fold[] folds ,String nameResult)
	{
		int numResults = 0;
		for(int i = 0; i<folds.length ; i++)
		{
			int num = folds[i].getResultadosValues().size();
			System.out.println("FOLD ("+nameResult+") #"+i+" Num results: "+num+"  File Teste: "+folds[i].getTeste().getAbsolutePath()+"  File Predict: "+folds[i].getPredict().getAbsolutePath());
			numResults += num;
		}
		System.out.println("TOTAL: "+numResults);
	}
	private static void criaFoldViewByIds(Fold[] folds,File arqToSplit, int i, List<Long> lstIds,String type,MetodoAprendizado metodoAprendizado,String nomExperimento,Colecao col,View v,List<String> linhasIdsAdicional,String idGrouper)
			throws IOException, Exception
	{
		File arqOrigemMetodo = metodoAprendizado.getArquivoOrigem();
		String nomExperimentoAnterior = metodoAprendizado.getNomExperimento();
		
		//create source
		File diretorio = new File(arqOrigemMetodo.getParent()+"/views"+(col!=null?"_"+col.getSigla():""));
		if(!diretorio.exists())
		{
			diretorio.mkdir();
		}
		//int tamAmostra = metodoAprendizado.getSourceIds().size();

		File sourceFoldFile = new File(diretorio,nomExperimento+"_"+type+"_fold"+i+".amostra");
		metodoAprendizado.filtraArquivoPorIds(lstIds,sourceFoldFile,linhasIdsAdicional);
		metodoAprendizado.setArquivoOrigem(sourceFoldFile);
		
		
		//cria folds de tese e faz fold crossvalidation
		Fold[] foldsView = null;
		switch(tpoDivisao)
		{
			case THREE_FOLD:
				foldsView = metodoAprendizado.dividePerFoldsByIds(lstIds, "_"+nomExperimento+"_"+type+"_fold_threeFold_"+i, "foldsView", NUM_FOLDS_VIEW,linhasIdsAdicional);
				metodoAprendizado.setNomExperimento("_"+nomExperimento+"_"+type+"_fold_threeFold_"+i);
				break;
			case FOLD_VALIDACAO_NOVE_FOLDS:
			case FOLD_VALIDACAO_NOVE_FOLDS_TESTE_IGUAL:
				foldsView = metodoAprendizado.criaFoldsTeste(folds[i].getIdsTreinoFile(), folds.length-1, false,	"id", idGrouper, false);;
				//metodoAprendizado.dividePerFoldsByIds(lstIds, "_"+nomExperimento+"_"+type+"_fold_validation_"+i, "foldsView",9, (int) (tamAmostra/(double)10));
				metodoAprendizado.setNomExperimento("_"+nomExperimento+"_"+type+"_fold_validation_"+i);
				break;
			case FOLD_VALIDACAO_NOVE_FOLDS_BALANCEADO:
			case FOLD_VALIDACAO_NOVE_FOLDS_BALANCEADO_TESTE_IGUAL:
				/*
				if(!v.isViewLocal())
				{
					foldsView = metodoAprendizado.dividePerFoldsByIdsClasse(metodoAprendizado.getSourceIdClass(), "_"+nomExperimento+"_"+type+"_fold_validation_"+i, "foldsView",9, (int) (tamAmostra/(double)10));
					metodoAprendizado.setNomExperimento("_"+nomExperimento+"_"+type+"_fold_"+i);
				}else
				{
					System.out.println("View local nao sera balanceada!");
					*/
					File arqFonte = metodoAprendizado.getArquivoOrigem();
					metodoAprendizado.setArquivoOrigem(folds[i].getTreino());
					foldsView = metodoAprendizado.criaFoldsTeste(arqToSplit, folds.length-1, false,	"id", idGrouper, false);
					metodoAprendizado.setArquivoOrigem(arqFonte);
					//foldsView = metodoAprendizado.dividePerFoldsByIds(lstIds, "_"+nomExperimento+"_"+type+"_fold_"+i, "foldsView",folds.length-1, (int) (tamAmostra/(double)folds.length));
					metodoAprendizado.setNomExperimento("_"+nomExperimento+"_"+type+"_fold_"+i);					
				//}
				break;
		}
		//Fold[] foldsView = metodoAprendizado.dividePerFoldsByIds(lstIds, nomExperimento+"_"+type+"_fold"+i, "foldsView", NUM_FOLDS_VIEW);
		//metodoAprendizado.setGetResultPreCalculado(true);
		
		
		if(TESTAR_TREINO)
		{
			//para cada fold do view, colocar o fold de validacao (caso exista)

			long time = System.currentTimeMillis();
			for(int f =0; f<foldsView.length ; f++)
			{
				foldsView[f].setValidationFiles(folds[i].getValidation(), folds[i].getIdsValidation());
				
				foldsView[f].addIdsToLstSemClasse((Set<Integer>) folds[i].getIdsSemClasse());
				if(folds[i].getIdsValidation() != null)
				{
					foldsView[f].addIdsToLstSemClasse(folds[i].getIdsValidation());
				}
				foldsView[f].addIdsToLstSemClasse(foldsView[f].getIdsFile());
			}
			foldsView = metodoAprendizado.testar(foldsView);

			System.out.println("Calculado em: "+(System.currentTimeMillis()-time)/1000.0+" segundos");
		}
		folds[i].setSubFolds(foldsView);
		
		//add results on fold
		for(int j =0 ; j< foldsView.length ; j++)
		{
			folds[i].setResultados(foldsView[j].getResultadosValues(), false);
			//System.out.println("*****Resultado do 6320532: "+foldsView[j].getResultadoPorId(6320532L));
		}
		metodoAprendizado.setArquivoOrigem(arqOrigemMetodo);
		System.out.println("NUM de RESULTADOS: "+folds[i].getResultadosValues().size());
		
		metodoAprendizado.setNomExperimento(nomExperimentoAnterior);
		
	}
	public double getValueResultTreino() throws SQLException
	{
		return Fold.getResultFold(foldsTreino, metodoAprendizado.isClassificacao());
	}
	public double getValueResultTreino(int foldIdx) throws SQLException
	{
		return Fold.getResultFold(foldsTreino[foldIdx], metodoAprendizado.isClassificacao());
	}
	public void calculaResultadoViewTreino(String idGrouper) throws Exception
	{		
		System.out.println("************************Calcula treino******************************");
		Colecao colecaoAnt = metodoAprendizado.getColecao();
		Fold[] foldsTeste = null;
		if(this.foldsTeste == null)
		{
			foldsTeste = criaFoldsTeste();
		}else
		{
			foldsTeste = this.foldsTeste;
		}
		
		 
		if(foldsTeste == null )
		{
			throw new Exception("Fold de teste nao criado");
		}
		Fold[] foldsTreino = new Fold[foldsTeste.length];
		
		//verifica a forma de fazer os folds
		if(colView != null && !cnfView.getColecao().equals(colView))
		{
			
			/**** Folds de coleção diferente **/
			//cria folds com o arquivo da colecao 
			
			foldsTreino = testaTreinoPorColecao(foldsTeste, foldsTreino);
			System.out.println("##########==============+> Fold diferente folds ids resultado: "+foldsTreino[0].getResultadosValues().size());
			//System.exit(0);
		}else
		{
			System.out.println("Fold igual");
			treinarPor3Folds(foldsTeste, foldsTreino,idGrouper);
		}
		if(boostView && foldsTreino != null)
		{
			doBoosting(foldsTreino);
		}

		//filtra fold se necessario
		if(this.filtroId.size()>0)
		{
			foldsTreino = filtraFold(foldsTreino);
		}
		this.foldsTreino = foldsTreino;
		
		metodoAprendizado.setColecao(colecaoAnt);

		
		resultTreino = new Resultado(nomExperimento+"_treino_fold",foldsTreino);
		if(this.metodoAprendizado.isClassificacao())
		{
			
			//System.out.println(CalculaResultados.resultadoClassificacaoToString(resultTreino,metodoAprendizado.getNumClasses(),new File (ResultadosWikiMultiviewMetodos.DIR_RESULT,nomExperimento+"_treino")));
		}else
		{
			//System.out.println(CalculaResultados.resultadoRegressaoToString(resultTreino,new File (foldsTeste[0].getTreino().getParent(),nomExperimento+"_treino")));	
		}
		//System.exit(0);
		
		
		View[] v = {this};
		resultTreino.setView(v);

	}
	public void doBoosting(Fold[] foldsTreino) throws IOException, SQLException {
		
		
		
			//cria folds para nova iteração, replica folds de folds que o resultado convergiu...até todos os folds convergirem
			//cria folds para nova visão, com os ids errados duplicados
			Fold[] foldsNewView = new Fold[this.foldsTeste.length];
			boolean convergiuTodos = true;
			for(int i =0 ; i<foldsNewView.length ; i++)
			{
				double resultFold = Fold.getResultFold(foldsTreino[i], metodoAprendizado.isClassificacao());
				Fold fTesteOld = this.foldsTeste[i];
				File arqTreino = fTesteOld.getTreino();
				List<String> newLines = new ArrayList<String>();
				if(this.parentView != null)
				{
					double ultResult = this.parentView.getValueResultTreino(i);
					double diff = Math.abs(this.parentView.getValueResultTreino(i)-resultFold);
					System.out.println("Ultimo resultado: "+ultResult+"  Resultado atual: "+resultFold+" diff:"+diff);
				}
				//checa convergencia 
				if(this.parentView == null || Math.abs(this.parentView.getValueResultTreino(i)-resultFold) > Math.pow(10, -2) )
				{
					

					//para cada fold, resgata os ids com erro
					List<Long> lstIdsErro = new ArrayList<Long>();
					for(ResultadoItem r : foldsTreino[i].getResultadosValues())
					{
						if(r.getClassePrevista() != r.getClasseReal())
						{
							lstIdsErro.add(r.getId());
						}
					}
					
					//faz nome
					int numBoost = 0;
					String nome = fTesteOld.getNomeBase()+"_"+i;
					int idxBoost = nome.indexOf("Boost");
					if(idxBoost > 0)
					{
						String num = "";
						int j = idxBoost+5;
						while(nome.charAt(j) >= '0' && nome.charAt(j) <= '9')
						{
							num += nome.charAt(j); 
							j++;
						}
						numBoost = Integer.parseInt(num)+1;
					}
					nome = nome.replaceAll("Boost[0-9]+", "")+"Boost"+numBoost;
					
					//cria arquivo de treino
					arqTreino = new File(fTesteOld.getTeste().getParentFile(),nome);
					metodoAprendizado.filtraArquivoPorIds(fTesteOld.getIdsTreino(), arqTreino);
					newLines = metodoAprendizado.criaArquivoDuplicandoInstanciasDeIds(lstIdsErro, arqTreino, arqTreino);
					
					convergiuTodos = false;
				}
				
				foldsNewView[i] = new Fold(fTesteOld.getNum(),fTesteOld.getOrigem(),arqTreino,fTesteOld.getTeste(),fTesteOld.getIdsFile(),fTesteOld.getIdsTreinoFile());
				//adiciona as antigas
				foldsNewView[i].addLstLinhasAdicionaisTreino(fTesteOld.getLstLinhasAdicionaisTreino());
				//e as novas linhas de treino
				foldsNewView[i].addLstLinhasAdicionaisTreino(newLines);
				
			}
			
			//cria nova visão caso nao tenha convergido todos os folds
			if(!convergiuTodos)
			{
				View v = new View(null,this.getArquivo(),metodoAprendizado);
				v.setFold(foldsNewView);
				v.setBoostView(true);
				childViews.add(v);
				v.setParentView(this);
			}
	}

	public List<View> getChildViews()
	{
		return this.childViews;
	}
	private void setParentView(View view) {
		// TODO Auto-generated method stub
		this.parentView = view;
		
	}
	private void treinarPor3Folds(Fold[] foldsTeste, Fold[] foldsTreino,String idGrouper) throws IOException, Exception
	{
		Colecao col = null;
		if(this.cnfView != null)
		{
			col = this.cnfView.getColecao();
		}
		treinarPor3Folds(foldsTeste, foldsTreino,metodoAprendizado,this.nomExperimento,col,this,idGrouper);
	}
	public static void treinarPor3Folds(Fold[] foldsTeste, Fold[] foldsTreino,MetodoAprendizado metodoAprendizado,String nomExperimento,Colecao col,View v,String idGrouper)
			throws IOException, Exception
	{
		/**** Folds para mesm coleção (fazer 3 fold cross validation) **/
		//executa teste, para cada fold dividindo cada fold em mais 3 folds
		//calcula ja o resultado do treino por fold
		for(int i=0 ; i<foldsTeste.length ; i++)
		{
			//resgata ids de treino
			List<Long> lstIdsTeste = foldsTeste[i].getIdsTeste();
			List<Long> lstIdsTreino = metodoAprendizado.getSourceIds();
			lstIdsTreino.removeAll(lstIdsTeste);
			
			
			           
			foldsTreino[i] = new Fold(i,foldsTeste[i].getNomeBase(),new ArrayList<ResultadoItem>());
			
			
			
			foldsTreino[i].setValidationFiles(foldsTeste[i].getValidation(),foldsTeste[i].getIdsValidation());
			if(foldsTeste[i].getIdsValidation() != null)
			{
				foldsTreino[i].addIdsToLstSemClasse(foldsTeste[i].getIdsValidation());
			}
			foldsTreino[i].addIdsToLstSemClasse(lstIdsTeste);
			
			criaFoldViewByIds(foldsTreino,foldsTeste[i].getTreino(), i, lstIdsTreino,"treino",metodoAprendizado,nomExperimento,col,v,foldsTeste[i].getLstLinhasAdicionaisTreino(),idGrouper);
		}
	}
	private Fold[] testaTreinoPorColecao(Fold[] foldsTeste, Fold[] foldsTreino)
			throws Exception, IOException
	{
		Fold[] foldsColecao = this.metodoAprendizado.criaTenFolds(cnfView.getArquivoColecao());
			
		//coloca aplica o teste da coleção no dataset da view
		for(int i =0 ; i<foldsTeste.length ; i++)
		{
			//resgata ids de treino
			List<Long> lstIdsTeste = foldsColecao[i].getIdsTeste();
			List<Long> lstIdsTreino = metodoAprendizado.getSourceIds();
			lstIdsTreino.removeAll(lstIdsTeste);
		
			
			//cria arquivo folds
			foldsTreino[i] = new Fold(i,metodoAprendizado.getArquivoOrigem(),foldsTeste[i].getTreino(),foldsColecao[i].getTreino(),foldsColecao[i].getIdsTreinoFile());
			System.out.println("TREINO: treino:"+foldsTreino[i].getTreino().getAbsolutePath()+" Teste:"+foldsTreino[i].getTeste().getAbsolutePath());
		}
		System.out.println("Treino: "+nomExperimento+"_treino");
		//System.exit(0);
		this.metodoAprendizado.setNomExperimento(nomExperimento+"_treino");
		this.metodoAprendizado.setColecao(this.getColecaoDatasetView());
		foldsTreino = this.metodoAprendizado.testar(foldsTreino);
		return foldsTreino;
	}
	public void setFiltroAleatorioTreino(double porcEliminacao, long seed)
	{
		this.porcfiltroTreinoAleatorio = porcEliminacao;
		this.seedTreinoAleatorio = seed;
	}
	private Fold[] filtraFold(Fold[] folds) throws IOException {
		
			Fold[] foldFiltrado = new Fold[folds.length];
			for(int i =0 ;i<folds.length ; i++)
			{
				List<Long> idsFoldTreino = folds[i].getIdsTreino();
				List<Long> idsFoldTeste = folds[i].getIdsTeste();
				idsFoldTreino.removeAll(idsFoldTeste);
				if(this.filtroId.size() > 0 )
				{
					idsFoldTreino.retainAll(this.filtroId);
					idsFoldTeste.retainAll(this.filtroId);
				}
				
				if(porcfiltroTreinoAleatorio > 0)
				{
					idsFoldTreino = Fold.divideIntoFolds(2, idsFoldTreino, (int)Math.ceil(idsFoldTreino.size()*(1-porcfiltroTreinoAleatorio)),seedTreinoAleatorio)[0];
				}
				
				foldFiltrado[i] = metodoAprendizado.criaFoldComIdsFiltrado(folds[i].getNum(),idsFoldTreino,this.nomExperimento+"/filtro",idsFoldTeste);
			}
			System.out.println("=======================>SEED DESTA VIEW: "+seedTreinoAleatorio);
			return foldFiltrado;
		
	}
	
	
	public void setResultado(Resultado resultTreino,Resultado resultTeste)
	{
		this.resultTreino = resultTreino;
		this.resultTeste = resultTeste;
	}
	
	public String toString()
	{
			return this.nomExperimento;
	}
	
	

}
