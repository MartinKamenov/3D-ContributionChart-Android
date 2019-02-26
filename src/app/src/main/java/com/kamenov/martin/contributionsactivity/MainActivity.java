package com.kamenov.martin.contributionsactivity;

import android.app.Activity;
import android.graphics.Paint;
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
import com.kamenov.martin.contributionsactivity.engine.models.game_objects.ComplexObject;
import com.kamenov.martin.contributionsactivity.engine.models.game_objects.Parallelepiped;
import com.kamenov.martin.contributionsactivity.engine.models.game_objects.Plane;
import com.kamenov.martin.contributionsactivity.engine.models.game_objects.contracts.Object3D;
import com.kamenov.martin.contributionsactivity.engine.services.DrawingService;
import com.kamenov.martin.contributionsactivity.engine.services.PaintService;
import com.kamenov.martin.contributionsactivity.engine.services.SortingService;
import com.kamenov.martin.contributionsactivity.engine.services.factories.FigureFactory;
import com.kamenov.martin.contributionsactivity.internet.HttpRequester;
import com.kamenov.martin.contributionsactivity.internet.contracts.GetHandler;
import com.kamenov.martin.contributionsactivity.models.Contributor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
                addDataToFigureFactory(contributor);
                gamePanel = new GamePanel(MainActivity.this, drawingService);
                relativeLayout.addView(gamePanel);
            }
        });
    }

    @Override
    public void handleError(Call call, Exception ex) {

    }

    private void addDataToFigureFactory(Contributor contributor) {
        int weeksBack = 10;
        Paint edgePaint = PaintService.createEdgePaint("white");
        Paint wallPaint = PaintService.createWallPaint("red");
        Float rotationCoef = 1f;
        Float barSize = 20f;
        Float sizeCoef = 10f;

        ArrayList<Integer> dateContributionsNumbers = contributor.data.dateContributionsNumbers;
        int startIndex = dateContributionsNumbers.size() - (weeksBack * 7);
        ArrayList<Integer> newData = new ArrayList<>();
        for(int i = startIndex; i < dateContributionsNumbers.size(); i++) {
            newData.add(dateContributionsNumbers.get(i));
        }

        dateContributionsNumbers = newData;

        int[][] contributionArray = new int[7][(dateContributionsNumbers.size() / 7) + 1];
        for(int i = 0; i < dateContributionsNumbers.size(); i++) {
            int row = i % 7;
            int col = i / 7;
            contributionArray[row][col] = dateContributionsNumbers.get(i);
        }

        ArrayList<Object3D> bars = new ArrayList<>();

        float startX = (-(contributionArray[0].length) * barSize) / 2;
        float startY = (-(contributionArray.length) * barSize) / 2;


        for(int i = 0; i < contributionArray.length; i++) {
            for(int j = 0; j < contributionArray[i].length; j++) {
                float x = (j * barSize) + startX;
                float y = (i * barSize) + startY;
                Parallelepiped parallelepiped = figureFactory.createParallelepiped(
                        x,
                        y,
                        (contributionArray[i][j] * sizeCoef) / 2,
                        barSize,
                        barSize,
                        contributionArray[i][j] * sizeCoef,
                        edgePaint,
                        wallPaint,
                        rotationCoef
                );
                bars.add(parallelepiped);
            }
        }

        ArrayList<Object3D> allObjects = new ArrayList<>();
        ComplexObject allBars = figureFactory.createComplexObject(0 ,0 ,0 ,
                wallPaint, edgePaint, rotationCoef, bars);
        allObjects.add(allBars);
        figureFactory.setFigures(allObjects);
    }
}
