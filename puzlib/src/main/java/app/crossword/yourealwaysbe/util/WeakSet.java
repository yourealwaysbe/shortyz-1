
package app.crossword.yourealwaysbe.util;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class WeakSet {

    /**
     * Returns a new set of weak references
     */
    public static <T> Set<T> buildSet() {
        return Collections.newSetFromMap(
            Collections.synchronizedMap(
                new WeakHashMap<T, Boolean>()
            )
        );
    }
}

