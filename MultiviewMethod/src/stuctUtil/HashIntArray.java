package stuctUtil;

import java.util.HashMap;
import java.util.Map;

public class HashIntArray<K> 
{
	private Map<K,Integer> mapInts = new HashMap<K,Integer>();
	
	
	public int getValue(K k)
	{
		if(!mapInts.containsKey(k))
		{
			mapInts.put(k, 0);
		}
		return mapInts.get(k);
	}
	public void setValue(K k,Integer v)
	{
		mapInts.put(k, v);
	}
	public void increment(K key)
	{
		int v = getValue(key);
		setValue(key,++v);
	}
	
	public K getMaxKey()
	{
		int max = Integer.MIN_VALUE;
		K maxKey = null;
		for(K k : mapInts.keySet())
		{
			int val = getValue(k);
			if(max < val)
			{
				 max = val;
				 maxKey = k;
			}
		}
		return maxKey;
	}
	public K getMinKey()
	{
		int min = Integer.MAX_VALUE;
		K minKey = null;
		for(K k : mapInts.keySet())
		{
			int val = getValue(k);
			if(min > val)
			{
				min = val;
				minKey = k;
			}
		}
		return minKey;
	}
	
	public String toString()
	{
		return mapInts.toString();
	}
}
