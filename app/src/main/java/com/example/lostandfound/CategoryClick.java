package com.example.lostandfound;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CategoryClick extends AppCompatActivity {

    private TextView itemsTab, personPetTab;
    private GridLayout categoryGrid, personGrid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_category);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.category), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        itemsTab = findViewById(R.id.items_tab);
        personPetTab = findViewById(R.id.person_pet_tab);
        categoryGrid = findViewById(R.id.category_grid);
        personGrid = findViewById(R.id.person_grid);

        selectItemsTab();

        itemsTab.setOnClickListener(v -> selectItemsTab());
        personPetTab.setOnClickListener(v -> selectPersonTab());
    }

    private void selectItemsTab() {
        itemsTab.setBackgroundColor(Color.WHITE);
        personPetTab.setBackgroundColor(Color.parseColor("#D9D9D9"));
        categoryGrid.setVisibility(View.VISIBLE);
        personGrid.setVisibility(View.GONE);
    }

    private void selectPersonTab() {
        itemsTab.setBackgroundColor(Color.parseColor("#D9D9D9"));
        personPetTab.setBackgroundColor(Color.WHITE);
        categoryGrid.setVisibility(View.GONE);
        personGrid.setVisibility(View.VISIBLE);
    }

    public void onCategoryClick(View view) {
        String tag = (String) view.getTag();
        if (tag == null) return;

        boolean editable = tag.equalsIgnoreCase("Other") || tag.equalsIgnoreCase("Others");

        Intent intent = new Intent(CategoryClick.this, PostActivity.class);
        intent.putExtra("categoryTag", tag);
        intent.putExtra("isEditable", editable);
        startActivity(intent);
    }


}
