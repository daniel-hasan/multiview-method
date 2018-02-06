package config_tmp;

public class MinMax
{
	private double min;
	private double max;
	
	public MinMax()
	{
		this.min = Double.MIN_VALUE;
		this.max = Double.MAX_VALUE;
	}
	public MinMax(double min,double max)
	{
		this.min = min;
		this.max = max;
	}


	public double getMin()
	{
		return min;
	}


	public void setMin(double min)
	{
		this.min = min;
	}


	public double getMax()
	{
		return max;
	}


	public void setMax(double max)
	{
		this.max = max;
	}
}
