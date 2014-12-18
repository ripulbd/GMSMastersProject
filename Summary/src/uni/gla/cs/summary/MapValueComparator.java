package uni.gla.cs.summary;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

public class MapValueComparator implements Comparator<Map.Entry<String, Double>> {  
    public int compare(Entry<String, Double> score1, Entry<String, Double> score2) {  
        return score2.getValue().compareTo(score1.getValue());  
    }  
}