package com.harmonicprocesses.penelopefree.settings;

import com.harmonicprocesses.penelopefree.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

public class UpSaleDialog extends DialogFragment {
    private int mId;
    int numButtons = 2;
    int button1id = R.string.dialog_button_more_info,
    		button2id = R.string.dialog_button_next_time;
    DialogInterface.OnClickListener listener1, listener2;
	
	public UpSaleDialog(int messageId){
		mId = messageId;
		listener1 = MoreInfoListener;
		listener2 = DefaultListener;
	}
	
	public UpSaleDialog(int messageId, int button1){
		mId = messageId;
		numButtons = 1;
		button1id = button1;
		listener1 = DefaultListener;
	}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
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
       	
       	DialogInterface.OnClickListener DefaultListener = new DialogInterface.OnClickListener() {
        	
        	public void onClick(DialogInterface dialog, int id) {
        		// Close Dialog
        	}
        };
}