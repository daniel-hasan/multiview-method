package apredizadoCombinacao;

import entidadesAprendizado.Fold;

public class ResultadoViewCache {
	private Fold[] arrFoldTreino;
	private Fold[] arrFoldTeste;
	private Fold[] arrFoldValidacao;
	public ResultadoViewCache(Fold[] arrTreino,Fold[] arrValidacao, Fold[] arrTeste)
	{
		this.arrFoldTreino = arrTreino;
		this.arrFoldValidacao = arrValidacao;
		this.arrFoldTeste = arrTeste;	
	}
	
	public Fold[] getArrFoldTreino() {
		return arrFoldTreino;
	}
	public void setArrFoldTreino(Fold[] arrFoldTreino) {
		this.arrFoldTreino = arrFoldTreino;
	}
	public Fold[] getArrFoldTeste() {
		return arrFoldTeste;
	}
	public void setArrFoldTeste(Fold[] arrFoldTeste) {
		this.arrFoldTeste = arrFoldTeste;
	}
	public Fold[] getArrFoldValidacao() {
		return arrFoldValidacao;
	}
	public void setArrFoldValidacao(Fold[] arrFoldValidacao) {
		this.arrFoldValidacao = arrFoldValidacao;
	}
	
}
