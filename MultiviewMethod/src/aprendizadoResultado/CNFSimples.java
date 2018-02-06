package aprendizadoResultado;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import stuctUtil.ListaAssociativa;

public class CNFSimples
{
	private int numView = 1; 
	private String prefixExp;
	private String metodo;
	private File dataset;
	private Map<String,String> paramTreino;
	private Map<String,String> paramTeste;
	private File dirResultTxt;
	private String sufixExp;
	private ListaAssociativa<String, String> lstParams;
	private boolean isLetor;
	
	
	
	public CNFSimples(String prefixExp, String metodo, File dataset,
			Map<String, String> paramTreino, Map<String, String> paramTeste,
			File dirResultTxt, String sufixExp,
			ListaAssociativa<String, String> lstParams,
			boolean isLetor)
	{
		super();
		this.prefixExp = prefixExp;
		this.metodo = metodo;
		this.dataset = dataset;
		this.paramTreino = paramTreino;
		this.paramTeste = paramTeste;
		this.dirResultTxt = dirResultTxt;
		this.sufixExp = sufixExp;
		this.lstParams = lstParams;
		this.isLetor = isLetor;
	}
	public void setPrefixExp(String prefix)
	{
		this.prefixExp = prefix;
	}
	public void setSufixExpt(String sufix)
	{
		this.sufixExp = sufix;
	}
	public int getNumView()
	{
		return this.numView;
	}
	public void setNumView(int numView)
	{
		this.numView = numView;
	}
	public String getNomExperimento()
	{
		return prefixExp+"_"+metodo+"_"+sufixExp+"_"+dataset.getName();
	}
	public boolean isLetor()
	{
		return this.isLetor;
	}
	public String getPrefixExp()
	{
		return prefixExp;
	}
	public String getMetodo()
	{
		return metodo;
	}
	public File getDataset()
	{
		return dataset;
	}
	public Map<String, String> getParamTreino()
	{
		return paramTreino;
	}
	public Map<String, String> getParamTeste()
	{
		return paramTeste;
	}
	public File getDirResult()
	{
		return dirResultTxt;
	}
	public String getSufixExp()
	{
		return sufixExp;
	}
	public ListaAssociativa<String, String> getLstParams()
	{
		return lstParams;
	}
	
}
