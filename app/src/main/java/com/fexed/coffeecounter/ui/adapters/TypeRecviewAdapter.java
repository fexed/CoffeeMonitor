package com.fexed.coffeecounter.ui.adapters;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.fexed.coffeecounter.R;
import com.fexed.coffeecounter.data.Coffeetype;
import com.fexed.coffeecounter.data.Cup;
import com.fexed.coffeecounter.db.DBAccess;
import com.fexed.coffeecounter.sys.SaveImageToInternalTask;
import com.fexed.coffeecounter.ui.MainActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.skydoves.balloon.ArrowOrientation;
import com.skydoves.balloon.Balloon;
import com.skydoves.balloon.BalloonAnimation;
import com.skydoves.balloon.OnBalloonOutsideTouchListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;


/**
 * Created by Federico Matteoni on 01/12/2017.
 */
public class TypeRecviewAdapter extends RecyclerView.Adapter<TypeRecviewAdapter.ViewHolder> {
    public static int WHITE = 0xFFFFFFFF;
    public static int BLACK = 0xFF000000;
    public final static int WIDTH = 500;
    DBAccess db;
    private List<Coffeetype> mDataset;
    Context context;
    Activity parent;
    RecyclerView recv;
    SharedPreferences state;

    public TypeRecviewAdapter(Activity parentActivity, DBAccess db, RecyclerView recv, SharedPreferences state) throws ExecutionException, InterruptedException {
        this.mDataset = db.getTypes().get();
        this.db = db;
        this.recv = recv;
        this.state = state;
        this.parent = parentActivity;
        this.context = parentActivity;
    }

    public Cup geoTag(Cup cup) {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Criteria criteria = new Criteria();
            criteria.setPowerRequirement(Criteria.POWER_LOW);
            criteria.setCostAllowed(false);
            String provider = locationManager.getBestProvider(criteria, true);
            try {
                Location location = locationManager.getLastKnownLocation(provider);
                cup.setLongitude(location.getLongitude());
                cup.setLatitude(location.getLatitude());
            } catch (Exception ex) {
                ex.printStackTrace();
                Toast.makeText(context, R.string.locationerror, Toast.LENGTH_SHORT).show();
            }
            return cup;
        }
        return cup;
    }

    @Override
    @NonNull
    public TypeRecviewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CardView v = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.coffee_card, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        String str;
        final TextView cupstxtv = holder.cupsTextView;
        Button removebtn = holder.mCardView.findViewById(R.id.removebtn);
        Button addbtn = holder.mCardView.findViewById(R.id.addbtn);

        View.OnClickListener clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.addbtn:
                        Cup cup = new Cup(mDataset.get(position).getKey());
                        cup = geoTag(cup);
                        db.insertCup(cup);
                        String str = "" + (mDataset.get(position).getQnt() + 1);
                        mDataset.get(position).setQnt(mDataset.get(position).getQnt() + 1);
                        cupstxtv.setText(str);
                        break;
                    case R.id.removebtn:
                        int n = mDataset.get(position).getQnt();
                        try {
                            List<Cup> typecups = db.getCups(mDataset.get(position).getKey()).get();
                            Collections.reverse(typecups);
                            Cup todelete = null;
                            if (typecups.size() > 0) {
                                todelete = typecups.get(0);
                                db.deleteCup(todelete);
                                n = Math.max(0, n-1);
                                mDataset.get(position).setQnt(n);
                                str = "" + (n);
                                cupstxtv.setText(str);
                            }
                        } catch (Exception ignored) {}
                        break;
                    case R.id.favbtn:
                        mDataset.get(position).setFav(!(mDataset.get(position).isFav()));
                        db.updateTypes(mDataset.get(position));
                        ImageButton btn = (ImageButton) v;
                        btn.setImageResource((mDataset.get(position).isFav()) ? R.drawable.ic_favstarfull : R.drawable.ic_favstarempty);
                        break;
                    case R.id.editbtn:
                        final AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(context);
                        final View form = View.inflate(context, R.layout.dialog_addtype, null);
                        final TextView literstxt = form.findViewById(R.id.ltrsmgtext);
                        final CheckBox liquidckbx = form.findViewById(R.id.liquidcheck);
                        final boolean liquido = mDataset.get(position).isLiquido();
                        final int qnt = mDataset.get(position).getLiters();
                        EditText nameedittxt = form.findViewById(R.id.nametxt);
                        EditText descedittxt = form.findViewById(R.id.desctxt);
                        EditText sostedittxt = form.findViewById(R.id.sosttxt);
                        EditText pricetedittxt = form.findViewById(R.id.pricetxt);
                        final ImageView typeimage = form.findViewById(R.id.typeimage);
                        ImageButton defaultdbbtn = form.findViewById(R.id.defaultbtn);
                        ImageButton qrbtn = form.findViewById(R.id.scanqrbtn);
                        defaultdbbtn.setVisibility(View.INVISIBLE);
                        qrbtn.setVisibility(View.INVISIBLE);

                        nameedittxt.setText(mDataset.get(position).getName());
                        descedittxt.setText(mDataset.get(position).getDesc());
                        sostedittxt.setText(mDataset.get(position).getSostanza());
                        str = "" + mDataset.get(position).getPrice();
                        pricetedittxt.setText(str);

                        if (liquido) liquidckbx.setChecked(true);
                        else liquidckbx.setChecked(false);
                        str = qnt + (liquido ? " ml" : " mg");
                        literstxt.setText(str);

                        ImageButton addbtn = form.findViewById(R.id.incrbtn);
                        addbtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDataset.get(position).setLiters(mDataset.get(position).getLiters() + 5);
                                String str = mDataset.get(position).getLiters() + (liquidckbx.isChecked() ? " ml" : " mg");
                                literstxt.setText(str);
                            }
                        });

                        ImageButton rmvbtn = form.findViewById(R.id.decrbtn);
                        rmvbtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDataset.get(position).setLiters(mDataset.get(position).getLiters() - 5);
                                if (mDataset.get(position).getLiters() < 0)
                                    mDataset.get(position).setLiters(0);
                                String str = mDataset.get(position).getLiters() + (liquidckbx.isChecked() ? " ml" : " mg");
                                literstxt.setText(str);
                            }
                        });

                        liquidckbx.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                mDataset.get(position).setLiquido(isChecked);
                                String str = mDataset.get(position).getLiters() + (liquidckbx.isChecked() ? " ml" : " mg");
                                literstxt.setText(str);
                            }
                        });

                        String imgurl = mDataset.get(position).getImg();
                        if (imgurl != null && !imgurl.equals("")) {
                            typeimage.setImageBitmap(loadImageFromStorage(imgurl));
                        }
                        typeimage.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                                ((MainActivity) parent).currentbitmap = null;
                                ((MainActivity) parent).currentimageview = typeimage;
                                parent.startActivityForResult(i, 9);
                                return true;
                            }
                        });

                        dialogbuilder.setView(form);
                        dialogbuilder.create();
                        final AlertDialog dialog = dialogbuilder.show();
                        if (mDataset.get(position).isDefaulttype()) {
                            Snackbar.make(form.findViewById(R.id.linearLayout), R.string.editdefaultalert, Snackbar.LENGTH_SHORT).show();
                        }

                        Button positive = form.findViewById(R.id.confirmbtn);
                        positive.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                EditText nameedittxt = form.findViewById(R.id.nametxt);
                                EditText descedittxt = form.findViewById(R.id.desctxt);
                                EditText sostedittxt = form.findViewById(R.id.sosttxt);
                                EditText pricetedittxt = form.findViewById(R.id.pricetxt);
                                CheckBox liquidckbx = form.findViewById(R.id.liquidcheck);

                                String name = nameedittxt.getText().toString();
                                if (name.isEmpty()) {
                                    final Balloon balloon = new Balloon.Builder(context)
                                            .setText(context.getString(R.string.nameemptyalert))
                                            .setBackgroundColorResource(R.color.colorAccent)
                                            .setWidthRatio(0.75f)
                                            .setBalloonAnimation(BalloonAnimation.FADE)
                                            .setArrowVisible(true)
                                            .setArrowOrientation(ArrowOrientation.TOP)
                                            .build();
                                    balloon.setOnBalloonOutsideTouchListener(new OnBalloonOutsideTouchListener() {
                                        @Override
                                        public void onBalloonOutsideTouch(@NonNull View view, @NonNull MotionEvent motionEvent) {
                                            balloon.dismiss();
                                        }
                                    });
                                    balloon.showAlignBottom(nameedittxt);
                                } else {
                                    mDataset.get(position).setName(nameedittxt.getText().toString());
                                    mDataset.get(position).setDesc(descedittxt.getText().toString());
                                    mDataset.get(position).setLiquido(liquidckbx.isChecked());
                                    mDataset.get(position).setSostanza(sostedittxt.getText().toString());
                                    mDataset.get(position).setPrice(Float.parseFloat(pricetedittxt.getText().toString()));
                                    mDataset.get(position).setDefaulttype(false);
                                    if (((MainActivity) parent).currentbitmap != null) {
                                        SaveImageToInternalTask saveTask = (SaveImageToInternalTask) new SaveImageToInternalTask(context).execute(((MainActivity) parent).currentbitmap);
                                        String uri = new ContextWrapper(context).getDir("images", Context.MODE_PRIVATE).getAbsolutePath() + "/" + ((MainActivity) parent).currentbitmap.hashCode() + ".png";
                                        mDataset.get(position).setImg(uri);
                                        ((MainActivity) parent).currentbitmap = null;
                                    }

                                    db.updateTypes(mDataset.get(position));
                                    try {
                                        TypeRecviewAdapter.this.recv.setAdapter(new TypeRecviewAdapter(parent, db, TypeRecviewAdapter.this.recv, state));
                                    } catch (Exception ignored) {}
                                    dialog.dismiss();
                                }
                            }
                        });

                        Button negative = form.findViewById(R.id.cancelbtn);
                        negative.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mDataset.get(position).setLiquido(liquido);
                                mDataset.get(position).setQnt(qnt);
                                dialog.dismiss();
                            }
                        });
                        break;
                    case R.id.qrbtn:
                        final String codedstring = mDataset.get(position).codedString();

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setNeutralButton(R.string.annulla, null);
                        final AlertDialog dialog2 = builder.create();
                        View dialogLayout = View.inflate(context, R.layout.dialog_qrpopup, null);

                        dialog2.setView(dialogLayout);
                        dialog2.requestWindowFeature(Window.FEATURE_NO_TITLE);

                        dialog2.show();
                        Display display =((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
                        Point screen = new Point();
                        display.getSize(screen);
                        int maxwidth = (6*screen.x)/7;
                        int maxheight = (4*screen.y)/5;

                        TextView titletxtv = dialog2.findViewById(R.id.popuptitletxtv);
                        ImageView image =  dialog2.findViewById(R.id.qrimage);
                        try {
                            Bitmap bmp = encodeAsBitmap(codedstring);
                            if (bmp != null) {
                                image.setImageBitmap(bmp);
                                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) image.getLayoutParams();
                                Log.e("NBMP", bmp.getWidth() + " " + bmp.getHeight());
                                float x = ((float) maxwidth) / bmp.getWidth();
                                float y = ((float) maxheight) / bmp.getHeight();
                                float scala = Math.min(x, y);
                                Matrix matrix = new Matrix();
                                matrix.postScale(scala, scala);
                                Bitmap bmpScalata = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
                                params.width = bmpScalata.getWidth();
                                params.height = bmpScalata.getHeight();
                                image.setLayoutParams(params);
                                titletxtv.setText(mDataset.get(position).getName());
                            } else dialog2.dismiss();
                        } catch (Exception ignored) {}
                        break;
                }
            }
        };

        View.OnLongClickListener longClickListener = new View.OnLongClickListener() {
            @SuppressLint("StringFormatInvalid")
            @Override
            public boolean onLongClick(View v) {
                switch (v.getId()) {
                    case R.id.nameTxtV:
                        AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(v.getContext());
                        dialogbuilder.setMessage(context.getString(R.string.eliminarecup, mDataset.get(position).getName()))
                                .setPositiveButton(R.string.si, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        removeAt(position);
                                    }
                                })
                                .setNegativeButton(R.string.no, null);
                        dialogbuilder.create();
                        dialogbuilder.show();

                        return true;
                    default:
                        return false;
                }
            }
        };

        holder.nameTextView.setText(mDataset.get(position).getName());
        holder.nameTextView.setOnLongClickListener(longClickListener);

        str = "" + mDataset.get(position).getQnt();
        cupstxtv.setText(str);

        holder.descTextView.setText(mDataset.get(position).toBigString());

        if(mDataset.get(position).isDefaulttype()) holder.defaultTextView.setText(R.string.defaulttxt);
        else holder.defaultTextView.setText("");

        addbtn.setOnClickListener(clickListener);

        removebtn.setOnClickListener(clickListener);
        holder.favbtn.setImageResource((mDataset.get(position).isFav()) ? R.drawable.ic_favstarfull : R.drawable.ic_favstarempty);
        holder.favbtn.setOnClickListener(clickListener);
        holder.editbtn.setOnClickListener(clickListener);
        holder.qrbtn.setOnClickListener(clickListener);

        if (mDataset.get(position).getImg() != null) {
            Bitmap bmp = loadImageFromStorage(mDataset.get(position).getImg());
            if (bmp != null) holder.typeimage.setImageBitmap(bmp);
        }
    }

    Bitmap encodeAsBitmap(String str) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, WIDTH, WIDTH, null);
        } catch (IllegalArgumentException iae) {
            Log.e("QRCODE", "IllegalArgument");
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, WIDTH, 0, 0, w, h);
        return bitmap;
    }


    @Override
    public int getItemCount() {
        return mDataset.size();
    }


    public Bitmap loadImageFromStorage(String path) {
        try {
            File f = new File(path);
            return BitmapFactory.decodeStream(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void removeAt(int position) {
        db.deleteType(mDataset.get(position));
        mDataset.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mDataset.size());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public CardView mCardView;
        public TextView nameTextView;
        public TextView cupsTextView;
        public TextView descTextView;
        public TextView defaultTextView;
        public ImageButton favbtn;
        public ImageButton qrbtn;
        public ImageView typeimage;
        public Button editbtn;

        public ViewHolder(CardView v) {
            super(v);
            mCardView = v;
            nameTextView = mCardView.findViewById(R.id.nameTxtV);
            cupsTextView = mCardView.findViewById(R.id.cups_textv);
            descTextView = mCardView.findViewById(R.id.desctxtv);
            defaultTextView = mCardView.findViewById(R.id.defaulttxtv);
            favbtn = mCardView.findViewById(R.id.favbtn);
            qrbtn = mCardView.findViewById(R.id.qrbtn);
            typeimage = mCardView.findViewById(R.id.cardtypeimageview);
            editbtn = mCardView.findViewById(R.id.editbtn);
        }
    }

}
