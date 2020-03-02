package kz.bsbnb.usci.util;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Artur Tkachenko
 */

public class SetUtils {

    public static <T> Set<T> difference(Set<T> setA, Set<T> setB) {
        Set<T> tmp = new HashSet<T>();
        for (T key : setA) {
            if (!setB.contains(key)) {
                tmp.add(key);
            }
        }
        return tmp;
    }

    public static <T> Set<T> intersection(Set<T> setA, Set<T> setB) {
        Set<T> tmp = new HashSet<T>();
        for (T x : setA)
            if (setB.contains(x))
                tmp.add(x);
        return tmp;
    }

}
