package com.thoughtcrimes.securesms.imageeditor;

import android.graphics.Matrix;
import android.graphics.PointF;
import androidx.annotation.NonNull;

import com.thoughtcrimes.securesms.imageeditor.model.EditorElement;
import com.thoughtcrimes.securesms.imageeditor.renderers.BezierDrawingRenderer;

/**
 * Passes touch events into a {@link BezierDrawingRenderer}.
 */
class DrawingBchat extends ElementEditBchat {

  private final BezierDrawingRenderer renderer;

  private DrawingBchat(@NonNull EditorElement selected, @NonNull Matrix inverseMatrix, @NonNull BezierDrawingRenderer renderer) {
    super(selected, inverseMatrix);
    this.renderer = renderer;
  }

  public static EditBchat start(EditorElement element, BezierDrawingRenderer renderer, Matrix inverseMatrix, PointF point) {
    DrawingBchat drawingBchat = new DrawingBchat(element, inverseMatrix, renderer);
    drawingBchat.setScreenStartPoint(0, point);
    renderer.setFirstPoint(drawingBchat.startPointElement[0]);
    return drawingBchat;
  }

  @Override
  public void movePoint(int p, @NonNull PointF point) {
    if (p != 0) return;
    setScreenEndPoint(p, point);
    renderer.addNewPoint(endPointElement[0]);
  }

  @Override
  public EditBchat newPoint(@NonNull Matrix newInverse, @NonNull PointF point, int p) {
    return this;
  }

  @Override
  public EditBchat removePoint(@NonNull Matrix newInverse, int p) {
    return this;
  }
}
