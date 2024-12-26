package com.lt.colorblock;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.appopen.AppOpenAd;

public class MyApplication extends Application implements Application.ActivityLifecycleCallbacks, LifecycleObserver {
    private Activity currentActivity;
    private AppOpenAdManager appOpenAdManager;
    private static final String TAG = "MyApplication";
    private SplashScreenActivity splashScreenActivity = null;
    private boolean isMainActivityLaunched = false;
    private AdClosedListener adClosedListener;

    @Override
    public void onCreate() {
        super.onCreate();
        this.registerActivityLifecycleCallbacks(this);
        MobileAds.initialize(this, initializationStatus -> {});
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        appOpenAdManager = new AppOpenAdManager(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected void onMoveToForeground() {
        appOpenAdManager.showAdIfAvailable(currentActivity);
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        currentActivity = activity;
    }

    @Override
    public void onActivityStarted(Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
        }
    }

    public void setSplashScreenActivity(SplashScreenActivity activity) {
        this.splashScreenActivity = activity;
        if (activity != null) {
            this.adClosedListener = activity;
        }
    }

    public void clearSplashScreenActivity() {
        this.splashScreenActivity = null;
        this.adClosedListener = null;
    }

    public void onAdClosed() {
        if (splashScreenActivity != null && !isMainActivityLaunched) {
            splashScreenActivity.startActivity(new Intent(splashScreenActivity, MainActivity.class));
            splashScreenActivity.finish();
            isMainActivityLaunched = true;
        }

        if (adClosedListener != null) {
            adClosedListener.onAdClosed();
        }
    }

    public class AppOpenAdManager {
        private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/9257395921";
        private AppOpenAd appOpenAd = null;
        private boolean isShowingAd = false;
        private final Context context;
        private long loadTime;
        private boolean isAdAlreadyShown = false;

        // Adicione o Handler aqui
        private Handler adTimeoutHandler = new Handler();
        private Runnable adTimeoutRunnable;

        public AppOpenAdManager(Context context) {
            this.context = context;
            loadAd();
        }

        public void loadAd() {
            if (isAdAvailable() || isAdAlreadyShown) {
                return;
            }

            AdRequest request = new AdRequest.Builder().build();
            AppOpenAd.load(
                    context, AD_UNIT_ID, request,
                    AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                    new AppOpenAd.AppOpenAdLoadCallback() {
                        @Override
                        public void onAdLoaded(AppOpenAd ad) {
                            appOpenAd = ad;
                            loadTime = System.currentTimeMillis();
                            Log.d(TAG, "Anúncio de abertura carregado.");
                            showAdIfAvailable(currentActivity);
                        }

                        @Override
                        public void onAdFailedToLoad(LoadAdError loadAdError) {
                            Log.e(TAG, "Falha ao carregar o anúncio de abertura: " + loadAdError.getMessage());
                            // Chame onAdClosed se o carregamento do anúncio falhar
                            MyApplication.this.onAdClosed();
                        }
                    });

            // Inicie o temporizador de 10 segundos
            adTimeoutRunnable = new Runnable() {
                @Override
                public void run() {
                    if (!isAdAlreadyShown && !isShowingAd) {
                        Log.d(TAG, "Anúncio não foi mostrado em 10 segundos, iniciando a atividade principal.");
                        MyApplication.this.onAdClosed();
                    }
                }
            };
            adTimeoutHandler.postDelayed(adTimeoutRunnable, 10000); // 10 segundos
        }

        public void showAdIfAvailable(Activity activity) {
            if (isAdAlreadyShown || !isAdAvailable() || activity.isFinishing() || !(activity instanceof SplashScreenActivity)) {
                Log.d(TAG, "Não há anúncio de abertura disponível ou a atividade está encerrada.");
                return;
            }

            appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    appOpenAd = null;
                    isShowingAd = false;
                    loadAd();
                    Log.d(TAG, "Anúncio de abertura foi fechado.");
                    isAdAlreadyShown = true;

                    // Remova o temporizador quando o anúncio for fechado
                    adTimeoutHandler.removeCallbacks(adTimeoutRunnable);

                    if (adClosedListener != null) {
                        adClosedListener.onAdClosed();
                    }
                }

                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    appOpenAd = null;
                    isShowingAd = false;
                    Log.e(TAG, "Falha ao exibir o anúncio de abertura em tela cheia: " + adError.getMessage());

                    // Remova o temporizador se o anúncio falhar ao exibir
                    adTimeoutHandler.removeCallbacks(adTimeoutRunnable);

                    // Chame onAdClosed para iniciar a atividade principal
                    MyApplication.this.onAdClosed();
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    isShowingAd = true;
                    isAdAlreadyShown = true;
                    Log.d(TAG, "Anúncio de abertura foi exibido em tela cheia.");

                    // Remova o temporizador quando o anúncio for exibido
                    adTimeoutHandler.removeCallbacks(adTimeoutRunnable);
                }
            });

            isShowingAd = true;
            appOpenAd.show(activity);
            Log.d(TAG, "Exibindo o anúncio de abertura.");
        }

        private boolean isAdAvailable() {
            return appOpenAd != null;
        }
    }
}
