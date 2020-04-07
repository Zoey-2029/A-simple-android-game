/*
 * We have classes: GameManager -> Game -> Page -> Shape
 */
package edu.stanford.cs108.bunnyworld;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import static android.view.View.MeasureSpec.UNSPECIFIED;


public class MainActivity extends AppCompatActivity {
    SQLiteDatabase db;
    ArrayList<String> saved_game_names;

    private Context context;
    private String chosen_game_name;

    // use popup window as dropdown instead of spinners
    // spinner only call select only if is the different selection
    PopupWindow popupWindowGames;
    Button popupButton;
    ArrayAdapter<String> arr_adapter;

    private TextView title;
    private Animation anim_1, anim_2, anim_3;

    private static final int PICK_JSON_FILE = 817;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = openOrCreateDatabase(GameManager.database_root, MODE_PRIVATE, null);
        GameManager.init_db(db);
        saved_game_names = GameManager.get_game_names(db);

        context = getApplicationContext();
        chosen_game_name = "";
        title = findViewById(R.id.title);

        arr_adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                saved_game_names) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                String gameName = getItem(position);

                // visual settings for the list item
                TextView listGame = new TextView(MainActivity.this);
                listGame.setTextSize(20);
                listGame.setGravity(Gravity.CENTER);
                listGame.setText(gameName);
                listGame.setBackgroundColor(Color.parseColor("#E5FFCC"));
                listGame.setPadding(20, 10, 10, 10);
                listGame.setTextColor(Color.BLACK);
                listGame.setTypeface(ResourcesCompat.getFont(context, R.font.lemon));
                return listGame;
            }
        };
        arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        popupWindowGames = popupWindowGames(arr_adapter);

        popupButton = findViewById(R.id.button_main_play);
        popupButton.setAllCaps(false);
        popupButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!saved_game_names.isEmpty()) {
                    popupWindowGames.showAsDropDown(v, 0, 3);
                }
            }
        });

        welcome();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // brute-force way to update game_names list when we going back to the activity
        for (String new_name : GameManager.get_game_names(db)) {
            if (!saved_game_names.contains(new_name)) {
                arr_adapter.add(new_name);
                break;
            }
        }
        saved_game_names = GameManager.get_game_names(db);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == PICK_JSON_FILE && resultCode == Activity.RESULT_OK) {
            Uri jsonUri;
            if (resultData != null) {
                jsonUri = resultData.getData();
                String jsonStr = readJSONHelper(jsonUri);
                Game game = GameManager.parse_json(jsonStr);
                if (game == null) {
                    Toast.makeText(this, "Can't import this game!", Toast.LENGTH_SHORT).show();
                    return;
                }
                // instead of directly start playing game, we load it into our database
                String game_name = game.getName();
                GameManager.save_game(db, game);

                popupButton.setText(game_name);
                popupButton.setTextSize(20);
                chosen_game_name = game_name;
            }
        }
    }

    private void welcome() {
        Button but = findViewById(R.id.btn_main_edit);
        anim_1 = AnimationUtils.loadAnimation(this, R.anim.main_1);
        anim_2 = AnimationUtils.loadAnimation(this, R.anim.main_2);
        anim_3 = AnimationUtils.loadAnimation(this, R.anim.main_3);
        final MediaPlayer mp = MediaPlayer.create(this, R.raw.welcome);
//        title.startAnimation(animation);
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        title.setVisibility(View.VISIBLE);
                        mp.start();
                        title.startAnimation(anim_1);
                    }
                },
                1000);
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        title.startAnimation(anim_2);
                    }
                },
                2500);
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        title.startAnimation(anim_3);
                    }
                },
                3000);
    }

    private String readJSONHelper(Uri uri) {
        String outJson = "";
        StringBuilder stringBuilder = new StringBuilder();

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = br.readLine()) != null) {
                stringBuilder.append(line + System.lineSeparator());
            }
            outJson = stringBuilder.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return outJson;
    }

    public void enterEdit(View view) {
        // we could choose to edit a new game or just continue the selected one
        if (!chosen_game_name.isEmpty()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    new ContextThemeWrapper(this, R.style.AlertDialogMainActivity));

            String[] choices = {"New Game", chosen_game_name};
            builder.setItems(choices, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 1) {
                        Game game = GameManager.load_game(db, chosen_game_name);
                        enterEditHelper(game);
                    } else if (which == 0) {
                        enterEditHelper(GameManager.getNewGame());
                    }
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();

        } else {
            enterEditHelper(GameManager.getNewGame());
        }

    }

    private void enterEditHelper(Game game) {
        GameManager.setCurGame(game);
        Intent intent = new Intent(context, EditActivity.class);
        startActivity(intent);
    }

    public void loadGame(View view) {
        if (!chosen_game_name.isEmpty()) {
            Game game = GameManager.load_game(db, chosen_game_name);
            GameManager.setCurGame(game);

            Intent intent = new Intent(context, PlayActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText
                    (getApplicationContext(), "Please choose a game to load", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void importGame(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("text/plain");
        startActivityForResult(intent, PICK_JSON_FILE);
    }

    public void clearGame(View view) {
        if (!chosen_game_name.isEmpty() && !chosen_game_name.equals("example")) {
            GameManager.del_game(db, chosen_game_name);
            Toast.makeText
                    (getApplicationContext(), "Cleared '" + chosen_game_name + "' successfully!", Toast.LENGTH_SHORT)
                    .show();

            notifyLocalDataStructuresHelper(chosen_game_name, false);
        } else if (chosen_game_name.equals("example")) {
            Toast.makeText
                    (getApplicationContext(), "Cannot delete test game!", Toast.LENGTH_SHORT)
                    .show();
        } else {
            Toast.makeText
                    (getApplicationContext(), "Please choose a game to delete", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public void clearAllGames(View view) {
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("CLEAR ALL GAMES");
        alertDialog.setMessage("Are you sure to delete all games?");
        alertDialog.setIcon(R.mipmap.hold);

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        GameManager.del_all_games(db);
                        dialog.dismiss();
                        notifyLocalDataStructuresHelper("", true);
                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    private void notifyLocalDataStructuresHelper(String name, boolean clear) {
        if (clear) {
            arr_adapter.clear();
            saved_game_names.clear();

            arr_adapter.add("example");
            saved_game_names.add("example");

        } else {
            arr_adapter.remove(name);
            saved_game_names.remove(name);
        }

        arr_adapter.notifyDataSetChanged();
        popupButton.setText("");
        chosen_game_name = "";
    }


    PopupWindow popupWindowGames(ArrayAdapter<String> adapter) {
        ListView listViewGames = new ListView(this);

        listViewGames.setAdapter(adapter);
        listViewGames.measure(View.MeasureSpec.makeMeasureSpec(660, View.MeasureSpec.AT_MOST), UNSPECIFIED);
        listViewGames.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Context context = view.getContext();

                // add some animation when a list item was clicked
                Animation fadeInAnimation = AnimationUtils.loadAnimation(context, android.R.anim.fade_in);
                fadeInAnimation.setDuration(10);
                view.startAnimation(fadeInAnimation);

                popupWindowGames.dismiss();

                // get the text and set it as the button text
                chosen_game_name = (String) adapterView.getItemAtPosition(i);
                popupButton.setText(chosen_game_name);
                popupButton.setTextSize(22);

                Toast.makeText(getApplicationContext(), "Selected " + chosen_game_name,
                        Toast.LENGTH_SHORT).show();
            }
        });

        // set the list view as pop up window content
        return new PopupWindow(listViewGames, listViewGames.getMeasuredWidth(),
                WindowManager.LayoutParams.WRAP_CONTENT, true);
    }


}
