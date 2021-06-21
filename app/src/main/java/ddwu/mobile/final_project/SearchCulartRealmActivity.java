package ddwu.mobile.final_project;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

public class SearchCulartRealmActivity extends AppCompatActivity {

    RadioGroup rGroup;

    ListView lvCulart;
    String apiAddr;

    ArrayList<CulartDTO> resultList;
    CulartAdapter adapter;
    CulartXmlParser parser;

    String realm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_culart_realm);

        rGroup = findViewById(R.id.rg);
        lvCulart = findViewById(R.id.lvCulart);

        resultList = new ArrayList();
        adapter = new CulartAdapter(this, R.layout.listview_culart, resultList);
        lvCulart.setAdapter(adapter);

        apiAddr = getResources().getString(R.string.api_culart_realm);
        parser = new CulartXmlParser();

        // 공연장 항목을 클릭 시 일련번호(seq)를 이용해 공연장의 상세 정보를 보여주는 액티비티 생성
        lvCulart.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CulartDTO dto = resultList.get(position);
                String seq = dto.getSeq();
                Intent intent = new Intent(SearchCulartRealmActivity.this, CulartDetailActivity.class);
                intent.putExtra("seq", seq);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void onClick(View v) {
        switch (rGroup.getCheckedRadioButtonId()) {
            case R.id.rbtn1:
                realm = "A000";
                break;
            case R.id.rbtn2:
                realm = "B000";
                break;
            case R.id.rbtn3:
                realm = "D000";
                break;
            case R.id.rbtn4:
                realm = "C000";
                break;
            case R.id.rbtn5:
                realm = "L000";
                break;
        }
        try {
            new SearchCulartAreaAsyncTask().execute(apiAddr + URLEncoder.encode(realm, "UTF-8")
                    + "&ServiceKey=" + getResources().getString(R.string.servicekey_culart));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    class SearchCulartAreaAsyncTask extends AsyncTask<String, Integer, String> {
        ProgressDialog progressDlg;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDlg = ProgressDialog.show(SearchCulartRealmActivity.this, "Wait", "Downloading...");
        }

        @Override
        protected String doInBackground(String... strings) {
            String address = strings[0];
            String result = downloadContents(address);
            if (result == null) return "Error!";
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            resultList = parser.parse(result);

            adapter.setList(resultList);
            adapter.notifyDataSetChanged();

            progressDlg.dismiss();
        }

        private boolean isOnline() {
            ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
            return (networkInfo != null && networkInfo.isConnected());
        }

        private InputStream getNetworkConnection(HttpURLConnection conn) throws Exception {
            conn.setReadTimeout(3000);
            conn.setConnectTimeout(3000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            if (conn.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code: " + conn.getResponseCode());
            }

            return conn.getInputStream();
        }

        protected String readStreamToString(InputStream stream){
            StringBuilder result = new StringBuilder();

            try {
                InputStreamReader inputStreamReader = new InputStreamReader(stream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                String readLine = bufferedReader.readLine();

                while (readLine != null) {
                    result.append(readLine + "\n");
                    readLine = bufferedReader.readLine();
                }

                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return result.toString();
        }

        protected String downloadContents(String address) {
            HttpURLConnection conn = null;
            InputStream stream = null;
            String result = null;

            try {
                URL url = new URL(address);
                conn = (HttpURLConnection)url.openConnection();
                stream = getNetworkConnection(conn);
                result = readStreamToString(stream);
                if (stream != null) stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) conn.disconnect();
            }

            return result;
        }
    }

}
