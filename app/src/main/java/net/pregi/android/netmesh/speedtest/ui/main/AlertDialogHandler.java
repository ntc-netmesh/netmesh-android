package net.pregi.android.netmesh.speedtest.ui.main;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;

import net.pregi.android.netmesh.R;

public abstract class AlertDialogHandler {
    private View dialogView;
    private Dialog dialog;

    protected final Activity activity;
    public Activity getActivity() {
        return activity;
    }

    public void onBeforeShow() {

    }
    public void onAfterShow(Dialog dialog) {

    }
    public final void show() {
        onBeforeShow();
        dialog.show();
        onAfterShow(dialog);
    }

    /** <p>Called on construction when the dialog view is set, whether by providing a view object
     * or by inflation, allowing for initializations.</p>
     */
    protected abstract void onSetView(View dialogView);


    /** <p>Called on construction when the dialog builder is being set. Common traits have
     * already been set by this point, but builder.create() has not been called yet.</p>
     */
    protected abstract void onBuild(AlertDialog.Builder builder);

    /** <p>Called on construction when the dialog has been created, but not yet shown.</p>
     *
     * <p>Some of the dialog's listeners, such as setOnShowListener, can be set here.</p>
     *
     * @param dialog
     */
    protected void onCreateDialog(Dialog dialog) {

    }

    private AlertDialogHandler(Activity activity) {
        this.activity = activity;
    }
    public AlertDialogHandler(Activity activity, int titleId, int layoutId) {
        this(activity);

        dialogView = View.inflate(new ContextThemeWrapper(activity, R.style.AppTheme), layoutId, null);
        onSetView(dialogView);

        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.AppTheme))
                .setTitle(titleId)
                .setView(dialogView);
        onBuild(builder);
        dialog = builder.create();
        onCreateDialog(dialog);
    }
}
