package com.amolg.flutterbarcodescanner;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.common.api.CommonStatusCodes;
// import com.google.android.gms.vision.barcode.Barcode; // Old Vision API
import com.google.mlkit.vision.barcode.common.Barcode; // ML Kit Vision API

import java.util.Map;

import io.flutter.embedding.android.FlutterActivity;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;
import io.flutter.plugin.common.EventChannel.StreamHandler;
import io.flutter.embedding.engine.plugins.lifecycle.FlutterLifecycleAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;


/**
 * FlutterBarcodeScannerPlugin
 */
public class FlutterBarcodeScannerPlugin implements MethodCallHandler, ActivityResultListener, StreamHandler, FlutterPlugin, ActivityAware {
    private static final String CHANNEL = "flutter_barcode_scanner";
    public static final String ACTION_BARCODE_SCANNED = "com.amolg.flutterbarcodescanner.BARCODE_SCANNED";
    public static final String EXTRA_BARCODE_RAW_VALUE = "rawValue";

    private FlutterActivity activity;
    private Result pendingResult;
    private Map<String, Object> arguments;

    private static final String TAG = FlutterBarcodeScannerPlugin.class.getSimpleName();
    private static final int RC_BARCODE_CAPTURE = 9001;
    public static String lineColor = "";
    public static boolean isShowFlashIcon = false;
    public static boolean isContinuousScan = false; 
    
    private EventChannel.EventSink barcodeStream; // Instance field
    private EventChannel eventChannel;
    private BroadcastReceiver barcodeScanReceiver; // Instance field

    private MethodChannel channel;
    private FlutterPluginBinding pluginBinding;
    private ActivityPluginBinding activityBinding;
    private Application applicationContext; // Initialized in onAttachedToEngine
    private Lifecycle lifecycle;
    private LifeCycleObserver observer;

    public FlutterBarcodeScannerPlugin() {
    }

    // private FlutterBarcodeScannerPlugin(FlutterActivity activity, final PluginRegistry.Registrar registrar) { // Removed v1 constructor
    //     FlutterBarcodeScannerPlugin.activity = activity;
    // }

    /**
     * Plugin registration.
     */
    // public static void registerWith(final PluginRegistry.Registrar registrar) { // Removed v1 registration
    //     if (registrar.activity() == null) {
    //         return;
    //     }
    //     Activity activity = registrar.activity();
    //     Application applicationContext = null;
    //     if (registrar.context() != null) {
    //         applicationContext = (Application) (registrar.context().getApplicationContext());
    //     }
    //     FlutterBarcodeScannerPlugin instance = new FlutterBarcodeScannerPlugin((FlutterActivity) registrar.activity(), registrar);
    //     instance.createPluginSetup(registrar.messenger(), applicationContext, activity, registrar, null);
    // }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        try {
            this.pendingResult = result; // Use instance field

            if (call.method.equals("scanBarcode")) {
                if (!(call.arguments instanceof Map)) {
                    throw new IllegalArgumentException("Plugin not passing a map as parameter: " + call.arguments);
                }
                arguments = (Map<String, Object>) call.arguments;
                lineColor = (String) arguments.get("lineColor");
                isShowFlashIcon = (boolean) arguments.get("isShowFlashIcon");
                if (null == lineColor || lineColor.equalsIgnoreCase("")) {
                    lineColor = "#DC143C";
                }
                if (null != arguments.get("scanMode")) {
                    if ((int) arguments.get("scanMode") == BarcodeCaptureActivity.SCAN_MODE_ENUM.DEFAULT.ordinal()) {
                        BarcodeCaptureActivity.SCAN_MODE = BarcodeCaptureActivity.SCAN_MODE_ENUM.QR.ordinal();
                    } else {
                        BarcodeCaptureActivity.SCAN_MODE = (int) arguments.get("scanMode");
                    }
                } else {
                    BarcodeCaptureActivity.SCAN_MODE = BarcodeCaptureActivity.SCAN_MODE_ENUM.QR.ordinal();
                }

                isContinuousScan = (boolean) arguments.get("isContinuousScan");

                startBarcodeScannerActivityView((String) arguments.get("cancelButtonText"), isContinuousScan);
            }
        } catch (Exception e) {
            Log.e(TAG, "onMethodCall: " + e.getLocalizedMessage());
        }
    }

    private void startBarcodeScannerActivityView(String buttonText, boolean isContinuousScan) {
        try {
            Intent intent = new Intent(activity, BarcodeCaptureActivity.class).putExtra("cancelButtonText", buttonText);
            if (isContinuousScan) {
                activity.startActivity(intent);
            } else {
                activity.startActivityForResult(intent, RC_BARCODE_CAPTURE);
            }
        } catch (Exception e) {
            Log.e(TAG, "startView: " + e.getLocalizedMessage());
        }
    }


    /**
     * Get the barcode scanning results in onActivityResult
     *
     * @param requestCode
     * @param resultCode
     * @param data
     * @return
     */
    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    try {
                        // Try to get the ML Kit Barcode object first
                        com.google.mlkit.vision.barcode.common.Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                        if (barcode != null) {
                            String barcodeResult = barcode.getRawValue();
                            this.pendingResult.success(barcodeResult != null ? barcodeResult : "-1"); // Use instance field
                        } else {
                            // Fallback to "BarcodeRawValue" if the object is null (e.g. if activity passed it as String)
                            String barcodeResult = data.getStringExtra("BarcodeRawValue");
                            if (barcodeResult != null) {
                                this.pendingResult.success(barcodeResult); // Use instance field
                            } else {
                                Log.e(TAG, "Barcode data is null in onActivityResult.");
                                this.pendingResult.success("-1"); // Use instance field
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing barcode result: " + e.getMessage());
                        this.pendingResult.success("-1"); // Use instance field
                    }
                } else {
                    Log.e(TAG, "Intent data is null in onActivityResult.");
                    this.pendingResult.success("-1"); // Use instance field
                }
                this.pendingResult = null; // Use instance field
                arguments = null;
                return true;
            } else {
                this.pendingResult.success("-1"); // Use instance field
            }
        }
        return false;
    }


    @Override
    public void onListen(Object o, EventChannel.EventSink eventSink) {
        this.barcodeStream = eventSink; // Assign to instance field
        // Ensure applicationContext is available
        if (this.applicationContext == null && this.pluginBinding != null) {
            this.applicationContext = (Application) this.pluginBinding.getApplicationContext();
        }

        if (this.applicationContext != null) {
            this.barcodeScanReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction() != null && intent.getAction().equals(ACTION_BARCODE_SCANNED)) {
                        String rawValue = intent.getStringExtra(EXTRA_BARCODE_RAW_VALUE);
                        if (rawValue != null && FlutterBarcodeScannerPlugin.this.barcodeStream != null) {
                            if (FlutterBarcodeScannerPlugin.this.activity != null) {
                               FlutterBarcodeScannerPlugin.this.activity.runOnUiThread(() -> {
                                   FlutterBarcodeScannerPlugin.this.barcodeStream.success(rawValue);
                               });
                            } else {
                                Log.w(TAG, "Activity context is null in broadcast receiver. Attempting to send barcode data directly.");
                                FlutterBarcodeScannerPlugin.this.barcodeStream.success(rawValue);
                            }
                        }
                    }
                }
            };
            LocalBroadcastManager.getInstance(this.applicationContext).registerReceiver(this.barcodeScanReceiver, new IntentFilter(ACTION_BARCODE_SCANNED));
        } else {
            Log.e(TAG, "ApplicationContext is null, cannot register barcodeScanReceiver for EventChannel.");
        }
    }

    @Override
    public void onCancel(Object o) {
        if (this.barcodeScanReceiver != null && this.applicationContext != null) {
            LocalBroadcastManager.getInstance(this.applicationContext).unregisterReceiver(this.barcodeScanReceiver);
            this.barcodeScanReceiver = null;
        }
        this.barcodeStream = null; // Clear instance field
    }

    // public static void onBarcodeScanReceiver removed

    @Override
    public void onAttachedToEngine(FlutterPluginBinding binding) {
        pluginBinding = binding;
        this.applicationContext = (Application) binding.getApplicationContext(); // Initialize applicationContext
        // Initialize channels here
        channel = new MethodChannel(binding.getBinaryMessenger(), CHANNEL);
        channel.setMethodCallHandler(this);

        eventChannel = new EventChannel(binding.getBinaryMessenger(), "flutter_barcode_scanner_receiver");
        eventChannel.setStreamHandler(this);
    }

    @Override
    public void onDetachedFromEngine(FlutterPluginBinding binding) {
        pluginBinding = null;
        // Clear channel handlers
        if (channel != null) {
            channel.setMethodCallHandler(null);
            channel = null;
        }
        if (eventChannel != null) {
            eventChannel.setStreamHandler(null);
            eventChannel = null;
        }
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    /**
     * Setup method
     * Created after Embedding V2 API release
     *
     * @param messenger
     * @param applicationContext
     * @param activity
     * @param registrar
     * @param activityBinding
     */
    private void createPluginSetup(
            // final BinaryMessenger messenger, // Messenger obtained from pluginBinding in onAttachedToEngine
            final Application applicationContext,
            final Activity activity,
            // final PluginRegistry.Registrar registrar, // Removed
            final ActivityPluginBinding activityBinding) {

        // 'activity' is already an instance field, assigned in onAttachedToActivity
        // this.activity = (FlutterActivity) activity; 

        // eventChannel is initialized in onAttachedToEngine
        // eventChannel =
        //         new EventChannel(messenger, "flutter_barcode_scanner_receiver");
        // eventChannel.setStreamHandler(this);

        this.applicationContext = applicationContext;
        // channel is initialized in onAttachedToEngine
        // channel = new MethodChannel(messenger, CHANNEL);
        // channel.setMethodCallHandler(this);

        // Remove V1 embedding setup for activity listeners.
        // if (registrar != null) {
        //     observer = new LifeCycleObserver(activity);
        //     applicationContext.registerActivityLifecycleCallbacks(
        //             observer); 
        //     registrar.addActivityResultListener(this);
        // } else { // This block becomes the main logic

        // V2 embedding setup for activity listeners.
        activityBinding.addActivityResultListener(this);
        lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(activityBinding);
        // Ensure observer is instantiated with the correct activity instance
        observer = new LifeCycleObserver(this.activity); 
        lifecycle.addObserver(observer);
        // }
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        activityBinding = binding;
        this.activity = (FlutterActivity) binding.getActivity(); 
        if (this.applicationContext == null && pluginBinding != null) { // Ensure applicationContext is set if not already
            this.applicationContext = (Application) pluginBinding.getApplicationContext();
        } else if (this.applicationContext == null) {
            this.applicationContext = this.activity.getApplication();
        }
        createPluginSetup(
                this.applicationContext, // Pass application context
                this.activity, 
                activityBinding);
    }

    @Override
    public void onDetachedFromActivity() {
        clearPluginSetup();
    }

    /**
     * Clear plugin setup
     */
    private void clearPluginSetup() {
        this.activity = null; // Clear instance activity
        if (activityBinding != null) {
            activityBinding.removeActivityResultListener(this);
        }
        activityBinding = null;
        if (lifecycle != null) {
            if (observer != null) {
                lifecycle.removeObserver(observer);
                // applicationContext.unregisterActivityLifecycleCallbacks(observer); 
            }
            lifecycle = null;
        }
        observer = null; 
        
        // Do not nullify applicationContext here, it's managed by onAttachedToEngine/onDetachedFromEngine
        // if (applicationContext != null && observer != null) { 
            // applicationContext.unregisterActivityLifecycleCallbacks(observer);
        // }
        // applicationContext = null; 
    }

    /**
     * Activity lifecycle observer
     */
    private class LifeCycleObserver
            implements Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
        private final Activity thisActivity;

        LifeCycleObserver(Activity activity) {
            this.thisActivity = activity;
        }

        @Override
        public void onCreate(@NonNull LifecycleOwner owner) {
        }

        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
        }

        @Override
        public void onResume(@NonNull LifecycleOwner owner) {
        }

        @Override
        public void onPause(@NonNull LifecycleOwner owner) {
        }

        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            onActivityStopped(thisActivity);
        }

        @Override
        public void onDestroy(@NonNull LifecycleOwner owner) {
            onActivityDestroyed(thisActivity);
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            // V2 embedding handles lifecycle observer differently, no need to unregister from Application context
            // if (thisActivity == activity && activity.getApplicationContext() != null) {
            //     ((Application) activity.getApplicationContext())
            //             .unregisterActivityLifecycleCallbacks(
            //                     this);
            // }
            if (thisActivity == activity) {
                // If the observed activity is destroyed, and if we tied the observer to application context,
                // we might unregister it here. But with V2, observer is tied to activity's lifecycle.
            }
        }

        @Override
        public void onActivityStopped(Activity activity) {

        }
    }
}