package com.miketwo.workouttracker;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Lightweight, continuously animated movement cue; it never rebuilds with the timer. */
final class IntervalMotionView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private ValueAnimator animator;private float motion;private boolean jogging,paused;
    IntervalMotionView(Context context){super(context);paint.setColor(Ui.FOREST);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Ui.dp(context,7));paint.setStrokeCap(Paint.Cap.ROUND);setContentDescription("Current interval movement");setJogging(true);}
    void setJogging(boolean value){if(jogging==value&&animator!=null)return;jogging=value;if(animator!=null)animator.cancel();animator=ValueAnimator.ofFloat(0f,1f);animator.setDuration(jogging?500:900);animator.setRepeatCount(ValueAnimator.INFINITE);animator.setInterpolator(new LinearInterpolator());animator.addUpdateListener(a->{motion=(float)a.getAnimatedValue();invalidate();});animator.start();}
    void setPaused(boolean value){if(paused==value)return;paused=value;if(animator==null)return;if(paused)animator.pause();else animator.resume();}
    @Override protected void onDetachedFromWindow(){if(animator!=null)animator.cancel();super.onDetachedFromWindow();}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),cycle=(float)Math.sin(motion*Math.PI*2),stride=cycle*w*(jogging?.19f:.12f),lift=Math.max(0,cycle)*(jogging?h*.10f:h*.045f),base=h*.84f,lean=jogging?-w*.035f:-w*.015f,hipX=w*.5f+lean,hipY=h*.59f-lift*.18f,shoulderX=hipX+lean,shoulderY=h*.36f-lift*.35f,headX=shoulderX+lean*.5f,headY=h*.21f-lift*.4f;
        c.drawCircle(headX,headY,w*.075f,paint);c.drawLine(shoulderX,shoulderY,hipX,hipY,paint);
        // Arms swing opposite the legs; elbows keep the movement recognizably human rather than radial.
        limb(c,shoulderX,shoulderY,shoulderX-stride*.45f,shoulderY+h*.13f,shoulderX-stride*.8f,shoulderY+h*.24f);limb(c,shoulderX,shoulderY,shoulderX+stride*.4f,shoulderY+h*.12f,shoulderX+stride*.72f,shoulderY+h*.23f);
        // Side-profile stride: the forward knee lifts and the rear leg extends behind the body.
        limb(c,hipX,hipY,hipX+stride*.42f,base-h*.18f-lift,hipX+stride,base);limb(c,hipX,hipY,hipX-stride*.32f,base-h*.13f,hipX-stride*.9f,base);
    }
    private void limb(Canvas c,float x1,float y1,float x2,float y2,float x3,float y3){c.drawLine(x1,y1,x2,y2,paint);c.drawLine(x2,y2,x3,y3,paint);}
}
