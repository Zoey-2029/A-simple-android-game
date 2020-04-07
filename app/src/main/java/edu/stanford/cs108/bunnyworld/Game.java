package edu.stanford.cs108.bunnyworld;

import android.animation.Animator;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewAnimationUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Game {

    private String name;
    private Page start_page;
    // could be obtained from other activities
    private Page current_page;
    private Shape current_shape;

    // used for copy/paste/cut in EditShape
    private Shape to_be_copied_shape;

    // for undo
    private int undoStep;
    private ArrayList<ArrayList<Object>> states;

    // shorten uri of files when display
    private HashMap<String, String> shortenImageNames;

    private ArrayList<Page> pages;
    private ArrayList<Shape> possessions;
    private Set<String> pages_name_pool;
    private Set<String> shapes_name_pool;
    private boolean is_editing;


    public Game(String name) {
        this.name = name;
        this.start_page = new Page("page1");
        this.current_page = this.start_page;
        this.current_shape = null;
        this.to_be_copied_shape = null;

        this.states = new ArrayList<ArrayList<Object>>();
        this.undoStep = 0;

        shortenImageNames = new HashMap<>();

        this.pages = new ArrayList<Page>();
        this.pages.add(start_page);

        this.possessions = new ArrayList<Shape>();
        this.is_editing = true;

        this.pages_name_pool = new HashSet<>();
        this.pages_name_pool.add("page1");

        this.shapes_name_pool = new HashSet<>();
    }

    public Game(String name, Page start_page) {
        this(name);
        // re-assign the start_page
        this.start_page = start_page;
        this.current_page = start_page;

        this.pages = new ArrayList<Page>();
        this.pages.add(start_page);

        this.is_editing = false;

        this.pages_name_pool = new HashSet<>();
        this.pages_name_pool.add(start_page.getName());

        this.shapes_name_pool = new HashSet<>();
    }

    void draw(Canvas canvas) {
        if (current_page != null) {
            current_page.draw(canvas);
            // draw possession area when playing
            if (!is_editing) {
                // TODO: beautify possession areas
                // e.g., we could resize and put in grids like |x|x|x|...
                for (Shape shape : possessions) {
                    shape.draw(canvas);
                }
            }
        }
    }

    public void updateShapesNamePool(String old_name, String new_name) {
        shapes_name_pool.remove(old_name);
        shapes_name_pool.add(new_name);

    }

    public void updateShapesNamePool(String to_be_deleted_shape_name) {
        shapes_name_pool.remove(to_be_deleted_shape_name);
    }


    public void updatePagesNamePool(String old_name, String new_name) {
        pages_name_pool.remove(old_name);
        pages_name_pool.add(new_name);

    }

    public void updatePagesNamePool(String to_be_deleted_page_name) {
        pages_name_pool.remove(to_be_deleted_page_name);
    }

    public void updateScriptsByNameChange(String old_name, String new_name) {
        for (Shape shape : getAllShapesInGame()) {
            boolean flag = false;
            ArrayList<String> scripts = shape.getScripts();
            for (int i = 0; i < scripts.size(); i++) {
                if (scripts.get(i).contains(old_name)) {
                    flag = true;
                    scripts.set(i, scripts.get(i).replace(old_name, new_name));
                }
            }
            // re-assign trigger parts
            if (flag) {
                shape.initSplitScripts(scripts);
            }
        }
    }


    public void updateScriptsByNameDeletion(String old_name) {
        for (Shape shape : getAllShapesInGame()) {
            ArrayList<String> new_scripts = new ArrayList<>();
            for (String script : shape.getScripts()) {
                if (!script.contains(old_name)) {
                    new_scripts.add(script);
                }
            }
            shape.setScripts(new_scripts);
        }
    }

    public void updateShapePropertyByScriptDeletion(Shape shape) {
        ArrayList<String> action = shape.getAction();
        ArrayList<String> actionObject = shape.getActionObject();

        for (int i = 0; i < action.size(); i++) {
            if (action.get(i).equals("hide")) {
                String[] parts = actionObject.get(i).split("-");
                String pageName = parts[0];
                String hiddenName = parts[parts.length - 1];
                Shape hiddenShape = getPage(pageName).getShape(hiddenName);
                hiddenShape.setHidden(false);
            }
        }

    }

    public void addShortenImageName(String short_name, String long_name) {
        shortenImageNames.put(short_name, long_name);
    }

    public HashMap<String, String> getShortenImageName() {
        return shortenImageNames;
    }

    public void setShortenImageName(HashMap<String, String> hashMap) {
        shortenImageNames = hashMap;
    }

    public boolean isShapeNameDuplicate(String new_name) {
        for (String name : getShapesNamePool()) {
            if (name.toLowerCase().equals(new_name.toLowerCase()))
                return true;
        }
        return false;
    }

    public boolean isPageNameDuplicate(String new_name) {
        for (String name : getPagesNamePool()) {
            if (name.toLowerCase().equals(new_name.toLowerCase()))
                return true;
        }
        return false;
    }

    public Page getPage(String name) {
        for (Page page : pages) {
            if (page.getName().toLowerCase().equals(name.toLowerCase())) {
                return page;
            }
        }
        return null;
    }

    public ArrayList<Page> getPages() {
        return pages;
    }

    public ArrayList<Shape> getAllShapesInGame() {
        ArrayList<Shape> ans = new ArrayList<>();
        for (Page page : pages) {
            ans.addAll(page.getShapes());
        }
        return ans;
    }

    public Shape getCurShape() {
        return current_shape;
    }

    public void setCurShape(Shape shape) {
        current_shape = shape;
    }

    public Set<String> getPagesNamePool() {
        return pages_name_pool;
    }

    public Set<String> getShapesNamePool() {
        for (Page p : pages) {
            shapes_name_pool.addAll(p.getShapeNamesInPage());
        }
        return shapes_name_pool;
    }


    public Set<String> getFullShapesNamePool() {
        Set<String> shape_names_with_page_name = new HashSet<>();
        for (Shape shape : getAllShapesInGame()) {
            shape_names_with_page_name.add(shape.getFullName());

        }
        return shape_names_with_page_name;
    }

    public Shape getShapeByFullName(String full_name) {
        for (Shape shape : getAllShapesInGame()) {
            if (shape.getFullName().equals(full_name)) {
                return shape;
            }
        }
        return null;
    }

    public Set<String> getFullShapesNamePoolByImageName(String image_name) {
        Set<String> shape_names_with_page_name_by_im_name = new HashSet<>();
        for (Shape shape : getAllShapesInGame()) {
            if (shape.getReferredImageName().equals(image_name)) {
                shape_names_with_page_name_by_im_name.add(shape.getFullName());
            }

        }
        return shape_names_with_page_name_by_im_name;
    }

    public int getPageIdx(String name) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).getName().toLowerCase().equals(name.toLowerCase())) {
                return i;
            }
        }
        return -1;  // we need this function in distinguishing the first/last page
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Page getStartPage() {
        return this.start_page;
    }

    public void setStartPage(Page page) {
        this.start_page = page;
    }

    public void deletePage(int idx) {
        pages.get(idx).removeAllShapes();
        pages_name_pool.remove(pages.get(idx).getName());
        pages.remove(idx);
    }

    public void removeAllShapesInPage(Page page) {
        for (String shapeName : page.getShapeNamesInPage()) {
            shapes_name_pool.remove(shapeName);
        }
        page.removeAllShapes();
    }

    public void addPage(Page page) {
        this.pages.add(page);
        this.pages_name_pool.add(page.getName());
    }

    public boolean isEditing() {
        return is_editing;
    }

    public void setEditing(boolean is_editing) {
        this.is_editing = is_editing;
    }

    public String setDefaultPageName() {
        int num = pages.size();
        StringBuilder defaultName = new StringBuilder("page");
        defaultName.append(Integer.valueOf(num + 1));
        String name = defaultName.toString();

        int offset = 1;
        while (checkPageName(name)) {
            defaultName = new StringBuilder("page");
            defaultName.append(Integer.valueOf(num + 1 + offset));
            name = defaultName.toString();
            offset++;
        }
        return name;
    }

    private boolean checkPageName(String pageName) {
        for (Page page : pages) {
            String curName = page.getName().toLowerCase();
            if (curName.equals(pageName.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public Page getCurPage() {
        return current_page;
    }

    public void setCurPage(Page page) {
        current_page = page;

        // re-paint the view
        GameManager.getGameCanvas().invalidate();

        // perform onEnter actions
        Script.doOnEnter();
    }

    public void setCurPage(Page page, boolean do_animation) {
        current_page = page;

        if (do_animation) {
            circularRevealHelper();
        }

        // re-paint the view
        GameManager.getGameCanvas().invalidate();

        // perform onEnter actions
        Script.doOnEnter();
    }

    public ArrayList<Shape> getPossessions() {
        return possessions;
    }

    public Shape getPossessions(int i) {
        return possessions.get(i);
    }

    public boolean isPossessionsContain(String name) {
        for (Shape shape : possessions) {
            if (shape.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }


    public void setPossessions(ArrayList<Shape> possessions) {
        this.possessions = possessions;
    }

    public void addPossession(Shape shape) {
        this.possessions.add(shape);
    }

    public void setToBeCopiedShape(Shape shape) {
        this.to_be_copied_shape = shape.deepCopy();
    }

    public Shape getToBeCopiedShape() {
        return this.to_be_copied_shape;
    }

    // for undo
    public ArrayList<ArrayList<Object>> getStates() {
        return states;
    }

    // have maximum states limit, can only undo/redo 20 steps
    public void addState(ArrayList<Shape> oldShapes, ArrayList<Shape> newShapes) {
        if (undoStep != states.size()) {
            for (int i = states.size() - 1; i >= undoStep; i--) {
                states.remove(i);
            }
        }
        if (states.size() == 20) {
            states.remove(0);
            undoStep--;
        }
        ArrayList<Object> arr = new ArrayList<>();
        arr.add(this.getPageIdx(current_page.getName().toLowerCase()));
        arr.add(oldShapes);
        arr.add(newShapes);
        states.add(arr);
        undoStep++;
    }

    public int getStep() {
        return this.undoStep;
    }

    public void changeStep(int step) {
        this.undoStep = step;
    }

    public void clearStates() {
        for (int i = states.size() - 1; i >= 0; i--) {
            states.remove(i);
        }
        undoStep = 0;
    }


    private void circularRevealHelper() {
        View mView = GameManager.getGameCanvas();
        mView.setVisibility(View.INVISIBLE);

        // get the center for the clipping circle
        int cx = GameManager.getGameCanvas().getWidth() / 2;
        int cy = GameManager.getGameCanvas().getHeight() / 2;

        // get the final radius for the clipping circle
        float finalRadius = (float) Math.hypot(cx, cy);

        // create the animator for this view (the start radius is zero)
        Animator anim = ViewAnimationUtils.createCircularReveal(mView, cx, cy, 0f, finalRadius);

        // make the view visible and start the animation
        mView.setVisibility(View.VISIBLE);
        anim.start();
    }

    // to load each page in our game, we put invisible shape in each page
    public void addDummyShapeInEachPageHelper() {
        int i = 0;
        for (Page page : getPages()) {
            Shape dummy_shape = new Shape("dummy-" + i, page.getName(), 0, 0);
            dummy_shape.setBoundingBox(new RectF(0f, 0f, 0f, 0f));
            dummy_shape.setHidden(true);
            page.addShape(dummy_shape);
            i++;
        }
    }
}
