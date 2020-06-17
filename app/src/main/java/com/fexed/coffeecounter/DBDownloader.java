package com.fexed.coffeecounter;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class DBDownloader extends AsyncTask<String, Void, String> {
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        //TODO dialog?
    }

    @Override
    protected String doInBackground(String... urls) {
        int count;
        try {
            URL url = new URL(urls[0]);
            URLConnection connection = url.openConnection();
            connection.connect();
            int fileLen = connection.getContentLength();
            long lastmod = connection.getLastModified(); //TODO check last modified

            InputStream istream = new BufferedInputStream(url.openStream(), 8192);
            StringBuffer strbuff = new StringBuffer();
            byte[] data = new byte[1024];

            while ((count = istream.read(data)) != -1) {
                strbuff.append(new String(data, 0, count));
            }

            istream.close();

            return new String(strbuff);
        } catch (MalformedURLException ex) {
            Log.e("URL", urls[0]);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
