package app.crossword.yourealwaysbe.versions;

import android.annotation.TargetApi;
import android.content.res.Configuration;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnClickListener;

import app.crossword.yourealwaysbe.ForkyzActivity;
import app.crossword.yourealwaysbe.util.NightModeHelper;


@TargetApi(11)
public class HoneycombUtil extends GingerbreadUtil {

	{
		System.out.println("Honeycomb Utils.");
	}

	@Override
    public void finishOnHomeButton(final AppCompatActivity a) {
		ActionBar bar = a.getSupportActionBar();
		if(bar == null){
			return;
		}
		bar.setDisplayHomeAsUpEnabled(true);
		View home = a.findViewById(android.R.id.home);
        if(home != null){
	        home.setOnClickListener(new OnClickListener() {
	                public void onClick(View arg0) {
	                    a.finish();
	                }
	            });
        }
    }

    @TargetApi(11)
	@Override
    public void holographic(AppCompatActivity a) {
        ActionBar bar = a.getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean isNightModeAvailable(){
		return true;
	}

	@Override
    public void nextNightMode(ForkyzActivity activity){
		activity.nightMode.next();
        if(activity.nightMode.isNightMode()){
            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES
            );
        } else {
            AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_NO
            );
        }
	}

	@Override
	public void restoreNightMode(ForkyzActivity forkyzActivity) {
        restoreNightMode(forkyzActivity.nightMode);
    }

    @Override
    public void restoreNightMode(NightModeHelper nightMode) {
        nightMode.restoreNightMode();
    }

	@Override
    public void onActionBarWithText(MenuItem a) {
        a.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT + MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    public void onActionBarWithoutText(MenuItem a) {
        a.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM + MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    public void hideTitleOnPortrait(AppCompatActivity a) {
        if(a.getResources().getConfiguration().orientation != Configuration.ORIENTATION_PORTRAIT){
            return;
        }
        ActionBar bar = a.getSupportActionBar();
        if (bar != null) {
            bar.setDisplayShowTitleEnabled(false);
        }
    }

    public void onActionBarWithText(SubMenu a) {
        this.onActionBarWithText(a.getItem());
    }

    public View onActionBarCustom(AppCompatActivity a, int id) {
    	ActionBar bar = a.getSupportActionBar();
    	if(bar == null){
    		return null;
    	}
    	bar.setDisplayShowCustomEnabled(true);
    	bar.setDisplayShowTitleEnabled(false);
    	bar.setDisplayShowHomeEnabled(true);
    	bar.setCustomView(id);
    	return bar.getCustomView();
	}

    public void hideWindowTitle(AppCompatActivity a) {
    	// no op;
    }

	public void hideActionBar(AppCompatActivity a) {
		ActionBar ab = a.getSupportActionBar();
		if(ab == null){
			return;
		}
		ab.hide();
	}

}
