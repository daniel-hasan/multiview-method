package aprendizadoUtils;

import java.util.Arrays;

public class MetricUtils
{

    public static void main(String[] args) {
            // Rank:
            Double[] ranking = new Double[] { 5d, 4d, 5d, 4d, 4d, 4d };
            Double[] perfecRanking = new Double[] { 5d, 5d, 5d, 4d, 4d, 4d };

            // LEXICOGRAPHIC ORDERING:
            double lexScore = lex(ranking);
            System.out.println("LEX = " + lexScore);
           
            // DCG TEST:
            double dcgScore = dcg(ranking,false);
            System.out.println("DCG = " + dcgScore);

            // NDCG TEST:
            double ndcgScore = ndcg(ranking,perfecRanking,false);
            System.out.println("NDCG = " + ndcgScore);
            
            // ERR Test
            double err = err(ranking,5);
            System.out.println("ERR = " + err);
    }

    public static double lex(Double[] orgRanking) {
            Double[] ranking = new Double[orgRanking.length];
            for(int i = 0 ; i < orgRanking.length ; i++){
                    ranking[i] = orgRanking[i];
            }
            double feedbackScore = 0;
            for (int i = 9; i >= 0; i--) {
                    int idx = 9 - i;
                    double currScore = Math.pow(2, i) * ranking[idx];
                    feedbackScore += currScore;
                    if (idx == ranking.length - 1) {
                            return feedbackScore;
                    }
            }
            return feedbackScore;
    }
   
    /**
     * Calculates the DCG (Discounted cumulative gain) score for the specified ranking evaluation.
     *
     * @param ranking
     *            the ranking evaluation
     * @return the DCG score for the ranking
     */
    public static double dcg(Double[] ranking,boolean expNDCG) {
            double score = 0;
            for (int i = 1; i < ranking.length; i++) {
                    score += calcDcgForPos(ranking[i], i + 1,expNDCG);
            }
            if(!expNDCG)
            {
            	return ranking[0] + score;	
            }else
            {
            	return Math.pow(ranking[0],2) + score;
            }
            
    }

    /**
     * Calculates the DCG (Discounted cumulative gain) score for the specified evaluation score at the specified rank
     * position.
     *
     * @param score
     *            the evaluation score
     * @param rank
     *            the rank position
     * @return the DCG score
     */
    private static double calcDcgForPos(double score, int rank,boolean expNDCG) {
            double log2Pos = Math.log(rank) / Math.log(2);
            if(expNDCG)
            {
            	return Math.pow(score, 2) / log2Pos;
            	
            }else
            {
            	return score / log2Pos;
            }
    }

    /**
     * Calculates the NDCG (Normalized discounted cumulative gain) score for the specified ranking evaluation.
     *
     * @param ranking
     *            the ranking evaluation
     * @return the NDCG score for the ranking
     */
    public static double ndcg(Double[] orgRanking,Double[] realClassArray,boolean expNDCG) {
            Double[] ranking = new Double[orgRanking.length];
            for(int i = 0 ; i < orgRanking.length ; i++){
                    ranking[i] = orgRanking[i];
            }
            double dcg =dcg(ranking,expNDCG);
            double norm =dcg(reverseSortDesc(realClassArray),expNDCG); 
            return  dcg/ norm;
    }

    /**
     * Reverse sorts the specified array in descending order based on the evaluation value.
     *
     * @param ranking
     *            the ranking evaluation
     * @return the sorted array
     */
    public static Double[] reverseSortDesc(Double[] ranking) {
            Double[] reverseSorted = new Double[ranking.length];
            Arrays.sort(ranking);
            int arrLen = ranking.length;
            for (int i = arrLen - 1; i >= 0; i--) {
                    int pos = arrLen - 1 - i;
                    reverseSorted[pos] = ranking[i];
            }
            return reverseSorted;
    }
   
    /**
     * Calculates the ERR (Expected Reciprocal Rank).
     *
     * @param ranking
     *            the ranking evaluation
     * @return the ERR score
     */
    public static double err(Double[] orgRanking,double maxGrade) {
            Double[] ranking = new Double[orgRanking.length];
            for(int i = 0 ; i < orgRanking.length ; i++){
                    ranking[i] = orgRanking[i];
            }
            double p = 1;
            double errScore = 0;
            int n = ranking.length;
            for (int r = 1; r <= n; r++) {
                    double g = ranking[r - 1];
                    double rg = (Math.pow(2, g) - 1) / Math.pow(2, maxGrade);
                    errScore += p * (rg / r);
                    p = p * (1 - rg);
            }
            return errScore;
    }

}
