package apredizadoCombinacao;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import stuctUtil.ArrayUtil;

import aprendizadoResultado.CalculaResultados;
import aprendizadoUtils.MetodoAprendizado;
import arquivo.ArquivoUtil;
import banco.GerenteBD;
import config_tmp.ConfigViewColecao;
import entidadesAprendizado.Fold;
import entidadesAprendizado.Resultado;
import entidadesAprendizado.ResultadoItem;
import entidadesAprendizado.ResultadoViews;
import entidadesAprendizado.View;

public class Combinador
{
	private View[] views;
	private ResultadoViews resultViews;
	private AbordagemCombinacao abordagem;
	private String nomExperimento;
	private MetodoAprendizado metSeletor;
	private final boolean imprimeResultadoPorView = false;

	/**
	 * Combinador como o nome do experimento, a abordagem de combinação e as
	 * vies
	 * 
	 * @param nomExperimento
	 * @param abordagem
	 * @param views
	 */
	public Combinador(String nomExperimento, AbordagemCombinacao abordagem,
			View[] views)
	{
		this.views = views;
		this.abordagem = abordagem;
		this.nomExperimento = nomExperimento;
	}
	public Combinador(String nomExperimento, AbordagemCombinacao abordagem,
			View[] views,ResultadoViews rv)
	{
		this.views = views;
		this.abordagem = abordagem;
		this.nomExperimento = nomExperimento;
		this.resultViews = rv;
	}
	

	/**
	 * Combinador como o nome do experimento, a abordagem de combinação e as
	 * vies
	 * 
	 * @param nomExperimento
	 * @param metSeletor
	 *            - Metodo para aprender a selecionar a melhor view
	 * @param abordagem
	 * @param views
	 */
	public Combinador(String nomExperimento, MetodoAprendizado metSeletor,
			AbordagemCombinacao abordagem, View[] views)
	{
		this(nomExperimento, abordagem, views);
		this.metSeletor = metSeletor;
	}
	public Resultado executaCombinacao(boolean gravarNoBanco, String diretorio) throws Exception
	{
		return  executaCombinacao( gravarNoBanco,  diretorio,"id");
	}
	public Resultado executaCombinacao(boolean gravarNoBanco, String diretorio,String idGrouper)
			throws Exception
	{
		File arq = criaArquivo(diretorio);

		// efetua a combinacao de cada resultado da view
		Resultado r = efetuaCombinacao(gravarNoBanco, arq, idGrouper);
		return r;
	}
	private Resultado efetuaCombinacao(boolean gravarNoBanco, File arq) throws IOException, Exception
	{
		return efetuaCombinacao( gravarNoBanco,  arq,"id");
	}
	private Resultado efetuaCombinacao(boolean gravarNoBanco, File arq,String idGrouper)
			throws Exception, IOException
	{
		Resultado r = this.combinar(this.abordagem.getClass().getName() + "_"
				+ this.nomExperimento, views, gravarNoBanco,idGrouper);
		r.setView(views);
		MetodoAprendizado objMetAprendizado = views[0].getMetodoAprendizado();;
		/*
		if (imprimeResultadoPorView)
		{
			
			ArquivoUtil
					.gravaTexto(
							"\n\n\n=================================Resultado por view========================\n",
							arq, true);
			System.out
					.println("==================RESULTADO POR VIEW=========================");
			
			for (int i = 0; i < views.length; i++)
			{
				System.out.println("View: " + views[i].getNomExperimento());
				System.out.println("Resultado dos folds treino:");
				objMetAprendizado = views[i].getMetodoAprendizado();
				if(objMetAprendizado.isClassificacao())
				{
					
					//System.out.println(CalculaResultados.resultadoClassificacaoToString(resultTreino,metodoAprendizado.getNumClasses(),new File (foldsTeste[0].getTreino().getParent(),nomExperimento+"_treino")));
					System.out.println(CalculaResultados
							.resultadoClassificacaoToString(views[i].getResultTreino(),objMetAprendizado.getNumClasses(),
									arq));
				}else
				{
					System.out.println(CalculaResultados
							.resultadoRegressaoToString(views[i].getResultTreino(),
									arq));	
				}
				
				System.out.println("Resultado dos folds teste:");
				if(objMetAprendizado.isClassificacao())
				{
					System.out.println(CalculaResultados
							.resultadoClassificacaoToString(views[i].getResultTeste(),objMetAprendizado.getNumClasses(),
									arq));
				}else
				{
					System.out.println(CalculaResultados
							.resultadoRegressaoToString(views[i].getResultTeste(),
									arq));	
				}
				
				System.out.println("--------------------");
				System.out.println("");
				System.out.println("");
				System.out.println("");
			}
		}
		
		//System.exit(0);
		System.out
				.println("========================Resultado da combinacao====================");
		ArquivoUtil
				.gravaTexto(
						"\n\n\n=================================Resultado da Combinação========================\n",
						arq, true);
						*/
		/*
		if(objMetAprendizado.isClassificacao())
		{
			System.out.println(CalculaResultados
					.resultadoClassificacaoToString(r,objMetAprendizado.getNumClasses(),
							arq));
		}else
		{
			//System.out.println(CalculaResultados.resultadoRegressaoToString(r, arq));	
		}
		*/
		return r;
	}

	private File criaArquivo(String diretorio) throws IOException, Exception
	{
		File dir = null;
		if (diretorio.startsWith("/"))
		{
			dir = new File(diretorio);
		} else
		{
			dir = new File("resultados/" + diretorio);
		}
		if (!dir.exists())
		{
			dir.mkdirs();
		}
		File arq = new File(dir.getAbsolutePath() + "/" + this.nomExperimento
				+ ".result");

		if (arq.exists())
		{
			arq.delete();
		} else
		{
			ArquivoUtil.gravaTexto("", arq, false);
		}

		return arq;
	}

	public Resultado combinar(String nomExperimento, View[] views,
			boolean gravaNoBanco) throws Exception
	{
		return combinar( nomExperimento, views,
				 gravaNoBanco,"id");
	}
	public Resultado combinar(String nomExperimento, View[] views,
			boolean gravaNoBanco,String idGrouper) throws Exception
	{
		/******************** Faz resultado de cada visao ***********/
		// computa e retorna resultado por view teste/treino
		this.resultViews = ResultadoViews.getResultadoViewsObject(views,this.metSeletor,idGrouper);
		
		views = resultViews.getViews();
		System.out.println("==============apenas para grava views===========");
		//System.exit(0);
		
		//filtra e soh coloca as views q existem em this.views 
		 List<View> lstUpdated = new ArrayList<View>();
		 for(int i =0 ; i<this.views.length ; i++)
		 {
			View viewUpdated = null;
			View viewAtual = this.views[i];
			for(View v : resultViews.getViews())
			{
				if(v.getArquivo().equals(viewAtual.getArquivo()))
				{
					viewUpdated = v;
					break;
				}
			}
			if(viewUpdated != null)
			{
				lstUpdated.add(viewUpdated);
			}else
			{
				System.err.println("Não foi possivel achar a view: "+viewAtual.getArquivo());
				System.exit(0);
			}
			
		 }
		 for(int i = 0 ; i<this.views.length ; i++)
		 {
				 this.views[i] = lstUpdated.get(i);
		 }

		
		
		/************* Executa combinacao *****************/
		return executaCombinacao(nomExperimento, this.views, gravaNoBanco);
		
		
	}
	private Fold[] filtraFoldPorViews(Fold[] foldPorView,View[] arrViews) throws Exception
	{
		Fold[] foldPorViewFiltrado = new Fold[views.length];
		int idxFiltro = 0;
		//procura os folds pertencentes a arrView
		for(int i = 0 ; i<foldPorView.length ; i++)
		{
			boolean encontrou = false;
			for(View v : arrViews)
			{
				File origem =foldPorView[i].getView()[0].getArquivo();
				
				
				
				if(origem.equals(v.getArquivo()))	
				{
					encontrou = true;
					break;
				}
			}
			if(encontrou)
			{
				foldPorViewFiltrado[idxFiltro] = foldPorView[i];
				idxFiltro++;
			}
			
		}
		//verifica se achou todos os folds
		for(Fold f : foldPorViewFiltrado)
		{
			if(f==null)
			{
				throw new Exception("Não foi possivel encontrar todas as visões para o filtro ");
			}
		}
		return foldPorViewFiltrado;
	}
	public Resultado executaCombinacao(String nomExperimento, View[] views,
			boolean gravaNoBanco) throws Exception, ClassNotFoundException,
			SQLException {
		Fold[][] resultPorViewTreino = resultViews.getResultadoPorViewTreino();
		Fold[][] resultPorViewTeste = resultViews.getResultadoPorViewTeste();
		Fold[][] resultPorViewValidacao = resultViews.getResultadoPorViewValidacao();

		// cria resultado
		int numFolds = views[0].getResultTeste().getFolds().length;
		this.abordagem.calculoPreTreino(views);
		Fold[] foldResultado = new Fold[numFolds];
		System.out.println("Criando combinação....");
		for (int foldNum = 0; foldNum < numFolds; foldNum++)
		{
			
			Fold[] arrFoldPorViewTreino = views.length==resultPorViewTreino[foldNum].length?resultPorViewTreino[foldNum]:filtraFoldPorViews(	resultPorViewTreino[foldNum],views);
			Fold[] arrFoldPorViewTeste = views.length==resultPorViewTeste[foldNum].length?resultPorViewTeste[foldNum]:filtraFoldPorViews(	resultPorViewTeste[foldNum],views);
			Fold[] arrFoldPorViewValidacao = null;
			if(resultPorViewValidacao[foldNum].length>0 && resultPorViewValidacao[foldNum][0]!=null)
			{
				arrFoldPorViewValidacao = views.length==resultPorViewValidacao[foldNum].length?resultPorViewValidacao[foldNum]:filtraFoldPorViews(	resultPorViewValidacao[foldNum],views);
			}
			
			foldResultado[foldNum] = this.abordagem.combinarResultadoFold(	arrFoldPorViewTreino,arrFoldPorViewTeste,arrFoldPorViewValidacao);
		}

		// grava no banco se necessario
		if (gravaNoBanco)
		{
			Connection conn = GerenteBD.getGerenteBD().obtemConexao("");
			conn.setAutoCommit(false);

			for (int i = 0; i < foldResultado.length; i++)
			{
				foldResultado[i].inserir();
				foldResultado[i].inserirParams();
				foldResultado[i].getResultadosValues().get(0)
						.excluirProbResult(nomExperimento);

				Iterator<ResultadoItem> j = foldResultado[i]
						.getResultadosValues().iterator();
				while (j.hasNext())
				{
					ResultadoItem r = j.next();
					r.gravaResultadoBanco(nomExperimento);
				}
			}
			System.out.println("Gravado no banco  como: "+nomExperimento);
			conn.commit();
			conn.setAutoCommit(true);
		}
		/*
		for(int i = 0 ; i< foldResultado.length ; i++)
		{
			System.out.println("Fold #"+i+": "+foldResultado[i].getAcuracia());
		}
		*/
		// calcula o teste e treino
		Resultado r = new Resultado(nomExperimento, foldResultado,
				this.abordagem.getMatrizCombinacao());

		// TODO Auto-generated method stub
		return r;
	}

	public static void main(String[] args)
	{
		try
		{
			AbordagemCombinacao abComb = new Poll();
			String nomExperimento = "wikiMultiview";
			boolean gravarNoBanco = true;
			View[] vs = ConfigViewColecao.WIKIPEDIA.getViews(nomExperimento,
					gravarNoBanco);
			Combinador c = new Combinador("Combinacao-" + nomExperimento,
					abComb, vs);
			c.executaCombinacao(false, "teste");
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
