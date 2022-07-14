package com.thoughtcrimes.securesms.backup;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.thoughtcrimes.securesms.util.BackupDirSelector;
import com.thoughtcrimes.securesms.util.BackupUtil;
import com.thoughtcrimes.securesms.components.SwitchPreferenceCompat;
import com.beldex.libsignal.utilities.Log;
import com.thoughtcrimes.securesms.util.BackupDirSelector;
import com.thoughtcrimes.securesms.util.BackupUtil;

import com.beldex.libbchat.utilities.Util;

import java.io.IOException;

import io.beldex.bchat.R;

public class BackupDialog {
  private static final String TAG = "BackupDialog";

  public static void showEnableBackupDialog(
          @NonNull Context context,
          @NonNull SwitchPreferenceCompat preference,
          @NonNull BackupDirSelector backupDirSelector) {

    String[] password   = BackupUtil.generateBackupPassphrase();
    String   passwordSt = Util.join(password, "");

    AlertDialog dialog = new AlertDialog.Builder(context)
                                          .setTitle(R.string.BackupDialog_enable_local_backups)
                                          .setView(R.layout.backup_enable_dialog)
                                          .setPositiveButton(R.string.BackupDialog_enable_backups, null)
                                          .setNegativeButton(android.R.string.cancel, null)
                                          .create();

    dialog.setOnShowListener(created -> {
      Button button = ((AlertDialog) created).getButton(AlertDialog.BUTTON_POSITIVE);
      button.setOnClickListener(v -> {
        CheckBox confirmationCheckBox = dialog.findViewById(R.id.confirmation_check);
        if (confirmationCheckBox.isChecked()) {
          backupDirSelector.selectBackupDir(true, uri -> {
            try {
              BackupUtil.enableBackups(context, passwordSt);
            } catch (IOException e) {
              Log.e(TAG, "Failed to activate backups.", e);
              Toast.makeText(context,
                      context.getString(R.string.dialog_backup_activation_failed),
                      Toast.LENGTH_LONG)
                      .show();
              return;
            }

            preference.setChecked(true);
            created.dismiss();
          });
        } else {
          Toast.makeText(context, R.string.BackupDialog_please_acknowledge_your_understanding_by_marking_the_confirmation_check_box, Toast.LENGTH_LONG).show();
        }
      });
    });

    dialog.show();

    CheckBox checkBox = dialog.findViewById(R.id.confirmation_check);
    TextView textView = dialog.findViewById(R.id.confirmation_text);

    ((TextView)dialog.findViewById(R.id.code_first)).setText(password[0]);
    ((TextView)dialog.findViewById(R.id.code_second)).setText(password[1]);
    ((TextView)dialog.findViewById(R.id.code_third)).setText(password[2]);

    ((TextView)dialog.findViewById(R.id.code_fourth)).setText(password[3]);
    ((TextView)dialog.findViewById(R.id.code_fifth)).setText(password[4]);
    ((TextView)dialog.findViewById(R.id.code_sixth)).setText(password[5]);

    textView.setOnClickListener(v -> checkBox.toggle());

    dialog.findViewById(R.id.number_table).setOnClickListener(v -> {
      ((ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("text", passwordSt));
      Toast.makeText(context, R.string.BackupDialog_copied_to_clipboard, Toast.LENGTH_SHORT).show();
    });


  }

  public static void showDisableBackupDialog(@NonNull Context context, @NonNull SwitchPreferenceCompat preference) {
    new AlertDialog.Builder(context)
                   .setTitle(R.string.BackupDialog_delete_backups)
                   .setMessage(R.string.BackupDialog_disable_and_delete_all_local_backups)
                   .setNegativeButton(android.R.string.cancel, null)
                   .setPositiveButton(R.string.BackupDialog_delete_backups_statement, (dialog, which) -> {
                     BackupUtil.disableBackups(context, true);
                     preference.setChecked(false);
                   })
                   .create()
                   .show();
  }
}
