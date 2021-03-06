package cms341.message_encryptor;


import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;

/**
 * Created by nicklheureux on 11/26/16.
 */

public class KeyGenerator extends AppCompatActivity {
    private SharedPreferences prefs;
        Button recordButton;
        String AudioSavePathInDevice = null;
        MediaRecorder mediaRecorder;
        Random random;
        String RandomAudioFileName = "ABCDEFGHIJKLMNOP";
        public static final int RequestPermissionCode = 1;
        private boolean permissionsCheck;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.key_generator);
            prefs = getSharedPreferences("user",MODE_PRIVATE);

            recordButton = (Button) findViewById(R.id.record);
            permissionsCheck = checkPermission();
            if (!permissionsCheck) {
                requestPermission();
            }
            random = new Random();

            recordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    recordButton.setText(R.string.start);
                    AudioSavePathInDevice =
                            Environment.getExternalStorageDirectory().getAbsolutePath() + "/" +
                                    CreateRandomAudioFileName(5) + "AudioRecording.3gp";

                    MediaRecorderReady();

                    try {
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                        Toast.makeText(KeyGenerator.this, R.string.recording_started, Toast.LENGTH_LONG).show();                      recordButton.setText(R.string.recording);
                        recordButton.setEnabled(false);
                        recordButton.setText(R.string.recording);
                        new CountDownTimer(5000, 1000) {
                            public void onTick(long millisUntilFinished) {

                            }

                            public void onFinish() {
                                mediaRecorder.stop();
                                createKey(AudioSavePathInDevice);
                                startActivity(new Intent(getApplicationContext(), KeyArchive.class));
                            }
                        }.start();

                    } catch (IllegalStateException e) {
                        System.err.println("Illegal State" + e);
                        e.printStackTrace();
                    } catch (IOException e) {
                        System.err.println("IO exception" + e);
                        e.printStackTrace();
                    }
                }
            });
        }

        public void MediaRecorderReady() {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
            mediaRecorder.setOutputFile(AudioSavePathInDevice);
            mediaRecorder.setMaxDuration(5000);
        }

        public String CreateRandomAudioFileName(int string) {
            StringBuilder stringBuilder = new StringBuilder( string );
            int i = 0 ;
            while(i < string ) {
                stringBuilder.append(RandomAudioFileName.
                        charAt(random.nextInt(RandomAudioFileName.length())));

                i++;
            }
            return stringBuilder.toString();
        }

        private void requestPermission() {
            ActivityCompat.requestPermissions(KeyGenerator.this, new
                    String[]{WRITE_EXTERNAL_STORAGE, RECORD_AUDIO}, RequestPermissionCode);
        }

        @Override
        public void onRequestPermissionsResult(int requestCode,
                                               String permissions[], int[] grantResults) {
            switch (requestCode) {
                case RequestPermissionCode:
                    if (grantResults.length> 0) {
                        boolean StoragePermission = grantResults[0] ==
                                PackageManager.PERMISSION_GRANTED;
                        boolean RecordPermission = grantResults[1] ==
                                PackageManager.PERMISSION_GRANTED;

                        if (StoragePermission && RecordPermission) {
                            Toast.makeText(KeyGenerator.this, R.string.permission_granted,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(KeyGenerator.this, R.string.permission_denied,Toast.LENGTH_LONG).show();
                        }
                    }
                    break;
            }
        }

        public boolean checkPermission() {
            int result = ContextCompat.checkSelfPermission(getApplicationContext(),
                    WRITE_EXTERNAL_STORAGE);
            int result1 = ContextCompat.checkSelfPermission(getApplicationContext(),
                    RECORD_AUDIO);
            return result == PackageManager.PERMISSION_GRANTED &&
                    result1 == PackageManager.PERMISSION_GRANTED;
         }

        public void createKey(String audioSavePathInDevice) {
            File file = new File(audioSavePathInDevice);
            int size = (int) file.length();
            byte[] bytes = new byte[size];
            try {
                BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                DBManager dbm = new DBManager(this);

                buf.read(bytes, 0, bytes.length);
                buf.close();
                String keyText = Base64.encodeToString(bytes, Base64.DEFAULT);
                String halfText = keyText.substring(keyText.length()/2, keyText.length()-1);
                String key = "";
                Random rand = new Random();
                while (key.length() < 32) {
                    key += halfText.charAt(rand.nextInt(halfText.length() - 1));
                }

                EditText title = (EditText) findViewById(R.id.key_generator);

                dbm.insert(prefs.getString("pass", "password"), title.getText().toString(), key);

            } catch (FileNotFoundException e) {
                System.err.println("File could not be found. " + e);
                e.printStackTrace();
            } catch (IOException e) {
                System.err.println("Input output exception. " + e);
                e.printStackTrace();
            }

        }
}