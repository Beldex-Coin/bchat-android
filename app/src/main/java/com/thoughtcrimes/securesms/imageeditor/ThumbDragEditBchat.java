package com.thoughtcrimes.securesms.imageeditor;

import android.graphics.Matrix;
import android.graphics.PointF;

import androidx.annotation.NonNull;

import com.thoughtcrimes.securesms.imageeditor.model.EditorElement;
import com.thoughtcrimes.securesms.imageeditor.model.ThumbRenderer;

class ThumbDragEditBchat extends ElementEditBchat {

  @NonNull
  private final ThumbRenderer.ControlPoint controlPoint;

  private ThumbDragEditBchat(@NonNull EditorElement selected, @NonNull ThumbRenderer.ControlPoint controlPoint, @NonNull Matrix inverseMatrix) {
    super(selected, inverseMatrix);
    this.controlPoint = controlPoint;
  }

  static EditBchat startDrag(@NonNull EditorElement selected, @NonNull Matrix inverseViewModelMatrix, @NonNull ThumbRenderer.ControlPoint controlPoint, @NonNull PointF point) {
    if (!selected.getFlags().isEditable()) return null;

    ElementEditBchat elementDragEditBchat = new ThumbDragEditBchat(selected, controlPoint, inverseViewModelMatrix);
    elementDragEditBchat.setScreenStartPoint(0, point);
    elementDragEditBchat.setScreenEndPoint(0, point);
    return elementDragEditBchat;
  }

  @Override
  public void movePoint(int p, @NonNull PointF point) {
    setScreenEndPoint(p, point);

    Matrix editorMatrix = selected.getEditorMatrix();

    editorMatrix.reset();

    float x = controlPoint.opposite().getX();
    float y = controlPoint.opposite().getY();

    float dx = endPointElement[0].x - startPointElement[0].x;
    float dy = endPointElement[0].y - startPointElement[0].y;

    float xEnd = controlPoint.getX() + dx;
    float yEnd = controlPoint.getY() + dy;

    boolean aspectLocked = selected.getFlags().isAspectLocked() && !controlPoint.isCenter();

    float defaultScale = aspectLocked ? 2 : 1;

    float scaleX = controlPoint.isVerticalCenter()   ? defaultScale : (xEnd - x) / (controlPoint.getX() - x);
    float scaleY = controlPoint.isHorizontalCenter() ? defaultScale : (yEnd - y) / (controlPoint.getY() - y);

    scale(editorMatrix, aspectLocked, scaleX, scaleY, controlPoint.opposite());
  }

  private void scale(Matrix editorMatrix, boolean aspectLocked, float scaleX, float scaleY, ThumbRenderer.ControlPoint around) {
    float x = around.getX();
    float y = around.getY();
    editorMatrix.postTranslate(-x, -y);
    if (aspectLocked) {
      float minScale = Math.min(scaleX, scaleY);
      editorMatrix.postScale(minScale, minScale);
    } else {
      editorMatrix.postScale(scaleX, scaleY);
    }
    editorMatrix.postTranslate(x, y);
  }

  @Override
  public EditBchat newPoint(@NonNull Matrix newInverse, @NonNull PointF point, int p) {
    return null;
  }

  @Override
  public EditBchat removePoint(@NonNull Matrix newInverse, int p) {
    return null;
  }
}