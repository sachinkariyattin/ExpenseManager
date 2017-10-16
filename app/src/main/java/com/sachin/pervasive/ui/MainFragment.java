package com.sachin.pervasive.ui;

import android.content.Context;

import com.sachin.pervasive.interfaces.IMainActivityListener;

public class MainFragment extends BaseFragment {

    protected IMainActivityListener mMainActivityListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMainActivityListener = (IMainActivityListener)context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMainActivityListener = null;
    }

}
