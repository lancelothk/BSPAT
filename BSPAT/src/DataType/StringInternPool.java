package DataType;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/**
 * Created by kehu on 9/10/14.
 */
public class StringInternPool {
    private static final WeakHashMap<String, WeakReference<String>> stringCache =
            new WeakHashMap<>(1000000);

    public static String intern(String str) {
        final WeakReference<String> cached = stringCache.get(str);
        if (cached != null) {
            final String value = cached.get();
            if (value != null) return value;
        }
        stringCache.put(str, new WeakReference<>(str));
        return str;
    }
}
