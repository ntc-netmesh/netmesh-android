package net.pregi.android.speedtester.speedtestweb;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.pregi.android.speedtester.R;

public class SpeedtestWebMainActivity extends AppCompatActivity {
    private WebView webView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speedtestweb);

        // TODO: I'm not sure what needs to be done to avoid bloating the memory with webViews
        //      when leaving this activity.
        //  Requires a bit of researching.
        webView = findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("https://sago-gulaman.xyz/web/speedtest/");
        webView.setWebViewClient(new WebViewClient() {
            private void setActivityTitle(String value, String subvalue) {
                ActionBar ab = getSupportActionBar();
                if (ab != null) {
                    ab.setTitle(value);
                    ab.setSubtitle(subvalue);
                }

                //setTitle(value);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);

                setActivityTitle(view.getTitle(), url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                setActivityTitle(view.getTitle(), url);
            }
        });

        if (Build.VERSION.SDK_INT >= 19) {
            webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
        } else {
            // webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_speedtestweb, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.refresh) {
            webView.reload();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
