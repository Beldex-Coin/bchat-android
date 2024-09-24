package io.beldex.bchat.jobmanager.impl;

import android.app.Application;
import android.app.job.JobInfo;
import androidx.annotation.NonNull;

import io.beldex.bchat.jobmanager.Constraint;
import io.beldex.bchat.sms.TelephonyServiceState;
import io.beldex.bchat.jobmanager.Constraint;
import io.beldex.bchat.sms.TelephonyServiceState;

public class CellServiceConstraint implements Constraint {

    public static final String KEY = "CellServiceConstraint";

    private final Application application;

    public CellServiceConstraint(@NonNull Application application) {
        this.application = application;
    }

    @Override
    public @NonNull String getFactoryKey() {
        return KEY;
    }

    @Override
    public boolean isMet() {
        TelephonyServiceState telephonyServiceState = new TelephonyServiceState();
        return telephonyServiceState.isConnected(application);
    }

    @Override
    public void applyToJobInfo(@NonNull JobInfo.Builder jobInfoBuilder) {
    }

    public static final class Factory implements Constraint.Factory<CellServiceConstraint> {

        private final Application application;

        public Factory(@NonNull Application application) {
            this.application = application;
        }

        @Override
        public CellServiceConstraint create() {
            return new CellServiceConstraint(application);
        }
    }
}
