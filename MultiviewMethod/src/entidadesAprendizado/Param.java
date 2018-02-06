package entidadesAprendizado;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;

import xml.XMLDomHelper;

public class Param implements Serializable
{
	private String name;
	private String defaultValue;
	private List<String> lstValuesVariation;
	
	public Param(String name, String defaultValue,List<String> valuesVariation)
	{
		super();
		this.name = name;
		this.defaultValue = defaultValue;
		this.lstValuesVariation = valuesVariation;
	}
	
	public List<String> getValuesVariation()
	{
		return lstValuesVariation;
		
	}
	
	public static float computeOp(int valueI,String strSign,String op)
	{
		int sign = 1;
		if(strSign != null && strSign.equalsIgnoreCase("-"))
		{
			sign = -1;
		}
		if(op.equals("i"))
		{
			return sign*valueI;
		}else
		{
			return sign*Float.parseFloat(op);
		}
	}
	
	public static double valueVariation(Node n,int value,String formula) throws Exception
	{
		//prepara expressoes regulares
		
		Pattern apenasUmOperando = Pattern.compile("(-|\\+)?([.0-9]+|i)");
		Matcher m = apenasUmOperando.matcher(formula);
		
		
		//operadores da esquerda é obrigadotiro

		
		if(m.matches())
		{
			return resgataOpEsquerda(value, m);
		}else
		{
			Pattern formPattOpCompleta = Pattern.compile("(-|\\+)?([.0-9]+|i)([-^+*/])(-|\\+)?([.0-9]+|i)");
			m = formPattOpCompleta.matcher(formula);
			//m = formPattOpCompleta.matcher(formula);
			if(m.matches())
			{
				//regata valores da regexp
				float leftValue = resgataOpEsquerda(value, m);
				
				String opMain = m.group(3);
				String signRight = m.group(4);
				String opRight = m.group(5);
				//valor dadireita
				float rightValue = computeOp(value,signRight,opRight);
				
				if(opMain.equals("-"))
				{
					return leftValue - rightValue;
				}
				if(opMain.equals("+"))
				{
					return leftValue + rightValue;	
				}
				if(opMain.equals("^"))
				{
					return Math.pow((double)leftValue, (double)rightValue);
				}
				if(opMain.equals("*"))
				{
					return leftValue * rightValue;
				}
				if(opMain.equals("/"))
				{
					return leftValue / rightValue;
				}
				return leftValue + rightValue;
			}else
			{
				throw new Exception("A formula '"+formula+"' do nodo "+n.getBaseURI()+ " é invalida ela deve ser do formato [+-][.0-9]+[-^+*/][+-][.0-9]+");
			}
		}
		
	}

	private static float resgataOpEsquerda(int value, Matcher m)
	{
		String signLeft = m.group(1);
		String opLeft = m.group(2);
		//pega o valor da esquerda
		float leftValue = computeOp(value,signLeft,opLeft);
		return leftValue;
	}
	public static List<String> getValuesVariation(Node variation) throws Exception
	{
		if(variation == null)
		{
			return new ArrayList<String>();
		}
		String strValuePos = XMLDomHelper.getNodeAttr("value_pos", variation,"i").replaceAll(" ", "");
		float step = Float.parseFloat(XMLDomHelper.getNodeAttr("step", variation,"1").replaceAll(" ", ""));
		String strVariation = XMLDomHelper.getNodeValue(variation);
		String[] arrPosPontoVirgula =  strVariation.replaceAll(" ", "").split(";");
		List<String> lstValues = new ArrayList<String>();
		
		
		for(String posVariated : arrPosPontoVirgula)
		{
			//verifica se tem um "--"
			String[] arrFromTo = posVariated.split("--");
			if(arrFromTo.length <= 2)
			{
				//caso tenha fazer o "from" to "to" 
				String strFrom = arrFromTo[0];Integer to = arrFromTo.length==2?Integer.parseInt(arrFromTo[1].trim()):null;
				
				Integer value = strFrom.matches("[+-]?[0-9]+")?Integer.parseInt(strFrom):null;
				boolean stepForward = true;
				int pos = 0;
				if(value!=null && to!= null && value > to)
				{
					stepForward = false;
				}
				do
				{
					
					//varificar sempre atraves do step se vai adicionar ou nao este valor
					if(pos%Math.abs(step)==0)
					{
						//	caso tenha que adicionar o valor, transforma atraves do strValuePos
						if(value != null)
						{
							double result = valueVariation(variation,value,strValuePos);
							
							lstValues.add(((int)result)==result?Integer.toString((int)result):Double.toString(result));
						}else
						{
							lstValues.add(strFrom);
						}
					}
					//incrementa posicao para cada from to to
					if(value != null)
						value+=stepForward?1:-1;
					pos++;
					if(to==null)
					{
						break;
					}
				}
				while((step >0 &&value<=to) || (step<0 && value>=to));
			

				

			}else
			{
					throw new Exception("O valor do nodo "+variation.getBaseURI()+" é invalido (o correto seria ex: 0;1;2;3-5;2");
			}

		}
		
		return lstValues;
	}
	
	public static List<Param> createParam(String params,Node cnfParams) throws Exception
	{
		HashMap<String,String> defaultValPerParam = new HashMap<String,String>();
		HashMap<String,Node> nodeParamPerName = new HashMap<String,Node>();
		if(cnfParams != null)
		{
			for(int i = 0; i<cnfParams.getChildNodes().getLength() ; i++)
			{
				Node nodeParamCnf = cnfParams.getChildNodes().item(i);
				if(nodeParamCnf.getNodeType() == Node.ELEMENT_NODE && nodeParamCnf.getLocalName().equalsIgnoreCase("param"))
				{
					String paramName = XMLDomHelper.getNodeValue("name", nodeParamCnf.getChildNodes());
					String defaultVal = XMLDomHelper.getNodeValue("default", nodeParamCnf.getChildNodes());
					defaultValPerParam.put(paramName, defaultVal);
					nodeParamPerName.put(paramName, nodeParamCnf);
				}
				
			}
		}
		
		List<Param> lstParam = new ArrayList<Param>();
		for(String param : params.split(" "))
		{
			String strParam = param.replaceAll("\\{|\\}", "").trim();
			String defaultValue = defaultValPerParam.get(strParam);
			Node nodeParam = nodeParamPerName.get(strParam);
			if(cnfParams != null && nodeParam!=null)
			{

				lstParam.add(new Param(strParam,defaultValue,Param.getValuesVariation(XMLDomHelper.getNode("variation", nodeParam.getChildNodes()))));
			}else
			{
				lstParam.add(new Param(strParam,defaultValue,new ArrayList<String>()));
			}
		}
		return lstParam;
	}


	public String getName()
	{
		return name;
	}


	public String getDefaultValue()
	{
		return defaultValue;
	}
	
	public String toString()
	{
		return "Param: "+this.name+" DefaultVal: "+this.defaultValue+" variation: "+this.lstValuesVariation;
	}
}
