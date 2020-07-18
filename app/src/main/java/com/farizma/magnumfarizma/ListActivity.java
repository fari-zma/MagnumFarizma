package com.farizma.magnumfarizma;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ListActivity extends AppCompatActivity {

    private static final String urlStart = "https://api.github.com/search/users?";

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private ProgressBar progressBar;
    private ImageView imageView;

    private ArrayList<Item> itemList = new ArrayList<>();

    private String url;
    private int no_of_page;
    private int count = 0;
    private boolean isLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        statusBarConfig();

        if (!mIConnected()) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(R.string.error);
            alertDialogBuilder.setMessage(R.string.error_message);

            alertDialogBuilder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    progressBar.setVisibility(View.INVISIBLE);
                    finish();
                }
            });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();
        }

        progressBar = findViewById(R.id.progressBar);
        recyclerViewConfig();

        url = getUrl();
        getData();
    }

    private void getData() {
        StringRequest request = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            int total_count = jsonObject.getInt("total_count");
                            Log.d("COUNT", "total_count = " + total_count);

                            // no result found
                            if(total_count == 0) {
                                progressBar.setVisibility(View.INVISIBLE);
                                imageView = findViewById(R.id.imageView);
                                imageView.setImageDrawable(getDrawable(R.drawable.empty));
                                return;
                            }

                            no_of_page = (int) Math.ceil(total_count/30) + 1;
                            Log.d("COUNT", "no_of_page = " + no_of_page);

                            fetchData(++count);

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d("Volley", "JSONException: " + e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.d("Volley", "ERROR: " + error);
            }
        });

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }

    private void fetchData(int page) {

        String mUrl = url + "&page=" + page;

        StringRequest request = new StringRequest(Request.Method.GET, mUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if(progressBar.getVisibility() == View.VISIBLE)
                            progressBar.setVisibility(View.INVISIBLE);
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            JSONArray jsonArray = jsonObject.getJSONArray("items");

                            for(int i=0; i<jsonArray.length(); i++) {
                                JSONObject object = jsonArray.getJSONObject(i);
                                String username = object.getString("login");
                                String avatarUrl = object.getString("avatar_url");
                                insertData(username, avatarUrl);
                            }
                            adapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.d("Volley", "JSONException: " + e);
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                Log.d("Volley", "ERROR: " + error);
            }
        });

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }

    private void insertData(String username, String avatarUrl) {
        itemList.add(new Item(username, avatarUrl));
    }

    private String getUrl() {
        Intent intent = getIntent();
        String query = intent.getStringExtra("QUERY");
        return (urlStart + "q="+query);
    }

    public void setLoaded() {
        isLoading = false;
    }

    private void recyclerViewConfig() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(30);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter(this, itemList);

        final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int totalItemCount = linearLayoutManager.getItemCount();
                int lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                if (!isLoading && totalItemCount == (lastVisibleItem + 2)) {
                    if (((Adapter) adapter).onLoadMoreListener != null) {
                        ((Adapter) adapter).onLoadMoreListener.onLoadMore();
                    }
                    isLoading = true;
                }
            }
        });


        ((Adapter) adapter).setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                itemList.add(itemList.size(),null);
                adapter.notifyItemInserted(itemList.size() - 1);
                //Load more data for recyclerView
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //Remove loading item
                        itemList.remove(itemList.size() - 1);
                        adapter.notifyItemRemoved(itemList.size());
                        //Load data
                        if (count < no_of_page) fetchData(++count);
                        else
                            Toast.makeText(getApplicationContext(), R.string.no_more_user, Toast.LENGTH_SHORT).show();
                        setLoaded();
                    }
                }, 5000);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private boolean mIConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void statusBarConfig() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View view = findViewById(R.id.rootView);
            int flags = view.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            view.setSystemUiVisibility(flags);
            this.getWindow().setStatusBarColor(Color.WHITE);
        }
    }
}