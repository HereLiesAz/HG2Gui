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

public class CommandRepository {

    private List<SuggestionsManager.Suggestion> index = new ArrayList<>();
    private StoppableThread worker;

    private static final long RECENT_APP_THRESHOLD = TimeUnit.DAYS.toMillis(2);
    private static final int RECENT_APP_BONUS = 50;

    public void update(final MainPack pack) {
        if (worker != null) worker.interrupt();
        worker = new StoppableThread() {
            @Override
            public void run() {
                List<SuggestionsManager.Suggestion> newIndex = new ArrayList<>();
                final long now = System.currentTimeMillis();

                // Add apps
                List<AppsManager.LaunchInfo> apps = pack.appsManager.shownApps();
                if (apps != null) {
                    for (AppsManager.LaunchInfo app : apps) {
                        // Use publicLabel as text
                        newIndex.add(new SuggestionsManager.Suggestion(null, app.publicLabel, true, SuggestionsManager.Suggestion.TYPE_APP, app));
                    }
                }

                // Add internal commands
                CommandAbstraction[] commands = pack.commandGroup.getCommands();
                if (commands != null) {
                    for (CommandAbstraction cmd : commands) {
                        newIndex.add(new SuggestionsManager.Suggestion(null, cmd.getClass().getSimpleName(), true, SuggestionsManager.Suggestion.TYPE_COMMAND, cmd));
                    }
                }

                // Sort
                Collections.sort(newIndex, new Comparator<SuggestionsManager.Suggestion>() {
                    @Override
                    public int compare(SuggestionsManager.Suggestion o1, SuggestionsManager.Suggestion o2) {
                        int score1 = getScore(o1, now);
                        int score2 = getScore(o2, now);
                        return score2 - score1; // Descending
                    }
                });

                index = newIndex;
            }
        };
        worker.start();
    }

    private int getScore(SuggestionsManager.Suggestion s, long now) {
        if (s.type == SuggestionsManager.Suggestion.TYPE_APP) {
            AppsManager.LaunchInfo info = (AppsManager.LaunchInfo) s.object;
            int score = info.launchedTimes;
            if (info.lastUpdateTime > 0 && (now - info.lastUpdateTime) < RECENT_APP_THRESHOLD) {
                score += RECENT_APP_BONUS;
            }
            return score;
        } else if (s.type == SuggestionsManager.Suggestion.TYPE_COMMAND) {
            CommandAbstraction cmd = (CommandAbstraction) s.object;
            return cmd.priority();
        }
        return 0;
    }

    public List<SuggestionsManager.Suggestion> getSuggestions() {
        return index;
    }
}
