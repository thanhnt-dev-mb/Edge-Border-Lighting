package com.merryblue.baseapplication.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class AspectRatioFrameLayout extends FrameLayout {

    private static final int SCALE_TYPE_WIDTH = 0;  // Width fixed, height scaled
    private static final int SCALE_TYPE_HEIGHT = 1; // Height fixed, width scaled
    private static final int SCALE_TYPE_FIT = 2;    // Fit within parent (no cropping)
    private static final int SCALE_TYPE_FILL = 3;   // Fill parent (may crop)

    private float aspectRatio = 16f / 9f; // Default 16:9 aspect ratio
    private int scaleType = SCALE_TYPE_FIT;

    public AspectRatioFrameLayout(Context context) {
        super(context);
    }

    public AspectRatioFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public AspectRatioFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // For future use with custom attributes
        // TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AspectRatioFrameLayout);
        // aspectRatio = a.getFloat(R.styleable.AspectRatioFrameLayout_aspectRatio, aspectRatio);
        // scaleType = a.getInt(R.styleable.AspectRatioFrameLayout_scaleType, scaleType);
        // a.recycle();
    }

    /**
     * Sets the aspect ratio for this view. The format is width:height.
     *
     * @param aspectRatio The aspect ratio (width:height)
     */
    public void setAspectRatio(float aspectRatio) {
        if (this.aspectRatio != aspectRatio) {
            this.aspectRatio = aspectRatio;
            requestLayout();
        }
    }

    /**
     * Sets the scale type for this view.
     *
     * @param scaleType The scale type to use
     */
    public void setScaleType(int scaleType) {
        if (this.scaleType != scaleType) {
            this.scaleType = scaleType;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            // Both dimensions are fixed
            width = widthSize;
            height = heightSize;
        } else if (widthMode == MeasureSpec.EXACTLY) {
            // Width is fixed, calculate height based on aspect ratio
            width = widthSize;
            height = (int) (width / aspectRatio);

            // If calculated height is greater than available height and we're using FIT scale type
            if (heightMode == MeasureSpec.AT_MOST && height > heightSize && scaleType == SCALE_TYPE_FIT) {
                height = heightSize;
                width = (int) (height * aspectRatio);
            }
        } else if (heightMode == MeasureSpec.EXACTLY) {
            // Height is fixed, calculate width based on aspect ratio
            height = heightSize;
            width = (int) (height * aspectRatio);

            // If calculated width is greater than available width and we're using FIT scale type
            if (widthMode == MeasureSpec.AT_MOST && width > widthSize && scaleType == SCALE_TYPE_FIT) {
                width = widthSize;
                height = (int) (width / aspectRatio);
            }
        } else {
            // Both dimensions can vary
            if (scaleType == SCALE_TYPE_WIDTH) {
                width = widthSize;
                height = (int) (width / aspectRatio);
            } else if (scaleType == SCALE_TYPE_HEIGHT) {
                height = heightSize;
                width = (int) (height * aspectRatio);
            } else if (scaleType == SCALE_TYPE_FILL) {
                // Fill parent while maintaining aspect ratio, may crop
                if (widthSize / aspectRatio > heightSize) {
                    // Width-constrained
                    width = widthSize;
                    height = (int) (width / aspectRatio);
                } else {
                    // Height-constrained
                    height = heightSize;
                    width = (int) (height * aspectRatio);
                }
            } else {
                // SCALE_TYPE_FIT - default
                // Fit within parent without cropping
                if (widthSize / aspectRatio < heightSize) {
                    // Width-constrained
                    width = widthSize;
                    height = (int) (width / aspectRatio);
                } else {
                    // Height-constrained
                    height = heightSize;
                    width = (int) (height * aspectRatio);
                }
            }
        }

        int finalWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int finalHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        super.onMeasure(finalWidthSpec, finalHeightSpec);
    }

    // Constants for public use
    public static final int SCALE_WIDTH = SCALE_TYPE_WIDTH;
    public static final int SCALE_HEIGHT = SCALE_TYPE_HEIGHT;
    public static final int SCALE_FIT = SCALE_TYPE_FIT;
    public static final int SCALE_FILL = SCALE_TYPE_FILL;
}