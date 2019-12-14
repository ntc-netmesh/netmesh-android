package net.pregi.android.form;

public interface OnIntValue {
    /** <p>Define the behavior when a value is received.
     *  A value is received if it is not denied by the validator.</p>
     *
     * @param value The value received. Null if blank, but only if not required.
     *              If IntSpecs#integer() was called, the value will fit in an integer.
     */
    public void onValue(Long value);
}