package model;

import java.io.Serializable;

public class Student implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String studentNo;   // unique, typed in by the user (e.g. 2024-0001)
    private String name;
    private int sectionId;

    public Student(String studentNo, String name, int sectionId) {
        this.studentNo = studentNo;
        this.name = name;
        this.sectionId = sectionId;
    }

    public String getStudentNo() { return studentNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getSectionId() { return sectionId; }
    public void setSectionId(int sectionId) { this.sectionId = sectionId; }

    @Override
    public String toString() { return studentNo + " - " + name; }
}
