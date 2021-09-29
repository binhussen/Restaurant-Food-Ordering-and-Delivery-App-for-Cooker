package com.group_7.mhd.cooker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.group_7.mhd.cooker.Common.Common;
import com.group_7.mhd.cooker.Interface.ItemClickListener;
import com.group_7.mhd.cooker.Model.Food;
import com.group_7.mhd.cooker.ViewHolder.FoodViewHolder;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    //Firebase RecyclerAdapter
    FirebaseRecyclerAdapter<Food, FoodViewHolder> adapter;
    //search functionality
    FirebaseRecyclerAdapter<Food, FoodViewHolder> searchadapter;
    List<String> suggestList = new ArrayList<>();
    MaterialSearchBar materialSearchBar;

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    FirebaseDatabase database;
    DatabaseReference foodList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);


        //Firebase
        database = FirebaseDatabase.getInstance();
        foodList = database.getReference(Common.FOOD_TABLE);

        recyclerView = findViewById(R.id.recycler_search);
        //recyclerView.setHasFixedSize(true);
        /*layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);*/
        recyclerView.setLayoutManager(new GridLayoutManager(this,2));

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

            @Override
            public void onButtonClicked(int buttonCode) {

            }
        });

        //load all food
        loadAllFoods();
    }

    private void loadAllFoods() {
        adapter = new FirebaseRecyclerAdapter<Food, FoodViewHolder>(Food.class,
                R.layout.food_item,
                FoodViewHolder.class,
                foodList ////Like : Select * from Food where MenuId =
        ){
            @Override
            protected void populateViewHolder(FoodViewHolder viewHolder, final Food model, final int position) {
                viewHolder.foodName.setText(model.getName());
                Picasso.get().load(model.getImage()).into(viewHolder.foodImage);

            }
        };

        //Set Adapter
        /*Log.d(TAG, "loaded List Food: "+adapter.getItemCount());
        Toast.makeText(this, "loaded List Food: "+adapter.getItemCount(), Toast.LENGTH_SHORT).show();*/
        recyclerView.setAdapter(adapter);
    }

    private void LoadSuggest() {
        foodList.addListenerForSingleValueEvent(new ValueEventListener() {
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

            }
        };

        recyclerView.setAdapter(searchadapter);
    }
}
