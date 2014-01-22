package com.harmonicprocesses.penelopefree.settings;

import com.harmonicprocesses.penelopefree.PenelopeMainActivity;
import com.harmonicprocesses.penelopefree.R;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

@SuppressLint("ValidFragment")
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class UpSaleDialog extends DialogFragment {
    private int mId;
    int numButtons = 2;
    int button1id = R.string.dialog_button_more_info,
    button2id = R.string.dialog_button_next_time;
    DialogInterface.OnClickListener listener1, listener2;
	private Context mContext;
	   
    public UpSaleDialog(){
    	mContext = getActivity();
		listener1 = MoreInfoListener;
		listener2 = DefaultListener;
    }
    
    
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
		Bundle bundle=getArguments();
		
		if (bundle.size()==1){
			mId = bundle.getInt("messageId");
			listener1 = MoreInfoListener;
			listener2 = DefaultListener;
		} else if (bundle.size()==2){
			mId = bundle.getInt("messageId");
			numButtons = 1;
			button1id = bundle.getInt("button1");
			listener1 = DefaultListener;
		} else if (bundle.size()==3){
			mId = bundle.getInt("messageId");
			numButtons = 2;
			button1id = bundle.getInt("button1");
			button2id = bundle.getInt("button2");
			listener1 = RatePennyListener;
		} else if (bundle.size()==4){
			mId = bundle.getInt("messageId");
			numButtons = 2;
			button1id = bundle.getInt("button1");
			button2id = bundle.getInt("button2");
			if (bundle.getInt("listenerInt1")==3){
				listener1 = BuyMugListener;
			}
		}
		
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(mId)
            	.setPositiveButton(button1id, listener1);
        
        if (numButtons>1){
	        builder.setNegativeButton(button2id, listener2);
        }
        // Create the AlertDialog object and return it
        return builder.create();
    }
	
	DialogInterface.OnClickListener MoreInfoListener = new DialogInterface.OnClickListener() {
		
		private String url = "http://penny.hpp.io/?p=63";

		public void onClick(DialogInterface dialog, int id) {
			if (checkConnectivity()){
				final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url));
				getActivity().startActivity(intent);
			} 
       	}
   	};
   	
	DialogInterface.OnClickListener BuyMugListener = new DialogInterface.OnClickListener() {
		
		private String url = "market://details?id=com.harmonicprocesses.penelopefree";

		public void onClick(DialogInterface dialog, int id) {
			if (checkConnectivity()){
				final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url));
				getActivity().startActivity(intent);
			} 
       	}
   	};
   	
   	DialogInterface.OnClickListener RatePennyListener = new DialogInterface.OnClickListener() {
		
   		private String url = "market://details?id=com.harmonicprocesses.penelopefree";
   		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if (checkConnectivity()){
				final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url));
				getActivity().startActivity(intent);
			}
		}
	};
   	
   	DialogInterface.OnClickListener DefaultListener = new DialogInterface.OnClickListener() {
    	
    	public void onClick(DialogInterface dialog, int id) {
    		// Close Dialog
    	}
    };

	public static UpSaleDialog BuildUpSaleDialog(Context context, int messageId, 
			Integer... buttons) {
	    
		assert buttons.length <= 1;
	    //ool param3 = params.length > 0 ? params[0].booleanValue() : false;
		Bundle bundle = new Bundle();
		bundle.putInt("messageId", messageId);
		if (buttons.length >= 1){
	    	int button1 = buttons[0].intValue();
	    	bundle.putInt("button1", button1);
	    } if (buttons.length >= 2){
	    	int button2 = buttons[1].intValue();
	    	bundle.putInt("button2", button2);
	    } if (buttons.length >= 3){
	    	int listenerInt1 = buttons[2].intValue();
	    	bundle.putInt("listenerInt1", listenerInt1);
	    }
		
	    UpSaleDialog dialog = new UpSaleDialog();
		dialog.setArguments(bundle);
		return dialog;
	}
	
	public boolean checkConnectivity(){
		ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni != null && ni.isAvailable() && ni.isConnected()){
			return true;
		} else {
			Toast.makeText(mContext, R.string.unable_to_connect, Toast.LENGTH_LONG).show();
			return false;
		}
	}

}