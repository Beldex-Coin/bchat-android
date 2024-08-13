package io.beldex.bchat.preferences.widgets;


import android.content.Context;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import io.beldex.bchat.R;

public class ProgressPreference extends Preference {

  private View        container;
  private TextView    progressText;

  public ProgressPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initialize();
  }

  public ProgressPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  public ProgressPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public ProgressPreference(Context context) {
    super(context);
    initialize();
  }

  private void initialize() {
    setWidgetLayoutResource(R.layout.preference_widget_progress);
  }

  @Override
  public void onBindViewHolder(PreferenceViewHolder view) {
    super.onBindViewHolder(view);

    this.container    = view.findViewById(R.id.container);
    this.progressText = (TextView) view.findViewById(R.id.progress_text);

    this.container.setVisibility(View.GONE);
  }

  public void setProgress(int count) {
    container.setVisibility(View.VISIBLE);
    progressText.setText(getContext().getString(R.string.ProgressPreference_d_messages_so_far, count));
  }

  public void setProgressVisible(boolean visible) {
    container.setVisibility(visible ? View.VISIBLE : View.GONE);
  }

}
