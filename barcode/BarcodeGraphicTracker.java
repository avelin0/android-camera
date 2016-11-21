package com.eyllo.vista.barcode;

import com.eyllo.vista.cameraGoogle.GraphicOverlay;
import com.eyllo.vista.fragment.BarcodeCaptureGoogle;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

public class BarcodeGraphicTracker extends Tracker<Barcode> {
    private GraphicOverlay<BarcodeGraphic> mOverlay;
    private BarcodeGraphic mGraphic;
    private BarcodeCaptureGoogle.BarcodeDetectedListener mBarcodeDetectedListener;

    BarcodeGraphicTracker(GraphicOverlay<BarcodeGraphic> overlay, BarcodeGraphic graphic, BarcodeCaptureGoogle.BarcodeDetectedListener barcodeDetectedListener) {
        mOverlay = overlay;
        mGraphic = graphic;
        mBarcodeDetectedListener = barcodeDetectedListener;
    }

    @Override
    public void onNewItem(int id, Barcode item) {
        mGraphic.setId(id);
        mBarcodeDetectedListener.onBarcodeDetected(item.displayValue);
    }

    @Override
    public void onUpdate(Detector.Detections<Barcode> detectionResults, Barcode item) {
        mOverlay.add(mGraphic);
        mGraphic.updateItem(item);
    }

    @Override
    public void onMissing(Detector.Detections<Barcode> detectionResults) {
        mOverlay.remove(mGraphic);
    }

    @Override
    public void onDone() {
        mOverlay.remove(mGraphic);
    }
}
