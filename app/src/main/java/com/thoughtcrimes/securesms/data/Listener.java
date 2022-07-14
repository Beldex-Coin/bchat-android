package com.thoughtcrimes.securesms.data;

import java.util.Set;

public interface Listener {
    Set<NodeInfo> getOrPopulateFavourites();
}
