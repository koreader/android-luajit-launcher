package org.koreader.launcher;

import android.app.Dialog;
import android.content.Context;
import org.koreader.launcher.R;
import android.widget.ProgressBar;
import android.view.ViewGroup.LayoutParams;
import android.content.DialogInterface.OnCancelListener;

public class FramelessProgressDialog extends Dialog {

    public static FramelessProgressDialog show(Context context, CharSequence title,
            CharSequence message) {
        return show(context, title, message, false);
    }

    public static FramelessProgressDialog show(Context context, CharSequence title,
            CharSequence message, boolean indeterminate) {
        return show(context, title, message, indeterminate, false, null);
    }

    public static FramelessProgressDialog show(Context context, CharSequence title,
            CharSequence message, boolean indeterminate, boolean cancelable) {
        return show(context, title, message, indeterminate, cancelable, null);
    }

    public static FramelessProgressDialog show(Context context, CharSequence title,
            CharSequence message, boolean indeterminate,
            boolean cancelable, OnCancelListener cancelListener) {
        FramelessProgressDialog dialog = new FramelessProgressDialog(context);
        dialog.setTitle(title);
        dialog.setCancelable(cancelable);
        dialog.setOnCancelListener(cancelListener);
        /* The next line will add the ProgressBar to the dialog. */
        dialog.addContentView(new ProgressBar(context),
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        dialog.show();

        return dialog;
    }

    public FramelessProgressDialog(Context context) {
        super(context, R.style.FramelessDialog);
    }
}
