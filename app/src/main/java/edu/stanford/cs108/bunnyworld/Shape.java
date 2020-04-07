package edu.stanford.cs108.bunnyworld;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.IOException;
import java.util.ArrayList;

public class Shape {
    // May be can set as private and access with bunch of getter/setter
    private String shape_name;
    private String page_name;

    private RectF bounding_box;
    private String referred_image_name = "";
    private String referred_text = "";

    private boolean is_hidden = false;
    private boolean is_movable = false;

    private boolean is_selected = false;
    private boolean is_ondrop = false;
    private boolean is_possession = false;

    static float DEFAULT_FONT_SIZE = 50;
    private float fontSize;

    private String fontFamily, fontStyle, fontColor;
    private ArrayList<String> scripts, trigger, triggerShape, action, actionObject;

    private Rect textBounds;
    private Paint text_paint;


    // Preload Painting
    private static Paint rectangle_gray_fill, alpha_rectangle_gray_fill, alpha_bitmap_paint;
    private static Paint ondrop_green_stroke, selected_blue_stroke;

    private static Typeface default_normal, default_italic, default_bold, default_bold_italic;
    private static Typeface sans_normal, sans_italic, sans_bold, sans_bold_italic;
    private static Typeface mono_normal, mono_italic, mono_bold, mono_bold_italic;
    private static Typeface serif_normal, serif_italic, serif_bold, serif_bold_italic;
    private static final int DEFAULT_ALPHA = 70;
    private final float resizeRange = 25;

    static float DEFAULT_WIDTH = 200;
    static float DEFAULT_HEIGHT = 200;


    Shape(String name, String page_name, float x, float y) {
        initPainting();
        this.shape_name = name.trim();
        this.page_name = page_name;
        this.bounding_box = new RectF(x, y, x + DEFAULT_WIDTH, y + DEFAULT_HEIGHT);

        scripts = new ArrayList<String>();

        fontSize = DEFAULT_FONT_SIZE;
        fontFamily = "default";
        fontStyle = "normal";
        fontColor = "black";
        trigger = new ArrayList<String>();
        triggerShape = new ArrayList<String>();
        action = new ArrayList<String>();
        actionObject = new ArrayList<String>();
    }


    Shape(float x, float y, float width, float height,
          String page_name, String shape_name, String image_name, String script, String text,
          float fontSize, String fontFamily, String fontStyle, String fontColor,
          int is_hidden, int is_movable) {
        this(shape_name, page_name, x, y);

        this.bounding_box = new RectF(x, y, x + width, y + height);

        this.referred_image_name = image_name;
        this.referred_text = text;
        this.scripts = convertScriptStrToArrayList(script);

        this.fontSize = fontSize;
        this.fontFamily = fontFamily;
        this.fontStyle = fontStyle;
        this.fontColor = fontColor;

        this.is_hidden = is_hidden == 1;
        this.is_movable = is_movable == 1;

        initSplitScripts(this.scripts);
    }

    Shape(Shape other) {
        this.shape_name = other.shape_name;
        this.page_name = other.page_name;
        this.bounding_box = new RectF(other.bounding_box);
        this.referred_image_name = other.referred_image_name;
        this.referred_text = other.referred_text;

        this.scripts = new ArrayList<String>();
        this.scripts.addAll(other.scripts);

        this.fontSize = other.fontSize;
        this.fontFamily = other.fontFamily;
        this.fontStyle = other.fontStyle;
        this.fontColor = other.fontColor;

        // deep copies
        this.trigger = new ArrayList<String>();
        this.trigger.addAll(other.trigger);
        this.triggerShape = new ArrayList<String>();
        this.triggerShape.addAll(other.triggerShape);
        this.action = new ArrayList<String>();
        this.action.addAll(other.action);
        this.actionObject = new ArrayList<String>();
        this.actionObject.addAll(other.actionObject);

        this.is_hidden = other.is_hidden;
        this.is_movable = other.is_movable;

        this.text_paint = new Paint(other.text_paint);
        this.textBounds = new Rect(other.textBounds);
    }

    private void initPainting() {
        // init pre-defined drawing
        rectangle_gray_fill = new Paint();
        rectangle_gray_fill.setColor(Color.LTGRAY);
        rectangle_gray_fill.setStyle(Paint.Style.FILL);

        text_paint = new Paint();
        alpha_bitmap_paint = new Paint();
        alpha_bitmap_paint.setAlpha(DEFAULT_ALPHA);

        alpha_rectangle_gray_fill = new Paint();
        alpha_rectangle_gray_fill.setStyle(Paint.Style.FILL);
        alpha_rectangle_gray_fill.setColor(Color.LTGRAY);
        alpha_rectangle_gray_fill.setAlpha(DEFAULT_ALPHA);

        selected_blue_stroke = new Paint();
        selected_blue_stroke.setColor(Color.BLUE);
        selected_blue_stroke.setStyle(Paint.Style.STROKE);
        selected_blue_stroke.setStrokeWidth(10);

        ondrop_green_stroke = new Paint();
        ondrop_green_stroke.setColor(Color.GREEN);
        ondrop_green_stroke.setStyle(Paint.Style.STROKE);
        ondrop_green_stroke.setStrokeWidth(10);

        default_normal = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL);
        default_bold = Typeface.create(Typeface.DEFAULT, Typeface.BOLD);
        default_italic = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC);
        default_bold_italic = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC);

        sans_normal = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
        sans_bold = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
        sans_italic = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC);
        sans_bold_italic = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC);

        mono_normal = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
        mono_bold = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
        mono_italic = Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC);
        mono_bold_italic = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD_ITALIC);

        serif_normal = Typeface.create(Typeface.SERIF, Typeface.NORMAL);
        serif_bold = Typeface.create(Typeface.SERIF, Typeface.BOLD);
        serif_italic = Typeface.create(Typeface.SERIF, Typeface.ITALIC);
        serif_bold_italic = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC);

        textBounds = new Rect();
    }

    private ArrayList<String> convertScriptStrToArrayList(String script_str) {
        ArrayList<String> scripts = new ArrayList<>();
        for (String s : script_str.split(";")) {
            if (!s.trim().isEmpty()) {
                scripts.add(s);
            }
        }
        return scripts;
    }

    public void initSplitScripts(ArrayList<String> scripts) {
        // Init trigger, triggerShape, action, actionObject from script
        trigger = new ArrayList<String>();
        triggerShape = new ArrayList<String>();
        action = new ArrayList<String>();
        actionObject = new ArrayList<String>();

        for (String script : scripts) {
            String[] phrases = script.trim().split(" ");

            // illegals script when phrase size less than 4
            if (phrases.length < 4) {
                return;
            }
            String trigger_str = String.join(" ", phrases[0], phrases[1]);
            trigger.add(trigger_str);

            String shape_str = "", action_str = "", action_obj_str = "";
            if (phrases.length == 4) {
                shape_str = "NO SHAPE";
                action_str = phrases[2];
                action_obj_str = phrases[3];

            } else if (phrases.length == 5) {
                shape_str = phrases[2];
                action_str = phrases[3];
                action_obj_str = phrases[4];
            }
            triggerShape.add(shape_str);
            action.add(action_str);
            actionObject.add(action_obj_str);
        }
    }

    public void draw(Canvas canvas) {
        // still draw hidden shapes if is editing
        Game game = GameManager.getCurGame();
        if (!is_hidden || game.isEditing()) {
            // text
            if (!this.referred_text.isEmpty()) {
                switch (fontColor) {
                    case "black":
                        text_paint.setColor(Color.BLACK);
                        break;
                    case "red":
                        text_paint.setColor(Color.RED);
                        break;
                    case "blue":
                        text_paint.setColor(Color.BLUE);
                        break;
                    default:
                        text_paint.setColor(Color.YELLOW);
                        break;
                }

                if (fontFamily.equals("default")) {
                    if (fontStyle.equals("normal")) text_paint.setTypeface(default_normal);
                    else if (fontStyle.equals("italic")) text_paint.setTypeface(default_italic);
                    else if (fontStyle.equals("bold")) text_paint.setTypeface(default_bold);
                    else text_paint.setTypeface(default_bold_italic);
                } else if (fontFamily.equals("sans serif")) {
                    if (fontStyle.equals("normal")) text_paint.setTypeface(sans_normal);
                    else if (fontStyle.equals("italic")) text_paint.setTypeface(sans_italic);
                    else if (fontStyle.equals("bold")) text_paint.setTypeface(sans_bold);
                    else text_paint.setTypeface(sans_bold_italic);
                } else if (fontFamily.equals("monospace")) {
                    if (fontStyle.equals("normal")) text_paint.setTypeface(mono_normal);
                    else if (fontStyle.equals("italic")) text_paint.setTypeface(mono_italic);
                    else if (fontStyle.equals("bold")) text_paint.setTypeface(mono_bold);
                    else text_paint.setTypeface(mono_bold_italic);
                } else {
                    if (fontStyle.equals("normal")) text_paint.setTypeface(serif_normal);
                    else if (fontStyle.equals("italic")) text_paint.setTypeface(serif_italic);
                    else if (fontStyle.equals("bold")) text_paint.setTypeface(serif_bold);
                    else text_paint.setTypeface(serif_bold_italic);
                }

                text_paint.setTextSize(fontSize);
                text_paint.getTextBounds(referred_text, 0, referred_text.length(), textBounds);

                // origin of the sentence
                bounding_box.right = bounding_box.left + textBounds.width();
                bounding_box.bottom = bounding_box.top + fontSize;

                float x = bounding_box.left;
                float y = bounding_box.bottom;
                canvas.drawText(referred_text, 0, referred_text.length(), x, y, text_paint);

            } else if (!this.referred_image_name.isEmpty()) {
                if (!referred_image_name.equals("other")) {
                    Context context = GameManager.getGameCanvas().getContext();
                    Bitmap bitmap = null;

                    // see if we use the shorten name
                    String real_image_name = referred_image_name;
                    if (game.getShortenImageName().containsKey(referred_image_name)) {
                        real_image_name = game.getShortenImageName().get(referred_image_name);
                    }

                    if (real_image_name.startsWith("content://")) {
                        // we load it from photo/files
                        try {
                            Uri uri = Uri.parse(real_image_name);
                            bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // bitmaps are in the resource
                        int image_id = context.getResources().getIdentifier(real_image_name,
                                "drawable", context.getPackageName());

                        // Problem: large memory allocated
                        // Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), image_id);
                        BitmapDrawable picToDraw = (BitmapDrawable) context.getResources().getDrawable(image_id);
                        bitmap = picToDraw.getBitmap();
                    }

                    // check we do load some bitmap
                    if (bitmap == null) {
                        return;
                    }

                    if (is_hidden) {
                        canvas.drawBitmap(bitmap, null, this.bounding_box, alpha_bitmap_paint);
                    } else {
                        canvas.drawBitmap(bitmap, null, getPossessionShapeBBHelper(), null);
                    }


                } else {
                    canvas.drawRect(bounding_box, rectangle_gray_fill);
                }
            }

            RectF bb_temp = getPossessionShapeBBHelper();
            if (is_selected) {
                canvas.drawRect(bb_temp, selected_blue_stroke);
            } else if (is_ondrop) {
                RectF larger_bb = new RectF(bb_temp);
                // Enlarge the OnDrop rectangle by 1.5, (1/4 = (1.5 - 1) / 2)
                larger_bb.inset(-bb_temp.width() / 4, -bb_temp.height() / 4);
                canvas.drawRect(larger_bb, ondrop_green_stroke);
            }
        }
    }

    // change width/height for shape in possession
    private RectF getPossessionShapeBBHelper() {
        RectF bb_temp = new RectF(this.bounding_box);
        if (is_possession) {
            bb_temp.right = bb_temp.left + DEFAULT_WIDTH;
            bb_temp.bottom = bb_temp.top + DEFAULT_HEIGHT;
        }
        return bb_temp;
    }

    public String readyToResize(float x, float y) {
        if (bounding_box.left <= x && x <= bounding_box.left + resizeRange
                && bounding_box.top <= y && y <= bounding_box.top + resizeRange) {
            return "top-left";
        }
        if (bounding_box.right - resizeRange <= x && x <= bounding_box.right
                && bounding_box.top <= y && y <= bounding_box.top + resizeRange) {
            return "top-right";
        }
        if (bounding_box.left <= x && x <= bounding_box.left + resizeRange
                && bounding_box.bottom - resizeRange <= y && y <= bounding_box.bottom) {
            return "bottom-left";
        }
        if (bounding_box.right - resizeRange <= x && x <= bounding_box.right
                && bounding_box.bottom - resizeRange <= y && y <= bounding_box.bottom) {
            return "bottom-right";
        }
        return "none";
    }

    public String getName() {
        return shape_name;
    }

    public void setName(String shape_name) {
        this.shape_name = shape_name.trim();
    }

    public String getPageName() {
        return page_name;
    }

    public void setPageName(String page_name) {
        this.page_name = page_name;
    }

    public String getFullName() {
        return page_name + "-" + shape_name;
    }

    public String getReferredImageName() {
        return referred_image_name;
    }

    public void setReferredImageName(String referred_image_name) {
        this.referred_image_name = referred_image_name;
    }

    public RectF getBoundingBox() {
        return bounding_box;
    }

    public void setBoundingBox(RectF bounding_box) {
        this.bounding_box = bounding_box;
    }

    public String getReferredText() {
        return this.referred_text;
    }

    public void setReferredText(String text) {
        this.referred_text = text;
    }

    public ArrayList<String> getScripts() {
        return this.scripts;
    }

    public void setScripts(ArrayList<String> scripts) {
        this.scripts = scripts;
    }

    public String getScriptStr() {
        return String.join(";", this.scripts);
    }

    public void addScript(String script) {
        this.scripts.add(script);
    }

    public boolean isHidden() {
        return is_hidden;
    }

    public void setHidden(boolean is_hidden) {
        this.is_hidden = is_hidden;
    }

    public boolean isMovable() {
        return is_movable;
    }

    public void setMovable(boolean is_movable) {
        this.is_movable = is_movable;
    }

    public boolean isOnDrop() {
        return is_ondrop;
    }

    public void setOnDrop(boolean is_ondrop) {
        this.is_ondrop = is_ondrop;
    }


    public boolean isPossession() {
        return is_possession;
    }

    public void setPossession(boolean is_possession) {
        this.is_possession = is_possession;
    }


    public boolean isSelected() {
        return is_selected;
    }

    public void setSelected(boolean is_selected) {
        this.is_selected = is_selected;
    }

    public float getFontSize() {
        return fontSize;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public String getFontStyle() {
        return fontStyle;
    }

    public void setFontStyle(String fontStyle) {
        this.fontStyle = fontStyle;
    }

    public String getFontColor() {
        return fontColor;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }

    public ArrayList<String> getTrigger() {
        return trigger;
    }

    public String getTrigger(int i) {
        return trigger.get(i);
    }

    public void addTrigger(String trigger) {
        this.trigger.add(trigger);
    }

    public void setTrigger(ArrayList<String> trigger) {
        this.trigger = trigger;
    }

    public ArrayList<String> getTriggerShape() {
        return triggerShape;
    }

    public String getTriggerShape(int index) {
        return triggerShape.get(index);
    }

    public void addTriggerShape(String triggerShape) {
        this.triggerShape.add(triggerShape);
    }

    public void setTriggerShape(ArrayList<String> triggerShape) {
        this.triggerShape = triggerShape;
    }

    public ArrayList<String> getAction() {
        return action;
    }

    public void setAction(ArrayList<String> action) {
        this.action = action;
    }

    public String getAction(int i) {
        return action.get(i);
    }

    public void addAction(String action) {
        this.action.add(action);
    }

    public void setActionObject(ArrayList<String> actionObject) {
        this.actionObject = actionObject;
    }

    public ArrayList<String> getActionObject() {
        return actionObject;
    }

    public String getActionObject(int i) {
        return actionObject.get(i);
    }


    public void addActionObject(String actionObject) {
        this.actionObject.add(actionObject);
    }

    public void removeScriptFieldsAtPositionHelper(int position) {
        this.trigger.remove(position);
        this.action.remove(position);
        this.triggerShape.remove(position);
        this.actionObject.remove(position);
        this.scripts.remove(position);
    }

    public void addScriptFieldsHelper(String trigger, String triggerShape, String action, String actionObject, String script) {
        this.trigger.add(trigger);
        this.triggerShape.add(triggerShape);
        this.action.add(action);
        this.actionObject.add(actionObject);
        this.scripts.add(script);
    }

    public void clearScriptFieldsHelper() {
        this.trigger.clear();
        this.triggerShape.clear();
        this.action.clear();
        this.actionObject.clear();
        this.scripts.clear();
    }

    public Shape deepCopy() {
        return new Shape(this);
    }


}
