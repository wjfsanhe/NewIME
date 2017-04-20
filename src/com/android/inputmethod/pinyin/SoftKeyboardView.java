/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.inputmethod.pinyin;

import com.android.inputmethod.pinyin.SoftKeyboard.KeyRow;

import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.*;
import android.util.Log;
import android.app.*;
import android.graphics.Color;
import android.view.KeyEvent;
import android.graphics.Point ;


/**
 * Class used to show a soft keyboard.
 * <p>
 * A soft keyboard view should not handle touch event itself, because we do bias
 * correction, need a global strategy to map an event into a proper view to
 * achieve better user experience.
 */
public class SoftKeyboardView extends View {
    private static String TAG = "SoftKeyboardView";
    float pos[] = {-1.02f, -0.48f, -1.8f, -1.02f, -1.22f, -1.4f, 1.14f, -1.22f, -1.4f};
    /**
     * The definition of the soft keyboard for the current this soft keyboard
     * view.
     */
    private SoftKeyboard mSoftKeyboard;

    /**
     * The popup balloon hint for key press/release.
     */
    private BalloonHint mBalloonPopup;

    /**
     * The on-key balloon hint for key press/release. If it is null, on-key
     * highlight will be drawn on th soft keyboard view directly.
     */
    private BalloonHint mBalloonOnKey;

    /**
     * Used to play key sounds.
     */
    private SoundManager mSoundManager;

    /**
     * The last key pressed.
     */
    private SoftKey mSoftKeyDown;

    /**
     * Used to indicate whether the user is holding on a key.
     */
    private boolean mKeyPressed = false;

    /**
     * The location offset of the view to the keyboard container.
     */
    private int mOffsetToSkbContainer[] = new int[2];

    /**
     * The location of the desired hint view to the keyboard container.
     */
    private int mHintLocationToSkbContainer[] = new int[2];

    /**
     * Text size for normal key.
     */
    private int mNormalKeyTextSize;

    /**
     * Text size for function key.
     */
    private int mFunctionKeyTextSize;

    /**
     * Long press timer used to response long-press.
     */
    private SkbContainer.LongPressTimer mLongPressTimer;

    /**
     * Repeated events for long press
     */
    private boolean mRepeatForLongPress = false;

    /**
     * If this parameter is true, the balloon will never be dismissed even if
     * user moves a lot from the pressed point.
     */
    private boolean mMovingNeverHidePopupBalloon = false;

    /**
     * Vibration for key press.
     */
    private Vibrator mVibrator;

    /**
     * Vibration pattern for key press.
     */
    protected long[] mVibratePattern = new long[]{1, 20};

    /**
     * The dirty rectangle used to mark the area to re-draw during key press and
     * release. Currently, whenever we can invalidate(Rect), view will call
     * onDraw() and we MUST draw the whole view. This dirty information is for
     * future use.
     */
    private Rect mDirtyRect = new Rect();

    private Paint mPaint;
    private FontMetricsInt mFmi;
    private boolean mDimSkb;
    public SoftKey focusKey;
    public boolean mSkbViewFocus = false;

    private Point mFocusPoint = new Point(-1,-1);
    private boolean mFocusEnable = true;
    private Point mFocusAxis = new Point(0,0);

    private void setSpace() {
/*
        try {
            ViewRootImpl root = getViewRootImpl();
            SurfaceHolder holder = root.getSurfaceHolder();
            Surface sur = root.getSurface();

            if (sur.isValid()) {
                SurfaceEx surx = new SurfaceEx(sur);

                surx.setDisplayMode(SurfaceEx.eVRDisplayMode);
                surx.setSpacePos(pos);
                surx.setSpaceSide(1);
            } else if (null != holder)
                holder.addCallback(new SurfaceHolder.Callback2() {
                    public void surfaceCreated(SurfaceHolder holder) {
                        Surface s = holder.getSurface();
                        SurfaceEx sur = new SurfaceEx(s);

                        sur.setDisplayMode(SurfaceEx.eVRDisplayMode);
                        sur.setSpacePos(pos);
                        sur.setSpaceSide(1);
                    }

                    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                    }

                    public void surfaceDestroyed(SurfaceHolder holder) {
                    }

                    public void surfaceRedrawNeeded(SurfaceHolder holder) {
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "space set e=" + e);
        }
*/
    }
    public Point getCurrentFocusAxis(){
        return mFocusAxis;
    }
    public SoftKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mSoundManager = SoundManager.getInstance(mContext);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mFmi = mPaint.getFontMetricsInt();
        mFocusPoint.x=-1;
        mFocusPoint.y=-1;
        this.setOnHoverListener(new OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                int what = event.getAction();
                Log.d(TAG,"on Key Hover");
                
                switch (what) {
                    case MotionEvent.ACTION_HOVER_ENTER:
                        break;
                    case MotionEvent.ACTION_HOVER_MOVE:
                        SoftKey mKey = mSoftKeyboard.mapToKey((int) event.getX(), (int) event.getY());
                        if (mKey != null) {
                            onSkbViewFocus(true);
                            setSoftKeyHl(mKey);
                        }
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                        onSkbViewFocus(false);
                        invalidate();
                        break;
                }
                return false;
            }
        });
        
        this.setOnKeyListener(new  OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                //Log.d(TAG,"on Key Listener trigger");
                int what = event.getAction();
                switch (what){
                      case  KeyEvent.ACTION_DOWN:
                        onSkbViewFocus(false);
                            receiveKeyDown(keyCode, event);
                            break ;
                      case  KeyEvent.ACTION_UP:
                            receiveKeyUp(keyCode, event);
                            break;
                }
                return false ;
            }
        });
    }

    public boolean setSoftKeyboard(SoftKeyboard softSkb) {
        if (null == softSkb) {
            return false;
        }
        mSoftKeyboard = softSkb;
        Drawable bg = softSkb.getSkbBackground();
        if (null != bg) setBackgroundDrawable(bg);
        return true;
    }

    public SoftKeyboard getSoftKeyboard() {
        return mSoftKeyboard;
    }

    public void resizeKeyboard(int skbWidth, int skbHeight) {
        mSoftKeyboard.setSkbCoreSize(skbWidth, skbHeight);
    }

    public void setBalloonHint(BalloonHint balloonOnKey,
                               BalloonHint balloonPopup, boolean movingNeverHidePopup) {
        mBalloonOnKey = balloonOnKey;
        mBalloonPopup = balloonPopup;
        mMovingNeverHidePopupBalloon = movingNeverHidePopup;
    }

    public void setOffsetToSkbContainer(int offsetToSkbContainer[]) {
        mOffsetToSkbContainer[0] = offsetToSkbContainer[0];
        mOffsetToSkbContainer[1] = offsetToSkbContainer[1];
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = 0;
        int measuredHeight = 0;
        if (null != mSoftKeyboard) {
            measuredWidth = mSoftKeyboard.getSkbCoreWidth();
            measuredHeight = mSoftKeyboard.getSkbCoreHeight();
            measuredWidth += mPaddingLeft + mPaddingRight;
            measuredHeight += mPaddingTop + mPaddingBottom;
        }
        setMeasuredDimension(measuredWidth, measuredHeight);
        setSpace();
    }

    private void showBalloon(BalloonHint balloon, int balloonLocationToSkb[],
                             boolean movePress) {
        long delay = BalloonHint.TIME_DELAY_SHOW;
        if (movePress) delay = 0;
        if (balloon.needForceDismiss()) {
            balloon.delayedDismiss(0);
        }
        if (!balloon.isShowing()) {
            balloon.delayedShow(delay, balloonLocationToSkb);
        } else {
            balloon.delayedUpdate(delay, balloonLocationToSkb, balloon
                    .getWidth(), balloon.getHeight());
        }
        long b = System.currentTimeMillis();
    }

    public void resetKeyPress(long balloonDelay) {
        if (!mKeyPressed) return;
        mKeyPressed = false;
        if (null != mBalloonOnKey) {
            mBalloonOnKey.delayedDismiss(balloonDelay);
        } else {
            if (null != mSoftKeyDown) {
                if (mDirtyRect.isEmpty()) {
                    mDirtyRect.set(mSoftKeyDown.mLeft, mSoftKeyDown.mTop,
                            mSoftKeyDown.mRight, mSoftKeyDown.mBottom);
                }
                invalidate(mDirtyRect);
            } else {
                invalidate();
            }
        }
        mBalloonPopup.delayedDismiss(balloonDelay);
    }




    // If movePress is true, means that this function is called because user
    // moves his finger to this button. If movePress is false, means that this
    // function is called when user just presses this key.
    public SoftKey onKeyPress(int x, int y,
                              SkbContainer.LongPressTimer longPressTimer, boolean movePress) {
        mKeyPressed = false;
        boolean moveWithinPreviousKey = false;
        if (movePress) {
            SoftKey newKey = mSoftKeyboard.mapToKey(x, y);
            if (newKey == mSoftKeyDown) moveWithinPreviousKey = true;
            mSoftKeyDown = newKey;
        } else {
            mSoftKeyDown = mSoftKeyboard.mapToKey(x, y);
        }
        if (moveWithinPreviousKey || null == mSoftKeyDown) return mSoftKeyDown;
        mKeyPressed = true;

//        if (!movePress) {
//            tryPlayKeyDown();
//            tryVibrate();
//        }

        mLongPressTimer = longPressTimer;

        if (!movePress) {
            if (mSoftKeyDown.getPopupResId() > 0 || mSoftKeyDown.repeatable()) {
                mLongPressTimer.startTimer();
            }
        } else {
            mLongPressTimer.removeTimer();
        }

        int desired_width;
        int desired_height;
        float textSize;
        Environment env = Environment.getInstance();

        if (null != mBalloonOnKey) {
            Drawable keyHlBg = mSoftKeyDown.getKeyHlBg();
            mBalloonOnKey.setBalloonBackground(keyHlBg);

            // Prepare the on-key balloon
            int keyXMargin = mSoftKeyboard.getKeyXMargin();
            int keyYMargin = mSoftKeyboard.getKeyYMargin();
            desired_width = mSoftKeyDown.width() - 2 * keyXMargin;
            desired_height = mSoftKeyDown.height() - 2 * keyYMargin;
            textSize = env
                    .getKeyTextSize(SoftKeyType.KEYTYPE_ID_NORMAL_KEY != mSoftKeyDown.mKeyType.mKeyTypeId);
            Drawable icon = mSoftKeyDown.getKeyIcon();
            if (null != icon) {
                mBalloonOnKey.setBalloonConfig(icon, desired_width,
                        desired_height);
            } else {
                mBalloonOnKey.setBalloonConfig(mSoftKeyDown.getKeyLabel(),
                        textSize, true, mSoftKeyDown.getColorHl(),
                        desired_width, desired_height);
            }

            mHintLocationToSkbContainer[0] = mPaddingLeft + mSoftKeyDown.mLeft
                    - (mBalloonOnKey.getWidth() - mSoftKeyDown.width()) / 2;
            mHintLocationToSkbContainer[0] += mOffsetToSkbContainer[0];
            mHintLocationToSkbContainer[1] = mPaddingTop
                    + (mSoftKeyDown.mBottom - keyYMargin)
                    - mBalloonOnKey.getHeight();
            mHintLocationToSkbContainer[1] += mOffsetToSkbContainer[1];
            //showBalloon(mBalloonOnKey, mHintLocationToSkbContainer, movePress);
        } else {
            mDirtyRect.union(mSoftKeyDown.mLeft, mSoftKeyDown.mTop,
                    mSoftKeyDown.mRight, mSoftKeyDown.mBottom);
            invalidate(mDirtyRect);
        }

        // Prepare the popup balloon
        if (mSoftKeyDown.needBalloon()) {
            Drawable balloonBg = mSoftKeyboard.getBalloonBackground();
            mBalloonPopup.setBalloonBackground(balloonBg);

            desired_width = mSoftKeyDown.width() + env.getKeyBalloonWidthPlus();
            desired_height = mSoftKeyDown.height()
                    + env.getKeyBalloonHeightPlus();
            textSize = env
                    .getBalloonTextSize(SoftKeyType.KEYTYPE_ID_NORMAL_KEY != mSoftKeyDown.mKeyType.mKeyTypeId);
            Drawable iconPopup = mSoftKeyDown.getKeyIconPopup();
            if (null != iconPopup) {
                mBalloonPopup.setBalloonConfig(iconPopup, desired_width,
                        desired_height);
            } else {
                mBalloonPopup.setBalloonConfig(mSoftKeyDown.getKeyLabel(),
                        textSize, mSoftKeyDown.needBalloon(), mSoftKeyDown
                                .getColorBalloon(), desired_width,
                        desired_height);
            }

            // The position to show.
            mHintLocationToSkbContainer[0] = mPaddingLeft + mSoftKeyDown.mLeft
                    + -(mBalloonPopup.getWidth() - mSoftKeyDown.width()) / 2;
            mHintLocationToSkbContainer[0] += mOffsetToSkbContainer[0];
            mHintLocationToSkbContainer[1] = mPaddingTop + mSoftKeyDown.mTop
                    - mBalloonPopup.getHeight();
            mHintLocationToSkbContainer[1] += mOffsetToSkbContainer[1];
            //showBalloon(mBalloonPopup, mHintLocationToSkbContainer, movePress);
        } else {
            mBalloonPopup.delayedDismiss(0);
        }

        if (mRepeatForLongPress) longPressTimer.startTimer();
        return mSoftKeyDown;
    }
    private void updateFocusAxis(){
            //update focus axis
            KeyRow keyRow = mSoftKeyboard.getKeyRowForDisplay(mFocusPoint.y);
            if(keyRow == null) {
               Log.e(TAG,"Error! keyRow is null") ;
               /*   mFocusPoint.y = 1; 
                  mFocusPoint.x = 1;
                  keyRow = mSoftKeyboard.getKeyRowForDisplay(mFocusPoint.y); */
               return ;
            }
            List<SoftKey> softKeys = keyRow.mSoftKeys;
            SoftKey softKey = softKeys.get(mFocusPoint.x );
            mFocusAxis.x = softKey.mLeft + 10;
            mFocusAxis.y = softKey.mTop + 10;
    }
    private boolean updateFocusPoint(int keycode){
            if (null == mSoftKeyboard) return false;

            int rowNum = mSoftKeyboard.getRowNum();
            int[] rowSet = new int[rowNum];
            Point lastPoint = new Point(mFocusPoint) ;
            Log.d(TAG,"row number:" + rowNum);
            //prepare keyboard statistics
            for (int row = 0; row < rowNum; row++) {
                    KeyRow keyRow = mSoftKeyboard.getKeyRowForDisplay(row);
                    if (null == keyRow) {
                        rowSet[row]=0;
                        continue;
                    }
                    List<SoftKey> softKeys = keyRow.mSoftKeys;
                    if (softKeys == null) {
                      rowSet[row] = 0;
                      continue;
                    }
                    int keyNum = softKeys.size();
                    rowSet[row]=keyNum;
                    Log.d(TAG,"row " + row + " : " + rowSet[row] + " keys;");
            }
            
            Log.d(TAG,"ld key position:" + lastPoint.x + "," + lastPoint.y);
            switch (keycode){                                                                            
                    case  KeyEvent.KEYCODE_DPAD_UP: //up key
                          if(mFocusPoint.y > 0) {
                              do {
                                  mFocusPoint.y -= 1;
                              } while ( rowSet[mFocusPoint.y] == 0 && mFocusPoint.y > 0) ;//skip toggle line
                              if(rowSet[mFocusPoint.y] == 0 && mFocusPoint.y == 0 )
                              return false;

                              if(mFocusPoint.x >= rowSet[mFocusPoint.y]){
                                  mFocusPoint.x = rowSet[mFocusPoint.y] - 1;
                              }
                          }
                          break;
                    case  KeyEvent.KEYCODE_DPAD_DOWN:
                          if(mFocusPoint.y < (rowNum - 1)) {
                              do {
                                    mFocusPoint.y += 1;
                              } while ( rowSet[mFocusPoint.y] == 0 && mFocusPoint.y < (rowNum - 1)) ;//skip toggle line
                              
                              if(rowSet[mFocusPoint.y] == 0 && mFocusPoint.y == (rowNum - 1)) //no need update 
                              return false;
                              
                              if(mFocusPoint.x >= rowSet[mFocusPoint.y]){
                                  mFocusPoint.x = rowSet[mFocusPoint.y] - 1;
                              }
                          }
                          break;
                    case  KeyEvent.KEYCODE_DPAD_LEFT: //
                          if(mFocusPoint.x > 0){
                              mFocusPoint.x -= 1;
                          }
                          break;
                    case  KeyEvent.KEYCODE_DPAD_RIGHT: //
                          if(mFocusPoint.x < (rowSet[mFocusPoint.y] - 1)){
                              mFocusPoint.x += 1;
                          }
                          break;
            }
            //update focus axis
            updateFocusAxis();
            Log.d(TAG,"new key position:" + mFocusPoint.x + "," + mFocusPoint.y);



            return ((lastPoint.x != mFocusPoint.x) || (lastPoint.y != mFocusPoint.y)) ;              
    }
    public void onDPADKey(int  keycode){
            boolean needRefresh=false;
            needRefresh = updateFocusPoint(keycode);
            
            if(needRefresh){
              Log.d(TAG,"!refresh");
              invalidate();
            }
    }
    public SoftKey onKeyRelease(int x, int y) {
        mKeyPressed = false;
        if (null == mSoftKeyDown) return null;

        mLongPressTimer.removeTimer();

        tryPlayKeyDown();
        tryVibrate();

        if (null != mBalloonOnKey) {
            mBalloonOnKey.delayedDismiss(BalloonHint.TIME_DELAY_DISMISS);
        } else {
            mDirtyRect.union(mSoftKeyDown.mLeft, mSoftKeyDown.mTop,
                    mSoftKeyDown.mRight, mSoftKeyDown.mBottom);
            invalidate(mDirtyRect);
        }

        if (mSoftKeyDown.needBalloon()) {
            mBalloonPopup.delayedDismiss(BalloonHint.TIME_DELAY_DISMISS);
        }

        if (mSoftKeyDown.moveWithinKey(x - mPaddingLeft, y - mPaddingTop)) {
            return mSoftKeyDown;
        }
        return null;
    }

    public SoftKey onKeyMove(int x, int y) {
        if (null == mSoftKeyDown) return null;

        if (mSoftKeyDown.moveWithinKey(x - mPaddingLeft, y - mPaddingTop)) {
            return mSoftKeyDown;
        }

        // The current key needs to be updated.
        mDirtyRect.union(mSoftKeyDown.mLeft, mSoftKeyDown.mTop,
                mSoftKeyDown.mRight, mSoftKeyDown.mBottom);

        if (mRepeatForLongPress) {
            if (mMovingNeverHidePopupBalloon) {
                return onKeyPress(x, y, mLongPressTimer, true);
            }

            if (null != mBalloonOnKey) {
                mBalloonOnKey.delayedDismiss(0);
            } else {
                invalidate(mDirtyRect);
            }

            if (mSoftKeyDown.needBalloon()) {
                mBalloonPopup.delayedDismiss(0);
            }

            if (null != mLongPressTimer) {
                mLongPressTimer.removeTimer();
            }
            return onKeyPress(x, y, mLongPressTimer, true);
        } else {
            // When user moves between keys, repeated response is disabled.
            return onKeyPress(x, y, mLongPressTimer, true);
        }
    }

    private void tryVibrate() {
        if (!Settings.getVibrate()) {
            return;
        }
        if (mVibrator == null) {
            mVibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        }
        mVibrator.vibrate(mVibratePattern, -1);
    }

    private void tryPlayKeyDown() {
        if (Settings.getKeySound()) {
            mSoundManager.playKeyDown();
        }
    }

    public void dimSoftKeyboard(boolean dimSkb) {
        mDimSkb = dimSkb;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (null == mSoftKeyboard) return;

        if(mFocusPoint.x < 0 ){
                mFocusPoint.x=1;
                mFocusPoint.y=1;
        }

        KeyRow focusRow = mSoftKeyboard.getKeyRowForDisplay(mFocusPoint.y);
        if(null == focusRow){
                Log.d(TAG,"update keyboard have null row");
                mFocusPoint.x=1;
                mFocusPoint.y=1;
                updateFocusAxis();
        }

        Log.d(TAG,"onDraw to refffresh");
        canvas.translate(mPaddingLeft, mPaddingTop);

        Environment env = Environment.getInstance();
        mNormalKeyTextSize = env.getKeyTextSize(false);
        mFunctionKeyTextSize = env.getKeyTextSize(true);
        // Draw the last soft keyboard
        int rowNum = mSoftKeyboard.getRowNum();
        int keyXMargin = mSoftKeyboard.getKeyXMargin();
        int keyYMargin = mSoftKeyboard.getKeyYMargin();
       
        Log.d(TAG,"onDraw row " + rowNum); 
        for (int row = 0; row < rowNum; row++) {
            KeyRow keyRow = mSoftKeyboard.getKeyRowForDisplay(row);
            if (null == keyRow) continue;
            List<SoftKey> softKeys = keyRow.mSoftKeys;
            int keyNum = softKeys.size();
            for (int i = 0; i < keyNum; i++) {
                SoftKey softKey = softKeys.get(i);
                if (SoftKeyType.KEYTYPE_ID_NORMAL_KEY == softKey.mKeyType.mKeyTypeId) {
                    mPaint.setTextSize(mNormalKeyTextSize);
                } else {
                    mPaint.setTextSize(mFunctionKeyTextSize);
                }
                if(mFocusPoint.x == i && mFocusPoint.y == row){
                        drawSoftKey(canvas, softKey, keyXMargin, keyYMargin,true);
                } else {
                        drawSoftKey(canvas, softKey, keyXMargin, keyYMargin,false);
                }
            }
        }

        if (mDimSkb) {
            mPaint.setColor(0xa0000000);
            canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);
        }

        mDirtyRect.setEmpty();
    }

    private void drawSoftKey(Canvas canvas, SoftKey softKey, int keyXMargin,
                             int keyYMargin, boolean focusEnable) {
        Drawable bg;
        int textColor;
        if (mKeyPressed && softKey == mSoftKeyDown) {
            bg = softKey.getKeyHlBg();
            textColor = softKey.getColorHl();
        } else {
            bg = softKey.getKeyBg();
            textColor = softKey.getColor();
        }
        mPaint.setColor(Color.WHITE); //focus line color 
        if(focusEnable){
                canvas.drawRoundRect(new RectF(softKey.mLeft+30, softKey.mTop+20, softKey.mRight-30, softKey.mBottom-30),150,150, mPaint);
        }
        if (null != bg) {
            bg.setBounds(softKey.mLeft + keyXMargin, softKey.mTop + keyYMargin,
                    softKey.mRight - keyXMargin, softKey.mBottom - keyYMargin);
            bg.draw(canvas);
        }

        String keyLabel = softKey.getKeyLabel();
        Drawable keyIcon = softKey.getKeyIcon();
        if (null != keyIcon) {
            Drawable icon = keyIcon;
            int marginLeft = (softKey.width() - icon.getIntrinsicWidth()) / 2;
            int marginRight = softKey.width() - icon.getIntrinsicWidth()
                    - marginLeft;
            int marginTop = (softKey.height() - icon.getIntrinsicHeight()) / 2;
            int marginBottom = softKey.height() - icon.getIntrinsicHeight()
                    - marginTop;
            icon.setBounds(softKey.mLeft + marginLeft,
                    softKey.mTop + marginTop, softKey.mRight - marginRight,
                    softKey.mBottom - marginBottom);
            icon.draw(canvas);
        } else if (null != keyLabel) {
            mPaint.setColor(textColor);
            float x = softKey.mLeft
                    + (softKey.width() - mPaint.measureText(keyLabel)) / 2.0f;
            int fontHeight = mFmi.bottom - mFmi.top;
            float marginY = (softKey.height() - fontHeight) / 2.0f;
            float y = softKey.mTop + marginY - mFmi.top + mFmi.bottom / 1.5f;
            canvas.drawText(keyLabel, x, y + 1, mPaint);
        }
    }

    public boolean receiveKeyDown(int keyCode, KeyEvent event) {
                    Log.d(TAG, "onKeyDown be called," + keyCode + "\n" + event.toString());
                    onDPADKey(keyCode);
                    return false;
    }

    public boolean receiveKeyUp(int keyCode, KeyEvent event) {
                    Log.d(TAG, "onKeyUp be called");
                    //onDPADKey(keyCode);
                    return false;
    }
    private void setSoftKeyHl(SoftKey softkey) {
        focusKey = softkey;
        invalidate();
    }

    private void onSkbViewFocus(boolean focus) {
        mSkbViewFocus = focus;
    }
}
