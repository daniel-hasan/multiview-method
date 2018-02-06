/*
 * Created on 30/03/2008
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package string;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Daniel Hasan Dalip
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class PadraoString
{

	/**
	 *  
	 */
	private PadraoString()
	{
		super();
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args)
	{
		//System.out.println(resgataTodosPadroes("@[a-zA-Z]+","The mark @Stewart @Watson (born September 8, 1970 in @Vancouver, @British @Columbia) is a professional soccer player @who has earned the second most caps in the history of the Canadian national team. Watson currently plays for the Charleston Battery of the USL First Division. He joined the Battery in 2006, after his third stint with the Vancouver Whitecaps, having played 10 games for the 86ers in the summer of 1993 when he was named an APSL First Team All Star, and 9 games in 1994"));
		System.out.println(resgataTodosPadroes("JavaScript:abreDetalheGrupo\\('[0-9A-Za-z]+","<lalallalala>  asokdopkasopdopskad  opaskdopaskopk kop askopk JavaScript:abreDetalheGrupo('0333103JFD8BE0') iasjdioj asiodjioasjdio aiosdjjasd" ));
	}
	

	public static String resgataPadrao(String padrao,String text)
	{
		return resgataPadrao(padrao,text,true);
	}
	
	public static String resgataPadrao(String padrao,String text,boolean caseSensitive)
	{
		//procura as sessoes
		Pattern padraoRegExp;
		if(!caseSensitive)
		{
			padraoRegExp = Pattern.compile(padrao,Pattern.CASE_INSENSITIVE);
		}else
		{
			padraoRegExp = Pattern.compile(padrao);
		}
		
		Matcher matcher = padraoRegExp.matcher(text);
		int posInicial = 0;
		int posFinal = 0;
		
		boolean encontrou = matcher.find();
		if(encontrou)
		{
			posInicial = matcher.start();
			posFinal = matcher.end();
		
			return text.substring(posInicial,posFinal);
		}else
		{
			return "";
		}
	}
	public static List<TermoTexto> resgataTodosPadroes(String padrao,String text)
	{
		List<TermoTexto> blocos = new ArrayList<TermoTexto>();
		//procura as sessoes 
		Pattern padraoRegExp = Pattern.compile(padrao);
		Matcher matcher = padraoRegExp.matcher(text);
		int posInicial = 0;
		int posFinal = 0;
		
		boolean encontrou = matcher.find();

		if(encontrou)
		{
			//System.out.println("encontrou");
			posInicial = matcher.start();
			posFinal = matcher.end();
	
			while(encontrou)
			{			
				blocos.add(new TermoTexto(text.substring(posInicial,posFinal),posInicial));
				encontrou = matcher.find(posFinal);
				if(encontrou)
				{
					posInicial = matcher.start();
					posFinal = matcher.end();
				}
				
			}
		}
		return blocos;
	}
	public static ArrayList<String> resgataBlocosPadroes(String padrao,String text)
	{
		ArrayList<String> blocos = new ArrayList<String>();
		//procura as sessoes 
		Pattern padraoRegExp = Pattern.compile(padrao);
		Matcher matcher = padraoRegExp.matcher(text);
		int posInicial = 0;
		int posFinal = 0;
		
		boolean encontrou = matcher.find();
		if(encontrou)
		{
			//System.out.println("encontrou");
			posInicial = 0;
			posFinal = matcher.start();
	
			while(encontrou)
			{			
				blocos.add(text.substring(posInicial,posFinal));
				encontrou = matcher.find();
				if(encontrou)
				{
					posInicial = posFinal;
					posFinal = matcher.start();
				}else
				{
					if(posFinal!=text.length())
					{
						posInicial = posFinal;
						posFinal = text.length();
						encontrou = true;
					}
				}
				
			}
		}else
		{
			blocos.add(text);
		}
		
		return blocos;
	}

}