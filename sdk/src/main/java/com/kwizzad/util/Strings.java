package com.kwizzad.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public final class Strings {
    private Strings() {
    }

    public static <T> String join(final String delimiter, final Collection<T> objs) {
        if (objs == null || objs.isEmpty())
            return "";

        final Iterator<T> iter = objs.iterator();
        final StringBuilder buffer = new StringBuilder(Strings.toString(iter.next()));

        while (iter.hasNext()) {
            final T obj = iter.next();
            if (notEmpty(obj))
                buffer.append(delimiter).append(Strings.toString(obj));
        }
        return buffer.toString();
    }

    public static String join(final String delimiter, final Object... objects) {
        return join(delimiter, Arrays.asList(objects));
    }

    public static String toString(final Object o) {
        return toString(o, "");
    }

    public static String toString(final Object o, final String def) {
        return o == null ? def : o instanceof Object[] ? Strings.join(", ", (Object[]) o) : o instanceof Collection ? Strings.join(", ", (Collection<?>) o) : o.toString();
    }

    public static boolean notEmpty(final Object o) {
        return !toString(o).trim().isEmpty();
    }
}
