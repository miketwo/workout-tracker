package com.miketwo.workouttracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

public class PackingListsActivity extends Activity {
    private final String[] types={"Strength","Run","Swim"};
    @Override public void onCreate(Bundle state){super.onCreate(state);render();}
    @Override protected void onResume(){super.onResume();render();}
    private void render(){
        LinearLayout body=Ui.column(this);Ui.page(this,body);
        Button back=Ui.smallButton(this,"‹ Back");back.setOnClickListener(v->finish());body.addView(back);
        body.addView(Ui.title(this,"Packing lists"));
        body.addView(Ui.text(this,"These shared lists apply to every workout of that type.",16,Ui.MUTED));
        for(String type:types){
            LinearLayout card=Ui.card(this);card.addView(Ui.heading(this,type));
            int count=Db.get(this).packingItems(type).size();card.addView(Ui.text(this,count+" items",15,Ui.MUTED));
            Button edit=Ui.smallButton(this,"Edit "+type.toLowerCase()+" list");edit.setOnClickListener(v->startActivity(new Intent(this,PackingListEditorActivity.class).putExtra("workout_type",type)));card.addView(edit);body.addView(card);
        }
    }
}
