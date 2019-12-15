package net.pregi.android.speedtester.speedtest.ui.main;

import android.app.Dialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.pregi.android.speedtester.R;

import net.pregi.android.form.FormValidation;
import net.pregi.android.form.Specs;
import net.pregi.android.speedtester.speedtest.process.SpeedtestTools;
import net.pregi.networking.speedtest.NetworkTestingOptions;
import net.pregi.networking.speedtest.provider.SpeedtestProvider;
import net.pregi.networking.speedtest.provider.SpeedtestProvider.Constraint;

import java.util.regex.Pattern;

public class SpeedtestSettingsDialogHandler extends AlertDialogHandler {
    public interface Listener {
        public NetworkTestingOptions getSpeedtestSettings();
        public void setSpeedtestSettings(NetworkTestingOptions value);
    }

    private static final Pattern NONNEGATIVE_INTEGER_PARSABLE_PATTERN = Pattern.compile("^\\d+$");

    private SpeedtestTools speedtestTools;
    /** <p>Set the SpeedtestTools object; the settings form will query the tools object to determine
     * the limits of its inputs.</p>
     */
    public SpeedtestSettingsDialogHandler setSpeedtestTools(SpeedtestTools value) {
        speedtestTools = value;
        return this;
    }
    private int getConstraintInt(Constraint prop, int defaultValue) {
        Object out = speedtestTools.getConstraint(prop);
        if (out instanceof Number) {
            return ((Number) out).intValue();
        }
        return defaultValue;
    }
    private long getConstraintLong(Constraint prop, long defaultValue) {
        Object out = speedtestTools.getConstraint(prop);
        if (out instanceof Number) {
            return ((Number) out).longValue();
        }
        return defaultValue;
    }

    // settings fields
    private View useHttpsLabel, useHttpsNote;
    private CheckBox useHttpsCheckbox;


    private FormValidation formValidation;
    private EditText pingCount;
    private EditText downloadSizeAmount;
    private Spinner downloadSizeScale;
    private EditText downloadCount;
    private EditText uploadSizeAmount;
    private Spinner uploadSizeScale;
    private EditText uploadCount;

    private long getSizeInput(EditText numberValue, Spinner scaleValue) {
        long out = Long.parseLong(numberValue.getText().toString().trim());
        for (int i=0, l=scaleValue.getSelectedItemPosition(); i<l ; i++) {
            out *= 1000;
        }
        return out;
    }


    public void onBeforeShow() {
        // Check if the form needs some constraint checking or updating.
        if (speedtestTools != null) {
            if (Boolean.FALSE.equals(speedtestTools.getConstraint(Constraint.HTTPS_TOGGLEABLE))) {
                useHttpsLabel.setVisibility(View.GONE);
                useHttpsCheckbox.setVisibility(View.GONE);
                useHttpsNote.setVisibility(View.GONE);
            } else {
                useHttpsLabel.setVisibility(View.VISIBLE);
                useHttpsCheckbox.setVisibility(View.VISIBLE);
                useHttpsNote.setVisibility(View.VISIBLE);
            }
        }

        NetworkTestingOptions options = ((Listener)activity).getSpeedtestSettings();
        if (options == null) {
            options = new NetworkTestingOptions();
        }

        useHttpsCheckbox.setChecked(options.getUseHttps());
        pingCount.setText(Integer.toString(options.getPingCount()));

        long downloadSize = options.getDownloadSize();
        /*
        int downloadScaleIndex = 0, downloadScaleMaxIndex = downloadSizeScale.getAdapter().getCount()-1;
        while (downloadSize>=1000 && downloadSize%1000 == 0 && downloadScaleIndex<downloadScaleMaxIndex) {
            downloadSize /= 1000;
            downloadScaleIndex++;
        }*/
        // Fix scale to MB.
        for (int i=0;i<2;i++) {
            downloadSize /= 1000;
        }
        downloadSizeAmount.setText(Long.toString(Math.max(downloadSize, 1)));
        downloadSizeScale.setSelection(2);
        downloadSizeScale.setEnabled(false);

        downloadCount.setText(Integer.toString(options.getDownloadCount()));

        long uploadSize = options.getUploadSize();
        /*
        int uploadScaleIndex = 0, uploadScaleMaxIndex = uploadSizeScale.getAdapter().getCount()-1;
        while (uploadSize>=1000 && uploadSize%1000 == 0 && uploadScaleIndex<downloadScaleMaxIndex) {
            uploadSize /= 1000;
            uploadScaleIndex++;
        }*/
        // Fix scale to MB.
        for (int i=0;i<2;i++) {
            uploadSize /= 1000;
        }
        uploadSizeAmount.setText(Long.toString(Math.max(uploadSize, 1)));
        uploadSizeScale.setSelection(2);
        uploadSizeScale.setEnabled(false);
        uploadCount.setText(Integer.toString(options.getUploadCount()));

        ///////
        // Form validation.
        formValidation = new FormValidation();
        formValidation.addInt(pingCount, true, null)
                .between(getConstraintInt(Constraint.PING_COUNT_MIN, 0), 10);
        formValidation.addInt(downloadCount, true, null)
                .between(getConstraintInt(Constraint.DOWNLOAD_COUNT_MIN, 0), 10);
        formValidation.addInt(uploadCount, true, null)
                .between(getConstraintInt(Constraint.UPLOAD_COUNT_MIN, 0), 10);
        // TODO: write a custom Specs that takes into account down/uploadSizeScale
        //       so we can properly check against Constraint.DOWN/UPLOAD_SIZE_MIN;
        formValidation.addLong(downloadSizeAmount, true, null)
                .between(1, 100);
        formValidation.addLong(uploadSizeAmount, true, null)
                .between(1, 100);
        formValidation.validate(true);
    }

    @Override
    protected void onSetView(View dialogView) {
        useHttpsLabel = dialogView.findViewById(R.id.label_usehttps);
        useHttpsNote = dialogView.findViewById(R.id.label_usehttps_note_ping);
        useHttpsCheckbox = dialogView.findViewById(R.id.checkbox_usehttps);

        pingCount = dialogView.findViewById(R.id.input_pingcount);
        downloadSizeAmount = dialogView.findViewById(R.id.input_downloadsize_amount);
        downloadSizeScale = dialogView.findViewById(R.id.input_downloadsize_scale);
        downloadCount = dialogView.findViewById(R.id.input_downloadcount);
        uploadSizeAmount = dialogView.findViewById(R.id.input_uploadsize_amount);
        uploadSizeScale = dialogView.findViewById(R.id.input_uploadsize_scale);
        uploadCount = dialogView.findViewById(R.id.input_uploadcount);
    }

    @Override
    protected void onBuild(AlertDialog.Builder builder) {
        builder.setPositiveButton(R.string.label_save, null)
            .setNegativeButton(R.string.label_cancel, null);
    }

    @Override
    protected void onCreateDialog(final Dialog dialog) {
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (formValidation.validate(false)) {
                            // Save settings.
                            NetworkTestingOptions options = new NetworkTestingOptions(((Listener) SpeedtestSettingsDialogHandler.this.activity).getSpeedtestSettings());

                            options.setUseHttps(useHttpsCheckbox.isChecked());
                            {
                                String input = pingCount.getText().toString().trim();
                                if (NONNEGATIVE_INTEGER_PARSABLE_PATTERN.matcher(input).matches()) {
                                    options.setPingCount(Integer.parseInt(input));
                                }
                            }
                            {
                                String input = downloadSizeAmount.getText().toString().trim();
                                if (NONNEGATIVE_INTEGER_PARSABLE_PATTERN.matcher(input).matches()) {
                                    options.setDownloadSize(getSizeInput(downloadSizeAmount, downloadSizeScale));
                                }
                            }
                            {
                                String input = downloadCount.getText().toString().trim();
                                if (NONNEGATIVE_INTEGER_PARSABLE_PATTERN.matcher(input).matches()) {
                                    options.setDownloadCount(Integer.parseInt(input));
                                }
                            }
                            {
                                String input = uploadSizeAmount.getText().toString().trim();
                                if (NONNEGATIVE_INTEGER_PARSABLE_PATTERN.matcher(input).matches()) {
                                    options.setUploadSize(getSizeInput(uploadSizeAmount, uploadSizeScale));
                                }
                            }
                            {
                                String input = uploadCount.getText().toString().trim();
                                if (NONNEGATIVE_INTEGER_PARSABLE_PATTERN.matcher(input).matches()) {
                                    options.setUploadCount(Integer.parseInt(input));
                                }
                            }

                            ((Listener) SpeedtestSettingsDialogHandler.this.activity).setSpeedtestSettings(options);

                            dialog.dismiss();
                        }
                    }
                });
            }
        });
    }

    /** <p>Creates the dialog. Because it will pre-inflate the dialog view for faster display,
     * this constructor must be called inside an onCreate() method.</p>
     *
     * @param activity
     * @param <T>
     */
    public <T extends AppCompatActivity & Listener> SpeedtestSettingsDialogHandler(T activity) {
        super(activity, R.string.dialog_title_settings, R.layout.dialog_speedtest_options);
    }
}
