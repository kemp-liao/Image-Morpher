package com.example.imagemorpher;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

import android.graphics.PointF;
import android.util.Pair;

public class Line {

    private PointF start, end;

    private boolean isSelected;

    public Line(PointF start, PointF end) {
        this.start = start;
        this.end = end;
        this.isSelected = false;
    }

//    public void moveLine(float deltaX, float deltaY) {
//        float ogStartX = this.start.x;
//        float ogStartY = this.start.y;
//        float ogEndX = this.end.x;
//        float ogEndY = this.end.y;
//        start.set(new PointF(ogStartX + deltaX, ogStartY + deltaY));
//        end.set(new PointF(ogEndX + deltaX, ogEndY + deltaY));
//    }

//    public int isClickOnEnd(float eventX, float eventY, float clickSensitivity) {
//
//    }

    /**
     * Check if the user clicks on the line or the two ends
     * 1 for click on the mid point to move the line
     * 2 for click on the start point
     * 3 for click on the end point
     * 0 for not on the line or the ends
     * @param eventX x coordinate of click/touch as float
     * @param eventY y coordinate of click/touch as float
     * @param circleRadius as float
     * @return result as integer
     */
    public int isClickOnLine(float eventX, float eventY, float circleRadius) {
        PointF midPoint = getMidPoint();
        float distanceToStart = distanceBetweenTwoPoints(eventX, eventY, start.x, start.y);
        float distanceToEnd = distanceBetweenTwoPoints(eventX, eventY, end.x, end.y);
        float distanceToMid = distanceBetweenTwoPoints(eventX, eventY, midPoint.x, midPoint.y);
        //20 is the radius of the end circle
        if (distanceToMid <= circleRadius) {
            return 1;
        } else if (distanceToStart <= circleRadius) {
            return 2;
        } else if (distanceToEnd <= circleRadius) {
            return 3;
        } else {
            return 0;
        }

    }

    /**
     * Return the distance from a point to the line
     * @param x coordinate as float
     * @param y coordinate as float
     * @return distance as float
     */
    public float distanceTo(float x, float y) {
        float vx = start.x - x;
        float vy = start.y - y;
        float[] normalVector = getNormalVector();
        return dotProduct(normalVector[0], normalVector[1], vx, vy) / length();
    }

    /**
     * Return the projection length
     * For instance, a line has start point P and end point Q
     * There is another point X, this method returns the projection length of
     * vector PX on vector PQ
     * @param x coordinate of point as float
     * @param y coordinate of point as float
     * @return projection length as float
     */
    public float projectionLengthOn(float x, float y) {
        float[] vector = getVector();
        float vX2 = x - start.x;
        float vY2 = y - start.y;
        return dotProduct(vector[0], vector[1], vX2, vY2) / length();
    }

    /**
     * Return the mid point of the line
     * @return mid point as PointF
     */
    public PointF getMidPoint() {
        return new PointF((start.x + end.x) / 2, (start.y + end.y) / 2);
    }

    /**
     * Get the length of the line
     * @return length as float
     */
    public float length() {
        return distanceBetweenTwoPoints(start.x, start.y, end.x, end.y);
    }

    private float distanceBetweenTwoPoints(float x1, float y1, float x2, float y2) {
        return (float)sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    private float dotProduct(float u, float v, float x, float y) {
        return u * x + v * y;
    }



    //Getters and setters
    public PointF getStart() {
        return start;
    }

    public PointF getEnd() {
        return end;
    }

    public void setStart(float x, float y) {
        this.start.set(x, y);
    }

    public void setEnd(float x, float y) {
        this.end.set(x, y);
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public float[] getVector() { return new float[]{end.x - start.x, end.y - start.y}; }

    public float[] getNormalVector() {
        float[] vector = getVector();
        return new float[]{-vector[1], vector[0]};
    }

    public float[] getUnityVector() {
        float[] vector = getVector();
        return new float[]{vector[0] / length(), vector[1] / length()};
    }

    public float[] getUnityNormalVector() {
        float[] normalVector = getNormalVector();
        return new float[]{normalVector[0] / length(), normalVector[1] / length()};
    }
}
