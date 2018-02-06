package stuctUtil;

import java.util.HashMap;
import java.util.Map;

public class ItemPerThread<T> {
	private Map<Long,T> itemPerThread = new HashMap<Long,T>();
	public ItemPerThread()
	{
		
	}

	public synchronized void set(T val)
	{
		Long idCurrentThread = Thread.currentThread().getId();
		itemPerThread.put(idCurrentThread, val);
	}
	
	public T get()
	{
		return get(null);
	}
	public synchronized T get(T defaultVal)
	{
		Long idCurrentThread = Thread.currentThread().getId();
		if(itemPerThread.containsKey(idCurrentThread))
		{
			return itemPerThread.get(idCurrentThread);
		}else
		{
			return defaultVal;
		}
	}
	
	
	
}
