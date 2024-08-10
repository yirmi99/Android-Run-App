package com.example.justrun;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class HistoryActivity extends BaseActivity {
    DatabaseManager dbManager;
    ListView historyList;
    private DatabaseHelper dbHelper;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> historyData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Context context = LocaleHelper.onAttach(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Button deleteBtn = findViewById(R.id.deleteBtn);
        historyList = findViewById(R.id.historyList);
        dbHelper = new DatabaseHelper(this);
        dbManager = new DatabaseManager(this);

        try {
            dbManager.open();
        } catch (Exception e) {
            e.printStackTrace();
        }

        historyData = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, R.layout.activity_history_list_view, R.id.historyListView, historyData);
        historyList.setAdapter(adapter);
        loadHistoryData();

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dbHelper.deleteAndRecreateTable();
                historyData.clear();
                adapter.notifyDataSetChanged();
                dbManager.close();
            }
        });

    }

    private void loadHistoryData() {
        historyData.clear();
        Cursor cursor = dbManager.fetch();
        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String ID = cursor.getString(cursor.getColumnIndex(DatabaseHelper.USER_ID));
                @SuppressLint("Range") String date = cursor.getString(cursor.getColumnIndex(DatabaseHelper.DATE));
                @SuppressLint("Range") String duration = cursor.getString(cursor.getColumnIndex(DatabaseHelper.DURATION));
                @SuppressLint("Range") String kilometers = cursor.getString(cursor.getColumnIndex(DatabaseHelper.KILOMETERS));
                historyData.add(getString(R.string.runNumber) + ": " + ID + "\n" + getString(R.string.Date) + ": " + date + "\n" + getString(R.string.Duration) + ": " + duration + "\n" + getString(R.string.Kilometers) + ": " + kilometers);
            } while (cursor.moveToNext());
        }
        cursor.close();
        adapter.notifyDataSetChanged();
    }
}
