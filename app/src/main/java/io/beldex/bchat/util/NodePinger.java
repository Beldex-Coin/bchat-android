package io.beldex.bchat.util;

import android.util.Log;

import io.beldex.bchat.data.NodeInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class NodePinger {
    static final public int NUM_THREADS = 10;
    static final public long MAX_TIME = 5L; // seconds

    public interface Listener {
        void publish(NodeInfo node);
    }

    static public void execute(Collection<NodeInfo> nodes, final Listener listener) {
        final ExecutorService exeService = Executors.newFixedThreadPool(NUM_THREADS);
        List<Callable<Boolean>> taskList = new ArrayList<>();
        try {
            for (NodeInfo node : nodes) {
                Log.d("Beldex", "Node list majorversion5 ");

                taskList.add(() -> node.testRpcService(listener));
            }
        } catch(ConcurrentModificationException ex){
            Timber.w(ex);
        }

        try {
            exeService.invokeAll(taskList, MAX_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Timber.w(ex);
        }
        exeService.shutdownNow();
    }
}
