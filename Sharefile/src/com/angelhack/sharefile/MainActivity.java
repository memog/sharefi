package com.angelhack.sharefile;

import android.os.Bundle;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.webkit.WebView;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.webkit.JavascriptInterface;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class MainActivity extends Activity {
    public class WebAppInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void requestWifiShare() {
            new Thread(new Runnable() {
                public void run() {
                    requestWifiShare();
                }
            }).start();
        }

        @JavascriptInterface
        public void enableWifiSharing() {
            new Thread(new Runnable() {
                public void run() {
                    startSharing();
                }
            }).start();
        }

        @JavascriptInterface
        public void disableWifiSharing() {
            new Thread(new Runnable() {
                public void run() {
                    stopSharing();
                }
            }).start();
        }
    }

    WifiApManager wifiApManager;
    WifiManager wifiManager;
    WifiConfiguration currentNetConfig;
    WebView webView;

    final String APP_PREFIX = "SHFI";
    final String DONATE_PREFIX = "D";
    final String REQUEST_PREFIX = "R";
    final String USER_IDENTIFIER = "31231";
    final String DONATE_FILTER = APP_PREFIX+"-"+DONATE_PREFIX;
    final String REQUEST_FILTER = APP_PREFIX+"-"+REQUEST_PREFIX;
    final String DONATE_SSID = DONATE_FILTER+"-"+USER_IDENTIFIER;
    final String REQUEST_SSID = REQUEST_FILTER+"-"+USER_IDENTIFIER;

    boolean searchingForSharedWifi = false;
    boolean sharingLookupEnabled = false;
    boolean currentlySharing = false;

    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        webView = (WebView) findViewById(R.id.webView);
        webView.loadUrl("file:///android_asset/login.html");



        wifiApManager = new WifiApManager(this);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        currentNetConfig = new WifiConfiguration();



        new Thread(new Runnable() {
            public void run() {
                startSharing();
            }
        }).start();

	}

	
	public static void showNotification( String contentTitle, String contentText, Context arg0 ) {
		int icon = R.drawable.ic_launcher;
        long when = System.currentTimeMillis();
        
        Notification notification = new Notification(icon, contentTitle, when);

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_LIGHTS;
        Intent notificationIntent = new Intent(arg0, MainActivity.class);
        notificationIntent.putExtra("EnterBool", true);

        PendingIntent contentIntent = PendingIntent.getActivity(arg0, 0, notificationIntent, 0);
        notification.setLatestEventInfo(arg0, contentTitle, contentText, contentIntent);
        
        NotificationManager mNoficiation = (NotificationManager)arg0.getSystemService(Context.NOTIFICATION_SERVICE);
        mNoficiation.notify(1, notification);
	}

    public void requestWifiShare(){
        searchingForSharedWifi = true;
        currentNetConfig.SSID = REQUEST_SSID;
        currentNetConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        enableAp();
        Integer numberOfCycles = 0;
        while(searchingForSharedWifi){
            sleep(20000);
            List<ScanResult> accessPoints = getAccessPoints();
            List<ScanResult> filteredAccessPoints = filterAccessPoints(accessPoints,DONATE_FILTER);
            if(filteredAccessPoints.size()>0){
                ScanResult bestAccessPointAvailable = getBestResult(filteredAccessPoints);
                wifiConnectToAccessPoint(bestAccessPointAvailable);
                searchingForSharedWifi=false;
                break;
            }
            numberOfCycles++;
        }
    }

    public void startSharing(){
        sharingLookupEnabled = true;
        while(sharingLookupEnabled){
            List<ScanResult> accessPoints = getAccessPoints();
            List<ScanResult> filteredAccessPoints = filterAccessPoints(accessPoints,REQUEST_FILTER);
            if(filteredAccessPoints.size()>0){
                currentNetConfig.SSID = "\""+DONATE_SSID+"\"";
                currentNetConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                enableAp();
                currentlySharing=true;
                sharingLookupEnabled=false;
                sleep(1000);
                break;
            }
            sleep(15000);
        }
    }

    public void stopSharing(){
        currentlySharing=false;
        disableAp();
        sleep(1000);
    }

    public boolean enableAp(){
        return wifiApManager.setWifiApEnabled(currentNetConfig,true);
    }

    public boolean disableAp(){
        return wifiApManager.setWifiApEnabled(currentNetConfig,false);
    }

    public void wifiConnectToAccessPoint(ScanResult accessPoint){
        WifiConfiguration wifiConnectConfiguration = new WifiConfiguration();
        wifiConnectConfiguration.SSID = "\""+accessPoint.SSID+"\"";//WTF
        wifiConnectConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        //TODO generate pass
        Integer networkId =  wifiManager.addNetwork(wifiConnectConfiguration);
        wifiManager.disconnect();
        wifiManager.enableNetwork(networkId, true);
        wifiManager.reconnect();
    }

    public List<ScanResult> filterAccessPoints(List<ScanResult> accessPoints,String SSIDPrefix){
        List<ScanResult> results = new ArrayList<ScanResult>();
        for(Iterator<ScanResult> i = accessPoints.iterator(); i.hasNext(); ) {
            ScanResult item = i.next();
            String ssid = item.SSID;
            if(ssid.contains(SSIDPrefix))results.add(item);
        }
        return results;
    }

    public ScanResult getBestResult(List<ScanResult> mScanResults){
        ScanResult bestResult = null;
        for(ScanResult results : mScanResults){
            if(bestResult == null || WifiManager.compareSignalLevel(bestResult.level,results.level) < 0){
                bestResult = results;
                String message = String.format("%s networks found. %s is the strongest.",
                        mScanResults.size(), bestResult.SSID);
            }
        }
        return bestResult;
    }

    public List<ScanResult> getAccessPoints(){
        List<ScanResult> results;
        boolean apWasEnabled = wifiApManager.isWifiApEnabled();
        if(apWasEnabled){
            disableAp();
            sleep(1000);
        }
        boolean wifiEnabled = wifiManager.setWifiEnabled(true);
        sleep(1000);
        wifiManager.startScan();
        sleep(10000);
        results =  wifiManager.getScanResults();
        boolean wifiDisabled = wifiManager.setWifiEnabled(false);
        sleep(1000);
        if(apWasEnabled){
            enableAp();
            sleep(1000);
        }
        return results;
    }

    public float getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        if(level == -1 || scale == -1) {
            return 50.0f;
        }
        return ((float)level / (float)scale) * 100.0f;
    }

    public void sleep(Integer secs){
        try {
            Thread.sleep(secs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
