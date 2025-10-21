package com.lythe.media.chats.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

public class UnreadBadgeView extends AppCompatTextView {
    private static final String TAG = "UnreadBadgeView";

    private Paint paint;
    private Path path;
    private PointF initPoint;
    private PointF dragPoint;
    private float radius = 20f;
    private float currentRadius;
    private boolean isDragging = false;
    private float maxDistance = 200f;
    private boolean isDisconnected = false;
    private OnDragDismissListener listener;
    private boolean isInitialized = false;
    private float originalRadius;
    private int[] location = new int[2];
    private float touchDownX, touchDownY;
    private boolean isTouchInProgress = false;
    private float dragThreshold = 10f; // 拖动阈值，避免轻微移动就触发拖动

    public UnreadBadgeView(Context context) {
        super(context);
        init();
    }

    public UnreadBadgeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public UnreadBadgeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        path = new Path();
        originalRadius = radius;
        currentRadius = radius;

        setGravity(android.view.Gravity.CENTER);

        // 确保视图可以接收触摸事件
        setClickable(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!isInitialized) {
            int centerX = w / 2;
            int centerY = h / 2;
            initPoint = new PointF(centerX, centerY);
            dragPoint = new PointF(initPoint.x, initPoint.y);
            isInitialized = true;

            // 获取视图在屏幕上的位置
            getLocationOnScreen(location);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isInitialized) {
            return super.onTouchEvent(event);
        }

        float x = event.getRawX() - location[0];
        float y = event.getRawY() - location[1];

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouchInProgress = true;
                touchDownX = x;
                touchDownY = y;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!isTouchInProgress) return true;

                float dx = Math.abs(x - touchDownX);
                float dy = Math.abs(y - touchDownY);

                // 检查是否达到拖动阈值
                if (!isDragging && (dx > dragThreshold || dy > dragThreshold)) {
                    // 开始拖动
                    isDragging = true;
                    setVisibility(INVISIBLE);
                }

                if (isDragging) {
                    dragPoint.set(x, y);
                    float distance = getDistance(initPoint, dragPoint);

                    if (distance > 0 && distance < maxDistance) {
                        float scale = 1 - distance / maxDistance;
                        currentRadius = radius * scale;
                    } else if (distance >= maxDistance) {
                        isDisconnected = true;
                        currentRadius = 0;
                    }
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTouchInProgress = false;

                if (isDragging) {
                    isDragging = false;
                    float distanceUp = getDistance(initPoint, dragPoint);

                    if (distanceUp < maxDistance && !isDisconnected) {
                        bounceBack();
                    } else {
                        disappearAnimation();
                        if (listener != null) {
                            listener.onDismiss();
                        }
                    }
                } else {
                    // 如果不是拖动，执行默认的点击行为
                    performClick();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        // 处理点击事件
        return super.performClick();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (isDragging) {
            // 绘制初始圆
            if (currentRadius > 0) {
                canvas.drawCircle(initPoint.x, initPoint.y, currentRadius, paint);

                // 绘制连接路径
                if (!isDisconnected) {
                    drawConnectionPath(canvas);
                }
            }

            // 绘制拖拽圆
            canvas.drawCircle(dragPoint.x, dragPoint.y, radius, paint);

            // 绘制文本
            String text = getText().toString();
            Paint textPaint = getPaint();
            textPaint.setColor(getCurrentTextColor());
            textPaint.setTextAlign(Paint.Align.CENTER);
            float textY = dragPoint.y - ((textPaint.descent() + textPaint.ascent()) / 2);
            canvas.drawText(text, dragPoint.x, textY, textPaint);
        } else {
            super.onDraw(canvas);
        }
    }

    private void drawConnectionPath(Canvas canvas) {
        float dx = dragPoint.x - initPoint.x;
        float dy = dragPoint.y - initPoint.y;
        float distance = getDistance(initPoint, dragPoint);

        float angle = (float) Math.atan2(dy, dx);

        float initX1 = initPoint.x + (float) Math.cos(angle) * currentRadius;
        float initY1 = initPoint.y + (float) Math.sin(angle) * currentRadius;
        float initX2 = initPoint.x - (float) Math.cos(angle) * currentRadius;
        float initY2 = initPoint.y - (float) Math.sin(angle) * currentRadius;

        float dragX1 = dragPoint.x + (float) Math.cos(angle) * radius;
        float dragY1 = dragPoint.y + (float) Math.sin(angle) * radius;
        float dragX2 = dragPoint.x - (float) Math.cos(angle) * radius;
        float dragY2 = dragPoint.y - (float) Math.sin(angle) * radius;

        path.reset();
        path.moveTo(initX1, initY1);
        path.quadTo((initPoint.x + dragPoint.x) / 2,
                (initPoint.y + dragPoint.y) / 2,
                dragX1, dragY1);
        path.lineTo(dragX2, dragY2);
        path.quadTo((initPoint.x + dragPoint.x) / 2,
                (initPoint.y + dragPoint.y) / 2,
                initX2, initY2);
        path.close();

        canvas.drawPath(path, paint);
    }

    private float getDistance(PointF p1, PointF p2) {
        float dx = p1.x - p2.x;
        float dy = p1.y - p2.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void bounceBack() {
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(300);
        animator.setInterpolator(new OvershootInterpolator(3));

        final PointF startPoint = new PointF(dragPoint.x, dragPoint.y);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float fraction = animation.getAnimatedFraction();
                dragPoint.x = startPoint.x + (initPoint.x - startPoint.x) * fraction;
                dragPoint.y = startPoint.y + (initPoint.y - startPoint.y) * fraction;
                currentRadius = radius * fraction;
                invalidate();
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isDragging = false;
                isDisconnected = false;
                setVisibility(VISIBLE);
                invalidate();
            }
        });

        animator.start();
    }

    private void disappearAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(1, 0);
        animator.setDuration(300);

        final float startRadius = radius;

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float fraction = (float) animation.getAnimatedValue();
                radius = startRadius * fraction;
                invalidate();
            }
        });

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setVisibility(GONE);
                isDisconnected = false;
                radius = originalRadius;
                currentRadius = radius;
                invalidate();
            }
        });

        animator.start();
    }

    public void setUnreadCount(int count) {
        if (count <= 0) {
            setVisibility(GONE);
        } else {
            setVisibility(VISIBLE);
            setText(String.valueOf(count));
            if (count > 99) {
                setText("99+");
            }
        }
    }

    public void setOnDragDismissListener(OnDragDismissListener listener) {
        this.listener = listener;
    }

    public interface OnDragDismissListener {
        void onDismiss();
    }
}