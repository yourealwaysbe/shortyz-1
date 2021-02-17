/**
 * Deliberately not using Keyboard or KeyboardView as they are deprecated.
 * First principles approach preferred instead.
 */

package app.crossword.yourealwaysbe.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.view.LayoutInflaterCompat;

import app.crossword.yourealwaysbe.forkyz.R;

public class ForkyzKeyboard
    extends LinearLayout
    implements View.OnClickListener {

    private static final String FORKYZ_TEXT_KEY = "ForkyzTextKey";
    private static final String FORKYZ_IMAGE_KEY = "ForkyzImageKey";

    private SparseArray<Integer> keyCodes = new SparseArray<>();
    private InputConnection inputConnection;

    public ForkyzKeyboard(Context context) {
        this(context, null, 0);
    }

    public ForkyzKeyboard(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ForkyzKeyboard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @Override
    public void onClick(View view) {
        if (inputConnection == null)
            return;
        Integer keyCode = keyCodes.get(view.getId());
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
        inputConnection.sendKeyEvent(event);
    }

    public void setInputConnection(InputConnection ic) {
        inputConnection = ic;
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater inflater
            = LayoutInflater.from(context).cloneInContext(context);
        LayoutInflaterCompat.setFactory2(inflater, new FKFactory());
        inflater.inflate(R.layout.forkyz_keyboard, this, true);
    }

    private void addKeyCode(int keyId, int keyCode) {
        keyCodes.put(keyId, keyCode);
    }

    private int getKeyCode(int keyId) {
        return keyCodes.get(keyId);
    }

    private class FKFactory implements LayoutInflater.Factory2 {
        @Override
        public View onCreateView(
            String tag, Context context, AttributeSet attrs
        ) {
            if (FORKYZ_TEXT_KEY.equals(tag)) {
                View view = new AppCompatButton(context, attrs);
                setupView(view, attrs);
                return view;
            } else if (FORKYZ_IMAGE_KEY.equals(tag)) {
                View view = new AppCompatImageButton(context, attrs);
                setupView(view, attrs);
                return view;
            } else {
                return null;
            }
        }

        public View onCreateView(
            View parent, String tag, Context context, AttributeSet attrs
        ) {
            return onCreateView(tag, context, attrs);
        }

        private void setupView(View view, AttributeSet attrs) {
            view.setOnClickListener(ForkyzKeyboard.this);
            int keyCode
                = attrs.getAttributeIntValue(R.styleable.ForkyzKey_keyCode, -1);
            ForkyzKeyboard.this.addKeyCode(view.getId(), keyCode);
        }
    }
}
