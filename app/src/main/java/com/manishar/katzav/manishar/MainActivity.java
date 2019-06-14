package com.manishar.katzav.manishar;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesResponse;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity {

    private Sheets service;
    Spinner spinner_category;
    GoogleAccountCredential mCredential;
    private Button btnSubmit;
    private EditText editText_amount, editText_comments;
    private String spreadsheetId = "1DKNC8Rsd7Wqav59wblqbCK6WU9IDdnHQm7qOjhgJxy4"; //Ohads ID
    //private String spreadsheetId = "10bcEclCNRGLlorphOJKyJCUaJTsKlmF5YAFhkp-sQMw"; //Adams ID
    private static final String PREF_ACCOUNT_NAME = "accountName";
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private final static String DATE_COLUMN_STR = "A";
    private final static String CATEGORY_COLUMN_STR = "D";
    private final static String AMOUNT_COLUMN_STR = "E";
    private final static String COMMENT_COLUMN_STR = "F";
    private final static String ROW_DATA_SHEET = "'נתונים'!";
    static String row_str = "";
    static int row_int = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editText_amount = findViewById(R.id.editText_amount);
        editText_comments = findViewById(R.id.editText_comments);
        addListenerOnButton();
        addListenerOnSpinnerItemSelection();

        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SheetsScopes.SPREADSHEETS))
                .setBackOff(new ExponentialBackOff());

        getAccount();
    }

    private void getAviloableRaw(ValueRange result) {
        String row = parseSingleCellData(result);
        if (!row.isEmpty()) {
            this.row_str = row;
            this.row_int = Integer.parseInt(row_str);
        } else { /* We dont have avilable row --> pop msg*/

        }
    }

    private void increaseRow() {
        this.row_int++;
        this.row_str = String.valueOf(this.row_int);
    }


    private class writeToSheets extends AsyncTask<String, Void, BatchUpdateValuesResponse> {
        String method;

        writeToSheets(String method_call) {this.method = method_call;}

        /* params is array of strings that holds the parameters
         * params[0]: range
         * params[1] ... params[n]: write data */
        @Override
        public BatchUpdateValuesResponse doInBackground(String... params) {
            BatchUpdateValuesResponse result = null;
            if (!params[1].isEmpty()) {
                service = getSheetsService();
                Date c = Calendar.getInstance().getTime();
                SimpleDateFormat curFormater = new SimpleDateFormat("dd/MM/yyyy");
                String strDate = curFormater.format(c);
                Object post_date[] = new String[1];
                post_date[0]=strDate;

                List<List<Object>> date = Arrays.asList(
                        Arrays.asList(post_date)               // Additional rows ...
                );

                String dateRange =  ROW_DATA_SHEET + DATE_COLUMN_STR +
                        row_str +":" + DATE_COLUMN_STR + row_str;

                String amountRange =  ROW_DATA_SHEET + AMOUNT_COLUMN_STR +
                        row_str +":" + AMOUNT_COLUMN_STR + row_str;


                Object post_vals[] = new String[params.length - 1];
                int insertion_index = 0;
                for (int param_index = 1; param_index<params.length; param_index++) {
                    post_vals[insertion_index++] = params[param_index];
                }

                List<List<Object>> values = Arrays.asList(
                        Arrays.asList(post_vals)               // Additional rows ...
                );

                List<ValueRange> data = new ArrayList<ValueRange>();
                data.add(new ValueRange()
                        .setRange(params[0])
                        .setValues(values));

                data.add(new ValueRange()
                        .setRange(dateRange)
                        .setValues(date));

                /* Here, we are rewrote the amount column since we need to convert it to
                 * integer. So We add one more write that will rewrite the coulumn */
                Object post_amount[] = new Integer[1];
                post_amount[0]=Integer.parseInt(params[2]);

                List<List<Object>> amount = Arrays.asList(
                        Arrays.asList(post_amount)               // Additional rows ...
                );

                data.add(new ValueRange()
                        .setRange(amountRange)
                        .setValues(amount));

                BatchUpdateValuesRequest body = new BatchUpdateValuesRequest()
                        .setValueInputOption("RAW")
                        .setData(data);
                try {
                    result = service.spreadsheets().values().batchUpdate(spreadsheetId, body)
                            .execute();
                } catch (UserRecoverableAuthIOException e) {
                    startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
                } catch (IOException e) {
                    Log.e("SHEETS", "Error writing cell");
                    e.printStackTrace();
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(BatchUpdateValuesResponse result) {
            writeSheetsAction_cb(method, result);
        }
    }


    private class readFromSheets extends AsyncTask<String, Void, ValueRange> {
        String method;
        readFromSheets(String method_call) {this.method = method_call;}

        /* params is array of strings that holds the parameters
         * params[0]: range */
        @Override
        public ValueRange doInBackground(String... params) {
            ValueRange result = null;
            service = getSheetsService();
            try {
                result = service.spreadsheets().values().get(spreadsheetId, params[0]).execute();
            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                Log.e("SHEETS", "Error reading cell");
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(ValueRange result) {
            readSheetsAction_cb(method, result);
        }
    }

    private void readSheetsAction_cb(String method, ValueRange result) {
        try {
            Class[] args = new Class[1];
            args[0] = ValueRange.class;
            MainActivity.class.getDeclaredMethod(method, args).invoke(this, result);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            System.out.format("Invocation of %s failed because of: %s%n", method, cause.getMessage());
            e.printStackTrace();
        }
    }

    private void writeSheetsAction_cb(String method, BatchUpdateValuesResponse result) {
        try {
            Class[] args = new Class[1];
            args[0] = BatchUpdateValuesResponse.class;
            MainActivity.class.getDeclaredMethod(method, args).invoke(this, result);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            System.out.format("Invocation of %s failed because of: %s%n", method, cause.getMessage());
            e.printStackTrace();
        }
    }


    private void populateCategories(ValueRange result) {
        String[] categories = parseMultipleCellData(result);
        initCategorySpinner(categories);
    }

    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                //getAccount();
                if (mCredential.getSelectedAccount() == null) {
                    // Start a dialog from which the user can choose an account
                    startActivityForResult(
                            mCredential.newChooseAccountIntent(),
                            REQUEST_ACCOUNT_PICKER);
                }
                new readFromSheets("populateCategories").execute("'data'!L2:L39");
                new readFromSheets("getAviloableRaw").execute("'data'!B45:B45");
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account.",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private void getAccount() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else {
            new readFromSheets("populateCategories").execute("'data'!L2:L39");
            new readFromSheets("getAviloableRaw").execute("'data'!B45:B45");
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(this,"This app requires Google Play Services. Please install Google Play Services on your device and relaunch this app.", Toast.LENGTH_LONG);
                } else {
                    getAccount();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getAccount();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getAccount();
                }
                break;

        }
    }


    private Sheets getSheetsService() {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        return new Sheets.Builder(transport, jsonFactory, mCredential)
                .setApplicationName("MaNishar")
                .build();
    }

    // add items into spinner dynamically
    public void initCategorySpinner(String[] items) {
        //get the spinner from the xml.
        spinner_category = findViewById(R.id.spinner_category);

        //Get the list of categories from sheets
        //new runOnSheets("Single").execute("'data'!L2:L20");

        //create an adapter to describe how the items are displayed, adapters are used in several places in android.
        //There are multiple variations of this, but this is the basic variant.
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

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

                String comment = String.valueOf(editText_comments.getText());
                String category = String.valueOf(spinner_category.getSelectedItem());
                String amount = String.valueOf(editText_amount.getText());

                /* Validate the amount and category are not empty
                 * and post to sheets
                  * The parameters order is very importent since
                  * the sheets columns are fixed - category, amount, comment */
                if (!category.isEmpty() && !amount.isEmpty() ) {
                    String rangeToWrite =  ROW_DATA_SHEET + CATEGORY_COLUMN_STR +
                                            row_str +":" + COMMENT_COLUMN_STR + row_str;
                    new writeToSheets("writeExpanseDone").execute(rangeToWrite, category, amount, comment);

                    hideKeyboard();

                    Toast.makeText(MainActivity.this,
                            "קטגוריה : " + category + "\nסכום : " + amount,
                            Toast.LENGTH_SHORT).show();
                } else { /* Empty values */

                }
            }

        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText_comments.getWindowToken(), 0);
    }

    private void writeExpanseDone(BatchUpdateValuesResponse result) {
        /* While we update expanse we update 4 columns for
         category, amount, comments and date */
        if (result.getTotalUpdatedColumns() == 4) { /* success */
            editText_amount.getText().clear();
            editText_comments.getText().clear();
            increaseRow();
            Toast.makeText(MainActivity.this,"הוצאה הוזנה בהצלחה",Toast.LENGTH_SHORT).show();
        } else { /* failed */
            Toast.makeText(MainActivity.this,"הוצאה לא הוזנה",Toast.LENGTH_SHORT).show();
        }
    }

    /* Get the value from the ValueRange object
     * {
            "range": string,
            "majorDimension": enum(Dimension),
            "values": [
            array
            ]
        }
    */
    public String parseSingleCellData(ValueRange range) {
        return String.valueOf(range.getValues().get(0).get(0));
    }

    public String[] parseMultipleCellData(ValueRange range) {
        List<List<Object>> objectArray = range.getValues();
        ArrayList<String> categories = new ArrayList<>();
        for (List cur_row: objectArray) {
            if (cur_row.size() > 0) {
                String a = (String) cur_row.get(0);
                categories.add(a);
            }
        }
        return categories.toArray(new String[categories.size()]);
    }

}
