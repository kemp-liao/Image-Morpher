package com.example.imagemorpher;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileUtils;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements DialogFragment.DialogListener {


    ImageView sourceImageView, destinationImageView;

    Uri sourceImageUri, destinationImageUri;

    DrawingView sourceImageDrawingView, destinationImageDrawingView;

    MenuItem redoBtn, undoBtn;

    ArrayList<Pair<Line, Line>> pairList;

    ArrayList<Pair<Line, Line>> lastRemovedLines;

    boolean isSourceSet = false;
    boolean isDestinationSet = false;

    boolean isThreadingOn = true;

    int imageWidth, imageHeight;

    int imageClicked = -1;

    final int DEFAULT_DRAWING_VIEW_HEIGHT = 320;

    final int MAX_SIZE = 300;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        setListeners();
    }

    private void init() {
        //Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Initialization
        pairList = new ArrayList<>();
        lastRemovedLines = new ArrayList<>();

        //Get objects
        sourceImageView = findViewById(R.id.sourceImage);
        destinationImageView = findViewById(R.id.destinationImage);
        sourceImageDrawingView = findViewById(R.id.sourceImageDrawingView);
        destinationImageDrawingView = findViewById(R.id.destinationImageDrawingView);
        redoBtn = findViewById(R.id.redoLastLineBtn);
        undoBtn = findViewById(R.id.undoLastLineBtn);

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
        } else if (item.getItemId() == R.id.undoLastLineBtn) {
            if (undoLastLine()) {
                showToast("Last line removed");
            } else {
                showToast("No line can be removed");
            }
        } else if (item.getItemId() == R.id.redoLastLineBtn) {
            if (redoLastLine()) {
                showToast("Last removed line added");
            } else {
                showToast("No line can be added");
            }
        } else if (item.getItemId() == R.id.morphBtn) {
            if (isSourceSet && isDestinationSet) {
                showDialog();
            } else {
                showToast("Please open images first");
            }
        } else if (item.getItemId() == R.id.threadingOn) {
            if (isThreadingOn) {
                item.setChecked(false);
                isThreadingOn = false;
            } else {
                item.setChecked(true);
                isThreadingOn = true;
            }
        }
        return true;
    }

    private void showDialog() {
        DialogFragment dialogFragment = new DialogFragment();
        dialogFragment.setListener(this);
        dialogFragment.show(getSupportFragmentManager(), null);
    }

    @Override
    public void onTextEntered(String text) {
        // Handle the entered text
        Toast.makeText(this, "Frames to generate: " + text, Toast.LENGTH_SHORT).show();
        //Open MorphResult
        this.openMorphResult(Integer.parseInt(text));
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setListeners() {
        sourceImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSourceSet) {
                    imageClicked = 0;
                    openGallery();
                }
            }
        });

        destinationImageView.setOnClickListener(new View.OnClickListener() {
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

    private void removeAllLines() {
        sourceImageDrawingView.removeAllLines();
        destinationImageDrawingView.removeAllLines();
        if (!pairList.isEmpty()) {
            pairList.clear();
        }
    }

    private boolean undoLastLine() {
        if (!pairList.isEmpty()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                PointF sourceStart = new PointF(pairList.get(pairList.size() - 1).first.getStart());
                PointF sourceEnd = new PointF(pairList.get(pairList.size() - 1).first.getEnd());
                PointF destinationStart = new PointF(pairList.get(pairList.size() - 1).second.getStart());
                PointF destinationEnd = new PointF(pairList.get(pairList.size() - 1).second.getEnd());
                lastRemovedLines.add(new Pair<>(new Line(sourceStart, sourceEnd), new Line(destinationStart, destinationEnd)));
            }
            sourceImageDrawingView.removeLastLine();
            destinationImageDrawingView.removeLastLine();
            pairList.remove(pairList.size() - 1);
            return true;
        }
        return false;
    }

    private boolean redoLastLine() {
        if (!lastRemovedLines.isEmpty()) {
            sourceImageDrawingView.addLine(lastRemovedLines.get(lastRemovedLines.size() - 1).first);
            destinationImageDrawingView.addLine(lastRemovedLines.get(lastRemovedLines.size() - 1).second);
            pairList.add(lastRemovedLines.get(lastRemovedLines.size() - 1));
            lastRemovedLines.remove(lastRemovedLines.size() - 1);
            return true;
        }
        return false;
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
        sourceImageView.setImageResource(R.drawable.add);
        destinationImageView.setImageResource(R.drawable.add);
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
                            sourceImageView.setImageURI(selectedImageUri);
                            //Set URI
                            sourceImageUri = selectedImageUri;
                            //Set new size for drawing view
                            int[] newSize = calculateNewDrawViewSize(sourceImageUri);
                            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(newSize[0], newSize[1]);
                            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                            sourceImageDrawingView.setLayoutParams(layoutParams);
                            //Set flags
                            isSourceSet = true;
                            showToast("Source image set");
                            if (isDestinationSet) {
                                sourceImageDrawingView.setEnabled(true);
                                destinationImageDrawingView.setEnabled(true);
                            }
                        } else if (imageClicked == 1) {
                            destinationImageView.setImageURI(selectedImageUri);
                            //Set URI
                            destinationImageUri = selectedImageUri;
                            //Set new size for drawing view
                            int[] newSize = calculateNewDrawViewSize(destinationImageUri);
                            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(newSize[0], newSize[1]);
                            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                            destinationImageDrawingView.setLayoutParams(layoutParams);
                            //Set flags
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

    private int[] calculateNewDrawViewSize(Uri imageUri) {
        //Get image width and height
        Bitmap img = null;
        try {
            img = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int imageWidth = img.getWidth();
        int imageHeight = img.getHeight();
        //Calculate new width
        int newWidth = (int)(((float)sourceImageView.getHeight() / (float)imageHeight) * (float)imageWidth);
        //Get screen width
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        //Check
        if (newWidth > screenWidth) {
            int newHeight = (int)(((float)screenWidth / (float)newWidth) * (float)sourceImageView.getHeight());
            return new int[]{(int)screenWidth, newHeight};
        }
        return new int[]{newWidth, sourceImageView.getHeight()};
    }

    private void showToast(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    private void openMorphResult(int numOfFrames) {
        //Get image view size
        int width = sourceImageDrawingView.getWidth();
        int height = sourceImageDrawingView.getHeight();
        Bitmap img = null;
        try {
            img = MediaStore.Images.Media.getBitmap(getContentResolver(), sourceImageUri);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int imageWidth = img.getWidth();
        int imageHeight = img.getHeight();
        Intent intent = new Intent(MainActivity.this, MorphResult.class);
        //Set number of frames for MorphResult
        intent.putExtra("numOfFrames", numOfFrames);
        //Set source and destination images Uris for MorphResult
        intent.putExtra("sourceImageUri", sourceImageUri);
        intent.putExtra("destinationImageUri", destinationImageUri);
        //Prepare line pairs for MorphResult
        int numOfPairs = pairList.size();
        float[] startX1 = new float[numOfPairs];
        float[] startY1 = new float[numOfPairs];
        float[] endX1 = new float[numOfPairs];
        float[] endY1 = new float[numOfPairs];
        float[] startX2 = new float[numOfPairs];
        float[] startY2 = new float[numOfPairs];
        float[] endX2 = new float[numOfPairs];
        float[] endY2 = new float[numOfPairs];
        //Clamp lines
        for (int i = 0; i < numOfPairs; i++) {
            startX1[i] = (pairList.get(i).first.getStart().x / width) * imageWidth;
            startY1[i] = (pairList.get(i).first.getStart().y / height) *imageHeight;
            endX1[i] = (pairList.get(i).first.getEnd().x / width) * imageWidth;
            endY1[i] = (pairList.get(i).first.getEnd().y / height) * imageHeight;
            startX2[i] = (pairList.get(i).second.getStart().x / width) * imageWidth;
            startY2[i] = (pairList.get(i).second.getStart().y / height) * imageHeight;
            endX2[i] = (pairList.get(i).second.getEnd().x / width) * imageWidth;
            endY2[i] = (pairList.get(i).second.getEnd().y / height) * imageHeight;
        }
        //Set these arrays for MorphResult
        intent.putExtra("startX1", startX1);
        intent.putExtra("startY1", startY1);
        intent.putExtra("endX1", endX1);
        intent.putExtra("endY1", endY1);
        intent.putExtra("startX2", startX2);
        intent.putExtra("startY2", startY2);
        intent.putExtra("endX2", endX2);
        intent.putExtra("endY2", endY2);
        intent.putExtra("isThreadingOn", isThreadingOn);
        //If image size is too big, resize it to a smaller size
        if (imageWidth > MAX_SIZE || imageHeight > MAX_SIZE) {
            if (imageWidth > imageHeight) {
                intent.putExtra("imgWidth", MAX_SIZE);
                intent.putExtra("imgHeight", imageHeight * MAX_SIZE / imageWidth);
            } else {
                intent.putExtra("imgWidth", imageWidth * MAX_SIZE / imageHeight);
                intent.putExtra("imgHeight", MAX_SIZE);
            }
        }
        intent.putExtra("imgWidth", imageWidth);
        intent.putExtra("imgHeight", imageHeight);
        //Start activity
        startActivity(intent);
    }
}