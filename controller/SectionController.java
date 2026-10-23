package controller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import model.Section;
import model.SectionManager;
import model.StudentManager;

/** All handle* methods return null on success, or an error message to show the user. */
public class SectionController extends ChangeNotifier {
    private final SectionManager model;
    private final StudentManager studentModel;

    public SectionController(SectionManager model, StudentManager studentModel) {
        this.model = model;
        this.studentModel = studentModel;
    }

    private boolean nameTaken(String name, int exceptId) {
        return model.findAll().values().stream().anyMatch(
                s -> s.getName().equalsIgnoreCase(name) && s.getId() != exceptId);
    }

    public String handleAddSection(String name) {
        name = name == null ? "" : name.trim();
        if (name.isEmpty()) return "Section name is required.";
        if (nameTaken(name, -1)) return "Section \"" + name + "\" already exists.";
        Section s = new Section(model.nextId(), name);
        model.addSection(s);
        if (model.saveSections() != 0) {
            model.deleteSection(s.getId());
            return "Could not save the section to file.";
        }
        fireChanged();
        return null;
    }

    public String handleUpdateSection(int id, String name) {
        name = name == null ? "" : name.trim();
        if (name.isEmpty()) return "Section name is required.";
        Section s = model.findSection(id);
        if (s == null) return "Section not found.";
        if (nameTaken(name, id)) return "Section \"" + name + "\" already exists.";
        s.setName(name);
        if (model.saveSections() != 0) return "Could not save the section to file.";
        fireChanged();
        return null;
    }

    public String handleDeleteSection(int id) {
        if (model.findSection(id) == null) return "Section not found.";
        if (studentModel.hasStudentInSection(id))
            return "Cannot delete: some students still belong to this section.";
        model.deleteSection(id);
        if (model.saveSections() != 0) return "Could not save the change to file.";
        fireChanged();
        return null;
    }

    public List<Section> getAllSections() {
        List<Section> list = new ArrayList<>(model.findAll().values());
        list.sort(Comparator.comparing(Section::getName, String.CASE_INSENSITIVE_ORDER));
        return list;
    }

    public Section getSectionDetails(int id) { return model.findSection(id); }
}
