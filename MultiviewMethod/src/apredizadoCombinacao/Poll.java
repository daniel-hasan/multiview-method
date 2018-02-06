package apredizadoCombinacao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import stuctUtil.HashIntArray;
import stuctUtil.Tupla;
import entidadesAprendizado.Fold;
import entidadesAprendizado.MatrizConfusao;
import entidadesAprendizado.ResultadoItem;
import entidadesAprendizado.ResultadoItemVoto;

/**
 * Method of combining views into one prediction. This method uses a polling for
 * choosing the instance class.
 */
public class Poll extends AbordagemCombinacao
{
	
    /** Same votes [0] = right, [1] = wrong */
    int voteSame3[] = { 0, 0 };
    int voteSame2[] = { 0, 0 };
    /** Who voted right? */
    int voteRight[] = { 0, 0, 0 };
    /** Confusion matrix */
    MatrizConfusao confusionMatrix = new MatrizConfusao(6);
    Tipo tipo = Tipo.DISCRETO;
    
    public enum Tipo {
    	DISCRETO(),
    	REAL();
    }
    public Poll()
    {
    	
    }
    public Poll(Tipo tipoPoll)
    {
    	this.tipo = tipoPoll;
    }
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
    public Fold combinarResultadoFold(Fold[] resultPorViewTreino,   Fold[] resultPorViewTeste) throws Exception
    {

        ArrayList<ResultadoItem> result = getResultVotacao(resultPorViewTeste,resultPorViewTreino);
        
        /* Print stats. */
        System.out.println("Same class 3 votes: Correct: " + voteSame3[0] + " Wrong: " + voteSame3[1]);
        System.out.println("Same class 2 votes: Correct: " + voteSame2[0] + " Wrong: " + voteSame2[1]);
        System.out.println("One vote who is correct?");
        System.out.println("View[0] = " + voteRight[0]);
        System.out.println("View[1] = " + voteRight[1]);
        System.out.println("View[2] = " + voteRight[2]);
        System.out.println("Confusion matrix:");
        confusionMatrix.imprime();

        Fold fold = new Fold(resultPorViewTeste[0].getNum(),resultPorViewTeste[0].getNomeBase(), result);
        for(Tupla<String,String> param : this.getParams())
        {
        	fold.adicionaParam(param.getX(), param.getY());
        }
        return fold;
    }
    public List<Tupla<String,String>> getParams()
    {
    	return new ArrayList<Tupla<String,String>>();
    }
	public ArrayList<ResultadoItem> getResultVotacao(Fold[] resultPorView,Fold[] resultPorViewTreino) throws Exception {
		
		
        //resgata a acuracia de cada visão e o somatorio do mesmo
        double[] arrResultPerView = new double[resultPorView.length];
        double sumAcc = 0;
        for(int k = 0 ; k < resultPorViewTreino.length ; k++)
        {
        	arrResultPerView[k] =resultPorViewTreino[k].getAcuracia();
        	sumAcc += resultPorViewTreino[k].getAcuracia(); 
        }
        
		//resgata o numero de instancias
		int numInstances = resultPorView[0].getNumResults();
		
		//resultado final 
        ArrayList<ResultadoItem> result = new ArrayList<ResultadoItem>();
        
        //para cada instancia i
        for (int i = 0; i < numInstances; i++)
        {
            /* Get prediction for each classifier. */
            List<ResultadoItem> predictions = new Vector<ResultadoItem>();
            
            //resgata cada instancia pelo seu id
            long idResult = resultPorView[0].getResultadosValues().get(i).getId();
            for (int j = 0; j < resultPorView.length; j++)
            {
                predictions.add(resultPorView[j].getResultadoPorId(idResult));
            }
            

            
            result.add(getResultsPerTestViews(predictions,arrResultPerView,sumAcc));
            
        }
		return result;
	}
	/**
	 * 
	 * @param predictions Predictions per view for a specific instance
	 * @param resultPerView for each idx in prediction, the result of this view
	 * @param sumAcc sum of the accuracies for each view
	 * @return
	 * @throws Exception
	 */
	public  ResultadoItem getResultsPerTestViews(	List<ResultadoItem> predictions, double[] resultPerView,double sumAcc) throws Exception {
		/* Do the polling. */
		if(tipo == Tipo.DISCRETO)
		{
			return pool_discrete(predictions);
		}else
		{
			return pool_real(predictions);
		}
	}
    /**
     * Compute the agreement between views analising the predicted values distance
     * @param values
     * @return
     * @throws Exception 
     */
    public ResultadoItem pool_real(List<ResultadoItem> values) throws Exception
    {
    	//compute how many times each view agreed with others and get the most agreed one
    	ArrayList<ResultadoItem> mostAgreementSet = getMostAgreementSet(values);
        
        
        //Compute the average result between the views that most agreed with other
        double prediction = avgViews(mostAgreementSet);
        
        //debug
        /*
        System.out.println("Predictions: ");
        printResults(values);
        System.out.print(" => Num. agreement: "+mostAgreementSet.size()+" Prediction:"+prediction+" realClass: "+values.get(0).getClasseReal()+" Agrees: ");
        printResults(mostAgreementSet);
        */
        
        return new ResultadoItemVoto(values.get(0).getId(),
                (float) values.get(0).getClasseReal(),(float)prediction, 0L,mostAgreementSet);
    }
    
    /**
     * Compute the average result between views result
     * @param mostAgreementSet
     * @return
     * @throws Exception 
     */
    public double avgViews(ArrayList<ResultadoItem> viewsResult) throws Exception {
		double sumPredicted = 0;
        Iterator<ResultadoItem> i = viewsResult.iterator();
        long id = -1;
        while(i.hasNext())
        {
        	ResultadoItem r = i.next();
        	sumPredicted += r.getClassePrevista();
        	
			//verifica se o id esta ok
			if(id == -1)
			{
				id = r.getId();
			}else
			{
				if(id != r.getId())
				{
					throw new Exception("Erro: ID de views incompativeis");
				}
			}
        }
        double prediction = sumPredicted/(double)viewsResult.size();
       
		return prediction;
	}
	private void printResults(List<ResultadoItem> viewsResult) throws Exception {
		Iterator<ResultadoItem> j = viewsResult.iterator();
		long id = -1;
        while(j.hasNext())
        {
        	ResultadoItem r = j.next();
        	System.out.print(r.getClassePrevista()+"\t");
			//verifica se o id esta ok
			if(id == -1)
			{
				id = r.getId();
			}else
			{
				if(id != r.getId())
				{
					throw new Exception("Erro: ID de views incompativeis");
				}
			}
        }
	}
    
    /**
     * compute how many times each view agreed with others and return the most agreed one
     * @param values
     * @return
     */
	public ArrayList<ResultadoItem> getMostAgreementSet(
			List<ResultadoItem> values) {
		ArrayList<ResultadoItem> mostAgreementSet = new ArrayList<ResultadoItem>(values.size());
        for (int i = 0; i < values.size(); i++)
        {
        	ArrayList<ResultadoItem> agreementSet = new ArrayList<ResultadoItem>(values.size());
        	agreementSet.add(values.get(i));
        	for(int j = 0 ; j < values.size() ; j++)
        	{
        		if(i!=j)
        		{
	        		double jPredicted = values.get(j).getClassePrevista();
	        		
	        		//the distance is at most 0.5 between classes in agreement
	        		Iterator<ResultadoItem> k = agreementSet.iterator();
	        		boolean disagree = false;
	        		while(k.hasNext())
	        		{
	        			double predicted = k.next().getClassePrevista();
	        			if(Math.abs(predicted-jPredicted)>=0.5)
	        			{
	        				disagree = true;
	        			}
	        		}
	        		if(!disagree)
	        		{
	        			agreementSet.add(values.get(j));
	        		}
        		}
        	}
    		if(mostAgreementSet.size() < agreementSet.size())
    		{
    			mostAgreementSet = agreementSet;
    		}
        	
        }
		return mostAgreementSet;
	}
    /**
     * Method that do the actual combination between views and return a discrete value representing the most voted view
     * 
     * @values Each view result for given class
     * @return Polling result.
     * @throws Exception 
     */
    private ResultadoItem pool_discrete(List<ResultadoItem> values) throws Exception
    {
        HashIntArray<Integer> votesPerClass = new HashIntArray<Integer>();

        long id =-1;
        for (int i = 0; i < values.size(); i++)
        {
            //System.out.println(values.get(i).getClassePrevista() + "\t Real:"
            //        + values.get(i).getClasseReal());
            int vote = (int) Math.round(values.get(i).getClassePrevista());
            
            /*
            if (vote > 5)
            {
                votes[5]++;
                voteByView[i] = 5;
            } else if (vote < 0)
            {
                votes[0]++;
                voteByView[i] = 0;
            } else
            {
                votes[vote]++;
                voteByView[i] = vote;
            }
            */
        	votesPerClass.increment(vote);
			//verifica se o id esta ok
			if(id == -1)
			{
				id = values.get(i).getId();
			}else
			{
				if(id != values.get(i).getId())
				{
					throw new Exception("Erro: ID de views incompativeis");
				}
			}
        }

        /* Find the most voted class. */
        int mostVoted = votesPerClass.getMaxKey();
        int numVotes = votesPerClass.getValue(mostVoted);

       

        /* Statistics */
        
        //get the results of the views that most agreed
        
        ArrayList<ResultadoItem> resultsAgreed = new ArrayList<ResultadoItem>();
        for (int i = 0; i < values.size(); i++)
        {
        	int vote = (int) Math.round(values.get(i).getClassePrevista());
        	if(vote == mostVoted)
        	{
        		resultsAgreed.add(values.get(i));
        	}
        }
        /*
        
        if (numVotes == 3)
        {
            if (mostVoted == ((int)Math.round(values.get(0).getClasseReal())))
            {
                voteSame3[0]++;
            }
            else
            {
                voteSame3[1]++;
            }
        }
        else if (numVotes == 2)
        {
            if (mostVoted == ((int)Math.round(values.get(0).getClasseReal())))
            {
                voteSame2[0]++;
            }
            else
            {
                voteSame2[1]++;
            }
        }
        else
        {
            for (int i = 0; i < voteByView.length; i++)
            {
                if (voteByView[i] == ((int)Math.round(values.get(0).getClasseReal())))
                {
                    voteRight[i]++;
                }
            }
        }
        */
        
        confusionMatrix.novaPredicao(mostVoted, (int) Math.round(values.get(0).getClasseReal()));

        ResultadoItem result = new ResultadoItemVoto(values.get(0).getId(),
                (float) values.get(0).getClasseReal(), mostVoted, null,resultsAgreed);
        return result;
    }
    
	public static void main(String[] args)
	{
		try {
			AbordagemCombinacao abComb = new Poll();
			String nomExperimento = "wikiMultiview";
			boolean gravarNoBanco = true;
			/*
			View[] vs= ConfigViewColecao.WIKIPEDIA.getViews(nomExperimento, gravarNoBanco);
			Combinador c = new Combinador("Combinacao-"+nomExperimento,abComb,vs);
			c.executaCombinacao(false,"teste");
			*/
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
