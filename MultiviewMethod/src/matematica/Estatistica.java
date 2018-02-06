package matematica;

import io.Sys;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import arquivo.ArquivoUtil;

/*
 * Created on 25/03/2008
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */

/**
 * @author Daniel Hasan Dalip
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Estatistica
{
	private static Random rdn = new Random(System.currentTimeMillis());
	
	/**
	 * Retorna uma amostra aleatoria de tamanho "tamanhoAmostra" dada uma colecao
	 * Nao pode-se ter valor na colecao que seja igual � Integer.MIN_VALUE se existir ele nunca ser� escolhido
	 * Atencao! O vetor de colecao ser� alterado
	 * @return
	 */
	public static int[] criaAmostra(int[] colecao,int tamanhoAmostra)
	{
		int[] amostra = new int[tamanhoAmostra];
		int posicaoEscolhida = -1;
		
		
		for(int i = 0; i<tamanhoAmostra ; i++)
		{
			do{
				//escolhe uma posicao do vetor
				posicaoEscolhida = (int)Math.floor(rdn.nextFloat()*colecao.length);	
			}while(colecao[posicaoEscolhida]==Integer.MIN_VALUE);
			//Insere no vetor o valor da posicao nova
			amostra[i] = colecao[posicaoEscolhida];
			//marca a posicao como posicao j� escolhida
			colecao[posicaoEscolhida] = Integer.MIN_VALUE;
		}
		return amostra;
		
	}
	public static Set<Integer> getPosNovaAmostra(int tamColecao, int tamAmostra,long seed)
	{
		Random rdnAmostra = new Random(seed);
		Set<Integer> setPosEscolhida = new HashSet<Integer>(tamAmostra);
		
		
		int posicaoEscolhida = -1;
		
		
		for(int i = 0; i<tamAmostra ; i++)
		{
			do{
				//escolhe uma posicao do vetor
				posicaoEscolhida = (int)Math.floor(rdnAmostra.nextFloat()*tamColecao);	
			}while(setPosEscolhida.contains(posicaoEscolhida));
			//Insere no vetor o valor da posicao nova
			setPosEscolhida.add(posicaoEscolhida);
		}
		return setPosEscolhida;
	}
	/**
	 * 
	 */
	public Estatistica()
	{
		super();
		// TODO Auto-generated constructor stub
	}
	public static double poison(double taxaOcorrencia,int numOcorrencias)
	{
		//System.out.println("TAXA: "+taxaOcorrencia+" NUM OCORRENCIA: "+numOcorrencias+" -- "+Math.pow(taxaOcorrencia,numOcorrencias)*Math.pow(Math.E,-numOcorrencias));
		return (Math.pow(taxaOcorrencia,numOcorrencias)*Math.pow(Math.E,-taxaOcorrencia))/(double) FuncMath.fat(numOcorrencias);
	}
	public static double binomial(int n,int ocorrencia,double p)
	{
		System.out.println(FuncMath.fat(997));
		return (FuncMath.fat(n)/(double) (FuncMath.fat(ocorrencia)*FuncMath.fat(n-ocorrencia)));//*Math.pow(p, ocorrencia)*Math.pow(1-p, ocorrencia);
	}
	public static double poisonIntervalo(double taxaOcorrencia,int inicioOcorrencia,int fimOcorrencia)
	{
		double resp = 0;
		for(int i = inicioOcorrencia; i<=fimOcorrencia ;i++)
		{
			resp += poison(taxaOcorrencia,i);
		}
		return resp;
	}
	public static double binomialIntervalo(int n,double p,int inicioOcorrencia,int fimOcorrencia)
	{
		double resp = 0;
		for(int i = inicioOcorrencia; i<=fimOcorrencia ;i++)
		{
			resp += binomial(n,i,p);
		}
		return resp;
	}
	public static double media(List<Double> lstValues)
	{
		double sum = 0;
		for(double val : lstValues)
		{
			double lastSum = sum;
			double lastVal = val;
			sum += val;
			
		}
		return sum/lstValues.size();
	}
	
	public static double getConfidenceErrorInterval(double[] arrVals,double pValue) throws IOException
	{
		
		double df = (arrVals.length-1)*desvioPadrao(arrVals)/Math.sqrt(arrVals.length);
		System.out.println("Desvio padrao:"+desvioPadrao(arrVals));
		System.out.println("df:"+df);
		//String df = "length(w1$vals)-1)*sd(w1$vals)/sqrt(length(w1$vals)
		String strScript = "qt("+(1-pValue)+",df="+(arrVals.length-1)+")*"+desvioPadrao(arrVals)/Math.sqrt(arrVals.length);
				/*"wilcox.test(v$"+colunaI+",v$"+colunaJ+", paired=T, alternative = \"greater\")$p.value"*/;
		
		//
		String resp = executeTestOnR(strScript);
		System.out.println(resp);
		//String[] arrResult = resp.split("\n");
		return getResultResp(resp);
	}
	public static double tTest(double[] arrValX,double[] arrValY) throws IOException
	{
		String varVector = addVectorOnR("x",arrValX)+"\n"+addVectorOnR("y",arrValY)+"\n";
		
		String strScript = varVector+
				"t.test(x,y, paired=T)$p.value";
				/*"wilcox.test(v$"+colunaI+",v$"+colunaJ+", paired=T, alternative = \"greater\")$p.value"*/;
		
		//
		String resp = executeTestOnR(strScript);
		//System.out.println(resp);
		return getResultResp(resp);
	}

	public static double getResultResp(String resp) {
		
		String[] arrResult = resp.split("\n");
		return Double.parseDouble(arrResult[arrResult.length-2].replaceAll("\\[1\\]","").trim());
	}
	public static double wilcoxonTest(double[] arrValX,double[] arrValY) throws IOException
	{
		String varVector = addVectorOnR("x",arrValX)+"\n"+addVectorOnR("y",arrValY);
		
		String strScript = varVector+
				/*"t.test(x,y, paired=T)$p.value";*/
				"wilcox.test(x,y, paired=T, alternative = \"greater\")$p.value";
		
		//System.out.println(resp);
		String resp = executeTestOnR(strScript);
		return getResultResp(resp);
	}
	public static String addVectorOnR(String strVarName,double[] arrVals)
	{
		String script = strVarName+" = c(";
		for(int i =0; i<arrVals.length ; i++)
		{
			script += arrVals[i];
			if(i+1<arrVals.length)
			{
				script += ",";
			}
		}
		
		return script+")";
	}
	public static String executeTestOnR(String strScript) throws IOException
	{
		File script = File.createTempFile("scriptR", ".R");
		//script.deleteOnExit();
		System.out.println("Executano SCRIPT...");
		ArquivoUtil.gravaTexto(strScript, script, false);
		Sys.executarComando("chmod 700 "+script.getAbsolutePath(),true);
		return  Sys.executarComando("R --file="+script.getAbsolutePath(), false);
	}
	
	
	public static double media(double[] valores)
	{
		double sum = 0;
		for(double val : valores)
		{
			sum += val;
		}
		
		return sum/valores.length;
	}
	public static double variancia(double[] valores)
	{
		double sumQuad = 0;
		double media = media(valores);
		for(double val : valores)
		{
			sumQuad += Math.pow(val-media,2);
		}
		return sumQuad/valores.length;
	}
	public static double desvioPadrao(double[] valores)
	{
		return Math.sqrt(variancia(valores));
	}
	public static void testaVariancia()
	{
		double[] valores = {1,1,1,1,10};
		
		System.out.println("Variancia = "+variancia(valores));
		
	}
	public static void testaDistribuicoes()
	{
		System.out.println("1 A - "+Estatistica.poison(3,3));
		System.out.println("1 A - "+Estatistica.poison(5,2));
		System.out.println("1 B - "+Estatistica.poisonIntervalo(6,1,20));
		System.out.println("1 C - "+Estatistica.poisonIntervalo(1.5,3,7));
		
		System.out.println("2 - "+Estatistica.binomial(1000,3,0.0001));
		
	}
	public static double calculaEntropia(double ... arrVetor)
	{
		double sumVals =0 ;
		for(double v : arrVetor)
		{
			sumVals += v;
		}
		
		
		double sumLog = 0;
		for(int i = 0 ; i < arrVetor.length ; i++)
		{
			//System.out.println("PROB da class: "+ probPorClasse[i]+" LOG:"+Math.log(probPorClasse[i]));
			if(arrVetor[i] != 0)
			{
				double proportion = (arrVetor[i]/sumVals);
				sumLog +=  proportion * Math.log(proportion);
			}
		}
		
		return -sumLog;
	}
	public static void main(String[] args) throws IOException
	{
		double[] arrVals = {0.43,0.40,0.45,0.82,0.52,1.32,0.90,1.18,0.48,0.21,0.27,0.31,0.65,0.18,0.52,0.30,0.58,0.48,0.58,0.58,0.41,0.48,1.76,1.21,1.18,0.83,1.22,0.77,1.02,0.13,0.68,0.61,0.70,0.82,0.76,0.77,1.69,1.48,0.74,1.24,1.12,0.75,0.39,0.87,0.41,0.56,0.55,0.67,1.26,0.97,0.84,0.97,1.07,1.22};
		
		System.out.println(getConfidenceErrorInterval(arrVals, 0.025));
		System.exit(0);
		System.out.println(Estatistica.calculaEntropia(1,1,50));
		
		//testaDistribuicoes();
		//testaVariancia();
		System.exit(0);
		//este teste calcula a porcentagem de ocorrencia de cada valor do vetor
		int numIteracoes =100000;
		
		int[] ocorrencias = new int[20];
		for(int i=0 ; i<numIteracoes ; i++)
		{
			int[] colecao = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19};
			int[] amostra = criaAmostra(colecao,3);
			for(int j=0; j<amostra.length ;j++)
			{
				ocorrencias[amostra[j]]++;
			}
		}
		for(int j =0 ; j<ocorrencias.length ; j++)
		{
			System.out.println(" Occorencia "+j+": "+((ocorrencias[j]/(double)numIteracoes)*100));
		}
		
		
	}
	
	
}
