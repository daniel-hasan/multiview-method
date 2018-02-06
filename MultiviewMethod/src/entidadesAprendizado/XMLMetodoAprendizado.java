package entidadesAprendizado;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import stuctUtil.Tupla;
import xml.XMLDomHelper; 



public class XMLMetodoAprendizado
{
	private Map<String,CnfMetodoAprendizado> mapMetodoAprendizado = new HashMap<String, CnfMetodoAprendizado>();
	private Document docXML = null;
	public XMLMetodoAprendizado(File xmlFile) throws Exception
	{
	    DOMParser parser = new DOMParser();
	    parser.parse(xmlFile.getAbsolutePath());
	    docXML = parser.getDocument();
	    
	    //regata o nodo de metodos 
	    Node root = docXML.getFirstChild();
	    NodeList methods = root.getChildNodes();
	    for(int i =0; i<methods.getLength() ; i++) 
	    {
	    	Node method = methods.item(i);
	    	int type =method.getNodeType(); 
	    	if(method.getNodeType() == Node.ELEMENT_NODE && method.getLocalName().equalsIgnoreCase("method"))
	    	{
		    	//atributos do metodo
		    	String name = XMLDomHelper.getNodeValue(XMLDomHelper.getNode("name", method.getChildNodes()));
		    	Tupla<String,List<Param>> treinoCnf = getScriptAndParams(XMLDomHelper.getNode("train", method.getChildNodes()));
		    	Tupla<String,List<Param>> testeCnf = getScriptAndParams(XMLDomHelper.getNode("test", method.getChildNodes()));
		    	
		    	mapMetodoAprendizado.put(name, new CnfMetodoAprendizado(name, treinoCnf.getY(), testeCnf.getY(),treinoCnf.getX(), testeCnf.getX()));
	    	}
	    	
	    	
	    }
	}
	private Tupla<String,List<Param>> getScriptAndParams(Node n) throws Exception
	{
		if(n == null)
		{
			return new Tupla<String,List<Param>>(null,null);
		}
		String params = XMLDomHelper.getNodeValue(XMLDomHelper.getNode("params", n.getChildNodes()));
		
    	Node cnfParams = XMLDomHelper.getNode("param_cnf",n.getChildNodes());
    	String script = XMLDomHelper.getNodeValue(XMLDomHelper.getNode("script",n.getChildNodes()));
    	
    	return new Tupla<String,List<Param>>(script,Param.createParam(params, cnfParams));
	}
	
	public CnfMetodoAprendizado getCNFMetodo(String nome)
	{
		return mapMetodoAprendizado.get(nome);
	}
	
	public static void main(String[] args) throws Exception
	{
		 XMLMetodoAprendizado xml = new XMLMetodoAprendizado(new File("/home//hasan/Dropbox/projetos_eclipse/Util/metodo_aprendizado.xml"));
		 System.out.println(xml.getCNFMetodo("ADTree"));
		 //System.out.println(xml.getCNFMetodo("MaxEnt"));
	}
	
}
