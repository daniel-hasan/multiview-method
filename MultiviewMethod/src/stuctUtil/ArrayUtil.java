package stuctUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArrayUtil<T> {
	
	
	public T[] union(T[] x, T[] y )
	{
		T[] arrResult = null;
		if(x instanceof String[])
		{
			arrResult = (T[]) new String[x.length+y.length];
		}
		
		int idxResult = 0;
		for(T xItem : x)
		{
			arrResult[idxResult] = xItem;
			idxResult++;
		}
		for(T yItem : x)
		{
			arrResult[idxResult] = yItem;
			idxResult++;
		}
		
		return arrResult;
	}
	public String toString(T[] array)
	{
		String strVals = "";
		for(T value : array)
		{
			strVals += value+";";
		}
		return strVals;
	}
	
	public List<T> toList(T[] array)
	{
		// TODO Auto-generated method stub
		ArrayList<T> lst = new ArrayList<T>();
		for(T v : array)
		{
			lst.add(v);
		}
		return lst;
	}
	
	
	public List<List<T>> getAllCombinations(List<T> lstItens,int intMinLengthComb,int maxLengthComb)
	{
		List<List<T>> lstCombinacoes = new ArrayList<List<T>>();
		
		//para cada posicao no vetor, colocar true ou false (presenca ou nao dessa posicao)
		ListaAssociativa<Integer,Boolean> lstAssociativa = new ListaAssociativa<Integer, Boolean>();
		for(int idx =0 ; idx<lstItens.size() ; idx++)
		{
			lstAssociativa.put(idx, true,false);
		}
		
		//faz todas as combinacoes de presenca e ausencia
		List<Map<Integer,Boolean>> lstCombinacoesPosicao = lstAssociativa.combineAll();
		//cria combionacao obedecendo lstCombinacoesPosicao
		for(Map<Integer,Boolean> mapPosCombinacao : lstCombinacoesPosicao)
		{
			List<T> newCombinacao = new ArrayList<T>();
			for(int pos : mapPosCombinacao.keySet())
			{
				//se ha precensa, colocar esta posicao pos nesta combinacao
				if(mapPosCombinacao.get(pos))
				{
					newCombinacao.add(lstItens.get(pos));
				}
			}
			//adiciona a combinacao caso tenha o tamanho pedido como parametro
			if(newCombinacao.size()>=intMinLengthComb &&  newCombinacao.size()<=maxLengthComb)
			{
				lstCombinacoes.add(newCombinacao);
			}
		}
		
		return lstCombinacoes;
		
	}
	
	public static void main(String[] args)
	{
		List<String> lstItens = new ArrayList<String>();
		
		lstItens.add("struct");
		lstItens.add("length");
		lstItens.add("Read");
		lstItens.add("relevance");
		lstItens.add("style");
		lstItens.add("user");
		lstItens.add("usergraph");
		lstItens.add("review");

		
		List<List<String>> lstCombs = new ArrayUtil<String>().getAllCombinations(lstItens,2,5);
		//imprime por tamanho
		for(int tam = 1 ; tam<lstCombs.size() ; tam++)
		{
			for(List<String> comb : lstCombs)
			{
				if(comb.size() == tam)
				{
					System.out.println(comb);
				}
			}
		}
		System.out.println("\n\nNumero de combinacoes: "+lstCombs.size());
		
		
	}

}
