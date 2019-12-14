package net.pregi.android.speedtester.speedtest.ui.main;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.pregi.android.speedtester.R;

// I would have called this a DiaLogHandler for the pun, but it may confuse people.
public class LogDialogHandler extends AlertDialogHandler {
    private Activity activity;
    private View dialogView;
    private Dialog dialog;

    private ScrollView scrollView;
    private TextView logView;

    /** <p>Get the log view.</p>
     * <p>This is intended to be used in listeners that are bound to events that update the log.</p>
     * @return
     */
    public TextView getLogView() {
        return logView;
    }

    @Override
    protected void onSetView(View dialogView) {
        scrollView = dialogView.findViewById(R.id.output_log_scroll);

        logView = dialogView.findViewById(R.id.output_log);
        logView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    protected void onBuild(AlertDialog.Builder builder) {
        builder.setNeutralButton(R.string.action_dismiss, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
    }

    public void setText(final CharSequence logText) {
        scrollView.post(
            new Runnable() {
                @Override
                public void run() {
                    logView.setText(logText);
                    scrollView.scrollTo(0, logView.getMeasuredHeight());
                }
            }
        );
    }

    /** <p>Creates the dialog. Because it will pre-inflate the dialog view for faster display,
     * this constructor must be called inside an onCreate() method.</p>
     *
     * @param activity
     * @param <T>
     */
    public <T extends AppCompatActivity> LogDialogHandler(T activity) {
        super(activity, R.string.dialog_title_log, R.layout.dialog_log);
    }

    public <T extends AppCompatActivity> LogDialogHandler(T activity, int titleId) {
        super(activity, titleId, R.layout.dialog_log);
    }
}
