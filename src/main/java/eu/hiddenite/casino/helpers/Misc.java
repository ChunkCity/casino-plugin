package eu.hiddenite.casino.helpers;

import java.util.*;

public class Misc {
    public static Optional<Integer> firstDuplicate(final Collection<Integer> values)
    {
        Set<Integer> lump = new HashSet<>();
        for (int i : values)
        {
            if (lump.contains(i)) return Optional.of(i);
            lump.add(i);
        }
        return Optional.empty();
    }
}
