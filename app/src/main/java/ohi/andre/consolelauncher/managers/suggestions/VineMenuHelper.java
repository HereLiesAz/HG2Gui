package ohi.andre.consolelauncher.managers.suggestions;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.tuils.Tuils;

public class VineMenuHelper {

    public static View createVineMenu(Context context, List<SuggestionsManager.Suggestion> options, final SuggestionsManager manager) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View rootView = inflater.inflate(R.layout.layout_vine_menu, null);

        LinearLayout capsule = rootView.findViewById(R.id.vine_capsule);
        ImageView vine = rootView.findViewById(R.id.vine_stem);

        for (int i = 0; i < options.size(); i++) {
            final SuggestionsManager.Suggestion s = options.get(i);

            // Container for icon + tooltip
            LinearLayout itemContainer = new LinearLayout(context);
            itemContainer.setOrientation(LinearLayout.VERTICAL);
            itemContainer.setGravity(Gravity.CENTER);
            itemContainer.setClipChildren(false);

            // Icon Button
            ImageView iconView = new ImageView(context);
            int iconRes = (s.type == SuggestionsManager.Suggestion.TYPE_MENU_OPTION && ((MenuOption)s.object).icon != -1)
                          ? ((MenuOption)s.object).icon
                          : R.drawable.ic_enter; // Fallback

            iconView.setImageResource(iconRes);
            int size = Tuils.dpToPx(context, 40);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(8, 0, 8, 0);
            iconView.setLayoutParams(lp);
            iconView.setPadding(10, 10, 10, 10);

            // Tint
            int color = Color.WHITE;
            if (s.type == SuggestionsManager.Suggestion.TYPE_MENU_OPTION) {
                color = ContextCompat.getColor(context, ((MenuOption)s.object).color);
            }
            iconView.setColorFilter(color, PorterDuff.Mode.SRC_IN);

            // Click Listener
            iconView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    manager.clickSuggestion(s);
                }
            });

            // Label (Tooltip style)
            TextView label = new TextView(context);
            label.setText(s.text);
            label.setTextSize(10);
            label.setTextColor(color);
            label.setGravity(Gravity.CENTER);
            label.setAlpha(0.6f);

            itemContainer.addView(label); // Add label above
            itemContainer.addView(iconView);

            capsule.addView(itemContainer);

            // Divider
            if (i < options.size() - 1) {
                View divider = new View(context);
                LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(Tuils.dpToPx(context, 1), Tuils.dpToPx(context, 20));
                divider.setLayoutParams(divLp);
                divider.setBackgroundColor(Color.parseColor("#44A4F644"));
                capsule.addView(divider);
            }
        }

        // Animate Entrance
        capsule.setTranslationY(100);
        vine.setScaleY(0);

        // Vine Growth
        ObjectAnimator vineGrow = ObjectAnimator.ofFloat(vine, "scaleY", 0f, 1f);
        vineGrow.setDuration(400);
        vineGrow.setInterpolator(new AccelerateDecelerateInterpolator());

        // Capsule Pop
        ObjectAnimator capsuleFade = ObjectAnimator.ofFloat(capsule, "alpha", 0f, 1f);
        ObjectAnimator capsuleMove = ObjectAnimator.ofFloat(capsule, "translationY", 100f, 0f);

        AnimatorSet popSet = new AnimatorSet();
        popSet.playTogether(capsuleFade, capsuleMove);
        popSet.setDuration(500);
        popSet.setStartDelay(200);
        popSet.setInterpolator(new OvershootInterpolator());

        // Sway
        ObjectAnimator sway = ObjectAnimator.ofFloat(rootView, "rotation", -2f, 2f);
        sway.setDuration(3000);
        sway.setRepeatCount(ObjectAnimator.INFINITE);
        sway.setRepeatMode(ObjectAnimator.REVERSE);
        sway.setInterpolator(new AccelerateDecelerateInterpolator());

        AnimatorSet fullSet = new AnimatorSet();
        fullSet.playSequentially(vineGrow, popSet);
        fullSet.start();
        sway.start();

        // Make visible after setup
        capsule.setVisibility(View.VISIBLE);

        return rootView;
    }
}
