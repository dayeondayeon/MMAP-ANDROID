package org.techtown.memory_map;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.media.MediaActionSound;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.MODE_PRIVATE;
import android.content.SharedPreferences;

public class EditView extends Fragment {

    private ImageView edit_image;
    private Context context;
    private static final int GALLERY_REQUEST_CODE = 301;
    private static final int REQUEST_EXTERNAL_STORAGE_PERMISSION = 401;

    private ServiceApi serviceApi;

    Button Date_edit;
    Date Selected_date;
    EditText address_input;
    TextView address_result;
    Button save_button;
    EditText text_input;
    String token;
    int userIdx;

    public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy년 MM월 dd일");

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_editview, container, false);

        Date_edit = rootView.findViewById(R.id.date_edit);
        Date_edit.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                showDateDialog();
            }
        });

        Date curDate = new Date();
        setSelectedDate(curDate);

        context = container.getContext();
        serviceApi = RetrofitClient.getClient().create(ServiceApi.class);

        //sharedPreference 해결해야함
        //제발
        //제발
        SharedPreferences sharedPreferences = context.getSharedPreferences("login", MODE_PRIVATE);
        token = sharedPreferences.getString("token","");
        userIdx = sharedPreferences.getInt("userIdx", -1);
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {

            } else {
                ActivityCompat.requestPermissions(getActivity(), new String [] {Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE_PERMISSION);
            }
        } else {

        }

        edit_image = rootView.findViewById(R.id.edit_Image);

        edit_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(intent, GALLERY_REQUEST_CODE);
            }
        });

        text_input =rootView.findViewById(R.id.text_input);
        address_input = rootView.findViewById(R.id.location_input);
        save_button = rootView.findViewById(R.id.edit_saveButton);
        address_result = rootView.findViewById(R.id.location_result);
        final Geocoder geocoder = new Geocoder(this.getContext());

        save_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Address> list = null;
                List<Address> citylist = null;

                String str = address_input.getText().toString();
                try {
                    list = geocoder.getFromLocationName
                            (str,
                                    10);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("test","입출력 오류 - 서버에서 주소변환시 에러발생");
                }

                if (list != null) {
                    String city = "";
                    if (list.size() == 0) {
                        address_result.setText("올바른 주소를 입력해주세요. ");
                    } else {
                        Address address = list.get(0);
                        double lat = address.getLatitude();
                        double lon = address.getLongitude();

                        try{
                            citylist = geocoder.getFromLocation(
                                    lat,
                                    lon,
                                    10
                            );
                        }catch(IOException e){
                            e.printStackTrace();
                        }
                        if(citylist != null) {
                            if(list.size() == 0){
                                Log.e("reverseGeocoding", "해당 도시 없음");
                            }else{
                                city = citylist.get(0).toString();
                                Toast.makeText(context, city, Toast.LENGTH_LONG).show();
                            }
                        }
                        String sss = String.format("위도 : %f, 경도 : %f", lat, lon);
                        address_result.setText(sss);
                        StartEdit(new EditData(city, city, text_input.getText().toString(), lat, lon, userIdx));
                        // 위에 임시로 city city넣었는데 역지오코딩 결과 보고 몇번째 인덱스가 국가고 도시인지
                        //찾아가지고 첫번째 인자에 도시, 두번째 인자에 국가로 수정(이건 테스트해보고 바꿔야함)
                        //userIdx 받는 방법좀...ㅠㅠ
                    }
                }
            }
        });


        return rootView;
    }

    private void StartEdit(EditData editData) {
        serviceApi.userEdit(token, editData).enqueue(new Callback<EditResponse>() {
            @Override
            public void onResponse(Call<EditResponse> call, Response<EditResponse> response) {
                EditResponse result = response.body();
                if(result.getStatus() == 200){
                    Toast.makeText(context,"저장이 완료되었습니다.",Toast.LENGTH_SHORT);
                    //여기다가 edit text 등 초기화하는 코드 추가할 것
                }
            }

            @Override
            public void onFailure(Call<EditResponse> call, Throwable t) {
                Toast.makeText(context, "작성에러",Toast.LENGTH_SHORT);
            }
        });
    }

    private void showDateDialog() {
        String birthDateStr = Date_edit.getText().toString();

        Calendar calendar = Calendar.getInstance();
        Date curBirthDate = new Date();
        try {
            curBirthDate = dateFormat.parse(birthDateStr);
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        calendar.setTime(curBirthDate);

        int curYear = calendar.get(Calendar.YEAR);
        int curMonth = calendar.get(Calendar.MONTH);
        int curDay = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(getContext(), R.style.DialogTheme, birthDateSetListener,  curYear, curMonth, curDay);
        dialog.show();
    }

    private DatePickerDialog.OnDateSetListener birthDateSetListener = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            Calendar selectedCalendar = Calendar.getInstance();
            selectedCalendar.set(Calendar.YEAR, year);
            selectedCalendar.set(Calendar.MONTH, monthOfYear);
            selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            Date curDate = selectedCalendar.getTime();
            setSelectedDate(curDate);
        }
    };

    private void setSelectedDate(Date curDate) {
        Selected_date = curDate;

        String selectedDateStr = dateFormat.format(curDate);
        Date_edit.setText(selectedDateStr);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GALLERY_REQUEST_CODE) {
            Uri photoUri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), photoUri);
                edit_image.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}