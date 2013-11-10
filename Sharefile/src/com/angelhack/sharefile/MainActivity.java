package com.angelhack.sharefile;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.webkit.*;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;

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
            requestWifiShare();
        }

        @JavascriptInterface
        public void enableWifiSharing() {
            startSharing();
        }

        @JavascriptInterface
        public void disableWifiSharing() {
            stopSharing();
        }
    }

    WifiApManager wifiApManager;
    WifiManager wifiManager;
    WifiConfiguration currentNetConfig;
    WebView webView;


    final String APP_PREFIX = "SHFI";
    final String DONATE_PREFIX = "D";
    final String REQUEST_PREFIX = "R";
    final String USER_IDENTIFIER = "TEST";
    final String DONATE_FILTER = APP_PREFIX+"-"+DONATE_PREFIX;
    final String REQUEST_FILTER = APP_PREFIX+"-"+REQUEST_PREFIX;
    final String DONATE_SSID = DONATE_FILTER+"-"+USER_IDENTIFIER;
    final String REQUEST_SSID = REQUEST_FILTER+"-"+USER_IDENTIFIER;

    boolean searchingForSharedWifi = false;
    boolean sharingLookupEnabled = false;
    boolean currentlySharingWifi = false;
    boolean firstClientConnected = false;
    boolean hasInternetConnection = false;
    boolean connectedToSharedWifi = false;

    Integer connectedClients = 0;

    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

        wifiApManager = new WifiApManager(this);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        currentNetConfig = new WifiConfiguration();

        webView = (WebView)findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccessFromFileURLs(true); //Maybe you don't need this rule
        webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {

            }

            /*@Override
            public WebResourceResponse shouldInterceptRequest (final WebView view, String url) {
                String filePath = url.substring(url.indexOf(appUrl)+appUrl.length());
                try {
                    return new WebResourceResponse("text/css", "UTF-8", getAssets().open("style.css"));
                } catch (IOException e) {
                    return super.shouldInterceptRequest(view, url);
                }
            }*/
        });

        webView.setWebChromeClient(new WebChromeClient());

        if(hasInternetConnection = isNetworkAvailable()){
            webView.loadUrl("file:///android_asset/login.html");
        }else{
            webView.loadUrl("file:///android_asset/home.html");
        }

        currentNetConfig.SSID = "\""+DONATE_SSID+"\"";
        currentNetConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);

        enableAp();
        //requestWifiShare();
        //currentlySharingWifi = true;
        //clientsWatcher();
	}

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void internetWatcher(){
        new Thread(new Runnable() {//Clients watcher
            public void run() {
                boolean internetAvailable = isNetworkAvailable();
                if(hasInternetConnection!=internetAvailable){
                    if(internetAvailable){//Se conecto
                        if(connectedToSharedWifi){

                        }
                        if(currentlySharingWifi){

                        }
                    }else{//Se desconecto
                        if(connectedToSharedWifi){

                        }
                        if(currentlySharingWifi){

                        }
                    }
                }
                hasInternetConnection=internetAvailable;
                sleep(15000);
            }
        }).start();
    }

    public void clientsWatcher(){
        new Thread(new Runnable() {//Clients watcher
            public void run() {
                while(true){
                    if(currentlySharingWifi){
                        ArrayList clients =  wifiApManager.getClientList(true,1000);
                        Integer clientsCount = clients.size();
                        if(clientsCount>0 && connectedClients==0){//Se acaba de conectar el primer cliente
                            if(!firstClientConnected){
                                //TODO notify that first user has connected
                                startCountdown();
                                firstClientConnected = true;
                            }
                        }
                        if(clientsCount==0 && connectedClients>0){//Se desconectaron todos los usuarios
                            String a = "a";

                        }
                        if(clientsCount!=connectedClients){
                            showNotification("Share-fi",clientsCount.toString()+" usuario conectado",MainActivity.this);
                            connectedClients = clientsCount;
                        }
                        sleep(30000);
                    }
                }
            }
        }).start();
    }

    public void startCountdown(){
        new Thread(new Runnable() {
            public void run() {
                sleep(2000);
                disableAp();
                resetSharingVars();
            }
        }).start();
    }

    public void resetSharingVars(){
        sharingLookupEnabled = false;
        currentlySharingWifi = false;
        firstClientConnected = false;
        connectedClients = 0;
    }

    public void resetRequestVars(){
        connectedToSharedWifi = false;
        searchingForSharedWifi = false;
    }
	
	public static void showNotification( String contentTitle, String contentText, Context arg0 ) {
		int icon = R.drawable.ic_launcher;
        long when = System.currentTimeMillis();
        
        Notification notification = new Notification(icon, contentTitle, when);

        notification.flags |= Notification.FLAG_AUTO_CANCEL;
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
        if(searchingForSharedWifi || connectedToSharedWifi || isNetworkAvailable())return;

        new Thread(new Runnable() {//Clients watcher
            public void run() {
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
                        connectedToSharedWifi = true;
                        //TODO notify ui that connection has been made! AHUEVO!
                        searchingForSharedWifi=false;
                        break;
                    }
                    numberOfCycles++;
                }
            }
        }).start();
    }

    public void startSharing(){
        if(currentlySharingWifi || sharingLookupEnabled || !isNetworkAvailable())return;//It is already searching for connections

        new Thread(new Runnable() {//Clients watcher
            public void run() {
                resetSharingVars();
                sharingLookupEnabled = true;
                while(sharingLookupEnabled || isNetworkAvailable()){
                    List<ScanResult> accessPoints = getAccessPoints();
                    List<ScanResult> filteredAccessPoints = filterAccessPoints(accessPoints,REQUEST_FILTER);
                    if(filteredAccessPoints.size()>0){
                        currentNetConfig.SSID = "\""+DONATE_SSID+"\"";
                        currentNetConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                        enableAp();
                        currentlySharingWifi = true;
                        sharingLookupEnabled=false;
                        sleep(1000);
                        break;
                    }
                    sleep(15000);
                }
            }
        }).start();
    }

    public void stopSharing(){
        new Thread(new Runnable() {//Clients watcher
            public void run() {
                currentlySharingWifi =false;
                disableAp();
                resetSharingVars();
                sleep(1000);
            }
        }).start();
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

    public void messageFromTheUnderworld(String eventName,String data){
        webView.loadUrl("javascript:messageFromTheUnderworld('"+eventName+"','"+data+"')");
    }

}
