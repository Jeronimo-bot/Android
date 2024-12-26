package com.lt.colorblock;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // Constantes para controle de velocidade
    private static final float INITIAL_SPEED_FACTOR = 5.0f; // Velocidade inicial dos blocos
    private static final float SPEED_INCREMENT = 0.5f; // Incremento de velocidade a cada evento
    private static final float MAX_SPEED_FACTOR = 20.0f; // Velocidade máxima permitida

    private int score = 0;
    private float speedFactor = INITIAL_SPEED_FACTOR; // Alterado para float
    private final int maxBlocks = 3;
    private TextView scoreTextView;
    private FrameLayout gameArea;
    private final Random random = new Random();
    private final Handler handler = new Handler();
    private MediaPlayer successSound, errorSound, backgroundMusic;
    private boolean isGamePaused = false;
    private Button menuButton;
    private String gameMode = "NORMAL";
    private boolean isDialogShowing = false;
    private SharedPreferences sharedPreferences;

    private Intersticial publicidade;
    private AdView adView;

    private final ArrayList<View> activeBlocks = new ArrayList<>();
    private final ArrayList<View> transformingBlocks = new ArrayList<>();
    private Runnable gameLoopRunnable;

    private int backgroundTransformationCounter = 0;
    private final int BACKGROUND_TRANSFORMATION_INTERVAL = 100; // Ajuste este valor para controlar a frequência

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar os elementos do layout
        scoreTextView = findViewById(R.id.scoreTextView);
        gameArea = findViewById(R.id.gameArea);
        menuButton = findViewById(R.id.menuButton);

        if (scoreTextView == null || gameArea == null || menuButton == null) {
            Toast.makeText(this, "Erro ao carregar a interface gráfica", Toast.LENGTH_SHORT).show();
            return;
        }

        // Inicializar o SharedPreferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        publicidade = new Intersticial(this);
        MobileAds.initialize(this, initializationStatus -> {
            // Anúncio inicializado
        });

        adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        // Obter o modo de jogo passado pelo MenuActivity
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("MODE")) {
            gameMode = intent.getStringExtra("MODE");
            Log.d("MainActivity", "Game mode: " + gameMode);
        }

        // Inicializar sons para ambos os modos
        successSound = MediaPlayer.create(this, R.raw.success);
        errorSound = MediaPlayer.create(this, R.raw.error);

        // Ajuste do Volume dos Sons successSound e errorSound
        if (successSound != null) {
            successSound.setVolume(0.3f, 0.3f);
        }

        if (errorSound != null) {
            errorSound.setVolume(0.1f, 0.1f);
        }

        // Inicializar música de fundo
        backgroundMusic = MediaPlayer.create(this, R.raw.late_nights_in_osaka);
        backgroundMusic.setLooping(true); // Definir para loop contínuo

        // Definir volume com base na preferência do usuário
        boolean isMusicEnabled = sharedPreferences.getBoolean("music_enabled", true);
        if (isMusicEnabled) {
            backgroundMusic.setVolume(0.1f, 0.1f);
            backgroundMusic.start();
        }

        // Configurar botão de menu
        menuButton.setOnClickListener(view -> {
            Intent intent1 = new Intent(MainActivity.this, MenuActivity.class);
            startActivity(intent1);
            finish(); // Finalizar a atividade atual
        });

        // Ajustar a visibilidade do scoreTextView com base no modo de jogo
        if (gameMode.equals("ZEN")) {
            scoreTextView.setVisibility(View.GONE);
        } else {
            scoreTextView.setVisibility(View.VISIBLE);
            scoreTextView.setText("Score: " + score); // Inicializar exibição de pontuação
        }

        addNewBlocks(); // Iniciar adicionando múltiplos blocos

        // Inicializar e iniciar o loop do jogo
        gameLoopRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isGamePaused) {
                    updateBlocks(); // Atualizar posições dos blocos
                    updateBackgroundTransformation(); // Gerenciar transformações de fundo
                }
                handler.postDelayed(this, 50); // Repetir a cada 50 ms
            }
        };
        handler.post(gameLoopRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Pausar a música de fundo quando a atividade é pausada
        if (backgroundMusic != null && backgroundMusic.isPlaying()) {
            backgroundMusic.pause();
            Log.d("MainActivity", "Background music paused.");
        }
        // Remover callbacks para evitar vazamentos
        handler.removeCallbacks(gameLoopRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Retomar a música de fundo quando a atividade é retomada
        boolean isMusicEnabled = sharedPreferences.getBoolean("music_enabled", true);
        if (backgroundMusic != null && isMusicEnabled && !backgroundMusic.isPlaying()) {
            backgroundMusic.start();
            Log.d("MainActivity", "Background music resumed.");
        }
        // Retomar o loop do jogo
        handler.post(gameLoopRunnable);
    }

    private void addNewBlocks() {
        for (int i = 0; i < maxBlocks; i++) {
            addNewBlock();
        }
    }

    private void addNewBlock() {
        gameArea.post(() -> {
            int width = gameArea.getWidth();
            int height = gameArea.getHeight();
            if (width > 200) { // Garantir que haja espaço para o novo tamanho
                final View block = new View(MainActivity.this);

                // Definir o tamanho do bloco para 150x150
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(150, 150);
                block.setLayoutParams(params);
                block.setBackgroundColor(randomColor());

                // Gerar uma posição aleatória para o bloco
                float x, y;
                boolean isOverlapping;
                do {
                    x = random.nextInt(Math.max(width - 150, 1)); // Prevenir faixa negativa
                    y = random.nextInt(Math.max(height / 2 - 150, 1)); // Limitar blocos à metade superior da tela
                    isOverlapping = false;

                    // Verificar se a nova posição sobrepõe algum bloco existente
                    for (View existingBlock : activeBlocks) {
                        float existingX = existingBlock.getX();
                        float existingY = existingBlock.getY();
                        if (Math.abs(x - existingX) < 150 && Math.abs(y - existingY) < 150) {
                            isOverlapping = true;
                            break;
                        }
                    }
                } while (isOverlapping && activeBlocks.size() > 0); // Repetir enquanto houver sobreposição

                block.setX(x);
                block.setY(y);
                gameArea.addView(block);
                activeBlocks.add(block); // Adicionar à lista de blocos ativos

                block.setOnTouchListener(new View.OnTouchListener() {
                    float dX, dY;

                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        if (isGamePaused) {
                            return false; // Ignorar toques quando o jogo está pausado
                        }
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                dX = view.getX() - event.getRawX();
                                dY = view.getY() - event.getRawY();
                                break;
                            case MotionEvent.ACTION_MOVE:
                                view.setX(event.getRawX() + dX);
                                view.setY(event.getRawY() + dY);
                                break;
                            case MotionEvent.ACTION_UP:
                                // Animação de expansão do bloco
                                view.animate()
                                        .scaleX(1.5f)
                                        .scaleY(1.5f)
                                        .setDuration(200)
                                        .setInterpolator(new LinearInterpolator())
                                        .withEndAction(() -> {
                                            if (gameMode.equals("NORMAL")) {
                                                if (sharedPreferences.getBoolean("sound_effects_enabled", true)) {
                                                    playSuccessSound(); // Tocar som de sucesso
                                                }
                                                score++;
                                                updateScoreAndSpeed();
                                                gameArea.removeView(view);
                                                activeBlocks.remove(view); // Remover da lista de blocos ativos
                                                addNewBlock(); // Adicionar um novo bloco após tocar
                                            } else if (gameMode.equals("ZEN")) {
                                                // No modo Zen, remover o bloco e tocar som
                                                if (sharedPreferences.getBoolean("sound_effects_enabled", true)) {
                                                    playSuccessSound(); // Tocar som mesmo no modo Zen
                                                }
                                                gameArea.removeView(view);
                                                activeBlocks.remove(view); // Remover da lista de blocos ativos
                                                addNewBlock(); // Adicionar um novo bloco após tocar
                                            }
                                        })
                                        .start();
                                break;
                        }
                        return true;
                    }
                });
            }
        });
    }

    private void updateBlocks() {
        if (activeBlocks.isEmpty()) return;

        for (int i = activeBlocks.size() - 1; i >= 0; i--) {
            View block = activeBlocks.get(i);
            if (block.getParent() != null) {
                if (!isGamePaused) {
                    block.setY(block.getY() + speedFactor);
                    if (block.getY() > gameArea.getHeight()) {
                        if (gameMode.equals("NORMAL")) {
                            gameArea.removeView(block);
                            activeBlocks.remove(block);
                            if (sharedPreferences.getBoolean("sound_effects_enabled", true)) {
                                playErrorSound();
                            }
                            showFinalScore(); // Mostrar pontuação final ao perder
                            break; // Sair do loop já que o jogo está pausado
                        } else if (gameMode.equals("ZEN")) {
                            // No modo Zen, apenas remover o bloco sem pausar o jogo
                            gameArea.removeView(block);
                            activeBlocks.remove(block);
                            addNewBlock(); // Adicionar um novo bloco
                        }
                    }
                }
            }
        }
    }

    private void updateBackgroundTransformation() {
        // Controlar a taxa em que os blocos transformadores são adicionados
        backgroundTransformationCounter++;
        if (backgroundTransformationCounter >= BACKGROUND_TRANSFORMATION_INTERVAL) {
            backgroundTransformationCounter = 0;
            // Limitar o número de blocos transformadores simultâneos
            int maxTransformingBlocks = 5; // Reduzido para 5 para diminuir o número de quadrados brancos
            if (transformingBlocks.size() < maxTransformingBlocks) {
                addTransformingBlock();
            }
        }

        // Atualizar e remover blocos transformadores
        for (int i = transformingBlocks.size() - 1; i >= 0; i--) {
            View block = transformingBlocks.get(i);
            if (block.getAlpha() <= 0) {
                gameArea.removeView(block);
                transformingBlocks.remove(i);
            } else {
                // Fazer o bloco desaparecer gradualmente
                block.setAlpha(block.getAlpha() - 0.02f);
            }
        }
    }

    private void addTransformingBlock() {
        int width = gameArea.getWidth();
        int height = gameArea.getHeight();

        // Gerar blocos não interativos para simular transformação
        final View block = new View(MainActivity.this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(100, 100);
        block.setLayoutParams(params);

        // Cor clara para o início da transformação
        block.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.white));
        block.setAlpha(1f);

        // Gerar posições aleatórias para os blocos
        block.setX(random.nextInt(Math.max(width - 150, 1)));
        block.setY(random.nextInt(Math.max(height - 150, 1)));
        gameArea.addView(block);
        transformingBlocks.add(block);
    }

    private void playSuccessSound() {
        // Verificar se o som não está tocando e tocá-lo
        if (successSound != null && !successSound.isPlaying()) {
            successSound.start();
        }
    }

    private void playErrorSound() {
        // Verificar se o som não está tocando e tocá-lo
        if (errorSound != null && !errorSound.isPlaying()) {
            errorSound.start();
        }
    }

    private void updateScoreAndSpeed() {
        if (scoreTextView.getVisibility() == View.VISIBLE) {
            scoreTextView.setText("Score: " + score);
        }

        // Aumentar a velocidade mais gradualmente, a cada 20 pontos
        if (score % 40 == 0 && score != 0) {
            if (speedFactor < MAX_SPEED_FACTOR) {
                speedFactor += SPEED_INCREMENT; // Incremento mais suave
                // Opcional: Limitar a velocidade máxima
                if (speedFactor > MAX_SPEED_FACTOR) {
                    speedFactor = MAX_SPEED_FACTOR;
                }
                Log.d("MainActivity", "Speed increased to: " + speedFactor);
            }
        }
    }

    private void showFinalScore() {
        if (isDialogShowing) {
            return; // Evitar múltiplos diálogos
        }
        isGamePaused = true; // Pausar o jogo
        showContinueDialog(); // Mostrar diálogo para o jogador
    }

    private void showContinueDialog() {
        runOnUiThread(() -> {
            if (isFinishing() || isDialogShowing) {
                return;
            }
            isDialogShowing = true;
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("You Lose!");
            builder.setMessage("Do you want to continue playing with the current score?");
            builder.setCancelable(false);
            builder.setPositiveButton("Yes", (dialog, which) -> {

                publicidade.tryToShowRewardedAd();

                isGamePaused = false; // Retomar o jogo
                resetBlocks(); // Reiniciar apenas os blocos
                isDialogShowing = false;
            });
            builder.setNegativeButton("No", (dialog, which) -> {
                isGamePaused = false; // Retomar o jogo
                resetGame(); // Reiniciar completamente o jogo
                isDialogShowing = false;
            });
            AlertDialog dialog = builder.create();
            dialog.show();

            // Definir fonte personalizada e tamanho para o diálogo
            TextView titleView = dialog.findViewById(android.R.id.title);
            if (titleView != null) {
                Typeface typeface = ResourcesCompat.getFont(MainActivity.this, R.font.font);
                titleView.setTypeface(typeface);
                titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
            }

            TextView messageView = dialog.findViewById(android.R.id.message);
            if (messageView != null) {
                Typeface typeface = ResourcesCompat.getFont(MainActivity.this, R.font.font);
                messageView.setTypeface(typeface);
                messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
            }

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null) {
                Typeface typeface = ResourcesCompat.getFont(MainActivity.this, R.font.font);
                positiveButton.setTypeface(typeface);
                positiveButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
            }

            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negativeButton != null) {
                Typeface typeface = ResourcesCompat.getFont(MainActivity.this, R.font.font);
                negativeButton.setTypeface(typeface);
                negativeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 25);
            }
        });
    }

    private void resetBlocks() {
        // Remover todos os blocos da tela para evitar sobrecarga
        for (View block : activeBlocks) {
            gameArea.removeView(block);
        }
        activeBlocks.clear();
        // Reiniciar o jogo com novos blocos
        addNewBlocks();
    }

    private void resetGame() {
        score = 0;
        speedFactor = INITIAL_SPEED_FACTOR; // Redefinir velocidade inicial de forma consistente
        if (scoreTextView.getVisibility() == View.VISIBLE) {
            scoreTextView.setText("Score: " + score);
        }

        // Remover todos os blocos da tela para evitar sobrecarga
        for (View block : activeBlocks) {
            gameArea.removeView(block);
        }
        activeBlocks.clear();

        // Remover blocos transformadores
        for (View block : transformingBlocks) {
            gameArea.removeView(block);
        }
        transformingBlocks.clear();

        // Reiniciar o jogo com novos blocos
        addNewBlocks();
    }

    private int randomColor() {
        int[] colors;
        if (gameMode.equals("NORMAL")) {
            colors = new int[]{
                    ContextCompat.getColor(this, R.color.colorAccent),
                    ContextCompat.getColor(this, R.color.colorSoftAccent2),
                    ContextCompat.getColor(this, R.color.colorVibrant),
                    ContextCompat.getColor(this, R.color.colorWarm1),
                    ContextCompat.getColor(this, R.color.colorWarm2),
                    ContextCompat.getColor(this, R.color.colorStrong),
                    ContextCompat.getColor(this, R.color.colorDarkAccent)
            };
        } else { // Modo Zen
            colors = new int[]{
                    ContextCompat.getColor(this, R.color.colorSoftAccent2),
                    ContextCompat.getColor(this, R.color.colorVibrant),
                    ContextCompat.getColor(this, R.color.colorAccent)
            };
        }
        return colors[random.nextInt(colors.length)];
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Liberar recursos de áudio ao destruir a atividade
        if (successSound != null) {
            successSound.release();
            successSound = null;
        }
        if (errorSound != null) {
            errorSound.release();
            errorSound = null;
        }
        if (backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.release();
            backgroundMusic = null;
        }
        // Remover todos os Runnables pendentes para evitar interações posteriores
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onBackPressed() {
        // Sobrescrever o comportamento do botão de voltar para retornar ao Menu
        Intent intent = new Intent(MainActivity.this, MenuActivity.class);
        startActivity(intent);
        finish();
    }
}
