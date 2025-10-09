package com.ead.sparkpoint.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.ead.sparkpoint.R;

public class LoadingDialog {

	private final Activity activity;
	private AlertDialog dialog;

	/**
	 * Constructs a new LoadingDialog.
	 */
	public LoadingDialog(Activity activity) {
		this.activity = activity;
	}

	/**
	 * Displays the loading dialog with a specified message. If the dialog is already
	 */
	public void show(String message) {
		// Avoid showing the dialog if the activity is finishing or destroyed.
		if (activity == null || activity.isFinishing()) return;
		// If dialog is already visible, just update the text.
		if (dialog != null && dialog.isShowing()) {
			updateMessage(message);
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);
		LayoutInflater inflater = activity.getLayoutInflater();
		View view = inflater.inflate(R.layout.dialog_loading, null);
		builder.setView(view);
		builder.setCancelable(false);
		dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);
		updateMessageOnView(view, message);

		// Set a transparent background for the dialog window for rounded corners to be visible.
		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			dialog.getWindow().setDimAmount(0.4f);
		}
		dialog.show();
	}

	/**
	 * Hides/dismisses the loading dialog if it is currently showing.
	 */
	public void hide() {
		if (dialog != null && dialog.isShowing()) {
			dialog.dismiss();
		}
	}

	/**
	 * Checks if the loading dialog is currently visible.
	 * @return True if the dialog is showing, false otherwise.
	 */
	public boolean isShowing() {
		return dialog != null && dialog.isShowing();
	}

	/**
	 * Updates the message text on an already visible dialog.
	 * @param message The new message to display.
	 */
	private void updateMessage(String message) {
		if (dialog != null && dialog.isShowing()) {
			TextView tv = dialog.findViewById(R.id.tvLoadingMessage);
			if (tv != null) tv.setText(message != null ? message : "Loading...");
		}
	}

	/**
	 * Sets the message text on the dialog's view before it is shown.
	 * @param view The dialog's content view.
	 * @param message The message to display.
	 */

	private void updateMessageOnView(View view, String message) {
		TextView tv = view.findViewById(R.id.tvLoadingMessage);
		if (tv != null) tv.setText(message != null ? message : "Loading...");
	}
}


