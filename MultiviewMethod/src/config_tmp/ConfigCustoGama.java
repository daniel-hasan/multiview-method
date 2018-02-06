package config_tmp;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import arquivo.ArquivoUtil;

public class ConfigCustoGama implements Comparable<ConfigCustoGama>,Serializable
{ 
	/**
	 *  
	 */
	private static final long serialVersionUID = 1L;
	private static Double EPSLON = 0.1;
	private Double custo;
	private Double gama;
	private Double epslon;
	private File arquivo;
	private String nomExperimento;
	private Double resultado;

	public ConfigCustoGama(Double custo, Double gama, Double epslon,File arquivo,Double resultado)
	{
		super();
		this.custo = custo;
		this.gama = gama;
		this.epslon = epslon;
		nomExperimento ="";
		this.arquivo = arquivo;
		this.resultado = resultado;
	}
	public ConfigCustoGama(Double custo, Double gama, Double epslon)
	{
		this(custo,gama,epslon,null,null);
	}
	

	public Double getCusto()
	{
		return custo;
	}

	public void setCusto(Double custo)
	{
		this.custo = custo;
	}

	public Double getGama()
	{
		return gama;
	}

	public void setGama(Double gama)
	{
		this.gama = gama;
	}

	public Double getEpslon()
	{
		return epslon;
	}

	public void setEpslon(Double epslon)
	{
		this.epslon = epslon;
	}

	public File getArquivo()
	{
		return arquivo;
	}

	public void setArquivo(File arquivo)
	{
		this.arquivo = arquivo;
	}
	public String getArquivoValuesString()
	{
		return "custo="+this.custo+",gama="+this.gama+",epslon="+this.epslon; 
	}
	public void gravaArquivo(File arquivo) throws IOException
	{
		ArquivoUtil.gravaTexto(getArquivoValuesString(),arquivo,false);
	}
	public static ConfigCustoGama leArquivo(File arquivo) throws IOException
	{
		if(!arquivo.exists())
		{
			return null;
		}
		String[] params = ArquivoUtil.leTexto(arquivo).split(",");
		
		float custo = Float.parseFloat(params[0].replaceAll("[^.0-9]*", ""));
		float gama = Float.parseFloat(params[1].replaceAll("[^.0-9]*", ""));
		float epslon = Float.parseFloat(params[2].replaceAll("[^.0-9]*", ""));
		
		return new ConfigCustoGama((double) custo,(double) gama,(double) epslon);
	}
	public static ConfigCustoGama getCustoGama(String nomArquivo,File dirProcura) throws IOException
	{
		//System.out.println("Procurando: "+nomArquivo+" EM:"+dirProcura.getAbsolutePath());
		if(dirProcura.isDirectory())
		{
			File[] arqs = dirProcura.listFiles();
			ConfigCustoGama custo = null;
			for(int i =0 ; i<arqs.length ; i++)
			{
				if(arqs[i].isDirectory() && !arqs[i].getName().startsWith("."))
				{
					custo = getCustoGama(nomArquivo,arqs[i]);
					if(custo != null)
					{
						return custo;
					}
				}else
				{
					if(arqs[i].getName().toLowerCase().contains(nomArquivo.toLowerCase()) && arqs[i].getName().toLowerCase().endsWith(".out.out"))
					{
						return getMinCustoGama(arqs[i]);
					}
				}
			}
		}
		//nao achou
		//System.err.println("NAO ACHOU!");
		return null;
	}
	public static ArrayList<ConfigCustoGama> getParamsDiretorio(File testeDir, File paramDir)	throws IOException
	{
		ArrayList<ConfigCustoGama> custoGamaColecao = new ArrayList<ConfigCustoGama>();
		File[] arqs = testeDir.listFiles();
		System.out.println(testeDir.getAbsolutePath());
		for(int i = 0 ; i<arqs.length ; i++)
		{ 
			//resgata todos os arquivos de datasets e procura seu parametro, se existir criar a configuração  ;
			if(!arqs[i].getName().endsWith("~"))
			{
				ConfigCustoGama custoGama = ConfigCustoGama.getCustoGama(arqs[i].getName().replaceAll("\\..*", "")+".out.out", paramDir);
				if(custoGama != null)
				{
					custoGama.setArquivo(arqs[i]);
					custoGama.setNomExperimento(paramDir.getParentFile().getParentFile().getName()+"_"+paramDir.getParentFile().getName()+"_"+arqs[i].getName().replaceAll("\\.txt", ""));
					custoGamaColecao.add(custoGama);
				}
			}
		}
		return custoGamaColecao;
	}
	public static ArrayList<ConfigCustoGama> getParamsDiretorio(ArrayList<ConfigCustoGama> custoGamaColecao,File paramDir)	throws IOException
	{
		File[] arqs = paramDir.listFiles();
		System.out.println(paramDir.getAbsolutePath());
		for(int i = 0 ; i<arqs.length ; i++)
		{ 
			//resgata todos os arquivos de datasets e procura seu parametro, se existir criar a configuração  ;
			if( (!arqs[i].getName().endsWith("~")) &&(!arqs[i].getName().startsWith(".")))
			{
				if(arqs[i].isDirectory())
				{
					custoGamaColecao = getParamsDiretorio(custoGamaColecao,arqs[i]);
				}else
				{
					if(arqs[i].getName().endsWith(".out.out"))
					{
						custoGamaColecao.add(getMinCustoGama(arqs[i], true));
					}
				}

			}
		}
		return custoGamaColecao;
	}
	
	private static ConfigCustoGama getMinCustoGama(File arquivoOut) throws IOException
	{
		return getMinCustoGama(arquivoOut,false);
	}
	
	public static ConfigCustoGama getMinCustoGama(File arquivoIn, boolean computeEpslon) throws IOException
	{
		 return getCustoGama( arquivoIn, computeEpslon, true);
	}
	public static ConfigCustoGama getMaxCustoGama(File arquivoIn, boolean computeEpslon) throws IOException
	{
		 return getCustoGama( arquivoIn, computeEpslon, false);
	}
	public static ConfigCustoGama getCustoGama(File arquivoIn, boolean computeEpslon, boolean minimizar) throws IOException
	{
		if(!arquivoIn.exists())
		{
			return null;
		}
		
		String[] custoGamaArq = ArquivoUtil.leTexto(arquivoIn).split("\n");
		double resultFinal = minimizar?Double.MAX_VALUE:Double.MIN_VALUE;
		double custoFinal = Double.MAX_VALUE;
		double gamaFinal = Double.MAX_VALUE;
		double epslonFinal =  EPSLON;
		String linhaVencedora = "";
		//System.out.println("Arquivo: "+arquivoIn.getAbsolutePath());
		for(int i = 0; i<custoGamaArq.length ; i++)
		{
			String[] custoGamaLinha = custoGamaArq[i].split(" ");
			double custo = Math.pow(2,Double.parseDouble(custoGamaLinha[0]));
			double gama = Math.pow(2,Double.parseDouble(custoGamaLinha[1]));
			double result = Double.parseDouble(custoGamaLinha[custoGamaLinha.length - 1]);
			
			if( (result<resultFinal && minimizar) || (result>resultFinal && !minimizar) )
			{
				resultFinal = result;
				custoFinal = custo;
				gamaFinal = gama;
				linhaVencedora = custoGamaArq[i];
				if(minimizar && custoGamaLinha.length == 4)
				{
					epslonFinal = Math.pow(2,Double.parseDouble(custoGamaLinha[2]));
				}
			}
		}
		System.out.println("Linha vencedora: "+linhaVencedora);
		
		return new ConfigCustoGama(custoFinal,gamaFinal,epslonFinal,arquivoIn,resultFinal);
	}

	public String getNomExperimento()
	{
		return this.nomExperimento;
	}
	
	public void setNomExperimento(String nomExperimento)
	{
		this.nomExperimento = nomExperimento;
	}
	
	public String toString()
	{
		String arquivo = "";
		if(this.getArquivo() != null)
		{
			arquivo = this.getArquivo().getName();
		}
		return "Arquivo: "+arquivo+"\tC: "+this.getCusto()+"\tG:"+this.getGama()+"\tEpslon:"+this.getEpslon()+" resultado: "+this.resultado;
	}
	
	@Override
	public int compareTo(ConfigCustoGama o)
	{
		// TODO Auto-generated method stub
		return this.getArquivo().getName().compareTo(o.getArquivo().getName());
	}
	public static void main(String[] args)
	{
		try
		{
			/*
			String DIRETORIO = "/data/experimentos/journal/baseLines";
			File testeDir = new File(DIRETORIO+"/testes");
			File paramDir = new File(DIRETORIO+"/parametros");
			*/
			String DIRETORIO = "/data/experimentos/jcdl_2012/";
			
			//File testeDir = new File(DIRETORIO+"/testes");
			//File paramDir = new File(DIRETORIO+"/parametros/6_visoes_primNivel_epslon");
			//File paramDir = new File(DIRETORIO+"/parametros/3_visoes_prim_nivel/starVote");
			File paramDir = new File(DIRETORIO+"/parametros/random_primNivel/finalizados");
			//File paramDir = new File(DIRETORIO+"/parametros/local_global_6Visoes_primNivel_epslon");
			ArrayList<ConfigCustoGama> lst = getParamsDiretorio(new ArrayList<ConfigCustoGama>(),paramDir);
			
			Collections.sort(lst);
			Iterator<ConfigCustoGama> i = lst.iterator();
			while(i.hasNext())
			{
				System.out.println(i.next());
			}
			
			
			
			//testarGetParamsDiretorio(DIRETORIO, paramDir);
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public static void testarGetParamsDiretorio(File testeDir, File paramDir)
			throws IOException
	{
		List<ConfigCustoGama> cnfs = getParamsDiretorio(testeDir, paramDir);
		Collections.sort(cnfs);
		Iterator<ConfigCustoGama> i = cnfs.iterator();
		
		int count = 0;
		while(i.hasNext())
		{
			ConfigCustoGama cnf = i.next();
			System.out.println(cnf.toString());
			count++;
		}
		System.out.println("quantidade encontrada: "+count);
	}


	

}
