package edu.stanford.cs108.bunnyworld;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class DrawActivity extends Activity {
    static Context context;

    static Paint paint;
    static Paint white_fill;

    static RadioGroup radio_group;
    static RadioButton pen_btn, eraser_btn;

    SeekBar r_seekbar, g_seekbar, b_seekbar, width_seekbar;
    TextView rgb_tv, width_tv;
    View color_view;

    private DrawCanvas draw_canvas;
    private Activity draw_activity;

    final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 201;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_draw);

        context = getApplicationContext();
        draw_canvas = findViewById(R.id.draw_canvas);
        draw_activity = this;
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
        init();
    }


    private void init() {
        color_view = findViewById(R.id.colorView);
        rgb_tv = findViewById(R.id.rgbText);
        width_tv = findViewById(R.id.width_tv);

        radio_group = findViewById(R.id.radio_btns);
        pen_btn = findViewById(R.id.pen_radio_btn);
        eraser_btn = findViewById(R.id.eraser_radio_btn);

        white_fill = new Paint();
        white_fill.setColor(Color.WHITE);
        white_fill.setStyle(Paint.Style.FILL);

        paint = new Paint();
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(15);

        r_seekbar = findViewById(R.id.redBar);
        g_seekbar = findViewById(R.id.greenBar);
        b_seekbar = findViewById(R.id.blueBar);
        width_seekbar = findViewById(R.id.widthBar);

        // set seekBar listener
        r_seekbar.setOnSeekBarChangeListener(new colorSeekBarListener());
        g_seekbar.setOnSeekBarChangeListener(new colorSeekBarListener());
        b_seekbar.setOnSeekBarChangeListener(new colorSeekBarListener());
        width_seekbar.setOnSeekBarChangeListener(new widthSeekBarListener());
    }

    public void saveDraw(View view) {
        final Bitmap bitmap = draw_canvas.toBitmap();

        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("SAVE DRAWING");
        adb.setMessage("Enter Drawing Name");

        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);

        String timeStamp = new SimpleDateFormat("yyMMdd_HHmmss").format(new Date());
        input.setHint("drawing_" + timeStamp);

        adb.setView(input);
        adb.setIcon(R.mipmap.save);

        adb.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String saved_drawing_name = input.getText().toString();
                // if not enter any name, we use the default name
                if (saved_drawing_name.isEmpty()) {
                    saved_drawing_name = input.getHint().toString();
                }
                // check the valid name first
                if (saved_drawing_name.matches("[a-zA-Z\\d][\\w#@.]{0,127}$")) {
                    Uri image_uri = saveBitmapHelper(bitmap, saved_drawing_name);
                    Toast.makeText(getApplicationContext(), "Saved " + saved_drawing_name, Toast.LENGTH_SHORT).show();

                    EditActivity.addShapeIntoCanvasHelper(image_uri.toString(), saved_drawing_name);

                    draw_canvas.clearStates();

                    draw_activity.finish();
                } else {
                    Toast.makeText(getApplicationContext(),
                            "Please enter the valid name!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "Save Cancelled", Toast.LENGTH_SHORT).show();
            }
        });

        adb.show();

    }

    public Uri saveBitmapHelper(Bitmap bitmap, String filename) {
        ContentResolver resolver = getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        OutputStream outs;
        try {
            outs = resolver.openOutputStream(Objects.requireNonNull(imageUri));
            bitmap.compress(Bitmap.CompressFormat.PNG, 0, outs);
            Objects.requireNonNull(outs).close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageUri;

    }

    public void clearDraw(View view) {
        draw_canvas.clearStates();

    }

    public void undoDraw(View view) {
        draw_canvas.undo();
    }

    public void redoDraw(View view) {
        draw_canvas.redo();
    }

    private class colorSeekBarListener implements SeekBar.OnSeekBarChangeListener {

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int red = r_seekbar.getProgress();
            int green = g_seekbar.getProgress();
            int blue = b_seekbar.getProgress();

            String cur_rgb = "RGB:  (" + red + ", " + green + ", " + blue + ")";
            rgb_tv.setText(cur_rgb);
            color_view.setBackgroundColor(Color.rgb(red, green, blue));

            paint.setColor(Color.rgb(red, green, blue));
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
        }

    }

    private class widthSeekBarListener implements SeekBar.OnSeekBarChangeListener {

        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int width = width_seekbar.getProgress();

            String cur_width = "Width: " + width;
            width_tv.setText(cur_width);

            paint.setStrokeWidth(width);
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
        }

    }
}
