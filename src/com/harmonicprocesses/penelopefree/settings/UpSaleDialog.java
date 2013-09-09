package com.harmonicprocesses.penelopefree.settings;

import com.harmonicprocesses.penelopefree.R;

import android.annotation.SuppressLint;
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

@SuppressLint("ValidFragment")
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class UpSaleDialog extends DialogFragment {
    private int mId;
    int numButtons = 2;
    int button1id = R.string.dialog_button_more_info,
    button2id = R.string.dialog_button_next_time;
    DialogInterface.OnClickListener listener1, listener2;
	   
    public UpSaleDialog(){
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
		
		private String url = "http://mypromotank.com/hp/penelope/?p=59";

		public void onClick(DialogInterface dialog, int id) {
			final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url ));
			getActivity().startActivity(intent);
        	   
       	}
   	};
   	
   	DialogInterface.OnClickListener RatePennyListener = new DialogInterface.OnClickListener() {
		
   		private String url = "market://details?id=com.harmonicprocesses.penelopefree";
   		
		@Override
		public void onClick(DialogInterface dialog, int which) {
			final Intent intent = new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url ));
			getActivity().startActivity(intent);
		}
	};
   	
   	DialogInterface.OnClickListener DefaultListener = new DialogInterface.OnClickListener() {
    	
    	public void onClick(DialogInterface dialog, int id) {
    		// Close Dialog
    	}
    };

	public static UpSaleDialog BuildUpSaleDialog(int messageId, 
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
	    }
		
	    UpSaleDialog dialog = new UpSaleDialog();
		dialog.setArguments(bundle);
		return dialog;
	}
	

}