package com.videoviewdemo;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.VideoView;

/**
 * Created by Administrator on 2017/2/9.
 */

public class CosVideoView extends VideoView {

    int defaultWidth = 1080;
    int defaultHeight = 1920;

    public CosVideoView(Context context) {
        super(context);
    }
    public CosVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public CosVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int width = getDefaultSize(defaultWidth, widthMeasureSpec);
        int height = getDefaultSize(defaultHeight, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

}
