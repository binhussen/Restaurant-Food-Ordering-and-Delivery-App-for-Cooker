package com.group_7.mhd.cooker;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.group_7.mhd.cooker.Common.Common;
import com.group_7.mhd.cooker.Model.Chaf;

import io.netopen.hotbitmapgg.library.view.RingProgressBar;
import io.paperdb.Paper;

public class MainActivity extends AppCompatActivity {

    //setting up a finite time for screen delay
    private static int splash_time_Out=2000;
    private Context context;

    RingProgressBar ringProgressBar;
    int progress=0;
    Handler myhandler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==0)
            {
                if(progress<100)
                {
                    progress+=5;
                    ringProgressBar.setProgress(progress);
                }
            }
        }
    };

    SharedPreferences sharedPreferences;
    Boolean save_login,skip;
    SharedPreferences.Editor edit;
    String type,name,resname;

    FirebaseDatabase database;
    DatabaseReference table_user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        //init paper
        Paper.init(this);

        //init Firebase
        database = FirebaseDatabase.getInstance();
        table_user = database.getReference(Common.CHAFS_TABLE);

        ringProgressBar = (RingProgressBar) findViewById(R.id.ringProgress);
        ringProgressBar.setOnProgressListener(new RingProgressBar.OnProgressListener() {
            @Override
            public void progressToComplete() {
                okDone();
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 100; i++) {
                    try {
                        Thread.sleep(100);
                        myhandler.sendEmptyMessage(0);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void signInUser(final String user, final String pwd) {
        if (Common.isConnectedToInternet(getBaseContext())) {
            //save user and password

            final ProgressDialog mDialog = new ProgressDialog(MainActivity.this);
            mDialog.setMessage("Please wait...");
            mDialog.show();



            table_user.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {


                    if (dataSnapshot.child(user).exists()) {
                        //Get user information


                        mDialog.dismiss();
                        Chaf chaf = dataSnapshot.child(user).getValue(Chaf.class);
                        chaf.setPhone(user);//set phone
                        if (chaf.getPassword().equals(pwd)) {

                            Toast.makeText(MainActivity.this, R.string.signeucc, Toast.LENGTH_SHORT).show();
                            Intent homeIntent = new Intent(MainActivity.this, Home.class);
                            Common.currentChaf = chaf;
                            startActivity(homeIntent);
                            finish();

                            //delete lisnr=ed data
                            table_user.removeEventListener(this);

                        } else {
                            mDialog.dismiss();
                            Toast.makeText(MainActivity.this, R.string.wropass, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        mDialog.dismiss();
                        Toast.makeText(MainActivity.this, R.string.userd, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
        else {
            Toast.makeText(MainActivity.this, R.string.check, Toast.LENGTH_SHORT).show();
            return;
        }
    }

    public void okDone(){
        if (!isNetworkAvilabe()) {
            //Creating an Alertdialog
            AlertDialog.Builder CheckBuild = new AlertDialog.Builder(MainActivity.this);
            CheckBuild.setIcon(R.drawable.no);
            CheckBuild.setTitle("Error!");
            CheckBuild.setMessage("Check Your Internet Connection");

            //Builder Retry Button

            CheckBuild.setPositiveButton("Skip", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int id) {
                    //creating intent and going to the home activity
                    Intent newintent = new Intent(MainActivity.this, SignIn.class);
                    //starting the activity
                    startActivity(newintent);

//                    Intent intent = new Intent(context, First_Activity.class);
//                    context.startActivity(intent);
                    //when intent is start and go to home class then main activity will finish
                    finish();
                }

            });
            /*CheckBuild.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int id) {
                    //Restart The Activity
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }

            });*/
            CheckBuild.setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    //Exit The Activity
                    finish();
                }

            });
            AlertDialog alertDialog = CheckBuild.create();
            alertDialog.show();
        } else {
            //login automatcally
            String user = Paper.book().read(Common.USER_KEY);
            String pwd = Paper.book().read(Common.PWD_KEY);
            if (user != null && pwd != null){
                if (!user.isEmpty() && !pwd.isEmpty()){
                    signInUser(user,pwd);
                }
            }else {
                //creating intent and going to the home activity
                Intent newintent = new Intent(MainActivity.this, SignIn.class);
                //starting the activity
                startActivity(newintent);
                //when intent is start and go to home class then main activity will finish
                finish();
            }
        }
    }
    private boolean isNetworkAvilabe()
    {
        ConnectivityManager connectivityManager = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}