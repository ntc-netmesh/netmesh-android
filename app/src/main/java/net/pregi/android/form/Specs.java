package net.pregi.android.form;

import android.widget.EditText;

public abstract class Specs {
    private boolean required;
    public boolean isRequired() {
        return required;
    }

    private EditText view;
    public EditText getView() {
        return view;
    }

    private FormValidation validator;

    /** <p>Run this specification's validation logic.</p>
     *
     * @param autocorrect whether the logic should correct the input, if applicable and possible.
     * @return whether it succeeded. If input wrong but is autocorrected, it returns true.
     */
    public abstract boolean validate(boolean autocorrect);

    public IntSpecs addInt(EditText v, boolean required, OnIntValue onValue) {
        return validator.addInt(v, required, onValue);
    }

    public IntSpecs addLong(EditText v, boolean required, OnIntValue onValue) {
        return validator.addLong(v, required, onValue);
    }

    public Specs(FormValidation validator, EditText view, boolean required) {
        this.validator = validator;
        this.required = required;
        this.view = view;
    }
}