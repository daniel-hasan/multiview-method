package entidadesAprendizado;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Classe que implementa uma matriz de confusão. As linhas representam as
 * predições feitas e as colunas as classes reais.
 */
public class MatrizConfusao implements Serializable
{
    private int[][] matriz;
    private int numAcertos = 0;
    private int numPredicoes = 0;
    private Map<String,Integer> classPerIndex = new HashMap<String,Integer>();
    
    
    
    /**
     * Construtora. Inicializa a matriz.
     * 
     * @param tamanho
     *            Número de classes no experimento.
     */
    public MatrizConfusao(int tamanho)
    {
        matriz = new int[tamanho][];
        for(int i = 0; i < matriz.length ; i++)
        {
        	matriz[i] = new int[tamanho];
        	for(int j =0 ; j < matriz[i].length ; j++)
        	{
        		matriz[i][j] = 0;
        	}
        }
    }
    
    public int getIdxOfClass(int classValue,boolean addIfNotExists){
    	String strClass = Integer.toString(classValue);
    	if(classPerIndex.containsKey(strClass))
    	{
    		return classPerIndex.get(strClass);
    	}
    	
    	if(addIfNotExists)
    	{
	    	int maxIdx = -1;
	    	int intClassIdx = 0;
	    	for(String classVal : classPerIndex.keySet())
	    	{
	    		intClassIdx = classPerIndex.get(classVal);
	    		if(intClassIdx > maxIdx)
	    		{
	    			maxIdx = intClassIdx;
	    		}
	    	}
	    	int newIdx = maxIdx+1;
	    	classPerIndex.put(strClass, newIdx);
	    	//System.out.println("IDX PER CLASSE: "+classPerIndex);
	    	return newIdx;
    	}
    	return -1;
    }
    public int getIdxOfClass(int classValue){
    	
    	return getIdxOfClass(classValue,true);
    }
    public int getTamanho()
    {
    	return this.matriz.length;
    }
    /**
     * Função chamada a cada predição realizada.
     * 
     * @param classePrevista
     *            Inteiro que representa a classe prevista.
     * @param classeReal
     *            Inteiro que representa a classe real da instancia.
     */
    public void novaPredicao(int classePrevista, int classeReal)
    {
    	
    	//System.out.println(classePrevista+" => "+classeReal);
    	
    	int idxClassePrevista = getIdxOfClass(classePrevista);
    	int idxClasseReal = getIdxOfClass(classeReal);
    	
    	this.matriz[idxClasseReal][idxClassePrevista]++;
    	
    	if(idxClassePrevista == idxClasseReal)
    	{
    		this.numAcertos++;
    	}
    	this.numPredicoes++;

    }
    
    public Integer getMatrixValue(String classeReal,String classePrevista)
    {
    	int idxClassReal = getIdxOfClass(Integer.parseInt(classeReal),false);
    	int idxClassePredita = getIdxOfClass(Integer.parseInt(classePrevista),false);
    	if(idxClassePredita < 0 || idxClassReal < 0)
    	{
    		return 0;
    	}
    	return getMatrixValue(idxClassReal, idxClassePredita);
    }
    
    /**
     * Get the value in a given peosition
     * @param classeReal
     * @param classePrevista
     */
    public Integer getMatrixValue(int classeReal,int classePrevista)
    {
    	/*
    	Map<Integer, Integer> classeMap = matriz.get(classePrevista);
    	
    	if(classeMap == null)
    	{
    		return 0;
    	}
    	return classeMap.get(classeReal) == null?0:classeMap.get(classeReal);
    	*/
    	if(classeReal > this.matriz.length || classePrevista > this.matriz.length)
    	{
    		return null;
    	}
    	return this.matriz[classeReal][classePrevista];
    	
    }
    
    public int getPredictionsCount(String classValue)
    {
    	int idx = getIdxOfClass(Integer.parseInt(classValue),false);
    	return idx >= 0?getPredictionsCount(idx):0;
    }
    /**
     * Quantos foram preditos com a seguinte classe classValue?
     * @param classValue
     * @return
     */
    public int getPredictionsCount(int classValue)
    {
    	int predCount = 0;
    	//navega sobre todas as linhas (classe real)
    	for(int i = 0; i < this.matriz.length ; i++)
    	{
    		//conta todos que foram preditos com a classe classValue
    		predCount += getMatrixValue(i,classValue);
    	}
    	return predCount;
    }
    public Integer getInstancesInClass(String classValue)
    {
    	int idx = getIdxOfClass(Integer.parseInt(classValue),false);
    	return idx >= 0?getInstancesInClass(idx):0;
    }
    /**
     * Quantas instancias a classe classValue possui?
     * @param classValue
     * @return
     */
    public Integer getInstancesInClass(int classValue)
    {
    	int realClassCount = 0;
    	
    	
    	for(int i = 0; i < this.matriz[classValue].length ; i++)
    	{
    		realClassCount += getMatrixValue(classValue, i);
    	}
    	
    	return realClassCount;
    }
    
    
    public Set<Integer> getAllRealClasses()
    {
    	Set<Integer> realClasses = new HashSet<Integer>();
    	for(int i =0 ; i < this.matriz.length ; i++)
    	{
    		realClasses.add(i);
    	}
    	
    	return realClasses;
    }
    public Set<String> getAllRealClassesString()
    {
    
    	return classPerIndex.keySet();
    }
    
    /**
     * Get the global precision
     * @return
     */
    public double getPrecision()
    {
    	Set<Integer> lstAllClasses = getAllRealClasses();
    	
    	int total = 0;
    	for(Integer existingClass : lstAllClasses)
    	{
    		total += getMatrixValue(existingClass, existingClass);
    	}
    	
    	return total/getNumPredicoes();
    }
    
    /**
     * Get the precision for a class
     * @param classValue
     * @return
     */
    public double getPrecision(int classValue)
    {
    	int precCount = getPredictionsCount(classValue);
    	if(precCount == 0)
    	{
    		return 0;
    	}
    	return getMatrixValue(classValue, classValue)/(double)precCount;
    }
    
    
    /**
     * Get the recall for a class
     * @param classValue
     * @return
     */
    public double getRecall(int classValue)
    {
    	int instInClass = getInstancesInClass(classValue);
    	if(instInClass == 0)
    	{
    		return 0;
    	}
    	return getMatrixValue(classValue, classValue)/(double)instInClass;
    }
    public double getRecall(String classVal)
    {
    	if(!this.classPerIndex.containsKey(classVal))
    	{
    		return 0;
    	}
    	return getRecall(this.classPerIndex.get(classVal));
    }
    public double getPrecision(String classVal)
    {
    	if(!this.classPerIndex.containsKey(classVal))
    	{
    		return 0;
    	}
    	return getPrecision(this.classPerIndex.get(classVal));
    }
    
    
    /**
     * Get the macro F1 to a class
     */
    public double getMacroF1(int classValue)
    {
    	double p = getPrecision(classValue);
    	double r = getRecall(classValue);
    	
    	return getF1(p,r);
    }
    public double getMacroF1(String classValue)
    {
    	double p = getPrecision(classValue);
    	double r = getRecall(classValue);
    	
    	return getF1(p,r);
    }
    public double getF1(double precision, double recall)
    {
    	if(precision+recall == 0)
    	{
    		return 0;
    	}
    	return (2*precision*recall)/(precision+recall);
    }
    
    /**
     * Get Global MacroF1
     */
    public double getMacroF1()
    {
    	
    	
    	double totalF1 = 0;
    	
    	
    	
    	
    	HashMap<Integer, Double> macroPerClass = getMacroPerClass();
    	for(double macro : macroPerClass.values())
    	{
    		totalF1 += macro;
    	}
    	return totalF1/macroPerClass.keySet().size();
    }

	public HashMap<Integer, Double> getMacroPerClass() {
		Set<Integer> lstAllClasses = getAllRealClasses();
		HashMap<Integer,Double> macroPerClass = new HashMap<Integer,Double>();
    	for(Integer existingClass : lstAllClasses)
    	{
    		double macro = getMacroF1(existingClass);
    		macroPerClass.put(existingClass, macro);
    		
    	}
		return macroPerClass;
	}
    public double getMicroF1()
    {
    	return getF1(getPrecision(),getPrecision()); 
    }
    public String getClassName(int idxClass)
    {
    	for(String strName : this.classPerIndex.keySet())
    	{
    		if(this.classPerIndex.get(strName) == idxClass)
    		{
    			return strName;
    		}
    	}
    	return null;
    }
    
    /**
     * Passa a matriz para uma string
     */
    public String toString()
    {
    	
    	StringBuilder strBuilder = new StringBuilder();
    	strBuilder.append("Target Class (lines) Predicted Class (columns):\n");
    	for(int i =0 ; i<matriz.length ; i++)
        {
    		strBuilder.append("\t"+getClassName(i));
        }
    	strBuilder.append("\n");
        for(int i =0 ; i<matriz.length ; i++)
        {
        	strBuilder.append(getClassName(i)+"\t");
            for(int j = 0 ; j<matriz[i].length ; j++)
            {
            	strBuilder.append(matriz[i][j]+"\t");

            }
            strBuilder.append("\n");
        }
        strBuilder.append("\n");
        strBuilder.append("Hits: "+this.numAcertos+"/"+this.numPredicoes+" ("+((this.numAcertos/(double)this.numPredicoes)*100.0)+"%)\n");
        strBuilder.append("Macro F1: "+getMacroF1()+"\nMacro F1 per class:"+getMacroPerClass());
        return strBuilder.toString();
    }
    public double getNumAcertos()
    {
    	return this.numAcertos;
    }
    public double getNumPredicoes()
    {
    	return this.numPredicoes;
    }
    public double getAcuracia()
    {
    	return (this.numAcertos/(double)this.numPredicoes)*100.0;
    }
    /**
     * Imprime a matriz de confusão.
     */
    public void imprime()
    {
    	System.out.println(this);
    }
    
    public static MatrizConfusao getMatrizConfusao(List<ResultadoItem> lstResultados)
    {
    	Set<Integer> numClasses = new HashSet<Integer>();
    	for(ResultadoItem r : lstResultados)
    	{
    		numClasses.add((int)r.getClasseReal());
    		
    	}
    	MatrizConfusao mt = new MatrizConfusao(numClasses.size());
    	for(ResultadoItem r : lstResultados)
    	{
    		mt.novaPredicao((int)r.getClassePrevista(), (int)r.getClasseReal());
    	}
    	return mt;
    }
    public static void main(String[] args)
    {
        MatrizConfusao mc = new MatrizConfusao(2);
        int idxZero = -1;
        int idxUm = 1;
        mc.novaPredicao(idxZero, idxZero);
        mc.novaPredicao(idxZero, idxZero);
        mc.novaPredicao(idxZero, idxZero);
        mc.novaPredicao(idxZero, idxZero);
        mc.novaPredicao(idxZero, idxZero);
        mc.novaPredicao(idxUm, idxUm);
        mc.novaPredicao(idxUm, idxUm);
        mc.novaPredicao(idxUm, idxUm);
        mc.novaPredicao(idxZero, idxUm);
        mc.novaPredicao(idxZero, idxUm);
        mc.novaPredicao(idxUm, idxZero);
        mc.imprime();
    }
}
