package stuctUtil;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * lista invertida de: 
 * [user_id] => lista de paginas que este usuario editou
 * @author hasan
 *
 */
public class ListaAssociativa<K,V> implements Serializable
{
	private static final long serialVersionUID = -7052519833556003525L;
	public enum ORDERBY_KEYS {
		TAM_LIST,KEY;
	}
	private Map<K, List<V>> listaInvertida = new HashMap<K,List<V>>();
	
	
	public ListaAssociativa()
	{
		
	}
	public ListaAssociativa(Map<K,List<V>> lst)
	{
		this.listaInvertida = lst;
	}
	public ListaAssociativa(HashMap<K,V> lst)
	{
		for(K key : lst.keySet())
		{
			this.put(key, lst.get(key));
		}
	}
	public void put(K key,List<V> lstValues)
	{
		for(V v : lstValues)
		{
			put(key,v);
		}
	}
	public void  reset(K key)
	{
		listaInvertida.put(key, new ArrayList<V>());
	}
	public void put(K key, V ... arrValue)
	{
		List<V> list = listaInvertida.get(key);
		
		for(V value : arrValue)
		{
			if(list == null)
			{
				listaInvertida.put(key, new ArrayList<V>());
				list = listaInvertida.get(key);
			}
			
			list.add(value);
		}
	}
	public V getFirst(K key)
	{
		return this.getList(key).get(0);
	}
	public V getFirst(K key,V valDefault)
	{
		if(this.getList(key,true).size()>0)
		{
			return this.getList(key).get(0);
			
		}else{
			return valDefault;
		}
	}
	public Set<V> getListAsSet(K key)
	{
		Set<V> setVals = new HashSet<V>();
	
		for(V v :getList(key,false))
		{
			setVals.add(v);
		}
		return setVals;
	}
	public List<V> getList(K key)
	{
		return getList(key,false);
	}

	public List<K> getKeysOrderedByListLength()
	{
		List<K> lstKeys = new ArrayList<K>();
		List<Tupla<K,List<V>>> lstTplList = new ArrayList<Tupla<K,List<V>>>();
		for(K key : listaInvertida.keySet())
		{
			lstTplList.add(new Tupla<K,List<V>>(key,listaInvertida.get(key)));
		}
		
		Collections.sort(lstTplList, new Comparator<Tupla<K,List<V>>>(){

			@Override
			public int compare(Tupla<K, List<V>> o1, Tupla<K, List<V>> o2)
			{
				return o1.getY().size()-o2.getY().size();
			}
			
		});
		
		for(Tupla<K,List<V>> tupla : lstTplList)
		{
			lstKeys.add(tupla.getX());
		}

		return lstKeys;
		
	}
	/**
	 * Combina todas as listas com todas as listas
	 * @return
	 */
	public List<Map<K,V>> combineAll()
	{
		if(this.listaInvertida.keySet().size() == 0)
		{
			return new ArrayList<Map<K,V>>();
		}
		List<Map<K,V>> lstValues = new ArrayList<Map<K,V>>();
		Map<K,Integer> idxPerKey = new HashMap<K,Integer>();
		
		//inicializa todos os ids das associacoes com 0
		List<K> lstKeys = new ArrayList<K>();
		for(K key : listaInvertida.keySet()){
			idxPerKey.put(key, 0);
			lstKeys.add(key);
		}
		//enqto o primeiro nao estiver terminado, continuar
		//K primKey = lstKeys.get(0);
		boolean incrementou = false;
		do
		{
			
			Map<K,V> mapCombinacao = new HashMap<K,V>();
			//vai adicionando em lstValue os valores de cada posicao da lista associativa indice idxPerKey
			for(K key : lstKeys)
			{
				//resgata o indice dessa chave
				int idxValueChave = idxPerKey.get(key);
				mapCombinacao.put(key, listaInvertida.get(key).get(idxValueChave));
			}
			lstValues.add(mapCombinacao);
			incrementou = false;
			//incrementa indice, do ultimo ao primeiro
			for(int idx = lstKeys.size()-1 ; idx>= 0 ; idx--)
			{
				K keyAIncrementarPos = lstKeys.get(idx);
				int idxKey = idxPerKey.get(keyAIncrementarPos);
				
				//verifica se este pode incrementar 
				if(idxKey+1 < listaInvertida.get(keyAIncrementarPos).size())
				{
					//caso possa, incrementa e voltar para o "0" todos os posteriores e sair do loop
					//incrementa
					idxPerKey.put(keyAIncrementarPos, idxKey+1);
					//votar pra 0 os posteriores
					for(int j = idx+1 ; j<lstKeys.size() ; j++)
					{
						idxPerKey.put(lstKeys.get(j), 0);
					}
					incrementou = true;
					break;
				}

			}
		}while(incrementou);
		
		return lstValues;
	}
	public Set<K> keySet()
	{
		return this.listaInvertida.keySet();
	}
	
	public String toString()
	{
		StringBuffer strLista = new StringBuffer();
		for(K key : listaInvertida.keySet())
		{
			strLista.append("["+key.toString()+"] => ");
			List<V> lstValues = listaInvertida.get(key);
			for(V value : lstValues)
			{
				strLista.append("; "+value.toString());
			}
			strLista.append("\n");
		}
		return strLista.toString();
	}
	public List<V> getValues()
	{
		List<V> lstVals = new ArrayList<V>();
		for(K key : this.listaInvertida.keySet())
		{
			lstVals.addAll(this.getList(key));
		}
		return lstVals;
	}
	public static void main(String[] args) 
	{
		ListaAssociativa<String, String> combinaTudo = new ListaAssociativa<String, String>();
		
		combinaTudo.put("x", "a"); 
		combinaTudo.put("x", "b");
		combinaTudo.put("x", "c");
		 
		combinaTudo.put("y", "i");
		combinaTudo.put("y", "j");
		combinaTudo.put("y", "k");
		
		combinaTudo.put("z", "I");
		combinaTudo.put("z", "II");
		combinaTudo.put("z", "III");
		
		List<Map<String,String>> combinacoes = combinaTudo.combineAll();
		System.out.println("Numero de combinacoes: "+combinacoes.size());
		for(Map<String,String> cobinacao : combinacoes)
		{
			System.out.println(cobinacao);	
		}
		
		
	}
	public boolean containsKey(K key) {
		// TODO Auto-generated method stub
		return this.keySet().contains(key);
	}
	public List<V> getList(K key, boolean emptyListWhenDonExist) {
		// TODO Auto-generated method stub
		if(emptyListWhenDonExist)
		{
			return listaInvertida.get(key) == null?new ArrayList<V>():listaInvertida.get(key);
		}else
		{
			return listaInvertida.get(key);
		}
		
		
	}
	public void removeKey(K key)
	{
		this.listaInvertida.remove(key);
	}
	public void removeValue(K key,V value)
	{
		this.listaInvertida.get(key).remove(value);
		
		
	}
}
