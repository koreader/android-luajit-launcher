package org.koreader.launcher.helper;

import java.util.concurrent.CountDownLatch;

import android.app.Activity;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;

import org.koreader.launcher.Logger;


public class ClipboardHelper extends BaseHelper {

    private final ClipboardManager clipboard;

    public ClipboardHelper(Context context) {
        super(context);
        this.clipboard = (ClipboardManager)
            getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
    }

    public String getClipboardText() {
        final Box<String> result = new Box<>();
        final CountDownLatch cd = new CountDownLatch(1);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (clipboard.hasPrimaryClip()) {
                        ClipData data = clipboard.getPrimaryClip();
                        if (data != null && data.getItemCount() > 0) {
                            CharSequence text = data.getItemAt(0).coerceToText(
                                getApplicationContext());
                            if (text != null) {
                                result.value = text.toString();
                            } else {
                                result.value = "";
                            }
                        }
                    }
                } catch (Exception e) {
                    Logger.w(getTag(), e.toString());
                    result.value = "";
                }
                cd.countDown();
            }
        });
        try {
            cd.await();
        } catch (InterruptedException ex) {
            return "";
        }

        if (result.value == null) {
            return "";
        }
        return result.value;
    }

    public void setClipboardText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    ClipData clip = ClipData.newPlainText("KOReader_clipboard", text);
                    clipboard.setPrimaryClip(clip);
                } catch (Exception e) {
                    Logger.w(getTag(), e.toString());
                }
            }
        });
    }

    public int hasClipboardText() {
        return (clipboardHasText()) ? 1 : 0;
    }

    private boolean clipboardHasText() {
        if (clipboard.hasPrimaryClip()) {
            ClipData data = clipboard.getPrimaryClip();
            return  (data != null && data.getItemCount() > 0);
        } else {
            return false;
        }
    }

    private class Box<T> {
        T value;
    }
}
