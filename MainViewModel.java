package com.example.sravya.tvdramas;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;

import com.example.sravya.tvdramas.database.FavoriteTv;
import com.example.sravya.tvdramas.database.TvDataBase;

import java.util.List;

public class MainViewModel extends AndroidViewModel {
    private static final String TAG =
            MainViewModel.class.getSimpleName();

    private LiveData<List<FavoriteTv>> tvshows;

    public MainViewModel(Application application) {
        super(application);
        TvDataBase database =
                TvDataBase.getInstance(this.getApplication());
        tvshows = database.tvDao().loadAllTv();
    }

    public LiveData<List<FavoriteTv>> getTv() {
        return tvshows;
    }
}