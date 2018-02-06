package matematica;

import java.util.ArrayList;
import java.util.List;

public class FuncMath {
	public static long fat(long numero) {
		long resposta = 1;
		long cont;
		for (cont = numero; cont > 1; cont--) {
			resposta = resposta * (cont);
			System.out.println("Resposta: " + resposta + "  COUNT: " + cont);
		}
		System.out.println("Fatorial de " + numero + " é " + resposta);

		return resposta;
 
	}

	public static double cortaCasasDecimais(double val, int numCasas) {
	
		int numPow = (int) Math.pow(10.0, numCasas);
		return Math.round(val * numPow) / (double) numPow;
	}

	/**
	 * Retorna maximo de numeros ordenados
	 */
	public static int maxIncreasingSubsequenceLength(int ... seq){
	    int[]L=new int[seq.length];
	    if(L.length == 0)
	    {
	    	return 0;
	    }
	    L[0]=1;
	    for(int i=1;i<L.length;i++){
	      int maxn=0;
	      for(int j=0;j<i;j++){
	        if(seq[j]<seq[i]&&L[j]>maxn){
	          maxn=L[j];
	        }
	      }
	      L[i]=maxn+1;
	    }
	    int maxi=0;
	    for(int i=0;i<L.length;i++){
	      if(L[i]>maxi){
	        maxi=L[i];
	      }
	    }
	    return(maxi);
	  }
	
	public static double log2(double num)
	{
		return (Math.log(num)/Math.log(2));
	} 
	public static void main(String[] arg) {
		System.out.println(cortaCasasDecimais(123.21742, 2));
		System.exit(0);
		// System.out.println(fat(997));
		System.out.println(maxIncreasingSubsequenceLength(6, 2, 3, 4, 1));
		System.out.println(maxIncreasingSubsequenceLength(2, 6, 3, 4, 1));

		List<Integer> lstOrder = new ArrayList<Integer>();
		int num = 0;
		while (num <= 10) {
			if (Math.random() > 0.5) {
				lstOrder.add(num);
				num++;
			} else {
				lstOrder.add((int) (Math.random() * 1000));
			}
		}
		System.out.println("Tamanho: " + lstOrder.size());
		System.out.println("Lista:" + lstOrder);
		int[] arrNums = new int[lstOrder.size()];
		int idx = 0;
		for (int i : lstOrder) {
			arrNums[idx] = i;
			idx++;
		}

		System.out.println("Ordenados:" + maxIncreasingSubsequenceLength(arrNums));
	}
}
