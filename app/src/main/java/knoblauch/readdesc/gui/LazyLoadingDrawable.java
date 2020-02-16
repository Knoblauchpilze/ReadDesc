package knoblauch.readdesc.gui;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import java.lang.ref.WeakReference;

class LazyLoadingDrawable extends ColorDrawable  {

    /**
     * Reference to the lazy loading task for this drawable. It is used by the
     * managing process to check whether for any image view the drawable that
     * is associated to it needs to be interrupted.
     */
    private WeakReference<LazyLoadingTask> m_loader;

    /**
     * Create a new lazy loading drawable which will display a transparent bg
     * until the task associated to the loading of the image is finished.
     * @param res - the loading task that this placeholder is hiding.
     */
    LazyLoadingDrawable(LazyLoadingTask res) {
        // Use a transparent background at first (i.e. until the image has
        // finished to be downloaded).
        super(Color.TRANSPARENT);

        // Assign the internal loading task.
        m_loader = new WeakReference<>(res);
    }

    /**
     * Used by external elements to retrieve the loading task for this drawable.
     * @return - a pointer to the loading task for this element.
     */
    LazyLoadingTask getLoader() {
        return m_loader.get();
    }

}
