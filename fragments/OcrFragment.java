package com.eyllo.vista.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.eyllo.vista.BuildConfig;
import com.eyllo.vista.R;
import com.eyllo.vista.cameraGoogle.CameraSource;
import com.eyllo.vista.cameraGoogle.CameraSourcePreview;
import com.eyllo.vista.cameraGoogle.GraphicOverlay;
import com.eyllo.vista.ocr.OcrDetectorProcessor;
import com.eyllo.vista.ocr.OcrGraphic;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.IOException;


@RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
public class OcrFragment extends Fragment{

        private static final String TAG = OcrFragment.class.getName();

        // Intent request code to handle updating play services if needed.
        private static final int RC_HANDLE_GMS = 9001;

        // Permission request codes need to be < 256
        private static final int RC_HANDLE_CAMERA_PERM = 2;

        public static final String AutoFocus = "AutoFocus";
        public static final String UseFlash = "UseFlash";
        public static final String TextBlockObject = "String";

        private CameraSource mCameraSource;
        private CameraSourcePreview mPreview;
        private GraphicOverlay<OcrGraphic> mGraphicOverlay;

        private ScaleGestureDetector scaleGestureDetector;
        private GestureDetector gestureDetector;


        protected boolean autoFocusValue;
        protected boolean useFlashValue;

        public OcrFragment() {
            // Required empty public constructor
        }

        public static OcrFragment newInstance() {
            return new OcrFragment();
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.fragment_ocr, container, false);
            mPreview = (CameraSourcePreview) v.findViewById(R.id.preview);
            mGraphicOverlay = (GraphicOverlay<OcrGraphic>) v.findViewById(R.id.graphicOverlay);

            // read parameters from the intent used to launch the activity.
//        boolean autoFocus = getIntent().getBooleanExtra(AutoFocus, false);
//        boolean useFlash = getIntent().getBooleanExtra(UseFlash, false);

            autoFocusValue = true;
            useFlashValue= false;

            Context thisContext = getActivity().getApplicationContext();
            int rc = ActivityCompat.checkSelfPermission(thisContext, Manifest.permission.CAMERA);
            if (rc == PackageManager.PERMISSION_GRANTED) {
                createCameraSource(autoFocusValue, useFlashValue);
            } else {
                requestCameraPermission();
            }

            gestureDetector = new GestureDetector(thisContext, new CaptureGestureListener());
            scaleGestureDetector = new ScaleGestureDetector(thisContext, new ScaleListener());

            Snackbar.make(mGraphicOverlay, "Tap to capture. Pinch/Stretch to zoom",
                    Snackbar.LENGTH_LONG)
                    .show();


            return v;

        }



        private void requestCameraPermission() {
            Log.w(TAG, "Camera permission is not granted. Requesting permission");

            final String[] permissions = new String[]{Manifest.permission.CAMERA};

            final Activity thisActivity = getActivity();
            if (!ActivityCompat.shouldShowRequestPermissionRationale(thisActivity,
                    Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(thisActivity, permissions, RC_HANDLE_CAMERA_PERM);
                return;
            }

            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityCompat.requestPermissions(thisActivity, permissions,
                            RC_HANDLE_CAMERA_PERM);
                }
            };

            Snackbar.make(mGraphicOverlay, R.string.permission_camera_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, listener)
                    .show();
        }


        public boolean onTouchEvent(MotionEvent e) {
            boolean b = scaleGestureDetector.onTouchEvent(e);

            boolean c = gestureDetector.onTouchEvent(e);

            return b || c ;
        }

        @SuppressLint("InlinedApi")
        private void createCameraSource(boolean autoFocus, boolean useFlash) {
            Context context = getActivity().getApplicationContext();

            TextRecognizer textRecognizer = new TextRecognizer.Builder(context).build();
            textRecognizer.setProcessor(new OcrDetectorProcessor(mGraphicOverlay));

            if (!textRecognizer.isOperational()) {

                Log.w(TAG, "Detector dependencies are not yet available.");

                IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
                boolean hasLowStorage = getActivity().registerReceiver(null, lowstorageFilter) != null;

                if (hasLowStorage) {
                    Toast.makeText(context, R.string.low_storage_error, Toast.LENGTH_LONG).show();
                    Log.w(TAG, getString(R.string.low_storage_error));
                }
            }

            mCameraSource =
                    new CameraSource.Builder(getActivity().getApplicationContext(), textRecognizer)
                            .setFacing(CameraSource.CAMERA_FACING_BACK)
                            .setRequestedPreviewSize(1280, 1024)
                            .setRequestedFps(2.0f)
                            .setFlashMode(useFlash ? Camera.Parameters.FLASH_MODE_TORCH : null)
                            .setFocusMode(autoFocus ? Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE : null)
                            .build();
        }

        @Override
        public void onResume() {
            super.onResume();
            startCameraSource();
        }

        @Override
        public void onPause() {
            super.onPause();
            if (mPreview != null) {
                mPreview.stop();
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (mPreview != null) {
                mPreview.release();
            }
        }

        @Override
        public void onRequestPermissionsResult(int requestCode,
                                               @NonNull String[] permissions,
                                               @NonNull int[] grantResults) {
            if (requestCode != RC_HANDLE_CAMERA_PERM) {
                Log.d(TAG, "Got unexpected permission result: " + requestCode);
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
            }

            if (grantResults.length != 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted - initialize the camera source");
                // We have permission, so create the camerasource
                boolean autoFocus = getActivity().getIntent().getBooleanExtra(AutoFocus,false);
                boolean useFlash = getActivity().getIntent().getBooleanExtra(UseFlash, false);
                createCameraSource(autoFocus, useFlash);
                return;
            }

            Log.e(TAG, "Permission not granted: results len = " + grantResults.length +
                    " Result code = " + (grantResults.length > 0 ? grantResults[0] : "(empty)"));

            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    getActivity().finish();
                }
            };
            Context thisContext = getActivity().getApplicationContext();
            AlertDialog.Builder builder = new AlertDialog.Builder(thisContext);
            builder.setTitle("Multitracker sample")
                    .setMessage(R.string.no_camera_permission)
                    .setPositiveButton(R.string.ok, listener)
                    .show();
        }

        private void startCameraSource() throws SecurityException {
            // Check that the device has play services available.
            int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                    getActivity().getApplicationContext());
            Activity thisActivity= getActivity();
            if (code != ConnectionResult.SUCCESS) {
                Dialog dlg =
                        GoogleApiAvailability.getInstance().getErrorDialog(thisActivity, code, RC_HANDLE_GMS);
                dlg.show();
            }
            if (mCameraSource != null) {
                try {
                    mPreview.start(mCameraSource, mGraphicOverlay);
                } catch (IOException e) {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Unable to start camera source.");
                    } else {
                        Crashlytics.getInstance().core.logException(new RuntimeException("Unable to start camera source."));
                    }
                    mCameraSource.release();
                    mCameraSource = null;
                }
            }
        }

        private boolean onTap(float rawX, float rawY) {
            OcrGraphic graphic = mGraphicOverlay.getGraphicAtLocation(rawX, rawY);
            TextBlock text = null;
            if (graphic != null) {
                text = graphic.getTextBlock();
                if (text != null && text.getValue() != null) {
                    Intent data = new Intent();
                    data.putExtra(TextBlockObject, text.getValue());
                    getActivity().setResult(CommonStatusCodes.SUCCESS, data);
                    getActivity().finish();
                }
                else {
                    Log.d(TAG, "text data is null");
                }
            }
            else {
                Log.d(TAG,"no text detected");
            }
            return text != null;
        }

        private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                return onTap(e.getRawX(), e.getRawY()) || super.onSingleTapConfirmed(e);
            }
        }

        private class ScaleListener implements ScaleGestureDetector.OnScaleGestureListener {

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                return false;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }
            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                mCameraSource.doZoom(detector.getScaleFactor());
            }
        }
}
