package com.lt.colorblock;


import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class SplashScreenActivity extends AppCompatActivity implements AdClosedListener {

    private TextView mensagemLoading;
    private TextView mensagemNova;
    private boolean isMenuActivityLaunched = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        customizeNavigationBar();






        final View backgroundView = findViewById(R.id.raiz);

        int colorStart = ContextCompat.getColor(this, R.color.background); // Sua cor inicial
        int colorEnd = ContextCompat.getColor(this, R.color.cor_final); // Sua cor final (adicione esta cor no arquivo colors.xml também)

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorStart, colorEnd);
        colorAnimation.setDuration(3000); // Duração da animação em milissegundos
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                backgroundView.setBackgroundColor((int) animator.getAnimatedValue());
            }
        });

        colorAnimation.start();










        mensagemLoading = findViewById(R.id.mensagemLoading);
        mensagemNova = findViewById(R.id.mensagemNova);
        mensagemNova = findViewById(R.id.mensagemNova);

        MyApplication app = (MyApplication) getApplicationContext();
        app.setSplashScreenActivity(this);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Animation animFadeOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out);
                mensagemLoading.startAnimation(animFadeOut);

                animFadeOut.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        mensagemLoading.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
            }
        }, 3000); // Atraso de 3 segundos
    }

    @Override
    public void onAdClosed() {
        if (!isMenuActivityLaunched) {
            Intent intent = new Intent(this, MenuActivity.class);
            startActivity(intent);
            // Define a animação de transição para nula
            overridePendingTransition(0, 0);
            finish();

            isMenuActivityLaunched = true;
        }

        mensagemNova.setText("Welcome");
        mensagemNova.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
        mensagemNova.setVisibility(View.VISIBLE);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyApplication app = (MyApplication) getApplicationContext();
        app.clearSplashScreenActivity();

    }


    @Override
    protected void onResume() {
        super.onResume();
        customizeNavigationBar();
    }

    public void customizeNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setNavigationBarColor(getResources().getColor(R.color.navigation_bar_color));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.setNavigationBarDividerColor(getResources().getColor(R.color.navigation_bar_border_color));
            }
        }
    }





}
