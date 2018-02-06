package apredizadoCombinacao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CacheResultadoView {
	private static CacheResultadoView cache = null;
	private Map<String,ResultadoViewCache> mapResultadoViewPorViewIdx = new HashMap<String,ResultadoViewCache>();
	
	
	private CacheResultadoView()
	{
		
	}
	public String idxFeaturesToKey(List<Integer> lstIdxFeatures)
	{
		List<Integer> lstIdx = new ArrayList<Integer>();
		for(int idx : lstIdxFeatures)
		{
			lstIdx.add(idx);
		}
		Collections.sort(lstIdx);
		StringBuilder strIdxs = new StringBuilder();
		for(int idx : lstIdx)
		{
			strIdxs.append(idx+";");
		}
		return strIdxs.toString();
	}
	public void addResultado(String fileResultName,List<Integer> lstIdxFeatures,ResultadoViewCache rViewCache)
	{
		mapResultadoViewPorViewIdx.put(fileResultName+"_"+idxFeaturesToKey(lstIdxFeatures), rViewCache);
		
	}
	public ResultadoViewCache getResultado(String fileResultName,List<Integer> lstIdxFeatures)
	{
		return this.mapResultadoViewPorViewIdx.get(fileResultName+"_"+idxFeaturesToKey(lstIdxFeatures));
		
	}
	
	private static CacheResultadoView getCache()
	{
		if(CacheResultadoView.cache == null)
		{
			CacheResultadoView.cache = new CacheResultadoView();
		}
		return CacheResultadoView.cache;
		
	}
	public static ResultadoViewCache getResultadoViewCached(String nomArquivo,List<Integer> lstIdxFeature)
	{
		if(nomArquivo == null || nomArquivo.trim().length()==0  || lstIdxFeature.size()==0)
		{
			return null;
		}
		return CacheResultadoView.getCache().getResultado(nomArquivo,lstIdxFeature);
	}
	public static void addResultadoView(String nomArquivo,List<Integer> lstIdxFeatures,ResultadoViewCache rViewCache)
	{
		CacheResultadoView.getCache().addResultado(nomArquivo,lstIdxFeatures, rViewCache);
	}
}
