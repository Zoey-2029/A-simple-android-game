package edu.stanford.cs108.bunnyworld;


import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.RectF;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

class GameManager {
    private static Game cur_game;
    private static GameCanvas game_canvas;

    // paste shape into other game;
    private static Shape to_be_copied_shape_in_game = null;

    static String database_root = "BunnyWorldDB";

    static Game getCurGame() {
        // Create game if we don't have instance of game
        if (cur_game == null) {
            cur_game = new Game("new_game");
        }
        return cur_game;
    }

    static Game getNewGame() {
        cur_game = new Game("new_game");
        return cur_game;
    }

    static void setCurGame(Game game) {
        cur_game = game;
    }

    static void setActivityRunsGameCanvas(Activity activity) {
        game_canvas = activity.findViewById(R.id.game_canvas);
    }

    static GameCanvas getGameCanvas() {
        return game_canvas;
    }

    static void setToBeCopiedShapeInGame(Shape shape) {
        to_be_copied_shape_in_game = shape.deepCopy();
    }

    static Shape getToBeCopiedShapeInGame() {
        return to_be_copied_shape_in_game;
    }


    static void init_db(SQLiteDatabase db) {
        Cursor tablesCursor = db.rawQuery(
                "SELECT * FROM sqlite_master WHERE type='table' AND name='allGames';", null);
        // Create new list
        if (tablesCursor.getCount() == 0) {
            String init_str = "CREATE TABLE allGames ("
                    + "gameName TEXT, startPage TEXT, imgMap TEXT, _id INTEGER PRIMARY KEY AUTOINCREMENT);";
            db.execSQL(init_str);
        }
        tablesCursor.close();
    }

    static ArrayList<String> get_game_names(SQLiteDatabase db) {
        ArrayList<String> game_names = new ArrayList<>();
        Cursor cursor = db.rawQuery(
                "SELECT gameName FROM allGames;", null);
        while (cursor.moveToNext()) {
            game_names.add(cursor.getString(0));
        }
        cursor.close();
        return game_names;
    }

    // case-insensitive
    static boolean is_game_name_duplicate(SQLiteDatabase db, String my_name) {
        for (String name : get_game_names(db)) {
            if (name.toLowerCase().equals(my_name.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static ArrayList<Shape> get_game_shapes(SQLiteDatabase db, String game_name) {
        if (!get_game_names(db).contains(game_name)) {
            return null;
        }

        ArrayList<Shape> shape_list = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM \"" + game_name + "\";", null);
        while (cursor.moveToNext()) {
            shape_list.add(new Shape(
                    cursor.getFloat(0), cursor.getFloat(1), cursor.getFloat(2), cursor.getFloat(3),
                    cursor.getString(4), cursor.getString(5), cursor.getString(6), cursor.getString(7),
                    cursor.getString(8), cursor.getFloat(9), cursor.getString(10), cursor.getString(11),
                    cursor.getString(12), cursor.getInt(13), cursor.getInt(14)));
        }

        cursor.close();
        return shape_list;
    }


    private static String get_game_start_page(SQLiteDatabase db, String game_name) {
        if (!get_game_names(db).contains(game_name)) {
            return null;
        }

        Cursor cursor = db.rawQuery(
                "SELECT startPage FROM allGames WHERE gameName = '" + game_name + "';", null);
        if (cursor.moveToNext()) {
            return cursor.getString(0);
        } else {
            return null;
        }
    }

    private static HashMap<String, String> get_game_hashmap(SQLiteDatabase db, String game_name) {
        if (!get_game_names(db).contains(game_name)) {
            return null;
        }

        Cursor cursor = db.rawQuery(
                "SELECT imgMap FROM allGames WHERE gameName = '" + game_name + "';", null);
        if (cursor.moveToNext()) {
            return hashMapStringDecoder(cursor.getString(0));
        } else {
            return null;
        }
    }

    static void save_game(SQLiteDatabase db, Game game) {
        String game_name = game.getName();
        Page start_page = game.getStartPage();
        String hashMapStr = hashMapStringEncoder(game.getShortenImageName());

        // insert or update startPage
        if (get_game_names(db).contains(game_name)) {
            db.execSQL("UPDATE allGames SET startPage = '" + start_page.getName() + "', imgMap='" + hashMapStr + "' WHERE gameName = '" + game_name + "';");
            db.execSQL("DROP TABLE IF EXISTS \"" + game_name + "\";");
        } else {
            db.execSQL("INSERT INTO allGames VALUES ('" + game_name + "', '" + start_page.getName() + "','" + hashMapStr + "', NULL);");
        }

        db.execSQL("CREATE TABLE \"" + game_name + "\" (x FLOAT, y FLOAT, w FLOAT, h FLOAT, " +
                " pageName TEXT, shapeName TEXT, imageName TEXT, script TEXT, text TEXT," +
                "fontSize FLOAT, fontFamily TEXT, fontStyle TEXT, fontColor TEXT," +
                "isHidden INTEGER, isMovable INTEGER, _id INTEGER PRIMARY KEY AUTOINCREMENT);");

        ArrayList<Page> pages = game.getPages();
        for (Page page : pages) {
            for (Shape s : page.getShapes()) {
                RectF bb = s.getBoundingBox();
                float x = bb.left;
                float y = bb.top;
                float w = bb.width();
                float h = bb.height();

                int is_hidden = s.isHidden() ? 1 : 0;
                int is_movable = s.isMovable() ? 1 : 0;

                db.execSQL("INSERT INTO \"" + game_name + "\" VALUES (" +
                        x + ", " + y + ", " + w + ", " + h + ", '" +
                        page.getName() + "', '" + s.getName() + "', '" + s.getReferredImageName() + "', '" +
                        s.getScriptStr() + "', '" + s.getReferredText() + "', " + s.getFontSize() + ", '" +
                        s.getFontFamily() + "', '" + s.getFontStyle() + "', '" + s.getFontColor() + "', " +
                        is_hidden + ", " + is_movable + ", NULL);");
            }
        }
    }

    // encode the HashMap to String, in the format of
    // shortImgName1=longImgName1;shortImgName2=longImgName2;...
    private static String hashMapStringEncoder(HashMap<String, String> hashMap) {
        if (hashMap.size() == 0) {
            return "None";
        }

        StringBuilder encoder = new StringBuilder();

        for (String key : hashMap.keySet()) {
            encoder.append(key).append("=").append(hashMap.get(key)).append(";");
        }
        encoder.delete(encoder.length() - 1, encoder.length());
        return encoder.toString();
    }

    private static HashMap<String, String> hashMapStringDecoder(String hashMapStr) {
        HashMap<String, String> hashMap = new HashMap<>();
        if (hashMapStr.equals("None")) {
            return hashMap;
        }

        String[] parts = hashMapStr.split(";");
        for (String part : parts) {
            String[] pair = part.split("=");
            hashMap.put(pair[0], pair[1]);
        }
        return hashMap;
    }

    static Game load_game(SQLiteDatabase db, String game_name) {
        // read from database
        // constructor could assign start_page
        Page start_page = new Page(get_game_start_page(db, game_name));
        HashMap<String, String> hashMap = get_game_hashmap(db, game_name);
        Game game = new Game(game_name, start_page);

        game.setShortenImageName(hashMap);

        ArrayList<Shape> shapes = get_game_shapes(db, game_name);
        for (Shape shape : shapes) {
            String parent_page_name = shape.getPageName();

            // find page in <ArrayList> game.pages
            Page found_parent_page = null;
            for (Page p : game.getPages()) {
                if (p.getName().toLowerCase().equals(parent_page_name.toLowerCase())) {
                    found_parent_page = p;
                }
            }
            // create new page for the page we just meet
            if (found_parent_page == null) {
                found_parent_page = new Page(parent_page_name);
                game.addPage(found_parent_page);
            }
            // add this shape to its parent page
            found_parent_page.addShape(shape);
        }
        return game;
    }

    static void del_game(SQLiteDatabase db, String game_name) {
        if (get_game_names(db).contains(game_name) && !game_name.equals("example")) {
            db.execSQL("DELETE FROM allGames WHERE gameName = '" + game_name + "';");
            db.execSQL("DROP TABLE IF EXISTS \"" + game_name + "\";");
        }
    }

    static void del_all_games(SQLiteDatabase db) {
        for (String name : get_game_names(db)) {
            del_game(db, name);
        }

        // TODO You may need uncomment and drop table once
        // db.execSQL("DROP TABLE IF EXISTS allGames");
    }

    static String export_json(SQLiteDatabase db, Game game) {
        save_game(db, game);

        String game_name = game.getName();
        JSONArray resultSet = new JSONArray();
        JSONObject rowObject = new JSONObject();

        // add information of start page
        try {
            rowObject.put("gameName", game.getName());
            rowObject.put("startPage", game.getStartPage().getName());
            rowObject.put("imgMap", hashMapStringEncoder(game.getShortenImageName()));

        } catch (JSONException e) {
            e.printStackTrace();
        }
        resultSet.put(rowObject);

        Cursor cursor = db.rawQuery("SELECT * FROM \"" + game_name + "\";", null);
        while (cursor.moveToNext()) {
            int totalColumn = cursor.getColumnCount();
            rowObject = new JSONObject();

            for (int i = 0; i < totalColumn; i++) {
                if (cursor.getColumnName(i) != null) {
                    try {
                        String value = cursor.getString(i);
                        rowObject.put(cursor.getColumnName(i), value == null ? "" : value);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            resultSet.put(rowObject);
        }
        cursor.close();

        String resultJSON = null;
        try {
            resultJSON = resultSet.toString(4);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return resultJSON;
    }

    static Game parse_json(String json_str) {
        // read from json file
        try {
            JSONArray json_array = new JSONArray(json_str);

            // extract game name and start page name
            JSONObject info = json_array.getJSONObject(0);
            String game_name = info.getString("gameName");
            String start_page_name = info.getString("startPage");
            HashMap<String, String> hashMap = hashMapStringDecoder(info.getString("imgMap"));

            Page start_page = new Page(start_page_name);

            Game game = new Game(game_name, start_page);
            game.setShortenImageName(hashMap);

            for (int i = 1; i < json_array.length(); i++) {
                JSONObject shape_JObject = json_array.getJSONObject(i);
                float x = Float.parseFloat(shape_JObject.getString("x"));
                float y = Float.parseFloat(shape_JObject.getString("y"));
                float w = Float.parseFloat(shape_JObject.getString("w"));
                float h = Float.parseFloat(shape_JObject.getString("h"));
                String pageName = shape_JObject.getString("pageName");
                String shapeName = shape_JObject.getString("shapeName");
                String imageName = shape_JObject.getString("imageName");
                String script = shape_JObject.getString("script");
                String text = shape_JObject.getString("text");
                float fontSize = Float.parseFloat(shape_JObject.getString("fontSize"));
                String fontFamily = shape_JObject.getString("fontFamily");
                String fontStyle = shape_JObject.getString("fontStyle");
                String fontColor = shape_JObject.getString("fontColor");
                int isHidden = Integer.parseInt(shape_JObject.getString("isHidden"));
                int isMovable = Integer.parseInt(shape_JObject.getString("isMovable"));


                Shape shape = new Shape(x, y, w, h, pageName, shapeName, imageName, script, text,
                        fontSize, fontFamily, fontStyle, fontColor, isHidden, isMovable);

                // find page in <ArrayList> game.pages
                Page found_parent_page = null;
                for (Page p : game.getPages()) {
                    if (p.getName().toLowerCase().equals(pageName.toLowerCase())) {
                        found_parent_page = p;
                    }
                }

                // create new page for the page we just meet
                if (found_parent_page == null) {
                    found_parent_page = new Page(pageName);
                    game.addPage(found_parent_page);
                }

                // add this shape to its parent page
                found_parent_page.addShape(shape);
            }

            return game;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    static String getDefaultGameName(SQLiteDatabase db) {
        ArrayList<String> exist_game_names = get_game_names(db);
        int num = exist_game_names.size();
        String new_game_name = "game" + (num);

        int offset = 1;
        while (exist_game_names.contains(new_game_name)) {
            new_game_name = "game" + (num + offset);
            offset++;
        }
        return new_game_name;
    }
}
