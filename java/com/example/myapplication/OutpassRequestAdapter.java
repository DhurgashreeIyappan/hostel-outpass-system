package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class OutpassRequestAdapter extends ArrayAdapter<OutpassRequest> {
    private final List<OutpassRequest> requestList;
    private final LayoutInflater inflater;
    private final UpdateRequestStatusCallback callback;

    public OutpassRequestAdapter(Context context, List<OutpassRequest> requestList, UpdateRequestStatusCallback callback) {
        super(context, R.layout.item_outpass_request, requestList);
        this.requestList = requestList;
        this.inflater = LayoutInflater.from(context);
        this.callback = callback;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_outpass_request, parent, false);
        }

        OutpassRequest request = requestList.get(position);

        // Find TextViews and set their values
        ((TextView) convertView.findViewById(R.id.nameTextView)).setText("Name: " + request.getName());
        ((TextView) convertView.findViewById(R.id.rollNumberTextView)).setText("Roll Number: " + request.getRollNumber());
        ((TextView) convertView.findViewById(R.id.reasonTextView)).setText("Reason: " + request.getReason());
        ((TextView) convertView.findViewById(R.id.dateFromTextView)).setText("Date From: " + request.getDateFrom());
        ((TextView) convertView.findViewById(R.id.dateToTextView)).setText("Date To: " + request.getDateTo());
        ((TextView) convertView.findViewById(R.id.inTimeTextView)).setText("In Time: " + request.getInTime());
        ((TextView) convertView.findViewById(R.id.outTimeTextView)).setText("Out Time: " + request.getOutTime());


        // Initialize buttons for pending requests
        Button approveButton = convertView.findViewById(R.id.acceptButton);
        Button rejectButton = convertView.findViewById(R.id.rejectButton);

        // Disable buttons for responded requests
        boolean isProcessed = "accepted".equalsIgnoreCase(request.getStatus()) || "rejected".equalsIgnoreCase(request.getStatus());
        approveButton.setEnabled(!isProcessed);
        rejectButton.setEnabled(!isProcessed);

        // Set click listeners for buttons
        approveButton.setOnClickListener(v -> callback.updateRequestStatus(request.getRequestId(), "accepted"));
        rejectButton.setOnClickListener(v -> callback.updateRequestStatus(request.getRequestId(), "rejected"));

        return convertView;
    }

    public interface UpdateRequestStatusCallback {
        void updateRequestStatus(String requestId, String newStatus);
    }
}
