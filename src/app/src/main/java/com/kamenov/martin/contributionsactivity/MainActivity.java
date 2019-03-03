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
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.gson.Gson;
import com.kamenov.martin.contributionsactivity.constants.Constants;
import com.kamenov.martin.contributionsactivity.contracts.BarType;
import com.kamenov.martin.contributionsactivity.engine.GamePanel;
import com.kamenov.martin.contributionsactivity.engine.models.game_objects.ComplexObject;
import com.kamenov.martin.contributionsactivity.engine.models.game_objects.Parallelepiped;
import com.kamenov.martin.contributionsactivity.engine.models.game_objects.PartsObject;
import com.kamenov.martin.contributionsactivity.engine.models.game_objects.Plane;
import com.kamenov.martin.contributionsactivity.engine.models.game_objects.Pyramid;
import com.kamenov.martin.contributionsactivity.engine.models.game_objects.contracts.DeepPoint;
import com.kamenov.martin.contributionsactivity.engine.models.game_objects.contracts.Object3D;
import com.kamenov.martin.contributionsactivity.engine.services.DrawingService;
import com.kamenov.martin.contributionsactivity.engine.services.PaintService;
import com.kamenov.martin.contributionsactivity.engine.services.SortingService;
import com.kamenov.martin.contributionsactivity.engine.services.factories.FigureFactory;
import com.kamenov.martin.contributionsactivity.internet.HttpRequester;
import com.kamenov.martin.contributionsactivity.internet.contracts.GetHandler;
import com.kamenov.martin.contributionsactivity.models.ContributionDate;
import com.kamenov.martin.contributionsactivity.models.Contributor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Response;

public class MainActivity extends Activity implements GetHandler, View.OnClickListener, RadioGroup.OnCheckedChangeListener {

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
    private int weeksBack;
    private BarType type;
    private Map<String, Paint> edgePaints;
    private Map<String, Paint> wallPaints;

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

        type = BarType.Line;
        weeksBack = 52;

        progressContainer = findViewById(R.id.loader_container);
        progressBarContainer = findViewById(R.id.progressbar_container);
        usernameContainer = findViewById(R.id.username_container);
        usernameInput = findViewById(R.id.username_input);
        usernameBtn = findViewById(R.id.username_btn);
        usernameBtn.setOnClickListener(this);

        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.type_bars);
        radioGroup.setOnCheckedChangeListener(this);

        wallPaints = new HashMap<>();
        edgePaints = new HashMap<>();

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
        final String exeption = ex.getMessage().toString();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, exeption, Toast.LENGTH_SHORT).show();
                progressContainer.setVisibility(View.VISIBLE);
                progressBarContainer.setVisibility(View.GONE);
            }
        });
    }

    private void addDataToFigureFactory(Contributor contributor) {
        Paint edgePaint = PaintService.createEdgePaint("white");
        Float rotationCoef = 0.5f;
        Float barSize = 40f;
        Float sizeCoef = 20f;


        ArrayList<ContributionDate> fullDateContributionInformation = contributor.data.fullDateConributionInformation;
        int additionalCommits = 7 - (fullDateContributionInformation.size() % 7);

        for(int i = 0; i < additionalCommits; i++) {
            ContributionDate emptyContributionDate = new ContributionDate();
            emptyContributionDate.color = "#ebedf0";
            emptyContributionDate.contributions = 0;
            fullDateContributionInformation.add(emptyContributionDate);
        }


        if(weeksBack > 0 && weeksBack < 52) {
            int startIndex = fullDateContributionInformation.size() - (weeksBack * 7);
            ArrayList<ContributionDate> newData = new ArrayList<>();
            for (int i = startIndex; i < fullDateContributionInformation.size(); i++) {
                newData.add(fullDateContributionInformation.get(i));
            }

            fullDateContributionInformation = newData;
        }

        ContributionDate[][] contributionArray = new ContributionDate[7][(fullDateContributionInformation.size() / 7)];
        for(int i = 0; i < fullDateContributionInformation.size(); i++) {
            int row = i % 7;
            int col = i / 7;
            contributionArray[row][col] = fullDateContributionInformation.get(i);
        }

        ArrayList<Object3D> bars = new ArrayList<>();

        float startX = ((-(contributionArray[0].length) * barSize) / 2) + (barSize / 2);
        float startY = ((-(contributionArray.length) * barSize) / 2) + (barSize / 2);


        for(int i = 0; i < contributionArray.length; i++) {
            for(int j = 0; j < contributionArray[i].length; j++) {

                float x = (j * barSize) + startX;
                float y = (i * barSize) + startY;


                float passedBarSize = barSize;


                if(!wallPaints.containsKey(contributionArray[i][j].color)) {
                    wallPaints.put(contributionArray[i][j].color,
                             PaintService.createWallPaint(contributionArray[i][j].color));
                }

                Paint wallPaint = wallPaints.get(contributionArray[i][j].color);

                if(type == BarType.Line) {
                    if(!edgePaints.containsKey(contributionArray[i][j].color)) {
                        edgePaints.put(contributionArray[i][j].color,
                                PaintService.createEdgePaint(contributionArray[i][j].color));
                    }

                    edgePaint = edgePaints.get(contributionArray[i][j].color);
                }

                addObjectToComplexObject(type, bars, x, y, contributionArray[i][j].contributions,
                        passedBarSize, edgePaint, wallPaint, rotationCoef, sizeCoef);

            }
        }

        if(type == BarType.Line) {
            float aLength = (contributionArray[0].length * barSize) / (weeksBack / 4);
            float bLength = contributionArray.length * barSize;
            Paint wallPaint = PaintService.createWallPaint("#ebedf0");

            for (int i = 0; i < weeksBack / 4; i++) {

                Plane bottomPlane = figureFactory.createPlane(
                        (i * aLength) + startX,
                        0,
                        0,
                        edgePaint,
                        wallPaint,
                        rotationCoef,
                        aLength,
                        bLength);
                bars.add(bottomPlane);
            }
        }

        ArrayList<Object3D> allObjects = new ArrayList<>();
        Paint wallPaint = PaintService.createWallPaint("#ebedf0");
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

    private void addObjectToComplexObject(BarType type,
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
            case Parallelepiped:
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
            case Line:
                x = ((Constants.SCREEN_WIDTH / 4) + x / 2);
                y = ((Constants.SCREEN_HEIGHT / 4) + y / 2);
                DeepPoint firstPoint = new DeepPoint(x, y,0);
                DeepPoint secondDeepPoint = new DeepPoint(x, y, contributions * sizeCoef);

                DeepPoint[] points = new DeepPoint[] {firstPoint, secondDeepPoint};
                ArrayList<DeepPoint[]> parts = new ArrayList<>();
                parts.add(points);

                PartsObject line =  new PartsObject(x, y, 0, edgePaint, wallPaint,
                        rotationCoef, points, parts);
                bars.add(line);
                return;
            case Pyramid:
                if(contributions == 0) {
                    Plane bottomPlane = figureFactory.createPlane(x, y, 0, edgePaint, wallPaint,
                            rotationCoef, barSize, barSize);
                    bars.add(bottomPlane);
                    return;
                }
                Pyramid pyramid = figureFactory.createPyramid(
                        x,
                        y,
                        0,
                        edgePaint,
                        wallPaint,
                        rotationCoef,
                        barSize,
                        barSize,
                        contributions * sizeCoef
                );
                bars.add(pyramid);
                return;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int i) {
        switch(i){
            case R.id.bars:
                Toast.makeText(this, "Bars", Toast.LENGTH_SHORT).show();
                type = BarType.Parallelepiped;
                weeksBack = 10;
                break;
            case R.id.lines:
                Toast.makeText(this, "Lines", Toast.LENGTH_SHORT).show();
                type = BarType.Line;
                weeksBack = 52;
                break;
            case R.id.pyramids:
                Toast.makeText(this, "Pyramids", Toast.LENGTH_SHORT).show();
                type = BarType.Pyramid;
                weeksBack = 20;
                break;
        }
    }
}
