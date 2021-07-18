package com.ynsuper.slideshowver1.view.custom_view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.ynsuper.slideshowver1.R;
import com.ynsuper.slideshowver1.callback.IHorizontalListChange;
import com.ynsuper.slideshowver1.callback.ImageGroupListener;
import com.ynsuper.slideshowver1.callback.MusicGroupListener;
import com.ynsuper.slideshowver1.util.VideoComposer;
import com.ynsuper.slideshowver1.util.editvideo.BitmapUtils;
import com.ynsuper.slideshowver1.view.LittleBox;
import com.ynsuper.slideshowver1.viewmodel.SlideShowViewModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public class HorizontalThumbnailListView extends View {

    private static final String TAG = "ThumbnailListView";
    private IHorizontalListChange iHorizontalListChange;
    private final int DEFAULT_IMAGE_WIDTH = 60, DEFAULT_IMAGE_HEIGHT = 60;
    private final int DEFAULT_MUSIC_WIDTH = 100;
    private final int DEFAULT_PADDING_HEIGHT_ITEM = 10;
    private final String DEFAULT_MUSIC_NAME = "Add Music";

    // 绘制相关
    private Paint mPaint;
    private Rect mImageDstRect;
    private Rect mMusicDstRect;

    private int mWidth, mHeight;

    private int mPaddingColor = Color.RED;
    private int mPaddingStartWidth, mPaddingEndWidth, mPaddingVerticalHeight;
    private int mGroupPaddingWidth;
    private Drawable mSelectedGroupBg;
    private int mImageWidth, mImageHeight;

    // listeners
    private ImageGroupListener mImageGroupListener;
    private MusicGroupListener mMusicGroupListener;

    private final List<ImageGroup> mImageGroupList = new ArrayList<>();
    private final List<ItemUtilityGroup> mItemUtilityGroupList = new ArrayList<>();
    public ImageGroup mCurImageGroup;
    public ItemUtilityGroup mCurItemUtilityGroup;

    private boolean mHoldScroll;
    private boolean mIsLeft;
    private boolean mFromLeftToRight;
    public boolean mIsScrollFromUser;

    private boolean mAdjustable = false;
    private boolean mAdjustableMusic = false;

    private float mLastX;

    public boolean isLast = false;
    private int mLastPosX = 0;
    private LittleBox littleBox;
    private VideoComposer composer;
    private Paint mTextPaint;
    private Paint mTextMusicPaint;
    private Paint mScaleLinePaint;
    private Paint mLinePaintTransition;
    private int lastIndex = -1;
    private int bottomSlideImageHeight;
    private String textMusicSong = DEFAULT_MUSIC_NAME;

    public boolean isLastImage() {
        return isLast;
    }

    public ImageGroup getImageGroup() {
        return mCurImageGroup;
    }

    private GestureDetector mGestureDetectorImage;
    private GestureDetector mGestureDetectorMusic;

    public HorizontalThumbnailListView(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public HorizontalThumbnailListView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public HorizontalThumbnailListView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public HorizontalThumbnailListView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            width = mPaddingStartWidth + mPaddingEndWidth + getTotalGroupWidth();
        }
        if (heightMode != MeasureSpec.EXACTLY) {
            height = mImageHeight + mPaddingVerticalHeight * 2;
        }

        Log.d(TAG, "onMeasure, width: " + width + ", height: " + height);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        Log.d(TAG, "onSizeChanged, width: " + w + ", height: " + h);
        mWidth = w;
        mHeight = h;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

//        Log.d(TAG, "onDraw, padding start: " + mPaddingStartWidth +
//                ", padding end: " + mPaddingEndWidth +
//                ", group padding: " + mGroupPaddingWidth +
//                ", vertical padding: " + mPaddingVerticalHeight +
//                ", image width: " + mImageWidth +
//                " image width: " + mImageHeight);
        drawImageGroup(canvas);
//        drawImageUtility(canvas);

        invalidate();
    }

    private void drawImageUtility(Canvas canvas) {
        int curPos = 0;
        int curPosHeight = 0;
        mPaint.setColor(mPaddingColor);
        curPos += mPaddingStartWidth;
        curPosHeight += bottomSlideImageHeight;

        for (ItemUtilityGroup utilityItem : mItemUtilityGroupList) {
            Log.d(TAG, "onDraw: ItemUtilityGroup" + utilityItem.BOTTOM_BOUND + " curPosHeight: " + curPosHeight);
            if (utilityItem.isHidden) {
                Log.d(TAG, "Image group hidden, so skip this!");
                continue;
            }
            utilityItem.drawItemInGroup(canvas, curPos, curPosHeight, utilityItem.utilityItemList.get(0));
            curPosHeight += DEFAULT_MUSIC_WIDTH + DEFAULT_PADDING_HEIGHT_ITEM;

        }

    }

    private void drawImageGroup(@NotNull Canvas canvas) {
        int curPos = 0;
        mPaint.setColor(mPaddingColor);
        curPos += mPaddingStartWidth;
        curPos += mGroupPaddingWidth;
        for (ImageGroup imageGroup : mImageGroupList) {
//            Log.d(TAG, "onDraw: " + imageGroup);
            if (imageGroup.isHidden) {
//                Log.d(TAG, "Image group hidden, so skip this!");
                continue;
            }
            imageGroup.drawImageInGroup(canvas, curPos, imageGroup.imageTransition);
            // draw transition
            Bitmap bitmapTransition = BitmapUtils.resizeBitmap(BitmapUtils.getBitmapFromAsset(getContext(), imageGroup.imageTransition),
                    100, 100);
            if (bitmapTransition != null) {
                canvas.drawLine(mImageDstRect.right + mPaddingVerticalHeight * 2 - mLinePaintTransition.getStrokeWidth() / 2,
                        (mImageDstRect.top) * 2 - 2,
                        mImageDstRect.right + mPaddingVerticalHeight * 2 - mLinePaintTransition.getStrokeWidth() / 2,
                        mImageDstRect.bottom + mPaddingVerticalHeight * 3, mLinePaintTransition
                );
                canvas.drawCircle(mImageDstRect.right + mPaddingVerticalHeight * 2- mLinePaintTransition.getStrokeWidth()/2,
                        (mImageDstRect.top) * 2, 8, mLinePaintTransition);
                canvas.drawBitmap(bitmapTransition,
                        mImageDstRect.right - bitmapTransition.getWidth() / 2 + mPaddingVerticalHeight * 2,
                        mImageDstRect.bottom + mPaddingVerticalHeight * 3, null);
            }

            curPos += imageGroup.getWidth() + mGroupPaddingWidth;
        }


        mPaint.setColor(mPaddingColor);

        // draw start
//        canvas.drawRect(curPos, 0, mPaddingStartWidth, mHeight, mPaint);
        // draw end
//        canvas.drawRect(curPos, 0, curPos + mPaddingEndWidth, mHeight, mPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        mGestureDetectorImage.onTouchEvent(event);
//        mGestureDetectorMusic.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = event.getRawX();
                mIsScrollFromUser = true;

//                if (mAdjustableMusic) {
//                    Log.d(TAG, "Down pos x: " + mLastX +
//                            ", left bound: [" + (mCurItemUtilityGroup.measuredLeft - mGroupPaddingWidth - getScrollX()) +
//                            ", " + (mCurItemUtilityGroup.measuredLeft + mGroupPaddingWidth - getScrollX()) +
//                            "], right bound: [" + (mCurItemUtilityGroup.measuredRight - mGroupPaddingWidth - getScrollX()) +
//                            ", " + (mCurItemUtilityGroup.measuredRight + mGroupPaddingWidth - getScrollX()) + "].");
//                    if (mLastX >= (mCurItemUtilityGroup.measuredLeft - mGroupPaddingWidth - getScrollX()) &&
//                            mLastX <= (mCurItemUtilityGroup.measuredLeft + mGroupPaddingWidth - getScrollX())) {
//                        mHoldScroll = true;
//                        mIsLeft = true;
//                    } else if (mLastX >= (mCurItemUtilityGroup.measuredRight - mGroupPaddingWidth - getScrollX()) &&
//                            mLastX <= (mCurItemUtilityGroup.measuredRight + mGroupPaddingWidth - getScrollX())) {
//                        mHoldScroll = true;
//                        mIsLeft = false;
//                    }
//                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mHoldScroll) {
                    int delta = (int) (event.getRawX() - mLastX);
                    Log.d(TAG, "Adjust delta: " + delta + ", is left: " + mIsLeft);
                    // adjust view width.
                    if (mIsLeft) {
                        if (delta > 0) {
                            mFromLeftToRight = true;
                            mCurImageGroup.shrinkLeft(delta);
                        } else if (delta < 0) {
                            mFromLeftToRight = false;
                            mCurImageGroup.expandLeft(-delta);
                        }
                    } else {
                        if (delta > 0) {
                            mFromLeftToRight = true;
                            mCurImageGroup.expandRight(delta);
                        } else if (delta < 0) {
                            mFromLeftToRight = false;
                            mCurImageGroup.shrinkRight(-delta);
                        }
                    }
                    invalidate();
                } else {
                    int scrollX = (int) (getScrollX() + (mLastX - event.getRawX()));
                    if (scrollX < 0) {
                        scrollX = 0;
                    } else if (scrollX > getTotalGroupWidth()) {
                        scrollX = getTotalGroupWidth();
                    }
                    scrollTo(scrollX, getScrollY());
                }
                mLastX = event.getRawX();
                break;
            case MotionEvent.ACTION_UP:
                // 当手指抬起的时候重新设置padding start
                mIsScrollFromUser = false;
                if (mHoldScroll) {
                    mHoldScroll = false;

                    int curIndex = 0;
                    for (int i = 0; i < mImageGroupList.size(); i++) {
                        ImageGroup imageGroup = mImageGroupList.get(i);
                        if (imageGroup == mCurImageGroup) {
                            break;
                        }
                        if (!mImageGroupList.get(i).isHidden) {
                            curIndex++;
                        }
                    }
                    if (mIsLeft) {
                        if (mFromLeftToRight) {
                            if (mImageGroupListener != null) {
                                mImageGroupListener.onImageGroupLeftShrink(curIndex,
                                        mCurImageGroup.curLeftPos * 1.0f / getGroupContentMaxWidth(),
                                        true);
                            }
                        } else {
                            if (mImageGroupListener != null) {
                                mImageGroupListener.onImageGroupLeftExpand(curIndex,
                                        mCurImageGroup.curLeftPos * 1.0f / getGroupContentMaxWidth(),
                                        true);
                            }
                        }
                    } else {
                        if (mFromLeftToRight) {
                            if (mImageGroupListener != null) {
                                mImageGroupListener.onImageGroupRightExpand(curIndex,
                                        mCurImageGroup.curRightPos * 1.0f / getGroupContentMaxWidth(),
                                        true);
                            }
                        } else {
                            if (mImageGroupListener != null) {
                                mImageGroupListener.onImageGroupRightShrink(curIndex,
                                        mCurImageGroup.curRightPos * 1.0f / getGroupContentMaxWidth(),
                                        true);
                            }
                        }
                    }
                    mPaddingStartWidth = getDisplay().getWidth() / 2 - mGroupPaddingWidth;
                    requestLayout();
                }
                break;
            default:
                break;
        }

        // 我们需要消费事件
        return true;
    }

    @Override
    public void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        Log.d(TAG, "onScrollChanged: " + l + " old l : " + oldl + " t: " + t + " old t: " + oldt + " size of the list" + mImageGroupList.size());

        select(l);
        mCurImageGroup.notifyPosX(l, oldl);
    }

    public void clear() {
        mCurImageGroup = null;
        mImageGroupList.clear();
        mItemUtilityGroupList.clear();
    }

    public void newImageGroup(List<ImageItem> imageItemList, String imageTransition) {
        if (imageItemList == null || imageItemList.size() == 0) {
            Log.e(TAG, "Image item list can not be null or empty!");
            return;
        }

        if (mImageGroupList.size() == 0) {
            mImageGroupList.add(new ImageGroup(0, imageItemList, imageTransition));
            // 默认第一个是选中的
            mCurImageGroup = mImageGroupList.get(0);
        } else {
            int leftBound = mImageGroupList.get(mImageGroupList.size() - 1).RIGHT_BOUND;
            mImageGroupList.add(new ImageGroup(leftBound, imageItemList, imageTransition));
        }
    }

    public void newUtilityGroup(List<UtilityItem> utilityItems) {
        if (utilityItems == null || utilityItems.size() == 0) {
            Log.e(TAG, "Image item list can not be null or empty!");
            return;
        }

        if (mItemUtilityGroupList.size() == 0) {
            mItemUtilityGroupList.add(new ItemUtilityGroup(bottomSlideImageHeight, utilityItems));
            // 默认第一个是选中的
//            mCurImageGroup = mImageGroupList.get(0);
        } else {
            int bottomBound = mItemUtilityGroupList.get(mItemUtilityGroupList.size() - 1).BOTTOM_BOUND;
            mItemUtilityGroupList.add(new ItemUtilityGroup(bottomBound, utilityItems));
        }
    }

//    public void replaceImageGroup(List<ImageItem> imageItemList, int position) {
//        if (imageItemList == null || imageItemList.size() == 0) {
//            Log.e(TAG, "Image item list can not be null or empty!");
//            return;
//        }
//
//        if (mImageGroupList.size() == 0) {
//            mImageGroupList.add(new ImageGroup(position, imageItemList, imageTransition));
//            // 默认第一个是选中的
//            mCurImageGroup = mImageGroupList.get(0);
//        } else {
//            int leftBound = mImageGroupList.get(position).LEFT_BOUND;
//            mImageGroupList.set(position, new ImageGroup(leftBound, imageItemList, imageTransition));
//        }
//        invalidate();
//    }

    public void setImageGroupListener(ImageGroupListener listener) {
        mImageGroupListener = listener;
    }

    /**
     * Given an X-axis position, judge the image group selected at this position according to this position
     *
     * @param posX The given position, the valid range of this position: [0, mWidth-mPaddingStartWidth-mPaddingEndWidth-mGroupPaddingWidth * 2]
     */
    public void select(int posX) {
//        if (posX < 0 ||
//                posX > (mWidth - mPaddingStartWidth - mPaddingEndWidth - mGroupPaddingWidth * 2)) {
//            Log.e(TAG, "Position X is out of range, should between: [" + (mPaddingStartWidth + mGroupPaddingWidth) +
//                    ", " + (mWidth - mPaddingEndWidth - mGroupPaddingWidth) + "]" + ", Current position: " + posX);
//            return;
//        }

        int curPos = mPaddingStartWidth + mGroupPaddingWidth;
        posX += curPos;
        int index = -1;
        for (ImageGroup imageGroup : mImageGroupList) {
            index++;
            if (imageGroup.isHidden) {
                continue;
            }
            if (posX >= curPos && posX <= (curPos + imageGroup.getWidth())) {
                if (mCurImageGroup != imageGroup && mGroupPaddingWidth == 0) {
                    mCurImageGroup.notifyPosX(posX - mPaddingStartWidth + mGroupPaddingWidth,
                            posX - mPaddingStartWidth + mGroupPaddingWidth);
                }
                if (mCurImageGroup != null) {
                    mCurImageGroup.isSelected = false;
                }
                imageGroup.isSelected = true;
                if (iHorizontalListChange != null && index != lastIndex) {
                    iHorizontalListChange.onItemListChange(index);
                }

                index = lastIndex;
                mCurImageGroup = imageGroup;
            } else {
                imageGroup.isSelected = false;
            }
            curPos += imageGroup.getWidth() + mGroupPaddingWidth;
        }
//        invalidate();
    }

    /**
     * Split the currently selected ImageGroup, and generate a new ImageGroup after splitting, with the old ImageGroup in the original position and the new ImageGroup in the next position
     *
     * @param posX divides at this position, the valid range of this position: (0, mWidth-mPaddingStartWidth-mPaddingEndWidth-mGroupPaddingWidth * 2)
     */
//    public void splitImageGroup(int posX) {
//        if (mCurImageGroup == null) {
//            Log.e(TAG, "Current no image group selected!");
//            return;
//        }
//
//        if (posX <= 0 || posX >= getTotalGroupWidth()) {
//            Log.e(TAG, "Position X is out of range, should between: (" + 0 +
//                    ", " + getTotalGroupWidth() + ")" + ", Current position: " + posX);
//            return;
//        }
//
//        posX += mPaddingStartWidth + mGroupPaddingWidth;
//        Log.d(TAG, "Split image group, position: " + posX);
//
//        int curIndex = mImageGroupList.indexOf(mCurImageGroup);
//        List<ImageItem> oldImageItemList = new ArrayList<>();
//        List<ImageItem> newImageItemList = new ArrayList<>();
//        int curStartPos = mPaddingStartWidth + mGroupPaddingWidth;
//        for (int i = 0; i < curIndex; i++) {
//            if (!mImageGroupList.get(i).isHidden) {
//                curStartPos += mImageGroupList.get(i).getWidth() + mGroupPaddingWidth;
//            }
//        }
//
//        int posXDelta = posX - curStartPos;
//        Log.d(TAG, "Split position x delta: " + posXDelta);
//        int imageItemPos = 0;
//        for (ImageItem imageItem : mCurImageGroup.imageItemList) {
//            if (posXDelta >= (imageItemPos + imageItem.getWidth())) {
//                oldImageItemList.add(imageItem);
//            } else if (posXDelta <= imageItemPos) {
//                newImageItemList.add(imageItem);
//            } else if (posXDelta > imageItemPos && posXDelta < (imageItemPos + imageItem.getWidth())) {
//                ImageItem oldItem = new ImageItem(imageItem.image,
//                        mImageWidth,
//                        imageItem.leftBound,
//                        imageItem.leftBound + posXDelta - imageItemPos);
//                ImageItem newItem = new ImageItem(imageItem.image,
//                        mImageWidth,
//                        posXDelta - imageItemPos,
//                        imageItem.rightBound);
//                oldImageItemList.add(oldItem);
//                newImageItemList.add(newItem);
//            }
//
//            imageItemPos += imageItem.getWidth();
//        }
//
//        // make new image group
//        ImageGroup oldImageGroup = new ImageGroup(mCurImageGroup.LEFT_BOUND, oldImageItemList);
//        ImageGroup newImageGroup = new ImageGroup(oldImageGroup.RIGHT_BOUND, newImageItemList);
//        newImageGroup.measuredLeft += 20;
//        newImageGroup.measuredRight += 20;
//
//        mImageGroupList.remove(curIndex);
//        mImageGroupList.add(curIndex, oldImageGroup);
//        mImageGroupList.add(curIndex + 1, newImageGroup);
//        oldImageGroup.isSelected = true;
//        mCurImageGroup = oldImageGroup;
//
//        if (mImageGroupListener != null) {
//            mImageGroupListener.onImageGroupSplit(curIndex, oldImageGroup.curRightPos * 1.0f / getGroupContentMaxWidth());
//        }
//
//        // for debug use
//        for (ImageGroup imageGroup : mImageGroupList) {
//            Log.d(TAG, "After spilt image group, image group: " + imageGroup);
//        }
//
//        requestLayout();
//    }

    /**
     * 隐藏当前选中的image group
     */
    public void hiddenImageGroup(int index) {
        if (index < 0 || index > mImageGroupList.size() - 1) {
            Log.e(TAG, "Index invalid! index: " + index);
            return;
        }

        mImageGroupList.get(index).isHidden = true;
        mImageGroupList.get(index).isSelected = false;
        int curIndex = mImageGroupList.indexOf(mCurImageGroup);
        if (index == curIndex) {
            // If the current image group is hidden, then the image group needs to be updated
            // The update logic is as follows:
            // 1. If curindex is not the last one, then change the current image group to the next
            // 2. If it is the last one, then change the current image group to the next one
            if (curIndex != mImageGroupList.size() - 1) {
                curIndex++;
            } else {
                curIndex--;
            }
            mCurImageGroup = mImageGroupList.get(curIndex);
            mCurImageGroup.isSelected = true;
        }

        int validIndex = 0;
        for (int i = 0; i < mImageGroupList.size(); i++) {
            if (i == index) {
                break;
            }
            if (!mImageGroupList.get(i).isHidden) {
                validIndex++;
            }
        }
        if (mImageGroupListener != null) {
            mImageGroupListener.onImageGroupHidden(validIndex);
        }

        requestLayout();
    }

    /**
     * 重新展示被隐藏的image group
     *
     * @param index 需要重新展示的index
     */
    public void showHiddenImageGroup(int index) {
        if (index < 0 || index > mImageGroupList.size() - 1) {
            Log.e(TAG, "Index invalid! index: " + index);
            return;
        }

        ImageGroup targetGroup = mImageGroupList.get(index);
        targetGroup.isHidden = false;

        requestLayout();
    }

    public void setStartPaddingWidth(int startPaddingWidth) {
        mPaddingStartWidth = startPaddingWidth;
    }

    public void setEndPaddingWidth(int endPaddingWidth) {
        mPaddingEndWidth = endPaddingWidth;
    }

    public void setGroupPaddingWidth(int width) {
        mGroupPaddingWidth = width;
    }

    public void setImageWidth(int mImageWidth) {
        this.mImageWidth = mImageWidth;
    }

    public void setImageHeight(int mImageHeight) {
        this.mImageHeight = mImageHeight;
    }

    public void setPaddingVerticalHeight(int mPaddingVerticalHeight) {
        this.mPaddingVerticalHeight = mPaddingVerticalHeight;
    }

    public void setSelectedGroupBg(Drawable mSelectedGroupBg) {
        this.mSelectedGroupBg = mSelectedGroupBg;
    }

    public void setPaddingColor(int mPaddingColor) {
        this.mPaddingColor = mPaddingColor;
    }

    public int getGroupPaddingWidth() {
        return mGroupPaddingWidth;
    }

    public int getPaddingStartWidth() {
        return mPaddingStartWidth;
    }

    public int getPaddingEndWidth() {
        return mPaddingEndWidth;
    }

    public int getGroupContentWidth() {
        int width = 0;
        for (ImageGroup imageGroup : mImageGroupList) {
            if (imageGroup.isHidden) {
                continue;
            }
            width += imageGroup.getWidth();
        }

        return width;
    }

    public int getGroupContentMaxWidth() {
        int width = 0;
        for (ImageGroup imageGroup : mImageGroupList) {
            width += imageGroup.getMaxWidth();
        }

        return width;
    }

    public int getCurImageGroupIndex() {
        return mImageGroupList.indexOf(mCurImageGroup);
    }

    /**
     * 获得除去start和end padding部分的宽度
     *
     * @return 除去start和end padding部分的宽度
     */
    private int getTotalGroupWidth() {
        int width = getGroupContentWidth();
        int validSize = 0;
        for (ImageGroup imageGroup : mImageGroupList) {
            if (!imageGroup.isHidden) {
                validSize++;
            }
        }
        if (validSize > 0) {
            width += (validSize - 1) * mGroupPaddingWidth;
        }
        return width;
    }

    public List<ImageGroup> getImageGroupList() {
        return mImageGroupList;
    }

    public List<ItemUtilityGroup> getItemUtilityGroupList() {
        return mItemUtilityGroupList;
    }

    public ImageGroup getCurImageGroup() {
        return mCurImageGroup;
    }

    private void init(Context context, AttributeSet attributeSet) {
        this.setWillNotDraw(false);

        // init params
        TypedArray typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.HorizontalThumbnailListView);
        mPaddingStartWidth = (int) typedArray.getDimension(R.styleable.HorizontalThumbnailListView_paddingStartWidth, 0);
        mPaddingEndWidth = (int) typedArray.getDimension(R.styleable.HorizontalThumbnailListView_paddingEndWidth, 0);
        mPaddingVerticalHeight = (int) typedArray.getDimension(R.styleable.HorizontalThumbnailListView_paddingVerticalHeight, 0);
        mGroupPaddingWidth = (int) typedArray.getDimension(R.styleable.HorizontalThumbnailListView_groupPaddingWidth, 0);
        mSelectedGroupBg = typedArray.getDrawable(R.styleable.HorizontalThumbnailListView_selectedGroupBg);
        mImageWidth = (int) typedArray.getDimension(R.styleable.HorizontalThumbnailListView_imageWidth, DEFAULT_IMAGE_WIDTH);
        mImageHeight = (int) typedArray.getDimension(R.styleable.HorizontalThumbnailListView_imageHeight, DEFAULT_IMAGE_HEIGHT);
        typedArray.recycle();

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mImageDstRect = new Rect();

        mScaleLinePaint = new Paint();
        mScaleLinePaint.setAntiAlias(true);
        mScaleLinePaint.setColor(context.getResources().getColor(R.color.color_text_bottom));
        mScaleLinePaint.setStyle(Paint.Style.FILL);
        mScaleLinePaint.setStrokeWidth(5F);

        mLinePaintTransition = new Paint();
        mLinePaintTransition.setAntiAlias(true);
        mLinePaintTransition.setColor(context.getResources().getColor(R.color.color_button_select));
        mLinePaintTransition.setStyle(Paint.Style.FILL);
        mLinePaintTransition.setStrokeWidth(3F);

        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(context.getResources().getColor(R.color.color_text_bottom));
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(25f);

        mTextMusicPaint = new Paint();
        mTextMusicPaint.setAntiAlias(true);
        mTextMusicPaint.setColor(Color.WHITE);
        mTextMusicPaint.setStyle(Paint.Style.FILL);
        mTextMusicPaint.setTextAlign(Paint.Align.CENTER);
        mTextMusicPaint.setTextSize(40f);

        initGestureImage();
    }

    private void initGestureImage() {
        mGestureDetectorImage = new GestureDetector(new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                if (mImageGroupListener != null && e.getY() <= mImageDstRect.top && e.getY() >= mImageDstRect.bottom) {
                    mImageGroupListener.onImageGroupClicked(mImageGroupList.indexOf(mCurImageGroup));
                }
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {

            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                return true;
            }
        });
    }


    public void setLittleBox(LittleBox littleBox, VideoComposer composer) {
        this.littleBox = littleBox;
        this.composer = composer;
    }

    public void setHorizontalListChange(@NotNull SlideShowViewModel slideShowViewModel) {
        iHorizontalListChange = slideShowViewModel;
    }

    public static class ImageItem {
        private int timeInHeader;
        private Bitmap image;

        // The rectangular area drawn by the picture, the saved positions are all relative positions
        private Rect srcRect;

        private int leftBound, rightBound;

        public ImageItem(Bitmap image, int imageSize, int leftBound, int rightBound, int timeInHeader) {
            int originWidth = image.getWidth();
            int originHeight = image.getHeight();
            float widthScale = imageSize * 1.0f / originWidth;
            float heightScale = imageSize * 1.0f / originHeight;
            Matrix matrix = new Matrix();
            matrix.postScale(widthScale, heightScale);
            this.image = Bitmap.createBitmap(image, 0, 0, originWidth, originHeight, matrix, true);
            this.leftBound = leftBound;
            this.rightBound = rightBound;
            this.srcRect = new Rect(leftBound, 0, rightBound, this.image.getHeight());
            this.timeInHeader = timeInHeader;
        }


        public int getLeft() {
            return srcRect.left;
        }

        public int getRight() {
            return srcRect.right;
        }

        public int getWidth() {
            return srcRect.right - srcRect.left;
        }

        public int getMaxWidth() {
            return rightBound - leftBound;
        }

//        @Override
//        public String toString() {
//            return "ImageItem{" +
//                    "srcRect=" + srcRect +
//                    ", leftBound=" + leftBound +
//                    ", rightBound=" + rightBound +
//                    '}';
//        }
    }


    public static class UtilityItem {
        private final Drawable drawableType;
        public String textItem;
        private Bitmap image;
        private int typeUtility;
        public static int TYPE_STICKER = 2;
        public static int TYPE_TEXT = 3;

        // The rectangular area drawn by the picture, the saved positions are all relative positions
        private Rect srcRect;

        private int leftBound, rightBound;

        public UtilityItem(String text, int typeUtility, Bitmap image, int imageSize, int leftBound, int rightBound, Drawable drawableType) {
            int originWidth = image.getWidth();
            int originHeight = image.getHeight();
            float widthScale = imageSize * 1.0f / originWidth;
            float heightScale = imageSize * 1.0f / originHeight;
            Matrix matrix = new Matrix();
            matrix.postScale(widthScale, heightScale);
            this.image = Bitmap.createBitmap(image, 0, 0, originWidth, originHeight, matrix, true);
            this.textItem = text;
            this.leftBound = leftBound;
            this.typeUtility = typeUtility;
            this.rightBound = rightBound;
            this.drawableType = drawableType;
            this.srcRect = new Rect(leftBound, 0, rightBound, this.image.getHeight());
        }


        public int getLeft() {
            return srcRect.left;
        }

        public int getRight() {
            return srcRect.right;
        }

        public int getWidth() {
            return srcRect.right - srcRect.left;
        }

        public int getHeight() {
            return srcRect.bottom - srcRect.top;
        }

        public int getMaxWidth() {
            return rightBound - leftBound;
        }


    }

    public class ImageGroup {
        private final List<ImageItem> imageItemList = new ArrayList<>();
        private final String imageTransition;

        private boolean isSelected;

        private boolean isHidden;

        // 绝对位置值
        private final int LEFT_BOUND, RIGHT_BOUND;
        private int curLeftPos, curRightPos;
        // 测量位置值
        private int measuredLeft, measuredRight;

        private String mStringExtra;

        /**
         * 构建image group
         *
         * @param leftBound       左边边界值，绝对位置
         * @param imageItemList   image item列表
         * @param imageTransition
         */
        ImageGroup(int leftBound, List<ImageItem> imageItemList, String imageTransition) {
            this.imageItemList.addAll(imageItemList);
            this.imageTransition = imageTransition;
            LEFT_BOUND = leftBound;
            curLeftPos = leftBound;
            RIGHT_BOUND = leftBound + getWidth();
            curRightPos = RIGHT_BOUND;
            measuredLeft = curLeftPos;
            measuredRight = curRightPos;
        }

        void drawImageInGroup(Canvas canvas, int basePos, String imageTransition) {

            bottomSlideImageHeight = (mPaddingVerticalHeight) + mImageHeight;
            if (isSelected && mSelectedGroupBg != null) {
                mSelectedGroupBg.setBounds(basePos - mGroupPaddingWidth,
                        (int) (mPaddingVerticalHeight + (mTextPaint.getTextSize() * 2)),
                        basePos + getWidth() + mGroupPaddingWidth,
                        bottomSlideImageHeight)
                ;
                mSelectedGroupBg.draw(canvas);
            }

            measuredLeft = basePos;
            int curPos = basePos;


            for (ImageItem imageItem : imageItemList) {
                mImageDstRect.set(
                        curPos,
                        (int) (mPaddingVerticalHeight + (mTextPaint.getTextSize() * 2)),
                        curPos + imageItem.getWidth(),
                        bottomSlideImageHeight
                );
//                Log.d(TAG, "Draw image item at: " + mImageDstRect);
                canvas.drawBitmap(imageItem.image, imageItem.srcRect, mImageDstRect, null);
                int totalSecs = imageItem.timeInHeader;
                Log.d(TAG, "Image group draw, index: " + mImageGroupList.indexOf(this) + ", totalSecs: " + totalSecs);

                if ((imageItem.timeInHeader)% 2 == 0) {
                    canvas.drawText(convertTimeToString(totalSecs), mImageDstRect.left, mPaddingVerticalHeight + mTextPaint.getTextSize() / 1.5f, mTextPaint);
                }

                canvas.drawCircle(mImageDstRect.left, (mImageDstRect.top + mPaddingVerticalHeight + mTextPaint.getTextSize() / 1.5f) / 2,4, mScaleLinePaint);
                curPos += imageItem.getWidth();
            }


            measuredRight = curPos;

        }

        void shrinkLeft(int size) {
            if (curLeftPos == curRightPos) {
                Log.e(TAG, "Current image group reach min size, can not adjust!");
                return;
            }

            int remain = size;
            if (remain > (curRightPos - curLeftPos)) {
                remain = curRightPos - curLeftPos;
                size = remain;
            }

            int leftIndex = mCurImageGroup.getLeftIndex();
            int rightIndex = mCurImageGroup.getRightIndex();
            for (int i = leftIndex; i <= rightIndex; i++) {
                ImageItem imageItem = mCurImageGroup.imageItemList.get(i);
                if (imageItem.getWidth() >= remain) {
                    imageItem.srcRect.left += remain;
                    mPaddingStartWidth += remain;
                    remain = 0;
                } else {
                    remain -= imageItem.getWidth();
                    mPaddingStartWidth += imageItem.getWidth();
                    imageItem.srcRect.left = imageItem.srcRect.right;
                }

                if (remain == 0) {
                    break;
                }
            }
            mCurImageGroup.curLeftPos += size;

            Log.d(TAG, "Adjust image group, shrink left, left progress: " + getCurLeftPos() + ", right process: " + getCurRightPos());
            int index = 0;
            for (int i = 0; i < mImageGroupList.size(); i++) {
                ImageGroup imageGroup = mImageGroupList.get(i);
                if (imageGroup == this) {
                    break;
                }
                if (!mImageGroupList.get(i).isHidden) {
                    index++;
                }
            }
            if (mImageGroupListener != null) {
                mImageGroupListener.onImageGroupLeftShrink(index,
                        curLeftPos * 1.0f / getGroupContentMaxWidth(), false);
            }
        }

        void expandLeft(int size) {
            if (curLeftPos == LEFT_BOUND) {
                Log.e(TAG, "Current image group reach left bound, can not adjust.");
                return;
            }

            int remain = size;
            if (remain > (curLeftPos - LEFT_BOUND)) {
                remain = (curLeftPos - LEFT_BOUND);
                size = remain;
            }

            int leftIndex = mCurImageGroup.getLeftIndex();
            for (int i = leftIndex; i >= 0; i--) {
                ImageItem imageItem = mCurImageGroup.imageItemList.get(i);
                if ((imageItem.srcRect.left - imageItem.leftBound) >= remain) {
                    imageItem.srcRect.left -= remain;
                    mPaddingStartWidth -= remain;
                    remain = 0;
                } else {
                    remain -= (imageItem.srcRect.left - imageItem.leftBound);
                    mPaddingStartWidth -= (imageItem.srcRect.left - imageItem.leftBound);
                    imageItem.srcRect.left = imageItem.leftBound;
                }

                if (remain == 0) {
                    break;
                }
            }
            curLeftPos -= size;

            Log.d(TAG, "Adjust image group, expand left, progress: " + curLeftPos * 1.0f / getGroupContentMaxWidth());
            int index = 0;
            for (int i = 0; i < mImageGroupList.size(); i++) {
                ImageGroup imageGroup = mImageGroupList.get(i);
                if (imageGroup == this) {
                    break;
                }
                if (!mImageGroupList.get(i).isHidden) {
                    index++;
                }
            }
            if (mImageGroupListener != null) {
                mImageGroupListener.onImageGroupLeftExpand(index,
                        curLeftPos * 1.0f / getGroupContentMaxWidth(), false);
            }
        }

        void shrinkRight(int size) {
            if (curRightPos == curLeftPos) {
                Log.e(TAG, "Adjust right, shrink right, current image group reach min size, can not adjust!");
                return;
            }

            int remain = size;
            if (remain > (curRightPos - curLeftPos)) {
                remain = curRightPos - curLeftPos;
                size = remain;
                Log.w(TAG, "Adjust right, shrink right, remain too big, adjust to: " + remain);
            }

            Log.d(TAG, "Adjust right, shrink right, remain: " + remain);
            int leftIndex = mCurImageGroup.getLeftIndex();
            int rightIndex = mCurImageGroup.getRightIndex();
            Log.d(TAG, "Adjust right, shrink right, left index: " + leftIndex + ", right index: " + rightIndex);
            for (int i = rightIndex; i >= leftIndex; i--) {
                ImageItem imageItem = mCurImageGroup.imageItemList.get(i);
                if (imageItem.getWidth() >= remain) {
                    imageItem.srcRect.right -= remain;
                    remain = 0;
                } else {
                    remain -= imageItem.getWidth();
                    imageItem.srcRect.right = imageItem.srcRect.left;
                }

                if (remain == 0) {
                    break;
                }
            }
            curRightPos -= size;

            Log.d(TAG, "Adjust image group, shrink right, progress: " + curRightPos * 1.0f / getGroupContentMaxWidth());
            int index = 0;
            for (int i = 0; i < mImageGroupList.size(); i++) {
                ImageGroup imageGroup = mImageGroupList.get(i);
                if (imageGroup == this) {
                    break;
                }
                if (!mImageGroupList.get(i).isHidden) {
                    index++;
                }
            }
            if (mImageGroupListener != null) {
                mImageGroupListener.onImageGroupRightShrink(index,
                        curRightPos * 1.0f / getGroupContentMaxWidth(), false);
            }
        }

        void expandRight(int size) {
            if (curRightPos == RIGHT_BOUND) {
                Log.e(TAG, "Adjust right, expand right, current image group reach right bound, can not adjust!");
                return;
            }

            int remain = size;
            if (remain > (RIGHT_BOUND - curRightPos)) {
                remain = RIGHT_BOUND - curRightPos;
                size = remain;
                Log.w(TAG, "Adjust right, expand right, remain too big, adjust to : " + remain);
            }

            Log.d(TAG, "Adjust right, expand right, remain: " + remain);
            int rightIndex = mCurImageGroup.getRightIndex();
            Log.d(TAG, "Adjust right, expand right, right index: " + rightIndex + ", right image item: " + mCurImageGroup.imageItemList.get(rightIndex));
            for (int i = rightIndex; i < mCurImageGroup.imageItemList.size(); i++) {
                ImageItem imageItem = mCurImageGroup.imageItemList.get(i);
                Log.d(TAG, "Adjust right, expand right, adjust image item, before: " + imageItem + ", remain: " + remain);
                if ((imageItem.rightBound - imageItem.srcRect.right) >= remain) {
                    imageItem.srcRect.right += remain;
                    remain = 0;
                } else {
                    remain -= (imageItem.rightBound - imageItem.srcRect.right);
                    imageItem.srcRect.right = imageItem.rightBound;
                }

                Log.d(TAG, "Adjust right, expand right, adjust image item, after: " + imageItem + ", remain: " + remain);
                if (remain == 0) {
                    break;
                }
            }
            curRightPos += size;

            Log.d(TAG, "Adjust image group, expand right, progress: " + curRightPos * 1.0f / getGroupContentMaxWidth());
            int index = 0;
            for (int i = 0; i < mImageGroupList.size(); i++) {
                ImageGroup imageGroup = mImageGroupList.get(i);
                if (imageGroup == this) {
                    break;
                }
                if (!mImageGroupList.get(i).isHidden) {
                    index++;
                }
            }
            if (mImageGroupListener != null) {
                mImageGroupListener.onImageGroupRightExpand(index, curRightPos * 1.0f / getGroupContentMaxWidth(), false);
            }
        }

        // 根据left pos获取当前左边的index
        int getLeftIndex() {
            int index = 0;
            int curPos = LEFT_BOUND;

            if (curLeftPos == RIGHT_BOUND) {
                index = mCurImageGroup.imageItemList.size() - 1;
            } else {
                for (int i = 0; i < imageItemList.size(); i++) {
                    ImageItem imageItem = imageItemList.get(i);
                    if (curLeftPos >= curPos && curLeftPos < (curPos + imageItem.getMaxWidth())) {
                        index = i;
                        break;
                    }
                    curPos += imageItem.getMaxWidth();
                }
            }

            return index;
        }

        int getRightIndex() {
            int index = imageItemList.size() - 1;
            int curPos = LEFT_BOUND;

            for (int i = 0; i < imageItemList.size(); i++) {
                ImageItem imageItem = imageItemList.get(i);
                if (curRightPos >= curPos && curRightPos < (curPos + imageItem.getMaxWidth())) {
                    index = i;
                    break;
                }
                curPos += imageItem.getMaxWidth();
            }

            return index;
        }

        public void addImageItem(ImageItem imageItem) {
            imageItemList.add(imageItem);
        }

        public int getWidth() {
            int width = 0;
            for (ImageItem imageItem : imageItemList) {
                width += imageItem.getWidth();
            }
            return width;
        }

        public int getMaxWidth() {
            int maxWidth = 0;
            for (ImageItem imageItem : imageItemList) {
                maxWidth += imageItem.getMaxWidth();
            }

            return maxWidth;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public boolean isHidden() {
            return isHidden;
        }

        public float getLeftBound() {
            return LEFT_BOUND * 1.0f / getGroupContentMaxWidth();
        }

        public float getRightBound() {
            return RIGHT_BOUND * 1.0f / getGroupContentMaxWidth();
        }

        public String getStringExtra() {
            return mStringExtra;
        }

        public void setStringExtra(String stringExtra) {
            this.mStringExtra = stringExtra;
        }

        /**
         * 获得当前左边百分比位置
         *
         * @return 左边的百分比位置
         */
        public float getCurLeftPos() {
            return curLeftPos * 1.0f / getGroupContentMaxWidth();
        }

        /**
         * 获得当前右边百分比位置
         *
         * @return 右边百分比位置
         */
        public float getCurRightPos() {
            return curRightPos * 1.0f / getGroupContentMaxWidth();
        }

        public int getMeasuredLeft() {
            return measuredLeft;
        }

        public int getMeasuredRight() {
            return measuredRight;
        }

        public void notifyPosX(int posX, int oldPosX) {
            mLastPosX = posX;
            setScrollX(mLastPosX);
            int index = mImageGroupList.indexOf(this);
            if ((posX + mPaddingStartWidth + mGroupPaddingWidth) >= measuredLeft &&
                    (posX + mPaddingStartWidth + mGroupPaddingWidth) <= measuredRight &&
                    mImageGroupListener != null) {

                float progress = (curLeftPos + posX + mPaddingStartWidth + mGroupPaddingWidth - measuredLeft) * 1.0f / getGroupContentMaxWidth();
                Log.d(TAG, "notifyPosX: index: " + index +
                        ", pos x: " + posX +
                        ", old pos x: " + oldPosX +
                        ", cur left pos: " + curLeftPos +
                        ", padding start: " + mPaddingStartWidth +
                        ", group padding: " + mGroupPaddingWidth +
                        ", measure left: " + measuredLeft +
                        ", measure right: " + measuredRight +
                        ", progress: " + progress);
                if (mIsScrollFromUser) {
                    composer.renderAtProgress(progress);
                }
                littleBox.draw();
                if ((oldPosX + mPaddingStartWidth + mGroupPaddingWidth) == measuredLeft ||
                        (posX + mPaddingStartWidth + mGroupPaddingWidth) == measuredLeft) {
                    Log.d(TAG, "notifyPosX: reach start!!!!!!!!! index: " + index);
                    mImageGroupListener.onImageGroupStart(index, mIsScrollFromUser);
                }
                mImageGroupListener.onImageGroupProcess(index, progress, mIsScrollFromUser);
                if ((posX + mPaddingStartWidth + mGroupPaddingWidth) == measuredRight) {
                    Log.d(TAG, "notifyPosX: reach end!!!!!!!!! index: " + index);
                    mImageGroupListener.onImageGroupEnd(index, mIsScrollFromUser);
                }
                Log.d(TAG, "progress time :" + progress);
                if (progress >= 1.0) {
                    isLast = true;
                } else {
                    isLast = false;
                }
            }
        }

//        @Override
//        public String toString() {
//            return "ImageGroup{" +
//                    "imageItemList=" + imageItemList +
//                    "\n, isSelected=" + isSelected +
//                    "\n, isHidden=" + isHidden +
//                    "\n, LEFT_BOUND=" + LEFT_BOUND +
//                    "\n, RIGHT_BOUND=" + RIGHT_BOUND +
//                    "\n, curLeftPos=" + curLeftPos +
//                    "\n, curRightPos=" + curRightPos +
//                    "\n, measuredLeft=" + measuredLeft +
//                    "\n, measuredRight=" + measuredRight +
//                    '}';
//        }
    }

    public class ItemUtilityGroup {
        public List<UtilityItem> utilityItemList = new ArrayList<>();

        private boolean isSelected;

        private boolean isHidden;

        // 绝对位置值
        private int LEFT_BOUND, RIGHT_BOUND;
        private int TOP_BOUND, BOTTOM_BOUND;
        private int curLeftPos, curRightPos;
        private int curTopPos, curBottomPos;
        // 测量位置值
        private int measuredLeft, measuredRight;

        private String mStringExtra;

        ItemUtilityGroup(int bottomBound, List<UtilityItem> utilityItemList) {
            this.utilityItemList.addAll(utilityItemList);
            curTopPos = bottomBound;
//            curLeftPos = leftBound;
            curBottomPos = bottomBound + getHeight();
//            curRightPos = RIGHT_BOUND;
//            measuredLeft = curLeftPos;
//            measuredRight = curRightPos;
        }

        void drawMusicInGroup(Canvas canvas, int basePos) {
            bottomSlideImageHeight = (mPaddingVerticalHeight) + mImageHeight;
            if (isSelected && mSelectedGroupBg != null) {
                mSelectedGroupBg.setBounds(basePos - mGroupPaddingWidth,
                        (int) (mPaddingVerticalHeight + (mTextPaint.getTextSize() * 2)),
                        basePos + getWidth() + mGroupPaddingWidth,
                        bottomSlideImageHeight)
                ;
                mSelectedGroupBg.draw(canvas);
            }

            measuredLeft = basePos;
            int curPos = basePos;
            for (UtilityItem utilityItem : utilityItemList) {
                mImageDstRect.set(
                        curPos,
                        (int) (mPaddingVerticalHeight + (mTextPaint.getTextSize() * 2)),
                        curPos + utilityItem.getWidth(),
                        bottomSlideImageHeight
                );
//                Log.d(TAG, "Draw image item at: " + mImageDstRect);
                canvas.drawBitmap(utilityItem.image, utilityItem.srcRect, mImageDstRect, null);
                curPos += utilityItem.getWidth();
            }
            measuredRight = curPos;

        }

        void shrinkLeft(int size) {
            if (curLeftPos == curRightPos) {
                Log.e(TAG, "Current image group reach min size, can not adjust!");
                return;
            }

            int remain = size;
            if (remain > (curRightPos - curLeftPos)) {
                remain = curRightPos - curLeftPos;
                size = remain;
            }

            int leftIndex = mCurItemUtilityGroup.getLeftIndex();
            int rightIndex = mCurItemUtilityGroup.getRightIndex();
            for (int i = leftIndex; i <= rightIndex; i++) {
                UtilityItem UtilityItem = mCurItemUtilityGroup.utilityItemList.get(i);
                if (UtilityItem.getWidth() >= remain) {
                    UtilityItem.srcRect.left += remain;
                    mPaddingStartWidth += remain;
                    remain = 0;
                } else {
                    remain -= UtilityItem.getWidth();
                    mPaddingStartWidth += UtilityItem.getWidth();
                    UtilityItem.srcRect.left = UtilityItem.srcRect.right;
                }

                if (remain == 0) {
                    break;
                }
            }
            mCurItemUtilityGroup.curLeftPos += size;

            Log.d(TAG, "Adjust image group, shrink left, left progress: " + getCurLeftPos() + ", right process: " + getCurRightPos());
            int index = 0;
            for (int i = 0; i < mItemUtilityGroupList.size(); i++) {
                ItemUtilityGroup ItemUtilityGroup = mItemUtilityGroupList.get(i);
                if (ItemUtilityGroup == this) {
                    break;
                }
                if (!mItemUtilityGroupList.get(i).isHidden) {
                    index++;
                }
            }
            if (mMusicGroupListener != null) {
                mMusicGroupListener.onMusicGroupLeftShrink(index,
                        curLeftPos * 1.0f / getGroupContentMaxWidth(), false);
            }
        }

        void expandLeft(int size) {
            if (curLeftPos == LEFT_BOUND) {
                Log.e(TAG, "Current image group reach left bound, can not adjust.");
                return;
            }

            int remain = size;
            if (remain > (curLeftPos - LEFT_BOUND)) {
                remain = (curLeftPos - LEFT_BOUND);
                size = remain;
            }

            int leftIndex = mCurItemUtilityGroup.getLeftIndex();
            for (int i = leftIndex; i >= 0; i--) {
                UtilityItem UtilityItem = mCurItemUtilityGroup.utilityItemList.get(i);
                if ((UtilityItem.srcRect.left - UtilityItem.leftBound) >= remain) {
                    UtilityItem.srcRect.left -= remain;
                    mPaddingStartWidth -= remain;
                    remain = 0;
                } else {
                    remain -= (UtilityItem.srcRect.left - UtilityItem.leftBound);
                    mPaddingStartWidth -= (UtilityItem.srcRect.left - UtilityItem.leftBound);
                    UtilityItem.srcRect.left = UtilityItem.leftBound;
                }

                if (remain == 0) {
                    break;
                }
            }
            curLeftPos -= size;

            Log.d(TAG, "Adjust image group, expand left, progress: " + curLeftPos * 1.0f / getGroupContentMaxWidth());
            int index = 0;
            for (int i = 0; i < mItemUtilityGroupList.size(); i++) {
                ItemUtilityGroup ItemUtilityGroup = mItemUtilityGroupList.get(i);
                if (ItemUtilityGroup == this) {
                    break;
                }
                if (!mItemUtilityGroupList.get(i).isHidden) {
                    index++;
                }
            }
            if (mMusicGroupListener != null) {
                mMusicGroupListener.onMusicGroupLeftExpand(index,
                        curLeftPos * 1.0f / getGroupContentMaxWidth(), false);
            }
        }

        void shrinkRight(int size) {
            if (curRightPos == curLeftPos) {
                Log.e(TAG, "Adjust right, shrink right, current image group reach min size, can not adjust!");
                return;
            }

            int remain = size;
            if (remain > (curRightPos - curLeftPos)) {
                remain = curRightPos - curLeftPos;
                size = remain;
                Log.w(TAG, "Adjust right, shrink right, remain too big, adjust to: " + remain);
            }

            Log.d(TAG, "Adjust right, shrink right, remain: " + remain);
            int leftIndex = mCurItemUtilityGroup.getLeftIndex();
            int rightIndex = mCurItemUtilityGroup.getRightIndex();
            Log.d(TAG, "Adjust right, shrink right, left index: " + leftIndex + ", right index: " + rightIndex);
            for (int i = rightIndex; i >= leftIndex; i--) {
                UtilityItem UtilityItem = mCurItemUtilityGroup.utilityItemList.get(i);
                if (UtilityItem.getWidth() >= remain) {
                    UtilityItem.srcRect.right -= remain;
                    remain = 0;
                } else {
                    remain -= UtilityItem.getWidth();
                    UtilityItem.srcRect.right = UtilityItem.srcRect.left;
                }

                if (remain == 0) {
                    break;
                }
            }
            curRightPos -= size;

            Log.d(TAG, "Adjust image group, shrink right, progress: " + curRightPos * 1.0f / getGroupContentMaxWidth());
            int index = 0;
            for (int i = 0; i < mItemUtilityGroupList.size(); i++) {
                ItemUtilityGroup ItemUtilityGroup = mItemUtilityGroupList.get(i);
                if (ItemUtilityGroup == this) {
                    break;
                }
                if (!mItemUtilityGroupList.get(i).isHidden) {
                    index++;
                }
            }
            if (mMusicGroupListener != null) {
                mMusicGroupListener.onMusicGroupRightShrink(index,
                        curRightPos * 1.0f / getGroupContentMaxWidth(), false);
            }
        }

        void expandRight(int size) {
            if (curRightPos == RIGHT_BOUND) {
                Log.e(TAG, "Adjust right, expand right, current image group reach right bound, can not adjust!");
                return;
            }

            int remain = size;
            if (remain > (RIGHT_BOUND - curRightPos)) {
                remain = RIGHT_BOUND - curRightPos;
                size = remain;
                Log.w(TAG, "Adjust right, expand right, remain too big, adjust to : " + remain);
            }

            Log.d(TAG, "Adjust right, expand right, remain: " + remain);
            int rightIndex = mCurItemUtilityGroup.getRightIndex();
            Log.d(TAG, "Adjust right, expand right, right index: " + rightIndex + ", right image item: " + mCurItemUtilityGroup.utilityItemList.get(rightIndex));
            for (int i = rightIndex; i < mCurItemUtilityGroup.utilityItemList.size(); i++) {
                UtilityItem UtilityItem = mCurItemUtilityGroup.utilityItemList.get(i);
                Log.d(TAG, "Adjust right, expand right, adjust image item, before: " + UtilityItem + ", remain: " + remain);
                if ((UtilityItem.rightBound - UtilityItem.srcRect.right) >= remain) {
                    UtilityItem.srcRect.right += remain;
                    remain = 0;
                } else {
                    remain -= (UtilityItem.rightBound - UtilityItem.srcRect.right);
                    UtilityItem.srcRect.right = UtilityItem.rightBound;
                }

                Log.d(TAG, "Adjust right, expand right, adjust image item, after: " + UtilityItem + ", remain: " + remain);
                if (remain == 0) {
                    break;
                }
            }
            curRightPos += size;

            Log.d(TAG, "Adjust image group, expand right, progress: " + curRightPos * 1.0f / getGroupContentMaxWidth());
            int index = 0;
            for (int i = 0; i < mItemUtilityGroupList.size(); i++) {
                ItemUtilityGroup ItemUtilityGroup = mItemUtilityGroupList.get(i);
                if (ItemUtilityGroup == this) {
                    break;
                }
                if (!mItemUtilityGroupList.get(i).isHidden) {
                    index++;
                }
            }
            if (mMusicGroupListener != null) {
                mMusicGroupListener.onMusicGroupRightExpand(index, curRightPos * 1.0f / getGroupContentMaxWidth(), false);
            }
        }

        // 根据left pos获取当前左边的index
        int getLeftIndex() {
            int index = 0;
            int curPos = LEFT_BOUND;

            if (curLeftPos == RIGHT_BOUND) {
                index = mCurItemUtilityGroup.utilityItemList.size() - 1;
            } else {
                for (int i = 0; i < utilityItemList.size(); i++) {
                    UtilityItem UtilityItem = utilityItemList.get(i);
                    if (curLeftPos >= curPos && curLeftPos < (curPos + UtilityItem.getMaxWidth())) {
                        index = i;
                        break;
                    }
                    curPos += UtilityItem.getMaxWidth();
                }
            }

            return index;
        }

        int getRightIndex() {
            int index = utilityItemList.size() - 1;
            int curPos = LEFT_BOUND;

            for (int i = 0; i < utilityItemList.size(); i++) {
                UtilityItem UtilityItem = utilityItemList.get(i);
                if (curRightPos >= curPos && curRightPos < (curPos + UtilityItem.getMaxWidth())) {
                    index = i;
                    break;
                }
                curPos += UtilityItem.getMaxWidth();
            }

            return index;
        }

        public void addMusicItem(UtilityItem UtilityItem) {
            utilityItemList.add(UtilityItem);
        }

        public int getWidth() {
            int width = 0;
            for (UtilityItem UtilityItem : utilityItemList) {
                width += UtilityItem.getWidth();
            }
            return width;
        }

        public int getHeight() {
            int height = 0;
            for (UtilityItem utilityItem : utilityItemList) {
                height += utilityItem.getHeight();
            }
            return height;
        }

        public int getMaxWidth() {
            int maxWidth = 0;
            for (UtilityItem UtilityItem : utilityItemList) {
                maxWidth += UtilityItem.getMaxWidth();
            }

            return maxWidth;
        }

        public boolean isSelected() {
            return isSelected;
        }

        public boolean isHidden() {
            return isHidden;
        }

        public float getLeftBound() {
            return LEFT_BOUND * 1.0f / getGroupContentMaxWidth();
        }

        public float getRightBound() {
            return RIGHT_BOUND * 1.0f / getGroupContentMaxWidth();
        }

        public String getStringExtra() {
            return mStringExtra;
        }

        public void setStringExtra(String stringExtra) {
            this.mStringExtra = stringExtra;
        }

        /**
         * 获得当前左边百分比位置
         *
         * @return 左边的百分比位置
         */
        public float getCurLeftPos() {
            return curLeftPos * 1.0f / getGroupContentMaxWidth();
        }

        /**
         * 获得当前右边百分比位置
         *
         * @return 右边百分比位置
         */
        public float getCurRightPos() {
            return curRightPos * 1.0f / getGroupContentMaxWidth();
        }

        public int getMeasuredLeft() {
            return measuredLeft;
        }

        public int getMeasuredRight() {
            return measuredRight;
        }

        public void notifyPosX(int posX, int oldPosX) {
            mLastPosX = posX;
            setScrollX(mLastPosX);
            int index = mItemUtilityGroupList.indexOf(this);
            if ((posX + mPaddingStartWidth + mGroupPaddingWidth) >= measuredLeft &&
                    (posX + mPaddingStartWidth + mGroupPaddingWidth) <= measuredRight &&
                    mMusicGroupListener != null) {

                float progress = (curLeftPos + posX + mPaddingStartWidth + mGroupPaddingWidth - measuredLeft) * 1.0f / getGroupContentMaxWidth();
                Log.d(TAG, "notifyPosX: index: " + index +
                        ", pos x: " + posX +
                        ", old pos x: " + oldPosX +
                        ", cur left pos: " + curLeftPos +
                        ", padding start: " + mPaddingStartWidth +
                        ", group padding: " + mGroupPaddingWidth +
                        ", measure left: " + measuredLeft +
                        ", measure right: " + measuredRight +
                        ", progress: " + progress);
                littleBox.draw();
                if ((oldPosX + mPaddingStartWidth + mGroupPaddingWidth) == measuredLeft ||
                        (posX + mPaddingStartWidth + mGroupPaddingWidth) == measuredLeft) {
                    Log.d(TAG, "notifyPosX: reach start!!!!!!!!! index: " + index);
                    mMusicGroupListener.onMusicGroupStart(index, mIsScrollFromUser);
                }
                mMusicGroupListener.onMusicGroupProcess(index, progress, mIsScrollFromUser);
                if ((posX + mPaddingStartWidth + mGroupPaddingWidth) == measuredRight) {
                    Log.d(TAG, "notifyPosX: reach end!!!!!!!!! index: " + index);
                    mMusicGroupListener.onMusicGroupEnd(index, mIsScrollFromUser);
                }
                Log.d(TAG, "progress time :" + progress);
                if (progress >= 1.0) {
                    isLast = true;
                } else {
                    isLast = false;
                }
            }
        }

        public void drawItemInGroup(Canvas canvas, int curPos, int cusPositionHeight, UtilityItem utilityItem) {
            int countPaddingWidthImage =
                    (mGroupPaddingWidth * getImageGroupList().size()) + mGroupPaddingWidth;

            if (utilityItem != null) {
                utilityItem.drawableType.setBounds(curPos,
                        (int) cusPositionHeight + DEFAULT_PADDING_HEIGHT_ITEM,
                        curPos + getGroupContentMaxWidth() +
                                countPaddingWidthImage,
                        cusPositionHeight + DEFAULT_MUSIC_WIDTH + DEFAULT_PADDING_HEIGHT_ITEM);
                utilityItem.drawableType.draw(canvas);
            }
            Bitmap bitmapImageAddMusic = utilityItem.image;
            Drawable drawableAddMusic = new BitmapDrawable(getResources(), bitmapImageAddMusic);
//            int paddingImage = 20;
//            mMusicDstRect = new Rect(curPos + mGroupPaddingWidth + paddingImage,
//                    (int) cusPositionHeight + DEFAULT_PADDING_HEIGHT_ITEM + paddingImage,
//                    curPos + mGroupPaddingWidth + DEFAULT_MUSIC_WIDTH - paddingImage,
//                    cusPositionHeight + DEFAULT_MUSIC_WIDTH + DEFAULT_PADDING_HEIGHT_ITEM - paddingImage);
//
//            drawableAddMusic.setBounds(mMusicDstRect);
//            drawableAddMusic.draw(canvas);

//            if (utilityItem.typeUtility == UtilityItem.TYPE_MUSIC) {
//                canvas.drawText(textMusicSong, mMusicDstRect.right + DEFAULT_MUSIC_WIDTH, mMusicDstRect.centerY() + mTextMusicPaint.getTextSize() / 4, mTextMusicPaint);
//            } else {
            canvas.drawText(utilityItem.textItem, mMusicDstRect.right + DEFAULT_MUSIC_WIDTH, mMusicDstRect.centerY() + mTextMusicPaint.getTextSize() / 4, mTextMusicPaint);
//            }
        }

//        private void drawSoundWaveForm(Canvas canvas) {
//            playerVisualizerView = new PlayerVisualizerView(getContext());
//            playerVisualizerView.layout(curPos,
//                    (int) cusPositionHeight + DEFAULT_PADDING_HEIGHT_ITEM,
//                    curPos + getGroupContentMaxWidth() +
//                            countPaddingWidthImage,
//                    cusPositionHeight + DEFAULT_MUSIC_WIDTH + DEFAULT_PADDING_HEIGHT_ITEM);
//            playerVisualizerView.setBackgroundColor(Color.BLACK);
//            playerVisualizerView.draw(canvas);
//        }

//        @Override
//        public String toString() {
//            return "MusicGroup{" +
//                    "MusicItemList=" + MusicItemList +
//                    "\n, isSelected=" + isSelected +
//                    "\n, isHidden=" + isHidden +
//                    "\n, LEFT_BOUND=" + LEFT_BOUND +
//                    "\n, RIGHT_BOUND=" + RIGHT_BOUND +
//                    "\n, curLeftPos=" + curLeftPos +
//                    "\n, curRightPos=" + curRightPos +
//                    "\n, measuredLeft=" + measuredLeft +
//                    "\n, measuredRight=" + measuredRight +
//                    '}';
//        }
    }

    public String getTextMusicSong() {
        return textMusicSong;
    }

//    public void setTextMusicSong(String textMusicSong) {
//        this.textMusicSong = textMusicSong;
//    }

    private String convertTimeToString(int totalSecs) {
        int minutes = (totalSecs % 3600) / 60;
        int seconds = totalSecs % 60;
        String timeString = String.format("%02d:%02d", minutes, seconds);
        return timeString;
    }


    public int getmLastPosX() {
        return mLastPosX;
    }
}
