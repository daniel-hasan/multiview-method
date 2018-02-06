package featSelector;

import java.util.List;

import stuctUtil.Tripla;

public interface ViewCreatorHelper 
{
	public String getFeatureVal(Integer foldId, Integer subFoldId,Long instanceId,Integer featGlobalIdx) throws Exception;
	
	
	
	
	public List<Tripla<Float,Integer,Integer>> getClassIdAndQidTrain(Integer foldId,Integer subFoldId);
	public List<Tripla<Float,Integer,Integer>> getClassIdAndQidTest(Integer foldId,Integer subFoldId);
	public List<Tripla<Float,Integer,Integer>> getClassIdAndQidValidation(Integer foldId,Integer subFoldId);




	String getFeatureVal(Integer foldId, Integer subFoldId, Long instanceId,
			int viewIdx, short featLocalIdx) throws Exception;
}
