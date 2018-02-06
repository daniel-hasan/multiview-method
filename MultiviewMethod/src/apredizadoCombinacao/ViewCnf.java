package apredizadoCombinacao;

import java.io.File;

import scriptsUtil.CombinaClassificacaoViews.CLASSIFICADOR;
import config_tmp.ConfigCustoGama;

public class ViewCnf
{
	private File diretorio;
	private ConfigCustoGama cnfCustoGama;
	private CLASSIFICADOR classificador;
	private String cmdTreino;
	private String cmdTeste;
	private String paramTreino;
	private String paramTeste;
	private int numView;
	
	public ViewCnf(int numView,File diretorio, CLASSIFICADOR classificador,String cmdTreino,String cmdTeste,String paramTreino,String paramTeste)
	{
		super();
		this.numView = numView;
		this.diretorio = diretorio;
		this.classificador = classificador;
		this.cmdTreino = cmdTreino;
		this.cmdTeste = cmdTeste;
		this.paramTreino = paramTreino;
		this.paramTeste = paramTeste;
	}
	public ViewCnf(File diretorio, CLASSIFICADOR classificador,ConfigCustoGama cnfCustoGama)
	{
		super();
		this.diretorio = diretorio;
		this.classificador = classificador;
		this.cnfCustoGama = cnfCustoGama;
	}
	public int getNumView()
	{
		return this.numView;
	}
	public void setNumView(int numView)
	{
		this.numView = numView;
	}
	public File getDiretorio()
	{
		return diretorio;
	}
	public void setDiretorio(File diretorio)
	{
		this.diretorio = diretorio;
	}
	public ConfigCustoGama getCnfCustoGama()
	{
		return cnfCustoGama;
	}
	public void setCnfCustoGama(ConfigCustoGama cnfCustoGama)
	{
		this.cnfCustoGama = cnfCustoGama;
	}
	public CLASSIFICADOR getClassificador()
	{
		return classificador;
	}
	public void setClassificador(CLASSIFICADOR classificador)
	{
		this.classificador = classificador;
	}
	public String getCmdTreino()
	{
		return cmdTreino;
	}
	public void setCmdTreino(String cmdTreino)
	{
		this.cmdTreino = cmdTreino;
	}
	public String getCmdTeste()
	{
		return cmdTeste;
	}
	public void setCmdTeste(String cmdTeste)
	{
		this.cmdTeste = cmdTeste;
	}
	public String getParamTreino()
	{
		return paramTreino;
	}
	public void setParamTreino(String paramTreino)
	{
		this.paramTreino = paramTreino;
	}
	public String getParamTeste()
	{
		return paramTeste;
	}
	public void setParamTeste(String paramTeste)
	{
		this.paramTeste = paramTeste;
	}

	
}
