package org.koreader.launcher;

import android.app.Dialog;
import android.content.Context;
import android.widget.ProgressBar;
import android.view.ViewGroup.LayoutParams;


class FramelessProgressDialog extends Dialog {

    private FramelessProgressDialog(Context context) {
        super(context, R.style.FramelessDialog);
    }

    static FramelessProgressDialog show(Context context, CharSequence title) {

        FramelessProgressDialog dialog = new FramelessProgressDialog(context);
        dialog.setTitle(title);
        dialog.setCancelable(false);
        dialog.setOnCancelListener(null);

        /* The next line will add the ProgressBar to the dialog. */
        dialog.addContentView(new ProgressBar(context),
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        dialog.show();

        return dialog;
    }
}
