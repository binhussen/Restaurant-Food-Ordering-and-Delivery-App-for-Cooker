package com.group_7.mhd.cooker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.group_7.mhd.cooker.Common.Common;
import com.group_7.mhd.cooker.Interface.ItemClickListener;
import com.group_7.mhd.cooker.Model.Category;
import com.group_7.mhd.cooker.Model.Comment;
import com.group_7.mhd.cooker.Model.Order;
import com.group_7.mhd.cooker.Model.Token;
import com.group_7.mhd.cooker.ViewHolder.MenuViewHolder;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import dmax.dialog.SpotsDialog;
import io.paperdb.Paper;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, BottomNavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "FoodList";
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^"+
                    "(?=.*[a-z])" +     //at least one lowercase
                    "(?=.*[A-Z])" +     //at least one upercase
                    "(?=.*[0-9])" +     //at least one digit
                    "(?=.*[@#$%^&+=])" +    //at least one special character
                    "(?=\\S+$)" +          //no white space
                    ".{6,}" +               //at least six digit
                    "$");

    FirebaseDatabase database;
    DatabaseReference category;

    //Name in top of navigation drawer
    TextView txtFullName,textphone;
    //comment
    TextView textTotalPrice;
    EditText commentt,commentd;
    List<Order> cart = new ArrayList<>();
    DatabaseReference comments;


    RecyclerView recycler_menu;
    RecyclerView.LayoutManager layoutManager;

    FirebaseRecyclerAdapter<Category, MenuViewHolder> adapter;


    private FrameLayout frameLayout;

    private static final String NAV = "Home";
    BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Menu");
        setSupportActionBar(toolbar);

        //Init Firebase
        database = FirebaseDatabase.getInstance();
        category = database.getReference(Common.CATEGORY_TABLE);

        //int paper
        Paper.init(this);

        //comment
        comments = database.getReference(Common.COMMENT_TABLE);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        //Set Name for user
        View headerView = navigationView.getHeaderView(0);
        txtFullName = headerView.findViewById(R.id.text_fullName);
        txtFullName.setText(Common.currentChaf.getName().toString());

        textphone = headerView.findViewById(R.id.text_phone);
        textphone.setText(Common.currentChaf.getPhone().toString());

        //Load menu
        recycler_menu = findViewById(R.id.recycler_menu);
        /*recycler_menu.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recycler_menu.setLayoutManager(layoutManager);*/
        recycler_menu.setLayoutManager(new GridLayoutManager(this,2));

        if (Common.isConnectedToInternet(this))
            loadMenu();
        else {
            Toast.makeText(this, R.string.check, Toast.LENGTH_SHORT).show();
            return;
        }

        /*//Register Service
        Intent service=new Intent(Home.this, ListenOrder.class);
        startService(service);*/

        updateToken(FirebaseInstanceId.getInstance().getToken());

        /*frameLayout = findViewById(R.id.main_);*/
        /*setFragment(new HomeFragment());*/
    }

    private void updateToken(String token) {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference(Common.TOKEN_TABLE);
        Token data = new Token(token,false);
        tokens.child(Common.currentChaf.getPhone()).setValue(data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //ix click back buton from oo and dono see catagoy
        if (adapter!= null)
            adapter.startListening();
    }
    //
    private void loadMenu() {
        adapter = new
                FirebaseRecyclerAdapter<Category, MenuViewHolder>
                        (Category.class, R.layout.menu_item, MenuViewHolder.class, category) {
                    @Override
                    protected void populateViewHolder(MenuViewHolder viewHolder, Category model, int position) {
                        viewHolder.txtMenuName.setText(model.getName());
                        Picasso.get().load(model.getImage()).into(viewHolder.imageView);
                        final Category clickItem = model;
                        viewHolder.setItemClickListener(new ItemClickListener() {
                            @Override
                            public void onClick(View view, int position, boolean isLongClick) {
                                //Get CategoryId and send to FoodList Activity
                                Intent foodList = new Intent(Home.this, FoodList.class);
                                //Because CategoryId is key, so we just get key of this item
                                foodList.putExtra("CategoryId", adapter.getRef(position).getKey());
                                Log.d(TAG, "HomeActivity Value of Key: "+adapter.getRef(position).getKey());
                                Toast.makeText(Home.this, adapter.getRef(position).getKey(), Toast.LENGTH_SHORT).show();
                                startActivity(foodList);
                            }
                        });
                    }
                };
        recycler_menu.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_search) {
            startActivity(new Intent(Home.this, SearchActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_menu) {
            // Handle the camera action
        } else if (id == R.id.nav_order) {
            Intent orderIntent = new Intent(Home.this, OrderStatus.class);
            startActivity(orderIntent);

        } else if (id == R.id.nav_log_out) {
            //Delete Rmwmber user password
            Paper.book().destroy();
            //Logout
            Intent signIn = new Intent(Home.this, SignIn.class);
            signIn.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(signIn);
        } else if (id == R.id.nav_change_password){

            showChangePasswordDialog();
        } else if (id == R.id.nav_language) {
            showChangeLanguageDialog();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle(R.string.changep);
        alertDialog.setMessage(R.string.fill);


        alertDialog.setIcon(R.drawable.ic_security_black_24dp);

        LayoutInflater inflater = LayoutInflater.from(this);
        View layout_pwd = inflater.inflate(R.layout.change_password_layout,null);

        final MaterialEditText edtPassword = (MaterialEditText) layout_pwd.findViewById(R.id.edtPassword);
        final MaterialEditText edtNewPassword = (MaterialEditText) layout_pwd.findViewById(R.id.edtNewPassword);
        final MaterialEditText edtRepeatPassword = (MaterialEditText) layout_pwd.findViewById(R.id.edtRepeatPassword);

        alertDialog.setView(layout_pwd);

        //create Dialog and show
        final AlertDialog dialog = alertDialog.create();
        dialog.show();

        //Get AlertDialog from dialog
        final AlertDialog diagview = ((AlertDialog) dialog);
        Button ok = (Button) diagview.findViewById(R.id.ok);
        Button cancel = (Button) diagview.findViewById(R.id.cancel);

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validate()){
                    //change password here
                    //fore use spot dialog
                    final android.app.AlertDialog waitDialog = new SpotsDialog(Home.this);
                    waitDialog.show();

                    //check old password
                    if (edtPassword.getText().toString().equals(Common.currentChaf.getPassword()))
                    {
                        //check new password and repeat password
                        if (edtNewPassword.getText().toString().equals(edtRepeatPassword.getText().toString()))
                        {
                            Map<String,Object> passwordUpdate = new HashMap<>();
                            passwordUpdate.put("password",edtNewPassword.getText().toString());

                            Paper.book().write(Common.PWD_KEY,edtNewPassword.getText().toString());

                            //make update
                            DatabaseReference chafs = FirebaseDatabase.getInstance().getReference(Common.CHAFS_TABLE);
                            chafs.child(Common.currentChaf.getPhone())
                                    .updateChildren(passwordUpdate)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            waitDialog.dismiss();
                                            Toast.makeText(Home.this,R.string.passu,Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(Home.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                                        }
                                    });
                            dialog.dismiss();
                        }
                        else
                        {
                            Toast.makeText(Home.this,R.string.newpassd,Toast.LENGTH_SHORT).show();
                        }
                    }
                    else
                    {
                        waitDialog.dismiss();
                        Toast.makeText(Home.this,R.string.wropass,Toast.LENGTH_SHORT).show();
                    }
                }
            }

            private boolean validate() {
                boolean valid = true;

                String oldpass = edtPassword.getText().toString().trim();
                String newpass = edtNewPassword.getText().toString().trim();
                String reppass = edtRepeatPassword.getText().toString().trim();

                if (!PASSWORD_PATTERN.matcher(oldpass).matches()){
                    edtPassword.setError(getString(R.string.wrongpass));
                    valid = false;
                }if (!PASSWORD_PATTERN.matcher(newpass).matches()){
                    edtNewPassword.setError(getString(R.string.err_password));
                    valid = false;
                }if (!reppass.equals(newpass)){
                    edtRepeatPassword.setError(getString(R.string.err_password_confirmation));
                    valid = false;
                }

                return valid;
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

      /* //nav home
     private void setFragment(Fragment fragment){
         FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
         fragmentTransaction.replace(frameLayout.getId(),fragment);
     }*/
    //langs
    private void showChangeLanguageDialog() {
        final String[] listItems = {"English","አማርኛ"};
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(Home.this);
        builder.setTitle(R.string.choosel);
        builder.setSingleChoiceItems(listItems, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(i==0){
                    setLocale("en");
                    recreate();
                }
                else if(i==1){
                    setLocale("am");
                    recreate();
                }
                dialogInterface.dismiss();
            }
        });
        android.support.v7.app.AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
    private void setLocale(String langs) {
        Locale locale = new Locale(langs);
        locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,getBaseContext().getResources().getDisplayMetrics());

        SharedPreferences.Editor editor = getSharedPreferences("Settings",MODE_PRIVATE).edit();
        editor.putString("My_Lang",langs);
        editor.apply();
    }
    public void loadLocale(){
        SharedPreferences prefs = getSharedPreferences("Settings", Activity.MODE_PRIVATE);
        String language = prefs.getString("My_Lang","");
        setLocale(language);
    }
}

























