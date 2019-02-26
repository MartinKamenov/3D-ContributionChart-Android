package com.kamenov.martin.contributionsactivity;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.kamenov.martin.contributionsactivity.engine.GamePanel;
import com.kamenov.martin.contributionsactivity.engine.services.DrawingService;
import com.kamenov.martin.contributionsactivity.engine.services.SortingService;
import com.kamenov.martin.contributionsactivity.engine.services.factories.FigureFactory;

public class MainActivity extends Activity {

    private DrawingService drawingService;
    private GamePanel gamePanel;
    private RelativeLayout relativeLayout;
    private FigureFactory figureFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        figureFactory = FigureFactory.getInstance();
        relativeLayout = findViewById(R.id.container);
        drawingService = DrawingService.getInstance(SortingService.getInstance());
        gamePanel = new GamePanel(this, drawingService);
        relativeLayout.addView(gamePanel);
    }
}
