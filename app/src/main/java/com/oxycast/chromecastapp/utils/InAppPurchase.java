package com.oxycast.chromecastapp.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.oxycast.chromecastapp.WebViewActivity;
import com.oxycast.chromecastapp.util.IabBroadcastReceiver;
import com.oxycast.chromecastapp.util.IabHelper;
import com.oxycast.chromecastapp.util.IabResult;
import com.oxycast.chromecastapp.util.Inventory;
import com.oxycast.chromecastapp.util.Purchase;


/**
 * Created by MY on 1/6/2017.
 */

public class InAppPurchase {
    // Debug tag, for logging
    static final String TAG = "Chrome_Case";

    static final String SKU_REMOVE_ADS = "com.chrome.pro";

    // (arbitrary) request code for the purchase flow
    static final int RC_REQUEST = 10111;

    Activity activity;

    // The helper object
    IabHelper mHelper;
    IabBroadcastReceiver mBroadcastReceiver;

   String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArLHYb/wuoei1ljvz95NTRGnAHXM4iSsNAkRegC0u3I+JxBjkTR86MTz0n4tDbbJWBvkfuLCCTzOpE/JeDCAaudfRCH5x1zIaovTKgrgHJtvTMOBtE8F80SQencyDTbnEelaeZF8xg5iu0s/H9kJ8Fms4JvnW2wQnlknbqKS5FgmdIvH1G20RZZ59fuqjZlAafF0GAIAwNMDnO2bNxmzIAheKy0cMhR80QKdtdeQtyFwy6dHNLl7YQ7aLDVgmtQBs58Waqmtg8Eyy/CFlAbIncNpcsnJBV7Znfy7ZQOvXoSc8O0Z/jHBC+zuLw9n1qtkRuUO/rslKFOsYLY2VRtORzQIDAQAB";
    Boolean isAdsDisabled = false;
    String payload = "com.chrome.ety123";

    public InAppPurchase(Activity launcher) {
        this.activity = launcher;
    }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        onCreate();
//    }

    public void onCreate() {

        // Create the helper, passing it our context and the public key to
        // verify signatures with
        Log.d(TAG, "Creating IAB helper.");
        mHelper = new IabHelper(activity, base64EncodedPublicKey);

        // enable debug logging (for a production application, you should set
        // this to false).
        mHelper.enableDebugLogging(true);

        // Start setup. This is asynchronous and the specified listener
        // will be called once setup completes.
        Log.d(TAG, "Starting setup.");
        mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) throws IabHelper.IabAsyncInProgressException {
                Log.d(TAG, "Setup finished.");

                if (!result.isSuccess()) {
                    // Oh noes, there was a problem.
                    Log.e(TAG, "Setup result. not suucess.");

                    // complain("Problem setting up in-app billing: " + result);
                    return;
                }

                // Have we been disposed off in the meantime? If so, quit.
                if (mHelper == null)
                    return;

                mBroadcastReceiver = new IabBroadcastReceiver((IabBroadcastReceiver.IabBroadcastListener) activity);
                IntentFilter broadcastFilter = new IntentFilter(IabBroadcastReceiver.ACTION);
                activity.registerReceiver(mBroadcastReceiver, broadcastFilter);

                // IAB is fully set up. Now, let's get an inventory of stuff we
                // own.
                Log.e(TAG, "Setup successful. Querying inventory.");
                try {
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    complain("Error querying inventory. Another async operation in progress.");
                }            }
        });
    }
    public void receivedBroadcast() {
        // Received a broadcast notification that the inventory of items has changed
       // Toast.makeText(activity,"Received broadcast notification",Toast.LENGTH_LONG).show();
        Log.d(TAG, "Received broadcast notification. Querying inventory.");
        try {
            mHelper.queryInventoryAsync(mGotInventoryListener);
        } catch (IabHelper.IabAsyncInProgressException e) {
            complain("Error querying inventory. Another async operation in progress.");
        }
    }
    // Listener that's called when we finish querying the items and
    // subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result,
                                             Inventory inventory) {
            Log.e(TAG, "Query inventory finished.");


            // Have we been disposed of in the meantime? If so, quit.
            if (mHelper == null)
                return;

            // Is it a failure?
            if (result.isFailure()) {
                // complain("Failed to query inventory: " + result);
                return;
            }
            Purchase gasPurchase = inventory.getPurchase(SKU_REMOVE_ADS);
            if (gasPurchase != null && verifyDeveloperPayload(gasPurchase)) {

              //  return;
            }


            Log.e(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the premium upgrade?
            Purchase removeAdsPurchase = inventory.getPurchase(SKU_REMOVE_ADS);
            isAdsDisabled= (removeAdsPurchase != null && verifyDeveloperPayload(removeAdsPurchase));
            if (isAdsDisabled)
            {
               // b removeAds();

                // MainActivity.mAdView.destroy();
              /*  isAdsDisabled=true;
                SessionManager sessionManager=new SessionManager(activity);
                sessionManager.setIsPro("yes");*/

            }

            //
          //  Toast.makeText(activity,"Query inventory finished.",Toast.LENGTH_SHORT).show();


            Log.e(TAG, "User has "
                    + (isAdsDisabled ? "REMOVED ADS"
                    : "NOT REMOVED ADS"));

            // setWaitScreen(false);
            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    // User clicked the "Remove Ads" button.
    public void purchaseRemoveAds() {

//if (activity==null)
//    return;

        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                try {
                    mHelper.launchPurchaseFlow(activity, SKU_REMOVE_ADS,
                            RC_REQUEST, mPurchaseFinishedListener, payload);
                } catch (IabHelper.IabAsyncInProgressException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    public boolean onActivityResul(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "onActivityResult(" + requestCode + "," + resultCode + ","
                + data);

    //    Toast.makeText(activity,"onActivityResult(" + requestCode + "," + resultCode + ",",Toast.LENGTH_LONG).show();
        if (mHelper == null)
           return true;

        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            return false;
        } else {

            Log.e(TAG, "onActivityResult handled by IABUtil.");

            return true;
        }

    }


    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            Log.e(TAG, "Consumption finished. Purchase: " + purchase + ", result: " + result);

          //  Toast.makeText(activity,"Consumption finished. Purchase: " + purchase + ", result: " + result,Toast.LENGTH_LONG).show();

            // if we were disposed of in the meantime, quit.
            if (mHelper == null) return;

            // We know this is the "gas" sku because it's the only one we consume,
            // so we don't check which sku was consumed. If you have more than one
            // sku, you probably should check...
            if (result.isSuccess()) {
                // successfully consumed, so we apply the effects of the item in our
                // game world's logic, which in our case means filling the gas tank a bit
                Log.d(TAG, "Consumption successful. Provisioning.");
                //saveData();
                //alert("You filled 1/4 tank. Your tank is now " + String.valueOf("") + "/4 full!");
            }
            else {
                complain("Error while consuming: " + result);
            }

            Log.d(TAG, "End consumption flow.");
        }
    };

    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
      String  payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct.
         * It will be the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase
         * and verifying it here might seem like a good approach, but this will
         * fail in the case where the user purchases an item on one device and
         * then uses your app on a different device, because on the other device
         * you will not have access to the random string you originally
         * generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different
         * between them, so that one user's purchase can't be replayed to
         * another user.
         *
         * 2. The payload must be such that you can verify it even when the app
         * wasn't the one who initiated the purchase flow (so that items
         * purchased by the user on one device work on other devices owned by
         * the user).
         *
         * Using your own server to store and verify developer payloads across
         * app installations is recommended.
         */
        return true;
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.e(TAG, "Purchase finished: " + result.getResponse() + ", purchase: "
                    + purchase);
        //   Toast.makeText(activity,"Purchase finished."+result.getResponse(),Toast.LENGTH_LONG).show();


            // if we were disposed of in the meantime, quit.
            if (mHelper == null)
                return;

            if (result.getResponse()==7)
            {
                Toast.makeText(activity,"You already purchased it.",Toast.LENGTH_LONG).show();

                removeAds();
            }

            if (result.isFailure()) {
                complain("Error purchasing: " + result);
                return;
            }
           /* if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.");
                return;
            }



*/
            Log.e(TAG, "Purchase successful.");
            Toast.makeText(activity,"Purchase successful.",Toast.LENGTH_LONG).show();

            if (purchase.getSku().equals(SKU_REMOVE_ADS)) {
                // bought the premium upgrade!
                //hel.consumeAsync (purchase, mConsumeFinishedListener);
                removeAds();


            }
        }
    };

    public void removeAds() {
        isAdsDisabled = true;

       // MainActivity.mAdView.destroy();
        isAdsDisabled=true;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putBoolean("remove_ads",true);
        editor.commit();


        activity.finish();



    }

    public void testing(){

        removeAds();
    }
    // We're being destroyed. It's important to dispose of the helper here!

    public void onDestroy()  {

        // very important:
        Log.d(TAG, "Destroying helper.");
        if (mHelper != null) {
            try {
                mHelper.dispose();
                mHelper = null;
            }
            catch (Exception e){

            }
        }
        if (mBroadcastReceiver != null) {
            activity.unregisterReceiver(mBroadcastReceiver);
        }
    }

    void complain(String message) {
        Log.e(TAG, "**** TrivialDrive Error: " + message);
        alert("Error: " + message);
    }

    void alert(final String message) {
        /*activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                AlertDialog.Builder bld = new AlertDialog.Builder(activity);
                bld.setMessage(message);
                bld.setNeutralButton("OK", null);
                Log.d(TAG, "Showing alert dialog: " + message);
                bld.create().show();
            }
        });*/
    }

}
