package io.beldex.bchat.jobmanager;

import android.app.job.JobInfo;
import androidx.annotation.NonNull;

public interface Constraint {

  boolean isMet();

  @NonNull String getFactoryKey();

  void applyToJobInfo(@NonNull JobInfo.Builder jobInfoBuilder);

  interface Factory<T extends Constraint> {
    T create();
  }
}
