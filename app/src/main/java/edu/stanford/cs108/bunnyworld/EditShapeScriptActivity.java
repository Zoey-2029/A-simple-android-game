package edu.stanford.cs108.bunnyworld;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

public class EditShapeScriptActivity extends Activity {

    private Game game;
    private Activity editShapeScriptActivity;

    private Spinner triggerSpinner, shapeSpinner, actionSpinner, objectSpinner;
    private ListView scripList;
    private ArrayList<String> shapeArray, objectArray, triggerArray, actionArray;
    private Button musicImporter;
    private Uri musicUri;

    private ArrayAdapter<String> scriptAdapter;

    private static final int READ_REQUEST_CODE = 42;

    // define the possible pairs could combine
    private static HashMap<String, ArrayList<String>> combinePairs = new HashMap<String, ArrayList<String>>() {
        {
            put("duck", new ArrayList<>(Arrays.asList("fire")));
            put("fire", new ArrayList<>(Arrays.asList("duck")));
            put("bunny", new ArrayList<>(Arrays.asList("carrot1", "carrot2")));
            put("carrot1", new ArrayList<>(Arrays.asList("bunny")));
            put("carrot2", new ArrayList<>(Arrays.asList("bunny")));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_edit_shape_script);

        game = GameManager.getCurGame();
        editShapeScriptActivity = this;

        init();
    }

    private void init() {
        triggerArray = new ArrayList<String>();
        shapeArray = new ArrayList<String>();
        actionArray = new ArrayList<String>();
        objectArray = new ArrayList<String>();
        triggerArray.add("on click");
        triggerArray.add("on enter");
        triggerArray.add("on drop");
        actionArray.add("play");
        actionArray.add("goto");
        actionArray.add("show");
        actionArray.add("hide");
        actionArray.add("combine");

        triggerSpinner = (Spinner) findViewById(R.id.trigger_spinner);
        shapeSpinner = (Spinner) findViewById(R.id.shape_spinner);
        actionSpinner = (Spinner) findViewById(R.id.action_spinner);
        objectSpinner = (Spinner) findViewById(R.id.object_spinner);
        musicImporter = (Button) findViewById(R.id.object_importer);
        musicUri = null;
        scripList = (ListView) findViewById(R.id.script_list);

        ArrayAdapter<String> triggerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, triggerArray);
        triggerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        triggerSpinner.setAdapter(triggerAdapter);
        triggerSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                shapeArray.clear();
                // on drop + shape
                if (i == 2) {
                    // remove this shape's itself
                    Shape cur_shape = game.getCurShape();
                    String cur_shape_name_with_page = cur_shape.getFullName();
                    for (String shapeName : game.getFullShapesNamePool()) {
                        if (!shapeName.equals(cur_shape_name_with_page) && !shapeName.contains("dummy"))
                            shapeArray.add(shapeName);
                    }
                } else { // on click/ on enter + activity
                    shapeArray.add("NO SHAPE");
                }

                ArrayAdapter<String> shapeAdapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_spinner_item, shapeArray);
                shapeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                shapeSpinner.setAdapter(shapeAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        ArrayAdapter<String> actionAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, actionArray);
        actionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        actionSpinner.setAdapter(actionAdapter);
        actionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                objectArray.clear();
                // we set the import button only clickable when we choose object `play`
                musicImporter.setClickable(position == 0);
                musicImporter.setAlpha(musicImporter.isClickable() ? 1f : 0.5f);

                // go to pages
                if (position == 0) { // play sound
                    objectArray.add("carrotcarrotcarrot");
                    objectArray.add("evillaugh");
                    objectArray.add("fire");
                    objectArray.add("hooray");
                    objectArray.add("munch");
                    objectArray.add("munching");
                    objectArray.add("woof");

                } else if (position == 1) { // goto page
                    objectArray.addAll(game.getPagesNamePool());
                    // remove this shape's page option
                    objectArray.remove(game.getCurPage().getName());

                    // if we don't have other pages to go
                    if (objectArray.isEmpty()) {
                        Toast.makeText(getApplicationContext(),
                                "No page can goto!", Toast.LENGTH_SHORT).show();
                    }
                } else if (position == 4) { // combine shapes
                    ArrayList<String> pairs = filterCombineShapeHelper();
                    if (pairs != null) {
                        objectArray.addAll(pairs);
                    }

                } else { // hide or show shape, should include other pages
                    for (String name : game.getFullShapesNamePool()) {
                        // hide dummy shape
                        if (!name.contains("dummy")) {
                            objectArray.add(name);
                        }
                    }
                }

                ArrayAdapter<String> objectAdapter = new ArrayAdapter<String>(view.getContext(), android.R.layout.simple_spinner_item, objectArray);
                objectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                objectSpinner.setAdapter(objectAdapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        triggerSpinner.setSelection(0);
        shapeSpinner.setSelection(0);
        actionSpinner.setSelection(0);
        objectSpinner.setSelection(0);

        scriptAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                game.getCurShape().getScripts()) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                String curScript = getItem(position);
                Shape curShape = game.getCurShape();
                int valid_code = isValidScriptAtPosition(curShape, position);

                // visual settings for the list item
                TextView listScript = new TextView(EditShapeScriptActivity.this);
                listScript.setTextSize(14);
                listScript.setText(curScript);
                listScript.setTextColor(Color.BLACK);
                listScript.setTypeface(ResourcesCompat.getFont(EditShapeScriptActivity.this,
                        R.font.lemon));
                listScript.setPaddingRelative(10, 10, 10, 10);

                // script has something wrong
                if (valid_code != SCRIPT_OK) {
                    if (valid_code == SCRIPT_MISSING_ERROR) {
                        // red - orange
                        listScript.setBackgroundColor(Color.parseColor("#ff8566"));
                    } else if (valid_code == SCRIPT_HIDE_SHOW_MISMATCH) {
                        // light blue
                        listScript.setBackgroundColor(Color.parseColor("#99b3ff"));
                    }
                    // duplicate play
                } else if (curShape.getAction(position).equals("play")) {
                    if (curShape.getTrigger(position).equals("on click") &&
                            isExistPlayScriptAtPosition(curShape, position, "on click") == SCRIPT_EXIST_PLAY) {
                        // light green
                        listScript.setBackgroundColor(Color.parseColor("#b3e6b3"));
                    } else if (curShape.getTrigger(position).equals("on enter") &&
                            isExistPlayScriptAtPosition(curShape, position, "on enter") == SCRIPT_EXIST_PLAY) {
                        // yellow
                        listScript.setBackgroundColor(Color.parseColor("#ffcc80"));
                    }
                }

                return listScript;
            }
        };

        scripList.setAdapter(scriptAdapter);
        scripList.setLongClickable(true);
        scripList.setOnItemLongClickListener(new myItemLongClickListener());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                musicUri = resultData.getData();
                objectSpinner.setVisibility(View.INVISIBLE);

                Toast.makeText(getApplicationContext(),
                        "Import '" + getFileNameFromUri(musicUri) + "' successfully!",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void importMusic(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    private ArrayList<String> filterCombineShapeHelper() {
        Shape cur_shape = game.getCurShape();
        String image_name = cur_shape.getReferredImageName();
        if (!combinePairs.containsKey(image_name)) {
            return null;
        }
        ArrayList<String> pairs = new ArrayList<>();
        for (String paired_image_name : combinePairs.get(image_name)) {
            pairs.addAll(game.getFullShapesNamePoolByImageName(paired_image_name));
        }
        return pairs;
    }

    private String triggerStr, triggerShapeStr, actionStr, actionObjectStr = "";

    public void addNewScript(View view) throws CloneNotSupportedException {
        if (triggerSpinner.getSelectedItem() != null &&
                shapeSpinner.getSelectedItem() != null &&
                actionSpinner.getSelectedItem() != null &&
                objectSpinner.getSelectedItem() != null) {

            triggerStr = triggerSpinner.getSelectedItem().toString();
            triggerShapeStr = shapeSpinner.getSelectedItem().toString();
            actionStr = actionSpinner.getSelectedItem().toString();
            actionObjectStr = objectSpinner.getSelectedItem().toString();
        }

        if (actionObjectStr.equals("")) {
            Toast.makeText(getApplicationContext(),
                    "No action object!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (objectSpinner.getSelectedItem() == null) {
            return;
        }

        // we load this actionObject (music) from file
        if (musicUri != null) {
            actionObjectStr = musicUri.toString();
        } else {
            actionObjectStr = objectSpinner.getSelectedItem().toString();
        }

        if (triggerShapeStr != null && triggerShapeStr.contains("NO SHAPE")) {
            triggerShapeStr = "";
        }

        String newScriptDisplay = triggerStr + " " + triggerShapeStr + " " + actionStr + " " + actionObjectStr + ";";
        String newScript = newScriptDisplay.substring(0, newScriptDisplay.length() - 1);  // remove the last comma
        Shape cur_shape = game.getCurShape();

        ArrayList<Shape> oldShapes = game.getCurPage().copyShapes();

        if (cur_shape.getScripts().contains(newScript)) {
            Toast.makeText(getApplicationContext(),
                    "Script duplicated!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (triggerStr.equals("on click") && actionStr.equals("goto")
                && isExistGoToScript(cur_shape, "on click") == SCRIPT_EXIST_GOTO) {
            Toast.makeText(getApplicationContext(),
                    "Can't add multiple 'on click-goto'!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (triggerStr.equals("on enter") && actionStr.equals("goto")
                && isExistGoToScript(cur_shape, "on enter") == SCRIPT_EXIST_GOTO) {
            Toast.makeText(getApplicationContext(),
                    "Can't add multiple 'on enter-goto'!", Toast.LENGTH_SHORT).show();
            return;
        }


        if (newScript.trim().isEmpty()) {
            return;
        }

        cur_shape.addScriptFieldsHelper(triggerStr, triggerShapeStr, actionStr, actionObjectStr, newScript);

        // To update display scripts in real-time
        scriptAdapter.notifyDataSetChanged();

        Toast.makeText(getApplicationContext(),
                "Added a new script!", Toast.LENGTH_SHORT).show();

        // add to states
        ArrayList<Shape> newShapes = game.getCurPage().copyShapes();
        game.addState(oldShapes, newShapes);
    }

    public void backEditShapeScript(View view) {
        // Intent intent = new Intent(this, EditShapeActivity.class);
        // startActivity(intent);
        this.finish();
    }

    public void clearAllEditShapeScript(View view) {
        ArrayList<Shape> oldShapes = game.getCurPage().copyShapes();

        Shape cur_shape = game.getCurShape();

        // if we hide a shape before, now set it visible
        game.updateShapePropertyByScriptDeletion(cur_shape);
        GameManager.getGameCanvas().invalidate();

        // clear all script fields
        cur_shape.clearScriptFieldsHelper();

        // To update display scripts in real-time
        scriptAdapter.notifyDataSetChanged();

        // add to states
        ArrayList<Shape> newShapes = game.getCurPage().copyShapes();
        game.addState(oldShapes, newShapes);
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

    private class myItemLongClickListener implements AdapterView.OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int pos, long id) {
            final int pos2 = pos;
            final PopupMenu popupMenu = new PopupMenu(editShapeScriptActivity, arg1);
            popupMenu.getMenuInflater().inflate(R.menu.edit_shape_script_popup, popupMenu.getMenu());
            popupMenu.show();

            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.del_script) {
                        ArrayList<Shape> oldshapes = game.getCurPage().copyShapes();

                        game.getCurShape().removeScriptFieldsAtPositionHelper(pos2);
                        scriptAdapter.notifyDataSetChanged();

                        ArrayList<Shape> newshapes = game.getCurPage().copyShapes();
                        game.addState(oldshapes, newshapes);
                    }
                    return true;
                }
            });
            return true;
        }
    }


    final int SCRIPT_EXIST_GOTO = 99;
    final int SCRIPT_EXIST_PLAY = 100;
    final int SCRIPT_MISSING_ERROR = 101;
    final int SCRIPT_HIDE_SHOW_MISMATCH = 102;
    final int SCRIPT_OK = 128;

    private int isValidScriptAtPosition(Shape shape, int i) {
        if (i >= shape.getScripts().size()) {
            return 0;
        }

        String trigger = shape.getTrigger(i);
        String triggerShape = shape.getTriggerShape(i);
        String action = shape.getAction(i);
        String actionObject = shape.getActionObject(i);

        // check trigger == on drop
        if (trigger.equals("on drop") && !game.getFullShapesNamePool().contains(triggerShape)) {
            return SCRIPT_MISSING_ERROR;
        }

        // check action == goto
        if (action.equals("goto") && !game.getPagesNamePool().contains(actionObject)) {
            return SCRIPT_MISSING_ERROR;
        }

        if ((action.equals("hide") || action.equals("show")) && !game.getFullShapesNamePool().contains(actionObject)) {
            return SCRIPT_MISSING_ERROR;
        }

        Shape actionShape = game.getShapeByFullName(actionObject);
        if ((action.equals("hide") && actionShape.isHidden()) || action.equals("show") && !actionShape.isHidden()) {
            return SCRIPT_HIDE_SHOW_MISMATCH;
        }

        return SCRIPT_OK;
    }

    private int isExistPlayScriptAtPosition(Shape shape, int i, String triggerStr) {
        int play_count = 0;
        for (int j = i; j >= 0; j--) {
            if (shape.getTrigger(j).equals(triggerStr) && shape.getAction(j).equals("play")) {
                play_count++;
            }
            if (play_count > 1) {
                return SCRIPT_EXIST_PLAY;
            }
        }
        return 0;
    }

    private int isExistGoToScript(Shape shape, String triggerStr) {
        int goto_count = 0;
        for (int j = shape.getScripts().size() - 1; j >= 0; j--) {
            if (shape.getTrigger(j).equals(triggerStr) && shape.getAction(j).equals("goto")) {
                goto_count++;
            }
            // we already have a goto script
            if (goto_count > 0) {
                return SCRIPT_EXIST_GOTO;
            }
        }
        return 0;
    }
}
