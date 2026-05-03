package com.lums.eventhub;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * AddVendorActivity.java
 *
 * CHANGES:
 *   - Phone and Email are now REQUIRED fields (validated before save)
 *
 * Organiser fills in details for a new vendor and adds it to the
 * vendors/ Firestore collection.
 *
 * Fields:
 *   Name (required), Phone (required), Email (required),
 *   Category (spinner), About, Address,
 *   Logo image (optional — JPEG/PNG, Base64 encoded)
 *
 * On Save → writes to vendors/ and returns RESULT_OK to VendorDirectoryActivity.
 */
public class AddVendorActivity extends AppCompatActivity {

    private static final int IMAGE_PICK_RC = 400;

    private EditText  etName, etAbout, etPhone, etEmail, etAddress;
    private Spinner   spinnerCategory;
    private Button    btnPickLogo, btnSaveVendor, btnCancelVendor;
    private ImageView imgLogoPreview;

    private String logoBase64 = "";
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_vendor);

        db = FirebaseFirestore.getInstance();

        etName          = findViewById(R.id.etVendorName);
        etAbout         = findViewById(R.id.etVendorAbout);
        etPhone         = findViewById(R.id.etVendorPhone);
        etEmail         = findViewById(R.id.etVendorEmail);
        etAddress       = findViewById(R.id.etVendorAddress);
        spinnerCategory = findViewById(R.id.spinnerVendorCategory);
        btnPickLogo     = findViewById(R.id.btnPickLogo);
        btnSaveVendor   = findViewById(R.id.btnSaveVendor);
        btnCancelVendor = findViewById(R.id.btnCancelVendor);
        imgLogoPreview  = findViewById(R.id.imgLogoPreview);

        // Category spinner
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Catering", "AV & Tech", "Printing", "Decor", "Transport", "Other"});
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);

        btnPickLogo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(
                    Intent.createChooser(intent, "Select Vendor Logo"), IMAGE_PICK_RC);
        });

        btnSaveVendor.setOnClickListener(v -> saveVendor());
        btnCancelVendor.setOnClickListener(v -> finish());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_RC && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri uri = data.getData();
            logoBase64 = encodeImage(uri);
            if (logoBase64 != null) {
                byte[] bytes = Base64.decode(logoBase64, Base64.NO_WRAP);
                Bitmap bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                imgLogoPreview.setImageBitmap(bmp);
                imgLogoPreview.setVisibility(android.view.View.VISIBLE);
                btnPickLogo.setText("✅ Logo selected");
            }
        }
    }

    private void saveVendor() {
        // --- Validation ---
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("Vendor name is required");
            etName.requestFocus();
            return;
        }

        String phone = etPhone.getText().toString().trim();
        if (phone.isEmpty()) {
            etPhone.setError("Phone number is required");
            etPhone.requestFocus();
            return;
        }

        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        // --- Collect remaining optional fields ---
        String category = spinnerCategory.getSelectedItem().toString();
        String about    = etAbout.getText().toString().trim();
        String address  = etAddress.getText().toString().trim();

        Map<String, Object> vendor = new HashMap<>();
        vendor.put("name",         name);
        vendor.put("category",     category);
        vendor.put("about",        about);
        vendor.put("phone",        phone);
        vendor.put("email",        email);
        vendor.put("address",      address);
        vendor.put("logoBase64",   logoBase64 != null ? logoBase64 : "");
        vendor.put("rating",       0.0);
        vendor.put("usedByCount",  0L);
        vendor.put("usageHistory", new ArrayList<>());
        vendor.put("createdAt",    System.currentTimeMillis());

        btnSaveVendor.setEnabled(false);
        btnSaveVendor.setText("Saving...");

        db.collection("vendors").add(vendor)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, name + " added to vendor directory!",
                            Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    btnSaveVendor.setEnabled(true);
                    btnSaveVendor.setText("Save Vendor");
                });
    }

    private String encodeImage(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) return null;
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            if (bmp == null) return null;
            int maxPx = 400, w = bmp.getWidth(), h = bmp.getHeight();
            if (w > maxPx || h > maxPx) {
                float s = Math.min((float) maxPx / w, (float) maxPx / h);
                bmp = Bitmap.createScaledBitmap(bmp, Math.round(w * s), Math.round(h * s), true);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 75, baos);
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) { return null; }
    }
}