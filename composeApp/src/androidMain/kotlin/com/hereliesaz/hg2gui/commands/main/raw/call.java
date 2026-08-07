package com.hereliesaz.hg2gui.commands.main.raw;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hereliesaz.hg2gui.PermissionCodes;
import com.hereliesaz.hg2gui.R;
import com.hereliesaz.hg2gui.commands.CommandAbstraction;
import com.hereliesaz.hg2gui.commands.ExecutePack;
import com.hereliesaz.hg2gui.commands.main.MainPack;
import com.hereliesaz.hg2gui.tuils.Tuils;

public class call implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        final MainPack info = (MainPack) pack;
        if (ContextCompat.checkSelfPermission(info.androidContext, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(info.androidContext, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions((Activity) info.androidContext, new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE}, PermissionCodes.COMMAND_REQUEST_PERMISSION);
            return info.getContext().getString(R.string.output_waitingpermission);
        }

        String number = info.getString();
        if(number == null) return pack.getContext().getString(R.string.invalid_number);

        StringBuilder s = new StringBuilder(Tuils.EMPTYSTRING);
        for(char c : number.toCharArray()) {
            if(c == '#') s.append(Uri.encode("#"));
            else s.append(c);
        }

        Uri uri = Uri.parse("tel:" + s);
        if(uri == null) return pack.getContext().getString(R.string.invalid_number);

        final Intent intent = new Intent(Intent.ACTION_CALL, uri);

        try {
            ((Activity) info.androidContext).runOnUiThread(() -> info.androidContext.startActivity(intent));
        } catch (SecurityException e) {
            return info.res.getString(R.string.output_nopermissions);
        }

        return info.res.getString(R.string.calling) + " " + number;
    }

    @Override
    public int helpRes() {
        return R.string.help_call;
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.CONTACTNUMBER};
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        return info.getContext().getString(helpRes());
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        MainPack info = (MainPack) pack;
        return info.res.getString(R.string.output_numbernotfound);
    }

}
