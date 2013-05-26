package ie.broadsheet.app.dialog;

import ie.broadsheet.app.BaseFragmentActivity;
import ie.broadsheet.app.R;
import ie.broadsheet.app.model.json.SubmitTipResponse;
import ie.broadsheet.app.requests.SubmitTipRequest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

public class TipDialog extends DialogFragment implements OnClickListener, android.view.View.OnClickListener {
    private static final String TAG = "TipDialog";

    private static int RESULT_LOAD_IMAGE = 1;

    private EditText name;

    private EditText email;

    private EditText message;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_submit_tip, null);

        name = (EditText) view.findViewById(R.id.submitterName);
        email = (EditText) view.findViewById(R.id.submitorEmail);
        message = (EditText) view.findViewById(R.id.submitBody);

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);

        email.setText(sharedPref.getString("email", ""));
        name.setText(sharedPref.getString("commenterName", ""));

        Button selectPhoto = (Button) view.findViewById(R.id.addImage);
        selectPhoto.setOnClickListener(this);

        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.submit_tip, this)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        TipDialog.this.getDialog().cancel();
                    }
                });

        final Dialog dialog = builder.create();

        name.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            Button positiveButton = (Button) d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(this);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getActivity().getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            Log.d(TAG, "Picture path is : " + picturePath);

            ImageView imageView = (ImageView) getDialog().findViewById(R.id.sumbitorImage);
            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));

        }

    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

        if (v.getId() == R.id.addImage) {
            Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            startActivityForResult(i, RESULT_LOAD_IMAGE);
        } else {
            Log.d(TAG, "clicked submit button");

            SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("email", email.getText().toString());
            editor.putString("commenterName", name.getText().toString());
            editor.commit();

            SubmitTipRequest request = new SubmitTipRequest();
            request.setName(name.getText().toString());
            request.setEmail(email.getText().toString());
            request.setMessage(message.getText().toString());

            BaseFragmentActivity activity = (BaseFragmentActivity) getActivity();

            activity.getSpiceManager().execute(request, "", DurationInMillis.NEVER, new SubmitTipRequestListener());
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        Log.d(TAG, "the other click listener");
    }

    // ============================================================================================
    // INNER CLASSES
    // ============================================================================================

    public final class SubmitTipRequestListener implements RequestListener<SubmitTipResponse> {

        @Override
        public void onRequestFailure(SpiceException spiceException) {
            Log.d(TAG, "Failed to get results: " + spiceException.toString());
        }

        @Override
        public void onRequestSuccess(final SubmitTipResponse result) {
            Log.d(TAG, "we got result: " + result.toString());

            TipDialog.this.dismiss();
        }
    }
}