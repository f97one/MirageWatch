/*
 * Copyright (C) 2014 The Android Open Source Project
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

package net.formula97.miragewatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class MyWatchFace extends CanvasWatchFaceService {

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    /**
     * assets/ のフォントファイルをTypefaceに設定する。
     *
     * @return 設定するフォントファイルのTypeface
     */
    protected Typeface getTypefaceFromAssets() {
        return Typeface.createFromAsset(getAssets(), "formation_sans_regular.ttf");
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFace.Engine> mWeakReference;

        public EngineHandler(MyWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        final Handler mUpdateTimeHandler = new EngineHandler(this);
        final int sHandTypeHour = 1;
        final int sHandTypeMinutes = 2;
        final int sHandTypeSeconds = 3;
        boolean mRegisteredTimeZoneReceiver = false;
        Paint mBackgroundPaint;
        Paint mClockfacePaint;
        Paint mTextPaint;
        Paint mRedClockMarkPaint;
        Paint mYellowClockMarkPaint;
        /**
         * 時刻部分のPaint(時間)
         */
        Paint mHourPaint;
        /**
         * 時刻部分のPaint(分)
         */
        Paint mMinutesPaint;
        /**
         * 時刻部分のPaint(セパレータ)
         */
        Paint mSeparatorPaint;
        /**
         * 日付部分のPaint
         */
        Paint mDatePaint;
        boolean mAmbient;
        Time mTime;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        int mTapCount;
        float mXOffset;
        float mYOffset;
        float mDateYOffset;
        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .setStatusBarGravity(Gravity.RIGHT | Gravity.TOP)
                    .build());
            Resources resources = MyWatchFace.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background));

            mClockfacePaint = new Paint();
            mClockfacePaint.setColor(resources.getColor(R.color.clock_face_circle));
            mClockfacePaint.setStyle(Paint.Style.STROKE);
            mClockfacePaint.setStrokeWidth(2.0f);
            mClockfacePaint.setAntiAlias(true);

            int digitalTextColor = resources.getColor(R.color.digital_text);

            mTextPaint = createTextPaint(digitalTextColor);

            mHourPaint = createTextPaint(digitalTextColor, Paint.Align.RIGHT);
            mMinutesPaint = createTextPaint(digitalTextColor, Paint.Align.LEFT);
            mSeparatorPaint = createTextPaint(digitalTextColor, Paint.Align.CENTER);

            mDatePaint = createTextPaint(digitalTextColor, Paint.Align.CENTER);

            mRedClockMarkPaint = createClockMarkPaint(Color.RED, Paint.Style.FILL);
            mYellowClockMarkPaint = createClockMarkPaint(Color.YELLOW, Paint.Style.FILL);

            mTime = new Time();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor) {
            return createTextPaint(textColor, Paint.Align.LEFT);
        }

        private Paint createTextPaint(int textColor, Paint.Align textAlignment) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(getTypefaceFromAssets());
            paint.setAntiAlias(true);
            paint.setTextAlign(textAlignment);
            return paint;
        }

        private Paint createClockMarkPaint(int color, Paint.Style paintStyle) {
            Paint paint = new Paint();
            paint.setColor(color);
            paint.setAntiAlias(true);
            paint.setStyle(paintStyle);

            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources res = MyWatchFace.this.getResources();
            boolean isRound = insets.isRound();

            // 時間部分のサイズ
            float textSize = res.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            // 日付部分のサイズ
            float dateSize = res.getDimension(isRound ? R.dimen.date_text_size_round : R.dimen.date_text_size_sq);

            mXOffset = res.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            mDateYOffset = res.getDimension(R.dimen.date_x_offset_spacing) + mYOffset + dateSize;

            mTextPaint.setTextSize(textSize);

            mHourPaint.setTextSize(textSize);
            mMinutesPaint.setTextSize(textSize);
            mSeparatorPaint.setTextSize(textSize);

            mDatePaint.setTextSize(dateSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mTextPaint.setAntiAlias(!inAmbientMode);
                }
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Resources resources = MyWatchFace.this.getResources();
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    mTapCount++;
                    mBackgroundPaint.setColor(resources.getColor(mTapCount % 2 == 0 ?
                            R.color.background : R.color.background2));
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // 現在時刻にアップデートする
            mTime.setToNow();

            drawDigitalFace(canvas, bounds, isInAmbientMode());
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        private HandRotation getRotation(Time time) {
            HandRotation rotation = new HandRotation();

            float minRotationUnit = 360 / 60;
            int min = time.minute;
            float hourH = time.hour < 12 ?
                    (float) time.hour + time.minute / 60f :
                    (float) (time.hour - 12) + time.minute / 60f;

            rotation.setSecondHand(time.second * minRotationUnit);
            rotation.setMinuteHand(min * minRotationUnit);
            rotation.setHourHand(hourH * 5f * minRotationUnit);

            String tag = this.getClass().getSimpleName();
            Log.d(tag, "時間回転角 = " + String.valueOf(rotation.getHourHand()));
            Log.d(tag, "分回転角 = " + String.valueOf(rotation.getMinuteHand()));
            Log.d(tag, "秒回転角 = " + String.valueOf(rotation.getSecondHand()));

            return rotation;
        }

        /**
         * デジタル表示のWatchfaceを描画する。
         *
         * @param canvas 描画先のCanvas
         * @param bounds 描画エリア
         * @param isAmbient Ambientモード時はtrue、そうでない時はfalse
         */
        private void drawDigitalFace(Canvas canvas, Rect bounds, boolean isAmbient) {
            // Ambientの場合は、配色をモノトーンにする
            int hourHandColor;
            int minHandColor;
            if (isAmbient) {
                canvas.drawColor(Color.BLACK);

                mRedClockMarkPaint.setColor(Color.GRAY);
                mYellowClockMarkPaint.setColor(Color.GRAY);

                hourHandColor = Color.GRAY;
                minHandColor = Color.GRAY;
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);

                mRedClockMarkPaint.setColor(Color.RED);
                mYellowClockMarkPaint.setColor(Color.YELLOW);

                hourHandColor = Color.CYAN;
                minHandColor = Color.MAGENTA;
            }
            // 0. 中心の計算
            float centerX = bounds.exactCenterX();
            float centerY = bounds.exactCenterY();
            int halfX = bounds.centerX();
            int halfY = bounds.centerY();

            HandRotation rotation = getRotation(mTime);

            // 短針の描画
            Bitmap hourHandBase = createHand(sHandTypeHour, bounds, hourHandColor);
            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.rotate(rotation.getHourHand(), halfX, halfY);
            canvas.drawBitmap(hourHandBase, 0, 0, null);

            canvas.restore();

            // 長針の描画
            Bitmap minHandBase = createHand(sHandTypeMinutes, bounds, minHandColor);
            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.rotate(rotation.getMinuteHand(), halfX, halfY);
            canvas.drawBitmap(minHandBase, 0, 0, null);
            canvas.restore();

            // 描画座標の計算\
            Resources res = MyWatchFace.this.getResources();
            // 時間、分を中心からずらすオフセット
            float offset = mSeparatorPaint.getTextSize() / 4f;

            // 1. セパレータ
            // TextAlignは中央なので、基準点は中央だが、文字サイズだけ上に上げる
            float separatorX = centerX;
            float separatorY = centerY;

            // 2. 時刻
            // TextAlignを右寄せにしているので、基準点は右肩になる
            float hourX = centerX - offset;
            float hourY = separatorY;

            // 3. 分
            // TextAlignは左寄せなので、基準点は左肩
            float minutesX = centerX + offset;
            float minutesY = separatorY;

            // 4. 日付
            float dateX = centerX;
            float dateY = separatorY + mDatePaint.getTextSize() + res.getDimension(R.dimen.date_x_offset_spacing);

            mTime.setToNow();
            String hh = String.format(Locale.US, "%02d", mTime.hour);
            String mm = String.format(Locale.US, "%02d", mTime.minute);

            // セパレータは、秒が偶数、またはAmbientモードのときだけ描画
            if (mTime.second % 2 == 0 || isAmbient) {
                canvas.drawText(":", separatorX, separatorY, mSeparatorPaint);
            }

            // 時間を描画
            canvas.drawText(hh, hourX, hourY, mHourPaint);
            canvas.drawText(mm, minutesX, minutesY, mMinutesPaint);

            // 日付を描画
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd (EEE)", Locale.US);
            String dateTxt = sdf.format(calendar.getTime());

            canvas.drawText(dateTxt, dateX, dateY, mDatePaint);

            // アナログ部分の描画
            // 12時
            Path twelvePath = new Path();
            float markHeight = res.getDimension(R.dimen.inner_clock_face_radius_offset);
            float markBase = markHeight / 3f;

            twelvePath.moveTo(centerX, markHeight);
            twelvePath.lineTo(centerX + markBase, 0);
            twelvePath.lineTo(centerX - markBase, 0);
            twelvePath.close();
            canvas.drawPath(twelvePath, mRedClockMarkPaint);

            // ３時
            Path threePath = new Path();
            threePath.moveTo(bounds.width() - markHeight, centerY);
            threePath.lineTo(bounds.width(), centerY + markBase);
            threePath.lineTo(bounds.width(), centerY - markBase);
            threePath.close();
            canvas.drawPath(threePath, mYellowClockMarkPaint);

            // ６時
            Path sixPath = new Path();
            sixPath.moveTo(centerX, bounds.height() - markHeight);
            sixPath.lineTo(centerX - markBase, bounds.height());
            sixPath.lineTo(centerX + markBase, bounds.height());
            sixPath.close();
            canvas.drawPath(sixPath, mYellowClockMarkPaint);

            // ９時
            Path ninePath = new Path();
            ninePath.moveTo(markHeight, centerY);
            ninePath.lineTo(0, centerY - markBase);
            ninePath.lineTo(0, centerY + markBase);
            ninePath.close();
            canvas.drawPath(ninePath, mYellowClockMarkPaint);
        }

        private Bitmap createHand(int handType, Rect bounds, int color) {
            Resources res = MyWatchFace.this.getResources();
            float handWidth = res.getDimension(R.dimen.inner_clock_face_radius_offset);

            // 元のBitmapを作成
            float offsetX = 0f;
            float offsetY = 0f;
            switch (handType) {
                case sHandTypeHour:
                    offsetX = handWidth * 2f;
                    offsetY = handWidth * 2f;
                    break;
                case sHandTypeSeconds:
                case sHandTypeMinutes:
                    offsetX = handWidth;
                    offsetY = handWidth;
                    break;
            }

            Bitmap baseHand = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(baseHand);

            Paint paint = getHandPaint(color);

            Path path = new Path();

            path.moveTo(bounds.width() / 2, offsetY);
            path.lineTo(bounds.width() / 2 - handWidth / 2, offsetY + handWidth);
            path.lineTo(bounds.width() / 2 + handWidth / 2, offsetY + handWidth);
            path.close();

            canvas.drawPath(path, paint);

            return baseHand;
        }

        @NonNull
        private Paint getHandPaint(int color) {
            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setAlpha(0);
            paint.setColor(color);
            paint.setStyle(Paint.Style.FILL);
            return paint;
        }

    }


}
