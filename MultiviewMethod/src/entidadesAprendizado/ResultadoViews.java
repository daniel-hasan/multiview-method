package entidadesAprendizado;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import string.StringUtil;
import aprendizadoResultado.CalculaResultados;
import aprendizadoUtils.MetodoAprendizado;
import aprendizadoUtils.SVM;
import arquivo.ArquivoUtil;
import config_tmp.ConfigViewColecao;

public class ResultadoViews implements Serializable {
	public static boolean READ_OBJECT = false;
	public static String CONFERENCE_EXP = CONFERENCE_EXPERIMENTO.QA_MULTIVIEW.toString().toLowerCase();
	public static TIPO_SUB_EXPERIMENTO TPO_SUB_EXPERIMENTO = TIPO_SUB_EXPERIMENTO.SEIS_VISOES;
	
	public static boolean USE_CATEGORY = false;
	private static ResultadoViews objViewLast;
	public static boolean USAR_RESULT_TESTE = false;
	private static String NOM_ARQUIVO_VIEWS = null;
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 2L;
	private Fold[][] resultadoPorViewTreino;
	private Fold[][] resultadoPorViewTeste;
	private Fold[][] resultadoPorViewValidacao;
	public String idGrouper = "id";
	private View[] views;
	private List<View> lstProducedViews = new ArrayList<View>();
	private MetodoAprendizado metSeletor;
	
	public enum CONFERENCE_EXPERIMENTO {
		VANDAL,
		JCDL12_r01,JCDL12_r02,
		JCDL12_r03,
		JCDL12_r04,
		YOUTUBE_KNN,
		YOUTUBE_SVR,
		YOUTUBE_SVR_KNN,
		YOUTUBE_TODOS,YOUTUBE_TODOS_SVR,YOUTUBE_TODOS_KNN,
		QA_MULTIVIEW,
		;
	};
	
	public enum TIPO_SUB_EXPERIMENTO {
			SEIS_VISOES,TRES_VISOES;
	};
	
	/**
	 * Gerar features do artigo no metodo de seleção
	 */
	private boolean gerarFeaturesMetSeletor = false;
	public ResultadoViews(View[] views, MetodoAprendizado metSeletor,
			boolean gerarFeaturesSeletor) throws Exception
	{
		this(views,metSeletor,gerarFeaturesSeletor,"id");
	}
	public ResultadoViews(View[] views, MetodoAprendizado metSeletor,
			boolean gerarFeaturesSeletor,String idGrouper) throws Exception {
		super();
		this.views = views;
		this.gerarFeaturesMetSeletor = gerarFeaturesSeletor;
		this.metSeletor = metSeletor;
		this.idGrouper = idGrouper;
		if(views != null)
		{
			criarResultadosPorView(views, metSeletor);
		}

	}

	public ResultadoViews(View[] views, MetodoAprendizado metSeletor)
			throws Exception {
		super();
		this.views = views;
		this.metSeletor = metSeletor;
		if(views!=null)
		{
			criarResultadosPorView(views, metSeletor);
		}

	}
	public void setIdGrouper(String idGrouper)
	{
		this.idGrouper = idGrouper;
	}
	public static void setNomArquivoView(String view)
	{
		NOM_ARQUIVO_VIEWS = view;
	}
	/**
	 * Cria resultado por view
	 * o
	 * 
	 * @param views
	 * @param metSeletor
	 * @throws Exception
	 */
	public void criarResultadosPorView(View[] views,
			MetodoAprendizado metSeletor) throws Exception {
		
		// calcula para cada view o resultado
		List<View> lstChildViews = new ArrayList<View>();
		for (int i = 0; i < views.length; i++) {
			
			System.out.println("====================================View: "+i+"========================================================");
			views[i].calculaResultadoViewTreino(idGrouper);
			views[i].calculaResultadoViewTeste();
			views[i].calculaResultadoViewValidacao();
			lstChildViews.addAll(views[i].getChildViews());
			
			if(USAR_RESULT_TESTE)
			{
				Fold[] fldResultTest = views[i].getResultTeste().getFolds();
				Fold[] fldResultTreino = views[i].getResultTreino().getFolds();
				
				//zera results do treino
				for(int f_tr = 0 ; f_tr<fldResultTreino.length ; f_tr++ )
				{
					fldResultTreino[f_tr].limpaResultados();
				}
				
				//coloca o resultado do teste no treino (exceto no mesmo fold
				for(int f = 0; f<fldResultTest.length ; f++)
				{
					for(int f_tr = 0 ; f_tr<fldResultTreino.length ; f_tr++ )
					{
						if(f != f_tr)
						{
							fldResultTreino[f_tr].setResultados(fldResultTest[f].getResultadosValues());
						}
					}
				}
			}
		}
		
		//adiciona as visões adicionadas pelas visões pre-processadas e todas as que serão adicionadas em consequencia
		while(lstChildViews.size()>0)
		{
			View v = lstChildViews.remove(0);
			v.calculaResultadoViewTreino(idGrouper);
			v.calculaResultadoViewTeste();
			
			View[] arrNewViews = new View[views.length+1];
			for(int i = 0 ; i<views.length ;i++)
			{
				arrNewViews[i] = views[i];
			}
			arrNewViews[views.length] = v;
			views = arrNewViews;
			
			
			lstChildViews.addAll(v.getChildViews());
		}
		
		// caso exista um metodo para selecionar a view ideal, cria o dataset
		// onde a clase é a view que obteve o melhor resultado
		if (metSeletor != null) {
			metSeletor.mapeiaIdPorLinha(views[0].getColecao()
					.getArquivoOrigem());

			// gerar treino do seletor e define na view o treino
			Fold[] foldSelViews = gerarFoldsSelecionadorView(views, metSeletor);

			// defineConfiancaView(foldSelViews, metSeletor, views);
		}

		// prepara os folds por view/
		preparaFoldsPorView(views);

		escreverObjeto();
	}

	public void preparaFoldsPorView(View[] views) {
		this.views = views;
		int numFolds = views[0].getResultTeste().getFolds().length;

		resultadoPorViewTreino = new Fold[numFolds][views.length];
		resultadoPorViewTeste = new Fold[numFolds][views.length];
		resultadoPorViewValidacao = new Fold[numFolds][views.length];
		for (int foldNum = 0; foldNum < numFolds; foldNum++) {

			// adiciona os folds com o resultado
			for (int viewNum = 0; viewNum < views.length; viewNum++) {
				// System.out.println("Teste: "+views[viewNum].getResultTreino().getFolds()[foldNum].getTeste().getName()+" Numero de folds: "+numFolds+" num de folds resultadoView: "+resultadoPorViewTreino.length+" numFolds Result"+views[viewNum].getResultTreino().getFolds().length);
				resultadoPorViewTreino[foldNum][viewNum] = views[viewNum]
						.getResultTreino().getFolds()[foldNum];
				resultadoPorViewTeste[foldNum][viewNum] = views[viewNum]
						.getResultTeste().getFolds()[foldNum];
				
				if(views[viewNum].getResultValidacao() != null)
				{
					resultadoPorViewValidacao[foldNum][viewNum] = views[viewNum].getResultValidacao().getFolds()[foldNum];
				}
			}
		}
	}

	public void escreverObjeto() throws FileNotFoundException, IOException {
		String nomArquivo = null;
		if(NOM_ARQUIVO_VIEWS != null)
		{
			nomArquivo = NOM_ARQUIVO_VIEWS;
		}else
		{
			nomArquivo = getCannonicalNameViews(this.views);	
		}
		
		
				
		File arqObj = getArquivoObj(nomArquivo);
		ObjectOutputStream arqOutput = new ObjectOutputStream(
				new FileOutputStream(arqObj));

		arqOutput.writeObject(this);
		arqOutput.close();
	}

	public View[] getViews() {
		return this.views;
	}

	public static String getCannonicalNameViews(View[] arrViews) {
		String views = "";
		int idxViewColecao = 0;
		for (int i = 0; i < arrViews.length; i++) {
			if (arrViews[i].getCnfView() != null) {
				if (arrViews[i].getColecaoDatasetView() == arrViews[i]
						.getColecao()) {
					idxViewColecao = i;
				}
			}
		}
		views += arrViews[idxViewColecao].getNomExperimento()+ (USAR_RESULT_TESTE?"_usarTeste":"")+"_"+TPO_SUB_EXPERIMENTO.toString().toLowerCase()+"_"+View.tpoDivisao.toString().toLowerCase();
		if(arrViews[idxViewColecao].getCnfView() != null)
		{
			switch (arrViews[idxViewColecao].getCnfView()) {
			case WIKIPEDIA_CULTURE:
				views += "_culture";
				break;
			case WIKIPEDIA_GEOGRAPHY:
				views += "_geography";
				break;
			case WIKIPEDIA_HISTORY:
				views += "_history";
				break;
			case WIKIPEDIA_SCIENCE:
				views += "_science";
				break;
			case WIKIPEDIA_RANDOM_1:
				views += "_Random_1";
				break;				
			case WIKIPEDIA_RANDOM_2:
				views += "_Random_2";
				break;				
			case WIKIPEDIA_RANDOM_3:
				views += "_Random_3";
				break;				
			case WIKIPEDIA_RANDOM_5:
				views += "_Random_5";
				break;	
			case WIKIPEDIA_CAT:
				views += "_cat_";
				break;
			}
		}
		
		for (int i = 0; i < arrViews.length; i++) {
			if (i != idxViewColecao) {
				String nomViewAtual = arrViews[i].getNomExperimento();
				if(arrViews[i].getSglView() != null)
				{
					nomViewAtual = (arrViews[i].getSglView());
				}
				String prefix = StringUtil.stringEqualPrefix(views,
						nomViewAtual);
				nomViewAtual = nomViewAtual.replaceAll(prefix, "");

				views += "_" + nomViewAtual;
			}

		}
		if(views.length() > 50)
		{
			String[] words = views.split("_");
			for(String palavra : words)
			{
				while(StringUtil.countOccorencias(views, "_"+palavra+"_")>=2)
				{
					views = views.replaceFirst("_"+palavra+"_", "_");
				}
			}
		}
		
		System.out.println("NOME: "+CONFERENCE_EXP+views);
		//System.exit(0);
		return CONFERENCE_EXP+views;
	}
	public static ResultadoViews getResultadoViewsObject(View[] views, MetodoAprendizado metSeletor) throws Exception
	{
		return getResultadoViewsObject(views,  metSeletor,"id");
	}
	public static ResultadoViews getResultadoViewsObject(View[] views, MetodoAprendizado metSeletor,String idGrouper) throws Exception {
		try {
			
			
			String nameArquivo = "";
			if(NOM_ARQUIVO_VIEWS != null)
			{
				nameArquivo = NOM_ARQUIVO_VIEWS;
			}else
			{
				nameArquivo = getCannonicalNameViews(views);	
			}
			/*
			if(objViewLast != null && nameArquivo.equals(getCannonicalNameViews(objViewLast.getViews())))
			{
				System.err.println("Resgatando mesmo objeto!");
				return objViewLast;
			}
			*/
			boolean gerarFeaturesMetSeletor = true;
			
			
			for(int i  = 0; i<10 ; i++)
			{
				System.gc();
			} 
			File arqObject = getArquivoObj(nameArquivo);
			System.out.println("****Arquivo: " + arqObject.getAbsolutePath() + "  *********");
			
			if ((!READ_OBJECT) || !arqObject.exists()) {
				return new ResultadoViews(views, metSeletor,
						gerarFeaturesMetSeletor,idGrouper);
			} else {
				
				ResultadoViews rv = null;
				try {
					ObjectInputStream arqInput = new ObjectInputStream(
								new FileInputStream(arqObject));
					rv = (ResultadoViews) arqInput.readObject();
					arqInput.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					System.out.println("ARQUIVO: "+arqObject.getAbsolutePath());
				}
				View[] v = rv.getViews();

				for (int i = 0; i < v.length; i++) {
					System.out.println();
					if (v[i].getMetodoAprendizado().isClassificacao()) {
						System.out.println(CalculaResultados
								.resultadoClassificacaoToString(
										v[i].getResultTreino(),
										v[i].getMetodoAprendizado()
												.getNumClasses(),
										new File(new File("resultados"), v[i].getNomExperimento()+"_"+v[i].getFeatureType() + "_treino")));
						System.out
								.println(CalculaResultados.resultadoClassificacaoToString(
										v[i].getResultTeste(),
										v[i].getMetodoAprendizado()
												.getNumClasses(),
										new File(new File("resultados"), v[i].getNomExperimento()+"_"+v[i].getFeatureType() + "_teste")));

					} else {
						/*
						System.out.println(CalculaResultados
								.resultadoRegressaoToString(v[i]
										.getResultTreino(), new File(new File(
										"resultados"), v[i].getNomExperimento()
										+ "_treino")));
						System.out.println(CalculaResultados
								.resultadoRegressaoToString(v[i]
										.getResultTeste(), new File(new File(
										"resultados"), v[i].getNomExperimento()
										+ "_teste")));
										*/
					}
				}
				objViewLast = rv;
				return rv;

			}

		} catch (NotSerializableException e) {
			System.out.println("A entidade " + e.getMessage()
					+ " nao foi serializada");
			return null;
		}
	}

	private static File getArquivoObj(String nomExperimento) {
		// create folder, if not exists
		File arqDirObject = new File("objResultadoViews");
		if (!arqDirObject.exists()) {
			arqDirObject.mkdir();
		}

		// tries to read object file
		File arqObject = new File(arqDirObject, nomExperimento + ".obj");
		return arqObject;
	}

	/**
	 * Gera dataset de resultado e seu resultado de ids
	 * 
	 * @param v
	 * @param numFold
	 * @param gerarTeste
	 * @return
	 * @throws Exception
	 */
	public String[] gerarDatasetSelecionadorView(View[] v, int numFold,
			boolean gerarTeste, MetodoAprendizado metSeletor) throws Exception {

		// para cada resultado, gera uma linha de dataset
		StringBuilder dataset = new StringBuilder();
		StringBuilder ids = new StringBuilder();
		int numResultados;
		if (gerarTeste) {
			numResultados = v[0].getResultTeste().getFolds()[numFold]
					.getNumResults();
		} else {
			numResultados = v[0].getResultTreino().getFolds()[numFold]
					.getNumResults();

		}
		FeatureNormalizer norm = new FeatureNormalizer(v[0].getColecao()
				.getMinMaxClass());

		for (int i = 0; i < numResultados; i++) {
			// resgata a classe atraves do menor erro e as features que sao o
			// resultado por view
			int classe = -1;
			long id = -1;
			double minErro = Double.MAX_VALUE;

			long idResult = -1;

			// get the id
			if (gerarTeste) {
				idResult = v[0].getResultTeste().getFolds()[numFold]
						.getResultadosValues().get(i).getId();
			} else {
				idResult = v[0].getResultTreino().getFolds()[numFold]
						.getResultadosValues().get(i).getId();
			}

			// get the features
			String[] features = null;
			String[] featArticle = new String[0];
			if (this.gerarFeaturesMetSeletor) {
				featArticle = this.metSeletor
						.getFeatVectorLinhaMapeada(idResult);
				features = new String[v.length + featArticle.length];
			} else {
				features = new String[v.length];
			}

			for (int j = 0; j < v.length; j++) {
				ResultadoItem r = null;
				if (gerarTeste) {
					r = v[j].getResultTeste().getFolds()[numFold]
							.getResultadoPorId(idResult);
				} else {
					r = v[j].getResultTreino().getFolds()[numFold]
							.getResultadoPorId(idResult);
				}
				if (r != null) {
					if (r.getErro() < minErro) {
						minErro = r.getErro();
						classe = j;
					}
					features[j] = Double.toString(norm.normValue(r
							.getClassePrevista()));
					// verifica se o id esta ok
					if (id == -1) {
						id = r.getId();
					} else {
						if (id != r.getId()) {
							throw new Exception(
									"Erro: ID de views incompativeis");
						}
					}

				} else {
					System.err.println("ID: " + idResult + " Não encontrado");
				}

			}
			/*** Adds feature article vector **/
			if (this.gerarFeaturesMetSeletor) {

				for (int j = v.length; j < features.length; j++) {
					features[j] = featArticle[j - v.length];
				}
			}

			dataset.append(metSeletor.gerarLinhaDataset(classe, (int) id,
					features) + "\n");
			ids.append(id + "\n");
		}
		String[] result = { dataset.toString(), ids.toString() };
		return result;

	}

	/**
	 * Cria um dataset para que se aprenda qual eh a melhor view a ser
	 * selecionada Para cada instancia, as features seriam os resultados de cada
	 * view, a classe é a view que obtem o menor erro de predição
	 * 
	 * @param v
	 * @return
	 * @throws Exception
	 */
	public Fold[] gerarFoldsSelecionadorView(View[] v,
			MetodoAprendizado metSeletor) throws Exception {
		String fileName = "folds_" + v[0].getColecao().getSigla();
		if (v[0].getColecaoDatasetView() != null) {
			fileName += "_" + v[0].getColecaoDatasetView().getSigla();
		}
		Fold[] foldsGerados = new Fold[v[0].getResultTeste().getFolds().length];
		File diretorio = new File(
				"/data/experimentos/multiview/amostras/seletorIdeal/");
		File diretorioColecao = new File(
				"/data/experimentos/multiview/amostras/seletorIdeal/folds_"
						+ fileName);
		if (!diretorio.exists()) {
			diretorio.mkdir();
		}
		if (!diretorioColecao.exists()) {
			diretorioColecao.mkdir();
		}
		System.out.println("Seletor view gravado em: "
				+ diretorioColecao.getAbsolutePath());
		File arqFonte = new File(diretorio.getAbsoluteFile() + "/"
				+ v[0].getColecao().getSigla()+ ".amostra");

		for (int i = 0; i < foldsGerados.length; i++) {

			String nomeBaseArquivo = diretorioColecao.getAbsolutePath() + "/"
					+ v[0].getColecao().getSigla();
			File arqTreino = new File(nomeBaseArquivo + i + ".treino");
			File arqTeste = new File(nomeBaseArquivo + i + ".teste");
			File arqPageIds = new File(nomeBaseArquivo + i + ".pageId");

			// gera e grava arquivos
			String[] datasetTeste = gerarDatasetSelecionadorView(v, i, true,
					metSeletor);
			String[] datasetTreino = gerarDatasetSelecionadorView(v, i, false,
					metSeletor);

			ArquivoUtil.gravaTexto(datasetTeste[0], arqFonte, i != 0);
			ArquivoUtil.gravaTexto(datasetTeste[0], arqTeste, false);
			ArquivoUtil.gravaTexto(datasetTeste[1], arqPageIds, false);
			ArquivoUtil.gravaTexto(datasetTreino[0], arqTreino, false);

			metSeletor.filtraIDsArquivo(arqTeste, arqTeste);
			metSeletor.filtraIDsArquivo(arqTreino, arqTreino);

			foldsGerados[i] = new Fold(i, arqFonte, arqTreino, arqTeste,
					arqPageIds);
		}
		return foldsGerados;
	}

	private void defineConfiancaView(Fold[] foldViewsSel,
			MetodoAprendizado metSeletor, View[] views) throws Exception {

		String nomExperimento = metSeletor.getNomExperimento();
		Fold[] fResultado = null;
		metSeletor.setArquivoOrigem(foldViewsSel[0].getOrigem());
		System.out.println("ORIGEM: "
				+ foldViewsSel[0].getOrigem().getAbsolutePath());

		/*** Calcula treino usando 3 folds **/
		/*
		 * System.out.println("****Calculando treino do seletor*****");
		 * metSeletor.setNomExperimento(metSeletor.getNomExperimento()+
		 * "_treino");
		 * 
		 * Fold[] fTreino = new Fold[foldViewsSel.length];
		 * View.treinarPor3Folds(foldViewsSel, fTreino, metSeletor,
		 * metSeletor.getNomExperimento(), views[0].getColecao()); fResultado =
		 * fTreino;
		 * 
		 * //define resultado setResultadosSelView(views, fResultado, false);
		 */

		/** Calcula resultado do teste **/
		System.out.println("****Calculando teste do seletor*****");
		// fResultado = metSeletor.testar(foldViewsSel);
		// setResultadosSelView(views, fResultado, true);

		metSeletor.setNomExperimento(nomExperimento);
	}

	private void setResultadosSelView(View[] views, Fold[] fResultado,
			boolean isTeste) throws Exception {
		for (int i = 0; i < fResultado.length; i++) {
			// para cada resultado, varrer cada view para adicionar a
			// probabilidade deste resultado
			int numResults = fResultado[i].getResultadosValues().size();
			for (int j = 0; j < numResults; j++) {
				ResultadoItem r = fResultado[i].getResultadosValues().get(j);
				long idResult = r.getId();
				float[] probsPorView = r.getProbPorClasse();
				// o indice da prob é exatamente a prob de ser uma determinada
				// view
				for (int k = 0; k < probsPorView.length; k++) {
					ResultadoItem rView = null;
					if (isTeste) {
						rView = views[k].getResultTeste().getFolds()[i]
								.getResultadoPorId(idResult);
					} else {
						rView = views[k].getResultTreino().getFolds()[i]
								.getResultadoPorId(idResult);
					}
					if (rView == null) {
						System.out.println("Num achou ID: " + idResult);
						throw new Exception("Num achou ID: " + idResult);
					}
					if (r.getId() != rView.getId()) {
						throw new Exception("ID Incorreto de resultado!");
					}
					rView.setConfianca(probsPorView[k]);
					rView.atualizaConfianca(views[k].getNomExperimento());
				}
			}
		}
	}

	public Fold[][] getResultadoPorViewTreino() {
		return resultadoPorViewTreino;
	}

	public void setResultadoPorViewTreino(Fold[][] resultadoPorViewTreino) {
		this.resultadoPorViewTreino = resultadoPorViewTreino;
	}

	public Fold[][] getResultadoPorViewTeste() {
		return resultadoPorViewTeste;
	}
	public Fold[][] getResultadoPorViewValidacao() {
		return resultadoPorViewValidacao ;
	}

	public void setResultadoPorViewTeste(Fold[][] resultadoPorViewTeste) {
		this.resultadoPorViewTeste = resultadoPorViewTeste;
	}

	public void gravarResultado() {

	}

	public static void main(String[] args) throws Exception {
		ConfigViewColecao[] arrCnf = {
		ConfigViewColecao.WIKIPEDIA,
		/*ConfigViewColecao.WIKIPEDIA_CAT,
		 
		 
		 
		 */
				/*
				ConfigViewColecao.STARWARS_LABEL,
				ConfigViewColecao.WIKIPEDIA,
				ConfigViewColecao.STARWARS_VOTE,
				ConfigViewColecao.MUPPETS,
				*/
		 
			/*
			ConfigViewColecao.WIKIPEDIA_CULTURE,
			 
			ConfigViewColecao.WIKIPEDIA_GEOGRAPHY,
			ConfigViewColecao.WIKIPEDIA_HISTORY,
			ConfigViewColecao.WIKIPEDIA_SCIENCE,
			*/
				//ConfigViewColecao.WIKIPEDIA_RANDOM_1,
				//ConfigViewColecao.WIKIPEDIA_RANDOM_2,
				//ConfigViewColecao.WIKIPEDIA_RANDOM_3,
				//ConfigViewColecao.WIKIPEDIA_RANDOM_5,
		 
		};
		for (ConfigViewColecao cnf : arrCnf) {
			
			if ( (cnf == ConfigViewColecao.WIKIPEDIA_SCIENCE || 
					cnf == ConfigViewColecao.WIKIPEDIA_CULTURE || 
					cnf == ConfigViewColecao.WIKIPEDIA_GEOGRAPHY ||
					cnf == ConfigViewColecao.WIKIPEDIA_HISTORY
					)
					
					&& USE_CATEGORY) {
				cnf.addLstCnfView(ConfigViewColecao.WIKIPEDIA);
				//cnf.addLstCnfView(ConfigViewColecao.WIKIPEDIA_GEOGRAPHY);
				//cnf.addLstCnfView(ConfigViewColecao.WIKIPEDIA_HISTORY);
				//cnf.addLstCnfView(ConfigViewColecao.WIKIPEDIA_SCIENCE);
			}

			System.out.println("=>> COLECAO: " + cnf.getColecao());

			//extractView(cnf);
			prepareViewPerCollection(cnf);

		}
	}

	private static void extractView(ConfigViewColecao cnf) throws Exception {
		// prepareViewPerCollection(cnf);

		//String nomExperimentoView = CONFERENCE_EXP + "_wikiMultiview_"+ cnf.getColecao().getSigla();
		//String nomExperimentoView = CONFERENCE_EXP + "_6viewsBal_"+ cnf.getColecao().getSigla();
		String subNome = "";
		View[] arrViews = null;
		String nomExperimentoView = "";
		switch (cnf) {
		case WIKIPEDIA_CULTURE:
			subNome += "_culture";
			break;
		case WIKIPEDIA_GEOGRAPHY:
			subNome += "_geography";
			break;
		case WIKIPEDIA_HISTORY:
			subNome += "_history";
			break;
		case WIKIPEDIA_SCIENCE:
			subNome += "_science";
			break;
		case WIKIPEDIA_RANDOM_1:
			subNome += "_Random_1";
			break;				
		case WIKIPEDIA_RANDOM_2:
			subNome += "_Random_2";
			break;				
		case WIKIPEDIA_RANDOM_3:
			subNome += "_Random_3";
			break;				
		case WIKIPEDIA_RANDOM_5:
			subNome += "_Random_5";
			break;	
		case WIKIPEDIA_CAT:
			subNome += "_cat_";
			break;
		}
		
		switch(TPO_SUB_EXPERIMENTO)
		{
			case	SEIS_VISOES:
				nomExperimentoView = CONFERENCE_EXP + "_6viewsBal_"+ cnf.getColecao().getSigla()+subNome;
				//nomExperimentoView = CONFERENCE_EXP + "_6views_"+ cnf.getColecao().getSigla()+subNome;
				arrViews = cnf.getSixViews(nomExperimentoView, true);
				break;
				
			case 	TRES_VISOES:
				nomExperimentoView = CONFERENCE_EXP+"_3viewsBal_"+ cnf.getColecao().getSigla()+subNome;
				//nomExperimentoView = CONFERENCE_EXP+"_3views_"+ cnf.getColecao().getSigla()+subNome;
				arrViews = cnf.getViews(nomExperimentoView, false);
				break;
		}
		
		


		SVM svmSeletorView = null; /*
									 * new
									 * SVM(nomExperimentoView+"_selView_simples"
									 * ,
									 * cnf.getCSeletorIdeal(),cnf.getGSeletorIdeal
									 * (
									 * ),1.0F,SVM.MODE_CLASSIFICATION,true,false
									 * );
									 * svmSeletorView.setColecao(cnf.getColecao
									 * ());
									 */
		
		
		ResultadoViews rViews = getResultadoViewsObject(arrViews, svmSeletorView);
		View[] v = rViews.getViews();
		System.out.println("NUMERO DE VISOES: "+v.length);
		
		// System.out.println("Resultado do 176011 Fold 0: "+v[0].getResultTreino().getFolds()[0].getResultadoPorId(176011L));
	}

	private static void prepareViewPerCollection(ConfigViewColecao cnf)
			throws Exception {
		SVM svmSeletorView = null;

		ResultadoViews rViews = null;
		boolean isLocal = false;
		String nomExperimentoView = CONFERENCE_EXP + "_wikiMVTransfer_"
				+ cnf.getColecao().getSigla();
		switch (cnf) {
		case WIKIPEDIA_CULTURE:
			nomExperimentoView += "_culture_SingleView";
			isLocal=true;
			break;
		case WIKIPEDIA_GEOGRAPHY:
			nomExperimentoView += "_geography_SingleView";
			isLocal=true;
			break;
		case WIKIPEDIA_HISTORY:
			nomExperimentoView += "_history_SingleView";
			isLocal=true;
			break;
		case WIKIPEDIA_SCIENCE:
			nomExperimentoView += "_science_SingleView";
			isLocal=true;
			break;
		case WIKIPEDIA_RANDOM_1:
			nomExperimentoView += "_Random_1_SingleView";
			break;				
		case WIKIPEDIA_RANDOM_2:
			nomExperimentoView += "_Random_2_SingleView";
			break;				
		case WIKIPEDIA_RANDOM_3:
			nomExperimentoView += "_Random_3_SingleView";
			break;				
		case WIKIPEDIA_RANDOM_5:
			nomExperimentoView += "_Random_5_SingleView";
			break;				
		case WIKIPEDIA_CAT:
			nomExperimentoView += "_cat_";
			break;
		}
		
		/*
		 * rViews =
		 * getResultadoViewsObject(cnf.getViewsTodosColecaoExcetoAtual(true),
		 * svmSeletorView);
		 * 
		 * rViews = getResultadoViewsObject(cnf.getViewsTodosColecao(true),
		 * svmSeletorView);
		 * 
		 * if (cnf != ConfigViewColecao.WIKIPEDIA) { rViews =
		 * getResultadoViewsObject(cnf.getViews2Colecao(Colecao.WIKIPEDIA,
		 * true), svmSeletorView); //
		 * System.out.println(rViews.getResultadoPorViewTreino
		 * ()[1][1].getResultadosValues().size()); } if (cnf !=
		 * ConfigViewColecao.STARWARS_LABEL) { rViews =
		 * getResultadoViewsObject(cnf.getViews2Colecao( Colecao.STARWARS_LABEL,
		 * true), svmSeletorView); }
		 */
		rViews = getResultadoViewsObject(cnf.getViewsColecaoAtual(nomExperimentoView,true),svmSeletorView);
		/*
		if(isLocal)
		{
			rViews = getResultadoViewsObject(cnf.getViewsColecaoAtual(nomExperimentoView,true),svmSeletorView);
		}else
		{

			rViews = getResultadoViewsObject(cnf.getViewsColecaoAtual(true),svmSeletorView);
		}
		*/
	}

	private static void testViewsColection(ConfigViewColecao cnf) {
		View[] views = cnf.getViewsTodosColecao(true);
		System.out.println("Coleção: " + cnf.getColecao());
		for (int i = 0; i < views.length; i++) {
			System.out.println("\t\t*\t" + views[i].getColecaoDatasetView());
		}
	}
}
