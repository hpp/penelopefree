package com.harmonicprocesses.penelopefree.camera;

import android.os.Parcel;
import android.os.Parcelable;

public class VideoCodecParcelable {
	private int mData;

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mData);
    }

    public static final Parcelable.Creator<VideoCodecParcelable> CREATOR
            = new Parcelable.Creator<VideoCodecParcelable>() {
        public VideoCodecParcelable createFromParcel(Parcel in) {
            return new VideoCodecParcelable(in);
        }

        public VideoCodecParcelable[] newArray(int size) {
            return new VideoCodecParcelable[size];
        }
    };
    
    private VideoCodecParcelable(Parcel in) {
        mData = in.readInt();
    }
}
