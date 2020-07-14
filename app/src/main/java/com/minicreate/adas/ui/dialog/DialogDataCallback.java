package com.minicreate.adas.ui.dialog;

import android.view.View;

public interface DialogDataCallback<T> {

	public void onClick(View v, boolean bl, T data);
	
}
