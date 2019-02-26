package com.kamenov.martin.contributionsactivity;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.google.gson.Gson;
import com.kamenov.martin.contributionsactivity.constants.Constants;
import com.kamenov.martin.contributionsactivity.engine.GamePanel;
import com.kamenov.martin.contributionsactivity.engine.services.DrawingService;
import com.kamenov.martin.contributionsactivity.engine.services.SortingService;
import com.kamenov.martin.contributionsactivity.engine.services.factories.FigureFactory;
import com.kamenov.martin.contributionsactivity.internet.HttpRequester;
import com.kamenov.martin.contributionsactivity.internet.contracts.GetHandler;
import com.kamenov.martin.contributionsactivity.models.Contributor;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Response;

public class MainActivity extends Activity implements GetHandler {

    private DrawingService drawingService;
    private GamePanel gamePanel;
    private RelativeLayout relativeLayout;
    private FigureFactory figureFactory;
    private HttpRequester requester;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        Constants.SCREEN_WIDTH = dm.widthPixels;
        Constants.SCREEN_HEIGHT = dm.heightPixels;
        setContentView(R.layout.activity_main);

        requester = new HttpRequester();
        gson = new Gson();
        figureFactory = FigureFactory.getInstance();
        relativeLayout = findViewById(R.id.container);
        drawingService = DrawingService.getInstance(SortingService.getInstance());
        makeRequestForContributions();
        //gamePanel = new GamePanel(this, drawingService);
        //relativeLayout.addView(gamePanel);
    }

    private void makeRequestForContributions() {
        requester.get(this, "https://github-analyzator-api.herokuapp.com/github/contributions/martinkamenov");
    }

    @Override
    public void handleGet(Call call, Response response) {
        String result = "";
        try {
            result = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        final String finalResult = result;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Contributor contributor = gson.fromJson(finalResult, Contributor.class);
                gamePanel = new GamePanel(MainActivity.this, drawingService);
                relativeLayout.addView(gamePanel);
            }
        });
    }

    @Override
    public void handleError(Call call, Exception ex) {

    }
}
