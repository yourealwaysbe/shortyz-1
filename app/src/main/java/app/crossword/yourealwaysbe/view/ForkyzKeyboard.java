/**
 * Deliberately not using Keyboard or KeyboardView as they are deprecated.
 * First principles approach preferred instead.
 */

package app.crossword.yourealwaysbe.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.view.LayoutInflaterCompat;

import app.crossword.yourealwaysbe.forkyz.R;

public class ForkyzKeyboard
    extends LinearLayout
    implements View.OnTouchListener {

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
    public boolean onTouch(View view, MotionEvent event) {
        if (inputConnection == null)
            return false;

        Integer keyCode = keyCodes.get(view.getId());

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            inputConnection.sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            );
            break;
        case MotionEvent.ACTION_UP:
            inputConnection.sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyCode)
            );
            break;
        }

        return false;
    }

    /**
     * Attach the keyboard to send events to the view
     */
    public void attachToView(View view) {
        inputConnection = view.onCreateInputConnection(new EditorInfo());
    }

    private void init(Context context, AttributeSet attrs) {
        LayoutInflater inflater
            = LayoutInflater.from(context).cloneInContext(context);
        LayoutInflaterCompat.setFactory2(inflater, new FKFactory());
        inflater.inflate(R.layout.forkyz_keyboard, this, true);

        TypedArray ta = context.obtainStyledAttributes(
            attrs, new int[] { android.R.attr.background }
        );
        try {
            String background = ta.getString(0);
            if (background == null)
                setBackgroundResource(R.color.background_light);
        } finally {
            ta.recycle();
        }
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
            View parent, String tag, Context context, AttributeSet attrs
        ) {
            if (FORKYZ_TEXT_KEY.equals(tag)) {
                View view = new AppCompatButton(context, attrs);
                setupView(view, context, attrs);
                return view;
            } else if (FORKYZ_IMAGE_KEY.equals(tag)) {
                View view = new AppCompatImageButton(context, attrs);
                setupView(view, context, attrs);
                return view;
            } else {
                return null;
            }
        }

        public View onCreateView(
            String tag, Context context, AttributeSet attrs
        ) {
            return onCreateView(null, tag, context, attrs);
        }

        private void setupView(View view, Context context, AttributeSet attrs) {
            TypedArray ta = context.obtainStyledAttributes(
                attrs, R.styleable.ForkyzKey, 0, 0
            );
            try {
                int keyCode = ta.getInt(R.styleable.ForkyzKey_keyCode, -1);
                if (keyCode > -1) {
                    ForkyzKeyboard.this.addKeyCode(view.getId(), keyCode);
                    view.setOnTouchListener(ForkyzKeyboard.this);
                }
            } finally {
                ta.recycle();
            }

            ta = context.obtainStyledAttributes(
                attrs, new int[] { android.R.attr.background }
            );
            try  {
                String background = ta.getString(0);
                if (background == null)
                    view.setBackgroundResource(R.drawable.keyboard_button);
            } finally {
                ta.recycle();
            }
        }
    }
}
