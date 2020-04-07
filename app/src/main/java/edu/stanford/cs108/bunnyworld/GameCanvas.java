package edu.stanford.cs108.bunnyworld;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.core.view.GestureDetectorCompat;


public class GameCanvas extends View {

    private static Game game;
    private View mView;

    private float selected_x, selected_y;
    private float xDistance, yDistance;

    private int shape_in_canvas_idx, shape_in_possession_idx;

    // for backup
    private ArrayList<Shape> oldShapes;
    private float distance, xPrec, yPrec;

    private GestureDetectorCompat mDetector;

    private static Paint possession_black_delimiter;
    private static float NON_POSSESSION_AREA_PORTION = 0.75f;
    private static float IMAGE_TOP_SHIFT_IN_POSSESSION_AREA = 10;
    private boolean is_up = false;

    private String readyToResize;
    private float curLeft, curRight, curTop, curBottom;
    private Shape merge_shape, cur_shape;
    private RectF originalShapeBB = new RectF();

    public GameCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);

        game = GameManager.getCurGame();
        mView = this;

        mDetector = new GestureDetectorCompat(getContext(), new MyGestureListener());

        // we only draw the possession delimiter when playing
        // otherwise, we have the delimiter line defined on `activity_edit.xml`
        possession_black_delimiter = new Paint();
        possession_black_delimiter.setColor(Color.BLACK);
        possession_black_delimiter.setStrokeWidth(7);
        possession_black_delimiter.setAlpha(200);

        shape_in_canvas_idx = -1;
        shape_in_possession_idx = -1;

        merge_shape = null;
        cur_shape = null;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the possession area boundary line
        if (!game.isEditing() && this.getVisibility() == View.VISIBLE) {
            canvas.drawLine(0.0f, Math.round(NON_POSSESSION_AREA_PORTION * getHeight()),
                    getWidth(), Math.round(NON_POSSESSION_AREA_PORTION * getHeight()),
                    possession_black_delimiter);
        }
        // draw current game
        game.draw(canvas);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (game.isEditing()) {
            mDetector.onTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                ActionDownHelper(event);
                break;
            case MotionEvent.ACTION_MOVE:
                ActionMoveHelper(event);
                break;
            case MotionEvent.ACTION_UP:
                is_up = true;
                ActionUpHelper(event);
                is_up = false;
                break;
        }
        return true;

    }

    private void ActionDownHelper(MotionEvent event) {
        // init the unselected state for all shapes
        Page cur_page = game.getCurPage();
        cur_shape = game.getCurShape();

        for (Shape shape : cur_page.getShapes()) {
            shape.setSelected(false);
            shape.setOnDrop(false);
        }
        for (Shape shape : game.getPossessions()) {
            shape.setSelected(false);
            shape.setOnDrop(false);
        }

        selected_x = event.getX();
        selected_y = event.getY();

        distance = 0;
        xPrec = selected_x;
        yPrec = selected_y;

        shape_in_canvas_idx = findSelectedShapeIndex(cur_page.getShapes());
        shape_in_possession_idx = findSelectedShapeIndex(game.getPossessions());

        if (shape_in_canvas_idx != -1) {
            // save old shapes
            oldShapes = game.getCurPage().copyShapes();

            Shape newly_selected_shape = cur_page.getShapes().get(shape_in_canvas_idx);
            newly_selected_shape.setSelected(true);
            originalShapeBB = newly_selected_shape.getBoundingBox();

            // bring to the top
            cur_page.removeShape(shape_in_canvas_idx);
            cur_page.addShape(newly_selected_shape);

            game.setCurShape(newly_selected_shape);

            for (int position = 0; position < newly_selected_shape.getTrigger().size(); position++) {
                if (newly_selected_shape.getTrigger(position).equals("on click")) {
                    Script.doAction(newly_selected_shape, position);
                }
            }

            xDistance = selected_x - newly_selected_shape.getBoundingBox().left;
            yDistance = selected_y - newly_selected_shape.getBoundingBox().top;

            curLeft = newly_selected_shape.getBoundingBox().left;
            curTop = newly_selected_shape.getBoundingBox().top;
            curRight = newly_selected_shape.getBoundingBox().right;
            curBottom = newly_selected_shape.getBoundingBox().bottom;
            readyToResize = newly_selected_shape.readyToResize(selected_x, selected_y);

        } else if (shape_in_possession_idx != -1 && !game.isEditing()) {
            Shape newly_selected_possession = game.getPossessions(shape_in_possession_idx);
            newly_selected_possession.setSelected(true);
            originalShapeBB = newly_selected_possession.getBoundingBox();

            game.setCurShape(newly_selected_possession);
        } else {
            game.setCurShape(null);
            if (game.isEditing()) {
                // if we tap the screen outside shapes
                // editing_shape should be unselected to prevent any shape editing
                EditActivity.display_shape_name.setText("None Shape");
            }
        }

        invalidate();
    }


    // Detect OnDrop when dragging
    private void ActionMoveHelper(MotionEvent event) {
        cur_shape = game.getCurShape();

        if (cur_shape == null || (!game.isEditing() && !cur_shape.isMovable())) {
            return;
        }

        selected_x = Math.max(0, event.getX());
        selected_y = Math.max(0, event.getY());

        final float dx = selected_x - xPrec;
        final float dy = selected_y - yPrec;
        final float dl = (float) Math.sqrt(dx * dx + dy * dy);
        distance += dl;
        xPrec = selected_x;
        yPrec = selected_y;

        float rectWidth = cur_shape.getBoundingBox().width();
        float rectHeight = cur_shape.getBoundingBox().height();

        if (!game.isEditing() || readyToResize.equals("none")) {
            cur_shape.setBoundingBox(new RectF(selected_x - xDistance, selected_y - yDistance,
                    selected_x - xDistance + rectWidth, selected_y - yDistance + rectHeight));
        } else {
            switch (readyToResize) {
                case "top-left":
                    cur_shape.setBoundingBox(new RectF(Math.min(selected_x, curRight), Math.min(selected_y, curBottom),
                            Math.max(selected_x, curRight), Math.max(selected_y, curBottom)));
                    break;
                case "top-right":
                    cur_shape.setBoundingBox(new RectF(Math.min(selected_x, curLeft), Math.min(selected_y, curBottom),
                            Math.max(selected_x, curLeft), Math.max(selected_y, curBottom)));
                    break;
                case "bottom-left":
                    cur_shape.setBoundingBox(new RectF(Math.min(selected_x, curRight), Math.min(selected_y, curTop),
                            Math.max(selected_x, curRight), Math.max(selected_y, curTop)));
                    break;
                case "bottom-right":
                    cur_shape.setBoundingBox(new RectF(Math.min(selected_x, curLeft), Math.min(selected_y, curTop),
                            Math.max(selected_x, curLeft), Math.max(selected_y, curTop)));
                    break;
            }
        }

        // **revisit** the shape position if they are out of bounds
        reviseOutOfBoundsShape(cur_shape);

        showOnDropRect();

        invalidate();
    }

    private void ActionUpHelper(MotionEvent event) {
        cur_shape = game.getCurShape();
        if (cur_shape == null) {
            return;
        }

        // update next planned coordinates
        game.getCurPage().updateNextShapeX(cur_shape);
        game.getCurPage().updateNextShapeY(cur_shape);

        // update display shape name in editor
        if (game.isEditing()) {
            EditActivity.display_shape_name.setText(cur_shape.getName());
        }

        selected_x = event.getX();
        selected_y = event.getY();

        RectF bb = cur_shape.getBoundingBox();
        float pre_left = bb.left;
        float pre_right = bb.right;
        float pre_top = bb.top;
        float pre_bottom = bb.bottom;
        float pre_height = bb.height();

        xDistance = selected_x - bb.left;
        yDistance = selected_y - bb.top;

        // **revisit** the shape position if they are out of bounds
        reviseOutOfBoundsShape(cur_shape);

        // we also need to determine the possession area when playing
        if (!game.isEditing()) {
            // possession area boundary line
            float boundaryLine = Math.round(getHeight() * NON_POSSESSION_AREA_PORTION);

            // if in between
            // closer to canvas area: redraw on the canvas
            if (pre_bottom > boundaryLine &&
                    pre_top < boundaryLine &&
                    pre_bottom - boundaryLine < boundaryLine - pre_top) {
                cur_shape.setBoundingBox(new RectF(pre_left, boundaryLine - pre_height, pre_right, boundaryLine));
            }

            // closer to possession area: put into the possession area and resize to smaller size
            // or we drag a huge shape to the bottom of the canvas
            if (pre_bottom > boundaryLine &&
                    pre_top < boundaryLine &&
                    pre_bottom - boundaryLine >= boundaryLine - pre_top
                    || pre_bottom >= getHeight() || pre_bottom <= getHeight() && pre_top >= boundaryLine) {

                // shift by 10 units to avoid lines collide
                cur_shape.setBoundingBox(new RectF(pre_left, boundaryLine + IMAGE_TOP_SHIFT_IN_POSSESSION_AREA,
                        pre_right, boundaryLine + IMAGE_TOP_SHIFT_IN_POSSESSION_AREA + pre_height));

                // avoid add the same shape
                if (!game.isPossessionsContain(cur_shape.getName())) {
                    cur_shape.setPossession(true);
                    game.getCurPage().removeShape(cur_shape);
                    game.addPossession(cur_shape);
                }

            } else {
                // outside the possession area
                if (game.isPossessionsContain(cur_shape.getName())) {
                    cur_shape.setPossession(false);
                    game.getPossessions().remove(cur_shape);
                    game.getCurPage().addShape(cur_shape);
                }
            }

            combineShapes();
        }

        showOnDropRect();
        invalidate();

        if (distance != 0) {
            ArrayList<Shape> newShapes = game.getCurPage().copyShapes();
            game.addState(oldShapes, newShapes);
        }
    }


    public void combineShapes() {
        cur_shape = game.getCurShape();
        Page cur_page = game.getCurPage();
        if(cur_shape == null) {return;}
        merge_shape = getMergeShape(cur_shape);
        if(merge_shape == null) {return;}
        for (int position = 0; position < merge_shape.getTrigger().size(); position++) {
            if (merge_shape.getTrigger(position).equals("on drop")) {
                if (merge_shape.getAction(position).equals("combine")) {
                    // duck + fire
                    if ((cur_shape.getReferredImageName().equals("duck")
                            && merge_shape.getReferredImageName().equals("fire")) |
                            (cur_shape.getReferredImageName().equals("fire")
                                    && merge_shape.getReferredImageName().equals("duck"))) {
                        cur_shape.setReferredImageName("roast_duck");
                        cur_page.removeShape(merge_shape);
                    }
                    // bunny + carrot1
                    if ((cur_shape.getReferredImageName().equals("bunny")
                            && merge_shape.getReferredImageName().equals("carrot1")) |
                            (cur_shape.getReferredImageName().equals("carrot1")
                                    && merge_shape.getReferredImageName().equals("bunny"))) {
                        cur_shape.setReferredImageName("bunny_with_carrot1");
                        cur_page.removeShape(merge_shape);
                    }
                    // bunny + carrot2
                    if ((cur_shape.getReferredImageName().equals("bunny")
                            && merge_shape.getReferredImageName().equals("carrot2")) |
                            (cur_shape.getReferredImageName().equals("carrot2")
                                    && merge_shape.getReferredImageName().equals("bunny"))) {
                        cur_shape.setReferredImageName("bunny_with_carrot2");
                        cur_page.removeShape(merge_shape);
                    }
                }
            }
        }
    }
    

    public Shape getMergeShape(Shape shape) {
        Page cur_page = game.getCurPage();
        for (Shape mergeShape : cur_page.getShapes()) {
            if (mergeShape != shape && mergeShape.getBoundingBox().contains(selected_x, selected_y)) {
                return mergeShape;
            }
        }
        return null;
    }



    // Detect and show the ondrop green rectangle if applied
    private void showOnDropRect() {
        if(game.isEditing()) {return;}
        Page cur_page = game.getCurPage();
        cur_shape = game.getCurShape();
        float cur_left = cur_shape.getBoundingBox().left;
        float cur_right = cur_shape.getBoundingBox().right;
        float cur_top = cur_shape.getBoundingBox().top;
        float cur_bottom = cur_shape.getBoundingBox().bottom;
        float cur_width = cur_right - cur_left;
        float cur_height = cur_bottom - cur_top;


        for(Shape potential_ondrop_shape : cur_page.getShapes()){
            System.out.println(potential_ondrop_shape.getTrigger());
            System.out.println(potential_ondrop_shape.getTriggerShape());
            if(potential_ondrop_shape == cur_shape) {continue;}
            RectF larger_bb = new RectF(potential_ondrop_shape.getBoundingBox());
            int ondrop_count = 0;
            for(int position = 0; position < potential_ondrop_shape.getTrigger().size(); position ++){
                if(potential_ondrop_shape.getTrigger().get(position).equals("on drop")){
                    String curshape_full_name = cur_shape.getFullName();
                    if(potential_ondrop_shape.getTriggerShape().get(position).equals(curshape_full_name)){
                        potential_ondrop_shape.setOnDrop(true);
                        // Enlarge the OnDrop rectangle by 1.5, (1/4 = (1.5 - 1) / 2)
                        larger_bb.inset(-potential_ondrop_shape.getBoundingBox().width() / 4,
                                -potential_ondrop_shape.getBoundingBox().height() / 4);
                        ondrop_count ++;
                        if(larger_bb.contains(selected_x, selected_y)){
                            if (is_up) {
                                Script.doAction(potential_ondrop_shape, position);
                            }
                        }
                    }
                }
            }
            if(ondrop_count == 0){
                if (potential_ondrop_shape != cur_shape) {
                    if ((cur_bottom >= larger_bb.top && cur_bottom <= larger_bb.bottom + cur_height
                            && cur_right >= larger_bb.left && cur_right <= larger_bb.right + cur_width)) {
                        cur_shape.setBoundingBox(originalShapeBB);
                    }
                }
            }
        }
    }

    // Boundary check: avoid shapes out of canvas
    private void reviseOutOfBoundsShape(Shape bad_shape) {
        RectF bb = bad_shape.getBoundingBox();

        if (bb.left < 0) {
            bad_shape.setBoundingBox(new RectF(0, bb.top, bb.width(), bb.bottom));
        }

        if (bb.right > getWidth()) {
            bad_shape.setBoundingBox(new RectF(getWidth() - bb.width(), bb.top, getWidth(), bb.bottom));
        }

        if (bb.top < 0) {
            bad_shape.setBoundingBox(new RectF(bb.left, 0, bb.right, bb.height()));
        }

        if (bb.bottom > getHeight()) {
            bad_shape.setBoundingBox(new RectF(bb.left, getHeight() - bb.height(), bb.right, getHeight()));
        }

    }

    private int findSelectedShapeIndex(ArrayList<Shape> shapes) {
        int idx = -1;

        for (int i = shapes.size() - 1; i >= 0; i--) {
            Shape sp = shapes.get(i);
            RectF bb_temp = new RectF(sp.getBoundingBox());
            if (sp.isPossession()) {
                bb_temp.bottom = bb_temp.top + Shape.DEFAULT_HEIGHT;
                bb_temp.right = bb_temp.left + Shape.DEFAULT_WIDTH;
            }

            if (bb_temp.contains(selected_x, selected_y)) {
                return i;
            }
        }
        return idx;
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent e) {
            showPopupMenu(GameManager.getGameCanvas());
        }
    }


    @SuppressLint("RestrictedApi")
    private void showPopupMenu(View view) {
        // Ref: https://stackoverflow.com/questions/6805756/is-it-possible-to-display-icons-in-a-popupmenu/20094711
        MenuBuilder menuBuilder = new MenuBuilder(mView.getContext());
        MenuInflater inflater = new MenuInflater(mView.getContext());
        inflater.inflate(R.menu.edit_shape_popup, menuBuilder);
        MenuPopupHelper popupMenu = new MenuPopupHelper(mView.getContext(), menuBuilder, view,
                true, R.attr.actionOverflowMenuStyle, 0);
        popupMenu.setForceShowIcon(true);
        popupMenu.setGravity(Gravity.END);

        // Set Item Click Listener
        menuBuilder.setCallback(new MenuBuilder.Callback() {
            @Override
            public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.cut:
                        copyShape(game.getCurShape());
                        deleteShape();
                        break;
                    case R.id.copy:
                        copyShape(game.getCurShape());
                        break;
                    case R.id.paste:
                        pasteShape();
                        break;
                    case R.id.del:
                        deleteShape();
                        break;
                }

                mView.invalidate();
                return true;
            }

            @Override
            public void onMenuModeChange(MenuBuilder menu) {
            }
        });

        popupMenu.show();
    }

    private void deleteShape() {
        if (game.getCurShape() == null) {
            Toast.makeText(this.getContext(), "No shape is selected",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Shape to_be_removed = game.getCurShape();
        Page cur_page = game.getCurPage();

        oldShapes = cur_page.copyShapes();

        cur_page.removeShape(to_be_removed);
        game.setCurShape(null);

        game.updateShapesNamePool(to_be_removed.getName());
        game.updateScriptsByNameDeletion(to_be_removed.getFullName());

        EditActivity.display_shape_name.setText("None Shape");


        // restore the pages's next_shape_x/y to the current position
        RectF bb = to_be_removed.getBoundingBox();
        cur_page.setNextShapeX(bb.left);
        cur_page.setNextShapeY(bb.top);

        // add to states
        ArrayList<Shape> newShapes = cur_page.copyShapes();
        game.addState(oldShapes, newShapes);
    }

    private void copyShape(Shape shape) {
        // if we set reference here when copy,
        // we may paste a different shape if the copied shape is changed
        // good point!

        game.setToBeCopiedShape(shape);
        GameManager.setToBeCopiedShapeInGame(shape);
    }

    private void pasteShape() {
        Shape to_be_copied = game.getToBeCopiedShape();
        Shape to_be_copied_in_game = GameManager.getToBeCopiedShapeInGame();

        if (to_be_copied == null && to_be_copied_in_game == null) {
            Toast.makeText(mView.getContext(), "Please copy a shape first!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Shape copied;
        if (to_be_copied == null) {
            copied = to_be_copied_in_game.deepCopy();
        } else {
            copied = to_be_copied.deepCopy();
        }

        // we already copied that in `game.setToBeCopiedShape` method
        Page cur_page = game.getCurPage();
        oldShapes = cur_page.copyShapes();

        // set a different shape/page name if applied (paste into different page)
        String old_full_name = copied.getFullName();

        copied.setName(cur_page.getDefaultNewShapeName());
        copied.setPageName(cur_page.getName());

        String new_full_name = copied.getFullName();

        // update script by name changing
        ArrayList<String> scripts = copied.getScripts();
        for (int i = 0; i < scripts.size(); i++) {
            scripts.set(i, scripts.get(i).replace(old_full_name, new_full_name));
        }

        RectF bb = copied.getBoundingBox();
        // if we select on a shape, then generate next position as usual
        // otherwise if we click on the blank area, we then use the `selected` position as the center
        float x = shape_in_canvas_idx == -1 ? selected_x - bb.width() / 2 : cur_page.getNextShapeX();
        float y = shape_in_canvas_idx == -1 ? selected_y - bb.height() / 2 : cur_page.getNextShapeY();
        copied.setBoundingBox(new RectF(x, y, x + bb.width(), y + bb.height()));

        cur_page.addShape(copied);
        game.setCurShape(copied);

        cur_page.updateNextShapeX(copied);
        cur_page.updateNextShapeY(copied);

        // add to states
        ArrayList<Shape> newShapes = cur_page.copyShapes();
        game.addState(oldShapes, newShapes);

        EditActivity.display_shape_name.setText(copied.getName());
    }


}
