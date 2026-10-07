package com.xiaojidiu.philosophy;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.DataOutputStream;
import java.io.IOException;

/** Standalone test-APK provider cannot use the target APK's Kotlin runtime. */
public final class TestAudioProvider extends ContentProvider {
    @Override public boolean onCreate() {
        File file=new File(getContext().getFilesDir(),"tone.wav");
        int rate=8000,samples=rate*3;
        try(DataOutputStream out=new DataOutputStream(new java.io.FileOutputStream(file))) {
            out.writeBytes("RIFF");writeInt(out,36+samples*2);out.writeBytes("WAVEfmt ");writeInt(out,16);
            writeShort(out,1);writeShort(out,1);writeInt(out,rate);writeInt(out,rate*2);writeShort(out,2);writeShort(out,16);
            out.writeBytes("data");writeInt(out,samples*2);
            for(int i=0;i<samples;i++) writeShort(out,(int)(Math.sin(2*Math.PI*440*i/rate)*2000));
        } catch(IOException error) {throw new IllegalStateException(error);}
        try(java.io.FileOutputStream out=new java.io.FileOutputStream(new File(getContext().getFilesDir(),"invalid"))) {
            out.write(new byte[]{1,2,3,4});
        } catch(IOException error) {throw new IllegalStateException(error);}
        return true;
    }
    private static void writeInt(DataOutputStream out,int value) throws IOException {out.writeInt(Integer.reverseBytes(value));}
    private static void writeShort(DataOutputStream out,int value) throws IOException {out.writeShort(Short.reverseBytes((short)value));}
    private File file(Uri uri) {
        String path=uri.getLastPathSegment();
        if("tone".equals(path) || "tone-b".equals(path)) return new File(getContext().getFilesDir(),"tone.wav");
        if("invalid".equals(path)) return new File(getContext().getFilesDir(),"invalid");
        throw new IllegalArgumentException("Unknown fixture");
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws java.io.FileNotFoundException {
        return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public String getType(Uri uri) {return "audio/wav";}
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] selectionArgs,String sortOrder) {
        String[] columns=projection!=null?projection:new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE};
        Object[] values=new Object[columns.length];
        for(int i=0;i<columns.length;i++) values[i]=OpenableColumns.DISPLAY_NAME.equals(columns[i])?("tone-b".equals(uri.getLastPathSegment())?"xjd-second-tone.wav":"xjd-test-tone.wav"):file(uri).length();
        MatrixCursor cursor=new MatrixCursor(columns);cursor.addRow(values);return cursor;
    }
    @Override public Uri insert(Uri uri,ContentValues values) {return null;}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args) {return 0;}
    @Override public int delete(Uri uri,String selection,String[] args) {return 0;}
}
