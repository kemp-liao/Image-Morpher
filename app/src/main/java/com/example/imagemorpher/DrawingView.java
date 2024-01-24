package com.example.imagemorpher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class DrawingView extends View {

    private Paint fillPaint, strokePaint;
    private PointF startPoint;
    private PointF endPoint;
    final int STROKE_WIDTH = 10;
    final int LINE_END_CIRCLE_RADIUS = 25;
    final int DEFAULT_COLOR = Color.BLACK;

    final int HIGHLIGHT_COLOR = Color.RED;

    private int editFlag = 0;

    //Use for moving the line
    private float ogStartX = 0, ogStartY = 0, ogEndX = 0, ogEndY = 0, ogMouseX = 0, ogMouseY = 0;

    private ArrayList<Line> lineList;


    //Default constructor
    public DrawingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        fillPaint = new Paint();
        fillPaint.setColor(DEFAULT_COLOR);
        fillPaint.setStrokeWidth(STROKE_WIDTH);
        fillPaint.setStyle(Paint.Style.FILL);

        startPoint = new PointF();
        endPoint = new PointF();
        lineList = new ArrayList<>();

        //Set the start and end points outside the canvas
        startPoint.set(-100,-100);
        endPoint.set(-100,-100);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        //Draw a temporary line when dragging
        drawTempLine(canvas);

        //Draw all the lines in the list
        if (!lineList.isEmpty()) {
            for(Line line : lineList) {
                if (line.isSelected()) {
                    fillPaint.setColor(HIGHLIGHT_COLOR);
                } else {
                    fillPaint.setColor(DEFAULT_COLOR);
                }
                float startX = line.getStart().x;
                float startY = line.getStart().y;
                float endX = line.getEnd().x;
                float endY = line.getEnd().y;
                //Draw line
                canvas.drawLine(startX, startY, endX, endY, fillPaint);
                //Draw start point
                canvas.drawCircle(startX, startY, LINE_END_CIRCLE_RADIUS, fillPaint);
                //Draw end point
                canvas.drawCircle(endX, endY, LINE_END_CIRCLE_RADIUS, fillPaint);
                //Draw mid point
                canvas.drawCircle(line.getMidPoint().x, line.getMidPoint().y, LINE_END_CIRCLE_RADIUS, fillPaint);

                //Reset the paint color to default
                fillPaint.setColor(DEFAULT_COLOR);
                invalidate();
            }
        }
    }

    public Line editLine(MotionEvent event, ArrayList<Pair<Line, Line>> pairList) {
        Line line = null;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startPoint.set(event.getX(), event.getY());
                endPoint.set(event.getX(), event.getY());

                //Check click
                if (Float.compare(startPoint.x, endPoint.x) < 1
                        && Float.compare(startPoint.y, endPoint.y) < 1) {
                    for (Line existingLine : lineList) {
                        int tempFlag = existingLine.isClickOnLine(event.getX(), event.getY(), LINE_END_CIRCLE_RADIUS);
                        if (editFlag == 0 && tempFlag > 0) {
                            editFlag = tempFlag;
                            existingLine.setSelected(true);
                            ogStartX = existingLine.getStart().x;
                            ogStartY = existingLine.getStart().y;
                            ogEndX = existingLine.getEnd().x;
                            ogEndY = existingLine.getEnd().y;
                            ogMouseX = event.getX();
                            ogMouseY = event.getY();
                            //Find the corresponding line
                            selectCorrespondingLine(pairList);
                            break;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (editFlag == 1) {
                    for (Line existingLine : lineList) {
                        if (existingLine.isSelected()) {
                            float deltaX = event.getX() - ogMouseX;
                            float deltaY = event.getY() - ogMouseY;
                            existingLine.setStart(ogStartX + deltaX, ogStartY + deltaY);
                            existingLine.setEnd(ogEndX + deltaX, ogEndY + deltaY);
                        }
                    }
                } else if (editFlag == 2) {
                    for (Line existingLine : lineList) {
                        if (existingLine.isSelected()) {
                            existingLine.setStart(event.getX(), event.getY());
                        }
                    }
                } else if (editFlag == 3) {
                    for (Line existingLine : lineList) {
                        if (existingLine.isSelected()) {
                            existingLine.setEnd(event.getX(), event.getY());
                        }
                    }
                } else {
                    endPoint.set(event.getX(), event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
                if (editFlag == 0) {
                    endPoint.set(event.getX(), event.getY());
                    //Check if start point equals end point
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                            !(Float.compare(startPoint.x, endPoint.x) == 0
                                    && Float.compare(startPoint.y, endPoint.y) == 0)) {
                        line = new Line(new PointF(startPoint), new PointF(endPoint));
                        lineList.add(line);
                    }
                }
                //Reset all lines to be unselected
                unselectAllLines(pairList);
                //Reset editFlag and selectingFlag
                editFlag = 0;
                //Reset the start and end points outside the canvas
                startPoint.set(-100,-100);
                endPoint.set(-100,-100);
                break;
        }
        invalidate();
        return line;
    }

    public void addLine(Line line) {
        lineList.add(line);
        invalidate();
    }

    private void selectCorrespondingLine(ArrayList<Pair<Line, Line>> pairList) {
        for (Pair<Line, Line> pair : pairList) {
            if (pair.first.isSelected()) {
                pair.second.setSelected(true);
            } else if (pair.second.isSelected()) {
                pair.first.setSelected(true);
            }
        }
    }

    private void unselectAllLines(ArrayList<Pair<Line, Line>> pairList) {
        for (Pair<Line, Line> pair : pairList) {
            pair.first.setSelected(false);
            pair.second.setSelected(false);
        }
    }

    private void drawTempLine(Canvas canvas) {
        if (!(Float.compare(startPoint.x, endPoint.x) == 0 && Float.compare(startPoint.y, endPoint.y) == 0)) {
            canvas.drawLine(startPoint.x, startPoint.y, endPoint.x, endPoint.y, fillPaint);
            canvas.drawCircle(startPoint.x, startPoint.y, LINE_END_CIRCLE_RADIUS, fillPaint);
            canvas.drawCircle(endPoint.x, endPoint.y, LINE_END_CIRCLE_RADIUS, fillPaint);
        }
    }

    public void removeLastLine() {
        if(!lineList.isEmpty()) {
            lineList.remove(lineList.size() - 1);
        }
        invalidate();
    }

    public void removeAllLines() {
        lineList.clear();
        invalidate();
    }
}
