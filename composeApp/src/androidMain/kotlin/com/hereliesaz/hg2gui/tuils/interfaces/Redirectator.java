package com.hereliesaz.hg2gui.tuils.interfaces;

import com.hereliesaz.hg2gui.commands.main.specific.RedirectCommand;

/**
 * Created by francescoandreuzzi on 03/03/2017.
 */

public interface Redirectator {

    void prepareRedirection(RedirectCommand cmd);
    void cleanup();
}
