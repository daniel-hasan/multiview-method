package entidadesAprendizado;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import aprendizadoResultado.CalculaResultados;

public class Resultado implements Serializable 
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String nomExperimento;
	private Fold[] folds;
	private MatrizConfusao matrizConfusao = null;
	
	/**
	 * which view this result pertence
	 */
	private View[] view;
	public Resultado(String nomExperimento,Fold[] folds)
	{
		this.nomExperimento = nomExperimento;
		this.folds = folds;
		for(int i =0; i<folds.length ; i++)
		{
			folds[i].setResultado(this);
		}
	}
	public Resultado(String nomExperimento,Fold[] folds,MatrizConfusao mConfu)
	{
		this(nomExperimento,folds);
		this.matrizConfusao = mConfu;
	}
	public Fold[] getFolds()
	{
		return this.folds;
		
	}
	/**
	 *  Set which view this result pertence
	 */
	public void setView(View[] v)
	{
		this.view = v;
	}
	public View[] getViews()
	{
		return this.view;
	}
	public String getNomExperimento()
	{
		return this.nomExperimento;
	}
	public MatrizConfusao getMatrizConfusao()
	{
		return this.matrizConfusao;
	}
	public double getMSE() throws SQLException
	{
		return CalculaResultados.getMSE(folds);
	}

	
	public static void imprimeResultadoClassificacao(ArrayList<Resultado> results,int numClasse) throws IOException
	{
		Iterator<Resultado> i = results.iterator();
		
		while(i.hasNext())
		{
			Resultado r = i.next();
			System.out.println(CalculaResultados.resultadoClassificacaoToString(r,numClasse,new File("resultGeral")));
		}
	}
	public static void imprimeResultadoRegressao(ArrayList<Resultado> results) throws Exception
	{
		Iterator<Resultado> i = results.iterator();
		
		while(i.hasNext())
		{
			Resultado r = i.next();
			System.out.println(CalculaResultados.resultadoRegressaoToString(r,new File("resultados/teste.result")));
		}
	}
	public Resultado clonaResultadoFiltrado(ArrayList<ResultadoItem> ids,String sufixoNomeBase) throws Exception
	{
		Fold[] foldResultadoNovo = new Fold[folds.length];
		for(int i = 0; i< folds.length ; i++)
		{
			foldResultadoNovo[i] = folds[i].clonaFoldFiltrandoResultados(ids, sufixoNomeBase);
			
		}
		
		return new Resultado(this.nomExperimento,foldResultadoNovo);
		
	}
	
}
