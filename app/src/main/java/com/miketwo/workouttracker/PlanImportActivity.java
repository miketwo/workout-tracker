package com.miketwo.workouttracker;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PlanImportActivity extends Activity {
    private static final int OPEN_JSON = 41;
    private EditText jsonInput;
    private String source = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) source = state.getString("source", "");
        renderEntry();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (jsonInput != null) source = jsonInput.getText().toString();
        state.putString("source", source);
        super.onSaveInstanceState(state);
    }

    private void renderEntry() {
        LinearLayout body = Ui.column(this); Ui.page(this, body);
        Button back = Ui.smallButton(this, "‹ Plans"); back.setOnClickListener(v -> finish()); body.addView(back);
        body.addView(Ui.title(this, "Import a workout plan"));
        body.addView(Ui.text(this, "Ask an AI to create a plan using the app’s instructions, then paste its JSON here or choose a saved file.", 16, Ui.MUTED));

        Button copy = Ui.button(this, "Copy AI instructions", false); copy.setOnClickListener(v -> copyInstructions()); body.addView(copy);
        Button file = Ui.button(this, "Choose JSON file", false); file.setOnClickListener(v -> chooseFile()); body.addView(file);

        body.addView(Ui.label(this, "Plan JSON"));
        jsonInput = Ui.input(this, "Paste the complete JSON response here");
        jsonInput.setGravity(Gravity.TOP | Gravity.START);
        jsonInput.setMinLines(12);
        jsonInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        jsonInput.setText(source);
        body.addView(jsonInput);
        Button review = Ui.button(this, "Review plan", true); review.setOnClickListener(v -> review(jsonInput.getText().toString())); body.addView(review);
        body.addView(Ui.text(this, "Nothing is saved until you review and confirm the complete plan.", 13, Ui.MUTED));
    }

    private void review(String text) {
        source = text;
        try {
            renderPreview(PlanImport.parse(text));
        } catch (PlanImport.FormatException e) {
            jsonInput.setError(e.getMessage());
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void renderPreview(PlanImport.PlanDraft plan) {
        jsonInput = null;
        LinearLayout body = Ui.column(this); Ui.page(this, body);
        Button back = Ui.smallButton(this, "‹ Edit JSON"); back.setOnClickListener(v -> renderEntry()); body.addView(back);
        body.addView(Ui.title(this, "Review imported plan"));
        body.addView(Ui.heading(this, plan.name));
        body.addView(Ui.text(this, plan.type + "  •  " + plan.exercises.size() + " exercises", 16, Ui.MUTED));
        if (!plan.notes.isBlank()) body.addView(Ui.text(this, plan.notes, 15, Ui.MUTED));

        int index = 1;
        for (PlanImport.ExerciseDraft exercise : plan.exercises) {
            LinearLayout card = Ui.card(this);
            card.addView(Ui.exerciseHeader(this, exercise.name, index++ + ". " + exercise.name, 21));
            String target = exercise.durationSeconds > 0
                    ? exercise.sets + " sets × " + exercise.durationSeconds + " seconds"
                    : exercise.sets + " sets × " + exercise.reps + " reps @ " + ("bodyweight".equals(exercise.unit) ? "bodyweight" : WorkoutMath.formatWeight(exercise.weight) + " " + exercise.unit);
            card.addView(Ui.text(this, target + "  •  " + exercise.muscleGroup, 16, Ui.MUTED));
            card.addView(Ui.text(this, "Rest " + exercise.restSeconds + " seconds", 14, Ui.MUTED));
            if (!exercise.notes.isBlank()) card.addView(Ui.text(this, exercise.notes, 14, Ui.MUTED));
            body.addView(card);
        }

        Button confirm = Ui.button(this, "Import this plan", true);
        confirm.setOnClickListener(v -> {
            confirm.setEnabled(false);
            try {
                long planId = PlanImporter.save(Db.get(this), plan);
                Toast.makeText(this, "Plan imported", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, PlanDetailActivity.class).putExtra("plan_id", planId));
                finish();
            } catch (Exception e) {
                confirm.setEnabled(true);
                Toast.makeText(this, "Could not import plan: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        body.addView(confirm);
    }

    private void copyInstructions() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("Workout Tracker plan instructions", PlanImport.aiInstructions()));
        Toast.makeText(this, "AI instructions copied", Toast.LENGTH_SHORT).show();
    }

    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, OPEN_JSON);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != OPEN_JSON || result != RESULT_OK || data == null || data.getData() == null) return;
        try {
            source = read(data.getData());
            review(source);
        } catch (Exception e) {
            Toast.makeText(this, "Could not read plan: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private String read(Uri uri) throws Exception {
        try (InputStream input = getContentResolver().openInputStream(uri); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) throw new IllegalArgumentException("Android could not open that file.");
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > PlanImport.MAX_JSON_CHARS) throw new IllegalArgumentException("The plan is larger than 256 KB.");
                output.write(buffer, 0, count);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }
}
