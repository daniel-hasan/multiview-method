package entidadesAprendizado;

public class Confianca {
	public enum Z{
		C_090(1.96),
		C_095(0),
		C_0975(0);
		
		private double z; 
		private Z(double z)
		{
			this.z = z;
		}
		
		public double getZ()
		{
			return z;
		}
		 
	}
	public enum T{
		T_090(90),
		T_095(95),
		T_0975(975);
		
		private double z; 
		private double[] t;
		private T(int confianca)
		{
			switch(confianca)
			{
				case 975:
					double[] tAux1 = {0,12.71, 4.3, 3.18, 2.78, 2.57, 2.45, 2.37, 2.31, 2.26, 2.23, 2.2, 2.18, 2.16, 2.15, 2.13, 2.12, 2.11, 2.1, 2.09, 2.09, 2.08, 2.07, 2.07, 2.06, 2.06, 2.06, 2.05, 2.05};
					this.t = tAux1;
					break;
				case 95:
					double[] tAux2 = {0,6.31, 2.92, 2.35, 2.13, 2.02, 1.94, 1.9, 1.86, 1.83, 1.81, 1.8, 1.78, 1.77, 1.76, 1.75, 1.75, 1.74, 1.73, 1.73, 1.73, 1.72, 1.72, 1.71, 1.71, 1.71, 1.71, 1.7, 1.7, 1.7, 1.7};
					this.t = tAux2;
					break;
				case 90:
					double[] tAux3 = {0,3.08, 1.89, 1.64, 1.53, 1.48, 1.44, 1.42, 1.4, 1.38, 1.37, 1.36, 1.36, 1.35, 1.35, 1.34, 1.34, 1.33, 1.33, 1.33, 1.33, 1.32, 1.32, 1.32, 1.32, 1.32, 1.32, 1.31, 1.31, 1.31, 1.31};
					this.t = tAux3;
					break;					
			}
		}
		
		public double getT(int n_menos1)
		{
			return this.t[n_menos1];
		}
		 
	}
	
	private double media;
	private double desvio;
	private int n;
	
	public Confianca(double media,double desvio,int n)
	{
		this.media = media;
		this.desvio = desvio;
		this.n = n;
	}
	
	public double[] calculaIntervalo(Z confianca)
	{
		double delta = getDelta(confianca);
		double[] conf= {media-delta , media+delta};
		
		return conf;
	}

	public double getDelta(Z confianca) {
		double delta = confianca.getZ() * (desvio/Math.sqrt(n));
		return delta;
	}

	public double[] calculaIntervalo(T confianca)
	{
		double delta = getDelta(confianca);
		double[] conf= {media-delta , media+delta};
		
		return conf;
	}

	public double getDelta(T confianca) {
		double delta = confianca.getT(n-1) * (desvio/Math.sqrt(n));
		return delta;
	}
	public double getMedia()
	{
		return this.media;
	}
	 
	
	
}
