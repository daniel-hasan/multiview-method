package entidadesAprendizado;

import io.Sys;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import arquivo.ArquivoUtil;
import arquivo.TempFiles;

public class CnfMetodoAprendizado implements Serializable
{
	private String name;
	private List<Param> lstParamsTreino;
	private List<Param> lstParamsTeste;
	private String scriptTreino;
	private String scriptTeste;
	
	public CnfMetodoAprendizado(String name, List<Param> lstParamsTreino,List<Param> lstParamsTeste,
			String scriptTreino, String scriptTeste)
	{
		super();
		this.name = name;
		this.lstParamsTreino = lstParamsTreino;
		this.lstParamsTeste = lstParamsTeste;
		this.scriptTreino = scriptTreino;
		this.scriptTeste = scriptTeste;
	}
	
	public String getName()
	{
		return name;
	}
	public List<Param> getLstParamsTreino()
	{
		return lstParamsTreino;
	}
	public List<Param> getLstParamsTeste()
	{
		return lstParamsTeste;
	}
	public String getScriptTreino()
	{
		return scriptTreino;
	}
	public String getScriptTeste()
	{
		return scriptTeste;
	}
	public File createTrainFile() throws IOException
	{
		if(this.scriptTreino != null)
		{
			return createScriptFile("train_"+name, scriptTreino);
		}
		return createScriptFile("train_"+name, "");
	}
	public File createTestFile() throws IOException
	{
		return createScriptFile("test_"+name, scriptTeste);
	}
	private static File createScriptFile(String prefix, String script) throws IOException
	{
		File tmpScript = File.createTempFile(prefix, ".sh");
		//tmpScript.deleteOnExit();
		ArquivoUtil.gravaTexto(script, tmpScript, false);
		TempFiles.getTempFiles().addFile(tmpScript);
		System.out.println(Sys.executarComando("chmod 700 "+tmpScript.getAbsolutePath(),true));
		return tmpScript;
	}
	
	public String toString()
	{
		return "======================"+name+"======================\n="+
				"==Params Treino==\n"+
				lstParamsTreino+
				"\n==Script Treino==\n"+
				scriptTreino+
				"\n\n==Params Teste==\n"+
				lstParamsTeste+
				"\n==Script Test==\n"+
				scriptTeste+"\n\n\n";
				
	}

}
