package dev.zeffut.flashbackserver.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java 8 replacements for {@code List.of}/{@code Map.of}/{@code Set.of}/{@code List.copyOf}.
 * Paper 1.16.1 only accepts Java &lt;= 14 at runtime, so core is compiled with {@code --release 8}.
 */
public final class Coll {
    private Coll() {}

    public static <T> List<T> listOf() {
        return Collections.emptyList();
    }

    public static <T> List<T> listOf(T item) {
        return Collections.singletonList(item);
    }

    @SafeVarargs
    public static <T> List<T> listOf(T... items) {
        List<T> list = new ArrayList<T>(items.length);
        Collections.addAll(list, items);
        return Collections.unmodifiableList(list);
    }

    public static <T> List<T> copyOf(Collection<? extends T> source) {
        return Collections.unmodifiableList(new ArrayList<T>(source));
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... items) {
        return Collections.unmodifiableSet(new LinkedHashSet<T>(Arrays.asList(items)));
    }

    public static <K, V> Map<K, V> mapOf(Object... keysAndValues) {
        Map<K, V> map = new HashMap<K, V>();
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            @SuppressWarnings("unchecked")
            K key = (K) keysAndValues[i];
            @SuppressWarnings("unchecked")
            V value = (V) keysAndValues[i + 1];
            map.put(key, value);
        }
        return Collections.unmodifiableMap(map);
    }

    public static <K, V> Map<K, V> mutableMap() {
        return new HashMap<K, V>();
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
