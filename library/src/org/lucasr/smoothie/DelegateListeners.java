package org.lucasr.smoothie;

import android.view.View.OnTouchListener;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemSelectedListener;

class DelegateListeners {
    private OnScrollListener mOnScrollListener;
    private OnTouchListener mOnTouchListener;
    private OnItemSelectedListener mOnItemSelectedListener;

    DelegateListeners() {
        mOnScrollListener = null;
        mOnTouchListener = null;
        mOnItemSelectedListener = null;
    }

    OnScrollListener getOnScrollListener() {
        return mOnScrollListener;
    }

    void setOnScrollListener(OnScrollListener l) {
        mOnScrollListener = l;
    }

    OnTouchListener getOnTouchListener() {
        return mOnTouchListener;
    }

    void setOnTouchListener(OnTouchListener l) {
        mOnTouchListener = l;
    }

    OnItemSelectedListener getOnItemSelectedListener() {
        return mOnItemSelectedListener;
    }

    void setOnItemSelectedListener(OnItemSelectedListener l) {
        mOnItemSelectedListener = l;
    }
}
