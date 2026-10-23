package model;

import java.util.HashMap;
import utils.FileDBHelper;

public class SectionManager {
    private HashMap<Integer, Section> sections = new HashMap<>();
    private final String FILE_PATH = "sections.ser";

    public SectionManager() {
        FileDBHelper.createFile(FILE_PATH);
    }

    /** FIX: ids come from the stored data (max + 1), not a static counter that reset on every run. */
    public int nextId() {
        return sections.keySet().stream().max(Integer::compare).orElse(0) + 1;
    }

    public void addSection(Section s) { sections.put(s.getId(), s); }
    public Section findSection(int id) { return sections.get(id); }
    public Section deleteSection(int id) { return sections.remove(id); }
    public HashMap<Integer, Section> findAll() { return sections; }

    public int saveSections() { return FileDBHelper.save(FILE_PATH, sections); }

    @SuppressWarnings("unchecked")
    public void loadSections() {
        Object o = FileDBHelper.load(FILE_PATH);
        if (o != null) sections = (HashMap<Integer, Section>) o;
    }
}
