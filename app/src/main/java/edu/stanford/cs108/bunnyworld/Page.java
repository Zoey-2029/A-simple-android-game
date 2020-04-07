package edu.stanford.cs108.bunnyworld;

import android.graphics.Canvas;
import android.graphics.RectF;

import java.util.ArrayList;

class Page {

    private String name;
    private ArrayList<Shape> shapes;

    // The next position should be changed per page
    private float init_shape_x, init_shape_y, next_shape_x, next_shape_y;

    Page(String name) {
        this.name = name.trim();
        this.shapes = new ArrayList<Shape>();

        // indicate the position of next shape
        this.init_shape_x = 0;
        this.init_shape_y = 0;
        this.next_shape_x = 0;
        this.next_shape_y = 0;
    }

    public void draw(Canvas canvas) {
        for (Shape shape : shapes) {
            shape.draw(canvas);
        }

    }

    public void initNextShapeXY(float canvas_width, float canvas_height, float width_ratio, float height_ratio) {
        init_shape_x = canvas_width * width_ratio;
        init_shape_y = canvas_height * height_ratio;

        next_shape_x = init_shape_x;
        next_shape_y = init_shape_y;
    }

    public float getNextShapeX() {
        return next_shape_x;
    }

    public float getNextShapeY() {
        return next_shape_y;
    }

    public void setNextShapeX(float x) {
        next_shape_x = x;
    }

    public void setNextShapeY(float y) {
        next_shape_y = y;
    }


    public float updateNextShapeX(Shape editing_shape) {
        // Update next_shape_x to the right of current editing shape + 10 margin
        RectF bb = editing_shape.getBoundingBox();
        next_shape_x = bb.right + 10;

        if (next_shape_x + bb.width() > GameManager.getGameCanvas().getWidth()) {
            next_shape_x = init_shape_x;
        }
        return next_shape_x;
    }

    public float updateNextShapeY(Shape editing_shape) {
        // Update next_shape_y to the bottom right of current editing shape + (height/4) margin
        RectF bb = editing_shape.getBoundingBox();
        next_shape_y = bb.top + bb.height() / 4;

        if (next_shape_y + bb.height() > GameManager.getGameCanvas().getHeight()) {
            next_shape_y = init_shape_y;
        }
        return next_shape_y;
    }


    public ArrayList<Shape> getShapes() {
        return shapes;
    }

    // get the index of shape in this page
    public int getShapeIdx(Shape shape) {
        for (int i = 0; i < shapes.size(); i++) {
            if (shape.equals(shapes.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public String getDefaultNewShapeName() {
        ArrayList<String> shape_names = getShapeNamesInPage();
        int offset = 1, num = shape_names.size();

        String new_name = "shape" + (num + 1);
        while (shape_names.contains(new_name)) {
            new_name = "shape" + (num + offset + 1);
            offset++;
        }
        return new_name;
    }

    public void addShape(Shape shape) {
        if (!shapes.contains(shape)) {
            this.shapes.add(shape);
        }
    }

    public Shape getShape(String name) {
        for (Shape shape : shapes) {
            if (shape.getName().toLowerCase().equals(name.toLowerCase())) {
                return shape;
            }
        }
        return null;
    }

    public void removeShape(Shape shape) {
        if (shapes.contains(shape)) {
            this.shapes.remove(shape);
        }
    }

    public void removeShape(int index) {
        this.shapes.remove(index);
    }

    public void removeAllShapes() {
        this.next_shape_x = init_shape_x;
        this.next_shape_y = init_shape_y;

        this.shapes.clear();
    }

    public ArrayList<String> getShapeNamesInPage() {
        ArrayList<String> shapes_name_pool_in_page = new ArrayList<String>();

        for (Shape sp : this.shapes) {
            shapes_name_pool_in_page.add(sp.getName());
        }
        return shapes_name_pool_in_page;
    }

    public ArrayList<Shape> copyShapes() {
        ArrayList<Shape> copy = new ArrayList<>();
        for (Shape shape : this.getShapes()) {
            copy.add(shape.deepCopy());
        }
        return copy;
    }

    public void setShapes(ArrayList<Shape> shapes) {
        this.shapes = shapes;
    }

}
