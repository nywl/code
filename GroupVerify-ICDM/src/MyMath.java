import java.math.BigDecimal;

public class MyMath 
{ 
    public static double add(double d1, double d2)
    {        // ���мӷ����� 
         BigDecimal b1 = new BigDecimal(Double.toString(d1)); 
         BigDecimal b2 = new BigDecimal(Double.toString(d2)); 
        return b1.add(b2).doubleValue(); 
    } 
    public static double sub(double d1, double d2)
    {        // ���м������� 
         BigDecimal b1 = new BigDecimal(Double.toString(d1)); 
         BigDecimal b2 = new BigDecimal(Double.toString(d2)); 
        return b1.subtract(b2).doubleValue(); 
    } 
    public static double mul(double d1, double d2)
    {        // ���г˷����� 
         BigDecimal b1 = new BigDecimal(Double.toString(d1)); 
         BigDecimal b2 = new BigDecimal(Double.toString(d2)); 
        return b1.multiply(b2).doubleValue(); 
    } 
    public static double div(double d1, double d2,int len) 
    {// ���г������� 
         BigDecimal b1 = new BigDecimal(Double.toString(d1)); 
         BigDecimal b2 = new BigDecimal(Double.toString(d2)); 
        return b1.divide(b2,len,BigDecimal.ROUND_HALF_UP).doubleValue(); 
    }
}
