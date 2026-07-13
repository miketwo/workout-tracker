package com.miketwo.workouttracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

public class PackingChecklistActivity extends Activity {
    private String type;
    @Override public void onCreate(Bundle state){super.onCreate(state);type=getIntent().getStringExtra("workout_type");if(type==null){finish();return;}render();}
    private void render(){
        LinearLayout body=Ui.column(this);Ui.page(this,body);
        Button back=Ui.smallButton(this,"‹ Back");back.setOnClickListener(v->finish());body.addView(back);
        body.addView(Ui.title(this,"Pack for "+type));
        body.addView(Ui.text(this,"Check items off as you pack. This list resets when you leave.",16,Ui.MUTED));
        for(Models.PackingItem item:Db.get(this).packingItems(type)){
            CheckBox check=new CheckBox(this);check.setText(item.name);check.setTextSize(18);check.setTextColor(Ui.INK);check.setPadding(Ui.dp(this,4),Ui.dp(this,8),Ui.dp(this,4),Ui.dp(this,8));body.addView(check);
        }
        Button edit=Ui.smallButton(this,"Edit "+type.toLowerCase()+" packing list");edit.setOnClickListener(v->startActivity(new Intent(this,PackingListEditorActivity.class).putExtra("workout_type",type)));body.addView(edit);
    }
}
