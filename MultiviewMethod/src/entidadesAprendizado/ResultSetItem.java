package entidadesAprendizado;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import arquivo.ArquivoUtil;
import entidadesAprendizado.ResultadoItemViews.ComparationType;

public class ResultSetItem
{
	private Map<Long,List<ResultadoItemViews>> resultadosPorClasse;
	private Fold[] results;

	public ResultSetItem(Fold[] results,boolean porClasse)
	{
		Set<Long> resultsIds = results[0].getIdsResultado();

		resultadosPorClasse = new HashMap<Long,List<ResultadoItemViews>>();
		this.results = results;
		for(Long id : resultsIds)
		{
			//obtem a classe e separa por classe se necessario
			ResultadoItemViews rV = new ResultadoItemViews(id,results);
			long classe =  0;
			if(porClasse)
			{
				classe =  Math.round(rV.getClasseReal());
			}
		
			//adiciona na classe correta
			if(!resultadosPorClasse.containsKey(classe))
			{
				resultadosPorClasse.put(classe, new ArrayList<ResultadoItemViews>());
			}
			List<ResultadoItemViews> lstResultadoClasse = resultadosPorClasse.get(classe);

			lstResultadoClasse.add(rV);
		
		}
		
		
	}
	
	/**
	 * Remove porcentTopEliminar com menor erro de acordo com a media, mse ou variancia (definido pelo ComparationType)
	 * @param compType
	 * @param porcentTopEliminar
	 * @return
	 * @throws IOException 
	 */
	public List<ResultadoItemViews> getTopLessError(List<ResultadoItemViews> resultsEliminados,ComparationType compType,float porcentTopEliminar) throws IOException
	{
		ArrayList<ResultadoItemViews> resultNovoTotal = new ArrayList<ResultadoItemViews>();
		//ArrayList<ResultadoItemViews> resultsEliminados = new ArrayList<ResultadoItemViews>();
		for(Long classeReal : resultadosPorClasse.keySet())
		{
			List<ResultadoItemViews> resultados = resultadosPorClasse.get(classeReal);
			
			//define o metodo de comparacao
			for(ResultadoItemViews r : resultados)
			{
				r.setCompType(compType);
			}
			
			//ordena
			Collections.sort(resultados);
			
			//elimina resultado menores que o threshold
			int numResultadosEliminar = Math.round(resultados.size()*porcentTopEliminar);
			ArrayList<ResultadoItemViews> resultNovo = new ArrayList<ResultadoItemViews>(resultados);
			for(int i =0 ; i<numResultadosEliminar && i<resultados.size() ; i++)
			{
				resultsEliminados.add(resultNovo.get(resultNovo.size()-1));
				resultNovo.remove(resultNovo.size()-1);
			}
			
			//adiciona no resultado global
			resultNovoTotal.addAll(resultNovo);
		}
		
		//imprime os eliminados em arquivo 
		gravaItensEliminados(compType, porcentTopEliminar, resultsEliminados);
		
		return resultNovoTotal;
	}

	private void gravaItensEliminados(ComparationType compType,
			float porcentTopEliminar,
			List<ResultadoItemViews> resultsEliminados) throws IOException
	{
		//File dir = results[0].getOrigem().getParentFile();
		File dirEliminados = new File("ruidoEliminados");
		if(!dirEliminados.exists())
		{
			dirEliminados.mkdir();
		}
		File arqEliminacao = new File(dirEliminados,results[0].getNomeBase()+"_"+compType+"_"+Math.round(porcentTopEliminar*100)+"_fold"+this.results[0].getNum());
		StringBuilder itensEliminados = new StringBuilder();
		for(ResultadoItemViews r : resultsEliminados)
		{
			itensEliminados.append(r.toString()+"\n");
		}
		ArquivoUtil.gravaTexto(itensEliminados.toString(), arqEliminacao, false);
		System.out.println("Ruidos eliminados gravado em:"+arqEliminacao.getAbsolutePath());
	}
	public List<Long> getTopLessErrorIdsResult(List<ResultadoItemViews> lstEliminados, ComparationType compType,float porcentTopEliminar) throws IOException
	{
		List<ResultadoItemViews> lstResultadoItemView = getTopLessError( lstEliminados, compType, porcentTopEliminar);
		List<Long> lstIds =  new ArrayList<Long>();
		
		for(ResultadoItemViews rv : lstResultadoItemView)
		{
			lstIds.add(rv.getId());
		}
		
		return lstIds;
	}
	/**
	 * Remove ou altera classe de views que discordam entre si
	 * @param agreeThreshould
	 * @param distance
	 * @param onlyHighClass
	 * @param changeClass
	 * @return
	 * @throws IOException
	 */
	public List<Long> getProcessaDisagree(List<ResultadoItemViews> itensEliminados, float agreeThreshould,float distance,boolean onlyHighClass) throws IOException
	{
		List<Long> lstIds =  new ArrayList<Long>();
		System.out.println("Processando... agree: "+agreeThreshould);
		//List<ResultadoItemViews> itensEliminados = new ArrayList<ResultadoItemViews>();

		for(Long classeReal : resultadosPorClasse.keySet())
		{
			List<ResultadoItemViews> resultados = resultadosPorClasse.get(classeReal);
			
			
			//define o metodo de comparacao
			for(ResultadoItemViews r : resultados)
			{

					
				//se todos concordam e estão distantes, entao é ruido
				boolean agree = true;
				boolean distantEnough = true;
				for(int v =0 ; v<r.getResultPorView().length ; v++)
				{
					//distante da classe real (erro) 
					if(r.getResultPorView()[v].getErro()<distance  )
					{
						distantEnough = false;
					}
					
					//se todas as visoes concordam com a visao atual
					for(int v2 = 0;  v2<r.getResultPorView().length ; v2++)
					{
						if(v != v2)
						{
							if(Math.abs(r.getResultPorView()[v].getClassePrevista()-r.getResultPorView()[v2].getClassePrevista())>agreeThreshould)
							{
								agree = false;
							}
						}
					}
				}
				
				if( (!(agree && distantEnough)) || (r.getClasseReal() < 4 && onlyHighClass))
				{
					lstIds.add(r.getId());
				}else
				{
					//System.out.println("Eliminado: "+r);
					itensEliminados.add(r);
				}
			}
		}

		gravaItensEliminados(ComparationType.DISCORDANTES,
				(agreeThreshould*10000)+distance,
				itensEliminados);
		
		return lstIds;
	}
	public String toString()
	{
		StringBuilder str = new StringBuilder();
		for(Long classeReal : resultadosPorClasse.keySet())
		{
			List<ResultadoItemViews> resultados = resultadosPorClasse.get(classeReal);
			str.append(toStringResultadoLst(resultados)+"\n");
		}
		return str.toString();
	}

	public static String toStringResultadoLst(List<ResultadoItemViews> resultados)
	{
		StringBuilder strBuild = new StringBuilder("");
		strBuild.append("ID\tResults\tMSE\tMédia\tVariancia\n");
		for(ResultadoItemViews r : resultados)
		{
			strBuild.append(r.getId()+"\t"+r.getResultString()+"\t"+r.getMSE()+"\t"+r.getMeanError()+"\t"+r.getVarianciaError()+"\n");
		}
		return strBuild.toString();
	}
	
	public static void main(String[] args) throws Exception
	{
		test();
		
	}

	private static void test() throws Exception
	{
		Fold[] arrFold = {new Fold(1,"",new ArrayList<ResultadoItem>()),
						  new Fold(2,"",new ArrayList<ResultadoItem>()),
						  new Fold(3,"",new ArrayList<ResultadoItem>())};
		
		for(int i=0 ; i<arrFold.length ; i++)
		{
			arrFold[i].adicionaResultado(new ResultadoItem(1,5.05F,7.5F*(i+1),0.4F));
			arrFold[i].adicionaResultado(new ResultadoItem(2,5.4F,2.5F*(i+1),0.4F));
			arrFold[i].adicionaResultado(new ResultadoItem(3,4.6F,8.5F*(i+1),0.4F));
			arrFold[i].adicionaResultado(new ResultadoItem(4,5.2F,1.5F*(i+1),0.4F));
			arrFold[i].adicionaResultado(new ResultadoItem(5,5.4F,3.5F*(i+1),0.4F));
			arrFold[i].adicionaResultado(new ResultadoItem(6,5.1F,0.5F*(i+1),0.4F));
			arrFold[i].adicionaResultado(new ResultadoItem(7,2.1F,0.5F*(i+1),0.4F));
			arrFold[i].adicionaResultado(new ResultadoItem(8,2.2F,0.5F*(i+1),0.4F));
			arrFold[i].adicionaResultado(new ResultadoItem(9,2.4F,0.5F*(i+1),0.4F));
			arrFold[i].adicionaResultado(new ResultadoItem(10,2.2F,0.5F*(i+1),0.4F));
			arrFold[i].adicionaResultado(new ResultadoItem(11,2.3F,0.5F*(i+1),0.4F));
			arrFold[i].adicionaResultado(new ResultadoItem(12,2.1F,0.5F*(i+1),0.4F));
		}
		
		ResultSetItem rSet = new ResultSetItem(arrFold,true);
		List<ResultadoItemViews> listEliminados = new ArrayList<ResultadoItemViews>();
		System.out.println("========================Normal==================");
		System.out.println(rSet.toString());
		
		System.out.println("========================Ordenado por Media de erro==================");
		System.out.println(toStringResultadoLst(rSet.getTopLessError(listEliminados,ComparationType.MEAN, 0F)));
		System.out.println(toStringResultadoLst(rSet.getTopLessError(listEliminados,ComparationType.MEAN, 0.50F)));
		
		System.out.println("========================Ordenado por MSE==================");
		System.out.println(toStringResultadoLst(rSet.getTopLessError(listEliminados,ComparationType.MSE, 0F)));
		System.out.println(toStringResultadoLst(rSet.getTopLessError(listEliminados,ComparationType.MSE, 0.50F)));
		
		System.out.println("========================Ordenado por Variancia==================");
		System.out.println(toStringResultadoLst(rSet.getTopLessError(listEliminados,ComparationType.VARIANCIA_CLASSE, 0F)));
		System.out.println(toStringResultadoLst(rSet.getTopLessError(listEliminados,ComparationType.VARIANCIA_CLASSE, 0.50F)));
		
		System.out.println("========================Normal==================");
		System.out.println(rSet.toString());
	}
	
}


