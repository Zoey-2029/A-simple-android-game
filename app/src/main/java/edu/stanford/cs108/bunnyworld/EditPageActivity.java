package edu.stanford.cs108.bunnyworld;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class EditPageActivity extends Activity {

    private Game editing_game;
    private GameCanvas game_canvas;

    private EditText et;
    private String edit;
    private TextView tv;
    private RadioGroup group;
    private int checkedId;
    private RadioButton r;
    private String checkedBtnName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_edit_page);

        // currPage = getIntent().getExtras().getParcelable(EditActivity.PAGE_EXTRA);
        group = findViewById(R.id.page_rdgroup);
        editing_game = GameManager.getCurGame();
        game_canvas = GameManager.getGameCanvas();

        int i = 0;
        for (Page page : editing_game.getPages()) {
            RadioButton rb = (RadioButton) group.getChildAt(i);
            rb.setText(page.getName());
            if (editing_game.getCurPage().getName().toLowerCase().equals(page.getName().toLowerCase())) {
                rb.setChecked(true);
            }
            i++;
        }
    }

    public void createPage(View view) {
        checkedBtnName = getCheckedBtnName();
        if (checkedBtnName.equals("empty")) {
            String pageName = editing_game.setDefaultPageName();
            Page newPage = new Page(pageName);
            editing_game.addPage(newPage);
            editing_game.setCurPage(newPage);

            r.setText(pageName);

            EditActivity.display_page_name.setText(pageName);
            EditActivity.display_shape_name.setText("None Shape");

            game_canvas.invalidate();

        } else {
            Toast.makeText(getApplicationContext(),
                    "This page already exists!",
                    Toast.LENGTH_SHORT).show();
        }
    }


    public void renamePage(View view) {
        et = (EditText) findViewById(R.id.edit_page_name);
        edit = et.getText().toString().trim();
        checkedBtnName = getCheckedBtnName();
        if (checkedBtnName.equals("empty")) {
            Toast.makeText(getApplicationContext(),
                    "This page hasn't been created!",
                    Toast.LENGTH_SHORT).show();
        } else if (checkedBtnName.equals("page1")) {
            Toast.makeText(getApplicationContext(),
                    "Cannot rename page1!",
                    Toast.LENGTH_SHORT).show();
        } else if (editing_game.isPageNameDuplicate(edit)) {
            Toast.makeText(getApplicationContext(),
                    "This name already exists!",
                    Toast.LENGTH_SHORT).show();
        } else if (!edit.isEmpty()) {
            r.setText(edit);

            if (editing_game.getCurPage().getName().toLowerCase().equals(checkedBtnName)) {
                EditActivity.display_page_name.setText(edit);
            }

            editing_game.getPage(checkedBtnName).setName(edit);
            editing_game.updatePagesNamePool(checkedBtnName, edit);
            et.setText("");

            // update scripts by the new page name
            editing_game.updateScriptsByNameChange(checkedBtnName, edit);

            Toast.makeText(getApplicationContext(),
                    "Name changed!",
                    Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(getApplicationContext(),
                    "Please enter a new name!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private String getCheckedBtnName() {
        checkedId = group.getCheckedRadioButtonId();
        r = (RadioButton) findViewById(checkedId);
        return r.getText().toString().toLowerCase();
    }


    public void deletePage(View view) {
        checkedBtnName = getCheckedBtnName();
        if (checkedBtnName.equals("page1")) {
            Toast.makeText(getBaseContext(),
                    "Cannot delete page1!",
                    Toast.LENGTH_SHORT).show();
        } else if (checkedBtnName.equals("empty")) {
            Toast.makeText(getBaseContext(),
                    "This page hasn't been created!",
                    Toast.LENGTH_SHORT).show();
        } else {
            int idx = editing_game.getPageIdx(checkedBtnName);
            if (editing_game.getCurPage() == editing_game.getPages().get(idx)) {
                editing_game.setCurPage(editing_game.getPages().get(0));
                EditActivity.display_page_name.setText("page1");

                game_canvas.invalidate();
            }
            editing_game.deletePage(idx);
            editing_game.updateScriptsByNameDeletion(checkedBtnName);

            r.setText("empty");
            Toast.makeText(getApplicationContext(),
                    "Page deleted successfully!",
                    Toast.LENGTH_SHORT).show();
            editing_game.clearStates();
        }
    }

    public void GoTo(View view) {
        checkedBtnName = getCheckedBtnName();
        if (checkedBtnName.equals("empty")) {
            Toast.makeText(getApplicationContext(),
                    "This page hasn't been created!",
                    Toast.LENGTH_SHORT).show();
        } else {
            editing_game.setCurPage(editing_game.getPage(checkedBtnName), true);
            game_canvas.invalidate();
        }
        EditActivity.display_page_name.setText(checkedBtnName);
        EditActivity.display_shape_name.setText("None Shape");
        this.finish();
    }

}


