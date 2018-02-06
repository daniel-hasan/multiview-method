package arquivo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TempFiles {
	private List<File> lstFiles = new ArrayList<File>();
	private static TempFiles tmpFiles = null;
	private TempFiles()
	{
		
	}
	
	public void addFile(File fle)
	{
		lstFiles.add(fle);
	}
	public static void deleteFolder(File folder) {
	    File[] files = folder.listFiles();
	    if(files!=null) { //some JVMs return null for empty dirs
	        for(File f: files) {
	            if(f.isDirectory()) {
	                deleteFolder(f);
	            } else {
	                f.delete();
	            }
	        }
	    }
	    folder.delete();
	}
	public void deleteFiles()
	{
		for(File f: lstFiles)
		{
			if(f.isDirectory())
			{
				System.out.println("Deletando folder: "+f.getAbsolutePath());
				deleteFolder(f);
			}else
			{
				System.out.println("Deletando arquivo: "+f.getAbsolutePath());
				f.delete();
			}
		}
	}
	
	public static TempFiles getTempFiles()
	{
		if(tmpFiles == null)
		{
			tmpFiles = new TempFiles();
		}
		return tmpFiles;
		
		
	}
}
