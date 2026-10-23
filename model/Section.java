package model;

import java.io.Serializable;

/** A class section / course, e.g. "BSIT 2A". (Replaces Category in the inventory sample.) */
public class Section implements Serializable {
    private static final long serialVersionUID = 1L;
    private final int id;
    private String name;

    public Section(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() { return name; }
}
