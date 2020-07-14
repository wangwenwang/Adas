package com.minicreate.adas.ui.dialog;

import android.view.View;

public interface OnClickDialogListener {

	/**
	 * 点击回调
	 * 
	 * @param v
	 *            当前点击的View
	 * @param bl
	 *            false:点击取消按钮,true:点击确定按钮
	 */
	public void onClick(View v, boolean bl);

}
