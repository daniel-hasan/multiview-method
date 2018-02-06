package aprendizadoUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import stuctUtil.Tupla;

public class ValidaFolds {
	private static Set<Integer>[] idsPerTrain;
	private static Set<Integer>[] idsPerTest;

	private static int numFolds = 5;
	private static File dir = new File("/home/profhasan/git/adaptive-qa-ranking/data/answers_features_completeFolds/stack");
	//private static File dir = new File("/home/profhasan/git/adaptive-qa-ranking/data/answers_features/cook");
	private static String foldTrainTemplate = "treino.fold$NUM";
	private static String foldTestTemplate = "teste.fold$NUM";
	private static String idToCompare = "qid";
	private static MetodoAprendizado formato = new GenericoLetorLike();
	private static void addFileToSet(File arquivo, Set<Integer> list) throws IOException {
		
		BufferedReader in = new BufferedReader(new FileReader(arquivo));
		String linha;

		while ((linha = in.readLine()) != null)
		{
			list.add(formato.getIdPorLinhaArquivo(linha, idToCompare));
		}
		in.close();
		System.out.println("Arquivo: "+arquivo.getAbsolutePath()+"\t# de ids: "+list.size());
	}
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		idsPerTrain = new HashSet[numFolds];
		idsPerTest = new HashSet[numFolds];
		
		
		for(int i =0; i<numFolds; i++) {
			File foldTrain = new File(dir,foldTrainTemplate.replace("$NUM", Integer.toString(i)));
			idsPerTrain[i] = new HashSet<Integer>();
			addFileToSet(foldTrain, idsPerTrain[i]);
			
			File foldTest = new File(dir,foldTestTemplate.replace("$NUM", Integer.toString(i)));
			idsPerTest[i] = new HashSet<Integer>();
			addFileToSet(foldTest, idsPerTest[i]);
			

			
		}
		
	
	}

	@Test
	public void testeETreinoSemIdsIguais() {
		for(int i =0; i<numFolds; i++) {
			for(int idTeste : idsPerTest[i]) {
				Assert.assertTrue("O treino "+i+" contem o mesmo id do que no teste("+idTeste+")",!idsPerTrain[i].contains(idTeste));
			}
		}

	}
	@Test
	public void verificaFoldsIguais() {
		List<Tupla<Integer,Integer>> foldsIguais = new ArrayList<>();
		for(int i =0; i<numFolds; i++) {
			for(int j = i+1 ; j<numFolds ; j++) {
				Set<Integer> inTreinoINotInTreinoJ = new HashSet<Integer>();
				for(int idTreinoI : idsPerTrain[i]) {	
						if(!idsPerTrain[j].contains(idTreinoI)) {
							inTreinoINotInTreinoJ.add(idTreinoI);
						}
					
				}
				if(inTreinoINotInTreinoJ.size()==0) {
					foldsIguais.add(new Tupla<Integer,Integer>(i,j));
					
				}
			
			}
		
		}
		System.out.println("Folds iguais: "+foldsIguais);		
	}
	@Test
	public void testesTotalmenteDiferentes() {
		int totalInstances = 0;
		for(int i =0; i<numFolds; i++) {
			totalInstances += idsPerTest[i].size();
			for(int idTeste : idsPerTest[i]) {
				
				for(int j=0; j<numFolds ; j++) {
					if(i!=j) {
						Assert.assertTrue("O teste "+i+" contem o mesmo id do que no teste "+j+" ("+idTeste+")",!idsPerTest[j].contains(idTeste));
					}
				}
			}
		}
		System.out.println("Total instances: "+totalInstances);
	}

}
