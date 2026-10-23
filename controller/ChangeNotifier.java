package controller;

import java.util.ArrayList;
import java.util.List;

/** Lets any number of views listen for data changes (the sample allowed only one view). */
public class ChangeNotifier {
    private final List<Runnable> listeners = new ArrayList<>();

    public void addListener(Runnable r) { listeners.add(r); }

    protected void fireChanged() {
        for (Runnable r : new ArrayList<>(listeners)) r.run();
    }
}
