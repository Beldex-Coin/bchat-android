package com.thoughtcrimes.securesms.data;

import android.content.Context;

import java.util.Set;

public interface Listener {
    Set<NodeInfo> getOrPopulateFavourites(Context context);
}
