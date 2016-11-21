package com.eyllo.vista.fragment;

import android.app.Fragment;

public abstract class BarcodeCapture extends Fragment {

    private static final String TAG = BarcodeCapture.class.getName();

    static BarcodeDetectedListener mCallback;

    public interface BarcodeDetectedListener {
        public void onBarcodeDetected(String code);
    }

}

