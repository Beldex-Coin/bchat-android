package io.beldex.bchat.data;

import java.util.Set;

public interface Listener {
    Set<NodeInfo> getOrPopulateFavourites();
}
