package com.fexed.coffeecounter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * Created by fexed on 01/12/2017.
 */

public class TypeRecviewAdapter extends RecyclerView.Adapter<TypeRecviewAdapter.ViewHolder> {
    AppDatabase db;
    private List<Coffeetype> mDataset;
    Context context;
    RecyclerView recv;

    public TypeRecviewAdapter(AppDatabase db, RecyclerView recv) {
        this.mDataset = db.coffetypeDao().getAll();
        this.db = db;
        this.recv = recv;
    }

    /*public static Bitmap getBitmapFromView(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        view.layout(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        view.draw(c);
        return bitmap;
    }*/

    @Override
    @NonNull
    public TypeRecviewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CardView v = (CardView) LayoutInflater.from(parent.getContext()).inflate(R.layout.coffee_card, parent, false);
        ViewHolder vh = new ViewHolder(v);
        this.context = parent.getContext();
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.nameTextView.setText(mDataset.get(position).getName());

        final TextView cupstxtv = holder.cupsTextView;
        String str = "" + mDataset.get(position).getQnt();
        cupstxtv.setText(str);

        TextView desctxtv = holder.descTextView;
        desctxtv.setText(mDataset.get(position).toBigString());

        if(mDataset.get(position).isDefaulttype()) holder.defaultTextView.setText(R.string.defaulttxt);
        else holder.defaultTextView.setText("");

        Button addbtn = holder.mCardView.findViewById(R.id.addbtn);
        addbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDataset.get(position).setQnt(mDataset.get(position).getQnt() + 1);
                db.coffetypeDao().update(mDataset.get(position));
                db.cupDAO().insert(new Cup(mDataset.get(position).getKey()));
                String str = "" + mDataset.get(position).getQnt();
                cupstxtv.setText(str);
            }
        });
        addbtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mDataset.get(position).setQnt(mDataset.get(position).getQnt() + 5);
                db.coffetypeDao().update(mDataset.get(position));
                for (int i = 0; i < 5; i++) db.cupDAO().insert(new Cup(mDataset.get(position).getKey()));
                String str = "" + mDataset.get(position).getQnt();
                cupstxtv.setText(str);
                return true;
            }
        });

        Button removebtn = holder.mCardView.findViewById(R.id.removebtn);
        removebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int n = mDataset.get(position).getQnt();
                mDataset.get(position).setQnt((n == 0) ? 0 : n-1);
                db.coffetypeDao().update(mDataset.get(position));
                db.cupDAO().deleteMostRecent(mDataset.get(position).getKey());
                String str = "" + mDataset.get(position).getQnt();
                cupstxtv.setText(str);
            }
        });
        removebtn.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mDataset.get(position).setQnt(0);
                db.coffetypeDao().update(mDataset.get(position));
                db.cupDAO().deleteAll(mDataset.get(position).getKey());
                String str = "" + mDataset.get(position).getQnt();
                cupstxtv.setText(str);
                return true;
            }
        });

        holder.nameTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @SuppressLint("StringFormatInvalid")
            @Override
            public boolean onLongClick(View v) {

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
            }
        });

        holder.favbtn.setImageResource((mDataset.get(position).isFav()) ? R.drawable.ic_favstarfull : R.drawable.ic_favstarempty);
        holder.favbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDataset.get(position).setFav(!(mDataset.get(position).isFav()));
                db.coffetypeDao().update(mDataset.get(position));
                ImageButton btn = (ImageButton) v;
                btn.setImageResource((mDataset.get(position).isFav()) ? R.drawable.ic_favstarfull : R.drawable.ic_favstarempty);
            }
        });

        holder.editbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder dialogbuilder = new AlertDialog.Builder(context);
                final View form = View.inflate(context, R.layout.addtypedialog, null);
                final TextView literstxt = form.findViewById(R.id.ltrsmgtext);
                final CheckBox liquidckbx = form.findViewById(R.id.liquidcheck);
                final boolean liquido = mDataset.get(position).isLiquido();
                final int qnt = mDataset.get(position).getLiters();
                EditText nameedittxt = form.findViewById(R.id.nametxt);
                EditText descedittxt = form.findViewById(R.id.desctxt);
                EditText sostedittxt = form.findViewById(R.id.sosttxt);
                EditText pricetedittxt = form.findViewById(R.id.pricetxt);
                ImageButton defaultdbbtn = form.findViewById(R.id.defaultbtn);
                ImageButton qrbtn = form.findViewById(R.id.scanqrbtn);
                defaultdbbtn.setVisibility(View.INVISIBLE);
                qrbtn.setVisibility(View.INVISIBLE);

                nameedittxt.setText(mDataset.get(position).getName());
                descedittxt.setText(mDataset.get(position).getDesc());
                sostedittxt.setText(mDataset.get(position).getSostanza());
                String str = "" + mDataset.get(position).getPrice();
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

                /*typeimage.setOnLongClickListener(new View.OnLongClickListener() { //TODO
                    @Override
                    public boolean onLongClick(View view) {
                        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                        currentbitmap = null;
                        currentimageview = typeimage;
                        startActivityForResult(i, 9);
                        return true;
                    }
                });*/

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
                            Snackbar.make(form.findViewById(R.id.linearLayout), R.string.nameemptyalert, Snackbar.LENGTH_SHORT).show();
                        } else {
                            mDataset.get(position).setName(nameedittxt.getText().toString());
                            mDataset.get(position).setDesc(descedittxt.getText().toString());
                            mDataset.get(position).setLiquido(liquidckbx.isChecked());
                            mDataset.get(position).setSostanza(sostedittxt.getText().toString());
                            mDataset.get(position).setPrice(Float.parseFloat(pricetedittxt.getText().toString()));
                            mDataset.get(position).setDefaulttype(false);

                            db.coffetypeDao().update(mDataset.get(position));
                            TypeRecviewAdapter.this.recv.setAdapter(new TypeRecviewAdapter(db, TypeRecviewAdapter.this.recv));
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
            }
        });


        if (mDataset.get(position).getImg() != null) {
            Bitmap bmp = loadImageFromStorage(mDataset.get(position).getImg());
            if (bmp != null) holder.typeimage.setImageBitmap(bmp);
        }
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
        db.cupDAO().deleteAll(mDataset.get(position).getKey());
        db.coffetypeDao().delete(mDataset.get(position));
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
            typeimage = mCardView.findViewById(R.id.cardtypeimageview);
            editbtn = mCardView.findViewById(R.id.editbtn);
        }
    }

}
