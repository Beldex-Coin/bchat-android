package io.beldex.bchat.service;

import android.app.IntentService;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;

import io.beldex.bchat.util.Rfc5724Uri;
import com.beldex.libbchat.mnode.MnodeAPI;
import io.beldex.bchat.util.Rfc5724Uri;

import io.beldex.bchat.R;

import com.beldex.libbchat.messaging.messages.visible.VisibleMessage;
import com.beldex.libbchat.utilities.Address;
import com.beldex.libsignal.utilities.Log;
import com.beldex.libbchat.messaging.sending_receiving.MessageSender;

import java.net.URISyntaxException;
import java.net.URLDecoder;

public class QuickResponseService extends IntentService {

  private static final String TAG = QuickResponseService.class.getSimpleName();

  public QuickResponseService() {
    super("QuickResponseService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (!TelephonyManager.ACTION_RESPOND_VIA_MESSAGE.equals(intent.getAction())) {
      Log.w(TAG, "Received unknown intent: " + intent.getAction());
      return;
    }

    if (KeyCachingService.isLocked(this)) {
      Log.w(TAG, "Got quick response request when locked...");
      Toast.makeText(this, R.string.QuickResponseService_quick_response_unavailable_when_Signal_is_locked, Toast.LENGTH_LONG).show();
      return;
    }

    try {
      Rfc5724Uri uri        = new Rfc5724Uri(intent.getDataString());
      String     content    = intent.getStringExtra(Intent.EXTRA_TEXT);
      String     number     = uri.getPath();

      if (number.contains("%")){
        number = URLDecoder.decode(number);
      }

      if (!TextUtils.isEmpty(content)) {
        VisibleMessage message = new VisibleMessage();
        message.setText(content);
        message.setSentTimestamp(MnodeAPI.getNowWithOffset());
        MessageSender.send(message, Address.fromExternal(this, number));
      }
    } catch (URISyntaxException e) {
      Toast.makeText(this, R.string.QuickResponseService_problem_sending_message, Toast.LENGTH_LONG).show();
      Log.w(TAG, e);
    }
  }
}
