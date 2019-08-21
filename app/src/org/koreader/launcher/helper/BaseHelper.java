package org.koreader.launcher.helper;

import android.content.Context;
import android.os.Handler;

import org.koreader.launcher.Logger;


/*
   Base class for application helpers.

   Intended for long-term things, like wifi, clipboard or power facilities.
   We do everything based on the application context and implement our own
   getApplicationContext() and runOnUiThread().

   Ui things need to be done on the activity context. Please do not hold a reference
   of an activity on your subclass. Instead just receive an activity as a parameter
   of your methods. */

abstract class BaseHelper {
    private final Handler handler;
    private final Context context;
    private final String tag;

    public BaseHelper(Context context) {

        // get a reference to the application context, which is already pre-leaked.
        this.context = context.getApplicationContext();

        // the name for this class and subclasses that extend from it.
        this.tag = this.getClass().getSimpleName();

        /* use a handler to execute runnables on the main thread.

           This is useful to do UI things, because the main thread
           is the only that can touch the UI thread. */

        this.handler = new Handler(this.context.getMainLooper());

        // for debugging
        Logger.d(tag, "Starting");
    }

    Context getApplicationContext() {
        // returns the context of the application
        return context;
    }

    String getTag() {
        // returns the name of the class
        return tag;
    }

    void runOnUiThread(Runnable runnable) {
        // forward the runnable to the main thread
        handler.post(runnable);
    }
}
