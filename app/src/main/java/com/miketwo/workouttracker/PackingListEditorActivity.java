package com.miketwo.workouttracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

public class PackingListEditorActivity extends Activity {
    private Db db; private String type;
    @Override public void onCreate(Bundle state){super.onCreate(state);db=Db.get(this);type=getIntent().getStringExtra("workout_type");if(type==null){finish();return;}render();}
    @Override protected void onResume(){super.onResume();if(db!=null)render();}
    private void render(){
        LinearLayout body=Ui.column(this);Ui.page(this,body);
        Button back=Ui.smallButton(this,"‹ Packing lists");back.setOnClickListener(v->finish());body.addView(back);
        body.addView(Ui.title(this,type+" packing"));
        body.addView(Ui.text(this,"Changes apply to every "+type.toLowerCase()+" workout.",16,Ui.MUTED));
        body.addView(Ui.heading(this,"Items"));
        for(Models.PackingItem item:db.packingItems(type)){
            LinearLayout card=Ui.card(this);card.addView(Ui.heading(this,item.name));
            Button remove=Ui.smallButton(this,"Remove");remove.setOnClickListener(v->{db.deletePackingItem(item.id);render();});card.addView(remove);body.addView(card);
        }
        Button add=Ui.button(this,"Add item",true);add.setOnClickListener(v->addItem());body.addView(add);
    }
    private void addItem(){
        EditText input=Ui.input(this,"Item to bring");
        new AlertDialog.Builder(this).setTitle("Add packing item").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Add",(d,w)->{
            String name=input.getText().toString().trim();
            if(!name.isEmpty()){db.addPackingItem(type,name);render();}
        }).show();
    }
}
