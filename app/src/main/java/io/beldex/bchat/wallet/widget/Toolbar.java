package io.beldex.bchat.wallet.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.appbar.MaterialToolbar;

import io.beldex.bchat.R;
import timber.log.Timber;

public class Toolbar extends MaterialToolbar {
    public interface OnButtonListener {
        void onButton(int type);
    }

    OnButtonListener onButtonListener;

    public void setOnButtonListener(OnButtonListener listener) {
        onButtonListener = listener;
    }

    //TextView toolbarImage;
    TextView toolbarTitle;
    //TextView toolbarSubtitle;
    ImageButton exitButton;
    public ImageView toolBarRescan;
    public ImageView toolBarSettings;

    public Toolbar(Context context) {
        super(context);
        initializeViews(context);
    }

    public Toolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeViews(context);
    }

    public Toolbar(Context context,
                   AttributeSet attrs,
                   int defStyle) {
        super(context, attrs, defStyle);
        initializeViews(context);
    }

    /**
     * Inflates the views in the layout.
     *
     * @param context the current context for the view.
     */
    private void initializeViews(Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_toolbar, this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        //toolbarImage = findViewById(R.id.toolbarImage);

        toolbarTitle = findViewById(R.id.toolbarTitle);
        //toolbarSubtitle = findViewById(R.id.toolbarSubtitle);
        exitButton = findViewById(R.id.exit_button);
        toolBarRescan = findViewById(R.id.toolBarRescan);
        toolBarSettings = findViewById(R.id.toolBarSettings);
        exitButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (onButtonListener != null) {
                    onButtonListener.onButton(buttonType);
                }
            }
        });
    }

    public void setTitle(String title, String subtitle) {
        setTitle(title);
        setSubtitle(subtitle);
    }

    public void setTitle(String title) {
        toolbarTitle.setText(title);
        if (title != null) {
            Timber.d("Set Title if");
            //toolbarImage.setVisibility(View.INVISIBLE);
            toolbarTitle.setVisibility(View.VISIBLE);
            if(title.equals("Receive")||title.equals("Send")||title.equals("Rescan")||title.equals("Scan")){
                toolBarRescan.setVisibility(View.GONE);
                toolBarSettings.setVisibility(View.GONE);
            }else{
                toolBarRescan.setVisibility(View.VISIBLE);
                toolBarSettings.setVisibility(View.VISIBLE);
            }
        } else {
            Timber.d("Set Title else");
            //toolbarImage.setVisibility(View.VISIBLE);
            toolbarTitle.setVisibility(View.INVISIBLE);
        }
    }

   /* public void hiddenRescan(Boolean status){
        if(!status) {
            toolBarRescan.setVisibility(View.GONE);
        }else{
            toolBarRescan.setVisibility(View.VISIBLE);
        }
    }*/

    public final static int BUTTON_NONE = 0;
    public final static int BUTTON_BACK = 1;
    public final static int BUTTON_CLOSE = 2;
    public final static int BUTTON_CANCEL = 3;

    int buttonType = BUTTON_BACK;

    public void setButton(int type) {
        switch (type) {
            case BUTTON_BACK:
                Timber.d("BUTTON_BACK");
                exitButton.setImageResource(R.drawable.ic_arrow_back);
                exitButton.setVisibility(View.VISIBLE);
                break;
            case BUTTON_CLOSE:
                Timber.d("BUTTON_CLOSE");
                exitButton.setImageResource(R.drawable.ic_close);
                exitButton.setVisibility(View.VISIBLE);
                break;
            case BUTTON_CANCEL:
                Timber.d("BUTTON_CANCEL");
                exitButton.setImageResource(R.drawable.ic_close);
                exitButton.setVisibility(View.VISIBLE);
                break;
            case BUTTON_NONE:
            default:
                Timber.d("BUTTON_NONE");
                exitButton.setVisibility(View.INVISIBLE);
        }
        buttonType = type;
    }

    public void setSubtitle(String subtitle) {
        //toolbarSubtitle.setText(subtitle);
        if (subtitle != null) {
            //toolbarSubtitle.setVisibility(View.VISIBLE);
        } else {
            //toolbarSubtitle.setVisibility(View.INVISIBLE);
        }
    }
}