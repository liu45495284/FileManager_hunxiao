/*
 * This file is part of FileManager.
 * FileManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * TYD Inc. (C) 2012. All rights reserved.
 */
package com.freeme.filemanager.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;

import com.freeme.filemanager.R;
import android.text.Selection;
import android.view.inputmethod.InputMethodManager;
public class TextInputDialog extends AlertDialog {
    private String mInputText;
    private String mTitle;
    private String mMsg;
    private OnFinishListener mListener;
    private Context mContext;
    private View mView;
    private EditText mFolderName;

    //add by tyd zhuya 20151211,for rename
    public  int fileNamelenth;
    private String extFormFilename;
    //end

    public interface OnFinishListener {
        // return true to accept and dismiss, false reject
        boolean onFinish(String text);
    }

    public TextInputDialog(Context context, String title, String msg, String text, OnFinishListener listener) {
        super(context);
        mTitle = title;
        mMsg = msg;
        mListener = listener;
        mInputText = text;
        mContext = context;
    }

    public String getInputText() {
        return mInputText;
    }

    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.textinput_dialog, null);

        setTitle(mTitle);
        setMessage(mMsg);

        mFolderName = (EditText) mView.findViewById(R.id.text);
        mFolderName.setText(mInputText);
        //add by tyd zhuya 20151211,for rename
        if(fileNamelenth!=mInputText.length()){
            extFormFilename=mInputText.substring(fileNamelenth,mInputText.length());
        }



        mFolderName.addTextChangedListener(new TextWatcher() {
            boolean isDisplayWarning = true;
            
            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
                
                //add by Droi mingjun for rename file name length
                    if (arg0.length() >= 85) {
                        Toast.makeText(mContext, R.string.invalid_file_rename, Toast.LENGTH_LONG).show();
                    }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String inputString = s.toString();
                if (inputString.startsWith(".") && start == 0) {

                    Toast.makeText(mContext, R.string.create_hidden_file, Toast.LENGTH_SHORT).show();
                }
                //modify by droi heqianqian on 20151223
                if(mTitle.equals(mContext.getString(R.string.operation_rename))) {
                    ////add by tyd zhuya 20151211,for rename
                    if (fileNamelenth != mInputText.length() && !inputString.endsWith(extFormFilename)) {
                        Toast.makeText(mContext, R.string.donot_change_extfromfilename, Toast.LENGTH_SHORT).show();
                    }
                }
                //end
            }
        });

        setView(mView);

        //add by tyd zhuya 20151211,for rename
        setOnShowListener(new OnShowListener() {
            public void onShow(DialogInterface dialog) {
                InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(mFolderName, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        //*/ freeme.liuhaoran , 20161018 , limit length
        if (fileNamelenth > 85) {
            fileNamelenth = mInputText.substring(0,85).length();
        }
        //*/
        mFolderName.setSelection(0, fileNamelenth);
        //end
        setButton(BUTTON_POSITIVE, mContext.getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == BUTTON_POSITIVE) {
                            mInputText = mFolderName.getText().toString();
                            if (mListener.onFinish(mInputText)) {
                                dismiss();
                            }

                        }
                    }
                });
        setButton(BUTTON_NEGATIVE, mContext.getString(android.R.string.cancel),
                (DialogInterface.OnClickListener) null);

        super.onCreate(savedInstanceState);
    }

}
