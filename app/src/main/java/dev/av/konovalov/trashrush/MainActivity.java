package dev.av.konovalov.trashrush;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private GameView gameView;
    private MainMenuView mainMenuView;
    private GameOverOverlay gameOverOverlay;
    private AppState currentState = AppState.MENU;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        FrameLayout mainLayout = new FrameLayout(this);
        setContentView(mainLayout);

        mainMenuView = new MainMenuView(this);
        mainMenuView.setMenuListener(new MainMenuView.MenuListener() {
            @Override
            public void onContinueClicked() {
                runOnUiThread(() -> continueGame());
            }

            @Override
            public void onPlayClicked() {
                runOnUiThread(() -> restartGame());
            }

            @Override
            public void onExitClicked() {
                runOnUiThread(() -> finish());
            }
        });

        gameView = new GameView(this);
        gameView.setGameEventListener(new GameView.GameEventListener() {
            @Override
            public void onGameOver(int itemsSorted, float treesSaved, float waterSaved, float co2Saved) {
                runOnUiThread(() -> showGameOver(itemsSorted, treesSaved, waterSaved, co2Saved));
            }

            @Override
            public void onBackToMenu() {
                runOnUiThread(() -> switchToMenu());
            }
        });

        gameOverOverlay = new GameOverOverlay(this);
        gameOverOverlay.setGameOverListener(new GameOverOverlay.GameOverListener() {
            @Override
            public void onRestartClicked() {
                runOnUiThread(() -> restartGame());
            }

            @Override
            public void onMenuClicked() {
                runOnUiThread(() -> switchToMenu());
            }
        });

        mainLayout.addView(gameView);
        mainLayout.addView(gameOverOverlay);
        mainLayout.addView(mainMenuView);

        switchToMenu();
    }

    private void switchToMenu() {
        currentState = AppState.MENU;
        mainMenuView.setVisibility(View.VISIBLE);
        mainMenuView.setContinueButtonVisibility(gameView.canContinue());
        gameOverOverlay.setVisibility(View.GONE);
        gameView.pauseGame();
    }

    private void showGameOver(int itemsSorted, float treesSaved, float waterSaved, float co2Saved) {
        currentState = AppState.GAME_OVER;
        gameOverOverlay.updateStats(itemsSorted, treesSaved, waterSaved, co2Saved);
        gameOverOverlay.setVisibility(View.VISIBLE);
        gameView.pauseGame();
    }

    private void restartGame() {
        mainMenuView.setVisibility(View.GONE);
        gameOverOverlay.setVisibility(View.GONE);
        gameView.restartGame();
        gameView.resumeGame();
        currentState = AppState.GAME;
    }

    private void continueGame() {
        currentState = AppState.GAME;
        mainMenuView.setVisibility(View.GONE);
        gameView.setVisibility(View.VISIBLE);
        gameOverOverlay.setVisibility(View.GONE);
        gameView.resumeGame();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null && currentState == AppState.GAME) {
            gameView.pauseGame();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null && currentState == AppState.GAME) {
            gameView.resumeGame();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentState == AppState.GAME) {
            switchToMenu();
        } else if (currentState == AppState.GAME_OVER) {
            restartGame();
        } else {
            super.onBackPressed();
        }
    }

    private enum AppState {
        MENU, GAME, GAME_OVER
    }
}