package aprendizadoUtils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import string.RegExpsConst;

import aprendizadoResultado.CalculaResultados;
import aprendizadoResultado.ValorResultado;
import aprendizadoResultado.ValorResultado.MetricaUsada;
import entidadesAprendizado.ResultadoItem;
import featSelector.ValorResultadoMultiplo;

public class GenericoLetorLike extends GenericoSVMLike
{
	public GenericoLetorLike()
	{
		this.setMode(SVM.MODE_REGRESSION);
	}


	public GenericoLetorLike(String cmdTreino,String paramTreino, String cmdTeste,String paramTeste)
	{
		super(cmdTreino,paramTreino,cmdTeste,paramTeste);
		this.setMode(SVM.MODE_REGRESSION);
	}
	
	public GenericoLetorLike(String nomeMetodo,Map<String,String> mapParamTreino,Map<String,String> mapParamTeste) throws Exception
	{
		super(nomeMetodo,mapParamTreino,mapParamTeste);
		this.setMode(SVM.MODE_REGRESSION);
	}
	public GenericoLetorLike(String nomeMetodo) throws Exception
	{
		super(nomeMetodo);
		this.setMode(SVM.MODE_REGRESSION);
		// TODO Auto-generated constructor stub
	}
	
	public boolean linhaMatchesFormat(String linha)
	{
		//System.out.println(linha);
		//System.out.println(RegExpsConst.DIGITO_FLOAT_OPCIONAL+"(( )+"+RegExpsConst.DIGITO+")?( )+qid:"+RegExpsConst.DIGITO+"(( )+[0-9]+:"+RegExpsConst.DIGITO_FLOAT_OPCIONAL+")+( )*(#.*)");
		return linha.matches("([-.eE0-9]+)[ \t]*([0-9]+)?[ \t]+(qid:[0-9]+)([^#]*).*");
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ValorResultado getResultado(List<ResultadoItem> lstResults) throws SQLException
	{
		return new ValorResultadoMultiplo(CalculaResultados.getResultado(lstResults,MetricaUsada.NDCG,10,0),
				CalculaResultados.getResultado(lstResults,MetricaUsada.NDCG,5,0),
				CalculaResultados.getResultado(lstResults,MetricaUsada.NDCG,2,0),
				CalculaResultados.getResultado(lstResults,MetricaUsada.NDCG,1,0)
				
				//CalculaResultados.getResultado(lstResults,MetricaUsada.ERR,10,0),
				//CalculaResultados.getResultado(lstResults,MetricaUsada.ERR,5,0),
				//CalculaResultados.getResultado(lstResults,MetricaUsada.ERR,2,0),
				//CalculaResultados.getResultado(lstResults,MetricaUsada.ERR,1,0)
				);
				
	} 
	
	
	public String gerarLinhaDataset(double classe,int id,int qid,HashMap<Long,String> features)
	{
		return classe+" "+id+" qid:"+qid+" "+gerarLinhaFeatures(features);
	}

	@Override
	public String gerarLinhaDataset(double classe,int id,int qid,HashMap<Long,String> features,Map<String,String> idsParamComment)
	
	{
		idsParamComment.put("id", Integer.toString(id));
		idsParamComment.put("qid", Integer.toString(qid));
		String params = super.criaCommentString(id, idsParamComment);
		// TODO Auto-generated method stub
		return classe+" qid:"+qid+" "+gerarLinhaFeatures(features)+" "+params.trim();
	}
	@Override
	public String getFeaturesString(String linha)
	{
		// TODO Auto-generated method stub
		Pattern formPatt = Pattern.compile("([-.eE0-9]+)[ \t]*([0-9]+)?[ \t]+(qid:[0-9]+)[ \t]+([^#]*).*");
		Matcher m = formPatt.matcher(linha);
		if(m.matches())
		{
			String features = m.group(4);
			return features.trim();
		}else
		{
			return null;
		}
		
	}

	public Integer getQIDFeatureString(String linha)
	{
		// TODO Auto-generated method stub
		Pattern formPatt = Pattern.compile("([-.eE0-9]+)[ \t]*([0-9]+)?[ \t]+(qid:[0-9]+)[ \t]+([^#]*).*");
		Matcher m = formPatt.matcher(linha);
		if(m.matches())
		{
			String features = m.group(3);
			return Integer.parseInt(features.replace("qid:", "").trim());
			
		}else
		{
			return null;
		}
		
	}
	@Override
	public String getClasseReal(String linha)
	{
		// TODO Auto-generated method stub
		String classe = linha.split(" |\t")[0];
		//System.out.println(linha+"\nCLASSE: "+classe);
		
		return classe;
	}
	public static void main(String[] args)
	{
		String feature = "0.0 qid:1250756 1:0.3548387096774194 2:2.0 3:31.0 4:4 5:468 #id:1250927 @qid:1250756";
		Pattern formPatt = Pattern.compile("([-.eE0-9]+)[ \t]*([0-9]+)?[ \t]+(qid:[0-9]+)([^#]*).*");
		Matcher m = formPatt.matcher(feature);
		if(m.matches())
		{
			for(int i =0 ; i<=m.groupCount() ; i++)
			{
				System.out.println("GROUP "+i+": "+m.group(i));
			}
		}
		
	}

}
