
package app.crossword.yourealwaysbe.view;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import app.crossword.yourealwaysbe.forkyz.R;

public class StoragePermissionDialog extends DialogFragment {
    public static final String RESULT_CODE_KEY = "resultCode";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(
            new ContextThemeWrapper(getActivity(), R.style.dialogStyle)
        );

        int resultCode = getArguments().getInt(RESULT_CODE_KEY);

        builder.setTitle(R.string.allow_permissions)
            .setMessage(R.string.please_allow_storage)
            .setPositiveButton(
                android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(
                        DialogInterface dialogInterface, int i
                    ) {
                        ActivityCompat.requestPermissions(
                            getActivity(),
                            new String[] {
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            },
                            resultCode
                        );
                    }
                }
            );

        return builder.create();
    }
}

