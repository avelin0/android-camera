package com.eyllo.vista.barcode;

import com.eyllo.vista.cameraGoogle.GraphicOverlay;
import com.eyllo.vista.fragment.BarcodeCaptureGoogle;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

public class BarcodeTrackerFactory implements MultiProcessor.Factory<Barcode> {
    private final String TAG = BarcodeTrackerFactory.class.getName();

    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;
    BarcodeCaptureGoogle.BarcodeDetectedListener mBarcodeDetectedListener;

    public BarcodeTrackerFactory(GraphicOverlay<BarcodeGraphic> barcodeGraphicOverlay, BarcodeCaptureGoogle.BarcodeDetectedListener barcodeDetectedListener) {
        mGraphicOverlay = barcodeGraphicOverlay;
        mBarcodeDetectedListener = barcodeDetectedListener;
    }

    @Override
    public Tracker<Barcode> create(Barcode barcode) {
        BarcodeGraphic graphic = new BarcodeGraphic(mGraphicOverlay);
        return new BarcodeGraphicTracker(mGraphicOverlay, graphic, mBarcodeDetectedListener);
    }

}

