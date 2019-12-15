package net.pregi.android.netmesh.speedtest.process;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import net.pregi.android.netmesh.R;
import net.pregi.android.text.SpanUtils;

public class CheckPermissionsProcessB {
    public interface OnLogListener {
        public void onLog(CharSequence log);
    }

    private static class RequestPermissionSpan extends ClickableSpan {
        private Context context;
        private String permissionName;

        @Override
        public void onClick(@NonNull View widget) {
            if (context instanceof Activity) {
                ActivityCompat.requestPermissions((Activity)context,
                        new String[] { permissionName },
                        context.getResources().getInteger(R.integer.permissionrequest_checkpermissionprocess));
            }
        }

        private RequestPermissionSpan(Context context, String permissionName) {
            this.context = context;
            this.permissionName = permissionName;
        }
    }

    public static boolean isEveryPermissionGrantedElseRequest(Activity activity, String[] permissionNames, int requestCode) {
        boolean allGranted = true;
        for (String s : permissionNames) {
            if (ContextCompat.checkSelfPermission(activity, s) == PackageManager.PERMISSION_DENIED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(activity, permissionNames,requestCode);
        }

        return allGranted;
    }

    public static final int PROP_OUTPUTLOG = 2;

    private static final int COLOR_BAD = Color.RED;
    private static final int COLOR_GOOD = 0xFF006600;

    private static final String[] PERMISSION_NAMES = new String[] {
            // Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final int[] PERMISSION_TITLES_R = new int[] {
            // R.string.checkpermissions_title_read_phone_state,
            R.string.checkpermissions_title_access_fine_location
    };
    private static final int[] PERMISSION_DESCS_R = new int[] {
            // R.string.checkpermissions_desc_read_phone_state,
            R.string.checkpermissions_desc_access_fine_location
    };

    public static void checkPermissions(Context context, OnLogListener onLogListener) {
        Resources r = context.getResources();
        onLogListener.onLog(r.getText(R.string.speedtest_html_checkpermissions_loading));

        SpannableStringBuilder output =  new SpannableStringBuilder(r.getText(R.string.speedtest_html_checkpermissions_main));
        CharSequence permissionGranted = r.getString(R.string.checkpermissions_granted);
        CharSequence permissionDenied = r.getString(R.string.checkpermissions_denied);
        CharSequence permissionGrant = r.getString(R.string.checkpermissions_grant);

        output.append("\n\n");

        // Iterate through each permission we need.
        // Print the title, its status, and a description as to why it's needed.
        for (int i=0,l=PERMISSION_NAMES.length; i<l;i++) {
            String permission = PERMISSION_NAMES[i];

            output.append(SpanUtils.bold(r.getString(PERMISSION_TITLES_R[i])));
            output.append(": ");

            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                // Granted. Good to go.
                output.append(SpanUtils.colored(permissionGranted, COLOR_GOOD));
                output.append(".");
            } else {
                // Not granted. Need the permission.
                output.append(SpanUtils.colored(permissionDenied, COLOR_BAD));
                output.append(" (");
                output.append(SpanUtils.clickable(permissionGrant, new RequestPermissionSpan(context, permission)));
                output.append(")");
            }
            output.append("\n");
            output.append(SpanUtils.small(r.getString(PERMISSION_DESCS_R[i])));
            output.append("\n\n");
        }

        onLogListener.onLog(output);
    }
}
