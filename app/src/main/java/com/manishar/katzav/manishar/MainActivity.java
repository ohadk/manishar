package com.manishar.katzav.manishar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private Sheets service;
    Spinner spinner_category;
    private Button btnSubmit;
    private EditText editText_amount, editText_comments;
    private String spreadsheetId = "1DKNC8Rsd7Wqav59wblqbCK6WU9IDdnHQm7qOjhgJxy4";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText_amount = findViewById(R.id.editText_amount);
        editText_comments = findViewById(R.id.editText_comments);
        initCategorySpinner();
        addListenerOnButton();
        addListenerOnSpinnerItemSelection();

        String range = "B45:B45";
        ValueRange result;
        try {
            result = service.spreadsheets().values().get(spreadsheetId, range).execute();
            int numRows = result.getValues() != null ? result.getValues().size() : 0;
            System.out.printf("%d rows retrieved.", numRows);
        } catch (IOException e) {
            System.out.printf("Error reading cell");
            e.printStackTrace();
        }

    }

    // add items into spinner dynamically
    public void initCategorySpinner() {
        //get the spinner from the xml.
        spinner_category = findViewById(R.id.spinner_category);
        //create a list of items for the spinner.
        String[] items = new String[]{"1", "2", "three"};
        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        //set the spinners adapter to the previously created one.
        spinner_category.setAdapter(adapter);

        addListenerOnSpinnerItemSelection();
    }

    public void addListenerOnSpinnerItemSelection() {
        spinner_category = (Spinner) findViewById(R.id.spinner_category);
        spinner_category.setOnItemSelectedListener(new CustomOnItemSelectedListener());
    }

    // get the selected dropdown list value
    public void addListenerOnButton() {

        spinner_category = (Spinner) findViewById(R.id.spinner_category);
        btnSubmit = (Button) findViewById(R.id.button_done);

        btnSubmit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                Toast.makeText(MainActivity.this,
                        "קטגוריה : " + String.valueOf(spinner_category.getSelectedItem()) +
                        "\nסכום : " + String.valueOf(editText_amount.getText()),
                        Toast.LENGTH_SHORT).show();

                /* Post to sheets */
            }

        });
    }



    public ValueRange getValues(String spreadsheetId, String range) throws IOException {
        Sheets service = this.service;
        // [START sheets_get_values]
        ValueRange result = service.spreadsheets().values().get(spreadsheetId, range).execute();
        int numRows = result.getValues() != null ? result.getValues().size() : 0;
        System.out.printf("%d rows retrieved.", numRows);
        // [END sheets_get_values]
        return result;
    }
}
