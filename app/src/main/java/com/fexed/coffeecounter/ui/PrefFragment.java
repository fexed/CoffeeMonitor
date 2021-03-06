package com.fexed.coffeecounter.ui;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.fexed.coffeecounter.BuildConfig;
import com.fexed.coffeecounter.R;
import com.fexed.coffeecounter.db.AppDatabase;
import com.fexed.coffeecounter.db.DBAccess;
import com.fexed.coffeecounter.sys.FileProvider;
import com.fexed.coffeecounter.sys.notif.AlarmBroadcastReceiver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Preferences {@code Fragment}
 * Created by Federico Matteoni on 22/06/2020
 */
public class PrefFragment extends Fragment implements View.OnClickListener {
    public static PrefFragment newInstance() {
        return new PrefFragment();
    }

    @Override
    public void onCreate(Bundle savedInstancestate) {
        super.onCreate(savedInstancestate);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstancestate) {
        View root = inflater.inflate(R.layout.activity_pref, container, false);
        Button resettutorialbtn = root.findViewById(R.id.resettutorialbtn);
        Button backupbtn = root.findViewById(R.id.backupbtn);
        Button exportdatabtn = root.findViewById(R.id.exportdatabtn);
        Button restorebtn = root.findViewById(R.id.restorebtn);
        Button statbtn = root.findViewById(R.id.statbtn);
        Button resetdbbtn = root.findViewById(R.id.resetdbbtn);

        resettutorialbtn.setOnClickListener(this);
        backupbtn.setOnClickListener(this);
        exportdatabtn.setOnClickListener(this);
        restorebtn.setOnClickListener(this);
        statbtn.setOnClickListener(this);
        resetdbbtn.setOnClickListener(this);

        final TextView notiftimetxtv = root.findViewById(R.id.notiftimetxt);
        if (MainActivity.state.getBoolean("notifonoff", true))
            notiftimetxtv.setText(String.format(Locale.getDefault(), "%02d:%02d", MainActivity.state.getInt("notifhour", 20), MainActivity.state.getInt("notifmin", 30)));
        else notiftimetxtv.setText("--:--");
        notiftimetxtv.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (MainActivity.state.getBoolean("notifonoff", true)) {
                    // Get Current Time
                    final Calendar c = Calendar.getInstance();
                    int mHour = c.get(Calendar.HOUR_OF_DAY);
                    int mMinute = c.get(Calendar.MINUTE);

                    // Launch Time Picker Dialog
                    TimePickerDialog timePickerDialog = new TimePickerDialog(v.getContext(), new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            MainActivity.state.edit().putInt("notifhour", hourOfDay).apply();
                            MainActivity.state.edit().putInt("notifmin", minute).apply();
                            notiftimetxtv.setText(String.format(Locale.getDefault(), "%02d:%02d", MainActivity.state.getInt("notifhour", 20), MainActivity.state.getInt("notifmin", 30)));
                            startAlarmBroadcastReceiver();
                        }
                    }, mHour, mMinute, true);
                    timePickerDialog.show();
                    return true;
                } else {
                    Toast.makeText(getContext(), getString(R.string.notifica_giornaliera) + " OFF", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        });

        TextView vertxtv = root.findViewById(R.id.vertxt);
        vertxtv.setText("v" + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");

        final Switch dailynotifswitch = root.findViewById(R.id.dailynotifswitch);
        dailynotifswitch.setChecked(MainActivity.state.getBoolean("notifonoff", true));
        dailynotifswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MainActivity.state.edit().putBoolean("notifonoff", isChecked).apply();

                if (isChecked) {
                    startAlarmBroadcastReceiver();
                    notiftimetxtv.setText(String.format(Locale.getDefault(), "%d:%d", MainActivity.state.getInt("notifhour", 20), MainActivity.state.getInt("notifmin", 30)));
                } else {
                    stopAlarmBroadcastReceiver();
                    notiftimetxtv.setText("--:--");
                }
            }
        });

        final Switch historyswitch = root.findViewById(R.id.historybarswitch);
        historyswitch.setChecked(MainActivity.state.getBoolean("historyline", false));
        historyswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                MainActivity.state.edit().putBoolean("historyline", b).apply();
            }
        });

        return root;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.backupbtn:
                Intent sharefile = new Intent(Intent.ACTION_SEND);
                try {
                    assert getActivity() != null; //Fragment is always launched by MainActivity
                    File file = saveDbToExternalStorage();
                    MainActivity.db = new DBAccess(getActivity().getApplication());
                    if (file != null && file.exists()) { //If not null should always exists
                        String type = "application/octet-stream"; //generic file

                        sharefile.setType(type);
                        sharefile.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getContext(), "com.fexed.coffeecounter.fileprovider", file));
                        startActivity(Intent.createChooser(sharefile, "Share File"));
                    } else {
                        Toast.makeText(getContext(), R.string.dbnotfounderror, Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException ex) {
                    Toast.makeText(getContext(), R.string.dbopenerror, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.restorebtn:
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("file/*");
                startActivityForResult(i, 10);
                break;
        }
    }

    /**
     * Starts the BroadcastReceiver for the notification
     */
    public void startAlarmBroadcastReceiver() {
        assert getActivity() != null; //Fragment is always launched by MainActivity
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getActivity(), AlarmBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);
        alarmManager.cancel(pendingIntent);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, MainActivity.state.getInt("notifhour", 20));
        calendar.set(Calendar.MINUTE, MainActivity.state.getInt("notifmin", 30));
        calendar.set(Calendar.SECOND, 0);
        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
    }

    /**
     * Stops the BroadcastReceiver for the notification
     */
    public void stopAlarmBroadcastReceiver() {
        assert getActivity() != null; //Fragment is always launched by MainActivity
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getActivity(), AlarmBroadcastReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, intent, 0);
        alarmManager.cancel(pendingIntent);
    }

    /**
     * Saves the app database to the internal storage
     * @return the saved database
     * @throws IOException
     */
    public File saveDbToExternalStorage() throws IOException {
        MainActivity.db.checkpoint();
        MainActivity.db.close();
        String currentDBPath = MainActivity.dbpath;
        File src = new File(currentDBPath);
        File savepathfile = new File(getContext().getExternalFilesDir(null) + File.separator + "coffeemonitor");
        if (!savepathfile.exists()) savepathfile.mkdir();
        String dstpath = savepathfile.getPath() + File.separator + "coffeemonitordb_" + new SimpleDateFormat("yyyMMddHHmmss", Locale.getDefault()).format(new Date()) + ".db";
        File savefile = new File(dstpath);
        savefile.createNewFile();
        try (FileChannel inch = new FileInputStream(src).getChannel(); FileChannel outch = new FileOutputStream(dstpath).getChannel()) {
            inch.transferTo(0, inch.size(), outch);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
            return null;
        }

        return savefile;
    }

    /**
     * Checks if the provided database if a {@code Coffee Monitor} database.
     * @param database the database to be checked
     * @return {@code true} if it's a {@code Coffee Monitor} database and has at least
     * a {@code Coffeetype} and a {@code Cup}, {@code false} otherwise
     */
    public boolean checkDatabase(SupportSQLiteDatabase database) {
        String query = "select * from coffeetype"; //Checks the existence of the Coffetype table
        try (Cursor cursor = database.query(query, null)) {
            if (cursor.getCount() <= 0) {
                cursor.close();
                return false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        query = "select * from Cup"; //Checks the existence of the Cups table
        try (Cursor cursor = database.query(query, null)) {
            if (cursor.getCount() <= 0) {
                cursor.close();
                return false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10) { //new database picked
            if (resultCode == Activity.RESULT_OK) {
                final Uri uri = data.getData();
                try {
                    ContentResolver cr = getContext().getContentResolver();
                    String mime = cr.getType(uri);
                    if ("application/octet-stream".equals(mime) && uri.toString().substring(uri.toString().lastIndexOf(".") + 1).equals("db") ) {

                        InputStream in = getContext().getContentResolver().openInputStream(uri);
                        File file = new File(getContext().getCacheDir(), "backupdb");
                        if (file.exists()) file.delete();
                        try {
                            OutputStream output = new FileOutputStream(file);
                            byte[] buffer = new byte[1024];
                            int read;

                            while ((read = in.read(buffer)) != -1) {
                                output.write(buffer, 0, read);
                            }

                            output.flush();
                            in.close();
                            AppDatabase testdb = Room.databaseBuilder(getContext(), AppDatabase.class, "testdb" + System.currentTimeMillis())
                                    .allowMainThreadQueries()
                                    .createFromFile(file)
                                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                                    .build();
                            if (checkDatabase(testdb.getOpenHelper().getReadableDatabase())) {
                                testdb.close();
                                MainActivity.db.close();
                                String dstpath = MainActivity.dbpath + File.separator + "typedb.db";

                                Log.d("DBC", dstpath);
                                Log.d("DBC", file.getAbsolutePath());

                                in = new FileInputStream(file);
                                try {
                                    output = new FileOutputStream(MainActivity.dbpath);
                                    try {
                                        buffer = new byte[1024];
                                        while ((read = in.read(buffer)) != -1) {
                                            output.write(buffer, 0, read);
                                        }
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    } finally {
                                        output.close();
                                    }
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                } finally {
                                    in.close();
                                }
                                File dest = new File(MainActivity.dbpath, "backupdb");
                                if (dest.exists()) dest.renameTo(new File(MainActivity.dbpath, "typedb.db"));

                                MainActivity.db = new DBAccess(getActivity().getApplication());
                                Toast.makeText(getContext(), R.string.dbopened, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(getContext(), R.string.dbopenerror, Toast.LENGTH_LONG).show();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), R.string.dbcreateerror, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(getContext(), R.string.fileinvaliderror, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(), R.string.fileopenerror, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
