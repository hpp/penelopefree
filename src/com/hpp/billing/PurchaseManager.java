package com.hpp.billing;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.view.View;

import com.android.vending.billing.IInAppBillingService;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

public class PurchaseManager {
	Context mContext;
	IInAppBillingService mService;
	public ArrayList<String> skuList, priceList, purchasedList;
	public final static String pitchCorrect = "Pitch Correction",
			adInfinitum = "ad_infinitum";
	public final int pitchCorrect_RequestCode = 102, adInfinitum_RequestCode = 101; 	
	
	
	
	public PurchaseManager(Context context){
		mContext = context;
		mContext.bindService(new 
				Intent("com.android.vending.billing.InAppBillingService.BIND"),
						mServiceConn, Context.BIND_AUTO_CREATE);
		
		skuList = new ArrayList<String>();
		skuList.add(pitchCorrect);
		skuList.add(adInfinitum);
		
		priceList = new ArrayList<String>();
		for (String sku:skuList){
			priceList.add("-.--");
		}
		
		purchasedList = new ArrayList<String>();
	}
	
	ServiceConnection mServiceConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName name, 
		IBinder service) {
			mService = IInAppBillingService.Stub.asInterface(service);
			queryPrices();
			purchasedList = queryPurchases();
		}
	};

	public void unbind() {
	    if (mServiceConn != null) {
	    	mContext.unbindService(mServiceConn);
	    }		
	}
	
	public void queryPrices(){ 
		Bundle querySkus = new Bundle();
		querySkus.putStringArrayList("ITEM_ID_LIST", skuList);
		new queryPurachasePricesTask().execute(querySkus);

	}
	
	private class queryPurachasePricesTask extends AsyncTask<Bundle, Integer, Bundle> {
		protected Bundle doInBackground(Bundle... querySkus) {
			Bundle skuDetails = null;
			try {
				skuDetails = mService.getSkuDetails(3, 
							mContext.getPackageName(), "inapp", querySkus[0]);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return skuDetails;
		}

		protected void onPostExecute(Bundle skuDetails) {
			int response = skuDetails.getInt("RESPONSE_CODE");
			if (response == 0) {
				ArrayList<String> responseList 
				= skuDetails.getStringArrayList("DETAILS_LIST");
				
			for (String thisResponse : responseList) {
				JSONObject object;
				String sku, price;
					try {
						object = new JSONObject(thisResponse);
						sku = object.getString("productId");
						price = object.getString("price");
						priceList.set(skuList.indexOf(sku),price);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}
			}
		}
	}
	
	public boolean purchase(String sku){
		
		try {
			Bundle buyIntentBundle = mService.getBuyIntent(3, mContext.getPackageName(),
							sku, "inapp", "thisIsThePayloadStringPenelopeWantsBack");
			int response = buyIntentBundle.getInt("RESPONSE_CODE");
			if (response == 0) {
				PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
				((Activity) mContext).startIntentSenderForResult(pendingIntent.getIntentSender(),
						pitchCorrect_RequestCode, new Intent(), Integer.valueOf(0), 
						Integer.valueOf(0), Integer.valueOf(0));
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SendIntentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
	}
	
	public ArrayList<String> queryPurchases(){
		Bundle ownedItems = null;
		try {
			ownedItems = mService.getPurchases(3, mContext.getPackageName(), "inapp", null);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (ownedItems == null) return null;
		ArrayList<String> ownedList = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
		
		if (ownedList == null) return null;
		if (ownedList.contains(adInfinitum)){
			ownedList = skuList;
		}
		return ownedList;
	}
	
	/**
	 * this was removed from use
	 * @author kloud9
	 *
	 */
	private class purchaseActivity extends Activity{
		public purchaseActivity(){
			
		}
		
		@Override
		protected void onActivityResult(int requestCode, int resultCode, Intent data) { 
			if (requestCode == 1001) {           
				int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
				String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
				String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");
				
				if (resultCode == RESULT_OK) {
					try {
						JSONObject jo = new JSONObject(purchaseData);
						String sku = jo.getString("productId");
						//alert("You have bought the " + sku + ". Excellent choice adventurer!");
						onPurchaseCompleted(jo);
					}
					catch (JSONException e) {
						//alert("Failed to parse purchase data.");
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public int checkTries(String sku) { 
		if (sku.contains(pitchCorrect)){
			SharedPreferences mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			int tries = mSharedPrefs.getInt(pitchCorrect + " tries", 7);
			return (int) Math.floor(tries/2);
		}
		return 0;
	}

	public boolean useTry(String sku) {
		if (sku.contains(pitchCorrect)){
			SharedPreferences mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
			int tries = mSharedPrefs.getInt(pitchCorrect + " tries", 7);
			mSharedPrefs.edit().putInt(pitchCorrect + " tries", --tries).apply();
			return true;
		}
		return false;  
	}
	
	/*
	 * Called when a purchase is processed and verified.
	 */
	public void onPurchaseCompleted(JSONObject jo) throws JSONException {

		// May return null if EasyTracker has not yet been initialized with a
		// property ID.
		EasyTracker easyTracker = EasyTracker.getInstance(mContext);

		easyTracker.send(MapBuilder.createItem(
				jo.getString("orderId"),               // (String) Transaction ID
				jo.getString("packageName"),      // (String) Product name
				jo.getString("productId"),                  // (String) Product SKU
				"In App Product",        // (String) Product category
				1.99d,                    // (Double) Product price
				1L,                       // (Long) Product quantity
				"USD")                    // (String) Currency code
				.build()
		);
	}
}
