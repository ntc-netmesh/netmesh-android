package net.pregi.android.speedtester.speedtest.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * A placeholder fragment containing a simple view.
 */
public abstract class BaseFragment extends Fragment {
    private int layoutId;

    // protected SpeedtestViewModel model;

    public BaseFragment(int layoutId) {
        setArguments(new Bundle());
        this.layoutId = layoutId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: at the moment, a new instance of the model is created each time we leave it
        //      (though not by orientation changes or the like.)
        //      such as by tapping the Back button on the activity, going back to the menu.
        //  If this must persist between activities,
        //      we'll probably need to do something like
        //  https://github.com/googlesamples/android-architecture-components/issues/29
        // model = ViewModelProviders.of(getActivity()).get(SpeedtestViewModel.class);
    }

    public abstract void onCreateView(View root,
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState);

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(layoutId, container, false);
        onCreateView(root, inflater, container, savedInstanceState);
        return root;
    }
}