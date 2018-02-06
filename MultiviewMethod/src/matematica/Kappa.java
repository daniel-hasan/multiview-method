package matematica;


public class Kappa {
    private double p;
    private double pe;
    private double kappa;
    private String input;
    
    public Kappa(double p, double pe, double kappa) {
        super();
        this.p = p;
        this.pe = pe;
        this.kappa = kappa;
    }
    /**
     * Probabilidade de agreement
     * @return
     */
    public double getP() {
        return p;
    }
    public void setP(double p) {
        this.p = p;
    }
    
    /**
     * Probabilidade de uma dada categoria ser selecionada
     * @return
     */
    public double getPe() {
        return pe;
    }
    public void setPe(double pe) {
        this.pe = pe;
    }
    public double getKappa() {
        return kappa;
    }
    public void setKappa(double kappa) {
        this.kappa = kappa;
    }
    public String getInput() {
        return input;
    }
    public void setInput(String input) {
        this.input = input;
    }
    public String toString()
    {
        return "p: "+p+" pe:"+pe+" kappa:"+this.kappa+" INPUT: "+this.input;
    }
   
   
    //http://en.wikipedia.org/wiki/Fleiss%27_kappa
    public static Kappa calculaKappa(int[][] arrCasos)
    {
        //numero N= de linhas
        int numCasos = arrCasos.length;
        int categorias = arrCasos[0].length;
       
		
        double[] p_i = new double[numCasos];
        double[] p_j = new double[categorias];
        double probP = 0;
        double p_e = 0;
        double kappa = 0;
        //numero de raters
        int numRaters = 0;
        for(int i = 0; i<categorias ; i++)
        {
            numRaters+= arrCasos[0][i];
        }
        if(numRaters == 1)
        {
        	return new Kappa(1, 1, 1);
        }
        for(int j = 0 ; j<p_j.length ; j++)
        {
            p_j[j] = 0;
        }
       
        //calcula p_i e comeca a somar p_j
        double sumPis = 0;
        for(int i = 0; i<arrCasos.length ; i++)
        {
            int sumQuadJ = 0;
           
            for(int j = 0; j<arrCasos[i].length ; j++)
            {
                sumQuadJ += Math.pow(arrCasos[i][j], 2);
                p_j[j] += arrCasos[i][j];
            }
            p_i[i] = (sumQuadJ-numRaters)/(double)(numRaters*(numRaters-1));
           // System.out.println("SUMPI: "+p_i[i]);
            sumPis+=p_i[i];
           
        }
       
        //termina de calcular p_j dividindo pela raters*casos
       
        for(int j = 0 ; j<p_j.length ; j++)
        {
            p_j[j] /= numCasos*numRaters;
            p_e +=  Math.pow(p_j[j],2);
        }
       
        //calcula not p
        probP = sumPis/(double) numCasos;
        if(p_e == 1)
        {
        	p_e = 0.99999999999;
        }
        kappa = (probP-p_e)/(1-p_e);
        
       
        return new Kappa(probP,p_e,kappa);
       
    }

    public static void main(String[] args)
    {
        int[][] observations = {{4,2,0,0,0,0,0,0}};
       
        System.out.println(Kappa.calculaKappa(observations));
    }
}
