package com.kamenov.martin.contributionsactivity;

import android.app.Activity;
import android.graphics.Paint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
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

public class MainActivity extends Activity implements GetHandler, View.OnClickListener {

    private DrawingService drawingService;
    private GamePanel gamePanel;
    private RelativeLayout relativeLayout;
    private FigureFactory figureFactory;
    private HttpRequester requester;
    private Gson gson;
    private View progressContainer;
    private Button usernameBtn;
    private EditText usernameInput;
    private View usernameContainer;
    private View progressBarContainer;

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

        progressContainer = findViewById(R.id.loader_container);
        progressBarContainer = findViewById(R.id.progressbar_container);
        usernameContainer = findViewById(R.id.username_container);
        usernameInput = findViewById(R.id.username_input);
        usernameBtn = findViewById(R.id.username_btn);
        usernameBtn.setOnClickListener(this);

        requester = new HttpRequester();
        gson = new Gson();
        figureFactory = FigureFactory.getInstance();
        relativeLayout = findViewById(R.id.container);
        drawingService = DrawingService.getInstance(SortingService.getInstance());
    }

    private void makeRequestForContributions(String username) {
        String url = "https://github-analyzator-api.herokuapp.com/github/contributions/" + username;
        requester.get(this, url);
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
                progressContainer.setVisibility(View.GONE);
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
        Float rotationCoef = 0.5f;
        Float barSize = 20f;
        Float sizeCoef = 10f;
        String type = "Para";


        ArrayList<Integer> dateContributionsNumbers = contributor.data.dateContributionsNumbers;
        int additionalCommits = 7 - (dateContributionsNumbers.size() % 7);

        for(int i = 0; i < additionalCommits; i++) {
            dateContributionsNumbers.add(0);
        }


        if(weeksBack > 0 && weeksBack < 52) {
            int startIndex = dateContributionsNumbers.size() - (weeksBack * 7);
            ArrayList<Integer> newData = new ArrayList<>();
            for (int i = startIndex; i < dateContributionsNumbers.size(); i++) {
                newData.add(dateContributionsNumbers.get(i));
            }

            dateContributionsNumbers = newData;
        }

        int[][] contributionArray = new int[7][(dateContributionsNumbers.size() / 7)];
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


                addObjectToComplexObject(type, bars, x, y, contributionArray[i][j],
                        barSize, edgePaint, wallPaint, rotationCoef, sizeCoef);

            }
        }

//        Plane bottomPlane = figureFactory.createPlane(0, 0,0 ,edgePaint, wallPaint,
//                rotationCoef, contributionArray[0].length * barSize, contributionArray.length * barSize);
//        bars.add(bottomPlane);

        ArrayList<Object3D> allObjects = new ArrayList<>();
        ComplexObject allBars = figureFactory.createComplexObject(0 ,0 ,0 ,
                wallPaint, edgePaint, rotationCoef, bars);
        allObjects.add(allBars);
        figureFactory.setFigures(allObjects);
    }

    @Override
    public void onClick(View view) {
        String username = usernameInput.getText().toString();
        usernameContainer.setVisibility(View.GONE);
        progressBarContainer.setVisibility(View.VISIBLE);
        makeRequestForContributions(username);
    }

    private void addObjectToComplexObject(String type,
                                          ArrayList<Object3D> bars,
                                          float x,
                                          float y,
                                          float contributions,
                                          float barSize,
                                          Paint edgePaint,
                                          Paint wallPaint,
                                          float rotationCoef,
                                          float sizeCoef) {
        switch (type) {
            case "Para":
                if(contributions == 0) {
                    Plane bottomPlane = figureFactory.createPlane(x, y, 0, edgePaint, wallPaint,
                            rotationCoef, barSize, barSize);
                    bars.add(bottomPlane);
                    return;
                }
                Parallelepiped parallelepiped = figureFactory.createParallelepiped(
                        x,
                        y,
                        (contributions * sizeCoef) / 2,
                        barSize,
                        barSize,
                        contributions * sizeCoef,
                        edgePaint,
                        wallPaint,
                        rotationCoef
                );
                bars.add(parallelepiped);
                return;
            case "Lines":
                return;
        }
    }
}
