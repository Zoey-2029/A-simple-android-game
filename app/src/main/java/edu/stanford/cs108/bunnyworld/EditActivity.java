package edu.stanford.cs108.bunnyworld;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


public class EditActivity extends AppCompatActivity {
    SQLiteDatabase db;
    GameCanvas game_canvas;

    // Instantiate current game from GameManager
    private Game editing_game;
    // assigned in onCreate
    private Page editing_page = null;
    static private Activity edit_activity;

    static TextView display_page_name;
    static TextView display_shape_name;

    static ArrayList<String> image_names = new ArrayList<>(Arrays.asList(
            "add_text", "image_add", "image_edit", "fire", "duck", "carrot1", "carrot2", "bunny", "angry_bunny", "door"));
    private ArrayList<Integer> image_ids;


    private static final int GALLERY_REQUEST = 1;
    private static final int CREATE_FILE = 14;
    private static final int DRAW_PNG = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        edit_activity = this;

        image_ids = getImageIDs();

        editing_game = GameManager.getCurGame();
        editing_game.setEditing(true);

        GameManager.setActivityRunsGameCanvas(this);
        game_canvas = GameManager.getGameCanvas();

        editing_page = editing_game.getCurPage();

        if (editing_game.isEditing()) {
            display_page_name = findViewById(R.id.display_page_name);
            display_page_name.setText(editing_page.getName());

            display_shape_name = findViewById(R.id.display_shape_name);
        }

        editing_page.initNextShapeXY(game_canvas.getWidth(), game_canvas.getHeight(), 0.1f, 0.1f);

        Gallery gallery = findViewById(R.id.drawable_gallery);
        gallery.setAdapter(new ImageAdapter(this));
        gallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {

                int img_id = image_ids.get(position);
                String img_name = getResources().getResourceEntryName(img_id);

                if (img_name.equals("image_add")) {
                    Intent photoPickerIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, GALLERY_REQUEST);
                } else if (img_name.equals("image_edit")) {
                    Intent drawPNGIntent = new Intent(edit_activity.getApplicationContext(), DrawActivity.class);
                    startActivityForResult(drawPNGIntent, DRAW_PNG);
                } else {
                    addShapeIntoCanvasHelper(img_name, img_name);
                }

            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK)
            if (requestCode == GALLERY_REQUEST) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri == null) {
                    return;
                }
                String referred_uri = selectedImageUri.toString();
                String image_name = getFileNameFromUri(selectedImageUri);

                addShapeIntoCanvasHelper(referred_uri, image_name);
            } else if (requestCode == CREATE_FILE) {
                Uri createdFileUri = data.getData();
                if (createdFileUri == null) {
                    return;
                }
                String json_str = GameManager.export_json(db, editing_game);
                Log.e("Exported Json", json_str);
                writeToFileHelper(data.getData(), json_str);
                edit_activity.finish();
            } else if (requestCode == DRAW_PNG) {

            }
    }

    private void writeToFileHelper(@NonNull Uri uri, @NonNull String string) {
        OutputStream outputStream;
        try {
            outputStream = getContentResolver().openOutputStream(uri);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(outputStream));
            bw.write(string);
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void editShape(View view) {
        if (editing_game.getCurShape() != null) {
            Intent intent = new Intent(this, EditShapeActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Please select a shape on the canvas first",
                    Toast.LENGTH_SHORT).show();
        }

    }

    public void clearAllShapes(View view) {
        // add to states
        ArrayList<Shape> oldShapes = editing_game.getCurPage().copyShapes();
        // including reset planned_x/y
        editing_game.removeAllShapesInPage(editing_game.getCurPage());
        ArrayList<Shape> newShapes = editing_game.getCurPage().copyShapes();
        editing_game.addState(oldShapes, newShapes);

        editing_game.setCurShape(null);
        display_shape_name.setText("None Shape");

        Toast.makeText(this, "Cleared all shapes!",
                Toast.LENGTH_SHORT).show();

        game_canvas.invalidate();
    }

    public void editPage(View view) {
        // we make all shape selected on the current shape
        for (Shape shape : editing_game.getCurPage().getShapes()) {
            shape.setSelected(false);
        }

        Intent intent = new Intent(this, EditPageActivity.class);
        startActivityForResult(intent, 1);
    }

    public void goPrePage(View view) {
        String pageName = editing_game.getCurPage().getName();
        int idx = editing_game.getPageIdx(pageName);
        if (idx == 0) {
            Toast.makeText(getApplicationContext(),
                    "This is the first page!",
                    Toast.LENGTH_SHORT).show();
        } else {
            Page pre = editing_game.getPages().get(idx - 1);
            editing_game.setCurPage(pre);
            game_canvas.invalidate();
            display_shape_name.setText("None Shape");
            display_page_name.setText(pre.getName());
            if (editing_game.getCurShape() != null) {
                editing_game.getCurShape().setSelected(false);
                editing_game.setCurShape(null);
            }
        }
    }

    public void goNextPage(View view) {
        String pageName = editing_game.getCurPage().getName();
        int idx = editing_game.getPageIdx(pageName);
        if (idx == editing_game.getPages().size() - 1) {
            Toast.makeText(getApplicationContext(),
                    "This is the last page!",
                    Toast.LENGTH_SHORT).show();
        } else {
            Page next = editing_game.getPages().get(idx + 1);
            editing_game.setCurPage(next);
            game_canvas.invalidate();
            display_shape_name.setText("None Shape");
            display_page_name.setText(next.getName());
            if (editing_game.getCurShape() != null) {
                editing_game.getCurShape().setSelected(false);
                editing_game.setCurShape(null);
            }
        }
    }

    public void undo(View view) {
        if (editing_game.getStates().isEmpty() || editing_game.getStep() == 0) {
            Toast.makeText(getApplicationContext(),
                    "Cannot undo!",
                    Toast.LENGTH_SHORT).show();
        } else {
            ArrayList<Object> undoList = editing_game.getStates().get(editing_game.getStep() - 1);

            int pageIdx = (int) undoList.get(0);
            ArrayList<Shape> oldShapes = (ArrayList<Shape>) undoList.get(1);
            editing_game.getPages().get(pageIdx).setShapes(oldShapes);
            if (editing_game.getCurShape() != null) {
                editing_game.getCurShape().setSelected(false);

            }
            editing_game.setCurShape(null);
            display_shape_name.setText("None Shape");
            editing_game.changeStep(editing_game.getStep() - 1);
            game_canvas.invalidate();
        }
    }

    public void redo(View view) {
        if (editing_game.getStates().size() == 0 || editing_game.getStep() == editing_game.getStates().size()) {
            Toast.makeText(getApplicationContext(),
                    "Cannot redo!",
                    Toast.LENGTH_SHORT).show();
        } else {
            ArrayList<Object> redoList = editing_game.getStates().get(editing_game.getStep());
            int pageIdx = (int) redoList.get(0);
            ArrayList<Shape> newShapes = (ArrayList<Shape>) redoList.get(2);
            editing_game.getPages().get(pageIdx).setShapes(newShapes);
            if (editing_game.getCurShape() != null) {
                editing_game.getCurShape().setSelected(false);
            }
            editing_game.setCurShape(null);
            display_shape_name.setText("None Shape");
            editing_game.changeStep(editing_game.getStep() + 1);
            game_canvas.invalidate();
        }
    }

    public void saveGame(View view) {
        showSaveConfirmDialogAndSave();
    }

    private void showSaveConfirmDialogAndSave() {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("SAVE GAME");
        adb.setMessage("Enter Game Name");

        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);

        db = openOrCreateDatabase(GameManager.database_root, MODE_PRIVATE, null);
        input.setHint(GameManager.getDefaultGameName(db));

        adb.setView(input);
        adb.setIcon(R.mipmap.save);

        adb.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String saved_game_name = input.getText().toString();
                // if not enter any name, we use the default name
                if (saved_game_name.isEmpty()) {
                    saved_game_name = input.getHint().toString();
                }

                if (GameManager.is_game_name_duplicate(db, saved_game_name)) {
                    Toast.makeText(getApplicationContext(),
                            "Name '" + saved_game_name + "' is duplicate!", Toast.LENGTH_SHORT).show();

                    // check the valid name first
                } else if (saved_game_name.matches("[a-zA-Z\\d][\\w#@.]{0,127}$")) {
                    editing_game.setName(saved_game_name);
                    editing_game.addDummyShapeInEachPageHelper();

                    GameManager.save_game(db, editing_game);
                    Toast.makeText(getApplicationContext(), "Saved " + saved_game_name, Toast.LENGTH_SHORT).show();

                    edit_activity.finish();
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

        adb.setNeutralButton("Save & Export JSON", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String saved_game_name = input.getText().toString();
                // if not enter any name, we use the default name
                if (saved_game_name.isEmpty()) {
                    saved_game_name = input.getHint().toString();
                }
                // check the valid name first
                if (saved_game_name.matches("[a-zA-Z\\d][\\w#@]{0,127}$")) {
                    editing_game.setName(saved_game_name);

                    // intent for creating file
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("text/plain");
                    // actually is json file but Android can't distinguish json
                    String file_name = saved_game_name + ".txt";
                    intent.putExtra(Intent.EXTRA_TITLE, file_name);
                    startActivityForResult(intent, CREATE_FILE);

                    Toast.makeText(getApplicationContext(),
                            "Exported '" + file_name + "'!", Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(getApplicationContext(),
                            "Please enter the valid file name!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        adb.show();
        editing_game.clearStates();
    }

    // image_url is the parameter for loading shape from device's file
    static void addShapeIntoCanvasHelper(String referred_uri, String image_name) {
        Game cur_game = GameManager.getCurGame();
        Page cur_page = cur_game.getCurPage();

        String shapeName = cur_page.getDefaultNewShapeName();

        Shape selected_shape_from_photo = new Shape(shapeName, cur_page.getName(),
                cur_page.getNextShapeX(), cur_page.getNextShapeY());

        selected_shape_from_photo.setReferredImageName(referred_uri);
        if (referred_uri.startsWith("content://") && !cur_game.getShortenImageName().containsKey(image_name)) {
            cur_game.addShortenImageName(image_name, referred_uri);
        }

        Toast.makeText(edit_activity, image_name + " is added",
                Toast.LENGTH_SHORT).show();

        // Update next planned coordinates
        cur_page.updateNextShapeX(selected_shape_from_photo);
        cur_page.updateNextShapeY(selected_shape_from_photo);

        // add to oldShapes for backup
        ArrayList<Shape> oldShapes = cur_game.getCurPage().copyShapes();

        // Add this new shape to our page
        cur_page.addShape(selected_shape_from_photo);

        // Display the shape name in the editor
        // display_shape_name.setText(selected_shape_from_gallery.getName());
        for (Shape shape : cur_page.getShapes()) {
            shape.setSelected(false);
        }

        // add to states
        ArrayList<Shape> newShapes = cur_game.getCurPage().copyShapes();
        cur_game.addState(oldShapes, newShapes);

        GameManager.getGameCanvas().invalidate();

    }

    public ArrayList<Integer> getImageIDs() {
        ArrayList<Integer> image_ids = new ArrayList<>();

        for (String img_name : image_names) {
            try {
                Integer cur_id = getResources().getIdentifier(img_name, "drawable", getPackageName());
                image_ids.add(cur_id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return image_ids;
    }

    private String getFileNameFromUri(Uri uri) {
        String name;

        assert uri != null;
        Cursor returnCursor = getContentResolver().query(uri, null, null, null, null);
        /*
         * Get the column indexes of the data in the Cursor,
         * move to the first row in the Cursor, get the data,
         * and display it.
         */
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        name = returnCursor.getString(nameIndex);
        returnCursor.close();
        return name;
    }

    public class ImageAdapter extends BaseAdapter {
        private Context context;
        private int itemBackground;

        public ImageAdapter(Context c) {
            context = c;
            // sets a grey background; wraps around the images
            TypedArray a = obtainStyledAttributes(R.styleable.DrawableGallery);
            itemBackground = a.getResourceId(R.styleable.DrawableGallery_android_galleryItemBackground, 0);
            a.recycle();
        }

        // returns the number of images
        public int getCount() {
            return image_ids.size();
        }

        // returns the ID of an item
        public Object getItem(int position) {
            return position;
        }

        // returns the ID of an item
        public long getItemId(int position) {
            return position;
        }

        // returns an ImageView view
        // Ref: https://www.androidinterview.com/android-gallery-view-example-displaying-a-list-of-images/
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView = new ImageView(context);
            imageView.setImageResource(image_ids.get(position));

            int gallery_width = 270;
            int gallery_height = 270;

            imageView.setLayoutParams(new Gallery.LayoutParams(gallery_width, gallery_height));
            imageView.setBackgroundResource(itemBackground);
            // Set the background to white instead of gray
            imageView.setBackgroundColor(Color.WHITE);
            return imageView;
        }
    }
}

