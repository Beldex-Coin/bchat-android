package io.beldex.bchat.imageeditor;

import android.graphics.Matrix;
import android.graphics.PointF;
import androidx.annotation.NonNull;

import io.beldex.bchat.imageeditor.model.EditorElement;

/**
 * Represents an underway edit of the image.
 * <p>
 * Accepts new touch positions, new touch points, released touch points and when complete can commit the edit.
 * <p>
 * Examples of edit bchat implementations are, Drag, Draw, Resize:
 * <p>
 * {@link ElementDragEditBchat} for dragging with a single finger.
 * {@link ElementScaleEditBchat} for resize/dragging with two fingers.
 * {@link DrawingBchat} for drawing with a single finger.
 */
interface EditBchat {

  void movePoint(int p, @NonNull PointF point);

  EditorElement getSelected();

  EditBchat newPoint(@NonNull Matrix newInverse, @NonNull PointF point, int p);

  EditBchat removePoint(@NonNull Matrix newInverse, int p);

  void commit();
}
