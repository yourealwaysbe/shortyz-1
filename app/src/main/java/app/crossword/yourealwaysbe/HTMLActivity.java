package app.crossword.yourealwaysbe;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.appcompat.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import app.crossword.yourealwaysbe.forkyz.R;
import app.crossword.yourealwaysbe.versions.AndroidVersionUtils;
import app.crossword.yourealwaysbe.view.recycler.ShowHideOnScroll;


public class HTMLActivity extends ForkyzActivity {
    protected AndroidVersionUtils utils = AndroidVersionUtils.Factory.getInstance();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        utils.holographic(this);
        utils.finishOnHomeButton(this);
        this.setContentView(R.layout.html_view);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        WebView webview = (WebView) this.findViewById(R.id.webkit);

        Uri u = this.getIntent()
                    .getData();
        webview.loadUrl(u.toString());
        FloatingActionButton download = (FloatingActionButton) this.findViewById(R.id.button_floating_action);
        if(download != null) {
            download.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            download.setImageBitmap(createBitmap("icons1.ttf", "k"));
            webview.setOnTouchListener(new ShowHideOnScroll(download));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        WebView webview = (WebView) this.findViewById(R.id.webkit);
        if(WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK) &&
           nightMode.isNightMode()) {
            WebSettingsCompat.setForceDark(webview.getSettings(),
                                           WebSettingsCompat.FORCE_DARK_ON);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getTitle() == null){
            finish();
            return true;
        }
        return false;
    }
}
