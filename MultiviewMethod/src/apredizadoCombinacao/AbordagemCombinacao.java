package apredizadoCombinacao;

import entidadesAprendizado.Fold;
import entidadesAprendizado.MatrizConfusao;
import entidadesAprendizado.View;

public abstract class AbordagemCombinacao {
	public void calculoPreTreino(View[] views) throws Exception
	{
		
	}
	

	public abstract MatrizConfusao getMatrizCombinacao();
	public abstract Fold combinarResultadoFold(Fold[] resultPorViewTreino, Fold[] resultPorViewTeste) throws Exception;
	
	public  Fold combinarResultadoFold(Fold[] resultPorViewTreino, Fold[] resultPorViewTeste,Fold[] resultPorViewValidacao) throws Exception
	{
		return combinarResultadoFold( resultPorViewTreino,  resultPorViewTeste,null);
	}
}
