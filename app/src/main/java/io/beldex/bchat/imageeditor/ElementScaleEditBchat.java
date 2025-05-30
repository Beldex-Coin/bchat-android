package io.beldex.bchat.imageeditor;

import android.graphics.Matrix;
import android.graphics.PointF;
import androidx.annotation.NonNull;

import io.beldex.bchat.imageeditor.model.EditorElement;

final class ElementScaleEditBchat extends ElementEditBchat {

  private ElementScaleEditBchat(@NonNull EditorElement selected, @NonNull Matrix inverseMatrix) {
    super(selected, inverseMatrix);
  }

  static ElementScaleEditBchat startScale(@NonNull ElementDragEditBchat bchat, @NonNull Matrix inverseMatrix, @NonNull PointF point, int p) {
    bchat.commit();
    ElementScaleEditBchat newBchat = new ElementScaleEditBchat(bchat.selected, inverseMatrix);
    newBchat.setScreenStartPoint(1 - p, bchat.endPointScreen[0]);
    newBchat.setScreenEndPoint(1 - p, bchat.endPointScreen[0]);
    newBchat.setScreenStartPoint(p, point);
    newBchat.setScreenEndPoint(p, point);
    return newBchat;
  }

  @Override
  public void movePoint(int p, @NonNull PointF point) {
    setScreenEndPoint(p, point);
    Matrix editorMatrix = selected.getEditorMatrix();

    editorMatrix.reset();

    if (selected.getFlags().isAspectLocked()) {

      float scale = (float) findScale(startPointElement, endPointElement);

      editorMatrix.postTranslate(-startPointElement[0].x, -startPointElement[0].y);
      editorMatrix.postScale(scale, scale);

      double angle = angle(endPointElement[0], endPointElement[1]) - angle(startPointElement[0], startPointElement[1]);

      if (!selected.getFlags().isRotateLocked()) {
        editorMatrix.postRotate((float) Math.toDegrees(angle));
      }

      editorMatrix.postTranslate(endPointElement[0].x, endPointElement[0].y);
    } else {
      editorMatrix.postTranslate(-startPointElement[0].x, -startPointElement[0].y);

      float scaleX = (endPointElement[1].x - endPointElement[0].x) / (startPointElement[1].x - startPointElement[0].x);
      float scaleY = (endPointElement[1].y - endPointElement[0].y) / (startPointElement[1].y - startPointElement[0].y);

      editorMatrix.postScale(scaleX, scaleY);

      editorMatrix.postTranslate(endPointElement[0].x, endPointElement[0].y);
    }
  }

  @Override
  public EditBchat newPoint(@NonNull Matrix newInverse, @NonNull PointF point, int p) {
    return this;
  }

  @Override
  public EditBchat removePoint(@NonNull Matrix newInverse, int p) {
    return convertToDrag(p, newInverse);
  }

  private static double angle(@NonNull PointF a, @NonNull PointF b) {
    return Math.atan2(a.y - b.y, a.x - b.x);
  }

  private ElementDragEditBchat convertToDrag(int p, @NonNull Matrix inverse) {
    return ElementDragEditBchat.startDrag(selected, inverse, endPointScreen[1 - p]);
  }

  /**
   * Find relative distance between an old and new set of Points.
   *
   * @param from Pair of points.
   * @param to   New pair of points.
   * @return Scale
   */
  private static double findScale(@NonNull PointF[] from, @NonNull PointF[] to) {
    float originalD2 = getDistanceSquared(from[0], from[1]);
    float newD2      = getDistanceSquared(to[0], to[1]);
    return Math.sqrt(newD2 / originalD2);
  }

  /**
   * Distance between two points squared.
   */
  private static float getDistanceSquared(@NonNull PointF a, @NonNull PointF b) {
    float dx = a.x - b.x;
    float dy = a.y - b.y;
    return dx * dx + dy * dy;
  }
}
