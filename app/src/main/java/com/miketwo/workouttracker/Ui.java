package com.miketwo.workouttracker;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

final class Ui {
    static final int INK=Color.rgb(23,33,27), FOREST=Color.rgb(23,107,77), CREAM=Color.rgb(247,245,238), MINT=Color.rgb(221,239,230), MUTED=Color.rgb(92,104,97), WHITE=Color.WHITE, RED=Color.rgb(160,48,48);
    private Ui() {}
    static int dp(Context c,int value){return (int)(value*c.getResources().getDisplayMetrics().density+.5f);}
    static LinearLayout column(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(c,20),dp(c,18),dp(c,20),dp(c,32));v.setBackgroundColor(CREAM);return v;}
    static ScrollView page(Activity a, LinearLayout body){ScrollView s=new ScrollView(a);s.setFillViewport(true);s.addView(body);a.setContentView(s);styleWindow(a);respectSystemBars(s);return s;}
    static void screen(Activity a, View body){a.setContentView(body);styleWindow(a);respectSystemBars(body);}
    static void openHome(Activity a){Intent home=new Intent(a,HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);a.startActivity(home);a.finish();}
    private static void styleWindow(Activity a){a.getWindow().setStatusBarColor(CREAM);a.getWindow().setNavigationBarColor(CREAM);a.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);}
    private static void respectSystemBars(View view){final int left=view.getPaddingLeft(),top=view.getPaddingTop(),right=view.getPaddingRight(),bottom=view.getPaddingBottom();view.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(left,top+insets.getSystemWindowInsetTop(),right,bottom+insets.getSystemWindowInsetBottom());return insets;});view.requestApplyInsets();}
    static TextView text(Context c,String value,int sp,int color){TextView t=new TextView(c);t.setText(value);t.setTextSize(sp);t.setTextColor(color);t.setLineSpacing(0,1.08f);return t;}
    static TextView title(Context c,String value){TextView t=text(c,value,32,INK);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,0,0,dp(c,8));return t;}
    static TextView heading(Context c,String value){TextView t=text(c,value,21,INK);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setPadding(0,dp(c,14),0,dp(c,8));return t;}
    static LinearLayout exerciseHeader(Context c,String exerciseName,String displayName,int textSize){
        LinearLayout row=row(c);int drawable=ExerciseVisuals.drawableFor(exerciseName);
        if(drawable!=0){ImageView image=new ImageView(c);image.setImageResource(drawable);image.setContentDescription(exerciseName+" exercise illustration");image.setScaleType(ImageView.ScaleType.FIT_CENTER);LinearLayout.LayoutParams imageParams=new LinearLayout.LayoutParams(dp(c,textSize>=30?94:76),dp(c,textSize>=30?82:64));imageParams.setMargins(0,0,dp(c,12),0);row.addView(image,imageParams);}
        TextView title=text(c,displayName,textSize,INK);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setLineSpacing(0,1.0f);title.setGravity(Gravity.CENTER_VERTICAL);title.setPadding(0,dp(c,4),0,dp(c,4));row.addView(title,new LinearLayout.LayoutParams(0,-2,1));return row;
    }
    static TextView label(Context c,String value){TextView t=text(c,value.toUpperCase(Locale.getDefault()),12,MUTED);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setLetterSpacing(.08f);t.setPadding(0,dp(c,8),0,dp(c,4));return t;}
    static GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(radius);return g;}
    static LinearLayout card(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);v.setPadding(dp(c,18),dp(c,16),dp(c,18),dp(c,16));v.setBackground(bg(WHITE,dp(c,18)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(c,8),0,dp(c,8));v.setLayoutParams(p);v.setElevation(dp(c,1));return v;}
    static Button button(Context c,String value,boolean primary){Button b=new Button(c);b.setText(value);b.setTextSize(17);b.setAllCaps(false);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(primary?WHITE:FOREST);b.setBackground(bg(primary?FOREST:MINT,dp(c,14)));b.setGravity(Gravity.CENTER);b.setMinHeight(dp(c,56));b.setPadding(dp(c,16),dp(c,10),dp(c,16),dp(c,10));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(c,7),0,dp(c,7));b.setLayoutParams(p);return b;}
    static Button iconButton(Context c,String value,int iconRes,boolean primary){Button b=button(c,value,primary);Drawable icon=c.getDrawable(iconRes);b.setCompoundDrawablesWithIntrinsicBounds(icon,null,null,null);b.setCompoundDrawablePadding(dp(c,12));b.setGravity(Gravity.CENTER_VERTICAL);return b;}
    static Button smallButton(Context c,String value){Button b=button(c,value,false);b.setTextSize(14);b.setMinHeight(dp(c,44));return b;}
    static EditText input(Context c,String hint){EditText e=new EditText(c);e.setHint(hint);e.setTextSize(18);e.setTextColor(INK);e.setHintTextColor(Color.rgb(130,138,133));e.setSingleLine(false);e.setBackground(bg(WHITE,dp(c,12)));e.setPadding(dp(c,14),dp(c,12),dp(c,14),dp(c,12));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(0,dp(c,4),0,dp(c,8));e.setLayoutParams(p);return e;}
    static LinearLayout row(Context c){LinearLayout r=new LinearLayout(c);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);return r;}
    static void weighted(View v,float weight){v.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,weight));}
    static void spacer(LinearLayout l,int h){View v=new View(l.getContext());l.addView(v,new LinearLayout.LayoutParams(1,dp(l.getContext(),h)));}
}
