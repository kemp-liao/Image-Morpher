package com.example.imagemorpher;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.util.ArrayList;

public class Passable implements Parcelable {

    private ArrayList<Pair<Line, Line>> pairsList;

    private int numOfFrames;

    private Uri sourceImageUri, destinationImageUri;

    public Passable(ArrayList<Pair<Line, Line>> list, int numOfFrames, Uri sourceImageUri, Uri destinationImageUri) {
        this.pairsList = list;
        this.numOfFrames = numOfFrames;
        this.sourceImageUri = sourceImageUri;
        this.destinationImageUri = destinationImageUri;
    }

    protected Passable(Parcel in) {
        numOfFrames = in.readInt();
        sourceImageUri = in.readParcelable(Uri.class.getClassLoader());
        destinationImageUri = in.readParcelable(Uri.class.getClassLoader());
    }

    public static final Creator<Passable> CREATOR = new Creator<Passable>() {
        @Override
        public Passable createFromParcel(Parcel in) {
            return new Passable(in);
        }

        @Override
        public Passable[] newArray(int size) {
            return new Passable[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeList(pairsList);
        dest.writeInt(numOfFrames);
        sourceImageUri.writeToParcel(dest, 0);
        destinationImageUri.writeToParcel(dest, 0);
    }

    public ArrayList<Pair<Line, Line>> getPairsList() {
        return pairsList;
    }

    public int getNumOfFrames() {
        return numOfFrames;
    }

    public Uri getSourceImageUri() {
        return sourceImageUri;
    }

    public Uri getDestinationImageUri() {
        return destinationImageUri;
    }
}
