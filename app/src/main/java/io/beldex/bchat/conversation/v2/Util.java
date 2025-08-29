package io.beldex.bchat.conversation.v2;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.annimon.stream.Stream;
import com.google.android.mms.pdu_alt.CharacterSets;
import com.google.android.mms.pdu_alt.EncodedStringValue;
import com.beldex.libsignal.utilities.Log;
import io.beldex.bchat.components.ComposeText;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import io.beldex.bchat.R;
public class Util {
    private static final String TAG = Log.tag(Util.class);
    private static final long BUILD_LIFESPAN = TimeUnit.DAYS.toMillis(90);
    public static <T> List<T> asList(T... elements) {
        List<T> result = new LinkedList<>();
        Collections.addAll(result, elements);
        return result;
    }
    public static String join(String[] list, String delimiter) {
        return join(Arrays.asList(list), delimiter);
    }
    public static <T> String join(Collection<T> list, String delimiter) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        for (T item : list) {
            result.append(item);
            if (++i < list.size())
                result.append(delimiter);
        }
        return result.toString();
    }
    public static String join(long[] list, String delimeter) {
        List<Long> boxed = new ArrayList<>(list.length);
        for (int i = 0; i < list.length; i++) {
            boxed.add(list[i]);
        }
        return join(boxed, delimeter);
    }
    @SafeVarargs
    public static @NonNull <E> List<E> join(@NonNull List<E>... lists) {
        int     totalSize = Stream.of(lists).reduce(0, (sum, list) -> sum + list.size());
        List<E> joined    = new ArrayList<>(totalSize);
        for (List<E> list : lists) {
            joined.addAll(list);
        }
        return joined;
    }
    public static String join(List<Long> list, String delimeter) {
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < list.size(); j++) {
            if (j != 0) sb.append(delimeter);
            sb.append(list.get(j));
        }
        return sb.toString();
    }
    public static String rightPad(String value, int length) {
        if (value.length() >= length) {
            return value;
        }
        StringBuilder out = new StringBuilder(value);
        while (out.length() < length) {
            out.append(" ");
        }
        return out.toString();
    }
    public static boolean isEmpty(EncodedStringValue[] value) {
        return value == null || value.length == 0;
    }
    public static boolean isEmpty(ComposeText value) {
        return value == null || value.getText() == null || TextUtils.isEmpty(value.getTextTrimmed());
    }
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
    public static boolean isEmpty(@Nullable CharSequence charSequence) {
        return charSequence == null || charSequence.length() == 0;
    }
    public static boolean hasItems(@Nullable Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }
    public static <K, V> V getOrDefault(@NonNull Map<K, V> map, K key, V defaultValue) {
        return map.containsKey(key) ? map.get(key) : defaultValue;
    }
    public static String getFirstNonEmpty(String... values) {
        for (String value : values) {
            if (!Util.isEmpty(value)) {
                return value;
            }
        }
        return "";
    }
    public static @NonNull String emptyIfNull(@Nullable String value) {
        return value != null ? value : "";
    }
    public static @NonNull CharSequence emptyIfNull(@Nullable CharSequence value) {
        return value != null ? value : "";
    }
    public static CharSequence getBoldedString(String value) {
        SpannableString spanned = new SpannableString(value);
        spanned.setSpan(new StyleSpan(Typeface.BOLD), 0,
                spanned.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spanned;
    }
    public static @NonNull String toIsoString(byte[] bytes) {
        try {
            return new String(bytes, CharacterSets.MIMENAME_ISO_8859_1);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("ISO_8859_1 must be supported!");
        }
    }
    public static byte[] toIsoBytes(String isoString) {
        try {
            return isoString.getBytes(CharacterSets.MIMENAME_ISO_8859_1);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("ISO_8859_1 must be supported!");
        }
    }
    public static byte[] toUtf8Bytes(String utf8String) {
        try {
            return utf8String.getBytes(CharacterSets.MIMENAME_UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF_8 must be supported!");
        }
    }
    public static void wait(Object lock, long timeout) {
        try {
            lock.wait(timeout);
        } catch (InterruptedException ie) {
            throw new AssertionError(ie);
        }
    }
    public static List<String> split(String source, String delimiter) {
        List<String> results = new LinkedList<>();
        if (TextUtils.isEmpty(source)) {
            return results;
        }
        String[] elements = source.split(delimiter);
        Collections.addAll(results, elements);
        return results;
    }
    public static byte[][] split(byte[] input, int firstLength, int secondLength) {
        byte[][] parts = new byte[2][];
        parts[0] = new byte[firstLength];
        System.arraycopy(input, 0, parts[0], 0, firstLength);
        parts[1] = new byte[secondLength];
        System.arraycopy(input, firstLength, parts[1], 0, secondLength);
        return parts;
    }
    public static byte[] combine(byte[]... elements) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            for (byte[] element : elements) {
                baos.write(element);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
    public static byte[] trim(byte[] input, int length) {
        byte[] result = new byte[length];
        System.arraycopy(input, 0, result, 0, result.length);
        return result;
    }
    public static byte[] getSecretBytes(int size) {
        return getSecretBytes(new SecureRandom(), size);
    }
    public static byte[] getSecretBytes(@NonNull SecureRandom secureRandom, int size) {
        byte[] secret = new byte[size];
        secureRandom.nextBytes(secret);
        return secret;
    }
    public static <T> T getRandomElement(T[] elements) {
        return elements[new SecureRandom().nextInt(elements.length)];
    }
    public static <T> T getRandomElement(List<T> elements) {
        return elements.get(new SecureRandom().nextInt(elements.size()));
    }
    public static boolean equals(@Nullable Object a, @Nullable Object b) {
        return a == b || (a != null && a.equals(b));
    }
    public static int hashCode(@Nullable Object... objects) {
        return Arrays.hashCode(objects);
    }
    public static @Nullable Uri uri(@Nullable String uri) {
        if (uri == null) return null;
        else             return Uri.parse(uri);
    }
    @TargetApi(VERSION_CODES.KITKAT)
    public static boolean isLowMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return (VERSION.SDK_INT >= VERSION_CODES.KITKAT && activityManager.isLowRamDevice()) ||
                activityManager.getLargeMemoryClass() <= 64;
    }
    public static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }
    public static long clamp(long value, long min, long max) {
        return Math.min(Math.max(value, min), max);
    }
    public static float clamp(float value, float min, float max) {
        return Math.min(Math.max(value, min), max);
    }
    /**
     * Returns half of the difference between the given length, and the length when scaled by the
     * given scale.
     */
    public static float halfOffsetFromScale(int length, float scale) {
        float scaledLength = length * scale;
        return (length - scaledLength) / 2;
    }
    public static @Nullable String readTextFromClipboard(@NonNull Context context) {
        {
            ClipboardManager clipboardManager = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboardManager.hasPrimaryClip() && clipboardManager.getPrimaryClip().getItemCount() > 0) {
                return clipboardManager.getPrimaryClip().getItemAt(0).getText().toString();
            } else {
                return null;
            }
        }
    }
    public static void writeTextToClipboard(@NonNull Context context, @NonNull String text) {
        writeTextToClipboard(context, context.getString(R.string.app_name), text);
    }
    public static void writeTextToClipboard(@NonNull Context context, @NonNull String label, @NonNull String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
    }
    public static int toIntExact(long value) {
        if ((int)value != value) {
            throw new ArithmeticException("integer overflow");
        }
        return (int)value;
    }
    public static boolean isEquals(@Nullable Long first, long second) {
        return first != null && first == second;
    }
    @SafeVarargs
    public static <T> List<T> concatenatedList(Collection <T>... items) {
        final List<T> concat = new ArrayList<>(Stream.of(items).reduce(0, (sum, list) -> sum + list.size()));
        for (Collection<T> list : items) {
            concat.addAll(list);
        }
        return concat;
    }
    public static boolean isLong(String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    public static int parseInt(String integer, int defaultValue) {
        try {
            return Integer.parseInt(integer);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}