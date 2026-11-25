package com.taskhabit.academic.asistencia;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.EnumMap;
import java.util.Map;

/** Convierte un texto en un Bitmap con código QR (negro sobre blanco, con margen de 4 módulos). */
public final class QrGenerator {

    private QrGenerator() { }

    public static Bitmap generar(String contenido, int tamanoPx) throws WriterException {
        Map<EncodeHintType, Object> pistas = new EnumMap<>(EncodeHintType.class);
        pistas.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        pistas.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        pistas.put(EncodeHintType.MARGIN, 4); // "zona de silencio" estándar; ayuda al escaneo en modo oscuro

        BitMatrix matriz = new QRCodeWriter()
                .encode(contenido, BarcodeFormat.QR_CODE, tamanoPx, tamanoPx, pistas);

        int ancho = matriz.getWidth();
        int alto = matriz.getHeight();
        int[] pixeles = new int[ancho * alto];
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                pixeles[y * ancho + x] = matriz.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixeles, 0, ancho, 0, 0, ancho, alto);
        return bitmap;
    }
}
