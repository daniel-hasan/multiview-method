package stuctUtil;

import java.io.Serializable;
import java.util.HashMap;

public class Tripla<X ,Y,Z> extends Tupla<X,Y> implements Serializable
{
	private Z z;
	public Tripla(X x,Y y,Z z)
	{
		super(x,y);
		this.z = z;
	}
	public Z getZ()
	{
		return z;
	}
	public void setZ(Z z)
	{
		this.z = z;
	}
	public String toString()
	{
		return "{"+super.getX()+","+super.getY()+","+this.getZ()+"}";
	}
	
	@Override
	public boolean equals(Object obj) {
		
		// TODO Auto-generated method stub
		Tripla<X,Y,Z> t = (Tripla<X,Y,Z>) obj;
		return  super.equals(t) && ((this.z == null && t.getZ() == null) || this.z.equals(t.getZ()));
	}
	
	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return new StringBuilder().append(this.getX()).append('_').append(this.getY()).append("_").append(this.z).toString().hashCode();
	}
	
	public static void main(String[] args)
	{
		HashMap<Tripla<Integer,Integer,Integer>,String> map = new HashMap<Tripla<Integer,Integer,Integer>, String>();
		map.put(new Tripla<Integer, Integer,Integer>(3, 2,null), "asdasd");
		boolean cont =  map.containsKey(new Tripla<Integer, Integer,Integer>(3, 2,null));
		System.out.println(cont);
	}
	
}
