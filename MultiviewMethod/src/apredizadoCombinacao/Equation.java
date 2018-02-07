package apredizadoCombinacao;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import config_tmp.ConfigViewColecao;
import entidadesAprendizado.Fold;
import entidadesAprendizado.MatrizConfusao;
import entidadesAprendizado.ResultadoItem;
import entidadesAprendizado.View;

/**
 * Method of combining views into one prediction. This method uses an equation
 * for choosing the instance class.
 */
public class Equation extends AbordagemCombinacao
{
    private MatrizConfusao confusionMatrix = new MatrizConfusao(6);
    
    /**
     * Combine views results.
     * 
     * @param resultPorViewTreino
     *            Fold used for training. The combinator can only use
     *            information from this view.
     * @param resultPorViewTeste
     *            Fold containing test instances. Used for validating the
     *            training phase.
     */
    @Override
    public Fold combinarResultadoFold(Fold[] resultPorViewTreino,
            Fold[] resultPorViewTeste) throws Exception
    {
        double alfa = 1.0;
        double beta = 1.0;
        double gama = 1.0;

        /* Best accuracy */
        double bestAcc = Double.MAX_VALUE;
        double bestAlfa = 0.0;
        double bestBeta = 0.0;
        double bestGama = 0.0;
        System.out.println("OI!");
        for (alfa = 0; alfa <= 10.0; alfa += 1)
        {
            for (beta = 0; beta <= 10.0; beta += 1)
            {
                for (gama = 0; gama <= 10.0; gama += 1)
                {
                    double res = testCombination(resultPorViewTreino, alfa,beta, gama);
                    //System.out.println("Resultado: "+res);
                    if (res < bestAcc)
                    {
                        bestAcc = res;
                        bestAlfa = alfa;
                        bestBeta = beta;
                        bestGama = gama;
                        
                        System.out.println("Melhor ate agora: MSE: "+bestAcc+"\t"+bestAlfa+"\t"+bestBeta+"\t"+bestGama);

                    }
                }
            }
        }
        System.out.println("Melhor: MSE: "+bestAcc+"\t"+bestAlfa+"\t"+bestBeta+"\t"+bestGama);

        
        /* Test results. */
        ArrayList<ResultadoItem> result = new ArrayList<ResultadoItem>();
        for (int i = 0; i < resultPorViewTeste[0].getNumResults(); i++)
        {
            /* Get prediction for each classifier. */
            List<ResultadoItem> predictions = new Vector<ResultadoItem>();
            long idResult = resultPorViewTeste[0].getResultadosValues().get(i).getId();
            for (int j = 0; j < resultPorViewTeste.length; j++)
            {
                predictions.add(resultPorViewTeste[j].getResultadoPorId(idResult));
            }

            /* Predict. */
            result.add(predict(predictions,bestAlfa, bestBeta, bestGama));
        }
        Fold fold = new Fold(resultPorViewTeste[0].getNum(),resultPorViewTeste[0].getNomeBase(), result);

        System.out.println("[Equation] Consufion Matrix:");
        confusionMatrix.imprime();
        
        //adiciona parametros usados
        fold.adicionaParam("alfa", Double.toString(bestAlfa));
        fold.adicionaParam("beta", Double.toString(beta));
        fold.adicionaParam("gama", Double.toString(gama));
        
        return fold;
    }

    /**
     * Predicts a class based on each view.
     * @param values
     * @param alfa
     * @param beta
     * @param gama
     * @return
     */
    private ResultadoItem predict(List<ResultadoItem> values, double alfa,
            double beta, double gama)
    {
        float pred = (float) ((alfa * values.get(0).getClassePrevista()
                + beta * values.get(1).getClassePrevista() + gama
                * values.get(2).getClassePrevista())
                / (alfa + beta + gama));
    
        confusionMatrix.novaPredicao((int) Math.round(pred), (int) Math.round(values.get(0).getClasseReal()));
        
        /*ResultadoItem result = new ResultadoItem(values.get(0).getId(),
                (float) values.get(0).getClasseReal(), pred, null);*/
        return null;
    }

    /**
     * Test given combination of parameters
     * @param resultPorViewTreino
     * @param alfa
     * @param beta
     * @param gama
     * @return
     */
    private double testCombination(Fold[] resultPorViewTreino, double alfa,
            double beta, double gama)
    {
    	if (alfa == 0 && beta == 0 && gama == 0)
    	{
    		return Double.MAX_VALUE;
    	}
        double totalMse = 0.0;
        double total = 0;
        
        for (int i = 0; i < resultPorViewTreino[0].getNumResults(); i++)
        {
        	long idResult = resultPorViewTreino[0].getResultadosValues().get(i).getId();
            double pred = (alfa
                    * resultPorViewTreino[0].getResultadoPorId(idResult)
                            .getClassePrevista()
                    + beta
                    * resultPorViewTreino[1].getResultadoPorId(idResult)
                            .getClassePrevista() + gama
                    * resultPorViewTreino[2].getResultadoPorId(idResult)
                            .getClassePrevista())
                    / (alfa + beta + gama);
            
            
            double mse = Math.pow(pred - resultPorViewTreino[0].getResultadoPorId(idResult).getClasseReal(),2);
            if(new Double(mse).isNaN())
            {
            	System.out.println("Prediction: " + pred);
            }
            totalMse += mse;
            total += 1;
        }
        //System.out.println("TotalMSE: "+totalMse+" Total: "+total+" div: "+totalMse / total);
        return totalMse / total;
    }
	public static void main(String[] args)
	{
		try {
			AbordagemCombinacao abComb = new Equation();
			String nomExperimento = "wikiMultiview";
			boolean gravarNoBanco = true;
			
			View[] vs= ConfigViewColecao.WIKIPEDIA.getViews(nomExperimento, gravarNoBanco);
			Combinador c = new Combinador("Combinacao-"+nomExperimento,abComb,vs);
			c.executaCombinacao(false,"teste");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public MatrizConfusao getMatrizCombinacao() {
		// TODO Auto-generated method stub
		return this.confusionMatrix;
	}
}
