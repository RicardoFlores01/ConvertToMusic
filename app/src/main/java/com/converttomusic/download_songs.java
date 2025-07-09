package com.converttomusic;

import android.Manifest;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.bumptech.glide.Glide; // Asegúrate de importar Glide

public class download_songs extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private EditText etYoutubeUrl;
    private ImageView videoThumbnail;
    private Button btnDescargar;
    private Button btnDescargarVideo;

    private ProgressBar progressBar;
    private ImageView btnClean;
    private TextView progressDownload;

    private final String RAPIDAPI_KEY = "b36ae2898amsh9c170c6f019cd0dp1673c7jsn54fe66cb0767";

    private long downloadId = -1;
    private final String CHANNEL_ID = "download_channel";
    private final int REQUEST_CODE_POST_NOTIFICATIONS = 101;

    private DownloadManager downloadManager;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.download_songs);

        createNotificationChannel();

        etYoutubeUrl = findViewById(R.id.editTextUrl);
        btnDescargar = findViewById(R.id.btnDescargar);
        progressBar = findViewById(R.id.progressBar);
        btnClean = findViewById(R.id.btnClean);
        progressBar.setVisibility(View.GONE);
        progressDownload = findViewById(R.id.progressDownload);
        videoThumbnail = findViewById(R.id.imageViewVideo);



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.barraColor));
        }

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerReceiver(onDownloadComplete,
                    new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_NOT_EXPORTED);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS);
            }
        }

        btnDescargar.setOnClickListener(v -> {
            String youtubeUrl = etYoutubeUrl.getText().toString().trim();
            String videoId = extraerVideoId(youtubeUrl);

            if (videoId == null) {
                Toast.makeText(this, "URL inválida", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "URL inválida: " + youtubeUrl);
                return;
            }

            Toast.makeText(this, "Descarga iniciada", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Iniciando descarga para video ID: " + videoId);
         //   mostrarMiniatura(videoId);
            obtenerLinkDeDescarga(videoId);
        });

        btnClean.setOnClickListener(v -> etYoutubeUrl.setText(""));

        btnDescargarVideo = findViewById(R.id.btnDescargarVideo);
        btnDescargarVideo.setOnClickListener(v -> {
            String youtubeUrl = etYoutubeUrl.getText().toString().trim();
            String videoId = extraerVideoId(youtubeUrl);

            if (videoId == null) {
                Toast.makeText(this, "URL inválida", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "URL inválida: " + youtubeUrl);
                return;
            }

            Toast.makeText(this, "Descarga de video iniciada", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Iniciando descarga de video para video ID: " + videoId);
           // mostrarMiniatura(videoId);
            obtenerLinkDeDescargaMp4(videoId);
        });
    }

    private void mostrarMiniatura(String videoId) {
        runOnUiThread(() -> {
            String thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
            videoThumbnail.setVisibility(View.VISIBLE);
            Glide.with(download_songs.this).load(thumbnailUrl).into(videoThumbnail);
        });
    }


        private void obtenerLinkDeDescargaMp4(String videoId) {
    new Thread(() -> {
        try {
            URL url = new URL("https://ytstream-download-youtube-videos.p.rapidapi.com/dl?id=" + videoId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("x-rapidapi-key", "b36ae2898amsh9c170c6f019cd0dp1673c7jsn54fe66cb0767");
            connection.setRequestProperty("x-rapidapi-host", "ytstream-download-youtube-videos.p.rapidapi.com");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }

            reader.close();
            connection.disconnect();

            Log.i(TAG, "Respuesta del servidor (video): " + result);

            JSONObject json = new JSONObject(result.toString());
            String status = json.getString("status");

            if ("OK".equals(status)) {
                String title = json.getString("title");
                String link = null;

                // Ejemplo para JSON con un array "formats" donde está el link
                if (json.has("formats")) {
                    JSONArray formats = json.getJSONArray("formats");
                    for (int i = 0; i < formats.length(); i++) {
                        JSONObject format = formats.getJSONObject(i);
                        String quality = format.optString("quality");
                        if ("720p".equals(quality)) {  // Busca la calidad que prefieras
                            link = format.getString("url");
                            break;
                        }
                    }
                    if (link == null && formats.length() > 0) {
                        // Si no encuentra 720p, toma la primera
                        link = formats.getJSONObject(0).getString("url");
                    }
                } else if (json.has("url")) {
                    // Si hay un campo url directo
                    link = json.getString("url");
                }

                if (link != null) {
                    String finalLink = link;
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Descargando video: " + title, Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "Link de descarga MP4: " + finalLink);
                        descargarArchivoMp4(finalLink, title);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "No se encontró link de descarga", Toast.LENGTH_SHORT).show());
                    Log.e(TAG, "No se encontró link en el JSON");
                }

            } else {
                runOnUiThread(() -> Toast.makeText(this, "No se pudo obtener el MP4", Toast.LENGTH_SHORT).show());
                Log.e(TAG, "Estado de respuesta video: " + status);
            }

        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(this, "Error video: " + e.getMessage(), Toast.LENGTH_LONG).show());
            Log.e(TAG, "Error en obtenerLinkDeDescargaMp4", e);
        }
    }).start();
}


    private void descargarArchivoMp4(String linkDescarga, String tituloArchivo) {
        try {
             Log.d("VIDEO_URL", "URL del video: " + linkDescarga);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(linkDescarga));
            request.setTitle(tituloArchivo);
            request.setDescription("Descargando video...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, tituloArchivo + ".mp4");

            downloadId = downloadManager.enqueue(request);

            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);
            handler.post(updateProgressRunnable);
        } catch (Exception e) {
            Log.e(TAG, "Error iniciando descarga MP4", e);
            runOnUiThread(() -> Toast.makeText(this, "Error al iniciar la descarga de video", Toast.LENGTH_LONG).show());
        }
    }



    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Descargas";
            String description = "Notificaciones de descargas completadas";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private String extraerVideoId(String url) {
        try {
            Uri uri = Uri.parse(url);
            String videoId = uri.getQueryParameter("v");
            if (videoId != null && videoId.length() == 11) return videoId;

            if (url.contains("youtu.be/")) {
                String lastPathSegment = uri.getLastPathSegment();
                if (lastPathSegment != null && lastPathSegment.length() == 11) return lastPathSegment;
            }

            String pattern = ".*(?:v=|/)([a-zA-Z0-9_-]{11})(?:\\?|&|\\b)";
            java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = compiledPattern.matcher(url);
            if (matcher.find()) return matcher.group(1);
        } catch (Exception e) {
            Log.e(TAG, "Error extrayendo ID del video", e);
        }
        return null;
    }

    private void obtenerLinkDeDescarga(String videoId) {
        new Thread(() -> {
            try {
                URL url = new URL("https://youtube-mp36.p.rapidapi.com/dl?id=" + videoId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("x-rapidapi-key", RAPIDAPI_KEY);
                connection.setRequestProperty("x-rapidapi-host", "youtube-mp36.p.rapidapi.com");

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                );

                StringBuilder result = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                connection.disconnect();

                Log.i(TAG, "Respuesta del servidor: " + result);

                JSONObject json = new JSONObject(result.toString());
                String status = json.getString("status");

                if ("ok".equals(status)) {
                    String link = json.getString("link");
                    String title = json.getString("title");

                    runOnUiThread(() -> {
                        Toast.makeText(this, "Descargando: " + title, Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "Link de descarga: " + link);
                        descargarArchivoDesdeUrl(link, title);
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "No se pudo obtener el MP3", Toast.LENGTH_SHORT).show());
                    Log.e(TAG, "Estado de respuesta: " + status);
                }

            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                Log.e(TAG, "Error en obtenerLinkDeDescarga", e);
            }
        }).start();
    }

    private void descargarArchivoDesdeUrl(String linkDescarga, String tituloArchivo) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(linkDescarga));
            request.setTitle(tituloArchivo);
            request.setDescription("Descargando audio...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, tituloArchivo + ".mp3");

            downloadId = downloadManager.enqueue(request);

            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);

            handler.post(updateProgressRunnable);
        } catch (Exception e) {
            Log.e(TAG, "Error iniciando descarga", e);
            runOnUiThread(() -> Toast.makeText(this, "Error al iniciar la descarga", Toast.LENGTH_LONG).show());
        }
    }

    private final Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            try (Cursor cursor = downloadManager.query(query)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int bytesDownloaded = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int bytesTotal = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                    if (bytesTotal > 0) {
                        int progress = (int) ((bytesDownloaded * 100L) / bytesTotal);
                        progressBar.setProgress(progress);
                        Log.d(TAG, "Progreso de descarga: " + progress + "%");
                        progressDownload.setText(progress + "%");
                        if(progress == 100){
                            progressDownload.setText("");
                        }

                    }

                    int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        progressBar.setVisibility(View.GONE);
                        handler.removeCallbacks(this);
                        Log.i(TAG, "Descarga finalizada. Estado: " + status);
                    } else {
                        handler.postDelayed(this, 500);
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    handler.removeCallbacks(this);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error actualizando progreso", e);
            }
        }
    };

    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == downloadId) {
                Toast.makeText(context, "Descarga finalizada", Toast.LENGTH_LONG).show();

                Intent intentOpen = new Intent(context, MainActivity.class);
                intentOpen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intentOpen,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setContentTitle("Descarga completada")
                        .setContentText("El archivo se descargó correctamente.")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                notificationManager.notify(1001, builder.build());
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
        handler.removeCallbacks(updateProgressRunnable);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de notificaciones concedido", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
