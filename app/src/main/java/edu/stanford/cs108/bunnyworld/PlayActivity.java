package edu.stanford.cs108.bunnyworld;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class PlayActivity extends AppCompatActivity {

    static Game game;
    static SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        db = openOrCreateDatabase(GameManager.database_root, MODE_PRIVATE, null);
        GameManager.init_db(db);

        game = GameManager.getCurGame();
        game.setEditing(false);
        GameManager.setActivityRunsGameCanvas(this);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

}
