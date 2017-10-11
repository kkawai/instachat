package com.instachat.android.app.activity;

import android.app.Activity;
import android.content.res.Resources;
import android.widget.RelativeLayout;

import com.instachat.android.Constants;
import com.instachat.android.R;
import com.smaato.soma.AdDimension;
import com.smaato.soma.AdListenerInterface;
import com.smaato.soma.BannerView;

public class AdHelper {

    private BannerView bannerView;
    private RelativeLayout relativeLayout;
    public AdHelper(Activity activity, int adContainerResId) {
        bannerView = new BannerView(activity.getApplication());
        relativeLayout = activity.findViewById(adContainerResId);
        relativeLayout.addView(bannerView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, dpToPx(50) ));
        bannerView.addAdListener((AdListenerInterface)activity);
        bannerView.getAdSettings().setAdDimension(AdDimension.DEFAULT);
    }

    public void loadAd() {

        bannerView.getAdSettings().setPublisherId(Constants.SMAATO_PUBLISHER_ID);
        bannerView.getAdSettings().setAdspaceId(Constants.SMAATO_BANNER_ADSPACE_ID);
        bannerView.setAutoReloadEnabled(true);
        bannerView.setAutoReloadFrequency(60);
        bannerView.setLocationUpdateEnabled(false);
        bannerView.asyncLoadNewBanner();
    }

    private static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
