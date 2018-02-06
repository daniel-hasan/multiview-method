package config_tmp;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import aprendizadoResultado.ResultadosWikiMultiviewMetodos.TIPO_VIEW;
import aprendizadoUtils.ConvertDataset;
import aprendizadoUtils.LAC;
import aprendizadoUtils.MetodoAprendizado;
import aprendizadoUtils.SVM;
import entidadesAprendizado.View;
import entidadesAprendizado.View.FeatureType;

public enum ConfigViewColecao implements Serializable {
	TESTE(Colecao.TESTE, 8, 0.5F, 0.1F, Colecao.TESTE.getSigla()
			+ "_text.amostra",// text

			32, 8, 0.1F, Colecao.WIKIPEDIA.getSigla() + "_read.amostra", // read
			8, 8, 0.1F, Colecao.WIKIPEDIA.getSigla() + "_tam.amostra",// tam
			8, 2, 0.1F, Colecao.WIKIPEDIA.getSigla() + "_content.amostra",// content
			0.5F, 8, 0.1F, Colecao.WIKIPEDIA.getSigla() + "_estorg.amostra", // stuct

			8, 8, 0.1F, Colecao.TESTE.getSigla() + "_grafo.amostra",// grafo
			8, 2, 0.1F, Colecao.TESTE.getSigla() + "_hist.amostra",// hist
			
			512F, 0.0078125F,// meta learning
			0.125F, 0.5F,// meta learning concordante
			32768, 0.0001220703125F,// meta learning discordante
			2, 0.5F,// SVR discordante
			0.5F, 8F,// seletor ideal
			128F, 0.0078125F// meta learning Seletor Ideal
	),

	WIKIPEDIA(Colecao.WIKIPEDIA, 2, 2F, 0.5F, "/wiki6/"
			+ Colecao.WIKIPEDIA.getSigla() + "_text.amostra",// text

			128, 8, 0.0625F, "/wiki6/" + Colecao.WIKIPEDIA.getSigla()
					+ "_read.amostra", // read
			32, 8, 0.5F, "/wiki6/" + Colecao.WIKIPEDIA.getSigla()
					+ "_tam.amostra",// tam
			8, 2, 0.5F, "/wiki6/" + Colecao.WIKIPEDIA.getSigla()
					+ "_content.amostra",// style
			8F, 2, 0.5F, "/wiki6/" + Colecao.WIKIPEDIA.getSigla()
					+ "_estorg.amostra", // stuct

			8, 8, 0.5F, "/wiki6/" + Colecao.WIKIPEDIA.getSigla()
					+ "_grafo.amostra",// grafo
			32, 2, 0.5F, "/wiki6/" + Colecao.WIKIPEDIA.getSigla()
					+ "_hist.amostra",// hist
					
			512F, 0.0078125F,// meta learning
			0.125F, 0.5F,// meta learning concordante
			32768, 0.0001220703125F,// meta learning discordante
			2, 0.5F,// SVR discordante
			0.5F, 8F,// seletor ideal
			128F, 0.0078125F// meta learning Seletor Ideal
	),

	WIKIPEDIA_CAT(Colecao.WIKIPEDIA, 8, 0.5F, 0.1F, "/categorias/wiki6/"
			+ Colecao.WIKIPEDIA.getSigla() + "_cat_text.amostra",// text

			32, 8, 0.1F, Colecao.WIKIPEDIA.getSigla() + "_read.amostra", // read
			8, 8, 0.1F, Colecao.WIKIPEDIA.getSigla() + "_tam.amostra",// tam
			8, 2, 0.1F, Colecao.WIKIPEDIA.getSigla() + "_content.amostra",// content
			0.5F, 8, 0.1F, Colecao.WIKIPEDIA.getSigla() + "_estorg.amostra", // stuct

			2, 8, 0.1F, "/categorias/wiki6/" + Colecao.WIKIPEDIA.getSigla()
					+ "_cat_grafo.amostra",// grafo
			128, 0.125F, 0.1F, "/categorias/wiki6/"
					+ Colecao.WIKIPEDIA.getSigla() + "_cat_hist.amostra",// hist
			512F, 0.0078125F,// meta learning
			0.125F, 0.5F,// meta learning concordante
			32768, 0.0001220703125F,// meta learning discordante
			2, 0.5F,// SVR discordante
			0.5F, 8F,// seletor ideal
			128F, 0.0078125F// meta learning Seletor Ideal
	), WIKIPEDIA_SCIENCE(Colecao.WIKI_SCIENCE, 8, 0.125F, 0.1F, "/categorias/"
			+ Colecao.WIKIPEDIA.getSigla() + "_science_text.amostra",// text

			8192, 0.03125F, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_science_read.amostra", // read
			32, 8, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_science_tam.amostra",// tam
			2, 2, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_science_content.amostra",// content
			8, 0.5F, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_science_estorg.amostra", // stuct

			8, 8, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_science_grafo.amostra",// grafo
			2, 2, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_science_hist.amostra",// hist
			512F, 0.0078125F,// meta learning
			0.125F, 0.5F,// meta learning concordante
			32768, 0.0001220703125F,// meta learning discordante
			2, 0.5F,// SVR discordante
			0.5F, 8F,// seletor ideal
			128F, 0.0078125F// meta learning Seletor Ideal
	), WIKIPEDIA_HISTORY(Colecao.WIKI_HISTORY, 8, 0.5F, 0.5F, "/categorias/"
			+ Colecao.WIKIPEDIA.getSigla() + "_history_text.amostra",// text

			128, 2, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_history_read.amostra", // read
			128, 2, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_history_tam.amostra",// tam
			2, 2, 0.125F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_history_content.amostra",// content
			2, 2, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_history_estorg.amostra", // stuct

			2, 2, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_history_grafo.amostra",// grafo
			32, 0.5F, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_history_hist.amostra",// hist
			512F, 0.0078125F,// meta learning
			0.125F, 0.5F,// meta learning concordante
			32768, 0.0001220703125F,// meta learning discordante
			2, 0.5F,// SVR discordante
			0.5F, 8F,// seletor ideal
			128F, 0.0078125F// meta learning Seletor Ideal
	), WIKIPEDIA_GEOGRAPHY(Colecao.WIKI_GEOGRAPHY, 2, 2, 0.1F, "/categorias/"
			+ Colecao.WIKIPEDIA.getSigla() + "_geography_text.amostra",// text

			32, 8, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_geography_read.amostra", // read
			32, 8, 0.125F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_geography_tam.amostra",// tam
			32, 0.5F, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_geography_content.amostra",// content
			2, 8, 0.25F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_geography_estorg.amostra", // stuct

			8, 8, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_geography_grafo.amostra",// grafo
			32, 2, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_geography_hist.amostra",// hist
			512F, 0.0078125F,// meta learning
			0.125F, 0.5F,// meta learning concordante
			32768, 0.0001220703125F,// meta learning discordante
			2, 0.5F,// SVR discordante
			0.5F, 8F,// seletor ideal
			128F, 0.0078125F// meta learning Seletor Ideal
	), 
	WIKIPEDIA_CULTURE(Colecao.WIKI_CULTURE, 8, 0.5F, 0.1F, "/categorias/"
			+ Colecao.WIKIPEDIA.getSigla() + "_culture_text.amostra",// text

			8, 8, 0.25F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_culture_read.amostra", // read
			8, 8, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_culture_tam.amostra",// tam
			8, 2, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_culture_content.amostra",// content
			2, 2, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_culture_estorg.amostra", // stuct

			2, 8, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_culture_grafo.amostra",// grafo
			8, 2F, 0.5F, "/categorias/" + Colecao.WIKIPEDIA.getSigla()
					+ "_culture_hist.amostra",// hist
			512F, 0.0078125F,// meta learning
			0.125F, 0.5F,// meta learning concordante
			32768, 0.0001220703125F,// meta learning discordante
			2, 0.5F,// SVR discordante
			0.5F, 8F,// seletor ideal
			128F, 0.0078125F// meta learning Seletor Ideal
	), 
	WIKIPEDIA_RANDOM_1(Colecao.WIKI_RANDOM_1, 8, 0.5F, 0.5F, "/wiki_rand/wiki6_Random_1_text.amostra",// text
			8, 8, 0.5F, "/wiki_rand/wiki6_Random_1_read.amostra", // read
			8192, 0.5F, 0.5F, "/wiki_rand/wiki6_Random_1_tam.amostra",// tam
			8, 2, 0.5F, "/wiki_rand/wiki6_Random_1_content.amostra",// content
			8, 2, 0.5F, "/wiki_rand/wiki6_Random_1_estOrg.amostra", // stuct

			8, 8, 0.5F, "/wiki_rand/wiki6_Random_1_grafo.amostra",// grafo
			512, 0.5F, 0.5F, "/wiki_rand/wiki6_Random_1_hist.amostra",// hist
			
			
			512F, 0.0078125F,// meta learning
			0.125F, 0.5F,// meta learning concordante
			32768, 0.0001220703125F,// meta learning discordante
			2, 0.5F,// SVR discordante
			0.5F, 8F,// seletor ideal
			128F, 0.0078125F// meta learning Seletor Ideal
	),
	WIKIPEDIA_RANDOM_2(Colecao.WIKI_RANDOM_2, 8, 2F, 0.5F, "/wiki_rand/wiki6_Random_2_text.amostra",// text
			8192, 0.03125F, 0.5F, "/wiki_rand/wiki6_Random_2_read.amostra", // read
			32, 8, 0.5F, "/wiki_rand/wiki6_Random_2_tam.amostra",// tam
			8, 2, 0.5F, "/wiki_rand/wiki6_Random_2_content.amostra",// content
			2, 8, 0.5F, "/wiki_rand/wiki6_Random_2_estOrg.amostra", // stuct

			8, 8, 0.5F,"/wiki_rand/wiki6_Random_2_grafo.amostra",// grafo
			128, 0.5F, 0.5F, "/wiki_rand/wiki6_Random_2_hist.amostra",// hist
			
			512F, 0.0078125F,// meta learning
			0.125F, 0.5F,// meta learning concordante
			32768, 0.0001220703125F,// meta learning discordante
			2, 0.5F,// SVR discordante
			0.5F, 8F,// seletor ideal
			128F, 0.0078125F// meta learning Seletor Ideal
	), 
	WIKIPEDIA_RANDOM_3(Colecao.WIKI_RANDOM_3, 8, 0.5F, 0.5F, "/wiki_rand/wiki6_Random_3_text.amostra",// text
			0.125F, 8, 0.5F, "/wiki_rand/wiki6_Random_3_read.amostra", // read
			8, 8, 0.5F, "/wiki_rand/wiki6_Random_3_tam.amostra",// tam
			2, 2, 0.5F, "/wiki_rand/wiki6_Random_3_content.amostra",// content
			2, 2, 0.5F, "/wiki_rand/wiki6_Random_3_estOrg.amostra", // stuct

			8, 8, 0.5F, "/wiki_rand/wiki6_Random_3_grafo.amostra",// grafo
			32, 0.5F, 0.5F, "/wiki_rand/wiki6_Random_3_hist.amostra",// hist
			
			512F, 0.0078125F,// meta learning
			0.125F, 0.5F,// meta learning concordante
			32768, 0.0001220703125F,// meta learning discordante
			2, 0.5F,// SVR discordante
			0.5F, 8F,// seletor ideal
			128F, 0.0078125F// meta learning Seletor Ideal
	), 
	WIKIPEDIA_RANDOM_5(Colecao.WIKI_RANDOM_5, 2, 0.5F, 0.5F, "/wiki_rand/wiki6_Random_5_text.amostra",// text
			2, 8, 0.5F, "/wiki_rand/wiki6_Random_5_read.amostra", // read
			32, 8, 0.5F, "/wiki_rand/wiki6_Random_5_tam.amostra",// tam
			8, 2, 0.5F, "/wiki_rand/wiki6_Random_5_content.amostra",// content
			8, 0.5F, 0.5F, "/wiki_rand/wiki6_Random_5_estOrg.amostra", // stuct

			8, 2, 0.5F, "/wiki_rand/wiki6_Random_5_grafo.amostra",// grafo
			8, 0.5F, 0.03125F, "/wiki_rand/wiki6_Random_5_hist.amostra",// hist
			
			512F, 0.0078125F,// meta learning
			0.125F, 0.5F,// meta learning concordante
			32768, 0.0001220703125F,// meta learning discordante
			2, 0.5F,// SVR discordante
			0.5F, 8F,// seletor ideal
			128F, 0.0078125F// meta learning Seletor Ideal
	), 
	
	
	STARWARS_LABEL(Colecao.STARWARS_LABEL, 8, 0.5F, 0.03125F,
			Colecao.STARWARS_LABEL.getSigla() + "/"
					+ Colecao.STARWARS_LABEL.getSigla() + "_text.amostra",// text

			32, 8, 0.25F, Colecao.STARWARS_LABEL.getSigla() + "/"
					+ Colecao.STARWARS_LABEL.getSigla() + "_read.amostra", // read
			8, 8, 0.125F, Colecao.STARWARS_LABEL.getSigla() + "/"
					+ Colecao.STARWARS_LABEL.getSigla() + "_tam.amostra",// tam
			8, 8, 0.0625F, Colecao.STARWARS_LABEL.getSigla() + "/"
					+ Colecao.STARWARS_LABEL.getSigla() + "_content.amostra",// content
			2, 8, 0.03125F, Colecao.STARWARS_LABEL.getSigla() + "/"
					+ Colecao.STARWARS_LABEL.getSigla() + "_estorg.amostra", // stuct

			8, 8, 0.0625F, Colecao.STARWARS_LABEL.getSigla() + "/"
					+ Colecao.STARWARS_LABEL.getSigla() + "_grafo.amostra",// grafo
			8, 2, 0.0625F, Colecao.STARWARS_LABEL.getSigla() + "/"
					+ Colecao.STARWARS_LABEL.getSigla() + "_hist.amostra",// hist
			2.0F, 2.0F,// meta learning
			32F, 0.5F,// meta learning concordante
			0.5F, 2F,// meta learning discordante
			2, 0.125F,// SVR discordante
			2048F, 2F,// seletor ideal
			0.5F, 0.5F// meta learning Seletor Ideal
	), STARWARS_VOTE(Colecao.STARWARS_VOTE, 2, 0.5F, 0.03125F,
			Colecao.STARWARS_VOTE.getSigla() + "/"
					+ Colecao.STARWARS_VOTE.getSigla() + "_text.amostra",// text

			0.125F, 8, 0.03125F, Colecao.STARWARS_VOTE.getSigla() + "/"
					+ Colecao.STARWARS_VOTE.getSigla() + "_read.amostra",// read
			32, 8, 0.25F, Colecao.STARWARS_VOTE.getSigla() + "/"
					+ Colecao.STARWARS_VOTE.getSigla() + "_tam.amostra",// tam
			2, 2, 0.03125F, Colecao.STARWARS_VOTE.getSigla() + "/"
					+ Colecao.STARWARS_VOTE.getSigla() + "_content.amostra",// content
			8, 8, 0.5F, Colecao.STARWARS_VOTE.getSigla() + "/"
					+ Colecao.STARWARS_VOTE.getSigla() + "_estorg.amostra",// stuct

			8, 8, 0.0625F, Colecao.STARWARS_VOTE.getSigla() + "/"
					+ Colecao.STARWARS_VOTE.getSigla() + "_grafo.amostra",// grafo
			128, 0.125F, 0.5F, Colecao.STARWARS_VOTE.getSigla() + "/"
					+ Colecao.STARWARS_VOTE.getSigla() + "_hist.amostra",// hist
			0.5F, 0.00048828125F,// meta learning
			0.03125F, 0.03125F,// meta learning concordante
			0.03125F, 0.03125F,// meta learning discordante
			0.5F, 0.5F,// SVR discordante
			0.5F, 8,// seletor ideal
			0.5F, 0.00048828125F// meta learning Seletor Ideal
	), MUPPETS(Colecao.MUPPETS, 0.125F, 2, 0.03125F, Colecao.MUPPETS.getSigla()
			+ "/" + Colecao.MUPPETS.getSigla() + "_text.amostra",// text

			0.5F, 8, 0.0625F, Colecao.MUPPETS.getSigla() + "/"
					+ Colecao.MUPPETS.getSigla() + "_read.amostra",// read
			32, 8, 0.5F, Colecao.MUPPETS.getSigla() + "/"
					+ Colecao.MUPPETS.getSigla() + "_tam.amostra",// tam
			0.125F, 8, 0.03125F, Colecao.MUPPETS.getSigla() + "/"
					+ Colecao.MUPPETS.getSigla() + "_content.amostra", // content
			2F, 8, 0.5F, Colecao.MUPPETS.getSigla() + "/"
					+ Colecao.MUPPETS.getSigla() + "_estorg.amostra",// struct

			512, 0.03125F, 0.0625F, Colecao.MUPPETS.getSigla() + "/"
					+ Colecao.MUPPETS.getSigla() + "_grafo.amostra",// grafo
			32, 0.125F, 0.5F, Colecao.MUPPETS.getSigla() + "/"
					+ Colecao.MUPPETS.getSigla() + "_hist.amostra",// hist
			0.03125F, 2F,// meta learning
			0.03125F, 2F,// meta learning concordante
			0.125F, 0.5F,// meta learning discordante
			0.5F, 0.5F,// svr discordante
			2F, 8F,// seletor ideal
			0.125F, 2.0F// meta learning Seletor Ideal
	);

	// private static final String DIRETORIO_BASE =
	// "/usr/wikipedia/testes_SVR/multiview/testes_iniciais/testes/";
	private static final String DIRETORIO_BASE = "/data/experimentos/fonte/";
	// public static final float EPSLON = 0.03125F;
	public static final float EPSLON = 0.1F;
	public static final int MODE = SVM.MODE_REGRESSION;
	private Colecao colecao;
	private List<ConfigViewColecao> lstCnfColecoes = new ArrayList<ConfigViewColecao>();
	private float cTexto;
	private float gTexto;
	private float epTexto;
	private File arquivoTexto;

	private float cTam;
	private float gTam;
	private float epTam;
	private File arquivoTam;

	private float cRead;
	private float gRead;
	private float epRead;
	private File arquivoRead;

	private float cContent;
	private float gContent;
	private float epContent;
	private File arquivoContent;

	private float cStruct;
	private float gStruct;
	private float epStruct;
	private File arquivoStruct;

	private float cGrafo;
	private float gGrafo;
	private float epGrafo;
	private File arquivoGrafo;

	private float cHist;
	private float gHist;
	private float epHist;
	private File arquivoHist;

	private float cMetaLearning;
	private float gMetaLearning;

	private float cMetaLearningConcord;
	private float gMetaLearningConcord;

	private float cMetaLearningDiscord;
	private float gMetaLearningDiscord;

	private float cSVRDiscord;
	private float gSVRDiscord;

	private float cSeletorIdeal;
	private float gSeletorIdeal;

	private float cMetaLearnSI;
	private float gMetaLearnSI;

	private boolean usarModeloExistente = true;
	private HashMap<TIPO_VIEW, ConfigCustoGama> mapConfigCustoGama;

	private ConfigViewColecao(Colecao colecao, float cTexto, float gTexto,
			float epTexto, String arquivoTexto,

			float cRead, float gRead, float epRead, String arquivoRead,
			float cTam, float gTam, float epTam, String arquivoTam,
			float cContent, float gContent, float epContent,
			String arquivoContent, float cStruct, float gStruct,
			float epStruct, String arquivoStruct,

			float cGrafo, float gGrafo, float epGrafo, String arquivoGrafo,
			float cHist, float gHist, float epHist, String arquivoHist,
			float cMetaLearning, float gMetaLearning,
			float cMetaLearningConcord, float gMetaLearningConcord,
			float cMetaLearningDiscord, float gMetaLearningDiscord,
			float cSVRDiscord, float gSVRDiscord, float cSeletorIdeal,
			float gSeletorIdeal, float cMetaLearnSI, float gMetaLearnSI) {
		this.colecao = colecao;

		this.cTexto = cTexto;
		this.gTexto = gTexto;
		this.epTexto = epTexto;
		this.arquivoTexto = new File(DIRETORIO_BASE + arquivoTexto);

		this.cTam = cTam;
		this.gTam = gTam;
		this.epTam = epTam;
		this.arquivoTam = new File(DIRETORIO_BASE + arquivoTam);

		this.cContent = cContent;
		this.gContent = gContent;
		this.epContent = epContent;
		this.arquivoContent = new File(DIRETORIO_BASE + arquivoContent);

		this.cRead = cRead;
		this.gRead = gRead;
		this.epRead = epRead;
		this.arquivoRead = new File(DIRETORIO_BASE + arquivoRead);

		this.cStruct = cStruct;
		this.gStruct = gStruct;
		this.epStruct = epStruct;
		this.arquivoStruct = new File(DIRETORIO_BASE + arquivoStruct);

		this.cGrafo = cGrafo;
		this.gGrafo = gGrafo;
		this.epGrafo = epGrafo;
		this.arquivoGrafo = new File(DIRETORIO_BASE + arquivoGrafo);

		this.cHist = cHist;
		this.gHist = gHist;
		this.epHist = epHist;
		this.arquivoHist = new File(DIRETORIO_BASE + arquivoHist);

		this.cMetaLearning = cMetaLearning;
		this.gMetaLearning = gMetaLearning;

		this.cMetaLearningConcord = cMetaLearningConcord;
		this.gMetaLearningConcord = gMetaLearningConcord;

		this.cMetaLearningDiscord = cMetaLearningDiscord;
		this.gMetaLearningDiscord = gMetaLearningDiscord;

		this.cSVRDiscord = cSVRDiscord;
		this.gSVRDiscord = gSVRDiscord;

		this.cSeletorIdeal = cSeletorIdeal;
		this.gSeletorIdeal = gSeletorIdeal;

		this.cMetaLearnSI = cMetaLearnSI;
		this.gMetaLearnSI = gMetaLearnSI;
	}

	public void addLstCnfView(ConfigViewColecao cnfView) {
		this.lstCnfColecoes.add(cnfView);

	}

	public List<ConfigViewColecao> getLstCnfViewFilhas() {
		return this.lstCnfColecoes;
	}

	public void setMultiviewCustoGamaParameters(boolean contemFeatures,
			boolean threeFold) throws IOException {
		mapConfigCustoGama = new HashMap<TIPO_VIEW, ConfigCustoGama>();

		// three fold sem artigo

		String expViewSimplesDir = "/data/experimentos/multiview/parametros/metalearning_so_view";
		String expViewColTodosDir = "/data/experimentos/multiview/parametros/metalearing_other_domains/todos";
		String expViewBaselineDir = "/data/experimentos/multiview/parametros/teste_threeFold_versus_validation/baseline_threeFold";

		// String expViewSimplesDir =
		// "/data/experimentos/jcdl_2012/parametros/multiview/view";

		// testes (trhee fold com artigo, validation com ou sem artigo)
		if (threeFold) {
			if (contemFeatures) {

				expViewSimplesDir = "/data/experimentos/multiview/parametros/metalearning_articles";
				expViewColTodosDir = "/data/experimentos/multiview/parametros/metalearing_other_domains_plus_article/todos";
				expViewBaselineDir = "/data/experimentos/multiview/parametros/teste_threeFold_versus_validation/baseline_article_threeFold";

				/*
				 * expViewSimplesDir =
				 * "/data/experimentos/jcdl_2012/parametros/multiview/view_article"
				 * ; expViewColTodosDir =
				 * "/data/experimentos/multiview/parametros/metalearing_other_domains_plus_article/todos"
				 * ; expViewBaselineDir =
				 * "/data/experimentos/multiview/parametros/teste_threeFold_versus_validation/baseline_article_threeFold"
				 * ;
				 */
				String expViewColTodosExAtualDir = "/data/experimentos/multiview/parametros/metalearing_other_domains_plus_article/todos_excetoEleMesmo";
				mapConfigCustoGama.put(TIPO_VIEW.VIEW_POR_COLECAO_EXCTO_ATUAL,
						ConfigCustoGama.getCustoGama(this.getColecao()
								.getSigla(),
								new File(expViewColTodosExAtualDir)));
			}
		} else {
			if (contemFeatures) {
				expViewSimplesDir = "/data/experimentos/multiview/parametros/teste_threeFold_versus_validation/view_article";
				expViewColTodosDir = "/data/experimentos/multiview/parametros/metalearing_other_domains_plus_article/todos";
				expViewBaselineDir = "/data/experimentos/multiview/parametros/teste_threeFold_versus_validation/baseline_article_validation";
			} else {
				expViewSimplesDir = "/data/experimentos/multiview/parametros/teste_threeFold_versus_validation/view";
				expViewColTodosDir = "/data/experimentos/multiview/parametros/metalearing_other_domains_plus_article/todos";
				expViewBaselineDir = "/data/experimentos/multiview/parametros/teste_threeFold_versus_validation/baseline_validation";
			}
		}

		mapConfigCustoGama.put(TIPO_VIEW.VIEW_SIMPLES, ConfigCustoGama
				.getCustoGama(this.getColecao().getSigla(), new File(
						expViewSimplesDir)));
		mapConfigCustoGama.put(TIPO_VIEW.VIEW_POR_COLECAO_TODOS,
				ConfigCustoGama.getCustoGama(this.getColecao().getSigla(),
						new File(expViewColTodosDir)));
		mapConfigCustoGama.put(TIPO_VIEW.BASELINE_VIEW, ConfigCustoGama
				.getCustoGama(this.getColecao().getSigla(), new File(
						expViewBaselineDir)));
	}

	public ConfigCustoGama getCustoGamaCategoria(boolean contemFeatures,
			boolean contemCategorias) throws IOException {
		String DIRETORIO_EXPS = "/home/hasan/data/experimentos/categoria/parametros";
		// Combinação das views com cat. como atributo no prim nivel
		if (this == WIKIPEDIA_CAT) {
			if (contemFeatures) {
				return ConfigCustoGama.getCustoGama("wiki6_feat_cat", new File(
						DIRETORIO_EXPS + "/nivel_2/"));
			} else {
				return ConfigCustoGama.getCustoGama("wiki6_cat", new File(
						DIRETORIO_EXPS + "/nivel_2/"));

			}
		}
		// Combinacao de cada tipo de categoria (Sci,his,cult,geo)
		if (this == WIKIPEDIA_CULTURE || this == WIKIPEDIA_GEOGRAPHY
				|| this == WIKIPEDIA_HISTORY || this == WIKIPEDIA_SCIENCE) {
			String nomArquivo = this.toString().replaceAll("WIKIPEDIA_", "")
					.toString().toLowerCase();
			if (contemFeatures) {
				nomArquivo += "_feat";
			}
			// nomArquivo += ".out";
			return ConfigCustoGama.getCustoGama(nomArquivo, new File(
					DIRETORIO_EXPS + "/nivel_2/"));
		}

		// Combinação Local+Global (4) WIKIPEDIA (com outros...)
		if (this == WIKIPEDIA && this.getLstCnfViewFilhas().size() > 0) {
			if (contemFeatures) {
				if (contemCategorias) {
					return ConfigCustoGama.getCustoGama("feat_cat", new File(
							DIRETORIO_EXPS + "/nivel2_local_global/"));
				} else {
					return ConfigCustoGama.getCustoGama("feat", new File(
							DIRETORIO_EXPS + "/nivel2_local_global/"));
				}
			} else {
				if (contemCategorias) {
					return ConfigCustoGama.getCustoGama("simples_cat",
							new File(DIRETORIO_EXPS + "/nivel2_local_global/"));
				} else {
					return ConfigCustoGama.getCustoGama("simples", new File(
							DIRETORIO_EXPS + "/nivel2_local_global/"));
				}
			}
		}

		return null;
	}

	public Map<TIPO_VIEW, ConfigCustoGama> getMapCustoGamaMultiview() {
		return this.mapConfigCustoGama;
	}

	public ConfigCustoGama getCustoGamaMultiview(TIPO_VIEW tpo) {
		return this.mapConfigCustoGama.get(tpo);
	}

	public void setUsarModeloExistente(boolean usarModeloExistente) {
		this.usarModeloExistente = usarModeloExistente;
	}

	public static float getEPSLON() {
		return EPSLON;
	}

	public Colecao getColecao() {
		return this.colecao;
	}

	public float getCTexto() {
		return cTexto;
	}

	public float getGTexto() {
		return gTexto;
	}

	public float getCGrafo() {
		return cGrafo;
	}

	public float getGGrafo() {
		return gGrafo;
	}

	public float getCHist() {
		return cHist;
	}

	public float getGHist() {
		return gHist;
	}

	public float getCMetaLearning() {
		return cMetaLearning;
	}

	public float getGMetaLearning() {
		return gMetaLearning;
	}

	public File getArquivoTexto() {
		return arquivoTexto;
	}

	public File getArquivoGrafo() {
		return arquivoGrafo;
	}

	public File getArquivoHist() {
		return arquivoHist;
	}

	public float getCMetaLearningConcord() {
		return cMetaLearningConcord;
	}

	public float getGMetaLearningConcord() {
		return gMetaLearningConcord;
	}

	public float getCMetaLearningDiscord() {
		return cMetaLearningDiscord;
	}

	public float getGMetaLearningDiscord() {
		return gMetaLearningDiscord;
	}

	public float getCSVRDiscord() {
		return cSVRDiscord;
	}

	public float getGSVRDiscord() {
		return gSVRDiscord;
	}

	public float getCSeletorIdeal() {
		return cSeletorIdeal;
	}

	public float getGSeletorIdeal() {
		return gSeletorIdeal;
	}

	public float getCMetaLearnSI() {
		return cMetaLearnSI;
	}

	public float getGMetaLearnSI() {
		return gMetaLearnSI;
	}

	/*********************************** Visões ***********************************************/
	public View getView(FeatureType feat,String nomExperimento, MetodoAprendizado met)
	{
		met.setNomExperimento(nomExperimento+feat.toString());
		met.setColecao(this.getColecao());
		
		if(met instanceof SVM)
		{
			((SVM)met).setUsarModeloExistente(this.usarModeloExistente);
		}
		return new View(this,met.getArquivoOrigem(),met,feat);
	}
	
	public View getViewText(String nomExperimento, boolean gravarNoBanco) {
		SVM svm = new SVM(nomExperimento + "_text", this.cTexto, this.gTexto,
				this.epTexto, MODE, true, gravarNoBanco);
		svm.setColecao(this.getColecao());
		svm.setUsarModeloExistente(this.usarModeloExistente);
		return new View(this, this.arquivoTexto, svm, FeatureType.TEXT);
	}

	public View getViewStructure(String nomExperimento, boolean gravarNoBanco) {
		SVM svm = new SVM(nomExperimento + "_struct", this.cStruct,
				this.gStruct, this.epStruct, MODE, true, gravarNoBanco);
		svm.setColecao(this.getColecao());
		svm.setUsarModeloExistente(this.usarModeloExistente);
		return new View(this, this.arquivoStruct, svm, FeatureType.STRUCTURE);
	}

	public View getViewStyle(String nomExperimento, boolean gravarNoBanco) {
		SVM svm = new SVM(nomExperimento + "_style", this.cContent,
				this.gContent, this.epContent, MODE, true, gravarNoBanco);
		svm.setColecao(this.getColecao());
		svm.setUsarModeloExistente(this.usarModeloExistente);
		return new View(this, this.arquivoContent, svm, FeatureType.STYLE);
	}

	public View getViewRead(String nomExperimento, boolean gravarNoBanco) {
		SVM svm = new SVM(nomExperimento + "_read", this.cRead, this.gRead,
				this.epRead, MODE, true, gravarNoBanco);
		svm.setColecao(this.getColecao());
		svm.setUsarModeloExistente(this.usarModeloExistente);
		return new View(this, this.arquivoRead, svm, FeatureType.READ);
	}

	public View getViewTam(String nomExperimento, boolean gravarNoBanco) {
		SVM svm = new SVM(nomExperimento + "_tam", this.cTam, this.gTam,
				this.epTam, MODE, true, gravarNoBanco);
		svm.setColecao(this.getColecao());
		svm.setUsarModeloExistente(this.usarModeloExistente);
		return new View(this, this.arquivoTam, svm, FeatureType.LENGTH);
	}

	public View getViewGrafo(String nomExperimento, boolean gravarNoBanco) {
		SVM svm = new SVM(nomExperimento + "_grafo", this.cGrafo, this.gGrafo,
				this.epGrafo, MODE, true, gravarNoBanco);
		svm.setColecao(this.getColecao());
		svm.setUsarModeloExistente(this.usarModeloExistente);
		return new View(this, this.arquivoGrafo, svm, FeatureType.NETWORK);
	}

	public View getViewHist(String nomExperimento, boolean gravarNoBanco) {
		SVM svm = criaSVMViewHist(nomExperimento, gravarNoBanco);
		return new View(this, this.arquivoHist, svm, FeatureType.HISTORY);
	}
	
	public View getViewColection(String nomExperimento, boolean gravarNoBanco) {
		SVM svm = criaSVMViewHist(nomExperimento, gravarNoBanco);
		return new View(this, this.arquivoHist, svm);
	}

	private View getViewSVMColecao(String nomExperimento, Colecao col,
			boolean gravarNoBanco) {
		SVM svm = new SVM(nomExperimento, (float) col.getCusto(),
				(float) col.getGama(), (float) col.getEpslon(), MODE, true,
				gravarNoBanco);
		svm.setColecao(this.getColecao());
		svm.setUsarModeloExistente(this.usarModeloExistente);

		View v = new View(this, col.getArquivoOrigem(), svm, col);

		return v;

	}

	/*********************************** Auxiliar das visoes ***********************************************/
	private SVM criaSVMViewHist(String nomExperimento, boolean gravarNoBanco) {
		SVM svm = new SVM(nomExperimento + "_hist", this.cHist, this.gHist,
				this.epHist, MODE, true, gravarNoBanco);
		svm.setColecao(this.getColecao());
		svm.setUsarModeloExistente(this.usarModeloExistente);
		return svm;
	}

	private View getViewSVMColecao(Colecao col, boolean gravarNoBanco) {
		return getViewSVMColecao("wikiMVTransfer_"
				+ this.getColecao().getSigla() + "_using_" + col.getSigla(),
				col, gravarNoBanco);
	}

	public View[] getViews(String nomExperimento, boolean gravarNoBanco) {
		View[] views = new View[3 + (this.lstCnfColecoes.size() * 3)];
		// views[0] = getViewText(nomExperimento, gravarNoBanco);
		// views[1] = getViewGrafo(nomExperimento, gravarNoBanco);
		views[0] = getViewText(nomExperimento, gravarNoBanco);
		views[1] = getViewGrafo(nomExperimento, gravarNoBanco);
		views[2] = getViewHist(nomExperimento, gravarNoBanco);

		int contJ = 3;
		for (int i = 0; i < this.lstCnfColecoes.size(); i++) {
			View[] arrSubView = this.lstCnfColecoes.get(i).getViews(
					nomExperimento, gravarNoBanco);
			for (int j = 0; j < arrSubView.length; j++) {
				views[contJ] = arrSubView[j];
				views[contJ].setAsLocal();
				contJ++;
			}
		}

		return views;
	}
	public View criaViewFromSVM(FeatureType feat, String nomExperimento, File arquivoFonteSVM, MetodoAprendizado metTo) throws IOException
	{
		SVM metFrom = new SVM();
		File dirRegular = new File(arquivoFonteSVM.getParent()+"/regular");
		File dirMetodo = new File(arquivoFonteSVM.getParent()+"/"+metTo.getClass().toString().replace("class ", "").replace("aprendizado.", "").trim());
		if(!dirMetodo.exists())
		{
			dirMetodo.mkdir();
		}
		
		File arqRegular = new File(dirRegular,arquivoFonteSVM.getName());
		File arqMetodo = new File(dirMetodo,arquivoFonteSVM.getName());
		
		
		arqMetodo = ConvertDataset.convertArquivo(arqRegular, arqMetodo, metFrom, metTo, true);
		
		
		
		metTo.setArquivoOrigem(arqMetodo);
		
		
		return getView(feat,nomExperimento, metTo);
		
	}
	public View[] getSixViewsLacSVM(String nomExperimento, boolean gravarNoBanco) throws IOException
	{
		View[] views = new View[12];
		
		
		views[0] = getViewStructure(nomExperimento, gravarNoBanco);
		views[1] = getViewStyle(nomExperimento, gravarNoBanco);
		views[2] = getViewRead(nomExperimento, gravarNoBanco);
		views[3] = getViewTam(nomExperimento, gravarNoBanco);
		views[4] = getViewGrafo(nomExperimento, gravarNoBanco);
		views[5] = getViewHist(nomExperimento, gravarNoBanco);
		
		
		//lac features
		views[6] =  criaViewFromSVM(FeatureType.LAC_STRUCTURE, nomExperimento, this.arquivoStruct, new LAC());
		views[7] =  criaViewFromSVM(FeatureType.LAC_STYLE, nomExperimento, this.arquivoContent, new LAC());
		views[8] =  criaViewFromSVM(FeatureType.LAC_LENGTH, nomExperimento, this.arquivoTam, new LAC());
		views[9] =  criaViewFromSVM(FeatureType.LAC_READ, nomExperimento, this.arquivoRead, new LAC());
		views[10] =  criaViewFromSVM(FeatureType.LAC_HISTORY, nomExperimento, this.arquivoHist, new LAC());
		views[11] =  criaViewFromSVM(FeatureType.LAC_NETWORK, nomExperimento, this.arquivoGrafo, new LAC());
		
		return views;
		
	}
	public View[] getSixViews(String nomExperimento, boolean gravarNoBanco) {
		View[] views = new View[6 + (this.lstCnfColecoes.size() * 6)];
		// views[0] = getViewText(nomExperimento, gravarNoBanco);
		// views[1] = getViewGrafo(nomExperimento, gravarNoBanco);
		views[0] = getViewStructure(nomExperimento, gravarNoBanco);
		views[1] = getViewStyle(nomExperimento, gravarNoBanco);
		views[2] = getViewRead(nomExperimento, gravarNoBanco);
		views[3] = getViewTam(nomExperimento, gravarNoBanco);
		views[4] = getViewGrafo(nomExperimento, gravarNoBanco);
		views[5] = getViewHist(nomExperimento, gravarNoBanco);

		int contJ = 6;

		if (this == WIKIPEDIA_CULTURE || this == WIKIPEDIA_GEOGRAPHY
				|| this == WIKIPEDIA_HISTORY || this == WIKIPEDIA_SCIENCE || this.toString().startsWith("WIKIPEDIA_RANDOM")) {
			for (int j = 0; j <= 5; j++) {
				views[j].setAsLocal();
			}
		}
		boolean isLocal = true;
		for (int i = 0; i < this.lstCnfColecoes.size(); i++) {
			String nomExperimentoView = nomExperimento;
			switch (this.lstCnfColecoes.get(i)) {
			case WIKIPEDIA_CULTURE:
				nomExperimentoView += "_culture";
				break;
			case WIKIPEDIA_GEOGRAPHY:
				nomExperimentoView += "_geography";
				break;
			case WIKIPEDIA_HISTORY:
				nomExperimentoView += "_history";
				break;
			case WIKIPEDIA_SCIENCE:
				nomExperimentoView += "_science";
				break;
			case WIKIPEDIA_RANDOM_1:
				nomExperimentoView += "_Random_1";
				break;				
			case WIKIPEDIA_RANDOM_2:
				nomExperimentoView += "_Random_2";
				break;				
			case WIKIPEDIA_RANDOM_3:
				nomExperimentoView += "_Random_3";
				break;				
			case WIKIPEDIA_RANDOM_5:
				nomExperimentoView += "_Random_5";
				break;								
			case WIKIPEDIA:
				nomExperimentoView = nomExperimento.replaceAll("_science", "")
						.replaceAll("_geography", "")
						.replaceAll("_culture", "").replaceAll("_history", "");
				isLocal = false;
				for (int j = 0; j <= 5; j++) {
					views[j].setAsLocal();
				}
				break;

			}
			View[] arrSubView = this.lstCnfColecoes.get(i).getSixViews(
					nomExperimentoView, gravarNoBanco);
			for (int j = 0; j < arrSubView.length; j++) {
				views[contJ] = arrSubView[j];
				if (isLocal) {
					views[contJ].setAsLocal();
				}
				contJ++;
			}
		}

		return views;
	}

	public File getArquivoColecao() {
		return this.getColecao().getArquivoOrigem();
	}

	public View[] getViewsTodosColecao(boolean gravarNoBanco) {
		Colecao[] arrColecoes = { Colecao.WIKIPEDIA, Colecao.MUPPETS,
				Colecao.STARWARS_VOTE, Colecao.STARWARS_LABEL };

		View[] views = new View[4];
		int idxView = 0;

		for (int i = 0; i < arrColecoes.length; i++) {
			views[idxView] = getViewSVMColecao(arrColecoes[i], gravarNoBanco);
			idxView++;
		}

		return views;
	}

	public View[] getViewsTodosColecaoExcetoAtual(boolean gravarNoBanco) {
		Colecao[] arrColecoes = { Colecao.WIKIPEDIA, Colecao.MUPPETS,
				Colecao.STARWARS_VOTE, Colecao.STARWARS_LABEL };

		View[] views = new View[3];
		int idxView = 0;

		for (int i = 0; i < arrColecoes.length; i++) {
			if (arrColecoes[i] != this.getColecao()) {
				views[idxView] = getViewSVMColecao(arrColecoes[i],
						gravarNoBanco);
				idxView++;
			}
		}

		return views;
	}

	public View[] getViews2Colecao(Colecao col, boolean gravarNoBanco) {
		View[] views = new View[2];

		views[0] = getViewSVMColecao(this.getColecao(), gravarNoBanco);
		views[1] = getViewSVMColecao(col, gravarNoBanco);

		return views;
	}

	public View[] getViewsColecaoAtual(boolean gravarNoBanco) {
		View[] views = new View[1];

		views[0] = getViewSVMColecao(this.getColecao(), gravarNoBanco);

		return views;
	}

	public View[] getViewsColecaoAtual(String experimento, boolean gravarNoBanco) {
		View[] views = new View[1];

		views[0] = getViewSVMColecao(experimento, this.getColecao(),
				gravarNoBanco);

		return views;
	}

	public static void main(String[] arg) throws IOException {
		ConfigViewColecao[] arrCnf = { // ConfigViewColecao.WIKIPEDIA_CAT,
		ConfigViewColecao.WIKIPEDIA_CULTURE,
				ConfigViewColecao.WIKIPEDIA_GEOGRAPHY,
				ConfigViewColecao.WIKIPEDIA_HISTORY,
				ConfigViewColecao.WIKIPEDIA_SCIENCE /*
													 * ,
													 * ConfigViewColecao.WIKIPEDIA
													 * , ConfigViewColecao.
													 * STARWARS_LABEL,
													 * ConfigViewColecao
													 * .STARWARS_VOTE,
													 * ConfigViewColecao.MUPPETS
													 */
		};
		for (ConfigViewColecao cnf : arrCnf) {
			// testMultiviewParameters(cnf);

		}
	}

	private static void testMultiviewParameters(ConfigViewColecao cnf)
			throws IOException {
		System.out.println("\n\n\n***************Colecao: " + cnf);

		System.out.println("**** Com features ****");
		cnf.setMultiviewCustoGamaParameters(true, true);
		Map<TIPO_VIEW, ConfigCustoGama> mpConfig = cnf
				.getMapCustoGamaMultiview();
		for (TIPO_VIEW tpo : mpConfig.keySet()) {
			ConfigCustoGama cnfCusto = mpConfig.get(tpo);
			if (cnfCusto != null) {
				System.out.println(tpo + ": " + cnfCusto.toString());
			}
		}
		System.out.println("\n**** Com features (Validation) *****");
		cnf.setMultiviewCustoGamaParameters(true, false);
		testViews(cnf);

		System.out.println("\n**** Sem features *****");
		cnf.setMultiviewCustoGamaParameters(false, true);
		testViews(cnf);

		System.out.println("\n**** sem features (Validation) *****");
		cnf.setMultiviewCustoGamaParameters(false, false);
		testViews(cnf);

	}

	private static void testViews(ConfigViewColecao cnf) {
		Map<TIPO_VIEW, ConfigCustoGama> mpConfig;
		mpConfig = cnf.getMapCustoGamaMultiview();
		for (TIPO_VIEW tpo : mpConfig.keySet()) {
			ConfigCustoGama cnfCusto = mpConfig.get(tpo);
			if (cnfCusto != null) {
				System.out.println(tpo + ": " + cnfCusto.toString());
			}
		}
	}
}
