package com.minicreate.adas.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.minicreate.adas.ui.dialog.DialogDataCallback;
import com.minicreate.adas.ui.dialog.OnClickDialogListener;

import com.minicreate.adas.R;
import com.minicreate.adas.utils.LogUtil;


public class VolumeAdjustDialog extends Dialog {

    private final String TAG = "VolumeAdjustDialog";
    private boolean isResource;
    private int contentResId, cancelResId, okResId;
    private String cancelStr, okStr;

    private TextView txt_content;
    private ImageButton btn_cancel;
    private Button btn_ok;
    private TextView tv_name;
    private TextView tv_progress;
    private String title;//标题名称

    private OnClickDialogListener mListener;
    private DialogDataCallback mCallback;
    private boolean enableBackKey = true; // 回退键是否有效
    private boolean enableOutside = true; // 点击对话框外部，对话框是否消失
    private int type;
    private int contentView = 0;       // 布局文件id
    private SeekBar sbar_volume;
    private int last_volume_value;

    public VolumeAdjustDialog(Context context, int contentView, String title,int volume_value,
                             DialogDataCallback listener) {
        super(context, R.style.dialog);
        this.isResource = false;
        this.title = title;
        this.contentView = contentView;
        this.last_volume_value = volume_value;
        this.mCallback = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(contentView);

        if (!enableOutside) {
            setCanceledOnTouchOutside(false);
        }
        if (!enableBackKey) {
            setCancelable(false);
        }
        windowDeploy();
        initView();
    }

    private void initView() {
//		txt_content = (TextView) findViewById(R.id.txt_content);
        btn_cancel = (ImageButton) findViewById(R.id.ib_close);
        btn_ok = (Button) findViewById(R.id.btn_sure);
        tv_name = findViewById(R.id.tv_name);
        tv_progress = findViewById(R.id.tv_progress);
        if (title != null) {
            tv_name.setText(title);
        } else {
            tv_name.setText("");
        }
        sbar_volume = (SeekBar)findViewById(R.id.sb_volume);
        sbar_volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                LogUtil.i("", "onProgressChanged: " + progress);
                tv_progress.setText(String.valueOf(progress));
                }
            }
        );
        sbar_volume.setProgress(last_volume_value);

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (mCallback != null) {
                   // mCallback.onClick(v, false, text.getText().toString().trim());
                }
            }
        });
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (mCallback != null) {
                    mCallback.onClick(v, true,String.valueOf(sbar_volume.getProgress()));
                }
            }
        });
    }

    private Window window = null;

    public void windowDeploy() {
        window = getWindow(); // 得到对话框
        WindowManager.LayoutParams wl = window.getAttributes();
        wl.gravity = Gravity.CENTER;
        WindowManager m = window.getWindowManager();
        Display d = m.getDefaultDisplay(); // 为获取屏幕宽、高
        wl.width = d.getWidth() * 9 / 10;           // dialog所占屏幕的宽度
//		wl.height = d.getHeight();         // dialog所占屏幕的高度，如果沾满，点击不到外面的窗口，就无法消失
        window.setAttributes(wl);
    }



}
