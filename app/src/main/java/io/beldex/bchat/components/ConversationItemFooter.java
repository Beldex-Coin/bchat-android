package io.beldex.bchat.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.beldex.libbchat.mnode.MnodeAPI;
import io.beldex.bchat.ApplicationContext;
import io.beldex.bchat.database.model.MessageRecord;
import io.beldex.bchat.service.ExpiringMessageManager;
import io.beldex.bchat.util.DateUtils;
import io.beldex.bchat.ApplicationContext;
import io.beldex.bchat.conversation.v2.components.ExpirationTimerView;
import io.beldex.bchat.database.model.MessageRecord;
import io.beldex.bchat.dependencies.DatabaseComponent;
import io.beldex.bchat.service.ExpiringMessageManager;
import io.beldex.bchat.util.DateUtils;

import java.util.Locale;

import io.beldex.bchat.R;

public class ConversationItemFooter extends LinearLayout {

  private TextView            dateView;
  private ExpirationTimerView timerView;
  private ImageView           insecureIndicatorView;
  private DeliveryStatusView  deliveryStatusView;

  public ConversationItemFooter(Context context) {
    super(context);
    init(null);
  }

  public ConversationItemFooter(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    init(attrs);
  }

  public ConversationItemFooter(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(attrs);
  }

  private void init(@Nullable AttributeSet attrs) {
    inflate(getContext(), R.layout.conversation_item_footer, this);

    dateView              = findViewById(R.id.footer_date);
    timerView             = findViewById(R.id.footer_expiration_timer);
    insecureIndicatorView = findViewById(R.id.footer_insecure_indicator);
    deliveryStatusView    = findViewById(R.id.footer_delivery_status);

    if (attrs != null) {
      TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.ConversationItemFooter, 0, 0);
      setTextColor(typedArray.getInt(R.styleable.ConversationItemFooter_footer_text_color, getResources().getColor(R.color.core_white)));
      setIconColor(typedArray.getInt(R.styleable.ConversationItemFooter_footer_icon_color, getResources().getColor(R.color.core_white)));
      typedArray.recycle();
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    timerView.stopAnimation();
  }

  public void setMessageRecord(@NonNull MessageRecord messageRecord, @NonNull Locale locale) {
    presentDate(messageRecord, locale);
    presentTimer(messageRecord);
    presentInsecureIndicator(messageRecord);
    presentDeliveryStatus(messageRecord);
  }

  public void setTextColor(int color) {
    dateView.setTextColor(color);
  }

  public void setIconColor(int color) {
    timerView.setColorFilter(color);
    insecureIndicatorView.setColorFilter(color);
    deliveryStatusView.setTint(color);
  }

  private void presentDate(@NonNull MessageRecord messageRecord, @NonNull Locale locale) {
    dateView.forceLayout();

    if (messageRecord.isFailed()) {
      dateView.setText(R.string.ConversationItem_error_not_delivered);
    } else {
      dateView.setText(DateUtils.getExtendedRelativeTimeSpanString(getContext(), locale, messageRecord.getTimestamp()));
    }
  }

  @SuppressLint("StaticFieldLeak")
  private void presentTimer(@NonNull final MessageRecord messageRecord) {
    if (messageRecord.getExpiresIn() > 0 && !messageRecord.isPending()) {
      this.timerView.setVisibility(View.VISIBLE);
      this.timerView.setPercentComplete(0);

      if (messageRecord.getExpireStarted() > 0) {
        this.timerView.setExpirationTime(messageRecord.getExpireStarted(),
                                         messageRecord.getExpiresIn());
        this.timerView.startAnimation();

        if (messageRecord.getExpireStarted() + messageRecord.getExpiresIn() <= MnodeAPI.getNowWithOffset()) {
          ApplicationContext.getInstance(getContext()).getExpiringMessageManager().checkSchedule();
        }
      } else if (!messageRecord.isOutgoing() && !messageRecord.isMediaPending()) {
        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... params) {
            ExpiringMessageManager expirationManager = ApplicationContext.getInstance(getContext()).getExpiringMessageManager();
            long                   id                = messageRecord.getId();
            boolean                mms               = messageRecord.isMms();

            if (mms) DatabaseComponent.get(getContext()).mmsDatabase().markExpireStarted(id);
            else     DatabaseComponent.get(getContext()).smsDatabase().markExpireStarted(id);

            expirationManager.scheduleDeletion(id, mms, messageRecord.getExpiresIn());
            return null;
          }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      }
    } else {
      this.timerView.setVisibility(View.GONE);
    }
  }

  private void presentInsecureIndicator(@NonNull MessageRecord messageRecord) {
    insecureIndicatorView.setVisibility(View.GONE);
  }

  private void presentDeliveryStatus(@NonNull MessageRecord messageRecord) {
    if (!messageRecord.isFailed()) {
      if      (!messageRecord.isOutgoing())  deliveryStatusView.setNone();
      else if (messageRecord.isPending())    deliveryStatusView.setPending();
      else if (messageRecord.isRead())       deliveryStatusView.setRead();
      else if (messageRecord.isDelivered())  deliveryStatusView.setDelivered();
      else                                   deliveryStatusView.setSent();
    } else {
      deliveryStatusView.setNone();
    }
  }
}
