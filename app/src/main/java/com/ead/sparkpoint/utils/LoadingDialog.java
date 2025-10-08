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

	public LoadingDialog(Activity activity) {
		this.activity = activity;
	}

	public void show(String message) {
		if (activity == null || activity.isFinishing()) return;
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
		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			dialog.getWindow().setDimAmount(0.4f);
		}
		dialog.show();
	}

	public void hide() {
		if (dialog != null && dialog.isShowing()) {
			dialog.dismiss();
		}
	}

	public boolean isShowing() {
		return dialog != null && dialog.isShowing();
	}

	private void updateMessage(String message) {
		if (dialog != null && dialog.isShowing()) {
			TextView tv = dialog.findViewById(R.id.tvLoadingMessage);
			if (tv != null) tv.setText(message != null ? message : "Loading...");
		}
	}

	private void updateMessageOnView(View view, String message) {
		TextView tv = view.findViewById(R.id.tvLoadingMessage);
		if (tv != null) tv.setText(message != null ? message : "Loading...");
	}
}


