package aprendizadoUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import string.PadraoString;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import arquivo.ArquivoUtil;

public class ConvertDataset
{
	private static int ultId = 1;
	
	public static String convertLinha(String linha,MetodoAprendizado metFrom, MetodoAprendizado metTo,boolean gerarId,Integer id)
	{
		String classe = metFrom.getClasseReal(linha);
		
		Integer docId = null;
		try
		{
			docId = metFrom.getIdPorLinhaArquivo(linha);
		}catch(NotImplementedException ne)
		{
			if(id != null)
			{
				docId = id;
			}else
			{
				throw ne;
			}
		}
		if(docId == null)
		{
			docId = id;
		}

		HashMap<Long, String> features = metFrom.getFeaturesVector(linha);
		
		if(docId != null && gerarId)
		{
			return metTo.gerarLinhaDataset(Double.parseDouble(classe.trim()),docId, features);
		}
		
		return metTo.gerarLinhaDataset(Double.parseDouble(classe.trim()),ultId++, features);
		
	}
	public static File  convertArquivo(File arqFrom, File arqTo,MetodoAprendizado metFrom, MetodoAprendizado metTo,boolean gerarId) throws IOException 
	{
		return convertArquivo( arqFrom,  arqTo, metFrom,  metTo, gerarId, new HashMap<Integer,String>());
	}
	public static File  convertArquivo(File arqFrom, File arqTo,MetodoAprendizado metFrom, MetodoAprendizado metTo,boolean gerarId,Map<Integer,String> idToNom) throws IOException 
	{
		
        
        BufferedReader inIds  = null; 
        BufferedReader in = new BufferedReader(new FileReader(arqFrom));
        BufferedWriter out = new BufferedWriter(new FileWriter(arqTo,false));
		
		if(arqFrom.getName().contains("Teste"))
		{
			//System.out.println("Achou teste!");
			File ids = new File(arqFrom.getParentFile(),arqFrom.getName().replaceAll("Teste", "foldIds"));
			if(ids.exists())
			{
				inIds = new BufferedReader(new FileReader(ids));
			}
		}
		
		//para cada instancia, gera um id incremental caso necessario
		if(gerarId)
		{
			//File ids = new File(arqFrom.getParentFile(),arqFrom.getName().replaceAll("Teste", "foldIds"));
			int id = 1;

			StringBuffer strArqIds = new StringBuffer();
			String str2 = "";
			int numLinha = 0;
			
			while ((str2 = in.readLine()) != null) {
				numLinha++;
				if(metFrom.ignoreFirstLine() && numLinha == 1)
				{
					
					continue;
				}
				strArqIds.append(id+"\n");
				id++;
			}
			in.close();
			 in = new BufferedReader(new FileReader(arqFrom));
			File ids = File.createTempFile("idsFile","");
			ArquivoUtil.gravaTexto(strArqIds.toString().substring(0,strArqIds.toString().length()-1), ids, false);
			inIds = new BufferedReader(new FileReader(ids));
		}
		
		//converte arquivo por linha
		int numLinha = 0;
        String str;

        while ((str = in.readLine()) != null) {
        	numLinha++;
        	if(numLinha == 1)
        	{
        		String cabecalho = metTo.gerarCabecalhoDataset(idToNom);
        		if(cabecalho.length()>0){
        			out.write(cabecalho+"\n");
        		}
        	}
        	if(numLinha == 1 && metFrom.ignoreFirstLine())
        	{
        		
        		continue;
        	}
        	
        	
        	Integer id = null;
        	if(inIds != null)
        	{
        		id = Integer.parseInt(inIds.readLine());
        	}
        	
        	String linha = convertLinha(str,metFrom, metTo,gerarId,id);
        	if(linha.length() > 0)
        	{
        		out.write(linha+"\n");
        	}
        	if(numLinha %1000 == 0)
        	{
        		System.out.println("Convertendo linha "+numLinha);
        	}
        	
        }
        in.close();
        out.close(); 
        
        return arqTo;

	}
	public static void convertAllFromDir(File fromDir,File toDir,MetodoAprendizado metFrom, MetodoAprendizado metTo,String format,String prefix,String ... arrFiltros) throws IOException
	{
		if(!toDir.exists())
		{
			toDir.mkdir();
		}
		File[] arrFile = fromDir.listFiles();
		
		for(File file : arrFile)
		{
			
			if(!file.isDirectory())
			{
				boolean encontrou = false;
				if(arrFiltros.length > 0){
					for(int i = 0 ; i<arrFiltros.length ; i++)
					{
						if(file.getName().contains(arrFiltros[i]) && !file.getName().contains("Model"))
						{
							encontrou = true;
						}		
					}
				
				}else
				{
					encontrou = true;
				}
				if(encontrou)
				{
					
					String name = format.replaceAll("\\{name\\}", file.getName());
					name = name.replaceAll("\\{num\\}", PadraoString.resgataPadrao("[0-9]+", file.getName()));
					name = name.replaceAll("\\{type\\}", PadraoString.resgataPadrao("Teste|Treino", file.getName()));
					if(name.length() == 0)
					{
						name = file.getName();
					}
					File fNew = new File(toDir,name);
					System.out.println("Criando arquivo: "+fNew.getAbsolutePath() +" de "+file.getAbsolutePath());
					convertArquivo(file, fNew,metFrom, metTo,true);
					//break;
				}
			}
		}
	}
	public static void convertAllFromDir(File fromDir,File toDir,MetodoAprendizado metFrom, MetodoAprendizado metTo,String format) throws IOException
	{
		convertAllFromDir(fromDir,toDir, metFrom,  metTo, format,"");
	}
	
	public static void main(String[] args) throws IOException
	{
		
		//svmToLetor(args);
		//converAllFromDir();
		/*
		convertArquivo(new File("/data/experimentos/fonte/LAC/wiki6/wiki6_todos.amostra"), 
				new File("/data/experimentos/fonte/LAC/wiki6/wiki6_todos.lac"),
				new SVM(), 
				new LAC(),true);
		*/
		HashMap<Integer,String> mapStringColunas = new HashMap<Integer,String>();
		mapStringColunas.put(1, "col1");
		mapStringColunas.put(5, "col_5");
		convertArquivo(new File("/home/hasan/Desktop/testeGrande.txt"), new File("/home/hasan/Desktop/testConvert.csv"),new GenericoLetorLike(), new CSV(10),false,mapStringColunas);
	}

	private static void converAllFromDir() throws IOException {
		
		/*
		File diretorio = new File("/home/hasan/views/youtube-sp.iff");
		String[] dirFromFold = {"title" 	,"tag"		,"comment"		,"description"		,"bagow"	,"concat"};
		String[] dirToFold = {"knn/title"	,"knn/tag"	,"knn/comment"	,"knn/description"	,"knn/bagow","knn/concat",};
		*/
		File diretorio = new File("/data/experimentos/fonte");
		String[] dirFromFold = {"LAC/wiki6" 	};//,"LAC/muppets"		,"LAC/starVote"		,"LAC/starAmostra",};
		String[] dirToFold = {"LAC/wiki6/Teste"	};//,"LAC/muppets"	,"LAC/starVote"	,"LAC/starAmostra",};		
		MetodoAprendizado metFrom = new SVM();
		MetodoAprendizado metTo = new LAC();
		
		for(int i =0 ; i<dirFromFold.length ; i++)
		{
			File objFleDirFold = new File(diretorio,dirToFold[i]);
			if(!objFleDirFold.exists())
			{
				objFleDirFold.mkdirs();
			}
			convertAllFromDir(new File(diretorio,dirFromFold[i]),new File(diretorio,dirToFold[i]),
								metFrom, metTo,"{name}","","amostra","Teste","Treino");
		}
	}

	private static void svmToLetor(String[] args) throws IOException {
		if(args.length <= 1)
		{
			System.out.println("Favor especificar o arquivo SVM para converter para LETOR e o destino ");
			System.exit(0);
		}
		
		//"/data/experimentos/vandalismo_maria/svm_test/dataset_underSamp"
		//FeatIdGetter.getMapIdToFeatures("/home/hasan/featIds");
		convertArquivo(new File("/data/experimentos/qa_multiview/params/selector/cook_english/cook.out"), new File("/home/hasan/cook.csv"),new GenericoSVMLike(), new CSV(151),false);
	}
}
