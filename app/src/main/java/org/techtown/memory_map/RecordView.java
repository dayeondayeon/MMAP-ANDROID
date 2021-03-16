package org.techtown.memory_map;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;


public class RecordView extends Fragment {

    RecyclerView recyclerView;
    RecordAdapter adapter;

    Context context;

    private ServiceApi serviceApi;
    private RecordResponse dataList;
    private List<Record> data;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_recordview, container, false);

        initUI(rootView);

        return rootView;
    }


    private void initUI(ViewGroup rootView) {
        recyclerView = rootView.findViewById(R.id.list_Recycler);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        data = new ArrayList<>();

        serviceApi = RetrofitClient.getClient().create(ServiceApi.class);

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("login", MODE_PRIVATE);
        int userIdx = sharedPreferences.getInt("userIdx", 0);

        Call<RecordResponse> call = serviceApi.getData(userIdx);

        call.enqueue(new Callback<RecordResponse>() {
            @Override
            public void onResponse(Call<RecordResponse> call, Response<RecordResponse> response) {
                dataList = response.body();
                if (dataList.getStatus() == 200) {
                    data = dataList.getBody();
                    adapter = new RecordAdapter(getContext(), data);

                    recyclerView.setAdapter(adapter);
                    adapter.setOnItemClickListener(new OnRecordItemClickListener() {
                        @Override
                        public void onItemClick(RecordAdapter.ViewHolder holder, View view, int position) {
                            Record item = adapter.getItem(position);
                            Toast.makeText(getContext(), "아이템 선택됨: " + item.getText(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
                else if (dataList.getStatus() == 400) {
                    Toast.makeText(getContext(), dataList.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<RecordResponse> call, Throwable t) {
                Log.e("기록 불러오기 실패", t.getMessage());
                t.printStackTrace();
            }
        });

    }

}