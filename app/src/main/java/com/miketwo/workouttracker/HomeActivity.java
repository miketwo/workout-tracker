package com.miketwo.workouttracker;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HomeActivity extends Activity {
    // Short excerpts from James Allen's public-domain 1903 book, As a Man Thinketh.
    // Source: https://www.gutenberg.org/ebooks/4507
    private static final String[] QUOTES = {
        "Good thoughts bear good fruit.",
        "Circumstance does not make the man; it reveals him to himself.",
        "Men do not attract that which they want, but that which they are.",
        "Aimlessness is a vice.",
        "Thought allied fearlessly to purpose becomes creative force.",
        "A man can only rise, conquer, and achieve by lifting up his thoughts.",
        "Achievement, of whatever kind, is the crown of effort.",
        "Victories attained by right thought can only be maintained by watchfulness.",
        "The dreamers are the saviours of the world.",
        "To desire is to obtain; to aspire is to achieve.",
        "Dreams are the seedlings of realities.",
        "Self-control is strength; right thought is mastery; calmness is power."
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout body = Ui.column(this);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout introduction = new LinearLayout(this);
        introduction.setOrientation(LinearLayout.VERTICAL);
        introduction.setGravity(Gravity.CENTER);
        TextView name = Ui.title(this, "Workout Tracker");
        name.setTextSize(38);
        name.setGravity(Gravity.CENTER);
        introduction.addView(name);
        TextView version = Ui.text(this, "Version " + BuildConfig.VERSION_NAME + "  ·  build " + BuildConfig.VERSION_CODE, 17, Ui.MUTED);
        version.setGravity(Gravity.CENTER);
        introduction.addView(version);
        TextView deployed = Ui.text(this, "Deployed " + BuildConfig.DEPLOYMENT_STAMP, 13, Ui.MUTED);
        deployed.setGravity(Gravity.CENTER);
        introduction.addView(deployed);
        TextView illustrationCredit = Ui.text(this, "Exercise illustrations © Everkinetic · CC BY-SA 3.0 · Wikimedia Commons", 12, Ui.MUTED);
        illustrationCredit.setGravity(Gravity.CENTER);
        illustrationCredit.setPadding(0, Ui.dp(this, 6), 0, 0);
        introduction.addView(illustrationCredit);
        Ui.spacer(introduction, 34);
        TextView quote = Ui.text(this, "“" + nextQuote() + "”", 21, Ui.INK);
        quote.setGravity(Gravity.CENTER);
        quote.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        introduction.addView(quote);
        TextView author = Ui.text(this, "— James Allen", 15, Ui.MUTED);
        author.setGravity(Gravity.CENTER);
        author.setPadding(0, Ui.dp(this, 14), 0, 0);
        introduction.addView(author);
        body.addView(introduction, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 2f));

        LinearLayout action = new LinearLayout(this);
        action.setOrientation(LinearLayout.VERTICAL);
        action.setGravity(Gravity.CENTER_VERTICAL);
        Button plan = Ui.iconButton(this, "Plan", R.drawable.ic_plan, true);
        plan.setOnClickListener(v -> open(PlansActivity.class));
        action.addView(plan);
        Button workout = Ui.iconButton(this, "Workout", R.drawable.ic_workout, true);
        workout.setOnClickListener(v -> open(WorkoutDashboardActivity.class));
        action.addView(workout);
        Button review = Ui.iconButton(this, "Review", R.drawable.ic_review, true);
        review.setOnClickListener(v -> open(ReviewActivity.class));
        action.addView(review);
        Button profile = Ui.smallButton(this, "Profile");
        profile.setOnClickListener(v -> open(ProfileActivity.class));
        action.addView(profile);
        body.addView(action, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        Ui.screen(this, body);
    }

    private void open(Class<? extends Activity> destination) {
        startActivity(new Intent(this, destination));
    }

    private String nextQuote() {
        SharedPreferences preferences = getSharedPreferences("home", MODE_PRIVATE);
        int index = preferences.getInt("next_quote", 0) % QUOTES.length;
        preferences.edit().putInt("next_quote", (index + 1) % QUOTES.length).apply();
        return QUOTES[index];
    }
}
