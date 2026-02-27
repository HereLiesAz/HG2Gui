package com.hereliesaz.hg2gui.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.hereliesaz.hg2gui.commands.main.MainPack;
import com.hereliesaz.hg2gui.managers.AppsManager;
import com.hereliesaz.hg2gui.managers.suggestions.SuggestionsManager;
import com.hereliesaz.hg2gui.tuils.StoppableThread;

/**
 * A central repository for all executable actions (Commands and Apps).
 * <p>
 * This class indexes available commands and installed apps to provide a unified list
 * for the suggestion engine (`SuggestionsManager`). It handles background updating and sorting
 * based on usage frequency and recency.
 * </p>
 */
public class CommandRepository {

    // The primary index of suggestions
    private List<SuggestionsManager.Suggestion> index = new ArrayList<>();

    // Background worker for updating the index without blocking UI
    private StoppableThread worker;

    // Constants for scoring algorithm
    private static final long RECENT_APP_THRESHOLD = TimeUnit.DAYS.toMillis(2);
    private static final int RECENT_APP_BONUS = 50;

    /**
     * Rebuilds the index of commands and apps.
     * Call this when apps are installed/uninstalled or context changes.
     * @param pack The main pack containing references to managers.
     */
    public void update(final MainPack pack) {
        // Stop previous update if running
        if (worker != null) worker.interrupt();

        worker = new StoppableThread() {
            @Override
            public void run() {
                List<SuggestionsManager.Suggestion> newIndex = new ArrayList<>();
                final long now = System.currentTimeMillis();

                // --- Index Apps ---
                List<AppsManager.LaunchInfo> apps = pack.appsManager.shownApps();
                if (apps != null) {
                    for (AppsManager.LaunchInfo app : apps) {
                        // Create suggestion wrapper for each app
                        // publicLabel is what the user types
                        newIndex.add(new SuggestionsManager.Suggestion(null, app.publicLabel, true, SuggestionsManager.Suggestion.TYPE_APP, app));
                    }
                }

                // --- Index Internal Commands ---
                CommandAbstraction[] commands = pack.commandGroup.getCommands();
                if (commands != null) {
                    for (CommandAbstraction cmd : commands) {
                        // Use class simple name (lowercase) as command alias (e.g. "clear")
                        newIndex.add(new SuggestionsManager.Suggestion(null, cmd.getClass().getSimpleName(), true, SuggestionsManager.Suggestion.TYPE_COMMAND, cmd));
                    }
                }

                // --- Sort Suggestions ---
                // Higher score = higher position in list
                Collections.sort(newIndex, new Comparator<SuggestionsManager.Suggestion>() {
                    @Override
                    public int compare(SuggestionsManager.Suggestion o1, SuggestionsManager.Suggestion o2) {
                        int score1 = getScore(o1, now);
                        int score2 = getScore(o2, now);
                        return score2 - score1; // Descending order
                    }
                });

                // Atomically swap the index
                index = newIndex;
            }
        };
        worker.start();
    }

    /**
     * Calculates a sorting score for a suggestion.
     * @param s The suggestion to score.
     * @param now Current timestamp for recency calculations.
     * @return An integer score (higher is better).
     */
    private int getScore(SuggestionsManager.Suggestion s, long now) {
        if (s.type == SuggestionsManager.Suggestion.TYPE_APP) {
            AppsManager.LaunchInfo info = (AppsManager.LaunchInfo) s.object;

            // Base score = number of times launched
            int score = info.launchedTimes;

            // Bonus for recently installed/updated apps (makes them easier to find)
            if (info.lastUpdateTime > 0 && (now - info.lastUpdateTime) < RECENT_APP_THRESHOLD) {
                score += RECENT_APP_BONUS;
            }
            return score;
        } else if (s.type == SuggestionsManager.Suggestion.TYPE_COMMAND) {
            CommandAbstraction cmd = (CommandAbstraction) s.object;
            // Commands have static priorities defined in their class
            return cmd.priority();
        }
        return 0;
    }

    /**
     * Retrieves the current index of suggestions.
     * @return List of suggestions.
     */
    public List<SuggestionsManager.Suggestion> getSuggestions() {
        return index;
    }
}
