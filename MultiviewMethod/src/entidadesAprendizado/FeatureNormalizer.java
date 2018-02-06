package entidadesAprendizado;

import config_tmp.MinMax;

public class FeatureNormalizer
{
	private MinMax objMinMaxFeature;
	
	public FeatureNormalizer(MinMax objMinMax)
	{
		this.objMinMaxFeature = objMinMax;
	}
	public FeatureNormalizer(int min, int max)
	{
		this(new MinMax(min,max));
	}
	
	/**
	 * Norm value between 0 and 1
	 * @param value
	 * @return
	 */
	public double normValue(double value)
	{
		double valNorm = value-objMinMaxFeature.getMin();
		valNorm = (valNorm/(objMinMaxFeature.getMax()-objMinMaxFeature.getMin()));
		if(valNorm>1)
		{
			valNorm = 1;
		}
		if(valNorm<0)
		{
			valNorm = 0;
		}
		
		return valNorm;
		
	}
	public double normValue(double value,MinMax between)
	{
		double valueNorm = this.normValue(value);
		//define largura
		valueNorm *= between.getMax()-between.getMin();
		//define deslocamento
		valueNorm += between.getMin();
		return valueNorm;
	}
	public static void main(String[] args)
	{
		FeatureNormalizer fNorm = new FeatureNormalizer(0,5);
		
		
		System.out.println(fNorm.normValue(0));
		System.out.println(fNorm.normValue(1));
		System.out.println(fNorm.normValue(2));
		System.out.println(fNorm.normValue(2.5));
		System.out.println(fNorm.normValue(3)); 
		System.out.println(fNorm.normValue(4));
		System.out.println(fNorm.normValue(5));
		System.out.println(fNorm.normValue(5.5));
		
		MinMax objMinMax = new MinMax(0,1);
		System.out.println(fNorm.normValue(0,objMinMax));
		System.out.println(fNorm.normValue(1,objMinMax));
		System.out.println(fNorm.normValue(2,objMinMax));
		System.out.println(fNorm.normValue(2.5,objMinMax));
		System.out.println(fNorm.normValue(3,objMinMax)); 
		System.out.println(fNorm.normValue(4,objMinMax));
		System.out.println(fNorm.normValue(5,objMinMax));
		System.out.println(fNorm.normValue(5.5,objMinMax));
	}
}
