package io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Utility thread class which consumes and displays stream input.
 * 
 * Original code taken from http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html?page=4
 */
public class StreamGobbler
    extends Thread
{
    private InputStream inputStream;
    private String streamType;
    private boolean displayStreamOutput;
    private String message = "";
    private boolean stop = false;
    private long timeStart;
    /**
     * Constructor.
     * 
     * @param inputStream the InputStream to be consumed
     * @param streamType the stream type (should be OUTPUT or ERROR)
     * @param displayStreamOutput whether or not to display the output of the stream being consumed
     */
    StreamGobbler(final InputStream inputStream,
                  final String streamType,
                  final boolean displayStreamOutput)
    {
        this.inputStream = inputStream;
        this.streamType = streamType;
        this.displayStreamOutput = displayStreamOutput;
        this.timeStart = System.currentTimeMillis();
    }
    public String getMessage()
    {
    	return this.message;
    }
    public void setToStop()
    {
    	/*try {
			
    		inputStream.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			
		}
		*/
    	stop = true;
    }
    /**
     * Consumes the output from the input stream and displays the lines consumed if configured to do so.
     */
    @Override
    public void run()
    {
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        try
        {

            String line = null;
            StringBuffer strMessage = new StringBuffer();
            while ((line = bufferedReader.readLine()) != null)
            {
            	/*
                if (displayStreamOutput)
                {
                    System.out.println(streamType + ">" + line);
                }
                */
                strMessage.append(line+"\n");
                if(stop)
                {
                	System.out.println(streamType+" saiu");
                	bufferedReader.close();
                	return;
                }
            }
            message = strMessage.toString();
            bufferedReader.close();
        }
        
        catch (IOException ex)
        {
            //ex.printStackTrace();
            
        }finally
        {
        	//System.out.println(streamType+" saiu em "+(System.currentTimeMillis()-this.timeStart)/1000.0);
        }
        ;
    }
}