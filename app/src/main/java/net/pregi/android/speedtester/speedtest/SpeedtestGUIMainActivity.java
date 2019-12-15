package net.pregi.android.speedtester.speedtest;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.pregi.android.speedtester.R;
import net.pregi.android.speedtester.speedtest.process.SpeedtestTools;
import net.pregi.android.speedtester.speedtest.process.SpeedtestToolsListener;
import net.pregi.android.speedtester.speedtest.ui.gui.SpeedtestGraphicDisplay;
import net.pregi.android.speedtester.speedtest.ui.main.LogDialogHandler;
import net.pregi.android.speedtester.speedtest.ui.main.ServerListAdapter;
import net.pregi.android.speedtester.speedtest.ui.main.SpeedtestSettingsDialogHandler;
import net.pregi.android.text.SpanUtils;
import net.pregi.math.StatisticsReportDouble;
import net.pregi.networking.speedtest.IOProcessMode;
import net.pregi.networking.speedtest.NetworkTestingOptions;
import net.pregi.networking.speedtest.ServerEntry;
import net.pregi.networking.speedtest.TransferMeasure;

import java.util.List;

public class SpeedtestGUIMainActivity extends AppCompatActivity implements SpeedtestSettingsDialogHandler.Listener {
    private static final String SP_SPEEDTEST_OPTIONS = "speedtestOptions";

    private String TEXT_SERVERLIST_LABEL;
    private String TEXT_SERVERLIST_GET;
    private String TEXT_SERVERLIST_DOWNLOADING;
    private String TEXT_MSG_PINGING, TEXT_MSG_PINGING_DONE;
    private String TEXT_MSG_DOWNLOADING, TEXT_MSG_DOWNLOADING_DONE;
    private String TEXT_MSG_UPLOADING, TEXT_MSG_UPLOADING_DONE;

    private SpeedtestTools speedtestTools;

    private SpeedtestGraphicDisplay odometerDisplay;
    private ServerListAdapter serverListAdapter;

    private Button startButton;
    private TextView serverListLabelView;
    private Spinner serverListDropdown;

    private SpeedtestSettingsDialogHandler settingsDialog;
    private LogDialogHandler logDialog;

    private void doToast(int stringId) {
        Toast.makeText(this, stringId, Toast.LENGTH_LONG).show();
    }
    private void doToast(String string) {
        Toast.makeText(this, string, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speedtestgui);

        // initialize oft-reused values.
        Resources r = getResources();
        TEXT_SERVERLIST_LABEL = r.getString(R.string.speedtest_text_serverlist_label);
        TEXT_SERVERLIST_GET = r.getString(R.string.speedtest_text_serverlist_get);
        TEXT_SERVERLIST_DOWNLOADING = r.getString(R.string.speedtest_text_serverlist_downloading);
        TEXT_MSG_PINGING = r.getString(R.string.message_pinging);
        TEXT_MSG_PINGING_DONE = r.getString(R.string.message_pinging_done);
        TEXT_MSG_DOWNLOADING = r.getString(R.string.message_downloading);
        TEXT_MSG_DOWNLOADING_DONE = r.getString(R.string.message_downloading_done);
        TEXT_MSG_UPLOADING = r.getString(R.string.message_uploading);
        TEXT_MSG_UPLOADING_DONE = r.getString(R.string.message_uploading_done);

        String activityTitle;

        // Get launch parameter.
        Bundle b = getIntent().getExtras();
        if (b != null && b.getString("provider", "").equals("ookla")) {
            speedtestTools = SpeedtestTools.getInstance(SpeedtestTools.Provider.OOKLA);
            activityTitle = r.getString(R.string.app_name)+" (Ookla)";
        } else {
            speedtestTools = SpeedtestTools.getInstance(SpeedtestTools.Provider.ASTI);
            activityTitle = r.getString(R.string.app_name);
        }
        setTitle(activityTitle);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(false);
            ab.setHomeButtonEnabled(false);
        }

        settingsDialog = new SpeedtestSettingsDialogHandler(this)
                .setSpeedtestTools(speedtestTools);
        logDialog = new LogDialogHandler(this);

        {
            // Prepare the server list label.
            serverListLabelView = findViewById(R.id.serverlist_label);

            serverListLabelView.setMovementMethod(LinkMovementMethod.getInstance());
            serverListLabelView.setHighlightColor(Color.TRANSPARENT);
            setServersListLabelForState(false, null);
        }
        {
            startButton = findViewById(R.id.button_start);
            startButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    speedtestTools.runTest();
                }
            });
        }

        {
            // Prepare server list

            serverListAdapter = new ServerListAdapter(this);
            serverListAdapter.setServerList(speedtestTools.getServerList());
            setServersListLabelForState(false, null);

            serverListDropdown = findViewById(R.id.serverlist_dropdown);
            serverListDropdown.setAdapter(serverListAdapter);
            onSetServerSelection(speedtestTools.getSelectedServer());

            serverListDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    speedtestTools.setSelectedServer((ServerEntry)parent.getItemAtPosition(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    speedtestTools.setSelectedServer(null);
                }
            });
        }

        SurfaceView surfaceView = findViewById(R.id.odometer_display);
        odometerDisplay = new SpeedtestGraphicDisplay(surfaceView,
            new Runnable() {
                @Override
                public void run() {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (!isFinishing()) {
                                try {
                                    odometerDisplay.draw();
                                    Thread.sleep(20);
                                } catch (InterruptedException e) {
                                    break;
                                }
                            }
                        }
                    }).start();
                }
            }
        );

        // 2019-12-15: because there is no plan of ever adding more than one server,
        //  server selection is hidden until further notice.
        if (speedtestTools.getProviderEnum() == SpeedtestTools.Provider.ASTI) {
            serverListLabelView.setVisibility(View.GONE);
            serverListDropdown.setVisibility(View.GONE);
        }
    }

    private SpeedtestToolsListener speedtestToolsListener;
    @Override
    protected void onResume() {
        super.onResume();

        // load settings from storage
        NetworkTestingOptions options = new NetworkTestingOptions();
        SharedPreferences pref = getSharedPreferences(SP_SPEEDTEST_OPTIONS, Context.MODE_PRIVATE);
        options.setUseHttps(pref.getBoolean("useHttps", options.getUseHttps()));
        options.setPingCount(pref.getInt("pingCount", options.getPingCount()));
        options.setDownloadCount(pref.getInt("downloadCount", options.getDownloadCount()));
        options.setDownloadSize(pref.getLong("downloadSize", options.getDownloadSize()));
        options.setUploadCount(pref.getInt("uploadCount", options.getUploadCount()));
        options.setUploadSize(pref.getLong("uploadSize", options.getUploadSize()));
        speedtestTools.setSpeedtestOptions(options);

        speedtestToolsListener = speedtestTools.addListener(new SpeedtestToolsListener() {
            int currentTestIndex, currentTestCount;


            private void displayNewTestCount(String message, int attempts) {
                currentTestIndex = 0;
                currentTestCount = attempts;
                displayUpdateTestCount(message);
            }
            private void displayUpdateTestCount(final String message) {
                currentTestIndex++;
                displayTestMessage(message+" ("+currentTestIndex+"/"+currentTestCount+")");
            }
            private void displayTestMessage(final String text) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startButton.setText(text);
                    }
                });
            }

            @Override
            public void onDownloadServerList(List<ServerEntry> list, Exception e) {
                serverListAdapter.setServerList(list);
                setServersListLabelForState(false, e);
            }

            @Override
            public void onChangeSelectedServer(ServerEntry value) {
                // This is only called when the value is changed,
                //      or when the server list is (re)downloaded.

                List<ServerEntry> list = serverListAdapter.getServerList();
                if (list != null) {
                    int i = 0;
                    int index = -1;
                    for (ServerEntry e : list) {
                        if (e == value) {
                            index = i;
                            break;
                        }
                        i++;
                    }

                    if (-1<index && index<list.size()) {
                        if (index != serverListDropdown.getSelectedItemPosition()) {
                            serverListDropdown.setSelection(index);
                        }
                    }
                }

                onSetServerSelection(value);
            }

            @Override
            public void onTestStart(String host, int port, NetworkTestingOptions options) {
                odometerDisplay.reset();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startButton.setText(R.string.butt_wait);
                        startButton.setEnabled(false);
                        serverListDropdown.setEnabled(false);
                    }
                });
            }

            @Override
            public void onPingStart(final int attempts) {
                odometerDisplay.setPing(0);
                odometerDisplay.clearIOProgress();
                displayNewTestCount(TEXT_MSG_PINGING, attempts);
            }

            @Override
            public void onPingResult(TransferMeasure result, long milliseconds, StatisticsReportDouble currentStats) {
                if (result != null) {
                    odometerDisplay.setPing(milliseconds); // TODO: decide whether to use instance output or running average.
                }
                displayUpdateTestCount(TEXT_MSG_PINGING);
            }

            @Override
            public void onPingException(Exception e) {
                displayUpdateTestCount(TEXT_MSG_PINGING);
            }

            @Override
            public void onPingEnd(List<TransferMeasure> results, int failCount, StatisticsReportDouble stats) {
                odometerDisplay.setPing(stats.getMean());
                odometerDisplay.clearIOProgress();
                displayTestMessage(TEXT_MSG_PINGING_DONE);
            }

            @Override
            public void onIOProgress(long current, long total, IOProcessMode ioProcessMode) {
                odometerDisplay.setIOProgress((double)current/(double)total, ioProcessMode);
            }

            @Override
            public void onDownloadStart(int attempts) {
                odometerDisplay.setDownloadSpeed(0);
                displayNewTestCount(TEXT_MSG_DOWNLOADING, attempts);
                odometerDisplay.clearIOProgress();
            }

            @Override
            public void onDownloadResult(TransferMeasure result, long byteCount, float seconds, StatisticsReportDouble currentStats) {
                if (result != null) {
                    odometerDisplay.setDownloadSpeed(byteCount / seconds);
                }
                displayUpdateTestCount(TEXT_MSG_DOWNLOADING);
            }

            @Override
            public void onDownloadException(Exception e) {
                displayUpdateTestCount(TEXT_MSG_DOWNLOADING);
            }

            @Override
            public void onDownloadEnd(List<TransferMeasure> results, int failCount, StatisticsReportDouble stats) {
                odometerDisplay.setDownloadSpeed(stats.getMean());
                displayTestMessage(TEXT_MSG_DOWNLOADING_DONE);
                odometerDisplay.clearIOProgress();
            }

            @Override
            public void onUploadStart(int attempts) {
                odometerDisplay.setUploadSpeed(0);
                displayNewTestCount(TEXT_MSG_UPLOADING, attempts);
                odometerDisplay.clearIOProgress();
            }

            @Override
            public void onUploadResult(TransferMeasure result, long byteCount, float seconds, StatisticsReportDouble currentStats) {
                if (result != null) {
                    odometerDisplay.setUploadSpeed(byteCount / seconds);
                }
                displayUpdateTestCount(TEXT_MSG_UPLOADING);
            }

            @Override
            public void onUploadException(Exception e) {
                displayUpdateTestCount(TEXT_MSG_UPLOADING);
            }

            @Override
            public void onUploadEnd(List<TransferMeasure> results, int failCount, StatisticsReportDouble stats) {
                odometerDisplay.setUploadSpeed(stats.getMean());
                displayTestMessage(TEXT_MSG_UPLOADING_DONE);
                odometerDisplay.clearIOProgress();
            }

            private void onTestConcluded() {
                odometerDisplay.clearIOProgress();
                startButton.setText(R.string.butt_start);
                serverListDropdown.setEnabled(true);
                startButton.setEnabled(serverListDropdown.getSelectedItem() != null);
            }

            @Override
            public void onTestInterrupted(final Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onTestConcluded();
                        if (e != null) {
                            doToast(getResources().getString(R.string.notif_speedtest_interrupted)+"("+e.getClass().getSimpleName()+")");
                        } else {
                            doToast(R.string.notif_speedtest_interrupted);
                        }
                    }
                });
            }

            @Override
            public void onTestEnd() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onTestConcluded();
                        doToast(R.string.notif_speedtest_done);
                    }
                });
            }

            @Override
            public boolean isRequestingTestStop() {
                return false;
            }

            @Override
            public void onUpdateLog(final CharSequence fullLog) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logDialog.setText(fullLog);
                    }
                });
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        NetworkTestingOptions options = speedtestTools.getSpeedtestOptions();
        SharedPreferences settings = getSharedPreferences(SP_SPEEDTEST_OPTIONS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("useHttps", options.getUseHttps());
        editor.putInt("pingCount", options.getPingCount());
        editor.putInt("downloadCount", options.getDownloadCount());
        editor.putLong("downloadSize", options.getDownloadSize());
        editor.putInt("uploadCount", options.getUploadCount());
        editor.putLong("uploadSize", options.getUploadSize());
        editor.commit();

        speedtestTools.removeListener(speedtestToolsListener);
    }

    private CharSequence setServersListLabelForState(boolean isDownloading, Exception thrownException) {
        final SpannableStringBuilder text = new SpannableStringBuilder();
        if (isDownloading) {
            text.append(TEXT_SERVERLIST_LABEL);
            text.append(" ");
            text.append(SpanUtils.colored(TEXT_SERVERLIST_DOWNLOADING, Color.GRAY));
        } else {
            text.append(TEXT_SERVERLIST_LABEL);
            text.append(" ");
            text.append(SpanUtils.clickable(TEXT_SERVERLIST_GET, new ClickableSpan() {
                @Override
                public void onClick(View view) {
                    setServersListLabelForState(true, null);
                    speedtestTools.downloadServerList();
                }
            }));
        }
        if (thrownException != null) {
            text.append(SpanUtils.colored(" Error: "+thrownException.getClass().getSimpleName()+": "+thrownException.getMessage(), Color.GRAY));
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                serverListLabelView.setText(text);
            }
        });
        return text;
    }

    private void createKeyValueSpannedLn(SpannableStringBuilder builder, String name, String value) {
        builder.append(new SpanUtils.Builder(name+": ").bold().build());
        builder.append(value);
        builder.append("\n");
    }
    private void onSetServerSelection(final ServerEntry entry) {
        // TODO: I want the text here to be viewable after clicking "get details" instead.
        /*
        TextView detailsView = findViewById(R.id.server_details);
        if (entry != null) {
            SpannableStringBuilder details = new SpannableStringBuilder();

            createKeyValueSpannedLn(details, "Id", Long.toString(entry.getId()));
            createKeyValueSpannedLn(details, "Sponsor", entry.getSponsor());
            createKeyValueSpannedLn(details, "Where", entry.getName()+" ("+entry.getCountryCode()+")");
            createKeyValueSpannedLn(details, "Distance", Integer.toString(entry.getDistance()));

            BigDecimal lat = entry.getLatitude();
            BigDecimal lon = entry.getLongitude();
            if (lat != null || lon != null) {
                createKeyValueSpannedLn(details, "Coordinates (lat, lon)",
                        (lat != null ? lat.toString() : "???") + ", " + (lon != null ? lon.toString() : "???"));
            }

            if (entry.getHttpsFunctional() != null) {
                createKeyValueSpannedLn(details, "Https available?", entry.getHttpsFunctional() ? "yes" : "no");
            }

            detailsView.setText(details);
        } else {
            detailsView.setText("");
        }*/

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startButton.setEnabled(entry != null && !speedtestTools.isRunning());
            }
        });
    }

    @Override
    public NetworkTestingOptions getSpeedtestSettings() {
        return speedtestTools.getSpeedtestOptions();
    }

    @Override
    public void setSpeedtestSettings(NetworkTestingOptions value) {
        speedtestTools.setSpeedtestOptions(value);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_speedtestgui, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch(id) {
            case R.id.settings:
                settingsDialog.show();
                break;
            case R.id.show_log:
                logDialog.show();
                break;
            case R.id.examine_networks:
                if (speedtestTools.runExamineNetworks(this)) {
                    logDialog.setText("");
                    logDialog.show();
                } else {
                    doToast(R.string.notif_cantstart_stillbusy);
                }
                break;
            case R.id.check_permissions:
                if (speedtestTools.runCheckPermissions(this)) {
                    logDialog.setText("");
                    logDialog.show();
                } else {
                    doToast(R.string.notif_cantstart_stillbusy);
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}