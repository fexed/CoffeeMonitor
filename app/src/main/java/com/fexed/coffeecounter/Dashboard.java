package com.fexed.coffeecounter;

import android.app.DatePickerDialog;
import android.arch.persistence.room.Room;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class Dashboard extends AppCompatActivity {
    public SharedPreferences state;
    public SharedPreferences.Editor editor;
    public AppDatabase db;
    public RecyclerView recview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        state = this.getSharedPreferences(getString(R.string.apppkg), MODE_PRIVATE);
        editor = state.edit();

        Toolbar mTopToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mTopToolbar);
        mTopToolbar.setBackgroundColor(getResources().getColor(R.color.transparent));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        mTopToolbar.setNavigationIcon(R.drawable.ic_hamburger);
        mTopToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO icone nel menù di navigazione
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.getMenuInflater().inflate(R.menu.navigation, popup.getMenu());

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        ViewFlipper vf = findViewById(R.id.viewflipper);

                        switch (item.getItemId()) {
                            case R.id.navigation_statistics:

                                if (vf.getDisplayedChild() != 0) {
                                    graphUpdater();
                                    vf.setDisplayedChild(0);
                                }

                                return true;
                            case R.id.navigation_dashboard:

                                if (vf.getDisplayedChild() != 1) {
                                    String[] funfacts = getResources().getStringArray(R.array.funfacts);
                                    Random rnd = new Random();
                                    int i = rnd.nextInt(funfacts.length);

                                    TextView funfactstxtv = findViewById(R.id.funfacttxt);
                                    funfactstxtv.setText(funfacts[i]);

                                    vf.setDisplayedChild(1);
                                }

                                return true;
                            case R.id.navigation_notifications:

                                if (vf.getDisplayedChild() != 2) {
                                    vf.setDisplayedChild(2);
                                }

                                return true;
                        }
                        return false;
                    }
                });
                popup.show();
            }
        });

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "typedb").allowMainThreadQueries().build();
        insertStandardTypes();

        adInitializer();
        graphInitializer();
        graphUpdater();

        recview = findViewById(R.id.recview);
        recview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recview.setAdapter(new RecviewAdapter(db));
        SnapHelper helper = new LinearSnapHelper() {
            @Override
            public int findTargetSnapPosition(RecyclerView.LayoutManager layoutManager, int velocityX, int velocityY) {
                View centerView = findSnapView(layoutManager);
                if (centerView == null)
                    return RecyclerView.NO_POSITION;

                int position = layoutManager.getPosition(centerView);
                int targetPosition = -1;
                if (layoutManager.canScrollHorizontally()) {
                    if (velocityX < 0) {
                        targetPosition = position - 1;
                    } else {
                        targetPosition = position + 1;
                    }
                }
                if (layoutManager.canScrollVertically()) {
                    if (velocityY < 0) {
                        targetPosition = position - 1;
                    } else {
                        targetPosition = position + 1;
                    }
                }

                final int firstItem = 0;
                final int lastItem = layoutManager.getItemCount() - 1;
                targetPosition = Math.min(lastItem, Math.max(targetPosition, firstItem));
                return targetPosition;
            }
        };
        helper.attachToRecyclerView(recview);

        Button rstdbbtn = findViewById(R.id.resetdbbtn);
        rstdbbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(v.getContext());
                dialogbuilder.setMessage(getString(R.string.resetdb) + "?")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                db.cupDAO().nuke();
                                db.coffetypeDao().nuke();
                                insertStandardTypes();
                                recview.setAdapter(new RecviewAdapter(db));
                                Snackbar.make(findViewById(R.id.container), "Database resettato", Snackbar.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Snackbar.make(findViewById(R.id.container), "Annullato", Snackbar.LENGTH_SHORT).show();
                            }
                        });
                dialogbuilder.create();
                dialogbuilder.show();
            }
        });

        Button showstatbtn = findViewById(R.id.statbtn);
        showstatbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int milliliterstotal = 0;
                int cupstotal = 0;

                for (Coffeetype type : db.coffetypeDao().getAll()) {
                    milliliterstotal += (type.getLiters() * type.getQnt());
                    cupstotal += type.getQnt();
                }

                AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(v.getContext());
                dialogbuilder.setMessage("Bevuti in totale " + milliliterstotal + " ml in " + cupstotal + " tazzine.")
                        .setNeutralButton("OK", null);
                dialogbuilder.create();
                dialogbuilder.show();
            }
        });

        Switch historyswitch = findViewById(R.id.historybarswitch);
        historyswitch.setChecked(state.getBoolean("historyline", false));
        historyswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                editor.putBoolean("historyline", b).apply();
                graphUpdater();
            }
        });

        ImageButton sharebtn1 = findViewById(R.id.sharegraph1);
        final GraphView graph1 = findViewById(R.id.historygraph);
        sharebtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                graph1.takeSnapshotAndShare(getApplicationContext(), "Coffee Monitor history", "Coffee Monitor History Graph");
            }
        });

        ImageButton sharebtn2 = findViewById(R.id.sharegraph2);
        final GraphView graph2 = findViewById(R.id.piegraph);
        sharebtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                graph2.takeSnapshotAndShare(getApplicationContext(), "Coffee Monitor history", "Coffee Monitor Pie Graph");
            }
        });

        final Button addcupdatebtn = findViewById(R.id.addcupdatebtn);
        Calendar cld = Calendar.getInstance();
        final DatePickerDialog StartTime = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Coffeetype type = db.coffetypeDao().getAll().get(0);
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MMM/yyy HH:mm:ss:SSS", Locale.getDefault());
                String Date = sdf.format(newDate.getTime());
                sdf = new SimpleDateFormat("yyy/MM/dd", Locale.getDefault());
                String Day = sdf.format(newDate.getTime());
                db.cupDAO().insert(new Cup(type.getKey(), Date, Day));
                type.setQnt(type.getQnt() + 1);
                db.coffetypeDao().update(type);
            }
        }, cld.get(Calendar.YEAR), cld.get(Calendar.MONTH), cld.get(Calendar.DAY_OF_MONTH));
        addcupdatebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StartTime.show();
            }
        });
    }


    private void insertStandardTypes() {
        if (db.coffetypeDao().getAll().size() == 0) {
            db.coffetypeDao().insert(new Coffeetype("Caffè espresso", 30, "Tazzina di caffè da bar o da moka.", true, "Caffeina", 0));
            db.coffetypeDao().insert(new Coffeetype("Cappuccino", 150, "Tazza di cappuccino da bar.", true, "Caffeina", 0));
            db.coffetypeDao().insert(new Coffeetype("Caffè ristretto", 16, "Tazzina di caffè ristretto.", true, "Caffeina", 0));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_favs:
                PopupMenu popup = new PopupMenu(findViewById(R.id.action_add).getContext(), findViewById(R.id.action_add));
                final List<Coffeetype> list = db.coffetypeDao().getFavs();
                for (Coffeetype type : list)
                    popup.getMenu().add(1, list.indexOf(type), 0, type.getName());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int pos = item.getItemId();
                        Coffeetype elem = list.get(pos);
                        elem.setQnt(elem.getQnt() + 1);
                        db.coffetypeDao().update(elem);
                        db.cupDAO().insert(new Cup(elem.getKey()));
                        graphUpdater();
                        recview.setAdapter(new RecviewAdapter(db));
                        return true;
                    }
                });
                popup.show();
                break;
            case R.id.action_notifs:
                //TODO implementare consigli e notifiche
                break;

            case R.id.action_add:
                final AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(findViewById(R.id.action_favs).getContext());
                final View form = getLayoutInflater().inflate(R.layout.addtypedialog, null);
                final TextView literstxt = form.findViewById(R.id.ltrsmgtext);
                CheckBox liquidckbx = form.findViewById(R.id.liquidcheck);

                editor.putInt("qnt", 0);
                editor.putString("suffix", (liquidckbx.isChecked()) ? " ml" : " mg");
                editor.commit();
                literstxt.setText(state.getInt("qnt", 0) + state.getString("suffix", " ml"));

                ImageButton addbtn = form.findViewById(R.id.incrbtn);
                addbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int qnt = state.getInt("qnt", 0);
                        qnt += 5;
                        editor.putInt("qnt", qnt);
                        editor.commit();
                        literstxt.setText(qnt + state.getString("suffix", " ml"));
                    }
                });

                ImageButton rmvbtn = form.findViewById(R.id.decrbtn);
                rmvbtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int qnt = state.getInt("qnt", 0);
                        qnt = (qnt == 0) ? 0 : qnt - 5;
                        editor.putInt("qnt", qnt);
                        editor.commit();
                        literstxt.setText(qnt + state.getString("suffix", " ml"));
                    }
                });

                liquidckbx.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        editor.putString("suffix", (isChecked) ? " ml" : " mg");
                        editor.commit();
                        literstxt.setText(state.getInt("qnt", 0) + state.getString("suffix", " ml"));
                    }
                });

                dialogbuilder.setView(form);
                dialogbuilder.create();
                final AlertDialog dialog = dialogbuilder.show();

                Button positive = form.findViewById(R.id.confirmbtn);
                positive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EditText nameedittxt = form.findViewById(R.id.nametxt);
                        EditText descedittxt = form.findViewById(R.id.desctxt);
                        EditText sostedittxt = form.findViewById(R.id.sosttxt);
                        CheckBox liquidckbx = form.findViewById(R.id.liquidcheck);

                        String name = nameedittxt.getText().toString();
                        if (name.isEmpty()) {
                            Snackbar.make(findViewById(R.id.container), "Il nome non può essere vuoto", Snackbar.LENGTH_SHORT).show();
                        } else {
                            int liters = state.getInt("qnt", 0);
                            String desc = descedittxt.getText().toString();
                            String sostanza = sostedittxt.getText().toString();

                            boolean liquid = liquidckbx.isChecked();
                            Coffeetype newtype = new Coffeetype(name, liters, desc, liquid, sostanza, 0);

                            db.coffetypeDao().insert(newtype);

                            recview.setAdapter(new RecviewAdapter(db));
                            Snackbar.make(findViewById(R.id.container), "Tipo " + newtype.getName() + " aggiunto", Snackbar.LENGTH_SHORT).show();

                            dialog.dismiss();
                        }
                    }
                });

                Button negative = form.findViewById(R.id.cancelbtn);
                negative.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                break;
        }
        return true;
    }

    public void adInitializer() {
        MobileAds.initialize(this, "ca-app-pub-9387595638685451~9345692620");
        AdView mAdView = findViewById(R.id.banner1);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    public void graphInitializer() {
        final GraphView graph = findViewById(R.id.historygraph);
        /*graph.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                graph.takeSnapshotAndShare(getApplicationContext(), "Coffe Monitor graph", "Coffee Monitor");
                return true;
            }
        });*/
        //graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMaxXAxisSize(30);
        graph.getViewport().setScrollable(true);
    }

    public void graphUpdater() {
        GraphView graph = findViewById(R.id.historygraph);
        //days[0] è sempre il primo giorno nel db
        //days.length = cups.length
        final List<String> days = db.cupDAO().getDays();
        final List<Integer> cups = db.cupDAO().perDay();

        if (days.size() > 0) {
            LocalDate fromDate = getLocalDateFromString(days.get(0));
            LocalDate toDate = LocalDate.now();
            LocalDate current = fromDate;
            toDate = toDate.plusDays(1);
            List<LocalDate> dates = new ArrayList<>(25);
            while (current.isBefore(toDate)) {
                dates.add(current);
                current = current.plusDays(1);
            }

            graph.getViewport().setScalable(true);
            graph.getViewport().setMaxX(Date.from(dates.get(dates.size() - 1).atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime());
            if (dates.size() <= 30)
                graph.getViewport().setMinX(Date.from(dates.get(0).atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime());
            else
                graph.getViewport().setMinX(Date.from(dates.get(dates.size() - 29).atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime());
            graph.getViewport().setScalable(false);

            List<DataPoint> points = new ArrayList<>();
            int j = 0;
            int i;
            for (i = 0; i < dates.size(); i++) {
                String day = getStringFromLocalDate(dates.get(i));
                Date daydate = Date.from(dates.get(i).atStartOfDay(ZoneId.systemDefault()).toInstant());
                if (j < days.size() && day.equals(days.get(j))) {
                    points.add(new DataPoint(daydate, cups.get(j)));
                    j++;
                } else points.add(new DataPoint(daydate, 0));
            }
            DataPoint[] pointsv = new DataPoint[points.size()];
            pointsv = points.toArray(pointsv);
            graph.removeAllSeries();

            if (state.getBoolean("historyline", false)) {
                BarGraphSeries<DataPoint> seriesb = new BarGraphSeries<>(pointsv);
                seriesb.setDrawValuesOnTop(true);
                seriesb.setColor(getColor(R.color.colorAccent));
                seriesb.setSpacing(25);
                graph.addSeries(seriesb);
            } else {
                LineGraphSeries<DataPoint> seriesl = new LineGraphSeries<>(pointsv);
                seriesl.setColor(getColor(R.color.colorAccent));
                graph.addSeries(seriesl);
            }

            // set date label formatter
            graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(this));
            graph.getGridLabelRenderer().setNumHorizontalLabels(3); // only 4 because of the space

            /*graph.getViewport().setMinX(Date.from(current.atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime());
            graph.getViewport().setMaxX(Date.from(toDate.atStartOfDay(ZoneId.systemDefault()).toInstant()).getTime());*/
            graph.getViewport().setXAxisBoundsManual(true);

            graph.getGridLabelRenderer().setHumanRounding(false);
        }
    }

    public LocalDate getLocalDateFromString(String date) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        try {
            LocalDate ret = format.parse(date).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return ret;
        } catch (ParseException e) {
            return null;
        }
    }

    public String getStringFromLocalDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        return date.format(formatter);
    }
}
