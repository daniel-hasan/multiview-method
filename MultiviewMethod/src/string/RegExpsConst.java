package string;

public class RegExpsConst {
	public static final String DIGITO = "[0-9]+";
	public static final String DIGITO_FLOAT_OPCIONAL = "[-0-9]+(\\.[0-9]+)?([eE][-0-9]+)?";
	public static final String ALPHA_MINUSCULA = "[a-záéíóúàéíóúâêîôûäëïöüãẽĩõũ]";
	public static final String ALPHA_CASE_INS = "("+ALPHA_MINUSCULA+"|"+ALPHA_MINUSCULA.toUpperCase()+")";
	
	public static final String WORD_CASE_INS = ALPHA_CASE_INS+"+";
	
	
	
	
}
