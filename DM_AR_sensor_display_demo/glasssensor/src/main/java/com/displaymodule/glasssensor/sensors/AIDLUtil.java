package com.displaymodule.glasssensor.sensors;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.util.List;

public class AIDLUtil {
    public static boolean bindAIDLService(Context pContext, ServiceConnection pConnection, String action) {
        boolean isBind = false;
        if (pContext != null && action != null && pConnection != null) {
            try {
                Intent intent = new Intent(getExplicitIntent(pContext, new Intent().setAction(action)));
//                Intent intent = new Intent(action);
//                ComponentName component = new ComponentName("com.tcl.usbservice", "com.tcl.usbservice.UsbService");
//                intent.setComponent(component);
                pContext.startService(intent);
                isBind = pContext.bindService(intent, pConnection, Context.BIND_AUTO_CREATE);
            } catch (Exception e) {
                Log.e("AIDL", e.getMessage());
            }
        }
        return isBind;
    }

    private static Intent getExplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);
        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }
        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);
        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);
        // Set the component to be explicit
        explicitIntent.setComponent(component);
        return explicitIntent;
    }
}
