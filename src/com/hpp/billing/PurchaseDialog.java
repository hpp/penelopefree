package com.hpp.billing;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import com.harmonicprocesses.penelopefree.R;
import com.harmonicprocesses.penelopefree.audio.AudioProcessor;
import com.harmonicprocesses.penelopefree.settings.SettingsFragment;
import com.harmonicprocesses.penelopefree.settings.UpSaleDialog;

public class PurchaseDialog extends DialogFragment{
    private int mId1 = R.string.dialog_purchase_pitch_correct,
    mId2 = R.string.dialog_purchase_please_try;
    int numButtons = 2;
    int button1id = R.string.dialog_button_try,
    button2id = R.string.dialog_button_buy;
    DialogInterface.OnClickListener listener1, listener2;
    SettingsFragment sp;
    String sku;
    
    /**
     * The root Purchase Manager Instance, passed in from the UI Acitivity
     */
    PurchaseManager pm;
    
    AudioProcessor ap;
	 
    
    /**
     * Constructor 
     * 
     * @param purchaseManager, this needs to be the root Purchase Manager 
     * Instance, passed in from the UI Acitivity
     */
    public PurchaseDialog(){
    	super();
		listener2 = PurchaseListener;
		listener1 = TryListener;
		
    }
    
    public PurchaseDialog setPurchaseManager(PurchaseManager purchaseManager){
    	pm = purchaseManager;
    	return this;
    }
    
    public PurchaseDialog setAudioProcessor(AudioProcessor audioProcessor){
    	ap = audioProcessor;
    	return this;
    }
    
    public PurchaseDialog setSettingsFragment(SettingsFragment settingsFragment) {
		sp = settingsFragment;
		return this;
	}
    
    public PurchaseDialog setSku(String Sku){
    	sku = Sku;
    	return this;
    }
    
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
		Bundle bundle=getArguments();
		//Activity act = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String msg = getString(mId1) + pm.priceList.get(pm.skuList.indexOf(sku)) +
        			getString(mId2);
        builder.setMessage(msg)
            	.setPositiveButton(button1id, listener1);
        
        if (numButtons>1){
	        builder.setNegativeButton(button2id, listener2);
        }
        // Create the AlertDialog object and return it
        return builder.create();
    }
	
	DialogInterface.OnClickListener PurchaseListener = new DialogInterface.OnClickListener() {
		
		@Override
		public void onClick(DialogInterface dialog, int id) {
			pm.purchase(sku);
			
       	}
   	};
   	
   	DialogInterface.OnClickListener TryListener = new DialogInterface.OnClickListener() {
		
   		@Override
		public void onClick(DialogInterface dialog, int which) {
			int tries = pm.checkTries(sku);
			if (tries>0){
				if (pm.useTry(sku)){
					tries-=1;
					Toast.makeText(pm.mContext, getString(R.string.dialog_you_have) + 
							" " + tries + " " + getString(R.string.dialog_more_tries), 
							Toast.LENGTH_LONG).show();
					ap.setPitchCorrect(true);
				} else {
					Toast.makeText(pm.mContext, getString(R.string.dialog_purchase_error) + 
							pm.pitchCorrect, Toast.LENGTH_LONG).show();
					sp.checkPref.setChecked(false);
				}
				
			} else {
				Toast.makeText(pm.mContext, getString(R.string.dialog_purchase_tries_exhausted1) + 
						sku + getString(R.string.dialog_purchase_tries_exhausted2), 
						Toast.LENGTH_LONG).show();
				sp.checkPref.setChecked(false);
			}
		}
	};
   	
   	DialogInterface.OnClickListener DefaultListener = new DialogInterface.OnClickListener() {
    	
    	public void onClick(DialogInterface dialog, int id) {
    		// Close Dialog
    	}
    };

	public static PurchaseDialog BuildUpSaleDialog(PurchaseManager pm, int messageId, 
			Integer... buttons) {
	    
		assert buttons.length >= 1;
	    //ool param3 = params.length > 0 ? params[0].booleanValue() : false;
		Bundle bundle = new Bundle();
		bundle.putInt("messageId", messageId);
		if (buttons.length >= 1){
	    	int button1 = buttons[0].intValue();
	    	bundle.putInt("button1", button1);
	    } if (buttons.length >= 2){
	    	int button2 = buttons[1].intValue();
	    	bundle.putInt("button2", button2);
	    }
		
	    PurchaseDialog dialog = new PurchaseDialog().setPurchaseManager(pm);
		dialog.setArguments(bundle);
		return dialog;
	}

	
}
