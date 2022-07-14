package com.thoughtcrimes.securesms.imageeditor;

import android.graphics.Matrix;
import android.graphics.PointF;
import androidx.annotation.NonNull;

import com.thoughtcrimes.securesms.imageeditor.model.EditorElement;

final class ElementDragEditBchat extends ElementEditBchat {

  private ElementDragEditBchat(@NonNull EditorElement selected, @NonNull Matrix inverseMatrix) {
    super(selected, inverseMatrix);
  }

  static ElementDragEditBchat startDrag(@NonNull EditorElement selected, @NonNull Matrix inverseViewModelMatrix, @NonNull PointF point) {
    if (!selected.getFlags().isEditable()) return null;

    ElementDragEditBchat elementDragEditBchat = new ElementDragEditBchat(selected, inverseViewModelMatrix);
    elementDragEditBchat.setScreenStartPoint(0, point);
    elementDragEditBchat.setScreenEndPoint(0, point);

    return elementDragEditBchat;
  }

  @Override
  public void movePoint(int p, @NonNull PointF point) {
    setScreenEndPoint(p, point);

    selected.getEditorMatrix()
            .setTranslate(endPointElement[0].x - startPointElement[0].x, endPointElement[0].y - startPointElement[0].y);
  }

  @Override
  public EditBchat newPoint(@NonNull Matrix newInverse, @NonNull PointF point, int p) {
    return ElementScaleEditBchat.startScale(this, newInverse, point, p);
  }

  @Override
  public EditBchat removePoint(@NonNull Matrix newInverse, int p) {
    return this;
  }
}
