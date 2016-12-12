/*
 * Copyright 2016-present The Material Motion Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.material.motion.gestures.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;

import com.google.android.material.motion.gestures.DragGestureRecognizer;
import com.google.android.material.motion.gestures.GestureRecognizer;
import com.google.android.material.motion.gestures.GestureRecognizer.GestureStateChangeListener;
import com.google.android.material.motion.gestures.RotateGestureRecognizer;
import com.google.android.material.motion.gestures.ScaleGestureRecognizer;

import java.util.Locale;

/**
 * Gestures sample Activity.
 */
public class MainActivity extends AppCompatActivity {

  private final DragGestureRecognizer dragGestureRecognizer = new DragGestureRecognizer();
  private final ScaleGestureRecognizer scaleGestureRecognizer = new ScaleGestureRecognizer();
  private final RotateGestureRecognizer rotateGestureRecognizer = new RotateGestureRecognizer();

  private TextView dragText;
  private TextView scaleText;
  private TextView rotateText;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.main_activity);

    View target = findViewById(R.id.target);
    dragText = (TextView) findViewById(R.id.drag_text);
    scaleText = (TextView) findViewById(R.id.scale_text);
    rotateText = (TextView) findViewById(R.id.rotate_text);

    dragGestureRecognizer.addStateChangeListener(stateChangeListener);
    scaleGestureRecognizer.addStateChangeListener(stateChangeListener);
    rotateGestureRecognizer.addStateChangeListener(stateChangeListener);

    target.setOnTouchListener(onTouchListener);
  }

  private final OnTouchListener onTouchListener = new OnTouchListener() {
    @Override
    public boolean onTouch(View v, MotionEvent event) {
      boolean handled = false;
      handled |= dragGestureRecognizer.onTouch(v, event);
      handled |= scaleGestureRecognizer.onTouch(v, event);
      handled |= rotateGestureRecognizer.onTouch(v, event);
      return handled;
    }
  };

  private final GestureStateChangeListener stateChangeListener = new GestureStateChangeListener() {
    @Override
    public void onStateChanged(GestureRecognizer gestureRecognizer) {
      CharSequence string = createDebugString(gestureRecognizer);

      if (gestureRecognizer == dragGestureRecognizer) {
        dragText.setText(string);
      } else if (gestureRecognizer == scaleGestureRecognizer) {
        scaleText.setText(string);
      } else if (gestureRecognizer == rotateGestureRecognizer) {
        rotateText.setText(string);
      }
    }
  };

  private CharSequence createDebugString(GestureRecognizer gestureRecognizer) {
    if (gestureRecognizer == dragGestureRecognizer) {
      return String.format(Locale.getDefault(),
        "Drag\nstate: %d, tx: %.3f, ty: %.3f, cx: %.3f, cy: %.3f, vx: %.3f, vy: %.3f",
        dragGestureRecognizer.getState(),
        dragGestureRecognizer.getTranslationX(),
        dragGestureRecognizer.getTranslationY(),
        dragGestureRecognizer.getCentroidX(),
        dragGestureRecognizer.getCentroidY(),
        dragGestureRecognizer.getVelocityX(),
        dragGestureRecognizer.getVelocityY());
    } else if (gestureRecognizer == scaleGestureRecognizer) {
      return String.format(Locale.getDefault(),
        "Scale\nstate: %d, s: %.3f, cx: %.3f, cy: %.3f, v: %.3f",
        scaleGestureRecognizer.getState(),
        scaleGestureRecognizer.getScale(),
        scaleGestureRecognizer.getCentroidX(),
        scaleGestureRecognizer.getCentroidY(),
        scaleGestureRecognizer.getVelocity());
    } else if (gestureRecognizer == rotateGestureRecognizer) {
      return String.format(Locale.getDefault(),
        "Rotate\nstate: %d, r: %.3f, cx: %.3f, cy: %.3f, v: %.3f",
        rotateGestureRecognizer.getState(),
        rotateGestureRecognizer.getRotation(),
        rotateGestureRecognizer.getCentroidX(),
        rotateGestureRecognizer.getCentroidY(),
        rotateGestureRecognizer.getVelocity());
    }
    return null;
  }
}
