package config_tmp;

import java.io.File;
import java.util.Calendar;

import calendario.DateUtil;
import entidadesAprendizado.View;



 
public enum Colecao {
	TESTE(20080106000000L,"en.wikipedia.org/w","wiki_teste","wiki_teste","wiki_en_new","wiki6/wiki_teste.amostra",2.0F,0.5F,0.1F,0F,0F,new MinMax(0,5),new File("/data/dumps/wiki_amostra_balanceada.obj")),
				
	STACK_OVER(20120301000000L,"","qa_stack","qa_stack_amostra","qa_stack","forum_qa/qa_stack.amostra",32F,0.125F,0.5F,0F,0F,new MinMax(-5,7),"",null),
	STACK_OVER_CORRECT(20120301000000L,"","qa_stack_correct","qa_stack_amostra","qa_stack","forum_qa/qa_stack_correto.amostra",0.5F,2F,0.125F,0F,0F,new MinMax(0,1),"",null),
	
	WIKIPEDIA_TMP(20080106000000L,"en.wikipedia.org/w","wiki6_balanceada","wiki_tmp","wiki_2011","wiki6/wiki6_todos.amostra",8.0F,0.5F,0.5F,0F,0F,new MinMax(0,5),new File("/data/dumps/")),
	WIKIPEDIA(20080106000000L,"en.wikipedia.org/w","wiki6_balanceada","wiki_amostra_balanceada","wiki_en_new","wiki6/wiki6_todos.amostra",8.0F,0.5F,0.5F,0F,0F,new MinMax(0,5),"nomColecao = 'Original'",new File("/data/dumps/wiki_amostra_balanceada.obj")),
	
	WIKI_CULTURE(20080106000000L,"en.wikipedia.org/w","wiki6_balanceada","wiki_amostra_balanceada","wiki_en_new","categorias/wiki6_balanceada_culture_todas.amostra",8.0F,0.5F,0.5F,0F,0F,new MinMax(0,5),"nomColecao = 'Original'",new File("/data/dumps/wiki_amostra_balanceada.obj")),
	WIKI_GEOGRAPHY(20080106000000L,"en.wikipedia.org/w","wiki6_balanceada","wiki_amostra_balanceada","wiki_en_new","categorias/wiki6_balanceada_geography_todas.amostra",2.0F,0.5F,0.5F,0F,0F,new MinMax(0,5),"nomColecao = 'Original'",new File("/data/dumps/wiki_amostra_balanceada.obj")),
	WIKI_HISTORY(20080106000000L,"en.wikipedia.org/w","wiki6_balanceada","wiki_amostra_balanceada","wiki_en_new","categorias/wiki6_balanceada_history_todas.amostra",32.0F,0.125F,0.03125F,0F,0F,new MinMax(0,5),"nomColecao = 'Original'",new File("/data/dumps/wiki_amostra_balanceada.obj")),
	WIKI_SCIENCE(20080106000000L,"en.wikipedia.org/w","wiki6_balanceada","wiki_amostra_balanceada","wiki_en_new","categorias/wiki6_balanceada_science_todas.amostra",2.0F,0.5F,0.5F,0F,0F,new MinMax(0,5),"nomColecao = 'Original'",new File("/data/dumps/wiki_amostra_balanceada.obj")),
	
	WIKI_RANDOM_1(20080106000000L,"en.wikipedia.org/w","wiki6_balanceada","wiki_amostra_balanceada","wiki_en_new","wiki_rand/wiki6_Random_1_todas.amostra",8F,0.5F,0.5F,0F,0F,new MinMax(0,5),"nomColecao = 'Random-1'",new File("/data/dumps/wiki_amostra_balanceada.obj")),
	WIKI_RANDOM_2(20080106000000L,"en.wikipedia.org/w","wiki6_balanceada","wiki_amostra_balanceada","wiki_en_new","wiki_rand/wiki6_Random_2_todas.amostra",8F,0.5F,0.5F,0F,0F,new MinMax(0,5),"nomColecao = 'Random-2'",new File("/data/dumps/wiki_amostra_balanceada.obj")),
	WIKI_RANDOM_3(20080106000000L,"en.wikipedia.org/w","wiki6_balanceada","wiki_amostra_balanceada","wiki_en_new","wiki_rand/wiki6_Random_3_todas.amostra",8F,0.5F,0.5F,0F,0F,new MinMax(0,5),"nomColecao = 'Random-3'",new File("/data/dumps/wiki_amostra_balanceada.obj")),
	WIKI_RANDOM_5(20080106000000L,"en.wikipedia.org/w","wiki6_balanceada","wiki_amostra_balanceada","wiki_en_new","wiki_rand/wiki6_Random_5_todas.amostra",2F,0.5F,0.125F,0F,0F,new MinMax(0,5),"nomColecao = 'Random-5'",new File("/data/dumps/wiki_amostra_balanceada.obj")),
	
	
	WIKIPEDIA_2011(20080106000000L,"en.wikipedia.org/w","wiki_2011","wiki_2011","wiki_2011","wiki6/wiki6_todos.amostra",2.0F,0.5F,0.5F,0F,0F,new MinMax(0,5),"nomColecao = 'Original'",new File("/data/dumps/wiki_amostra_balanceada.obj")),
	WIKIPEDIA_PT(20080106000000L,"pt.wikipedia.org/w","wiki_pt",null,"wiki_pt",null,2.0F,0.5F,0.5F,0F,0F,new MinMax(0,5),"nomColecao = 'Original'",new File("/data/dumps/wiki_amostra_balanceada.obj")),
	WIKIPEDIA_TEMPORAL(20080106000000L,"en.wikipedia.org/w","wiki_temporal","wiki_temporal","wiki_en_new",null,2.0F,0.5F,0.5F,0F,0F,new MinMax(0,5),"nomColecao = 'Original'",new File("/data/dumps/wiki_amostra_balanceada.obj")),
	WIKIPEDIA_TEMPORAL_ULTAV(20080106000000L,"en.wikipedia.org/w","wiki_temporal_ultav","wiki_temporal_ultAv","wiki_en_new",null,2.0F,0.5F,0.5F,0F,0F,new MinMax(0,5),"nomColecao = 'Original'",new File("/data/dumps/wiki_amostra_balanceada.obj")),
	WIKIPEDIA_CONTROLE_TEMPORAL(20080106000000L,"en.wikipedia.org/w","wiki_ctrl_temporal","wiki_ctrl_temporal","wiki_en_new",null,2.0F,0.5F,0.5F,0F,0F,new MinMax(0,5),"nomColecao = 'Original'",new File("/data/dumps/wiki_amostra_balanceada.obj")),
	WIKIPEDIA_FA(20080106000000L,"en.wikipedia.org/w","wiki_FAs","wiki_FA","wiki_en_new",null,2.0F,0.5F,0.5F,0F,0F,new MinMax(0,5),"nomColecao = 'Original'",new File("/data/dumps/wiki_amostra_balanceada.obj")),
	
	
	
	MUPPETS(20091006000000L,"muppet.wikia.com","muppets","wikia_muppets_vote","wikia_muppets","muppets/muppets_todos.amostra",0.5F,0.5F,0.25F,0.5F,0.5F,new MinMax(1,5),new File("/data/dumps/probReview_wikia_muppets_vote_0.obj")),
	STARWARS_LABEL(20091005000000L,"starwars.wikia.com","starAmostra","wikia_starwars_amostra","wikia_starwars","starAmostra/starAmostra_todos.amostra",2F,0.5F,0.03125F,0F,0F,new MinMax(0,2),new File("/data/dumps/probReview_wikia_starwars_amostra_0.obj")),
	STARWARS_VOTE(20091005000000L,"starwars.wikia.com","starVote","wikia_starwars_vote","wikia_starwars","starVote/starVote_todos.amostra",0.5F,2F,0.25F,0.5F,0.1F,new MinMax(1,5),new File("/data/dumps/probReview_wikia_starwars_vote_0.obj"));
	
	public static final Colecao[] arrColecoes = {
													WIKIPEDIA,
													WIKIPEDIA_PT,
													WIKIPEDIA_TEMPORAL,
													WIKIPEDIA_TEMPORAL_ULTAV,
													WIKIPEDIA_CONTROLE_TEMPORAL,
													MUPPETS,
													STARWARS_LABEL,
													STARWARS_VOTE
												}; 
	private static final String DIRETORIO = "/data/experimentos/fonte/";
	private String sigla;
	private String esquemaAmostra;
	private String esquemaColecao;
	private String urlApi;
	private File colecaoOrigem;
	private double custo;
	private double gama;
	private double epslon;
	private double cRuido;
	private double gRuido;
	private Calendar dateOfDump;
	private MinMax minMaxClass;
	private File arqProbReview;
	
	private String strFiltroFold = "";
	
	private Colecao(long datDump,String urlApi,String sigla, String esquemaAmostra, String esquemaColecao,String arqColecao,double custo,double gama,double epslon,double cRuido,double gRuido,MinMax classMinMaxValue, String strFiltroFold,File arqProbReview)
	{
		this(datDump,urlApi,sigla, esquemaAmostra, esquemaColecao,arqColecao,custo,gama,epslon, cRuido,gRuido,classMinMaxValue,arqProbReview);
		this.strFiltroFold = strFiltroFold;
	}
	private Colecao(long datDump,String urlApi,String sigla, String esquemaAmostra, String esquemaColecao,String arqColecao,double custo,double gama,double epslon,double cRuido,double gRuido,MinMax classMinMaxValue,File arqProbReview) 
	{
		this.sigla = sigla;
		this.esquemaAmostra = esquemaAmostra;
		this.esquemaColecao = esquemaColecao;
		this.urlApi = urlApi;
		this.colecaoOrigem = new File(DIRETORIO+"/"+arqColecao);
		this.dateOfDump = DateUtil.timestampToDate(datDump);
		
		this.custo = custo;
		this.gama = gama; 
		this.epslon = epslon;
		
		this.cRuido = cRuido;
		this.gRuido = gRuido;
		
		this.minMaxClass = classMinMaxValue;
		
		this.arqProbReview = arqProbReview;
	}
	public File getArqProbReview()
	{
		return this.arqProbReview;
	}
	public void setArqProbReview(File arq)
	{
		this.arqProbReview = arq;
	}
	public String getFiltroFold()
	{
		return strFiltroFold;
	}
	public double getEpslon()
	{
		return this.epslon;
	}
	public void setEpslon(double epslon)
	{
		this.epslon = epslon;
	}
	public MinMax getMinMaxClass()
	{
		return this.minMaxClass;
	}
	
	public Calendar getDateOfDump()
	{
		return this.dateOfDump;
	}
	public String getUrlApi()
	{
		return this.urlApi;
	}
	
	public String getSigla() {
		return sigla;
	}

	public String getEsquemaAmostra() {
		return esquemaAmostra;
	}

	public String getEsquemaColecao() {
		return esquemaColecao;
	}

	public File getArquivoOrigem() {
		// TODO Auto-generated method stub
		return colecaoOrigem;
	}

	public double getCusto() {
		return custo;
	}

	public double getGama() {
		return gama;
	}

	public double getCRuido() {
		return cRuido;
	}

	public double getGRuido() {
		return gRuido;
	}
	public View[] getViewsColecaoTodos()
	{
		switch(this)
		{
			case WIKIPEDIA:
			case WIKIPEDIA_PT: 
			case WIKIPEDIA_TEMPORAL:
			case WIKIPEDIA_TEMPORAL_ULTAV:
			case WIKIPEDIA_CONTROLE_TEMPORAL:
				return ConfigViewColecao.WIKIPEDIA.getViewsTodosColecao(false);
			case MUPPETS:
				return ConfigViewColecao.MUPPETS.getViewsTodosColecao(false);
			case STARWARS_LABEL:
				return ConfigViewColecao.STARWARS_LABEL.getViewsTodosColecao(false);
			case STARWARS_VOTE:
				return ConfigViewColecao.STARWARS_VOTE.getViewsTodosColecao(false);
			default:
				return ConfigViewColecao.WIKIPEDIA.getViewsTodosColecao(false);
		}
	}
	public static Colecao getColecaoBySigla(String sigla)
	{
		for(Colecao col : arrColecoes )
		{
			if(col.getSigla().equals(sigla))
			{
				return col;
			}
		}
		return null;
	}
}
