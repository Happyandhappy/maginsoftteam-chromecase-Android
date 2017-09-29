package com.oxycast.chromecastapp.cloudserver;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.oxycast.chromecastapp.R;


/**
 * A simple {@link Fragment} subclass.
 * to handle interaction events.
 * Use the {@link Home#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Home extends Fragment {

    public Home() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment Home.
     */
    public static Home newInstance() {
        return new Home();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_home, container, false);

        return v;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}
