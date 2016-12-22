package org.osmdroid.debug;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.R;
import org.osmdroid.StarterMapActivity;
import org.osmdroid.debug.browser.CacheBrowserActivity;
import org.osmdroid.debug.model.SqlTileWriterExt;
import org.osmdroid.tileprovider.modules.SqlTileWriter;
import org.osmdroid.tileprovider.util.StorageUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * created on 12/21/2016.
 *
 * @author Alex O'Ree
 */

public class CacheAnalyzerActivity extends Activity implements AdapterView.OnItemClickListener, Runnable {
    SqlTileWriterExt cache = null;
    TextView cacheStats;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cache_analyzer);
        cacheStats = (TextView) findViewById(R.id.cacheStats);

        final ArrayList<String> list = new ArrayList<>();
        list.add("Browse the cache");
        list.add("Purge the cache");
        list.add("Purge a specific tile source");

        ListView lv = (ListView) findViewById(R.id.statslist);
        ArrayAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);

        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);


    }

    public void onResume(){
        super.onResume();
        cache = new SqlTileWriterExt();
        new Thread(this).start();
    }
    public void onPause(){
        super.onPause();
        cache.onDetach();
        cache = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (position) {
            case 0:
                this.startActivity(new Intent(this, CacheBrowserActivity.class));
                break;
            case 1:
                purgeCache();
                break;
            case 2:
                purgeTileSource();
                break;
        }
    }

    private void purgeTileSource() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tile Source");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.select_dialog_singlechoice);
        List<SqlTileWriterExt.SourceCount> sources = cache.getSources();
        for (int i = 0; i < sources.size(); i++) {
            arrayAdapter.add(sources.get(i).source);
        }


        builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String item = arrayAdapter.getItem(which);
                boolean b = cache.purgeCache(item);
                if (b)
                    Toast.makeText(CacheAnalyzerActivity.this, "SQL Cache purged", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(CacheAnalyzerActivity.this, "SQL Cache purge failed, see logcat for details", Toast.LENGTH_LONG).show();

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void purgeCache() {
        SqlTileWriter sqlTileWriter = new SqlTileWriter();
        boolean b = sqlTileWriter.purgeCache();
        sqlTileWriter.onDetach();
        sqlTileWriter = null;
        if (b)
            Toast.makeText(this, "SQL Cache purged", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(this, "SQL Cache purge failed, see logcat for details", Toast.LENGTH_LONG).show();
    }

    @Override
    public void run() {
        List<SqlTileWriterExt.SourceCount> sources = cache.getSources();
        final StringBuilder sb = new StringBuilder("Source: tile count\n");
        if (sources.isEmpty())
            sb.append("None");
        for (int i=0; i < sources.size(); i++) {
            sb.append(sources.get(i).source + ": " + sources.get(i).rowCount + "\n");
        }
        long expired = cache.getRowCountExpired();
        sb.append("Expired tiles: " + expired);

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    TextView tv = (TextView) findViewById(R.id.cacheStats);

                    if (tv != null) {
                        tv.setText(sb.toString());
                    }
                }catch (Exception ex){

                }
            }
        });


    }
}