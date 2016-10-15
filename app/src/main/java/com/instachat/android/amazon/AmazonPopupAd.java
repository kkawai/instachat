package com.instachat.android.amazon;

import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.PopupWindow;

import com.amazon.device.ads.Ad;
import com.amazon.device.ads.AdError;
import com.amazon.device.ads.AdLayout;
import com.amazon.device.ads.AdListener;
import com.amazon.device.ads.AdProperties;
import com.amazon.device.ads.AdRegistration;
import com.amazon.device.ads.AdTargetingOptions;
import com.instachat.android.R;
import com.instachat.android.Constants;
import com.instachat.android.util.MLog;

public final class AmazonPopupAd implements AdListener {

    private static final String LOG_TAG = AmazonPopupAd.class.getSimpleName();
    private View closeButton;
    private AdLayout adView; // The ad view used to load and display the ad.
    private View view;
    private PopupWindow window;
    private LayoutInflater layoutInflater;

    public AmazonPopupAd(final LayoutInflater layoutInflater) {
        this.layoutInflater = layoutInflater;
    }

    public AmazonPopupAd initialize() {
        view = layoutInflater.inflate(R.layout.amazon_popup_ad, null);
        closeButton = view.findViewById(R.id.close_ad_button);
        closeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dismiss();
            }

        });
        return this;
    }

    public void start() {

        adView = (AdLayout) view.findViewById(R.id.ad_view);
        //adView.setVisibility(View.GONE);
        adView.setListener(this);

        window = new PopupWindow(view, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, true); //Creation of popup
        window.setAnimationStyle(android.R.style.Animation_Dialog);

        try {
            window.showAtLocation(view, Gravity.CENTER, 0, 0);    // Displaying popup
            window.getContentView().setFocusableInTouchMode(true);
            window.getContentView().setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(final View v, final int keyCode, final KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        dismiss();
                    }
                    return false;
                }
            });
        } catch (final Throwable t) {
            return;
        }


        final AdTargetingOptions adOptions = new AdTargetingOptions();
        adView.loadAd(adOptions);

        MLog.i(LOG_TAG, "Loading popup ad.");
    }

    public boolean isShowing() {
        return window != null && window.isShowing();
    }

    public void dismiss() {
        if (isShowing()) {
            try {
                window.dismiss();
            } catch (final Exception e) {
            }
            try {
                adView.destroy();
            } catch (final Exception e) {
            }
            view = null;
            window = null;
            layoutInflater = null;
        }
    }

    private static boolean initamz;

    /**
     * Call this in the main Application class onCreate
     */
    public static void initAmazonAds() {

        if (initamz) {
            return;
        }
        initamz = true;

        if (!Constants.IS_AMAZON_ADS_ENABLED) {
            return;
        }

        AdRegistration.enableLogging(false);
        // For debugging purposes flag all ad requests as tests, but set to
        // false for production builds
        AdRegistration.enableTesting(Constants.IS_AMAZON_DEBUG_AD);

        try {
            if (Constants.IS_AMAZON_DEBUG_AD) {
                AdRegistration.setAppKey(Constants.AMAZON_APP_KEY_TEST);
            } else {
                AdRegistration.setAppKey(Constants.AMAZON_APP_KEY_PROD);
            }
        } catch (final Exception e) {
            MLog.e(LOG_TAG, "", e);
            return;
        }
    }

    @Override
    public void onAdCollapsed(Ad arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAdDismissed(Ad arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAdExpanded(Ad arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onAdFailedToLoad(Ad arg0, AdError arg1) {
        // TODO Auto-generated method stub
        dismiss();
    }

    @Override
    public void onAdLoaded(Ad arg0, AdProperties arg1) {

    }
}
