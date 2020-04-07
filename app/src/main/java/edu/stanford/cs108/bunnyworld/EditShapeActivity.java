package edu.stanford.cs108.bunnyworld;

import android.app.Activity;
import android.content.Intent;
import android.graphics.RectF;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

// extends `Activity` rather than AppCompatActivity
// to hide the title `BunnyWorld`
public class EditShapeActivity extends Activity {
    private Game game;

    private EditText shape_name;
    private EditText x_input, y_input, width_input, height_input;
    private EditText fontSize;
    private CheckBox hidden_cbox, movable_cbox;

    private Spinner pictureNameSpinner, fontFamilySpinner, fontStyleSpinner, fontColorSpinner;
    private TextView shapeText;
    private ArrayList<String> pictureNameArray, fontFamilyArray, fontStyleArray, fontColorArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_edit_shape);
        init();

    }

    private void init() {
        game = GameManager.getCurGame();

        pictureNameArray = new ArrayList<String>(Arrays.asList(
                "bunny", "angry_bunny", "fire", "duck",
                "carrot1", "carrot2", "add_text", "door", "other"));
        pictureNameArray.addAll(game.getShortenImageName().keySet());

        fontFamilyArray = new ArrayList<String>(Arrays.asList(
                "default", "sans serif", "monospace", "serif"));
        fontStyleArray = new ArrayList<String>(Arrays.asList(
                "normal", "italic", "bold", "bold italic"));
        fontColorArray = new ArrayList<String>(Arrays.asList(
                "black", "red", "blue", "yellow"));

        pictureNameSpinner = findViewById(R.id.picture_name_spinner);
        shape_name = findViewById(R.id.shape_name_editText);
        hidden_cbox = findViewById(R.id.shape_hidden_checkBox);
        movable_cbox = findViewById(R.id.shape_movable_checkBox);
        x_input = findViewById(R.id.shape_X_editText);
        y_input = findViewById(R.id.shape_Y_editText);
        width_input = findViewById(R.id.shape_W_editText);
        height_input = findViewById(R.id.shape_H_editText);

        fontSize = findViewById(R.id.edit_font_size);
        fontFamilySpinner = findViewById(R.id.font_family_spinner);
        fontStyleSpinner = findViewById(R.id.font_style_spinner);
        fontColorSpinner = findViewById(R.id.font_color_spinner);
        disableFontPropertiesHelper();

        shapeText = findViewById(R.id.shape_text_editText);
        shapeText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() != 0) {
                    width_input.setAlpha(0.5f);
                    width_input.setFocusable(false);
                    height_input.setAlpha(0.5f);
                    height_input.setFocusable(false);

                    enableFontPropertiesHelper();
                }
            }
        });


        Shape cur_shape = game.getCurShape();
        shape_name.setText(cur_shape.getName());
        movable_cbox.setChecked(cur_shape.isMovable());
        hidden_cbox.setChecked(cur_shape.isHidden());
        x_input.setText(String.valueOf(cur_shape.getBoundingBox().left));
        y_input.setText(String.valueOf(cur_shape.getBoundingBox().top));
        width_input.setText(String.valueOf(cur_shape.getBoundingBox().width()));
        height_input.setText(String.valueOf(cur_shape.getBoundingBox().height()));
        shapeText.setText(cur_shape.getReferredText());
        fontSize.setText(String.valueOf(cur_shape.getFontSize()));
        initiateSpinner();

    }

    private void initiateSpinner() {
        ArrayAdapter<String> pictureNameAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, pictureNameArray);
        pictureNameSpinner.setAdapter(pictureNameAdapter);
        ArrayAdapter<String> fontFamilyAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fontFamilyArray);
        fontFamilySpinner.setAdapter(fontFamilyAdapter);
        ArrayAdapter<String> fontStyleAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fontStyleArray);
        fontStyleSpinner.setAdapter(fontStyleAdapter);
        ArrayAdapter<String> fontColorAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fontColorArray);
        fontColorSpinner.setAdapter(fontColorAdapter);

        Shape cur_shape = game.getCurShape();

        // select other if we load this picture from external storage
        int pictureIdx = pictureNameArray.indexOf(cur_shape.getReferredImageName());
        if (pictureIdx > -1) {
            pictureNameSpinner.setSelection(pictureIdx);
        } else {
            pictureNameSpinner.setSelection(pictureNameArray.size() - 1);
        }
        fontFamilySpinner.setSelection(fontFamilyArray.indexOf(cur_shape.getFontFamily()));
        fontStyleSpinner.setSelection(fontStyleArray.indexOf(cur_shape.getFontStyle()));
        fontColorSpinner.setSelection(fontColorArray.indexOf(cur_shape.getFontColor()));
    }

    public void updateShapeProperty(View view) {
        if (game.getCurShape() == null) {
            return;
        }

        Shape cur_shape = game.getCurShape();
        String new_name = shape_name.getText().toString();
        String old_name = cur_shape.getName();

        // save the old shape for comparison
        Shape old_shape = cur_shape.deepCopy();
        ArrayList<Shape> oldShapes = game.getCurPage().copyShapes();

        if (new_name.isEmpty()) {
            Toast.makeText(getApplicationContext(),
                    "Invalid shape name!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // we do change the name and is duplicate
        if (!new_name.equals(old_name) && game.isShapeNameDuplicate(new_name)) {
            Toast.makeText(getApplicationContext(),
                    "Shape name is duplicate!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        game.updateShapesNamePool(old_name, new_name);
        // we need to consider the page name when replacing scripts
        game.updateScriptsByNameChange(cur_shape.getPageName() + "-" + old_name,
                cur_shape.getPageName() + "-" + new_name);

        cur_shape.setName(new_name);
        cur_shape.setReferredImageName(pictureNameSpinner.getSelectedItem().toString());
        cur_shape.setHidden(hidden_cbox.isChecked());
        cur_shape.setMovable(movable_cbox.isChecked());

        if (!shapeText.getText().toString().isEmpty()) {
            cur_shape.setReferredText(shapeText.getText().toString());
            cur_shape.setFontSize(Float.parseFloat(fontSize.getText().toString()));
            cur_shape.setFontFamily(fontFamilySpinner.getSelectedItem().toString());
            cur_shape.setFontStyle(fontStyleSpinner.getSelectedItem().toString());
            cur_shape.setFontColor(fontColorSpinner.getSelectedItem().toString());
        }

        RectF old_bb = cur_shape.getBoundingBox();

        String str_x = x_input.getText().toString();
        String str_y = y_input.getText().toString();
        String str_w = width_input.getText().toString();
        String str_h = height_input.getText().toString();

        float new_left = str_x.isEmpty() ? old_bb.left : Float.parseFloat(str_x);
        float new_top = str_y.isEmpty() ? old_bb.top : Float.parseFloat(str_y);
        float new_right = str_w.isEmpty() ? old_bb.left : Float.parseFloat(str_w) + new_left;
        float new_bottom = str_h.isEmpty() ? old_bb.bottom : Float.parseFloat(str_h) + new_top;

        RectF new_bb = new RectF(new_left, new_top, new_right, new_bottom);


        cur_shape.setBoundingBox(new_bb);

        EditActivity.display_shape_name.setText(cur_shape.getName());

        // Add the edited shape to states for backup
        Shape new_shape = cur_shape.deepCopy();
        if (!old_shape.equals(new_shape)) {
            ArrayList<Shape> newShapes = game.getCurPage().copyShapes();
            game.addState(oldShapes, newShapes);

            Toast.makeText(getApplicationContext(),
                    "Updated Successfully!",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getApplicationContext(),
                    "No updates!",
                    Toast.LENGTH_SHORT).show();
        }

        GameManager.getGameCanvas().invalidate();
    }

    public void setDefaultProperty(View view) {
        ArrayList<Shape> oldShapes = game.getCurPage().copyShapes();

        Shape cur_shape = game.getCurShape();
        cur_shape.setMovable(false);
        cur_shape.setHidden(false);

        RectF bb = cur_shape.getBoundingBox();
        cur_shape.setBoundingBox(new RectF(bb.left, bb.top,
                bb.left + Shape.DEFAULT_WIDTH, bb.top + Shape.DEFAULT_HEIGHT));


        cur_shape.setReferredText("");
        cur_shape.setFontSize(Shape.DEFAULT_FONT_SIZE);
        cur_shape.setFontFamily("default");
        cur_shape.setFontStyle("normal");
        cur_shape.setFontColor("black");

        movable_cbox.setChecked(false);
        hidden_cbox.setChecked(false);

        x_input.setText(String.valueOf(bb.left));
        y_input.setText(String.valueOf(bb.top));
        width_input.setText(String.valueOf(Shape.DEFAULT_WIDTH));
        height_input.setText(String.valueOf(Shape.DEFAULT_HEIGHT));
        shapeText.setText("");
        fontSize.setText(String.valueOf(Shape.DEFAULT_FONT_SIZE));

        initiateSpinner();

        ArrayList<Shape> newShapes = game.getCurPage().copyShapes();
        game.addState(oldShapes, newShapes);

        GameManager.getGameCanvas().invalidate();
    }

    public void cancelEditShape(View view) {
//        Intent intent = new Intent(this, EditActivity.class);
//        startActivity(intent);
        this.finish();
    }

    public void editShapeScript(View view) {
        Intent intent = new Intent(this, EditShapeScriptActivity.class);
        startActivity(intent);
    }

    private void disableFontPropertiesHelper() {
        fontSize.setAlpha(0.5f);
        fontSize.setEnabled(false);

        fontFamilySpinner.setAlpha(0.5f);
        fontFamilySpinner.setEnabled(false);

        fontStyleSpinner.setAlpha(0.5f);
        fontStyleSpinner.setEnabled(false);

        fontColorSpinner.setAlpha(0.5f);
        fontColorSpinner.setEnabled(false);
    }

    private void enableFontPropertiesHelper() {
        fontSize.setAlpha(1f);
        fontSize.setEnabled(true);

        fontFamilySpinner.setAlpha(1f);
        fontFamilySpinner.setEnabled(true);

        fontStyleSpinner.setAlpha(1f);
        fontStyleSpinner.setEnabled(true);

        fontColorSpinner.setAlpha(1f);
        fontColorSpinner.setEnabled(true);
    }
}
