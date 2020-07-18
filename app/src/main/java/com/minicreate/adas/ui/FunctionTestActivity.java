package com.minicreate.adas.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.minicreate.adas.App;
import com.minicreate.adas.R;
import com.minicreate.adas.transmission.WifiEndpoint;
import com.minicreate.adas.transmission.protocol.OnResponseListener;
import com.minicreate.adas.transmission.protocol.Param;
import com.minicreate.adas.transmission.protocol.ParamQuery_0x75_0x56;
import com.minicreate.adas.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class FunctionTestActivity extends AppCompatActivity {
    private static final String TAG = "FunctionTestActivity";
    FruitAdapter adapter;

    private List<Fruit> fruitList = new ArrayList<Fruit>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.function_test);

        adapter = new FruitAdapter(FunctionTestActivity.this, R.layout.fruit_item_layout, fruitList);
        ListView listView = findViewById(R.id.list_view);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Fruit fruit = fruitList.get(position);
                Toast.makeText(FunctionTestActivity.this, fruit.getName(), Toast.LENGTH_LONG).show();
            }
        });
        queryParam();
    }

    public void queryParam(){
        ParamQuery_0x75_0x56 param = new ParamQuery_0x75_0x56(this);
        WifiEndpoint wifiEndpoint = ((App)getApplication()).getWifiEndpoint();
        if (wifiEndpoint != null) {
            wifiEndpoint.send(new OnResponseListener() {
                @Override
                public void onResponse(Param result) {
                    if (result.isTimeOut()) {
                        LogUtil.d(TAG, "参数查询超时");
                    } else {
                        LogUtil.d(TAG, result.toString());
                        String msg = ((ParamQuery_0x75_0x56) result).getValue1();

                        // 累计15行后，清屏
                        if(fruitList.size() >= 15){
                            fruitList.clear();
                        }
                        Fruit apple = new Fruit(msg, R.mipmap.ic_launcher);
                        fruitList.add(apple);
                        LogUtil.e(TAG, "WIFI Socket连接断开");
                        adapter.notifyDataSetChanged();
                    }
                    queryParam();
                }
            }, param);
        } else {
            LogUtil.e(TAG, "WIFI Socket连接断开");
            queryParam();
        }
    }
}
