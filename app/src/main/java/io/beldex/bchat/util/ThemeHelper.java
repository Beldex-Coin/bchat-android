
package io.beldex.bchat.util;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;

import androidx.annotation.ColorInt;

public class ThemeHelper {
    static public int getThemedResourceId(Context ctx, int attrId) {
        final TypedValue typedValue = new TypedValue();
        if (ctx.getTheme().resolveAttribute(attrId, typedValue, true))
            return typedValue.resourceId;
        else
            return 0;
    }

    @ColorInt
    static public int getThemedColor(Context ctx, int attrId) {
        final TypedValue typedValue = new TypedValue();
        if (ctx.getTheme().resolveAttribute(attrId, typedValue, true))
            return typedValue.data;
        else
            return Color.BLACK;
    }
}
