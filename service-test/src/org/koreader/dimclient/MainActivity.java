package org.koreader.dimclient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import org.koreader.IService;

import java.util.Locale;

@SuppressLint("SetTextI18n")
public class MainActivity extends Activity {
    private final static String TAG = "example";
    private IService remoteService;
    private TextView serviceTextView;
    private TextView statusTextView;

    private int dimStep = 0;
    private int warmthStep = 0;
    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            Log.d(TAG, "onServiceConnected()");
            remoteService = IService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            Log.d(TAG, "onServiceDisconnected()");
            remoteService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        statusTextView = findViewById(R.id.statusText);
        serviceTextView = findViewById(R.id.serviceText);
        SeekBar dimSeekBar = findViewById(R.id.dimSeekBar);
        SeekBar warmthSeekBar = findViewById(R.id.warmthSeekBar);

        dimSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (remoteService != null) {
                    try {
                        remoteService.setDim(i);
                    } catch (RemoteException re) {
                        re.printStackTrace();
                    }
                    dimStep = i;
                    printStatusDelayed(50);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                printStatusDelayed(50);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                printStatusDelayed(100);
            }
        });

        warmthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (remoteService != null) {
                    try {
                        remoteService.setWarmth(i);
                    } catch (RemoteException re) {
                        re.printStackTrace();
                    }
                    warmthStep = i;
                    printStatusDelayed(50);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                printStatusDelayed(50);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                printStatusDelayed(100);
            }
        });

        Intent intent = new Intent();
        intent.setAction("org.koreader.service");
        intent.setPackage("org.koreader.service");
        bindService(intent, conn, Service.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        super.onPause();
        if (remoteService != null) {
            try {
                remoteService.pause();
            } catch (RemoteException re) {
                re.printStackTrace();
            }
        }

    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();
        if (remoteService != null) {
            try {
                remoteService.resume();
            } catch (RemoteException re) {
                re.printStackTrace();
            }
        }
        printConnectionDelayed(1000);
    }

    @Override
    public  void onDestroy() {
        unbindService(conn);
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    private void printConnectionDelayed(long millis) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (remoteService != null) {
                    try {
                        String status = "service " + (remoteService.enabled() ? "available" : "disabled");
                        serviceTextView.setText(status);
                    } catch (RemoteException re) {
                        re.printStackTrace();
                        serviceTextView.setText("service not available");
                    }
                }
            }
        }, millis);
    }

    private void printStatusDelayed(long millis) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (remoteService != null) {
                    try {
                        statusTextView.setText(
                            String.format(
                                Locale.US,
                                "%s\ndim: %d, warmth: %d",
                                remoteService.status(),
                                dimStep,
                                warmthStep
                            )
                        );
                    } catch (RemoteException re) {
                        re.printStackTrace();
                    }
                }
            }
        }, millis);
    }
}
