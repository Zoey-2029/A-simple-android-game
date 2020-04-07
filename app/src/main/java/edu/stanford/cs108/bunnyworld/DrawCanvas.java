package edu.stanford.cs108.bunnyworld;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.Nullable;


public class DrawCanvas extends View {
    private Bitmap bitmap_bg;
    private Canvas my_canvas;
    private Frame cur_frame = new Frame();
    private Path cur_path = new Path();
    private Paint bitmap_paint = new Paint();
    private float selected_x, selected_y;

    private boolean reset = false;

    private ArrayList<Frame> frames = new ArrayList<>();
    private ArrayList<Frame> frames_backup = new ArrayList<>();

    public DrawCanvas(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        bitmap_bg = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        my_canvas = new Canvas(bitmap_bg);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawBitmap(bitmap_bg, 0f, 0f, bitmap_paint);
        cur_frame.draw(my_canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // set cur_paint is regular paint or is the eraser
        Paint cur_paint = new Paint(DrawActivity.paint);
        if (DrawActivity.eraser_btn.isChecked()) {
            cur_paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        } else {
            cur_paint.setXfermode(null);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                selected_x = event.getX();
                selected_y = event.getY();

                cur_path.reset();
                cur_path.moveTo(selected_x, selected_y);

                break;
            case MotionEvent.ACTION_MOVE:
                float new_x = event.getX();
                float new_y = event.getY();

                cur_path.moveTo(selected_x, selected_y);
                cur_path.lineTo(new_x, new_y);

                if (cur_frame.isEmpty() || reset) {
                    cur_frame.addStroke(new Stroke(cur_path, cur_paint));
                    reset = false;
                } else {
                    cur_frame.updateLastStroke(new Stroke(cur_path, cur_paint));
                }

                selected_x = new_x;
                selected_y = new_y;

                break;
            case MotionEvent.ACTION_UP:
                cur_path = new Path();
                reset = true;
                frames.add(new Frame(cur_frame));

                break;
        }

        invalidate();
        return true;
    }


    public void clearStates() {
        frames.clear();
        frames_backup.clear();
        cur_frame.clear();

        bitmap_bg = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        my_canvas = new Canvas(bitmap_bg);

        invalidate();
    }

    public void undo() {
        if (frames.size() > 0) {
            frames_backup.add(frames.remove(frames.size() - 1));
            if (frames.size() == 0) {
                cur_frame = new Frame();
            } else {
                cur_frame = frames.get(frames.size() - 1);
            }
            initBitmapBackground();
            invalidate();
        } else {
            Toast.makeText(DrawActivity.context, "Can't undo anymore!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void redo() {
        if (frames_backup.size() > 0) {
            frames.add(frames_backup.remove(frames_backup.size() - 1));

            cur_frame = frames.get(frames.size() - 1);

            initBitmapBackground();
            invalidate();
        } else {
            Toast.makeText(DrawActivity.context, "Can't redo anymore!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public Bitmap toBitmap() {
        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        draw(canvas);

        return bitmap;
    }

    private void initBitmapBackground() {
        bitmap_bg = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        my_canvas = new Canvas(bitmap_bg);
    }

    class Frame {
        private ArrayList<Stroke> strokes;

        Frame() {
            strokes = new ArrayList<>();
        }

        Frame(Frame other) {
            strokes = new ArrayList<>();
            for (Stroke stroke : other.strokes) {
                strokes.add(new Stroke(stroke));
            }
        }

        void draw(Canvas canvas) {
            for (Stroke stroke : strokes) {
                stroke.draw(canvas);
            }
        }

        void clear() {
            strokes = new ArrayList<>();
        }

        boolean isEmpty() {
            return this.strokes.isEmpty();
        }

        void updateLastStroke(Stroke s) {
            strokes.set(strokes.size() - 1, s);
        }

        void addStroke(Stroke s) {
            strokes.add(s);
        }
    }

    class Stroke {
        Path path;
        Paint paint;

        Stroke(Path path, Paint paint) {
            this.path = new Path(path);
            this.paint = new Paint(paint);
        }

        Stroke(Stroke other) {
            this.path = new Path(other.path);
            this.paint = new Paint(other.paint);
        }

        void draw(Canvas canvas) {
            canvas.drawPath(path, paint);
        }
    }

}