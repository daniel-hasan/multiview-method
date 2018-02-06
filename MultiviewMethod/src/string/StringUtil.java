package string;

import info.bliki.wiki.dump.WikiArticle;
import io.Sys;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import stuctUtil.Tupla;
import wikiUtil.WikiUtil;

/*
 * Created on 05/07/2007
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
/**
 * @author Daniel Hasan Dalip
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class StringUtil {
	private static String END_STOP_WORDS = "/home/hasan/data/dumps/stopWords.csv";
	//private static String END_STOP_WORDS = "/home/curupira/hasan/stopWords.csv";
	private static String[] stopWords= carregaStopWords();
	public static final String APENAS_ACENTOS = ((char)225)+"-"+((char)250);
	public static Pattern objPatternStopWord;
	private static Pattern objPonctuation;
	private static HashMap<String,Boolean> mapWords = new HashMap<String, Boolean>();
	public static String[] stemmAndRemoveStopWords(String text)
	{

		
				
		
		//retira espaços em brandco duplicados 
		while(text.contains("  "))
		{
			text = text.replaceAll("  ", " ");
		}
		
		//remove stopwords
		//carregaStopWords();
		text = retiraStopWords(text.split(" ")).trim();
		
		
		String[] words = stemmText(text);
		

		return words;
	
	}
	public static String stemmAndRemoveStopWordsString(String text)
	{
		String strText = "";
		for(String termo : stemmAndRemoveStopWords(text))
		{
			strText += termo+" ";
		}
		return strText.trim();
	
	}
	/**
	 * Retorna o texto sem os blocoscom a tag inicioTag e uma lista com os blocos retirados
	 * @param texto
	 * @param inicioTag
	 * @param fimTag
	 * @return
	 */
	public static Tupla<String,List<String>> retiraBlocoTag(String texto,String inicioTag,String fimTag)
	{
		/*System.out.println("------------------------------------------------");
		System.out.println(texto);
		System.out.println("------------------------------------------------");*/
		List<String> lstBlocos = new ArrayList<String>();
		String inicioTagLower = inicioTag.toLowerCase();
		String fimTagLower = fimTag.toLowerCase();
		
		while(texto.toLowerCase().contains(inicioTagLower))
		{
			String textoLowerCase = texto.toLowerCase();	
			//resgata o inicio e o fim da proxima tag
			
			int idxInicioTag = textoLowerCase.indexOf(inicioTagLower);
			int idxFimTag = textoLowerCase.indexOf(fimTagLower);
			int idxInicioProxTag = textoLowerCase.indexOf(inicioTagLower,idxInicioTag+1);
			if(idxFimTag == -1)
			{
				return new Tupla<String,List<String>>(texto,lstBlocos);
			}
			
			//se o inicio da proxima tag é antes do fim da tag, quer dizer que este fim tag é de outra tag (ex: <x> <x> </x> 
			while((idxInicioProxTag < idxFimTag) && idxInicioProxTag>0)
			{
				idxInicioTag = idxInicioProxTag;
				idxInicioProxTag = textoLowerCase.indexOf(inicioTagLower,idxInicioTag+1);
			}
			if(idxInicioTag < 0 || idxFimTag < 0)
			{
				return new Tupla<String,List<String>>(textoLowerCase,lstBlocos);
			}
			//System.out.println("retirou de "+idxInicioTag+" até "+idxFimTag+": "+texto.substring(idxInicioTag,idxFimTag+fimTag.length()));
			if(idxInicioTag < idxFimTag)
			{
				lstBlocos.add(texto.substring(idxInicioTag+inicioTagLower.length(), idxFimTag));
				texto = texto.substring(0,idxInicioTag)+texto.substring(idxFimTag+fimTagLower.length(),texto.length());
				
			}else
			{
				//exclui o fimtag invalido
				texto = texto.substring(0,idxFimTag)+texto.substring(idxFimTag+fimTagLower.length(),texto.length());
			}
		}
		
		return new Tupla<String,List<String>>(texto,lstBlocos);
	}
	/**
	 * Retorna a lista de blocos definidos por uma tag inicial ex: <h1> lala </h1> X <h1>  asdasd</h1> Z
	 * Retorna uma lista com os itens X e Z
	 * @param texto
	 * @param inicioTag
	 * @param fimTag
	 * @return
	 */
	public static List<String> retiraBlocoTagInicial(String texto,String inicioTag,String fimTag)
	{
		List<String> lstBlocos = new ArrayList<String>();
		while(texto.contains(inicioTag))
		{
			
			//resgata o inicio e o fim da proxima tag
			int idxInicioTag = texto.indexOf(inicioTag);
			int idxFimTag = texto.indexOf(fimTag);
			int idxInicioProxTag = texto.indexOf(inicioTag,idxInicioTag+1);
			if(idxInicioProxTag == -1)
			{
				idxInicioProxTag = texto.length();
			}
			if(idxInicioTag == -1)
			{
				return lstBlocos;
			}
			

			
			//System.out.println("retirou de "+idxInicioTag+" até "+idxFimTag+": "+texto.substring(idxInicioTag,idxFimTag+fimTag.length()));
			//adiciona do fim da tag atual até o inicio da proxima tag
			lstBlocos.add(texto.substring(idxFimTag+fimTag.length(), idxInicioProxTag));
			texto = texto.substring(idxInicioProxTag);
		}
		
		return lstBlocos;
	}
	public static String strArrToString(String[] arrStr,String separator)
	{
		String strText = "";
		for(String str : arrStr)
		{
			strText += str+separator;
			
		}
		return strText;
	}
	public static String[] stemmText(String text) {

		
		//stem
		Stemmer s = new Stemmer();
		String[] words = text.split(" ");
		List<String> lstWordsSteamed = new ArrayList<String>();
		StringBuilder textStemmed = new StringBuilder();

		for(String word : words)
		{
			if(word.length() > 0)
			{
				s.add(word.toLowerCase().toCharArray(), word.length());
				s.stem();
				lstWordsSteamed.add(s.toString());
			}
		}
		String[] wdsStemmed = new String[lstWordsSteamed.size()];
		for(int i = 0; i< lstWordsSteamed.size() ; i++)
		{
			wdsStemmed[i] = lstWordsSteamed.get(i);
		}
		return wdsStemmed;
	}
	
	
	/**
	 * Returns the substring which is the prefix of the both strings
	 * @param textOne
	 * @param textTwo
	 * @return
	 */
	public static String stringEqualPrefix(String textOne, String textTwo)
	{
		StringBuilder prefix = new StringBuilder("");
		int min = textOne.length();
		if(min>textTwo.length())
		{
			min = textTwo.length();
		}
		for(int i =0 ; i<min ; i++)
		{
			if(textOne.charAt(i) == textTwo.charAt(i))
			{
				prefix.append(textOne.charAt(i));
			}else
			{
				return prefix.toString();
			}
		}
		
		return prefix.toString();
	}
	public static String removePonctuation(String text,String strCharToMantain)
	{
		
		//objPonctuation = Pattern.compile("[^a-z0-9"+strCharToMantain+" ]",Pattern.CASE_INSENSITIVE);
		
		
		return text.replaceAll("[^a-zA-Z0-9"+strCharToMantain+" ]", " ");
	}
	public static String removePonctuation(String text)
	{
		
		
		if(objPonctuation == null)
		{
			objPonctuation = Pattern.compile("[^a-z0-9 ]",Pattern.CASE_INSENSITIVE);
		}
		
		return objPonctuation.matcher(text).replaceAll(" ");
		
	}
	public static String removeDoubleSpace(String t)
	{
		while(t.contains("  "))
		{
			t = t.replaceAll("  ", " ");
		}
		return t;
	}
	public static int countOccorencias(String text,String word)
	{
		int i =0;
		int index = -1;
		
		while( (index = text.indexOf(word,index+1))>=0 )
		{
			i++;
		}
		return i;
	}
	public static String retiraStopWords(String text) 
	{
		
		
		for(int i =0 ;i<stopWords.length; i++)
		{
			text = Pattern.compile("[^a-z]"+stopWords[i]+"[^a-z]",Pattern.CASE_INSENSITIVE).matcher(text).replaceAll(" ");
		}
		
		
		

		
		
		//text = objPatternStopWord.matcher(text).replaceAll(" ");
		return text;

	}
	public static String retiraStopWordsAndPonctuation(String text) 
	{

		String[] arrPalavras = removePonctuation(text).split(" ");
		
		String strString = retiraStopWords(arrPalavras);
		return StringUtil.removeDoubleSpace(strString);
	}
	public static boolean isStopWord(String palavra)
	{
		if(mapWords.size() == 0)
		{
			carregaStopWords();
		}
		return mapWords.containsKey(palavra.toUpperCase()); 
	}

	public static String retiraStopWords(String[] arrPalavras) {
		if(mapWords.size() == 0)
		{
			carregaStopWords();
		}
		StringBuilder strString = new StringBuilder();
		for(String palavra : arrPalavras)
		{
			if(!mapWords.containsKey(palavra.toUpperCase()))
			{
				strString.append(palavra+" ");
			}
		}
		String str = strString.toString().replaceAll("('|’)(s|t)", "");
		return str;
	}
	public static String[] carregaStopWords()
	{
		try
		{
			BufferedReader r = new BufferedReader(new FileReader(new File(END_STOP_WORDS)));
			String listaPalavras = "";
			String linha;
			while((linha = r.readLine())!=null)
			{
				listaPalavras += linha.replaceAll(" ","");
			}
			r.close();
			
			
			
			//faz uma expressao regular com todas as stopwords
			String[] arrPalavras = listaPalavras.split(",");
			StringBuilder strRegExp = new StringBuilder();
			mapWords = new HashMap<String, Boolean>();
			for(int i =0 ;i<arrPalavras.length; i++)
			{
				strRegExp.append("("+arrPalavras[i]+")");
				if(i<arrPalavras.length-1)
				{
					strRegExp.append("|");	
				}
				mapWords.put(arrPalavras[i].toUpperCase(),true);
				
			}
			objPatternStopWord = Pattern.compile("[^a-z]("+strRegExp.toString()+")[^a-z]",Pattern.CASE_INSENSITIVE);
			
			
			return listaPalavras.split(",");
		} catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		return null;
	}
	private static HashMap<String,Character> mapStringHtml = new HashMap<String,Character>();
	
	static{ 
		 mapStringHtml.put("&Agrave;",'À' );
		    mapStringHtml.put("&Aacute;",'Á' );
		    mapStringHtml.put("&Acirc;",'Â' );
		    mapStringHtml.put("&Atilde;",'Ã');
		    mapStringHtml.put("&Auml;",'Ä');
		    mapStringHtml.put("&Aring;",'Å');
		    mapStringHtml.put("&Aelig;",'Æ' );
		    mapStringHtml.put("&agrave;",'à');
		    mapStringHtml.put("&aacute;",'á');
		    mapStringHtml.put("&acirc;",'â');
		    mapStringHtml.put("&atilde;",'ã' );
		    mapStringHtml.put("&auml;",'ä');
		    mapStringHtml.put("&aring;",'å');
		    mapStringHtml.put("&aelig;",'æ');
		    mapStringHtml.put("&Ccedil;",'Ç');
		    mapStringHtml.put("&ccedil;",'ç');
		    mapStringHtml.put("&ETH;",'Ð');
		    mapStringHtml.put("&eth;",'ð');
		    mapStringHtml.put("&Egrave;",'È');
		    mapStringHtml.put("&Eacute;",'É');
		    mapStringHtml.put("&Ecirc;",'Ê');
		    mapStringHtml.put("&Euml;",'Ë');
		    mapStringHtml.put("&egrave;",'è');
		    mapStringHtml.put("&eacute;",'é');
		    mapStringHtml.put("&ecirc;",'ê');
		    mapStringHtml.put("&euml;",'ë');
		    mapStringHtml.put("&Igrave;",'Ì');
		    mapStringHtml.put("&Iacute;",'Í');
		    mapStringHtml.put("&Icirc;",'Î');
		    mapStringHtml.put("&Iuml;",'Ï');
		    mapStringHtml.put("&igrave;",'ì');
		    mapStringHtml.put("&iacute;",'í');
		    mapStringHtml.put("&icirc;",'î');
		    mapStringHtml.put("&iuml;",'ï');
		    mapStringHtml.put("&Ntilde;",'Ñ');
		    mapStringHtml.put("&ntilde;",'ñ');
		    mapStringHtml.put("&Ograve;",'Ò');
		    mapStringHtml.put("&Oacute;",'Ó');
		    mapStringHtml.put("&Ocirc;",'Ô');
		    mapStringHtml.put("&Otilde;",'Õ');
		    mapStringHtml.put("&Ouml;",'Ö');
		    mapStringHtml.put("&Oslash;",'Ø');
		    mapStringHtml.put("&OElig;",'Œ');
		    mapStringHtml.put("&ograve;",'ò');
		    mapStringHtml.put("&oacute;",'ó');
		    mapStringHtml.put("&ocirc;",'ô');
		    mapStringHtml.put("&otilde;",'õ');
		    mapStringHtml.put("&ouml;",'ö');
		    mapStringHtml.put("&oslash;",'ø');
		    mapStringHtml.put("&oelig;",'œ');
		    mapStringHtml.put("&Ugrave;",'Ù');
		    mapStringHtml.put("&Uacute;",'Ú');
		    mapStringHtml.put("&Ucirc;",'Û');
		    mapStringHtml.put("&Uuml;",'Ü');
		    mapStringHtml.put("&ugrave;",'ù');
		    mapStringHtml.put("&uacute;",'ú');
		    mapStringHtml.put("&ucirc;",'û');
		    mapStringHtml.put("&uuml;",'ü');
		    mapStringHtml.put("&Yacute;",'Ý');
		    mapStringHtml.put("&Yuml;",'Ÿ');
		    mapStringHtml.put("&yacute;",'ý');
		    mapStringHtml.put("&yuml;",'ÿ');
		    
		    
			mapStringHtml.put("&quot;",'"');
			mapStringHtml.put("&amp;",'&');
			mapStringHtml.put("&lt;",'<');
			mapStringHtml.put("&gt;",'>');
			mapStringHtml.put("&euro;",'€');
			/*
			mapStringHtml.put("&fnof;",null);
			mapStringHtml.put("&hellip;",null);
			mapStringHtml.put("&dagger;",null);
			mapStringHtml.put("&Dagger;",null);
			mapStringHtml.put("&circ;",null);
			mapStringHtml.put("&permil;",null);
			mapStringHtml.put("&Scaron;",null);
			mapStringHtml.put("&lsaquo;",null);
			mapStringHtml.put("&OElig;",null);
			mapStringHtml.put("&lsquo;",null);
			mapStringHtml.put("&rsquo;",null);
			mapStringHtml.put("&ldquo;",null);
			mapStringHtml.put("&rdquo;",null);
			mapStringHtml.put("&bull;",null);
			mapStringHtml.put("&ndash;",null);
			mapStringHtml.put("&mdash;",null);
			mapStringHtml.put("&tilde;",null);
			mapStringHtml.put("&trade;",null);
			mapStringHtml.put("&scaron;",null);
			mapStringHtml.put("&rsaquo;",null);
			mapStringHtml.put("&oelig;",null);
			mapStringHtml.put("&Yuml;",null);
			mapStringHtml.put("&nbsp;",null);
			mapStringHtml.put("&iexcl;",null);
			mapStringHtml.put("&cent;",null);
			mapStringHtml.put("&pound;",null);
			mapStringHtml.put("&curren;",null);
			mapStringHtml.put("&yen;",null);
			mapStringHtml.put("&brvbar;",null);
			mapStringHtml.put("&sect;",null);
			mapStringHtml.put("&uml;",null);
			mapStringHtml.put("&copy;",null);
			mapStringHtml.put("&ordf;",null);
			mapStringHtml.put("&laquo;",null);
			mapStringHtml.put("&not;",null);
			mapStringHtml.put("&shy;",null);
			mapStringHtml.put("&reg;",null);
			mapStringHtml.put("&macr;",null);
			mapStringHtml.put("&deg;",null);
			mapStringHtml.put("&plusmn;",null);
			mapStringHtml.put("&sup2;",null);
			mapStringHtml.put("&sup3;",null);
			mapStringHtml.put("&acute;",null);
			mapStringHtml.put("&micro;",null);
			mapStringHtml.put("&para;",null);
			mapStringHtml.put("&middot;",null);
			mapStringHtml.put("&cedil;",null);
			mapStringHtml.put("&sup1;",null);
			mapStringHtml.put("&ordm;",null);
			mapStringHtml.put("&raquo;",null);
			mapStringHtml.put("&frac14;",null);
			mapStringHtml.put("&frac12;",null);
			mapStringHtml.put("&frac34;",null);
			mapStringHtml.put("&iquest;",null);
			mapStringHtml.put("&Agrave;",null);
			mapStringHtml.put("&Aacute;",null);
			mapStringHtml.put("&Acirc;",null);
			mapStringHtml.put("&Atilde;",null);
			mapStringHtml.put("&Auml;",null);
			mapStringHtml.put("&Aring;",null);
			mapStringHtml.put("&AElig;",null);
			mapStringHtml.put("&Ccedil;",null);
			mapStringHtml.put("&Egrave;",null);
			mapStringHtml.put("&Eacute;",null);
			mapStringHtml.put("&Ecirc;",null);
			mapStringHtml.put("&Euml;",null);
			mapStringHtml.put("&Igrave;",null);
			mapStringHtml.put("&Iacute;",null);
			mapStringHtml.put("&Icirc;",null);
			mapStringHtml.put("&Iuml;",null);
			mapStringHtml.put("&ETH;",null);
			mapStringHtml.put("&Ntilde;",null);
			mapStringHtml.put("&Ograve;",null);
			mapStringHtml.put("&Oacute;",null);
			mapStringHtml.put("&Ocirc;",null);
			mapStringHtml.put("&Otilde;",null);
			mapStringHtml.put("&Ouml;",null);
			mapStringHtml.put("&times;",null);
			mapStringHtml.put("&Oslash;",null);
			mapStringHtml.put("&Ugrave;",null);
			mapStringHtml.put("&Uacute;",null);
			mapStringHtml.put("&Ucirc;",null);
			mapStringHtml.put("&Uuml;",null);
			mapStringHtml.put("&Yacute;",null);
			mapStringHtml.put("&THORN;",null);
			mapStringHtml.put("&szlig;",null);
			mapStringHtml.put("&agrave;",null);
			mapStringHtml.put("&aacute;",null);
			mapStringHtml.put("&acirc;",null);
			mapStringHtml.put("&atilde;",null);
			mapStringHtml.put("&auml;",null);
			mapStringHtml.put("&aring;",null);
			mapStringHtml.put("&aelig;",null);
			mapStringHtml.put("&ccedil;",null);
			mapStringHtml.put("&egrave;",null);
			mapStringHtml.put("&eacute;",null);
			mapStringHtml.put("&ecirc;",null);
			mapStringHtml.put("&euml;",null);
			mapStringHtml.put("&igrave;",null);
			mapStringHtml.put("&iacute;",null);
			mapStringHtml.put("&icirc;",null);
			mapStringHtml.put("&iuml;",null);
			mapStringHtml.put("&eth;",null);
			mapStringHtml.put("&ntilde;",null);
			mapStringHtml.put("&ograve;",null);
			mapStringHtml.put("&oacute;",null);
			mapStringHtml.put("&ocirc;",null);
			mapStringHtml.put("&otilde;",null);
			mapStringHtml.put("&ouml;",null);
			mapStringHtml.put("&divide;",null);
			mapStringHtml.put("&oslash;",null);
			mapStringHtml.put("&ugrave;",null);
			mapStringHtml.put("&uacute;",null);
			mapStringHtml.put("&ucirc;",null);
			mapStringHtml.put("&uuml;",null);
			mapStringHtml.put("&yacute;",null);
			mapStringHtml.put("&thorn;",null);
			mapStringHtml.put("&yuml;",null);
			mapStringHtml.put("&OElig;",null);
			mapStringHtml.put("&oelig;",'?');
			mapStringHtml.put("&Scaron;",'?');
			mapStringHtml.put("&scaron;",null);
			mapStringHtml.put("&Yuml;",null);
			mapStringHtml.put("&fnof;",null);
			mapStringHtml.put("&circ;",null);
			mapStringHtml.put("&tilde;",null);
			mapStringHtml.put("&ndash;",null);
			mapStringHtml.put("&mdash;",null);


			mapStringHtml.put("&lsquo;",null);
			mapStringHtml.put("&rsquo;",null);
			mapStringHtml.put("&sbquo;",null);

			mapStringHtml.put("&ldquo;",null);
			mapStringHtml.put("&rdquo;",null);
			mapStringHtml.put("&bdquo;",null);
			mapStringHtml.put("&dagger;",null);
			mapStringHtml.put("&Dagger;",null);
			mapStringHtml.put("&bull;",null);
			mapStringHtml.put("&hellip;",null);
			




			mapStringHtml.put("&permil;",null);
			mapStringHtml.put("&lsaquo;",null);
			mapStringHtml.put("&rsaquo;",null);
			mapStringHtml.put("&euro;",null);
			mapStringHtml.put("&trade;",null);
			*/

			//string em dec
			mapStringHtml.put("&#32;",null);
			mapStringHtml.put("&#33;",'!');
			mapStringHtml.put("&#34;",'\"');
			mapStringHtml.put("&#35;",'#');
			mapStringHtml.put("&#36;",'$');
			mapStringHtml.put("&#37;",'%');
			mapStringHtml.put("&#38;",'&');
			mapStringHtml.put("&#39;",'\'');
			mapStringHtml.put("&#40;",'(');
			mapStringHtml.put("&#41;",')');
			mapStringHtml.put("&#42;",'*');
			mapStringHtml.put("&#43;",'+');
			mapStringHtml.put("&#44;",',');
			mapStringHtml.put("&#45;",'-');
			mapStringHtml.put("&#46;",'.');
			mapStringHtml.put("&#47;",'/');
			mapStringHtml.put("&#48;",'0');
			mapStringHtml.put("&#49;",'1');
			mapStringHtml.put("&#50;",'2');
			mapStringHtml.put("&#51;",'3');
			mapStringHtml.put("&#52;",'4');
			mapStringHtml.put("&#53;",'5');
			mapStringHtml.put("&#54;",'6');
			mapStringHtml.put("&#55;",'7');
			mapStringHtml.put("&#56;",'8');
			mapStringHtml.put("&#57;",'9');
			mapStringHtml.put("&#58;",':');
			mapStringHtml.put("&#59;",';');
			mapStringHtml.put("&#60;",'<');
			mapStringHtml.put("&#61;",'=');
			mapStringHtml.put("&#62;",'>');
			mapStringHtml.put("&#63;",'?');
			mapStringHtml.put("&#64;",'@');
			mapStringHtml.put("&#65;",'A');
			mapStringHtml.put("&#66;",'B');
			mapStringHtml.put("&#67;",'C');
			mapStringHtml.put("&#68;",'D');
			mapStringHtml.put("&#69;",'E');
			mapStringHtml.put("&#70;",'F');
			mapStringHtml.put("&#71;",'G');
			mapStringHtml.put("&#72;",'H');
			mapStringHtml.put("&#73;",'I');
			mapStringHtml.put("&#74;",'J');
			mapStringHtml.put("&#75;",'K');
			mapStringHtml.put("&#76;",'L');
			mapStringHtml.put("&#77;",'M');
			mapStringHtml.put("&#78;",'N');
			mapStringHtml.put("&#79;",'O');
			mapStringHtml.put("&#80;",'P');
			mapStringHtml.put("&#81;",'Q');
			mapStringHtml.put("&#82;",'R');
			mapStringHtml.put("&#83;",'S');
			mapStringHtml.put("&#84;",'T');
			mapStringHtml.put("&#85;",'U');
			mapStringHtml.put("&#86;",'V');
			mapStringHtml.put("&#87;",'W');
			mapStringHtml.put("&#88;",'X');
			mapStringHtml.put("&#89;",'Y');
			mapStringHtml.put("&#90;",'Z');
			mapStringHtml.put("&#91;",'[');
			mapStringHtml.put("&#92;",'\\');
			mapStringHtml.put("&#93;",']');
			mapStringHtml.put("&#94;",'^');
			mapStringHtml.put("&#95;",'_');
			mapStringHtml.put("&#96;",'`');
			mapStringHtml.put("&#97;",'a');
			mapStringHtml.put("&#98;",'b');
			mapStringHtml.put("&#99;",'c');
			mapStringHtml.put("&#100;",'d');
			mapStringHtml.put("&#101;",'e');
			mapStringHtml.put("&#102;",'f');
			mapStringHtml.put("&#103;",'g');
			mapStringHtml.put("&#104;",'h');
			mapStringHtml.put("&#105;",'i');
			mapStringHtml.put("&#106;",'j');
			mapStringHtml.put("&#107;",'k');
			mapStringHtml.put("&#108;",'l');
			mapStringHtml.put("&#109;",'m');
			mapStringHtml.put("&#110;",'n');
			mapStringHtml.put("&#111;",'o');
			mapStringHtml.put("&#112;",'p');
			mapStringHtml.put("&#113;",'q');
			mapStringHtml.put("&#114;",'r');
			mapStringHtml.put("&#115;",'s');
			mapStringHtml.put("&#116;",'t');
			mapStringHtml.put("&#117;",'u');
			mapStringHtml.put("&#118;",'v');
			mapStringHtml.put("&#119;",'w');
			mapStringHtml.put("&#120;",'x');
			mapStringHtml.put("&#121;",'y');
			mapStringHtml.put("&#122;",'z');
			mapStringHtml.put("&#123;",'{');
			mapStringHtml.put("&#124;",'|');
			mapStringHtml.put("&#125;",'}');
			mapStringHtml.put("&#126;",'~');
			mapStringHtml.put("&#127;",'');
			mapStringHtml.put("&#128;",null);
			mapStringHtml.put("&#129;",'?');
			mapStringHtml.put("&#130;",null);
			mapStringHtml.put("&#131;",null);
			mapStringHtml.put("&#132;",null);
			mapStringHtml.put("&#133;",null);
			mapStringHtml.put("&#134;",null);
			mapStringHtml.put("&#135;",null);
			mapStringHtml.put("&#136;",null);
			mapStringHtml.put("&#137;",null);
			mapStringHtml.put("&#138;",null);
			mapStringHtml.put("&#139;",null);
			mapStringHtml.put("&#140;",null);
			mapStringHtml.put("&#141;",'?');
			mapStringHtml.put("&#142;",null);
			mapStringHtml.put("&#143;",'?');
			mapStringHtml.put("&#144;",'?');
			mapStringHtml.put("&#145;",null);
			mapStringHtml.put("&#146;",null);
			mapStringHtml.put("&#147;",null);
			mapStringHtml.put("&#148;",null);
			mapStringHtml.put("&#149;",null);
			mapStringHtml.put("&#150;",null);
			mapStringHtml.put("&#151;",null);
			mapStringHtml.put("&#152;",null);
			mapStringHtml.put("&#153;",null);
			mapStringHtml.put("&#154;",null);
			mapStringHtml.put("&#155;",null);
			mapStringHtml.put("&#156;",null);
			mapStringHtml.put("&#157;",'?');
			mapStringHtml.put("&#158;",null);
			mapStringHtml.put("&#159;",null);
			mapStringHtml.put("&#160;",null);
			mapStringHtml.put("&#161;",null);
			mapStringHtml.put("&#162;",null);
			mapStringHtml.put("&#163;",null);
			mapStringHtml.put("&#164;",null);
			mapStringHtml.put("&#165;",null);
			mapStringHtml.put("&#166;",null);
			mapStringHtml.put("&#167;",null);
			mapStringHtml.put("&#168;",null);
			mapStringHtml.put("&#169;",null);
			mapStringHtml.put("&#170;",null);
			mapStringHtml.put("&#171;",null);
			mapStringHtml.put("&#172;",null);
			mapStringHtml.put("&#173;",null);
			mapStringHtml.put("&#174;",null);
			mapStringHtml.put("&#175;",null);
			mapStringHtml.put("&#176;",null);
			mapStringHtml.put("&#177;",null);
			mapStringHtml.put("&#178;",null);
			mapStringHtml.put("&#179;",null);
			mapStringHtml.put("&#180;",null);
			mapStringHtml.put("&#181;",null);
			mapStringHtml.put("&#182;",null);
			mapStringHtml.put("&#183;",null);
			mapStringHtml.put("&#184;",null);
			mapStringHtml.put("&#185;",null);
			mapStringHtml.put("&#186;",null);
			mapStringHtml.put("&#187;",null);
			mapStringHtml.put("&#188;",null);
			mapStringHtml.put("&#189;",null);
			mapStringHtml.put("&#190;",null);
			mapStringHtml.put("&#191;",null);
			mapStringHtml.put("&#192;",null);
			mapStringHtml.put("&#193;",null);
			mapStringHtml.put("&#194;",null);
			mapStringHtml.put("&#195;",null);
			mapStringHtml.put("&#196;",null);
			mapStringHtml.put("&#197;",null);
			mapStringHtml.put("&#198;",null);
			mapStringHtml.put("&#199;",null);
			mapStringHtml.put("&#200;",null);
			mapStringHtml.put("&#201;",null);
			mapStringHtml.put("&#202;",null);
			mapStringHtml.put("&#203;",null);
			mapStringHtml.put("&#204;",null);
			mapStringHtml.put("&#205;",null);
			mapStringHtml.put("&#206;",null);
			mapStringHtml.put("&#207;",null);
			mapStringHtml.put("&#208;",null);
			mapStringHtml.put("&#209;",null);
			mapStringHtml.put("&#210;",null);
			mapStringHtml.put("&#211;",null);
			mapStringHtml.put("&#212;",null);
			mapStringHtml.put("&#213;",null);
			mapStringHtml.put("&#214;",null);
			mapStringHtml.put("&#215;",null);
			mapStringHtml.put("&#216;",null);
			mapStringHtml.put("&#217;",null);
			mapStringHtml.put("&#218;",null);
			mapStringHtml.put("&#219;",null);
			mapStringHtml.put("&#220;",null);
			mapStringHtml.put("&#221;",null);
			mapStringHtml.put("&#222;",null);
			mapStringHtml.put("&#223;",null);
			mapStringHtml.put("&#224;",null);
			mapStringHtml.put("&#225;",null);
			mapStringHtml.put("&#226;",null);
			mapStringHtml.put("&#227;",null);
			mapStringHtml.put("&#228;",null);
			mapStringHtml.put("&#229;",null);
			mapStringHtml.put("&#230;",null);
			mapStringHtml.put("&#231;",null);
			mapStringHtml.put("&#232;",null);
			mapStringHtml.put("&#233;",null);
			mapStringHtml.put("&#234;",null);
			mapStringHtml.put("&#235;",null);
			mapStringHtml.put("&#236;",null);
			mapStringHtml.put("&#237;",null);
			mapStringHtml.put("&#238;",null);
			mapStringHtml.put("&#239;",null);
			mapStringHtml.put("&#240;",null);
			mapStringHtml.put("&#241;",null);
			mapStringHtml.put("&#242;",null);
			mapStringHtml.put("&#243;",null);
			mapStringHtml.put("&#244;",null);
			mapStringHtml.put("&#245;",null);
			mapStringHtml.put("&#246;",null);
			mapStringHtml.put("&#247;",null);
			mapStringHtml.put("&#248;",null);
			mapStringHtml.put("&#249;",null);
			mapStringHtml.put("&#250;",null);
			mapStringHtml.put("&#251;",null);
			mapStringHtml.put("&#252;",null);
			mapStringHtml.put("&#253;",null);
			mapStringHtml.put("&#254;",null);
			mapStringHtml.put("&#255;",null);
			mapStringHtml.put("&#338;",null);
			mapStringHtml.put("&#339;",'?');

			mapStringHtml.put("&#352;",'?');
			mapStringHtml.put("&#353;",null);
			mapStringHtml.put("&#376;",null);
			mapStringHtml.put("&#381;",null);
			mapStringHtml.put("&#382;",null);
			mapStringHtml.put("&#383;",'?');
			mapStringHtml.put("&#399;",'?');
			mapStringHtml.put("&#402;",null);
			mapStringHtml.put("&#710;",null);
			mapStringHtml.put("&#1632;",'1');
			mapStringHtml.put("&#1633;",'1');
			mapStringHtml.put("&#1634;",'2');
			mapStringHtml.put("&#1635;",'3');
			mapStringHtml.put("&#1636;",'4');
			mapStringHtml.put("&#1637;",'5');
			mapStringHtml.put("&#1638;",'6');
			mapStringHtml.put("&#1639;",'7');
			mapStringHtml.put("&#1640;",'8');
			mapStringHtml.put("&#1641;",'9');
			mapStringHtml.put("&#8211;",null);
			mapStringHtml.put("&#8212;",null);
			mapStringHtml.put("&#8213;",'?');
			mapStringHtml.put("&#8215;",'?');
			mapStringHtml.put("&#8216;",null);
			mapStringHtml.put("&#8217;",null);
			mapStringHtml.put("&#8218;",null);
			mapStringHtml.put("&#8219;",'?');
			mapStringHtml.put("&#8220;",null);
			mapStringHtml.put("&#8221;",null);
			mapStringHtml.put("&#8222;",null);
			mapStringHtml.put("&#8224;",null);
			mapStringHtml.put("&#8225;",null);
			mapStringHtml.put("&#8226;",null);
			mapStringHtml.put("&#8230;",null);
			mapStringHtml.put("&#8238;",'?');
			mapStringHtml.put("&#8240;",null);
			mapStringHtml.put("&#8249;",null);
			mapStringHtml.put("&#8250;",null);

			mapStringHtml.put("&#8364;",null);

			mapStringHtml.put("&#8482;",null);


			//string em hexa
			mapStringHtml.put("&#x20;",null);
			mapStringHtml.put("&#x21;",'!');
			mapStringHtml.put("&#x22;",'\"');
			mapStringHtml.put("&#x23;",'#');
			mapStringHtml.put("&#x24;",'$');
			mapStringHtml.put("&#x25;",'%');
			mapStringHtml.put("&#x26;",'&');
			mapStringHtml.put("&#x27;",null);
			mapStringHtml.put("&#x28;",'(');
			mapStringHtml.put("&#x29;",')');
			mapStringHtml.put("&#x2A;",'*');
			mapStringHtml.put("&#x2B;",'+');
			mapStringHtml.put("&#x2C;",',');
			mapStringHtml.put("&#x2D;",'-');
			mapStringHtml.put("&#x2E;",'.');
			mapStringHtml.put("&#x2F;",'/');
			mapStringHtml.put("&#x30;",'0');
			mapStringHtml.put("&#x31;",'1');
			mapStringHtml.put("&#x32;",'2');
			mapStringHtml.put("&#x33;",'3');
			mapStringHtml.put("&#x34;",'4');
			mapStringHtml.put("&#x35;",'5');
			mapStringHtml.put("&#x36;",'6');
			mapStringHtml.put("&#x37;",'7');
			mapStringHtml.put("&#x38;",'8');
			mapStringHtml.put("&#x39;",'9');
			mapStringHtml.put("&#x3A;",':');
			mapStringHtml.put("&#x3B;",';');
			mapStringHtml.put("&#x3C;",'<');
			mapStringHtml.put("&#x3D;",'=');
			mapStringHtml.put("&#x3E;",'>');
			mapStringHtml.put("&#x3F;",'?');
			mapStringHtml.put("&#x40;",'@');
			mapStringHtml.put("&#x41;",'A');
			mapStringHtml.put("&#x42;",'B');
			mapStringHtml.put("&#x43;",'C');
			mapStringHtml.put("&#x44;",'D');
			mapStringHtml.put("&#x45;",'E');
			mapStringHtml.put("&#x46;",'F');
			mapStringHtml.put("&#x47;",'G');
			mapStringHtml.put("&#x48;",'H');
			mapStringHtml.put("&#x49;",'I');
			mapStringHtml.put("&#x4A;",'J');
			mapStringHtml.put("&#x4B;",'K');
			mapStringHtml.put("&#x4C;",'L');
			mapStringHtml.put("&#x4D;",'M');
			mapStringHtml.put("&#x4E;",'N');
			mapStringHtml.put("&#x4F;",'O');
			mapStringHtml.put("&#x50;",'P');
			mapStringHtml.put("&#x51;",'Q');
			mapStringHtml.put("&#x52;",'R');
			mapStringHtml.put("&#x53;",'S');
			mapStringHtml.put("&#x54;",'T');
			mapStringHtml.put("&#x55;",'U');
			mapStringHtml.put("&#x56;",'V');
			mapStringHtml.put("&#x57;",'W');
			mapStringHtml.put("&#x58;",'X');
			mapStringHtml.put("&#x59;",'Y');
			mapStringHtml.put("&#x5A;",'Z');
			mapStringHtml.put("&#x5B;",'[');
			mapStringHtml.put("&#x5C;",'\\');
			mapStringHtml.put("&#x5D;",']');
			mapStringHtml.put("&#x5E;",'^');
			mapStringHtml.put("&#x5F;",'_');
			mapStringHtml.put("&#x60;",'`');
			mapStringHtml.put("&#x61;",'a');
			mapStringHtml.put("&#x62;",'b');
			mapStringHtml.put("&#x63;",'c');
			mapStringHtml.put("&#x64;",'d');
			mapStringHtml.put("&#x65;",'e');
			mapStringHtml.put("&#x66;",'f');
			mapStringHtml.put("&#x67;",'g');
			mapStringHtml.put("&#x68;",'h');
			mapStringHtml.put("&#x69;",'i');
			mapStringHtml.put("&#x6A;",'j');
			mapStringHtml.put("&#x6B;",'k');
			mapStringHtml.put("&#x6C;",'l');
			mapStringHtml.put("&#x6D;",'m');
			mapStringHtml.put("&#x6E;",'n');
			mapStringHtml.put("&#x6F;",'o');
			mapStringHtml.put("&#x70;",'p');
			mapStringHtml.put("&#x71;",'q');
			mapStringHtml.put("&#x72;",'r');
			mapStringHtml.put("&#x73;",'s');
			mapStringHtml.put("&#x74;",'t');
			mapStringHtml.put("&#x75;",'u');
			mapStringHtml.put("&#x76;",'v');
			mapStringHtml.put("&#x77;",'w');
			mapStringHtml.put("&#x78;",'x');
			mapStringHtml.put("&#x79;",'y');
			mapStringHtml.put("&#x7A;",'z');
			mapStringHtml.put("&#x7B;",'{');
			mapStringHtml.put("&#x7C;",'|');
			mapStringHtml.put("&#x7D;",'}');
			mapStringHtml.put("&#x7E;",'~');
			mapStringHtml.put("&#x7F;",'');
			mapStringHtml.put("&#x80;",null);
			mapStringHtml.put("&#x81;",'?');
			mapStringHtml.put("&#x82;",null);
			mapStringHtml.put("&#x83;",null);
			mapStringHtml.put("&#x84;",null);
			mapStringHtml.put("&#x85;",null);
			mapStringHtml.put("&#x86;",null);
			mapStringHtml.put("&#x87;",null);
			mapStringHtml.put("&#x88;",null);
			mapStringHtml.put("&#x89;",null);
			mapStringHtml.put("&#x8A;",null);
			mapStringHtml.put("&#x8B;",null);
			mapStringHtml.put("&#x8C;",null);
			mapStringHtml.put("&#x8D;",'?');
			mapStringHtml.put("&#x8E;",null);
			mapStringHtml.put("&#x8F;",'?');
			mapStringHtml.put("&#x90;",'?');
			mapStringHtml.put("&#x91;",null);
			mapStringHtml.put("&#x92;",null);
			mapStringHtml.put("&#x93;",null);
			mapStringHtml.put("&#x94;",null);
			mapStringHtml.put("&#x95;",null);
			mapStringHtml.put("&#x96;",null);
			mapStringHtml.put("&#x97;",null);
			mapStringHtml.put("&#x98;",null);
			mapStringHtml.put("&#x99;",null);
			mapStringHtml.put("&#x9A;",null);
			mapStringHtml.put("&#x9B;",null);
			mapStringHtml.put("&#x9C;",null);
			mapStringHtml.put("&#x9D;",'?');
			mapStringHtml.put("&#x9E;",null);
			mapStringHtml.put("&#x9F;",null);
			mapStringHtml.put("&#xA0;",null);
			mapStringHtml.put("&#xA1;",null);
			mapStringHtml.put("&#xA2;",null);
			mapStringHtml.put("&#xA3;",null);
			mapStringHtml.put("&#xA4;",null);
			mapStringHtml.put("&#xA5;",null);
			mapStringHtml.put("&#xA6;",null);
			mapStringHtml.put("&#xA7;",null);
			mapStringHtml.put("&#xA8;",null);
			mapStringHtml.put("&#xA9;",null);
			mapStringHtml.put("&#xAA;",null);
			mapStringHtml.put("&#xAB;",null);
			mapStringHtml.put("&#xAC;",null);
			mapStringHtml.put("&#xAD;",null);
			mapStringHtml.put("&#xAE;",null);
			mapStringHtml.put("&#xAF;",null);
			mapStringHtml.put("&#xB0;",null);
			mapStringHtml.put("&#xB1;",null);
			mapStringHtml.put("&#xB2;",null);
			mapStringHtml.put("&#xB3;",null);
			mapStringHtml.put("&#xB4;",null);
			mapStringHtml.put("&#xB5;",null);
			mapStringHtml.put("&#xB6;",null);
			mapStringHtml.put("&#xB7;",null);
			mapStringHtml.put("&#xB8;",null);
			mapStringHtml.put("&#xB9;",null);
			mapStringHtml.put("&#xBA;",null);
			mapStringHtml.put("&#xBB;",null);
			mapStringHtml.put("&#xBC;",null);
			mapStringHtml.put("&#xBD;",null);
			mapStringHtml.put("&#xBE;",null);
			mapStringHtml.put("&#xBF;",null);
			mapStringHtml.put("&#xC0;",null);
			mapStringHtml.put("&#xC1;",null);
			mapStringHtml.put("&#xC2;",null);
			mapStringHtml.put("&#xC3;",null);
			mapStringHtml.put("&#xC4;",null);
			mapStringHtml.put("&#xC5;",null);
			mapStringHtml.put("&#xC6;",null);
			mapStringHtml.put("&#xC7;",null);
			mapStringHtml.put("&#xC8;",null);
			mapStringHtml.put("&#xC9;",null);
			mapStringHtml.put("&#xCA;",null);
			mapStringHtml.put("&#xCB;",null);
			mapStringHtml.put("&#xCC;",null);
			mapStringHtml.put("&#xCD;",null);
			mapStringHtml.put("&#xCE;",null);
			mapStringHtml.put("&#xCF;",null);
			mapStringHtml.put("&#xD0;",null);
			mapStringHtml.put("&#xD1;",null);
			mapStringHtml.put("&#xD2;",null);
			mapStringHtml.put("&#xD3;",null);
			mapStringHtml.put("&#xD4;",null);
			mapStringHtml.put("&#xD5;",null);
			mapStringHtml.put("&#xD6;",null);
			mapStringHtml.put("&#xD7;",null);
			mapStringHtml.put("&#xD8;",null);
			mapStringHtml.put("&#xD9;",null);
			mapStringHtml.put("&#xDA;",null);
			mapStringHtml.put("&#xDB;",null);
			mapStringHtml.put("&#xDC;",null);
			mapStringHtml.put("&#xDD;",null);
			mapStringHtml.put("&#xDE;",null);
			mapStringHtml.put("&#xDF;",null);
			mapStringHtml.put("&#xE0;",null);
			mapStringHtml.put("&#xE1;",null);
			mapStringHtml.put("&#xE2;",null);
			mapStringHtml.put("&#xE3;",null);
			mapStringHtml.put("&#xE4;",null);
			mapStringHtml.put("&#xE5;",null);
			mapStringHtml.put("&#xE6;",null);
			mapStringHtml.put("&#xE7;",null);
			mapStringHtml.put("&#xE8;",null);
			mapStringHtml.put("&#xE9;",null);
			mapStringHtml.put("&#xEA;",null);
			mapStringHtml.put("&#xEB;",null);
			mapStringHtml.put("&#xEC;",null);
			mapStringHtml.put("&#xED;",null);
			mapStringHtml.put("&#xEE;",null);
			mapStringHtml.put("&#xEF;",null);
			mapStringHtml.put("&#xF0;",null);
			mapStringHtml.put("&#xF1;",null);
			mapStringHtml.put("&#xF2;",null);
			mapStringHtml.put("&#xF3;",null);
			mapStringHtml.put("&#xF4;",null);
			mapStringHtml.put("&#xF5;",null);
			mapStringHtml.put("&#xF6;",null);
			mapStringHtml.put("&#xF7;",null);
			mapStringHtml.put("&#xF8;",null);
			mapStringHtml.put("&#xF9;",null);
			mapStringHtml.put("&#xFA;",null);
			mapStringHtml.put("&#xFB;",null);
			mapStringHtml.put("&#xFC;",null);
			mapStringHtml.put("&#xFD;",null);
			mapStringHtml.put("&#xFE;",null);
			mapStringHtml.put("&#xFF;",null);
			mapStringHtml.put("&#x152;",null);
			mapStringHtml.put("&#x153;",'?');
			mapStringHtml.put("&#x160;",'?');
			mapStringHtml.put("&#x161;",null);
		
			mapStringHtml.put("&#x178;",null);
			mapStringHtml.put("&#x17D;",null);
			mapStringHtml.put("&#x17E;",null);
			mapStringHtml.put("&#x192;",null);
			mapStringHtml.put("&#x2C6;",null);
			mapStringHtml.put("&#x660;",'1');
			mapStringHtml.put("&#x661;",'1');
			mapStringHtml.put("&#x662;",'2');
			mapStringHtml.put("&#x663;",'3');
			mapStringHtml.put("&#x664;",'4');
			mapStringHtml.put("&#x665;",'5');
			mapStringHtml.put("&#x666;",'6');
			mapStringHtml.put("&#x667;",'7');
			mapStringHtml.put("&#x668;",'8');
			mapStringHtml.put("&#x669;",'9');
			
			mapStringHtml.put("&#x2013;",null);
			mapStringHtml.put("&#x2014;",null);
			
			mapStringHtml.put("&#x2018;",null);
			mapStringHtml.put("&#x2019;",null);
			mapStringHtml.put("&#x201A;",null);
			mapStringHtml.put("&#x201B;",'?');
			mapStringHtml.put("&#x201C;",null);
			mapStringHtml.put("&#x201D;",null);
			mapStringHtml.put("&#x201E;",null);
			mapStringHtml.put("&#x2020;",null);
			mapStringHtml.put("&#x2021;",null);
			mapStringHtml.put("&#x2022;",null);
			mapStringHtml.put("&#x2026;",null);
			
			mapStringHtml.put("&#x2030;",null);
			mapStringHtml.put("&#x2032;",'?');
			mapStringHtml.put("&#x2033;",'?');
			mapStringHtml.put("&#x2039;",null);
			mapStringHtml.put("&#x203A;",null);
			
			mapStringHtml.put("&#x20AC;",null);
			
			mapStringHtml.put("&#x2122;",null);
			
			
	}
	public static String replaceHtmlEntitiesToChar(String text,HashMap<String,Character> mapStringToChar)
	{
		char[] arrText = text.toCharArray();
		Character[] arrObjText = new Character[arrText.length];
		for(int j =0; j<arrText.length ; j++)
		{
			arrObjText[j] = arrText[j];
		}
		
		//acha html entities e substitui elas por nulo no char array
		int idxIniHtmlEnt = text.indexOf("&");
		int idxFimHtmlEnt = text.indexOf(";");
		while(idxIniHtmlEnt >=0 && idxFimHtmlEnt >= 0)
		{
			if(idxIniHtmlEnt < idxFimHtmlEnt)
			{
				String htmlEnty = text.substring(idxIniHtmlEnt,idxFimHtmlEnt+1);
				//acha htmlentity
				if(mapStringToChar.containsKey(htmlEnty))
				{
					
					Character c = mapStringToChar.get(htmlEnty);
					arrObjText[idxIniHtmlEnt] = c;
					for(int i = idxIniHtmlEnt+1 ; i<= idxFimHtmlEnt ; i++)
					{
						arrObjText[i] = null;	
					}
				}
				
				idxIniHtmlEnt = text.indexOf('&',idxIniHtmlEnt+1);
				idxFimHtmlEnt = text.indexOf(';',idxFimHtmlEnt+1);
			}else
			{
				//achou um ; antes do &, achar um depois
				idxFimHtmlEnt = text.indexOf(';',idxFimHtmlEnt+1);
			}
		}
		
		//onstroi string 
		StringBuilder strBuilder = new StringBuilder();
		for(int j =0; j<arrText.length ; j++)
		{
			if(arrObjText[j] != null)
			{
				strBuilder.append(arrObjText[j]);
			}
			
		}
		
		return strBuilder.toString();
		
	}
	 public static String accentsHtmlToText(String input) {
		
		 return replaceHtmlEntitiesToChar(input, mapStringHtml);
		   		
		  }
	 
	 
public static String htmlToText(String strTexto)
	{ 
		
		
		
	return replaceHtmlEntitiesToChar(strTexto, mapStringHtml);

	}
public static String[] REPLACES = { "a", "e", "i", "o", "u"};  

public static Pattern[] PATTERNS_ACENTO = null;  
 
public static void compilePatterns() {  
    PATTERNS_ACENTO = new Pattern[REPLACES.length];  
    PATTERNS_ACENTO[0] = Pattern.compile("[âãáàä]", Pattern.CASE_INSENSITIVE);  
    PATTERNS_ACENTO[1] = Pattern.compile("[éèêë]", Pattern.CASE_INSENSITIVE);  
    PATTERNS_ACENTO[2] = Pattern.compile("[íìîï]", Pattern.CASE_INSENSITIVE);  
    PATTERNS_ACENTO[3] = Pattern.compile("[óòôõö]", Pattern.CASE_INSENSITIVE);  
    PATTERNS_ACENTO[4] = Pattern.compile("[úùûü]", Pattern.CASE_INSENSITIVE);  
    
}  

public static String replaceAcento(String text) {  
    if(PATTERNS_ACENTO == null){  
        compilePatterns();  
    }  

    String result = text;  
    for (int i = 0; i < PATTERNS_ACENTO.length; i++) {  
        Matcher matcher = PATTERNS_ACENTO[i].matcher(result);  
        result = matcher.replaceAll(REPLACES[i]);  
    }  
    return result;  
}  
public static String encodeXML(String txt)
{
    return txt.replaceAll("&","&amp;");
}
 
public static void main(String[] args)
{
	/*
	System.out.println(retiraStopWords("What is the fastest, yet secure way to encrypt passwords in (PHP Prefered), and for."));
	System.exit(0);
	List<String> lst = retiraBlocoTagInicial("<h1> oi </h1> lalala oi oi oi lalal <h1> xpto </h1> lalal xpto lalal xpto","<h1>","</h1>");
System.out.println(lst);
	*/
	
	String texto = "<p>ns1:UserName xmlns:ns1=\"urn:xxx:remtp:schemas:appxxxTypes:1.00\"&gt;xxxx&lt;/UserName&gt;</code> is it valid xml?"+ 
			"If so, can we write this line using xml writer? I'm using VS2008. What confuses me is it starts with ns1 and end with <code>&lt;/username&gt;</code> thanks</p>";
	System.out.println(retiraBlocoTag(texto,"<code>","</code>"));
	
	Tupla<String,List<String>> tplBlocoTag = null;
	tplBlocoTag = retiraBlocoTag("oasoaosd oasidpo aispodi a" +
																		"<blocoTag> quarto " +
																			"<blocoTag> terceiro " +
																				"	<blocoTag> primeiro </blocoTag> " +
																				 "	<blocoTag> seg </blocoTag> asdijasdiojsaiodjasjiod " +
																		     "</blocoTag> fim do quarto " +
																		"</blocoTag> entre cod " +
																		"<blocoTag> quinto </blocoTag>","<blocoTag>","</blocoTag>");
	System.out.println(tplBlocoTag);

	System.out.println(retiraBlocoTag("<code> oasoaosp </code>","<code>","</code>"));
	
	
	
	
	
	System.out.println(stringEqualPrefix("asdas", "Ola Daniel"));
	
	System.exit(0);
	
	StringBuilder text = new StringBuilder("The m&#x40;rk Stewart W&Agrave;tson (born September 8, 1970 in Vancouver, British Columbia) is a professional soccer player who has earned the second most caps in the history of the Canadian national team. Watson currently plays for the Charleston Battery of the USL First Division. He joined the Battery in 2006, after his third stint with the Vancouver Whitecaps, having played 10 games for the 86ers in the summer of 1993 when he was named an APSL First Team All Star, and 9 games in 1994.");
	
	
	
	//System.out.println("NUM:"+countOccorencias("tigre tres pratos de tigres para tres tigres tristes","tigre"));
	
	//System.exit(0);
	/*
	for(int i =0 ; i<15 ; i++)
	{
		text.append(" "+text.toString()+" ");
	}
	*/
	String strText = text.toString();
	System.out.println("Texto1: "+htmlToText(strText));
	System.out.println("Texto2: "+ accentsHtmlToText(strText));
	
	System.out.print("Stemming: ");
	String[] words = stemmAndRemoveStopWords(text.toString());
	for(String word : words)
	{
		System.out.print(word+" ");
	}
	System.out.println();
	System.exit(0);
	//System.out.println(text);
	carregaStopWords();
	System.out.println("tamanho: "+text.length());
	 
	for(int i =0 ; i<2 ; i++)
	{
		Long time = System.currentTimeMillis();
		//strText = retiraStopWords(strText);
		strText = retiraStopWordsAndPonctuation(strText);
		System.out.println("tamanho: "+strText.length()+" tempo: "+(System.currentTimeMillis()-time)/1000.0+" %Men livre:"+Sys.getPorcentMemLivre());
		System.out.println("StopWords removed: "+strText);
	}
	strText = removePonctuation(strText);
	System.out.println("Removing pontuation: "+strText.length());
}
}
