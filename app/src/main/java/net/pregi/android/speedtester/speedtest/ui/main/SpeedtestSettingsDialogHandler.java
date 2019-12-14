package net.pregi.android.speedtester.speedtest.ui.main;

import android.content.DialogInterface;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.pregi.android.speedtester.R;
import net.pregi.networking.speedtest.NetworkTestingOptions;

import java.util.regex.Pattern;

public class SpeedtestSettingsDialogHandler extends AlertDialogHandler {
    public interface Listener {
        public NetworkTestingOptions getSpeedtestSettings();
        public void setSpeedtestSettings(NetworkTestingOptions value);
    }

    private static final Pattern NONNEGATIVE_INTEGER_PARSABLE_PATTERN = Pattern.compile("^\\d+$");

    // settings fields
    private CheckBox useHttpsCheckbox;
    private EditText pingCount;
    private EditText downloadSizeAmount;
    private Spinner downloadSizeScale;
    private EditText downloadCount;
    private EditText uploadSizeAmount;
    private Spinner uploadSizeScale;
    private EditText uploadCount;

    public void onBeforeShow() {
        NetworkTestingOptions options = ((Listener)activity).getSpeedtestSettings();
        if (options == null) {
            options = new NetworkTestingOptions();
        }

        useHttpsCheckbox.setChecked(options.getUseHttps());
        pingCount.setText(Integer.toString(options.getPingCount()));

        long downloadSize = options.getDownloadSize();
        int downloadScaleIndex = 0, downloadScaleMaxIndex = downloadSizeScale.getAdapter().getCount()-1;
        while (downloadSize>=1000 && downloadSize%1000 == 0 && downloadScaleIndex<downloadScaleMaxIndex) {
            downloadSize /= 1000;
            downloadScaleIndex++;
        }
        downloadSizeAmount.setText(Long.toString(downloadSize));
        downloadSizeScale.setSelection(downloadScaleIndex);
        downloadCount.setText(Integer.toString(options.getDownloadCount()));

        long uploadSize = options.getUploadSize();
        int uploadScaleIndex = 0, uploadScaleMaxIndex = uploadSizeScale.getAdapter().getCount()-1;
        while (uploadSize>=1000 && uploadSize%1000 == 0 && uploadScaleIndex<downloadScaleMaxIndex) {
            uploadSize /= 1000;
            uploadScaleIndex++;
        }
        uploadSizeAmount.setText(Long.toString(uploadSize));
        uploadSizeScale.setSelection(uploadScaleIndex);
        uploadCount.setText(Integer.toString(options.getUploadCount()));
    }

    @Override
    protected void onSetView(View dialogView) {
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
        builder.setPositiveButton(R.string.label_save, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Save settings.
                NetworkTestingOptions options = new NetworkTestingOptions(((Listener)SpeedtestSettingsDialogHandler.this.activity).getSpeedtestSettings());

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
                        long downloadSize = Long.parseLong(input);
                        for (int i=0, l=downloadSizeScale.getSelectedItemPosition(); i<l ; i++) {
                            downloadSize *= 1000;
                        }
                        options.setDownloadSize(downloadSize);
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
                        long uploadSize = Long.parseLong(input);
                        for (int i=0, l=uploadSizeScale.getSelectedItemPosition(); i<l ; i++) {
                            uploadSize *= 1000;
                        }
                        options.setUploadSize(uploadSize);
                    }
                }
                {
                    String input = uploadCount.getText().toString().trim();
                    if (NONNEGATIVE_INTEGER_PARSABLE_PATTERN.matcher(input).matches()) {
                        options.setUploadCount(Integer.parseInt(input));
                    }
                }

                ((Listener)SpeedtestSettingsDialogHandler.this.activity).setSpeedtestSettings(options);
            }
        })
        .setNegativeButton(R.string.label_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Do nothing.
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
