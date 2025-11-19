package com.example.project1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MyPageFragment extends Fragment {

    private static final String ARG_USERNAME = "username";
    private static final String ARG_EMAIL = "email";

    private String username;
    private String email;

    public static MyPageFragment newInstance(String username, String email) {
        MyPageFragment fragment = new MyPageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USERNAME, username);
        args.putString(ARG_EMAIL, email);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            username = getArguments().getString(ARG_USERNAME);
            email = getArguments().getString(ARG_EMAIL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_my_page, container, false);

        TextView usernameText = view.findViewById(R.id.text_username);
        TextView emailText = view.findViewById(R.id.text_email);
        Button logoutButton = view.findViewById(R.id.button_logout);

        usernameText.setText(username != null ? username : "-");
        emailText.setText(email != null ? email : "-");

        logoutButton.setOnClickListener(v -> {
            AuthHelper authHelper = new AuthHelper(requireContext());
            authHelper.logout(requireContext());

            Intent intent = new Intent(requireActivity(), ActivityFortest.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            requireActivity().finish();
        });

        return view;
    }
}
