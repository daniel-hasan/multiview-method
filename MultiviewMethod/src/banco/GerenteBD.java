/*
 * Created on Oct 16, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package banco;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import stuctUtil.Tupla;

/**
 * @author j2se04b3
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
//SINGLETON
public class GerenteBD
{
    private static GerenteBD pool=null;
    private static HashMap conexoes;

    

    private GerenteBD()
    {
        //inicializa BD

        conexoes = new HashMap();

    }
    
    public static GerenteBD getGerenteBD()throws SQLException,ClassNotFoundException
    {	
        if (pool == null)
        {
            pool=new GerenteBD();
        }
        return pool;
    }
    public void fechaConexao(String nomConexao)throws SQLException
    {
    	String nomConexaoTh = getThreadName();
    	if(conexoes.containsKey(nomConexaoTh))
    	{ 
    		((Connection) conexoes.get(nomConexaoTh)).close();
    		conexoes.remove(nomConexaoTh);
    		//System.out.println("fecha conexao ");
    		
    	}
    }
    public static Statement getStatementStream(Connection conn) throws SQLException
    {
    	Statement stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
		stmt.setFetchSize(Integer.MIN_VALUE);
		return stmt;
    }
    private Connection criaConexao()throws ClassNotFoundException,SQLException
    {
    	boolean isUFMG = false; 
    	
    	
    	try {
    		Enumeration<NetworkInterface> netIts = NetworkInterface.getNetworkInterfaces();
    		while(netIts.hasMoreElements())
    		{
    			NetworkInterface ni = netIts.nextElement();
    			Enumeration<InetAddress> netAdresses = ni.getInetAddresses();
    			while(netAdresses.hasMoreElements())
    			{
    				InetAddress netAd = netAdresses.nextElement();
    				if(netAd.getHostAddress().startsWith("150.164.2"))
    				{
    					System.out.println("UFMG!");
    					isUFMG = true;
    				}
    				
					byte[] arrByteAddress = netAd.getAddress();
					if(arrByteAddress[0] == 150 && arrByteAddress[1] == 164 && arrByteAddress[2] == 2)
					{
						System.out.println("UFMG!");
						//isUFMG = true;
					}else
					{
						System.out.println("Ip nao eh ufmg: "+arrByteAddress[0]+"."+arrByteAddress[1]+"."+arrByteAddress[2]+"."+arrByteAddress[3]);
					}
    			}
    		}
    		if(!isUFMG)
			{
				System.out.println("Acesso externo... ");
			}
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			System.out.println("UFMG! (nao encontrou host)");
			isUFMG = true;
		}
    	
    	//isUFMG = fals;
        Class.forName("com.mysql.jdbc.Driver");
        //String url="jdbc:mysql://150.164.2.28:3306/wikipedia_amostra_inicial?user=root&password=&";
        //String url="jdbc:mysql://150.164.2.24:3306/wikipedia_amostra_inicial?user=root&password=&";
        //String url="jdbc:mysql://127.0.0.1:3306/abec?user=root&password=&";
        String url = "";
        if(isUFMG)
        {
        	System.out.println("URL EH UFMG!!! host");
        	url="jdbc:mysql://XXX:3306/wikipedia_amostra_inicial?user=XXXX&password=XXXX@XX";
        	
        	
        }else
        {
        	System.out.println("URL nao EH UFMG!!!");
        	url="jdbc:mysql://127.0.0.1:3306/resultados_multiview?user=XXXXXXS&password=CCCC@XXXX";	
        }
        //
        
        Connection aconn=DriverManager.getConnection(url);
        aconn.setAutoCommit(true);
        return aconn;
    }
    public String getThreadName()
    {
    	return getThreadName(false);
    }
    public String getThreadName(boolean nameIsId)
    {
    	if(nameIsId)
    		return Thread.currentThread().getName()/*+"_"+Thread.currentThread().getId()+"_"*/;
    	else
	    	return Thread.currentThread().getName()+"_"+Thread.currentThread().getId()+"_";
    }
    public synchronized Connection obtemConexao(String nomConexao)throws ClassNotFoundException,SQLException
    {
    	return obtemConexao( nomConexao,false);
    }
    public synchronized Connection obtemConexao(String nomConexaoa,boolean nameIsId)throws ClassNotFoundException,SQLException
    {
    	String nomConexaoTh = getThreadName(nameIsId);
    	if(!conexoes.containsKey(nomConexaoTh))
    	{
    		Connection aConexao = criaConexao();
    		conexoes.put(nomConexaoTh,aConexao);

    		//System.out.println("criou nova conexao: "+nomConexaoTh+" Conexoes: "+conexoes.keySet());
    		return aConexao;
    	}
    	else 
    	{
    		//System.out.println("Conexao thread: "+nomConexaoTh);
    		return (Connection)conexoes.get(nomConexaoTh);
    	}
    }
    
    public void efetuaCommit(Connection conn)throws SQLException
    {
        conn.commit();
    }
    
    public enum DataType{
    	CHAR,BINARY,INT,DOUBLE,DATE_TIME,BIT,NOT_FOUND;
    }
    public static String toInList(Collection lstValues)
    {
    	StringBuffer strIn = new StringBuffer();
    	for(Object value : lstValues)
    	{
    		strIn.append(value.toString()+",");
    	}
    	String strInNotBuff = strIn.substring(0, strIn.length()-1);
    	
    	return strInNotBuff;
    }

}
