package com.example.project1;

import static androidx.camera.core.impl.utils.ContextUtil.getApplicationContext;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MyBottomSheet extends BottomSheetDialogFragment {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private String selectedDate;
    private FirebaseFirestore firestore;
    private ImageButton image1, image2, image3;
    private TextView medicine1, medicine2, medicine3, hospital1, hospital2, hospital3;
    private String currentImageName; // 현재 캡처 중인 이미지 이름
    private static final String[] HOSPITAL_KEYWORDS = {"병원", "의원", "클리닉"};
    private static final String[] MEDICATION_KEYWORDS = {"정", "캡슐", "시럽", "스프레이"}; // 약물 관련 단어

    public static BottomSheetDialogFragment newInstance(String date) {
        MyBottomSheet fragment = new MyBottomSheet();
        Bundle args = new Bundle();
        args.putString("selected_date", date);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedDate = getArguments().getString("selected_date");
        }
        firestore = FirebaseFirestore.getInstance();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_bottom_sheet, container, false);



        image1 = view.findViewById(R.id.image1);
        image2 = view.findViewById(R.id.image2);
        image3 = view.findViewById(R.id.image3);
        medicine1 = view.findViewById(R.id.medicine_line1);
        medicine2 = view.findViewById(R.id.medicine_line2);
        medicine3 = view.findViewById(R.id.medicine_line3);
        hospital1 = view.findViewById(R.id.hospital_line1);
        hospital2 = view.findViewById(R.id.hospital_line2);
        hospital3 = view.findViewById(R.id.hospital_line3);

        File directory = requireContext().getFilesDir();
        File imageFile = new File(directory, selectedDate + "_image1" + ".png");
        File imageFile2 = new File(directory, selectedDate + "_image2" + ".png");
        File imageFile3 = new File(directory, selectedDate + "_image3" + ".png");
        // 로컬 이미지 불러오기
        loadLocalImage(image1, selectedDate + "_image1");
        loadLocalImage(image2, selectedDate + "_image2");
        loadLocalImage(image3, selectedDate + "_image3");
        if(imageFile.exists()) view.findViewById(R.id.minus1).setVisibility(View.VISIBLE);
        if(imageFile2.exists())view.findViewById(R.id.minus2).setVisibility(View.VISIBLE);
        if(imageFile3.exists())view.findViewById(R.id.minus3).setVisibility(View.VISIBLE);



        // Firestore에서 텍스트 데이터 불러오기
        firestore.collection("History_Medicine")
                .document(selectedDate)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String medicine_1 = documentSnapshot.getString("medicine_line1");
                        String medicine_2 = documentSnapshot.getString("medicine_line2");
                        String medicine_3 = documentSnapshot.getString("medicine_line3");
                        medicine1.setText(medicine_1 != null ? medicine_1 : "");
                        medicine2.setText(medicine_2 != null ? medicine_2 : "");
                        medicine3.setText(medicine_3 != null ? medicine_3 : "");
                        String hospital_1 = documentSnapshot.getString("hospital_line1");
                        String hospital_2 = documentSnapshot.getString("hospital_line2");
                        String hospital_3 = documentSnapshot.getString("hospital_line3");
                        hospital1.setText(hospital_1 != null ? hospital_1 : "");
                        hospital2.setText(hospital_1 != null ? hospital_2 : "");
                        hospital3.setText(hospital_1 != null ? hospital_3 : "");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load data", Toast.LENGTH_SHORT).show());

//        내부 저장소 모든 사진 kill 코드


        File[] files = directory.listFiles();
        if (files != null) {
            boolean success = true;

            for (File file : files) {
                // .png 확장자를 가진 파일만 삭제
                if (file.getName().endsWith(".png")) {
                    if (!file.delete()) {
                        success = false; // 삭제 실패한 파일이 있으면 플래그 설정
                    }
                }
            }
        }
        return view;

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //텍스트 클릭 이벤트
        addshowdialog(medicine1);
        addshowdialog(medicine2);
        addshowdialog(medicine3);
        addshowdialog(hospital1);
        addshowdialog(hospital2);
        addshowdialog(hospital3);


        // 카메라 버튼 클릭 이벤트
        view.findViewById(R.id.cameraButton).setOnClickListener(v -> {
            File directory = requireContext().getFilesDir(); // 내부 저장소 디렉토리
            File imageFile = new File(directory, selectedDate + "_image1" + ".png");
            File imageFile2 = new File(directory, selectedDate + "_image2" + ".png");
            File imageFile3 = new File(directory, selectedDate + "_image3" + ".png");
            if (!imageFile.exists()) {
                currentImageName = selectedDate + "_image1";
                captureImage();
                addDeletBt(1);
            } else if (!imageFile2.exists()) {
                currentImageName = selectedDate + "_image2";
                captureImage();
                addDeletBt(2);
            } else if (!imageFile3.exists()) {
                currentImageName = selectedDate + "_image3";
                captureImage();
                addDeletBt(3);
            } else
                Toast.makeText(requireContext(), "사진이 가득 찼습니다. 사진을 지워주세요", Toast.LENGTH_SHORT).show();


        });

        // 갤러리 버튼 클릭 이벤트
        view.findViewById(R.id.GalleryButton).setOnClickListener(v -> {
            File directory = requireContext().getFilesDir(); // 내부 저장소 디렉토리
            File imageFile = new File(directory, selectedDate + "_image1" + ".png");
            File imageFile2 = new File(directory, selectedDate + "_image2" + ".png");
            File imageFile3 = new File(directory, selectedDate + "_image3" + ".png");

            if (!imageFile.exists()) {
                currentImageName = selectedDate + "_image1";
                addDeletBt(1);
                openGallery();
            } else if (!imageFile2.exists()) {
                currentImageName = selectedDate + "_image2";
                addDeletBt(2);
                openGallery();
            } else if (!imageFile3.exists()) {
                currentImageName = selectedDate + "_image3";
                addDeletBt(3);
                openGallery();
            } else
                Toast.makeText(requireContext(), "사진이 가득 찼습니다. 사진을 지워주세요", Toast.LENGTH_SHORT).show();


        });


        //제거 버튼 클릭 이벤트
        view.findViewById(R.id.minus1).setOnClickListener(v->{
            deleteImageFromInternalStorage(1);
        });
        view.findViewById(R.id.minus2).setOnClickListener(v->{
            deleteImageFromInternalStorage(2);
        });
        view.findViewById(R.id.minus3).setOnClickListener(v->{
            deleteImageFromInternalStorage(3);
        });


    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);

    }


    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
    }

    private void saveImageToInternalStorage(Bitmap bitmap, String imageName) {
        File directory = requireContext().getFilesDir(); // 내부 저장소 디렉토리
        File imageFile = new File(directory, imageName + ".png");

        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos); // Bitmap 저장
            Toast.makeText(getContext(), "Image saved successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteImageFromInternalStorage(int position) {
        File directory = requireContext().getFilesDir();
        File imageFile;
        if(position==1) {
            imageFile = new File(directory, selectedDate + "_image1" + ".png");
        }
        else if(position==2) {
            imageFile = new File(directory, selectedDate + "_image2" + ".png");
        }
        else {
            imageFile = new File(directory, selectedDate + "_image3" + ".png");
        }

        if (imageFile.exists()) {
            if (imageFile.delete()) {
                Toast.makeText(getContext(), "Image deleted successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to delete image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "Image not found", Toast.LENGTH_SHORT).show();
        }

        if(position==1){
            getView().findViewById(R.id.minus1).setVisibility(View.INVISIBLE);
            image1.setImageResource(R.drawable.plus_im);
        }
        else if (position==2){
            getView().findViewById(R.id.minus2).setVisibility(View.INVISIBLE);
            image2.setImageResource(R.drawable.plus_im);
        }
        else if (position==3){
            getView().findViewById(R.id.minus3).setVisibility(View.INVISIBLE);
            image3.setImageResource(R.drawable.plus_im);
        }



        if (position==1) {
            medicine1.setText("");
            hospital1.setText("");

            Map<String, Object> data = new HashMap<>();
            data.put("date", Integer.parseInt(selectedDate));
            data.put("medicine_line1", "");
            data.put("hospital_line1", "");

            firestore.collection("History_Medicine")
                    .document(selectedDate)
                    .set(data, SetOptions.merge());

        } else if (position==2) {
            medicine2.setText("");
            hospital2.setText("");

            Map<String, Object> data = new HashMap<>();
            data.put("date", Integer.parseInt(selectedDate));
            data.put("medicine_line2", "");
            data.put("hospital_line2", "");
            firestore.collection("History_Medicine")
                    .document(selectedDate)
                    .set(data, SetOptions.merge());
        } else if (position==3) {
            medicine3.setText("");
            hospital3.setText("");

            Map<String, Object> data = new HashMap<>();
            data.put("date", Integer.parseInt(selectedDate));
            data.put("medicine_line3", "");
            data.put("hospital_line3", "");
            firestore.collection("History_Medicine")
                    .document(selectedDate)
                    .set(data, SetOptions.merge());
        }







    }


    private void loadLocalImage(ImageButton imageButton, String imageName) {
        File directory = requireContext().getFilesDir(); // 내부 저장소 디렉토리
        File imageFile = new File(directory, imageName + ".png");

        if (imageFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            imageButton.setImageBitmap(bitmap); // 이미지 버튼에 설정
        } else {
            imageButton.setImageResource(R.drawable.plus_im); // 기본 이미지 설정
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            //지금 여기 예시로 설정하고있음
            convertToText(imageBitmap);

            if (imageBitmap != null) {
                // 현재 캡처 중인 이미지 저장
                saveImageToInternalStorage(imageBitmap, currentImageName);

                // 해당 버튼에 이미지 설정
                if (currentImageName.endsWith("_image1")) {
                    image1.setImageBitmap(imageBitmap);
                } else if (currentImageName.endsWith("_image2")) {
                    image2.setImageBitmap(imageBitmap);
                } else if (currentImageName.endsWith("_image3")) {
                    image3.setImageBitmap(imageBitmap);
                }
            }
        } else if (requestCode == REQUEST_IMAGE_PICK && data != null && data.getData() != null && resultCode == Activity.RESULT_OK) {
            try {
                Uri imageUri = data.getData();
                Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), imageUri);
                // 2. 텍스트 인식 처리
                convertToText(imageBitmap);

                // 3. 저장 및 버튼 설정 (기존 로직 유지)
                if (imageBitmap != null) {
                    // 현재 캡처 중인 이미지 저장
                    saveImageToInternalStorage(imageBitmap, currentImageName);

                    // 해당 버튼에 이미지 설정
                    if (currentImageName.endsWith("_image1")) {
                        image1.setImageBitmap(imageBitmap);
                    } else if (currentImageName.endsWith("_image2")) {
                        image2.setImageBitmap(imageBitmap);
                    } else if (currentImageName.endsWith("_image3")) {
                        image3.setImageBitmap(imageBitmap);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void convertToText(Bitmap imageBitmap) {
        final String[] resultText = new String[1];

        InputImage imagee = InputImage.fromBitmap(imageBitmap, 0);
        TextRecognizer detector =
                TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());

        Task<Text> result =
                detector.process(imagee)
                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text firebaseVisionText) {
                                // Task completed successfully
                                Log.d("MLKIT","success");
                                resultText[0] = firebaseVisionText.getText();
                                String hospitalName = extractHospitalInfo(firebaseVisionText);
                                String medicationName = extractMedications(firebaseVisionText);

                                if (currentImageName.endsWith("_image1")) {
                                    medicine1.setText(medicationName);
                                    hospital1.setText(hospitalName);

                                    //database에 저장
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("date", Integer.parseInt(selectedDate) );
                                    data.put("medicine_line1", medicationName);
                                    data.put("hospital_line1", hospitalName);

                                    firestore.collection("History_Medicine")
                                            .document(selectedDate)
                                            .set(data, SetOptions.merge());

                                } else if (currentImageName.endsWith("_image2")) {
                                    medicine2.setText(medicationName);
                                    hospital2.setText(hospitalName);

                                    //database에 저장
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("date", Integer.parseInt(selectedDate));
                                    data.put("medicine_line2", medicationName);
                                    data.put("hospital_line2", hospitalName);
                                    firestore.collection("History_Medicine")
                                            .document(selectedDate)
                                            .set(data, SetOptions.merge());
                                } else if (currentImageName.endsWith("_image3")) {
                                    medicine3.setText(medicationName);
                                    hospital3.setText(hospitalName);

                                    //database에 저장
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("date", Integer.parseInt(selectedDate));
                                    data.put("medicine_line3", medicationName);
                                    data.put("hospital_line3", hospitalName);
                                    firestore.collection("History_Medicine")
                                            .document(selectedDate)
                                            .set(data, SetOptions.merge());
                                }


                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                Log.d("MLKIT","failure");
                                e.printStackTrace();
                            }
                        });
    }

    private String extractHospitalInfo(Text firebaseVisionText) {
        // 병원명 추출을 위한 정규식
        String hospitalPattern = "\\b[가-힣]+(병원|의원|센터|클리닉|한의원)\\b";

        StringBuilder hospitalInfo = new StringBuilder();

        for (Text.TextBlock block : firebaseVisionText.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                // 각 줄에서 정규식으로 병원명 추출
                Pattern pattern = Pattern.compile(hospitalPattern);
                Matcher matcher = pattern.matcher(line.getText());

                while (matcher.find()) {
                    String hospitalName = matcher.group().trim();
                    hospitalInfo.append(hospitalName).append("\n");
                }
            }
        }

        return hospitalInfo.toString().trim();
    }



    private String extractMedications(Text firebaseVisionText) {
        StringBuilder medications = new StringBuilder();

        // 약물명을 추출하기 위한 정규식
        String medicationPattern = "\\b[가-힣]+[정|캡슐|액|산|파우더|주사|크림]";

        for (Text.TextBlock block : firebaseVisionText.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                // 각 줄의 텍스트에서 정규식으로 약물명 추출
                Pattern pattern = Pattern.compile(medicationPattern);
                Matcher matcher = pattern.matcher(line.getText());

                while (matcher.find()) {
                    String medicationName = matcher.group().trim();
                    medications.append(medicationName).append(", ");
                }
            }
        }

        if (medications.length() > 0) {
            medications.setLength(medications.length() - 2); // Remove trailing comma and space
        }

        return medications.toString();
    }


    public void addDeletBt(int position) {
        if(position==1) {
            getView().findViewById(R.id.minus1).setVisibility(View.VISIBLE);
        }
        else if(position==2){
            getView().findViewById(R.id.minus2).setVisibility(View.VISIBLE);
        }
        else getView().findViewById(R.id.minus3).setVisibility(View.VISIBLE);

    }

    ;

    public void addshowdialog(TextView view){
        view.setOnClickListener(v->{
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_line, null);
            builder.setView(dialogView);

            EditText editText = dialogView.findViewById(R.id.edit);
            Button closeButton = dialogView.findViewById(R.id.lineconfirm);
            editText.setText(view.getText());

            AlertDialog dialog = builder.create();

            closeButton.setOnClickListener(vv -> {


                String editedtext = editText.getText().toString();
                view.setText(editedtext);
                if(view.getId()==R.id.medicine_line1){
                    //database에 저장
                    Map<String, Object> data = new HashMap<>();
                    data.put("date", Integer.parseInt(selectedDate) );
                    data.put("medicine_line1", editedtext);

                    firestore.collection("History_Medicine")
                            .document(selectedDate)
                            .set(data, SetOptions.merge());
                }
                else if(view.getId()==R.id.medicine_line2){
                    //database에 저장
                    Map<String, Object> data = new HashMap<>();
                    data.put("date", Integer.parseInt(selectedDate) );
                    data.put("medicine_line2", editedtext);

                    firestore.collection("History_Medicine")
                            .document(selectedDate)
                            .set(data, SetOptions.merge());
                }
                else if(view.getId()==R.id.medicine_line3){
                    //database에 저장
                    Map<String, Object> data = new HashMap<>();
                    data.put("date", Integer.parseInt(selectedDate) );
                    data.put("medicine_line3", editedtext);

                    firestore.collection("History_Medicine")
                            .document(selectedDate)
                            .set(data, SetOptions.merge());
                }
                else if(view.getId()==R.id.hospital_line1){
                    //database에 저장
                    Map<String, Object> data = new HashMap<>();
                    data.put("date", Integer.parseInt(selectedDate) );
                    data.put("hospital_line1", editedtext);

                    firestore.collection("History_Medicine")
                            .document(selectedDate)
                            .set(data, SetOptions.merge());
                }
                else if(view.getId()==R.id.hospital_line2){
                    //database에 저장
                    Map<String, Object> data = new HashMap<>();
                    data.put("date", Integer.parseInt(selectedDate) );
                    data.put("hospital_line2", editedtext);

                    firestore.collection("History_Medicine")
                            .document(selectedDate)
                            .set(data, SetOptions.merge());
                }
                else if(view.getId()==R.id.hospital_line3){
                    //database에 저장
                    Map<String, Object> data = new HashMap<>();
                    data.put("date", Integer.parseInt(selectedDate) );
                    data.put("hospital_line3", editedtext);

                    firestore.collection("History_Medicine")
                            .document(selectedDate)
                            .set(data, SetOptions.merge());
                }
                dialog.dismiss();

        });

            dialog.show();
        });
    }



}


//for (FirebaseVisionText.TextBlock block : firebaseVisionText.getTextBlocks()) {
//String blockText = block.getText();
//Float blockConfidence = block.getConfidence();
//List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
//Point[] blockCornerPoints = block.getCornerPoints();
//Rect blockFrame = block.getBoundingBox();
//
//                            for (FirebaseVisionText.Line line : block.getLines()) {
//String lineText = line.getText();
//Float lineConfidence = line.getConfidence();
//List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
//Point[] lineCornerPoints = line.getCornerPoints();
//Rect lineFrame = line.getBoundingBox();
//
//                                for (FirebaseVisionText.Element element : line.getElements()) {
//String elementText = element.getText();
//Float elementConfidence = element.getConfidence();
//List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
//Point[] elementCornerPoints = element.getCornerPoints();
//Rect elementFrame = element.getBoundingBox();
//                                }
//                                        }
//                                        }