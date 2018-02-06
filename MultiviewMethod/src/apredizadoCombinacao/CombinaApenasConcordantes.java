package apredizadoCombinacao;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;


import apredizadoCombinacao.Poll.Tipo;
import aprendizadoResultado.ResultadosWikiMultiviewMetodos;
import aprendizadoUtils.MetodoAprendizado;
import aprendizadoUtils.SVM;
import config_tmp.ConfigViewColecao;
import entidadesAprendizado.Fold;
import entidadesAprendizado.MatrizConfusao;
import entidadesAprendizado.ResultadoItem;
import entidadesAprendizado.ResultadoItemVoto;
import entidadesAprendizado.View;
/**
 * Utiliza um método de combinação apenas para as views concordantes,
 * Caso nao concorda utiliza ou um metodo de aprendizado sob os nao concordantes ou um metodo de combinção tb.
 * @author hasan
 *
 */
public class CombinaApenasConcordantes extends AbordagemCombinacao 
{
	private int numMinConcordancia;
	private AbordagemCombinacao combinacaoConcordante;
	
	private AbordagemCombinacao metCombinacaoNaoConcordante;
	private MetodoAprendizado metAprendizadoNaoConcordante;
	
	public CombinaApenasConcordantes(AbordagemCombinacao combConcordante,int numMinConcordancia,AbordagemCombinacao metCombinacaoNaoConcordante)
	{
		this.combinacaoConcordante = combConcordante;
		this.numMinConcordancia = numMinConcordancia;
		this.metCombinacaoNaoConcordante = metCombinacaoNaoConcordante;
	}
	public CombinaApenasConcordantes(AbordagemCombinacao combConcordante,int numMinConcordancia,MetodoAprendizado metAprendizadoNaoConcordante)
	{
		this.combinacaoConcordante = combConcordante;
		this.numMinConcordancia = numMinConcordancia;
		this.metAprendizadoNaoConcordante = metAprendizadoNaoConcordante;
	}
	public void calculoPreTreino(View[] views) throws Exception
	{
		Poll pollExtraiConcordante = new Poll(Tipo.REAL);
		
		//caso exista um metodo de combinacao para concordar/discordar, passar os parametros das views, removendo os resultados discordantes
		


		//para cada fold, separa os discordantes dos concordantes
		int numFolds = views[0].getResultTeste().getFolds().length;
		ArrayList<ResultadoItem> idsDiscordante = new ArrayList<ResultadoItem>();
		ArrayList<ResultadoItem> idsConcordante = new ArrayList<ResultadoItem>();
		for(int foldNum =0 ; foldNum < numFolds ; foldNum++)
		{
			//prepara para visualizar resultados
			Fold[] foldResultTreino = new Fold[views.length];
			Fold[] foldResultTeste = new Fold[views.length];
			for(int i = 0 ; i<views.length ; i++)
			{
				foldResultTreino[i] = views[i].getResultTreino().getFolds()[foldNum];
				foldResultTeste[i] = views[i].getResultTeste().getFolds()[foldNum];
			}
			
			Fold fold = pollExtraiConcordante.combinarResultadoFold(foldResultTreino, foldResultTeste);
			Iterator<ResultadoItem> results = fold.getResultadosValues().iterator();
			while(results.hasNext())
			{
				ResultadoItemVoto r = (ResultadoItemVoto) results.next();
				if(r.getNumConcordantes() >= numMinConcordancia)
				{
					//resultado concordante
					idsConcordante.add(r);
				}else
				{
					//resultado discordante
					idsDiscordante.add(r);
					
				}
			}
		}
		
		//caso tenha um metodo de aprendizado...
		if(metAprendizadoNaoConcordante != null)
		{
			File arquivoOrigem = new File(metAprendizadoNaoConcordante.getArquivoOrigem().getAbsolutePath()+"_discordantes");
			metAprendizadoNaoConcordante.filtraArquivoPorIds(ResultadoItem.getIdsResultadoItem(idsDiscordante), arquivoOrigem);
			metAprendizadoNaoConcordante.filtraIDsArquivo(arquivoOrigem,new File(arquivoOrigem.getAbsoluteFile()+".out"));
			System.out.println("FILTRANDO DISCORDANTES...");
		}
		
		//caso seja uma combinacao
		View[] viewConcordante = new View[views.length];
		View[] viewDiscordante = new View[views.length];

		 
		//inicializa novas views
		for(int i = 0 ; i<viewConcordante.length ; i++)
		{
			viewDiscordante[i] = new View(views[i].getCnfView(),views[i].getArquivo(),views[i].getMetodoAprendizado());
			viewConcordante[i] = new View(views[i].getCnfView(),views[i].getArquivo(),views[i].getMetodoAprendizado());
			viewConcordante[i].setResultado(views[i].getResultTreino().clonaResultadoFiltrado(idsConcordante,"concordante_"), views[i].getResultTeste().clonaResultadoFiltrado(idsConcordante,"concordante_"));
			viewDiscordante[i].setResultado(views[i].getResultTreino().clonaResultadoFiltrado(idsDiscordante,"disconcordante_"), views[i].getResultTeste().clonaResultadoFiltrado(idsDiscordante,"disconcordante_"));
		}
		
		//calcula pre treino  
		combinacaoConcordante.calculoPreTreino(viewConcordante);
		if(metCombinacaoNaoConcordante != null)
		{
			metCombinacaoNaoConcordante.calculoPreTreino(viewDiscordante);
		}
	}
	public ArrayList<ResultadoItem> getConcorcordantes(ArrayList<ResultadoItem> result)
	{
		ArrayList<ResultadoItem> concordantes = new ArrayList<ResultadoItem>();
		Iterator<ResultadoItem> i = result.iterator();
		while(i.hasNext())
		{
			ResultadoItemVoto r = (ResultadoItemVoto) i.next();
			if(r.getNumConcordantes() >= numMinConcordancia)
			{
				concordantes.add(r);
			}
		}
		return concordantes;
	}
	@Override
	public Fold combinarResultadoFold(Fold[] resultPorViewTreino,
			Fold[] resultPorViewTeste) throws Exception {
		//poll para extrair concordantes
		Poll poll = new Poll(Tipo.REAL);
		ArrayList<ResultadoItem> resultTreino = poll.getResultVotacao(resultPorViewTreino,resultPorViewTreino);
		ArrayList<ResultadoItem> resultTeste = poll.getResultVotacao(resultPorViewTeste,resultPorViewTreino);
		
		//separa os concordantes dos discordantes
		ArrayList<ResultadoItem> concordantesTreino = getConcorcordantes(resultTreino);
		ArrayList<ResultadoItem> discordantesTreino = new ArrayList<ResultadoItem>(resultTreino);
		discordantesTreino.removeAll(concordantesTreino);
		
		
		ArrayList<ResultadoItem> concordantesTeste = getConcorcordantes(resultTeste);
		ArrayList<ResultadoItem> discordantesTeste = new ArrayList<ResultadoItem>(resultTeste);
		discordantesTeste.removeAll(concordantesTeste);
		
		/**************** Pre processamento dos concordantes e discordantes*/
		//cria fold de result para combinacao 
		Fold[] resultConcordantePorViewTreino = new Fold[resultPorViewTreino.length];
		Fold[] resultConcordantePorViewTeste = new Fold[resultPorViewTeste.length];
		
		Fold[] resultDiscordantePorViewTreino = new Fold[resultPorViewTreino.length];
		Fold[] resultDiscordantePorViewTeste = new Fold[resultPorViewTeste.length];		
		
		for(int v = 0 ; v<resultPorViewTreino.length ; v++)
		{
			resultConcordantePorViewTreino[v] = resultPorViewTreino[v].clonaFoldFiltrandoResultados(concordantesTreino, "concordante");	
			resultDiscordantePorViewTreino[v] = resultPorViewTreino[v].clonaFoldFiltrandoResultados(discordantesTreino, "discordante");
			
			resultConcordantePorViewTeste[v] = resultPorViewTeste[v].clonaFoldFiltrandoResultados(concordantesTeste, "concordante");
			resultDiscordantePorViewTeste[v] = resultPorViewTeste[v].clonaFoldFiltrandoResultados(discordantesTeste, "discordante");
		}
		
		/************* Concordantes **/
		//combinacao
		Fold fGeral = this.combinacaoConcordante.combinarResultadoFold(resultConcordantePorViewTreino, resultConcordantePorViewTeste);
		fGeral.setOrigem(resultDiscordantePorViewTeste[0].getOrigem());
		

		/************* Discordantes **/	
		//caso seja combinacao 
		if(this.metCombinacaoNaoConcordante != null)
		{			
			Fold fDiscordante = this.metCombinacaoNaoConcordante.combinarResultadoFold(resultDiscordantePorViewTreino, resultDiscordantePorViewTeste);
			fGeral.adicionaTodosResultados(fDiscordante.getResultadosValues());
		}else
		{
			Fold resultParaMetodoAprendizado = this.metAprendizadoNaoConcordante.criaFoldComIdsFiltrado(resultPorViewTreino[0].getNum(), ResultadoItem.getIdsResultadoItem(discordantesTreino), ResultadoItem.getIdsResultadoItem(discordantesTeste));
			//	caso contrario, cria fold de aprendizado com os discordantes e o aplica, se necessario
			ArrayList<ResultadoItem> resultDiscord = this.metAprendizadoNaoConcordante.testar(resultParaMetodoAprendizado);
			fGeral.adicionaTodosResultados(resultDiscord);
			
			fGeral.setModeloTreino(resultParaMetodoAprendizado.getModeloTreino());
			fGeral.setTreino(resultParaMetodoAprendizado.getTreino());
			fGeral.setTeste(resultParaMetodoAprendizado.getTeste());
			fGeral.setPredict(resultParaMetodoAprendizado.getPredict());
			fGeral.setIdsFile(resultParaMetodoAprendizado.getIdsFile());
		}
		

		return fGeral;
	}
	@Override
	public MatrizConfusao getMatrizCombinacao() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static void main(String[] args)
	{
		try {
			SVM metAprendizadoNaoConcordante = new SVM("testenovo",ConfigViewColecao.WIKIPEDIA.getCSVRDiscord(),ConfigViewColecao.WIKIPEDIA.getGSVRDiscord(),0.1F,SVM.MODE_REGRESSION,false,false);
			metAprendizadoNaoConcordante.setArquivoOrigem(ConfigViewColecao.WIKIPEDIA.getColecao().getArquivoOrigem());
			String nomExperimento = "wikiMultiview_"+ConfigViewColecao.WIKIPEDIA.getColecao().getSigla();
						
			
			//Poll_SVR			
			CombinaApenasConcordantes cConcord = new CombinaApenasConcordantes(new Poll(Tipo.REAL),3,metAprendizadoNaoConcordante);
			ResultadosWikiMultiviewMetodos.combinar(ConfigViewColecao.WIKIPEDIA, cConcord,nomExperimento, "teste2_combina_concord_MetaLearning_combinacao", false, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
}
