package uni.glas.cs.test.extractkeywords;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Keyword implements Comparable<Keyword> {

    private String stem;
    private Integer frequency;
    private Set<String> terms;

    public Keyword(String stem) {
        this.stem = stem;
        terms = new HashSet<String>();
        frequency = 0;
    }

    public void add(String term) {
        terms.add(term);
        frequency++;
    }

    @Override
    public int compareTo(Keyword o) {
        return o.frequency.compareTo(frequency);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Keyword && obj.hashCode() == hashCode();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[] { stem });
    }

    @Override
    public String toString() {
        return stem + " x" + frequency;
    }

}