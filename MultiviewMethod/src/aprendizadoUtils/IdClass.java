package aprendizadoUtils;

public class IdClass {
	private long id;
	private String classe;
	public IdClass(long id, String classe) {
		super();
		this.id = id;
		this.classe = classe;
	}
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getClasse() {
		return classe;
	}
	public void setClasse(String classe) {
		this.classe = classe;
	}
	public boolean equals(Object o )
	{
		if(o instanceof IdClass)
		{
			IdClass idClass = (IdClass) o;
			return idClass.getId() == id;
		}
		if(o instanceof Long)
		{
			Long id = (Long) o;
			return id == this.id;
		}
		return false;
	}
	
	public String toString()
	{
		return id+":"+classe;
	}

}
