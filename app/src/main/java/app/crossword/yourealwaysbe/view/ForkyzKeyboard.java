/**
 * Deliberately not using Keyboard or KeyboardView as they are deprecated.
 * First principles approach preferred instead.
 */

package app.crossword.yourealwaysbe.view;

import java.util.Timer;
import java.util.TimerTask;

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
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.view.LayoutInflaterCompat;

import app.crossword.yourealwaysbe.forkyz.R;

public class ForkyzKeyboard
    extends LinearLayout
    implements View.OnTouchListener {

    private static final String FORKYZ_TEXT_KEY = "ForkyzTextKey";
    private static final String FORKYZ_IMAGE_KEY = "ForkyzImageKey";

    private static final int KEY_REPEAT_DELAY = 300;
    private static final int KEY_REPEAT_INTERVAL = 50;

    private SparseArray<Integer> keyCodes = new SparseArray<>();
    private SparseArray<Timer> keyTimers = new SparseArray<>();
    private InputConnection inputConnection;

    public ForkyzKeyboard(Context context) {
        this(context, null, 0);
    }

    public ForkyzKeyboard(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ForkyzKeyboard(
        Context context, AttributeSet attrs, int defStyleAttr
    ) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    /**
     * Call when activity paused to cancel unfinished key repeats
     */
    public void onPause() {
        for (int i = 0; i < keyTimers.size(); i++) {
            Timer timer = keyTimers.valueAt(i);
            if (timer != null)
                timer.cancel();
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (inputConnection == null)
            return false;

        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            onKeyDown(view.getId());
            break;
        case MotionEvent.ACTION_UP:
            onKeyUp(view.getId());
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
    }

    private void addKeyCode(int keyId, int keyCode) {
        keyCodes.put(keyId, keyCode);
    }

    private int getKeyCode(int keyId) {
        return keyCodes.get(keyId);
    }

    private void onKeyUp(int keyId) {
        sendKeyUp(keyId);
        cancelKeyTimer(keyId);
    }

    private void sendKeyUp(int keyId) {
        int keyCode = getKeyCode(keyId);
        inputConnection.sendKeyEvent(
            new KeyEvent(KeyEvent.ACTION_UP, keyCode)
        );
    }

    private void onKeyDown(final int keyId) {
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sendKeyUp(keyId);
                sendKeyDown(keyId);
            }
        }, KEY_REPEAT_DELAY, KEY_REPEAT_INTERVAL);
        setKeyTimer(keyId, timer);
    }

    private void sendKeyDown(int keyId) {
        int keyCode = getKeyCode(keyId);
        inputConnection.sendKeyEvent(
            new KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        );
    }

    private void setKeyTimer(int keyId, Timer timer) {
        cancelKeyTimer(keyId);
        keyTimers.put(keyId, timer);
    }

    private void cancelKeyTimer(int keyId) {
        Timer timer = keyTimers.get(keyId);
        if (timer != null) {
            timer.cancel();
            // no point keeping references to expired timers
            keyTimers.put(keyId, null);
        }
    }

    private class FKFactory implements LayoutInflater.Factory2 {
        @Override
        public View onCreateView(
            View parent, String tag, Context context, AttributeSet attrs
        ) {
            if (FORKYZ_TEXT_KEY.equals(tag)) {
                TextView view = new AppCompatButton(context, attrs);
                setButtonTextSize(view, context, attrs);
                setButtonPadding(view, context, attrs);
                setupButton(view, context, attrs);
                return view;
            } else if (FORKYZ_IMAGE_KEY.equals(tag)) {
                View view = new AppCompatImageButton(context, attrs);
                setButtonPadding(view, context, attrs);
                setupButton(view, context, attrs);
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

        private void setupButton(
            View view, Context context, AttributeSet attrs
        ) {
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
        }

        private void setButtonTextSize(
            TextView view, Context context, AttributeSet attrs
        ) {
            if (!hasAttribute(android.R.attr.textSize, context, attrs)) {
                view.setTextSize(
                    context
                        .getResources()
                        .getInteger(R.integer.keyboardTextSize)
                );
            }
        }

        private void setButtonPadding(
            View view, Context context, AttributeSet attrs
        ) {
            int paddingTop = view.getPaddingTop();
            int paddingBottom = view.getPaddingBottom();
            int paddingLeft = view.getPaddingLeft();
            int paddingRight = view.getPaddingRight();

            if (!hasAttribute(android.R.attr.padding, context, attrs) &&
                !hasAttribute(android.R.attr.paddingTop, context, attrs) &&
                !hasAttribute(android.R.attr.paddingBottom, context, attrs)) {

                int btnPaddingPcnt
                    = context
                        .getResources()
                        .getInteger(R.integer.keyboardButtonPaddingPcnt);
                int dispHght
                    = context.getResources().getDisplayMetrics().heightPixels;
                int paddingTopBot = (int) ((btnPaddingPcnt / 100.0) * dispHght);

                paddingTop = paddingTopBot;
                paddingBottom = paddingTopBot;
            }

            view.setPadding(
                paddingLeft, paddingTop, paddingRight, paddingBottom
            );
        }

        private boolean hasAttribute(
            int id, Context context, AttributeSet attrs
        ) {
            boolean hasAttribute = false;
            TypedArray ta = context.obtainStyledAttributes(
                attrs, new int[] { id }
            );
            try {
                hasAttribute = ta.getString(0) != null;
            } finally {
                ta.recycle();
            }
            return hasAttribute;
        }

        private int dpToPx(Context context, int dp) {
            final float scale
                = context.getResources().getDisplayMetrics().density;
            return (int) (dp * scale + 0.5f);
        }
    }
}
