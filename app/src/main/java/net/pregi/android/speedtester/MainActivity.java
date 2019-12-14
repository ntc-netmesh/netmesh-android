package net.pregi.android.speedtester;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.pregi.android.speedtester.R;

import net.pregi.android.speedtester.speedtest.SpeedtestGUIMainActivity;
import net.pregi.android.speedtester.speedtestweb.SpeedtestWebMainActivity;
import net.pregi.android.speedtester.speedtestweb.SpeedtestWebOoklaMainActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /*
        NetworkProperties props = NetworkProperties.getProperties(this);
        System.out.println("Props: "+props.getNetworkOperatorName()+", "+props.getActiveNetworkSubtypeName()+", "+props.getActiveNetworkTypeName());

        TelephonyManager tm = ((TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE));
        try {
            List<CellInfo> cellInfo = tm.getAllCellInfo();

            System.out.println("CellInfo Count: "+cellInfo.size());
            for (CellInfo ci : cellInfo) {
                System.out.println("=============");
                System.out.println("CellInfo Type: "+ci.getClass().getSimpleName());
                System.out.println("isRegistered: "+ci.isRegistered());
                if (ci instanceof CellInfoCdma) {
                    CellInfoCdma cdma = (CellInfoCdma)ci;

                    CellIdentityCdma id = cdma.getCellIdentity();
                }
            }
        } catch (SecurityException e) {
            System.out.println("Cannot secure permission.");
        }*/
    }
    public void startSpeedtestGUIActivity_Ookla(View view) {
        Intent i = new Intent(this, SpeedtestGUIMainActivity.class);
        i.putExtra("provider", "ookla");
        startActivity(i);
    }

    public void startSpeedtestGUIActivity_ASTI(View view) {
        Intent i = new Intent(this, SpeedtestGUIMainActivity.class);
        i.putExtra("provider", "asti");
        startActivity(i);
    }

    public void startSpeedtestWebActivity_Ookla(View view) {
        startActivity(new Intent(this, SpeedtestWebMainActivity.class));
    }

    public void startSpeedtestWebOoklaActivity(View view) {
        startActivity(new Intent(this, SpeedtestWebOoklaMainActivity.class));
    }
}
