package com.example.bhati.myemojifier;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.SparseArray;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

/**
 * Created by Bhati on 7/29/2017.
 */

public class Emojify {

    private static final double SMILING_PROB_THRESHOLD = .15;
    private static final double EYE_OPEN_PROB_THRESHOLD = .5;
    private static final float EMOJI_SCALE_FACTOR = .9f;

    public static Bitmap emojifyme(Context context, Bitmap resultantBitmap) {
        FaceDetector faceDetector= new FaceDetector.Builder(context)
                                     .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                                     .setTrackingEnabled(false)
                                     .build();
        Frame frame=new Frame.Builder().setBitmap(resultantBitmap).build();
        SparseArray<Face> faceSparseArray=faceDetector.detect(frame);
        if(faceSparseArray.size()==0) {
            Toast.makeText(context, "No faces detected", Toast.LENGTH_SHORT).show();
            return null;
        }
        Bitmap result=resultantBitmap;
        for(int i=0;i<faceSparseArray.size();i++){
            Face face=faceSparseArray.valueAt(i);
            int emojiId=whichEmoji(face);
            Bitmap emoji= BitmapFactory.decodeResource(context.getResources(),emojiId);
            result=addBitmapToFace(result,emoji,face);
        }
        return result;
    }

    private static Bitmap addBitmapToFace(Bitmap backgroundBitmap, Bitmap emojiBitmap, Face face) {

        // Initialize the results bitmap to be a mutable copy of the original image
        Bitmap resultBitmap = Bitmap.createBitmap(backgroundBitmap.getWidth(),
                backgroundBitmap.getHeight(), backgroundBitmap.getConfig());

        // Scale the emoji so it looks better on the face
        float scaleFactor = EMOJI_SCALE_FACTOR;

        // Determine the size of the emoji to match the width of the face and preserve aspect ratio
        int newEmojiWidth = (int) (face.getWidth() * scaleFactor);
        int newEmojiHeight = (int) (emojiBitmap.getHeight() *
                newEmojiWidth / emojiBitmap.getWidth() * scaleFactor);


        // Scale the emoji
        emojiBitmap = Bitmap.createScaledBitmap(emojiBitmap, newEmojiWidth, newEmojiHeight, false);

        // Determine the emoji position so it best lines up with the face
        float emojiPositionX =
                (face.getPosition().x + face.getWidth() / 2) - emojiBitmap.getWidth() / 2;
        float emojiPositionY =
                (face.getPosition().y + face.getHeight() / 2) - emojiBitmap.getHeight() / 3;

        // Create the canvas and draw the bitmaps to it
        Canvas canvas = new Canvas(resultBitmap);
        canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        canvas.drawBitmap(emojiBitmap, emojiPositionX, emojiPositionY, null);

        return resultBitmap;

    }

    private static int whichEmoji(Face face) {
        float smiling=face.getIsSmilingProbability();
        float leftWink=face.getIsLeftEyeOpenProbability();
        float rightWink=face.getIsRightEyeOpenProbability();

        boolean isSmiling=smiling>SMILING_PROB_THRESHOLD;
        boolean isLeftEyeClosed=leftWink<EYE_OPEN_PROB_THRESHOLD;
        boolean isRightEyeClosed=rightWink<EYE_OPEN_PROB_THRESHOLD;

        Emojis emojis=null;
        if(isSmiling) {
            if (isLeftEyeClosed && !isRightEyeClosed) {
                emojis = Emojis.LEFT_WINK_SMILING;
            }  else if(isRightEyeClosed && !isLeftEyeClosed){
                emojis = Emojis.RIGHT_WINK_SMILING;
            } else if (isLeftEyeClosed){
                emojis = Emojis.CLOSED_EYES_SMILING;
            } else {
                emojis = Emojis.SMILING;
            }
        } else {
            if (isLeftEyeClosed && !isRightEyeClosed) {
                emojis = Emojis.LEFT_WINK_FROWNING;
            }  else if(isRightEyeClosed && !isLeftEyeClosed){
                emojis = Emojis.RIGHT_WINK_FROWNING;
            } else if (isLeftEyeClosed){
                emojis = Emojis.CLOSED_EYES_FROWNING;
            } else {
                emojis = Emojis.FROWNING;
            }
        }
        return emojis.getResId();
        
    }
    private enum Emojis {
        SMILING(R.drawable.smile),
        LEFT_WINK_SMILING(R.drawable.leftwink),
        RIGHT_WINK_SMILING(R.drawable.rightwink),
        CLOSED_EYES_SMILING(R.drawable.closed_smile),
        FROWNING(R.drawable.frown),
        LEFT_WINK_FROWNING(R.drawable.leftwinkfrown),
        RIGHT_WINK_FROWNING(R.drawable.rightwinkfrown),
        CLOSED_EYES_FROWNING(R.drawable.closed_frown);


        int resId;

        Emojis(int resId) {
            this.resId = resId;
        }

        public int getResId() {
            return resId;
        }
    }
}
