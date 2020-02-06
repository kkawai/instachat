package com.instachat.android.app.activity;

import android.app.Activity;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.instachat.android.Constants;
import com.instachat.android.R;
import com.instachat.android.util.MLog;
import com.instachat.android.util.SimpleRxWrapper;
import com.instachat.android.util.UserPreferences;
import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.IUnityBannerListener;
import com.unity3d.services.banners.UnityBanners;
import com.unity3d.services.banners.view.BannerPosition;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

public class AdsHelper {

    private static final String TAG = "AdsHelper";

    private static final String bannerPlacementId = "actual_banner";
    private static final String rewardedVideoPlacementId = "rewardedVideo";

    private ViewGroup adContainer;

    @Inject
    public AdsHelper() {
    }

    public static void init(Activity activity) {
        UnityAds.initialize (activity, Constants.UNITY_ADS_GAME_ID, Constants.UNITY_ADS_IS_TESTING, true);
    }

    public void loadRewardedAd(final Activity activity) {

        if (!UserPreferences.getInstance().canShowRewardedAd())
            return;
        MLog.i(TAG,"load unity rewarded ad");
        UnityAds.addListener(new IUnityAdsListener() {
            @Override
            public void onUnityAdsReady(String s) {
                MLog.i(TAG,"onUnityAdsReady " + s);
                try {
                    if (activity != null && !activity.isFinishing()) {
                        UnityAds.show(activity, s);
                    }
                }catch (Throwable t){
                    MLog.e(TAG,"");
                }
            }

            @Override
            public void onUnityAdsStart(String s) {
                MLog.i(TAG,"onUnityAdsStart " + s);
                UserPreferences.getInstance().setShownRewardedAd();
            }

            @Override
            public void onUnityAdsFinish(String s, UnityAds.FinishState finishState) {
                MLog.i(TAG,"onUnityAdsFinish " + s + " state: " + finishState);
                UnityAds.removeListener(this);
            }

            @Override
            public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String s) {
                MLog.i(TAG,"onUnityAdsError " + s);
                UnityAds.removeListener(this);
            }
        });
        MLog.i(TAG,"load unity rewarded ad ");

        UnityAds.load(rewardedVideoPlacementId);
    }

    public void loadBannerAd(Activity activity, FirebaseRemoteConfig firebaseRemoteConfig) {

        final WeakReference<Activity> activityWeakReference = new WeakReference<>(activity);

        if (!firebaseRemoteConfig.getBoolean(Constants.KEY_DO_SHOW_ADS)) {
            View view = activity.findViewById(R.id.ads_container);
            if (view != null) {
                view.setVisibility(View.GONE);
            }
            MLog.i(TAG,"loadBannerAd exited ");
            return;
        }

        adContainer = activity.findViewById(R.id.ads_container);
        final IUnityBannerListener myBannerListener = new UnityBannerListener ();

        UnityBanners.setBannerListener(myBannerListener);
        UnityBanners.setBannerPosition(BannerPosition.BOTTOM_CENTER);

        SimpleRxWrapper.executeInWorkerThread(new Runnable() {
            @Override
            public void run() {
                MLog.i(TAG,"checking unity initialized ");
                while (!UnityAds.isInitialized() && !activityWeakReference.isEnqueued()) {
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                MLog.i(TAG,"unity initialized ");
                SimpleRxWrapper.executeInUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!activityWeakReference.isEnqueued()) {
                            UnityBanners.destroy();
                            MLog.i(TAG,"loading banner");
                            UnityBanners.loadBanner(activity, bannerPlacementId);
                        }
                    }
                });
            }
        });
        /*
        if (bannerView == null) {
            bannerView = new BannerView(activity.getApplication());
            relativeLayout = activity.findViewById(R.id.ads_container);
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
         */
    }
    // Implement the banner listener interface methods:
    private class UnityBannerListener implements IUnityBannerListener {

        @Override
        public void onUnityBannerLoaded (String placementId, View view) {
            // When the banner content loads, add it to the view hierarchy:
            MLog.i(TAG,"onUnityBannerLoaded " + placementId);
            try {
                ((ViewGroup) view.getParent()).removeAllViews();
            }catch (Throwable t){}
            try {
                adContainer.removeAllViews();
            }catch (Throwable t){}
            try {
                adContainer.addView(view);
            }catch (Throwable t){}
        }

        @Override
        public void onUnityBannerUnloaded (String placementId) {
            // When the banner’s no longer in use, remove it from the view hierarchy:
            MLog.i(TAG,"onUnityBannerUnloaded " + placementId);
            //bannerView = null;
        }

        @Override
        public void onUnityBannerShow (String placementId) {
            // Called when the banner is first visible to the user.
            MLog.i(TAG,"onUnityBannerShow " + placementId);
        }

        @Override
        public void onUnityBannerClick (String placementId) {
            // Called when the banner is clicked.
            MLog.i(TAG,"onUnityBannerClick " + placementId);
        }

        @Override
        public void onUnityBannerHide (String placementId) {
            // Called when the banner is hidden from the user.
            MLog.i(TAG,"onUnityBannerHide " + placementId);
        }

        @Override
        public void onUnityBannerError (String message) {
            // Called when an error occurred, and the banner failed to load or show.
            MLog.i(TAG,"onUnityBannerError " + message);
        }
    }

    private static int dpToPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }
}
