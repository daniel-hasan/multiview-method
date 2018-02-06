package entidadesAprendizado;

import java.util.HashMap;
import java.util.Set;

public class MatrizConfusaoCalculos extends MatrizConfusao{

	public MatrizConfusaoCalculos(int tamanho) {
		super(tamanho);
		// TODO Auto-generated constructor stub
	}

	
	public HashMap<Integer, Double> getRecallPerClass() {
		Set<Integer> lstAllClasses = getAllRealClasses();
		HashMap<Integer,Double> macroPerClass = new HashMap<Integer,Double>();
    	for(Integer existingClass : lstAllClasses)
    	{
    		macroPerClass.put(existingClass, this.getRecall(existingClass));
    	}
		return macroPerClass;
	}
	public HashMap<Integer, Double> getPrecisionPerClass() {
		Set<Integer> lstAllClasses = getAllRealClasses();
		HashMap<Integer,Double> macroPerClass = new HashMap<Integer,Double>();
    	for(Integer existingClass : lstAllClasses)
    	{
    		macroPerClass.put(existingClass, this.getPrecision(existingClass));
    		
    	}
		return macroPerClass;
	}
	
	public String toString()
	{
		String str = super.toString();
		str += "\nPrecisao per class: "+getPrecisionPerClass();
		str += "\nRecall per class: "+getRecallPerClass();
		
		return str;
	}
}
