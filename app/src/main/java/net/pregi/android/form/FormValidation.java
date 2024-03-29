package net.pregi.android.form;

import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

public class FormValidation {
    private List<Specs> specsList = new ArrayList<Specs>();
    public <T extends Specs> T add(T specs) {
        specsList.add(specs);
        return specs;
    }

    public boolean validate(boolean autocorrect) {
        boolean valid = true;
        for (Specs specs : specsList) {
            if (!specs.validate(autocorrect)) {
                valid = false;
            }
        }
        return valid;
    }

    public IntSpecs addInt(EditText v, boolean required, OnIntValue onValue) {
        return add(new IntSpecs(this, v, required, onValue)).integer();
    }
    public IntSpecs addLong(EditText v, boolean required, OnIntValue onValue) {
        return add(new IntSpecs(this, v, required, onValue));
    }
}
