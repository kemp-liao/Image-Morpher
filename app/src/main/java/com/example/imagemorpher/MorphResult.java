package com.example.imagemorpher;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

import android.util.Pair;

public class MorphResult extends AppCompatActivity {

    int numOfFrames;

    ArrayList<Pair<Line, Line>> pairsList;

    Bitmap sourceImage, destinationImage;

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
        pairsList = new ArrayList<>();

        //Get data
        this.getData();


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

    private void getData() {
        //Extract the data passed in
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("numOfFrames")
                && intent.hasExtra("sourceImageUri")
                && intent.hasExtra("destinationImageUri")
                && intent.hasExtra("startX1") && intent.hasExtra("startY1")
                && intent.hasExtra("endX1") && intent.hasExtra("endY1")
                && intent.hasExtra("startX2") && intent.hasExtra("startY2")
                && intent.hasExtra("endX2") && intent.hasExtra("endY2")) {
            //Set data
            this.numOfFrames = intent.getIntExtra("numOfFrames", 0);
            try {
                this.sourceImage = MediaStore.Images.Media.getBitmap(getContentResolver(), intent.getParcelableExtra("sourceImageUri"));
                this.destinationImage = MediaStore.Images.Media.getBitmap(getContentResolver(), intent.getParcelableExtra("destinationImageUri"));
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
