package calendario;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class DateUtil
{
	public static Calendar timestampToDate(Long timestamp)
	{
		return timestampToDate(Long.toString(timestamp));
	}
	public static Calendar timestampToDate(String timestamp)
	{
		
		return new GregorianCalendar(Integer.parseInt(timestamp.substring(0, 4)),
									Integer.parseInt(timestamp.substring(4, 6))-1,
									Integer.parseInt(timestamp.substring(6, 8)),
									Integer.parseInt(timestamp.substring(8, 10)),
									Integer.parseInt(timestamp.substring(10, 12)),
									Integer.parseInt(timestamp.substring(12, 14))
									 );
	}
	public static String dateToString(Calendar date)
	{
		return Integer.toString(date.get(Calendar.DAY_OF_MONTH))+"/"+Integer.toString(date.get(Calendar.MONTH)+1)+"/"+Integer.toString(date.get(Calendar.YEAR))+" "+
				Integer.toString(date.get(Calendar.HOUR_OF_DAY))+":"+Integer.toString(date.get(Calendar.MINUTE))+":"+Integer.toString(date.get(Calendar.SECOND));
	}
	public static Long dateToTimestamp(Calendar date)
	{
		String ano = Integer.toString(date.get(Calendar.YEAR));
		String mes = Integer.toString(date.get(Calendar.MONTH)+1);
		String dia = Integer.toString(date.get(Calendar.DAY_OF_MONTH));
		String hora = Integer.toString(date.get(Calendar.HOUR_OF_DAY));
		String minuto = Integer.toString(date.get(Calendar.MINUTE));
		String segundo = Integer.toString(date.get(Calendar.SECOND));
		
		
		return Long.parseLong(ano+(mes.length()==1?"0":"")+mes+(dia.length()==1?"0":"")+dia+
				(hora.length()==1?"0":"")+hora+(minuto.length()==1?"0":"")+minuto+(segundo.length()==1?"0":"")+segundo);
	}
	public static Long addTimestamp(Long dateTimestamp,int field,int value)
	{
		Calendar date= timestampToDate(dateTimestamp);
		date.add(field, value);
		return dateToTimestamp(date);
	}
	public static void main(String[] args)
	{
		
		System.out.println(timestampToDate(20070403235331L));
		Calendar calend = timestampToDate(20070303235331L);
		calend.add(Calendar.MONTH, -3);
		System.out.println(calend);
		System.out.println(dateToTimestamp(timestampToDate(20070403235331L)));
	}
}











