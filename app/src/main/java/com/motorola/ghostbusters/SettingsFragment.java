package com.motorola.ghostbusters;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SettingsFragment extends PreferenceFragment implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    //private final String[] keys = { "freq_shift", "fs_density", "noise", "medium_noise",
    //"freq_scan", "cid_im", "quiet_im", "fnm_exit", "samples", "custom_path"};
    private final String[] keys = { "samples", "custom_path"};
    private static String[] entrySet;
    private static String[] valSet;
    private static String[] defSet;
    public static SharedPreferences userPref;
    private static MultiSelectListPreference myMultiList;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_general);
        userPref = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        //myMultiList = (MultiSelectListPreference) findPreference("advanced");
        //setMulti();
        setSummary();
        //myMultiList.setDefaultValue(defVals);
    }

    public void onPause() {
        super.onPause();
        Context context = getActivity();
        PreferenceManager.getDefaultSharedPreferences(context)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onResume() {
        super.onResume();
        setSummary();
        Context context = getActivity();
        PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        setSummary();
    }

    private void setSummary() {

        //SharedPreferences userPref = PreferenceManager
        //        .getDefaultSharedPreferences(getActivity());
        for (int i = 0; i < keys.length; i++) {
            String key_string = keys[i];
            Preference pref = (Preference) findPreference(key_string);
            String value = userPref.getString(key_string, "5");
            pref.setSummary(value);
        }
        //Set<String> selectedVals = myMultiList.getValues();
       // myMultiList.setSummary(getMask());

    }

    private void setMultiList(int resolution) {

        switch (resolution) {
            case 720:
                entrySet = getResources().getStringArray(
                        R.array.drawablesArray720);
                valSet = getResources().getStringArray(
                        R.array.drawablesValues720);
                defSet = getResources().getStringArray(
                        R.array.drawablesDefaults720);
                break;
            case 1080:
                entrySet = getResources().getStringArray(
                        R.array.drawablesArray1080);
                valSet = getResources().getStringArray(
                        R.array.drawablesValues1080);
                defSet = getResources().getStringArray(
                        R.array.drawablesDefaults1080);
                break;
            case 1440:
                entrySet = getResources().getStringArray(
                        R.array.drawablesArray1440);
                valSet = getResources().getStringArray(
                        R.array.drawablesValues1440);
                defSet = getResources().getStringArray(
                        R.array.drawablesDefaults1440);
                break;
            default:
                entrySet = getResources().getStringArray(
                        R.array.drawablesArray720);
                valSet = getResources().getStringArray(
                        R.array.drawablesValues720);
                defSet = getResources().getStringArray(
                        R.array.drawablesDefaults720);
                break;
        }
    }

    public void setMulti() {
        //myMultiList = (MultiSelectListPreference) findPreference("advanced");
        setMultiList(MainActivity.screenWidth);
        //setMultiList(2160); //FOR TEST ONLY!!!!
        myMultiList.setEntries(entrySet);
        myMultiList.setPersistent(true);
        myMultiList.setEntryValues(valSet);

        if (!userPref.getBoolean("isListSet", false)) {
            Set<String> defVals = new HashSet<String>(Arrays.asList(valSet));
            myMultiList.setValues(defVals);
            SharedPreferences.Editor editor = userPref.edit();
            editor.putBoolean("isListSet", true);
            editor.commit();
        }
    }

    private static String getMask() {
        //String[] bitsToAdd = new String [MainActivity.valSet.length];
        String maskAsBinaryString = "";
        String imgMaskAsString = "";
        Set<String> selectedVals = myMultiList.getValues();
        String[] imgs = new String[selectedVals.size()];
        imgs = selectedVals.toArray(new String[]{});

        boolean[] checked = new boolean[valSet.length];
        for (int i=0; i < valSet.length; i++) {
            checked[i] = false;
            for (int j=0; j < imgs.length; j++) {
                if (valSet[i].equals(imgs[j])) {
                    checked[i] = true;
                    continue;
                }
            }
            if (checked[i]) {
                maskAsBinaryString += "1";
            } else {
                maskAsBinaryString += "0";
            }
        }
        String imgMask = new StringBuilder(maskAsBinaryString).reverse().toString();
        int decimal = Integer.parseInt(imgMask,2);
        imgMaskAsString = "0x" + Integer.toString(decimal,16);
        return imgMaskAsString;
    }

    }