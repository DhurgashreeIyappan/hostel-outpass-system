package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class AcceptedRequestAdapter extends ArrayAdapter<OutpassRequest> {

    private final List<OutpassRequest> requestList;
    private final LayoutInflater inflater;
    private final RequestActionCallback callback;

    public AcceptedRequestAdapter(Context context, List<OutpassRequest> requestList, RequestActionCallback callback) {
        super(context, R.layout.item_accepted_request, requestList);
        this.requestList = requestList;
        this.inflater = LayoutInflater.from(context);
        this.callback = callback;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_accepted_request, parent, false);
        }

        OutpassRequest request = requestList.get(position);

        // Bind data to the views
        ((TextView) convertView.findViewById(R.id.nameTextView)).setText("Name: " + request.getName());
        ((TextView) convertView.findViewById(R.id.rollNumberTextView)).setText("Roll Number: " + request.getRollNumber());
        ((TextView) convertView.findViewById(R.id.roomNumberTextView)).setText("Room Number: " + request.getRoomNumber());
        ((TextView) convertView.findViewById(R.id.dateFromTextView)).setText("Date From: " + request.getDateFrom());
        ((TextView) convertView.findViewById(R.id.dateToTextView)).setText("Date To: " + request.getDateTo());
        ((TextView) convertView.findViewById(R.id.inTimeTextView)).setText("In Time: " + request.getInTime());
        ((TextView) convertView.findViewById(R.id.outTimeTextView)).setText("Out Time: " + request.getOutTime());
        ((TextView) convertView.findViewById(R.id.reasonTextView)).setText("Reason: " + request.getReason());

        Button acceptButton = convertView.findViewById(R.id.acceptWardenButton);
        Button rejectButton = convertView.findViewById(R.id.rejectWardenButton);

        acceptButton.setOnClickListener(v -> {
            callback.onRequestAction(request.getRequestId(), "accepted");
            // Remove the request from the list after accepting
            requestList.remove(position);
            notifyDataSetChanged();  // Notify the adapter that the data set has changed
        });

        rejectButton.setOnClickListener(v -> {
            callback.onRequestAction(request.getRequestId(), "rejected");
            // Remove the request from the list after rejecting
            requestList.remove(position);
            notifyDataSetChanged();  // Notify the adapter that the data set has changed
        });

        return convertView;
    }

    public interface RequestActionCallback {
        void onRequestAction(String requestId, String wstatus);
        void updateRequestStatus(String requestId, String newStatus);
    }
}
