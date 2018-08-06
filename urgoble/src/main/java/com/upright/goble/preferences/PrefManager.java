package com.upright.goble.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.upright.goble.R;
import com.upright.goble.utils.Logger;

public class PrefManager {

    private static final String PREFS_NAME = "urgoble";

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    Context context;


    public PrefManager(Context context) {
        this.context = context;
        sharedPref = context.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);

        editor = sharedPref.edit();
    }

    public void setMacAddress(String address) {
        editor.putString(context.getString(R.string.pref_mac_address), address);
        editor.commit();
        Logger.log("sharedPref: saveMacAddress: " + address);
    }

    public String getMacAddress() {
        String address = sharedPref.getString(context.getString(R.string.pref_mac_address), "");
        Logger.log("sharedPref: getMacAddress: " + address);
        return address;
    }
}
