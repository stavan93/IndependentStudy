import java.util.HashMap;
import java.util.Map;

public class Offset {
    public Map<String, Integer> get_offset(String s){
        s = s.replaceAll("\\p{Punct}"," ");
        System.out.println(s);
        String[] ss = s.split(" ");
        Map<String, Integer> map = new HashMap<>();
        for(String text : ss){
            map.put(text, s.indexOf(text));
        }
        for(Map.Entry<String, Integer> entry : map.entrySet()){
            System.out.println(entry.getKey() + " " + entry.getValue() + " "
            + ((int)(entry.getKey().length())+entry.getValue()-1));
        }
        return map;
    }
    public static void main(String[] args){
        String s = "Obama was born in USA, he is persident.";
        //get_offset(s);
    }
}
