package io.beldex.bchat.database;

import android.database.ContentObserver;
import android.database.Cursor;
import android.database.MatrixCursor;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A list backed by a {@link Cursor} that retrieves models using a provided {@link ModelBuilder}.
 * Allows you to abstract away the use of a {@link Cursor} while still getting the benefits of a
 * {@link Cursor} (e.g. windowing).
 *
 * The one special consideration that must be made is that because this contains a cursor, you must
 * call {@link #close()} when you are finished with it.
 *
 * Given that this is cursor-backed, it is effectively immutable.
 */
public class CursorList<T> implements List<T>, ObservableContent {

  private final Cursor          cursor;
  private final ModelBuilder<T> modelBuilder;

  public CursorList(@NonNull Cursor cursor, @NonNull ModelBuilder<T> modelBuilder) {
    this.cursor       = cursor;
    this.modelBuilder = modelBuilder;

    forceQueryLoad();
  }

  public static <T> CursorList<T> emptyList() {
    //noinspection ConstantConditions,unchecked
    return (CursorList<T>) new CursorList(emptyCursor(), null);
  }

  private static Cursor emptyCursor() {
    return new MatrixCursor(new String[] { "a" }, 0);
  }

  @Override
  public int size() {
    return cursor.getCount();
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean contains(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @NonNull Iterator<T> iterator() {
    return new Iterator<T>() {
      int index = 0;

      @Override
      public boolean hasNext() {
        return cursor.getCount() > 0 && !cursor.isLast();
      }

      @Override
      public T next() {
        cursor.moveToPosition(index++);
        return modelBuilder.build(cursor);
      }
    };
  }

  @Override
  public @NonNull Object[] toArray() {
    Object[] out = new Object[size()];
    for (int i = 0; i < cursor.getCount(); i++) {
      cursor.moveToPosition(i);
      out[i] = modelBuilder.build(cursor);
    }
    return out;
  }

  @Override
  public boolean add(T o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(@NonNull Collection collection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean addAll(int i, @NonNull Collection collection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException();
  }

  @Override
  public T get(int i) {
    if(!cursor.isClosed()) {
      cursor.moveToPosition(i);
      return modelBuilder.build(cursor);
    }
    return null;
  }

  @Override
  public T set(int i, T o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void add(int i, T o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public T remove(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int indexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int lastIndexOf(Object o) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @NonNull ListIterator<T> listIterator() {
    throw new UnsupportedOperationException();
  }

  @Override
  public @NonNull ListIterator<T> listIterator(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @NonNull List<T> subList(int i, int i1) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean retainAll(@NonNull Collection collection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean removeAll(@NonNull Collection collection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean containsAll(@NonNull Collection collection) {
    throw new UnsupportedOperationException();
  }

  @Override
  public @NonNull T[] toArray(@Nullable Object[] objects) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() {
    if (!cursor.isClosed()) {
      cursor.close();
    }
  }

  @Override
  public void registerContentObserver(@NonNull ContentObserver observer) {
    cursor.registerContentObserver(observer);
  }

  @Override
  public void unregisterContentObserver(@NonNull ContentObserver observer) {
    cursor.unregisterContentObserver(observer);
  }

  private void forceQueryLoad() {
    cursor.getCount();
  }

  public interface ModelBuilder<T> {
    T build(@NonNull Cursor cursor);
  }
}

