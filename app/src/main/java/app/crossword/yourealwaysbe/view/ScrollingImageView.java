package app.crossword.yourealwaysbe.view;

import java.util.Timer;
import java.util.logging.Logger;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;


public class ScrollingImageView extends FrameLayout implements OnGestureListener {
    private static final Logger LOG = Logger.getLogger("app.crossword.yourealwaysbe");

    private static final double SCROLL_SNAP_BUFFER_PCNT = 0.07;

    private AuxTouchHandler aux = null;
    private ClickListener ctxListener;
    private GestureDetector gestureDetector;
    private ImageView imageView;
    private ScaleListener scaleListener = null;
    private ScrollLocation scaleScrollLocation;
    private Timer longTouchTimer = new Timer();
    private boolean longTouched;
    private float maxScale = 1.5f;
    private float minScale = 0.20f;
    private float runningScale = 1.0f;
    private boolean haveAdded = false;
    private boolean allowOverScroll = true;

    public ScrollingImageView(Context context, AttributeSet as) {
        super(context, as);
        gestureDetector = new GestureDetector(context, this);
        gestureDetector.setIsLongpressEnabled(true);
        imageView = new ImageView(context);


        if (android.os.Build.VERSION.SDK_INT >= 8) {
            try {
                aux = (AuxTouchHandler) Class.forName("app.crossword.yourealwaysbe.view.MultitouchHandler")
                                             .newInstance();
                aux.init(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void setBitmap(Bitmap bitmap) {
        this.setBitmap(bitmap, true);
    }

    public void setBitmap(Bitmap bitmap, boolean rescale) {
        if (bitmap == null) {
            return;
        }

        LOG.finest("New Bitmap Size: " + bitmap.getWidth() + " x " + bitmap.getHeight());

        if (rescale){
//            if (imageView != null) {
//                this.removeView(imageView);
//            }
//

	FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(bitmap.getWidth(), bitmap.getHeight());
            imageView.setImageBitmap(bitmap);
            if(!haveAdded){
	            this.addView(imageView, params);
	            haveAdded = true;
            } else {
	imageView.setLayoutParams(params);
            }
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }

    public void setContextMenuListener(ClickListener l) {
        this.ctxListener = l;
    }

    public ImageView getImageView() {
        return this.imageView;
    }

    public void setMaxScale(float maxScale) {
        this.maxScale = maxScale;
    }

    public float getMaxScale() {
        return maxScale;
    }

    public int getMaxScrollX() {
        return imageView.getWidth() - this.getWidth();
    }

    public int getMaxScrollY() {
        return imageView.getHeight() - this.getHeight();
    }

    public void setMinScale(float minScale) {
        this.minScale = minScale;
    }

    public float getMinScale() {
        return minScale;
    }

    public void setScaleListener(ScaleListener scaleListener) {
        this.scaleListener = scaleListener;
    }

    private float currentScale = 1.0f;

    public void setCurrentScale(float scale){
	this.currentScale = scale;
    }

    public boolean isVisible(Point p) {
        int currentMinX = this.getScrollX();
        int currentMaxX = this.getWidth() + this.getScrollX();
        int currentMinY = this.getScrollY();
        int currentMaxY = this.getHeight() + this.getScrollY();

        return (p.x >= currentMinX) && (p.x <= currentMaxX) && (p.y >= currentMinY) && (p.y <= currentMaxY);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if ((aux != null) && aux.onTouchEvent(ev)) {
            return true;
        }

        gestureDetector.onTouchEvent(ev);

        return true;
    }

    public void ensureVisible(Point p) {
        int maxScrollX = this.getMaxScrollX();
        int x = p.x;
        int maxScrollY = this.getMaxScrollY();

        int y = p.y;

        int currentMinX = this.getScrollX();
        int currentMaxX = this.getWidth() + this.getScrollX();
        int currentMinY = this.getScrollY();
        int currentMaxY = this.getHeight() + this.getScrollY();

//        LOG.info("X range " + currentMinX + " to " + currentMaxX);
//        LOG.info("Desired X:" + x);
//        LOG.info("Y range " + currentMinY + " to " + currentMaxY);
//        LOG.info("Desired Y:" + y);

        if ((x < currentMinX) || (x > currentMaxX)) {
            this.scrollTo((x > maxScrollX) ? maxScrollX : (x), this.getScrollY());
        }

        if ((y < currentMinY) || (y > currentMaxY)) {
//            LOG.info("Y adjust " + (y > maxScrollY ? maxScrollY : (y)));
            this.scrollTo(this.getScrollX(), (y > maxScrollY) ? maxScrollY : (y));
        }
    }

    public boolean onDown(MotionEvent e) {
        return false;
    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    public void onLongPress(MotionEvent e) {
        if ((aux != null) && aux.inProgress()) {
            return;
        }

        final Point p = this.resolveToImagePoint(e.getX(), e.getY());

        if (ScrollingImageView.this.ctxListener != null) {
            ScrollingImageView.this.ctxListener.onContextMenu(p);
            ScrollingImageView.this.longTouched = true;
        }
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        this.longTouchTimer.cancel();
        this.longTouched = false;

        this.scrollBy((int) distanceX, (int) distanceY);

        return true;
    }

    public void onShowPress(MotionEvent e) {
    }

    public boolean onSingleTapUp(MotionEvent e) {
        Point p = this.resolveToImagePoint(e.getX(), e.getY());
        this.longTouchTimer.cancel();

        if (this.longTouched) {
            this.longTouched = false;
        } else {
            if (this.ctxListener != null) {
                this.ctxListener.onTap(p);
            }
        }

        return true;
    }

    public Point resolveToImagePoint(float x, float y) {
        return this.resolveToImagePoint((int) x, (int) y);
    }

    public Point resolveToImagePoint(int x, int y) {
        return new Point(x + this.getScrollX(), y + this.getScrollY());
    }

    /**
     * Set whether overscroll is allowed
     *
     * Overscroll is when the board is dragged so that an edge of the
     * board pulls away from the corresponding edge of the view. E.g. a
     * gap between the left view edge and the left board edge. Default
     * is yes.
     */
    public void setAllowOverScroll(boolean allowOverScroll) {
        this.allowOverScroll = allowOverScroll;
    }

    public void scrollBy(int x, int y) {
        int curX = this.getScrollX();
        int curY = this.getScrollY();
        int newX = curX + x;
        int newY = curY + y;

        int screenWidth = this.getWidth();
        int screenHeight = this.getHeight();
        int boardWidth = this.imageView.getWidth();
        int boardHeight= this.imageView.getHeight();

        int scrollSnapBufferWidth = (int)(boardWidth * SCROLL_SNAP_BUFFER_PCNT);
        int scrollSnapBufferHeight = (int)(boardHeight * SCROLL_SNAP_BUFFER_PCNT);

        // don't allow space between right/bot edge of screen and
        // board (careful of negatives, since co-ords are neg)
        // only adjust if we're scrolling up and just stay put if there
        // was already a gap
        int newRight = newX - boardWidth;
        if (x > 0 &&
            -newRight < screenWidth &&
            (!allowOverScroll || -newRight > screenWidth - scrollSnapBufferWidth))
            newX = Math.max(-(screenWidth - boardWidth), curX);

        if (x < 0 &&
            -newRight > screenWidth &&
            (!allowOverScroll || -newRight < screenWidth + scrollSnapBufferWidth))
            newX = Math.max(-(screenWidth - boardWidth), curX);

        int newBot = newY - boardHeight;
        if (y > 0 &&
            -newBot < screenHeight &&
            (!allowOverScroll || -newBot > screenHeight - scrollSnapBufferHeight))
            newY = Math.max(-(screenHeight - boardHeight), curY);

        if (y < 0 &&
            -newBot > screenHeight &&
            (!allowOverScroll || -newBot < screenHeight + scrollSnapBufferHeight))
            newY = Math.max(-(screenHeight - boardHeight), curY);

        // don't allow space between left/top edge of screen and board
        // by doing second this is prioritised over bot/right
        // fix even if scrolling down to stop flipping from one edge to
        // the other (i.e. never allow a gap top/left, but sometime
        // allow bot/right if needed)
        if (newX < 0 &&
            (!allowOverScroll || newX > -scrollSnapBufferWidth))
            newX = 0;
        if (newY < 0 &&
            (!allowOverScroll || newY > -scrollSnapBufferHeight))
            newY = 0;

        // as above but for scrolling top/left off screen
        if (newX > 0 &&
            (!allowOverScroll || newX < scrollSnapBufferWidth))
            newX = 0;
        if (newY > 0 &&
            (!allowOverScroll || newY < scrollSnapBufferHeight))
            newY = 0;

        super.scrollTo(newX, newY);
    }

    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
    }

    public void zoom(float scale, int x, int y) {
        if (this.scaleScrollLocation == null) {
            this.scaleScrollLocation = new ScrollLocation(this.resolveToImagePoint(x, y), this.imageView);
        }

        if ((runningScale * scale) < minScale) {
            scale = 1.0F;
        }

        if ((runningScale * scale) > maxScale) {
            scale = 1.0F;
        }

        if(scale * this.currentScale > maxScale ){
	return;
        }
        if(scale * this.currentScale < minScale){
	return;
        }
        int h = imageView.getHeight();
        int w = imageView.getWidth();
        h *= scale;
        w *= scale;
        runningScale *= scale;
        currentScale *= scale;
        this.removeView(imageView);
        this.addView(imageView, new FrameLayout.LayoutParams(w,h));
        this.scaleScrollLocation.fixScroll(w, h, false);
    }

    public void zoomEnd() {
        if ((this.scaleListener != null) && (this.scaleScrollLocation != null)) {
            LOG.info("Zoom end "+runningScale);
            scaleListener.onScale(runningScale,
                this.scaleScrollLocation.findNewPoint(imageView.getWidth(), imageView.getHeight()));
            this.scaleScrollLocation.fixScroll(imageView.getWidth(), imageView.getHeight(), true);

            Point origin = this.resolveToImagePoint(0, 0);

            if (origin.x < 0) {
                this.scrollTo(0, this.getScrollY());
            }

            if (origin.y < 0) {
                this.scrollBy(this.getScrollX(), 0);
            }
        }

        this.scaleScrollLocation = null;
        runningScale = 1.0f;
    }

    public interface AuxTouchHandler {
        boolean inProgress();

        void init(ScrollingImageView view);

        boolean onTouchEvent(MotionEvent ev);
    }

    public interface ClickListener {
        void onContextMenu(Point e);

        void onTap(Point e);
    }

    public interface ScaleListener {
        void onScale(float scale, Point center);
    }

    public static class Point {
        int x;
        int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Point() {
        }

        public int distance(Point p) {
            double d = Math.sqrt(((double) this.x - (double) p.x) + ((double) this.y - (double) p.y));

            return (int) Math.round(d);
        }

        @Override
        public String toString(){
	return "["+x+", "+y+"]";
        }
    }

    private class ScrollLocation {
        private final double percentAcrossImage;
        private final double percentDownImage;
        private final int absoluteX;
        private final int absoluteY;

        public ScrollLocation(Point p, ImageView imageView) {
            this.percentAcrossImage = (double) p.x / (double) imageView.getWidth();
            this.percentDownImage = (double) p.y / (double) imageView.getHeight();
            this.absoluteX = p.x - ScrollingImageView.this.getScrollX();
            this.absoluteY = p.y - ScrollingImageView.this.getScrollY();
        }

        public Point findNewPoint(int newWidth, int newHeight) {
            int newX = (int) Math.round((double) newWidth * this.percentAcrossImage);
            int newY = (int) Math.round((double) newHeight * this.percentDownImage);

            return new Point(newX, newY);
        }

        public void fixScroll(int newWidth, int newHeight, boolean snap) {
            Point newPoint = this.findNewPoint(newWidth, newHeight);

            int newScrollX = newPoint.x - this.absoluteX;
            int newScrollY = newPoint.y - this.absoluteY;

            int maxX = ScrollingImageView.this.getMaxScrollX();
            int maxY = ScrollingImageView.this.getMaxScrollY();

            if (snap && (newScrollX > maxX)) {
                newScrollX = maxX;
            }

            if (snap && (newScrollY > maxY)) {
                newScrollY = maxY;
            }

            ScrollingImageView.this.scrollTo(newScrollX, newScrollY);
        }
    }
}
