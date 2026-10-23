package com.khang.photovault;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/** Vùng chọn cắt ảnh: kéo bên trong để di chuyển, kéo góc để đổi kích thước,
 * hoặc kéo bên ngoài khung để tạo vùng cắt mới. */
public class CropImageView extends View {
    private Bitmap bitmap;
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint shadePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF imageRect = new RectF();
    private final RectF cropRect = new RectF();

    private float downX, downY;
    private float startLeft, startTop, startRight, startBottom;
    private float handleRadius;
    private int dragMode = NONE;
    private int resizeCorner = -1;
    private boolean cropInitialized = false;

    private static final int NONE = 0;
    private static final int MOVE = 1;
    private static final int RESIZE = 2;
    private static final int DRAW_NEW = 3;
    private static final int TOP_LEFT = 0;
    private static final int TOP_RIGHT = 1;
    private static final int BOTTOM_LEFT = 2;
    private static final int BOTTOM_RIGHT = 3;

    public CropImageView(Context context) { super(context); init(); }
    public CropImageView(Context context, AttributeSet attrs) { super(context, attrs); init(); }
    public CropImageView(Context context, AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }

    private void init() {
        float density = getResources().getDisplayMetrics().density;
        handleRadius = 9f * density;
        shadePaint.setColor(0x99000000);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(2f * density);
        handlePaint.setColor(Color.WHITE);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.cropInitialized = false;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmap == null || bitmap.isRecycled()) return;

        float availableWidth = getWidth() - getPaddingLeft() - getPaddingRight();
        float availableHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        if (availableWidth <= 0 || availableHeight <= 0) return;
        float scale = Math.min(availableWidth / bitmap.getWidth(), availableHeight / bitmap.getHeight());
        float drawnWidth = bitmap.getWidth() * scale;
        float drawnHeight = bitmap.getHeight() * scale;
        float left = getPaddingLeft() + (availableWidth - drawnWidth) / 2f;
        float top = getPaddingTop() + (availableHeight - drawnHeight) / 2f;
        imageRect.set(left, top, left + drawnWidth, top + drawnHeight);
        canvas.drawBitmap(bitmap, null, imageRect, bitmapPaint);

        if (!cropInitialized) {
            float insetX = imageRect.width() * 0.12f;
            float insetY = imageRect.height() * 0.12f;
            cropRect.set(imageRect.left + insetX, imageRect.top + insetY,
                    imageRect.right - insetX, imageRect.bottom - insetY);
            cropInitialized = true;
        }
        cropRect.intersect(imageRect);

        // Tối màu phần nằm ngoài vùng chọn.
        canvas.drawRect(imageRect.left, imageRect.top, imageRect.right, cropRect.top, shadePaint);
        canvas.drawRect(imageRect.left, cropRect.bottom, imageRect.right, imageRect.bottom, shadePaint);
        canvas.drawRect(imageRect.left, cropRect.top, cropRect.left, cropRect.bottom, shadePaint);
        canvas.drawRect(cropRect.right, cropRect.top, imageRect.right, cropRect.bottom, shadePaint);
        canvas.drawRect(cropRect, borderPaint);

        // Tay nắm ở bốn góc để người dùng dễ chỉnh khung cắt.
        canvas.drawCircle(cropRect.left, cropRect.top, handleRadius, handlePaint);
        canvas.drawCircle(cropRect.right, cropRect.top, handleRadius, handlePaint);
        canvas.drawCircle(cropRect.left, cropRect.bottom, handleRadius, handlePaint);
        canvas.drawCircle(cropRect.right, cropRect.bottom, handleRadius, handlePaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (bitmap == null || !cropInitialized) return false;
        float x = event.getX();
        float y = event.getY();
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!imageRect.contains(x, y)) return false;
                downX = x;
                downY = y;
                startLeft = cropRect.left;
                startTop = cropRect.top;
                startRight = cropRect.right;
                startBottom = cropRect.bottom;
                resizeCorner = hitCorner(x, y);
                if (resizeCorner != -1) {
                    dragMode = RESIZE;
                } else if (cropRect.contains(x, y)) {
                    dragMode = MOVE;
                } else {
                    dragMode = DRAW_NEW;
                    cropRect.set(x, y, x, y);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = x - downX;
                float dy = y - downY;
                if (dragMode == MOVE) {
                    float width = startRight - startLeft;
                    float height = startBottom - startTop;
                    float left = clamp(startLeft + dx, imageRect.left, imageRect.right - width);
                    float top = clamp(startTop + dy, imageRect.top, imageRect.bottom - height);
                    cropRect.set(left, top, left + width, top + height);
                } else if (dragMode == RESIZE) {
                    resizeTo(x, y);
                } else if (dragMode == DRAW_NEW) {
                    cropRect.set(clamp(downX, imageRect.left, imageRect.right),
                            clamp(downY, imageRect.top, imageRect.bottom),
                            clamp(x, imageRect.left, imageRect.right),
                            clamp(y, imageRect.top, imageRect.bottom));
                    normalizeRect(cropRect);
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (dragMode == DRAW_NEW && (cropRect.width() < dp(28) || cropRect.height() < dp(28))) {
                    cropRect.set(imageRect.left + imageRect.width() * 0.12f,
                            imageRect.top + imageRect.height() * 0.12f,
                            imageRect.right - imageRect.width() * 0.12f,
                            imageRect.bottom - imageRect.height() * 0.12f);
                }
                dragMode = NONE;
                invalidate();
                return true;
        }
        return true;
    }

    private int hitCorner(float x, float y) {
        float hit = handleRadius * 2.5f;
        if (distance(x, y, cropRect.left, cropRect.top) <= hit) return TOP_LEFT;
        if (distance(x, y, cropRect.right, cropRect.top) <= hit) return TOP_RIGHT;
        if (distance(x, y, cropRect.left, cropRect.bottom) <= hit) return BOTTOM_LEFT;
        if (distance(x, y, cropRect.right, cropRect.bottom) <= hit) return BOTTOM_RIGHT;
        return -1;
    }

    private void resizeTo(float x, float y) {
        float minW = dp(40), minH = dp(40);
        float left = cropRect.left, top = cropRect.top, right = cropRect.right, bottom = cropRect.bottom;
        switch (resizeCorner) {
            case TOP_LEFT:
                left = clamp(x, imageRect.left, right - minW);
                top = clamp(y, imageRect.top, bottom - minH);
                break;
            case TOP_RIGHT:
                right = clamp(x, left + minW, imageRect.right);
                top = clamp(y, imageRect.top, bottom - minH);
                break;
            case BOTTOM_LEFT:
                left = clamp(x, imageRect.left, right - minW);
                bottom = clamp(y, top + minH, imageRect.bottom);
                break;
            case BOTTOM_RIGHT:
                right = clamp(x, left + minW, imageRect.right);
                bottom = clamp(y, top + minH, imageRect.bottom);
                break;
        }
        cropRect.set(left, top, right, bottom);
    }

    public Bitmap getCroppedBitmap() {
        if (bitmap == null || !cropInitialized || cropRect.width() < 1 || cropRect.height() < 1
                || imageRect.width() <= 0 || imageRect.height() <= 0) return null;
        float scaleX = bitmap.getWidth() / imageRect.width();
        float scaleY = bitmap.getHeight() / imageRect.height();
        int left = Math.max(0, Math.min(bitmap.getWidth() - 1,
                Math.round((cropRect.left - imageRect.left) * scaleX)));
        int top = Math.max(0, Math.min(bitmap.getHeight() - 1,
                Math.round((cropRect.top - imageRect.top) * scaleY)));
        int right = Math.max(left + 1, Math.min(bitmap.getWidth(),
                Math.round((cropRect.right - imageRect.left) * scaleX)));
        int bottom = Math.max(top + 1, Math.min(bitmap.getHeight(),
                Math.round((cropRect.bottom - imageRect.top) * scaleY)));
        return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
    }

    private void normalizeRect(RectF rect) {
        float l = Math.min(rect.left, rect.right);
        float r = Math.max(rect.left, rect.right);
        float t = Math.min(rect.top, rect.bottom);
        float b = Math.max(rect.top, rect.bottom);
        rect.set(l, t, r, b);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(value, max));
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
}
