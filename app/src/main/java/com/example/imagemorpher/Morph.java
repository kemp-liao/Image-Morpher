package com.example.imagemorpher;

import static java.lang.Math.sqrt;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Arrays;

public class Morph {

    /**
     * Number of middle frames to generate
     */
    private int numOfFrames;

    /**
     * Threading switch
     */
    private boolean isThreadingOn;

    /**
     * The control line pairs
     */
    private ArrayList<Pair<Line, Line>> pairsList;

    /**
     * Final dissolved result
     */
    private Bitmap[] images;

    /**
     * Forward warped images
     */
    private Bitmap[] forwardImages;

    /**
     * Reverse warped images
     */
    private Bitmap[] reverseImages;

    /**
     * Interpolated lines
     */
    private Line[][] intermediateFrameLines;

    /**
     * Width and height of the image
     */
    private int width, height;

    /**
     * Weight calculation parameters
     */
    final float P = 0, A = 0.001f, B = 2;

    public Morph(int numOfFrames, Bitmap sourceImage, Bitmap destinationImage, ArrayList<Pair<Line, Line>> pairsList, boolean isThreadingOn) {
        //Set data
        this.numOfFrames = numOfFrames;
        this.pairsList = pairsList;
        this.width = sourceImage.getWidth();
        this.height = sourceImage.getHeight();
        this.isThreadingOn = isThreadingOn;

        //Set images array
        this.forwardImages = new Bitmap[numOfFrames + 2];
        this.forwardImages[0] = sourceImage;
        this.reverseImages = new Bitmap[numOfFrames + 2];
        this.reverseImages[numOfFrames + 1] = destinationImage;
        this.images = new Bitmap[numOfFrames + 2];

        for (int i = 1; i < numOfFrames + 2; i++) {
            this.forwardImages[i] = Bitmap.createBitmap(width, height, sourceImage.getConfig());
        }
        for (int i = 0; i < numOfFrames + 1; i++) {
            this.reverseImages[i] = Bitmap.createBitmap(width, height, destinationImage.getConfig());
        }
        for (int i = 0; i < numOfFrames + 2; i++) {
            this.images[i] = Bitmap.createBitmap(width, height, sourceImage.getConfig());
        }

        //Morph
        try {
            morph();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private void morph() throws InterruptedException {
        //Create intermediate frame lines
        createIntermediateFrameLines(numOfFrames);

        if (!isThreadingOn) {
            //Warp source image
            for (int i = 1; i < numOfFrames + 2; i++) {
                warp(0, i, forwardImages, width, height, 0, 0);
            }
            //Warp destination image
            for (int i = numOfFrames; i >= 0; i--) {
                warp(numOfFrames + 1, i, reverseImages, width, height, 0, 0);
            }
        } else {
            //Warp source image
            for (int i = 1; i < numOfFrames + 2; i++) {
                threadingWarp(0, i, forwardImages, width, height);
            }
            //Warp destination image
            for (int i = numOfFrames; i >= 0; i--) {
                threadingWarp(numOfFrames + 1, i, reverseImages, width, height);
            }
        }

        //Cross-dissolve images
        crossDissolve();
    }

    private void createIntermediateFrameLines(int numOfFramesToGenerate) {
        //Initialize array
        intermediateFrameLines = new Line[numOfFramesToGenerate + 2][pairsList.size()];

        for (int j = 0; j < pairsList.size(); j++) {
            //Calculate intermediate stepper for x and y
            float sourceStartX = pairsList.get(j).first.getStart().x;
            float sourceStartY = pairsList.get(j).first.getStart().y;
            float sourceEndX = pairsList.get(j).first.getEnd().x;
            float sourceEndY = pairsList.get(j).first.getEnd().y;

            float startXStepper = (pairsList.get(j).second.getStart().x - sourceStartX) / (numOfFramesToGenerate + 1);
            float startYStepper = (pairsList.get(j).second.getStart().y - sourceStartY) / (numOfFramesToGenerate + 1);

            float endXStepper = (pairsList.get(j).second.getEnd().x - sourceEndX) / (numOfFramesToGenerate + 1);
            float endYStepper = (pairsList.get(j).second.getEnd().y - sourceEndY) / (numOfFramesToGenerate + 1);

            for (int i = 0; i < numOfFramesToGenerate + 2; i++) {
                //Calculate intermediate stepper for x and y
                PointF interStart = new PointF(sourceStartX + startXStepper * i, sourceStartY + startYStepper * i);
                PointF interEnd = new PointF(sourceEndX + endXStepper * i, sourceEndY + endYStepper * i);
                intermediateFrameLines[i][j] = new Line(interStart, interEnd);
            }
        }
    }

    /**
     * threadingWarp function
     * Divide the image into 4 sections, and process each section with a thread independently
     * @param sourceIndex source image index as integer
     * @param destinationIndex destination image index as integer
     * @param images output image array
     * @param width the width of the image as integer
     * @param height the height of the image as integer
     */
    private void threadingWarp(int sourceIndex, int destinationIndex, Bitmap[] images, int width, int height) throws InterruptedException {
        int widthStep = width / 3;
        int heightStep = height / 3;
        Thread[] threads = new Thread[9];

        threads[0] = new Thread(() -> warp(sourceIndex, destinationIndex, images, widthStep, heightStep, 0, 0));
        threads[1] = new Thread(() -> warp(sourceIndex, destinationIndex, images, widthStep * 2, heightStep, widthStep, 0));
        threads[2] = new Thread(() -> warp(sourceIndex, destinationIndex, images, width, heightStep, widthStep * 2, 0));
        threads[3] = new Thread(() -> warp(sourceIndex, destinationIndex, images, widthStep, heightStep * 2, 0, heightStep));
        threads[4] = new Thread(() -> warp(sourceIndex, destinationIndex, images, widthStep * 2, heightStep * 2, widthStep, heightStep));
        threads[5] = new Thread(() -> warp(sourceIndex, destinationIndex, images, width, heightStep * 2, widthStep * 2, heightStep));
        threads[6] = new Thread(() -> warp(sourceIndex, destinationIndex, images, widthStep, height, 0, heightStep * 2));
        threads[7] = new Thread(() -> warp(sourceIndex, destinationIndex, images, widthStep * 2, height, widthStep, heightStep * 2));
        threads[8] = new Thread(() -> warp(sourceIndex, destinationIndex, images, width, height, widthStep * 2, heightStep * 2));

        for (Thread t : threads) {
            t.start();;
        }
        for (Thread t : threads) {
            t.join();
        }
    }

    /**
     * Warp function
     * Warp the image based on control lines
     *
     * @param sourceIndex source image index as integer
     * @param destinationIndex destination image index as integer
     * @param images images array for output
     * @param width the width of the image as integer
     * @param height the height of the image as integer
     * @param startX the start x as int
     * @param startY the start y as int
     */
    private void warp(int sourceIndex, int destinationIndex, Bitmap[] images, int width, int height, int startX, int startY) {
        for (int x = startX; x < width; x++) {
            for (int y = startY; y < height; y++) {
                float[] weights = new float[pairsList.size()];
                float[] deltaXs = new float[pairsList.size()];
                float[] deltaYs = new float[pairsList.size()];

                for (int i = 0; i < pairsList.size(); i++) {
                    //Calculate distance and fraction
                    float d = intermediateFrameLines[destinationIndex][i].distanceTo(x, y);
                    float f = intermediateFrameLines[destinationIndex][i].projectionLengthOn(x, y) /
                            intermediateFrameLines[destinationIndex][i].length();

                    //Calculate corresponding x and y in source image
                    int sourceX = (int)(intermediateFrameLines[sourceIndex][i].getStart().x +
                            f * intermediateFrameLines[sourceIndex][i].getVector()[0] -
                            d * intermediateFrameLines[sourceIndex][i].getUnityNormalVector()[0]);
                    int sourceY = (int)(intermediateFrameLines[sourceIndex][i].getStart().y +
                            f * intermediateFrameLines[sourceIndex][i].getVector()[1] -
                            d * intermediateFrameLines[sourceIndex][i].getUnityNormalVector()[1]);
                    sourceX = clamp(sourceX, this.width);
                    sourceY = clamp(sourceY, this.height);

                    //Calculate delta = new - original
                    deltaXs[i] = sourceX - x;
                    deltaYs[i] = sourceY - y;

                    //Check fraction
                    if (f < 0) {
                        float x2 = intermediateFrameLines[destinationIndex][i].getStart().x;
                        float y2 = intermediateFrameLines[destinationIndex][i].getStart().y;
                        d = (float)sqrt((x2 - x) * (x2 - x) + (y2 - y) * (y2 - y));
                    } else if (f > 1) {
                        float x2 = intermediateFrameLines[destinationIndex][i].getEnd().x;
                        float y2 = intermediateFrameLines[destinationIndex][i].getEnd().y;
                        d = (float)sqrt((x2 - x) * (x2 - x) + (y2 - y) * (y2 - y));
                    }

                    //Calculate weight
                    weights[i] = (float)Math.pow((Math.pow(intermediateFrameLines[destinationIndex][i].length(), P) / (A + d)), B);
                }

                //Calculate sum of weights
                float sumOfWeights = 0;
                for (float weight : weights) {
                    sumOfWeights += weight;
                }

                //Calculate total weighted deltas
                float totalWeightedDeltaX = 0;
                float totalWeightedDeltaY = 0;
                for (int j = 0; j < pairsList.size(); j++) {
                    totalWeightedDeltaX += weights[j] * deltaXs[j];
                    totalWeightedDeltaY += weights[j] * deltaYs[j];
                }

                //Calculate corresponding x and y in source image
                int newX = clamp((int)(x + (totalWeightedDeltaX / sumOfWeights)), this.width);
                int newY = clamp((int)(y + (totalWeightedDeltaY / sumOfWeights)), this.height);

                //Reverse mapping
                //Get pixel from the source image then set it to the destination image
                int pixel = images[sourceIndex].getPixel(newX, newY);
                images[destinationIndex].setPixel(x, y, pixel);
            }
        }
    }

    private void crossDissolve() {
        int totalNumOfFrames = numOfFrames + 2;
        for (int i = 0; i < totalNumOfFrames; i++) {
            float forwardDissolveRatio = (float) (totalNumOfFrames - i) / totalNumOfFrames;
            float reverseDissolveRatio = (float) i / totalNumOfFrames;

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int forwardImagePixel = forwardImages[i].getPixel(x, y);
                    int reverseImagePixel = reverseImages[i].getPixel(x, y);

                    int forwardRed = Color.red(forwardImagePixel);
                    int forwardGreen = Color.green(forwardImagePixel);
                    int forwardBlue = Color.blue(forwardImagePixel);

                    int reverseRed = Color.red(reverseImagePixel);
                    int reverseGreen = Color.green(reverseImagePixel);
                    int reverseBlue = Color.blue(reverseImagePixel);

                    int newRed = (int) (forwardRed * forwardDissolveRatio + reverseRed * reverseDissolveRatio);
                    int newGreen = (int) (forwardGreen * forwardDissolveRatio + reverseGreen * reverseDissolveRatio);
                    int newBlue = (int) (forwardBlue * forwardDissolveRatio + reverseBlue * reverseDissolveRatio);

                    int newPixel = Color.rgb(newRed, newGreen, newBlue);
                    images[i].setPixel(x, y, newPixel);
                }
            }
        }
    }

    /**
     * Restrain the output
     * @param input
     * @param max
     * @return
     */
    private int clamp(int input, int max) {
        if (input < 0) {
            return 0;
        } else return Math.min(input, max - 1);
    }

    public Bitmap[] getResults() { return this.images; }
}
