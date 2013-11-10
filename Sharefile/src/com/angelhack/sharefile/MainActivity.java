package com.angelhack.sharefile;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.os.Handler;
import android.webkit.*;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;


public class MainActivity extends Activity {
    public class WebAppInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void uiRequestWifiShare() {
            fastRequestWifiShare();
        }

        @JavascriptInterface
        public void uiEnableWifiSharing() {
            startSharing();
        }

        @JavascriptInterface
        public void uiDisableWifiSharing() {
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
    String USER_IDENTIFIER;
    final String DONATE_FILTER = APP_PREFIX+"-"+DONATE_PREFIX;
    final String REQUEST_FILTER = APP_PREFIX+"-"+REQUEST_PREFIX;
    String DONATE_SSID;
    String REQUEST_SSID;

    boolean searchingForSharedWifi = false;
    boolean sharingLookupEnabled = false;
    boolean currentlySharingWifi = false;
    boolean firstClientConnected = false;
    boolean hasInternetConnection = false;
    boolean connectedToSharedWifi = false;
    boolean fastSharingEnabled = false;

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
        webView.getSettings().setDatabasePath("/data/data/com.angelhack.sharefile/databases/");
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        Handler handler = new Handler();

        Random rand = new Random();
        rand.setSeed(System.currentTimeMillis());
        int n = rand.nextInt(10000) + 1;
        USER_IDENTIFIER = "TEST"+n;
        DONATE_SSID = DONATE_FILTER+"-"+USER_IDENTIFIER;
        REQUEST_SSID = REQUEST_FILTER+"-"+USER_IDENTIFIER;

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


        /*currentNetConfig.hiddenSSID = true;
        currentNetConfig.status = WifiConfiguration.Status.ENABLED;
        currentNetConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        currentNetConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        currentNetConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        currentNetConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        currentNetConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        currentNetConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);*/

        currentNetConfig.SSID = "\""+DONATE_SSID+"\"";
        currentNetConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        //currentNetConfig.preSharedKey  = "\"olakase123\"";
        //enableAp();
        //disableAp();
        //sleep(500);
        //wifiManager.setWifiEnabled(false);
        //requestWifiShare();
        //currentlySharingWifi = true;
        clientsWatcher();
        internetWatcher();
        //List<ScanResult> list = filterAccessPoints(getAccessPoints(),DONATE_FILTER);
        //ScanResult sr = getBestResult(list);
        //wifiConnectToAccessPoint(sr);
        //startSharing();
        //getAccessPoints();
        fastSharing();
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
                                //startCountdown();
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
                        sleep(15000);
                    }
                }
            }
        }).start();
    }

    public void fastSharing(){
        new Thread(new Runnable() {
            public void run() {
                currentlySharingWifi=true;
                fastSharingEnabled = true;
                currentNetConfig.SSID = "\""+DONATE_SSID+"\"";
                currentNetConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                enableAp();
            }
        }).start();
    }

    public void start0down(){
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
        if(searchingForSharedWifi || connectedToSharedWifi)return;

        new Thread(new Runnable() {//Clients watcher
            public void run() {
                searchingForSharedWifi = true;
                currentNetConfig.SSID = "\""+REQUEST_SSID+"\"";
                currentNetConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                Integer numberOfCycles = 0;
                while(searchingForSharedWifi){
                    sleep(1000);
                    enableAp();
                    sleep(60000);
                    disableAp();
                    sleep(1000);
                    List<ScanResult> accessPoints = getAccessPoints();
                    List<ScanResult> filteredAccessPoints = filterAccessPoints(accessPoints,DONATE_FILTER);
                    if(filteredAccessPoints.size()>0){
                        disableAp();
                        ScanResult bestAccessPointAvailable = getBestResult(filteredAccessPoints);
                        wifiConnectToAccessPoint(bestAccessPointAvailable);
                        connectedToSharedWifi = true;
                        //TODO notify ui that connection has been made! AHUEVO!
                        searchingForSharedWifi=false;
                        break;
                    }
                    if(numberOfCycles++>20)break;
                }
            }
        }).start();
    }

    public void fastRequestWifiShare(){
        new Thread(new Runnable() {//Clients watcher
            public void run() {
                searchingForSharedWifi = true;
                Integer numberOfCycles = 0;
                while(searchingForSharedWifi){
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
                    if(numberOfCycles++>50)break;
                    sleep(3000);
                }
            }
        }).start();
    }

    public void startSharing(){
        if(currentlySharingWifi || sharingLookupEnabled)return;//It is already searching for connections

        new Thread(new Runnable() {//Clients watcher
            public void run() {
                resetSharingVars();
                sharingLookupEnabled = true;
                while(sharingLookupEnabled){
                    sleep(2000);
                    List<ScanResult> accessPoints = getAccessPoints();
                    List<ScanResult> filteredAccessPoints = filterAccessPoints(accessPoints,REQUEST_FILTER);
                    if(filteredAccessPoints.size()>0){
                        currentNetConfig.SSID = "\""+DONATE_SSID+"\"";
                        //currentNetConfig.preSharedKey = "\"olakase123\"";
                        currentNetConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                        sleep(1000);
                        enableAp();
                        sleep(1000);
                        currentlySharingWifi = true;
                        sharingLookupEnabled=false;
                        sleep(500);
                        break;
                    }
                    sleep(1000);
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
                sleep(200);
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
        disableAp();
        sleep(1000);
        wifiManager.setWifiEnabled(true);

        sleep(2500);
        WifiConfiguration wifiConnectConfiguration = new WifiConfiguration();

        /*wifiConnectConfiguration.hiddenSSID = true;
        wifiConnectConfiguration.status = WifiConfiguration.Status.ENABLED;
        wifiConnectConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wifiConnectConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wifiConnectConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wifiConnectConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wifiConnectConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wifiConnectConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);*/

        wifiConnectConfiguration.SSID = "\""+accessPoint.SSID+"\"";
        wifiConnectConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        //wifiConnectConfiguration.preSharedKey  = "\"\"olakase123\"\"";

        //TODO generate pass
        Integer networkId =  wifiManager.addNetwork(wifiConnectConfiguration);
        sleep(4000);
        wifiManager.disconnect();
        sleep(4000);
        new asyncConnect().execute(networkId.toString());
    }

    public List<ScanResult> filterAccessPoints(List<ScanResult> accessPoints,String SSIDPrefix){
        List<ScanResult> results = new ArrayList<ScanResult>();
        if(accessPoints==null)return results;
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
            sleep(300);
        }
        boolean wifiEnabled = wifiManager.setWifiEnabled(true);
        sleep(1000);
        wifiManager.startScan();
        sleep(10000);
        results =  wifiManager.getScanResults();
        boolean wifiDisabled = wifiManager.setWifiEnabled(false);
        sleep(300);
        if(apWasEnabled){
            enableAp();
            sleep(300);
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

    public void messageFromAndroid(String eventName,String data){
        webView.loadUrl("javascript:messageFromAndroid('"+eventName+"','"+data+"')");
    }

    String sha1Hash( String toHash )
    {
        String hash = null;
        try
        {
            MessageDigest digest = MessageDigest.getInstance( "SHA-1" );
            byte[] bytes = toHash.getBytes("UTF-8");
            digest.update(bytes, 0, bytes.length);
            bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for( byte b : bytes )
            {
                sb.append( String.format("%02X", b) );
            }
            hash = sb.toString();
        }
        catch( NoSuchAlgorithmException e )
        {
            e.printStackTrace();
        }
        catch( UnsupportedEncodingException e )
        {
            e.printStackTrace();
        }
        return hash;
    }

    class asyncConnect extends AsyncTask<String, String, String> {

        String netId;
        protected void onPreExecute() {

        }

        protected String doInBackground(String... params) {
            sleep(3000);
            netId = params[0];
            wifiManager.enableNetwork(Integer.valueOf(netId), true);
            sleep(3000);
            wifiManager.reconnect();
            sleep(3000);
            return "lol";
        }

        protected void onPostExecute() {



        }

    }
}
