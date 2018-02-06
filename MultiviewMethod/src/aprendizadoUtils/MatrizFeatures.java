package aprendizadoUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import config_tmp.Colecao;

public class MatrizFeatures<FEAT_TYPE> implements Serializable {
	private Float[][] matrizFeatVal = null;
	private Map<Integer,Integer> mapInstanceToIdx = new HashMap<Integer,Integer>();
	private int numFeatures;
	private int numInstances;
	private int nextPosInstance = 0;
	private short nextPosFeature = 0;
	private Map<Short,Short> mapFeatToIdx = new HashMap<Short,Short>();
	/**
	 * Cria uma grande matriz com as intancias e features representanco cada instancia
	 * @param numInstances
	 * @param numFeatures
	 */
	public MatrizFeatures(int numInstances,int numFeatures)
	{
		//inicializa com o numero de instancias e features
		matrizFeatVal = new Float[numInstances][numFeatures];
		this.numFeatures = numFeatures;
		this.numInstances = numInstances;
	}
	/**
	 * Instancia uma instanceId na proxima posicao livre da matriz
	 * @param instanceId
	 * @return
	 */
	public int getInstanciaIdx(int instanceId)
	{
		if(!this.mapInstanceToIdx.containsKey(instanceId))
		{
			this.mapInstanceToIdx.put(instanceId, nextPosInstance);
			matrizFeatVal[nextPosInstance] = new Float[this.numFeatures];
			nextPosInstance++;
			
		}
		
		return mapInstanceToIdx.get(instanceId);
		
	}
	/**
	 * Adiciona hashmap com todas as features de instanceId na matriz na posicao correspndente a instance id
	 * @param instanceId
	 * @param mFeatures
	 */
	public void adicionaFeature(int instanceId,Map<FEAT_TYPE,String> mFeatures)
	{
		int idxInstPos = getInstanciaIdx(instanceId);
		
		for(FEAT_TYPE idxFeat : mFeatures.keySet())
		{
			short idxFeatReal = Short.parseShort(idxFeat.toString());
			if(!mapFeatToIdx.containsKey(idxFeatReal))
			{
				mapFeatToIdx.put(idxFeatReal, nextPosFeature);
				nextPosFeature++;
				
			}
			short idxFeatArray = mapFeatToIdx.get(idxFeatReal);
			matrizFeatVal[idxInstPos][idxFeatArray] = mFeatures.get(idxFeatReal)!=null?Float.valueOf(mFeatures.get(idxFeatReal)):null;

			//matrizFeatVal[idxInstPos][idxFeatArray] = mFeatures.get(idxFeatReal);

		}
	}
	
	public int getNumFeatures()
	{
		return this.numFeatures;
	}
	public int getNumInstances()
	{
		return this.numInstances;
	}
	public boolean hasInstance(int instanceId)
	{
		return mapInstanceToIdx.containsKey(instanceId);
	}
	public Float getFeature(int instanceId, int numFeature)
	{
		int instanceIdx = mapInstanceToIdx.get(instanceId);
		
		return this.matrizFeatVal[instanceIdx][mapFeatToIdx.get((short)numFeature)];
	}
	
	/**
	 * Retorna a lista de features idx na ordem (em que eles foram adicionados se addOrder = true 
	 * ou na ordem dos idx real - do dataset source que foi adicionado)
	 * @return
	 */
	public List<Short> getFeatureListOrderedyByFeatIdx(final boolean addOrder)
	{
		ArrayList<Short> lstIdxFeats = new ArrayList<Short>();
		//adiciona todas
		for(short idx : mapFeatToIdx.keySet())
		{
			lstIdxFeats.add(idx);
		}
		
		//ordena elas
		Collections.sort(lstIdxFeats, new Comparator<Short>() {
			//boolean addOrder = addOrder;
			

			@Override
			public int compare(Short idxFeat1, Short idxFeat2) {
				// ordena pelo indice das features
				short idx1 = addOrder?mapFeatToIdx.get(idxFeat1):idxFeat1;
				short idx2 = addOrder?mapFeatToIdx.get(idxFeat2):idxFeat2;
				return idx1-idx2;
			}
			
		});
		return lstIdxFeats;
	}
}
