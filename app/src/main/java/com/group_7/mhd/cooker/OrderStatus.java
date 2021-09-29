package com.group_7.mhd.cooker;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.group_7.mhd.cooker.Common.Common;
import com.group_7.mhd.cooker.Model.DataMessage;
import com.group_7.mhd.cooker.Model.MyResponse;
import com.group_7.mhd.cooker.Model.Request;
import com.group_7.mhd.cooker.Model.Token;
import com.group_7.mhd.cooker.Remote.APIService;
import com.group_7.mhd.cooker.ViewHolder.OrderViewHolder;
import com.jaredrummler.materialspinner.MaterialSpinner;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderStatus extends AppCompatActivity {

    public RecyclerView recyclerView;
    public RecyclerView.LayoutManager layoutManager;

    FirebaseRecyclerAdapter<Request, OrderViewHolder> adapter;


    FirebaseDatabase db;
    DatabaseReference requests;

    MaterialSpinner spinner, shipperSpinner;

    APIService mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_status);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Order Status");

        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);

        //init service
        mService = Common.getFCMClient();

        //init firebase database
        db= FirebaseDatabase.getInstance();
        requests=db.getReference(Common.ORDER_TABLE);

        recyclerView=(RecyclerView) findViewById(R.id.listorders);
        recyclerView.setHasFixedSize(true);
        layoutManager=new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);



        //if we start Orderstatus acctivity from Home
        //we will not put any extra
        if(getIntent().getExtras()==null)
        {
            loadOrders(Common.currentChaf.getPhone());


        }else
        {
            loadOrders(getIntent().getStringExtra("userPhone"));
        }




    }

    private void loadOrders(String phone) {
        adapter=new FirebaseRecyclerAdapter<Request, OrderViewHolder>(
                Request.class,
                R.layout.order_layout,
                OrderViewHolder.class,
                requests/*.orderByChild("phone")
                .equalTo(phone)*/

        ) {
            @Override
            protected void populateViewHolder(final OrderViewHolder viewHolder, final Request model, final int position) {

                viewHolder.txtOrderId.setText(adapter.getRef(position).getKey());
                viewHolder.txtOrderStatus.setText(Common.convertCodeToStatus(model.getStatus()));
                viewHolder.txtOrderAddress.setText(model.getAddresslat()+" , "+ model.getAddresslon());
                viewHolder.txtOrderPhone.setText(model.getPhone());
                viewHolder.txtOrderDate.setText(Common.getDate(Long.parseLong(adapter.getRef(position).getKey())));

                if (getItem(position).getPaymentMethod().equals("COD")){
                    viewHolder.chkpayemnt.setChecked(true);
                }

                if (getItem(position).getTackAway().equals("false")) {
                    Picasso.get(/*cart.getBaseContext()*/)
                            .load(R.drawable.table)
                            .resize(70,70)
                            .centerCrop()
                            .into(viewHolder.imglogo);

                }else if (getItem(position).getTackAway().equals("true")){
                    Picasso.get(/*cart.getBaseContext()*/)
                            .load(R.drawable.shipper)
                            .resize(70,70)
                            .centerCrop()
                            .into(viewHolder.imglogo);
                }

                viewHolder.btnEdit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String chk = "false";
                        if (viewHolder.chkpayemnt.isChecked()){
                            chk = "true";
                        }
                        ShowUpdateDialog(adapter.getRef(position).getKey(),adapter.getItem(position),model.getStatus(),chk);
                    }
                });

                viewHolder.btnDetail.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent orderDetail = new Intent(OrderStatus.this, OrderDetail.class);
                        Common.currentRequest = model;
                        orderDetail.putExtra("OrderId",adapter.getRef(position).getKey());
                        startActivity(orderDetail);
                    }
                });
            }
        };
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);

    }

    private void ShowUpdateDialog(String key, final Request item, final String validate, final String chk) {

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(OrderStatus.this);
        alertDialog.setTitle("Update Order");
        alertDialog.setMessage("Please choose status");
       // alertDialog.setIcon(R.drawable.ic_access_time_black_24dp);

        LayoutInflater inflater=this.getLayoutInflater();
        final View view = inflater.inflate(R.layout.update_order_layout, null);


         spinner= (MaterialSpinner) view.findViewById(R.id.statusSpinner);
         spinner.setItems("Cooked");


        alertDialog.setView(view);

        //spinner.setSelectedIndex(Integer.parseInt(item.getStatus()));

        final  String localKey = key;
        alertDialog.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                if (Integer.parseInt(validate)<(spinner.getSelectedIndex()+2)) {
                    if (Integer.parseInt(validate)==0 && chk.equals("false")) {
                        Toast.makeText(OrderStatus.this,"You Can't Cooke Before Payed",Toast.LENGTH_SHORT).show();
                    }else{

                        item.setStatus(String.valueOf(spinner.getSelectedIndex()+2));
                        requests.child(localKey).setValue(item);

                        adapter.notifyDataSetChanged();//add to update item size
                        sendOrderStatusToUser(localKey,item);
                    }
                }else{
                    Toast.makeText(OrderStatus.this,"You Can't Update from Current Statues to Previos",Toast.LENGTH_SHORT).show();
                }
            }

        });

        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        alertDialog.show();
    }

    private void sendOrderRequestToManager(String shipperPhone, Request item) {
        DatabaseReference tokens = db.getReference("Tokens");

        tokens.child(shipperPhone)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        /*for (DataSnapshot postSnapShot : dataSnapshot.getChildren()) {*/
                            Token serverToken = dataSnapshot.getValue(Token.class);

                            Map<String, String> content = new HashMap<>();
                            content.put("title","Kana Restaurant");
                            content.put("Message","Your Order need Driver");
                            DataMessage dataMessage = new DataMessage(serverToken.getToken(),content);

                            String test = new Gson().toJson(dataMessage);
                            Log.d("content ",test);

                            mService.sendNotification(dataMessage)
                                    .enqueue(new Callback<MyResponse>() {
                                        @Override
                                        public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {

                                            if (response.body().success == 1) {
                                                Toast.makeText(OrderStatus.this, "Sent to Shipper.", Toast.LENGTH_SHORT).show();
                                            } else {
                                                Toast.makeText(OrderStatus.this, "Failed to send notification !!!", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<MyResponse> call, Throwable t) {

                                            Log.e("ERROR ", t.getMessage());
                                        }
                                    });
                        }
                    /*}*/
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
    }

    private void sendOrderStatusToUser(final String key, final Request item) {

        DatabaseReference tokens = db.getReference("Tokens");
        tokens.child(item.getPhone())/*orderByKey().equalTo(item.getPhone())*/
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        /*for (DataSnapshot postSnapShot : dataSnapshot.getChildren()) {*/
                        if (dataSnapshot.exists())
                        {
                            Token serverToken = dataSnapshot.getValue(Token.class);

                            //create pay laoad
                            /*Notification notification = new Notification("MHD", " Your order " + key + " was updated");
                            Sender content = new Sender(serverToken.getToken(), notification);
*/
                            Map<String, String> content = new HashMap<>();
                            content.put("title","Kana Restaurant");
                            content.put("Message","Your Order need Driver");
                            DataMessage dataMessage = new DataMessage(serverToken.getToken(),content);

                            String test = new Gson().toJson(dataMessage);
                            Log.d("content ",test);

                            mService.sendNotification(dataMessage)
                                    .enqueue(new Callback<MyResponse>() {
                                        @Override
                                        public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                                            if (response.body()!=null) {
                                                if (response.body().success == 1) {
                                                    Toast.makeText(OrderStatus.this, "Order was Updated.", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(OrderStatus.this, "Order was updated bu Failed to send notification !!!", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        }

                                        @Override
                                        public void onFailure(Call<MyResponse> call, Throwable t) {

                                            Log.e("ERROR ", t.getMessage());
                                        }
                                    });
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) // Press Back Icon
        {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}
