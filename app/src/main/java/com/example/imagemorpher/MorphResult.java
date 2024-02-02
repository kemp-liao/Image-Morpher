package com.example.imagemorpher;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;

import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MorphResult extends AppCompatActivity {

    /**
     * The result image view
     */
    private ImageView resultView;

    /**
     * Three main buttons: previous image, play all images, next image
     */
    private ImageButton previous, play, next;

    /**
     * Current image index
     */
    private int currentImageIndex;

    /**
     * Number of frames to generate
     */
    private int numOfFrames;

    /**
     * The user input lines
     */
    private ArrayList<Pair<Line, Line>> pairsList;

    /**
     * Source image and destination image
     */
    private Bitmap sourceImage, destinationImage;

    /**
     * Text message that shows current image index
     */
    private TextView imageIndexText;

    /**
     * Time benchmark
     */
    private TextView timeElapsedText;

    /**
     * The morphing result that returned by Morph
     */
    private Bitmap[] results;

    /**
     * Threading switch
     */
    private boolean isThreadingOn;

    private Handler handler;

    private boolean isPlaying;

    private int frameDelay = 200;

    private int width, height;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.morph_result);

        //Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        //Initialize
        currentImageIndex = 0;
        isPlaying = false;
        pairsList = new ArrayList<>();
        handler = new Handler();
        this.resultView = findViewById(R.id.morphResultImage);
        this.previous = findViewById(R.id.backBtn);
        this.play = findViewById(R.id.playBtn);
        this.next = findViewById(R.id.nextBtn);
        this.imageIndexText = findViewById(R.id.imageIndex);
        this.timeElapsedText = findViewById(R.id.timeElapsed);

        //Get data
        this.getData();

        //Run morph process
        this.runMorph();
    }

    @SuppressLint("SetTextI18n")
    private void runMorph() {
        //Set benchmark
        long startTime = System.nanoTime();
        long endTime = 0;
        //Create Morph object
        Morph morph = new Morph(numOfFrames, sourceImage, destinationImage, pairsList, isThreadingOn);
        //Get results
        results = morph.getResults();
        if (results != null) {
            endTime = System.nanoTime();
            //Show results
            this.playResults();
            //Add listeners
            this.setListeners();
        }
        //Show benchmark
        double elapsedTimeInSeconds = (endTime - startTime) / 1000000000.0;
        @SuppressLint("DefaultLocale")
        String formattedTime = String.format("%.6f", elapsedTimeInSeconds);
        timeElapsedText.setText("Time elapsed: " + formattedTime + "s");
    }

    @SuppressLint("SetTextI18n")
    private void playResults() {
        if (isPlaying) {
            isPlaying = false;
            onPause();
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isPlaying = true;
                    resultView.setImageBitmap(results[currentImageIndex]);
                    imageIndexText.setText("Current image: " + (currentImageIndex + 1));
                    currentImageIndex = (currentImageIndex + 1) % results.length;
                    handler.postDelayed(this, frameDelay);
                }
            }, frameDelay);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Handle return button
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void setListeners() {
        previous.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                onPause();
                isPlaying = false;
                if (currentImageIndex > 0) {
                    resultView.setImageBitmap(results[--currentImageIndex]);
                    imageIndexText.setText("Current image: " + (currentImageIndex + 1));
                }
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playResults();
            }
        });

        next.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                onPause();
                isPlaying = false;
                if (currentImageIndex < (numOfFrames + 1)) {
                    resultView.setImageBitmap(results[++currentImageIndex]);
                    imageIndexText.setText("Current image: " + (currentImageIndex + 1));
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacksAndMessages(null);
    }

    private Bitmap cropImage(Bitmap img, int width, int height) {
        float ratioA = (float) width / img.getWidth();
        float ratioB = (float) height / img.getHeight();
        Bitmap temp;
        if (ratioA > ratioB) {
            //Take width as base
            temp = Bitmap.createScaledBitmap(img, width, (width * img.getHeight() / img.getWidth()), true);
        } else {
            //Take height as base
            temp = Bitmap.createScaledBitmap(img, width, (height * img.getWidth() / img.getHeight()), true);
        }
        int startX = (img.getWidth() - width) / 2;
        int startY = (img.getHeight() - height) / 2;
        startX = Math.max(startX, 0);
        startY = Math.max(startY, 0);
        int cropWidth = Math.min(width, img.getWidth() - startX);
        int cropHeight = Math.min(height, img.getHeight() - startY);
        return Bitmap.createBitmap(temp, startX, startY, cropWidth, cropHeight);
    }

    private void getData() {
        //Extract the data passed in
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("numOfFrames")
                && intent.hasExtra("sourceImageUri")
                && intent.hasExtra("destinationImageUri")
                && intent.hasExtra("startX1") && intent.hasExtra("startY1")
                && intent.hasExtra("endX1") && intent.hasExtra("endY1")
                && intent.hasExtra("startX2") && intent.hasExtra("startY2")
                && intent.hasExtra("endX2") && intent.hasExtra("endY2")
                && intent.hasExtra("isThreadingOn") && intent.hasExtra("imgWidth")
                && intent.hasExtra("imgHeight")) {
            //Set data
            this.numOfFrames = intent.getIntExtra("numOfFrames", 0);
            this.isThreadingOn = intent.getBooleanExtra("isThreadingOn", true);
            this.width = intent.getIntExtra("imgWidth", 0);
            this.height = intent.getIntExtra("imgHeight", 0);
            try {
                Bitmap originalSource = MediaStore.Images.Media.getBitmap(getContentResolver(), intent.getParcelableExtra("sourceImageUri"));
                this.sourceImage = Bitmap.createScaledBitmap(originalSource, width, height, true);
                Bitmap originalDestination = MediaStore.Images.Media.getBitmap(getContentResolver(), intent.getParcelableExtra("destinationImageUri"));
                this.destinationImage = Bitmap.createScaledBitmap(originalDestination, width, height, true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            float[] startX1 = intent.getFloatArrayExtra("startX1");
            float[] startY1 = intent.getFloatArrayExtra("startY1");
            float[] endX1 = intent.getFloatArrayExtra("endX1");
            float[] endY1 = intent.getFloatArrayExtra("endY1");
            float[] startX2 = intent.getFloatArrayExtra("startX2");
            float[] startY2 = intent.getFloatArrayExtra("startY2");
            float[] endX2 = intent.getFloatArrayExtra("endX2");
            float[] endY2 = intent.getFloatArrayExtra("endY2");
            //Assert arrays
            assert startX1 != null;
            assert startY1 != null;
            assert endX1 != null;
            assert endY1 != null;
            assert startX2 != null;
            assert startY2 != null;
            assert endX2 != null;
            assert endY2 != null;
            for (int i = 0; i < startY1.length; i++) {
                PointF start1 = new PointF(startX1[i], startY1[i]);
                PointF end1 = new PointF(endX1[i], endY1[i]);
                PointF start2 = new PointF(startX2[i], startY2[i]);
                PointF end2 = new PointF(endX2[i], endY2[i]);
                Line line1 = new Line(start1, end1);
                Line line2 = new Line(start2, end2);
                pairsList.add(new Pair<>(line1, line2));
            }
        }
    }
}
