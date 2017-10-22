package com.instachat.android.app.activity;

import android.app.Activity;
import android.content.res.Resources;
import android.widget.RelativeLayout;

import com.instachat.android.Constants;
import com.instachat.android.R;
import com.smaato.soma.AdDimension;
import com.smaato.soma.AdListenerInterface;
import com.smaato.soma.BannerView;

import javax.inject.Inject;

public class AdHelper {

    private BannerView bannerView;
    private RelativeLayout relativeLayout;

    @Inject
    public AdHelper() {
    }

    public void loadAd(Activity activity) {

        if (bannerView == null) {
            bannerView = new BannerView(activity.getApplication());
            relativeLayout = activity.findViewById(R.id.ad_container);
            if (relativeLayout == null) {
                throw new IllegalStateException("activity layout does not contain RelativeLayout with R.id.ad_container");
            }
            relativeLayout.addView(bannerView, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, dpToPx(50) ));
            bannerView.addAdListener((AdListenerInterface)activity);
            bannerView.getAdSettings().setAdDimension(AdDimension.DEFAULT);
            bannerView.getAdSettings().setPublisherId(Constants.SMAATO_PUBLISHER_ID);
            bannerView.getAdSettings().setAdspaceId(Constants.SMAATO_BANNER_ADSPACE_ID);
            bannerView.setAutoReloadEnabled(true);
            bannerView.setAutoReloadFrequency(60);
            bannerView.setLocationUpdateEnabled(false);
        }
        bannerView.asyncLoadNewBanner();
    }

    private static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
