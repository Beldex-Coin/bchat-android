package io.beldex.bchat.util;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.system.ErrnoException;
import android.system.Os;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

import io.beldex.bchat.BuildConfig;
import io.beldex.bchat.R;
import timber.log.Timber;

public class Helper {
    static public final String NOCRAZYPASS_FLAGFILE = ".nocrazypass";

    static public final boolean SHOW_EXCHANGERATES = true;
    static public final boolean ALLOW_SHIFT = true;

    static public int DISPLAY_DIGITS_INFO = 5;


    static public File getStorage(Context context, String folderName) {
        File dir = new File(context.getFilesDir(), folderName);
        if (!dir.exists()) {
            Timber.i("Creating %s", dir.getAbsolutePath());
            dir.mkdirs(); // try to make it
        }
        if (!dir.isDirectory()) {
            String msg = "Directory " + dir.getAbsolutePath() + " does not exist.";
            Timber.e(msg);
            throw new IllegalStateException(msg);
        }
        return dir;
    }

    static public final int PERMISSIONS_REQUEST_CAMERA = 7;
    static public  final int PERMISSION_REQUEST_PHONE_STATE = 1;

    static public boolean getCameraPermission(Activity context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_DENIED) {
                Timber.w("Permission denied for CAMERA - requesting it");
                String[] permissions = {Manifest.permission.CAMERA};
                context.requestPermissions(permissions, PERMISSIONS_REQUEST_CAMERA);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    static public void showKeyboard(Activity act) {
        InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
        final View focus = act.getCurrentFocus();
        if (focus != null)
            imm.showSoftInput(focus, InputMethodManager.SHOW_IMPLICIT);
    }

    static public void hideKeyboard(Activity act) {
        if (act == null) return;
        if (act.getCurrentFocus() == null) {
            act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        } else {
            InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow((null == act.getCurrentFocus()) ? null : act.getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    static public void showKeyboard(Dialog dialog) {
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    static public void hideKeyboardAlways(Activity act) {
        act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    static public Bitmap getBitmap(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable instanceof BitmapDrawable) {
            return BitmapFactory.decodeResource(context.getResources(), drawableId);
        } else if (drawable instanceof VectorDrawable) {
            return getBitmap((VectorDrawable) drawable);
        } else {
            throw new IllegalArgumentException("unsupported drawable type");
        }
    }

    static private Bitmap getBitmap(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    static final int HTTP_TIMEOUT = 5000;

    static public String getUrl(String httpsUrl) {
        HttpsURLConnection urlConnection = null;
        try {
            URL url = new URL(httpsUrl);
            urlConnection = (HttpsURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(HTTP_TIMEOUT);
            urlConnection.setReadTimeout(HTTP_TIMEOUT);
            InputStreamReader in = new InputStreamReader(urlConnection.getInputStream());
            StringBuffer sb = new StringBuffer();
            final int BUFFER_SIZE = 512;
            char[] buffer = new char[BUFFER_SIZE];
            int length = in.read(buffer, 0, BUFFER_SIZE);
            while (length >= 0) {
                sb.append(buffer, 0, length);
                length = in.read(buffer, 0, BUFFER_SIZE);
            }
            return sb.toString();
        } catch (SocketTimeoutException ex) {
            Timber.w("C %s", ex.getLocalizedMessage());
        } catch (MalformedURLException ex) {
            Timber.e("A %s", ex.getLocalizedMessage());
        } catch (IOException ex) {
            Timber.e("B %s", ex.getLocalizedMessage());
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return null;
    }

    static public void clipBoardCopy(Context context, String label, String text) {
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboardManager.setPrimaryClip(clip);
    }

    static public String getClipBoardText(Context context) {
        final ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        try {
            if (clipboardManager.hasPrimaryClip()
                    && clipboardManager.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                final ClipData.Item item = clipboardManager.getPrimaryClip().getItemAt(0);
                return item.getText().toString();
            }
        } catch (NullPointerException ex) {
            // if we have don't find a text in the clipboard
            return null;
        }
        return null;
    }

    static private Animation ShakeAnimation;

    //Important
   static public Animation getShakeAnimation(Context context) {
        if (ShakeAnimation == null) {
            synchronized (Helper.class) {
                if (ShakeAnimation == null) {
                    ShakeAnimation = AnimationUtils.loadAnimation(context, R.anim.shake);
                }
            }
        }
        return ShakeAnimation;
    }

    private final static char[] HexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] data) {
        if ((data != null) && (data.length > 0))
            return String.format("%0" + (data.length * 2) + "X", new BigInteger(1, data));
        else return "";
    }

    public static byte[] hexToBytes(String hex) {
        final int len = hex.length();
        final byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    static AlertDialog openDialog = null; // for preventing opening of multiple dialogs
    static AsyncTask<Void, Void, Boolean> passwordTask = null;

    //Important
    /*static public void promptPassword(final Context context, final String wallet, boolean fingerprintDisabled, final PasswordAction action) {
        if (openDialog != null) return; // we are already asking for password
        LayoutInflater li = LayoutInflater.from(context);
        final View promptsView = li.inflate(R.layout.prompt_password, null);

        //AlertDialog.Builder alertDialogBuilder = new MaterialAlertDialogBuilder(context);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context,R.style.backgroundColor);
        alertDialogBuilder.setView(promptsView);

        final TextInputLayout etPassword = promptsView.findViewById(R.id.etPassword);
        etPassword.setHint(context.getString(R.string.prompt_password, wallet));

        final TextView tvOpenPrompt = promptsView.findViewById(R.id.tvOpenPrompt);
        final Drawable icFingerprint = context.getDrawable(R.drawable.ic_fingerprint);
        final Drawable icError = context.getDrawable(R.drawable.ic_error_red_36dp);
        final Drawable icInfo = context.getDrawable(R.drawable.ic_info_green_36dp);

        final boolean fingerprintAuthCheck = FingerprintHelper.isFingerPassValid(context, wallet);

        final boolean fingerprintAuthAllowed = !fingerprintDisabled && fingerprintAuthCheck;
        final CancellationSignal cancelSignal = new CancellationSignal();

        final AtomicBoolean incorrectSavedPass = new AtomicBoolean(false);

        class PasswordTask extends AsyncTask<Void, Void, Boolean> {
            private String pass;
            private boolean fingerprintUsed;

            PasswordTask(String pass, boolean fingerprintUsed) {
                this.pass = pass;
                this.fingerprintUsed = fingerprintUsed;
            }

            @Override
            protected void onPreExecute() {
                tvOpenPrompt.setCompoundDrawablesRelativeWithIntrinsicBounds(icInfo, null, null, null);
                tvOpenPrompt.setText(context.getText(R.string.prompt_open_wallet));
                tvOpenPrompt.setVisibility(View.VISIBLE);
            }

            @Override
            protected Boolean doInBackground(Void... unused) {
                return processPasswordEntry(context, wallet, pass, fingerprintUsed, action);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    Helper.hideKeyboardAlways((Activity) context);
                    cancelSignal.cancel();
                    openDialog.dismiss();
                    openDialog = null;
                } else {
                    if (fingerprintUsed) {
                        incorrectSavedPass.set(true);
                        tvOpenPrompt.setCompoundDrawablesRelativeWithIntrinsicBounds(icError, null, null, null);
                        tvOpenPrompt.setText(context.getText(R.string.bad_saved_password));
                    } else {
                        if (!fingerprintAuthAllowed) {
                            tvOpenPrompt.setVisibility(View.GONE);
                        } else if (incorrectSavedPass.get()) {
                            tvOpenPrompt.setCompoundDrawablesRelativeWithIntrinsicBounds(icError, null, null, null);
                            tvOpenPrompt.setText(context.getText(R.string.bad_password));
                        } else {
                            tvOpenPrompt.setCompoundDrawablesRelativeWithIntrinsicBounds(icFingerprint, null, null, null);
                            tvOpenPrompt.setText(context.getText(R.string.prompt_fingerprint_auth));
                        }
                        etPassword.setError(context.getString(R.string.bad_password));
                    }
                }
                passwordTask = null;
            }
        }

        etPassword.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (etPassword.getError() != null) {
                    etPassword.setError(null);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.ok), null)
                .setNegativeButton(context.getString(R.string.cancel),
                        (dialog, id) -> {
                            action.fail(wallet);
                            Helper.hideKeyboardAlways((Activity) context);
                            cancelSignal.cancel();
                            if (passwordTask != null) {
                                passwordTask.cancel(true);
                                passwordTask = null;
                            }
                            dialog.cancel();
                            openDialog = null;
                        });
        openDialog = alertDialogBuilder.create();

        final FingerprintManager.AuthenticationCallback fingerprintAuthCallback;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            fingerprintAuthCallback = null;
        } else {
            fingerprintAuthCallback = new FingerprintManager.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errMsgId, CharSequence errString) {
                    tvOpenPrompt.setCompoundDrawablesRelativeWithIntrinsicBounds(icError, null, null, null);
                    tvOpenPrompt.setText(errString);
                }

                @Override
                public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                    tvOpenPrompt.setCompoundDrawablesRelativeWithIntrinsicBounds(icError, null, null, null);
                    tvOpenPrompt.setText(helpString);
                }

                @Override
                public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                    try {
                        String userPass = KeyStoreHelper.loadWalletUserPass(context, wallet);
                        if (passwordTask == null) {
                            passwordTask = new PasswordTask(userPass, true);
                            passwordTask.execute();
                        }
                    } catch (KeyStoreHelper.BrokenPasswordStoreException ex) {
                        etPassword.setError(context.getString(R.string.bad_password));
                        // TODO: better error message here - what would it be?
                    }
                }

                @Override
                public void onAuthenticationFailed() {
                    tvOpenPrompt.setCompoundDrawablesRelativeWithIntrinsicBounds(icError, null, null, null);
                    tvOpenPrompt.setText(context.getString(R.string.bad_fingerprint));
                }
            };
        }

        openDialog.setOnShowListener(dialog -> {
            if (fingerprintAuthAllowed && fingerprintAuthCallback != null) {
                tvOpenPrompt.setCompoundDrawablesRelativeWithIntrinsicBounds(icFingerprint, null, null, null);
                tvOpenPrompt.setText(context.getText(R.string.prompt_fingerprint_auth));
                tvOpenPrompt.setVisibility(View.VISIBLE);
                FingerprintHelper.authenticate(context, cancelSignal, fingerprintAuthCallback);
            } else {
                etPassword.requestFocus();
            }
            Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String pass = etPassword.getEditText().getText().toString();
                if (passwordTask == null) {
                    passwordTask = new PasswordTask(pass, false);
                    passwordTask.execute();
                }
            });
        });

        // accept keyboard "ok"
        etPassword.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) && (event.getAction() == KeyEvent.ACTION_DOWN))
                    || (actionId == EditorInfo.IME_ACTION_DONE)) {
                String pass = etPassword.getEditText().getText().toString();
                if (passwordTask == null) {
                    passwordTask = new PasswordTask(pass, false);
                    passwordTask.execute();
                }
                return true;
            }
            return false;
        });

        if (Helper.preventScreenshot()) {
            openDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        Helper.showKeyboard(openDialog);
        openDialog.show();
    }*/

    public interface PasswordAction {
        void act(String walletName, String password, boolean fingerprintUsed);

        void fail(String walletName);
    }

    public interface Action {
        boolean run();
    }

    static public boolean runWithNetwork(Action action) {
        StrictMode.ThreadPolicy currentPolicy = StrictMode.getThreadPolicy();
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);
        try {
            return action.run();
        } finally {
            StrictMode.setThreadPolicy(currentPolicy);
        }
    }

    static public boolean preventScreenshot() {
        return !(BuildConfig.DEBUG || BuildConfig.FLAVOR.equals("alpha"));
    }
}