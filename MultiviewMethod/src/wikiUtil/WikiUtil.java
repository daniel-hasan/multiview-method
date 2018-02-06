/*
 * Created on 27/03/2008
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package wikiUtil;

import info.bliki.wiki.model.WikiModel;
import it.unimi.dsi.webgraph.ArrayListMutableGraph;
import it.unimi.dsi.webgraph.BVGraph;
import it.unimi.dsi.webgraph.ImmutableGraph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.clapper.util.html.HTMLUtil;

import string.PadraoString;
 
/**
 * @author Daniel Hasan Dalip
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class WikiUtil
{
	private static WikiModel wikiModel =	new WikiModel("http://en.wikipedia.org/wiki/${image}", 
    "http://en.wikipedia.org/wiki/${title}");
	
	public static String wikiToHtml(String text)
	{
		
		return wikiModel.render(text);
	}
	private static Random rdn = new Random(System.currentTimeMillis());
	
	/**
	 * 
	 */
	private WikiUtil()
	{
		super();
		// TODO Auto-generated constructor stub
	}
	
	public static String toPlainText(String text)
	{
		//System.out.println(text);
		text = WikiUtil.wikiToHtml(text);
		
		//System.out.println(text);
		text = HTMLUtil.textFromHTML(text);
		text = HTMLUtil.stripHTMLTags(text);
		
		
		//text = text.replaceAll("\n","");
		//text = text.replaceAll("\r","");
		//text = text.replaceAll("<[bB][rR]>","\n");
		//text = text.replaceAll("<p[^>]*>","\\n");
		//text = text.replaceAll("<[0-9a-zA-Z'\",= ]*>","");
		//text = text.replaceAll("--[^-]*--","");
		//text = text.replaceAll("&#?[0-9a-zA-Z]*;","");
		//text = text.replaceAll("\\{\\{[^\\}]*\\}","");
		
		return text;
	}
	public static ArrayList<String> getPageInnerLinks(String text)
	{
		return getPageLinks("[","]",false,text);
	}
	public static ArrayList<String> getPageLinks(String separadorEsq,String sepDireita,boolean titNoFinal,String text)
	{
		System.out.println("\\"+separadorEsq+"\\"+separadorEsq+"[^"+"\\"+sepDireita+"]*+"+"\\"+sepDireita+"\\"+sepDireita);
		ArrayList<String> blocos = PadraoString.resgataBlocosPadroes("\\"+separadorEsq+"\\"+separadorEsq+"[^"+"\\"+sepDireita+"]*+"+"\\"+sepDireita+"\\"+sepDireita, text);
		//TreeSet<TermoTexto> blocos = PadraoString.resgataTodosPadroes("\\"+separadorEsq+"\\"+separadorEsq+"[^"+"\\"+sepDireita+"]*+"+"\\"+sepDireita+"\\"+sepDireita, text);
        Iterator<String> i = blocos.iterator();

        ArrayList<String> titulos = new ArrayList<String>();
        
        while(i.hasNext())
        {
        		String tituloLinha = i.next();
        		if(tituloLinha.indexOf(separadorEsq)>=0)
        		{
        			String[] link = tituloLinha.substring(tituloLinha.indexOf(separadorEsq)+2, tituloLinha.indexOf(sepDireita)).split("\\|");
        			String titulo = "";
        			if(!titNoFinal)
        			{
        				titulo = link[0];
        			}else
        			{
        				titulo = link[link.length-1];
        			}
        			
        			titulos.add(titulo);
        		}
        		
        }
        return titulos;
        
	}
	public static ArrayListMutableGraph criaGrafo(String nomGrafo,int numNodos) throws IOException
	{
		ArrayListMutableGraph grafo = new ArrayListMutableGraph();
		grafo.addNodes(numNodos);
		ImmutableGraph grafoGravado = grafo.immutableView();
		ImmutableGraph.store(BVGraph.class,grafoGravado,nomGrafo);	

		return grafo;
	}
	public static ArrayListMutableGraph carregaGrafo(String nomGrafo) throws IOException
	{
		ImmutableGraph grafoAtual = BVGraph.load(nomGrafo);
		return new ArrayListMutableGraph(grafoAtual);
	}
	/**
	 * Grava grafo
	 * Matriz  pageDePara: lista de paginas, para cada pagina, indice 0=pageDe indice 1 = pagePara
	 * @param nomGrafo
	 * @param pageDePara
	 * @throws IOException 
	 */
	public static ArrayListMutableGraph adicionaListaGrafo(String nomGrafo,int[][] pageDePara) throws IOException
	{	
		return adicionaListaGrafo(nomGrafo,carregaGrafo(nomGrafo),pageDePara);

	}
	/**
	 * Grava grafo
	 * Matriz  pageDePara: lista de paginas, para cada pagina, indice 0=pageDe indice 1 = pagePara
	 * @param nomGrafo
	 * @param pageDePara
	 * @throws IOException 
	 */	
	public static ArrayListMutableGraph adicionaListaGrafo(String nomGrafo,ArrayListMutableGraph grafo,int[][] pageDePara) throws IOException
	{
		for(int i=0; i<pageDePara.length ; i++)
		{
			try
			{
				grafo.addArc(pageDePara[i][0],pageDePara[i][1]);
			}catch (RuntimeException e)
			{
				// TODO Auto-generated catch block
				System.out.println("Mensagem===>"+e.getMessage());
			}
		}
		
		System.out.println("-------------Gravando-------------------"+grafo.numArcs());
		ImmutableGraph grafoGravado = grafo.immutableView();
		ImmutableGraph.store(BVGraph.class,grafoGravado,nomGrafo);
		grafo = new ArrayListMutableGraph(grafoGravado);
		System.out.println("Num arestas: "+grafoGravado.numArcs());
		System.out.println("-------------Gravado-------------------");
		
		return new ArrayListMutableGraph(grafoGravado);
	}
	public static void main(String[] args)
	{
		
		String text = "<img src='kaka.jpg/> oioi <!-- aalla -->";
		//text = HTMLUtil.textFromHTML(text);
		
		text = HTMLUtil.stripHTMLTags(text);
		System.out.println(text);
		System.exit(0);
		//WikiUtil.wikiToHtml(text)
		//try
		//{
			/*
			//grafo aleatorio
			ArrayListMutableGraph grafo = criaGrafo("grafoLegal",100);
			int[][] pageDePara = new int[10][2];
			double rand = Math.random();
			for(int i = 0; i<pageDePara.length; i++)
			{	
				
				pageDePara[i][0] = (int)Math.round((rdn.nextFloat()*99));				
				pageDePara[i][1] = (int)Math.round((rdn.nextFloat()*99));
				System.out.println(pageDePara[i][0]+", "+pageDePara[i][1]);
			}
			
			grafo = adicionaListaGrafo(grafo,pageDePara);
			System.out.println("Numero de arestas: "+grafo.numArcs());
			
			
			for(int i = 0; i<pageDePara.length; i++)
			{	
					pageDePara[i][0] = (int)Math.round((rdn.nextFloat()*99));
					pageDePara[i][1] = (int)Math.round((rdn.nextFloat()*99));
					System.out.println(pageDePara[i][0]+", "+pageDePara[i][1]);
			}	
			grafo = adicionaListaGrafo(grafo,pageDePara);
			System.out.println("Numero de arestas: "+grafo.numArcs());
			
			//grafo total
			int[][] pageDeParaTotal = new int[10000][2];
			int iPos = 0;
			for(int i=0; i<100; i++)
			{
				for(int j=0 ; j<100; j++)
				{
					pageDeParaTotal[iPos][0] = i; 
					pageDeParaTotal[iPos][1] = j;
					iPos++;
				}
			} 
			grafo = adicionaListaGrafo(grafo,pageDeParaTotal);
			System.out.println("Numero de arestas: "+grafo.numArcs());
			*/
			testaLinks();
		//}
		/*catch(IOException e)
		{
			
		}*/
	}
	/*
	public static String getPlainWikiText(long pageId, Colecao col) throws IOException
	{
		File arq = new File("/data/dumps/artigos/"+col.getSigla()+"/"+pageId);
		if(arq.exists())
		{
			return ArquivoUtil.leTexto(arq);
		}
		return  null;
		
	}
	*/
	private static void testaLinks() {
		//String text = "'''Brazil''' ({{lang-pt|Brasil}}), officially the '''Federative Republic of Brazil''' ({{lang-pt|República Federativa do Brasil}}) {{Audio|Pt-br-República Federativa do Brasil.ogg|listen}}, is the largest country in [[South America]] and the only Portuguese-speaking country on that continent.<ref name=>{{cite web | title = Geography of Brazil | booktitle = The World Factbook | publisher = Central Intelligence Agency | year = 2008 | url = https://www.cia.gov/library/publications/the-world-factbook/geos/br.html | accessdate = 2008-06-03 }}</ref> It is the [[List of countries and outlying territories by total area|fifth largest]] country by geographical area, occupying nearly half of [[South America]]<ref>{{cite web|url=http://www.encyclopedia.com/doc/1E1-Brazil.html|title=Brazil|publisher=[[Encyclopedia.com]]|accessdate=28 October 2009}}</ref> and the [[List of countries by population|fifth most populous]] country in the world.<ref name=/><ref name=>{{cite web | title = People of Brazil | booktitle = The World Factbook | publisher = Central Intelligence Agency | year = 2008 | url = https://www.cia.gov/library/publications/the-world-factbook/geos/br.html | accessdate = 2008-06-03 }}</ref>"; 
		//text += "Bounded by the [[Atlantic Ocean]] on the east, Brazil has a coastline of over {{km to mi|7491|abbrev=yes|precision=0}}.<ref name=/> It is bordered on the north by [[Venezuela]], [[Guyana]], [[Suriname]] and the [[France|French]] overseas department of [[French Guiana]]; on the northwest by [[Colombia]]; on the west by [[Bolivia]] and [[Peru]]; on the southwest by [[Argentina]] and [[Paraguay]] and on the south by [[Uruguay]]. Numerous [[archipelago]]s are part of the Brazilian territory, such as [[Fernando de Noronha]], [[Rocas Atoll]], [[Saint Peter and Paul Rocks]], and [[Trindade and Martim Vaz]].<ref name>";
		String text = "{{performer-voice|Julia Louis-Dreyfus}} [[File:Mollie.png|thumb|300px|Mollie with her ribbons.]] '''Mollie''' is a pampered carriage horse in the [[Jim Henson's Creature Shop|Creature Shop]]-effects film ''[[Animal Farm]]''. Mollie first appears when informed by [[Jessie]] that there is a meeting. Unlike the other animals, who look forward to freedom from humans, Mollie worries about no longer being able to wear the ribbons she treasures. However, early on, she does join [[Boxer]] in finding the food stores, and she marks the spot of [[Old Major|Old Major's]] death with a ribbon. Later, she appears among the animals investigating the farmhouse. ==Literary Source==";
		text += "Mollie represented the upper and middle class [[Russia|Russians]] that fled to the West. In the book, she ran off to another farm when she is forbidden to eat sugar cubes and wear ribbons on her head; the film adapatation glosses over her actual departure.";
		text += "[[Category:Creatures]] [[Category:Animal Farm Characters]]";
		
		ArrayList<String> lista = getPageLinks("[","]",false,text);
		Iterator<String> i = lista.iterator();
		while(i.hasNext())
		{
			System.out.println("Titulo: "+i.next());
		}
	}
}
