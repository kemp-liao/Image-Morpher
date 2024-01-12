package com.example.imagemorpher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    ImageView sourceImage, destinationImage;

    DrawingView sourceImageDrawingView, destinationImageDrawingView;

    ArrayList<Pair<Line, Line>> pairList;

    boolean isSourceSet = false;
    boolean isDestinationSet = false;

    int imageClicked = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        setListeners();
    }

    private void init() {
        //Set up toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Initialization
        pairList = new ArrayList<>();

        //Get objects
        sourceImage = findViewById(R.id.sourceImage);
        destinationImage = findViewById(R.id.destinationImage);
        sourceImageDrawingView = findViewById(R.id.sourceImageDrawingView);
        destinationImageDrawingView = findViewById(R.id.destinationImageDrawingView);

        //Disable views
        sourceImageDrawingView.setEnabled(false);
        destinationImageDrawingView.setEnabled(false);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.resetAllBtn) {
            resetAll();
            showToast("All images and lines removed");
        } else if (item.getItemId() == R.id.removeAllLinesBtn) {
            removeAllLines();
            showToast("All lines removed");
        } else if (item.getItemId() == R.id.removeLastLineBtn) {
            removeLastLine();
            showToast("Last line removed");
        } else if (item.getItemId() == R.id.numberOfFramesBtn) {

        } else if (item.getItemId() == R.id.morphBtn) {

        }
        return true;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setListeners() {
        sourceImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSourceSet) {
                    imageClicked = 0;
                    openGallery();
                }
            }
        });

        destinationImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDestinationSet) {
                    imageClicked = 1;
                    openGallery();
                }
            }
        });

        //Regular click/press event
        sourceImageDrawingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Line line = sourceImageDrawingView.editLine(event, pairList);
                if (line != null) {
                    //Create another line for the destination view
                    Line newLine = new Line(new PointF(line.getStart().x, line.getStart().y), new PointF(line.getEnd().x, line.getEnd().y));
                    //Add new line to the destination view
                    destinationImageDrawingView.addLine(newLine);
                    //Make a pair and it to the list
                    pairList.add(new Pair<>(line, newLine));
                }
                return true;
            }
        });

        destinationImageDrawingView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Line line = destinationImageDrawingView.editLine(event, pairList);
                if (line != null) {
                    Line newLine = new Line(new PointF(line.getStart().x, line.getStart().y), new PointF(line.getEnd().x, line.getEnd().y));
                    sourceImageDrawingView.addLine(newLine);
                    pairList.add(new Pair<>(newLine, line));
                }
                return true;
            }
        });
    }

    private void showToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    private void removeAllLines() {
        sourceImageDrawingView.removeAllLines();
        destinationImageDrawingView.removeAllLines();
        if (!pairList.isEmpty()) {
            pairList.clear();
        }
    }

    private void removeLastLine() {
        sourceImageDrawingView.removeLastLine();
        destinationImageDrawingView.removeLastLine();
        if (!pairList.isEmpty()) {
            pairList.remove(pairList.size() - 1);
        }
    }

    private void resetAll() {
        //Remove all lines
        removeAllLines();
        //Reset flags
        isSourceSet = false;
        isDestinationSet = false;
        sourceImageDrawingView.setEnabled(false);
        destinationImageDrawingView.setEnabled(false);
        //Reset the images
        sourceImage.setImageResource(R.drawable.add);
        destinationImage.setImageResource(R.drawable.add);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        openPicture.launch(intent);
    }

    ActivityResultLauncher<Intent> openPicture = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        assert data != null;
                        Uri selectedImageUri = data.getData();
                        if (imageClicked == 0) {
                            sourceImage.setImageURI(selectedImageUri);
                            isSourceSet = true;
                            showToast("Source image set");
                            if (isDestinationSet) {
                                sourceImageDrawingView.setEnabled(true);
                                destinationImageDrawingView.setEnabled(true);
                            }
                        } else if (imageClicked == 1) {
                            destinationImage.setImageURI(selectedImageUri);
                            isDestinationSet = true;
                            showToast("Destination image set");
                            if (isSourceSet) {
                                sourceImageDrawingView.setEnabled(true);
                                destinationImageDrawingView.setEnabled(true);
                            }
                        }
                    }
                }
            });


}