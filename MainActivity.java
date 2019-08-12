package com.example.sravya.tvdramas;


import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;

import android.content.Context;
import android.content.Intent;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.sravya.tvdramas.adapter.TvAdapter;
import com.example.sravya.tvdramas.database.FavoriteTv;
import com.example.sravya.tvdramas.model.TvClass;
import com.example.sravya.tvdramas.uictivity.TvDetailsActivity;
import com.example.sravya.tvdramas.utils.JsonUtils;
import com.example.sravya.tvdramas.utils.NetworkUtils;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity implements TvAdapter.ListItemClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String SORT_POPULAR = "popular";
    private static final String SORT_TOP_RATED = "top_rated";
    private static final String SORT_FAVORITE = "favorite";
    private static String currentSort = SORT_POPULAR;


    private FirebaseAuth mAuth;
    private ArrayList<TvClass> tvList;

    private RecyclerView mtvRecyclerView;
    private TvAdapter tvAdapter;

    //Favorite movies
    private ArrayList<FavoriteTv> favtv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        mtvRecyclerView = (RecyclerView) findViewById(R.id.recycler);
        GridLayoutManager layoutManager = new GridLayoutManager(this,
                2);
        mtvRecyclerView.setLayoutManager(layoutManager);
        mtvRecyclerView.setHasFixedSize(true);

        tvAdapter = new TvAdapter(tvList,
                (TvAdapter.ListItemClickListener) this, this);
        mtvRecyclerView.setAdapter(tvAdapter);

        // favorites database
        favtv = new ArrayList<FavoriteTv>();

        setTitle(getString(R.string.app_name));

        setupViewModel();


    }

    private void loadTv() {
        makeTvSearchQuery();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (!isconnected()) {
            Toast.makeText(this, R.string.no_network, Toast.LENGTH_SHORT).show();
        } else {
            if (id == R.id.popular && !currentSort.equals(SORT_POPULAR)) {
                ClearTvItemList();
                currentSort = SORT_POPULAR;
                setTitle(getString(R.string.popular_tv_shows));
                loadTv();
                return true;
            }
            if (id == R.id.toprated && !currentSort.equals(SORT_TOP_RATED)) {
                ClearTvItemList();
                currentSort = SORT_TOP_RATED;
                setTitle(getString(R.string.top_rated_tv_shows));
                loadTv();
                return true;
            }
            if (id == R.id.favorite && !currentSort.equals(SORT_FAVORITE)) {
                ClearTvItemList();
                currentSort = SORT_FAVORITE;
                setTitle(getString(R.string.favorite_tv_shows));
                loadTv();
                return true;

            }
            if (id == R.id.signoutitem) {
                mAuth.signOut();
                Intent intent = new Intent(this, SplashActivity.class);
                startActivity(intent);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void ClearTvItemList() {
        if (tvList != null) {
            tvList.clear();
        } else {
            tvList = new ArrayList<TvClass>();
        }
    }

    private void makeTvSearchQuery() {
        if (currentSort.equals(SORT_FAVORITE)) {
            ClearTvItemList();
            for (int i = 0; i < favtv.size(); i++) {
                TvClass mov = new TvClass(
                        String.valueOf(favtv.get(i).getId()),
                        favtv.get(i).getTitle(),
                        favtv.get(i).getReleaseDate(),
                        favtv.get(i).getVote(),
                        favtv.get(i).getPopularity(),
                        favtv.get(i).getSynopsis(),
                        favtv.get(i).getImage(),
                        favtv.get(i).getBackdrop()
                );
                tvList.add(mov);

            }


            tvAdapter.setMovieData(tvList);
        } else {
            String tvQuery = currentSort;
            URL tvSearchUrl = NetworkUtils.buildUrl(tvQuery, getText(R.string.api_key).toString());
            new TvQueryTask().execute(tvSearchUrl);
        }


    }


    public class TvQueryTask extends AsyncTask<URL, Void, String> {

        @Override
        protected String doInBackground(URL... params) {
            URL searchUrl = params[0];
            String searchResults = null;
            try {
                searchResults = NetworkUtils.getResponseFromHttpUrl(searchUrl);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return searchResults;
        }

        @Override
        protected void onPostExecute(String searchResults) {
            if (searchResults != null && !searchResults.equals("")) {
                tvList = JsonUtils.parseTvJson(searchResults);
                tvAdapter.setMovieData(tvList);
            }
        }
    }

    private void setupViewModel() {
        MainViewModel viewModel = ViewModelProviders.of(this).get(MainViewModel.class);
        viewModel.getTv().observe(this, new Observer<List<FavoriteTv>>() {
            @Override
            public void onChanged(@Nullable List<FavoriteTv> favs) {
                if (favs.size() > 0) {
                    favtv.clear();
                    favtv = (ArrayList<FavoriteTv>) favs;
                }
                for (int i = 0; i < favtv.size(); i++) {
                    Log.d(TAG, favtv.get(i).getTitle());
                }
                loadTv();
            }


        });
    }

    public boolean isconnected() {
        boolean connected = false;
        try {
            ConnectivityManager manager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            connected = networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
            return connected;

        } catch (Exception e) {
        }
        return connected;
    }

    @Override
    protected void onResume() {
        super.onResume();
        makeTvSearchQuery();
    }

    public void onBackPressed() {
        super.onBackPressed();
        Intent myIntent = new Intent(this,
                TvDetailsActivity.class);
        startActivity(myIntent);

    }

    @Override
    public void OnListItemClick(TvClass tvItem) {
        Intent myIntent = new Intent(this,
                TvDetailsActivity.class);
        myIntent.putExtra("tvItem", tvItem);
        startActivity(myIntent);
    }
}
