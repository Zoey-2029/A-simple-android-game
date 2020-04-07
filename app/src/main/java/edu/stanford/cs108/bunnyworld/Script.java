/*
 * Helper functions of scripts
 */
package edu.stanford.cs108.bunnyworld;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.util.HashSet;

class Script {
    private static MediaPlayer mp;

    static void doAction(Shape shape, int position) {
        GameCanvas gameCanvas = GameManager.getGameCanvas();
        Context context = gameCanvas.getContext();
        Game game = GameManager.getCurGame();

        if (game.isEditing()) {
            return;
        }

        String action = shape.getAction(position);
        String actionObject = shape.getActionObject(position);

        if (action.equals("play")) {
            stopPlayingHelper();

            if (actionObject.startsWith("content://")) {
                mp = new MediaPlayer();
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);

                try {
                    mp.setDataSource(context, Uri.parse(actionObject));
                } catch (IOException e) {
                    Log.e("Music DataSource Not Found Error", e.toString());
                }

                try {
                    mp.prepare();
                } catch (IOException e) {
                    stopPlayingHelper();
                }

            } else {
                int media_id = context.getResources().getIdentifier(
                        actionObject, "raw", context.getPackageName());
                mp = MediaPlayer.create(context, media_id);
            }

            try {
                mp.start();
            } catch (IllegalStateException e) {
                stopPlayingHelper();
            }

            // don't actually go to another page when editing
        } else if (action.equals("goto")) {
            for (Page currPage : game.getPages()) {
                if (currPage.getName().equals(actionObject)) {
                    game.setCurPage(currPage, true);
                    game.getCurShape().setSelected(false);
                    game.setCurShape(null);
                    break;
                }
            }
        } else if (action.equals("show")) {
            for (Shape currShape : game.getAllShapesInGame()) {
                String shape_full_name = currShape.getFullName();
                //  use brackets to include `break` inside for early quit
                if (shape_full_name.equals(actionObject)) {
                    currShape.setHidden(false);
                    break;
                }
            }
        } else if (action.equals("hide")) {
            HashSet<Shape> allShapes = new HashSet<>();
            allShapes.addAll(game.getPossessions());
            allShapes.addAll(game.getAllShapesInGame());

            for (Shape currShape : allShapes) {
                String shape_full_name = currShape.getFullName();
                if (shape_full_name.equals(actionObject)) {
                    currShape.setHidden(true);
                    break;
                }
            }
            // only show combine effect when playing
        } /*else if (action.equals("combine")) {
            for (Shape currShape : game.getCurPage().getShapes()) {
                Shape mergeShape = gameCanvas.getMergeShape(currShape);
                System.out.println(mergeShape.getName());
                System.out.println(mergeShape);
                *//*String shape_full_name = game.getCurPage().getName() + "-" + mergeShape.getName();*//*
                if (mergeShape.equals(actionObject)) {
                    gameCanvas.combineShapes();
                    *//*currShape.setCombined(true);*//*
                    break;
                }
            }
        }*/
    }

    // deal with on enter when switching pages
    static void doOnEnter() {
        Game game = GameManager.getCurGame();
        for (Shape shape : game.getCurPage().getShapes()) {
            for (int position = 0; position < shape.getTrigger().size(); position++) {
                if (shape.getTrigger(position).equals("on enter")) {
                    doAction(shape, position);
                }
            }
        }
    }

    // Ref: https://stackoverflow.com/questions/48325963/e-mediaplayer-error-1-19-e-mediaplayer-error-0-38
    // Ref: https://stackoverflow.com/questions/9609479/android-mediaplayer-went-away-with-unhandled-events
    static private void stopPlayingHelper() {
        if (mp != null) {
            if (mp.isPlaying())
                mp.stop();
            mp.reset();
            mp.release();

            mp = null;
        }
    }


}
