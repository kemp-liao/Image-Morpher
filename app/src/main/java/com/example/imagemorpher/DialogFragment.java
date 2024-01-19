package com.example.imagemorpher;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.Toast;

public class DialogFragment extends androidx.fragment.app.DialogFragment {
    private EditText frameNumberInput;

    public interface DialogListener {
        void onTextEntered(String text);
    }

    private DialogListener listener;

    public void setListener(DialogListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_layout, container, false);

        frameNumberInput = view.findViewById(R.id.frameNumberInput);
        Button generateBtn = view.findViewById(R.id.generateBtn);

        generateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInput()) {
                    if (listener != null) {
                        String enteredText = frameNumberInput.getText().toString();
                        listener.onTextEntered(enteredText);
                    }
                    dismiss();
                } else {
                    Toast.makeText(requireContext(), "Please enter a non-zero integer.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    private boolean validateInput() {
        String inputText = frameNumberInput.getText().toString().trim();
        try {
            int number = Integer.parseInt(inputText);
            return number != 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
