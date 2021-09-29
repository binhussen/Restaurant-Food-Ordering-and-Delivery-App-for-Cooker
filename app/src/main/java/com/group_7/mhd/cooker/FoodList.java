package com.group_7.mhd.cooker;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.group_7.mhd.cooker.Common.Common;
import com.group_7.mhd.cooker.Interface.ItemClickListener;
import com.group_7.mhd.cooker.Model.Category;
import com.group_7.mhd.cooker.Model.Food;
import com.group_7.mhd.cooker.ViewHolder.FoodViewHolder;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FoodList extends AppCompatActivity {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    LinearLayout rootLayout;

    FloatingActionButton fab;

    //Firebase
    FirebaseDatabase db;
    DatabaseReference foodList, category;
    FirebaseStorage storage;
    StorageReference storageReference;

    String categoryId = "";

    FirebaseRecyclerAdapter<Food, FoodViewHolder> adapter;

    FirebaseRecyclerAdapter<Food, FoodViewHolder> searchadapter;
    List<String> suggestList = new ArrayList<>();
    MaterialSearchBar materialSearchBar;
    //Add new food
    EditText editName, editDescription, editPrice, editDiscount;
    Button btnSelect, btnUpload;

    Food newFood;

    Uri saveUri;

    TextView cat_name;
    ImageView cat_image;
    Category currentCategory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_list);

        //Firebase
        db = FirebaseDatabase.getInstance();
        foodList = db.getReference(Common.FOOD_TABLE);
        category = db.getReference(Common.CATEGORY_TABLE);

        cat_name = findViewById(R.id.cat_name);
        cat_image = findViewById(R.id.cat_image);

        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        //Init
        recyclerView = findViewById(R.id.recycler_food);
        /*recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);*/
        recyclerView.setLayoutManager(new GridLayoutManager(this,2));

        rootLayout = findViewById(R.id.root_Layout);


        if (getIntent() != null) {
            categoryId = getIntent().getStringExtra("CategoryId");
        }
        if (!categoryId.isEmpty()) {
            loadListFood(categoryId);
            getCategoryDetail(categoryId);
        }

        //search
        materialSearchBar = (MaterialSearchBar)findViewById(R.id.searchBar);
        materialSearchBar.setHint("Enter your food");
        //materialSearchBar.setSpeechMode(false);

        LoadSuggest();
        materialSearchBar.setLastSuggestions(suggestList);
        // materialSearchBar =(MaterialSearchBar) findViewById(R.id.searchBar);
        materialSearchBar.setCardViewElevation(10);
        materialSearchBar.addTextChangeListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                List<String> suggest = new ArrayList<>();
                for(String search:suggestList){

                    if(search.toLowerCase().contains(materialSearchBar.getText().toLowerCase()))
                        suggest.add(search);

                }
                materialSearchBar.setLastSuggestions(suggest);

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        materialSearchBar.setOnSearchActionListener(new MaterialSearchBar.OnSearchActionListener() {
            @Override
            public void onSearchStateChanged(boolean enabled) {

                if(!enabled)
                    recyclerView.setAdapter(adapter);
            }

            @Override
            public void onSearchConfirmed(CharSequence text) {

                startSearch(text);
            }

            private void startSearch(CharSequence text) {

                searchadapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(
                        Food.class,
                        R.layout.food_item,
                        FoodViewHolder.class,
                        foodList.orderByChild("name").equalTo(text.toString())
                ) {
                    @Override
                    protected void populateViewHolder(FoodViewHolder viewHolder, Food model, int position) {

                        viewHolder.foodName.setText(model.getName());
                        Picasso.get().load(model.getImage()).into(viewHolder.foodImage);

                        final Food local = model;
                        viewHolder.setItemClickListener(new ItemClickListener() {
                            @Override
                            public void onClick(View view, int position, boolean isLongClick) {
                                //Start new Activity
                                //Intent foodDetail = new Intent(FoodList.this, FoodDetails.class);
                                //Save food id to activity
                                // foodDetail.putExtra("FoodId", searchadapter.getRef(position).getKey());
                                // startActivity(foodDetail);
                            }
                        });
                    }
                };

                recyclerView.setAdapter(searchadapter);
            }

            @Override
            public void onButtonClicked(int buttonCode) {

            }
        });

    }

    private void LoadSuggest() {
        foodList.orderByChild("menuId").equalTo(categoryId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for(DataSnapshot postSnapshot : dataSnapshot.getChildren())
                        {
                            Food item = postSnapshot.getValue(Food.class);
                            //assert item != null;
                            suggestList.add(item.getName());
                        }
                        materialSearchBar.setLastSuggestions(suggestList);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    //loadListFood() method
    private void loadListFood(String categoryId) {
        adapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(
                Food.class,
                R.layout.food_item,
                FoodViewHolder.class,
                foodList.orderByChild("menuId").equalTo(categoryId)
        ) {
            @Override
            protected void populateViewHolder(FoodViewHolder viewHolder, Food model,final int position) {
                viewHolder.foodName.setText(model.getName());
                Picasso.get().load(model.getImage()).into(viewHolder.foodImage);

                viewHolder.setItemClickListener(new ItemClickListener() {
                    @Override
                    public void onClick(View view, int position, boolean isLongClick) {
                        //Code late
                    }
                });
            }
        };
        adapter.notifyDataSetChanged();
        recyclerView.setAdapter(adapter);
    }



    //Method for delete and update food item
    //Press Ctrl+o
    @Override
    public boolean onContextItemSelected(MenuItem item) {

        return super.onContextItemSelected(item);
    }

    // getCatagory detail() method
    private void getCategoryDetail(String categoryId) {
        category.child(categoryId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                currentCategory = dataSnapshot.getValue(Category.class);

                Picasso.get().load(currentCategory.getImage()).into(cat_image);
                cat_name.setText(currentCategory.getName());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}