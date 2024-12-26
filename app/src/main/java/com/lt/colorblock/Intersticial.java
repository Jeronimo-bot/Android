package com.lt.colorblock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class Intersticial {
    private Context mContext;
    private RewardedAd rewardedAd;
    private Handler adHandler = new Handler();
    private static final String PREFS_NAME = "AdPrefs";
    private static final String LAST_AD_TIME = "LastAdTime";
    private static final String REMAINING_TIME = "RemainingTime";
    private static final long TWO_HOURS = 1800000; // 2 horas em milissegundos
    private static final long COUNTDOWN_INTERVAL = 1000; // Intervalo de contagem regressiva (1 segundo)
    private long remainingTime = 0;
    private RewardedAdListener listener;

    public interface RewardedAdListener {
        void onRewardedAdAvailable(boolean available);
    }

    public Intersticial(Context context) {
        this.mContext = context;
        initializeMobileAds();
        loadLastAdTime();
        updateTextViewVisibility();
        loadRewardedAd();
        scheduleRewardedAd();
    }

    private void initializeMobileAds() {
        MobileAds.initialize(mContext, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(mContext, "ca-app-pub-3940256099942544/1033173712", adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                rewardedAd = null;
                notifyRewardedAdAvailable(false);
            }

            @Override
            public void onAdLoaded(RewardedAd ad) {
                rewardedAd = ad;
                notifyRewardedAdAvailable(true);
            }
        });
    }

    public void showRewardedAd() {
        if (rewardedAd != null) {
            rewardedAd.show((Activity) mContext, rewardItem -> {
                startCountdown();
            });
        }
    }

    private void saveLastAdTime() {
        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putLong(LAST_AD_TIME, System.currentTimeMillis());
        editor.putLong(REMAINING_TIME, remainingTime);
        editor.apply();
    }

    private void loadLastAdTime() {
        SharedPreferences preferences = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastAdTime = preferences.getLong(LAST_AD_TIME, 0);
        long savedRemainingTime = preferences.getLong(REMAINING_TIME, 0);
        long currentTime = System.currentTimeMillis();

        if (lastAdTime > 0) {
            remainingTime = savedRemainingTime - (currentTime - lastAdTime);
            if (remainingTime < 0) {
                remainingTime = 0;
                notifyRewardedAdAvailable(rewardedAd != null);
            }
        }
    }

    public void updateTextViewVisibility() {
        if (remainingTime > 0) {
        } else {
            notifyRewardedAdAvailable(rewardedAd != null);
        }
    }

    private void startCountdown() {
        remainingTime = TWO_HOURS;
        saveLastAdTime();

        Runnable countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (remainingTime > 0) {
                    remainingTime -= COUNTDOWN_INTERVAL;
                    adHandler.postDelayed(this, COUNTDOWN_INTERVAL);
                } else {
                    notifyRewardedAdAvailable(rewardedAd != null);
                }
            }
        };

        adHandler.postDelayed(countdownRunnable, COUNTDOWN_INTERVAL);
    }

    private boolean isCountdownRunning() {
        return remainingTime > 0;
    }

    public void tryToShowRewardedAd() {
        loadLastAdTime();

        if (!isCountdownRunning() && rewardedAd != null) {
            showRewardedAd();
        }
    }

    public void scheduleRewardedAd() {
        Runnable adRunnable = new Runnable() {
            @Override
            public void run() {
                tryToShowRewardedAd();
                adHandler.postDelayed(this, TWO_HOURS);
            }
        };
        adHandler.postDelayed(adRunnable, TWO_HOURS);
    }

    public void setRewardedAdListener(RewardedAdListener listener) {
        this.listener = listener;
    }

    private void notifyRewardedAdAvailable(boolean available) {
        if (listener != null) {
            listener.onRewardedAdAvailable(available);
        }
    }
}
