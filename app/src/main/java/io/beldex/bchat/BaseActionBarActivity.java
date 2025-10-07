package io.beldex.bchat;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.beldex.libbchat.utilities.TextSecurePreferences;
import com.beldex.libbchat.utilities.dynamiclanguage.DynamicLanguageActivityHelper;
import com.beldex.libbchat.utilities.dynamiclanguage.DynamicLanguageContextWrapper;

import io.beldex.bchat.R;
import timber.log.Timber;

public abstract class BaseActionBarActivity extends AppCompatActivity {
  private static final String TAG = BaseActionBarActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setHomeAsUpIndicator(R.drawable.ic_back_arrow);
    }

    super.onCreate(savedInstanceState);
  }

  @Override
  protected void onResume() {
    super.onResume();
    DynamicLanguageActivityHelper.recreateIfNotInCorrectLanguage(this, TextSecurePreferences.getLanguage(this));
    String name = getResources().getString(R.string.app_name);
    Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_foreground);
    int color = getResources().getColor(R.color.app_icon_background);
    //setTaskDescription(new ActivityManager.TaskDescription(name, icon, color));
  }

  @Override
  public boolean onSupportNavigateUp() {
    if (super.onSupportNavigateUp()) return true;

    onBackPressed();
    return true;
  }

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(DynamicLanguageContextWrapper.updateContext(newBase, TextSecurePreferences.getLanguage(newBase)));
  }


  //New Line
  ProgressDialog progressDialog = null;

  private static class SimpleProgressDialog extends ProgressDialog {

    SimpleProgressDialog(Context context, int msgId) {
      super(context);
      setCancelable(false);
      setMessage(context.getString(msgId));
    }

    @Override
    public void onBackPressed() {
      // prevent back button
    }
  }

  public void showProgressDialog(int msgId, long delayMillis) {
    dismissProgressDialog(); // just in case
    progressDialog = new BaseActionBarActivity.SimpleProgressDialog(BaseActionBarActivity.this, msgId);
    if (delayMillis > 0) {
      Handler handler = new Handler();
      handler.postDelayed(new Runnable() {
        public void run() {
          if (progressDialog != null) progressDialog.show();
        }
      }, delayMillis);
    } else {
      progressDialog.show();
    }
  }

  public void dismissProgressDialog() {
    if (progressDialog == null) return; // nothing to do
        /*if (progressDialog instanceof Ledger.Listener) {
            Ledger.unsetListener((Ledger.Listener) progressDialog);
        }*/
    if (progressDialog.isShowing()) {
      progressDialog.dismiss();
    }
    progressDialog = null;
  }

  static final int RELEASE_WAKE_LOCK_DELAY = 5000; // millisconds

  private PowerManager.WakeLock wl = null;

  public void acquireWakeLock() {
    if ((wl != null) && wl.isHeld()) return;
    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    this.wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, getString(R.string.app_name));
    try {
      wl.acquire();
      Timber.d("WakeLock acquired");
    } catch (SecurityException ex) {
      Timber.w("WakeLock NOT acquired: %s", ex.getLocalizedMessage());
      wl = null;
    }
  }

  public void releaseWakeLock(int delayMillis) {
    Handler handler = new Handler(Looper.getMainLooper());
    Log.d("Battery","handler running");
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        releaseWakeLock();
      }
    }, delayMillis);
  }

  void releaseWakeLock() {
    if ((wl == null) || !wl.isHeld()) return;
    wl.release();
    wl = null;
    Timber.d("WakeLock released");
  }
}
