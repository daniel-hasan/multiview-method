package entidadesAprendizado;

import java.util.ArrayList;
import java.util.Collections;

public class ResultadoItemVoto extends ResultadoItem {
	private ArrayList<ResultadoItem> resultsConcordantes = new ArrayList<ResultadoItem>();
	
	public ResultadoItemVoto(long id, float classeReal, float classePrevista,
			float[] probPorClasse,ArrayList<ResultadoItem> resultsConcordantes) {
		super(id, classeReal, classePrevista, probPorClasse);
		// TODO Auto-generated constructor stub
		this.resultsConcordantes = resultsConcordantes;
	}

	public ResultadoItemVoto(long id, float classeReal, float classePrevista,
			float confianca,ArrayList<ResultadoItem> resultsConcordantes) {
		super(id, classeReal, classePrevista, confianca);
		// TODO Auto-generated constructor stub
		this.resultsConcordantes = resultsConcordantes;
	}
	public int getNumConcordantes()
	{
		return resultsConcordantes.size();
	}
	public double getMedianaResultado()
	{
		Collections.sort(resultsConcordantes);
		return resultsConcordantes.get((int)Math.ceil(resultsConcordantes.size()/2.0)).getClassePrevista();
	}
	public double getMediaResultado()
	{
		double sumResults = 0;
		double bestErro = Double.MAX_VALUE;
		double result = -1;
		for(int i = 0 ; i<resultsConcordantes.size(); i++)
		{
			sumResults += resultsConcordantes.get(i).getClassePrevista();
			if(resultsConcordantes.get(i).getErro() < bestErro )
			{
				bestErro = resultsConcordantes.get(i).getErro();
				result = resultsConcordantes.get(i).getClassePrevista();
			}
		}
		return sumResults/(double)resultsConcordantes.size();
		//return result;
	}
	
}
