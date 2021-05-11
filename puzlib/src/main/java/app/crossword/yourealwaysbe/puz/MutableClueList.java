
package app.crossword.yourealwaysbe.puz;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

class MutableClueList implements ClueList {
    private NavigableMap<Integer, Clue> clueMap = new TreeMap<>();

    // access through invalidateIndexCache and getIndexCache
    private Map<Integer, Integer> indexMap;

    public void addClue(Clue clue) {
        clueMap.put(clue.getNumber(), clue);
        invalidateIndexCache();
    }

    @Override
    public Iterator<Clue> iterator() {
        return clueMap.values().iterator();
    }

    @Override
    public Clue getClue(int number) {
        return clueMap.get(number);
    }

    @Override
    public Collection<Clue> getClues() {
        // number order guaranteed by NavigableMap
        return clueMap.values();
    }

    @Override
    public boolean hasClue(int number) {
        return clueMap.containsKey(number);
    }

    @Override
    public int size() {
        return clueMap.size();
    }

    @Override
    public int getFirstClueNumber() {
        return clueMap.firstEntry().getKey();
    }

    @Override
    public int getNextClueNumber(int number, boolean wrap) {
        Integer next = clueMap.higherKey(number);
        if (next == null)
            return wrap ? clueMap.firstEntry().getKey() : -1;
        else
            return next;
    }

    @Override
    public int getPreviousClueNumber(int number, boolean wrap) {
        Integer previous = clueMap.lowerKey(number);
        if (previous == null)
            return wrap ? clueMap.lastEntry().getKey() : -1;
        else
            return previous;
    }

    @Override
    public int getClueIndex(int number) {
        return getIndexCache().get(number);
    }

    @Override
    public int hashCode() {
        return clueMap.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MutableClueList))
            return false;

        return clueMap.equals(((MutableClueList) o).clueMap);
    }

    @Override
    public String toString() {
        return clueMap.toString();
    }

    private void invalidateIndexCache() {
        indexMap = null;
    }

    private Map<Integer, Integer> getIndexCache() {
        if (indexMap == null) {
            indexMap = new HashMap<>();
            int idx = 0;
            for (Clue clue : getClues()) {
                indexMap.put(clue.getNumber(), idx);
                idx += 1;
            }
        }
        return indexMap;
    }

}
