package net.pregi.android.form;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import net.pregi.android.netmesh.R;

import java.util.regex.Pattern;

/** <p>Define specifications of a field intended for number input.</p>
 *
 * <p>This caters for both long and int.
 * Internally, long is used, but the integer() method can be used to set the lower/upper bounds
 *      to Integer.MIN_VALUE/Integer.MAX_VALUE</p>
 *
 */
public class IntSpecs extends Specs {
    private static final Pattern FORMAT_INTEGER = Pattern.compile("^-?\\d+$");

    private OnIntValue onValueListener;

    private long lowerBound = Long.MIN_VALUE, upperBound = Long.MAX_VALUE;
    private boolean lowerBounded = false, upperBounded = false;

    public IntSpecs integer() {
        lowerBound = Integer.MIN_VALUE;
        upperBound = Integer.MAX_VALUE;
        return this;
    }

    public IntSpecs nonNegative() {
        lowerBound = 0;
        lowerBounded = true;
        return this;
    }

    public IntSpecs positive() {
        lowerBound = 1;
        lowerBounded = true;
        return this;
    }

    public IntSpecs minimum(long value) {
        lowerBound = value;
        lowerBounded = true;
        return this;
    }
    public IntSpecs maximum(long value) {
        upperBound = value;
        upperBounded = true;
        return this;
    }
    public IntSpecs between(long lower, long upper) {
        lowerBound = lower;
        lowerBounded = true;
        upperBound = upper;
        upperBounded = true;
        return this;
    }

    @Override
    public boolean validate(boolean autocorrect) {
        EditText v = getView();
        CharSequence input = v.getText();

        if (input != null && input.length()>0) {
            long value = Long.parseLong(input.toString());
            if (upperBounded && lowerBounded) {
                if (value<lowerBound || value>upperBound) {
                    if (autocorrect) {
                        v.setText(Long.toString(value<lowerBound ? lowerBound : upperBound));
                    } else {
                        v.setError(v.getResources().getString(R.string.form_error_int_mustbe_between, lowerBound, upperBound));
                        return false;
                    }
                }
            } else if (lowerBounded) {
                if (value<lowerBound) {
                    if (autocorrect) {
                        v.setText(Long.toString(lowerBound));
                    } else {
                        v.setError(v.getResources().getString(R.string.form_error_int_mustbe_atleast, lowerBound));
                        return false;
                    }
                }
            } else if (upperBounded) {
                if (value>upperBound) {
                    if (autocorrect) {
                        v.setText(Long.toString(upperBound));
                    } else {
                        v.setError(v.getResources().getString(R.string.form_error_int_mustbe_nomorethan, lowerBound));
                        return false;
                    }
                }
            }
        } else if (isRequired()) {
            v.setError(v.getResources().getString(R.string.form_error_blank));
            return false;
        }

        return true;
    }

    IntSpecs(FormValidation validator, EditText view, boolean required, OnIntValue onValue) {
        super(validator, view, required);
        onValueListener = onValue;

        view.addTextChangedListener(new TextWatcher() {
            private CharSequence before;
            private CharSequence after;
            private int beforeDiffStart;
            private int beforeDiffLength;
            private int afterDiffStart;
            private int afterDiffLength;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                before = s;
                beforeDiffStart = start;
                beforeDiffLength = count;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                after = s;
                afterDiffStart = start;
                afterDiffLength = count;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length()>0) {
                    if (FORMAT_INTEGER.matcher(after).matches()) {
                        // It is an almost valid input, so apply it.
                        if (validate(false)) {
                            if (onValueListener != null) {
                                onValueListener.onValue(Long.parseLong(s.toString()));
                            }
                        }
                    } else {
                        // only addition of invalid characters will cause the format check to fail
                        //      other than erasing the entire input..
                        // revert that change.
                        s.replace(afterDiffStart, afterDiffStart + afterDiffLength, before, beforeDiffStart, beforeDiffStart + beforeDiffLength);
                    }
                } else if (isRequired()) {
                   validate(false);
                }
            }
        });
    }

}