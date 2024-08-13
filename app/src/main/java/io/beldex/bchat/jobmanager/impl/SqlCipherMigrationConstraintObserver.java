package io.beldex.bchat.jobmanager.impl;

import androidx.annotation.NonNull;

import io.beldex.bchat.jobmanager.ConstraintObserver;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import io.beldex.bchat.jobmanager.ConstraintObserver;

public class SqlCipherMigrationConstraintObserver implements ConstraintObserver {

    private static final String REASON = SqlCipherMigrationConstraintObserver.class.getSimpleName();

    private Notifier notifier;

    public SqlCipherMigrationConstraintObserver() {
        EventBus.getDefault().register(this);
    }

    @Override
    public void register(@NonNull Notifier notifier) {
        this.notifier = notifier;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SqlCipherNeedsMigrationEvent event) {
        if (notifier != null) notifier.onConstraintMet(REASON);
    }

    public static class SqlCipherNeedsMigrationEvent {
    }
}
