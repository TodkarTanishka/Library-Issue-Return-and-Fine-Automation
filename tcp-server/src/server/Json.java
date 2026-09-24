package server;
import java.util.*;
public class Json {
    public static String esc(Object v){if(v==null)return "null";if(v instanceof Number||v instanceof Boolean)return String.valueOf(v);return "\""+String.valueOf(v).replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r")+"\"";}
    public static String object(Map<String,?> m){StringBuilder b=new StringBuilder("{");int n=0;for(var e:m.entrySet()){if(n++>0)b.append(',');b.append(esc(e.getKey())).append(':').append(value(e.getValue()));}return b.append('}').toString();}
    public static String value(Object v){if(v instanceof Map<?,?> m){Map<String,Object>x=new LinkedHashMap<>();for(var e:m.entrySet())x.put(String.valueOf(e.getKey()),e.getValue());return object(x);}if(v instanceof Collection<?> c){StringBuilder b=new StringBuilder("[");int n=0;for(Object x:c){if(n++>0)b.append(',');b.append(value(x));}return b.append(']').toString();}return esc(v);}
}
