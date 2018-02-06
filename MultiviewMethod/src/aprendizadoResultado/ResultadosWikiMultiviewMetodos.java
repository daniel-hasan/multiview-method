package aprendizadoResultado;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;


import apredizadoCombinacao.AbordagemCombinacao;
import apredizadoCombinacao.CombinaApenasConcordantes;
import apredizadoCombinacao.Combinador;
import apredizadoCombinacao.EliminaRuidoTreino;
import apredizadoCombinacao.Equation;
import apredizadoCombinacao.MetaLearning;
import apredizadoCombinacao.MetaLearning.TIPO_CONTEUDO_DATASET;
import apredizadoCombinacao.Poll;
import apredizadoCombinacao.Poll.Tipo;
import apredizadoCombinacao.TipoFeatureCombinacao;
import aprendizadoUtils.MetodoAprendizado;
import aprendizadoUtils.SVM;
import arquivo.ArquivoUtil;
import config_tmp.Colecao;
import config_tmp.ConfigCustoGama;
import config_tmp.ConfigViewColecao;
import entidadesAprendizado.Resultado;
import entidadesAprendizado.ResultadoItemViews.ComparationType;
import entidadesAprendizado.ResultadoViews;
import entidadesAprendizado.View;
 
public class ResultadosWikiMultiviewMetodos {
	public enum TIPO_VIEW {
		VIEW_POR_COLECAO_VERSUS_WIKIPEDIA, BASELINE_VIEW, VIEW_POR_COLECAO_VERSUS_SW_LABEL, VIEW_POR_COLECAO_TODOS, VIEW_POR_COLECAO_EXCTO_ATUAL, VIEW_SIMPLES,VIEW_LAC_SIMPLES;
	}

	public static String EXP_ROOT = "6viewsBal";//"6views";
	//private static String EXP_ROOT = "3viewsBal";//"6views";
	
	public static String SUB_VIEW_NAME = "";
	
	//private static File DIR_PARAM = new File(	"/data/experimentos/jcdl_2012/parametros/sixViews_segNivel/kappa");
	public static File DIR_PARAM = new File(	"/data/experimentos/jcdl_2012/parametros/sixViewsRandom_segNivel");
	//private static final File DIR_PARAM = new File(	"/data/experimentos/jcdl_2012/parametros/3Views_segNivel/");
	public static File DIR_RESULT = new File("/data/experimentos/jcdl_2012/resultados");
	public static boolean USAR_CONHECIMENTO_LOCAL = false;
	public static boolean USAR_CONHECIMENTO_GLOBAL_WIKI = false;
	public static TIPO_VIEW tpoView = TIPO_VIEW.VIEW_SIMPLES;
	private static HashMap<ConfigViewColecao, HashMap<String, Resultado>> resultViewGeral = new HashMap<ConfigViewColecao, HashMap<String, Resultado>>();
	public static File DIR_PARAMS_EXP = null;
	public static ConfigViewColecao[] cnfViewColecao = {};
	public static Set<TipoFeatureCombinacao> objCombinacoes= null;
	public static TIPO_CONTEUDO_DATASET[] tpoConteudoDatasetML;
	public static String prefixResultado = "";
	public static boolean GRAVAR_VIEW = false;
	public static boolean GRAVAR_COMBINACAO = false;

	
	static
	{
		
		switch(ResultadoViews.TPO_SUB_EXPERIMENTO)
		{
		case SEIS_VISOES:
			EXP_ROOT = "6viewsBal";
			//EXP_ROOT = "6views";
			//DIR_PARAM = new File(	"/data/experimentos/jcdl_2012/parametros/sixViews_segNivel/kappa");
			//DIR_PARAM = new File(	"/data/experimentos/jcdl_2012/parametros/sixViews_segNivel/finalizados");
			DIR_PARAM = new File(	"/data/experimentos/tpdl_2012/");
			break;
		case TRES_VISOES:
			EXP_ROOT = "3viewsBal";
			//EXP_ROOT = "3views";
			//DIR_PARAM = new File(	"/data/experimentos/jcdl_2012/parametros/3Views_segNivel/kappa");
			DIR_PARAM = new File(	"/data/experimentos/jcdl_2012/parametros/3Views_segNivel/sem_kappa");
			break;
		}
		
	}
	
	public static void executaCombinacaoViewsMetaLearning(
			boolean gravarViewsNoBanco, boolean gravaMetaLearningBanco,
			boolean gravarCombNoBanco) throws Exception {
		 
			

		HashMap<Colecao, HashMap<String, Resultado>> resultGeral = new HashMap<Colecao, HashMap<String, Resultado>>();
		
		//gera combinacoes
		Set<TipoFeatureCombinacao> lstComb = objCombinacoes;
		
		 
		for (int i = 0; i < cnfViewColecao.length; i++) {
			if ((tpoView == TIPO_VIEW.VIEW_POR_COLECAO_VERSUS_WIKIPEDIA && cnfViewColecao[i]
					.getColecao() == Colecao.WIKIPEDIA)
					|| (tpoView == TIPO_VIEW.VIEW_POR_COLECAO_VERSUS_SW_LABEL && cnfViewColecao[i]
							.getColecao() == Colecao.STARWARS_LABEL)) {
				continue;
			}
			String nomResult = "";
			HashMap<String, Resultado> result = new HashMap<String, Resultado>();
			boolean firstTime = true;
			for(TipoFeatureCombinacao objTipoComb : lstComb)
			{
				//aceitar apenas com global completo usando o conhecimento global/local
				if((USAR_CONHECIMENTO_LOCAL || USAR_CONHECIMENTO_GLOBAL_WIKI) && 
						objTipoComb.getNumViewsGlobal() < 6)
				{
					continue;
				}
				
				System.out.println("COMBINCAO: "+objTipoComb.getName());
				String nomExperimento = "";
				if (tpoView != TIPO_VIEW.VIEW_SIMPLES) {
					nomExperimento = "6views_MV"
							+ cnfViewColecao[i].getColecao().getSigla();
				} else {
					nomExperimento = ResultadoViews.CONFERENCE_EXP + "_" + EXP_ROOT
							+ "_" + cnfViewColecao[i].getColecao().getSigla();
				}
				if (cnfViewColecao[i] == ConfigViewColecao.WIKIPEDIA
						&& USAR_CONHECIMENTO_LOCAL) {
					cnfViewColecao[i]
							.addLstCnfView(ConfigViewColecao.WIKIPEDIA_CULTURE);
					cnfViewColecao[i]
							.addLstCnfView(ConfigViewColecao.WIKIPEDIA_GEOGRAPHY);
					cnfViewColecao[i]
							.addLstCnfView(ConfigViewColecao.WIKIPEDIA_HISTORY);
					cnfViewColecao[i]
							.addLstCnfView(ConfigViewColecao.WIKIPEDIA_SCIENCE);
					nomExperimento += "_global_and_local_";
				}
				
				if(USAR_CONHECIMENTO_GLOBAL_WIKI && firstTime)
				{
					cnfViewColecao[i].addLstCnfView(ConfigViewColecao.WIKIPEDIA);
					firstTime = false;
				}

				Resultado r;
				
				
				
				
				
				ConfigCustoGama objCustoGama = ConfigCustoGama.getCustoGama(MetaLearning.getFileNameParam(DIR_PARAM, cnfViewColecao[i], objTipoComb).getName(),DIR_PARAM);
				if(objCustoGama == null )
				{
					System.err.println("NAO ACHOU!"+MetaLearning.getFileNameParam(DIR_PARAM, cnfViewColecao[i], objTipoComb).getName());
					if(GRAVAR_COMBINACAO)
					{
						System.exit(0);
					}else
					{
						objCustoGama = new ConfigCustoGama(512.0,0.00048828125 , 0.5);
					}

				}
				System.out.println("ARQUIVO para a combinação: "+objTipoComb+" :"+objCustoGama.toString());
				
				
				
				//objCustoGama.setCusto(8192.0);
				//objCustoGama.setGama(0.0078125);
				//objCustoGama.setEpslon(0.5);
				//ConfigCustoGama objCustoGama = new ConfigCustoGama(512.0,0.00048828125 , 0.5);
				//System.exit(0);
				
				boolean wiki_local = false;
				nomResult = cnfViewColecao[i].getColecao().toString();
				switch (cnfViewColecao[i]) {
				case WIKIPEDIA_CULTURE:
					nomExperimento += "_culture";
					wiki_local = true;
					break;
				case WIKIPEDIA_GEOGRAPHY:
					nomExperimento += "_geography";
					wiki_local = true;
					break;
				case WIKIPEDIA_HISTORY:
					nomExperimento += "_history";
					wiki_local = true;
					break;
				case WIKIPEDIA_SCIENCE:
					nomExperimento += "_science";
					wiki_local = true;
					break;
				case WIKIPEDIA_RANDOM_1:
					nomExperimento += "_Random_1";
					wiki_local = true;
					break;				
				case WIKIPEDIA_RANDOM_2:
					nomExperimento += "_Random_2";
					wiki_local = true;
					break;				
				case WIKIPEDIA_RANDOM_3:
					nomExperimento += "_Random_3";
					wiki_local = true;
					break;				
				case WIKIPEDIA_RANDOM_5:
					nomExperimento += "_Random_5";
					wiki_local = true;
					break;	
				case WIKIPEDIA_CAT:
					break;
				}
				boolean setedAsLocal = false;
				if(wiki_local)
				{
					nomResult = nomExperimento;
				}
				if(wiki_local && objTipoComb.getNumViewsLocal() == 0)
				{
					objTipoComb.setAsLocal();
					setedAsLocal = true;
				}
				
				MetaLearning mt = metaLearning(nomExperimento+"_"+objTipoComb.getName(), cnfViewColecao[i],	gravaMetaLearningBanco, tpoConteudoDatasetML,objCustoGama);
				mt.setDirParamPrimTreino(DIR_PARAM);
				mt.setTipoCombinacao(objTipoComb);
				mt.setIgnoraLinhaSemResult(true);
				r = combinar(cnfViewColecao[i], mt, nomExperimento,	mt.getNomExperimento(), gravarViewsNoBanco,	gravarCombNoBanco);
				
				
				
				if(setedAsLocal)
				{
					objTipoComb.setAsGlobal();
				}
				
				result.put(cnfViewColecao[i].getColecao().getSigla()+"_"+nomExperimento+"_"+objTipoComb.getName(), r);
				
				
				
				//System.exit(0);
				
			}
			
			if(resultGeral.containsKey(cnfViewColecao[i].getColecao()))
			{
				HashMap<String, Resultado> resultAntigo = resultGeral.get(cnfViewColecao[i].getColecao());
				for(String key : resultAntigo.keySet())
				{
					
					result.put(key, resultAntigo.get(key));	
				}
				
			}
			resultGeral.put(cnfViewColecao[i].getColecao(), result);
			
			
		} 
		
		System.out.println("DIR Result: "+DIR_RESULT.getAbsolutePath());
		CalculaResultados.imprimeTabelaResultadoMSEPorFold(resultGeral, "\t", "\n", new File(DIR_RESULT,prefixResultado+"_resultado.xls"), 4, 10);
		

		String arqPrefixo = "resultados/wikiMultiview_result_";


		String arqPrefixoView = "resultados/view";
	}

	/**
	 * executa a combinacoes em todas as colecoes
	 * 
	 * @param gravarViewsNoBanco
	 * @param gravaMetaLearningBanco
	 * @param gravarCombNoBanco
	 * @throws Exception
	 */
	public static void executaCombinacaoTodasColecoes(
			boolean gravarViewsNoBanco, boolean gravaMetaLearningBanco,
			boolean gravarCombNoBanco) throws Exception {
		ConfigViewColecao[] cnfViewColecao = {
		// {
		//ConfigViewColecao.WIKIPEDIA,
		// ConfigViewColecao.WIKIPEDIA_CAT//,
		/*
		 * ConfigViewColecao.WIKIPEDIA_CULTURE,
		 * ConfigViewColecao.WIKIPEDIA_GEOGRAPHY,
		 * ConfigViewColecao.WIKIPEDIA_HISTORY,
		 * ConfigViewColecao.WIKIPEDIA_SCIENCE,
		 */
		/*
		 
		ConfigViewColecao.STARWARS_LABEL,
		
		*/
				ConfigViewColecao.MUPPETS,
				ConfigViewColecao.STARWARS_VOTE,
		};
		HashMap<ConfigViewColecao, HashMap<String, Resultado>> resultGeral = new HashMap<ConfigViewColecao, HashMap<String, Resultado>>();
		for (int i = 0; i < cnfViewColecao.length; i++) {
			if ((tpoView == TIPO_VIEW.VIEW_POR_COLECAO_VERSUS_WIKIPEDIA && cnfViewColecao[i]
					.getColecao() == Colecao.WIKIPEDIA)
					|| (tpoView == TIPO_VIEW.VIEW_POR_COLECAO_VERSUS_SW_LABEL && cnfViewColecao[i]
							.getColecao() == Colecao.STARWARS_LABEL)) {
				continue;
			}

			String nomExperimento = "";
			if (tpoView != TIPO_VIEW.VIEW_SIMPLES) {
				nomExperimento = "wikiMVTransfer_"
						+ cnfViewColecao[i].getColecao().getSigla();
			} else {
				nomExperimento = ResultadoViews.CONFERENCE_EXP + "_" + EXP_ROOT
						+ "_" + cnfViewColecao[i].getColecao().getSigla();
			}
			if (cnfViewColecao[i] == ConfigViewColecao.WIKIPEDIA
					&& USAR_CONHECIMENTO_LOCAL) {
				cnfViewColecao[i]
						.addLstCnfView(ConfigViewColecao.WIKIPEDIA_CULTURE);
				cnfViewColecao[i]
						.addLstCnfView(ConfigViewColecao.WIKIPEDIA_GEOGRAPHY);
				cnfViewColecao[i]
						.addLstCnfView(ConfigViewColecao.WIKIPEDIA_HISTORY);
				cnfViewColecao[i]
						.addLstCnfView(ConfigViewColecao.WIKIPEDIA_SCIENCE);
				nomExperimento += "_global_and_local_";
			}

			switch (cnfViewColecao[i]) {
			case WIKIPEDIA_CULTURE:
				nomExperimento += "_culture";
				break;
			case WIKIPEDIA_GEOGRAPHY:
				nomExperimento += "_geography";
				break;
			case WIKIPEDIA_HISTORY:
				nomExperimento += "_history";
				break;
			case WIKIPEDIA_SCIENCE:
				nomExperimento += "_science";
				break;
			case WIKIPEDIA_RANDOM_1:
				nomExperimento += "_Random_1";
				break;				
			case WIKIPEDIA_RANDOM_2:
				nomExperimento += "_Random_2";
				break;				
			case WIKIPEDIA_RANDOM_3:
				nomExperimento += "_Random_3";
				break;				
			case WIKIPEDIA_RANDOM_5:
				nomExperimento += "_Random_5";
				break;					
			case WIKIPEDIA_CAT:
				nomExperimento += "_cat_";
				break;
			}

			Resultado r;
			HashMap<String, Resultado> result = new HashMap<String, Resultado>();

			/*
			 * //pooling r = combinar(cnfViewColecao[i], new Poll(Tipo.REAL),
			 * nomExperimento,nomExperimento+"_Pooling_combinacao",
			 * gravarViewsNoBanco, gravarCombNoBanco); result.put("1-Polling",
			 * r);
			 * 
			 * //equation r = combinar(cnfViewColecao[i], new Equation(),
			 * nomExperimento,nomExperimento+"_Equation_combinacao",
			 * gravarViewsNoBanco, gravarCombNoBanco); result.put("2-Equation",
			 * r);
			 */

			// metaLearning
			// TIPO_CONTEUDO_DATASET[] tpoConteudoDatasetML =
			// {TIPO_CONTEUDO_DATASET.FEATURES_SET,TIPO_CONTEUDO_DATASET.PERTENCE_VIEW};

			// TIPO_CONTEUDO_DATASET[] tpoConteudoDatasetML =
			// {TIPO_CONTEUDO_DATASET.PERTENCE_VIEW};
			// TIPO_CONTEUDO_DATASET[] tpoConteudoDatasetML =
			// {TIPO_CONTEUDO_DATASET.FEATURES_SET};
			TIPO_CONTEUDO_DATASET[] tpoConteudoDatasetML = {};
			MetaLearning mt = metaLearning(nomExperimento, cnfViewColecao[i],
					gravaMetaLearningBanco, tpoConteudoDatasetML,null);
			r = combinar(cnfViewColecao[i], mt, nomExperimento,
					mt.getNomExperimento()/*
										 * nomExperimento+"_MetaLearning_combinacao"
										 */, gravarViewsNoBanco,
					gravarCombNoBanco);
			result.put("3-MetaLearning", r);
			if (DIR_PARAMS_EXP != null) {
				File fileDest = new File(DIR_PARAMS_EXP, r.getNomExperimento()
						+ ".out");
				ArquivoUtil.copyfile(r.getFolds()[0].getTreino(), fileDest);
			}
			
			/*
			 * //metaLearning MetaLearning mt2 =
			 * metaLearning(nomExperimento,cnfViewColecao[i],
			 * gravaMetaLearningBanco,true); r = combinar(cnfViewColecao[i],
			 * mt2,nomExperimento,
			 * nomExperimento+"_MetaLearning_combinacao_confianca",
			 * gravarViewsNoBanco, gravarCombNoBanco);
			 * result.put("3-MetaLearning_confianca", r);
			 * 
			 * 
			 * //Oraculo
			 * 
			 * r = combinar(cnfViewColecao[i], new CombinacaoOraculo(false),
			 * nomExperimento,nomExperimento+"_Oracle_combinacao",
			 * gravarViewsNoBanco, gravarCombNoBanco); result.put("4-Oracle",
			 * r);
			 * 
			 * resultGeral.put(cnfViewColecao[i], result);
			 */
			// combinacao apenas concordantes
			// SVM metAprendizadoNaoConcordante = new
			// SVM("testenovo",2F,0.5F,0.1F,SVM.MODE_REGRESSION,false,false);
			// metAprendizadoNaoConcordante.setArquivoOrigem(cnfViewColecao[i].getColecao().getArquivoOrigem());
			// CombinaApenasConcordantes cConcord = new
			// CombinaApenasConcordantes(new
			// Poll(Tipo.REAL),3,metAprendizadoNaoConcordante);
			// combinar(cnfViewColecao[i], cConcord,nomExperimento,
			// nomExperimento+"_MetaLearning_combinacao", gravarViewsNoBanco,
			// gravarCombNoBanco);
		}
		String arqPrefixo = "resultados/wikiMultiview_result_";
		// CalculaResultados.imprimeTabelaResultadMSEExcel(resultGeral, true,
		// new File(arqPrefixo+"_random.xls"),2);
		// CalculaResultados.imprimeTabelaResultadMSELatex(resultGeral, true,
		// new File(arqPrefixo+"_random.tex"),2);

		String arqPrefixoView = "resultados/view";
		// CalculaResultados.imprimeTabelaResultadMSEExcel(resultViewGeral,
		// true, new File(arqPrefixoView+"_random.xls"),2);
		// CalculaResultados.imprimeTabelaResultadMSELatex(resultViewGeral,
		// true, new File(arqPrefixoView+"_random.tex"),2);
	}

	/**
	 * executa a combinacoes em todas as colecoes
	 * 
	 * @param gravarViewsNoBanco
	 * @param gravaMetaLearningBanco
	 * @param gravarCombNoBanco
	 * @throws Exception
	 */
	public static void executaCombinacaoTodasColecoesRuido(
			boolean gravarViewsNoBanco, boolean gravarCombNoBanco)
			throws Exception {
		ConfigViewColecao[] cnfViewColecao = { ConfigViewColecao.WIKIPEDIA /*
																			 * ,
																			 * ConfigViewColecao
																			 * .
																			 * MUPPETS
																			 * ,
																			 * ConfigViewColecao
																			 * .
																			 * STARWARS_LABEL
																			 * ,
																			 * ConfigViewColecao
																			 * .
																			 * STARWARS_VOTE
																			 */
		};
		HashMap<Colecao, HashMap<String, Resultado>> resultGeral = new HashMap<Colecao, HashMap<String, Resultado>>();

		for (int i = 0; i < cnfViewColecao.length; i++) {
			String nomExperimento = "";
			if (tpoView != TIPO_VIEW.VIEW_SIMPLES) {
				nomExperimento = "wikiMVTransfer_"
						+ cnfViewColecao[i].getColecao().getSigla();
			} else {
				nomExperimento = "wikiMultiview_"
						+ cnfViewColecao[i].getColecao().getSigla();
			}
			HashMap<String, Resultado> result = new HashMap<String, Resultado>();

			// eliminação de ruido por porcentagem
			String nomExpRuido = nomExperimento + "_eliminaRuido";

			// eliminaRuido(gravarViewsNoBanco, gravarCombNoBanco,
			// cnfViewColecao, i, nomExperimento, result,
			// nomExpRuido+"_discordantes_mudaClasse_0", 0F, 0F,true);

			for (float agree = 0.5F; agree <= 1.5; agree += 0.5) {
				for (float dist = 2.0F; dist <= 2.0; dist += 0.5) {
					eliminaRuido(gravarViewsNoBanco, gravarCombNoBanco,
							cnfViewColecao, i, nomExperimento, result,
							nomExpRuido + "_discordantes_" + (agree * 10.0)
									+ "_" + (dist * 10.0), agree, dist, false);
				}
			}

			for (double porcRuido = 0.05; porcRuido <= 0.05; porcRuido += 0.1) {
				// eliminaRuido(gravarViewsNoBanco, gravarCombNoBanco,
				// cnfViewColecao,
				// i, nomExperimento, result,
				// nomExpRuido+"_discordantes_"+(porcRuido*100), porcRuido,
				// ComparationType.MSE);

				eliminaRuido(gravarViewsNoBanco, gravarCombNoBanco,
						cnfViewColecao, i, nomExperimento, result, nomExpRuido
								+ "_mse_" + (porcRuido * 100), porcRuido,
						ComparationType.MSE);

				/*
				 * 
				 * eliminaRuido(gravarViewsNoBanco, gravarCombNoBanco,
				 * cnfViewColecao, i, nomExperimento, result,
				 * nomExpRuido+"_variancia_erro_"+(porcRuido*100), porcRuido,
				 * ComparationType.VARIANCIA_ERRO);
				 * eliminaRuido(gravarViewsNoBanco, gravarCombNoBanco,
				 * cnfViewColecao, i, nomExperimento, result,
				 * nomExpRuido+"_variancia_erro_"+(porcRuido*100), porcRuido,
				 * ComparationType.MEAN); eliminaRuido(gravarViewsNoBanco,
				 * gravarCombNoBanco, cnfViewColecao, i, nomExperimento, result,
				 * nomExpRuido+"_variancia_classe_"+(porcRuido*100), porcRuido,
				 * ComparationType.VARIANCIA_CLASSE);
				 * eliminaRuido(gravarViewsNoBanco, gravarCombNoBanco,
				 * cnfViewColecao, i, nomExperimento, result,
				 * nomExpRuido+"_mse_"+(porcRuido*100), porcRuido,
				 * ComparationType.MSE);
				 */

			}

			resultGeral.put(cnfViewColecao[i].getColecao(), result);

		}
		String arqPrefixo = "resultados/wikiMultiview_ruido_result_";
		CalculaResultados.imprimeTabelaResultadMSEExcel(resultGeral, true,
				new File(arqPrefixo + "_apenas_porcentagem.xls"), 2, false);
		CalculaResultados.imprimeTabelaResultadMSELatex(resultGeral, true,
				new File(arqPrefixo + "_apenas_porcentagem.tex"), 2, false);

		String arqPrefixoView = "resultados/view";
		// CalculaResultados.imprimeTabelaResultadMSEExcel(resultViewGeral,
		// true, new File(arqPrefixoView+"_random.xls"),2);
		// CalculaResultados.imprimeTabelaResultadMSELatex(resultViewGeral,
		// true, new File(arqPrefixoView+"_random.tex"),2);
	}

	private static void eliminaRuido(boolean gravarViewsNoBanco,
			boolean gravarCombNoBanco, ConfigViewColecao[] cnfViewColecao,
			int i, String nomExperimento, HashMap<String, Resultado> result,
			String nomExpRuido, double porcRuido, ComparationType compType)
			throws Exception {
		Resultado r;
		Colecao col = cnfViewColecao[i].getColecao();
		EliminaRuidoTreino eliminaRuido = new EliminaRuidoTreino(false,
				porcRuido, false, true, compType, new SVM(nomExpRuido,
						(float) col.getCusto(), (float) col.getGama(), 0.1F,
						SVM.MODE_REGRESSION, false, gravarCombNoBanco, col));
		eliminaRuido.setOnlyHighClass(false);
		r = combinar(cnfViewColecao[i], eliminaRuido, nomExperimento,
				nomExpRuido, gravarViewsNoBanco, gravarCombNoBanco);
		result.put(nomExpRuido, r);
	}

	private static void eliminaRuido(boolean gravarViewsNoBanco,
			boolean gravarCombNoBanco, ConfigViewColecao[] cnfViewColecao,
			int i, String nomExperimento, HashMap<String, Resultado> result,
			String nomExpRuido, float agree, float distance, boolean changeClass)
			throws Exception {
		Resultado r;
		Colecao col = cnfViewColecao[i].getColecao();
		EliminaRuidoTreino eliminaRuido = new EliminaRuidoTreino(false, 0,
				ComparationType.DISCORDANTES, new SVM(nomExpRuido,
						(float) col.getCusto(), (float) col.getGama(), 0.1F,
						SVM.MODE_REGRESSION, false, gravarCombNoBanco, col));
		eliminaRuido.setOnlyHighClass(false);
		eliminaRuido.setThreshoulds(agree, distance, changeClass);
		r = combinar(cnfViewColecao[i], eliminaRuido, nomExperimento,
				nomExpRuido, gravarViewsNoBanco, gravarCombNoBanco);
		result.put(nomExpRuido, r);
	}

	public static void combinaConcordantes(boolean gravarViewsNoBanco,
			boolean gravaMetaLearningBanco, boolean gravarCombNoBanco)
			throws Exception {
		ConfigViewColecao[] cnfViewColecao = { ConfigViewColecao.MUPPETS,
				ConfigViewColecao.STARWARS_LABEL,
				ConfigViewColecao.STARWARS_VOTE, ConfigViewColecao.WIKIPEDIA };
		HashMap<ConfigViewColecao, HashMap<String, Resultado>> resultGeral = new HashMap<ConfigViewColecao, HashMap<String, Resultado>>();
		boolean apenasSVR = true;

		for (int i = 0; i < cnfViewColecao.length; i++) {
			HashMap<String, Resultado> result = new HashMap<String, Resultado>();

			String nomExperimento = "wikiMultiview_"
					+ cnfViewColecao[i].getColecao().getSigla();
			Resultado r;
			// SVR
			SVM metAprendizadoDiscordante = new SVM(nomExperimento
					+ "_discordantes", cnfViewColecao[i].getCSVRDiscord(),
					cnfViewColecao[i].getGSVRDiscord(),
					ConfigViewColecao.EPSLON, SVM.MODE_REGRESSION, false, false);
			metAprendizadoDiscordante.setArquivoOrigem(cnfViewColecao[i]
					.getColecao().getArquivoOrigem());

			// meta learning
			MetodoAprendizado metodoConcordMetaLearning = new SVM(
					nomExperimento + "_metaLearning_concordantes_conf",
					cnfViewColecao[i].getCMetaLearningConcord(),
					cnfViewColecao[i].getGMetaLearningConcord(),
					ConfigViewColecao.EPSLON, SVM.MODE_REGRESSION, false, false);
			MetodoAprendizado metodoDiscordMetaLearning = new SVM(
					nomExperimento + "_metaLearning_discordantes_conf",
					cnfViewColecao[i].getCMetaLearningDiscord(),
					cnfViewColecao[i].getGMetaLearningDiscord(),
					ConfigViewColecao.EPSLON, SVM.MODE_REGRESSION, false, false);
			TIPO_CONTEUDO_DATASET[] tpoConteudoDatasetML = { TIPO_CONTEUDO_DATASET.CONFIANCA };
			MetaLearning metLearningConcordante = new MetaLearning(
					metodoConcordMetaLearning, tpoConteudoDatasetML);
			MetaLearning metLearningDiscordante = new MetaLearning(
					metodoDiscordMetaLearning, tpoConteudoDatasetML);

			// Poll_SVR
			CombinaApenasConcordantes cConcord = new CombinaApenasConcordantes(
					new Poll(Tipo.REAL), 3, metAprendizadoDiscordante);
			r = combinar(cnfViewColecao[i], cConcord, nomExperimento,
					nomExperimento + "_POLL_SVR_combinacao",
					gravarViewsNoBanco, gravarCombNoBanco);
			result.put("poll+SVR", r);

			if (!apenasSVR) {
				// Poll_MetaLearning
				cConcord = new CombinaApenasConcordantes(new Poll(Tipo.REAL),
						3, metLearningDiscordante);
				r = combinar(cnfViewColecao[i], cConcord, nomExperimento,
						nomExperimento + "_Poll_MetaLearning_combinacao",
						gravarViewsNoBanco, gravarCombNoBanco);
				result.put("poll+metalearning", r);

				// Poll_Equation
				cConcord = new CombinaApenasConcordantes(new Poll(Tipo.REAL),
						3, new Equation());
				r = combinar(cnfViewColecao[i], cConcord, nomExperimento,
						nomExperimento + "_POLL_EQUATION_combinacao",
						gravarViewsNoBanco, gravarCombNoBanco);
				result.put("poll+equation", r);
			}
			// Equation_SVR
			cConcord = new CombinaApenasConcordantes(new Equation(), 3,
					metAprendizadoDiscordante);
			r = combinar(cnfViewColecao[i], cConcord, nomExperimento,
					nomExperimento + "_Equation_SVR_combinacao",
					gravarViewsNoBanco, gravarCombNoBanco);
			result.put("equation+svr", r);

			if (!apenasSVR) {
				// Equation_MetaLearning
				cConcord = new CombinaApenasConcordantes(new Equation(), 3,
						metLearningDiscordante);
				r = combinar(cnfViewColecao[i], cConcord, nomExperimento,
						nomExperimento + "_Equation_MetaLearning",
						gravarViewsNoBanco, gravarCombNoBanco);
				result.put("equation+metalearning", r);

			}

			// Metalearning_MetaLearning
			cConcord = new CombinaApenasConcordantes(metLearningConcordante, 3,
					metLearningDiscordante);
			r = combinar(cnfViewColecao[i], cConcord, nomExperimento,
					nomExperimento
							+ "_MetaLearning_MetaLearning_combinacao_conf",
					gravarViewsNoBanco, gravarCombNoBanco);
			result.put("metalearning+metalearning", r);

			// Metalearning_SVR
			/*
			 * cConcord = new
			 * CombinaApenasConcordantes(metLearningConcordante,3,
			 * metAprendizadoDiscordante); r = combinar(cnfViewColecao[i],
			 * cConcord,nomExperimento,
			 * nomExperimento+"_MetaLearning_SVR_combinacao",
			 * gravarViewsNoBanco, gravarCombNoBanco);
			 * result.put("metalearning+svr", r);
			 * 
			 * if(!apenasSVR) { //Metalearning_MetaLearning cConcord = new
			 * CombinaApenasConcordantes
			 * (metLearningConcordante,3,metLearningDiscordante); r =
			 * combinar(cnfViewColecao[i], cConcord,nomExperimento,
			 * nomExperimento+"_MetaLearning_MetaLearning_combinacao",
			 * gravarViewsNoBanco, gravarCombNoBanco);
			 * result.put("metalearning+metalearning", r);
			 * 
			 * //Metalearning_Equation cConcord = new
			 * CombinaApenasConcordantes(metLearningConcordante,3,new
			 * Equation()); r = combinar(cnfViewColecao[i],
			 * cConcord,nomExperimento,
			 * nomExperimento+"_MetaLearning_Equation_combinacao",
			 * gravarViewsNoBanco, gravarCombNoBanco);
			 * result.put("metalearning+equation", r); }
			 */

			resultGeral.put(cnfViewColecao[i], result);
		}
		String arqPrefixo = "resultados/wikiMultiview_concordantes";
		// CalculaResultados.imprimeTabelaResultadMSEExcel(resultGeral, true,
		// new File(arqPrefixo+".xls"),2);
		// CalculaResultados.imprimeTabelaResultadMSELatex(resultGeral, true,
		// new File(arqPrefixo+".tex"),2);

	}

	/**
	 * executa a combinacoes em todas as colecoes
	 * 
	 * @param gravarViewsNoBanco
	 * @param gravaMetaLearningBanco
	 * @param gravarCombNoBanco
	 * @throws Exception
	 */
	public static void executaCombinacaoTodasColecoes(AbordagemCombinacao comb,
			boolean gravarViewsNoBanco, boolean gravaMetaLearningBanco,
			boolean gravarCombNoBanco) throws Exception {
		ConfigViewColecao[] cnfViewColecao = { ConfigViewColecao.MUPPETS,
				ConfigViewColecao.STARWARS_LABEL,
				ConfigViewColecao.STARWARS_VOTE, ConfigViewColecao.WIKIPEDIA };

		for (int i = 0; i < cnfViewColecao.length; i++) {
			String nomExperimento = "wikiMultiview_"
					+ cnfViewColecao[i].getColecao().getSigla();

			combinar(cnfViewColecao[i], comb, nomExperimento, nomExperimento
					+ "_concordancia_combinacao", gravarViewsNoBanco,
					gravarCombNoBanco);
		}
	}

	/**
	 * Retorna o metalearning do banco
	 * 
	 * @param cnfColecao
	 * @param gravarMetaLearningNoBanco
	 * @return
	 * @throws IOException
	 */
	public static MetaLearning metaLearning(String nomExperimento,
			ConfigViewColecao cnfColecao, boolean gravarMetaLearningNoBanco,
			TIPO_CONTEUDO_DATASET[] tpoConteudoDatasetML,ConfigCustoGama objCustoGama) throws IOException {

		float custo = cnfColecao.getCMetaLearning();
		float gama = cnfColecao.getGMetaLearning();
		float epslon = 0.1F;
		boolean utilizarConfianca = false;
		boolean contemFeatures = false;
		boolean usarCategoria = false;
		String sufixExp = "_metalearning";
		
		for (TIPO_CONTEUDO_DATASET tpo : tpoConteudoDatasetML) {
			if (tpo == TIPO_CONTEUDO_DATASET.CONFIANCA) {
				utilizarConfianca = true;
				sufixExp += "_conf";
			}
			if (tpo == TIPO_CONTEUDO_DATASET.FEATURES_SET) {
				contemFeatures = true;
				sufixExp += "_feat";
			}
			if (tpo == TIPO_CONTEUDO_DATASET.PERTENCE_VIEW) {
				usarCategoria = true;
				sufixExp += "_cat";
			}
		}
		sufixExp += "_" + tpoView.toString().toLowerCase();
		if (View.tpoDivisao != View.TIPO_DIVISAO_TREINO.THREE_FOLD) {
			sufixExp += "_" + View.tpoDivisao.toString().toLowerCase();
		}
		
		ConfigCustoGama custoGama = null;
		if(objCustoGama == null)
		{
			cnfColecao.setMultiviewCustoGamaParameters(contemFeatures, true);// View.tpoDivisao
																			// ==														// View.TIPO_DIVISAO_TREINO.THREE_FOLD);
			custoGama = cnfColecao.getCustoGamaCategoria(
					contemFeatures, usarCategoria);
		}else
		{
			custoGama = objCustoGama;
		}
		// sufixExp += "_threeFoldParam";

		if (utilizarConfianca) {
			custo = cnfColecao.getCMetaLearnSI();
			gama = cnfColecao.getGMetaLearnSI();
		}

		
		if (custoGama == null) {
			custoGama = cnfColecao.getCustoGamaMultiview(tpoView);
		}

		if (custoGama != null) {
			custo = (float) (double) custoGama.getCusto();
			gama = (float) (double) custoGama.getGama();
			System.out.println("Parametros Experimento: " + nomExperimento
					+ sufixExp + " param: " + custoGama.toString());
		} else {
			System.out
					.println("Nao foi possível achar os parametros do experimento: "
							+ nomExperimento + sufixExp);
			System.exit(0);
		}
		//System.exit(0);
		MetodoAprendizado metodo = new SVM(nomExperimento + sufixExp, custo,
				gama, objCustoGama.getEpslon().floatValue(), ConfigViewColecao.MODE, false,
				gravarMetaLearningNoBanco);
		metodo.setColecao(cnfColecao.getColecao());
		return new MetaLearning(metodo, tpoConteudoDatasetML);
	}

	/**
	 * Utiliza um método de combinação para as views Cria um seletor de views,
	 * esse seletor mostra a confianca de selecionar cada view isso ajuda alguns
	 * combinadores (meta learning, por ex)
	 * 
	 * @param ab
	 *            - Abordagem de combinacao utilizada
	 * @param nomExperimento
	 *            - Nome do Experimento
	 * @param gravarViewsBanco
	 *            Grava ou nao o resultado de cada view no banco
	 * @param gravarCombinacaoBanco
	 *            Grava ou nao o resultado da combinação no banco
	 * @throws Exception
	 */
	public static Resultado combinar(ConfigViewColecao cViewColecao,
			AbordagemCombinacao ab, String nomExperimentoView,
			String nomExperimento, boolean gravarViewsBanco,
			boolean gravarCombinacaoBanco) throws Exception {

		View[] vs = null;
		switch (tpoView) {
		case BASELINE_VIEW:
			vs = cViewColecao.getViewsColecaoAtual(gravarViewsBanco);
			break;
		case VIEW_POR_COLECAO_VERSUS_WIKIPEDIA:
			vs = cViewColecao.getViews2Colecao(Colecao.WIKIPEDIA,
					gravarViewsBanco);
			break;
		case VIEW_POR_COLECAO_VERSUS_SW_LABEL:
			vs = cViewColecao.getViews2Colecao(Colecao.STARWARS_LABEL,
					gravarViewsBanco);
			break;
		case VIEW_POR_COLECAO_TODOS:
			vs = cViewColecao.getViewsTodosColecao(gravarViewsBanco);
			break;
		case VIEW_POR_COLECAO_EXCTO_ATUAL:
			vs = cViewColecao.getViewsTodosColecaoExcetoAtual(gravarViewsBanco);
			break;
		case VIEW_SIMPLES:
			if(ResultadoViews.TIPO_SUB_EXPERIMENTO.SEIS_VISOES == ResultadoViews.TPO_SUB_EXPERIMENTO)
			{
				vs = cViewColecao.getSixViews(nomExperimentoView, gravarViewsBanco);
			}else{
				vs = cViewColecao.getViews(nomExperimentoView, gravarViewsBanco);
				
			}
			break;
		case VIEW_LAC_SIMPLES:
			vs = cViewColecao.getSixViewsLacSVM(nomExperimentoView, GRAVAR_COMBINACAO);
			
			
			break;
		}

		SVM svmSeletorView = null;/*
								 * new SVM(nomExperimentoView + "_selView",
								 * cViewColecao.getCSeletorIdeal(), cViewColecao
								 * .getGSeletorIdeal(), 1.0F,
								 * SVM.MODE_CLASSIFICATION, true,
								 * gravarViewsBanco)
								 */
		// svmSeletorView.setColecao(cViewColecao.getColecao());
		Combinador c = new Combinador("Combinacao-" + nomExperimento,
				svmSeletorView, ab, vs);

		Resultado r = c.executaCombinacao(gravarCombinacaoBanco, cViewColecao
				.getColecao().getSigla());

		// grava resultado da view
		HashMap<String, Resultado> resultView = new HashMap<String, Resultado>();
		for (int i = 0; i < vs.length; i++) {
			resultView.put(vs[i].getNomExperimento(), vs[i].getResultTeste());
		}
		resultViewGeral.put(cViewColecao, resultView);

		return r;
	}

	public static void main(String[] args) {
		try {
			/*
			boolean gravarViewsNoBanco = false;
			boolean gravaMetaLearningBanco = true;
			boolean gravaCombinacaoBanco = false;
			*/
			// executaCombinacaoTodasColecoesRuido(gravarViewsNoBanco,gravaCombinacaoBanco);
			// combinaConcordantes(gravarViewsNoBanco,gravaMetaLearningBanco,gravaCombinacaoBanco);
			executaCombinacaoViewsMetaLearning(GRAVAR_VIEW,
					GRAVAR_COMBINACAO, false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
