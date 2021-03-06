package com.motorola.ghostbusters;

import android.util.Log;

/**
 * Created by elenalast on 6/10/15.
 */
public class TouchDevice {

    public native static int diagInit(String jpath);
    public native static int diagGearCount();
    public native static int diagGearsEnabled();
    public native static int diagGearCurrent();
    public native static void diagGearAuto(int disable);
    public native static void diagGearSelect(int gear);
    public native static int diagPowerIM();
    public native static int diagCoherentIM();
    public native static int diagDeltaPeaks(int loops);
    public native static int[] diagDeltaPeaks2(int loops);
    public native static int diagRxDeltaPeaks(int loops);
    public native static int diagFrameY();
    public native static int diagFrameX();
    public native static short[] diagDeltaFrame();
    public native static void diagDisableTouch();
    public native static void diagEnableTouch();
    public native static int diagTouchEventCount();
    public native static int diagFingerThreshold();
    public native static int diagFingerHysteresis();
    public native static int diagSaturationLevel();
    public native static void diagEnableGears(int gears);
    public native static void diagForceUpdate();
    public native static String[] diagStats();
    public native static void diagResetTouch();
    public native static void diagClose();

    public native static int diagHasHybridBaselineControl();
    public native static int diagEnHybridOnRx();
    public native static int diagEnHybridOnTx();
    public native static int diagTxObjThresh();
    public native static int diagRxObjThresh();
    public native static int diagSetTxObjThresh(int thresh);
    public native static int diagSetRxObjThresh(int thresh);
    public native static int[] diagRxTxDeltaPeaks(int loops, int report_type);
    public native static int[] diagRxTxDeltaPeaks2(int loops, int report_type);

    public native static int diagSetHybridIntDur(int val);
    public native static int diagHybridIntDur();

    public native static int diagSetHybridStretchDur(int val);
    public native static int diagHybridStretchDur();

    public native static int diagSetTranscapIntDur(int val);
    public native static int diagTranscapIntDur();

    // must call diagForceUpdate after this function for change to take effect
    public native static int diagSetC95FilterBwBurstLen(int filter_bw);

    public static int gearCount;
    private final String TAG = "Ghostbusters";

    public TouchDevice () {
         // load JNI lib
         System.loadLibrary("synaptics");
        //diagInit();
    }

    public void testTouch() {
        Log.d(TAG, "call test ");
        //diagInit();
        gearCount = diagGearCount();
        Log.d(TAG, "gears " + gearCount);
        Log.d(TAG, "gearsEnabled " + Integer.toHexString(diagGearsEnabled()));
        Log.d(TAG, "gearCurrent " + diagGearCurrent());
        //public native void diagGearAuto();
        //public native void diagGearSelect(int gear);
        Log.d(TAG, "PowerIM " + diagPowerIM());
        Log.d(TAG, "Coherent IM " + diagCoherentIM());
        Log.d(TAG, "Delta Peaks " + diagDeltaPeaks(0));
        Log.d(TAG, "Frame Y " +  diagFrameY());
        Log.d(TAG, "Frame X " + diagFrameX());
        //ArrayList<Integer> tmp = new ArrayList<Integer>();
        //tmp = diagDeltaFrame();
        //for (int i=0; i < diagFrameTx() * diagFrameRx(); i++) {
        //    Log.d(TAG, tmp[i] + " ");
        //}
        //public native void diagDisableTouch();
        //public native void diagEnableTouch();
        Log.d(TAG, "Saturation level " + diagSaturationLevel());
        Log.d(TAG, "Finger Threshold " + diagFingerThreshold());
        Log.d(TAG, "Finger Hysteresis " + diagFingerHysteresis());

        //public native void diagClose();
    }
}
