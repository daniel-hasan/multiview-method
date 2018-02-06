package matematica;

public class MathUtil {
	public static int max(int ... vals)
	{
		int maxItem = Integer.MIN_VALUE;
		for(int val : vals)
		{
			if(val > maxItem)
			{
				maxItem = val;
			}
		}
		return maxItem;
	}
	public static int sum(int ... vals)
	{
		int sum = 0;
		for(int val : vals)
		{
			sum += val;
		}
		return sum;
	}
}
