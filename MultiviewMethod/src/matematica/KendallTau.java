package matematica;

import java.util.ArrayList;
import java.util.List;

import stuctUtil.Tupla;

public class KendallTau {

	
	
	public static double compareRanking(List<Tupla<Double, Double>> lstPairs)
	{
		int numConcordante = 0;
		int numDiscordante = 0;
		
		//compara para a par descobrindo todos os concordantes e discordantes
		for(int i = 0 ; i<lstPairs.size() ; i++)
		{
			for(int j = i+1 ; j<lstPairs.size() ; j++)
			{
				boolean bolAgree = (lstPairs.get(i).getX() < lstPairs.get(j).getX() && lstPairs.get(i).getY() < lstPairs.get(j).getY()) ||
								(lstPairs.get(i).getX() > lstPairs.get(j).getX() && lstPairs.get(i).getY() > lstPairs.get(j).getY());
				if(bolAgree)
				{
					numConcordante ++;
				}else
				{
					numDiscordante ++;
				}
			}
		}
		//System.out.println(" Concordantes: "+numConcordante);
		//System.out.println(" Discordantes: "+numDiscordante);
		return (numConcordante - numDiscordante)/ (double) (numConcordante + numDiscordante);
	}
	
	public static void main(String[] args)
	{
		List<Tupla<Double,Double>> lstTuplas = new ArrayList<Tupla<Double,Double>>();
		
		lstTuplas.add(new Tupla<Double, Double>(1.0,2.0));
		lstTuplas.add(new Tupla<Double, Double>(2.0 ,8.0));
		lstTuplas.add(new Tupla<Double, Double>(4.0, 7.0));
		lstTuplas.add(new Tupla<Double, Double>(5.0, 6.0));
		lstTuplas.add(new Tupla<Double, Double>(6.0, 5.0));
		lstTuplas.add(new Tupla<Double, Double>(7.0, 4.0));
		lstTuplas.add(new Tupla<Double, Double>(8.0, 3.0));
		lstTuplas.add(new Tupla<Double, Double>(9.0, 1.0));
		System.out.println(KendallTau.compareRanking(lstTuplas));
		
		double[] x1 = {1,2,4,5,6,7,8,9};
		double[] y1 = {2,8,7,6,5,4,3,1};
		System.out.println(it.unimi.dsi.law.stat.KendallTau.compute(x1, y1));
	}
}
