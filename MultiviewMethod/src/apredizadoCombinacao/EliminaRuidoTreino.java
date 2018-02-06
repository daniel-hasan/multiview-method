package apredizadoCombinacao;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import aprendizadoUtils.MetodoAprendizado;
import aprendizadoUtils.SVM;


import config_tmp.Colecao;
import config_tmp.ConfigViewColecao;
import entidadesAprendizado.Fold;
import entidadesAprendizado.MatrizConfusao;
import entidadesAprendizado.ResultSetItem;
import entidadesAprendizado.ResultadoItem;
import entidadesAprendizado.ResultadoItemViews;
import entidadesAprendizado.ResultadoItemViews.ComparationType;
import entidadesAprendizado.ResultadoViews;

/**
 * Utiliza um método de combinação apenas para as views concordantes, Caso nao
 * concorda utiliza ou um metodo de aprendizado sob os nao concordantes ou um
 * metodo de combinção tb.
 * 
 * @author hasan
 * 
 */
public class EliminaRuidoTreino extends AbordagemCombinacao
{
	private double porcRemocaoRuido;
	private MetodoAprendizado metAprendizado;
	private ComparationType compType;
	private boolean porClasse;
	private boolean onlyHighClass = false;
	private boolean changeClass = false;
	private float agreementThreshould = 1F;
	private float distanceThreshould = 1F;
	public EliminaRuidoTreino(boolean porClasse,double porcRemocaoRuido,boolean changeClass,boolean onlyHighClass,
			ComparationType compType, MetodoAprendizado metAprendizado)
	{
		this(porClasse,porcRemocaoRuido,compType, metAprendizado);
		this.onlyHighClass = onlyHighClass;
		this.changeClass = changeClass;
	}
	public EliminaRuidoTreino(boolean porClasse,double porcRemocaoRuido,
			ComparationType compType, MetodoAprendizado metAprendizado)
	{
		this.metAprendizado = metAprendizado;
		this.porcRemocaoRuido = porcRemocaoRuido;
		this.compType = compType;
		this.porClasse = porClasse;
	}
	public void setThreshoulds(float agreement,float distance,boolean changeClass)
	{
		agreementThreshould = agreement;
		distanceThreshould = distance;
		this.changeClass = changeClass;
	}
	public void setOnlyHighClass(boolean only)
	{
		this.onlyHighClass = only;
	}
	
	@Override
	public Fold combinarResultadoFold(Fold[] resultPorViewTreino,
			Fold[] resultPorViewTeste) throws Exception
	{
		List<ResultadoItemViews> lstItensEliminados = new ArrayList<ResultadoItemViews>();
		
		// calcula a porcentagem de treino que será usada
		List<Long> lstIdsTreino = getItensTreino(resultPorViewTreino,lstItensEliminados);
		
		
		// cria fold
		Colecao col = this.metAprendizado.getColecao();
		this.metAprendizado.setArquivoOrigem(col.getArquivoOrigem());
		Fold resultParaMetodoAprendizado = null; 
		if(!this.changeClass)
		{
			System.out.println("NAO MUDA CLASSE! Itens Treino:"+lstIdsTreino.size());
			resultParaMetodoAprendizado = this.metAprendizado.criaFoldComIdsFiltrado(resultPorViewTreino[0].getNum(), lstIdsTreino,"foldsSemRuido", ResultadoItem
					.getIdsResultadoItem(resultPorViewTeste[0]
							.getResultadosValues()));
		}else
		{
			System.out.println("MUDA CLASSE!");
			List<Long> lstTeste = ResultadoItem.getIdsResultadoItem(resultPorViewTeste[0].getResultadosValues());
			List<Long> lstTreino = ResultadoItem.getIdsResultadoItem(resultPorViewTreino[0].getResultadosValues());
			
			resultParaMetodoAprendizado = this.metAprendizado.criaFoldComIdsFiltrado(resultPorViewTreino[0].getNum(), lstTreino,"foldsSemRuido",lstTeste,lstItensEliminados);
		}

		// test
		//this.metAprendizado.treinaParametros(resultParaMetodoAprendizado.getTreino());
		ArrayList<ResultadoItem> result = this.metAprendizado.testar(resultParaMetodoAprendizado);
		resultParaMetodoAprendizado.adicionaTodosResultados(result);

		return resultParaMetodoAprendizado;
	}
	public List<Long> getItensTreino(Fold[] resultPorViewTreino,List<ResultadoItemViews> lstItensEliminados) throws IOException
	{
		ResultSetItem rstItem = new ResultSetItem(resultPorViewTreino,porClasse);
		List<Long> lstIdsTreino = null;
		if(this.compType.equals(ComparationType.DISCORDANTES))
		{
			
			lstIdsTreino = rstItem.getProcessaDisagree(lstItensEliminados,agreementThreshould, distanceThreshould,onlyHighClass);
		}else
		{
			lstIdsTreino = rstItem.getTopLessErrorIdsResult(lstItensEliminados,this.compType, (float) this.porcRemocaoRuido);
		}
		return lstIdsTreino;
	}

	@Override
	public MatrizConfusao getMatrizCombinacao()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public static void main(String[] args) throws Exception
	{

		ConfigViewColecao cnf = ConfigViewColecao.WIKIPEDIA;

		String nomExperimentoView = "wikiMultiview_"+cnf.getColecao().getSigla();
		SVM svmSeletorView = null;/*new SVM(nomExperimentoView+"_selView",cnf.getCSeletorIdeal(),cnf.getGSeletorIdeal(),1.0F,SVM.MODE_CLASSIFICATION,true,true);
			svmSeletorView.setColecao(cnf.getColecao());*/
			
			
		ResultadoViews rViews = ResultadoViews.getResultadoViewsObject(cnf.getViews(nomExperimentoView, true),svmSeletorView);
		
		SVM metAprendizado = new SVM(nomExperimentoView+"_ruidoTeste",(float)cnf.getColecao().getCusto(),(float)cnf.getColecao().getGama(),1.0F,SVM.MODE_REGRESSION,false,false);
		metAprendizado.setArquivoOrigem(new File("/data/experimentos/multiview/testes_iniciais/wiki6.amostra"));
			
		Fold[] foldTreino = rViews.getResultadoPorViewTreino()[0];
		Fold[] foldTeste = rViews.getResultadoPorViewTeste()[0];
		List<Long> arrIdsTeste = new ArrayList<Long>();
		List<Long> arrIdsTreino = new ArrayList<Long>();
		
		arrIdsTeste.add(2995L);
		arrIdsTeste.add(5025L);
		arrIdsTeste.add(8099L);
		arrIdsTeste.add(71234L);

		arrIdsTreino.add(25L);
		arrIdsTreino.add(663L);
		arrIdsTreino.add(717L);
		arrIdsTreino.add(737L);
		arrIdsTreino.add(770L);
		arrIdsTreino.add(336L);
		arrIdsTreino.add(1680L);
		arrIdsTreino.add(59812L);
		arrIdsTreino.add(2103199L);
		
		for(int i=0; i<foldTreino.length ; i++)
		{
			List<ResultadoItem> resultTreino = foldTreino[i].getResultadosValues();
			List<ResultadoItem> resultTeste = foldTeste[i].getResultadosValues();
			foldTreino[i] = metAprendizado.criaFoldComIdsFiltrado(0, arrIdsTreino,"foldTreinoTeste", arrIdsTeste);
			foldTeste[i] = metAprendizado.criaFoldComIdsFiltrado(0, arrIdsTreino,"foldTesteTeste", arrIdsTeste);
			
			foldTreino[i].adicionaTodosResultados(resultTreino);
			foldTeste[i].adicionaTodosResultados(resultTeste);
		}
		
		EliminaRuidoTreino el = new EliminaRuidoTreino(false,0,true,true, ComparationType.DISCORDANTES, metAprendizado);
		List<ResultadoItemViews> lstItensEliminados = new ArrayList<ResultadoItemViews>();
		arrIdsTreino = el.getItensTreino(foldTreino,lstItensEliminados);
		
		metAprendizado.criaFoldComIdsFiltrado(0, arrIdsTreino,"foldsTesteSemRuido",arrIdsTeste,lstItensEliminados);
	}

}
