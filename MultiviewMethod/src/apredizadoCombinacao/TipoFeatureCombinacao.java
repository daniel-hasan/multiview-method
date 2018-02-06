package apredizadoCombinacao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import entidadesAprendizado.View;
import entidadesAprendizado.View.FeatureType;

public class TipoFeatureCombinacao implements Comparable<TipoFeatureCombinacao>{
	
	
	private Set<FeatureType> tipoFeatures;
	private static final boolean DEBUG = false;
	public TipoFeatureCombinacao(Set<FeatureType> lstTipoFeatures)
	{
		
		this.tipoFeatures = new TreeSet<View.FeatureType>();
		for (FeatureType featureType : lstTipoFeatures) {
			tipoFeatures.add(featureType);
		}
	}
	public TipoFeatureCombinacao(FeatureType ... lstTipoFeatures)
	{
	
		this.tipoFeatures = new TreeSet<View.FeatureType>();
		for (FeatureType featureType : lstTipoFeatures) {
			tipoFeatures.add(featureType);
		}
	}
	
	public Set<FeatureType> getTipoFeatures()
	{
		return tipoFeatures;
	}
	
	public void addFeatureType(FeatureType objFeat)
	{
		this.tipoFeatures.add(objFeat);
	}
	public String getName()
	{

		//separa as globais das locals
		Set<FeatureType> lstGlobal = new TreeSet<View.FeatureType>();
		Set<FeatureType> lstLocal = new TreeSet<View.FeatureType>();
		
		for(FeatureType objType : tipoFeatures)
		{
			if(objType.isLocal())
			{
				lstLocal.add(objType);
			}else
			{
				lstGlobal.add(objType);
			}
		}
		
		String nome = "";
		if(lstGlobal.size() > 0)
		{
			nome = getName(  lstGlobal);
		}
		if(lstLocal.size() > 0)
		{
			nome += "_L"+getName(lstLocal);
		}
		
		return nome;
	}
	public int getNumViewsLocal()
	{
		int num = 0;
		for(FeatureType objType : tipoFeatures)
		{
			if(objType.isLocal())
			{
				num++;
			}
		}
		return num++;
	}
	public int getNumViewsGlobal()
	{
		int num = 0;
		for(FeatureType objType : tipoFeatures)
		{
			if(!objType.isLocal())
			{
				num++;
			}
		}
		return num++;
	}
	
	/**
	 * 
	 * Resgata o nome do conjunto
	 */
	private String getName( Set<FeatureType> lstTipoFeatures)
	{

				
		//caso seja 5, colocar apenas exceto e a que estiver faltando
		if(lstTipoFeatures.size() == 5)
		{
			Set<FeatureType> objFeatMissing = getMissingFeatureType(lstTipoFeatures, false);
				
			return "allExcept"+objFeatMissing.iterator().next().getName(); 
		}
		String nome = "";
		if(lstTipoFeatures.contains(FeatureType.STRUCTURE) && 
				lstTipoFeatures.contains(FeatureType.STYLE) && 
				lstTipoFeatures.contains(FeatureType.LENGTH) && 
				lstTipoFeatures.contains(FeatureType.READ) && 
				lstTipoFeatures.contains(FeatureType.HISTORY) &&
				lstTipoFeatures.contains(FeatureType.NETWORK))
		{
			if(lstTipoFeatures.contains(FeatureType.LAC_STRUCTURE) && 
					lstTipoFeatures.contains(FeatureType.LAC_STYLE) && 
					lstTipoFeatures.contains(FeatureType.LAC_LENGTH) && 
					lstTipoFeatures.contains(FeatureType.LAC_READ) && 
					lstTipoFeatures.contains(FeatureType.LAC_HISTORY) &&
					lstTipoFeatures.contains(FeatureType.LAC_NETWORK))
			{
				return "all6SVM_LAC";
			}
			return "all6SVM";
		}
		if(lstTipoFeatures.contains(FeatureType.LAC_STRUCTURE) && 
				lstTipoFeatures.contains(FeatureType.LAC_STYLE) && 
				lstTipoFeatures.contains(FeatureType.LAC_LENGTH) && 
				lstTipoFeatures.contains(FeatureType.LAC_READ) && 
				lstTipoFeatures.contains(FeatureType.LAC_HISTORY) &&
				lstTipoFeatures.contains(FeatureType.LAC_NETWORK))
		{
			return "all6LAC";
		}
		
		
		//separa as de texto
		Set<FeatureType> textual = new TreeSet<FeatureType>();
		Set<FeatureType> nonTextual = new TreeSet<FeatureType>();
		
		for(FeatureType objType : lstTipoFeatures)
		{
			if(objType.isTextual())
			{
				textual.add(objType);
			}else
			{
				nonTextual.add(objType);
			}
		}
		
		
		

		
		
		//verifica o texto		
		
		//todas de texto
		if(textual.size() == 4)
		{
			nome += "Text";
		}else
		{
			/*
			if(textual.size() == 3)
			{
				Set<FeatureType> objFeatMissing = getMissingFeatureType(textual, true);
				nome += "TextExcept"+objFeatMissing.iterator().next().getName();			
			}else
			{
			*/
				if(textual.size()>0)
				{
					//caso seja uma ou duas textuais
					nome += "T";
					for(FeatureType objTextual : textual)
					{
						nome += objTextual.getName();
					}
				}
			//}
				
		}
		
		//nao-textuais
		for(FeatureType nonTextualFeat : nonTextual)
		{
			nome += nonTextualFeat.getName();
		}
		return nome;
		
	}
	public boolean isOnlyTextual()
	{
		for(FeatureType objType : tipoFeatures)
		{
			if(!objType.isTextual())
			{
				return false;
			}
		}
		return true;
	}
	public boolean isOnlyTextualLocal()
	{
		for(FeatureType objType : tipoFeatures)
		{
			if(objType.isLocal() && !objType.isTextual())
			{
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Resgata a feature que está faltando
	 * @param lstTipoFeatures
	 * @param onlyTextual
	 * @return
	 */
	public Set<FeatureType> getMissingFeatureType(Set<FeatureType> lstTipoFeatures, boolean onlyTextual)
	{
		FeatureType[] arrFeaturesType = {FeatureType.STRUCTURE,FeatureType.STYLE,FeatureType.LENGTH,FeatureType.READ,FeatureType.HISTORY,FeatureType.NETWORK};
		Set<FeatureType> lstMissing = new TreeSet<View.FeatureType>();
		for(FeatureType objType : arrFeaturesType)
		{
			boolean encontrou = false;
			for(FeatureType objTypeLista : lstTipoFeatures)
			{
				if(objTypeLista.toString().startsWith(objType.toString()))
				{
					encontrou = true;
					break;
				}
			}
			
			if(!encontrou)
			{
				lstMissing.add(objType);
			}
		}
		
		if(onlyTextual)
		{
			FeatureType[] arrFeatures = FeatureType.values();
			for(FeatureType objType : arrFeatures)
			{
				if(!objType.isTextual())
				{
					lstMissing.remove(objType);
				}
			}
		}
		return lstMissing;
	}
	
	public boolean contains(FeatureType objType)
	{
		return this.tipoFeatures.contains(objType);
	}
	
	
	public String toString()
	{
		if(!DEBUG)
		{
			return this.getName();
		}else
		{
			return this.getName()+" - \t "+this.tipoFeatures.toString();
		}
	}

	@Override
	public int compareTo(TipoFeatureCombinacao o) {
		// TODO Auto-generated method stub
		if(this.equals(o))
		{
			return 0;
		}
		int comp =  this.getName().compareTo(o.getName());
		if(comp == 0)
		{
			return -1;
		}
		return comp;
	}
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		if(obj instanceof TipoFeatureCombinacao)
		{
			
			TipoFeatureCombinacao objTipo = (TipoFeatureCombinacao) obj;
			if(this.tipoFeatures.size() != objTipo.getTipoFeatures().size())
			{
				return false;
			}
			
			//procura cada feture, se todas contiverem em objTipo, entao retorna true
			for(FeatureType objFeat : this.tipoFeatures)
			{
				if(!objTipo.getTipoFeatures().contains(objFeat))
				{
					return false;
				}
			}
			return true;
			
		}
		return false;
		
		
	}
	
	
	/**
	 * Retorna todas as combinacoes par a par desses tipos de features 
	 * @param tamPar
	 * @param arrFeatureTypes
	 * @return
	 */
	public static Set<TipoFeatureCombinacao> getCombinacao(int tamPar,FeatureType ... arrFeatureTypes)
	{
		Set<TipoFeatureCombinacao> lstTipoCombinacoes = new TreeSet<TipoFeatureCombinacao>();
		
		//combinacao 1 a 1
		for(FeatureType objFeat : arrFeatureTypes)
		{
			TreeSet<FeatureType> lstTipoCombinacaoNovo = new TreeSet<View.FeatureType>();
			lstTipoCombinacaoNovo.add(objFeat);
			lstTipoCombinacoes.add(new TipoFeatureCombinacao(lstTipoCombinacaoNovo));
		}
		
		//para maiores cobinacoes
		for(int i =1 ; i<tamPar ; i++)
		{
			if(DEBUG)
				System.out.println("---- Combinacao "+i+" a "+i);
			lstTipoCombinacoes = combinaTodos(lstTipoCombinacoes,arrFeatureTypes);
		}
		
		return lstTipoCombinacoes;
		
	}
	
	
	

	/**
	 * Combina todos os da lista com todas as features do array
	 * para cada combinacao, combina com todos em arrFeatures e retorna numa lista
	 * @param objCombina
	 * @param arrFeatureTypes
	 * @return
	 */
	public static Set<TipoFeatureCombinacao> combinaTodos(Set<TipoFeatureCombinacao> lstTipoCombina,FeatureType ... arrFeatureTypes)
	{
		Set<TipoFeatureCombinacao> lstCombinacaoFinal = new TreeSet<TipoFeatureCombinacao>();
		
		for(FeatureType objFeature : arrFeatureTypes)
		{
			if(DEBUG)
				System.out.println(objFeature);
			for(TipoFeatureCombinacao objTipoCombina : lstTipoCombina)
			{
				//cria todas as combinacoes que a feature q ainda nao existe no conjunto
				if(!objTipoCombina.contains(objFeature))
				{
					//adiciona a feature em questao no feature novo, resgatado baseado no tipo anterior
					TipoFeatureCombinacao objTipoNovo = new TipoFeatureCombinacao(objTipoCombina.getTipoFeatures());
					objTipoNovo.addFeatureType(objFeature);
					if(DEBUG)
					{
						if(!lstCombinacaoFinal.contains(objTipoNovo))
						{
							System.out.println("+NOVA COMB: "+objTipoNovo);
						}
						else
						{
							System.out.println("COMB EXISTENTE:"+objTipoNovo);	
						}
					}
					
					lstCombinacaoFinal.add(objTipoNovo);
					

				}
			}
		}
		return lstCombinacaoFinal;
	}

	public void setAsLocal()
	{
		List<FeatureType> remFeatures = new ArrayList<FeatureType>();
		List<FeatureType> addFeatures = new ArrayList<FeatureType>();
		for(FeatureType objFeature : tipoFeatures)
		{
			FeatureType featNew=null;
			switch(objFeature)
			{
				case TEXT:
						featNew = FeatureType.TEXT_LOCAL;

						break;
				case STRUCTURE:
						featNew = FeatureType.STRUCTURE_LOCAL;
						break;
				case STYLE:
						featNew = FeatureType.STYLE_LOCAL;
						break;
				case LENGTH:
						featNew = FeatureType.LENGTH_LOCAL;
						break;
				case READ:
						featNew = FeatureType.READ_LOCAL;
						break;
				case HISTORY:
						featNew = FeatureType.HISTORY_LOCAL;					
						break;
				case NETWORK:
						featNew = FeatureType.NETWORK_LOCAL;
						break;	
			}
			if(featNew != null)
			{
				addFeatures.add(featNew);
				remFeatures.add(objFeature);
			}
		}
		this.tipoFeatures.removeAll(remFeatures);
		this.tipoFeatures.addAll(addFeatures);
	}
	public void setAsGlobal()
	{
		List<FeatureType> remFeatures = new ArrayList<FeatureType>();
		List<FeatureType> addFeatures = new ArrayList<FeatureType>();
		for(FeatureType objFeature : tipoFeatures)
		{
			FeatureType featNew=null;
			switch(objFeature)
			{
				case TEXT_LOCAL:
						featNew = FeatureType.TEXT;

						break;
				case STRUCTURE_LOCAL:
						featNew = FeatureType.STRUCTURE;
						break;
				case STYLE_LOCAL:
						featNew = FeatureType.STYLE;
						break;
				case LENGTH_LOCAL:
						featNew = FeatureType.LENGTH;
						break;
				case READ_LOCAL:
						featNew = FeatureType.READ;
						break;
				case HISTORY_LOCAL:
						featNew = FeatureType.HISTORY;					
						break;
				case NETWORK_LOCAL:
						featNew = FeatureType.NETWORK;
						break;	
			}
			if(featNew != null)
			{
				addFeatures.add(featNew);
				remFeatures.add(objFeature);
			}
		}
		this.tipoFeatures.removeAll(remFeatures);
		this.tipoFeatures.addAll(addFeatures);
	}
	public static void main(String[] args)
	{
		
		Set<TipoFeatureCombinacao> lstComb = getCombinacao(6,FeatureType.STRUCTURE,FeatureType.STYLE,FeatureType.LENGTH,FeatureType.READ,FeatureType.HISTORY,FeatureType.NETWORK);
		List<TipoFeatureCombinacao> objListComb = new ArrayList<TipoFeatureCombinacao>(lstComb);
		Collections.sort(objListComb);
		
		System.out.println("Total de combinações: "+objListComb.size());
		for(TipoFeatureCombinacao objTipo : objListComb)
		{
			System.out.println(objTipo);
		}
	}


}

