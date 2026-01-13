package io.beldex.bchat.permissions;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.annimon.stream.Stream;
import com.annimon.stream.function.Consumer;
import io.beldex.bchat.util.LRUCache;

import com.beldex.libbchat.utilities.ServiceUtil;

import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.beldex.bchat.R;

public class Permissions {

  private static final Map<Integer, PermissionsRequest> OUTSTANDING = new LRUCache<>(2);

  public static PermissionsBuilder with(@NonNull Activity activity) {
    return new PermissionsBuilder(new ActivityPermissionObject(activity));
  }

  public static PermissionsBuilder with(@NonNull Fragment fragment) {
    return new PermissionsBuilder(new FragmentPermissionObject(fragment));
  }

  public static class PermissionsBuilder {

    private final PermissionObject permissionObject;

    private String[] requestedPermissions;

    private Runnable allGrantedListener;

    private Runnable anyDeniedListener;
    private Runnable anyPermanentlyDeniedListener;
    private Runnable anyResultListener;

    private Consumer<List<String>> someGrantedListener;
    private Consumer<List<String>> someDeniedListener;
    private Consumer<List<String>> somePermanentlyDeniedListener;

    private @DrawableRes int[]  rationalDialogHeader;
    private              String rationaleDialogMessage;

    private              String rationalDialogTitle;

    private int minSdkVersion = 0;
    private int maxSdkVersion = Integer.MAX_VALUE;

    PermissionsBuilder(PermissionObject permissionObject) {
      this.permissionObject = permissionObject;
    }

    public PermissionsBuilder request(String... requestedPermissions) {
      this.requestedPermissions = requestedPermissions;
      return this;
    }

    public PermissionsBuilder withRationaleDialog(@NonNull String message, @NonNull String title, @NonNull @DrawableRes int... headers) {
      this.rationalDialogHeader   = headers;
      this.rationaleDialogMessage = message;
      this.rationalDialogTitle    = title;
      return this;
    }

    public PermissionsBuilder withPermanentDenialDialog(@NonNull String message) {
      return onAnyPermanentlyDenied(new SettingsDialogListener(permissionObject.getContext(), message));
    }

    public PermissionsBuilder onAllGranted(Runnable allGrantedListener) {
      this.allGrantedListener = allGrantedListener;
      return this;
    }

    public PermissionsBuilder onAnyDenied(Runnable anyDeniedListener) {
      this.anyDeniedListener = anyDeniedListener;
      return this;
    }

    @SuppressWarnings("WeakerAccess")
    public PermissionsBuilder onAnyPermanentlyDenied(Runnable anyPermanentlyDeniedListener) {
      this.anyPermanentlyDeniedListener = anyPermanentlyDeniedListener;
      return this;
    }

    public PermissionsBuilder onAnyResult(Runnable anyResultListener) {
      this.anyResultListener = anyResultListener;
      return this;
    }

    public PermissionsBuilder onSomeGranted(Consumer<List<String>> someGrantedListener) {
      this.someGrantedListener = someGrantedListener;
      return this;
    }

    public PermissionsBuilder onSomeDenied(Consumer<List<String>> someDeniedListener) {
      this.someDeniedListener = someDeniedListener;
      return this;
    }

    public PermissionsBuilder onSomePermanentlyDenied(Consumer<List<String>> somePermanentlyDeniedListener) {
      this.somePermanentlyDeniedListener = somePermanentlyDeniedListener;
      return this;
    }

    /**
     * Min Android SDK version to request the permissions for (inclusive).
     */
    public PermissionsBuilder minSdkVersion(int minSdkVersion) {
      this.minSdkVersion = minSdkVersion;
      return this;
    }

    /**
     * Max Android SDK version to request the permissions for (inclusive).
     */
    public PermissionsBuilder maxSdkVersion(int maxSdkVersion) {
      this.maxSdkVersion = maxSdkVersion;
      return this;
    }

    public void execute() {
      PermissionsRequest request = new PermissionsRequest(allGrantedListener, anyDeniedListener, anyPermanentlyDeniedListener, anyResultListener,
                                                          someGrantedListener, someDeniedListener, somePermanentlyDeniedListener);

      boolean isInTargetSDKRange = (Build.VERSION.SDK_INT >= minSdkVersion && Build.VERSION.SDK_INT <= maxSdkVersion);

      if (!isInTargetSDKRange || permissionObject.hasAll(requestedPermissions)) {
        executePreGrantedPermissionsRequest(request);
      } else if (rationaleDialogMessage != null && rationalDialogHeader != null) {
        executePermissionsRequestWithRationale(request);
      } else {
        executePermissionsRequest(request);
      }
    }

    private void executePreGrantedPermissionsRequest(PermissionsRequest request) {
      int[] grantResults = new int[requestedPermissions.length];
      for (int i=0;i<grantResults.length;i++) grantResults[i] = PackageManager.PERMISSION_GRANTED;

      request.onResult(requestedPermissions, grantResults, new boolean[requestedPermissions.length]);
    }

    @SuppressLint("MissingInflatedId")
    @SuppressWarnings("ConstantConditions")
    private void executePermissionsRequestWithRationale(PermissionsRequest request) {
      LayoutInflater factory = LayoutInflater.from(permissionObject.getContext());
      View callPermissionDialogView =
              factory.inflate(R.layout.permission_rationale_dialog, null);
      AlertDialog callPermissionDialog = new AlertDialog.Builder(permissionObject.getContext()).create();
      callPermissionDialog.setView(callPermissionDialogView);
      callPermissionDialogView.<ImageView>findViewById(R.id.permissionImageview).setImageDrawable(ContextCompat.getDrawable(permissionObject.getContext(),rationalDialogHeader[0]));
      if(!Objects.equals(rationaleDialogMessage, permissionObject.getContext().getString(R.string.ConversationActivity_to_send_photos_and_video_allow_signal_access_to_storage))) {
        Log.d("Message-String",""+rationaleDialogMessage+","+permissionObject.getContext().getString(R.string.ConversationActivity_to_send_photos_and_video_allow_signal_access_to_storage));
        callPermissionDialogView.<ImageView>findViewById(R.id.permissionImageview).setColorFilter(ContextCompat.getColor(permissionObject.getContext(), R.color.download_icon));
      }
      callPermissionDialogView.<TextView>findViewById(R.id.permissionDescriptionTextview).setText(rationaleDialogMessage);
      callPermissionDialogView.<TextView>findViewById(R.id.permissionTitleTextview).setText(rationalDialogTitle);
      callPermissionDialogView.<Button>findViewById(R.id.allowDialogBoxButton).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          executePermissionsRequest(request);
          callPermissionDialog.dismiss();
        }
      });
      callPermissionDialogView.<Button>findViewById(R.id.denyDialogBoxButton).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          executeNoPermissionsRequest(request);
          callPermissionDialog.dismiss();
        }
      });
      callPermissionDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
      callPermissionDialog.show();
    }

    private void executePermissionsRequest(PermissionsRequest request) {
      int requestCode = new SecureRandom().nextInt(65434) + 100;

      synchronized (OUTSTANDING) {
        OUTSTANDING.put(requestCode, request);
      }

      for (String permission : requestedPermissions) {
        request.addMapping(permission, permissionObject.shouldShowPermissionRationale(permission));
      }

      permissionObject.requestPermissions(requestCode, requestedPermissions);
    }

    private void executeNoPermissionsRequest(PermissionsRequest request) {
      for (String permission : requestedPermissions) {
        request.addMapping(permission, true);
      }

      String[] permissions  = filterNotGranted(permissionObject.getContext(), requestedPermissions);
      int[]    grantResults = Stream.of(permissions).mapToInt(permission -> PackageManager.PERMISSION_DENIED).toArray();
      boolean[] showDialog   = new boolean[permissions.length];
      Arrays.fill(showDialog, true);

      request.onResult(permissions, grantResults, showDialog);
    }

  }

  private static void requestPermissions(@NonNull Activity activity, int requestCode, String... permissions) {
    ActivityCompat.requestPermissions(activity, filterNotGranted(activity, permissions), requestCode);
  }

  private static void requestPermissions(@NonNull Fragment fragment, int requestCode, String... permissions) {
    fragment.requestPermissions(filterNotGranted(fragment.getContext(), permissions), requestCode);
  }

  private static String[] filterNotGranted(@NonNull Context context, String... permissions) {
    return Stream.of(permissions)
                 .filter(permission -> ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED)
                 .toList()
                 .toArray(new String[0]);
  }

  public static boolean hasAll(@NonNull Context context, String... permissions) {
    return Stream.of(permissions).allMatch(permission -> ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED);

  }

  public static void onRequestPermissionsResult(Fragment fragment, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    onRequestPermissionsResult(new FragmentPermissionObject(fragment), requestCode, permissions, grantResults);
  }

  public static void onRequestPermissionsResult(Activity activity, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    onRequestPermissionsResult(new ActivityPermissionObject(activity), requestCode, permissions, grantResults);
  }

  private static void onRequestPermissionsResult(@NonNull PermissionObject context, int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    PermissionsRequest resultListener;

    synchronized (OUTSTANDING) {
      resultListener = OUTSTANDING.remove(requestCode);
    }

    if (resultListener == null) return;

    boolean[] shouldShowRationaleDialog = new boolean[permissions.length];

    for (int i=0;i<permissions.length;i++) {
      if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
        shouldShowRationaleDialog[i] = context.shouldShowPermissionRationale(permissions[i]);
      }
    }

    resultListener.onResult(permissions, grantResults, shouldShowRationaleDialog);
  }

  private static Intent getApplicationSettingsIntent(@NonNull Context context) {
    Intent intent = new Intent();
    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    Uri uri = Uri.fromParts("package", context.getPackageName(), null);
    intent.setData(uri);

    return intent;
  }

  private abstract static class PermissionObject {

    abstract Context getContext();
    abstract boolean shouldShowPermissionRationale(String permission);
    abstract boolean hasAll(String... permissions);
    abstract void requestPermissions(int requestCode, String... permissions);

    int getWindowWidth() {
      WindowManager  windowManager = ServiceUtil.getWindowManager(getContext());
      Display        display       = windowManager.getDefaultDisplay();
      DisplayMetrics metrics       = new DisplayMetrics();
      display.getMetrics(metrics);

      return metrics.widthPixels;
    }
  }

  private static class ActivityPermissionObject extends PermissionObject {

    private final Activity activity;

    ActivityPermissionObject(@NonNull Activity activity) {
      this.activity = activity;
    }

    @Override
    public Context getContext() {
      return activity;
    }

    @Override
    public boolean shouldShowPermissionRationale(String permission) {
      return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    @Override
    public boolean hasAll(String... permissions) {
      return Permissions.hasAll(activity, permissions);
    }

    @Override
    public void requestPermissions(int requestCode, String... permissions) {
      try {
        Permissions.requestPermissions(activity, requestCode, permissions);
      }catch (IllegalArgumentException ex){
        Log.d("Exception ",ex.getMessage().toString());
      }
    }
  }

  private static class FragmentPermissionObject extends PermissionObject {

    private final Fragment fragment;

    FragmentPermissionObject(@NonNull Fragment fragment) {
      this.fragment = fragment;
    }

    @Override
    public Context getContext() {
      return fragment.getContext();
    }

    @Override
    public boolean shouldShowPermissionRationale(String permission) {
      return fragment.shouldShowRequestPermissionRationale(permission);
    }

    @Override
    public boolean hasAll(String... permissions) {
      return Permissions.hasAll(fragment.getContext(), permissions);
    }

    @Override
    public void requestPermissions(int requestCode, String... permissions) {
      Permissions.requestPermissions(fragment, requestCode, permissions);
    }
  }

  private static class SettingsDialogListener implements Runnable {

    private final WeakReference<Context> context;
    private final String                 message;

    SettingsDialogListener(Context context, String message) {
      this.message = message;
      this.context = new WeakReference<>(context);
    }

    @Override
    public void run() {
      Context context = this.context.get();

      if (context != null) {
        new AlertDialog.Builder(context)
            .setTitle(R.string.Permissions_permission_required)
            .setMessage(message)
            .setPositiveButton(R.string.Permissions_allow, (dialog, which) -> context.startActivity(getApplicationSettingsIntent(context)))
            .setNeutralButton(R.string.Permissions_deny, null)
            .show();
      }
    }
  }
}
