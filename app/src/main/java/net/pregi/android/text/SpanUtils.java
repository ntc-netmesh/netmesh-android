package net.pregi.android.text;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import java.util.ArrayList;
import java.util.List;

public class SpanUtils {
    public static class Builder {
        private SpannableString text;
        private int flags = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

        private List<CharacterStyle> characterStyles = new ArrayList<CharacterStyle>();

        public Builder on(ClickableSpan cs) {
            characterStyles.add(cs);
            return this;
        }

        public Builder setColor(int hex) {
            characterStyles.add(new ForegroundColorSpan(hex));
            return this;
        }
        public Builder color(int hex) {
            return setColor(hex);
        }

        public Builder bold() {
            characterStyles.add(new StyleSpan(Typeface.BOLD));
            return this;
        }

        public Builder italic() {
            characterStyles.add(new StyleSpan(Typeface.ITALIC));
            return this;
        }

        public Builder add(CharacterStyle style) {
            characterStyles.add(style);
            return this;
        }

        public SpannableString build() {
            SpannableString spanned = new SpannableString(text);
            int end = spanned.length();

            for (CharacterStyle cs : characterStyles) {
                spanned.setSpan(cs, 0, end, flags);
            }
            return spanned;
        }

        public Builder(CharSequence text) {
            this.text = new SpannableString(text);
        }
    }

    public static SpannableString bold(CharSequence text) {
        return new Builder(text).bold().build();
    }

    public static SpannableString italic(CharSequence text) {
        return new Builder(text).italic().build();
    }

    public static SpannableString colored(CharSequence text, int color) {
        return new Builder(text).color(color).build();
    }

    public static SpannableString small(CharSequence text) {
        return new Builder(text).add(new RelativeSizeSpan(0.8f)).build();
    }

    public static SpannableString clickable(CharSequence text, ClickableSpan onClick) {
        return new Builder(text).on(onClick).color(Color.BLUE).build();
    }

    private SpanUtils() {

    }
}
