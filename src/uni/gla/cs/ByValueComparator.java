package uni.gla.cs;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Comparator to sort a Map by Value.
 * @author Sujit Pal
 * @version $Revision: 2 $
 */
public class ByValueComparator<K,V extends Comparable<? super V>> 
    implements Comparator<K> {

  private Map<K,V> map = new HashMap<K,V>();
  
  public ByValueComparator(Map<K,V> map) {
    this.map = map;
  }

  public int compare(K k1, K k2) {
    return map.get(k1).compareTo(map.get(k2));
  }
  
}
