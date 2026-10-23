package model;

import java.util.HashMap;
import utils.FileDBHelper;

public class StudentManager {
    private HashMap<String, Student> students = new HashMap<>();
    private final String FILE_PATH = "students.ser";

    public StudentManager() {
        FileDBHelper.createFile(FILE_PATH);
    }

    public void addStudent(Student s) { students.put(s.getStudentNo(), s); }
    public Student findStudent(String studentNo) { return students.get(studentNo); }
    public Student deleteStudent(String studentNo) { return students.remove(studentNo); }
    public HashMap<String, Student> findAll() { return students; }

    public boolean hasStudentInSection(int sectionId) {
        return students.values().stream().anyMatch(s -> s.getSectionId() == sectionId);
    }

    public int saveStudents() { return FileDBHelper.save(FILE_PATH, students); }

    @SuppressWarnings("unchecked")
    public void loadStudents() {
        Object o = FileDBHelper.load(FILE_PATH);
        if (o != null) students = (HashMap<String, Student>) o;
    }
}
