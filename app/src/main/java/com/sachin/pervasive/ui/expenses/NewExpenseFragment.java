package com.sachin.pervasive.ui.expenses;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sachin.pervasive.R;
import com.sachin.pervasive.adapters.CategoriesSpinnerAdapter;
import com.sachin.pervasive.entities.Category;
import com.sachin.pervasive.entities.Expense;
import com.sachin.pervasive.interfaces.IExpensesType;
import com.sachin.pervasive.interfaces.IUserActionsMode;
import com.sachin.pervasive.utils.DialogManager;
import com.sachin.pervasive.utils.RealmManager;
import com.sachin.pervasive.utils.Util;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;

public class NewExpenseFragment extends DialogFragment implements View.OnClickListener{

    private TextView tvTitle;
    private Button btnDate;
    private ImageButton btnCamera;
    private Spinner spCategory;
    private EditText etDescription;
    private EditText etTotal;

    private CategoriesSpinnerAdapter mCategoriesSpinnerAdapter;
    private Date selectedDate;
    private Expense mExpense;

    private @IUserActionsMode int mUserActionMode;
    private @IExpensesType int mExpenseType;
    private StorageReference mStorageRef;
    private ProgressDialog mProgress;
    private File photoFile;
    Uri photoURI;
    private String Uid;

    private static final int CAMERA_REQUEST_CODE = 1;

    static NewExpenseFragment newInstance(@IUserActionsMode int mode, String expenseId) {
        NewExpenseFragment newExpenseFragment = new NewExpenseFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(IUserActionsMode.MODE_TAG, mode);
        if (expenseId != null) bundle.putString(ExpenseDetailFragment.EXPENSE_ID_KEY, expenseId);
        newExpenseFragment.setArguments(bundle);
        return newExpenseFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_dialog_new_expense, container, false);
        tvTitle = (TextView)rootView.findViewById(R.id.tv_title);
        btnDate = (Button)rootView.findViewById(R.id.btn_date);
        btnCamera = (ImageButton) rootView.findViewById(R.id.btn_image);
        spCategory = (Spinner)rootView.findViewById(R.id.sp_categories);
        etDescription = (EditText)rootView.findViewById(R.id.et_description);
        etTotal = (EditText)rootView.findViewById(R.id.et_total);
        mExpenseType = IExpensesType.MODE_EXPENSES;
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mProgress = new ProgressDialog(getActivity());
        Uid = UUID.randomUUID().toString();
        return rootView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (getArguments() != null) {
            mUserActionMode = getArguments().getInt(IUserActionsMode.MODE_TAG) == IUserActionsMode.MODE_CREATE ? IUserActionsMode.MODE_CREATE : IUserActionsMode.MODE_UPDATE;
        }
        setModeViews();
        btnDate.setOnClickListener(this);
        (getView().findViewById(R.id.btn_cancel)).setOnClickListener(this);
        (getView().findViewById(R.id.btn_save)).setOnClickListener(this);
        (getView().findViewById(R.id.btn_image)).setOnClickListener(this);
    }

    private void setModeViews() {
        List<Category> categoriesList = Category.getCategoriesExpense();
        Category[] categoriesArray = new Category[categoriesList.size()];
        categoriesArray = categoriesList.toArray(categoriesArray);
        mCategoriesSpinnerAdapter = new CategoriesSpinnerAdapter(getActivity(), categoriesArray);
        spCategory.setAdapter(mCategoriesSpinnerAdapter);
        switch (mUserActionMode) {
            case IUserActionsMode.MODE_CREATE:
                selectedDate = new Date();
                break;
            case IUserActionsMode.MODE_UPDATE:
                if (getArguments() != null) {
                    String id = getArguments().getString(ExpenseDetailFragment.EXPENSE_ID_KEY);
                    Uid = id;
                    mExpense = (Expense) RealmManager.getInstance().findById(Expense.class, id);
                    tvTitle.setText("Edit");
                    selectedDate = mExpense.getDate();
                    etDescription.setText(mExpense.getDescription());
                    etTotal.setText(String.valueOf(mExpense.getTotal()));
                    int categoryPosition = 0;
                    for (int i=0; i<categoriesArray.length; i++) {
                        if (categoriesArray[i].getId().equalsIgnoreCase(mExpense.getCategory().getId())) {
                            categoryPosition = i;
                            break;
                        }
                    }
                    spCategory.setSelection(categoryPosition);
                }
                break;
        }
        updateDate();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.btn_date) {
            showDateDialog();
        } else if (view.getId() == R.id.btn_cancel) {
            dismiss();
        } else if (view.getId() == R.id.btn_save) {
            onSaveExpense();
        }
        else if (view.getId() == R.id.btn_image) {
            uploadImage();
        }
    }

    private void onSaveExpense() {
        if (mCategoriesSpinnerAdapter.getCount() > 0 ) {
            if (!Util.isEmptyField(etTotal)) {
                Category currentCategory = (Category) spCategory.getSelectedItem();
                String total = etTotal.getText().toString();
                String description = etDescription.getText().toString();
                if (mUserActionMode == IUserActionsMode.MODE_CREATE) {
                    RealmManager.getInstance().save(new Expense(description, selectedDate, mExpenseType, currentCategory, Float.parseFloat(total), Uid), Expense.class);
                } else {
                    Expense editExpense = new Expense();
                    editExpense.setId(mExpense.getId());
                    editExpense.setTotal(Float.parseFloat(total));
                    editExpense.setDescription(description);
                    editExpense.setCategory(currentCategory);
                    editExpense.setDate(selectedDate);
                    RealmManager.getInstance().update(editExpense);
                }
                getTargetFragment().onActivityResult(getTargetRequestCode(), RESULT_OK, null);
                dismiss();
            } else {
                DialogManager.getInstance().showShortToast(getString(R.string.error_total));
            }
        } else {
            DialogManager.getInstance().showShortToast(getString(R.string.no_categories_error));
        }
    }

    private void showDateDialog() {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(selectedDate);
        DialogManager.getInstance().showDatePickerDialog(getActivity(), new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                calendar.set(year, month, day);
                selectedDate = calendar.getTime();
                updateDate();
            }
        }, calendar);
    }

    private void updateDate() {
        btnDate.setText(Util.formatDateToString(selectedDate, Util.getCurrentDateFormat()));
    }

    private void uploadImage() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            // Create the File where the photo should go
            photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File...
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(getActivity(),
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String mCurrentPhotoPath;
        // Create an image file name
        String imageFileName = "receipt";
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(
                storageDir,  /* prefix */
                imageFileName + ".jpg"     /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            mProgress.setMessage("Uploading image...");
            mProgress.show();

            StorageReference filepath = mStorageRef.child("Photos").child(Uid).child(photoURI.getLastPathSegment());

            filepath.putFile(photoURI).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>(){

                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    mProgress.dismiss();
                    Toast.makeText(getActivity(),"Success", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
