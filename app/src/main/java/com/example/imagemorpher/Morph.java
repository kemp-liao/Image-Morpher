package com.example.imagemorpher;

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

    private ArrayList<Pair<Line, Line>> pairsList;

    private Bitmap[] images, forwardImages, reverseImages;

    private Line[][] intermediateFrameLines;

    private int width, height;

    final float P = 0, A = 0.001f, B = 2;

    public Morph(int numOfFrames, Bitmap sourceImage, Bitmap destinationImage, ArrayList<Pair<Line, Line>> pairsList) {
        //Set data
        this.numOfFrames = numOfFrames;
        this.pairsList = pairsList;
        this.width = sourceImage.getWidth();
        this.height = sourceImage.getHeight();

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
        morph();
    }

    private void morph() {
        //Create intermediate frame lines
        createIntermediateFrameLines(numOfFrames);

        //Warp source image
        for (int i = 1; i < numOfFrames + 2; i++) {
            warp(0, i, forwardImages);
        }

        //Warp destination image
        for (int i = numOfFrames; i >= 0; i--) {
            warp(numOfFrames + 1, i, reverseImages);
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

    private void warp(int sourceIndex, int destinationIndex, Bitmap[] images) {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float[] weights = new float[pairsList.size()];
                float[] deltaXs = new float[pairsList.size()];
                float[] deltaYs = new float[pairsList.size()];

                for (int i = 0; i < pairsList.size(); i++) {
                    //Reverse mapping
                    float d = intermediateFrameLines[destinationIndex][i].distanceTo(x, y);
                    float f = intermediateFrameLines[destinationIndex][i].projectionLengthOn(x, y) /
                            intermediateFrameLines[destinationIndex][i].length();

                    //Calculate weight
                    weights[i] = (float)Math.pow((Math.pow(intermediateFrameLines[destinationIndex][i].length(), P) / (A + d)), B);

                    //Calculate corresponding x and y in source image
                    int sourceX = (int)(intermediateFrameLines[sourceIndex][i].getStart().x +
                            f * intermediateFrameLines[sourceIndex][i].getVector()[0] -
                            d * intermediateFrameLines[sourceIndex][i].getUnityNormalVector()[0]);
                    int sourceY = (int)(intermediateFrameLines[sourceIndex][i].getStart().y +
                            f * intermediateFrameLines[sourceIndex][i].getVector()[1] -
                            d * intermediateFrameLines[sourceIndex][i].getUnityNormalVector()[1]);
                    sourceX = clamp(sourceX, width);
                    sourceY = clamp(sourceY, height);

                    //Calculate delta = new - original
                    deltaXs[i] = sourceX - x;
                    deltaYs[i] = sourceY - y;


//                    int pixel = images[sourceIndex].getPixel(sourceX, sourceY);
//                    images[destinationIndex].setPixel(x, y, pixel);
                }

                //Calculate sum of weights
                float sumOfWeights = 0;
                for (float weight : weights) {
                    sumOfWeights += weight;
                }

                //Calculate total weighted delta's
                float totalWeightedDeltaX = 0;
                float totalWeightedDeltaY = 0;
                for (int j = 0; j < pairsList.size(); j++) {
                    totalWeightedDeltaX += weights[j] * deltaXs[j];
                    totalWeightedDeltaY += weights[j] * deltaYs[j];
                }

                //Calculate corresponding x and y in source image
                int newX = clamp((int)(x + (totalWeightedDeltaX / sumOfWeights)), width);
                int newY = clamp((int)(y + (totalWeightedDeltaY / sumOfWeights)), height);

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
