package io;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import stuctUtil.Tupla;
import arquivo.ArquivoUtil;



/*
 * Created on 12/06/2008 
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
public class Sys 
{
	//public static boolean EXIBIR_ERRO = false;
	public static String executarComando(String cmd,boolean onlyLastLine) throws IOException
	{
		return executarComando(cmd,onlyLastLine,null);
	} 
	public static String executarComando(String cmd,boolean onlyLastLine,String diretorio) throws IOException
	{
		return executarComando(cmd,onlyLastLine,diretorio,null,false);
	}

	public static String executarComando(String cmd,boolean onlyLastLine,String diretorio,String textToWrite,boolean EXIBIR_ERRO) throws IOException
	{
		//System.out.println(cmd+" (@"+diretorio+")");
	
		String line;
		 
		//Process p = Runtime.getRuntime().exec("cmd.exe /c "+cmd);//Runtime.getRuntime().exec(cmd);
		File dir = null;
		if(diretorio != null)
		{
			dir = new File(diretorio);
		}
		Process p = Runtime.getRuntime().exec(cmd,null,dir);//Runtime.getRuntime().exec(cmd);
		
		return executeCommand(onlyLastLine, textToWrite, EXIBIR_ERRO, p);

	}
	public static String executeCommand(boolean onlyLastLine,
			String textToWrite, boolean EXIBIR_ERRO, Process p)
			throws IOException {
		try
		{
			//System.out.println("Aguardando termino...");
			//p.waitFor();
			//System.out.println("Terminou...");
			//Process p = Runtime.getRuntime().exec("echo oi");
			if(EXIBIR_ERRO)
			{
				BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				
				if(textToWrite != null)
				{
					BufferedWriter out = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
					out.write(textToWrite);
					close(out);
					
				}
				
				//InputStreamReader input = new InputStreamReader(p.getInputStream());
				String textErro = leStreamProcesso(p, new BufferedReader(new InputStreamReader(p.getErrorStream())));
				/*
				if(textErro.length()>0)
				{
					throw new IOException(textErro);
				}
				*/
				textErro += "\n" + leInputStream(onlyLastLine, p);
				input.close();
				return textErro;
			}else
			{
				if(textToWrite != null)
				{
					BufferedWriter out = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
					out.write(textToWrite);
					close(out);
					
				}
			
			//{*/
				return leInputStream(onlyLastLine, p);
			}
			//}
		}finally {
		      if (p != null) {
		          
		          close(p.getInputStream());
		          close(p.getErrorStream());
		          close(p.getOutputStream());
		          
		          p.destroy();
		          
		        }
		}
	}
	private static String leInputStream(boolean onlyLastLine, Process p)
			throws IOException {
		String text = leStreamProcesso(p, new BufferedReader(new InputStreamReader(p.getInputStream())));
		if(onlyLastLine)
		{
			String[] linhas = text.split("\n");
			return linhas[linhas.length-1];
		}else
		{
			return text;
		}
	}
	private static String leStreamProcesso(Process p, BufferedReader input)
			throws IOException {
		String line;
		String text = "";
		
		while ((line = input.readLine()) != null) 
		{
			//System.out.println("Linha:"+line);
			//ultimaLinha = line;
			text+=line+"\n";
			
		}
		
		try {
			
			p.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		input.close();
		return text;
	}
	private static void learquivo(File arq)
	{
		File[] arquivos = arq.listFiles();
		for(int i = 0; i< arquivos.length ; i++)
		{
			if(arquivos[i].isDirectory())
			{
				learquivo(arquivos[i].getAbsoluteFile());
			}else
			{
					//executa o sys
				if(arquivos[i].getName().endsWith("dmi"))
				{
					//executa
				}
			}
		        

		}
	}
	  private static void close(Closeable c) 
	  {
		    if (c != null) {
		      try {
		        c.close();
		      } catch (IOException e) {
		        // ignored
		      }
		    }
	}
	public static synchronized double getPorcentMemLivre()
	{
		return Runtime.getRuntime().freeMemory()/(double) Runtime.getRuntime().totalMemory();
	}
	public static synchronized long getUsedMemory()
	{
		return Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();
	}
	public static String executarComandoComoScript(String cmd,boolean onlyLasLine,String diretorio) throws IOException
	{
		File arq = File.createTempFile("script", ".sh", new File(diretorio));
		//arq.deleteOnExit();
		Sys.executarComando("chmod 700 "+arq.getAbsolutePath(),true);
		ArquivoUtil.gravaTexto(cmd, arq, false);
		return Sys.executarComando(cmd, onlyLasLine, diretorio);
		
	}

	
	public static Tupla<String,String> executarComandoWithErrorAndStdInput(final String commandLine,
	                                     final boolean printOutput,
	                                     final boolean printError,
	                                     final long timeoutMilli)
	  throws IOException, TimeoutException
	{
	//System.out.println("Numero de threads:"+ Thread.getAllStackTraces().keySet().size());
	//System.out.println(commandLine);
	  Runtime runtime = Runtime.getRuntime();
	  Process process = runtime.exec(commandLine);
	  /* Set up process I/O. */
      StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "OUTPUT", printOutput);
      StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), "ERROR", printError);
      outputGobbler.start();
      errorGobbler.start();
      
	  Worker worker = new Worker(process);
	  worker.start();
	  //System.out.println("Numero de threads:"+ Thread.getAllStackTraces().keySet().size());
	  try {
	    worker.join(timeoutMilli);
	    if(worker.isOK())
	    {
	    	return new Tupla<String,String>(outputGobbler.getMessage(),errorGobbler.getMessage());
	    }else
	    {
		    outputGobbler.setToStop();
		    errorGobbler.setToStop();
		    outputGobbler.interrupt();
		    errorGobbler.interrupt();
		    //worker.interrupt();
		    System.out.println("Numero de threads:"+ Thread.getAllStackTraces().keySet());
		    System.out.println("Deu timeout!!!!!!");
		    throw new TimeoutException();
		    
	    }

	    
	  } catch(InterruptedException ex) {
		  
	    //worker.interrupt();
	    Thread.currentThread().interrupt();
	    outputGobbler.interrupt();
	    errorGobbler.interrupt();
	    throw new TimeoutException();
	  } finally {
		  
		 // System.out.println("Numero de threads:"+ Thread.getAllStackTraces().keySet().size());
		 // System.out.println("Matando process.. >:D");
		  try {
			killProcessTree(process);
			System.out.println("foioi");
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		  //System.out.println("Proc morto!!!.. >:D");
	  }
	  
	}
	
	private static class Worker extends Thread {
	  private final Process process;
	  private boolean isOK = false;
	  private Worker(Process process) {
	    this.process = process;
	  }
	  public boolean isOK()
	  {
		  return this.isOK;
	  }
	  public void run() {
	    try { 
	      process.waitFor();
	      isOK = true;
	    } catch (InterruptedException ignore) {
	    	process.destroy();
	    	System.out.println("Worker saiu");
	      return;
	    }
	  }  
	}
	

	/**
	 * 
	 */
	public Sys()
	{
		super();
		// TODO Auto-generated constructor stub
	}
	public static synchronized void waitMet() throws InterruptedException
	{
		Integer t = 1;
		t.wait(10000L);
	}
	public static int getProcessIdLinux(Process p ) throws IllegalArgumentException, IllegalAccessException, SecurityException, NoSuchFieldException
	{
		Field f = p.getClass().getDeclaredField("pid");
		f.setAccessible(true);
		
		return f.getInt(p);
	}
	public static void killProcess(Process p ) throws IllegalArgumentException, SecurityException, IOException, IllegalAccessException, NoSuchFieldException{
		killProcess(getProcessIdLinux(p));
		
	}
	public static List<Integer> getProcessIdTree(Process p) throws IOException, IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException
	{
		return getProcessIdTree(getProcessIdLinux(p));
	}
	
	public static List<Integer> getProcessIdTree(int pid) throws IOException
	{
		List<Integer> lstIds = new ArrayList<Integer>();
		String[] arrStrPids = Sys.executarComando("ps -o pid --ppid "+pid+" --noheaders", false).split("\n");
		
		for(String strPid : arrStrPids)
		{
			
			if(strPid.matches("[0-9]+"))
			{
				int intPid = Integer.parseInt(strPid);
				lstIds.add(intPid);
				lstIds.addAll(getProcessIdTree(intPid));
			}
		}
		return lstIds;
	}
	public static void killProcessTree(Process p ) throws IllegalArgumentException, SecurityException, IOException, IllegalAccessException, NoSuchFieldException{
		List<Integer> lstProcess = getProcessIdTree(p);
		killProcess(p);
		for(Integer pid : lstProcess)
		{
			killProcess(pid);
		}
		
	}
	private static void killProcess(Integer pid) throws IOException {
		// TODO Auto-generated method stub
		String pidAlive = "";
		pidAlive = Sys.executarComando("ps -o pid --pid "+pid+" --noheaders",false);
		do
		{
			
			Sys.executarComando("kill -9 "+pid, true);
			pidAlive = Sys.executarComando("ps -o pid --pid "+pid+" --noheaders",false);
			if(pidAlive.contains(Integer.toString(pid)))
			{
				System.out.println("=================== Ainda existe o pid "+pid+" tentando novamente!!!===================");
				
			}
		}while(pidAlive.contains(Integer.toString(pid)));
	}
	public static void main(String[] args) throws IOException
	{
		
		/*
		File file = new File("/home/hasan");
		for(File arq : file.listFiles())
		{
			if(arq.getName().endsWith(".sql"))
			{
				String texto = Sys.executarComando("echo 'teste "+arq.getName()+"' ", false, file.getAbsolutePath());
				System.out.println("SAIDA:"+texto);
				String ac= "Perpecti = 1 (0.2%)" +
							"1-n-gram = 10%\n" +
							"2-n-gram = 20% \n";
				
				String pe = PadraoString.resgataPadrao("Perpecti = [0-9]+ \\([.0-9%]+\\)", ac);
				
		
				
				
			}
		}
		*/
		
			try {
				Sys.executarComandoWithErrorAndStdInput("/home/hasan/sleep.sh",true,true,5000);
				
			}  catch (TimeoutException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Timout!!!");
			}
			System.out.println("Oioioii");
			System.exit(0);
		
		
		
	}
}
